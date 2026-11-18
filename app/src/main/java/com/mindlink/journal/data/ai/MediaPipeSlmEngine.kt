package com.mindlink.journal.data.ai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.mindlink.journal.domain.model.LifeSphereClassifier
import com.mindlink.journal.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-Device Small Language Model (SLM) Brainstorming Engine powered by Google MediaPipe GenAI.
 * Supports executing quantized on-device models such as Qwen 2.5 (0.5B), Gemma 2B, or SmolLM2.
 * 100% offline, privacy-first, zero cloud API key required.
 */
class MediaPipeSlmEngine(
    private val context: Context,
    private val fallbackEngine: LocalHeuristicBrainstormingEngine = LocalHeuristicBrainstormingEngine()
) : BrainstormingEngine {

    private val gson = Gson()
    private var llmInference: LlmInference? = null
    private var activeModelFile: File? = null

    // Lazy initialization: SLM is loaded strictly on-demand, never on app startup.

    /**
     * Locates any valid SLM model (.bin or .task) in internal filesDir or external app storage.
     */
    fun findModelFile(): File? {
        val candidateDirs = listOfNotNull(
            File(context.filesDir, "models"),
            context.getExternalFilesDir("models")
        )
        for (dir in candidateDirs) {
            if (dir.exists() && dir.isDirectory) {
                val modelFile = dir.listFiles()?.firstOrNull { file ->
                    file.isFile && (file.extension == "bin" || file.extension == "task") && file.length() > 20 * 1024 * 1024 // >20MB
                }
                if (modelFile != null) return modelFile
            }
        }
        return null
    }

    fun isModelAvailable(): Boolean {
        return llmInference != null || findModelFile() != null
    }

    fun getModelInfo(): String {
        val file = activeModelFile ?: findModelFile()
        return if (file != null) {
            val sizeMb = file.length() / (1024 * 1024)
            "${file.name} (${sizeMb} MB)"
        } else {
            "Not installed"
        }
    }

    @Synchronized
    fun reloadModel(): Boolean {
        try {
            llmInference?.close()
        } catch (_: Exception) {}
        llmInference = null
        activeModelFile = null
        return findAndInitModel()
    }

    suspend fun generateRawResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val initialized = synchronized(this@MediaPipeSlmEngine) { findAndInitModel() }
        if (!initialized || llmInference == null) {
            return@withContext "SLM model not loaded or unavailable on device."
        }
        try {
            llmInference?.generateResponse(prompt) ?: "Empty response from on-device SLM."
        } catch (e: Exception) {
            "Inference error: ${e.message}"
        }
    }

    @Synchronized
    fun findAndInitModel(): Boolean {
        if (llmInference != null) return true
        val modelFile = findModelFile() ?: return false
        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(512)
                .setTemperature(0.7f)
                .setTopK(40)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            activeModelFile = modelFile
            Log.i(TAG, "MediaPipe SLM initialized successfully with ${modelFile.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe SLM with ${modelFile.name}: ${e.message}", e)
            llmInference = null
            false
        }
    }

    override suspend fun analyzeNote(title: String, content: String): NoteAnalysisResult = withContext(Dispatchers.IO) {
        val classification = LifeSphereClassifier.classify(title, content)
        val initialized = synchronized(this@MediaPipeSlmEngine) { findAndInitModel() }

        if (!initialized || llmInference == null) {
            Log.d(TAG, "On-device SLM model file not present; using Context-Siloed Local Engine")
            return@withContext fallbackEngine.analyzeNote(title, content)
        }

        val prompt = """
            You are MindLink on-device AI assistant.
            Life Domain: ${classification.primarySphere.displayName}
            Key Entities: ${classification.detectedEntities.joinToString()}

            Analyze this journal entry and return ONLY a strict JSON object with these keys:
            - "summary": 1-sentence TL;DR summary (under 25 words).
            - "mood": Dominant mood ("Reflective", "Analytical", "Creative", "Energized", "Stressed", "Calm").
            - "tags": Array of 2 to 4 topic tags.
            - "actionItems": Array of concrete extracted follow-up tasks (or empty []).
            - "reflectionPrompts": Array of 2 to 3 deep Socratic questions anchored in the domain.
            - "contextualReminder": Optional object (or null) with "triggerPattern", "question", "suggestedDelayMinutes".

            STRICT RULE: Match the domain (${classification.primarySphere.displayName}).
            Title: $title
            Content:
            $content
        """.trimIndent()

        try {
            val responseText = llmInference?.generateResponse(prompt)
            if (!responseText.isNullOrBlank()) {
                val parsed = parseJsonResponse(responseText)
                if (parsed != null) return@withContext parsed
            }
        } catch (e: Exception) {
            Log.w(TAG, "SLM inference error: ${e.message}, falling back to local heuristic engine", e)
        }

        fallbackEngine.analyzeNote(title, content)
    }

    override suspend fun synthesizeNotes(query: String, notes: List<Note>): SynthesisResult = withContext(Dispatchers.IO) {
        val initialized = synchronized(this@MediaPipeSlmEngine) { findAndInitModel() }
        if (!initialized || llmInference == null) {
            return@withContext fallbackEngine.synthesizeNotes(query, notes)
        }

        // Limit retrieved notes to top 3 to respect SLM context window limits
        val topNotes = notes.take(3)
        val contextBuilder = StringBuilder()
        topNotes.forEachIndexed { idx, n ->
            contextBuilder.append("Entry #${idx + 1} [Title: ${n.title}]:\n${n.previewText}\n\n")
        }

        val prompt = """
            Synthesize these journal entries for the question: "$query"
            Notes:
            $contextBuilder

            Provide a thoughtful, concise synthesis (under 80 words) and 2 key takeaways.
        """.trimIndent()

        try {
            val response = llmInference?.generateResponse(prompt)
            if (!response.isNullOrBlank()) {
                return@withContext SynthesisResult(
                    answer = response.trim(),
                    keyTakeaways = topNotes.map { "${it.title}: ${it.summary ?: it.previewText}" }
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "SLM synthesis error: ${e.message}", e)
        }

        fallbackEngine.synthesizeNotes(query, notes)
    }

    private fun parseJsonResponse(raw: String): NoteAnalysisResult? {
        return try {
            val cleanJson = raw.substringAfter("{", "").substringBeforeLast("}", "")
            if (cleanJson.isBlank()) return null
            val jsonObject = gson.fromJson("{$cleanJson}", JsonObject::class.java)

            val summary = jsonObject.get("summary")?.asString ?: "Thought synthesis"
            val mood = jsonObject.get("mood")?.asString ?: "Reflective"
            val tags = jsonObject.getAsJsonArray("tags")?.map { it.asString } ?: emptyList()
            val actionItems = jsonObject.getAsJsonArray("actionItems")?.map { it.asString } ?: emptyList()
            val reflectionPrompts = jsonObject.getAsJsonArray("reflectionPrompts")?.map { it.asString } ?: emptyList()

            var reminder: ContextualReminder? = null
            val remObj = jsonObject.getAsJsonObject("contextualReminder")
            if (remObj != null && !remObj.isJsonNull) {
                reminder = ContextualReminder(
                    triggerPattern = remObj.get("triggerPattern")?.asString ?: "Mindful Reminder",
                    question = remObj.get("question")?.asString ?: "",
                    suggestedDelayMinutes = remObj.get("suggestedDelayMinutes")?.asLong ?: 120L
                )
            }

            NoteAnalysisResult(
                summary = summary,
                mood = mood,
                tags = tags,
                actionItems = actionItems,
                reflectionPrompts = reflectionPrompts,
                contextualReminder = reminder
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse SLM JSON output: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "MediaPipeSlmEngine"
    }
}

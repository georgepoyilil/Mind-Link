package com.mindlink.journal.data.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.mindlink.journal.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiBrainstormingEngine(
    private var apiKeyProvider: () -> String?
) : BrainstormingEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun analyzeNote(title: String, content: String): NoteAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            throw IllegalStateException("Gemini API key is not configured")
        }

        val prompt = """
            Analyze the following journal entry and return ONLY a strict JSON object with these exact keys:
            - "summary": A concise 1-sentence TL;DR summary (under 25 words).
            - "mood": The single dominant mood/sentiment (one of: "Reflective", "Analytical", "Creative", "Energized", "Stressed", "Calm").
            - "tags": Array of 2 to 5 relevant topic or category strings.
            - "actionItems": Array of concrete extracted follow-up tasks or to-dos mentioned in or implied by the note (if none, return empty array).
            - "reflectionPrompts": Array of 2 to 3 thought-provoking follow-up reflection questions or constructive counter-arguments to deepen the user's thinking.

            Journal Title: $title
            Journal Content:
            $content
        """.trimIndent()

        val responseJson = callGeminiApi(apiKey, prompt, jsonSchemaMode = true)
        parseAnalysisResponse(responseJson)
    }

    override suspend fun synthesizeNotes(query: String, notes: List<Note>): SynthesisResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            throw IllegalStateException("Gemini API key is not configured")
        }

        val contextBuilder = StringBuilder()
        notes.forEachIndexed { idx, n ->
            contextBuilder.append("Entry #${idx + 1} [ID: ${n.id}, Date: ${n.formattedDate}, Title: ${n.title}]:\n")
            contextBuilder.append("${n.content}\n\n")
        }

        val prompt = """
            You are MindLink Second Brain, an intelligent associative memory assistant.
            The user asked the following question about their past thoughts and notes:
            "$query"

            Relevant notes retrieved from their journal:
            $contextBuilder

            Synthesize an answer addressing their question directly based ONLY on their notes.
            Return ONLY a strict JSON object with:
            - "answer": A helpful, conversational synthesis answering the user's query with references to their entries.
            - "keyTakeaways": Array of 2 to 4 bullet points highlighting key patterns, blockers, or recurring themes.
        """.trimIndent()

        val responseJson = callGeminiApi(apiKey, prompt, jsonSchemaMode = true)
        parseSynthesisResponse(responseJson)
    }

    private fun callGeminiApi(apiKey: String, prompt: String, jsonSchemaMode: Boolean): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val payload = JsonObject().apply {
            val contentsArray = com.google.gson.JsonArray().apply {
                val contentObj = JsonObject().apply {
                    val partsArray = com.google.gson.JsonArray().apply {
                        val part = JsonObject().apply { addProperty("text", prompt) }
                        add(part)
                    }
                    add("parts", partsArray)
                }
                add(contentObj)
            }
            add("contents", contentsArray)

            if (jsonSchemaMode) {
                val generationConfig = JsonObject().apply {
                    addProperty("response_mime_type", "application/json")
                    addProperty("temperature", 0.3)
                }
                add("generationConfig", generationConfig)
            }
        }

        val body = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Gemini API error: HTTP ${response.code} - $errorBody")
                throw RuntimeException("Gemini API error ${response.code}: $errorBody")
            }

            val respString = response.body?.string() ?: ""
            return extractTextFromGeminiResponse(respString)
        }
    }

    private fun extractTextFromGeminiResponse(json: String): String {
        val root = gson.fromJson(json, JsonObject::class.java)
        val candidates = root.getAsJsonArray("candidates")
        if (candidates != null && candidates.size() > 0) {
            val firstCandidate = candidates[0].asJsonObject
            val content = firstCandidate.getAsJsonObject("content")
            val parts = content.getAsJsonArray("parts")
            if (parts != null && parts.size() > 0) {
                return parts[0].asJsonObject.get("text").asString
            }
        }
        throw RuntimeException("Unexpected response format from Gemini API: $json")
    }

    private fun parseAnalysisResponse(text: String): NoteAnalysisResult {
        val clean = cleanJsonMarkdown(text)
        val dto = gson.fromJson(clean, NoteAnalysisDto::class.java)
        return NoteAnalysisResult(
            summary = dto.summary ?: "",
            mood = dto.mood ?: "Reflective",
            tags = dto.tags ?: emptyList(),
            actionItems = dto.actionItems ?: emptyList(),
            reflectionPrompts = dto.reflectionPrompts ?: emptyList()
        )
    }

    private fun parseSynthesisResponse(text: String): SynthesisResult {
        val clean = cleanJsonMarkdown(text)
        val dto = gson.fromJson(clean, SynthesisDto::class.java)
        return SynthesisResult(
            answer = dto.answer ?: clean,
            keyTakeaways = dto.keyTakeaways ?: emptyList()
        )
    }

    private fun cleanJsonMarkdown(text: String): String {
        var trimmed = text.trim()
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.removePrefix("```json").trim()
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.removePrefix("```").trim()
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.removeSuffix("```").trim()
        }
        return trimmed
    }

    private data class NoteAnalysisDto(
        @SerializedName("summary") val summary: String?,
        @SerializedName("mood") val mood: String?,
        @SerializedName("tags") val tags: List<String>?,
        @SerializedName("actionItems") val actionItems: List<String>?,
        @SerializedName("reflectionPrompts") val reflectionPrompts: List<String>?
    )

    private data class SynthesisDto(
        @SerializedName("answer") val answer: String?,
        @SerializedName("keyTakeaways") val keyTakeaways: List<String>?
    )

    companion object {
        private const val TAG = "GeminiEngine"
    }
}

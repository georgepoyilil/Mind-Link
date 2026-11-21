package com.mindlink.journal.data.ai

import android.util.Log
import com.mindlink.journal.domain.model.Note

/**
 * Intelligent Tri-Tier Brainstorming Engine with User Engine Switching:
 * - "AUTO": Gemini Cloud (if key) -> MediaPipe SLM (if model file) -> Local NLP
 * - "SLM": Forces On-Device SLM (MediaPipe Tasks GenAI)
 * - "GEMINI": Forces Gemini Flash API
 * - "LOCAL": Forces Local Life-Sphere NLP Engine
 */
class CompositeBrainstormingEngine(
    private val geminiEngine: GeminiBrainstormingEngine,
    val mediaPipeEngine: MediaPipeSlmEngine,
    val localEngine: LocalHeuristicBrainstormingEngine = LocalHeuristicBrainstormingEngine(),
    private val hasApiKey: () -> Boolean,
    private val engineModeProvider: () -> String = { "AUTO" }
) : BrainstormingEngine {

    override suspend fun analyzeNote(title: String, content: String): NoteAnalysisResult {
        val mode = engineModeProvider()

        if (mode == "SLM") {
            if (mediaPipeEngine.isModelAvailable()) {
                try {
                    return mediaPipeEngine.analyzeNote(title, content)
                } catch (e: Exception) {
                    Log.w(TAG, "Forced SLM failed: ${e.message}, falling back to local engine")
                }
            }
            return localEngine.analyzeNote(title, content)
        }

        if (mode == "GEMINI") {
            if (hasApiKey()) {
                try {
                    return geminiEngine.analyzeNote(title, content)
                } catch (e: Exception) {
                    Log.w(TAG, "Forced Gemini failed: ${e.message}, falling back to local engine")
                }
            }
            return localEngine.analyzeNote(title, content)
        }

        if (mode == "LOCAL") {
            return localEngine.analyzeNote(title, content)
        }

        // Default "AUTO" mode
        if (hasApiKey()) {
            try {
                return geminiEngine.analyzeNote(title, content)
            } catch (e: Exception) {
                Log.w(TAG, "Gemini analysis failed (${e.message}), trying on-device SLM")
            }
        }

        if (mediaPipeEngine.isModelAvailable()) {
            try {
                return mediaPipeEngine.analyzeNote(title, content)
            } catch (e: Exception) {
                Log.w(TAG, "MediaPipe SLM analysis failed (${e.message}), falling back to local engine")
            }
        }

        return localEngine.analyzeNote(title, content)
    }

    override suspend fun synthesizeNotes(query: String, notes: List<Note>): SynthesisResult {
        val mode = engineModeProvider()

        if (mode == "SLM") {
            if (mediaPipeEngine.isModelAvailable()) {
                try {
                    return mediaPipeEngine.synthesizeNotes(query, notes)
                } catch (e: Exception) {
                    Log.w(TAG, "Forced SLM synthesis failed: ${e.message}")
                }
            }
            return localEngine.synthesizeNotes(query, notes)
        }

        if (mode == "GEMINI") {
            if (hasApiKey()) {
                try {
                    return geminiEngine.synthesizeNotes(query, notes)
                } catch (e: Exception) {
                    Log.w(TAG, "Forced Gemini synthesis failed: ${e.message}")
                }
            }
            return localEngine.synthesizeNotes(query, notes)
        }

        if (mode == "LOCAL") {
            return localEngine.synthesizeNotes(query, notes)
        }

        // AUTO
        if (hasApiKey()) {
            try {
                return geminiEngine.synthesizeNotes(query, notes)
            } catch (e: Exception) {
                Log.w(TAG, "Gemini synthesis failed (${e.message}), trying on-device SLM")
            }
        }

        if (mediaPipeEngine.isModelAvailable()) {
            try {
                return mediaPipeEngine.synthesizeNotes(query, notes)
            } catch (e: Exception) {
                Log.w(TAG, "MediaPipe SLM synthesis failed (${e.message}), falling back to local engine")
            }
        }

        return localEngine.synthesizeNotes(query, notes)
    }

    fun getActiveEngineName(): String {
        val mode = engineModeProvider()
        return when (mode) {
            "SLM" -> "On-Device SLM (Forced: ${mediaPipeEngine.getModelInfo()})"
            "GEMINI" -> "Gemini 2.0 Flash (Forced Cloud)"
            "LOCAL" -> "Local Life-Sphere NLP Engine (Forced)"
            else -> {
                when {
                    hasApiKey() -> "Auto: Gemini 2.0 Flash (Cloud SOTA)"
                    mediaPipeEngine.isModelAvailable() -> "Auto: On-Device SLM (${mediaPipeEngine.getModelInfo()})"
                    else -> "Auto: Local Life-Sphere NLP Engine (Zero Weights)"
                }
            }
        }
    }

    override suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String): String {
        if (!hasApiKey()) {
            throw IllegalStateException("A Gemini API key is required to transcribe audio recordings. Please set your free key in Settings, or use the live Microphone button to dictate in real-time.")
        }
        return geminiEngine.transcribeAudio(audioBytes, mimeType)
    }

    companion object {
        private const val TAG = "CompositeBrainstorming"
    }
}

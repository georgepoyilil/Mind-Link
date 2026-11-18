package com.mindlink.journal.data.ai

import android.util.Log
import com.mindlink.journal.domain.model.Note

/**
 * Intelligent Tri-Tier Brainstorming Engine:
 * 1. Cloud Tier: Gemini Flash (if API key is present and device is online)
 * 2. On-Device SLM Tier: MediaPipe GenAI (Qwen 2.5 / Gemma / SmolLM2) running 100% locally
 * 3. Domain-Siloed Local Tier: Instant, zero-weight semantic classifier with Life Spheres
 */
class CompositeBrainstormingEngine(
    private val geminiEngine: GeminiBrainstormingEngine,
    val mediaPipeEngine: MediaPipeSlmEngine,
    val localEngine: LocalHeuristicBrainstormingEngine = LocalHeuristicBrainstormingEngine(),
    private val hasApiKey: () -> Boolean
) : BrainstormingEngine {

    override suspend fun analyzeNote(title: String, content: String): NoteAnalysisResult {
        if (hasApiKey()) {
            try {
                return geminiEngine.analyzeNote(title, content)
            } catch (e: Exception) {
                Log.w(TAG, "Gemini analysis failed (${e.message}), trying on-device engines")
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
        if (hasApiKey()) {
            try {
                return geminiEngine.synthesizeNotes(query, notes)
            } catch (e: Exception) {
                Log.w(TAG, "Gemini synthesis failed (${e.message}), trying on-device engines")
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
        return when {
            hasApiKey() -> "Gemini 2.0 Flash (Cloud SOTA)"
            mediaPipeEngine.isModelAvailable() -> "On-Device SLM (${mediaPipeEngine.getModelInfo()})"
            else -> "Local Life-Sphere NLP Engine (Zero Weights)"
        }
    }

    companion object {
        private const val TAG = "CompositeBrainstorming"
    }
}

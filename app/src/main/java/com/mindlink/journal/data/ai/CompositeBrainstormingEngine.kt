package com.mindlink.journal.data.ai

import android.util.Log
import com.mindlink.journal.domain.model.Note

/**
 * Composite engine that routes requests to Gemini Flash when configured and online,
 * or transparently falls back to the client-side local heuristic engine.
 */
class CompositeBrainstormingEngine(
    private val geminiEngine: GeminiBrainstormingEngine,
    private val localEngine: LocalHeuristicBrainstormingEngine = LocalHeuristicBrainstormingEngine(),
    private val hasApiKey: () -> Boolean
) : BrainstormingEngine {

    override suspend fun analyzeNote(title: String, content: String): NoteAnalysisResult {
        if (hasApiKey()) {
            try {
                return geminiEngine.analyzeNote(title, content)
            } catch (e: Exception) {
                Log.w(TAG, "Gemini analysis failed (${e.message}), falling back to local engine")
            }
        }
        return localEngine.analyzeNote(title, content)
    }

    override suspend fun synthesizeNotes(query: String, notes: List<Note>): SynthesisResult {
        if (hasApiKey()) {
            try {
                return geminiEngine.synthesizeNotes(query, notes)
            } catch (e: Exception) {
                Log.w(TAG, "Gemini synthesis failed (${e.message}), falling back to local engine")
            }
        }
        return localEngine.synthesizeNotes(query, notes)
    }

    companion object {
        private const val TAG = "CompositeBrainstorming"
    }
}

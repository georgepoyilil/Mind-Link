package com.mindlink.journal.data.ai

import com.mindlink.journal.domain.model.Note

data class NoteAnalysisResult(
    val summary: String,
    val mood: String,
    val tags: List<String>,
    val actionItems: List<String>,
    val reflectionPrompts: List<String>
)

data class SynthesisResult(
    val answer: String,
    val keyTakeaways: List<String>
)

/**
 * Interface for AI-powered note analysis, metadata extraction, reflection prompts,
 * and conversational multi-note synthesis.
 */
interface BrainstormingEngine {
    suspend fun analyzeNote(title: String, content: String): NoteAnalysisResult
    suspend fun synthesizeNotes(query: String, notes: List<Note>): SynthesisResult
}

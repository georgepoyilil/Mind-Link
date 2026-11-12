package com.mindlink.journal.domain.model

data class SynthesisResponse(
    val query: String,
    val synthesisText: String,
    val citedNotes: List<Note> = emptyList(),
    val keyTakeaways: List<String> = emptyList()
)

package com.mindlink.journal.domain.model

data class SearchResult(
    val note: Note,
    val hybridScore: Float,
    val isSemanticMatch: Boolean = false,
    val isKeywordMatch: Boolean = false
)

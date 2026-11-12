package com.mindlink.journal.domain.model

import kotlin.math.roundToInt

data class RelatedThought(
    val note: Note,
    val similarityScore: Float,
    val sharedConcepts: List<String> = emptyList()
) {
    val similarityPercentage: Int
        get() = (similarityScore.coerceIn(0f, 1f) * 100).roundToInt()
}

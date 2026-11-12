package com.mindlink.journal.data.ai

/**
 * Interface for generating dense vector embeddings from text for semantic search.
 */
interface EmbeddingEngine {
    val dimension: Int
    suspend fun generateEmbedding(text: String): FloatArray
}

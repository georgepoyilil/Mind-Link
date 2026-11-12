package com.mindlink.journal.data.ai

import com.mindlink.journal.data.local.entity.NoteEmbeddingEntity
import kotlin.math.sqrt

class VectorSearchEngine {

    /**
     * Calculates cosine similarity between two float vectors.
     * Returns a float in [-1.0, 1.0], typically [0.0, 1.0] for normalized embeddings.
     */
    fun cosineSimilarity(vecA: FloatArray, vecB: FloatArray): Float {
        if (vecA.isEmpty() || vecB.isEmpty() || vecA.size != vecB.size) return 0.0f

        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in vecA.indices) {
            val a = vecA[i]
            val b = vecB[i]
            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        if (normA == 0.0f || normB == 0.0f) return 0.0f

        val similarity = dotProduct / (sqrt(normA) * sqrt(normB))
        return similarity.coerceIn(0.0f, 1.0f)
    }

    /**
     * Finds top-K most similar embeddings to the query vector, excluding a specific note ID (e.g. self).
     */
    fun findTopKSimilar(
        queryVector: FloatArray,
        candidates: List<NoteEmbeddingEntity>,
        excludeNoteId: Long? = null,
        topK: Int = 3,
        threshold: Float = 0.02f
    ): List<Pair<Long, Float>> {
        return candidates
            .asSequence()
            .filter { excludeNoteId == null || it.noteId != excludeNoteId }
            .map { candidate ->
                val similarity = cosineSimilarity(queryVector, candidate.vector)
                candidate.noteId to similarity
            }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .take(topK)
            .toList()
    }

    /**
     * Reciprocal Rank Fusion (RRF) combining keyword and semantic ranks into a unified score.
     */
    fun reciprocalRankFusion(
        keywordRankedIds: List<Long>,
        semanticRankedMatches: List<Pair<Long, Float>>,
        kConstant: Float = 60.0f
    ): List<Pair<Long, Float>> {
        val scores = mutableMapOf<Long, Float>()

        keywordRankedIds.forEachIndexed { index, id ->
            val rank = index + 1
            val rrf = 1.0f / (kConstant + rank)
            scores[id] = (scores[id] ?: 0f) + (rrf * 1.2f)
        }

        semanticRankedMatches.forEachIndexed { index, pair ->
            val id = pair.first
            val similarity = pair.second
            val rank = index + 1
            val rrf = (1.0f / (kConstant + rank)) + (similarity * 0.05f)
            scores[id] = (scores[id] ?: 0f) + rrf
        }

        return scores.toList()
            .sortedByDescending { it.second }
    }
}

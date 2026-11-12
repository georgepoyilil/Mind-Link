package com.mindlink.journal

import com.mindlink.journal.data.ai.VectorSearchEngine
import com.mindlink.journal.data.local.entity.NoteEmbeddingEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VectorSearchEngineTest {

    private lateinit var engine: VectorSearchEngine

    @Before
    fun setUp() {
        engine = VectorSearchEngine()
    }

    @Test
    fun testCosineSimilarity_identicalVectors_returnsOne() {
        val v1 = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)
        val v2 = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)

        val similarity = engine.cosineSimilarity(v1, v2)
        assertEquals(1.0f, similarity, 0.001f)
    }

    @Test
    fun testCosineSimilarity_orthogonalVectors_returnsZero() {
        val v1 = floatArrayOf(1.0f, 0.0f)
        val v2 = floatArrayOf(0.0f, 1.0f)

        val similarity = engine.cosineSimilarity(v1, v2)
        assertEquals(0.0f, similarity, 0.001f)
    }

    @Test
    fun testFindTopKSimilar_ranksCandidatesCorrectly() {
        val query = floatArrayOf(1.0f, 0.0f, 0.0f)

        val candidate1 = NoteEmbeddingEntity(noteId = 1L, vector = floatArrayOf(0.9f, 0.1f, 0.0f), dimension = 3)
        val candidate2 = NoteEmbeddingEntity(noteId = 2L, vector = floatArrayOf(0.1f, 0.9f, 0.0f), dimension = 3)
        val candidate3 = NoteEmbeddingEntity(noteId = 3L, vector = floatArrayOf(0.99f, 0.01f, 0.0f), dimension = 3)

        val topK = engine.findTopKSimilar(
            queryVector = query,
            candidates = listOf(candidate1, candidate2, candidate3),
            excludeNoteId = null,
            topK = 2
        )

        assertEquals(2, topK.size)
        assertEquals(3L, topK[0].first) // candidate 3 is closest
        assertEquals(1L, topK[1].first) // candidate 1 is second closest
        assertTrue(topK[0].second > topK[1].second)
    }

    @Test
    fun testReciprocalRankFusion_blendsKeywordAndSemanticRanks() {
        val keywordIds = listOf(1L, 2L, 3L)
        val semanticMatches = listOf(
            2L to 0.95f,
            4L to 0.88f,
            1L to 0.75f
        )

        val blended = engine.reciprocalRankFusion(keywordIds, semanticMatches)

        assertNotNull(blended)
        assertTrue(blended.isNotEmpty())
        // ID 2 appears high in both lists, so should have the highest blended rank
        assertEquals(2L, blended.first().first)
    }
}

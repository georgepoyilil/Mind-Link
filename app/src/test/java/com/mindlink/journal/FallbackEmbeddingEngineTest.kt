package com.mindlink.journal

import com.mindlink.journal.data.ai.FallbackEmbeddingEngine
import com.mindlink.journal.data.ai.VectorSearchEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

class FallbackEmbeddingEngineTest {

    private lateinit var embeddingEngine: FallbackEmbeddingEngine
    private lateinit var vectorSearchEngine: VectorSearchEngine

    @Before
    fun setUp() {
        embeddingEngine = FallbackEmbeddingEngine()
        vectorSearchEngine = VectorSearchEngine()
    }

    @Test
    fun testGenerateEmbedding_dimensionAndNormalization() = runBlocking {
        val text = "Exploring machine learning embeddings in native Android applications."
        val embedding = embeddingEngine.generateEmbedding(text)

        assertEquals(384, embedding.size)

        var sumSq = 0f
        for (v in embedding) sumSq += v * v
        val norm = sqrt(sumSq)

        assertEquals(1.0f, norm, 0.01f)
    }

    @Test
    fun testSemanticSimilarity_similarTextsHaveHigherScoreThanDissimilar() = runBlocking {
        val baseText = "Android app architecture using Jetpack Compose and Room SQLite database."
        val similarText = "Building an Android application with Room SQLite and Jetpack Compose."
        val unrelatedText = "Baking sourdough bread using whole wheat flour and wild yeast fermentation."

        val baseVec = embeddingEngine.generateEmbedding(baseText)
        val similarVec = embeddingEngine.generateEmbedding(similarText)
        val unrelatedVec = embeddingEngine.generateEmbedding(unrelatedText)

        val simScore = vectorSearchEngine.cosineSimilarity(baseVec, similarVec)
        val unrelatedScore = vectorSearchEngine.cosineSimilarity(baseVec, unrelatedVec)

        assertTrue(
            "Expected similar text score ($simScore) to exceed unrelated text score ($unrelatedScore)",
            simScore > unrelatedScore
        )
    }
}

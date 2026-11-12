package com.mindlink.journal

import com.mindlink.journal.data.ai.FallbackEmbeddingEngine
import com.mindlink.journal.data.ai.LocalHeuristicBrainstormingEngine
import com.mindlink.journal.data.ai.VectorSearchEngine
import com.mindlink.journal.data.local.SampleDataSeeder
import com.mindlink.journal.data.local.entity.NoteEmbeddingEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SecondBrainQueryTest {

    private lateinit var embeddingEngine: FallbackEmbeddingEngine
    private lateinit var vectorSearchEngine: VectorSearchEngine
    private lateinit var brainstormingEngine: LocalHeuristicBrainstormingEngine

    @Before
    fun setUp() {
        embeddingEngine = FallbackEmbeddingEngine()
        vectorSearchEngine = VectorSearchEngine()
        brainstormingEngine = LocalHeuristicBrainstormingEngine()
    }

    @Test
    fun testTotalNotesCount_is28() {
        val notes = SampleDataSeeder.sampleNotes
        assertEquals(28, notes.size)
    }

    @Test
    fun testQueryLoveAndElena_synthesizesRelationshipNotes() = runBlocking {
        val notes = SampleDataSeeder.sampleNotes
        val query = "What have I written about love and Elena?"

        // Check semantic similarity against Elena note
        val elenaNote = notes.first { it.title.contains("Elena") }
        val queryVec = embeddingEngine.generateEmbedding(query)
        val noteVec = embeddingEngine.generateEmbedding("${elenaNote.title}\n${elenaNote.content}")
        val similarity = vectorSearchEngine.cosineSimilarity(queryVec, noteVec)

        assertTrue("Expected positive similarity for Elena query, got $similarity", similarity > 0.10f)

        // Run synthesis
        val synthesis = brainstormingEngine.synthesizeNotes(query, listOf(elenaNote))
        assertNotNull(synthesis)
        assertTrue(synthesis.answer.contains("Elena"))
        assertTrue(synthesis.answer.contains("relationship") || synthesis.answer.contains("love"))
    }

    @Test
    fun testQueryFailureAndRejection_synthesizesStartupFailure() = runBlocking {
        val notes = SampleDataSeeder.sampleNotes
        val failureNote = notes.first { it.title.contains("investor pass") }

        val query = "What failures or rejections have I experienced?"
        val synthesis = brainstormingEngine.synthesizeNotes(query, listOf(failureNote))

        assertNotNull(synthesis)
        assertTrue(synthesis.answer.contains("failure") || synthesis.answer.contains("rejection") || synthesis.answer.contains("Sequoia"))
    }

    @Test
    fun testQueryParentsAndDad_synthesizesFamilyConcern() = runBlocking {
        val notes = SampleDataSeeder.sampleNotes
        val dadNote = notes.first { it.title.contains("Dad") }

        val query = "How do I feel about my parents and Dad's health?"
        val synthesis = brainstormingEngine.synthesizeNotes(query, listOf(dadNote))

        assertNotNull(synthesis)
        assertTrue(synthesis.answer.contains("Dad") || synthesis.answer.contains("parents") || synthesis.answer.contains("family"))
    }

    @Test
    fun testQueryBurnout_synthesizesMentalExhaustion() = runBlocking {
        val notes = SampleDataSeeder.sampleNotes
        val burnoutNote = notes.first { it.title.contains("burnt out") }

        val query = "Am I dealing with burnout or exhaustion?"
        val synthesis = brainstormingEngine.synthesizeNotes(query, listOf(burnoutNote))

        assertNotNull(synthesis)
        assertTrue(synthesis.answer.contains("burnout") || synthesis.answer.contains("exhaustion") || synthesis.answer.contains("PTO"))
    }
}

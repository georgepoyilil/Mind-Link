package com.mindlink.journal

import com.mindlink.journal.data.ai.FallbackEmbeddingEngine
import com.mindlink.journal.data.ai.LocalHeuristicBrainstormingEngine
import com.mindlink.journal.data.ai.VectorSearchEngine
import com.mindlink.journal.data.local.SampleDataSeeder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SecondBrainEmotionalQueryTest {

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
    fun testEmotionalQuery_LoveAndElena() = runBlocking {
        val notes = SampleDataSeeder.sampleNotes
        val elenaNote = notes.first { it.title.contains("Elena") }
        val result = brainstormingEngine.synthesizeNotes("What have I written about love and Elena?", listOf(elenaNote))

        println("=== LOVE QUERY RESULT ===")
        println(result.answer)
        println("Takeaways: " + result.keyTakeaways)

        assertTrue(result.answer.contains("Elena"))
        assertTrue(result.answer.contains("presence") || result.answer.contains("love"))
        assertTrue(result.keyTakeaways.isNotEmpty())
    }

    @Test
    fun testEmotionalQuery_DadAndAging() = runBlocking {
        val notes = SampleDataSeeder.sampleNotes
        val dadNote = notes.first { it.title.contains("Dad") }
        val result = brainstormingEngine.synthesizeNotes("How am I feeling about Dad and my parents aging?", listOf(dadNote))

        println("=== FAMILY AGING QUERY RESULT ===")
        println(result.answer)
        println("Takeaways: " + result.keyTakeaways)

        assertTrue(result.answer.contains("Dad") || result.answer.contains("parents"))
        assertTrue(result.keyTakeaways.isNotEmpty())
    }

    @Test
    fun testEmotionalQuery_FailureAndRejection() = runBlocking {
        val notes = SampleDataSeeder.sampleNotes
        val failureNote = notes.first { it.title.contains("investor pass") }
        val result = brainstormingEngine.synthesizeNotes("What failures or rejections have I noted?", listOf(failureNote))

        println("=== FAILURE QUERY RESULT ===")
        println(result.answer)
        println("Takeaways: " + result.keyTakeaways)

        assertTrue(result.answer.contains("rejection") || result.answer.contains("failure") || result.answer.contains("Sequoia"))
        assertTrue(result.keyTakeaways.isNotEmpty())
    }

    @Test
    fun testEmotionalQuery_LonelinessAndBurnout() = runBlocking {
        val notes = SampleDataSeeder.sampleNotes
        val emotionalNotes = notes.filter { it.title.contains("lonely") || it.title.contains("burnt out") || it.title.contains("invisible") }
        val result = brainstormingEngine.synthesizeNotes("What feelings of loneliness or burnout have I had?", emotionalNotes)

        println("=== LONELINESS & BURNOUT QUERY RESULT ===")
        println(result.answer)
        println("Takeaways: " + result.keyTakeaways)

        assertTrue(result.answer.contains("loneliness") || result.answer.contains("burnout") || result.answer.contains("exhaustion"))
        assertTrue(result.keyTakeaways.isNotEmpty())
    }
}

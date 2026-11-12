package com.mindlink.journal

import com.mindlink.journal.data.ai.LocalHeuristicBrainstormingEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LocalHeuristicBrainstormingEngineTest {

    private lateinit var engine: LocalHeuristicBrainstormingEngine

    @Before
    fun setUp() {
        engine = LocalHeuristicBrainstormingEngine()
    }

    @Test
    fun testAnalyzeNote_extractsActionItemsCorrectly() = runBlocking {
        val title = "Weekly Planning"
        val content = """
            Here are my priorities for this week:
            - [ ] Ship the MindLink Android MVP
            - [ ] Refactor the vector search engine
            Remember to email Sarah regarding database migrations.
            General notes on architecture.
        """.trimIndent()

        val result = engine.analyzeNote(title, content)

        assertNotNull(result)
        assertTrue(result.actionItems.contains("Ship the MindLink Android MVP"))
        assertTrue(result.actionItems.contains("Refactor the vector search engine"))
    }

    @Test
    fun testAnalyzeNote_classifiesAnalyticalMood() = runBlocking {
        val title = "Architecture Benchmark"
        val content = "Analyzing SQLite FTS performance vs ONNX model latency with metrics and benchmark data."

        val result = engine.analyzeNote(title, content)

        assertEquals("Analytical", result.mood)
        assertTrue(result.reflectionPrompts.isNotEmpty())
    }

    @Test
    fun testAnalyzeNote_extractsSummaryAndTags() = runBlocking {
        val title = "Jetpack Compose Performance"
        val content = "Jetpack Compose provides extraordinary declarative UI velocity but requires careful recomposition profiling."

        val result = engine.analyzeNote(title, content)

        assertNotNull(result.summary)
        assertTrue(result.summary.isNotBlank())
        assertTrue(result.tags.isNotEmpty())
    }
}

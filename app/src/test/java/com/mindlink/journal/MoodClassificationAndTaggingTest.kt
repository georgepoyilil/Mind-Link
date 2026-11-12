package com.mindlink.journal

import com.mindlink.journal.data.ai.LocalHeuristicBrainstormingEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MoodClassificationAndTaggingTest {

    private lateinit var engine: LocalHeuristicBrainstormingEngine

    @Before
    fun setUp() {
        engine = LocalHeuristicBrainstormingEngine()
    }

    @Test
    fun testElenaRelationshipNote_isClassifiedAsReflectiveNotAnalytical() = runBlocking {
        val title = "Hard conversation with Elena tonight"
        val content = """
            We had our first real argument in months over dinner tonight.
            She said: "You're physically sitting on this couch with me, but mentally you're 5,000 miles away debugging servers. I miss my best friend."
            Hearing her voice break cut right through my chest. We sat in the dark and held hands. I love this woman with everything in me.
        """.trimIndent()

        val analysis = engine.analyzeNote(title, content)
        println("Elena Note Mood: ${analysis.mood}, Tags: ${analysis.tags}")

        assertEquals("Reflective", analysis.mood)
        assertTrue("Tags should contain Relationship or Love: ${analysis.tags}", analysis.tags.contains("Relationship") || analysis.tags.contains("Love"))
    }

    @Test
    fun testBurnoutNote_isClassifiedAsStressedNotAnalytical() = runBlocking {
        val title = "Admitting to myself that I am completely burnt out"
        val content = """
            I have to stop lying to myself: I am totally and utterly exhausted.
            Waking up every morning with a heavy stone in my chest. My brain feels like dry sponge.
            Taking 4 days off PTO to sleep and unplug completely.
        """.trimIndent()

        val analysis = engine.analyzeNote(title, content)
        println("Burnout Note Mood: ${analysis.mood}, Tags: ${analysis.tags}")

        assertEquals("Stressed", analysis.mood)
        assertTrue("Tags should contain Burnout or Wellness: ${analysis.tags}", analysis.tags.contains("Burnout") || analysis.tags.contains("Wellness"))
    }

    @Test
    fun testPeacefulMorning_isClassifiedAsCalm() = runBlocking {
        val title = "A quiet Sunday morning that felt like pure peace"
        val content = """
            Woke up at 7:15 AM without an alarm clock. Amber morning sun on the hardwood floor.
            Ground the Ethiopian beans by hand, quiet stillness, breathing beside the person I love.
            Unfiltered gratitude.
        """.trimIndent()

        val analysis = engine.analyzeNote(title, content)
        println("Peace Note Mood: ${analysis.mood}, Tags: ${analysis.tags}")

        assertEquals("Calm", analysis.mood)
        assertTrue("Tags should contain Gratitude or Mindfulness: ${analysis.tags}", analysis.tags.contains("Gratitude") || analysis.tags.contains("Mindfulness"))
    }

    @Test
    fun testTechnicalPostMortem_isClassifiedAsAnalytical() = runBlocking {
        val title = "Incident Post-Mortem: Auth Gateway Cascade Failure #891"
        val content = """
            Post-mortem analysis for the 24-minute authentication outage.
            Primary DNS packet loss caused mobile clients to storm retries without jitter.
            Redis cache hit memory limits and entered swap, causing architecture cascade failure.
        """.trimIndent()

        val analysis = engine.analyzeNote(title, content)
        println("Post-Mortem Note Mood: ${analysis.mood}, Tags: ${analysis.tags}")

        assertEquals("Analytical", analysis.mood)
    }

    @Test
    fun testTempoRun_isClassifiedAsEnergized() = runBlocking {
        val title = "Half-Marathon Tempo Run 14km"
        val content = """
            Crisp 8°C morning in the park. Clocked 14km at 4:32/km pace.
            Legs felt incredible, huge momentum and energy after week 6 of marathon block.
        """.trimIndent()

        val analysis = engine.analyzeNote(title, content)
        println("Tempo Run Note Mood: ${analysis.mood}, Tags: ${analysis.tags}")

        assertEquals("Energized", analysis.mood)
        assertTrue("Tags should contain Fitness: ${analysis.tags}", analysis.tags.contains("Fitness"))
    }
}

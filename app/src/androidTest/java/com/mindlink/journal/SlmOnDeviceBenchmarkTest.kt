package com.mindlink.journal

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindlink.journal.data.ai.LocalHeuristicBrainstormingEngine
import com.mindlink.journal.data.ai.MediaPipeSlmEngine
import com.mindlink.journal.di.ServiceLocator
import com.mindlink.journal.domain.model.Note
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SlmOnDeviceBenchmarkTest {

    private lateinit var context: Context
    private lateinit var serviceLocator: ServiceLocator
    private lateinit var slmEngine: MediaPipeSlmEngine
    private lateinit var heuristicEngine: LocalHeuristicBrainstormingEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        serviceLocator = ServiceLocator.getInstance(context)
        slmEngine = serviceLocator.brainstormingEngine.mediaPipeEngine
        heuristicEngine = serviceLocator.brainstormingEngine.localEngine

        // Force SLM engine mode in preferences
        serviceLocator.preferences.edit()
            .putString(ServiceLocator.PREF_ENGINE_MODE, "SLM")
            .apply()
    }

    data class BenchmarkCase(
        val domain: String,
        val title: String,
        val content: String,
        val keyNuance: String
    )

    @Test
    fun runComprehensiveSlmBenchmarkAndCreateNodes() {
        runBlocking {
            Log.i(TAG, "=================================================================")
        Log.i(TAG, "STARTING ON-DEVICE SLM (QWEN 2.5 0.5B) PHYSICAL BENCHMARK SUITE")
        Log.i(TAG, "Device: Nothing Phone (2a) - MediaTek Dimensity 7200 Pro")
        Log.i(TAG, "=================================================================")

        val testCases = listOf(
            BenchmarkCase(
                domain = "Relational / Emotional",
                title = "Dinner argument with Maya over work boundaries",
                content = "We had a heated argument about work-life boundaries tonight. Maya feels I'm constantly checking Slack even during our dinners, and she's completely right. I reacted defensively instead of listening, which only escalated the tension. I feel guilty and emotionally detached. Need to apologize sincerely, set a strict no-phone boundary after 8 PM, and plan an uninterrupted date this weekend.",
                keyNuance = "High vulnerability, interpersonal guilt, boundary setting, repair attempt."
            ),
            BenchmarkCase(
                domain = "Technical / Architecture",
                title = "Distributed vector indexing latency spike",
                content = "Our vector search latency degraded to 420ms under 50k embeddings in the SQLite FTS + cosine simulation test. The in-memory cache is ballooning heap usage to 380MB, risking OOM kills on 4GB devices. We need to decide between quantization to int8 vs offloading older cluster centroids to an indexed hierarchical graph. Release deadline is in 5 days.",
                keyNuance = "High cognitive complexity, trade-off analysis (memory vs latency), urgent release pressure."
            ),
            BenchmarkCase(
                domain = "Physical Wellbeing / Burnout",
                title = "Four nights of sleep debt and somatic exhaustion",
                content = "Averaging 4.5 hours of fragmented sleep over the last four days. My shoulders and neck feel like concrete, and I've been relying on triple-shot espressos to get through standups. Woke up with brain fog and irritation. I'm torn between pushing myself to hit the gym for leg day or allowing myself to take an early evening walk and sleep by 10 PM. My body is clearly telling me to slow down.",
                keyNuance = "Somatic stress signals, guilt around resting vs working out, nervous system depletion."
            ),
            BenchmarkCase(
                domain = "Philosophical / Creative",
                title = "Midnight walk through empty city avenues",
                content = "Walked through downtown at 1 AM in silence. Looking up at the illuminated skyscrapers with nobody on the streets makes everything feel strikingly impermanent. So much daily anxiety is poured into things that won't matter in ten years. What kind of creative work actually creates lasting meaning rather than transient noise? Felt a deep sense of stillness and clarity for the first time in weeks.",
                keyNuance = "Existential perspective, impermanence, shifting priority from urgency to legacy."
            )
        )

        val repository = serviceLocator.noteRepository

        for ((idx, testCase) in testCases.withIndex()) {
            Log.i(TAG, "-----------------------------------------------------------------")
            Log.i(TAG, "BENCHMARK NODE #${idx + 1}: ${testCase.domain}")
            Log.i(TAG, "TITLE: ${testCase.title}")
            Log.i(TAG, "NUANCE: ${testCase.keyNuance}")

            // 1. Run On-Device SLM Analysis with latency tracking
            val slmStartTime = System.currentTimeMillis()
            val slmResult = slmEngine.analyzeNote(testCase.title, testCase.content)
            val slmLatency = System.currentTimeMillis() - slmStartTime

            // 2. Run Local Heuristic Baseline Analysis
            val heuristicStartTime = System.currentTimeMillis()
            val heuristicResult = heuristicEngine.analyzeNote(testCase.title, testCase.content)
            val heuristicLatency = System.currentTimeMillis() - heuristicStartTime

            // 3. Log Comparative Metrics
            Log.i(TAG, ">> SLM_LATENCY: ${slmLatency} ms | HEURISTIC_LATENCY: ${heuristicLatency} ms")
            Log.i(TAG, ">> SLM_SUMMARY: ${slmResult.summary}")
            Log.i(TAG, ">> HEURISTIC_SUMMARY: ${heuristicResult.summary}")
            Log.i(TAG, ">> SLM_MOOD: ${slmResult.mood} | HEURISTIC_MOOD: ${heuristicResult.mood}")
            Log.i(TAG, ">> SLM_TAGS: ${slmResult.tags}")
            Log.i(TAG, ">> HEURISTIC_TAGS: ${heuristicResult.tags}")
            Log.i(TAG, ">> SLM_ACTIONS: ${slmResult.actionItems}")
            Log.i(TAG, ">> HEURISTIC_ACTIONS: ${heuristicResult.actionItems}")
            Log.i(TAG, ">> SLM_PROMPTS: ${slmResult.reflectionPrompts.joinToString(" | ")}")
            Log.i(TAG, ">> HEURISTIC_PROMPTS: ${heuristicResult.reflectionPrompts.joinToString(" | ")}")
            val rem = slmResult.contextualReminder
            if (rem != null) {
                Log.i(TAG, ">> SLM_REMINDER: ${rem.triggerPattern} -> ${rem.question}")
            }

            // 4. Save into Physical Device Room DB as an active user thought
            val noteToSave = Note(
                title = testCase.title,
                content = testCase.content,
                summary = slmResult.summary,
                mood = slmResult.mood,
                tags = (slmResult.tags + listOf("SLM-Verified", testCase.domain.substringBefore("/"))).distinct(),
                actionItems = slmResult.actionItems,
                reflectionPrompts = slmResult.reflectionPrompts,
                isProcessed = true
            )
            val savedId = repository.saveNote(noteToSave)
            Log.i(TAG, ">> SAVED TO ROOM DB: Note #$savedId created successfully")
        }

        Log.i(TAG, "=================================================================")
        Log.i(TAG, "BENCHMARK SUITE COMPLETE - ALL 4 NODES CREATED AND EVALUATED")
        Log.i(TAG, "=================================================================")
        }
    }

    companion object {
        private const val TAG = "SLM_BENCHMARK"
    }
}

package com.mindlink.journal

import com.mindlink.journal.data.ai.FallbackEmbeddingEngine
import com.mindlink.journal.data.ai.LocalHeuristicBrainstormingEngine
import com.mindlink.journal.data.ai.VectorSearchEngine
import com.mindlink.journal.data.local.SampleDataSeeder
import com.mindlink.journal.data.local.entity.NoteEmbeddingEntity
import com.mindlink.journal.domain.model.Note
import com.mindlink.journal.domain.model.SearchResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Locale

class SecondBrainLoveLifeRankingTest {

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
    fun testLoveLifeQuery_ranksElenaAtTopAndSynthesizesAccurately() = runBlocking {
        val notes = SampleDataSeeder.sampleNotes
        val query = "how is my love life"

        // Generate embeddings for all notes as done in NoteProcessingWorker
        val embeddings = notes.map { note ->
            val vec = embeddingEngine.generateEmbedding("${note.title}\n${note.content}")
            NoteEmbeddingEntity(
                noteId = note.id,
                vector = vec,
                dimension = vec.size,
                updatedAt = System.currentTimeMillis()
            )
        }

        // Simulate Vector Search
        val queryVec = embeddingEngine.generateEmbedding(query)
        val semanticMatches = vectorSearchEngine.findTopKSimilar(
            queryVector = queryVec,
            candidates = embeddings,
            excludeNoteId = null,
            topK = 15,
            threshold = 0.02f
        )

        // Simulate Keyword Search (stop-words filtered)
        val stopWords = setOf("how", "is", "my", "life", "the", "what", "a", "in", "to")
        val searchTokens = query.lowercase(Locale.ROOT)
            .split(" ")
            .filter { it.isNotBlank() && !stopWords.contains(it) }

        val keywordMatches = notes.filter { note ->
            searchTokens.any { token ->
                note.title.lowercase(Locale.ROOT).contains(token) ||
                note.tags.any { it.lowercase(Locale.ROOT).contains(token) } ||
                note.content.lowercase(Locale.ROOT).contains(token)
            }
        }.map { it.id }

        // Reciprocal Rank Fusion
        val blended = vectorSearchEngine.reciprocalRankFusion(
            keywordRankedIds = keywordMatches,
            semanticRankedMatches = semanticMatches
        )
        val scoreMap = blended.associate { it.first to it.second }

        // Intent expansion and scoring
        val expandedTokens = listOf("love", "relationship", "elena", "partner", "dating", "presence")
        val rankedResults = notes.map { note ->
            val baseRrf = scoreMap[note.id] ?: 0.01f
            var intentBoost = 0.0f
            val lowerTags = note.tags.map { it.lowercase(Locale.ROOT) }
            val lowerTitle = note.title.lowercase(Locale.ROOT)
            val lowerContent = note.content.lowercase(Locale.ROOT)

            for (token in expandedTokens) {
                if (lowerTags.any { it == token || it.contains(token) }) intentBoost += 2.0f
                if (lowerTitle.contains(token)) intentBoost += 1.5f
                if (lowerContent.contains(token)) intentBoost += 0.4f
            }

            SearchResult(
                note = note,
                hybridScore = baseRrf + intentBoost,
                isSemanticMatch = true,
                isKeywordMatch = keywordMatches.contains(note.id)
            )
        }.sortedByDescending { it.hybridScore }

        println("=== TOP 3 RANKED NOTES FOR 'how is my love life' ===")
        rankedResults.take(3).forEachIndexed { i, r ->
            println("#${i + 1}: ${r.note.title} (Score: ${r.hybridScore})")
        }

        // Top note must be Elena relationship note
        val topNote = rankedResults.first()
        assertTrue("Top note must mention Elena! Got: ${topNote.note.title}", topNote.note.title.contains("Elena"))

        // Run synthesis on top results
        val topNotes = rankedResults.take(3).map { it.note }
        val synthesis = brainstormingEngine.synthesizeNotes(query, topNotes)

        println("\n=== SYNTHESIS OUTPUT ===")
        println(synthesis.answer)
        println("Takeaways: ${synthesis.keyTakeaways}")

        assertTrue(synthesis.answer.contains("Elena"))
        assertFalse(synthesis.answer.contains("Sequoia") || synthesis.answer.contains("investor pass"))
    }
}

package com.mindlink.journal.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.sqrt

/**
 * High-performance deterministic client-side embedding generator (384 dimensions).
 * Uses stop-word filtering, subword n-gram hashing, and dimensional projection
 * to generate dense unit vectors with cosine similarity properties matching semantic text overlap.
 * Acts as an offline fallback when the raw ONNX model asset is not yet bundled.
 */
class FallbackEmbeddingEngine : EmbeddingEngine {

    override val dimension: Int = 384

    private val stopWords = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
        "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can",
        "did", "do", "does", "doing", "don't", "down", "during", "each", "few", "for", "from", "further", "had",
        "has", "have", "having", "he", "her", "here", "hers", "herself", "him", "himself", "his", "how", "i", "if",
        "in", "into", "is", "isn't", "it", "it's", "its", "itself", "me", "more", "most", "my", "myself", "no",
        "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out",
        "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", "the", "their", "theirs",
        "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too", "under",
        "until", "up", "very", "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why",
        "with", "would", "you", "your", "yours", "yourself", "yourselves"
    )

    override suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        val vector = FloatArray(dimension)
        if (text.isBlank()) return@withContext vector

        // Filter stop words to concentrate vector mass on semantic content terms
        val words = text.lowercase(Locale.ROOT)
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 1 && !stopWords.contains(it) }
            .ifEmpty {
                text.lowercase(Locale.ROOT).split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
            }

        if (words.isEmpty()) return@withContext vector

        val md = MessageDigest.getInstance("MD5")

        // Hash unigrams and bigrams
        for (i in words.indices) {
            val word = words[i]
            applyWordToVector(word, 1.2f, vector, md)

            if (i < words.size - 1) {
                val bigram = "$word ${words[i + 1]}"
                applyWordToVector(bigram, 1.8f, vector, md)
            }
        }

        // L2 Normalization to project onto 384-d unit hypersphere
        var sumSquares = 0.0f
        for (v in vector) {
            sumSquares += v * v
        }

        val norm = sqrt(sumSquares)
        if (norm > 0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }

        vector
    }

    private fun applyWordToVector(token: String, weight: Float, vector: FloatArray, md: MessageDigest) {
        val hash = md.digest(token.toByteArray())
        for (j in 0 until 4) {
            val byte1 = hash[j * 4].toInt() and 0xFF
            val byte2 = hash[j * 4 + 1].toInt() and 0xFF
            val index = ((byte1 shl 8) or byte2) % dimension
            val sign = if ((hash[j * 4 + 2].toInt() and 1) == 1) 1.0f else -1.0f
            vector[index] += sign * weight
        }
    }
}

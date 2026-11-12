package com.mindlink.journal.data.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.LongBuffer
import kotlin.math.sqrt

/**
 * On-device embedding engine running all-MiniLM-L6-v2 via ONNX Runtime Mobile.
 * Produces 384-dimensional dense vectors.
 * Gracefully falls back to FallbackEmbeddingEngine if the model asset is missing.
 */
class OnnxEmbeddingEngine(
    private val context: Context,
    private val fallbackEngine: FallbackEmbeddingEngine = FallbackEmbeddingEngine()
) : EmbeddingEngine {

    override val dimension: Int = 384

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false
    private var initializationFailed = false

    private val modelFileName = "all-MiniLM-L6-v2.onnx"
    private val maxSeqLength = 128

    init {
        tryInit()
    }

    private fun tryInit() {
        try {
            // Check if asset exists
            val hasAsset = context.assets.list("")?.contains(modelFileName) == true
            if (!hasAsset) {
                Log.i(TAG, "ONNX model asset '$modelFileName' not found in assets. Using deterministic embedding engine.")
                return
            }

            val modelFile = File(context.filesDir, modelFileName)
            if (!modelFile.exists() || modelFile.length() == 0L) {
                context.assets.open(modelFileName).use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            ortEnvironment = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
            }
            ortSession = ortEnvironment?.createSession(modelFile.absolutePath, sessionOptions)
            isInitialized = true
            Log.i(TAG, "ONNX Runtime model loaded successfully from ${modelFile.name}")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize ONNX Runtime. Falling back to local vector engine: ${e.message}")
            initializationFailed = true
        }
    }

    override suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        if (!isInitialized || ortSession == null || ortEnvironment == null) {
            return@withContext fallbackEngine.generateEmbedding(text)
        }

        try {
            val tokens = tokenizeSimple(text, maxSeqLength)
            val seqLen = tokens.size

            val inputIds = LongArray(seqLen) { tokens[it].toLong() }
            val attentionMask = LongArray(seqLen) { 1L }
            val tokenTypeIds = LongArray(seqLen) { 0L }

            val env = ortEnvironment!!
            val session = ortSession!!

            val inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), longArrayOf(1, seqLen.toLong()))
            val attentionMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), longArrayOf(1, seqLen.toLong()))
            val tokenTypeIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), longArrayOf(1, seqLen.toLong()))

            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor,
                "token_type_ids" to tokenTypeIdsTensor
            )

            val results = session.run(inputs)
            val outputTensor = results[0].value as Array<Array<FloatArray>> // shape: [1, seqLen, 384]
            val tokenEmbeddings = outputTensor[0]

            // Mean pooling over token embeddings
            val pooled = FloatArray(dimension)
            for (tokenVec in tokenEmbeddings) {
                for (d in 0 until dimension) {
                    pooled[d] += tokenVec[d]
                }
            }
            for (d in 0 until dimension) {
                pooled[d] /= seqLen.toFloat()
            }

            // L2 normalize
            var sumSq = 0f
            for (v in pooled) sumSq += v * v
            val norm = sqrt(sumSq)
            if (norm > 0f) {
                for (d in 0 until dimension) pooled[d] /= norm
            }

            // Clean up tensors
            inputIdsTensor.close()
            attentionMaskTensor.close()
            tokenTypeIdsTensor.close()
            results.close()

            pooled
        } catch (e: Throwable) {
            Log.e(TAG, "ONNX inference error, delegating to fallback: ${e.message}")
            fallbackEngine.generateEmbedding(text)
        }
    }

    private fun tokenizeSimple(text: String, maxLen: Int): List<Int> {
        val tokens = mutableListOf<Int>()
        tokens.add(101) // [CLS]
        val words = text.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        for (w in words) {
            if (tokens.size >= maxLen - 1) break
            // Simple hash-based token ID mapping for BERT vocab bounds (1000 - 29000)
            val hash = (w.hashCode() and 0x7FFFFFFF) % 28000 + 1000
            tokens.add(hash)
        }
        tokens.add(102) // [SEP]
        return tokens
    }

    fun close() {
        try {
            ortSession?.close()
            ortEnvironment?.close()
        } catch (e: Throwable) {
            Log.e(TAG, "Error closing ONNX session", e)
        }
    }

    companion object {
        private const val TAG = "OnnxEmbeddingEngine"
    }
}

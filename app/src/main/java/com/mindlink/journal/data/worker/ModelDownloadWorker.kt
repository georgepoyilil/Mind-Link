package com.mindlink.journal.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mindlink.journal.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: "qwen2.5_0.5b"
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: "Qwen 2.5"
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()

        val targetDir = File(applicationContext.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(targetDir, "$modelId.task")
        val tempFile = File(targetDir, "$modelId.tmp")

        try {
            setProgress(workDataOf(KEY_PROGRESS to 0f, KEY_STATUS to "Connecting to $modelName repository..."))

            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 25000
                readTimeout = 90000
                setRequestProperty("User-Agent", "MindLink-Android/1.0")
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP ${connection.responseCode} for model download")
                return@withContext Result.failure(workDataOf(KEY_STATUS to "HTTP Error ${connection.responseCode}"))
            }

            val totalBytes = connection.contentLengthLong
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var totalRead = 0L
            var lastUpdate = System.currentTimeMillis()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 300) {
                    val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                    val readMb = totalRead / (1024 * 1024)
                    val totalMb = totalBytes / (1024 * 1024)
                    val statusStr = "Downloading $modelName: $readMb / $totalMb MB (${(progress * 100).toInt()}%)"
                    setProgress(workDataOf(KEY_PROGRESS to progress, KEY_STATUS to statusStr))
                    lastUpdate = now
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            if (tempFile.renameTo(targetFile)) {
                Log.i(TAG, "Model downloaded and saved to ${targetFile.absolutePath}")
            }

            val container = ServiceLocator.getInstance(applicationContext)
            container.brainstormingEngine.mediaPipeEngine.reloadModel()

            setProgress(workDataOf(KEY_PROGRESS to 1.0f, KEY_STATUS to "$modelName successfully installed."))
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed: ${e.message}", e)
            tempFile.delete()
            Result.failure(workDataOf(KEY_STATUS to "Download failed: ${e.message}"))
        }
    }

    companion object {
        private const val TAG = "ModelDownloadWorker"
        const val UNIQUE_WORK_NAME = "model_download_work"
        const val KEY_MODEL_ID = "key_model_id"
        const val KEY_MODEL_NAME = "key_model_name"
        const val KEY_DOWNLOAD_URL = "key_download_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
    }
}

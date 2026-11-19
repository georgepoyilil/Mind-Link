package com.mindlink.journal.data.ota

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.mindlink.journal.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val tagName: String,
    val versionName: String,
    val changelog: String,
    val downloadUrl: String,
    val fileName: String,
    val sizeBytes: Long,
    val isNewer: Boolean
)

class OtaUpdateManager(private val context: Context) {

    private val releasesApiUrl = "https://api.github.com/repos/georgepoyilil/Mind-Link/releases/latest"

    suspend fun checkForUpdates(): Result<AppReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val url = URL(releasesApiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "MindLink-Android/${BuildConfig.VERSION_NAME}")
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("GitHub API returned HTTP ${connection.responseCode}"))
            }

            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            val releaseObj = JSONObject(jsonText)

            val tagName = releaseObj.optString("tag_name", "")
            val changelog = releaseObj.optString("body", "General bug fixes and performance improvements.")
            val rawVersion = tagName.removePrefix("v").trim()

            val assets = releaseObj.optJSONArray("assets")
            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize: Long = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isNullOrBlank()) {
                return@withContext Result.success(null)
            }

            val currentVersion = BuildConfig.VERSION_NAME
            val isNewer = isVersionNewer(rawVersion, currentVersion)

            val info = AppReleaseInfo(
                tagName = tagName,
                versionName = rawVersion,
                changelog = changelog,
                downloadUrl = apkUrl,
                fileName = apkName ?: "MindLink-update.apk",
                sizeBytes = apkSize,
                isNewer = isNewer
            )

            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun downloadApk(
        releaseInfo: AppReleaseInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val updateDir = File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(updateDir, releaseInfo.fileName)
        val tempFile = File(updateDir, "${releaseInfo.fileName}.tmp")

        try {
            val url = URL(releaseInfo.downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 25000
                readTimeout = 90000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MindLink-Android/${BuildConfig.VERSION_NAME}")
            }
            connection.connect()

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
                if (now - lastUpdate > 250) {
                    val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                    onProgress(progress)
                    lastUpdate = now
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            if (tempFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
            }

            onProgress(1.0f)
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download update APK: ${e.message}", e)
            tempFile.delete()
            Result.failure(e)
        }
    }

    fun triggerInstall(apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    companion object {
        private const val TAG = "OtaUpdateManager"
    }
}

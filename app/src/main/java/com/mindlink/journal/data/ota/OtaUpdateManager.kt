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
import java.util.regex.Pattern

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
    private val releasesAtomUrl = "https://github.com/georgepoyilil/Mind-Link/releases.atom"
    private val releasesRedirectUrl = "https://github.com/georgepoyilil/Mind-Link/releases/latest"

    suspend fun checkForUpdates(): Result<AppReleaseInfo?> = withContext(Dispatchers.IO) {
        // Strategy 1: GitHub REST API
        val apiResult = runCatching { checkViaApi() }
        if (apiResult.isSuccess && apiResult.getOrNull() != null) {
            return@withContext Result.success(apiResult.getOrNull())
        }

        // Strategy 2: GitHub Atom XML Feed (Rate-limit free, always accessible)
        val atomResult = runCatching { checkViaAtomFeed() }
        if (atomResult.isSuccess && atomResult.getOrNull() != null) {
            return@withContext Result.success(atomResult.getOrNull())
        }

        // Strategy 3: GitHub 302 Redirect Location header
        val redirectResult = runCatching { checkViaRedirect() }
        if (redirectResult.isSuccess && redirectResult.getOrNull() != null) {
            return@withContext Result.success(redirectResult.getOrNull())
        }

        val error = apiResult.exceptionOrNull()
            ?: atomResult.exceptionOrNull()
            ?: redirectResult.exceptionOrNull()
            ?: Exception("Unable to contact GitHub releases")

        Log.e(TAG, "Update check failed across all strategies: ${error.message}", error)
        Result.failure(error)
    }

    private fun checkViaApi(): AppReleaseInfo? {
        val url = URL(releasesApiUrl)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            setRequestProperty("User-Agent", "MindLink-Android/${BuildConfig.VERSION_NAME}")
        }
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("GitHub API HTTP ${connection.responseCode}")
        }

        val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
        val releaseObj = JSONObject(jsonText)

        val tagName = releaseObj.optString("tag_name", "")
        if (tagName.isBlank()) return null

        val changelog = releaseObj.optString("body", "General bug fixes, performance improvements, and UI enhancements.")
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
            apkUrl = "https://github.com/georgepoyilil/Mind-Link/releases/download/$tagName/MindLink-$tagName.apk"
            apkName = "MindLink-$tagName.apk"
        }

        val currentVersion = BuildConfig.VERSION_NAME
        val isNewer = isVersionNewer(rawVersion, currentVersion)

        return AppReleaseInfo(
            tagName = tagName,
            versionName = rawVersion,
            changelog = changelog,
            downloadUrl = apkUrl,
            fileName = apkName ?: "MindLink-$tagName.apk",
            sizeBytes = apkSize,
            isNewer = isNewer
        )
    }

    private fun checkViaAtomFeed(): AppReleaseInfo? {
        val url = URL(releasesAtomUrl)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("User-Agent", "Mozilla/5.0 MindLink-Android/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Accept", "application/atom+xml, text/xml, */*")
        }
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Atom feed HTTP ${connection.responseCode}")
        }

        val xmlText = connection.inputStream.bufferedReader().use { it.readText() }

        // Extract latest tag from <link rel="alternate" ... href=".../tag/vX.Y.Z"/>
        val tagMatcher = Pattern.compile("href=\"https://github\\.com/georgepoyilil/Mind-Link/releases/tag/([^\"]+)\"").matcher(xmlText)
        if (!tagMatcher.find()) return null

        val tagName = tagMatcher.group(1) ?: return null
        val rawVersion = tagName.removePrefix("v").trim()

        // Extract title
        val titleMatcher = Pattern.compile("<title>([^<]+)</title>").matcher(xmlText)
        var title = "MindLink $tagName"
        if (titleMatcher.find() && titleMatcher.find()) {
            title = titleMatcher.group(1) ?: title
        }

        val currentVersion = BuildConfig.VERSION_NAME
        val isNewer = isVersionNewer(rawVersion, currentVersion)
        val downloadUrl = "https://github.com/georgepoyilil/Mind-Link/releases/download/$tagName/MindLink-$tagName.apk"

        return AppReleaseInfo(
            tagName = tagName,
            versionName = rawVersion,
            changelog = title,
            downloadUrl = downloadUrl,
            fileName = "MindLink-$tagName.apk",
            sizeBytes = 0L,
            isNewer = isNewer
        )
    }

    private fun checkViaRedirect(): AppReleaseInfo? {
        val url = URL(releasesRedirectUrl)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 15000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "Mozilla/5.0 MindLink-Android/${BuildConfig.VERSION_NAME}")
        }
        connection.connect()

        val location = connection.getHeaderField("Location") ?: ""
        connection.disconnect()

        val tagPrefix = "/tag/"
        val idx = location.lastIndexOf(tagPrefix)
        if (idx == -1) return null

        val tagName = location.substring(idx + tagPrefix.length).trim()
        val rawVersion = tagName.removePrefix("v").trim()
        val currentVersion = BuildConfig.VERSION_NAME
        val isNewer = isVersionNewer(rawVersion, currentVersion)
        val downloadUrl = "https://github.com/georgepoyilil/Mind-Link/releases/download/$tagName/MindLink-$tagName.apk"

        return AppReleaseInfo(
            tagName = tagName,
            versionName = rawVersion,
            changelog = "Release $tagName on GitHub",
            downloadUrl = downloadUrl,
            fileName = "MindLink-$tagName.apk",
            sizeBytes = 0L,
            isNewer = isNewer
        )
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
            var currentUrl = releaseInfo.downloadUrl
            var connection: HttpURLConnection? = null
            var redirects = 0

            while (redirects < 6) {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 25000
                    readTimeout = 90000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "Mozilla/5.0 MindLink-Android/${BuildConfig.VERSION_NAME}")
                    setRequestProperty("Accept", "*/*")
                }
                conn.connect()
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                    val location = conn.getHeaderField("Location") ?: break
                    conn.disconnect()
                    currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                    redirects++
                } else if (code == HttpURLConnection.HTTP_OK) {
                    connection = conn
                    break
                } else {
                    conn.disconnect()
                    return@withContext Result.failure(Exception("HTTP $code while downloading APK"))
                }
            }

            val finalConnection = connection ?: return@withContext Result.failure(Exception("Could not connect to download mirror"))
            val totalBytes = finalConnection.contentLengthLong
            val inputStream = finalConnection.inputStream
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
            finalConnection.disconnect()

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

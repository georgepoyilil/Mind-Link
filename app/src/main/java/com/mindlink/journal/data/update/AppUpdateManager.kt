package com.mindlink.journal.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mindlink.journal.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AppReleaseInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val downloadUrl: String,
    val apkSizeMb: Float?
)

class AppUpdateManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {
    private val _latestUpdate = MutableStateFlow<AppReleaseInfo?>(null)
    val latestUpdate: StateFlow<AppReleaseInfo?> = _latestUpdate.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _isDismissed = MutableStateFlow(false)
    val isDismissed: StateFlow<Boolean> = _isDismissed.asStateFlow()

    fun dismissUpdateNotification() {
        _isDismissed.value = true
    }

    suspend fun checkForUpdates(manual: Boolean = false): Result<AppReleaseInfo?> = withContext(Dispatchers.IO) {
        _isChecking.value = true
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/georgepoyilil/Mind-Link/releases/latest")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "MindLink-Android-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                _isChecking.value = false
                return@withContext Result.failure(Exception("GitHub API error HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: ""
            val json = gson.fromJson(body, JsonObject::class.java)

            val tagName = json.get("tag_name")?.asString ?: ""
            val remoteVersion = tagName.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

            if (isNewer(currentVersion, remoteVersion)) {
                val title = json.get("name")?.asString ?: "MindLink $tagName"
                val releaseNotes = json.get("body")?.asString ?: "Performance improvements, security enhancements, and UI polish."
                val htmlUrl = json.get("html_url")?.asString ?: "https://github.com/georgepoyilil/Mind-Link/releases"

                var downloadUrl: String? = null
                var apkSizeMb: Float? = null

                val assets = json.getAsJsonArray("assets")
                if (assets != null && assets.size() > 0) {
                    for (assetElem in assets) {
                        val assetObj = assetElem.asJsonObject
                        val name = assetObj.get("name")?.asString ?: ""
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = assetObj.get("browser_download_url")?.asString
                            val sizeBytes = assetObj.get("size")?.asLong ?: 0L
                            if (sizeBytes > 0) {
                                apkSizeMb = sizeBytes / (1024f * 1024f)
                            }
                            break
                        }
                    }
                }

                val releaseInfo = AppReleaseInfo(
                    tagName = tagName,
                    versionName = remoteVersion,
                    title = title,
                    releaseNotes = releaseNotes,
                    htmlUrl = htmlUrl,
                    downloadUrl = downloadUrl ?: htmlUrl,
                    apkSizeMb = apkSizeMb
                )
                _latestUpdate.value = releaseInfo
                _isDismissed.value = false
                _isChecking.value = false
                return@withContext Result.success(releaseInfo)
            } else {
                if (manual) {
                    _latestUpdate.value = null
                }
                _isChecking.value = false
                return@withContext Result.success(null)
            }
        } catch (e: Exception) {
            _isChecking.value = false
            return@withContext Result.failure(e)
        }
    }

    private fun isNewer(current: String, remote: String): Boolean {
        try {
            val curParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val remParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(curParts.size, remParts.size)
            for (i in 0 until maxLen) {
                val c = curParts.getOrElse(i) { 0 }
                val r = remParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (_: Exception) {}
        return false
    }

    fun openDownloadPage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

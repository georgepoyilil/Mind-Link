package com.mindlink.journal.data.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mindlink.journal.BuildConfig
import com.mindlink.journal.data.local.dao.NoteDao
import com.mindlink.journal.data.local.entity.NoteEntity
import com.mindlink.journal.data.worker.BackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class GoogleUserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null
)

data class BackupSlotInfo(
    val slot: Int, // 1 = Latest, 2 = Previous, 3 = Oldest
    val fileName: String,
    val timestamp: Long,
    val notesCount: Int,
    val sizeBytes: Long,
    val appVersion: String
) {
    val formattedDate: String
        get() = if (timestamp <= 0) "No backup" else SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(timestamp))

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            if (sizeBytes < 1024) return "$sizeBytes B"
            val kb = sizeBytes / 1024f
            if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
            return String.format(Locale.getDefault(), "%.2f MB", kb / 1024f)
        }
}

data class BackupNoteDto(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val summary: String?,
    val mood: String?,
    val tags: List<String>,
    val actionItems: List<String>,
    val reflectionPrompts: List<String>,
    val isArchived: Boolean,
    val isDeleted: Boolean,
    val isLocked: Boolean,
    val isPinned: Boolean = false,
    val fontFamily: String,
    val fontSizeScale: Float?,
    val imageFileName: String? = null,
    val imageCaption: String? = null
)

data class MindLinkBackupPayload(
    val appVersion: String,
    val backupTimestamp: Long,
    val notesCount: Int,
    val notes: List<BackupNoteDto>
)

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val error: String) : SyncState()
}

class GoogleSyncManager(
    private val context: Context,
    private val noteDao: NoteDao,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {
    private val prefs = context.getSharedPreferences("mindlink_google_sync", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<GoogleUserProfile?>(null)
    val currentUser: StateFlow<GoogleUserProfile?> = _currentUser.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getLong("last_backup_time", 0L))
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _lastBackupSize = MutableStateFlow(prefs.getLong("last_backup_size", 0L))
    val lastBackupSize: StateFlow<Long> = _lastBackupSize.asStateFlow()

    private val _backupSlots = MutableStateFlow<List<BackupSlotInfo>>(emptyList())
    val backupSlots: StateFlow<List<BackupSlotInfo>> = _backupSlots.asStateFlow()

    private val _backupFrequency = MutableStateFlow(prefs.getString("backup_frequency", "Daily") ?: "Daily")
    val backupFrequency: StateFlow<String> = _backupFrequency.asStateFlow()

    private val _userConsentIntent = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val userConsentIntent: SharedFlow<Intent> = _userConsentIntent.asSharedFlow()

    val formattedLastBackup: String
        get() {
            val t = _lastBackupTime.value
            if (t <= 0) return "Never backed up"
            return SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(t))
        }

    val formattedLastBackupSize: String
        get() {
            val bytes = _lastBackupSize.value
            if (bytes <= 0) return ""
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024f
            if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
            return String.format(Locale.getDefault(), "%.2f MB", kb / 1024f)
        }

    private val googleSignInClient: GoogleSignInClient

    private val backupsDir = File(context.filesDir, "backups").apply {
        if (!exists()) mkdirs()
    }

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // Restore saved account from persistent prefs first
        val savedEmail = prefs.getString("user_email", null)
        val savedName = prefs.getString("user_name", null)
        val savedId = prefs.getString("user_id", null)
        val savedPhoto = prefs.getString("user_photo", null)

        if (!savedEmail.isNullOrBlank()) {
            _currentUser.value = GoogleUserProfile(
                id = savedId ?: savedEmail,
                displayName = savedName ?: savedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = savedEmail,
                photoUrl = savedPhoto
            )
        } else {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastAccount != null && !lastAccount.email.isNullOrBlank()) {
                val profile = GoogleUserProfile(
                    id = lastAccount.id ?: lastAccount.email!!,
                    displayName = lastAccount.displayName ?: lastAccount.email!!.substringBefore("@"),
                    email = lastAccount.email!!,
                    photoUrl = lastAccount.photoUrl?.toString()
                )
                _currentUser.value = profile
                saveUserProfile(profile)
            }
        }

        refreshBackupSlots()
    }

    private fun saveUserProfile(profile: GoogleUserProfile) {
        prefs.edit()
            .putString("user_id", profile.id)
            .putString("user_name", profile.displayName)
            .putString("user_email", profile.email)
            .putString("user_photo", profile.photoUrl)
            .apply()
    }

    private fun getGoogleAccount(): Account? {
        val user = _currentUser.value
        if (user != null && user.email.isNotBlank()) {
            return Account(user.email, "com.google")
        }
        val last = GoogleSignIn.getLastSignedInAccount(context)
        if (last != null && last.account != null) {
            return last.account
        }
        return null
    }

    /**
     * Mirrors the given backup ZIP file to public Downloads/MindLink/mindlink_backup_latest.zip.
     * This location survives app uninstallation and allows instant auto-restore on reinstallation.
     */
    private fun mirrorBackupToDownloads(zipFile: File) {
        if (!zipFile.exists() || zipFile.length() <= 0) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("mindlink_backup_latest.zip", "%MindLink%")
                try {
                    resolver.query(queryUri, arrayOf(MediaStore.MediaColumns._ID), selection, selectionArgs, null)?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                            val itemUri = ContentUris.withAppendedId(queryUri, id)
                            resolver.delete(itemUri, null, null)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("GoogleSyncManager", "MediaStore cleanup warning: ${e.message}")
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "mindlink_backup_latest.zip")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MindLink")
                }
                val uri = resolver.insert(queryUri, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        zipFile.inputStream().use { input -> input.copyTo(out) }
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MindLink")
                if (!dir.exists()) dir.mkdirs()
                val targetFile = File(dir, "mindlink_backup_latest.zip")
                zipFile.copyTo(targetFile, overwrite = true)
            }
            Log.i("GoogleSyncManager", "Mirrored backup to public Downloads/MindLink/mindlink_backup_latest.zip")
        } catch (e: Exception) {
            Log.w("GoogleSyncManager", "Failed to mirror backup to Downloads: ${e.message}")
        }
    }

    /**
     * Discovers and opens an InputStream for the mirrored backup in Downloads/MindLink/.
     */
    fun openMirroredBackupStream(): InputStream? {
        // 1. Direct file access (fastest if accessible)
        try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MindLink")
            if (dir.exists()) {
                val latest = File(dir, "mindlink_backup_latest.zip")
                if (latest.exists() && latest.length() > 0) {
                    return latest.inputStream()
                }
                val zips = dir.listFiles { f -> f.extension == "zip" && f.length() > 0 }
                val newest = zips?.maxByOrNull { it.lastModified() }
                if (newest != null) {
                    return newest.inputStream()
                }
            }
        } catch (_: Exception) {}

        // 2. Query MediaStore.Downloads (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATE_MODIFIED
                )
                val selection = "(${MediaStore.MediaColumns.DISPLAY_NAME} = ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?) AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("mindlink_backup_latest.zip", "mindlink_backup_%.zip", "%MindLink%")
                val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

                resolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val contentUri = ContentUris.withAppendedId(uri, id)
                        return resolver.openInputStream(contentUri)
                    }
                }
            } catch (e: Exception) {
                Log.w("GoogleSyncManager", "MediaStore query error: ${e.message}")
            }
        }

        return null
    }

    fun refreshBackupSlots() {
        val slot1 = File(backupsDir, "backup_slot_1.zip")
        if (!slot1.exists() || slot1.length() == 0L) {
            // Auto-recover from mirrored public Downloads/MindLink backup
            val stream = openMirroredBackupStream()
            if (stream != null) {
                try {
                    stream.use { input ->
                        slot1.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.i("GoogleSyncManager", "Recovered slot 1 from public Downloads/MindLink mirror!")
                } catch (e: Exception) {
                    Log.w("GoogleSyncManager", "Failed to restore mirrored backup: ${e.message}")
                }
            }
        }

        val slots = mutableListOf<BackupSlotInfo>()
        for (i in 1..3) {
            val zipFile = File(backupsDir, "backup_slot_$i.zip")
            val jsonFile = File(backupsDir, "backup_slot_$i.json")
            val targetFile = if (zipFile.exists() && zipFile.length() > 0) zipFile else if (jsonFile.exists() && jsonFile.length() > 0) jsonFile else null

            if (targetFile != null) {
                try {
                    val payload = if (targetFile.name.endsWith(".zip")) {
                        readPayloadFromZip(targetFile)
                    } else {
                        gson.fromJson(targetFile.readText(), MindLinkBackupPayload::class.java)
                    }

                    if (payload != null) {
                        slots.add(
                            BackupSlotInfo(
                                slot = i,
                                fileName = targetFile.name,
                                timestamp = payload.backupTimestamp,
                                notesCount = payload.notesCount,
                                sizeBytes = targetFile.length(),
                                appVersion = payload.appVersion
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }
        _backupSlots.value = slots

        val s1 = slots.firstOrNull { it.slot == 1 }
        if (s1 != null && _lastBackupTime.value <= 0L) {
            _lastBackupTime.value = s1.timestamp
            _lastBackupSize.value = s1.sizeBytes
        }
    }

    private fun readPayloadFromZip(file: File): MindLinkBackupPayload? {
        return try {
            ZipInputStream(file.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "mindlink_notes.json" || entry.name.endsWith(".json")) {
                        val json = zis.bufferedReader().readText()
                        return gson.fromJson(json, MindLinkBackupPayload::class.java)
                    }
                    entry = zis.nextEntry
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun getAccountChooserIntent(): Intent {
        return AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            null,
            null,
            null,
            null
        )
    }

    fun handleSignInResult(data: Intent?): Result<GoogleUserProfile> {
        if (data == null) {
            val err = "Sign-in cancelled"
            _syncState.value = SyncState.Error(err)
            return Result.failure(Exception(err))
        }

        // Try GoogleSignIn first
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null && !account.email.isNullOrBlank()) {
                val profile = GoogleUserProfile(
                    id = account.id ?: account.email!!,
                    displayName = account.displayName ?: account.email!!.substringBefore("@"),
                    email = account.email!!,
                    photoUrl = account.photoUrl?.toString()
                )
                _currentUser.value = profile
                saveUserProfile(profile)
                _syncState.value = SyncState.Success("Signed in as ${profile.email}")
                return Result.success(profile)
            }
        } catch (apiEx: Exception) {
            Log.w("GoogleSyncManager", "GoogleSignInApiException: ${apiEx.message}")
        }

        // Try AccountManager fallback
        try {
            val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                val profile = GoogleUserProfile(
                    id = accountName,
                    displayName = accountName.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = accountName,
                    photoUrl = null
                )
                _currentUser.value = profile
                saveUserProfile(profile)
                _syncState.value = SyncState.Success("Signed in as ${profile.email}")
                return Result.success(profile)
            }
        } catch (_: Exception) {}

        val err = "Could not select Google account"
        _syncState.value = SyncState.Error(err)
        return Result.failure(Exception(err))
    }

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            _currentUser.value = null
            prefs.edit().clear().apply()
            _syncState.value = SyncState.Idle
            onComplete()
        }
    }

    fun setBackupFrequency(frequency: String) {
        _backupFrequency.value = frequency
        prefs.edit().putString("backup_frequency", frequency).apply()

        val workManager = WorkManager.getInstance(context)
        when (frequency) {
            "Daily" -> {
                val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS).build()
                workManager.enqueueUniquePeriodicWork("mindlink_auto_backup", ExistingPeriodicWorkPolicy.UPDATE, request)
            }
            "Weekly" -> {
                val request = PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS).build()
                workManager.enqueueUniquePeriodicWork("mindlink_auto_backup", ExistingPeriodicWorkPolicy.UPDATE, request)
            }
            else -> {
                workManager.cancelUniqueWork("mindlink_auto_backup")
            }
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }

    /**
     * Creates a new backup and rotates slots:
     * slot 2 -> slot 3
     * slot 1 -> slot 2
     * new -> slot 1
     * Keeps exactly the last 3 redundant backups.
     * Bundles all thoughts and attached photos into a compressed ZIP package.
     * Also mirrors to public Downloads and uploads to Google Drive cloud.
     */
    suspend fun createBackup(isAuto: Boolean = false): Result<BackupSlotInfo> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing("Reading thoughts database and photos...")

        try {
            val allNotes = noteDao.getAllNotesListSync()
            val imagesDir = File(context.filesDir, "note_images").apply { if (!exists()) mkdirs() }
            val imageFilesToPack = mutableListOf<File>()

            val dtos = allNotes.map { entity ->
                var imageFileName: String? = null
                if (!entity.imageUri.isNullOrBlank()) {
                    try {
                        val uri = Uri.parse(entity.imageUri)
                        val nameCandidate = uri.lastPathSegment?.substringAfterLast('/')
                            ?: File(uri.path ?: "").name
                        val localCandidate = File(imagesDir, nameCandidate)
                        val candidate = if (localCandidate.exists()) localCandidate else File(uri.path ?: "")
                        if (candidate.exists() && candidate.isFile) {
                            imageFileName = candidate.name
                            if (!imageFilesToPack.contains(candidate)) {
                                imageFilesToPack.add(candidate)
                            }
                        }
                    } catch (_: Exception) {}
                }

                BackupNoteDto(
                    id = entity.id,
                    title = entity.title,
                    content = entity.content,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    summary = entity.summary,
                    mood = entity.mood,
                    tags = entity.tags,
                    actionItems = entity.actionItems,
                    reflectionPrompts = entity.reflectionPrompts,
                    isArchived = entity.isArchived,
                    isDeleted = entity.isDeleted,
                    isLocked = entity.isLocked,
                    isPinned = entity.isPinned,
                    fontFamily = entity.fontFamily,
                    fontSizeScale = entity.fontSizeScale,
                    imageFileName = imageFileName,
                    imageCaption = entity.imageCaption
                )
            }

            val payload = MindLinkBackupPayload(
                appVersion = BuildConfig.VERSION_NAME,
                backupTimestamp = System.currentTimeMillis(),
                notesCount = dtos.size,
                notes = dtos
            )
            val jsonString = gson.toJson(payload)

            // Redundant rotation: shift slot 2 to 3, slot 1 to 2
            val slot3 = File(backupsDir, "backup_slot_3.zip")
            val slot2 = File(backupsDir, "backup_slot_2.zip")
            val slot1 = File(backupsDir, "backup_slot_1.zip")

            if (slot2.exists()) {
                slot2.copyTo(slot3, overwrite = true)
            }
            if (slot1.exists()) {
                slot1.copyTo(slot2, overwrite = true)
            }

            // Write compressed ZIP containing mindlink_notes.json and all attached images
            ZipOutputStream(slot1.outputStream()).use { zos ->
                val jsonEntry = ZipEntry("mindlink_notes.json")
                zos.putNextEntry(jsonEntry)
                zos.write(jsonString.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                imageFilesToPack.forEach { imgFile ->
                    try {
                        val imgEntry = ZipEntry("images/${imgFile.name}")
                        zos.putNextEntry(imgEntry)
                        imgFile.inputStream().use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    } catch (_: Exception) {}
                }
            }

            val now = System.currentTimeMillis()
            val size = slot1.length()
            _lastBackupTime.value = now
            _lastBackupSize.value = size
            prefs.edit()
                .putLong("last_backup_time", now)
                .putLong("last_backup_size", size)
                .apply()

            refreshBackupSlots()

            // 1. Mirror backup to public device Downloads (persists across uninstalls!)
            mirrorBackupToDownloads(slot1)

            // 2. Try Cloud Drive Upload (ZIP containing thoughts + photos)
            tryCloudUpload(slot1)

            val info = _backupSlots.value.firstOrNull { it.slot == 1 }
                ?: BackupSlotInfo(1, "backup_slot_1.zip", now, dtos.size, size, BuildConfig.VERSION_NAME)

            val photoSuffix = if (imageFilesToPack.isNotEmpty()) " with ${imageFilesToPack.size} photos" else ""
            val msg = "Backed up ${dtos.size} thoughts$photoSuffix (${info.formattedSize}) successfully!"
            _syncState.value = SyncState.Success(msg)
            Result.success(info)
        } catch (e: Exception) {
            val err = e.message ?: "Backup failed"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Uploads the full ZIP backup (thoughts + images) to Google Drive appDataFolder.
     */
    private suspend fun tryCloudUpload(zipFile: File) {
        if (!zipFile.exists() || zipFile.length() <= 0) return
        if (_currentUser.value == null) return
        val account = getGoogleAccount() ?: return

        try {
            var uploadAuthError: String? = null
            val token = try {
                GoogleAuthUtil.getToken(
                    context,
                    account,
                    "oauth2:https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file"
                )
            } catch (userEx: UserRecoverableAuthException) {
                Log.w("GoogleSyncManager", "User recoverable auth exception for Drive: ${userEx.message}")
                userEx.intent?.let { _userConsentIntent.emit(it) }
                return
            } catch (authEx: Exception) {
                uploadAuthError = authEx.message
                Log.w("GoogleSyncManager", "Could not obtain Drive OAuth token: ${authEx.message}")
                null
            }

            if (token == null) {
                if (uploadAuthError?.contains("UnregisteredOnApiConsole", ignoreCase = true) == true) {
                    _syncState.value = SyncState.Error(
                        "Google Drive OAuth pending registration in Google Cloud Console. Local & Downloads backup created successfully!"
                    )
                }
                return
            }

            // Search for existing mindlink_backup.zip in appDataFolder
            val searchReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='mindlink_backup.zip'&fields=files(id)")
                .header("Authorization", "Bearer $token")
                .build()
            val searchRes = okHttpClient.newCall(searchReq).execute()
            var fileId: String? = null
            if (searchRes.isSuccessful) {
                val sJson = gson.fromJson(searchRes.body?.string(), JsonObject::class.java)
                val arr = sJson.getAsJsonArray("files")
                if (arr != null && arr.size() > 0) {
                    fileId = arr[0].asJsonObject.get("id")?.asString
                }
            }

            val uploadUrl = if (fileId != null) {
                "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
            } else {
                "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            }

            val uploadReq = if (fileId != null) {
                Request.Builder()
                    .url(uploadUrl)
                    .patch(zipFile.asRequestBody("application/zip".toMediaType()))
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                val metadata = "{\"name\":\"mindlink_backup.zip\",\"parents\":[\"appDataFolder\"]}"
                val multipart = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("metadata", "", metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                    .addFormDataPart("file", "mindlink_backup.zip", zipFile.asRequestBody("application/zip".toMediaType()))
                    .build()
                Request.Builder()
                    .url(uploadUrl)
                    .post(multipart)
                    .header("Authorization", "Bearer $token")
                    .build()
            }
            val res = okHttpClient.newCall(uploadReq).execute()
            if (res.isSuccessful) {
                Log.i("GoogleSyncManager", "Uploaded mindlink_backup.zip to Google Drive appDataFolder successfully!")
            } else {
                Log.w("GoogleSyncManager", "Google Drive upload HTTP ${res.code}: ${res.message}")
            }
        } catch (e: Exception) {
            Log.w("GoogleSyncManager", "tryCloudUpload exception: ${e.message}")
        }
    }

    /**
     * Restores thoughts and photos from one of the 3 redundant backup slots (1 = Latest, 2 = Previous, 3 = Oldest).
     */
    suspend fun restoreFromSlot(slot: Int): Result<Int> = withContext(Dispatchers.IO) {
        val zipFile = File(backupsDir, "backup_slot_$slot.zip")
        val jsonFile = File(backupsDir, "backup_slot_$slot.json")
        val file = if (zipFile.exists()) zipFile else if (jsonFile.exists()) jsonFile else null

        if (file == null || !file.exists()) {
            val err = "Backup slot $slot does not exist."
            _syncState.value = SyncState.Error(err)
            return@withContext Result.failure(Exception(err))
        }

        try {
            val result = if (file.name.endsWith(".zip")) {
                restoreFromZipFile(file)
            } else {
                restoreFromJsonString(file.readText())
            }
            result.onSuccess { count ->
                _syncState.value = SyncState.Success("Restored $count thoughts from Backup #$slot!")
            }
            result
        } catch (e: Exception) {
            val err = e.message ?: "Restore failed"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    suspend fun restoreFromZipFile(file: File): Result<Int> = withContext(Dispatchers.IO) {
        file.inputStream().use { input ->
            restoreFromZipStream(input)
        }
    }

    suspend fun restoreFromZipStream(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing("Unpacking backup archive and photos...")
        try {
            val imagesDir = File(context.filesDir, "note_images").apply { if (!exists()) mkdirs() }
            var jsonContent: String? = null

            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "mindlink_notes.json" || entry.name.endsWith(".json")) {
                        jsonContent = zis.bufferedReader().readText()
                    } else if (entry.name.startsWith("images/")) {
                        val fileName = entry.name.substringAfterLast("/")
                        if (fileName.isNotBlank()) {
                            val targetImg = File(imagesDir, fileName)
                            targetImg.outputStream().use { out ->
                                zis.copyTo(out)
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            if (jsonContent.isNullOrBlank()) {
                val err = "No valid notes found inside backup archive."
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            restoreFromJsonString(jsonContent!!)
        } catch (e: Exception) {
            val err = e.message ?: "Failed to restore archive"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Parses JSON string and restores all thoughts into Room SQLite database with linked photos.
     */
    suspend fun restoreFromJsonString(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing("Parsing backup data...")
        try {
            val payload = gson.fromJson(jsonString, MindLinkBackupPayload::class.java)
            if (payload == null || payload.notes.isEmpty()) {
                val err = "Backup file is empty or corrupted."
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            _syncState.value = SyncState.Syncing("Restoring ${payload.notes.size} thoughts into database...")
            val imagesDir = File(context.filesDir, "note_images").apply { if (!exists()) mkdirs() }

            var count = 0
            payload.notes.forEach { dto ->
                val resolvedImageUri = if (!dto.imageFileName.isNullOrBlank()) {
                    val localImg = File(imagesDir, dto.imageFileName)
                    if (localImg.exists()) {
                        Uri.fromFile(localImg).toString()
                    } else {
                        val match = imagesDir.listFiles()?.firstOrNull { it.name == dto.imageFileName }
                        if (match != null) Uri.fromFile(match).toString() else null
                    }
                } else null

                val entity = NoteEntity(
                    id = dto.id,
                    title = dto.title,
                    content = dto.content,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    summary = dto.summary,
                    mood = dto.mood,
                    tags = dto.tags,
                    actionItems = dto.actionItems,
                    reflectionPrompts = dto.reflectionPrompts,
                    isProcessed = true,
                    imageUri = resolvedImageUri,
                    imageCaption = dto.imageCaption,
                    isArchived = dto.isArchived,
                    isDeleted = dto.isDeleted,
                    isLocked = dto.isLocked,
                    isPinned = dto.isPinned,
                    fontFamily = dto.fontFamily,
                    fontSizeScale = dto.fontSizeScale
                )
                noteDao.insertNote(entity)
                count++
            }

            val msg = "Successfully restored $count thoughts and attached photos!"
            _syncState.value = SyncState.Success(msg)
            refreshBackupSlots()
            Result.success(count)
        } catch (e: Exception) {
            val err = e.message ?: "Failed to restore thoughts"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Reads a backup slot file and writes it to an output stream (e.g. for Google Drive export).
     */
    suspend fun exportSlotToStream(slot: Int, outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        val zipFile = File(backupsDir, "backup_slot_$slot.zip")
        val jsonFile = File(backupsDir, "backup_slot_$slot.json")
        val file = if (zipFile.exists()) zipFile else if (jsonFile.exists()) jsonFile else null
        if (file == null || !file.exists()) {
            return@withContext Result.failure(Exception("Backup slot $slot does not exist"))
        }
        try {
            outputStream.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads the latest backup package (.zip) directly to the device's public Downloads directory.
     */
    suspend fun downloadBackupToDevice(): Result<String> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing("Preparing device backup archive...")
        val slot1 = File(backupsDir, "backup_slot_1.zip")
        if (!slot1.exists() || slot1.length() == 0L) {
            val bRes = createBackup()
            if (bRes.isFailure) {
                val err = bRes.exceptionOrNull()?.message ?: "Could not create backup"
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "mindlink_backup_$timestamp.zip"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MindLink")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(Exception("Failed to create file in Downloads"))
                resolver.openOutputStream(uri)?.use { out ->
                    slot1.inputStream().use { input -> input.copyTo(out) }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MindLink")
                if (!dir.exists()) dir.mkdirs()
                val targetFile = File(dir, fileName)
                slot1.copyTo(targetFile, overwrite = true)
            }
            val msg = "Saved to Downloads/MindLink/$fileName"
            _syncState.value = SyncState.Success(msg)
            Result.success(msg)
        } catch (e: Exception) {
            val err = "Download failed: ${e.message}"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Imports a backup from an input stream (supports both .zip with photos and legacy .json).
     */
    suspend fun importFromInputStream(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val bis = java.io.BufferedInputStream(inputStream)
            bis.mark(1024)
            val header = ByteArray(4)
            val read = bis.read(header)
            bis.reset()

            val isZip = read >= 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
            if (isZip) {
                restoreFromZipStream(bis)
            } else {
                val text = bis.bufferedReader().use { it.readText() }
                restoreFromJsonString(text)
            }
        } catch (e: Exception) {
            val err = e.message ?: "Import failed"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Automatically discovers and restores thoughts and photos from:
     * 1. Google Drive cloud (appDataFolder mindlink_backup.zip or json)
     * 2. Persistent public device storage (Downloads/MindLink/mindlink_backup_latest.zip)
     * 3. Local internal backup slots
     * Automatically unpacking notes and photos into the Room database and files directory.
     */
    suspend fun restoreNotesFromGoogleDrive(): Result<Int> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing("Searching for Google cloud backup...")
        val user = _currentUser.value
        val account = getGoogleAccount()
        var lastAuthError: String? = null

        // 1. Try Google Drive cloud download if account is available
        if (account != null) {
            try {
                val token = try {
                    GoogleAuthUtil.getToken(
                        context,
                        account,
                        "oauth2:https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file"
                    )
                } catch (userEx: UserRecoverableAuthException) {
                    Log.w("GoogleSyncManager", "User recoverable auth required: ${userEx.message}")
                    userEx.intent?.let { _userConsentIntent.emit(it) }
                    null
                } catch (authEx: Exception) {
                    lastAuthError = authEx.message
                    Log.w("GoogleSyncManager", "Drive auth warning: ${authEx.message}")
                    null
                }

                if (token != null) {
                    _syncState.value = SyncState.Syncing("Connecting to Google Drive (${account.name})...")

                    // Search for mindlink_backup.zip first
                    val searchReq = Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='mindlink_backup.zip'&fields=files(id,name,modifiedTime)")
                        .header("Authorization", "Bearer $token")
                        .build()
                    val searchRes = okHttpClient.newCall(searchReq).execute()
                    var zipFileId: String? = null
                    if (searchRes.isSuccessful) {
                        val sJson = gson.fromJson(searchRes.body?.string(), JsonObject::class.java)
                        val arr = sJson.getAsJsonArray("files")
                        if (arr != null && arr.size() > 0) {
                            zipFileId = arr[0].asJsonObject.get("id")?.asString
                        }
                    }

                    if (zipFileId != null) {
                        _syncState.value = SyncState.Syncing("Downloading backup archive and photos from Google Drive...")
                        val dlReq = Request.Builder()
                            .url("https://www.googleapis.com/drive/v3/files/$zipFileId?alt=media")
                            .header("Authorization", "Bearer $token")
                            .build()
                        val dlRes = okHttpClient.newCall(dlReq).execute()
                        if (dlRes.isSuccessful && dlRes.body != null) {
                            val stream = dlRes.body!!.byteStream()
                            val res = restoreFromZipStream(stream)
                            if (res.isSuccess) {
                                refreshBackupSlots()
                                return@withContext res
                            }
                        }
                    }

                    // Fallback to legacy mindlink_backup.json
                    val jsonReq = Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='mindlink_backup.json'&fields=files(id,name,modifiedTime)")
                        .header("Authorization", "Bearer $token")
                        .build()
                    val jsonRes = okHttpClient.newCall(jsonReq).execute()
                    var jsonFileId: String? = null
                    if (jsonRes.isSuccessful) {
                        val sJson = gson.fromJson(jsonRes.body?.string(), JsonObject::class.java)
                        val arr = sJson.getAsJsonArray("files")
                        if (arr != null && arr.size() > 0) {
                            jsonFileId = arr[0].asJsonObject.get("id")?.asString
                        }
                    }

                    if (jsonFileId != null) {
                        _syncState.value = SyncState.Syncing("Downloading backup from Google Drive...")
                        val dlReq = Request.Builder()
                            .url("https://www.googleapis.com/drive/v3/files/$jsonFileId?alt=media")
                            .header("Authorization", "Bearer $token")
                            .build()
                        val dlRes = okHttpClient.newCall(dlReq).execute()
                        if (dlRes.isSuccessful && dlRes.body != null) {
                            val json = dlRes.body!!.string()
                            val res = restoreFromJsonString(json)
                            if (res.isSuccess) {
                                refreshBackupSlots()
                                return@withContext res
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("GoogleSyncManager", "Drive restore attempt warning: ${e.message}")
            }
        }

        // 2. Fallback: Search persistent device storage (Downloads/MindLink/)
        _syncState.value = SyncState.Syncing("Searching persistent device backup...")
        val mirroredStream = openMirroredBackupStream()
        if (mirroredStream != null) {
            val res = restoreFromZipStream(mirroredStream)
            if (res.isSuccess) {
                refreshBackupSlots()
                return@withContext res
            }
        }

        // 3. Fallback: Local slots
        val slots = _backupSlots.value
        if (slots.isNotEmpty()) {
            return@withContext restoreFromSlot(slots.first().slot)
        }

        val err = when {
            lastAuthError?.contains("UnregisteredOnApiConsole", ignoreCase = true) == true ->
                "Google Drive API requires an OAuth Client ID in Google Cloud Console. Tap 'Restore File' or 'Save to Drive' above to backup & restore directly via device storage."
            user != null ->
                "No backup found on Google Drive for ${user.email} or on device storage. Tap 'Restore File' above to load a backup file."
            else ->
                "No local or cloud backup found. Tap 'Download to Device' or 'Backup Now' to create one."
        }
        _syncState.value = SyncState.Error(err)
        Result.failure(Exception(err))
    }
}

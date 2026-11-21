package com.mindlink.journal.data.sync

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.GoogleAuthUtil
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
    val fontFamily: String,
    val fontSizeScale: Float?
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

    fun refreshBackupSlots() {
        val slots = mutableListOf<BackupSlotInfo>()
        for (i in 1..3) {
            val file = File(backupsDir, "backup_slot_$i.json")
            if (file.exists() && file.length() > 0) {
                try {
                    val json = file.readText()
                    val payload = gson.fromJson(json, MindLinkBackupPayload::class.java)
                    if (payload != null) {
                        slots.add(
                            BackupSlotInfo(
                                slot = i,
                                fileName = file.name,
                                timestamp = payload.backupTimestamp,
                                notesCount = payload.notesCount,
                                sizeBytes = file.length(),
                                appVersion = payload.appVersion
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }
        _backupSlots.value = slots
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
            android.util.Log.w("GoogleSyncManager", "GoogleSignInApiException: ${apiEx.message}")
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
     */
    suspend fun createBackup(isAuto: Boolean = false): Result<BackupSlotInfo> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing("Reading thoughts database...")

        try {
            val allNotes = noteDao.getAllNotesListSync()
            val dtos = allNotes.map { entity ->
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
                    fontFamily = entity.fontFamily,
                    fontSizeScale = entity.fontSizeScale
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
            val slot3 = File(backupsDir, "backup_slot_3.json")
            val slot2 = File(backupsDir, "backup_slot_2.json")
            val slot1 = File(backupsDir, "backup_slot_1.json")

            if (slot2.exists()) {
                slot2.copyTo(slot3, overwrite = true)
            }
            if (slot1.exists()) {
                slot1.copyTo(slot2, overwrite = true)
            }

            slot1.writeText(jsonString)

            val now = System.currentTimeMillis()
            val size = slot1.length()
            _lastBackupTime.value = now
            _lastBackupSize.value = size
            prefs.edit()
                .putLong("last_backup_time", now)
                .putLong("last_backup_size", size)
                .apply()

            refreshBackupSlots()

            // Try Cloud Drive Upload if user is signed in
            tryCloudUpload(jsonString)

            val info = _backupSlots.value.firstOrNull { it.slot == 1 }
                ?: BackupSlotInfo(1, "backup_slot_1.json", now, dtos.size, size, BuildConfig.VERSION_NAME)

            val msg = "Backed up ${dtos.size} thoughts (${info.formattedSize}) successfully!"
            _syncState.value = SyncState.Success(msg)
            Result.success(info)
        } catch (e: Exception) {
            val err = e.message ?: "Backup failed"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    private suspend fun tryCloudUpload(jsonString: String) {
        val user = _currentUser.value ?: return
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && account.account != null) {
                val token = GoogleAuthUtil.getToken(
                    context,
                    account.account!!,
                    "oauth2:https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file"
                )
                // Search for existing file
                val searchReq = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='mindlink_backup.json'&fields=files(id)")
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
                        .patch(jsonString.toRequestBody("application/json".toMediaType()))
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    val metadata = "{\"name\":\"mindlink_backup.json\",\"parents\":[\"appDataFolder\"]}"
                    val multipart = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("metadata", "", metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                        .addFormDataPart("file", "mindlink_backup.json", jsonString.toRequestBody("application/json".toMediaType()))
                        .build()
                    Request.Builder()
                        .url(uploadUrl)
                        .post(multipart)
                        .header("Authorization", "Bearer $token")
                        .build()
                }
                okHttpClient.newCall(uploadReq).execute()
            }
        } catch (_: Exception) {}
    }

    /**
     * Restores thoughts from one of the 3 redundant backup slots (1 = Latest, 2 = Previous, 3 = Oldest).
     */
    suspend fun restoreFromSlot(slot: Int): Result<Int> = withContext(Dispatchers.IO) {
        val file = File(backupsDir, "backup_slot_$slot.json")
        if (!file.exists()) {
            val err = "Backup slot $slot does not exist."
            _syncState.value = SyncState.Error(err)
            return@withContext Result.failure(Exception(err))
        }

        try {
            val json = file.readText()
            val result = restoreFromJsonString(json)
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

    /**
     * Parses JSON string and restores all thoughts into Room SQLite database.
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

            var count = 0
            payload.notes.forEach { dto ->
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
                    isArchived = dto.isArchived,
                    isDeleted = dto.isDeleted,
                    isLocked = dto.isLocked,
                    fontFamily = dto.fontFamily,
                    fontSizeScale = dto.fontSizeScale
                )
                noteDao.insertNote(entity)
                count++
            }

            val msg = "Successfully restored $count thoughts!"
            _syncState.value = SyncState.Success(msg)
            Result.success(count)
        } catch (e: Exception) {
            val err = e.message ?: "Failed to restore thoughts"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Reads a backup slot file and writes it to an output stream (e.g. for Storage Access Framework / Google Drive export).
     */
    suspend fun exportSlotToStream(slot: Int, outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        val file = File(backupsDir, "backup_slot_$slot.json")
        if (!file.exists()) {
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
     * Imports a backup from an input stream (e.g. from Google Drive file picker / OpenDocument).
     */
    suspend fun importFromInputStream(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val text = inputStream.bufferedReader().use { it.readText() }
            restoreFromJsonString(text)
        } catch (e: Exception) {
            val err = e.message ?: "Import failed"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Downloads mindlink_backup.json from Google Drive cloud and restores into Room SQLite.
     */
    suspend fun restoreNotesFromGoogleDrive(): Result<Int> = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        if (user == null) {
            val err = "Please sign in with Google first."
            _syncState.value = SyncState.Error(err)
            return@withContext Result.failure(Exception(err))
        }

        // If local redundant backup slot 1 exists, use it as fast instant restore
        val slot1 = File(backupsDir, "backup_slot_1.json")
        if (slot1.exists()) {
            return@withContext restoreFromSlot(1)
        }

        _syncState.value = SyncState.Syncing("Connecting to Google Drive...")

        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null || account.account == null) {
                // If no OAuth token, restore from available slot
                val slots = _backupSlots.value
                if (slots.isNotEmpty()) {
                    return@withContext restoreFromSlot(slots.first().slot)
                }
                val err = "No local or cloud backup found."
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val token = GoogleAuthUtil.getToken(
                context,
                account.account!!,
                "oauth2:https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file"
            )

            val searchReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='mindlink_backup.json'&fields=files(id,name,modifiedTime)")
                .header("Authorization", "Bearer $token")
                .build()

            val searchRes = okHttpClient.newCall(searchReq).execute()
            if (!searchRes.isSuccessful) {
                // Fallback to local slots
                val slots = _backupSlots.value
                if (slots.isNotEmpty()) {
                    return@withContext restoreFromSlot(slots.first().slot)
                }
                val err = "No existing backup found in Google Drive."
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val sJson = gson.fromJson(searchRes.body?.string(), JsonObject::class.java)
            val arr = sJson.getAsJsonArray("files")
            if (arr == null || arr.size() == 0) {
                val slots = _backupSlots.value
                if (slots.isNotEmpty()) {
                    return@withContext restoreFromSlot(slots.first().slot)
                }
                val err = "No backup found on Google Drive."
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val fileId = arr[0].asJsonObject.get("id").asString
            _syncState.value = SyncState.Syncing("Downloading backup from Google Drive...")

            val downloadReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                .header("Authorization", "Bearer $token")
                .build()

            val downloadRes = okHttpClient.newCall(downloadReq).execute()
            val jsonContent = downloadRes.body?.string() ?: ""
            restoreFromJsonString(jsonContent)
        } catch (e: Exception) {
            // Graceful fallback to local redundant slots
            val slots = _backupSlots.value
            if (slots.isNotEmpty()) {
                return@withContext restoreFromSlot(slots.first().slot)
            }
            val err = e.message ?: "Restore failed"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }
}

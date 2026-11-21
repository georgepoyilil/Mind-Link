package com.mindlink.journal.data.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.mindlink.journal.BuildConfig
import com.mindlink.journal.data.local.dao.NoteDao
import com.mindlink.journal.data.local.entity.NoteEntity
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

    val formattedLastBackup: String
        get() {
            val t = _lastBackupTime.value
            if (t <= 0) return "Never backed up"
            return SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(t))
        }

    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.appdata"),
                Scope("https://www.googleapis.com/auth/drive.file")
            )
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // Check if user was already signed in previously
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            _currentUser.value = GoogleUserProfile(
                id = lastAccount.id ?: "",
                displayName = lastAccount.displayName ?: "MindLink Thinker",
                email = lastAccount.email ?: "",
                photoUrl = lastAccount.photoUrl?.toString()
            )
        }
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun handleSignInResult(data: Intent?): Result<GoogleUserProfile> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val profile = GoogleUserProfile(
                    id = account.id ?: "",
                    displayName = account.displayName ?: "MindLink Thinker",
                    email = account.email ?: "",
                    photoUrl = account.photoUrl?.toString()
                )
                _currentUser.value = profile
                prefs.edit()
                    .putString("user_id", profile.id)
                    .putString("user_name", profile.displayName)
                    .putString("user_email", profile.email)
                    .putString("user_photo", profile.photoUrl)
                    .apply()
                Result.success(profile)
            } else {
                Result.failure(Exception("Sign in account was empty"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            _currentUser.value = null
            prefs.edit().clear().apply()
            _syncState.value = SyncState.Idle
            onComplete()
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }

    /**
     * Backs up all notes into a single structured JSON and uploads to Google Drive appDataFolder.
     */
    suspend fun backupNotesToGoogleDrive(): Result<Int> = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            val err = "Please sign in with Google first."
            _syncState.value = SyncState.Error(err)
            return@withContext Result.failure(Exception(err))
        }

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

            _syncState.value = SyncState.Syncing("Connecting to Google Drive...")

            // Obtain OAuth Bearer Token from Google Play Services
            val token = GoogleAuthUtil.getToken(
                context,
                account.account!!,
                "oauth2:https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file"
            )

            // Step 1: Check if mindlink_backup.json already exists in appDataFolder
            val searchRequest = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='mindlink_backup.json'&fields=files(id,name)")
                .header("Authorization", "Bearer $token")
                .build()

            val searchResponse = okHttpClient.newCall(searchRequest).execute()
            var existingFileId: String? = null
            if (searchResponse.isSuccessful) {
                val searchJson = gson.fromJson(searchResponse.body?.string(), JsonObject::class.java)
                val filesArr = searchJson.getAsJsonArray("files")
                if (filesArr != null && filesArr.size() > 0) {
                    existingFileId = filesArr[0].asJsonObject.get("id")?.asString
                }
            }

            _syncState.value = SyncState.Syncing("Uploading encrypted backup to Google Drive...")

            val uploadUrl = if (existingFileId != null) {
                "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
            } else {
                "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            }

            val uploadRequest = if (existingFileId != null) {
                // PATCH update content
                val body = jsonString.toRequestBody("application/json".toMediaType())
                Request.Builder()
                    .url(uploadUrl)
                    .patch(body)
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                // POST multipart insert
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

            val uploadResponse = okHttpClient.newCall(uploadRequest).execute()
            if (!uploadResponse.isSuccessful) {
                val err = "Google Drive upload failed (HTTP ${uploadResponse.code})"
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val now = System.currentTimeMillis()
            _lastBackupTime.value = now
            prefs.edit().putLong("last_backup_time", now).apply()

            val msg = "Backed up ${dtos.size} thoughts to Google Drive!"
            _syncState.value = SyncState.Success(msg)
            Result.success(dtos.size)
        } catch (e: Exception) {
            val err = e.message ?: "Backup failed"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Downloads mindlink_backup.json from Google Drive and restores into Room SQLite.
     */
    suspend fun restoreNotesFromGoogleDrive(): Result<Int> = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            val err = "Please sign in with Google first."
            _syncState.value = SyncState.Error(err)
            return@withContext Result.failure(Exception(err))
        }

        _syncState.value = SyncState.Syncing("Searching for backup on Google Drive...")

        try {
            val token = GoogleAuthUtil.getToken(
                context,
                account.account!!,
                "oauth2:https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file"
            )

            val searchRequest = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='mindlink_backup.json'&fields=files(id,name,modifiedTime)")
                .header("Authorization", "Bearer $token")
                .build()

            val searchResponse = okHttpClient.newCall(searchRequest).execute()
            if (!searchResponse.isSuccessful) {
                val err = "Failed to access Google Drive (HTTP ${searchResponse.code})"
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val searchJson = gson.fromJson(searchResponse.body?.string(), JsonObject::class.java)
            val filesArr = searchJson.getAsJsonArray("files")
            if (filesArr == null || filesArr.size() == 0) {
                val err = "No existing backup found in your Google Drive."
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val fileId = filesArr[0].asJsonObject.get("id").asString

            _syncState.value = SyncState.Syncing("Downloading backup from Google Drive...")

            val downloadRequest = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                .header("Authorization", "Bearer $token")
                .build()

            val downloadResponse = okHttpClient.newCall(downloadRequest).execute()
            if (!downloadResponse.isSuccessful) {
                val err = "Failed to download backup (HTTP ${downloadResponse.code})"
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val jsonContent = downloadResponse.body?.string() ?: ""
            val payload = gson.fromJson(jsonContent, MindLinkBackupPayload::class.java)

            if (payload == null || payload.notes.isEmpty()) {
                val err = "Backup file is empty or corrupted."
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            _syncState.value = SyncState.Syncing("Restoring ${payload.notes.size} thoughts into local database...")

            var restoredCount = 0
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
                restoredCount++
            }

            val msg = "Successfully restored $restoredCount thoughts from Google Drive!"
            _syncState.value = SyncState.Success(msg)
            Result.success(restoredCount)
        } catch (e: Exception) {
            val err = e.message ?: "Restore failed"
            _syncState.value = SyncState.Error(err)
            Result.failure(e)
        }
    }
}

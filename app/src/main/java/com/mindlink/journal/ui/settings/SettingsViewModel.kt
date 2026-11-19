package com.mindlink.journal.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.mindlink.journal.data.ai.CompositeBrainstormingEngine
import com.mindlink.journal.data.ai.ModelRegistry
import com.mindlink.journal.data.ai.SlmModelSpec
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.data.worker.ModelDownloadWorker
import com.mindlink.journal.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class SettingsUiState(
    val apiKey: String = "",
    val totalNotes: Int = 0,
    val totalEmbeddings: Int = 0,
    val isReindexing: Boolean = false,
    val reindexMessage: String? = null,
    val activeEngineName: String = "Local Life-Sphere NLP Engine (Zero Weights)",
    val slmModelInfo: String = "Not installed",
    val isModelInstalled: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadStatus: String? = null,
    val isCheckingUpdates: Boolean = false,
    val updateCheckMessage: String? = null,
    val availableUpdateModel: SlmModelSpec? = null,
    val engineMode: String = "AUTO", // "AUTO", "SLM", "GEMINI", "LOCAL"
    val testPrompt: String = "Give 2 thoughtful questions for someone reflecting on an argument with their partner.",
    val testOutput: String? = null,
    val isTesting: Boolean = false,
    val testLatencyMs: Long = 0L,
    val fontScale: Float = 1.0f,
    val isCheckingAppUpdate: Boolean = false,
    val appUpdateInfo: com.mindlink.journal.data.ota.AppReleaseInfo? = null,
    val appUpdateMessage: String? = null,
    val isDownloadingAppUpdate: Boolean = false,
    val appUpdateProgress: Float = 0f,
    val downloadedApkFile: File? = null
)

class SettingsViewModel(
    private val repository: NoteRepository,
    private val preferences: SharedPreferences,
    private val compositeEngine: CompositeBrainstormingEngine? = null,
    private val appContext: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val savedKey = (appContext?.let { ServiceLocator.getInstance(it).getApiKey() } ?: preferences.getString(ServiceLocator.PREF_GEMINI_API_KEY, "")) ?: ""
        val savedMode = preferences.getString(ServiceLocator.PREF_ENGINE_MODE, "AUTO") ?: "AUTO"
        val savedFontScale = preferences.getFloat(ServiceLocator.PREF_GLOBAL_FONT_SCALE, 1.0f)
        _uiState.update { it.copy(apiKey = savedKey, engineMode = savedMode, fontScale = savedFontScale) }
        refreshEngineStatus()

        viewModelScope.launch {
            repository.getNotesCount().collect { count ->
                _uiState.update { it.copy(totalNotes = count) }
            }
        }

        viewModelScope.launch {
            repository.getEmbeddingsCount().collect { count ->
                _uiState.update { it.copy(totalEmbeddings = count) }
            }
        }

        if (appContext != null) {
            val workManager = WorkManager.getInstance(appContext)
            viewModelScope.launch {
                workManager.getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.UNIQUE_WORK_NAME).collect { workInfos ->
                    val workInfo = workInfos.firstOrNull() ?: return@collect
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getFloat(ModelDownloadWorker.KEY_PROGRESS, 0f)
                            val status = workInfo.progress.getString(ModelDownloadWorker.KEY_STATUS) ?: "Downloading SLM model in background..."
                            _uiState.update {
                                it.copy(
                                    isDownloadingModel = true,
                                    downloadProgress = progress,
                                    downloadStatus = status
                                )
                            }
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            _uiState.update {
                                it.copy(
                                    isDownloadingModel = false,
                                    downloadProgress = 1.0f,
                                    downloadStatus = "Model installed successfully!"
                                )
                            }
                            refreshEngineStatus()
                        }
                        WorkInfo.State.FAILED -> {
                            val status = workInfo.outputData.getString(ModelDownloadWorker.KEY_STATUS) ?: "Model download failed."
                            _uiState.update {
                                it.copy(
                                    isDownloadingModel = false,
                                    downloadStatus = status
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun refreshEngineStatus() {
        val engineName = compositeEngine?.getActiveEngineName() ?: "Local Life-Sphere NLP Engine (Zero Weights)"
        val slmInfo = compositeEngine?.mediaPipeEngine?.getModelInfo() ?: "Not installed"
        val isInstalled = compositeEngine?.mediaPipeEngine?.isModelAvailable() ?: false
        val savedMode = preferences.getString(ServiceLocator.PREF_ENGINE_MODE, "AUTO") ?: "AUTO"
        _uiState.update {
            it.copy(
                activeEngineName = engineName,
                slmModelInfo = slmInfo,
                isModelInstalled = isInstalled,
                engineMode = savedMode
            )
        }
    }

    fun setEngineMode(mode: String) {
        preferences.edit().putString(ServiceLocator.PREF_ENGINE_MODE, mode).apply()
        _uiState.update { it.copy(engineMode = mode) }
        refreshEngineStatus()
    }

    fun updateApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key) }
        if (appContext != null) {
            ServiceLocator.getInstance(appContext).saveApiKey(key)
        } else {
            preferences.edit().putString(ServiceLocator.PREF_GEMINI_API_KEY, key.trim()).apply()
        }
        refreshEngineStatus()
    }

    fun updateFontScale(scale: Float) {
        _uiState.update { it.copy(fontScale = scale) }
        if (appContext != null) {
            ServiceLocator.getInstance(appContext).updateGlobalFontScale(scale)
        } else {
            preferences.edit().putFloat(ServiceLocator.PREF_GLOBAL_FONT_SCALE, scale).apply()
        }
    }

    fun updateTestPrompt(prompt: String) {
        _uiState.update { it.copy(testPrompt = prompt) }
    }

    fun checkForModelUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdates = true, updateCheckMessage = null) }
            val currentModel = compositeEngine?.mediaPipeEngine?.getModelInfo()
            val result = ModelRegistry.checkForUpdates(currentModel)
            _uiState.update {
                it.copy(
                    isCheckingUpdates = false,
                    updateCheckMessage = result.message,
                    availableUpdateModel = if (result.isUpdateAvailable) result.latestModel else null
                )
            }
        }
    }

    fun runTestPrompt() {
        val prompt = _uiState.value.testPrompt
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testOutput = null) }
            val startTime = System.currentTimeMillis()
            val output = try {
                compositeEngine?.mediaPipeEngine?.generateRawResponse(prompt)
                    ?: "Engine not initialized."
            } catch (e: Exception) {
                "Error running on-device inference: ${e.message}"
            }
            val elapsed = System.currentTimeMillis() - startTime
            _uiState.update {
                it.copy(
                    isTesting = false,
                    testOutput = output,
                    testLatencyMs = elapsed
                )
            }
        }
    }

    fun downloadModel(spec: SlmModelSpec? = null) {
        if (_uiState.value.isDownloadingModel) return
        val context = appContext ?: return
        val modelToDownload = spec ?: ModelRegistry.DEFAULT_MODELS.first()

        val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(
                workDataOf(
                    ModelDownloadWorker.KEY_MODEL_ID to modelToDownload.id,
                    ModelDownloadWorker.KEY_MODEL_NAME to modelToDownload.name,
                    ModelDownloadWorker.KEY_DOWNLOAD_URL to modelToDownload.downloadUrl
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ModelDownloadWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        _uiState.update {
            it.copy(
                isDownloadingModel = true,
                downloadProgress = 0f,
                downloadStatus = "Background download started for ${modelToDownload.name}..."
            )
        }
    }

    fun deleteModel() {
        val targetDir = appContext?.filesDir?.let { File(it, "models") } ?: return
        targetDir.listFiles()?.filter { it.extension == "task" || it.extension == "tmp" || it.extension == "bin" }?.forEach {
            it.delete()
        }
        compositeEngine?.mediaPipeEngine?.reloadModel()
        refreshEngineStatus()
    }

    fun checkForAppUpdate() {
        val context = appContext ?: return
        val otaManager = com.mindlink.journal.data.ota.OtaUpdateManager(context)
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingAppUpdate = true, appUpdateMessage = null) }
            val result = otaManager.checkForUpdates()
            result.onSuccess { info ->
                if (info != null && info.isNewer) {
                    _uiState.update {
                        it.copy(
                            isCheckingAppUpdate = false,
                            appUpdateInfo = info,
                            appUpdateMessage = "New version ${info.tagName} is available!"
                        )
                    }
                } else if (info != null) {
                    _uiState.update {
                        it.copy(
                            isCheckingAppUpdate = false,
                            appUpdateInfo = null,
                            appUpdateMessage = "You're up to date (v${com.mindlink.journal.BuildConfig.VERSION_NAME})."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCheckingAppUpdate = false,
                            appUpdateMessage = "No APK releases found on GitHub."
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isCheckingAppUpdate = false,
                        appUpdateMessage = "Update check failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun downloadAndInstallAppUpdate() {
        val info = _uiState.value.appUpdateInfo ?: return
        val context = appContext ?: return
        val otaManager = com.mindlink.journal.data.ota.OtaUpdateManager(context)

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingAppUpdate = true, appUpdateProgress = 0f) }
            val result = otaManager.downloadApk(info) { progress ->
                _uiState.update { it.copy(appUpdateProgress = progress) }
            }
            result.onSuccess { apkFile ->
                _uiState.update {
                    it.copy(
                        isDownloadingAppUpdate = false,
                        downloadedApkFile = apkFile,
                        appUpdateMessage = "Update downloaded. Ready to install!"
                    )
                }
                otaManager.triggerInstall(apkFile)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isDownloadingAppUpdate = false,
                        appUpdateMessage = "Download failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun installDownloadedApk() {
        val file = _uiState.value.downloadedApkFile ?: return
        val context = appContext ?: return
        com.mindlink.journal.data.ota.OtaUpdateManager(context).triggerInstall(file)
    }

    fun reindexAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isReindexing = true, reindexMessage = null) }
            repository.reindexAllNotes()
            _uiState.update {
                it.copy(
                    isReindexing = false,
                    reindexMessage = "Re-indexing tasks enqueued in background WorkManager!"
                )
            }
            refreshEngineStatus()
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isReindexing = true, reindexMessage = null) }
            com.mindlink.journal.data.local.SampleDataSeeder.seedSampleNotes(repository)
            _uiState.update {
                it.copy(
                    isReindexing = false,
                    reindexMessage = "8 diverse sample thoughts loaded & vector indexed!"
                )
            }
        }
    }

    class Factory(
        private val repository: NoteRepository,
        private val preferences: SharedPreferences,
        private val compositeEngine: CompositeBrainstormingEngine? = null,
        private val appContext: Context? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, preferences, compositeEngine, appContext) as T
        }
    }
}

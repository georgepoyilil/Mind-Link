package com.mindlink.journal.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.ai.CompositeBrainstormingEngine
import com.mindlink.journal.data.ai.ModelRegistry
import com.mindlink.journal.data.ai.SlmModelSpec
import com.mindlink.journal.data.repository.NoteRepository
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
    val testLatencyMs: Long = 0L
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
        val savedKey = preferences.getString(ServiceLocator.PREF_GEMINI_API_KEY, "") ?: ""
        val savedMode = preferences.getString(ServiceLocator.PREF_ENGINE_MODE, "AUTO") ?: "AUTO"
        _uiState.update { it.copy(apiKey = savedKey, engineMode = savedMode) }
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
        preferences.edit().putString(ServiceLocator.PREF_GEMINI_API_KEY, key.trim()).apply()
        refreshEngineStatus()
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
        val targetDir = appContext?.filesDir?.let { File(it, "models") } ?: return
        if (!targetDir.exists()) targetDir.mkdirs()
        val targetFile = File(targetDir, "qwen2.5_0.5b.task")

        val modelToDownload = spec ?: ModelRegistry.DEFAULT_MODELS.first()
        val modelUrl = modelToDownload.downloadUrl

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isDownloadingModel = true,
                    downloadProgress = 0f,
                    downloadStatus = "Connecting to model repository (${modelToDownload.name})..."
                )
            }

            try {
                val url = URL(modelUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 60000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("Server returned HTTP ${connection.responseCode}")
                }

                val totalBytes = connection.contentLengthLong
                val inputStream = connection.inputStream
                val tempFile = File(targetDir, "qwen2.5_0.5b.tmp")
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
                        _uiState.update {
                            it.copy(
                                downloadProgress = progress,
                                downloadStatus = "Downloading ${modelToDownload.name}: $readMb / $totalMb MB (${(progress * 100).toInt()}%)"
                            )
                        }
                        lastUpdate = now
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (tempFile.renameTo(targetFile)) {
                    Log.i("SettingsViewModel", "Model downloaded successfully to ${targetFile.name}")
                }

                compositeEngine?.mediaPipeEngine?.reloadModel()

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isDownloadingModel = false,
                            downloadProgress = 1f,
                            downloadStatus = "Installed & Active: ${modelToDownload.name}",
                            isModelInstalled = true,
                            availableUpdateModel = null
                        )
                    }
                    refreshEngineStatus()
                }

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to download SLM model: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isDownloadingModel = false,
                            downloadStatus = "Download error: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    fun deleteModel() {
        val targetDir = appContext?.filesDir?.let { File(it, "models") } ?: return
        val targetFile = File(targetDir, "qwen2.5_0.5b.task")
        if (targetFile.exists()) {
            targetFile.delete()
        }
        compositeEngine?.mediaPipeEngine?.reloadModel()
        refreshEngineStatus()
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

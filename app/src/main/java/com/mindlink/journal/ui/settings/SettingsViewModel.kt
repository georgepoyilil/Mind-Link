package com.mindlink.journal.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.ai.CompositeBrainstormingEngine
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.di.ServiceLocator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val totalNotes: Int = 0,
    val totalEmbeddings: Int = 0,
    val isReindexing: Boolean = false,
    val reindexMessage: String? = null,
    val activeEngineName: String = "Local Life-Sphere NLP Engine (Zero Weights)",
    val slmModelInfo: String = "Not installed"
)

class SettingsViewModel(
    private val repository: NoteRepository,
    private val preferences: SharedPreferences,
    private val compositeEngine: CompositeBrainstormingEngine? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val savedKey = preferences.getString(ServiceLocator.PREF_GEMINI_API_KEY, "") ?: ""
        _uiState.update { it.copy(apiKey = savedKey) }
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
        _uiState.update { it.copy(activeEngineName = engineName, slmModelInfo = slmInfo) }
    }

    fun updateApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key) }
        preferences.edit().putString(ServiceLocator.PREF_GEMINI_API_KEY, key.trim()).apply()
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
        private val compositeEngine: CompositeBrainstormingEngine? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, preferences, compositeEngine) as T
        }
    }
}

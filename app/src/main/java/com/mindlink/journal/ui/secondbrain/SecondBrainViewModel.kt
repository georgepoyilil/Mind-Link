package com.mindlink.journal.ui.secondbrain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.domain.model.SearchResult
import com.mindlink.journal.domain.model.SynthesisResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecondBrainUiState(
    val query: String = "",
    val isSynthesizing: Boolean = false,
    val isSearching: Boolean = false,
    val synthesisResponse: SynthesisResponse? = null,
    val hybridResults: List<SearchResult> = emptyList(),
    val errorMessage: String? = null
)

class SecondBrainViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecondBrainUiState())
    val uiState: StateFlow<SecondBrainUiState> = _uiState.asStateFlow()

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun submitQuery(userQuery: String? = null) {
        val targetQuery = userQuery ?: _uiState.value.query
        if (targetQuery.isBlank()) return

        _uiState.update {
            it.copy(
                query = targetQuery,
                isSynthesizing = true,
                isSearching = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                // 1. Run Hybrid Search (FTS + Vector cosine similarity)
                val searchResults = repository.searchNotesHybrid(targetQuery)

                // 2. Run Conversational Synthesis
                val synthesis = repository.synthesizeSecondBrain(targetQuery)

                _uiState.update {
                    it.copy(
                        isSynthesizing = false,
                        isSearching = false,
                        synthesisResponse = synthesis,
                        hybridResults = searchResults
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSynthesizing = false,
                        isSearching = false,
                        errorMessage = e.message ?: "Failed to query Second Brain"
                    )
                }
            }
        }
    }

    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SecondBrainViewModel(repository) as T
        }
    }
}

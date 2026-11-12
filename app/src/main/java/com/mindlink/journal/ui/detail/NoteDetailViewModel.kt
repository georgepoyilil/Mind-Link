package com.mindlink.journal.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.domain.model.Note
import com.mindlink.journal.domain.model.RelatedThought
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteDetailUiState(
    val note: Note? = null,
    val relatedThoughts: List<RelatedThought> = emptyList(),
    val completedActionIndices: Set<Int> = emptySet(),
    val isLoadingRelated: Boolean = true,
    val isDeleted: Boolean = false
)

class NoteDetailViewModel(
    private val repository: NoteRepository,
    private val noteId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        loadNoteAndRelated()
    }

    private fun loadNoteAndRelated() {
        viewModelScope.launch {
            repository.getNoteById(noteId).collect { note ->
                _uiState.update { it.copy(note = note) }
                if (note != null) {
                    loadRelatedThoughts(note.id)
                }
            }
        }
    }

    private fun loadRelatedThoughts(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRelated = true) }
            val related = repository.getRelatedThoughts(id, topK = 3)
            _uiState.update {
                it.copy(
                    relatedThoughts = related,
                    isLoadingRelated = false
                )
            }
        }
    }

    fun toggleActionItem(index: Int) {
        _uiState.update { state ->
            val set = state.completedActionIndices.toMutableSet()
            if (set.contains(index)) set.remove(index) else set.add(index)
            state.copy(completedActionIndices = set)
        }
    }

    fun appendContent(additionalText: String) {
        val currentNote = _uiState.value.note ?: return
        val newContent = if (currentNote.content.isBlank()) {
            additionalText
        } else {
            "${currentNote.content}\n\n$additionalText"
        }
        val updatedNote = currentNote.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveNote(updatedNote)
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            _uiState.update { it.copy(isDeleted = true) }
            onDeleted()
        }
    }

    class Factory(
        private val repository: NoteRepository,
        private val noteId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteDetailViewModel(repository, noteId) as T
        }
    }
}

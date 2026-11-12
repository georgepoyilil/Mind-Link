package com.mindlink.journal.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.domain.model.Note
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val filteredNotes: List<Note> = emptyList(),
    val selectedMood: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class NotesViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllNotes().collect { notesList ->
                _uiState.update { state ->
                    val filtered = applyFilter(notesList, state.selectedMood, state.searchQuery)
                    state.copy(
                        notes = notesList,
                        filteredNotes = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectMoodFilter(mood: String?) {
        _uiState.update { state ->
            val newMood = if (state.selectedMood == mood) null else mood
            val filtered = applyFilter(state.notes, newMood, state.searchQuery)
            state.copy(selectedMood = newMood, filteredNotes = filtered)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.notes, state.selectedMood, query)
            state.copy(searchQuery = query, filteredNotes = filtered)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            com.mindlink.journal.data.local.SampleDataSeeder.seedSampleNotes(repository)
        }
    }

    private fun applyFilter(notes: List<Note>, mood: String?, query: String): List<Note> {
        return notes.filter { note ->
            val matchesMood = mood == null || note.mood?.equals(mood, ignoreCase = true) == true
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    (note.summary?.contains(query, ignoreCase = true) == true) ||
                    note.tags.any { it.contains(query, ignoreCase = true) }
            matchesMood && matchesQuery
        }
    }

    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesViewModel(repository) as T
        }
    }
}

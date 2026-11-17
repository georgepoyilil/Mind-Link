package com.mindlink.journal.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.domain.model.Note
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class NotesTab(val label: String) {
    JOURNAL("Journal"),
    ARCHIVE("Archive"),
    TRASH("Trash")
}

data class NotesUiState(
    val selectedTab: NotesTab = NotesTab.JOURNAL,
    val notes: List<Note> = emptyList(),
    val archivedNotes: List<Note> = emptyList(),
    val trashNotes: List<Note> = emptyList(),
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
            combine(
                repository.getAllNotes(),
                repository.getArchivedNotes(),
                repository.getTrashNotes()
            ) { active, archived, trash ->
                Triple(active, archived, trash)
            }.collect { (active, archived, trash) ->
                _uiState.update { state ->
                    val currentSource = when (state.selectedTab) {
                        NotesTab.JOURNAL -> active
                        NotesTab.ARCHIVE -> archived
                        NotesTab.TRASH -> trash
                    }
                    val filtered = applyFilter(currentSource, state.selectedMood, state.searchQuery)
                    state.copy(
                        notes = active,
                        archivedNotes = archived,
                        trashNotes = trash,
                        filteredNotes = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectTab(tab: NotesTab) {
        _uiState.update { state ->
            val sourceList = when (tab) {
                NotesTab.JOURNAL -> state.notes
                NotesTab.ARCHIVE -> state.archivedNotes
                NotesTab.TRASH -> state.trashNotes
            }
            val filtered = applyFilter(sourceList, state.selectedMood, state.searchQuery)
            state.copy(selectedTab = tab, filteredNotes = filtered)
        }
    }

    fun selectMoodFilter(mood: String?) {
        _uiState.update { state ->
            val newMood = if (state.selectedMood == mood) null else mood
            val sourceList = when (state.selectedTab) {
                NotesTab.JOURNAL -> state.notes
                NotesTab.ARCHIVE -> state.archivedNotes
                NotesTab.TRASH -> state.trashNotes
            }
            val filtered = applyFilter(sourceList, newMood, state.searchQuery)
            state.copy(selectedMood = newMood, filteredNotes = filtered)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val sourceList = when (state.selectedTab) {
                NotesTab.JOURNAL -> state.notes
                NotesTab.ARCHIVE -> state.archivedNotes
                NotesTab.TRASH -> state.trashNotes
            }
            val filtered = applyFilter(sourceList, state.selectedMood, query)
            state.copy(searchQuery = query, filteredNotes = filtered)
        }
    }

    fun archiveNote(noteId: Long) {
        viewModelScope.launch {
            repository.archiveNote(noteId)
        }
    }

    fun unarchiveNote(noteId: Long) {
        viewModelScope.launch {
            repository.unarchiveNote(noteId)
        }
    }

    fun moveToTrash(noteId: Long) {
        viewModelScope.launch {
            repository.moveToTrash(noteId)
        }
    }

    fun restoreFromTrash(noteId: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(noteId)
        }
    }

    fun deletePermanently(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNotePermanently(noteId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun deleteNote(noteId: Long) {
        moveToTrash(noteId)
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

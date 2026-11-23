package com.mindlink.journal.ui.notes

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.di.ServiceLocator
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
    val isLoading: Boolean = true,
    val showOnboarding: Boolean = false,
    val availableUpdate: com.mindlink.journal.data.ota.AppReleaseInfo? = null,
    val isOtaDismissed: Boolean = false,
    val selectedNoteIds: Set<Long> = emptySet()
) {
    val isSelectionMode: Boolean get() = selectedNoteIds.isNotEmpty()
}

class NotesViewModel(
    private val repository: NoteRepository,
    private val preferences: SharedPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        val hasSeen = preferences?.getBoolean(ServiceLocator.PREF_HAS_SEEN_ONBOARDING, false) ?: false
        if (!hasSeen) {
            _uiState.update { it.copy(showOnboarding = true) }
        }

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

    fun toggleSelectNote(noteId: Long) {
        _uiState.update { state ->
            val updated = if (state.selectedNoteIds.contains(noteId)) {
                state.selectedNoteIds - noteId
            } else {
                state.selectedNoteIds + noteId
            }
            state.copy(selectedNoteIds = updated)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.filteredNotes.map { it.id }.toSet()
            state.copy(selectedNoteIds = allIds)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedNoteIds = emptySet()) }
    }

    fun deleteSelectedNotes() {
        val ids = _uiState.value.selectedNoteIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            if (_uiState.value.selectedTab == NotesTab.TRASH) {
                repository.deleteMultiplePermanently(ids)
            } else {
                repository.moveMultipleToTrash(ids)
            }
            clearSelection()
        }
    }

    fun togglePinNote(noteId: Long) {
        viewModelScope.launch {
            val note = repository.getNoteByIdSync(noteId) ?: return@launch
            repository.updatePinnedStatus(noteId, !note.isPinned)
        }
    }

    fun togglePinSelectedNotes() {
        val ids = _uiState.value.selectedNoteIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val notes = ids.mapNotNull { repository.getNoteByIdSync(it) }
            val shouldPin = notes.any { !it.isPinned }
            ids.forEach { repository.updatePinnedStatus(it, shouldPin) }
            clearSelection()
        }
    }

    fun dismissOnboarding() {
        preferences?.edit()?.putBoolean(ServiceLocator.PREF_HAS_SEEN_ONBOARDING, true)?.apply()
        _uiState.update { it.copy(showOnboarding = false) }
    }

    fun loadSampleThoughtsAndDismiss() {
        preferences?.edit()?.putBoolean(ServiceLocator.PREF_HAS_SEEN_ONBOARDING, true)?.apply()
        _uiState.update { it.copy(showOnboarding = false) }
        seedSampleData()
    }

    fun showOnboarding() {
        _uiState.update { it.copy(showOnboarding = true) }
    }

    fun checkForOtaUpdates(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val otaManager = com.mindlink.journal.data.ota.OtaUpdateManager(context)
                val result = otaManager.checkForUpdates()
                result.onSuccess { info ->
                    if (info != null && info.isNewer) {
                        _uiState.update { it.copy(availableUpdate = info) }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun dismissOtaUpdate() {
        _uiState.update { it.copy(isOtaDismissed = true) }
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

    class Factory(
        private val repository: NoteRepository,
        private val preferences: SharedPreferences? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesViewModel(repository, preferences) as T
        }
    }
}

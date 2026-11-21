package com.mindlink.journal.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.local.entity.NoteVersionEntity
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.domain.model.Note
import com.mindlink.journal.domain.model.RelatedThought
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class NoteDetailUiState(
    val note: Note? = null,
    val relatedThoughts: List<RelatedThought> = emptyList(),
    val completedActionIndices: Set<Int> = emptySet(),
    val isLoadingRelated: Boolean = true,
    val isDeleted: Boolean = false,
    val isTranscribing: Boolean = false,
    val transcribeError: String? = null
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
        val currentNote = _uiState.value.note ?: return
        if (index !in currentNote.actionItems.indices) return

        val currentRaw = currentNote.actionItems[index]
        val isCurrentlyChecked = currentRaw.startsWith("[x] ", ignoreCase = true)
        val textOnly = when {
            currentRaw.startsWith("[x] ", ignoreCase = true) -> currentRaw.substring(4)
            currentRaw.startsWith("[ ] ") -> currentRaw.substring(4)
            else -> currentRaw
        }
        val updatedRaw = if (isCurrentlyChecked) "[ ] $textOnly" else "[x] $textOnly"
        val newActionItems = currentNote.actionItems.toMutableList().apply {
            set(index, updatedRaw)
        }
        val updatedNote = currentNote.copy(
            actionItems = newActionItems,
            updatedAt = System.currentTimeMillis()
        )

        _uiState.update { state ->
            val set = state.completedActionIndices.toMutableSet()
            if (isCurrentlyChecked) set.remove(index) else set.add(index)
            state.copy(note = updatedNote, completedActionIndices = set)
        }

        viewModelScope.launch {
            repository.saveNote(updatedNote)
        }
    }

    fun appendFollowUp(updateText: String, onDone: () -> Unit = {}) {
        val currentNote = _uiState.value.note ?: return
        if (updateText.isBlank()) return
        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val timestamp = sdf.format(java.util.Date())
        val newContent = if (currentNote.content.isBlank()) {
            "**Update ($timestamp):**\n$updateText"
        } else {
            "${currentNote.content}\n\n**Update ($timestamp):**\n$updateText"
        }
        val updatedNote = currentNote.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveNote(updatedNote)
            loadRelatedThoughts(noteId)
            onDone()
        }
    }

    fun submitPromptReflection(promptQuestion: String, userAnswer: String, onDone: () -> Unit = {}) {
        val currentNote = _uiState.value.note ?: return
        if (userAnswer.isBlank()) return
        val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
        val timestamp = sdf.format(java.util.Date())
        val formattedReflection = """
            
            > **Reflection Question:** $promptQuestion
            > **Insight ($timestamp):** $userAnswer
        """.trimIndent()

        val newContent = if (currentNote.content.isBlank()) {
            formattedReflection
        } else {
            "${currentNote.content}\n\n$formattedReflection"
        }

        val updatedNote = currentNote.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRelated = true, note = updatedNote) }
            repository.saveNote(updatedNote)
            loadRelatedThoughts(noteId)
            onDone()
        }
    }

    fun reprocessNote(onDone: () -> Unit = {}) {
        val currentNote = _uiState.value.note ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRelated = true) }
            repository.saveNote(currentNote)
            loadRelatedThoughts(noteId)
            onDone()
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

    fun archiveNote(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.archiveNote(noteId)
            onSuccess()
        }
    }

    fun unarchiveNote(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.unarchiveNote(noteId)
            onSuccess()
        }
    }

    fun moveToTrash(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.moveToTrash(noteId)
            _uiState.update { it.copy(isDeleted = true) }
            onSuccess()
        }
    }

    fun restoreFromTrash(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.restoreFromTrash(noteId)
            onSuccess()
        }
    }

    fun updateFontSizeScale(scale: Float?) {
        val current = _uiState.value.note ?: return
        val updated = current.copy(fontSizeScale = scale, updatedAt = System.currentTimeMillis())
        _uiState.update { it.copy(note = updated) }
        viewModelScope.launch {
            repository.saveNote(updated)
        }
    }

    fun deletePermanently(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNotePermanently(noteId)
            _uiState.update { it.copy(isDeleted = true) }
            onDeleted()
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        moveToTrash(onDeleted)
    }

    val noteVersions: StateFlow<List<NoteVersionEntity>> = repository.getNoteVersions(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreVersion(version: NoteVersionEntity) {
        viewModelScope.launch {
            repository.restoreNoteVersion(version.versionId)?.let { restoredNote ->
                _uiState.update { it.copy(note = restoredNote) }
            }
        }
    }

    fun transcribeAudio() {
        val currentNote = _uiState.value.note ?: return
        val audioUriStr = currentNote.audioUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isTranscribing = true, transcribeError = null) }
            try {
                val file = if (audioUriStr.startsWith("file://")) {
                    File(java.net.URI(audioUriStr).path)
                } else {
                    File(audioUriStr)
                }
                val result = repository.transcribeAudioFile(file)
                result.onSuccess { transcript ->
                    val updatedContent = if (currentNote.content.isBlank()) {
                        transcript
                    } else if (!currentNote.content.contains(transcript)) {
                        "${currentNote.content}\n\n$transcript"
                    } else {
                        currentNote.content
                    }
                    val updatedNote = currentNote.copy(
                        content = updatedContent,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.saveNote(updatedNote)
                    _uiState.update { it.copy(note = updatedNote, isTranscribing = false) }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isTranscribing = false,
                            transcribeError = error.message ?: "Transcription failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTranscribing = false,
                        transcribeError = e.message ?: "Audio file access error"
                    )
                }
            }
        }
    }

    fun clearTranscribeError() {
        _uiState.update { it.copy(transcribeError = null) }
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

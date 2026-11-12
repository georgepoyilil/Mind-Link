package com.mindlink.journal.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val isSaved: Boolean = false,
    val isVoiceRecording: Boolean = false,
    val speechPartialText: String = "",
    val speechError: String? = null,
    val imageUri: String? = null,
    val imageCaption: String? = null,
    val audioUri: String? = null,
    val audioDurationMs: Long = 0L,
    val suggestedTags: List<String> = emptyList()
)

class NoteEditorViewModel(
    private val repository: NoteRepository,
    private val initialNoteId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        if (initialNoteId != null && initialNoteId > 0) {
            loadNote(initialNoteId)
        }

        viewModelScope.launch {
            repository.getAllNotes().collect { allNotes ->
                val existing = allNotes.flatMap { it.tags }
                    .filter { it.isNotBlank() }
                    .distinct()
                val popular = listOf("Ideas", "Work", "Reflective", "Personal", "Health", "Burnout", "Goals")
                val combined = (existing + popular).distinct()
                _uiState.update { it.copy(suggestedTags = combined) }
            }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            repository.getNoteByIdSync(id)?.let { note ->
                _uiState.update {
                    it.copy(
                        id = note.id,
                        title = note.title,
                        content = note.content,
                        tags = note.tags,
                        imageUri = note.imageUri,
                        imageCaption = note.imageCaption,
                        audioUri = note.audioUri,
                        audioDurationMs = note.audioDurationMs
                    )
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    fun updateTagInput(input: String) {
        _uiState.update { it.copy(tagInput = input) }
    }

    fun addTag() {
        val tag = _uiState.value.tagInput.trim().removePrefix("#")
        if (tag.isNotBlank() && !_uiState.value.tags.contains(tag)) {
            _uiState.update {
                it.copy(
                    tags = it.tags + tag,
                    tagInput = ""
                )
            }
        }
    }

    fun selectSuggestedTag(tag: String) {
        val clean = tag.trim().removePrefix("#")
        if (clean.isNotBlank() && !_uiState.value.tags.contains(clean)) {
            _uiState.update { it.copy(tags = it.tags + clean) }
        }
    }

    fun removeTag(tag: String) {
        _uiState.update {
            it.copy(tags = it.tags.filter { t -> t != tag })
        }
    }

    fun setImage(uri: String?, caption: String? = null) {
        _uiState.update { it.copy(imageUri = uri, imageCaption = caption ?: it.imageCaption) }
    }

    fun updateImageCaption(caption: String) {
        _uiState.update { it.copy(imageCaption = caption) }
    }

    fun removeImage() {
        _uiState.update { it.copy(imageUri = null, imageCaption = null) }
    }

    fun setAudio(uri: String?, durationMs: Long) {
        _uiState.update { it.copy(audioUri = uri, audioDurationMs = durationMs) }
    }

    fun removeAudio() {
        _uiState.update { it.copy(audioUri = null, audioDurationMs = 0L) }
    }

    fun appendVoiceText(text: String) {
        if (text.isBlank()) return
        _uiState.update { state ->
            val updated = if (state.content.isBlank()) text else "${state.content} $text"
            state.copy(content = updated, speechPartialText = "")
        }
    }

    fun updateSpeechPartial(partial: String) {
        _uiState.update { it.copy(speechPartialText = partial) }
    }

    fun setRecordingState(isRecording: Boolean) {
        _uiState.update { it.copy(isVoiceRecording = isRecording) }
    }

    fun setSpeechError(error: String?) {
        _uiState.update { it.copy(speechError = error) }
    }

    fun insertMarkdown(prefix: String, suffix: String = "") {
        _uiState.update { state ->
            val updated = if (state.content.endsWith("\n") || state.content.isEmpty()) {
                "${state.content}$prefix$suffix"
            } else {
                "${state.content}\n$prefix$suffix"
            }
            state.copy(content = updated)
        }
    }

    fun saveNote(onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val titleToSave = state.title.ifBlank {
                state.content.lineSequence().firstOrNull()?.take(40) ?: "Untitled Entry"
            }

            val note = Note(
                id = state.id,
                title = titleToSave,
                content = state.content,
                tags = state.tags,
                imageUri = state.imageUri,
                imageCaption = state.imageCaption,
                audioUri = state.audioUri,
                audioDurationMs = state.audioDurationMs
            )

            val savedId = repository.saveNote(note)
            _uiState.update { it.copy(id = savedId, isSaved = true) }
            onSaved(savedId)
        }
    }

    class Factory(
        private val repository: NoteRepository,
        private val noteId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteEditorViewModel(repository, noteId) as T
        }
    }
}

package com.mindlink.journal.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.local.entity.NoteVersionEntity
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
)

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
    val isTranscribingAudio: Boolean = false,
    val transcriptionError: String? = null,
    val imageUri: String? = null,
    val imageCaption: String? = null,
    val audioUri: String? = null,
    val audioDurationMs: Long = 0L,
    val suggestedTags: List<String> = emptyList(),
    val checklistItems: List<ChecklistItem> = emptyList(),
    val fontFamily: String = "Sans",
    val fontSizeScale: Float? = null,
    val recordedAudioTranscript: String = "",
    val originalCreatedAt: Long? = null,
    val summary: String? = null,
    val mood: String? = null,
    val reflectionPrompts: List<String> = emptyList(),
    val isProcessed: Boolean = false,
    val noteVersions: List<NoteVersionEntity> = emptyList()
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
                // Parse checklist items from actionItems
                val parsedItems = note.actionItems.map { raw ->
                    when {
                        raw.startsWith("[x] ", ignoreCase = true) ->
                            ChecklistItem(text = raw.substring(4), isChecked = true)
                        raw.startsWith("[ ] ") ->
                            ChecklistItem(text = raw.substring(4), isChecked = false)
                        else ->
                            ChecklistItem(text = raw, isChecked = false)
                    }
                }.toMutableList()

                // If actionItems was empty, extract any legacy markdown "- [ ]" from content
                var cleanedContent = note.content
                if (parsedItems.isEmpty() && (cleanedContent.contains("- [ ]") || cleanedContent.contains("- [x]"))) {
                    val remainingLines = mutableListOf<String>()
                    cleanedContent.lines().forEach { line ->
                        val trimmed = line.trim()
                        when {
                            trimmed.startsWith("- [x] ", ignoreCase = true) ->
                                parsedItems.add(ChecklistItem(text = trimmed.substring(6), isChecked = true))
                            trimmed.startsWith("- [ ] ") ->
                                parsedItems.add(ChecklistItem(text = trimmed.substring(6), isChecked = false))
                            else -> remainingLines.add(line)
                        }
                    }
                    cleanedContent = remainingLines.joinToString("\n").trim()
                }

                _uiState.update {
                    it.copy(
                        id = note.id,
                        title = note.title,
                        content = cleanedContent,
                        tags = note.tags,
                        imageUri = note.imageUri,
                        imageCaption = note.imageCaption,
                        audioUri = note.audioUri,
                        audioDurationMs = note.audioDurationMs,
                        checklistItems = parsedItems,
                        fontFamily = note.fontFamily,
                        fontSizeScale = note.fontSizeScale,
                        originalCreatedAt = note.createdAt,
                        summary = note.summary,
                        mood = note.mood,
                        reflectionPrompts = note.reflectionPrompts,
                        isProcessed = note.isProcessed
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getNoteVersions(id).collect { versions ->
                _uiState.update { it.copy(noteVersions = versions) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    fun setFontFamily(font: String) {
        _uiState.update { it.copy(fontFamily = font) }
    }

    fun setFontSizeScale(scale: Float?) {
        _uiState.update { it.copy(fontSizeScale = scale) }
    }

    // --- Interactive Checklist Item Management ---

    fun addChecklistItem(text: String = ""): String {
        val newItem = ChecklistItem(text = text, isChecked = false)
        _uiState.update { it.copy(checklistItems = it.checklistItems + newItem) }
        return newItem.id
    }

    fun addChecklistItems(texts: List<String>) {
        val newItems = texts.filter { it.isNotBlank() }.map { ChecklistItem(text = it.trim(), isChecked = false) }
        if (newItems.isNotEmpty()) {
            _uiState.update { it.copy(checklistItems = it.checklistItems + newItems) }
        }
    }

    fun updateChecklistItem(id: String, newText: String) {
        _uiState.update { state ->
            state.copy(
                checklistItems = state.checklistItems.map {
                    if (it.id == id) it.copy(text = newText) else it
                }
            )
        }
    }

    fun toggleChecklistItem(id: String) {
        _uiState.update { state ->
            state.copy(
                checklistItems = state.checklistItems.map {
                    if (it.id == id) it.copy(isChecked = !it.isChecked) else it
                }
            )
        }
    }

    fun removeChecklistItem(id: String) {
        _uiState.update { state ->
            state.copy(
                checklistItems = state.checklistItems.filter { it.id != id }
            )
        }
    }

    // --- Tag Management ---

    fun updateTagInput(input: String) {
        _uiState.update { it.copy(tagInput = input) }
    }

    fun addTag() {
        val raw = _uiState.value.tagInput.trim()
        if (raw.isNotBlank()) {
            val tokens = raw.split(',', ' ', ';', '\n')
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotBlank() }
            if (tokens.isNotEmpty()) {
                _uiState.update { state ->
                    val updated = (state.tags + tokens).distinct()
                    state.copy(
                        tags = updated,
                        tagInput = ""
                    )
                }
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

    // --- Media Management ---

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
        _uiState.update { it.copy(audioUri = null, audioDurationMs = 0L, recordedAudioTranscript = "") }
    }

    fun appendVoiceText(text: String) {
        if (text.isBlank()) return
        _uiState.update { state ->
            val updated = if (state.content.isBlank()) text else "${state.content} $text"
            val updatedTranscript = if (state.recordedAudioTranscript.isBlank()) text else "${state.recordedAudioTranscript} $text"
            state.copy(
                content = updated,
                speechPartialText = "",
                recordedAudioTranscript = updatedTranscript
            )
        }
    }

    fun transcribeAudio() {
        val audioUriStr = _uiState.value.audioUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isTranscribingAudio = true, transcriptionError = null) }
            try {
                val file = if (audioUriStr.startsWith("file://")) {
                    File(java.net.URI(audioUriStr).path)
                } else {
                    File(audioUriStr)
                }
                val result = repository.transcribeAudioFile(file)
                result.onSuccess { transcript ->
                    _uiState.update { state ->
                        val newContent = if (state.content.isBlank()) {
                            transcript
                        } else if (!state.content.contains(transcript)) {
                            "${state.content}\n\n$transcript"
                        } else {
                            state.content
                        }
                        state.copy(
                            content = newContent,
                            recordedAudioTranscript = transcript,
                            isTranscribingAudio = false
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isTranscribingAudio = false,
                            transcriptionError = error.message ?: "Transcription failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTranscribingAudio = false,
                        transcriptionError = e.message ?: "Could not open audio recording"
                    )
                }
            }
        }
    }

    fun clearTranscriptionError() {
        _uiState.update { it.copy(transcriptionError = null) }
    }

    fun restoreVersion(version: NoteVersionEntity) {
        _uiState.update {
            it.copy(
                title = version.title,
                content = version.content
            )
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

    fun saveNote(onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val titleToSave = state.title.ifBlank {
                state.content.lineSequence().firstOrNull()?.take(40) ?: "Untitled Entry"
            }

            // Convert interactive checklist items to action items
            val actionItemsToSave = state.checklistItems
                .filter { it.text.isNotBlank() }
                .map { item ->
                    if (item.isChecked) "[x] ${item.text.trim()}" else "[ ] ${item.text.trim()}"
                }

            val note = Note(
                id = state.id,
                title = titleToSave,
                content = state.content,
                createdAt = state.originalCreatedAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                summary = state.summary,
                mood = state.mood,
                tags = state.tags,
                actionItems = actionItemsToSave,
                reflectionPrompts = state.reflectionPrompts,
                isProcessed = state.isProcessed,
                imageUri = state.imageUri,
                imageCaption = state.imageCaption,
                audioUri = state.audioUri,
                audioDurationMs = state.audioDurationMs,
                fontFamily = state.fontFamily,
                fontSizeScale = state.fontSizeScale
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

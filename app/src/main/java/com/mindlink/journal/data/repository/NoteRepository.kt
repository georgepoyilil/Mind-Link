package com.mindlink.journal.data.repository

import com.mindlink.journal.domain.model.Note
import com.mindlink.journal.domain.model.RelatedThought
import com.mindlink.journal.domain.model.SearchResult
import com.mindlink.journal.domain.model.SynthesisResponse
import com.mindlink.journal.domain.model.ThoughtGraph
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getArchivedNotes(): Flow<List<Note>>
    fun getTrashNotes(): Flow<List<Note>>
    fun getNotesByMood(mood: String): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    suspend fun getNoteByIdSync(id: Long): Note?
    suspend fun saveNote(note: Note): Long
    suspend fun deleteNote(id: Long)
    suspend fun moveToTrash(id: Long)
    suspend fun restoreFromTrash(id: Long)
    suspend fun archiveNote(id: Long)
    suspend fun unarchiveNote(id: Long)
    suspend fun deleteNotePermanently(id: Long)
    suspend fun emptyTrash()
    suspend fun purgeExpiredTrash()
    suspend fun getRelatedThoughts(noteId: Long, topK: Int = 3): List<RelatedThought>
    suspend fun searchNotesHybrid(query: String): List<SearchResult>
    suspend fun synthesizeSecondBrain(query: String, precomputedSearchResults: List<SearchResult>? = null): SynthesisResponse
    suspend fun reindexAllNotes()
    fun getNotesCount(): Flow<Int>
    fun getArchivedCount(): Flow<Int>
    fun getTrashCount(): Flow<Int>
    fun getEmbeddingsCount(): Flow<Int>
    fun getThoughtGraph(): Flow<ThoughtGraph>
    suspend fun rebuildSynapticGraph()
    fun getSynapseCount(): Flow<Int>
}

package com.mindlink.journal.data.local.dao

import androidx.room.*
import com.mindlink.journal.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteByIdSync(id: Long): NoteEntity?

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE mood = :mood ORDER BY createdAt DESC")
    fun getNotesByMood(mood: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id IN (:ids)")
    suspend fun getNotesByIds(ids: List<Long>): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isProcessed = 0 ORDER BY createdAt ASC")
    suspend fun getUnprocessedNotes(): List<NoteEntity>

    @Query("""
        UPDATE notes 
        SET summary = :summary,
            mood = :mood,
            tags = :tags,
            actionItems = :actionItems,
            reflectionPrompts = :reflectionPrompts,
            isProcessed = :isProcessed,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateProcessingMetadata(
        id: Long,
        summary: String?,
        mood: String?,
        tags: List<String>,
        actionItems: List<String>,
        reflectionPrompts: List<String>,
        isProcessed: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM notes")
    fun getNotesCount(): Flow<Int>
}

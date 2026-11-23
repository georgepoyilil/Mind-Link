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

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 1 ORDER BY isPinned DESC, updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE mood = :mood AND isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun getNotesByMood(mood: String): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinnedStatus(id: Long, isPinned: Boolean)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun moveMultipleToTrash(ids: List<Long>, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteNotesByIds(ids: List<Long>)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("UPDATE notes SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveNote(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isArchived = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun unarchiveNote(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt <= :cutoffTimestamp")
    suspend fun purgeExpiredTrash(cutoffTimestamp: Long)

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

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 0 AND isArchived = 0")
    fun getNotesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 0 AND isArchived = 1")
    fun getArchivedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 1")
    fun getTrashCount(): Flow<Int>

    @Query("UPDATE notes SET isLocked = :isLocked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLockStatus(id: Long, isLocked: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAllNotesListSync(): List<NoteEntity>
}

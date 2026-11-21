package com.mindlink.journal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mindlink.journal.data.local.entity.NoteVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteVersionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: NoteVersionEntity): Long

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY timestamp DESC")
    fun getVersionsForNote(noteId: Long): Flow<List<NoteVersionEntity>>

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY timestamp DESC")
    suspend fun getVersionsForNoteSync(noteId: Long): List<NoteVersionEntity>

    @Query("SELECT * FROM note_versions WHERE versionId = :versionId")
    suspend fun getVersionById(versionId: Long): NoteVersionEntity?

    @Query("SELECT COUNT(*) FROM note_versions WHERE noteId = :noteId")
    suspend fun getVersionCount(noteId: Long): Int

    @Query("DELETE FROM note_versions WHERE noteId = :noteId")
    suspend fun deleteVersionsForNote(noteId: Long)
}

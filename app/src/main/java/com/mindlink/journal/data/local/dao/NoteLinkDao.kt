package com.mindlink.journal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mindlink.journal.data.local.entity.NoteLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteLinkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: NoteLinkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<NoteLinkEntity>)

    @Query("SELECT * FROM note_links")
    fun getAllLinks(): Flow<List<NoteLinkEntity>>

    @Query("SELECT * FROM note_links")
    suspend fun getAllLinksSync(): List<NoteLinkEntity>

    @Query("SELECT * FROM note_links WHERE sourceNoteId = :noteId OR targetNoteId = :noteId")
    fun getLinksForNote(noteId: Long): Flow<List<NoteLinkEntity>>

    @Query("DELETE FROM note_links WHERE sourceNoteId = :noteId OR targetNoteId = :noteId")
    suspend fun deleteLinksForNote(noteId: Long)

    @Query("DELETE FROM note_links")
    suspend fun deleteAllLinks()

    @Query("SELECT COUNT(*) FROM note_links")
    fun getLinkCount(): Flow<Int>
}

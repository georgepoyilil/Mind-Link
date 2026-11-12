package com.mindlink.journal.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.mindlink.journal.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteFtsDao {

    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :query
        ORDER BY notes.updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes_fts MATCH :query
        ORDER BY notes.updatedAt DESC
    """)
    suspend fun searchNotesSync(query: String): List<NoteEntity>
}

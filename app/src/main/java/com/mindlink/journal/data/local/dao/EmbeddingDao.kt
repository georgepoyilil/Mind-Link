package com.mindlink.journal.data.local.dao

import androidx.room.*
import com.mindlink.journal.data.local.entity.NoteEmbeddingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmbeddingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(embedding: NoteEmbeddingEntity)

    @Query("SELECT * FROM note_embeddings WHERE noteId = :noteId")
    suspend fun getEmbeddingByNoteId(noteId: Long): NoteEmbeddingEntity?

    @Query("SELECT * FROM note_embeddings")
    suspend fun getAllEmbeddings(): List<NoteEmbeddingEntity>

    @Query("DELETE FROM note_embeddings WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Long)

    @Query("DELETE FROM note_embeddings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM note_embeddings")
    fun getEmbeddingsCount(): Flow<Int>
}

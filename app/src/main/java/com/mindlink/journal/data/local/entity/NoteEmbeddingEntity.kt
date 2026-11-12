package com.mindlink.journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores on-device 384-dimensional dense vector embeddings for semantic similarity & associative memory.
 */
@Entity(tableName = "note_embeddings")
data class NoteEmbeddingEntity(
    @PrimaryKey
    val noteId: Long,
    val vector: FloatArray,
    val dimension: Int = 384,
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NoteEmbeddingEntity
        if (noteId != other.noteId) return false
        if (!vector.contentEquals(other.vector)) return false
        if (dimension != other.dimension) return false
        return true
    }

    override fun hashCode(): Int {
        var result = noteId.hashCode()
        result = 31 * result + vector.contentHashCode()
        result = 31 * result + dimension
        return result
    }
}

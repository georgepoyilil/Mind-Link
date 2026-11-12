package com.mindlink.journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Core Note entity representing a journal entry with AI-extracted metadata,
 * image attachments with captions, and recorded voice audio.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val summary: String? = null,
    val mood: String? = null,
    val tags: List<String> = emptyList(),
    val actionItems: List<String> = emptyList(),
    val reflectionPrompts: List<String> = emptyList(),
    val isProcessed: Boolean = false,
    val imageUri: String? = null,
    val imageCaption: String? = null,
    val audioUri: String? = null,
    val audioDurationMs: Long = 0L
)

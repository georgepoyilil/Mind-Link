package com.mindlink.journal.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Core Note entity representing a journal entry with AI-extracted metadata,
 * image attachments with captions, recorded voice audio, archiving, and trash lifecycle.
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
    val audioDurationMs: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val isArchived: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false,
    @ColumnInfo(defaultValue = "NULL")
    val deletedAt: Long? = null,
    @ColumnInfo(defaultValue = "'Sans'")
    val fontFamily: String = "Sans",
    @ColumnInfo(defaultValue = "NULL")
    val fontSizeScale: Float? = null,
    @ColumnInfo(defaultValue = "0")
    val isLocked: Boolean = false
)

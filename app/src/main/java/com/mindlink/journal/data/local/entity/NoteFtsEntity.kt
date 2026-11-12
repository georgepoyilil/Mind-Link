package com.mindlink.journal.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-Text Search (FTS4) shadow table for fast keyword searching across notes.
 */
@Entity(tableName = "notes_fts")
@Fts4(contentEntity = NoteEntity::class)
data class NoteFtsEntity(
    val title: String,
    val content: String
)

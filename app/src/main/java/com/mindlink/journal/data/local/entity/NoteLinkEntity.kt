package com.mindlink.journal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Synaptic Graph Link Entity:
 * Represents an edge connecting two thoughts in the knowledge graph.
 * Supports explicit wikilinks ([[Note Title]]), shared tags, and semantic similarity.
 */
@Entity(
    tableName = "note_links",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceNoteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetNoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceNoteId"]),
        Index(value = ["targetNoteId"]),
        Index(value = ["sourceNoteId", "targetNoteId", "linkType"], unique = true)
    ]
)
data class NoteLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceNoteId: Long,
    val targetNoteId: Long,
    val linkType: String, // "WIKILINK", "TAG_CLUSTER", "SEMANTIC_SIMILARITY", "EMOTION_FLOW"
    val weight: Float = 1.0f,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

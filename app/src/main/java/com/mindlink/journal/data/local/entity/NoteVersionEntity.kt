package com.mindlink.journal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "note_versions",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class NoteVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val versionId: Long = 0,
    val noteId: Long,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(timestamp))

    val compactDate: String
        get() = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))

    val wordCount: Int
        get() = content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
}

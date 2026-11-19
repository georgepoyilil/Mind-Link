package com.mindlink.journal.domain.model

import com.mindlink.journal.data.local.entity.NoteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Note(
    val id: Long = 0,
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
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val fontFamily: String = "Sans",
    val fontSizeScale: Float? = null
) {
    val formattedDate: String
        get() = dateCache.get()?.format(Date(createdAt)) ?: ""

    val compactDate: String
        get() = compactDateCache.get()?.format(Date(createdAt)) ?: ""

    val moodEmoji: String
        get() = when (mood?.lowercase()?.trim()) {
            "reflective" -> "🪞"
            "analytical" -> "🔬"
            "creative" -> "💡"
            "energized" -> "⚡"
            "stressed" -> "🌧️"
            "calm" -> "🍃"
            else -> if (!mood.isNullOrBlank()) "✨" else ""
        }

    val daysUntilExpiry: Int
        get() {
            val del = deletedAt ?: return 30
            val elapsedMs = System.currentTimeMillis() - del
            val elapsedDays = (elapsedMs / (1000L * 60 * 60 * 24)).toInt()
            return (30 - elapsedDays).coerceAtLeast(0)
        }

    val previewText: String
        get() = summary?.takeIf { it.isNotBlank() } ?: content.take(160).replace("\n", " ")

    val formattedAudioDuration: String
        get() {
            if (audioDurationMs <= 0) return "0:00"
            val totalSeconds = audioDurationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%d:%02d", minutes, seconds)
        }

    fun toEntity(): NoteEntity {
        return NoteEntity(
            id = id,
            title = title,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
            summary = summary,
            mood = mood,
            tags = tags,
            actionItems = actionItems,
            reflectionPrompts = reflectionPrompts,
            isProcessed = isProcessed,
            imageUri = imageUri,
            imageCaption = imageCaption,
            audioUri = audioUri,
            audioDurationMs = audioDurationMs,
            isArchived = isArchived,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            fontFamily = fontFamily,
            fontSizeScale = fontSizeScale
        )
    }

    companion object {
        private val dateCache = ThreadLocal.withInitial {
            SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        }

        private val compactDateCache = ThreadLocal.withInitial {
            SimpleDateFormat("MMM d", Locale.getDefault())
        }

        fun fromEntity(entity: NoteEntity): Note {
            return Note(
                id = entity.id,
                title = entity.title,
                content = entity.content,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                summary = entity.summary,
                mood = entity.mood,
                tags = entity.tags,
                actionItems = entity.actionItems,
                reflectionPrompts = entity.reflectionPrompts,
                isProcessed = entity.isProcessed,
                imageUri = entity.imageUri,
                imageCaption = entity.imageCaption,
                audioUri = entity.audioUri,
                audioDurationMs = entity.audioDurationMs,
                isArchived = entity.isArchived,
                isDeleted = entity.isDeleted,
                deletedAt = entity.deletedAt,
                fontFamily = entity.fontFamily,
                fontSizeScale = entity.fontSizeScale
            )
        }
    }
}

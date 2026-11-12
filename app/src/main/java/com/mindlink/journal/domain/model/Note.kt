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
    val audioDurationMs: Long = 0L
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
            return sdf.format(Date(createdAt))
        }

    val moodEmoji: String
        get() = when (mood?.lowercase(Locale.ROOT)?.trim()) {
            "reflective" -> "🧘"
            "analytical" -> "🧠"
            "creative" -> "💡"
            "energized" -> "⚡"
            "stressed" -> "🌧️"
            "calm" -> "🌿"
            "focused" -> "🎯"
            "vulnerable" -> "💔"
            "grateful" -> "🌸"
            else -> "✨"
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
            audioDurationMs = audioDurationMs
        )
    }

    companion object {
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
                audioDurationMs = entity.audioDurationMs
            )
        }
    }
}

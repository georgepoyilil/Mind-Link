package com.mindlink.journal.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindlink.journal.data.local.entity.NoteEmbeddingEntity
import com.mindlink.journal.di.ServiceLocator

class NoteProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        if (noteId == -1L) {
            Log.e(TAG, "Invalid noteId supplied to NoteProcessingWorker")
            return Result.failure()
        }

        val container = ServiceLocator.getInstance(applicationContext)
        val noteDao = container.database.noteDao()
        val embeddingDao = container.database.embeddingDao()
        val brainstormingEngine = container.brainstormingEngine
        val embeddingEngine = container.embeddingEngine

        val note = noteDao.getNoteByIdSync(noteId) ?: run {
            Log.w(TAG, "Note with ID $noteId not found, aborting worker")
            return Result.failure()
        }

        try {
            Log.d(TAG, "Starting AI processing pipeline for Note #$noteId: '${note.title}'")

            // 1. Metadata Extraction & Reflection Prompts via Brainstorming Engine
            val analysis = brainstormingEngine.analyzeNote(note.title, note.content)

            // Merge user manual tags with auto-extracted tags (deduplicated)
            val combinedTags = (note.tags + analysis.tags).distinct()

            // 2. Embeddings & Vector Indexing (384-d on-device embedding)
            val textToIndex = "${note.title}\n${note.content}"
            val embeddingVector = embeddingEngine.generateEmbedding(textToIndex)

            // 3. Save Vector Embedding to Local Storage
            embeddingDao.insertOrReplace(
                NoteEmbeddingEntity(
                    noteId = noteId,
                    vector = embeddingVector,
                    dimension = embeddingVector.size,
                    updatedAt = System.currentTimeMillis()
                )
            )

            // 4. Update Note Entity in Room Database
            noteDao.updateProcessingMetadata(
                id = noteId,
                summary = analysis.summary,
                mood = analysis.mood,
                tags = combinedTags,
                actionItems = analysis.actionItems,
                reflectionPrompts = analysis.reflectionPrompts,
                isProcessed = true,
                updatedAt = System.currentTimeMillis()
            )

            Log.i(TAG, "Successfully processed Note #$noteId (Mood: ${analysis.mood}, Tags: ${combinedTags.size}, Actions: ${analysis.actionItems.size})")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error in NoteProcessingWorker for note #$noteId: ${e.message}", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_NOTE_ID = "com.mindlink.journal.worker.KEY_NOTE_ID"
        const val WORK_TAG = "note_processing"
        private const val TAG = "NoteProcessingWorker"
    }
}

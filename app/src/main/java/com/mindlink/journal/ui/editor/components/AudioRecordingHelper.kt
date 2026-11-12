package com.mindlink.journal.ui.editor.components

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecordingHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMs: Long = 0L

    fun startRecording(): File? {
        try {
            val audioDir = File(context.filesDir, "audio_notes").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(audioDir, "audio_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            startTimeMs = System.currentTimeMillis()
            Log.d(TAG, "Audio recording started -> ${outputFile.absolutePath}")
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording: ${e.message}", e)
            mediaRecorder?.release()
            mediaRecorder = null
            return null
        }
    }

    fun stopRecording(): Pair<File?, Long> {
        val durationMs = if (startTimeMs > 0) System.currentTimeMillis() - startTimeMs else 0L
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop media recorder safely: ${e.message}")
        } finally {
            mediaRecorder = null
            startTimeMs = 0L
        }
        val file = currentOutputFile
        currentOutputFile = null
        return Pair(file, durationMs)
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaRecorder = null
            currentOutputFile?.delete()
            currentOutputFile = null
            startTimeMs = 0L
        }
    }

    companion object {
        private const val TAG = "AudioRecordingHelper"
    }
}

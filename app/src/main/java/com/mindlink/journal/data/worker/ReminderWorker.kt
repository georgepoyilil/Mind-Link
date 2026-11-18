package com.mindlink.journal.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.mindlink.journal.MainActivity
import com.mindlink.journal.R
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "MindLink Mindful Reminder"
        val message = inputData.getString(KEY_MESSAGE) ?: "Time to check in on your journal thought."
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)

        Log.d(TAG, "Triggering mindful reminder notification for note #$noteId: $title")
        showNotification(title, message, noteId)
        return Result.success()
    }

    private fun showNotification(title: String, message: String, noteId: Long) {
        val channelId = "mindlink_mindful_reminders"
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MindLink Mindful Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Proactive contextual reminders and habit check-ins based on your journal"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NOTE_ID", noteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            (noteId.hashCode() + System.currentTimeMillis().toInt()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify((noteId.toInt() + 1000), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification: ${e.message}", e)
        }
    }

    companion object {
        const val KEY_TITLE = "REMINDER_TITLE"
        const val KEY_MESSAGE = "REMINDER_MESSAGE"
        const val KEY_NOTE_ID = "REMINDER_NOTE_ID"
        private const val TAG = "ReminderWorker"

        fun schedule(
            context: Context,
            title: String,
            message: String,
            noteId: Long,
            delayMinutes: Long = 120
        ): Long {
            val targetTimeMs = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayMinutes)

            val inputData = workDataOf(
                KEY_TITLE to title,
                KEY_MESSAGE to message,
                KEY_NOTE_ID to noteId
            )

            val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMinutes.coerceAtLeast(1), TimeUnit.MINUTES)
                .setInputData(inputData)
                .addTag("mindlink_reminder_$noteId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "reminder_note_$noteId",
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )

            return targetTimeMs
        }
    }
}

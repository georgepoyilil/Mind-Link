package com.mindlink.journal

import android.app.Application
import android.util.Log
import com.mindlink.journal.data.local.SampleDataSeeder
import com.mindlink.journal.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MindLinkApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing MindLink Intelligent Journal & Second Brain")
        val serviceLocator = ServiceLocator.getInstance(this)

        // Pre-populate with sample notes if database is empty on first launch
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notes = serviceLocator.noteRepository.getAllNotes().firstOrNull() ?: emptyList()
                if (notes.isEmpty()) {
                    Log.i(TAG, "Database is empty. Automatically seeding sample Second Brain notes...")
                    SampleDataSeeder.seedSampleNotes(serviceLocator.noteRepository)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking or seeding notes: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "MindLinkApp"
    }
}

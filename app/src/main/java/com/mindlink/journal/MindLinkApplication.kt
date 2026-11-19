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
        ServiceLocator.getInstance(this)
    }

    companion object {
        private const val TAG = "MindLinkApp"
    }
}

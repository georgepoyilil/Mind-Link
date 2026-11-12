package com.mindlink.journal.di

import android.content.Context
import android.content.SharedPreferences
import com.mindlink.journal.data.ai.*
import com.mindlink.journal.data.local.MindLinkDatabase
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.data.repository.NoteRepositoryImpl

class ServiceLocator private constructor(context: Context) {

    val applicationContext: Context = context.applicationContext

    val preferences: SharedPreferences by lazy {
        applicationContext.getSharedPreferences("mindlink_preferences", Context.MODE_PRIVATE)
    }

    val database: MindLinkDatabase by lazy {
        MindLinkDatabase.getInstance(applicationContext)
    }

    val vectorSearchEngine: VectorSearchEngine by lazy {
        VectorSearchEngine()
    }

    val embeddingEngine: EmbeddingEngine by lazy {
        OnnxEmbeddingEngine(applicationContext)
    }

    val brainstormingEngine: BrainstormingEngine by lazy {
        val geminiEngine = GeminiBrainstormingEngine {
            preferences.getString(PREF_GEMINI_API_KEY, null)?.takeIf { it.isNotBlank() }
        }
        CompositeBrainstormingEngine(
            geminiEngine = geminiEngine,
            localEngine = LocalHeuristicBrainstormingEngine(),
            hasApiKey = {
                val key = preferences.getString(PREF_GEMINI_API_KEY, null)
                !key.isNullOrBlank()
            }
        )
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(
            context = applicationContext,
            database = database,
            embeddingEngine = embeddingEngine,
            vectorSearchEngine = vectorSearchEngine,
            brainstormingEngine = brainstormingEngine
        )
    }

    companion object {
        const val PREF_GEMINI_API_KEY = "pref_gemini_api_key"
        const val PREF_ON_DEVICE_ONLY = "pref_on_device_only"

        @Volatile
        private var INSTANCE: ServiceLocator? = null

        fun getInstance(context: Context): ServiceLocator {
            return INSTANCE ?: synchronized(this) {
                val instance = ServiceLocator(context)
                INSTANCE = instance
                instance
            }
        }
    }
}

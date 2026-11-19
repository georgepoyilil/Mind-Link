package com.mindlink.journal.di

import android.content.Context
import android.content.SharedPreferences
import com.mindlink.journal.data.ai.*
import com.mindlink.journal.data.local.MindLinkDatabase
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.data.repository.NoteRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServiceLocator private constructor(context: Context) {

    val applicationContext: Context = context.applicationContext

    val preferences: SharedPreferences by lazy {
        applicationContext.getSharedPreferences("mindlink_preferences", Context.MODE_PRIVATE)
    }

    val securePreferences: SharedPreferences by lazy {
        try {
            val masterKey = androidx.security.crypto.MasterKey.Builder(applicationContext)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()
            androidx.security.crypto.EncryptedSharedPreferences.create(
                applicationContext,
                "mindlink_secure_prefs",
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            android.util.Log.w("ServiceLocator", "EncryptedSharedPreferences fallback: ${e.message}")
            preferences
        }
    }

    fun getApiKey(): String? {
        val secureKey = securePreferences.getString(PREF_GEMINI_API_KEY, null)?.takeIf { it.isNotBlank() }
        if (secureKey != null) return secureKey
        val legacyKey = preferences.getString(PREF_GEMINI_API_KEY, null)?.takeIf { it.isNotBlank() }
        if (legacyKey != null) {
            saveApiKey(legacyKey)
            preferences.edit().remove(PREF_GEMINI_API_KEY).apply()
            return legacyKey
        }
        return null
    }

    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        securePreferences.edit().putString(PREF_GEMINI_API_KEY, trimmed).apply()
        preferences.edit().remove(PREF_GEMINI_API_KEY).apply()
    }

    private val _globalFontScale by lazy {
        MutableStateFlow(preferences.getFloat(PREF_GLOBAL_FONT_SCALE, 1.0f))
    }
    val globalFontScale: StateFlow<Float> by lazy { _globalFontScale.asStateFlow() }

    fun updateGlobalFontScale(scale: Float) {
        preferences.edit().putFloat(PREF_GLOBAL_FONT_SCALE, scale).apply()
        _globalFontScale.value = scale
    }

    fun hasSeenOnboarding(): Boolean {
        return preferences.getBoolean(PREF_HAS_SEEN_ONBOARDING, false)
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        preferences.edit().putBoolean(PREF_HAS_SEEN_ONBOARDING, seen).apply()
    }

    val database: MindLinkDatabase by lazy {
        MindLinkDatabase.getInstance(applicationContext)
    }

    val vectorSearchEngine: VectorSearchEngine by lazy {
        VectorSearchEngine()
    }

    val embeddingEngine: EmbeddingEngine by lazy {
        // FallbackEmbeddingEngine is deterministic, fast, and guarantees valid cosine semantics
        // without token ID collision or unverified ONNX tokenization corruption.
        FallbackEmbeddingEngine()
    }

    val brainstormingEngine: CompositeBrainstormingEngine by lazy {
        val geminiEngine = GeminiBrainstormingEngine {
            getApiKey()
        }
        val mediaPipeEngine = MediaPipeSlmEngine(applicationContext)
        CompositeBrainstormingEngine(
            geminiEngine = geminiEngine,
            mediaPipeEngine = mediaPipeEngine,
            localEngine = LocalHeuristicBrainstormingEngine(),
            hasApiKey = {
                val key = getApiKey()
                !key.isNullOrBlank()
            },
            engineModeProvider = {
                preferences.getString(PREF_ENGINE_MODE, "AUTO") ?: "AUTO"
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
        const val PREF_ENGINE_MODE = "pref_engine_mode" // "AUTO", "SLM", "GEMINI", "LOCAL"
        const val PREF_GLOBAL_FONT_SCALE = "pref_global_font_scale"
        const val PREF_HAS_SEEN_ONBOARDING = "pref_has_seen_onboarding"

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

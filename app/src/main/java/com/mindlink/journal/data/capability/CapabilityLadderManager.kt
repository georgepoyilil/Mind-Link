package com.mindlink.journal.data.capability

import android.content.Context
import android.content.SharedPreferences
import com.mindlink.journal.domain.model.Note
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CapabilityItem(
    val key: String,
    val tier: Int, // Level 0..5
    val title: String,
    val iconSymbol: String,
    val shortDescription: String,
    val whyUse: String,
    val whyNot: String,
    val settingsLocation: String? = null,
    val isCore: Boolean = false
)

data class TierInfo(
    val tier: Int, // Level 0..5
    val title: String,
    val subtitle: String,
    val requiredNotes: Int,
    val badgeIcon: String,
    val accentHex: Long
)

data class ProgressionSuggestion(
    val featureKey: String,
    val featureTitle: String,
    val featureIcon: String,
    val targetLevel: Int,
    val targetLevelTitle: String,
    val headline: String,
    val reason: String,
    val drawback: String,
    val accentHex: Long
)

data class LadderState(
    val currentTier: Int = 0, // Currently active complexity level (0..5)
    val activeNotesCount: Int = 0,
    val isGodMode: Boolean = false,
    val unlockedTiers: Set<Int> = setOf(0),
    val enabledFeatures: Map<String, Boolean> = emptyMap(),
    val justUnlockedTier: TierInfo? = null,
    val activeSuggestion: ProgressionSuggestion? = null
) {
    fun isLevelUnlocked(level: Int): Boolean =
        isGodMode || level <= currentTier || unlockedTiers.contains(level)

    fun isEnabled(key: String): Boolean {
        val item = CapabilityLadderManager.ALL_CAPABILITIES.firstOrNull { it.key == key } ?: return true
        if (item.isCore) return true
        return enabledFeatures[key] ?: false
    }

    fun activeCountForLevel(level: Int): Pair<Int, Int> {
        val items = CapabilityLadderManager.ALL_CAPABILITIES.filter { it.tier == level }
        val active = items.count { isEnabled(it.key) }
        return active to items.size
    }
}

class CapabilityLadderManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mindlink_capability_ladder", Context.MODE_PRIVATE)

    companion object {
        const val PREF_GOD_MODE = "pref_god_mode"
        const val PREF_ACTIVE_LEVEL = "pref_active_level"
        const val PREF_UNLOCKED_TIERS = "pref_unlocked_tiers"
        const val PREF_LAST_NOTIFIED_TIER = "pref_last_notified_tier"
        private const val PREF_MIGRATED_SELECTABLE_V3 = "pref_migrated_selectable_v3"
        private const val PREF_LAST_SUGGESTION_TIME = "pref_last_suggestion_time"
        private const val PREF_LAST_SUGGESTION_NOTES = "pref_last_suggestion_notes"
        private const val PREF_DISMISSED_SUGGESTIONS = "pref_dismissed_suggestions"
        private const val SUGGESTION_COOLDOWN_MS = 12 * 60 * 60 * 1000L // 12 hours

        // Level 0 · Blank Slate (Core: + New Note, Simple Text, Add Follow-Up, Search, Themes)
        const val FEATURE_BASIC_JOURNAL = "feature_basic_journal"
        const val FEATURE_KEYWORD_SEARCH = "feature_keyword_search"
        const val FEATURE_THEMES = "feature_themes"

        // Level 1 · Field Notes (Rich Text Formatting, Checklists, Hashtags, Typography, Feed Lenses)
        const val FEATURE_RICH_TEXT = "feature_rich_text"
        const val FEATURE_CHECKLISTS = "feature_checklists"
        const val FEATURE_TYPOGRAPHY = "feature_typography"
        const val FEATURE_HASHTAGS = "feature_hashtags"
        const val FEATURE_LAYOUT_SWITCH = "feature_layout_switch"

        // Level 2 · Living Canvas (Photos, Voice, OCR, Mood Aura)
        const val FEATURE_MEDIA_CAPTURE = "feature_media_capture"
        const val FEATURE_OCR = "feature_ocr"
        const val FEATURE_VOICE_DICTATION = "feature_voice_dictation"
        const val FEATURE_MOOD_BADGES = "feature_mood_badges"

        // Level 3 · Mirror & Spark (Time Machine Diff, Task Distiller, Jester, Nudges, Lock, Cloud Vault)
        const val FEATURE_VERSION_HISTORY = "feature_version_history"
        const val FEATURE_TASK_SPLITTER = "feature_task_splitter"
        const val FEATURE_JESTER = "feature_jester"
        const val FEATURE_MINDFUL_REMINDERS = "feature_mindful_reminders"
        const val FEATURE_BIOMETRIC_LOCK = "feature_biometric_lock"
        const val FEATURE_CLOUD_BACKUP = "feature_cloud_backup"

        // Level 4 · Mind Atlas (Memory Oracle, Echo Prompts, Constellation)
        const val FEATURE_SECOND_BRAIN = "feature_second_brain"
        const val FEATURE_REFLECTION_PROMPTS = "feature_reflection_prompts"
        const val FEATURE_SYNAPTIC_GRAPH = "feature_synaptic_graph"

        // Level 5 · Neural Forge (Edge Mind AI & Telemetry X-Ray)
        const val FEATURE_ON_DEVICE_SLM = "feature_on_device_slm"
        const val FEATURE_TECH_MODE = "feature_tech_mode"

        val TIERS = listOf(
            TierInfo(0, "Blank Slate", "Pure text note, follow-up & + button", 0, "🌿", 0xFF34D399),
            TierInfo(1, "Field Notes", "Rich text, checklists, tags & fonts", 2, "📐", 0xFF38BDF8),
            TierInfo(2, "Living Canvas", "Photos, voice memos, OCR & mood", 5, "🎨", 0xFFC084FC),
            TierInfo(3, "Mirror & Spark", "Diffs, lock, cloud vault & nudges", 10, "⚡", 0xFFFBBF24),
            TierInfo(4, "Mind Atlas", "Memory Oracle Q&A & 2D constellation", 15, "🧭", 0xFFA78BFA),
            TierInfo(5, "Neural Forge", "Local Edge SLM & vector telemetry", 25, "🔮", 0xFF818CF8)
        )

        val ALL_CAPABILITIES = listOf(
            // Level 0 · Blank Slate
            CapabilityItem(
                key = FEATURE_BASIC_JOURNAL,
                tier = 0,
                title = "Note & Follow-Up",
                iconSymbol = "✍️",
                shortDescription = "Tap +, write simple text, and append follow-up points.",
                whyUse = "Zero visual clutter — pure paper-like calm for honest writing and updates.",
                whyNot = "Plain text only until you choose to enable higher features.",
                settingsLocation = "Core (Always Active)",
                isCore = true
            ),
            CapabilityItem(
                key = FEATURE_KEYWORD_SEARCH,
                tier = 0,
                title = "Instant Recall",
                iconSymbol = "🔍",
                shortDescription = "Real-time offline keyword search across titles and entries.",
                whyUse = "Finds exact words and phrases instantaneously without internet.",
                whyNot = "Literal word matching rather than AI concept search.",
                settingsLocation = "Core (Always Active)",
                isCore = true
            ),
            CapabilityItem(
                key = FEATURE_THEMES,
                tier = 0,
                title = "Atmospheres",
                iconSymbol = "🌌",
                shortDescription = "Frosted Glass, AMOLED Pitch Black, Paper Light & Pixel palettes.",
                whyUse = "Match your surroundings with restful dark or crisp daylight palettes.",
                whyNot = "Pure visual preference.",
                settingsLocation = "Settings → Essentials",
                isCore = true
            ),

            // Level 1 · Field Notes
            CapabilityItem(
                key = FEATURE_RICH_TEXT,
                tier = 1,
                title = "Rich Text & Word View",
                iconSymbol = "¶",
                shortDescription = "Bold, italic, headings, quotes, bullet lists & formatted document view.",
                whyUse = "Adds emphasis, structure, and clean section headings to longer notes.",
                whyNot = "Adds formatting buttons above the keyboard instead of pure plain text.",
                settingsLocation = "Editor Toolbar & Detail"
            ),
            CapabilityItem(
                key = FEATURE_CHECKLISTS,
                tier = 1,
                title = "Action Checklists",
                iconSymbol = "☑️",
                shortDescription = "Interactive checkboxes tucked cleanly inside any note.",
                whyUse = "Turn intentions into checkable tasks right where ideas arise.",
                whyNot = "Hide if you want pure unpressured reflection without to-do items.",
                settingsLocation = "Editor & Thought Cards"
            ),
            CapabilityItem(
                key = FEATURE_HASHTAGS,
                tier = 1,
                title = "Topic Tags & Explorer",
                iconSymbol = "#",
                shortDescription = "Pin top 7 tags in the drawer and browse all tags in Tag Explorer.",
                whyUse = "Group life chapters (#Career, #Fitness) cleanly without clutter.",
                whyNot = "Skip if you rely solely on chronological scrolling or keyword search.",
                settingsLocation = "Drawer, Editor & Detail"
            ),
            CapabilityItem(
                key = FEATURE_TYPOGRAPHY,
                tier = 1,
                title = "Type Studio",
                iconSymbol = "Aa",
                shortDescription = "Font families (Sans, Serif, Mono, Script), font scale & line spacing.",
                whyUse = "Customize reading rhythm and editorial warmth for comfort.",
                whyNot = "Default Sans spacing is already clean if you want fewer controls.",
                settingsLocation = "Settings → Writing & Editor"
            ),
            CapabilityItem(
                key = FEATURE_LAYOUT_SWITCH,
                tier = 1,
                title = "Feed Lenses",
                iconSymbol = "🪟",
                shortDescription = "Switch between Staggered Grid, Story Cards & Compact List.",
                whyUse = "Fast skimming in Compact List or rich visual cards in Grid.",
                whyNot = "Removes the layout button for a quieter top bar.",
                settingsLocation = "Home Top Bar"
            ),

            // Level 2 · Living Canvas
            CapabilityItem(
                key = FEATURE_MEDIA_CAPTURE,
                tier = 2,
                title = "Photo Moments",
                iconSymbol = "📷",
                shortDescription = "Snap camera photos or attach gallery images with captions.",
                whyUse = "Anchor feelings to real-world moments, sketches, and whiteboards.",
                whyNot = "Keeps your journal strictly text-only and saves local storage.",
                settingsLocation = "Editor Media Tray"
            ),
            CapabilityItem(
                key = FEATURE_VOICE_DICTATION,
                tier = 2,
                title = "Voice Studio",
                iconSymbol = "🎙️",
                shortDescription = "Hands-free live dictation, AAC audio memos & transcription.",
                whyUse = "Speak thoughts while walking without touching the keyboard.",
                whyNot = "Removes microphone icons across capture and search bars.",
                settingsLocation = "Capture Sheet & Top Bars"
            ),
            CapabilityItem(
                key = FEATURE_OCR,
                tier = 2,
                title = "Page Scanner",
                iconSymbol = "📄",
                shortDescription = "Extract editable text from physical books and handwritten pages.",
                whyUse = "Digitize book quotes and physical notes in seconds offline.",
                whyNot = "Hides camera OCR prompts when you only write digitally.",
                settingsLocation = "Capture Sheet & Editor"
            ),
            CapabilityItem(
                key = FEATURE_MOOD_BADGES,
                tier = 2,
                title = "Mood Aura",
                iconSymbol = "🎭",
                shortDescription = "Color-coded emotional badges and optional emoji accents on cards.",
                whyUse = "Spot emotional rhythms across weeks at a single glance.",
                whyNot = "Turn off for a calm, unlabelled notepad feel.",
                settingsLocation = "Settings → Writing & Cards"
            ),

            // Level 3 · Mirror & Spark
            CapabilityItem(
                key = FEATURE_VERSION_HISTORY,
                tier = 3,
                title = "Time Machine",
                iconSymbol = "🕰️",
                shortDescription = "Browse previous revisions, compare diffs & restore older drafts.",
                whyUse = "Edit fearlessly knowing every saved state can be restored.",
                whyNot = "Hides the history clock icon and diff dialogs completely.",
                settingsLocation = "Editor & Detail Top Bar"
            ),
            CapabilityItem(
                key = FEATURE_TASK_SPLITTER,
                tier = 3,
                title = "Task Distiller",
                iconSymbol = "⚡",
                shortDescription = "Auto-detect & convert raw paragraph lines into checklist tasks.",
                whyUse = "Turns messy brain-dumps into clean action steps with one tap.",
                whyNot = "Only needed if you actively turn prose into checklists.",
                settingsLocation = "Editor Toolbar"
            ),
            CapabilityItem(
                key = FEATURE_JESTER,
                tier = 3,
                title = "Perspective Jester",
                iconSymbol = "🃏",
                shortDescription = "Playful roasts, movie-trailer reframes & fortune cookies.",
                whyUse = "Cuts through writer's block and perfectionism with light humor.",
                whyNot = "Hide if you keep your journal strictly solemn.",
                settingsLocation = "Editor Toolbar"
            ),
            CapabilityItem(
                key = FEATURE_MINDFUL_REMINDERS,
                tier = 3,
                title = "Mindful Nudges",
                iconSymbol = "🔔",
                shortDescription = "Gentle contextual reminders & lockscreen check-in scheduling.",
                whyUse = "Follows up automatically when notes mention open blockers.",
                whyNot = "Zero notifications — journal strictly when you open the app.",
                settingsLocation = "Settings → Habits & Detail"
            ),
            CapabilityItem(
                key = FEATURE_BIOMETRIC_LOCK,
                tier = 3,
                title = "Vault Lock",
                iconSymbol = "🔐",
                shortDescription = "Protect sensitive entries behind device fingerprint authentication.",
                whyUse = "Conceals previews in feeds so private thoughts stay private.",
                whyNot = "Skip if nobody else ever uses your phone.",
                settingsLocation = "Detail Top Bar"
            ),
            CapabilityItem(
                key = FEATURE_CLOUD_BACKUP,
                tier = 3,
                title = "Cloud Vault",
                iconSymbol = "☁️",
                shortDescription = "3-slot Google Drive snapshots and portable ZIP archive backups.",
                whyUse = "Never lose years of reflections if your phone is lost or wiped.",
                whyNot = "Turn off for strict 100% air-gapped local-only storage.",
                settingsLocation = "Settings → Cloud Vault"
            ),

            // Level 4 · Mind Atlas
            CapabilityItem(
                key = FEATURE_SECOND_BRAIN,
                tier = 4,
                title = "Memory Oracle",
                iconSymbol = "🧠",
                shortDescription = "Ask natural questions to past notes + AI TL;DR summaries.",
                whyUse = "Synthesizes recurring bottlenecks and patterns across months.",
                whyNot = "Turns the app back into a clean manual notebook.",
                settingsLocation = "Search Bar, Drawer & Detail"
            ),
            CapabilityItem(
                key = FEATURE_REFLECTION_PROMPTS,
                tier = 4,
                title = "Echo Prompts",
                iconSymbol = "💭",
                shortDescription = "Socratic reflection questions & related memory cross-links.",
                whyUse = "Deepens self-inquiry and surfaces connected past entries.",
                whyNot = "Keeps note reading view free of AI follow-up cards.",
                settingsLocation = "Detail Screen Cards"
            ),
            CapabilityItem(
                key = FEATURE_SYNAPTIC_GRAPH,
                tier = 4,
                title = "Thought Constellation",
                iconSymbol = "🌐",
                shortDescription = "Interactive 2D star-map showing semantic clusters and links.",
                whyUse = "Visualize the big picture of how ideas orbit each other.",
                whyNot = "Hides the Constellation button for a leaner top bar.",
                settingsLocation = "Top Bar & Drawer"
            ),

            // Level 5 · Neural Forge
            CapabilityItem(
                key = FEATURE_ON_DEVICE_SLM,
                tier = 5,
                title = "Edge Mind AI",
                iconSymbol = "🤖",
                shortDescription = "Local Qwen 2.5 GPU inference, Gemini Flash key & Engine switch.",
                whyUse = "Full private neural synthesis running locally on device silicon.",
                whyNot = "Requires ~350–520 MB storage for local model weights.",
                settingsLocation = "Settings → Neural Forge"
            ),
            CapabilityItem(
                key = FEATURE_TECH_MODE,
                tier = 5,
                title = "Telemetry X-Ray",
                iconSymbol = "🔬",
                shortDescription = "Reveal 384-d vector embeddings, cosine scores & latency metrics.",
                whyUse = "Inspect exact semantic similarity ranks and inference timings.",
                whyNot = "Strips away developer metrics in favor of quiet human language.",
                settingsLocation = "Settings → Neural Forge"
            )
        )
    }

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<LadderState> = _state.asStateFlow()

    private val _unlockEvents = MutableSharedFlow<TierInfo>(extraBufferCapacity = 5)
    val unlockEvents: SharedFlow<TierInfo> = _unlockEvents.asSharedFlow()

    private fun loadInitialState(): LadderState {
        // Migrate cleanly: determine starting activeLevel (default 0 for new users)
        if (!prefs.getBoolean(PREF_MIGRATED_SELECTABLE_V3, false)) {
            val prevUnlocked = (prefs.getString(PREF_UNLOCKED_TIERS, "0") ?: "0")
                .split(",").mapNotNull { it.toIntOrNull() }.filter { it in 0..5 }.toSet()
            val inferredLevel = if (prefs.getBoolean(PREF_GOD_MODE, false)) 5 else (prevUnlocked.maxOrNull() ?: 0)
            val editor = prefs.edit()
                .putInt(PREF_ACTIVE_LEVEL, inferredLevel)
                .putBoolean(PREF_MIGRATED_SELECTABLE_V3, true)
            ALL_CAPABILITIES.forEach { item ->
                if (!item.isCore) {
                    val active = item.tier <= inferredLevel
                    editor.putBoolean("feat_${item.key}", active)
                }
            }
            editor.apply()
        }

        val activeLevel = prefs.getInt(PREF_ACTIVE_LEVEL, 0).coerceIn(0, 5)
        val unlockedString = prefs.getString(PREF_UNLOCKED_TIERS, "0") ?: "0"
        val unlocked = (unlockedString.split(",")
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 0..5 }
            .toSet() + (0..activeLevel).toSet()).ifEmpty { setOf(0) }

        val enabledMap = mutableMapOf<String, Boolean>()
        ALL_CAPABILITIES.forEach { item ->
            val defaultVal = item.isCore || item.tier <= activeLevel
            val isEnabled = if (item.isCore) true else prefs.getBoolean("feat_${item.key}", defaultVal)
            enabledMap[item.key] = isEnabled
        }

        return LadderState(
            currentTier = activeLevel,
            isGodMode = activeLevel == 5 && enabledMap.values.all { it },
            unlockedTiers = unlocked,
            enabledFeatures = enabledMap
        )
    }

    fun isFeatureEnabled(key: String): Boolean = _state.value.isEnabled(key)

    fun isTierUnlocked(tier: Int): Boolean = _state.value.isLevelUnlocked(tier)

    /**
     * Instantly switch the app complexity preset to any Level (0..5).
     * Enables all features at or below [targetLevel] and cleanly removes features above [targetLevel].
     */
    fun selectActiveLevel(targetLevel: Int) {
        val level = targetLevel.coerceIn(0, 5)
        val editor = prefs.edit()
        editor.putInt(PREF_ACTIVE_LEVEL, level)
        editor.putBoolean(PREF_GOD_MODE, level == 5)

        val currentUnlocked = _state.value.unlockedTiers.toMutableSet()
        for (lvl in 0..level) currentUnlocked.add(lvl)
        editor.putString(PREF_UNLOCKED_TIERS, currentUnlocked.sorted().joinToString(","))

        val updatedMap = mutableMapOf<String, Boolean>()
        ALL_CAPABILITIES.forEach { item ->
            val shouldEnable = item.isCore || item.tier <= level
            updatedMap[item.key] = shouldEnable
            if (!item.isCore) {
                editor.putBoolean("feat_${item.key}", shouldEnable)
            }
        }
        editor.apply()

        _state.update { current ->
            current.copy(
                currentTier = level,
                isGodMode = level == 5,
                unlockedTiers = currentUnlocked.toSet(),
                enabledFeatures = updatedMap,
                activeSuggestion = if (current.activeSuggestion != null && level >= current.activeSuggestion.targetLevel) null else current.activeSuggestion
            )
        }
    }

    fun toggleFeature(key: String, enabled: Boolean) {
        val item = ALL_CAPABILITIES.firstOrNull { it.key == key } ?: return
        if (item.isCore) return

        prefs.edit().putBoolean("feat_$key", enabled).apply()

        val updatedMap = _state.value.enabledFeatures.toMutableMap()
        updatedMap[key] = enabled

        val updatedUnlocked = _state.value.unlockedTiers.toMutableSet()
        if (enabled) updatedUnlocked.add(item.tier)

        // Reflect effective highest level with active optional features
        val highestActiveTier = ALL_CAPABILITIES
            .filter { !it.isCore && (updatedMap[it.key] == true) }
            .maxOfOrNull { it.tier } ?: 0

        prefs.edit()
            .putInt(PREF_ACTIVE_LEVEL, highestActiveTier)
            .putString(PREF_UNLOCKED_TIERS, updatedUnlocked.sorted().joinToString(","))
            .apply()

        _state.update { current ->
            val clearSuggestion = enabled && current.activeSuggestion?.featureKey == key
            current.copy(
                currentTier = highestActiveTier,
                isGodMode = highestActiveTier == 5 && updatedMap.values.all { it },
                unlockedTiers = updatedUnlocked.toSet(),
                enabledFeatures = updatedMap,
                activeSuggestion = if (clearSuggestion) null else current.activeSuggestion
            )
        }
    }

    fun toggleAllInLevel(level: Int, enabled: Boolean) {
        val levelItems = ALL_CAPABILITIES.filter { it.tier == level && !it.isCore }
        if (levelItems.isEmpty()) return
        val editor = prefs.edit()
        val updated = _state.value.enabledFeatures.toMutableMap()
        levelItems.forEach {
            editor.putBoolean("feat_${it.key}", enabled)
            updated[it.key] = enabled
        }

        val highestActiveTier = ALL_CAPABILITIES
            .filter { !it.isCore && (updated[it.key] == true) }
            .maxOfOrNull { it.tier } ?: 0
        editor.putInt(PREF_ACTIVE_LEVEL, highestActiveTier)
        editor.apply()

        _state.update { current ->
            current.copy(
                currentTier = highestActiveTier,
                isGodMode = highestActiveTier == 5 && updated.values.all { it },
                enabledFeatures = updated
            )
        }
    }

    fun setGodMode(enabled: Boolean) {
        selectActiveLevel(if (enabled) 5 else 0)
    }

    fun evaluateProgress(notes: List<Note>) {
        val activeNotesCount = notes.size
        val storedString = prefs.getString(PREF_UNLOCKED_TIERS, "0") ?: "0"
        val currentUnlocked = storedString.split(",")
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 0..5 }
            .toMutableSet()
        currentUnlocked.add(0)

        var newlyUnlockedLevel: TierInfo? = null
        TIERS.forEach { level ->
            if (activeNotesCount >= level.requiredNotes && !currentUnlocked.contains(level.tier)) {
                currentUnlocked.add(level.tier)
                newlyUnlockedLevel = level
            }
        }

        val lastNotified = prefs.getInt(PREF_LAST_NOTIFIED_TIER, 0)
        if (newlyUnlockedLevel != null && newlyUnlockedLevel!!.tier > lastNotified) {
            prefs.edit().putInt(PREF_LAST_NOTIFIED_TIER, newlyUnlockedLevel!!.tier).apply()
            _unlockEvents.tryEmit(newlyUnlockedLevel!!)
        }

        prefs.edit().putString(PREF_UNLOCKED_TIERS, currentUnlocked.sorted().joinToString(",")).apply()

        val suggestion = evaluateIntelligentSuggestion(notes, _state.value.copy(
            activeNotesCount = activeNotesCount,
            unlockedTiers = currentUnlocked.toSet()
        ))

        _state.update { current ->
            current.copy(
                activeNotesCount = activeNotesCount,
                unlockedTiers = currentUnlocked.toSet(),
                justUnlockedTier = newlyUnlockedLevel,
                activeSuggestion = suggestion
            )
        }
    }

    /**
     * Context-aware "once in a while" suggestion engine.
     * Examines actual notes written and active capabilities, respecting cooldowns and user dismissals.
     */
    private fun evaluateIntelligentSuggestion(notes: List<Note>, current: LadderState): ProgressionSuggestion? {
        if (notes.size < 2) return null

        val lastTime = prefs.getLong(PREF_LAST_SUGGESTION_TIME, 0L)
        val lastNoteCount = prefs.getInt(PREF_LAST_SUGGESTION_NOTES, 0)
        val now = System.currentTimeMillis()

        // Enforce "once in a while" pacing: wait at least 12h AND +2 new thoughts after a dismissal
        if (lastTime > 0L) {
            val cooldownElapsed = (now - lastTime) >= SUGGESTION_COOLDOWN_MS
            val noteDeltaMet = (notes.size - lastNoteCount) >= 2
            if (!cooldownElapsed || !noteDeltaMet) {
                return current.activeSuggestion
            }
        }

        val dismissed = (prefs.getString(PREF_DISMISSED_SUGGESTIONS, "") ?: "")
            .split(",").filter { it.isNotBlank() }.toSet()

        fun buildSuggestion(
            key: String,
            headline: String,
            reason: String
        ): ProgressionSuggestion? {
            if (dismissed.contains(key) || current.isEnabled(key)) return null
            val item = ALL_CAPABILITIES.firstOrNull { it.key == key } ?: return null
            val lvl = TIERS.firstOrNull { it.tier == item.tier } ?: return null
            return ProgressionSuggestion(
                featureKey = item.key,
                featureTitle = item.title,
                featureIcon = item.iconSymbol,
                targetLevel = item.tier,
                targetLevelTitle = lvl.title,
                headline = headline,
                reason = reason,
                drawback = item.whyNot,
                accentHex = lvl.accentHex
            )
        }

        // 1. Detecting action/task lines at Level 0 -> Suggest Action Checklists (L1)
        val actionNote = notes.firstOrNull { note ->
            note.content.lines().any { l ->
                val t = l.trim().lowercase()
                t.startsWith("- ") || t.startsWith("* ") || t.startsWith("todo") || t.contains("need to ") || t.contains("remember to ")
            }
        }
        if (actionNote != null) {
            buildSuggestion(
                key = FEATURE_CHECKLISTS,
                headline = "Turn plans into checkable tasks?",
                reason = "Noticed action steps in \"${actionNote.title.ifBlank { "your recent note" }}\". Action Checklists lets you tick items cleanly inside notes."
            )?.let { return it }
        }

        // 2. Long multi-paragraph writing -> Suggest Rich Text & Headings (L1)
        val richNote = notes.firstOrNull { it.content.split(Regex("\\s+")).size >= 45 || it.content.contains("\n\n") }
        if (richNote != null) {
            buildSuggestion(
                key = FEATURE_RICH_TEXT,
                headline = "Give longer thoughts crisp structure?",
                reason = "\"${richNote.title.ifBlank { "Your thought" }}\" has deep paragraphs. Unlocking Rich Text adds bold highlights, section headings & clean Word view."
            )?.let { return it }
        }

        // 3. Growing library (>= 3 notes) -> Suggest Topic Tags (L1)
        if (notes.size >= 3) {
            buildSuggestion(
                key = FEATURE_HASHTAGS,
                headline = "Organize recurring themes with Topic Tags?",
                reason = "With ${notes.size} thoughts captured, Topic Tags pins your top 7 themes in the drawer and unlocks the dedicated Tag Explorer."
            )?.let { return it }
        }

        // 4. >= 4 notes -> Suggest Type Studio (L1)
        if (notes.size >= 4) {
            buildSuggestion(
                key = FEATURE_TYPOGRAPHY,
                headline = "Tailor fonts & reading rhythm?",
                reason = "Type Studio unlocks Serif, Monospace, and Editorial fonts plus comfortable line spacing."
            )?.let { return it }
        }

        // 5. >= 5 notes -> Suggest Voice Studio (L2)
        if (notes.size >= 5) {
            buildSuggestion(
                key = FEATURE_VOICE_DICTATION,
                headline = "Capture ideas faster hands-free?",
                reason = "You've written ${notes.size} thoughts! Voice Studio lets you dictate on the go and save voice memos effortlessly."
            )?.let { return it }
        }

        // 6. Emotional vocabulary detected -> Suggest Mood Aura (L2)
        val moodNote = notes.firstOrNull { n ->
            val lower = n.content.lowercase()
            listOf("feel", "feeling", "stress", "calm", "happy", "anxious", "grateful", "tired").any { lower.contains(it) }
        }
        if (moodNote != null && notes.size >= 5) {
            buildSuggestion(
                key = FEATURE_MOOD_BADGES,
                headline = "Spot emotional patterns with Mood Aura?",
                reason = "Your reflections carry clear emotional depth. Mood Aura highlights emotional themes across your journal timeline."
            )?.let { return it }
        }

        // 7. >= 8 notes -> Suggest Cloud Vault (L3)
        if (notes.size >= 8) {
            buildSuggestion(
                key = FEATURE_CLOUD_BACKUP,
                headline = "Keep your ${notes.size} thoughts safely backed up?",
                reason = "Cloud Vault adds automatic 3-slot encrypted Google Drive snapshots so your memories are never lost."
            )?.let { return it }
        }

        // 8. >= 12 notes -> Suggest Memory Oracle (L4)
        if (notes.size >= 12) {
            buildSuggestion(
                key = FEATURE_SECOND_BRAIN,
                headline = "Ask questions across all ${notes.size} thoughts?",
                reason = "Your journal is rich enough for semantic synthesis! Memory Oracle answers questions by connecting your past thoughts."
            )?.let { return it }
        }

        // 9. >= 15 notes -> Suggest Thought Constellation (L4)
        if (notes.size >= 15) {
            buildSuggestion(
                key = FEATURE_SYNAPTIC_GRAPH,
                headline = "Explore your ideas as a 2D star map?",
                reason = "Thought Constellation visualizes how your ${notes.size} notes cluster and link in a navigable 2D space."
            )?.let { return it }
        }

        // 10. >= 22 notes -> Suggest Edge Mind AI (L5)
        if (notes.size >= 22) {
            buildSuggestion(
                key = FEATURE_ON_DEVICE_SLM,
                headline = "Unlock private offline Edge AI?",
                reason = "Edge Mind AI runs a local Qwen 2.5 Small Language Model directly on your device GPU with zero cloud calls."
            )?.let { return it }
        }

        return null
    }

    fun acceptSuggestionFeature(suggestion: ProgressionSuggestion) {
        toggleFeature(suggestion.featureKey, true)
        prefs.edit()
            .putLong(PREF_LAST_SUGGESTION_TIME, System.currentTimeMillis())
            .putInt(PREF_LAST_SUGGESTION_NOTES, _state.value.activeNotesCount)
            .apply()
        _state.update { it.copy(activeSuggestion = null) }
    }

    fun acceptSuggestionLevel(suggestion: ProgressionSuggestion) {
        selectActiveLevel(suggestion.targetLevel)
        prefs.edit()
            .putLong(PREF_LAST_SUGGESTION_TIME, System.currentTimeMillis())
            .putInt(PREF_LAST_SUGGESTION_NOTES, _state.value.activeNotesCount)
            .apply()
        _state.update { it.copy(activeSuggestion = null) }
    }

    fun dismissSuggestion(suggestion: ProgressionSuggestion) {
        val existing = (prefs.getString(PREF_DISMISSED_SUGGESTIONS, "") ?: "")
            .split(",").filter { it.isNotBlank() }.toMutableSet()
        existing.add(suggestion.featureKey)
        prefs.edit()
            .putString(PREF_DISMISSED_SUGGESTIONS, existing.joinToString(","))
            .putLong(PREF_LAST_SUGGESTION_TIME, System.currentTimeMillis())
            .putInt(PREF_LAST_SUGGESTION_NOTES, _state.value.activeNotesCount)
            .apply()
        _state.update { it.copy(activeSuggestion = null) }
    }

    fun resetDismissedSuggestionsForReview() {
        prefs.edit()
            .remove(PREF_DISMISSED_SUGGESTIONS)
            .remove(PREF_LAST_SUGGESTION_TIME)
            .apply()
    }

    fun clearUnlockToast() {
        _state.update { it.copy(justUnlockedTier = null) }
    }

    fun getNextTierInfo(): Pair<TierInfo?, Int> {
        val s = _state.value
        val highest = s.currentTier
        if (highest >= 5) return Pair(null, 0)
        val next = TIERS.firstOrNull { it.tier == highest + 1 } ?: return Pair(null, 0)
        val remaining = maxOf(0, next.requiredNotes - s.activeNotesCount)
        return Pair(next, remaining)
    }
}

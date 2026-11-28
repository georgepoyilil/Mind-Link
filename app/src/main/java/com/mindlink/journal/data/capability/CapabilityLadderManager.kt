package com.mindlink.journal.data.capability

import android.content.Context
import android.content.SharedPreferences
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

data class LadderState(
    val currentTier: Int = 0, // Level 0..5
    val activeNotesCount: Int = 0,
    val isGodMode: Boolean = false,
    val unlockedTiers: Set<Int> = setOf(0),
    val enabledFeatures: Map<String, Boolean> = emptyMap(),
    val justUnlockedTier: TierInfo? = null
) {
    fun isLevelUnlocked(level: Int): Boolean = isGodMode || unlockedTiers.contains(level)

    fun isEnabled(key: String): Boolean {
        val item = CapabilityLadderManager.ALL_CAPABILITIES.firstOrNull { it.key == key } ?: return true
        if (item.isCore) return true
        if (!isLevelUnlocked(item.tier)) return false
        return enabledFeatures[key] ?: true
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
        const val PREF_UNLOCKED_TIERS = "pref_unlocked_tiers"
        const val PREF_LAST_NOTIFIED_TIER = "pref_last_notified_tier"
        private const val PREF_MIGRATED_LEVEL_ZERO = "pref_migrated_level_zero_v2"

        // Level 0 · Blank Slate (Core Essentials)
        const val FEATURE_BASIC_JOURNAL = "feature_basic_journal"
        const val FEATURE_KEYWORD_SEARCH = "feature_keyword_search"
        const val FEATURE_THEMES = "feature_themes"

        // Level 1 · Field Notes (Organization & Style)
        const val FEATURE_CHECKLISTS = "feature_checklists"
        const val FEATURE_TYPOGRAPHY = "feature_typography"
        const val FEATURE_HASHTAGS = "feature_hashtags"
        const val FEATURE_LAYOUT_SWITCH = "feature_layout_switch"

        // Level 2 · Living Canvas (Multimodal Capture)
        const val FEATURE_MEDIA_CAPTURE = "feature_media_capture"
        const val FEATURE_OCR = "feature_ocr"
        const val FEATURE_VOICE_DICTATION = "feature_voice_dictation"
        const val FEATURE_MOOD_BADGES = "feature_mood_badges"

        // Level 3 · Mirror & Spark (Depth, Privacy & Habits)
        const val FEATURE_VERSION_HISTORY = "feature_version_history"
        const val FEATURE_TASK_SPLITTER = "feature_task_splitter"
        const val FEATURE_JESTER = "feature_jester"
        const val FEATURE_MINDFUL_REMINDERS = "feature_mindful_reminders"
        const val FEATURE_BIOMETRIC_LOCK = "feature_biometric_lock"
        const val FEATURE_CLOUD_BACKUP = "feature_cloud_backup"

        // Level 4 · Mind Atlas (Synthesis & Graph)
        const val FEATURE_SECOND_BRAIN = "feature_second_brain"
        const val FEATURE_REFLECTION_PROMPTS = "feature_reflection_prompts"
        const val FEATURE_SYNAPTIC_GRAPH = "feature_synaptic_graph"

        // Level 5 · Neural Forge (Edge AI & Diagnostics)
        const val FEATURE_ON_DEVICE_SLM = "feature_on_device_slm"
        const val FEATURE_TECH_MODE = "feature_tech_mode"

        val TIERS = listOf(
            TierInfo(0, "Blank Slate", "Pure, distraction-free sanctuary", 0, "🌿", 0xFF34D399),
            TierInfo(1, "Field Notes", "Shape, tag & structure thoughts", 2, "📐", 0xFF38BDF8),
            TierInfo(2, "Living Canvas", "Photos, voice memos & page scans", 5, "🎨", 0xFFC084FC),
            TierInfo(3, "Mirror & Spark", "Revisions, locks, backups & wit", 10, "⚡", 0xFFFBBF24),
            TierInfo(4, "Mind Atlas", "Ask past notes & map constellations", 15, "🧭", 0xFFA78BFA),
            TierInfo(5, "Neural Forge", "Local edge AI & vector telemetry", 25, "🔮", 0xFF818CF8)
        )

        val ALL_CAPABILITIES = listOf(
            // Level 0 · Blank Slate
            CapabilityItem(
                key = FEATURE_BASIC_JOURNAL,
                tier = 0,
                title = "Stream Editor",
                iconSymbol = "✍️",
                shortDescription = "Clean markdown canvas with bold, italic, headings & undo.",
                whyUse = "Zero cognitive load — open the app and immediately pour out raw thoughts.",
                whyNot = "Minimal styling only; no embedded checklists or media attachments.",
                settingsLocation = "Core (Always Active)",
                isCore = true
            ),
            CapabilityItem(
                key = FEATURE_KEYWORD_SEARCH,
                tier = 0,
                title = "Instant Recall",
                iconSymbol = "🔍",
                shortDescription = "Real-time SQLite full-text search across titles and notes.",
                whyUse = "Finds exact words and phrases instantaneously offline.",
                whyNot = "Matches literal words rather than conceptual synonyms.",
                settingsLocation = "Core (Always Active)",
                isCore = true
            ),
            CapabilityItem(
                key = FEATURE_THEMES,
                tier = 0,
                title = "Atmospheres",
                iconSymbol = "🌌",
                shortDescription = "Frosted Glass, AMOLED Pitch Black, Paper Light & Pixel palettes.",
                whyUse = "Match your environment with low-strain dark or crisp daylight visuals.",
                whyNot = "Purely visual preference.",
                settingsLocation = "Settings → Essentials",
                isCore = true
            ),

            // Level 1 · Field Notes
            CapabilityItem(
                key = FEATURE_CHECKLISTS,
                tier = 1,
                title = "Action Checklists",
                iconSymbol = "☑️",
                shortDescription = "Interactive checkboxes tucked cleanly inside any note.",
                whyUse = "Turn intentions into trackable action items right where insights happen.",
                whyNot = "Hide if you strictly want pure prose without to-do pressure.",
                settingsLocation = "Note Editor & Cards"
            ),
            CapabilityItem(
                key = FEATURE_TYPOGRAPHY,
                tier = 1,
                title = "Type Studio",
                iconSymbol = "Aa",
                shortDescription = "Customize font families (Sans, Serif, Mono, Script), scale & line spacing.",
                whyUse = "Tailor reading rhythm, font size, and editorial warmth to your eyes.",
                whyNot = "Default Sans typography is already calibrated for fast reading.",
                settingsLocation = "Settings → Writing & Editor"
            ),
            CapabilityItem(
                key = FEATURE_HASHTAGS,
                tier = 1,
                title = "Topic Tags & Explorer",
                iconSymbol = "#",
                shortDescription = "Pin top 7 tags in the drawer and browse all tags in Tag Explorer.",
                whyUse = "Group life chapters (#Career, #Fitness) cleanly without drawer bloat.",
                whyNot = "Turn off if you rely purely on search or folders/archive.",
                settingsLocation = "Drawer, Editor & Detail"
            ),
            CapabilityItem(
                key = FEATURE_LAYOUT_SWITCH,
                tier = 1,
                title = "Feed Lenses",
                iconSymbol = "🪟",
                shortDescription = "Toggle between Staggered Grid, Story Cards, and Compact List.",
                whyUse = "Dense skimming in Compact List or visual browsing in Grid.",
                whyNot = "Removes the layout switcher button for a quieter top bar.",
                settingsLocation = "Top Bar Switcher"
            ),

            // Level 2 · Living Canvas
            CapabilityItem(
                key = FEATURE_MEDIA_CAPTURE,
                tier = 2,
                title = "Photo Moments",
                iconSymbol = "📷",
                shortDescription = "Snap camera photos or attach gallery images with captions.",
                whyUse = "Anchor feelings to real-world moments, sketches, and whiteboards.",
                whyNot = "Keeps journal strictly text-only and saves local storage space.",
                settingsLocation = "Editor Media Tray"
            ),
            CapabilityItem(
                key = FEATURE_OCR,
                tier = 2,
                title = "Page Scanner",
                iconSymbol = "📄",
                shortDescription = "Extract editable text from physical books and handwritten pages.",
                whyUse = "Digitize paper quotes and notebooks in seconds offline.",
                whyNot = "Hides camera scan prompts when you only type directly.",
                settingsLocation = "Capture Sheet & Editor"
            ),
            CapabilityItem(
                key = FEATURE_VOICE_DICTATION,
                tier = 2,
                title = "Voice Studio",
                iconSymbol = "🎙️",
                shortDescription = "Hands-free live dictation, AAC audio memos & transcription.",
                whyUse = "Capture fast thoughts while walking without touching the keyboard.",
                whyNot = "Removes mic buttons from top bars and search boxes.",
                settingsLocation = "Capture Sheet & Top Bars"
            ),
            CapabilityItem(
                key = FEATURE_MOOD_BADGES,
                tier = 2,
                title = "Mood Aura",
                iconSymbol = "🎭",
                shortDescription = "Color-coded emotional badges and optional emoji accents on cards.",
                whyUse = "Spot emotional patterns across weeks at a single glance.",
                whyNot = "Turn off for a monochrome, clinical notepad aesthetic.",
                settingsLocation = "Settings → Writing"
            ),

            // Level 3 · Mirror & Spark
            CapabilityItem(
                key = FEATURE_VERSION_HISTORY,
                tier = 3,
                title = "Time Machine",
                iconSymbol = "🕰️",
                shortDescription = "Browse previous revisions, compare diffs, and restore older drafts.",
                whyUse = "Edit fearlessly knowing every saved state can be recovered.",
                whyNot = "Hides the clock/history button in editor and detail screens.",
                settingsLocation = "Editor & Detail Top Bar"
            ),
            CapabilityItem(
                key = FEATURE_TASK_SPLITTER,
                tier = 3,
                title = "Task Distiller",
                iconSymbol = "⚡",
                shortDescription = "Auto-detect & convert raw brain-dump lines into checklist tasks.",
                whyUse = "Turns messy paragraphs into clean action steps with one tap.",
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
                whyNot = "Hide if you keep your journal strictly solemn and serious.",
                settingsLocation = "Editor Toolbar"
            ),
            CapabilityItem(
                key = FEATURE_MINDFUL_REMINDERS,
                tier = 3,
                title = "Mindful Nudges",
                iconSymbol = "🔔",
                shortDescription = "Gentle contextual reminders and lockscreen check-in scheduling.",
                whyUse = "Follows up automatically when notes mention lingering blockers.",
                whyNot = "Zero notifications — journal strictly on your own terms.",
                settingsLocation = "Settings → Growth & Detail"
            ),
            CapabilityItem(
                key = FEATURE_BIOMETRIC_LOCK,
                tier = 3,
                title = "Vault Lock",
                iconSymbol = "🔐",
                shortDescription = "Protect sensitive entries behind device fingerprint authentication.",
                whyUse = "Conceals previews in feeds so private entries stay private.",
                whyNot = "Skip if only you ever hold your unlocked phone.",
                settingsLocation = "Detail Screen Top Bar"
            ),
            CapabilityItem(
                key = FEATURE_CLOUD_BACKUP,
                tier = 3,
                title = "Cloud Vault",
                iconSymbol = "☁️",
                shortDescription = "3-slot Google Drive snapshots and portable ZIP archive backups.",
                whyUse = "Never lose years of reflections if your phone is lost or wiped.",
                whyNot = "Turn off for strict 100% air-gapped local-only storage.",
                settingsLocation = "Settings → Vault & Drawer"
            ),

            // Level 4 · Mind Atlas
            CapabilityItem(
                key = FEATURE_SECOND_BRAIN,
                tier = 4,
                title = "Memory Oracle",
                iconSymbol = "🧠",
                shortDescription = "Ask natural questions to your notes + auto AI TL;DR summaries.",
                whyUse = "Synthesizes hidden patterns across months of entries.",
                whyNot = "Turns the app back into a manual archive without AI synthesis.",
                settingsLocation = "Search Bar, Drawer & Detail"
            ),
            CapabilityItem(
                key = FEATURE_REFLECTION_PROMPTS,
                tier = 4,
                title = "Echo Prompts",
                iconSymbol = "💭",
                shortDescription = "Socratic reflection questions & related memory cross-links.",
                whyUse = "Deepens self-inquiry and surfaces forgotten relevant entries.",
                whyNot = "Keeps note reading view uncluttered by reflection cards.",
                settingsLocation = "Detail Screen Cards"
            ),
            CapabilityItem(
                key = FEATURE_SYNAPTIC_GRAPH,
                tier = 4,
                title = "Thought Constellation",
                iconSymbol = "🌐",
                shortDescription = "Interactive 2D star-map showing semantic clusters and links.",
                whyUse = "See the big picture of how themes orbit each other in your mind.",
                whyNot = "Hides the Constellation button for a leaner home screen.",
                settingsLocation = "Top Bar & Drawer"
            ),

            // Level 5 · Neural Forge
            CapabilityItem(
                key = FEATURE_ON_DEVICE_SLM,
                tier = 5,
                title = "Edge Mind AI",
                iconSymbol = "🤖",
                shortDescription = "Local Qwen 2.5 GPU inference, Gemini Flash key & Engine switcher.",
                whyUse = "Full private neural reasoning directly on your phone silicon.",
                whyNot = "Uses 350–520 MB storage for local model weights and GPU compute.",
                settingsLocation = "Settings → Neural Forge"
            ),
            CapabilityItem(
                key = FEATURE_TECH_MODE,
                tier = 5,
                title = "Telemetry X-Ray",
                iconSymbol = "🔬",
                shortDescription = "Reveal 384-d vector embeddings, cosine scores & latency metrics.",
                whyUse = "Full transparency into how the semantic ranking engine thinks.",
                whyNot = "Strips away developer metrics in favor of serene human copy.",
                settingsLocation = "Settings → Neural Forge"
            )
        )
    }

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<LadderState> = _state.asStateFlow()

    private val _unlockEvents = MutableSharedFlow<TierInfo>(extraBufferCapacity = 5)
    val unlockEvents: SharedFlow<TierInfo> = _unlockEvents.asSharedFlow()

    private fun loadInitialState(): LadderState {
        // Migrate legacy 1-based Tier schema cleanly to 0-based Level 0..5 schema
        if (!prefs.getBoolean(PREF_MIGRATED_LEVEL_ZERO, false)) {
            prefs.edit()
                .putString(PREF_UNLOCKED_TIERS, "0")
                .putInt(PREF_LAST_NOTIFIED_TIER, 0)
                .putBoolean(PREF_MIGRATED_LEVEL_ZERO, true)
                .apply()
        }

        val isGod = prefs.getBoolean(PREF_GOD_MODE, false)
        val unlockedString = prefs.getString(PREF_UNLOCKED_TIERS, "0") ?: "0"
        val unlocked = unlockedString.split(",")
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 0..5 }
            .toSet()
            .ifEmpty { setOf(0) } + setOf(0)

        val enabledMap = mutableMapOf<String, Boolean>()
        ALL_CAPABILITIES.forEach { item ->
            val isEnabled = if (item.isCore) true else prefs.getBoolean("feat_${item.key}", true)
            enabledMap[item.key] = isEnabled
        }

        val highestLevel = if (isGod) 5 else (unlocked.maxOrNull() ?: 0)
        return LadderState(
            currentTier = highestLevel,
            isGodMode = isGod,
            unlockedTiers = if (isGod) (0..5).toSet() else unlocked,
            enabledFeatures = enabledMap
        )
    }

    fun isFeatureEnabled(key: String): Boolean = _state.value.isEnabled(key)

    fun isTierUnlocked(tier: Int): Boolean = _state.value.isLevelUnlocked(tier)

    fun toggleFeature(key: String, enabled: Boolean) {
        val item = ALL_CAPABILITIES.firstOrNull { it.key == key }
        if (item?.isCore == true) return

        prefs.edit().putBoolean("feat_$key", enabled).apply()
        _state.update { current ->
            val updated = current.enabledFeatures.toMutableMap()
            updated[key] = enabled
            current.copy(enabledFeatures = updated)
        }
    }

    fun toggleAllInLevel(level: Int, enabled: Boolean) {
        val levelItems = ALL_CAPABILITIES.filter { it.tier == level && !it.isCore }
        if (levelItems.isEmpty()) return
        val editor = prefs.edit()
        levelItems.forEach { editor.putBoolean("feat_${it.key}", enabled) }
        editor.apply()

        _state.update { current ->
            val updated = current.enabledFeatures.toMutableMap()
            levelItems.forEach { updated[it.key] = enabled }
            current.copy(enabledFeatures = updated)
        }
    }

    fun setGodMode(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_GOD_MODE, enabled).apply()
        val storedUnlocked = (prefs.getString(PREF_UNLOCKED_TIERS, "0") ?: "0")
            .split(",").mapNotNull { it.toIntOrNull() }.filter { it in 0..5 }.toSet() + setOf(0)
        _state.update { current ->
            val highestLevel = if (enabled) 5 else (storedUnlocked.maxOrNull() ?: 0)
            current.copy(
                isGodMode = enabled,
                currentTier = highestLevel,
                unlockedTiers = if (enabled) (0..5).toSet() else storedUnlocked
            )
        }
    }

    fun evaluateProgress(activeNotesCount: Int) {
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
        _state.update { current ->
            val isGod = current.isGodMode
            val highest = if (isGod) 5 else (currentUnlocked.maxOrNull() ?: 0)
            current.copy(
                activeNotesCount = activeNotesCount,
                currentTier = highest,
                unlockedTiers = if (isGod) (0..5).toSet() else currentUnlocked.toSet(),
                justUnlockedTier = newlyUnlockedLevel
            )
        }
    }

    fun clearUnlockToast() {
        _state.update { it.copy(justUnlockedTier = null) }
    }

    fun getNextTierInfo(): Pair<TierInfo?, Int> {
        val s = _state.value
        val highest = s.unlockedTiers.maxOrNull() ?: 0
        if (highest >= 5) return Pair(null, 0)
        val next = TIERS.firstOrNull { it.tier == highest + 1 } ?: return Pair(null, 0)
        val remaining = maxOf(0, next.requiredNotes - s.activeNotesCount)
        return Pair(next, remaining)
    }
}

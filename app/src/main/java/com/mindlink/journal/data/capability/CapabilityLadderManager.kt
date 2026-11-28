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
    val tier: Int,
    val title: String,
    val iconSymbol: String,
    val shortDescription: String,
    val whyUse: String,
    val whyNot: String,
    val isCore: Boolean = false // If true, cannot be turned off (e.g. basic editor)
)

data class TierInfo(
    val tier: Int,
    val title: String,
    val subtitle: String,
    val requiredNotes: Int,
    val badgeIcon: String
)

data class LadderState(
    val currentTier: Int = 1,
    val activeNotesCount: Int = 0,
    val isGodMode: Boolean = false,
    val unlockedTiers: Set<Int> = setOf(1),
    val enabledFeatures: Map<String, Boolean> = emptyMap(),
    val justUnlockedTier: TierInfo? = null
)

class CapabilityLadderManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("mindlink_capability_ladder", Context.MODE_PRIVATE)

    companion object {
        const val PREF_GOD_MODE = "pref_god_mode"
        const val PREF_UNLOCKED_TIERS = "pref_unlocked_tiers"
        const val PREF_LAST_NOTIFIED_TIER = "pref_last_notified_tier"

        // Feature Keys
        const val FEATURE_BASIC_JOURNAL = "feature_basic_journal"
        const val FEATURE_KEYWORD_SEARCH = "feature_keyword_search"
        const val FEATURE_THEMES = "feature_themes"

        const val FEATURE_CHECKLISTS = "feature_checklists"
        const val FEATURE_TYPOGRAPHY = "feature_typography"
        const val FEATURE_HASHTAGS = "feature_hashtags"

        const val FEATURE_MEDIA_CAPTURE = "feature_media_capture"
        const val FEATURE_OCR = "feature_ocr"
        const val FEATURE_VOICE_DICTATION = "feature_voice_dictation"

        const val FEATURE_JESTER = "feature_jester"
        const val FEATURE_TASK_SPLITTER = "feature_task_splitter"
        const val FEATURE_MINDFUL_REMINDERS = "feature_mindful_reminders"

        const val FEATURE_SECOND_BRAIN = "feature_second_brain"
        const val FEATURE_SYNAPTIC_GRAPH = "feature_synaptic_graph"
        const val FEATURE_CONTRARIAN = "feature_contrarian"

        const val FEATURE_ON_DEVICE_SLM = "feature_on_device_slm"
        const val FEATURE_TECH_MODE = "feature_tech_mode"
        const val FEATURE_CLOUD_BACKUP = "feature_cloud_backup"

        val TIERS = listOf(
            TierInfo(1, "Minimalist Sanctuary", "Distraction-free clarity & pure prose", 0, "🌿"),
            TierInfo(2, "Structured Thinker", "Checklists, typography & topic tags", 2, "📐"),
            TierInfo(3, "Sensory Capture", "Camera photos, real-world OCR & voice notes", 5, "📸"),
            TierInfo(4, "Dynamic Cognition", "MindLink Jester, smart splitter & habits", 10, "⚡"),
            TierInfo(5, "Associative Second Brain", "Knowledge graph, Q&A & contrarian tests", 15, "🧠"),
            TierInfo(6, "Edge AI Pioneer", "On-Device SLM neural engine & cloud sync", 25, "🚀")
        )

        val ALL_CAPABILITIES = listOf(
            // Tier 1
            CapabilityItem(
                key = FEATURE_BASIC_JOURNAL,
                tier = 1,
                title = "Minimalist Thought Editor",
                iconSymbol = "✍️",
                shortDescription = "Distraction-free markdown writing and instant thought capture.",
                whyUse = "Zero mental friction. Pure blank canvas for genuine stream-of-consciousness.",
                whyNot = "No rich media or advanced AI features.",
                isCore = true
            ),
            CapabilityItem(
                key = FEATURE_KEYWORD_SEARCH,
                tier = 1,
                title = "Instant Keyword Search",
                iconSymbol = "🔍",
                shortDescription = "Real-time search across titles and notes.",
                whyUse = "Blazing fast exact string lookup across your entire journal.",
                whyNot = "Does not understand semantic intent or synonyms.",
                isCore = true
            ),
            CapabilityItem(
                key = FEATURE_THEMES,
                tier = 1,
                title = "Visual Atmospheres",
                iconSymbol = "🎨",
                shortDescription = "Frosted Dark, Light, and Amethyst color palettes.",
                whyUse = "Reduces eye strain with customized OLED and paper-like themes.",
                whyNot = "Pure aesthetic preference.",
                isCore = true
            ),

            // Tier 2
            CapabilityItem(
                key = FEATURE_CHECKLISTS,
                tier = 2,
                title = "Interactive Checklists (+ Task)",
                iconSymbol = "☑️",
                shortDescription = "Add actionable checklist items directly inside notes.",
                whyUse = "Keep commitments, todos, and routines organized alongside your reflections.",
                whyNot = "May introduce task anxiety if you want pure unburdened reflection."
            ),
            CapabilityItem(
                key = FEATURE_TYPOGRAPHY,
                tier = 2,
                title = "Aa Typography Sheet",
                iconSymbol = "Aa",
                shortDescription = "Sliders for font size, family (Sans/Serif/Mono/Elegant) & spacing.",
                whyUse = "Fine-tune reading comfort and line height tailored to your eyes.",
                whyNot = "Default typography is already carefully calibrated."
            ),
            CapabilityItem(
                key = FEATURE_HASHTAGS,
                tier = 2,
                title = "Hashtags & Tag Explorer",
                iconSymbol = "#",
                shortDescription = "Categorize thoughts with hashtags and explore them in a dedicated screen.",
                whyUse = "Filter and track life chapters (#Career, #Fitness, #Reflection) effortlessly.",
                whyNot = "If you prefer AI semantic retrieval over manual tagging."
            ),

            // Tier 3
            CapabilityItem(
                key = FEATURE_MEDIA_CAPTURE,
                tier = 3,
                title = "Photo & Camera Attachments",
                iconSymbol = "📷",
                shortDescription = "Attach moments, whiteboards, and notebook photos to thoughts.",
                whyUse = "Preserve visual memories and real-world whiteboard inspiration.",
                whyNot = "Increases local phone storage and backup archive sizes."
            ),
            CapabilityItem(
                key = FEATURE_OCR,
                tier = 3,
                title = "On-Device OCR Scanner",
                iconSymbol = "🔎",
                shortDescription = "Extract editable text from camera snapshots and book excerpts.",
                whyUse = "Instantly digitize physical book highlights without manual typing.",
                whyNot = "Takes 1-2 seconds of on-device processing."
            ),
            CapabilityItem(
                key = FEATURE_VOICE_DICTATION,
                tier = 3,
                title = "Voice Notes & Queries",
                iconSymbol = "🎙️",
                shortDescription = "Dictate thoughts and speak natural-language queries.",
                whyUse = "Capture spontaneous insights while walking or commuting hands-free.",
                whyNot = "Requires microphone permission and a reasonably quiet space."
            ),

            // Tier 4
            CapabilityItem(
                key = FEATURE_JESTER,
                tier = 4,
                title = "🎭 MindLink Jester (Wit & Roast)",
                iconSymbol = "🎭",
                shortDescription = "Playful AI roasts, movie trailers, and fortune cookies.",
                whyUse = "Breaks perfectionism and writer's block with humor and alternative perspectives.",
                whyNot = "If you treat journaling strictly as solemn meditation or serious logging."
            ),
            CapabilityItem(
                key = FEATURE_TASK_SPLITTER,
                tier = 4,
                title = "⚡ Dynamic Task Splitter",
                iconSymbol = "⚡",
                shortDescription = "1-tap conversion of paragraphs and raw text into checklists.",
                whyUse = "Instantly transforms messy brain dumps into actionable bullet points.",
                whyNot = "Unnecessary if you don't embed checklists in thoughts."
            ),
            CapabilityItem(
                key = FEATURE_MINDFUL_REMINDERS,
                tier = 4,
                title = "Mindful Reflection Notifications",
                iconSymbol = "🔔",
                shortDescription = "Gentle lockscreen reminders for daily journaling cadence.",
                whyUse = "Builds a consistent, lasting daily reflection habit.",
                whyNot = "If you dislike notification interruptions and prefer spontaneous writing."
            ),

            // Tier 5
            CapabilityItem(
                key = FEATURE_SECOND_BRAIN,
                tier = 5,
                title = "Associative Q&A Hub",
                iconSymbol = "🧠",
                shortDescription = "Ask questions to past notes using 384-dimensional vector links.",
                whyUse = "Synthesizes recurring bottlenecks and life insights across months.",
                whyNot = "Adds extra cognitive navigation if you just want a chronological list."
            ),
            CapabilityItem(
                key = FEATURE_SYNAPTIC_GRAPH,
                tier = 5,
                title = "Thought Constellation Graph",
                iconSymbol = "🌐",
                shortDescription = "Interactive 2D solar system mapping connections between notes.",
                whyUse = "Visualizes how ideas, people, and themes cluster in your mind.",
                whyNot = "Purely exploratory visualization; text list is faster for search."
            ),
            CapabilityItem(
                key = FEATURE_CONTRARIAN,
                tier = 5,
                title = "Contrarian Challenge & Experiments",
                iconSymbol = "🔄",
                shortDescription = "Devil's advocate inquiries and 15-minute micro-experiments.",
                whyUse = "Pushes back on your blind spots and converts insights into immediate action.",
                whyNot = "Can feel intellectually provocative when you just want a comforting diary."
            ),

            // Tier 6
            CapabilityItem(
                key = FEATURE_ON_DEVICE_SLM,
                tier = 6,
                title = "On-Device SLM (Edge LLM)",
                iconSymbol = "🤖",
                shortDescription = "Private local language model (Qwen 2.5 0.5B / SmolLM2).",
                whyUse = "Zero cloud dependency. Deep neural reasoning with complete privacy.",
                whyNot = "Requires ~350MB-520MB storage download and phone GPU compute."
            ),
            CapabilityItem(
                key = FEATURE_TECH_MODE,
                tier = 6,
                title = "Neural Inspector (Tech Mode)",
                iconSymbol = "🔬",
                shortDescription = "Expose raw 384-d vector embeddings, cosine scores, and latency.",
                whyUse = "Full technical transparency into on-device AI algorithms.",
                whyNot = "Adds developer jargon that is irrelevant to normal journaling."
            ),
            CapabilityItem(
                key = FEATURE_CLOUD_BACKUP,
                tier = 6,
                title = "Encrypted Google Drive 3-Slot Backup",
                iconSymbol = "☁️",
                shortDescription = "Automatic multi-slot backup and disaster recovery.",
                whyUse = "Guarantees you never lose your journal even if your phone is lost.",
                whyNot = "Requires connecting a Google account."
            )
        )
    }

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<LadderState> = _state.asStateFlow()

    private val _unlockEvents = MutableSharedFlow<TierInfo>(extraBufferCapacity = 5)
    val unlockEvents: SharedFlow<TierInfo> = _unlockEvents.asSharedFlow()

    private fun loadInitialState(): LadderState {
        val isGod = prefs.getBoolean(PREF_GOD_MODE, false)
        val unlockedString = prefs.getString(PREF_UNLOCKED_TIERS, "1") ?: "1"
        val unlocked = unlockedString.split(",").mapNotNull { it.toIntOrNull() }.toSet().ifEmpty { setOf(1) }

        val enabledMap = mutableMapOf<String, Boolean>()
        ALL_CAPABILITIES.forEach { item ->
            // Core items are always true, otherwise check pref (default to true)
            val isEnabled = if (item.isCore) true else prefs.getBoolean("feat_${item.key}", true)
            enabledMap[item.key] = isEnabled
        }

        val highestTier = if (isGod) 6 else (unlocked.maxOrNull() ?: 1)
        return LadderState(
            currentTier = highestTier,
            isGodMode = isGod,
            unlockedTiers = if (isGod) (1..6).toSet() else unlocked,
            enabledFeatures = enabledMap
        )
    }

    fun isFeatureEnabled(key: String): Boolean {
        val s = _state.value
        val item = ALL_CAPABILITIES.firstOrNull { it.key == key } ?: return true
        if (item.isCore) return true
        if (s.isGodMode) return s.enabledFeatures[key] ?: true

        // Must be in an unlocked tier AND toggled on
        val isTierUnlocked = s.unlockedTiers.contains(item.tier)
        return isTierUnlocked && (s.enabledFeatures[key] ?: true)
    }

    fun isTierUnlocked(tier: Int): Boolean {
        val s = _state.value
        return s.isGodMode || s.unlockedTiers.contains(tier)
    }

    fun toggleFeature(key: String, enabled: Boolean) {
        val item = ALL_CAPABILITIES.firstOrNull { it.key == key }
        if (item?.isCore == true) return // Cannot disable core

        prefs.edit().putBoolean("feat_$key", enabled).apply()
        _state.update { current ->
            val updated = current.enabledFeatures.toMutableMap()
            updated[key] = enabled
            current.copy(enabledFeatures = updated)
        }
    }

    fun setGodMode(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_GOD_MODE, enabled).apply()
        _state.update { current ->
            val highestTier = if (enabled) 6 else (current.unlockedTiers.maxOrNull() ?: 1)
            current.copy(
                isGodMode = enabled,
                currentTier = highestTier,
                unlockedTiers = if (enabled) (1..6).toSet() else current.unlockedTiers
            )
        }
    }

    fun evaluateProgress(activeNotesCount: Int) {
        val currentUnlocked = _state.value.unlockedTiers.toMutableSet()
        var newlyUnlockedTier: TierInfo? = null

        TIERS.forEach { tier ->
            if (activeNotesCount >= tier.requiredNotes && !currentUnlocked.contains(tier.tier)) {
                currentUnlocked.add(tier.tier)
                newlyUnlockedTier = tier
            }
        }

        val lastNotified = prefs.getInt(PREF_LAST_NOTIFIED_TIER, 1)
        if (newlyUnlockedTier != null && newlyUnlockedTier!!.tier > lastNotified) {
            prefs.edit().putInt(PREF_LAST_NOTIFIED_TIER, newlyUnlockedTier!!.tier).apply()
            _unlockEvents.tryEmit(newlyUnlockedTier!!)
        }

        prefs.edit().putString(PREF_UNLOCKED_TIERS, currentUnlocked.joinToString(",")).apply()
        _state.update { current ->
            val isGod = current.isGodMode
            val highest = if (isGod) 6 else (currentUnlocked.maxOrNull() ?: 1)
            current.copy(
                activeNotesCount = activeNotesCount,
                currentTier = highest,
                unlockedTiers = if (isGod) (1..6).toSet() else currentUnlocked,
                justUnlockedTier = newlyUnlockedTier
            )
        }
    }

    fun clearUnlockToast() {
        _state.update { it.copy(justUnlockedTier = null) }
    }

    fun getNextTierInfo(): Pair<TierInfo?, Int> {
        val s = _state.value
        val highest = s.unlockedTiers.maxOrNull() ?: 1
        if (highest >= 6) return Pair(null, 0)
        val next = TIERS.firstOrNull { it.tier == highest + 1 } ?: return Pair(null, 0)
        val remaining = maxOf(0, next.requiredNotes - s.activeNotesCount)
        return Pair(next, remaining)
    }
}

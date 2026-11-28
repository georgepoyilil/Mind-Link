package com.mindlink.journal.data.feedback

import android.content.Context
import android.content.SharedPreferences
import com.mindlink.journal.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FeatureProposal(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val timestamp: Long,
    val status: String = "Under Review"
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(timestamp))
}

class FeatureRequestStore(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("mindlink_feature_proposals", Context.MODE_PRIVATE)

    companion object {
        const val PREF_LAST_SUBMIT_TIME = "pref_last_submit_time"
        const val PREF_PROPOSALS_JSON = "pref_proposals_json"
        const val COOLDOWN_MILLIS = 7 * 24 * 60 * 60 * 1000L // 7 Days strict cooldown
        const val MIN_NOTES_REQUIRED = 5
        const val MIN_TIER_REQUIRED = 2
        const val MIN_DESCRIPTION_LENGTH = 25

        val CATEGORIES = listOf(
            "🎨 UI & Atmosphere",
            "🧠 Second Brain & AI",
            "⚡ Editor & Tasks",
            "☁️ Cloud & Privacy",
            "💡 Wild Creative Idea"
        )
    }

    fun checkEligibility(activeNotesCount: Int, currentTier: Int): Pair<Boolean, String> {
        if (activeNotesCount < MIN_NOTES_REQUIRED || currentTier < MIN_TIER_REQUIRED) {
            return Pair(
                false,
                "Creator Portal Locked: Write at least $MIN_NOTES_REQUIRED thoughts (Currently: $activeNotesCount/$MIN_NOTES_REQUIRED) and reach Tier $MIN_TIER_REQUIRED to unlock feature requests. We prioritize feedback from committed thinkers."
            )
        }

        val lastSubmit = prefs.getLong(PREF_LAST_SUBMIT_TIME, 0L)
        val now = System.currentTimeMillis()
        val elapsed = now - lastSubmit
        if (lastSubmit > 0 && elapsed < COOLDOWN_MILLIS) {
            val remainingMillis = COOLDOWN_MILLIS - elapsed
            val days = remainingMillis / (24 * 60 * 60 * 1000L)
            val hours = (remainingMillis % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L)
            return Pair(
                false,
                "Cooldown Active: Next proposal unlocks in $days days, $hours hrs. MindLink enforces a 7-day cooldown to ensure every proposal is given full engineering consideration."
            )
        }

        return Pair(true, "Eligible to submit a creative feature proposal.")
    }

    fun submitProposal(title: String, description: String, category: String): Result<FeatureProposal> {
        if (title.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be empty"))
        if (description.trim().length < MIN_DESCRIPTION_LENGTH) {
            return Result.failure(IllegalArgumentException("Description must be at least $MIN_DESCRIPTION_LENGTH characters so we can understand your vision."))
        }

        val proposal = FeatureProposal(
            id = UUID.randomUUID().toString().take(8),
            title = title.trim(),
            description = description.trim(),
            category = category,
            timestamp = System.currentTimeMillis(),
            status = "Under Review"
        )

        // Save proposal to local archive
        val existing = getProposals().toMutableList()
        existing.add(0, proposal)
        saveProposals(existing)

        // Record cooldown timestamp
        prefs.edit().putLong(PREF_LAST_SUBMIT_TIME, System.currentTimeMillis()).apply()

        return Result.success(proposal)
    }

    fun getProposals(): List<FeatureProposal> {
        val json = prefs.getString(PREF_PROPOSALS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<FeatureProposal>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FeatureProposal(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        category = obj.getString("category"),
                        timestamp = obj.getLong("timestamp"),
                        status = obj.optString("status", "Under Review")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveProposals(list: List<FeatureProposal>) {
        val arr = JSONArray()
        list.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("description", p.description)
                put("category", p.category)
                put("timestamp", p.timestamp)
                put("status", p.status)
            }
            arr.put(obj)
        }
        prefs.edit().putString(PREF_PROPOSALS_JSON, arr.toString()).apply()
    }

    fun formatEmailBody(proposal: FeatureProposal, activeNotesCount: Int, currentTier: Int): String {
        return """
            Hi George,
            
            I have a creative feature proposal for MindLink!
            
            Proposal: ${proposal.title}
            Category: ${proposal.category}
            
            Description:
            ${proposal.description}
            
            ---
            User Context:
            - MindLink Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
            - Progression Tier: Tier $currentTier
            - Active Thoughts: $activeNotesCount
            - Proposal ID: #${proposal.id}
        """.trimIndent()
    }
}

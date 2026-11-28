package com.mindlink.journal

import com.mindlink.journal.data.capability.CapabilityLadderManager
import com.mindlink.journal.data.capability.LadderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityLadderStateTest {

    @Test
    fun levelZeroOnlyExposesCoreCapabilities() {
        val state = LadderState(currentTier = 0, unlockedTiers = setOf(0))

        // Level 0 core always enabled
        assertTrue(state.isEnabled(CapabilityLadderManager.FEATURE_BASIC_JOURNAL))
        assertTrue(state.isEnabled(CapabilityLadderManager.FEATURE_KEYWORD_SEARCH))
        assertTrue(state.isEnabled(CapabilityLadderManager.FEATURE_THEMES))

        // Level 1..5 features all OFF in Level 0
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_RICH_TEXT))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_HASHTAGS))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_TYPOGRAPHY))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_MEDIA_CAPTURE))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_VERSION_HISTORY))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_SECOND_BRAIN))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_ON_DEVICE_SLM))
    }

    @Test
    fun featureByFeatureOverrideWorksIndependently() {
        val allL1Enabled = CapabilityLadderManager.ALL_CAPABILITIES
            .filter { !it.isCore && it.tier <= 1 }
            .associate { it.key to true }
            .toMutableMap()
        // Disable just Checklists inside Level 1 while leaving Rich Text and Tags ON
        allL1Enabled[CapabilityLadderManager.FEATURE_CHECKLISTS] = false

        val state = LadderState(
            currentTier = 1,
            unlockedTiers = setOf(0, 1),
            enabledFeatures = allL1Enabled
        )

        assertTrue(state.isEnabled(CapabilityLadderManager.FEATURE_RICH_TEXT))
        assertTrue(state.isEnabled(CapabilityLadderManager.FEATURE_HASHTAGS))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_CHECKLISTS))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_VERSION_HISTORY))

        val (activeL1, totalL1) = state.activeCountForLevel(1)
        assertEquals(5, totalL1)
        assertEquals(4, activeL1)
    }

    @Test
    fun coreCapabilitiesCannotBeDisabled() {
        val state = LadderState(
            currentTier = 0,
            enabledFeatures = mapOf(CapabilityLadderManager.FEATURE_BASIC_JOURNAL to false)
        )
        assertTrue(state.isEnabled(CapabilityLadderManager.FEATURE_BASIC_JOURNAL))
    }

    @Test
    fun levelThresholdOrderingIsStrictlyIncreasing() {
        val levels = CapabilityLadderManager.TIERS
        assertEquals(listOf(0, 1, 2, 3, 4, 5), levels.map { it.tier })
        for (i in 1 until levels.size) {
            assertTrue(levels[i].requiredNotes > levels[i - 1].requiredNotes)
        }
    }
}

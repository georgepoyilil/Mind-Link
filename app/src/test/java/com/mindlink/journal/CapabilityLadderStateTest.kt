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

        // Level 1..5 gated out completely
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_HASHTAGS))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_TYPOGRAPHY))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_MEDIA_CAPTURE))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_CLOUD_BACKUP))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_SECOND_BRAIN))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_ON_DEVICE_SLM))
    }

    @Test
    fun togglingOffUnlockedFeatureRemovesItEvenInGodMode() {
        val state = LadderState(
            currentTier = 5,
            isGodMode = true,
            unlockedTiers = (0..5).toSet(),
            enabledFeatures = mapOf(
                CapabilityLadderManager.FEATURE_JESTER to false,
                CapabilityLadderManager.FEATURE_TECH_MODE to false
            )
        )

        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_JESTER))
        assertFalse(state.isEnabled(CapabilityLadderManager.FEATURE_TECH_MODE))
        assertTrue(state.isEnabled(CapabilityLadderManager.FEATURE_SECOND_BRAIN))
        assertTrue(state.isEnabled(CapabilityLadderManager.FEATURE_ON_DEVICE_SLM))
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
    fun levelModuleCountsAndThresholdOrderAreConsistent() {
        val levels = CapabilityLadderManager.TIERS
        assertEquals(listOf(0, 1, 2, 3, 4, 5), levels.map { it.tier })
        for (i in 1 until levels.size) {
            assertTrue(levels[i].requiredNotes > levels[i - 1].requiredNotes)
        }

        val state = LadderState(
            currentTier = 1,
            unlockedTiers = setOf(0, 1),
            enabledFeatures = mapOf(CapabilityLadderManager.FEATURE_CHECKLISTS to false)
        )
        val (activeL1, totalL1) = state.activeCountForLevel(1)
        assertEquals(4, totalL1)
        assertEquals(3, activeL1)
    }
}

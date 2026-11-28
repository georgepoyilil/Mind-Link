package com.mindlink.journal

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mindlink.journal.data.capability.CapabilityLadderManager
import com.mindlink.journal.data.capability.LadderState
import com.mindlink.journal.di.ServiceLocator
import com.mindlink.journal.ui.components.LiquidGlassAtmosphere
import com.mindlink.journal.ui.navigation.MindLinkNavHost
import com.mindlink.journal.ui.theme.MindLinkTheme
import com.mindlink.journal.ui.theme.MindTheme

val LocalTechMode = compositionLocalOf { false }
val LocalLadderState = compositionLocalOf { LadderState() }

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val serviceLocator = ServiceLocator.getInstance(applicationContext)

        setContent {
            val globalFontScale by serviceLocator.globalFontScale.collectAsState()
            val isTechMode by serviceLocator.techMode.collectAsState()
            val appTheme by serviceLocator.appTheme.collectAsState()
            val showEmojis by serviceLocator.showEmojis.collectAsState()
            val lineSpacingMultiplier by serviceLocator.lineSpacingMultiplier.collectAsState()
            val ladderState by serviceLocator.capabilityLadderManager.state.collectAsState()

            val effectiveTechMode = isTechMode && ladderState.isEnabled(CapabilityLadderManager.FEATURE_TECH_MODE)
            val effectiveShowEmojis = showEmojis && ladderState.isEnabled(CapabilityLadderManager.FEATURE_MOOD_BADGES)

            val mindTheme = remember(appTheme) { MindTheme.fromId(appTheme) }

            // Dynamic edge-to-edge system bar contrast
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !mindTheme.isDark
                    insetsController.isAppearanceLightNavigationBars = !mindTheme.isDark
                }
            }

            CompositionLocalProvider(
                LocalTechMode provides effectiveTechMode,
                LocalLadderState provides ladderState
            ) {
                MindLinkTheme(
                    themeKey = appTheme,
                    fontScale = if (ladderState.isEnabled(CapabilityLadderManager.FEATURE_TYPOGRAPHY)) globalFontScale else 1.0f,
                    showEmojis = effectiveShowEmojis,
                    lineSpacingMultiplier = if (ladderState.isEnabled(CapabilityLadderManager.FEATURE_TYPOGRAPHY)) lineSpacingMultiplier else 1.45f
                ) {
                    LiquidGlassAtmosphere(
                        isDark = mindTheme.isDark,
                        isGlassy = mindTheme.isGlassy
                    ) {
                        MindLinkNavHost()
                    }
                }
            }
        }
    }
}

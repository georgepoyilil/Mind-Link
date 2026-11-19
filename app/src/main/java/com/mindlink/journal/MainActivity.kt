package com.mindlink.journal

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.mindlink.journal.di.ServiceLocator
import com.mindlink.journal.ui.navigation.MindLinkNavHost
import com.mindlink.journal.ui.theme.MindLinkTheme
import com.mindlink.journal.ui.theme.MindTheme

val LocalTechMode = compositionLocalOf { false }

class MainActivity : ComponentActivity() {

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

            CompositionLocalProvider(LocalTechMode provides isTechMode) {
                MindLinkTheme(
                    themeKey = appTheme,
                    fontScale = globalFontScale,
                    showEmojis = showEmojis,
                    lineSpacingMultiplier = lineSpacingMultiplier
                ) {
                    MindLinkNavHost()
                }
            }
        }
    }
}

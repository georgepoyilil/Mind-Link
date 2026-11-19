package com.mindlink.journal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import com.mindlink.journal.di.ServiceLocator
import com.mindlink.journal.ui.navigation.MindLinkNavHost
import com.mindlink.journal.ui.theme.MindLinkTheme

val LocalTechMode = compositionLocalOf { false }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val serviceLocator = ServiceLocator.getInstance(applicationContext)

        setContent {
            val globalFontScale by serviceLocator.globalFontScale.collectAsState()
            val isTechMode by serviceLocator.techMode.collectAsState()

            CompositionLocalProvider(LocalTechMode provides isTechMode) {
                MindLinkTheme(fontScale = globalFontScale) {
                    MindLinkNavHost()
                }
            }
        }
    }
}

package com.mindlink.journal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mindlink.journal.ui.navigation.MindLinkNavHost
import com.mindlink.journal.ui.theme.MindLinkTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MindLinkTheme {
                MindLinkNavHost()
            }
        }
    }
}

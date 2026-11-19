package com.mindlink.journal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mindlink.journal.ui.theme.*

@Composable
fun MindLinkOnboardingDialog(
    onDismiss: () -> Unit,
    onLoadSamples: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MindSurfaceAmoled,
            border = BorderStroke(1.dp, MindGlassButtonBorder),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PastelLavender.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.35f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PastelLavender,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome to MindLink",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Text(
                            text = "Your Associative Second Brain",
                            style = MaterialTheme.typography.bodySmall,
                            color = PastelLavender
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Scrollable features list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OnboardingFeatureCard(
                        icon = Icons.Default.EditNote,
                        iconColor = PastelLavender,
                        title = "1. Mindful Capture",
                        description = "Markdown notes, live speech dictation, high-fidelity local voice memos, and smart OCR document scanning that cleans and structures text."
                    )
                    OnboardingFeatureCard(
                        icon = Icons.Default.Hub,
                        iconColor = PastelSky,
                        title = "2. Synaptic Knowledge Graph",
                        description = "Interconnect thoughts automatically with #tags and [[Wikilinks]]. Explore relationships in an interactive 2D physics graph."
                    )
                    OnboardingFeatureCard(
                        icon = Icons.Default.Memory,
                        iconColor = PastelViolet,
                        title = "3. Second Brain Inquiries",
                        description = "Ask questions across all past notes. Synthesize insights using on-device SLMs (Qwen 2.5 / SmolLM2) or Cloud Gemini."
                    )
                    OnboardingFeatureCard(
                        icon = Icons.Default.Security,
                        iconColor = PastelMint,
                        title = "4. 100% Offline & Private",
                        description = "All thoughts, vector embeddings, and media are stored locally in Room SQLite. Zero trackers, zero cloud dependencies."
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PastelLavender,
                            contentColor = MindBackgroundAmoled
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (onLoadSamples != null) "Get Started (Clean Slate)" else "Close Intro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    if (onLoadSamples != null) {
                        OutlinedButton(
                            onClick = onLoadSamples,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PastelSky
                            ),
                            border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Load 8 Sample Thoughts",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingFeatureCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MindGlassSurface,
        border = BorderStroke(1.dp, MindPrismGlassCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, iconColor.copy(alpha = 0.35f)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MindTextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MindTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

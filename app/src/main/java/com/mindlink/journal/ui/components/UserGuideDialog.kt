package com.mindlink.journal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mindlink.journal.ui.theme.*

@Composable
fun MindLinkUserGuideDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MindSurfaceAmoled,
            border = BorderStroke(1.dp, MindGlassButtonBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PastelSky.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.35f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = PastelSky,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MindLink User Guide",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Text(
                            text = "Instructions & Core Capabilities",
                            style = MaterialTheme.typography.bodySmall,
                            color = PastelSky
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MindTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Instructions Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GuideSectionCard(
                        icon = Icons.Default.EditNote,
                        iconColor = PastelLavender,
                        title = "1. Capturing Thoughts & Media",
                        initialExpanded = true,
                        content = """
• Journal Thoughts: Write freely with full Markdown support (# Headers, **bold**, *italic*, bullet points, and - [ ] checkboxes). Attach photos with captions.

• Live Voice Dictation: Tap the mic icon in the editor toolbar to speak naturally. Speech is transcribed in real-time into the note body.

• Voice Audio Notes: Record audio notes with crystal-clear local encoding. Recorded memos can be replayed with scrubber controls and duration indicators directly in note details.

• Camera Document & OCR Scanner: Point your camera at whiteboards, books, or documents. MindLink's OCR engine automatically strips status bar clocks, battery levels, network indicators, camera watermarks, and reformats into clean paragraphs.
                        """.trimIndent()
                    )

                    GuideSectionCard(
                        icon = Icons.Default.Hub,
                        iconColor = PastelSky,
                        title = "2. Associative Linking & Graph",
                        initialExpanded = false,
                        content = """
• Bi-directional Wikilinks: Type [[Note Title]] anywhere inside a note to create an associative link. Tapping a wikilink instantly opens that note.

• #Tags: Organize by themes using tags (e.g. #ideas, #wellness, #engineering). Filter notes easily by tapping tags.

• Synaptic Knowledge Graph: Tap the Graph icon on the home screen to visualize your knowledge base as an interactive 2D physics network. Pinch to zoom, drag nodes, and explore cluster connections.
                        """.trimIndent()
                    )

                    GuideSectionCard(
                        icon = Icons.Default.Psychology,
                        iconColor = PastelViolet,
                        title = "3. Second Brain Inquiries",
                        initialExpanded = false,
                        content = """
• Asking Questions: Tap the Brain icon on the home screen. Inquire about anything across your past thoughts (e.g., "What were my major breakthroughs this week?", "Summarize my book notes").

• Hybrid Search: Combines exact keyword matching (SQLite FTS) with 384-dimensional semantic vector embeddings to surface conceptually relevant thoughts even with different wording.

• Dynamic Synthesis: Synthesizes patterns, recurring themes, and actionable reflections derived directly from your notes.
                        """.trimIndent()
                    )

                    GuideSectionCard(
                        icon = Icons.Default.Memory,
                        iconColor = PastelMint,
                        title = "4. AI Engines & SLM Setup",
                        initialExpanded = false,
                        content = """
• Auto Mode: Automatically selects the best available engine. Prefers on-device SLM if installed, falls back to Gemini if API key is configured, or instant Local NLP Heuristics.

• On-Device SLM (SmolLM2 360M / Qwen 2.5 0.5B): 100% offline, GPU-accelerated language models running via Google MediaPipe Tasks GenAI. Download once in Settings.

• Gemini Cloud: Optional Google AI Studio API key for deep reasoning. API keys are encrypted securely using Android Keystore.

• Local NLP Heuristics: Zero battery drain, zero downloads. Extracts tags, action items, and reflections using intelligent local rule engines.
                        """.trimIndent()
                    )

                    GuideSectionCard(
                        icon = Icons.Default.FormatSize,
                        iconColor = PastelPeach,
                        title = "5. Typography & Font Scaling",
                        initialExpanded = false,
                        content = """
• Global Font Scaling: Adjust the entire app typography in Settings (Small 0.85x, Default 1.0x, Large 1.15x, Huge 1.30x).

• Per-Note Override: Need bigger text while drafting or reading a specific note? Use the font pills (App, S, M, L, XL) in the note editor or detail toolbar to customize individual notes.
                        """.trimIndent()
                    )

                    GuideSectionCard(
                        icon = Icons.Default.SystemUpdate,
                        iconColor = PastelRose,
                        title = "6. In-App OTA Updates & Privacy",
                        initialExpanded = false,
                        content = """
• In-App OTA Updates: MindLink checks GitHub Releases directly. Download and install new updates seamlessly from Settings without manual browser downloads.

• 100% Local-First: All notes, voice recordings, and vector embeddings are stored locally in Room SQLite. Zero trackers, zero analytics, complete data ownership.
                        """.trimIndent()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PastelSky,
                        contentColor = MindBackgroundAmoled
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Got It",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GuideSectionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    content: String,
    initialExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MindGlassSurface,
        border = BorderStroke(1.dp, MindPrismGlassCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, iconColor.copy(alpha = 0.35f)),
                    modifier = Modifier.size(36.dp)
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MindTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MindTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = MindBorderDark.copy(alpha = 0.5f),
                        thickness = 0.8.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MindTextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

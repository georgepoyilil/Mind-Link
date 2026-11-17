package com.mindlink.journal.ui.secondbrain.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.domain.model.SynthesisResponse
import com.mindlink.journal.ui.theme.*

@Composable
fun ConversationalSynthesisCard(
    synthesis: SynthesisResponse,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(1.5.dp, MindAiGlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MindAiGlassGradient)
                .padding(20.dp)
        ) {
            // Distinct AI Synthesis Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MindGlassButtonContainer,
                    border = BorderStroke(1.dp, MindGlassButtonBorder),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = PastelSky, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AI Brain Synthesis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MindGlassButtonContainer
                        ) {
                            Text(
                                text = "ON-DEVICE AI",
                                color = PastelLavender,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Cross-referenced synthesis from your journal memory bank",
                        style = MaterialTheme.typography.labelSmall,
                        color = MindTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Synthesized Answer Body
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MindGlassSurfaceElevated,
                border = BorderStroke(1.dp, MindGlassBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = synthesis.synthesisText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MindTextPrimary,
                    modifier = Modifier.padding(16.dp),
                    lineHeight = 24.sp
                )
            }

            // Proactive Wellness & Pattern Reminder Card
            if (synthesis.suggestedReminder != null) {
                var isReminderScheduled by remember { mutableStateOf(false) }
                var isReminderDismissed by remember { mutableStateOf(false) }

                if (!isReminderDismissed) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PastelAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.2.dp, PastelAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = PastelAmber.copy(alpha = 0.25f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = PastelAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = synthesis.patternAlert ?: "Pattern Alert & Suggested Reminder",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelAmber
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = synthesis.suggestedReminder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MindTextPrimary,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { isReminderDismissed = true }) {
                                    Text("Dismiss", color = MindTextTertiary, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isReminderScheduled) PastelMint.copy(alpha = 0.2f) else MindGlassButtonContainer,
                                    border = BorderStroke(1.dp, if (isReminderScheduled) PastelMint else MindGlassButtonBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { isReminderScheduled = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isReminderScheduled) Icons.Default.Check else Icons.Default.Alarm,
                                            contentDescription = null,
                                            tint = if (isReminderScheduled) PastelMint else PastelAmber,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isReminderScheduled) "Reminder Scheduled" else "Set Reminder",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isReminderScheduled) PastelMint else MindGlassButtonText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Key Takeaways Section
            if (synthesis.keyTakeaways.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = PastelViolet, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Key Patterns & Realizations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PastelViolet
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    synthesis.keyTakeaways.forEach { takeaway ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MindGlassSurfaceVariant,
                            border = BorderStroke(0.5.dp, MindGlassBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.ArrowRight, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = takeaway,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MindTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Cited Notes Section
            if (synthesis.citedNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = MindTextTertiary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Source Memories Cited (${synthesis.citedNotes.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MindTextTertiary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    synthesis.citedNotes.forEach { note ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MindGlassSurfaceVariant,
                            border = BorderStroke(0.5.dp, MindGlassBorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onNoteClick(note.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(PastelLavender, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MindTextPrimary,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Note",
                                    tint = MindTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

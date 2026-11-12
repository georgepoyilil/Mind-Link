package com.mindlink.journal.ui.notes.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mindlink.journal.domain.model.Note
import com.mindlink.journal.ui.theme.*

enum class NoteViewMode(val label: String, val icon: ImageVector) {
    CARDS("Cards", Icons.Default.GridView),
    COMPACT_LIST("List", Icons.Default.ViewAgenda),
    FULL_WIDTH("Stream", Icons.Default.ViewStream)
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    viewMode: NoteViewMode = NoteViewMode.CARDS,
    modifier: Modifier = Modifier
) {
    when (viewMode) {
        NoteViewMode.COMPACT_LIST -> NoteCompactItem(note = note, onClick = onClick, modifier = modifier)
        NoteViewMode.FULL_WIDTH -> NoteFullWidthCard(note = note, onClick = onClick, modifier = modifier)
        NoteViewMode.CARDS -> NoteStandardCard(note = note, onClick = onClick, modifier = modifier)
    }
}

/**
 * Standard Card View: Balanced preview with tags and mood chip
 */
@Composable
fun NoteStandardCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MindGlassSurface),
        border = BorderStroke(1.dp, MindPrismGlassCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Mood Chip & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.mood != null) {
                    MoodBadge(mood = note.mood, emoji = note.moodEmoji)
                } else {
                    Box(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.audioUri != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MindIndigo.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "🎙️",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp
                            )
                        }
                    }

                    if (note.imageUri != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MindCyan.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "📷",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp
                            )
                        }
                    }

                    Text(
                        text = note.formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MindTextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = note.title.ifBlank { "Untitled Thought" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MindTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // AI TL;DR or Preview
            Text(
                text = note.previewText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (note.summary != null) MindTextSecondary else MindTextTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Action items count + Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    note.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MindGlassSurfaceVariant,
                            border = BorderStroke(0.5.dp, MindGlassBorderSubtle)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextSecondary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                if (note.actionItems.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = MindEmerald.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "✅ ${note.actionItems.size} tasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MindEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact List View: Sleek, dense rows for rapid skimming
 */
@Composable
fun NoteCompactItem(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MindGlassSurface,
        border = BorderStroke(1.dp, MindPrismGlassCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = note.moodEmoji, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = note.title.ifBlank { "Untitled Thought" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MindTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = note.formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MindTextTertiary
                        )
                        if (note.tags.isNotEmpty()) {
                            Text(
                                text = " · #${note.tags.first()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindIndigo,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (note.actionItems.isNotEmpty()) {
                            Text(
                                text = " · ✅ ${note.actionItems.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindEmerald
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MindTextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Full Width Stream View: Immersive card with image preview and full summary
 */
@Composable
fun NoteFullWidthCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MindGlassSurface),
        border = BorderStroke(1.dp, MindPrismBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Optional Header Image
            if (note.imageUri != null) {
                AsyncImage(
                    model = note.imageUri,
                    contentDescription = note.imageCaption ?: note.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                // Header: Mood badge & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.mood != null) {
                        MoodBadge(mood = note.mood, emoji = note.moodEmoji)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.audioUri != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MindIndigo.copy(alpha = 0.2f),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "🎙️ ${note.formattedAudioDuration}",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    fontSize = 9.5.sp,
                                    color = MindIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = note.formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MindTextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title
                Text(
                    text = note.title.ifBlank { "Untitled Thought" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MindTextPrimary
                )

                // Image Caption if present
                if (!note.imageCaption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📷 ${note.imageCaption}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MindCyan,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Full Preview or Summary
                Text(
                    text = note.previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindTextSecondary,
                    lineHeight = 22.sp
                )

                // Action items teaser
                if (note.actionItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MindEmerald.copy(alpha = 0.1f),
                        border = BorderStroke(0.5.dp, MindEmerald.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "✅ Action Items (${note.actionItems.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            note.actionItems.take(2).forEach { action ->
                                Text(
                                    text = "• $action",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MindTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    note.tags.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MindGlassSurfaceVariant,
                            border = BorderStroke(0.5.dp, MindGlassBorderSubtle)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodBadge(mood: String, emoji: String) {
    val (bgColor, textColor) = when (mood.lowercase().trim()) {
        "reflective" -> MindIndigo.copy(alpha = 0.2f) to MindIndigo
        "analytical" -> MindCyan.copy(alpha = 0.2f) to MindCyan
        "creative" -> MindPink.copy(alpha = 0.2f) to MindPink
        "energized" -> MindAmber.copy(alpha = 0.2f) to MindAmber
        "stressed" -> MindRose.copy(alpha = 0.2f) to MindRose
        "calm" -> MindEmerald.copy(alpha = 0.2f) to MindEmerald
        "vulnerable" -> MindRose.copy(alpha = 0.25f) to MindRose
        "grateful" -> MindPink.copy(alpha = 0.25f) to MindPink
        else -> MindEmerald.copy(alpha = 0.2f) to MindEmerald
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = mood,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

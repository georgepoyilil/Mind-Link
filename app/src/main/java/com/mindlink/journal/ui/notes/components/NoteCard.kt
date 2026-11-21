package com.mindlink.journal.ui.notes.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mindlink.journal.domain.model.Note
import com.mindlink.journal.ui.components.liquidGlass
import com.mindlink.journal.ui.theme.*

enum class NoteViewMode(val label: String, val icon: ImageVector) {
    GRID("Grid", Icons.Default.ViewAgenda),
    CARDS("Cards", Icons.Default.GridView),
    COMPACT_LIST("List", Icons.Default.Dashboard)
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    viewMode: NoteViewMode = NoteViewMode.GRID,
    modifier: Modifier = Modifier
) {
    when (viewMode) {
        NoteViewMode.COMPACT_LIST -> NoteCompactItem(note = note, onClick = onClick, modifier = modifier)
        NoteViewMode.CARDS -> NoteFullWidthCard(note = note, onClick = onClick, modifier = modifier)
        NoteViewMode.GRID -> NoteStandardCard(note = note, onClick = onClick, modifier = modifier)
    }
}

/**
 * Standard Card View: Balanced preview with tags and mood chip, optimized for 2-column staggered Google Keep grid
 */
@Composable
fun NoteStandardCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardFont = resolveNoteFontFamily(note.fontFamily)
    val isGlassy = MindIsGlassy
    val isDark = LocalMindLinkColors.current.isDark
    val cardShape = RoundedCornerShape(14.dp)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = cardShape,
                cornerRadius = 14.dp,
                isGlassy = isGlassy,
                isDark = isDark,
                cardBackground = MindGlassCardBackground,
                cardBorder = MindGlassCardBorder
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = if (isGlassy) Color.Transparent else MindGlassSurface),
        border = if (isGlassy) null else BorderStroke(1.2.dp, MindPrismGlassCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Optional attached image header (Google Keep style) - hidden if locked
            if (note.imageUri != null && !note.isLocked) {
                AsyncImage(
                    model = note.imageUri,
                    contentDescription = "Attached Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(11.dp)
            ) {
                // Header Row: Mood Chip / Lock Badge & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.isLocked) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MindIndigo.copy(alpha = 0.16f),
                            border = BorderStroke(0.6.dp, MindIndigo.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MindIndigo,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Locked",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MindIndigo
                                )
                            }
                        }
                    } else if (note.mood != null) {
                        MoodBadge(mood = note.mood, emoji = note.moodEmoji)
                    } else {
                        Box(modifier = Modifier.width(1.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.audioUri != null && !note.isLocked) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MindGlassButtonContainer,
                                border = BorderStroke(0.5.dp, MindGlassButtonBorder),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Note",
                                    tint = PastelLavender,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .size(10.dp)
                                )
                            }
                        }

                        Text(
                            text = note.compactDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MindTextTertiary,
                            fontSize = 10.5.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title
                Text(
                    text = note.title.ifBlank { "Untitled Thought" },
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = cardFont),
                    fontWeight = FontWeight.Bold,
                    color = MindTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (note.isLocked) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MindGlassSurfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MindTextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tap to unlock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MindTextTertiary,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))

                    // AI TL;DR or Body Preview (Staggered natural text wrap)
                    Text(
                        text = note.previewText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = cardFont),
                        color = if (note.summary != null) MindTextSecondary else MindTextTertiary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (note.tags.isNotEmpty() || note.actionItems.isNotEmpty()) {
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
                                note.tags.take(2).forEach { tag ->
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
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (note.actionItems.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(
                                            color = PastelMint.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TaskAlt,
                                        contentDescription = null,
                                        tint = PastelMint,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${note.actionItems.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PastelMint,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
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
    val isGlassy = MindIsGlassy
    val isDark = LocalMindLinkColors.current.isDark
    val compactShape = RoundedCornerShape(10.dp)

    Surface(
        onClick = onClick,
        shape = compactShape,
        color = if (isGlassy) Color.Transparent else MindGlassSurface,
        border = if (isGlassy) null else BorderStroke(1.dp, MindPrismGlassCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = compactShape,
                cornerRadius = 10.dp,
                isGlassy = isGlassy,
                isDark = isDark,
                cardBackground = MindGlassCardBackground,
                cardBorder = MindGlassCardBorder
            )
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
                if (note.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MindIndigo,
                        modifier = Modifier.size(13.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(getMoodColor(note.mood), CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
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
                            text = if (note.isLocked) "${note.formattedDate} · 🔒 Protected" else note.formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MindTextTertiary
                        )
                        if (!note.isLocked) {
                            if (note.tags.isNotEmpty()) {
                                Text(
                                    text = " · #${note.tags.first()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindIndigo,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (note.actionItems.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = " · ", style = MaterialTheme.typography.labelSmall, color = MindTextTertiary)
                                    Icon(
                                        imageVector = Icons.Default.TaskAlt,
                                        contentDescription = null,
                                        tint = PastelMint,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${note.actionItems.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PastelMint,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
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
    val isGlassy = MindIsGlassy
    val isDark = LocalMindLinkColors.current.isDark
    val fullShape = RoundedCornerShape(16.dp)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = fullShape,
                cornerRadius = 16.dp,
                isGlassy = isGlassy,
                isDark = isDark,
                cardBackground = MindGlassCardBackground,
                cardBorder = MindGlassCardBorder
            ),
        shape = fullShape,
        colors = CardDefaults.cardColors(containerColor = if (isGlassy) Color.Transparent else MindGlassSurface),
        border = if (isGlassy) null else BorderStroke(1.dp, MindPrismBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isGlassy) 0.dp else 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Optional Header Image (hidden if locked)
            if (note.imageUri != null && !note.isLocked) {
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
                // Header: Mood badge / Lock & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.isLocked) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MindIndigo.copy(alpha = 0.16f),
                            border = BorderStroke(0.6.dp, MindIndigo.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MindIndigo,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Protected Thought",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MindIndigo
                                )
                            }
                        }
                    } else if (note.mood != null) {
                        MoodBadge(mood = note.mood, emoji = note.moodEmoji)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.audioUri != null && !note.isLocked) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MindGlassButtonContainer,
                                border = BorderStroke(0.5.dp, MindGlassButtonBorder),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = PastelLavender,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = note.formattedAudioDuration,
                                        fontSize = 10.sp,
                                        color = PastelLavender,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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

                if (note.isLocked) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MindIndigo.copy(alpha = 0.10f),
                        border = BorderStroke(0.6.dp, MindIndigo.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MindIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Biometric protected thought. Tap to unlock.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MindIndigo,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Image Caption if present
                    if (!note.imageCaption.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = PastelSky,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = note.imageCaption,
                                style = MaterialTheme.typography.bodySmall,
                                color = PastelSky,
                                fontWeight = FontWeight.Medium
                            )
                        }
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
                            color = PastelMint.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, PastelMint.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TaskAlt,
                                        contentDescription = null,
                                        tint = PastelMint,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Action Items (${note.actionItems.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PastelMint,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                note.actionItems.take(2).forEach { rawAction ->
                                    val isDone = rawAction.startsWith("[x] ", ignoreCase = true) || rawAction.startsWith("x ", ignoreCase = true)
                                    val actionText = rawAction
                                        .removePrefix("[ ] ")
                                        .removePrefix("[x] ")
                                        .removePrefix("[X] ")
                                        .removePrefix("• ")
                                        .trim()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clip(RoundedCornerShape(3.5.dp))
                                                .background(if (isDone) PastelMint.copy(alpha = 0.25f) else Color.Transparent)
                                            .border(
                                                width = 1.2.dp,
                                                color = if (isDone) PastelMint else MindTextTertiary,
                                                shape = RoundedCornerShape(3.5.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = PastelMint,
                                                modifier = Modifier.size(9.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = actionText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDone) MindTextTertiary else MindTextPrimary,
                                        textDecoration = if (isDone) TextDecoration.LineThrough else null,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Tags (FlowRow with no-wrap pills)
                if (note.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        note.tags.take(5).forEach { tag ->
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
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun MoodBadge(mood: String, emoji: String = "") {
    val showEmojis = LocalShowEmojis.current
    val isDark = LocalMindLinkColors.current.isDark
    val textColor = getMoodColor(mood, isDark)
    val bgColor = textColor.copy(alpha = if (isDark) 0.16f else 0.12f)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(0.6.dp, textColor.copy(alpha = if (isDark) 0.45f else 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showEmojis && emoji.isNotBlank()) {
                Text(text = emoji, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(textColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = mood,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun getMoodColor(mood: String?, isDark: Boolean = true): Color {
    return if (isDark) {
        when (mood?.lowercase()?.trim()) {
            "reflective" -> PastelLavender
            "analytical" -> PastelSky
            "creative" -> PastelViolet
            "energized" -> PastelAmber
            "stressed" -> PastelRose
            "calm" -> PastelMint
            "vulnerable" -> PastelPeach
            "grateful" -> PastelRose
            else -> PastelMint
        }
    } else {
        when (mood?.lowercase()?.trim()) {
            "reflective" -> Color(0xFF4F46E5)
            "analytical" -> Color(0xFF0284C7)
            "creative" -> Color(0xFF7C3AED)
            "energized" -> Color(0xFFD97706)
            "stressed" -> Color(0xFFE11D48)
            "calm" -> Color(0xFF059669)
            "vulnerable" -> Color(0xFFEA580C)
            "grateful" -> Color(0xFFDB2777)
            else -> Color(0xFF059669)
        }
    }
}

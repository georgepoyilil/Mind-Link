package com.mindlink.journal.ui.detail

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.mindlink.journal.ui.detail.components.ReflectionPromptsCard
import com.mindlink.journal.ui.detail.components.RelatedThoughtsCard
import com.mindlink.journal.ui.notes.components.MoodBadge
import com.mindlink.journal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onThoughtClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isFullScreenImageOpen by remember { mutableStateOf(false) }

    // Audio Playback state
    var isPlayingAudio by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Thought", color = MindTextPrimary) },
            text = { Text("Are you sure you want to delete this entry and its semantic embeddings from your Second Brain?", color = MindTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteNote { onNavigateBack() }
                    }
                ) {
                    Text("Delete", color = MindRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = MindTextSecondary)
                }
            },
            containerColor = MindGlassSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    val note = uiState.note

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thought Detail", color = MindTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MindTextPrimary)
                    }
                },
                actions = {
                    if (note != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MindRose)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MindBackgroundDark
                )
            )
        },
        containerColor = MindBackgroundDark
    ) { paddingValues ->
        if (note == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MindIndigo)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header: Mood badge & Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (note.mood != null) {
                            MoodBadge(mood = note.mood, emoji = note.moodEmoji)
                        }
                        Text(
                            text = note.formattedDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = MindTextTertiary
                        )
                    }

                    // Title
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MindTextPrimary
                    )

                    // Attached Voice Audio Player Bar
                    if (note.audioUri != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MindGlassSurfaceElevated,
                            border = BorderStroke(1.dp, MindIndigo.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isPlayingAudio) MindRose else MindIndigo,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                try {
                                                    if (isPlayingAudio) {
                                                        mediaPlayer?.pause()
                                                        isPlayingAudio = false
                                                    } else {
                                                        if (mediaPlayer == null) {
                                                            val player = MediaPlayer.create(context, Uri.parse(note.audioUri))
                                                            player.setOnCompletionListener {
                                                                isPlayingAudio = false
                                                            }
                                                            mediaPlayer = player
                                                        }
                                                        mediaPlayer?.start()
                                                        isPlayingAudio = true
                                                    }
                                                } catch (e: Exception) {
                                                    isPlayingAudio = false
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isPlayingAudio) "⏸️" else "▶️",
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isPlayingAudio) "Playing Voice Note..." else "Recorded Voice Note",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MindTextPrimary
                                        )
                                        Text(
                                            text = "🎙️ Duration: ${note.formattedAudioDuration} · High-efficiency AAC",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MindTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Attached Image Card with Caption
                    if (note.imageUri != null) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MindGlassSurface,
                            border = BorderStroke(1.dp, MindGlassCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                AsyncImage(
                                    model = note.imageUri,
                                    contentDescription = note.imageCaption ?: "Note Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { isFullScreenImageOpen = true }
                                )

                                if (!note.imageCaption.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📷", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = note.imageCaption,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MindTextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tags
                    if (note.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            note.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MindGlassSurfaceVariant,
                                    border = BorderStroke(0.5.dp, MindGlassBorderSubtle)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindTextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 1. RAW USER JOURNAL CONTENT
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MindGlassSurface),
                        border = BorderStroke(1.dp, MindGlassCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📝", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Journal Entry",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MindTextTertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MindTextPrimary,
                                lineHeight = 24.sp
                            )
                        }
                    }

                    // SECTION DIVIDER: DISTINCT AI INTELLIGENCE SUITE
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("✨", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Second Brain Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindIndigo
                        )
                    }

                    // 2. AI TL;DR Summary Card
                    if (note.summary != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MindGlassSurfaceVariant),
                            border = BorderStroke(1.dp, MindIndigo.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("🤖", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "AI TL;DR Summary",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindIndigo,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MindTextPrimary,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    // 3. Extracted Action Items
                    if (note.actionItems.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MindGlassSurface),
                            border = BorderStroke(1.dp, MindEmerald.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✅", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Extracted Action Items (${note.actionItems.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MindTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                note.actionItems.forEachIndexed { index, item ->
                                    val isChecked = uiState.completedActionIndices.contains(index)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.toggleActionItem(index) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { viewModel.toggleActionItem(index) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MindEmerald,
                                                uncheckedColor = MindTextTertiary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isChecked) MindTextTertiary else MindTextPrimary,
                                            textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Brainstorming & Reflection Prompts
                    ReflectionPromptsCard(prompts = note.reflectionPrompts)

                    // 5. Contextual "Related Thoughts" Card (Cosine similarity vector search)
                    RelatedThoughtsCard(
                        relatedThoughts = uiState.relatedThoughts,
                        isLoading = uiState.isLoadingRelated,
                        onThoughtClick = onThoughtClick
                    )
                }

                // Ergonomic Bottom Edit Dock (Thumb reachable!)
                Surface(
                    color = MindGlassBottomDock,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    border = BorderStroke(1.dp, MindGlassBorder),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onNavigateBack) {
                            Text("← Back", color = MindTextSecondary)
                        }

                        Button(
                            onClick = { onEditClick(note.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MindIndigo),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Edit Thought", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Zoomable Image Viewer Dialog
    if (isFullScreenImageOpen && note?.imageUri != null) {
        Dialog(
            onDismissRequest = { isFullScreenImageOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var zoomScale by remember { mutableFloatStateOf(1f) }
            var zoomOffset by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Interactive Zoomable Image
                AsyncImage(
                    model = note.imageUri,
                    contentDescription = note.imageCaption ?: "Full Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(1f, 6f)
                                if (zoomScale > 1f) {
                                    zoomOffset += pan
                                } else {
                                    zoomOffset = Offset.Zero
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (zoomScale > 1.2f) {
                                        zoomScale = 1f
                                        zoomOffset = Offset.Zero
                                    } else {
                                        zoomScale = 2.5f
                                    }
                                }
                            )
                        }
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale,
                            translationX = zoomOffset.x,
                            translationY = zoomOffset.y
                        )
                )

                // Top Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!note.imageCaption.isNullOrBlank()) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp)
                        ) {
                            Text(
                                text = note.imageCaption,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                maxLines = 2
                            )
                        }
                    } else {
                        Box(modifier = Modifier.width(1.dp))
                    }

                    IconButton(
                        onClick = { isFullScreenImageOpen = false },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Bottom Hint
                Text(
                    text = "Pinch or double-tap to zoom",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

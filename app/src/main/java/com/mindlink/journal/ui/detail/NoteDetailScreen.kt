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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import coil.compose.AsyncImage
import com.mindlink.journal.data.ai.LocalHeuristicBrainstormingEngine
import com.mindlink.journal.ui.detail.components.ReflectionPromptsCard
import com.mindlink.journal.ui.detail.components.RelatedThoughtsCard
import com.mindlink.journal.ui.editor.components.TextRecognitionHelper
import com.mindlink.journal.ui.notes.components.MoodBadge
import com.mindlink.journal.ui.components.RichTextView
import com.mindlink.journal.ui.components.liquidGlass
import com.mindlink.journal.ui.history.NoteVersionHistoryDialog
import com.mindlink.journal.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onThoughtClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isFullScreenImageOpen by remember { mutableStateOf(false) }
    var isOcrScanning by remember { mutableStateOf(false) }

    val ocrHelper = remember { TextRecognitionHelper(context) }
    val brainstormingEngine = remember { LocalHeuristicBrainstormingEngine() }

    // Audio Playback state
    var isPlayingAudio by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val noteVersions by viewModel.noteVersions.collectAsState()
    var showVersionHistoryDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val note = uiState.note
    val noteFont = resolveNoteFontFamily(note?.fontFamily)
    var followUpInput by remember { mutableStateOf("") }

    if (showDeleteDialog) {
        val isTrash = note?.isDeleted == true
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(if (isTrash) "Delete Thought Forever" else "Move to Trash", color = MindTextPrimary) },
            text = {
                Text(
                    text = if (isTrash)
                        "Permanently delete this thought and its embeddings? This action cannot be undone."
                    else
                        "Move this thought to Trash? Items in Trash are automatically deleted after 30 days.",
                    color = MindTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        if (isTrash) {
                            viewModel.deletePermanently { onNavigateBack() }
                        } else {
                            viewModel.moveToTrash { onNavigateBack() }
                        }
                    }
                ) {
                    Text(if (isTrash) "Delete Forever" else "Move to Trash", color = MindRose, fontWeight = FontWeight.Bold)
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
                        if (note.isDeleted) {
                            // Restore from Trash
                            IconButton(onClick = { viewModel.restoreFromTrash { onNavigateBack() } }) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = PastelMint)
                            }
                            // Delete Permanently
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", tint = PastelRose)
                            }
                        } else {
                            // Version History & Diff Viewer
                            IconButton(onClick = { showVersionHistoryDialog = true }) {
                                Icon(Icons.Default.History, contentDescription = "Version History & Diff", tint = PastelLavender)
                            }

                            // Font Size Override Menu
                            var showFontSizeMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showFontSizeMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = "Font Size",
                                        tint = if (note.fontSizeScale != null) PastelMint else PastelLavender
                                    )
                                }
                                DropdownMenu(
                                    expanded = showFontSizeMenu,
                                    onDismissRequest = { showFontSizeMenu = false },
                                    modifier = Modifier.background(MindSurfaceDark)
                                ) {
                                    val sizeChoices = listOf(
                                        Pair(null, "Follow App Size"),
                                        Pair(0.85f, "Small (0.85x)"),
                                        Pair(1.0f, "Standard (1.0x)"),
                                        Pair(1.15f, "Large (1.15x)"),
                                        Pair(1.30f, "Huge (1.30x)")
                                    )
                                    sizeChoices.forEach { (scale, label) ->
                                        val isCurrent = if (scale == null) note.fontSizeScale == null else note.fontSizeScale != null && Math.abs(note.fontSizeScale - scale) < 0.05f
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = label,
                                                    color = if (isCurrent) PastelMint else MindTextPrimary,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                showFontSizeMenu = false
                                                viewModel.updateFontSizeScale(scale)
                                            }
                                        )
                                    }
                                }
                            }

                            // Archive / Unarchive
                            if (note.isArchived) {
                                IconButton(onClick = { viewModel.unarchiveNote { onNavigateBack() } }) {
                                    Icon(Icons.Default.Unarchive, contentDescription = "Unarchive", tint = PastelAmber)
                                }
                            } else {
                                IconButton(onClick = { viewModel.archiveNote { onNavigateBack() } }) {
                                    Icon(Icons.Default.Archive, contentDescription = "Archive", tint = PastelAmber)
                                }
                            }
                            // Move to Trash
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Move to Trash", tint = PastelRose)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (MindIsGlassy) Color.Transparent else MindBackgroundDark
                )
            )
        },
        containerColor = if (MindIsGlassy) Color.Transparent else MindBackgroundDark
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
                    // Status Banners (Trash Lifecycle with 30-Day Auto-Purge or Archived)
                    if (note.isDeleted) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PastelRose.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, PastelRose.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = PastelRose, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "In Trash · Auto-purges in ${note.daysUntilExpiry} days",
                                    color = PastelRose,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else if (note.isArchived) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PastelAmber.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, PastelAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = null, tint = PastelAmber, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Archived Thought · Kept safe outside main journal",
                                    color = PastelAmber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

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
                    val noteScale = note.fontSizeScale ?: 1.0f
                    val titleFontSize = (MaterialTheme.typography.headlineMedium.fontSize.value * noteScale).sp
                    val titleLineHeight = (titleFontSize.value * 1.35f).sp
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = titleFontSize,
                            lineHeight = titleLineHeight,
                            fontFamily = noteFont
                        ),
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
                                        color = if (isPlayingAudio) PastelRose else PastelLavender,
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
                                                            player?.setOnCompletionListener {
                                                                isPlayingAudio = false
                                                            }
                                                            player?.setOnErrorListener { mp, _, _ ->
                                                                mp.release()
                                                                mediaPlayer = null
                                                                isPlayingAudio = false
                                                                true
                                                            }
                                                            mediaPlayer = player
                                                        }
                                                        mediaPlayer?.let { player ->
                                                            if (player.currentPosition >= player.duration - 200) {
                                                                player.seekTo(0)
                                                            }
                                                            player.start()
                                                            isPlayingAudio = true
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    isPlayingAudio = false
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlayingAudio) "Pause" else "Play",
                                                tint = Color.Black,
                                                modifier = Modifier.size(22.dp)
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
                                            text = "Duration: ${note.formattedAudioDuration} · High-efficiency AAC",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MindTextSecondary
                                        )
                                    }
                                }
                            }

                            // Transcribe Audio Action Button in Detail Screen
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (uiState.isTranscribing) MindGlassSurfaceElevated else MindGlassButtonContainer,
                                border = BorderStroke(1.dp, MindGlassButtonBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !uiState.isTranscribing) {
                                        viewModel.transcribeAudio()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (uiState.isTranscribing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(15.dp),
                                            color = PastelLavender,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Transcribing Audio with Gemini...", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PastelLavender)
                                    } else {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Transcribe Audio to Note", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PastelLavender)
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
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = PastelSky,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = note.imageCaption,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MindTextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MindGlassButtonContainer,
                                    border = BorderStroke(1.dp, MindGlassButtonBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isOcrScanning) {
                                            coroutineScope.launch {
                                                isOcrScanning = true
                                                try {
                                                    val uri = Uri.parse(note.imageUri)
                                                    val result = ocrHelper.extractTextFromImageUri(uri)
                                                    result.onSuccess { rawText ->
                                                        if (rawText.isNotBlank()) {
                                                            val (_, polishedBody) = brainstormingEngine.polishOcrText(rawText)
                                                            viewModel.appendContent(polishedBody)
                                                            Toast.makeText(context, "Text extracted & saved to thought", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "No text detected in this image", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }.onFailure { err ->
                                                        Toast.makeText(context, "OCR failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isOcrScanning = false
                                                }
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 11.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isOcrScanning) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = PastelViolet
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Extracting Text...", fontSize = 13.sp, color = PastelViolet, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.DocumentScanner,
                                                contentDescription = null,
                                                modifier = Modifier.size(17.dp),
                                                tint = PastelViolet
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Extract Text from Image",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PastelViolet
                                            )
                                        }
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
                    val detailGlassy = MindIsGlassy
                    val detailDark = LocalMindLinkColors.current.isDark
                    val journalCardShape = RoundedCornerShape(18.dp)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = journalCardShape,
                                cornerRadius = 18.dp,
                                isGlassy = detailGlassy,
                                isDark = detailDark,
                                cardBackground = MindGlassCardBackground,
                                cardBorder = MindGlassCardBorder
                            ),
                        shape = journalCardShape,
                        colors = CardDefaults.cardColors(containerColor = if (detailGlassy) Color.Transparent else MindGlassSurface),
                        border = if (detailGlassy) null else BorderStroke(1.dp, MindGlassCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notes, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Journal Entry",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MindTextTertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val lineSpacing = LocalLineSpacingMultiplier.current
                            val bodyFontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * noteScale).sp
                            val bodyLineHeight = (bodyFontSize.value * lineSpacing).sp
                            RichTextView(
                                text = note.content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = bodyFontSize,
                                    lineHeight = bodyLineHeight,
                                    fontFamily = noteFont
                                ),
                                color = MindTextPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Quick Follow-up & Add Points to Note Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MindGlassSurfaceVariant),
                        border = BorderStroke(1.dp, MindGlassBorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AddComment, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add Follow-up Point",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MindTextPrimary,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "Auto-reprocesses AI correlations",
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MindTextTertiary.copy(alpha = 0.60f),
                                    modifier = Modifier.padding(start = 14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = followUpInput,
                                onValueChange = { followUpInput = it },
                                placeholder = {
                                    Text("Add an update, evening reflection, or new point...", color = MindTextTertiary, fontSize = 13.sp)
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MindTextPrimary, fontFamily = noteFont),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MindGlassSurface,
                                    unfocusedContainerColor = MindGlassSurface,
                                    focusedBorderColor = PastelLavender,
                                    unfocusedBorderColor = MindGlassBorderSubtle,
                                    focusedTextColor = MindTextPrimary,
                                    unfocusedTextColor = MindTextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 4
                            )

                            if (followUpInput.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MindGlassButtonContainer,
                                        border = BorderStroke(1.dp, MindGlassButtonBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                viewModel.appendFollowUp(followUpInput) {
                                                    followUpInput = ""
                                                    Toast.makeText(context, "Point added & correlations reprocessed!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save & Reprocess", fontSize = 12.sp, color = PastelLavender, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION DIVIDER: DISTINCT AI INTELLIGENCE SUITE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Second Brain Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PastelLavender
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MindGlassButtonContainer,
                            border = BorderStroke(0.8.dp, MindGlassButtonBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.reprocessNote {
                                        Toast.makeText(context, "Correlations & analysis recalculated", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = PastelSky, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-analyze", fontSize = 11.sp, color = PastelSky, fontWeight = FontWeight.Bold)
                            }
                        }
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
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = PastelSky, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "AI TL;DR Summary",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PastelSky,
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

                    // Proactive Pattern & Contextual Reminder Alert (Extracted from Note Context)
                    val contextualReminder = remember(note.title, note.content) {
                        brainstormingEngine.extractContextualReminder(note.title, note.content)
                    }
                    if (contextualReminder != null) {
                        var isReminderSet by remember { mutableStateOf(false) }
                        var scheduledTimeText by remember { mutableStateOf("") }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PastelAmber.copy(alpha = 0.12f),
                            border = BorderStroke(1.2.dp, PastelAmber.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PastelAmber, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(contextualReminder.triggerPattern, color = PastelAmber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(contextualReminder.question, color = MindTextPrimary, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isReminderSet) PastelMint.copy(alpha = 0.2f) else MindGlassButtonContainer,
                                        border = BorderStroke(0.8.dp, if (isReminderSet) PastelMint else MindGlassButtonBorder),
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                            val targetMs = com.mindlink.journal.data.worker.ReminderWorker.schedule(
                                                context = context,
                                                title = contextualReminder.triggerPattern,
                                                message = contextualReminder.question,
                                                noteId = note.id,
                                                delayMinutes = contextualReminder.suggestedDelayMinutes
                                            )
                                            val timeFmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(targetMs))
                                            scheduledTimeText = "Scheduled for $timeFmt"
                                            isReminderSet = true
                                            Toast.makeText(context, "System notification scheduled for $timeFmt", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(if (isReminderSet) Icons.Default.Check else Icons.Default.Alarm, contentDescription = null, tint = if (isReminderSet) PastelMint else PastelAmber, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isReminderSet) scheduledTimeText else "Schedule Reminder", color = if (isReminderSet) PastelMint else MindGlassButtonText, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Extracted Action Items (Real interactive Checkboxes, NO raw [] syntax!)
                    if (note.actionItems.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MindGlassSurface),
                            border = BorderStroke(1.dp, PastelMint.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = PastelMint, modifier = Modifier.size(19.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Action Items (${note.actionItems.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MindTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                note.actionItems.forEachIndexed { index, rawItem ->
                                    val isChecked = rawItem.startsWith("[x] ", ignoreCase = true) || uiState.completedActionIndices.contains(index)
                                    val cleanText = when {
                                        rawItem.startsWith("[x] ", ignoreCase = true) -> rawItem.substring(4)
                                        rawItem.startsWith("[ ] ") -> rawItem.substring(4)
                                        else -> rawItem
                                    }
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
                                                checkedColor = PastelMint,
                                                uncheckedColor = MindTextTertiary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cleanText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isChecked) MindTextTertiary else MindTextPrimary,
                                            textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                            fontFamily = noteFont
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Brainstorming & Dynamic Evolving Socratic Reflection Prompts
                    val livePrompts = remember(note.title, note.content, note.reflectionPrompts) {
                        val isLegacyMismatch = note.reflectionPrompts.any { 
                            it.contains("bottleneck") || it.contains("architectural assumption") || it.contains("hidden premise") 
                        }
                        if (note.reflectionPrompts.isNotEmpty() && !isLegacyMismatch && !note.content.contains("> **Reflection")) {
                            note.reflectionPrompts
                        } else {
                            brainstormingEngine.generateReflectionPrompts(note.title, note.content, note.mood ?: "Reflective")
                        }
                    }

                    ReflectionPromptsCard(
                        prompts = livePrompts,
                        onAnswerSubmitted = { question, answer ->
                            viewModel.submitPromptReflection(question, answer) {
                                Toast.makeText(context, "Reflection integrated! Context & correlations evolved.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

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

                        if (!note.isDeleted) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MindGlassButtonContainer,
                                border = BorderStroke(1.dp, MindGlassButtonBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onEditClick(note.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit Thought", fontWeight = FontWeight.Bold, color = MindGlassButtonText, fontSize = 14.sp)
                                }
                            }
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

    // Version History & Diff Viewer Dialog
    if (showVersionHistoryDialog && note != null) {
        NoteVersionHistoryDialog(
            currentTitle = note.title,
            currentContent = note.content,
            versions = noteVersions,
            onDismiss = { showVersionHistoryDialog = false },
            onRestoreVersion = { ver ->
                viewModel.restoreVersion(ver)
                Toast.makeText(context, "Restored revision from ${ver.compactDate}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Audio Transcription Error Alert
    if (uiState.transcribeError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearTranscribeError() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = PastelRose, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audio Transcription", fontWeight = FontWeight.Bold, color = MindTextPrimary)
                }
            },
            text = {
                Text(
                    text = uiState.transcribeError ?: "Failed to transcribe audio recording.",
                    color = MindTextSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearTranscribeError() }) {
                    Text("OK", color = PastelLavender, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MindBackgroundAmoled
        )
    }
}

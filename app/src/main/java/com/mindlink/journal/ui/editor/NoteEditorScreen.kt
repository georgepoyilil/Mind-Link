package com.mindlink.journal.ui.editor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.mindlink.journal.data.ai.LocalHeuristicBrainstormingEngine
import com.mindlink.journal.di.ServiceLocator
import com.mindlink.journal.ui.editor.components.AudioRecordingHelper
import com.mindlink.journal.ui.editor.components.SpeechRecognitionHelper
import com.mindlink.journal.ui.editor.components.SpeechState
import com.mindlink.journal.ui.editor.components.TextRecognitionHelper
import com.mindlink.journal.ui.components.RichTextView
import com.mindlink.journal.ui.history.NoteVersionHistoryDialog
import com.mindlink.journal.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    initialMode: String = "text",
    onNavigateBack: () -> Unit,
    onSaveSuccess: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val serviceLocator = remember { ServiceLocator.getInstance(context) }
    val defaultLineSpacing = LocalLineSpacingMultiplier.current
    var activeLineSpacing by remember { mutableStateOf(defaultLineSpacing) }

    // Helpers
    val speechHelper = remember { SpeechRecognitionHelper(context) }
    val speechState by speechHelper.speechState.collectAsState()
    val audioRecorder = remember { AudioRecordingHelper(context) }
    val ocrHelper = remember { TextRecognitionHelper(context) }
    val brainstormingEngine = remember { LocalHeuristicBrainstormingEngine() }

    var dictationTargetIsCaption by remember { mutableStateOf(false) }
    var isOcrScanning by remember { mutableStateOf(false) }

    // Media Player state for in-editor audio review
    var isPlayingAudio by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Selection-aware text field value for markdown formatting & selection operations
    var contentValue by remember {
        mutableStateOf(TextFieldValue(uiState.content, TextRange(uiState.content.length)))
    }

    val undoRedoManager = remember { UndoRedoManager() }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var showVersionHistoryDialog by remember { mutableStateOf(false) }
    var isWordPreviewMode by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    var focusedTaskId by remember { mutableStateOf<String?>(null) }
    var isOcrCardDismissed by remember { mutableStateOf(false) }

    var showWitDialog by remember { mutableStateOf(false) }
    var witCategory by remember { mutableStateOf("Roast") }
    var witText by remember { mutableStateOf("") }

    var showTaskSplitterDialog by remember { mutableStateOf(false) }
    var taskSplitterInput by remember { mutableStateOf("") }

    val capabilityManager = remember { com.mindlink.journal.di.ServiceLocator.getInstance(context).capabilityLadderManager }
    val ladderState by capabilityManager.state.collectAsState()
    val canUseTypography = ladderState.isEnabled(com.mindlink.journal.data.capability.CapabilityLadderManager.FEATURE_TYPOGRAPHY)
    val canUseMedia = ladderState.isEnabled(com.mindlink.journal.data.capability.CapabilityLadderManager.FEATURE_MEDIA_CAPTURE)
    val canUseOcr = ladderState.isEnabled(com.mindlink.journal.data.capability.CapabilityLadderManager.FEATURE_OCR)
    val canUseVoice = ladderState.isEnabled(com.mindlink.journal.data.capability.CapabilityLadderManager.FEATURE_VOICE_DICTATION)
    val canUseChecklists = ladderState.isEnabled(com.mindlink.journal.data.capability.CapabilityLadderManager.FEATURE_CHECKLISTS)
    val canUseSplitter = canUseChecklists && ladderState.isEnabled(com.mindlink.journal.data.capability.CapabilityLadderManager.FEATURE_TASK_SPLITTER)
    val canUseJester = ladderState.isEnabled(com.mindlink.journal.data.capability.CapabilityLadderManager.FEATURE_JESTER)
    val canUseHashtags = ladderState.isEnabled(com.mindlink.journal.data.capability.CapabilityLadderManager.FEATURE_HASHTAGS)
    val canUseVersionHistory = ladderState.isEnabled(com.mindlink.journal.data.capability.CapabilityLadderManager.FEATURE_VERSION_HISTORY)

    fun addNewTask() {
        val newId = viewModel.addChecklistItem("")
        focusedTaskId = newId
    }

    val saveAndExit = {
        val hasContent = uiState.title.isNotBlank() ||
                contentValue.text.isNotBlank() ||
                uiState.checklistItems.any { it.text.isNotBlank() } ||
                uiState.imageUri != null ||
                uiState.audioUri != null

        if (hasContent) {
            viewModel.updateContent(contentValue.text)
            viewModel.saveNote { _ ->
                onNavigateBack()
            }
        } else {
            onNavigateBack()
        }
    }

    BackHandler(onBack = saveAndExit)

    val performUndo: () -> Unit = {
        undoRedoManager.undo(contentValue)?.let { prev ->
            contentValue = prev
            viewModel.updateContent(prev.text)
            canUndo = undoRedoManager.canUndo
            canRedo = undoRedoManager.canRedo
        }
    }

    val performRedo: () -> Unit = {
        undoRedoManager.redo(contentValue)?.let { next ->
            contentValue = next
            viewModel.updateContent(next.text)
            canUndo = undoRedoManager.canUndo
            canRedo = undoRedoManager.canRedo
        }
    }

    // Keep contentValue in sync when content changes from external sources (e.g. load, OCR, dictation)
    LaunchedEffect(uiState.content) {
        if (contentValue.text != uiState.content) {
            contentValue = contentValue.copy(
                text = uiState.content,
                selection = TextRange(uiState.content.length)
            )
        }
    }

    DisposableEffect(uiState.audioUri) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            isPlayingAudio = false
        }
    }

    var activeCameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var activeOcrPhotoUri by remember { mutableStateOf<Uri?>(null) }

    fun createHighResPhotoUri(): Uri {
        val imagesDir = File(context.filesDir, "note_images").apply { if (!exists()) mkdirs() }
        val photoFile = File(imagesDir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    // Standard Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && activeCameraPhotoUri != null) {
            viewModel.setImage(activeCameraPhotoUri.toString(), "Captured with camera")
        }
    }

    // OCR Document Scanning Camera Launcher
    val ocrCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && activeOcrPhotoUri != null) {
            val uri = activeOcrPhotoUri!!
            viewModel.setImage(uri.toString(), "Scanned document / notes")
            coroutineScope.launch {
                isOcrScanning = true
                val result = ocrHelper.extractTextFromImageUri(uri)
                result.onSuccess { rawText ->
                    if (rawText.isNotBlank()) {
                        val (polishedTitle, polishedBody) = brainstormingEngine.polishOcrText(rawText)
                        if (uiState.title.isBlank()) viewModel.updateTitle(polishedTitle)
                        val merged = if (uiState.content.isBlank()) polishedBody else "${uiState.content}\n\n$polishedBody"
                        viewModel.updateContent(merged)
                        Toast.makeText(context, "Text extracted and polished into note", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No text detected in photo", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { err ->
                    Toast.makeText(context, "OCR failed: ${err.message}", Toast.LENGTH_SHORT).show()
                }
                isOcrScanning = false
            }
        }
    }

    // Gallery Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val imagesDir = File(context.filesDir, "note_images").apply { if (!exists()) mkdirs() }
                val destFile = File(imagesDir, "img_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
                viewModel.setImage(Uri.fromFile(destFile).toString(), "")
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to attach photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Voice Dictation handler
    LaunchedEffect(speechState) {
        when (val state = speechState) {
            is SpeechState.Listening -> {
                viewModel.setRecordingState(true)
                if (state.partialText.isNotBlank()) {
                    viewModel.updateSpeechPartial(state.partialText)
                }
            }
            is SpeechState.Result -> {
                if (dictationTargetIsCaption) {
                    val currentCaption = uiState.imageCaption ?: ""
                    val updated = if (currentCaption.isBlank()) state.finalText else "$currentCaption ${state.finalText}"
                    viewModel.updateImageCaption(updated)
                    dictationTargetIsCaption = false
                } else {
                    viewModel.appendVoiceText(state.finalText)
                }
                viewModel.setRecordingState(false)
            }
            is SpeechState.Error -> {
                viewModel.setRecordingState(false)
                viewModel.setSpeechError(state.message)
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            is SpeechState.Idle -> {
                viewModel.setRecordingState(false)
            }
        }
    }

    var isRecordingAudioMemo by remember { mutableStateOf(false) }
    var showTypographyPanel by remember { mutableStateOf(false) }
    var showMediaPanel by remember { mutableStateOf(false) }

    var performToggleSpeech: ((Boolean) -> Unit)? by remember { mutableStateOf(null) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            performToggleSpeech?.invoke(dictationTargetIsCaption)
        } else {
            Toast.makeText(context, "Audio permission needed for speech recognition", Toast.LENGTH_SHORT).show()
        }
    }

    // Audio & Speech safe lifecycle disposal
    DisposableEffect(Unit) {
        onDispose {
            speechHelper.stopListening()
            if (isRecordingAudioMemo) {
                audioRecorder.cancelRecording()
                isRecordingAudioMemo = false
            }
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Speech-to-Text Dictation trigger (uses SpeechRecognizer exclusively, never MediaRecorder!)
    val toggleSpeech: (Boolean) -> Unit = { isCaption ->
        dictationTargetIsCaption = isCaption
        if (uiState.isVoiceRecording) {
            speechHelper.stopListening()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                if (isRecordingAudioMemo) {
                    audioRecorder.cancelRecording()
                    isRecordingAudioMemo = false
                }
                speechHelper.startListening()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(toggleSpeech) {
        performToggleSpeech = toggleSpeech
    }

    // Voice Memo Attachment trigger (uses MediaRecorder exclusively, never SpeechRecognizer!)
    val toggleAudioMemo: () -> Unit = {
        if (isRecordingAudioMemo) {
            val (audioFile, duration) = audioRecorder.stopRecording()
            isRecordingAudioMemo = false
            if (audioFile != null && audioFile.exists() && duration > 500L) {
                viewModel.setAudio(Uri.fromFile(audioFile).toString(), duration)
                Toast.makeText(context, "Voice memo attached (${duration / 1000}s)", Toast.LENGTH_SHORT).show()
            }
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                speechHelper.stopListening()
                val file = audioRecorder.startRecording()
                if (file != null) {
                    isRecordingAudioMemo = true
                    Toast.makeText(context, "Recording voice memo...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not start audio recorder", Toast.LENGTH_SHORT).show()
                }
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val launchCameraHighRes: () -> Unit = {
        val uri = createHighResPhotoUri()
        activeCameraPhotoUri = uri
        cameraLauncher.launch(uri)
    }

    val launchOcrHighRes: () -> Unit = {
        val uri = createHighResPhotoUri()
        activeOcrPhotoUri = uri
        ocrCameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchCameraHighRes()
        else Toast.makeText(context, "Camera permission needed to snap photos", Toast.LENGTH_SHORT).show()
    }

    val ocrPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchOcrHighRes()
        else Toast.makeText(context, "Camera permission needed to scan text", Toast.LENGTH_SHORT).show()
    }

    // IMPORTANT FIX: For OCR mode, do NOT launch camera directly on screen entry!
    // Instead, the screen opens with a prominent camera scan button ready for the user.

    // Markdown & MS Word-style Selection-Aware Formatting Helpers
    fun wrapSelection(prefix: String, suffix: String = prefix) {
        undoRedoManager.record(contentValue, force = true)
        val text = contentValue.text
        val selection = contentValue.selection
        if (!selection.collapsed) {
            val start = selection.min
            val end = selection.max
            val selected = text.substring(start, end)
            val newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
            contentValue = TextFieldValue(
                text = newText,
                selection = TextRange(start + prefix.length, end + prefix.length)
            )
            viewModel.updateContent(newText)
        } else {
            val cursor = selection.start
            val newText = text.substring(0, cursor) + prefix + suffix + text.substring(cursor)
            contentValue = TextFieldValue(
                text = newText,
                selection = TextRange(cursor + prefix.length)
            )
            viewModel.updateContent(newText)
        }
        canUndo = undoRedoManager.canUndo
        canRedo = undoRedoManager.canRedo
    }

    fun insertLinePrefix(prefix: String) {
        undoRedoManager.record(contentValue, force = true)
        val text = contentValue.text
        val cursor = contentValue.selection.start
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        contentValue = TextFieldValue(
            text = newText,
            selection = TextRange(cursor + prefix.length)
        )
        viewModel.updateContent(newText)
        canUndo = undoRedoManager.canUndo
        canRedo = undoRedoManager.canRedo
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.stopListening()
            audioRecorder.cancelRecording()
            mediaPlayer?.release()
        }
    }

    val currentFontFamily = if (canUseTypography) resolveNoteFontFamily(uiState.fontFamily) else resolveNoteFontFamily(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.id == 0L) "New Thought" else "Edit Thought",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MindTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = saveAndExit) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MindTextPrimary)
                    }
                },
                actions = {
                    // Time Machine (Revisions & Version Diff History)
                    if (canUseVersionHistory && (uiState.id > 0 || uiState.noteVersions.isNotEmpty())) {
                        IconButton(
                            onClick = { showVersionHistoryDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Version History & Diff",
                                tint = PastelLavender,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Undo Button
                    IconButton(
                        onClick = performUndo,
                        enabled = canUndo,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) MindTextPrimary else MindTextTertiary.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = performRedo,
                        enabled = canRedo,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) MindTextPrimary else MindTextTertiary.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Voice recording icon in top bar (Voice Studio)
                    if (canUseVoice) {
                        IconButton(
                            onClick = { toggleSpeech(false) },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isVoiceRecording) PastelRose.copy(alpha = 0.25f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = if (uiState.isVoiceRecording) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Record",
                                tint = if (uiState.isVoiceRecording) PastelRose else PastelLavender,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Save Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MindGlassButtonContainer,
                        border = BorderStroke(1.dp, MindGlassButtonBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.saveNote { id -> onSaveSuccess(id) }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = PastelLavender, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", fontWeight = FontWeight.Bold, color = MindGlassButtonText, fontSize = 13.5.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (MindIsGlassy) Color.Transparent else MindBackgroundAmoled)
            )
        },
        containerColor = if (MindIsGlassy) Color.Transparent else MindBackgroundAmoled
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Live Voice Streaming Bar
            AnimatedVisibility(visible = uiState.isVoiceRecording || uiState.speechPartialText.isNotBlank()) {
                Surface(
                    color = MindRose.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MindRose.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Audio input",
                            tint = MindRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.speechPartialText.isNotBlank())
                                "\"${uiState.speechPartialText}\""
                            else
                                "Recording audio & listening... speak naturally",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MindRose,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // OCR Scanning Spinner Bar
            AnimatedVisibility(visible = isOcrScanning) {
                Surface(
                    color = MindGlassButtonContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MindGlassButtonBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = PastelLavender,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Scanning document text with on-device OCR...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PastelLavender,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Reorganized Professional Editing Toolbar & Inspectors
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MindGlassSurfaceVariant)
            ) {
                // 1. Two-Row Categorized Toolbar (Grouped by Tool Type)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // ROW 1: Structural & Block Tools (Aa Typography, Attach Media, H3, Quote, List, + Task, ⚡ Tasks, 🎭 Wit & Roast)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Typography / Font Size Inspector Toggle (Aa)
                        if (canUseTypography) {
                            val activeScale = uiState.fontSizeScale
                            val sizeLabel = when {
                                activeScale == null -> "Aa"
                                activeScale < 0.9f -> "Aa · S"
                                activeScale < 1.1f -> "Aa · M"
                                activeScale < 1.25f -> "Aa · L"
                                else -> "Aa · XL"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (showTypographyPanel) MindIndigo.copy(alpha = 0.35f) else MindGlassButtonContainer,
                                border = BorderStroke(1.dp, if (showTypographyPanel) PastelLavender else MindGlassButtonBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showTypographyPanel = !showTypographyPanel
                                        if (showTypographyPanel) showMediaPanel = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TextFields,
                                        contentDescription = "Typography",
                                        tint = if (showTypographyPanel) PastelLavender else MindTextPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = sizeLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (showTypographyPanel) PastelLavender else MindTextPrimary
                                    )
                                }
                            }
                        }

                        // Insert Media Tray Toggle (+)
                        if (canUseMedia || canUseVoice || canUseOcr) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (showMediaPanel) PastelSky.copy(alpha = 0.35f) else MindGlassButtonContainer,
                                border = BorderStroke(1.dp, if (showMediaPanel) PastelSky else MindGlassButtonBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showMediaPanel = !showMediaPanel
                                        if (showMediaPanel) showTypographyPanel = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Attach Media",
                                        tint = if (showMediaPanel) PastelSky else MindTextPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Media", fontSize = 11.sp, color = if (showMediaPanel) PastelSky else MindTextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .width(1.dp)
                                .background(MindGlassBorderSubtle)
                        )

                        // Heading H3
                        IconButton(onClick = { insertLinePrefix("### ") }, modifier = Modifier.size(30.dp)) {
                            Text("H3", fontWeight = FontWeight.Bold, color = MindTextPrimary, fontSize = 12.sp)
                        }

                        // Quote
                        IconButton(onClick = { insertLinePrefix("> ") }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.FormatQuote, contentDescription = "Quote", tint = MindTextPrimary, modifier = Modifier.size(16.dp))
                        }

                        // Bullet List
                        IconButton(onClick = { insertLinePrefix("- ") }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", tint = MindTextPrimary, modifier = Modifier.size(16.dp))
                        }

                        // + Task checklist button
                        if (canUseChecklists) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MindGlassButtonContainer,
                                border = BorderStroke(0.8.dp, MindGlassButtonBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { addNewTask() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = PastelMint, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("+ Task", fontSize = 11.sp, color = PastelMint, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // ⚡ Dynamic Tasks Quick Splitter
                        if (canUseSplitter) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PastelSky.copy(alpha = 0.15f),
                                border = BorderStroke(0.8.dp, PastelSky.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showTaskSplitterDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = PastelSky, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("⚡ Tasks", fontSize = 11.sp, color = PastelSky, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 🎭 Wit & Roast (Funny Tool)
                        if (canUseJester) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PastelRose.copy(alpha = 0.15f),
                                border = BorderStroke(0.8.dp, PastelRose.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        witText = generateMindLinkWit(uiState.title, contentValue.text, uiState.checklistItems.size, witCategory)
                                        showWitDialog = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🎭 Wit & Roast", fontSize = 11.sp, color = PastelRose, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Subtle Row Divider
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MindBorderDark.copy(alpha = 0.35f)
                    )

                    // ROW 2: Inline Text Styling & Action History (Bold, Italic, Underline, Strikethrough, Select All, Undo, Redo)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Bold
                        IconButton(onClick = { wrapSelection("**") }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = MindTextPrimary, modifier = Modifier.size(16.dp))
                        }

                        // Italic
                        IconButton(onClick = { wrapSelection("*") }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = MindTextPrimary, modifier = Modifier.size(16.dp))
                        }

                        // Underline
                        IconButton(onClick = { wrapSelection("__") }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", tint = MindTextPrimary, modifier = Modifier.size(16.dp))
                        }

                        // Strikethrough
                        IconButton(onClick = { wrapSelection("~~") }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.FormatStrikethrough, contentDescription = "Strikethrough", tint = MindTextPrimary, modifier = Modifier.size(16.dp))
                        }

                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .width(1.dp)
                                .background(MindGlassBorderSubtle)
                        )

                        // Select All
                        IconButton(
                            onClick = {
                                contentValue = contentValue.copy(selection = TextRange(0, contentValue.text.length))
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = MindTextSecondary, modifier = Modifier.size(16.dp))
                        }

                        // Undo
                        IconButton(onClick = performUndo, enabled = canUndo, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) MindTextPrimary else MindTextTertiary.copy(alpha = 0.35f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Redo
                        IconButton(onClick = performRedo, enabled = canRedo, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Default.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) MindTextPrimary else MindTextTertiary.copy(alpha = 0.35f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 2. Expandable Typography & Text Size Inspector (Proper structured editing tool)
                AnimatedVisibility(visible = canUseTypography && showTypographyPanel) {
                    val activeScale = uiState.fontSizeScale
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (LocalMindLinkColors.current.isDark) Color(0xFF131826) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TextFields, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Typography & Font Sizing",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MindTextPrimary
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { showTypographyPanel = false }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MindTextTertiary, modifier = Modifier.size(16.dp))
                                }
                            }

                            // Section A: Font Size Segmented Control (Default, Small, Medium, Large, XL)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "FONT SIZE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextTertiary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val fontSizeOptions = listOf(
                                        Pair(null, "Default"),
                                        Pair(0.85f, "Small"),
                                        Pair(1.0f, "Medium"),
                                        Pair(1.15f, "Large"),
                                        Pair(1.30f, "XL")
                                    )
                                    fontSizeOptions.forEach { (scale, label) ->
                                        val isSelected = if (scale == null) activeScale == null else activeScale != null && Math.abs(activeScale - scale) < 0.05f
                                        Surface(
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (isSelected) PastelLavender.copy(alpha = 0.25f) else MindGlassButtonContainer,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.2.dp else 0.8.dp,
                                                color = if (isSelected) PastelLavender else MindGlassButtonBorder
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(9.dp))
                                                .clickable { viewModel.setFontSizeScale(scale) }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) PastelLavender else MindTextSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Section B: Typeface / Font Family
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "TYPEFACE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextTertiary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    NoteFontOption.entries.forEach { option ->
                                        val isSelected = uiState.fontFamily.equals(option.key, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (isSelected) PastelSky.copy(alpha = 0.25f) else MindGlassButtonContainer,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.2.dp else 0.8.dp,
                                                color = if (isSelected) PastelSky else MindGlassButtonBorder
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(9.dp))
                                                .clickable { viewModel.setFontFamily(option.key) }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = option.label,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) PastelSky else MindTextSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Section C: Line Height Spacing
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "LINE SPACING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextTertiary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val lineSpacingOptions = listOf(
                                        Pair(1.25f, "1.2x"),
                                        Pair(1.45f, "1.5x"),
                                        Pair(1.75f, "1.8x"),
                                        Pair(2.0f, "2.0x")
                                    )
                                    lineSpacingOptions.forEach { (mult, label) ->
                                        val isSelected = Math.abs(activeLineSpacing - mult) < 0.05f
                                        Surface(
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (isSelected) PastelMint.copy(alpha = 0.25f) else MindGlassButtonContainer,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.2.dp else 0.8.dp,
                                                color = if (isSelected) PastelMint else MindGlassButtonBorder
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(9.dp))
                                                .clickable {
                                                    activeLineSpacing = mult
                                                    serviceLocator.updateLineSpacingMultiplier(mult)
                                                }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) PastelMint else MindTextSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Expandable Media & Attachments Tray
                AnimatedVisibility(visible = (canUseMedia || canUseVoice || canUseOcr) && showMediaPanel) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (LocalMindLinkColors.current.isDark) Color(0xFF131826) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gallery & Camera (Photo Moments)
                            if (canUseMedia) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            imagePickerLauncher.launch("image/*")
                                            showMediaPanel = false
                                        }
                                        .padding(6.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = PastelLavender, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Gallery", fontSize = 11.sp, color = MindTextSecondary)
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) launchCameraHighRes() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            showMediaPanel = false
                                        }
                                        .padding(6.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = PastelSky, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Camera", fontSize = 11.sp, color = MindTextSecondary)
                                }
                            }

                            // Voice Memo (Voice Studio)
                            if (canUseVoice) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            toggleAudioMemo()
                                            showMediaPanel = false
                                        }
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isRecordingAudioMemo) Icons.Default.Stop else Icons.Default.GraphicEq,
                                        contentDescription = "Audio Memo",
                                        tint = if (isRecordingAudioMemo) PastelRose else PastelAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(if (isRecordingAudioMemo) "Stop" else "Audio Memo", fontSize = 11.sp, color = if (isRecordingAudioMemo) PastelRose else MindTextSecondary)
                                }
                            }

                            // Scan Document OCR (Page Scanner)
                            if (canUseOcr) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) launchOcrHighRes() else ocrPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            showMediaPanel = false
                                        }
                                        .padding(6.dp)
                                ) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = "Scan Document", tint = PastelViolet, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Page Scan", fontSize = 11.sp, color = MindTextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Note Content Editor Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sleek, Compact Dismissible OCR Pill (Only if Page Scanner is enabled)
                if (canUseOcr && (initialMode == "ocr" || uiState.imageUri == null) && !isOcrCardDismissed) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PastelViolet.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, PastelViolet.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) launchOcrHighRes() else ocrPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Scan Document",
                                tint = PastelViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scan Document / Whiteboard (OCR)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MindTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = PastelViolet,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { isOcrCardDismissed = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MindTextTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Audio Recording Live Bar (When recording voice thought)
                AnimatedVisibility(visible = isRecordingAudioMemo) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PastelRose.copy(alpha = 0.15f),
                        border = BorderStroke(1.2.dp, PastelRose.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = PastelRose,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Recording Voice Thought...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PastelRose,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PastelRose.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { toggleAudioMemo() }
                            ) {
                                Text(
                                    text = "Stop & Attach",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelRose,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // In-Editor Audio Player & Review Card (Listen & Decide Before Saving)
                if (canUseVoice && uiState.audioUri != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MindGlassSurfaceElevated,
                        border = BorderStroke(1.2.dp, MindIndigo.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isPlayingAudio) PastelRose else PastelLavender,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                try {
                                                    if (isPlayingAudio) {
                                                        mediaPlayer?.pause()
                                                        isPlayingAudio = false
                                                    } else {
                                                        if (mediaPlayer == null) {
                                                            val player = MediaPlayer.create(context, Uri.parse(uiState.audioUri))
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
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isPlayingAudio) "Playing Audio Note..." else "Recorded Audio Preview",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MindTextPrimary
                                        )
                                        val durationSec = uiState.audioDurationMs / 1000
                                        val formatted = "${durationSec / 60}:${String.format("%02d", durationSec % 60)}"
                                        Text(
                                            text = "Duration: $formatted · Listen & decide before saving",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MindTextSecondary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        isPlayingAudio = false
                                        viewModel.removeAudio()
                                    }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Discard Audio", tint = PastelRose)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Decision & Transcribe Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Transcribe Option
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (uiState.isTranscribingAudio) MindGlassSurfaceElevated else MindGlassButtonContainer,
                                    border = BorderStroke(1.dp, MindGlassButtonBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !uiState.isTranscribingAudio) {
                                            viewModel.transcribeAudio()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (uiState.isTranscribingAudio) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(15.dp),
                                                color = PastelLavender,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Transcribing with Gemini...", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PastelLavender)
                                        } else {
                                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Transcribe to Text", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PastelLavender)
                                        }
                                    }
                                }

                                // Discard Option
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MindGlassSurfaceVariant,
                                    border = BorderStroke(0.8.dp, PastelRose.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                            isPlayingAudio = false
                                            viewModel.removeAudio()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = PastelRose, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Discard", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = PastelRose)
                                    }
                                }
                            }
                        }
                    }
                }

                // Attached Image & Caption Card
                if (canUseMedia && uiState.imageUri != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MindGlassSurface,
                        border = BorderStroke(1.dp, MindPrismGlassCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = uiState.imageUri,
                                    contentDescription = "Attached Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                IconButton(
                                    onClick = { viewModel.removeImage() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.imageCaption ?: "",
                                onValueChange = { viewModel.updateImageCaption(it) },
                                placeholder = { Text("Add caption (voice or text)...", color = MindTextTertiary, fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MindGlassSurfaceVariant,
                                    unfocusedContainerColor = MindGlassSurfaceVariant,
                                    focusedBorderColor = PastelLavender,
                                    unfocusedBorderColor = MindGlassBorderSubtle,
                                    focusedTextColor = MindTextPrimary,
                                    unfocusedTextColor = MindTextPrimary
                                ),
                                trailingIcon = if (canUseVoice) {
                                    {
                                        IconButton(onClick = { toggleSpeech(true) }) {
                                            Icon(Icons.Default.Mic, contentDescription = "Dictate", tint = PastelLavender, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (canUseOcr) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Extract Text Button for image
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MindGlassButtonContainer,
                                    border = BorderStroke(1.dp, MindGlassButtonBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isOcrScanning) {
                                            val uriStr = uiState.imageUri ?: return@clickable
                                            val uri = Uri.parse(uriStr)
                                            coroutineScope.launch {
                                                isOcrScanning = true
                                                val result = ocrHelper.extractTextFromImageUri(uri)
                                                result.onSuccess { rawText ->
                                                    if (rawText.isNotBlank()) {
                                                        val (polishedTitle, polishedBody) = brainstormingEngine.polishOcrText(rawText)
                                                        if (uiState.title.isBlank()) viewModel.updateTitle(polishedTitle)
                                                        val merged = if (uiState.content.isBlank()) polishedBody else "${uiState.content}\n\n$polishedBody"
                                                        viewModel.updateContent(merged)
                                                        Toast.makeText(context, "Text extracted and polished into note", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "No text detected in this image", Toast.LENGTH_SHORT).show()
                                                    }
                                                }.onFailure { err ->
                                                    Toast.makeText(context, "OCR failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                                }
                                                isOcrScanning = false
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

                // Title Field
                val editorNoteScale = if (canUseTypography) (uiState.fontSizeScale ?: 1.0f) else 1.0f
                val titleFontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * editorNoteScale).sp
                val titleLineHeight = (titleFontSize.value * 1.35f).sp
                TextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    placeholder = {
                        Text(
                            text = "Title of your thought...",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = titleFontSize,
                                lineHeight = titleLineHeight,
                                fontFamily = currentFontFamily
                            ),
                            color = MindTextTertiary
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = titleFontSize,
                        lineHeight = titleLineHeight,
                        fontWeight = FontWeight.Bold,
                        fontFamily = currentFontFamily
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MindTextPrimary,
                        unfocusedTextColor = MindTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Hashtags Section
                if (canUseHashtags) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.tags.isNotEmpty()) {
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                uiState.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MindGlassButtonContainer,
                                        border = BorderStroke(0.8.dp, MindGlassButtonBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PastelLavender
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove tag",
                                                tint = PastelRose,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.removeTag(tag) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Hashtag Input Bar
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MindGlassSurfaceVariant,
                            border = BorderStroke(1.dp, MindGlassBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Tag, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (uiState.tagInput.isEmpty()) {
                                        Text("Tag multiple hashtags (e.g. #ideas, #work, #journal)...", style = MaterialTheme.typography.bodyMedium, color = MindTextTertiary)
                                    }
                                    BasicTextField(
                                        value = uiState.tagInput,
                                        onValueChange = { viewModel.updateTagInput(it) },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MindTextPrimary),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { viewModel.addTag() }),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (uiState.tagInput.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MindGlassButtonContainer,
                                        border = BorderStroke(0.8.dp, MindGlassButtonBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.addTag() }
                                    ) {
                                        Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PastelLavender, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Action Items & Checklists Card (Action Checklists)
                if (canUseChecklists) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MindGlassSurface,
                        border = BorderStroke(1.dp, PastelMint.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Dynamic Task Candidate Detector from Note Content
                            val detectedTaskCandidates = remember(contentValue.text, uiState.checklistItems, canUseSplitter) {
                                if (!canUseSplitter) emptyList() else
                                contentValue.text.lines().mapNotNull { line ->
                                    val trimmed = line.trim()
                                    val clean = when {
                                        trimmed.startsWith("- [ ]") -> trimmed.removePrefix("- [ ]").trim()
                                        trimmed.startsWith("[ ]") -> trimmed.removePrefix("[ ]").trim()
                                        trimmed.startsWith("TODO:", ignoreCase = true) -> trimmed.substring(5).trim()
                                        trimmed.startsWith("TASK:", ignoreCase = true) -> trimmed.substring(5).trim()
                                        trimmed.startsWith("DO:", ignoreCase = true) -> trimmed.substring(3).trim()
                                        trimmed.startsWith("- ") && trimmed.length > 3 -> trimmed.removePrefix("- ").trim()
                                        trimmed.startsWith("* ") && trimmed.length > 3 -> trimmed.removePrefix("* ").trim()
                                        trimmed.matches(Regex("""^\d+\.\s+.+""")) -> trimmed.replace(Regex("""^\d+\.\s+"""), "").trim()
                                        else -> null
                                    }
                                    if (!clean.isNullOrBlank() && uiState.checklistItems.none { it.text.equals(clean, ignoreCase = true) }) clean else null
                                }
                            }

                            if (canUseSplitter && detectedTaskCandidates.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PastelMint.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, PastelMint.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.addChecklistItems(detectedTaskCandidates)
                                        Toast.makeText(context, "Added ${detectedTaskCandidates.size} task(s) to checklist!", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = PastelMint, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "⚡ Found ${detectedTaskCandidates.size} task(s) in note · Tap to add to Checklist",
                                        color = PastelMint,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TaskAlt, contentDescription = null, tint = PastelMint, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Action Items & Checklists (${uiState.checklistItems.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextPrimary
                                )
                            }

                            // "+ Add Task" button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MindGlassButtonContainer,
                                border = BorderStroke(0.8.dp, MindGlassButtonBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { addNewTask() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = PastelMint, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Add Task", fontSize = 11.5.sp, color = PastelMint, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (uiState.checklistItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.checklistItems.forEach { item ->
                                    val itemFocusRequester = remember { FocusRequester() }

                                    LaunchedEffect(focusedTaskId) {
                                        if (focusedTaskId == item.id) {
                                            itemFocusRequester.requestFocus()
                                            keyboardController?.show()
                                            focusedTaskId = null
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MindGlassSurfaceVariant, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = item.isChecked,
                                            onCheckedChange = { viewModel.toggleChecklistItem(item.id) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = PastelMint,
                                                uncheckedColor = MindTextTertiary
                                            ),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (item.text.isEmpty()) {
                                                Text("Type action item or task...", color = MindTextTertiary, fontSize = 13.sp)
                                            }
                                            BasicTextField(
                                                value = item.text,
                                                onValueChange = { viewModel.updateChecklistItem(item.id, it) },
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                    color = if (item.isChecked) MindTextTertiary else MindTextPrimary,
                                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                                                    fontFamily = currentFontFamily
                                                ),
                                                cursorBrush = SolidColor(PastelMint),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                                keyboardActions = KeyboardActions(onNext = { addNewTask() }),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(itemFocusRequester)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.removeChecklistItem(item.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete Task", tint = MindTextTertiary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }

                HorizontalDivider(color = MindBorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))

                val bodyFontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * editorNoteScale).sp
                val bodyLineHeight = (bodyFontSize.value * activeLineSpacing).sp

                // View Mode Pill: Live Rich Editor vs MS Word Document View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isWordPreviewMode) "MS Word Document View (Formatted)" else "Journal Body (Rich Text)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MindTextTertiary
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isWordPreviewMode) PastelLavender.copy(alpha = 0.25f) else MindGlassButtonContainer,
                        border = BorderStroke(1.dp, if (isWordPreviewMode) PastelLavender else MindGlassButtonBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isWordPreviewMode = !isWordPreviewMode }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isWordPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (isWordPreviewMode) PastelLavender else MindTextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isWordPreviewMode) "Edit Mode" else "Word View",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWordPreviewMode) PastelLavender else MindTextPrimary
                            )
                        }
                    }
                }

                if (isWordPreviewMode) {
                    // MS Word Document View: zero raw markdown punctuation!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 350.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MindGlassSurface),
                        border = BorderStroke(1.dp, MindGlassBorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            RichTextView(
                                text = if (contentValue.text.isBlank()) "*(Empty note body)*" else contentValue.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = bodyFontSize,
                                    lineHeight = bodyLineHeight,
                                    fontFamily = currentFontFamily
                                ),
                                color = MindTextPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Markdown Body Field (Selection-Aware & Real-time Styled!)
                    TextField(
                        value = contentValue,
                        onValueChange = { newValue ->
                            undoRedoManager.record(contentValue)
                            contentValue = newValue
                            viewModel.updateContent(newValue.text)
                            canUndo = undoRedoManager.canUndo
                            canRedo = undoRedoManager.canRedo
                        },
                        visualTransformation = remember(bodyFontSize) {
                            MarkdownVisualTransformation(baseFontSizeSp = bodyFontSize.value)
                        },
                        placeholder = {
                            Text(
                                text = if (canUseVoice || canUseMedia || canUseOcr) {
                                    "Write your thoughts freely. Dictate with the mic, snap photos, or scan pages..."
                                } else {
                                    "Write your thoughts freely..."
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = bodyFontSize,
                                    lineHeight = bodyLineHeight,
                                    fontFamily = currentFontFamily
                                ),
                                color = MindTextTertiary
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = bodyFontSize,
                            lineHeight = bodyLineHeight,
                            fontFamily = currentFontFamily
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MindTextPrimary,
                            unfocusedTextColor = MindTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 350.dp)
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Revisions & Version Diff Dialog
    if (showVersionHistoryDialog) {
        NoteVersionHistoryDialog(
            currentTitle = uiState.title,
            currentContent = contentValue.text,
            versions = uiState.noteVersions,
            onDismiss = { showVersionHistoryDialog = false },
            onRestoreVersion = { version ->
                undoRedoManager.record(contentValue, force = true)
                viewModel.restoreVersion(version)
                contentValue = TextFieldValue(version.content, TextRange(version.content.length))
                canUndo = undoRedoManager.canUndo
                canRedo = undoRedoManager.canRedo
                Toast.makeText(context, "Restored revision from ${version.compactDate}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Audio Transcription Error Notification Dialog
    if (uiState.transcriptionError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearTranscriptionError() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = PastelRose, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audio Transcription", fontWeight = FontWeight.Bold, color = MindTextPrimary)
                }
            },
            text = {
                Text(
                    text = uiState.transcriptionError ?: "Failed to transcribe audio recording.",
                    color = MindTextSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearTranscriptionError() }) {
                    Text("OK", color = PastelLavender, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MindBackgroundAmoled
        )
    }

    // 🎭 Wit & Roast (MindLink Jester Dialog)
    if (showWitDialog) {
        AlertDialog(
            onDismissRequest = { showWitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎭 MindLink Jester", fontWeight = FontWeight.Bold, color = MindTextPrimary, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Style selection pills
                    val styles = listOf("Roast" to "🔥 Roast", "Trailer" to "🎬 Trailer", "Cookie" to "🥠 Fortune", "Pirate" to "🏴‍☠️ Pirate")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        styles.forEach { (key, label) ->
                            val isSelected = witCategory == key
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PastelRose.copy(alpha = 0.25f) else MindGlassSurfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) PastelRose else MindBorderDark.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        witCategory = key
                                        witText = generateMindLinkWit(uiState.title, contentValue.text, uiState.checklistItems.size, key)
                                    }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PastelRose else MindTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // The Witty Output Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MindGlassSurface,
                        border = BorderStroke(1.dp, PastelRose.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = witText.ifBlank { "Tap Reroll to summon a witty remark!" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MindTextPrimary,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                witText = generateMindLinkWit(uiState.title, contentValue.text, uiState.checklistItems.size, witCategory)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PastelSky)
                        ) {
                            Text("🎲 Reroll", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(witText))
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, MindBorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MindTextSecondary)
                        ) {
                            Text("📋 Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val footnote = "\n\n> 🎭 **MindLink Jester:** \"$witText\""
                                val updated = contentValue.text + footnote
                                contentValue = TextFieldValue(updated, TextRange(updated.length))
                                viewModel.updateContent(updated)
                                Toast.makeText(context, "Inserted as footnote!", Toast.LENGTH_SHORT).show()
                                showWitDialog = false
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PastelRose, contentColor = MindBackgroundDark)
                        ) {
                            Text("Insert Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWitDialog = false }) {
                    Text("Close", color = MindTextSecondary)
                }
            },
            containerColor = MindBackgroundAmoled
        )
    }

    // ⚡ Quick Task Splitter Dialog
    if (showTaskSplitterDialog) {
        AlertDialog(
            onDismissRequest = { showTaskSplitterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = PastelSky, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚡ Quick Task Splitter", fontWeight = FontWeight.Bold, color = MindTextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste or type a list of items (one per line) to convert them into interactive checklist items in one tap:",
                        fontSize = 12.5.sp,
                        color = MindTextSecondary
                    )
                    OutlinedTextField(
                        value = taskSplitterInput,
                        onValueChange = { taskSplitterInput = it },
                        placeholder = { Text("Buy almond milk\nCall doctor\nReview pull request\nShip v1.1.9 release", fontSize = 12.sp, color = MindTextTertiary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PastelSky,
                            unfocusedBorderColor = MindBorderDark
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lines = taskSplitterInput.lines()
                            .map { it.trim().removePrefix("- [ ]").removePrefix("- ").removePrefix("* ").trim() }
                            .filter { it.isNotBlank() }
                        if (lines.isNotEmpty()) {
                            viewModel.addChecklistItems(lines)
                            Toast.makeText(context, "Added ${lines.size} tasks!", Toast.LENGTH_SHORT).show()
                        }
                        taskSplitterInput = ""
                        showTaskSplitterDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PastelSky, contentColor = MindBackgroundDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Convert to Tasks", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTaskSplitterDialog = false }) {
                    Text("Cancel", color = MindTextSecondary)
                }
            },
            containerColor = MindBackgroundAmoled
        )
    }
}

fun generateMindLinkWit(title: String, content: String, tasksCount: Int, category: String): String {
    val wordCount = content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    val cleanTitle = title.ifBlank { "Untitled Thought" }
    return when (category) {
        "Roast" -> {
            val list = listOf(
                "A majestic to-do list of $tasksCount tasks. We all know you'll do task #1, feel proud, and reward yourself with a 2-hour YouTube spiral.",
                "Look at these profound thoughts. If overthinking burned calories, you'd be an Olympic decathlete by now.",
                "Writing '$cleanTitle' down definitely counts as productive work... right up until you actually have to do it.",
                "Deep reflections captured with $wordCount words. Groundbreaking philosophy, yet your coffee mug from Tuesday is still sitting there.",
                "Your ambitious plan looks brilliant on screen. Let's see if tomorrow's you agrees, or quietly ignores it.",
                "Ah yes, the sacred art of organizing thoughts into bullet points so they look organized while you procrastinate."
            )
            list.random()
        }
        "Trailer" -> {
            val list = listOf(
                "IN A WORLD where tasks were meant to be done... One visionary wrote '$cleanTitle'. Coming this summer to an app near you.",
                "He had the ambition. He had the markdown editor. What he didn't have... was any intention of waking up early tomorrow.",
                "From the creator of 'I will do it later' comes the epic sequel: '$cleanTitle'. Rated PG-13 for intense procrastination.",
                "One man. One journal. Zero completed action items. This fall... the deadline strikes back."
            )
            list.random()
        }
        "Cookie" -> {
            val list = listOf(
                "Ancient MindLink proverb: A thought written down is halfway to being forgotten until next year. Breathe deeply and take a nap.",
                "The stars align for your great breakthrough on '$cleanTitle'. Unfortunately, your phone battery is at 12%.",
                "Great wisdom lies within your words. Greater wisdom lies in closing your laptop and getting 8 hours of sleep.",
                "Your future self will thank you for this note. Or your future self will stare at it wondering what you were drinking."
            )
            list.random()
        }
        else -> { // Pirate
            val list = listOf(
                "Ahoy matey! Yer logbook says ye got work to do, but dead men tell no tales and procrastinators find no buried doubloons!",
                "Shiver me timbers! That's a mighty fine captain's log for '$cleanTitle'. Now hoist the sails and get to it!",
                "Avast ye! Ye charted the course, now stop staring at the compass and steer the ship!",
                "By Blackbeard's ghost! $tasksCount quests on yer pirate scroll! Which treasure chest are ye plunderin' first?"
            )
            list.random()
        }
    }
}

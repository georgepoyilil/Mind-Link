package com.mindlink.journal.ui.editor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.mindlink.journal.data.ai.LocalHeuristicBrainstormingEngine
import com.mindlink.journal.ui.editor.components.AudioRecordingHelper
import com.mindlink.journal.ui.editor.components.SpeechRecognitionHelper
import com.mindlink.journal.ui.editor.components.SpeechState
import com.mindlink.journal.ui.editor.components.TextRecognitionHelper
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

    // Helpers
    val speechHelper = remember { SpeechRecognitionHelper(context) }
    val speechState by speechHelper.speechState.collectAsState()
    val audioRecorder = remember { AudioRecordingHelper(context) }
    val ocrHelper = remember { TextRecognitionHelper(context) }
    val brainstormingEngine = remember { LocalHeuristicBrainstormingEngine() }

    var dictationTargetIsCaption by remember { mutableStateOf(false) }
    var isOcrScanning by remember { mutableStateOf(false) }
    var hasLaunchedInitialMode by remember { mutableStateOf(false) }

    var activeCameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var activeOcrPhotoUri by remember { mutableStateOf<Uri?>(null) }

    fun createHighResPhotoUri(): Uri {
        val imagesDir = File(context.filesDir, "note_images").apply { if (!exists()) mkdirs() }
        val photoFile = File(imagesDir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    // High-Resolution Camera Launcher for standard Photo Journal
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && activeCameraPhotoUri != null) {
            viewModel.setImage(activeCameraPhotoUri.toString(), "Captured with camera")
        }
    }

    // High-Resolution Camera Launcher for OCR Document / Whiteboard Scanning
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
                        Toast.makeText(context, "Text extracted and polished by AI", Toast.LENGTH_SHORT).show()
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

    // Gallery Image Picker Launcher
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

    // Handle speech state updates
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

    // Microphone permission
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            audioRecorder.startRecording()
            speechHelper.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice journaling", Toast.LENGTH_SHORT).show()
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

    // Camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraHighRes()
        } else {
            Toast.makeText(context, "Camera permission is required to snap photos", Toast.LENGTH_SHORT).show()
        }
    }

    val ocrPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchOcrHighRes()
        } else {
            Toast.makeText(context, "Camera permission is required to scan text", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleSpeech: (Boolean) -> Unit = { isCaption ->
        dictationTargetIsCaption = isCaption
        if (uiState.isVoiceRecording) {
            speechHelper.stopListening()
            val (audioFile, duration) = audioRecorder.stopRecording()
            if (audioFile != null && audioFile.exists() && duration > 500L && !isCaption) {
                viewModel.setAudio(Uri.fromFile(audioFile).toString(), duration)
            }
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                audioRecorder.startRecording()
                speechHelper.startListening()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Handle initial journaling mode triggers
    LaunchedEffect(Unit) {
        if (!hasLaunchedInitialMode) {
            hasLaunchedInitialMode = true
            when (initialMode) {
                "voice" -> toggleSpeech(false)
                "image" -> imagePickerLauncher.launch("image/*")
                "camera" -> {
                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) launchCameraHighRes() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                "ocr" -> {
                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) launchOcrHighRes() else ocrPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.stopListening()
            audioRecorder.cancelRecording()
        }
    }

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MindTextPrimary)
                    }
                },
                actions = {
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
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = PastelLavender, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", fontWeight = FontWeight.Bold, color = MindGlassButtonText, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MindBackgroundAmoled)
            )
        },
        floatingActionButton = {
            // Floating Mic Button with pulse animation when recording
            Box(contentAlignment = Alignment.Center) {
                if (uiState.isVoiceRecording) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(pulseScale)
                            .background(MindRose.copy(alpha = 0.35f), CircleShape)
                    )
                }

                FloatingActionButton(
                    onClick = { toggleSpeech(false) },
                    containerColor = if (uiState.isVoiceRecording) MindRose else MindIndigo,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isVoiceRecording) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        containerColor = MindBackgroundAmoled
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding() // CRITICAL FIX: Keyboard will not obscure input fields
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
                                "Listening & recording voice note... speak naturally",
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
                            text = "Scanning document text on-device & polishing notes...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PastelLavender,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Glassy Prismatic Media Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MindGlassSurfaceVariant)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Gallery
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MindGlassButtonContainer,
                    border = BorderStroke(1.dp, MindGlassButtonBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { imagePickerLauncher.launch("image/*") }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Gallery", fontSize = 12.sp, color = PastelLavender, fontWeight = FontWeight.Bold)
                    }
                }

                // 2. Camera Snap
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MindGlassButtonContainer,
                    border = BorderStroke(1.dp, MindGlassButtonBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) launchCameraHighRes() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = PastelSky, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Camera", fontSize = 12.sp, color = PastelSky, fontWeight = FontWeight.Bold)
                    }
                }

                // 3. Camera OCR Scanner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MindGlassButtonContainer,
                    border = BorderStroke(1.dp, MindGlassButtonBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) launchOcrHighRes() else ocrPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = PastelViolet, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Scan Text", fontSize = 12.sp, color = PastelViolet, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(onClick = { viewModel.insertMarkdown("**", "**") }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = MindTextSecondary)
                }
                IconButton(onClick = { viewModel.insertMarkdown("### ") }) {
                    Text("H3", fontWeight = FontWeight.Bold, color = MindTextSecondary, fontSize = 14.sp)
                }
                IconButton(onClick = { viewModel.insertMarkdown("- [ ] ") }) {
                    Icon(Icons.Default.TaskAlt, contentDescription = "Task Checkbox", tint = PastelMint)
                }
                IconButton(onClick = { viewModel.insertMarkdown("- ") }) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", tint = MindTextSecondary)
                }
                IconButton(onClick = { viewModel.insertMarkdown("> ") }) {
                    Icon(Icons.Default.FormatQuote, contentDescription = "Quote", tint = MindTextSecondary)
                }
            }

            // Note Content Editor Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Attached Voice Note Preview Bar
                if (uiState.audioUri != null) {
                    val durationSec = uiState.audioDurationMs / 1000
                    val formatted = "${durationSec / 60}:${String.format("%02d", durationSec % 60)}"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MindGlassSurfaceElevated,
                        border = BorderStroke(1.dp, MindPrismGlassCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Voice Note Attached",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MindTextPrimary
                                    )
                                    Text(
                                        text = "Duration: $formatted · Saved locally in high-efficiency AAC",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindTextTertiary
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.removeAudio() }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove audio", tint = MindRose)
                            }
                        }
                    }
                }

                // Attached Image & Caption Card
                if (uiState.imageUri != null) {
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

                            // Image Caption with Voice Dictation Button
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
                                trailingIcon = {
                                    IconButton(onClick = { toggleSpeech(true) }) {
                                        Icon(Icons.Default.Mic, contentDescription = "Dictate", tint = PastelLavender, modifier = Modifier.size(18.dp))
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // On-demand OCR for uploaded/captured image (Classy Pastel Glass Button)
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
                                    if (isOcrScanning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = PastelViolet
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Extracting Text with ML Kit...", fontSize = 13.sp, color = PastelViolet, fontWeight = FontWeight.Bold)
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

                // Title Field
                TextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    placeholder = {
                        Text(
                            text = "Title of your thought...",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MindTextTertiary
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Active Selected Tags (Glassy Pastel Pills)
                    if (uiState.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
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

                    // Comfortable, Full-Width Hashtag Input Bar
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
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = PastelLavender,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (uiState.tagInput.isEmpty()) {
                                    Text(
                                        text = "Add hashtag (e.g. ideas, work, journal)...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MindTextTertiary
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
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
                                    Text(
                                        text = "Add",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = PastelLavender,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Suggested Tags to Select
                    val unselectedSuggestions = uiState.suggestedTags.filter { !uiState.tags.contains(it) }
                    if (unselectedSuggestions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Suggested:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextTertiary
                            )
                            unselectedSuggestions.take(8).forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MindGlassSurfaceVariant,
                                    border = BorderStroke(0.5.dp, MindGlassButtonBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.selectSuggestedTag(tag) }
                                ) {
                                    Text(
                                        text = "+ #$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PastelLavender,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MindBorderDark.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Markdown Body Field
                TextField(
                    value = uiState.content,
                    onValueChange = { viewModel.updateContent(it) },
                    placeholder = {
                        Text(
                            text = "Write your thoughts freely. Dictate with the mic, snap photos, or scan documents with OCR...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MindTextTertiary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
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

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

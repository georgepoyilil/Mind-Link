package com.mindlink.journal.ui.secondbrain

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mindlink.journal.LocalLadderState
import com.mindlink.journal.data.ai.ModelRegistry
import com.mindlink.journal.data.capability.CapabilityLadderManager
import com.mindlink.journal.data.worker.ModelDownloadWorker
import com.mindlink.journal.di.ServiceLocator
import com.mindlink.journal.ui.editor.components.SpeechRecognitionHelper
import com.mindlink.journal.ui.editor.components.SpeechState
import com.mindlink.journal.ui.secondbrain.components.ConversationalSynthesisCard
import com.mindlink.journal.ui.secondbrain.components.HybridSearchSection
import com.mindlink.journal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondBrainScreen(
    viewModel: SecondBrainViewModel,
    onNavigateBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val ladderState = LocalLadderState.current
    val showVoice = ladderState.isEnabled(CapabilityLadderManager.FEATURE_VOICE_DICTATION)
    val showEdgeAi = ladderState.isEnabled(CapabilityLadderManager.FEATURE_ON_DEVICE_SLM)

    // Speech helper for voice queries
    val speechHelper = remember { SpeechRecognitionHelper(context) }
    val speechState by speechHelper.speechState.collectAsState()
    var isRecordingQuery by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.stopListening()
        }
    }

    LaunchedEffect(speechState) {
        when (val state = speechState) {
            is SpeechState.Listening -> {
                isRecordingQuery = true
                if (state.partialText.isNotBlank()) {
                    viewModel.updateQuery(state.partialText)
                }
            }
            is SpeechState.Result -> {
                isRecordingQuery = false
                viewModel.updateQuery(state.finalText)
                viewModel.submitQuery(state.finalText)
            }
            is SpeechState.Error -> {
                isRecordingQuery = false
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            is SpeechState.Idle -> {
                isRecordingQuery = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) speechHelper.startListening()
    }

    val toggleVoiceQuery = {
        if (isRecordingQuery) {
            speechHelper.stopListening()
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) speechHelper.startListening() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // On-Device SLM status tracking
    val mediaPipeEngine = remember { ServiceLocator.getInstance(context).brainstormingEngine.mediaPipeEngine }
    var isSlmInstalled by remember { mutableStateOf(mediaPipeEngine.isModelAvailable()) }
    var showSlmDownloadDialog by remember { mutableStateOf(false) }

    val workManager = remember { WorkManager.getInstance(context) }
    val downloadWorkInfos by workManager
        .getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.UNIQUE_WORK_NAME)
        .collectAsState(initial = emptyList())

    val activeDownloadWork = downloadWorkInfos.firstOrNull()
    val isDownloading = activeDownloadWork?.state == WorkInfo.State.RUNNING || activeDownloadWork?.state == WorkInfo.State.ENQUEUED
    val downloadProgress = activeDownloadWork?.progress?.getFloat(ModelDownloadWorker.KEY_PROGRESS, 0f) ?: 0f

    LaunchedEffect(activeDownloadWork?.state) {
        if (activeDownloadWork?.state == WorkInfo.State.SUCCEEDED) {
            mediaPipeEngine.reloadModel()
        }
        isSlmInstalled = mediaPipeEngine.isModelAvailable()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isSlmInstalled = mediaPipeEngine.isModelAvailable()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showEdgeAi && showSlmDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showSlmDownloadDialog = false },
            icon = {
                Icon(
                    imageVector = if (isDownloading) Icons.Default.CloudSync else Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = if (isDownloading) PastelSky else Color(0xFFFB7185),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isDownloading) "Downloading On-Device SLM" else "On-Device SLM Not Downloaded",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isDownloading) {
                        val pct = (downloadProgress * 100).toInt()
                        Text(
                            text = "Downloading Qwen 2.5 0.5B in the background ($pct%). You can continue using MindLink while it installs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MindTextPrimary
                        )
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PastelSky,
                            trackColor = MindGlassBorder
                        )
                    } else {
                        Text(
                            text = "MindLink is currently running in local heuristic mode because the On-Device Small Language Model (SLM) is not installed.\n\n" +
                                "Downloading the SLM (~350 MB) unlocks 100% private, on-device neural synthesis and deep associative intelligence with zero cloud dependency.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MindTextPrimary
                        )
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    Button(
                        onClick = {
                            showSlmDownloadDialog = false
                            val modelToDownload = ModelRegistry.DEFAULT_MODELS.first()
                            val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                                .setInputData(
                                    workDataOf(
                                        ModelDownloadWorker.KEY_MODEL_ID to modelToDownload.id,
                                        ModelDownloadWorker.KEY_MODEL_NAME to modelToDownload.name,
                                        ModelDownloadWorker.KEY_DOWNLOAD_URL to modelToDownload.downloadUrl
                                    )
                                )
                                .build()
                            WorkManager.getInstance(context).enqueueUniqueWork(
                                ModelDownloadWorker.UNIQUE_WORK_NAME,
                                ExistingWorkPolicy.REPLACE,
                                workRequest
                            )
                            Toast.makeText(context, "Background download started for ${modelToDownload.name}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MindIndigo)
                    ) {
                        Text("Download SLM Now")
                    }
                } else {
                    Button(
                        onClick = {
                            showSlmDownloadDialog = false
                            onSettingsClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MindIndigo)
                    ) {
                        Text("View in Settings")
                    }
                }
            },
            dismissButton = {
                Row {
                    if (!isDownloading) {
                        TextButton(onClick = {
                            showSlmDownloadDialog = false
                            onSettingsClick()
                        }) {
                            Text("Open Settings")
                        }
                    }
                    TextButton(onClick = { showSlmDownloadDialog = false }) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Memory Oracle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MindGlassButtonContainer,
                            border = BorderStroke(0.5.dp, MindGlassButtonBorder)
                        ) {
                            Text(
                                text = "Associative Q&A",
                                color = PastelLavender,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // SLM download visibility right next to Associative Q&A (if Edge Mind AI active)
                        if (showEdgeAi && isDownloading) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PastelSky.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, PastelSky.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable { showSlmDownloadDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        strokeWidth = 1.5.dp,
                                        color = PastelSky
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val pct = (downloadProgress * 100).toInt()
                                    Text(
                                        text = if (pct > 0) "SLM $pct%" else "Downloading SLM...",
                                        color = PastelSky,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (showEdgeAi && !isSlmInstalled) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x28F43F5E),
                                border = BorderStroke(0.5.dp, Color(0x66F43F5E)),
                                modifier = Modifier.clickable { showSlmDownloadDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = Color(0xFFFB7185),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SLM Not Downloaded",
                                        color = Color(0xFFFB7185),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MindTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MindBackgroundDark)
            )
        },
        containerColor = MindBackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scrollable Content Area (Above Bottom Query Input)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Prominent Neural Memory Link Status Banner (Warning if offline heuristics)
                val memoryLinkStatus = remember {
                    com.mindlink.journal.di.ServiceLocator.getInstance(context).brainstormingEngine.getMemoryLinkStatus()
                }
                var showSecondBrainAiDialog by remember { mutableStateOf(false) }

                if (showSecondBrainAiDialog) {
                    AlertDialog(
                        onDismissRequest = { showSecondBrainAiDialog = false },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PsychologyAlt,
                                contentDescription = null,
                                tint = Color(0xFFFB7185),
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        title = {
                            Text("Neural Memory Link Offline", fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Text(
                                "MindLink operates with deep neural reasoning when connected to an On-Device Small Language Model (SLM) or Gemini Cloud AI.\n\n" +
                                "Currently, no AI model is active. MindLink is operating in fallback heuristic mode.\n\n" +
                                "Download an edge model in Settings to activate the neural link with zero cloud dependency."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showSecondBrainAiDialog = false
                                    onSettingsClick()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MindIndigo)
                            ) {
                                Text("Open AI Settings")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSecondBrainAiDialog = false }) {
                                Text("Dismiss")
                            }
                        }
                    )
                }

                if (showEdgeAi && memoryLinkStatus.isBroken) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x22F43F5E),
                        border = BorderStroke(1.dp, Color(0x66F43F5E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSecondBrainAiDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOff,
                                contentDescription = null,
                                tint = Color(0xFFFB7185),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⚡ Memory Link Broken · Local Heuristics Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFB7185)
                                )
                                Text(
                                    text = "No on-device SLM or API key. Tap to install model for full associative reasoning.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MindTextTertiary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFFFB7185),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                // Initial State: Hero Card when no search has been run
                if (uiState.synthesisResponse == null && !uiState.isSynthesizing && uiState.hybridResults.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MindGlassSurface),
                        border = BorderStroke(1.dp, MindGlassCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MindGlassButtonContainer,
                                border = BorderStroke(1.dp, MindGlassButtonBorder),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Associative Journal Memory",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MindTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ask your Second Brain questions in natural language. It links past thoughts using 384-dimensional on-device semantic vectors.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MindTextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Dynamic inquiries synthesized from your thoughts:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            uiState.dynamicInquiries.take(4).forEach { inquiry ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MindGlassSurfaceVariant,
                                    border = BorderStroke(0.5.dp, MindGlassBorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.updateQuery(inquiry.query)
                                            viewModel.submitQuery(inquiry.query)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MindGlassButtonContainer,
                                                border = BorderStroke(0.5.dp, MindGlassButtonBorder)
                                            ) {
                                                Text(
                                                    text = inquiry.category,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PastelSky,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = inquiry.display,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MindTextPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Ask",
                                            tint = MindIndigo,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Loading Indicator
                if (uiState.isSynthesizing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MindGlassSurface),
                        border = BorderStroke(1.dp, MindAiGlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = MindIndigo,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Synthesizing Memory Bank...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MindTextPrimary
                                )
                                Text(
                                    text = "Scanning 384-d semantic embeddings & FTS tokens",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindTextTertiary
                                )
                            }
                        }
                    }
                }

                // Error Message
                if (uiState.errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PastelRose.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, PastelRose.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = PastelRose, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = PastelRose,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // 1. DISTINCT AI SYNTHESIS CARD
                uiState.synthesisResponse?.let { synthesis ->
                    ConversationalSynthesisCard(
                        synthesis = synthesis,
                        onNoteClick = onNoteClick
                    )
                }

                // 2. DISTINCT MATCHED RAW JOURNAL ENTRIES
                HybridSearchSection(
                    results = uiState.hybridResults,
                    onNoteClick = onNoteClick
                )
            }

            // Ergonomic Glassy Bottom Query Dock (Within comfortable thumb reach!)
            Surface(
                color = MindGlassBottomDock,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, MindPrismBorder),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Quick Question Pills Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.dynamicInquiries.forEach { inquiry ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MindGlassSurfaceVariant,
                                border = BorderStroke(0.5.dp, MindGlassBorderSubtle),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.updateQuery(inquiry.query)
                                        viewModel.submitQuery(inquiry.query)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = inquiry.display,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MindTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ergonomic Bottom Query Input Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = { viewModel.updateQuery(it) },
                            placeholder = {
                                Text(
                                    text = "Ask your Second Brain anything...",
                                    color = MindTextTertiary,
                                    fontSize = 14.sp
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.submitQuery() }),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MindGlassSurfaceElevated,
                                unfocusedContainerColor = MindGlassSurfaceElevated,
                                focusedBorderColor = MindIndigo,
                                unfocusedBorderColor = MindGlassBorderSubtle,
                                focusedTextColor = MindTextPrimary,
                                unfocusedTextColor = MindTextPrimary
                            ),
                            trailingIcon = if (showVoice) {
                                {
                                    IconButton(onClick = toggleVoiceQuery) {
                                        Icon(
                                            imageVector = if (isRecordingQuery) Icons.Default.MicOff else Icons.Default.Mic,
                                            contentDescription = "Voice Query",
                                            tint = if (isRecordingQuery) MindRose else MindIndigo
                                        )
                                    }
                                }
                            } else null,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Glowing Send Button
                        Surface(
                            shape = CircleShape,
                            color = if (uiState.query.isNotBlank() && !uiState.isSynthesizing) MindIndigo else MindGlassSurfaceVariant,
                            border = BorderStroke(1.dp, if (uiState.query.isNotBlank()) MindViolet else MindGlassBorderSubtle),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(enabled = uiState.query.isNotBlank() && !uiState.isSynthesizing) {
                                    viewModel.submitQuery()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (uiState.query.isNotBlank() && !uiState.isSynthesizing) Color.White else MindTextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

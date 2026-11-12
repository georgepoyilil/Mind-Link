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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
    onNoteClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Speech helper for voice queries
    val speechHelper = remember { SpeechRecognitionHelper(context) }
    val speechState by speechHelper.speechState.collectAsState()
    var isRecordingQuery by remember { mutableStateOf(false) }

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

    val quickQuestions = listOf(
        "💖 How is my love life?" to "how is my love life",
        "🧘 How am I feeling?" to "how am I feeling",
        "⚡ What blockers do I have?" to "What recurring blockers have I noted recently?",
        "👨‍👩‍👦 Reflections on family" to "How do I feel about my family and parents?",
        "💔 Failures & rejections" to "What failures or rejections have I noted?",
        "📋 Pending action items" to "What action items are still open?",
        "🌿 Moments of peace" to "What brings me peace and gratitude?"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🧠 Second Brain",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MindIndigo.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, MindIndigo.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Associative Q&A",
                                color = MindIndigo,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
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
                                color = MindIndigo.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, MindIndigo.copy(alpha = 0.4f)),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🧠", fontSize = 28.sp)
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
                                text = "Try tapping one of these:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            quickQuestions.take(4).forEach { (display, query) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MindGlassSurfaceVariant,
                                    border = BorderStroke(0.5.dp, MindGlassBorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.updateQuery(query)
                                            viewModel.submitQuery(query)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = display,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MindTextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
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
                                    text = "✨ Synthesizing Memory Bank...",
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
                        color = MindRose.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MindRose.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ ${uiState.errorMessage}",
                            color = MindRose,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                        quickQuestions.forEach { (display, query) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MindGlassSurfaceVariant,
                                border = BorderStroke(0.5.dp, MindGlassBorderSubtle),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.updateQuery(query)
                                        viewModel.submitQuery(query)
                                    }
                            ) {
                                Text(
                                    text = display,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MindTextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
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
                            trailingIcon = {
                                IconButton(onClick = toggleVoiceQuery) {
                                    Icon(
                                        imageVector = if (isRecordingQuery) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Voice Query",
                                        tint = if (isRecordingQuery) MindRose else MindIndigo
                                    )
                                }
                            },
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

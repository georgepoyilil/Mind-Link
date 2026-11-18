package com.mindlink.journal.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isKeyVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & AI Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MindTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MindTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MindBackgroundDark)
            )
        },
        containerColor = MindBackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Engine Mode Switcher Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MindSurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Switch Engine",
                            tint = PastelMint,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Processing Engine Switch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Choose how thoughts, summaries, and reflection prompts are synthesized:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MindTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val modes = listOf(
                        Triple("AUTO", "Auto Smart", "Cloud if key -> On-Device SLM -> Local NLP"),
                        Triple("SLM", "On-Device SLM", "Force local Qwen 2.5 neural model (100% offline)"),
                        Triple("GEMINI", "Cloud Gemini", "Force Gemini 2.0 Flash (Requires API key)"),
                        Triple("LOCAL", "Local NLP", "Instant rule-based heuristic with Life Spheres")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEach { (modeKey, modeTitle, modeDesc) ->
                            val isSelected = uiState.engineMode == modeKey
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MindSurfaceVariantDark else Color.Transparent,
                                border = BorderStroke(
                                    width = if (isSelected) 1.2.dp else 0.8.dp,
                                    color = if (isSelected) PastelMint else MindBorderDark.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setEngineMode(modeKey) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.setEngineMode(modeKey) },
                                        colors = RadioButtonDefaults.colors(selectedColor = PastelMint)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = modeTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PastelMint else MindTextPrimary
                                        )
                                        Text(
                                            text = modeDesc,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MindTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. On-Device SLM (MediaPipe Tasks GenAI) & Model Updater Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MindSurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "SLM",
                                tint = PastelMint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "On-Device SLM Model",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MindTextPrimary
                            )
                        }

                        // Refresh / Check for Updates Button
                        IconButton(
                            onClick = { viewModel.checkForModelUpdates() },
                            enabled = !uiState.isCheckingUpdates
                        ) {
                            if (uiState.isCheckingUpdates) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PastelSky, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Check for Updates", tint = PastelSky)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MindSurfaceVariantDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current Model Status:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindTextSecondary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (uiState.isModelInstalled) PastelMint.copy(alpha = 0.15f) else PastelAmber.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (uiState.isModelInstalled) "Active & Ready" else "Not Installed",
                                        color = if (uiState.isModelInstalled) PastelMint else PastelAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = uiState.slmModelInfo,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isModelInstalled) PastelMint else MindTextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Active Engine: ${uiState.activeEngineName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MindTextSecondary
                            )
                        }
                    }

                    if (uiState.updateCheckMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.updateCheckMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = PastelSky
                        )
                    }

                    if (uiState.availableUpdateModel != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MindIndigo.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MindIndigo.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Newer Model Available: ${uiState.availableUpdateModel?.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PastelLavender
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.availableUpdateModel?.description ?: "",
                                    fontSize = 11.sp,
                                    color = MindTextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.downloadModel(uiState.availableUpdateModel) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MindIndigo),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download & Upgrade to ${uiState.availableUpdateModel?.name} (${uiState.availableUpdateModel?.sizeMb} MB)", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Download Progress Bar
                    if (uiState.isDownloadingModel) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = uiState.downloadStatus ?: "Downloading model...",
                                style = MaterialTheme.typography.bodySmall,
                                color = PastelMint
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { uiState.downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = PastelMint,
                                trackColor = MindSurfaceVariantDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!uiState.isModelInstalled && !uiState.isDownloadingModel) {
                            Button(
                                onClick = { viewModel.downloadModel() },
                                colors = ButtonDefaults.buttonColors(containerColor = PastelMint),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download Qwen 2.5 (521 MB)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else if (uiState.isModelInstalled) {
                            OutlinedButton(
                                onClick = { viewModel.deleteModel() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PastelRose),
                                border = BorderStroke(1.dp, PastelRose.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete Local Model", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. Live On-Device SLM Capability Sandbox & Latency Benchmark Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MindSurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Test Sandbox",
                            tint = PastelSky,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test On-Device SLM Prompt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Benchmark the real model running locally on your phone GPU:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MindTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.testPrompt,
                        onValueChange = { viewModel.updateTestPrompt(it) },
                        label = { Text("Test Prompt") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MindSurfaceVariantDark,
                            unfocusedContainerColor = MindSurfaceVariantDark,
                            focusedBorderColor = PastelSky,
                            unfocusedBorderColor = MindBorderDark,
                            focusedTextColor = MindTextPrimary,
                            unfocusedTextColor = MindTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.runTestPrompt() },
                        enabled = !uiState.isTesting && uiState.isModelInstalled,
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSky),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating on device...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Live On-Device Inference", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uiState.testOutput != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MindBackgroundDark,
                            border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Generated Output (Raw SLM):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PastelSky
                                    )
                                    Text(
                                        text = "${uiState.testLatencyMs} ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PastelMint
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = uiState.testOutput ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MindTextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. Google Gemini Flash Configuration Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MindSurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini",
                            tint = MindIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Gemini Flash API",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Used for advanced multi-note synthesis and deep reflection prompts. When empty or offline, MindLink uses the on-device SLM or local NLP engine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = uiState.apiKey,
                        onValueChange = { viewModel.updateApiKey(it) },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = MindTextSecondary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MindSurfaceVariantDark,
                            unfocusedContainerColor = MindSurfaceVariantDark,
                            focusedBorderColor = MindIndigo,
                            unfocusedBorderColor = MindBorderDark,
                            focusedTextColor = MindTextPrimary,
                            unfocusedTextColor = MindTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 5. Memory Bank Stats Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MindSurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Stats",
                            tint = MindIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Second Brain Index Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.totalNotes}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MindIndigo
                            )
                            Text(
                                text = "Total Notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.totalEmbeddings}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MindCyan
                            )
                            Text(
                                text = "Vector Embeddings",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "384-d",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MindViolet
                            )
                            Text(
                                text = "Embedding Dim",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { viewModel.reindexAll() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MindIndigo),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reindex", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-index All Notes in WorkManager", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.seedSampleData() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MindIndigo),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Load Samples", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Load 8 Diverse Sample Thoughts", fontWeight = FontWeight.Bold)
                    }

                    if (uiState.reindexMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.reindexMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MindEmerald
                        )
                    }
                }
            }

            // 6. Architecture & Privacy Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MindSurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Local-First Architecture & Privacy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MindTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• All notes and vector embeddings are stored locally in Room SQLite.\n• Full-text search runs completely offline via SQLite FTS.\n• WorkManager guarantees background note enrichment without UI freezes.\n• Embeddings run via ONNX Runtime Mobile (`all-MiniLM-L6-v2.onnx`).\n• SLMs run on device GPU via Google MediaPipe Tasks GenAI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindTextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

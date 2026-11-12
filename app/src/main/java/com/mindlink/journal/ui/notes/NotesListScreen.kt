package com.mindlink.journal.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.mindlink.journal.domain.model.Note
import com.mindlink.journal.ui.notes.components.NoteCard
import com.mindlink.journal.ui.notes.components.NoteViewMode
import com.mindlink.journal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNoteClick: (Long) -> Unit,
    onCreateNoteClick: (String) -> Unit,
    onSecondBrainClick: () -> Unit,
    onThoughtGraphClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(NoteViewMode.CARDS) }
    var showJournalingMethodsSheet by remember { mutableStateOf(false) }

    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(120)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val filterChips = listOf(
        "Reflective" to "🧘",
        "Analytical" to "🧠",
        "Creative" to "💡",
        "Energized" to "⚡",
        "Stressed" to "🌧️",
        "Calm" to "🌿",
        "Blocker" to "⚡",
        "Milestone" to "🎯",
        "Love" to "💖",
        "AI" to "🤖",
        "Fitness" to "🏃"
    )

    // Essential Space Journaling Methods Modal Bottom Sheet
    if (showJournalingMethodsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showJournalingMethodsSheet = false },
            containerColor = MindSurfaceAmoled,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(MindBorderDark, RoundedCornerShape(2.dp))
                )
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Essential Capture",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MindTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MindIndigo.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MindPrismGlassCardBorder)
                    ) {
                        Text(
                            text = "✨ Intention Space",
                            color = MindIndigo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose your journaling medium for intentional, distraction-free capture.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MindTextTertiary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Method 1: Unified Journal Thought (Text + Camera + Photo + Caption)
                JournalMethodItem(
                    emoji = "✍️",
                    title = "Journal Thought",
                    subtitle = "Write freely, snap camera photos, or attach images with captions",
                    accentColor = MindIndigo,
                    onClick = {
                        showJournalingMethodsSheet = false
                        onCreateNoteClick("text")
                    }
                )

                // Method 2: Voice Audio Journal
                JournalMethodItem(
                    emoji = "🎙️",
                    title = "Voice Audio Note",
                    subtitle = "Stream speech-to-text + record high-efficiency audio locally",
                    accentColor = MindRose,
                    onClick = {
                        showJournalingMethodsSheet = false
                        onCreateNoteClick("voice")
                    }
                )

                // Method 3: Camera OCR Scan & Polish
                JournalMethodItem(
                    emoji = "🔍",
                    title = "Scan Document & OCR",
                    subtitle = "Snap books, whiteboards or papers -> AI polishes into structured notes",
                    accentColor = MindViolet,
                    onClick = {
                        showJournalingMethodsSheet = false
                        onCreateNoteClick("ocr")
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MindBackgroundAmoled)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "MindLink",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                    },
                    actions = {
                        // 1. Google Keep Style Layout Switching Vector Icon
                        IconButton(onClick = {
                            viewMode = when (viewMode) {
                                NoteViewMode.CARDS -> NoteViewMode.COMPACT_LIST
                                NoteViewMode.COMPACT_LIST -> NoteViewMode.FULL_WIDTH
                                NoteViewMode.FULL_WIDTH -> NoteViewMode.CARDS
                            }
                        }) {
                            Icon(
                                imageVector = viewMode.icon,
                                contentDescription = viewMode.label,
                                tint = MindTextPrimary
                            )
                        }

                        // 2. Intelligent Synaptic Network ("i" Icon)
                        IconButton(onClick = onThoughtGraphClick) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Intelligence Graph",
                                tint = MindIndigo
                            )
                        }

                        // 3. Settings Icon
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MindTextSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MindBackgroundAmoled
                    )
                )

                // WhatsApp-Style Persistent Search & Second Brain Bar
                Surface(
                    color = MindGlassSurface,
                    border = BorderStroke(1.dp, MindPrismGlassCardBorder),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            searchFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                searchFocusRequester.requestFocus()
                                keyboardController?.show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MindTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    searchFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                        ) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Ask Second Brain or search thoughts...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MindTextTertiary
                                )
                            }
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MindTextPrimary),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                            )
                        }
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.updateSearchQuery("") },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MindTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        // Second Brain AI Trigger Icon
                        Surface(
                            shape = CircleShape,
                            color = MindIndigo.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, MindIndigo.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onSecondBrainClick)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Second Brain",
                                    tint = MindIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showJournalingMethodsSheet = true },
                containerColor = MindIndigo,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Thought",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = MindBackgroundAmoled
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Chips (Moods + Patterns)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedMood == null,
                    onClick = { viewModel.selectMoodFilter(null) },
                    label = { Text("All (${uiState.notes.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MindIndigo,
                        selectedLabelColor = Color.White,
                        containerColor = MindGlassSurface,
                        labelColor = MindTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (uiState.selectedMood == null) MindIndigo else MindGlassBorderSubtle,
                        selectedBorderColor = MindIndigo,
                        enabled = true,
                        selected = uiState.selectedMood == null
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                filterChips.forEach { (filter, emoji) ->
                    val isSelected = uiState.selectedMood.equals(filter, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectMoodFilter(filter) },
                        label = { Text("$emoji $filter") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MindIndigo,
                            selectedLabelColor = Color.White,
                            containerColor = MindGlassSurface,
                            labelColor = MindTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) MindIndigo else MindGlassBorderSubtle,
                            selectedBorderColor = MindIndigo,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Notes Stream with Active ViewMode
            if (uiState.filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MindGlassSurfaceVariant,
                            border = BorderStroke(1.dp, MindPrismGlassCardBorder),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "✨", fontSize = 32.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "🔍 No thoughts match your search" else "Essential Journal is ready",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MindTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty())
                                "Try broader keywords or ask the Second Brain."
                            else
                                "Tap ➕ to capture a new thought, voice note, photo, or scanned document.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MindTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        if (uiState.notes.isEmpty()) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { viewModel.seedSampleData() },
                                colors = ButtonDefaults.buttonColors(containerColor = MindIndigo),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Load Sample Thoughts", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(if (viewMode == NoteViewMode.COMPACT_LIST) 6.dp else 8.dp)
                ) {
                    items(
                        items = uiState.filteredNotes,
                        key = { it.id }
                    ) { note ->
                        NoteCard(
                            note = note,
                            viewMode = viewMode,
                            onClick = { onNoteClick(note.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JournalMethodItem(
    emoji: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MindGlassSurface,
        border = BorderStroke(1.dp, MindPrismGlassCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 22.sp)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MindTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MindTextTertiary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MindTextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

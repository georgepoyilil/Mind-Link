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
import androidx.compose.ui.graphics.vector.ImageVector
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
        "Reflective",
        "Analytical",
        "Creative",
        "Energized",
        "Stressed",
        "Calm",
        "Blocker",
        "Milestone",
        "Love",
        "AI",
        "Fitness"
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
                        color = MindGlassButtonContainer,
                        border = BorderStroke(1.dp, MindGlassButtonBorder)
                    ) {
                        Text(
                            text = "Intention Space",
                            color = PastelLavender,
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
                    icon = Icons.Default.EditNote,
                    title = "Journal Thought",
                    subtitle = "Write freely, snap camera photos, or attach images with captions",
                    accentColor = PastelLavender,
                    onClick = {
                        showJournalingMethodsSheet = false
                        onCreateNoteClick("text")
                    }
                )

                // Method 2: Voice Audio Journal
                JournalMethodItem(
                    icon = Icons.Default.Mic,
                    title = "Voice Audio Note",
                    subtitle = "Stream speech-to-text + record high-efficiency audio locally",
                    accentColor = PastelRose,
                    onClick = {
                        showJournalingMethodsSheet = false
                        onCreateNoteClick("voice")
                    }
                )

                // Method 3: Camera OCR Scan & Polish
                JournalMethodItem(
                    icon = Icons.Default.DocumentScanner,
                    title = "Scan Document & OCR",
                    subtitle = "Snap books, whiteboards or papers -> AI polishes into structured notes",
                    accentColor = PastelViolet,
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

                // Journal / Archive / Trash Tab Selector (Glassy Pastel Pills)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotesTab.values().forEach { tab ->
                        val isSelected = uiState.selectedTab == tab
                        val count = when (tab) {
                            NotesTab.JOURNAL -> uiState.notes.size
                            NotesTab.ARCHIVE -> uiState.archivedNotes.size
                            NotesTab.TRASH -> uiState.trashNotes.size
                        }
                        val icon = when (tab) {
                            NotesTab.JOURNAL -> Icons.Default.Notes
                            NotesTab.ARCHIVE -> Icons.Default.Archive
                            NotesTab.TRASH -> Icons.Default.DeleteOutline
                        }
                        val accent = when (tab) {
                            NotesTab.JOURNAL -> PastelLavender
                            NotesTab.ARCHIVE -> PastelAmber
                            NotesTab.TRASH -> PastelRose
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accent.copy(alpha = 0.2f) else MindGlassSurface,
                            border = BorderStroke(1.dp, if (isSelected) accent.copy(alpha = 0.5f) else MindGlassBorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectTab(tab) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) accent else MindTextTertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${tab.label} ($count)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) accent else MindTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (uiState.selectedTab == NotesTab.JOURNAL) {
                FloatingActionButton(
                    onClick = { showJournalingMethodsSheet = true },
                    containerColor = PastelLavender,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Thought",
                        modifier = Modifier.size(28.dp)
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
        ) {
            when (uiState.selectedTab) {
                NotesTab.JOURNAL -> {
                    // Filter Chips (Moods + Patterns - Clean Typography)
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
                                selectedContainerColor = PastelLavender,
                                selectedLabelColor = Color.Black,
                                containerColor = MindGlassSurface,
                                labelColor = MindTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (uiState.selectedMood == null) PastelLavender else MindGlassBorderSubtle,
                                selectedBorderColor = PastelLavender,
                                enabled = true,
                                selected = uiState.selectedMood == null
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )

                        filterChips.forEach { filter ->
                            val isSelected = uiState.selectedMood.equals(filter, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectMoodFilter(filter) },
                                label = { Text(filter) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PastelLavender,
                                    selectedLabelColor = Color.Black,
                                    containerColor = MindGlassSurface,
                                    labelColor = MindTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) PastelLavender else MindGlassBorderSubtle,
                                    selectedBorderColor = PastelLavender,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
                NotesTab.ARCHIVE -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PastelAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, PastelAmber.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = PastelAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Archived Thoughts are stored safely outside your active stream",
                                style = MaterialTheme.typography.bodySmall,
                                color = PastelAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                NotesTab.TRASH -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PastelRose.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, PastelRose.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = PastelRose, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Notes in Trash auto-purge after 30 days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PastelRose,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (uiState.trashNotes.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PastelRose.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, PastelRose.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.emptyTrash() }
                                ) {
                                    Text(
                                        text = "Empty Trash",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PastelRose,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
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
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val emptyIcon = when {
                                    uiState.searchQuery.isNotEmpty() -> Icons.Default.SearchOff
                                    uiState.selectedTab == NotesTab.TRASH -> Icons.Default.DeleteOutline
                                    uiState.selectedTab == NotesTab.ARCHIVE -> Icons.Default.Archive
                                    else -> Icons.Default.AutoAwesome
                                }
                                val iconTint = when (uiState.selectedTab) {
                                    NotesTab.TRASH -> PastelRose
                                    NotesTab.ARCHIVE -> PastelAmber
                                    else -> PastelLavender
                                }
                                Icon(
                                    imageVector = emptyIcon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when {
                                uiState.searchQuery.isNotEmpty() -> "No thoughts match your search"
                                uiState.selectedTab == NotesTab.TRASH -> "Trash is empty"
                                uiState.selectedTab == NotesTab.ARCHIVE -> "No archived thoughts"
                                else -> "Essential Journal is ready"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MindTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when {
                                uiState.searchQuery.isNotEmpty() -> "Try broader keywords or ask the Second Brain."
                                uiState.selectedTab == NotesTab.TRASH -> "Deleted notes will appear here and auto-purge after 30 days."
                                uiState.selectedTab == NotesTab.ARCHIVE -> "Archive notes you want to keep without cluttering your main stream."
                                else -> "Tap the add button below to capture a new thought, voice note, photo, or document."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MindTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        if (uiState.notes.isEmpty() && uiState.selectedTab == NotesTab.JOURNAL) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MindGlassButtonContainer,
                                border = BorderStroke(1.dp, MindGlassButtonBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.seedSampleData() }
                            ) {
                                Text(
                                    text = "Load Sample Thoughts",
                                    fontWeight = FontWeight.Bold,
                                    color = PastelLavender,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(if (viewMode == NoteViewMode.COMPACT_LIST) 6.dp else 10.dp)
                ) {
                    items(
                        items = uiState.filteredNotes,
                        key = { it.id },
                        contentType = { "note_item" }
                    ) { note ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            NoteCard(
                                note = note,
                                viewMode = viewMode,
                                onClick = { onNoteClick(note.id) }
                            )

                            // Quick actions under cards for Trash or Archive
                            if (uiState.selectedTab == NotesTab.TRASH) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = PastelRose, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Expires in ${note.daysUntilExpiry} days",
                                            fontSize = 11.sp,
                                            color = PastelRose,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PastelMint.copy(alpha = 0.16f),
                                            border = BorderStroke(0.6.dp, PastelMint.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.restoreFromTrash(note.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null, tint = PastelMint, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Restore", fontSize = 11.sp, color = PastelMint, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PastelRose.copy(alpha = 0.16f),
                                            border = BorderStroke(0.6.dp, PastelRose.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.deletePermanently(note.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = PastelRose, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Delete", fontSize = 11.sp, color = PastelRose, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else if (uiState.selectedTab == NotesTab.ARCHIVE) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PastelAmber.copy(alpha = 0.16f),
                                        border = BorderStroke(0.6.dp, PastelAmber.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.unarchiveNote(note.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Unarchive, contentDescription = null, tint = PastelAmber, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Unarchive", fontSize = 11.sp, color = PastelAmber, fontWeight = FontWeight.Bold)
                                        }
                                    }
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
fun JournalMethodItem(
    icon: ImageVector,
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
                color = accentColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
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

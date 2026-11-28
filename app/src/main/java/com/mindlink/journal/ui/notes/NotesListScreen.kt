package com.mindlink.journal.ui.notes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.mindlink.journal.data.capability.CapabilityLadderManager
import com.mindlink.journal.di.ServiceLocator
import com.mindlink.journal.domain.model.Note
import com.mindlink.journal.ui.components.MindLinkOnboardingDialog
import com.mindlink.journal.ui.components.liquidGlass
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
    onSettingsClick: () -> Unit,
    onTagExplorerClick: (String?) -> Unit = {},
    onComplexityLadderClick: () -> Unit = {},
    onFeatureRequestsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val serviceLocator = remember { ServiceLocator.getInstance(context) }
    val capabilityManager = remember { serviceLocator.capabilityLadderManager }
    val ladderState by capabilityManager.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val showVoice = ladderState.isEnabled(CapabilityLadderManager.FEATURE_VOICE_DICTATION)
    val showOcr = ladderState.isEnabled(CapabilityLadderManager.FEATURE_OCR)
    val showMedia = ladderState.isEnabled(CapabilityLadderManager.FEATURE_MEDIA_CAPTURE)
    val showHashtags = ladderState.isEnabled(CapabilityLadderManager.FEATURE_HASHTAGS)
    val showLayoutSwitch = ladderState.isEnabled(CapabilityLadderManager.FEATURE_LAYOUT_SWITCH)
    val showSecondBrain = ladderState.isEnabled(CapabilityLadderManager.FEATURE_SECOND_BRAIN)
    val showGraph = ladderState.isEnabled(CapabilityLadderManager.FEATURE_SYNAPTIC_GRAPH)
    val showCloudBackup = ladderState.isEnabled(CapabilityLadderManager.FEATURE_CLOUD_BACKUP)
    val showEdgeAi = ladderState.isEnabled(CapabilityLadderManager.FEATURE_ON_DEVICE_SLM)

    val savedViewModeStr by serviceLocator.noteViewMode.collectAsState()
    val initialViewMode = remember(savedViewModeStr) {
        when (savedViewModeStr) {
            "grid" -> NoteViewMode.GRID
            "cards" -> NoteViewMode.CARDS
            "compact" -> NoteViewMode.COMPACT_LIST
            else -> NoteViewMode.GRID
        }
    }
    val uiState by viewModel.uiState.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(initialViewMode) }
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

    LaunchedEffect(Unit) {
        viewModel.checkForOtaUpdates(context)
    }

    // Evaluate progressive capability unlocks & intelligent suggestions based on note content
    LaunchedEffect(uiState.notes) {
        capabilityManager.evaluateProgress(uiState.notes)
    }

    val ladderIntroPrefs = remember {
        context.getSharedPreferences("mindlink_capability_ladder", android.content.Context.MODE_PRIVATE)
    }
    var showProgressiveIntroBanner by remember {
        mutableStateOf(!ladderIntroPrefs.getBoolean("pref_seen_progressive_intro_v1", false))
    }
    fun markIntroSeen() {
        ladderIntroPrefs.edit().putBoolean("pref_seen_progressive_intro_v1", true).apply()
        showProgressiveIntroBanner = false
    }

    // Non-interruptive toast when a new complexity level unlocks
    LaunchedEffect(Unit) {
        capabilityManager.unlockEvents.collect { newTier ->
            val res = snackbarHostState.showSnackbar(
                message = "✨ Unlocked Level ${newTier.tier}: ${newTier.title}!",
                actionLabel = "Open Blueprint",
                duration = SnackbarDuration.Short
            )
            if (res == SnackbarResult.ActionPerformed) {
                onComplexityLadderClick()
            }
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

    val allDynamicTags = remember(uiState.notes) {
        val counts = mutableMapOf<String, Int>()
        uiState.notes.forEach { note ->
            note.tags.forEach { rawTag ->
                val clean = rawTag.trim().removePrefix("#")
                if (clean.isNotBlank()) {
                    counts[clean] = (counts[clean] ?: 0) + 1
                }
            }
        }
        counts.toList().sortedByDescending { it.second }
    }

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

                // Method 1: Unified Journal Thought (Text + optional Media)
                JournalMethodItem(
                    icon = Icons.Default.EditNote,
                    title = "Journal Thought",
                    subtitle = if (showMedia) "Write freely, snap camera photos, or attach images with captions" else "Write freely in a distraction-free markdown stream",
                    accentColor = PastelLavender,
                    onClick = {
                        showJournalingMethodsSheet = false
                        onCreateNoteClick("text")
                    }
                )

                // Method 2: Voice Audio Journal (Voice Studio)
                if (showVoice) {
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
                }

                // Method 3: Camera OCR Scan & Polish (Page Scanner)
                if (showOcr) {
                    JournalMethodItem(
                        icon = Icons.Default.DocumentScanner,
                        title = "Page Scanner (OCR)",
                        subtitle = "Snap books, whiteboards or papers -> polish into structured notes",
                        accentColor = PastelViolet,
                        onClick = {
                            showJournalingMethodsSheet = false
                            onCreateNoteClick("ocr")
                        }
                    )
                }
            }
        }
    }

    val googleUser by serviceLocator.googleSyncManager.currentUser.collectAsState()

    val accountChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data != null) {
            val res = serviceLocator.googleSyncManager.handleSignInResult(result.data)
            if (res.isSuccess) {
                coroutineScope.launch {
                    val currentNotes = serviceLocator.database.noteDao().getAllNotesListSync()
                    if (currentNotes.isEmpty()) {
                        serviceLocator.googleSyncManager.restoreNotesFromGoogleDrive()
                    } else {
                        serviceLocator.googleSyncManager.createBackup()
                    }
                }
                viewModel.dismissOnboarding()
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val res = serviceLocator.googleSyncManager.handleSignInResult(result.data)
        if (res.isSuccess) {
            coroutineScope.launch {
                val currentNotes = serviceLocator.database.noteDao().getAllNotesListSync()
                if (currentNotes.isEmpty()) {
                    serviceLocator.googleSyncManager.restoreNotesFromGoogleDrive()
                } else {
                    serviceLocator.googleSyncManager.createBackup()
                }
            }
            viewModel.dismissOnboarding()
        } else {
            val fallback = serviceLocator.googleSyncManager.getAccountChooserIntent()
            if (fallback != null) accountChooserLauncher.launch(fallback)
        }
    }

    // First-Time User Onboarding Modal
    if (uiState.showOnboarding) {
        MindLinkOnboardingDialog(
            onDismiss = {
                viewModel.dismissOnboarding()
                markIntroSeen()
            },
            onLoadSamples = { viewModel.loadSampleThoughtsAndDismiss() },
            onOpenBlueprint = {
                viewModel.dismissOnboarding()
                markIntroSeen()
                onComplexityLadderClick()
            },
            onGoogleSignIn = {
                val chooser = serviceLocator.googleSyncManager.getAccountChooserIntent()
                if (chooser != null) {
                    accountChooserLauncher.launch(chooser)
                } else {
                    googleSignInLauncher.launch(serviceLocator.googleSyncManager.getSignInIntent())
                }
            },
            googleUser = googleUser
        )
    }

    val isHeaderDark = LocalMindLinkColors.current.isDark
    val topBarBg = if (MindIsGlassy) {
        if (isHeaderDark) Color(0xF8070B14) else Color(0xF8E8EEF7)
    } else MindBackgroundAmoled

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val memoryLinkStatus = remember(serviceLocator) {
        serviceLocator.brainstormingEngine.getMemoryLinkStatus()
    }
    var showMemoryLinkInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = if (uiState.selectedTab == NotesTab.TRASH) "Permanently delete ${uiState.selectedNoteIds.size} thoughts?" else "Move ${uiState.selectedNoteIds.size} thoughts to Trash?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (uiState.selectedTab == NotesTab.TRASH) "This action is permanent and cannot be undone." else "Thoughts in Trash will automatically be purged after 30 days."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteSelectedNotes()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PastelRose)
                ) {
                    Text(if (uiState.selectedTab == NotesTab.TRASH) "Delete Permanently" else "Move to Trash", color = Color(0xFF1E293B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMemoryLinkInfoDialog) {
        AlertDialog(
            onDismissRequest = { showMemoryLinkInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.PsychologyAlt,
                    contentDescription = null,
                    tint = Color(0xFFFB7185),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Memory Link Broken", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "MindLink operates with deep neural reasoning when connected to an On-Device Small Language Model (SLM) or Gemini Cloud AI.\n\n" +
                    "Currently, no AI model is active. MindLink is safely operating on local heuristic pattern matching.\n\n" +
                    "Download a 100% offline edge model in Settings to activate the neural intelligence link with zero cloud dependency."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMemoryLinkInfoDialog = false
                        onSettingsClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MindIndigo)
                ) {
                    Text("Open AI Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMemoryLinkInfoDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = if (isHeaderDark) Color(0xFF0B0F19) else Color(0xFFF1F5F9),
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                    // 1. App Header with Icon & Subtitle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, PastelLavender.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(com.mindlink.journal.R.drawable.ic_launcher_foreground),
                                contentDescription = "MindLink Logo",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MindLink",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MindTextPrimary
                            )
                            Text(
                                text = "Intelligent Neural Journal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. Google Account Status Card (Only if Cloud Vault is active)
                    if (showCloudBackup) {
                        if (googleUser != null) {
                            val initial = (googleUser!!.displayName.firstOrNull() ?: googleUser!!.email.firstOrNull() ?: 'G').uppercaseChar()
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MindGlassSurfaceVariant,
                                border = BorderStroke(0.8.dp, MindGlassBorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch { drawerState.close() }
                                        onSettingsClick()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    listOf(Color(0xFF4285F4), Color(0xFF34A853))
                                                ),
                                                shape = CircleShape
                                            )
                                            .border(1.2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initial.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = googleUser!!.displayName.ifBlank { "Google User" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MindTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = googleUser!!.email,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MindTextTertiary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Synced",
                                        tint = MindEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MindGlassSurfaceVariant,
                                border = BorderStroke(0.8.dp, MindGlassBorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch { drawerState.close() }
                                        onSettingsClick()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = PastelSky,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Cloud Vault",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MindTextPrimary
                                        )
                                        Text(
                                            text = "Sign in to enable Drive snapshots",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MindTextTertiary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 3. AI Status Badge inside Drawer (Only when Memory Oracle or Edge Mind AI is active)
                    if (showSecondBrain || showEdgeAi) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (memoryLinkStatus.isBroken) Color(0x22F43F5E) else Color(0x2210B981),
                            border = BorderStroke(0.8.dp, if (memoryLinkStatus.isBroken) Color(0x66F43F5E) else Color(0x6610B981)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch { drawerState.close() }
                                    if (memoryLinkStatus.isBroken) {
                                        showMemoryLinkInfoDialog = true
                                    } else {
                                        onSettingsClick()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (memoryLinkStatus.isBroken) Icons.Default.FlashOff else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (memoryLinkStatus.isBroken) Color(0xFFFB7185) else MindEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = memoryLinkStatus.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (memoryLinkStatus.isBroken) Color(0xFFFB7185) else MindEmerald
                                    )
                                    Text(
                                        text = memoryLinkStatus.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindTextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    HorizontalDivider(color = MindBorderDark.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Navigation Drawer Items
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Notes, contentDescription = null, tint = if (uiState.selectedTab == NotesTab.JOURNAL) MindIndigo else MindTextSecondary) },
                        label = { Text("Journal Thoughts (${uiState.notes.size})", fontWeight = if (uiState.selectedTab == NotesTab.JOURNAL) FontWeight.Bold else FontWeight.Medium) },
                        selected = uiState.selectedTab == NotesTab.JOURNAL,
                        onClick = {
                            viewModel.selectTab(NotesTab.JOURNAL)
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    if (showSecondBrain) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Psychology, contentDescription = null, tint = MindIndigo) },
                            label = { Text("Memory Oracle (Q&A)", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onSecondBrainClick()
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    if (showGraph) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Hub, contentDescription = null, tint = PastelSky) },
                            label = { Text("Thought Constellation", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onThoughtGraphClick()
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PastelLavender) },
                        label = { Text("Complexity Blueprint (L${ladderState.currentTier})", fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            onComplexityLadderClick()
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    // Quick 1-Tap Selectable Level Strip inside Drawer (L0..L5)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CapabilityLadderManager.TIERS.forEach { lvl ->
                            val isSelected = ladderState.currentTier == lvl.tier
                            val accent = Color(lvl.accentHex)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) accent.copy(alpha = 0.25f) else MindGlassSurfaceVariant,
                                border = BorderStroke(
                                    width = if (isSelected) 1.3.dp else 0.7.dp,
                                    color = if (isSelected) accent else MindGlassBorderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { capabilityManager.selectActiveLevel(lvl.tier) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "L${lvl.tier}",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) accent else MindTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    if (ladderState.currentTier >= 2 || ladderState.activeNotesCount >= 5) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = PastelSky) },
                            label = { Text("Creator Portal & Proposals", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onFeatureRequestsClick()
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Archive, contentDescription = null, tint = if (uiState.selectedTab == NotesTab.ARCHIVE) PastelAmber else MindTextSecondary) },
                        label = { Text("Archived (${uiState.archivedNotes.size})", fontWeight = if (uiState.selectedTab == NotesTab.ARCHIVE) FontWeight.Bold else FontWeight.Medium) },
                        selected = uiState.selectedTab == NotesTab.ARCHIVE,
                        onClick = {
                            viewModel.selectTab(NotesTab.ARCHIVE)
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = if (uiState.selectedTab == NotesTab.TRASH) PastelRose else MindTextSecondary) },
                        label = { Text("Trash (${uiState.trashNotes.size})", fontWeight = if (uiState.selectedTab == NotesTab.TRASH) FontWeight.Bold else FontWeight.Medium) },
                        selected = uiState.selectedTab == NotesTab.TRASH,
                        onClick = {
                            viewModel.selectTab(NotesTab.TRASH)
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    // 5. Hashtags & Topics Section (Max 7 Tags + "View All Tags" Screen)
                    if (showHashtags) {
                        HorizontalDivider(
                            color = MindBorderDark.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOP HASHTAGS (MAX 7)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MindTextTertiary,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.selectedTags.isNotEmpty() || uiState.selectedMood != null) {
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PastelSky,
                                        modifier = Modifier
                                            .clickable { viewModel.clearFilters() }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "All Tags (${allDynamicTags.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelLavender,
                                    modifier = Modifier
                                        .clickable {
                                            coroutineScope.launch { drawerState.close() }
                                            onTagExplorerClick(null)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // "All" Thoughts Chip
                            val isAllSelected = uiState.selectedTags.isEmpty() && uiState.selectedMood == null && uiState.selectedTab == NotesTab.JOURNAL
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAllSelected) PastelLavender else MindGlassSurfaceVariant,
                                border = BorderStroke(1.dp, if (isAllSelected) PastelLavender else MindGlassBorderSubtle),
                                modifier = Modifier.clickable {
                                    viewModel.selectTab(NotesTab.JOURNAL)
                                    viewModel.clearFilters()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = if (isAllSelected) Color.Black else MindTextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "All (${uiState.notes.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAllSelected) Color.Black else MindTextPrimary
                                    )
                                }
                            }

                            // Strictly Top 7 Dynamic Hashtags
                            allDynamicTags.take(7).forEach { (tagName, count) ->
                                val isSelected = uiState.selectedTags.any { it.equals(tagName, ignoreCase = true) }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) PastelLavender else MindGlassSurfaceVariant,
                                    border = BorderStroke(1.dp, if (isSelected) PastelLavender else MindGlassBorderSubtle),
                                    modifier = Modifier.clickable {
                                        viewModel.selectTab(NotesTab.JOURNAL)
                                        viewModel.toggleTagFilter(tagName)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#$tagName",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.Black else MindTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$count",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black.copy(alpha = 0.7f) else MindTextTertiary
                                        )
                                    }
                                }
                            }
                        }

                        // "View All Tags" Dedicated Pill
                        if (allDynamicTags.size > 7) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PastelLavender.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch { drawerState.close() }
                                        onTagExplorerClick(null)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Tag, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "View All Tags (${allDynamicTags.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PastelLavender
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                } // End inner scrollable Column

                HorizontalDivider(color = MindBorderDark.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. Drawer Footer: Theme Toggle & Settings
                    val currentAppThemeId by serviceLocator.appTheme.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MindGlassSurfaceVariant,
                            border = BorderStroke(0.8.dp, MindGlassBorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val targetTheme = if (isHeaderDark) {
                                        if (currentAppThemeId == "ios_glassy") "ios_glassy_light" else "light_clean"
                                    } else {
                                        if (currentAppThemeId == "ios_glassy_light") "ios_glassy" else "amoled_black"
                                    }
                                    serviceLocator.updateAppTheme(targetTheme)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isHeaderDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = if (isHeaderDark) PastelAmber else MindTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHeaderDark) "Light" else "Dark",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MindGlassSurfaceVariant,
                            border = BorderStroke(0.8.dp, MindGlassBorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    coroutineScope.launch { drawerState.close() }
                                    onSettingsClick()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MindTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(topBarBg)) {
                    if (uiState.isSelectionMode) {
                        // Contextual Action Top Bar for Multi-Selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.clearSelection() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit Selection",
                                    tint = MindTextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "${uiState.selectedNoteIds.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MindTextPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { viewModel.selectAll() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Select All",
                                    tint = MindTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (uiState.selectedTab == NotesTab.JOURNAL) {
                                IconButton(
                                    onClick = { viewModel.togglePinSelectedNotes() },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pin / Unpin Selected",
                                        tint = PastelAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Selected",
                                    tint = PastelRose,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        // Streamlined 1-Row Unified Top Bar: [ ☰ Drawer ] + [ Search Bar & Second Brain ] + [ Layout Switcher ] + [ Constellation ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Drawer Hamburger Button (☰)
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Drawer",
                                    tint = MindTextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // 2. Integrated Search Bar (center, fills weight, with clear and Second Brain inside)
                            Surface(
                                color = if (MindIsGlassy) Color.Transparent else MindGlassSurface,
                                border = if (MindIsGlassy) null else BorderStroke(1.dp, MindPrismGlassCardBorder),
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(22.dp),
                                        cornerRadius = 22.dp,
                                        isGlassy = MindIsGlassy,
                                        isDark = isHeaderDark,
                                        cardBackground = MindGlassCardBackground,
                                        cardBorder = MindGlassCardBorder
                                    )
                                    .clickable {
                                        searchFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MindTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
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
                                                text = if (showHashtags) "Search thoughts, tags..." else "Search thoughts...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MindTextTertiary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = MindTextTertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else if (showSecondBrain) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MindIndigo.copy(alpha = 0.2f),
                                            border = BorderStroke(0.5.dp, MindIndigo.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .clickable(onClick = onSecondBrainClick)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Psychology,
                                                    contentDescription = "Memory Oracle",
                                                    tint = MindIndigo,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Feed Lenses (Layout Switching Button)
                            if (showLayoutSwitch) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        val nextMode = when (viewMode) {
                                            NoteViewMode.GRID -> NoteViewMode.CARDS
                                            NoteViewMode.CARDS -> NoteViewMode.COMPACT_LIST
                                            NoteViewMode.COMPACT_LIST -> NoteViewMode.GRID
                                        }
                                        viewMode = nextMode
                                        serviceLocator.updateNoteViewMode(
                                            when (nextMode) {
                                                NoteViewMode.GRID -> "grid"
                                                NoteViewMode.CARDS -> "cards"
                                                NoteViewMode.COMPACT_LIST -> "compact"
                                            }
                                        )
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = viewMode.icon,
                                        contentDescription = viewMode.label,
                                        tint = MindTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // 4. Thought Constellation Button
                            if (showGraph) {
                                Spacer(modifier = Modifier.width(2.dp))
                                IconButton(
                                    onClick = onThoughtGraphClick,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                color = if (isHeaderDark) Color(0x33818CF8) else Color(0x206366F1),
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isHeaderDark) Color(0x55A5B4FC) else Color(0x406366F1),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(com.mindlink.journal.R.drawable.ic_thought_constellation),
                                            contentDescription = "Thought Constellation",
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Prominent "Memory Link Broken" Warning Banner (Only if Edge AI or Memory Oracle is active)
                    if ((showSecondBrain || showEdgeAi) && memoryLinkStatus.isBroken) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x22F43F5E),
                            border = BorderStroke(1.dp, Color(0x66F43F5E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 3.dp)
                                .clickable { showMemoryLinkInfoDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOff,
                                    contentDescription = null,
                                    tint = Color(0xFFFB7185),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "⚡ Memory Link Broken · Local Heuristics Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFB7185)
                                    )
                                    Text(
                                        text = "On-Device SLM or API key offline. Tap to activate neural intelligence.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindTextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFFFB7185),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Search Active Header Counter Pill
                    if (uiState.searchQuery.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MindGlassSurfaceVariant,
                            border = BorderStroke(0.6.dp, MindGlassBorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FindInPage, contentDescription = null, tint = PastelSky, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Search: ${uiState.filteredNotes.size} match${if (uiState.filteredNotes.size == 1) "" else "es"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MindTextPrimary
                                    )
                                }
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PastelSky,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { viewModel.updateSearchQuery("") }
                                )
                            }
                        }
                    }

                // 4. Active Tag / Mood Filter Indicator (Only shown if a filter is active)
                if (uiState.selectedTags.isNotEmpty() || uiState.selectedMood != null) {
                    val activeFilterText = if (uiState.selectedTags.isNotEmpty()) {
                        uiState.selectedTags.joinToString(" ") { if (it.startsWith("#")) it else "#$it" }
                    } else {
                        uiState.selectedMood?.let { "#$it" } ?: ""
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PastelLavender.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filtered by ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindTextSecondary
                                )
                                Text(
                                    text = activeFilterText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Filter",
                                    tint = MindTextSecondary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.clearFilters() }
                                )
                            }
                        }
                        Text(
                            text = "${uiState.filteredNotes.size} thoughts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MindTextTertiary
                        )
                    }
                }

                // 5. In-App OTA Update Notification Banner
                AnimatedVisibility(
                    visible = uiState.availableUpdate != null && !uiState.isOtaDismissed,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val update = uiState.availableUpdate
                    if (update != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MindGlassSurface,
                            border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = "New Release",
                                        tint = PastelSky,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MindLink ${update.tagName} Available",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MindTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissOtaUpdate() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MindTextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = update.changelog.ifBlank { "New version available with speed and security optimizations." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MindTextSecondary,
                                    maxLines = 2,
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { viewModel.dismissOtaUpdate() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Later", fontSize = 11.sp, color = MindTextTertiary)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = { onSettingsClick() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PastelSky,
                                            contentColor = MindBackgroundDark
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Update", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Delicate hairline divider at bottom of sticky bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(if (isHeaderDark) Color(0x22FFFFFF) else Color(0x120F172A))
                )
            }
        },
        floatingActionButton = {
            if (uiState.selectedTab == NotesTab.JOURNAL) {
                val isFabDark = LocalMindLinkColors.current.isDark
                val fabShape = CircleShape
                FloatingActionButton(
                    onClick = {
                        if (showVoice || showOcr) {
                            showJournalingMethodsSheet = true
                        } else {
                            onCreateNoteClick("text")
                        }
                    },
                    containerColor = if (MindIsGlassy) {
                        if (isFabDark) Color(0xEB1E2738) else Color(0xF2FFFFFF)
                    } else PastelLavender,
                    contentColor = if (MindIsGlassy) {
                        if (isFabDark) Color.White else Color(0xFF0F172A)
                    } else Color.Black,
                    shape = fabShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    ),
                    modifier = if (MindIsGlassy) {
                        Modifier
                            .shadow(
                                elevation = 10.dp,
                                shape = fabShape,
                                ambientColor = if (isFabDark) Color(0x70000000) else Color(0x250F172A),
                                spotColor = if (isFabDark) Color(0x90000000) else Color(0x350F172A)
                            )
                            .border(
                                width = 1.5.dp,
                                brush = if (isFabDark) {
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x99FFFFFF),
                                            Color(0x30FFFFFF),
                                            Color(0x10FFFFFF)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0x80CBD5E1),
                                            Color(0x3094A3B8)
                                        )
                                    )
                                },
                                shape = fabShape
                            )
                    } else Modifier
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Thought",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = if (MindIsGlassy) Color.Transparent else MindBackgroundAmoled
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // One-Time Post-Install Blueprint Overview Banner
                AnimatedVisibility(
                    visible = showProgressiveIntroBanner && !uiState.showOnboarding && uiState.selectedTab == NotesTab.JOURNAL,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MindIndigo.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text("🌿", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "MindLink Grows With You (L0 – L5)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MindTextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { markIntroSeen() },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MindTextTertiary, modifier = Modifier.size(15.dp))
                                }
                            }

                            Text(
                                text = "All 23 features (Checklists, Tags, Photos, Voice, Lock, Cloud Vault, Memory Oracle & Edge AI) are built in. Start calm at Level 0 (just + note & follow-up), progress feature-by-feature via occasional smart suggestions, or toggle any level manually in Settings or the Drawer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MindTextSecondary,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (ladderState.currentTier > 0) {
                                    OutlinedButton(
                                        onClick = {
                                            capabilityManager.selectActiveLevel(0)
                                            markIntroSeen()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, PastelMint.copy(alpha = 0.6f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PastelMint),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Switch to L0 Simple", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Button(
                                    onClick = {
                                        markIntroSeen()
                                        onComplexityLadderClick()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MindIndigo),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Open Blueprint", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { markIntroSeen() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Got it", fontSize = 11.sp, color = MindTextTertiary)
                                }
                            }
                        }
                    }
                }

                // Intelligent Feature-by-Feature Suggestion Banner (Shown once in a while)
                val activeSuggestion = ladderState.activeSuggestion
                AnimatedVisibility(
                    visible = activeSuggestion != null && !showProgressiveIntroBanner && !uiState.showOnboarding && uiState.selectedTab == NotesTab.JOURNAL,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    activeSuggestion?.let { suggestion ->
                        val sAccent = Color(suggestion.accentHex)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = sAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, sAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(suggestion.featureIcon, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = sAccent.copy(alpha = 0.22f)
                                        ) {
                                            Text(
                                                text = "Smart Suggestion · Level ${suggestion.targetLevel}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = sAccent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { capabilityManager.dismissSuggestion(suggestion) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MindTextTertiary, modifier = Modifier.size(15.dp))
                                    }
                                }

                                Text(
                                    text = suggestion.headline,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextPrimary
                                )

                                Text(
                                    text = suggestion.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MindTextSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )

                                Text(
                                    text = "Skip if: ${suggestion.drawback}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindTextTertiary,
                                    fontSize = 10.5.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { capabilityManager.acceptSuggestionFeature(suggestion) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = sAccent, contentColor = Color(0xFF0B0F19)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Add ${suggestion.featureTitle}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (ladderState.currentTier < suggestion.targetLevel) {
                                        OutlinedButton(
                                            onClick = { capabilityManager.acceptSuggestionLevel(suggestion) },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(0.8.dp, sAccent.copy(alpha = 0.6f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = sAccent),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Unlock L${suggestion.targetLevel}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    TextButton(
                                        onClick = { capabilityManager.dismissSuggestion(suggestion) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Not Now", fontSize = 11.sp, color = MindTextTertiary)
                                    }
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // 1. Notes Stream with Active ViewMode
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
                                uiState.searchQuery.isNotEmpty() -> if (showSecondBrain) "Try broader keywords or ask Memory Oracle." else "Try broader keywords in your search."
                                uiState.selectedTab == NotesTab.TRASH -> "Deleted notes will appear here and auto-purge after 30 days."
                                uiState.selectedTab == NotesTab.ARCHIVE -> "Archive notes you want to keep without cluttering your main stream."
                                else -> if (showVoice || showMedia || showOcr) "Tap the add button below to capture a thought, voice note, photo, or page scan." else "Tap + below to capture a clear, distraction-free thought."
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
                val effectiveViewMode = if (showLayoutSwitch) viewMode else NoteViewMode.GRID
                val isGrid = effectiveViewMode == NoteViewMode.GRID
                LazyVerticalStaggeredGrid(
                    columns = if (isGrid) StaggeredGridCells.Fixed(2) else StaggeredGridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(
                        items = uiState.filteredNotes,
                        key = { it.id }
                    ) { note ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            NoteCard(
                                note = note,
                                viewMode = effectiveViewMode,
                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                isInSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelectNote(note.id)
                                    } else if (note.isLocked) {
                                        val activity = context as? androidx.fragment.app.FragmentActivity
                                        if (activity != null && com.mindlink.journal.ui.security.BiometricHelper.canAuthenticate(context)) {
                                            com.mindlink.journal.ui.security.BiometricHelper.authenticate(
                                                activity = activity,
                                                title = "Unlock \"${note.title.ifBlank { "Protected Thought" }}\"",
                                                subtitle = "Scan fingerprint to access this thought",
                                                onSuccess = { onNoteClick(note.id) }
                                            )
                                        } else {
                                            onNoteClick(note.id)
                                        }
                                    } else {
                                        onNoteClick(note.id)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelectNote(note.id)
                                }
                            )

                            // Quick actions under cards for Trash or Archive
                            if (uiState.selectedTab == NotesTab.TRASH) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${note.daysUntilExpiry}d left",
                                        fontSize = 10.sp,
                                        color = PastelRose
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = PastelMint.copy(alpha = 0.16f),
                                            border = BorderStroke(0.6.dp, PastelMint.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { viewModel.restoreFromTrash(note.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null, tint = PastelMint, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Restore", fontSize = 10.sp, color = PastelMint, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = PastelRose.copy(alpha = 0.16f),
                                            border = BorderStroke(0.6.dp, PastelRose.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { viewModel.deletePermanently(note.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = PastelRose, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Delete", fontSize = 10.sp, color = PastelRose, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else if (uiState.selectedTab == NotesTab.ARCHIVE) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PastelAmber.copy(alpha = 0.16f),
                                        border = BorderStroke(0.6.dp, PastelAmber.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewModel.unarchiveNote(note.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Unarchive, contentDescription = null, tint = PastelAmber, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Unarchive", fontSize = 10.sp, color = PastelAmber, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            } // End Box around stream
            } // End Column (Banners + Stream)

            // 2. Ambient Drop Shadow below the All(39) horizontal bar so scrolling looks naturally going under
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                if (isHeaderDark) Color(0x80000000) else Color(0x350F172A),
                                if (isHeaderDark) Color(0x40000000) else Color(0x180F172A),
                                if (isHeaderDark) Color(0x15000000) else Color(0x060F172A),
                                Color.Transparent
                            )
                        )
                    )
            )
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

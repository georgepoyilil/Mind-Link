package com.mindlink.journal.ui.ladder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.data.capability.CapabilityItem
import com.mindlink.journal.data.capability.CapabilityLadderManager
import com.mindlink.journal.data.capability.LadderState
import com.mindlink.journal.data.capability.TierInfo
import com.mindlink.journal.ui.components.liquidGlass
import com.mindlink.journal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplexityLadderScreen(
    manager: CapabilityLadderManager,
    onNavigateBack: () -> Unit
) {
    val state by manager.state.collectAsState()
    val (nextLevel, thoughtsNeeded) = remember(state) { manager.getNextTierInfo() }
    var focusedLevel by remember { mutableIntStateOf(state.currentTier) }
    var showAllLevels by remember { mutableStateOf(false) }
    var inspectedCapability by remember { mutableStateOf<CapabilityItem?>(null) }

    val totalCapabilities = CapabilityLadderManager.ALL_CAPABILITIES.size
    val activeCapabilities = remember(state) {
        CapabilityLadderManager.ALL_CAPABILITIES.count { state.isEnabled(it.key) }
    }

    val isGlassy = MindIsGlassy
    val isDark = LocalMindLinkColors.current.isDark

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "App Complexity Blueprint",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Text(
                            text = "Level ${state.currentTier} · $activeCapabilities/$totalCapabilities Modules Active · ${state.activeNotesCount} Thoughts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MindTextTertiary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MindTextPrimary)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (state.isGodMode) MindIndigo.copy(alpha = 0.25f) else MindGlassSurfaceVariant,
                        border = BorderStroke(1.dp, if (state.isGodMode) MindIndigo else MindGlassBorderSubtle),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { manager.setGodMode(!state.isGodMode) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.isGodMode) Icons.Default.AutoAwesome else Icons.Default.LockOpen,
                                contentDescription = "Unlock All",
                                tint = if (state.isGodMode) PastelSky else MindTextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.isGodMode) "All Unlocked" else "Unlock All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isGodMode) PastelSky else MindTextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isGlassy) Color.Transparent else MindBackgroundDark
                )
            )
        },
        containerColor = if (isGlassy) Color.Transparent else MindBackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 44.dp)
        ) {
            // 1. Visual Ascending Block Staircase Chart
            item {
                val activeLevelInfo = CapabilityLadderManager.TIERS.firstOrNull { it.tier == state.currentTier }
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isGlassy) Color.Transparent else MindSurfaceDark),
                    border = if (isGlassy) null else BorderStroke(1.dp, MindGlassBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(22.dp),
                            cornerRadius = 22.dp,
                            isGlassy = isGlassy,
                            isDark = isDark,
                            cardBackground = MindGlassCardBackground,
                            cardBorder = MindGlassCardBorder
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PROGRESSION ARCHITECTURE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelLavender,
                                    letterSpacing = 1.sp,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${activeLevelInfo?.badgeIcon ?: "🌿"} Level ${state.currentTier}: ${activeLevelInfo?.title ?: "Blank Slate"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextPrimary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(activeLevelInfo?.accentHex ?: 0xFF34D399).copy(alpha = 0.16f),
                                border = BorderStroke(0.8.dp, Color(activeLevelInfo?.accentHex ?: 0xFF34D399).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "$activeCapabilities/$totalCapabilities ON",
                                    color = Color(activeLevelInfo?.accentHex ?: 0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Ascending Step-Block Visualization Chart (Levels 0 -> 5)
                        AscendingStepBlockChart(
                            levels = CapabilityLadderManager.TIERS,
                            state = state,
                            focusedLevel = focusedLevel,
                            onSelectLevel = { level ->
                                focusedLevel = level
                                showAllLevels = false
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Next Level Unlock Progress Bar
                        if (nextLevel != null && !state.isGodMode) {
                            val prevReq = activeLevelInfo?.requiredNotes ?: 0
                            val delta = (nextLevel.requiredNotes - prevReq).coerceAtLeast(1)
                            val prog = (state.activeNotesCount - prevReq).coerceAtLeast(0)
                            val fraction = (prog.toFloat() / delta.toFloat()).coerceIn(0f, 1f)
                            val nextAccent = Color(nextLevel.accentHex)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Next Block: ${nextLevel.badgeIcon} Level ${nextLevel.tier} (${nextLevel.title})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (thoughtsNeeded > 0) "$thoughtsNeeded thoughts away" else "Ready!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = nextAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = nextAccent,
                                trackColor = MindGlassBorder
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MindIndigo.copy(alpha = 0.14f),
                                border = BorderStroke(0.6.dp, MindIndigo.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (state.isGodMode) {
                                        "👑 All-Access Mode Active · Flip any block below to shape your exact app."
                                    } else {
                                        "👑 Level 5 Reached · Toggle any module off to hide it completely from the app."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PastelLavender,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. View Mode Switcher Pill (Focused Block Deck vs Full Architecture Stack)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!showAllLevels) PastelSky.copy(alpha = 0.18f) else MindGlassSurfaceVariant,
                        border = BorderStroke(1.dp, if (!showAllLevels) PastelSky else MindGlassBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAllLevels = false }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Level $focusedLevel Block Deck",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!showAllLevels) PastelSky else MindTextSecondary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (showAllLevels) PastelLavender.copy(alpha = 0.18f) else MindGlassSurfaceVariant,
                        border = BorderStroke(1.dp, if (showAllLevels) PastelLavender else MindGlassBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAllLevels = true }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Full Stack (L0 – L5)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showAllLevels) PastelLavender else MindTextSecondary
                            )
                        }
                    }
                }
            }

            // 3. Decision Rationale Inspector Card (Shows when user taps a module block's info/body)
            item {
                AnimatedVisibility(
                    visible = inspectedCapability != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    inspectedCapability?.let { cap ->
                        val accent = Color(CapabilityLadderManager.TIERS.first { it.tier == cap.tier }.accentHex)
                        val isCapUnlocked = state.isLevelUnlocked(cap.tier)
                        val isCapOn = state.isEnabled(cap.key)

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131B2E) else Color(0xFFF8FAFC)),
                            border = BorderStroke(1.2.dp, accent.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(cap.iconSymbol, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = cap.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MindTextPrimary
                                            )
                                            Text(
                                                text = "Level ${cap.tier} · ${cap.settingsLocation ?: "Workspace"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accent,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    if (isCapUnlocked && !cap.isCore) {
                                        Switch(
                                            checked = isCapOn,
                                            onCheckedChange = { manager.toggleFeature(cap.key, it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = accent
                                            )
                                        )
                                    }
                                    IconButton(onClick = { inspectedCapability = null }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Inspector", tint = MindTextTertiary, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PastelMint.copy(alpha = 0.10f),
                                    border = BorderStroke(0.5.dp, PastelMint.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(9.dp), verticalAlignment = Alignment.Top) {
                                        Text("✅ ", fontSize = 11.sp)
                                        Text(
                                            text = "Why turn ON: ${cap.whyUse}",
                                            fontSize = 11.5.sp,
                                            color = MindTextPrimary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PastelAmber.copy(alpha = 0.10f),
                                    border = BorderStroke(0.5.dp, PastelAmber.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(9.dp), verticalAlignment = Alignment.Top) {
                                        Text("⚠️ ", fontSize = 11.sp)
                                        Text(
                                            text = "Why leave OFF: ${cap.whyNot}",
                                            fontSize = 11.5.sp,
                                            color = MindTextSecondary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                if (!cap.isCore) {
                                    Text(
                                        text = if (isCapOn) "Active — visible across MindLink." else "Disabled — completely removed from UI & Settings.",
                                        fontSize = 10.5.sp,
                                        color = if (isCapOn) PastelMint else PastelRose,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Capability Block Grid (Focused Level or All Levels)
            val levelsToDisplay = if (showAllLevels) {
                CapabilityLadderManager.TIERS
            } else {
                CapabilityLadderManager.TIERS.filter { it.tier == focusedLevel }
            }

            levelsToDisplay.forEach { levelInfo ->
                item(key = "level_${levelInfo.tier}") {
                    LevelModuleBlockSection(
                        level = levelInfo,
                        capabilities = CapabilityLadderManager.ALL_CAPABILITIES.filter { it.tier == levelInfo.tier },
                        state = state,
                        inspectedKey = inspectedCapability?.key,
                        onInspect = { cap ->
                            inspectedCapability = if (inspectedCapability?.key == cap.key) null else cap
                        },
                        onToggleFeature = { key, enabled -> manager.toggleFeature(key, enabled) },
                        onToggleAllInLevel = { enabled -> manager.toggleAllInLevel(levelInfo.tier, enabled) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AscendingStepBlockChart(
    levels: List<TierInfo>,
    state: LadderState,
    focusedLevel: Int,
    onSelectLevel: (Int) -> Unit
) {
    val isDark = LocalMindLinkColors.current.isDark
    val lineColor = if (isDark) Color(0x4094A3B8) else Color(0x40475569)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .background(
                color = if (isDark) Color(0x450B1120) else Color(0x120F172A),
                shape = RoundedCornerShape(16.dp)
            )
            .border(0.8.dp, MindGlassBorderSubtle, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        // Connecting architectural baseline grid & staircase line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = levels.size.coerceAtLeast(1)
            val colWidth = size.width / count
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

            for (i in 0 until count - 1) {
                val x1 = colWidth * i + colWidth * 0.5f
                val x2 = colWidth * (i + 1) + colWidth * 0.5f
                val h1 = (54f + i * 16f).dp.toPx()
                val h2 = (54f + (i + 1) * 16f).dp.toPx()
                val y1 = size.height - h1 * 0.55f
                val y2 = size.height - h2 * 0.55f
                val unlocked = state.isLevelUnlocked(i + 1)

                drawLine(
                    color = if (unlocked) Color(levels[i + 1].accentHex).copy(alpha = 0.55f) else lineColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = if (unlocked) 3.5f else 2f,
                    pathEffect = if (unlocked) null else dashEffect
                )
            }
        }

        // Interactive Ascending Staircase Blocks
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            levels.forEach { level ->
                val isUnlocked = state.isLevelUnlocked(level.tier)
                val isFocused = focusedLevel == level.tier
                val (activeCount, totalCount) = state.activeCountForLevel(level.tier)
                val fillFraction = if (totalCount > 0 && isUnlocked) activeCount.toFloat() / totalCount.toFloat() else 0f
                val accent = Color(level.accentHex)

                val targetHeight = (62 + level.tier * 16).dp
                val blockHeight by animateDpAsState(targetValue = targetHeight, label = "blockH_${level.tier}")
                val borderColor by animateColorAsState(
                    targetValue = when {
                        isFocused -> Color.White
                        isUnlocked -> accent.copy(alpha = 0.7f)
                        else -> MindGlassBorderSubtle
                    },
                    label = "borderC_${level.tier}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Selection indicator arrow/dot above top of block
                    if (isFocused) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(accent, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(blockHeight)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = if (isUnlocked) {
                                    Brush.verticalGradient(
                                        listOf(
                                            accent.copy(alpha = if (isFocused) 0.34f else 0.22f),
                                            accent.copy(alpha = 0.08f)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(Color(0x18334155), Color(0x0A1E293B))
                                    )
                                }
                            )
                            .border(
                                width = if (isFocused) 1.8.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectLevel(level.tier) }
                    ) {
                        // Active ratio vertical bar fill from bottom
                        if (isUnlocked && fillFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(fillFraction * 0.25f)
                                    .background(accent.copy(alpha = 0.45f))
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 6.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Icon or Lock
                            if (isUnlocked) {
                                Text(text = level.badgeIcon, fontSize = 14.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MindTextTertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            // Center Level Code (L0..L5)
                            Text(
                                text = "L${level.tier}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isUnlocked) MindTextPrimary else MindTextTertiary
                            )

                            // Bottom module dots indicator
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isUnlocked) {
                                    Text(
                                        text = "$activeCount/$totalCount",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                } else {
                                    Text(
                                        text = "${level.requiredNotes}n",
                                        fontSize = 9.sp,
                                        color = MindTextTertiary
                                    )
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
private fun LevelModuleBlockSection(
    level: TierInfo,
    capabilities: List<CapabilityItem>,
    state: LadderState,
    inspectedKey: String?,
    onInspect: (CapabilityItem) -> Unit,
    onToggleFeature: (String, Boolean) -> Unit,
    onToggleAllInLevel: (Boolean) -> Unit
) {
    val isUnlocked = state.isLevelUnlocked(level.tier)
    val accent = Color(level.accentHex)
    val (activeCount, totalCount) = state.activeCountForLevel(level.tier)
    val hasOptional = capabilities.any { !it.isCore }
    val allOptionalOn = capabilities.filter { !it.isCore }.all { state.isEnabled(it.key) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Level Header Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isUnlocked) accent.copy(alpha = 0.12f) else Color(0x161E293B),
            border = BorderStroke(1.dp, if (isUnlocked) accent.copy(alpha = 0.45f) else Color(0x28334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = level.badgeIcon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Level ${level.tier} · ${level.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) MindTextPrimary else MindTextTertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isUnlocked) accent.copy(alpha = 0.2f) else Color(0x22F43F5E)
                            ) {
                                Text(
                                    text = if (isUnlocked) "$activeCount/$totalCount" else "LOCKED · ${level.requiredNotes}n",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) accent else Color(0xFFFB7185),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = level.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MindTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isUnlocked && hasOptional) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MindGlassButtonContainer,
                        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleAllInLevel(!allOptionalOn) }
                    ) {
                        Text(
                            text = if (allOptionalOn) "Turn Off" else "Enable All",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 2-Column Visual Capability Module Grid
        val rows = capabilities.chunked(2)
        rows.forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { item ->
                    CapabilityTileBlock(
                        item = item,
                        accent = accent,
                        isLevelUnlocked = isUnlocked,
                        isEnabled = state.isEnabled(item.key),
                        isInspected = inspectedKey == item.key,
                        onInspect = { onInspect(item) },
                        onToggle = { enabled -> onToggleFeature(item.key, enabled) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CapabilityTileBlock(
    item: CapabilityItem,
    accent: Color,
    isLevelUnlocked: Boolean,
    isEnabled: Boolean,
    isInspected: Boolean,
    onInspect: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAndUnlocked = isLevelUnlocked && isEnabled
    val cardAlpha = if (isLevelUnlocked) 1f else 0.58f

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            activeAndUnlocked -> MindGlassSurfaceVariant
            isLevelUnlocked -> Color(0x160F172A)
            else -> Color(0x0C0F172A)
        },
        border = BorderStroke(
            width = if (isInspected) 1.5.dp else 1.dp,
            color = when {
                isInspected -> Color.White
                activeAndUnlocked -> accent.copy(alpha = 0.55f)
                else -> MindGlassBorderSubtle.copy(alpha = 0.5f)
            }
        ),
        modifier = modifier
            .alpha(cardAlpha)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onInspect() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Top Row: Emoji Badge + Toggle/Core/Lock Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeAndUnlocked) accent.copy(alpha = 0.18f) else MindGlassSurface,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = item.iconSymbol, fontSize = 16.sp)
                        }
                    }

                    when {
                        !isLevelUnlocked -> {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MindTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        item.isCore -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accent.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "CORE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        else -> {
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = onToggle,
                                modifier = Modifier.height(26.dp),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accent,
                                    uncheckedThumbColor = MindTextTertiary,
                                    uncheckedTrackColor = Color(0x33475569)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (activeAndUnlocked) MindTextPrimary else MindTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.shortDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MindTextTertiary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!isLevelUnlocked) "Locked" else if (isEnabled) "Active" else "Removed",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !isLevelUnlocked -> MindTextTertiary
                            isEnabled -> accent
                            else -> PastelRose.copy(alpha = 0.85f)
                        }
                    )
                    Text(
                        text = if (isInspected) "Hide info ▲" else "Why? ▾",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PastelLavender,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

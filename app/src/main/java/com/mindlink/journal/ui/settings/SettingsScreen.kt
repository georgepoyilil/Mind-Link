package com.mindlink.journal.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.BuildConfig
import com.mindlink.journal.ui.components.MindLinkOnboardingDialog
import com.mindlink.journal.ui.components.MindLinkUserGuideDialog
import com.mindlink.journal.ui.components.liquidGlass
import com.mindlink.journal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isKeyVisible by remember { mutableStateOf(false) }
    var isThemeCardExpanded by rememberSaveable { mutableStateOf(false) }
    var showUserGuideDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(false) }

    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result.data)
    }

    if (showUserGuideDialog) {
        MindLinkUserGuideDialog(onDismiss = { showUserGuideDialog = false })
    }

    if (showOnboardingDialog) {
        MindLinkOnboardingDialog(onDismiss = { showOnboardingDialog = false })
    }

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (MindIsGlassy) Color.Transparent else MindBackgroundDark)
            )
        },
        containerColor = if (MindIsGlassy) Color.Transparent else MindBackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Visual Atmosphere & Themes Card
            val isSettingsGlassy = MindIsGlassy
            val isSettingsDark = LocalMindLinkColors.current.isDark
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSettingsGlassy) Color.Transparent else MindSurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(16.dp),
                        cornerRadius = 16.dp,
                        isGlassy = isSettingsGlassy,
                        isDark = isSettingsDark,
                        cardBackground = MindGlassCardBackground,
                        cardBorder = MindGlassCardBorder
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isThemeCardExpanded = !isThemeCardExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Themes",
                                tint = PastelLavender,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Visual Atmosphere & Themes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextPrimary
                                )
                                val currentTheme = remember(uiState.appTheme) { MindTheme.fromId(uiState.appTheme) }
                                Text(
                                    text = currentTheme.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PastelLavender
                                )
                            }
                        }
                        IconButton(onClick = { isThemeCardExpanded = !isThemeCardExpanded }) {
                            Icon(
                                imageVector = if (isThemeCardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isThemeCardExpanded) "Minimize" else "Expand",
                                tint = MindTextSecondary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isThemeCardExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Tailor your aesthetic ritual with custom glassy, AMOLED, Pixel, and light modes:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MindTextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                    MindTheme.entries.forEach { theme ->
                        val isSelected = uiState.appTheme.equals(theme.id, ignoreCase = true)
                        val themeColors = remember(theme) { getThemeColors(theme) }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MindSurfaceVariantDark else Color.Transparent,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.8.dp,
                                color = if (isSelected) themeColors.accentPrimary else MindBorderDark.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setAppTheme(theme.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Swatch preview
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = themeColors.background,
                                    border = BorderStroke(1.dp, themeColors.border),
                                    modifier = Modifier.size(width = 38.dp, height = 38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Surface(
                                            shape = CircleShape,
                                            color = themeColors.accentPrimary,
                                            modifier = Modifier.size(12.dp)
                                        ) {}
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = theme.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) themeColors.accentPrimary else MindTextPrimary
                                        )
                                        if (!theme.isDark) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val isAppDark = LocalMindLinkColors.current.isDark
                                            val badgeBg = if (isAppDark) PastelAmber.copy(alpha = 0.2f) else Color(0x26B45309)
                                            val badgeText = if (isAppDark) PastelAmber else Color(0xFFB45309)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = badgeBg,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "LIGHT",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeText
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = theme.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindTextSecondary
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setAppTheme(theme.id) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = themeColors.accentPrimary,
                                        unselectedColor = MindTextTertiary
                                    )
                                )
                            }
                        }
                    }
                        }
                    }
                }
            }

            // 0.2. Mood & Reaction Emojis Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MindSurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PastelMint.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PastelMint.copy(alpha = 0.35f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mood,
                                contentDescription = null,
                                tint = PastelMint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎭 Mood & Reaction Emojis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Display emotional emoji accents on note cards, mood badges, and thought graph nodes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MindTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = uiState.showEmojis,
                        onCheckedChange = { viewModel.setShowEmojis(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MindBackgroundDark,
                            checkedTrackColor = PastelMint,
                            uncheckedThumbColor = MindTextSecondary,
                            uncheckedTrackColor = MindSurfaceVariantDark
                        )
                    )
                }
            }

            // 0.3. App Typography & Font Size Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MindSurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Font Size",
                            tint = PastelLavender,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "App Typography & Font Size",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Global font size applied across the entire app. Individual notes can override this with their own size:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MindTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val fontOptions = listOf(
                        Triple(0.85f, "Small", "0.85x"),
                        Triple(1.0f, "Default", "1.0x"),
                        Triple(1.15f, "Large", "1.15x"),
                        Triple(1.30f, "Huge", "1.30x")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fontOptions.forEach { (scale, label, sub) ->
                            val isSelected = Math.abs(uiState.fontScale - scale) < 0.05f
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MindSurfaceVariantDark else Color.Transparent,
                                border = BorderStroke(
                                    width = if (isSelected) 1.2.dp else 0.8.dp,
                                    color = if (isSelected) PastelLavender else MindBorderDark.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.updateFontScale(scale) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PastelLavender else MindTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) PastelLavender.copy(alpha = 0.8f) else MindTextTertiary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Line Height Spacing Row
                    Text(
                        text = "Journal Body Line Spacing:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MindTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val lineSpacingOptions = listOf(
                        Pair(1.25f, "Compact"),
                        Pair(1.45f, "Normal"),
                        Pair(1.75f, "Relaxed"),
                        Pair(2.0f, "Spacious")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        lineSpacingOptions.forEach { (mult, label) ->
                            val isSelected = Math.abs(uiState.lineSpacingMultiplier - mult) < 0.05f
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MindSurfaceVariantDark else Color.Transparent,
                                border = BorderStroke(
                                    width = if (isSelected) 1.2.dp else 0.8.dp,
                                    color = if (isSelected) PastelMint else MindBorderDark.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.updateLineSpacingMultiplier(mult) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PastelMint else MindTextSecondary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Preview Area
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MindGlassSurfaceVariant,
                        border = BorderStroke(0.8.dp, MindGlassBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Preview Text",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextTertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val previewFontSize = MaterialTheme.typography.bodyMedium.fontSize
                            val previewLineHeight = (previewFontSize.value * uiState.lineSpacingMultiplier).sp
                            Text(
                                text = "MindLink captures thoughts into permanent clarity with on-device intelligence. Line height and typography scale proportionally.",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = previewLineHeight),
                                color = MindTextPrimary
                            )
                        }
                    }
                }
            }

            // 0.5. Neural Inspector (Tech Mode) Toggle Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MindSurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PastelSky.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.35f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = PastelSky,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔬 Neural Inspector (Tech Mode)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Reveal 384-d vector embeddings, cosine match scores, hybrid RRF ranks, and engine latency.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MindTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = uiState.isTechMode,
                        onCheckedChange = { viewModel.setTechMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MindBackgroundDark,
                            checkedTrackColor = PastelSky,
                            uncheckedThumbColor = MindTextSecondary,
                            uncheckedTrackColor = MindSurfaceVariantDark
                        )
                    )
                }
            }

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
                        Triple("LOCAL", "Local NLP", "Instant rule-based heuristic with Life Chapters (Heart, Work, Wellness, Personal)")
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

            // Google Cloud SSO & Drive Backup Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSettingsGlassy) Color.Transparent else MindSurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(16.dp),
                        cornerRadius = 16.dp,
                        isGlassy = isSettingsGlassy,
                        isDark = isSettingsDark,
                        cardBackground = MindGlassCardBackground,
                        cardBorder = MindGlassCardBorder
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Cloud Backup",
                            tint = PastelSky,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Cloud & Drive Backup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MindTextPrimary
                            )
                            Text(
                                text = if (uiState.googleUser != null) "Connected: ${uiState.googleUser?.email}" else "Sign in to backup to private Google Drive",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.googleUser != null) PastelMint else MindTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.googleUser == null) {
                        Text(
                            text = "Back up all your thoughts, tags, audio durations, and AI summaries securely to your personal Google Drive (AppData).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MindTextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val intent = viewModel.getGoogleSignInIntent()
                                if (intent != null) {
                                    googleSignInLauncher.launch(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1F1F1F)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F)
                            )
                        }
                    } else {
                        val user = uiState.googleUser!!
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MindGlassSurface,
                            border = BorderStroke(1.dp, MindGlassBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = PastelSky.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = user.displayName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = PastelSky,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MindTextPrimary
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MindTextSecondary
                                    )
                                }
                                TextButton(onClick = { viewModel.signOutGoogle() }) {
                                    Text("Sign Out", color = PastelRose, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val formattedBackupTime = remember(uiState.lastBackupTimestamp) {
                            if (uiState.lastBackupTimestamp <= 0) "Never backed up"
                            else java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", java.util.Locale.getDefault()).format(java.util.Date(uiState.lastBackupTimestamp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last Drive Backup:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextTertiary
                            )
                            Text(
                                text = formattedBackupTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.lastBackupTimestamp > 0) PastelMint else MindTextTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Backup and Restore Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.backupToGoogleDrive() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PastelSky,
                                    contentColor = MindBackgroundDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                enabled = uiState.syncState !is com.mindlink.journal.data.sync.SyncState.Syncing
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.restoreFromGoogleDrive() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PastelLavender),
                                border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                enabled = uiState.syncState !is com.mindlink.journal.data.sync.SyncState.Syncing
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        // Sync State Banner
                        when (val sync = uiState.syncState) {
                            is com.mindlink.journal.data.sync.SyncState.Syncing -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PastelSky, strokeWidth = 2.dp)
                                    Text(text = sync.message, style = MaterialTheme.typography.bodySmall, color = PastelSky)
                                }
                            }
                            is com.mindlink.journal.data.sync.SyncState.Success -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MindEmerald, modifier = Modifier.size(16.dp))
                                    Text(text = sync.message, style = MaterialTheme.typography.bodySmall, color = MindEmerald)
                                }
                            }
                            is com.mindlink.journal.data.sync.SyncState.Error -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = PastelRose, modifier = Modifier.size(16.dp))
                                    Text(text = sync.error, style = MaterialTheme.typography.bodySmall, color = PastelRose)
                                }
                            }
                            com.mindlink.journal.data.sync.SyncState.Idle -> {}
                        }
                    }
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

            // 6. In-App OTA Updates Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MindSurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Updates",
                            tint = PastelSky,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MindLink Updates (OTA)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MindGlassButtonContainer,
                            border = BorderStroke(1.dp, MindGlassButtonBorder)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                color = PastelSky,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Seamless in-app updates powered directly by GitHub Releases (georgepoyilil/Mind-Link).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MindTextSecondary
                    )

                    if (uiState.appUpdateMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = uiState.appUpdateMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.appUpdateInfo?.isNewer == true) PastelMint else MindTextSecondary
                        )
                    }

                    if (uiState.isCheckingAppUpdate) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = PastelSky
                            )
                            Text(
                                text = "Checking GitHub Releases...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MindTextSecondary
                            )
                        }
                    }

                    val updateInfo = uiState.appUpdateInfo
                    if (updateInfo != null && updateInfo.isNewer) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MindGlassSurface,
                            border = BorderStroke(1.dp, PastelMint.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = updateInfo.tagName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PastelMint
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    val sizeMb = if (updateInfo.sizeBytes > 0) {
                                        String.format("%.1f MB", updateInfo.sizeBytes / (1024f * 1024f))
                                    } else {
                                        "APK"
                                    }
                                    Text(
                                        text = sizeMb,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindTextSecondary
                                    )
                                }

                                if (updateInfo.changelog.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = updateInfo.changelog,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MindTextSecondary,
                                        maxLines = 4
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (uiState.isDownloadingAppUpdate) {
                                    LinearProgressIndicator(
                                        progress = { uiState.appUpdateProgress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = PastelMint,
                                        trackColor = MindBorderDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Downloading APK update... ${(uiState.appUpdateProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MindTextSecondary
                                    )
                                } else if (uiState.downloadedApkFile != null) {
                                    Button(
                                        onClick = { viewModel.installDownloadedApk() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PastelMint,
                                            contentColor = MindBackgroundDark
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DownloadDone,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Install Update Now", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.downloadAndInstallAppUpdate() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PastelMint,
                                            contentColor = MindBackgroundDark
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download & Install (${updateInfo.tagName})", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (!uiState.isCheckingAppUpdate && (updateInfo == null || !updateInfo.isNewer)) {
                        if (updateInfo != null && !updateInfo.isNewer) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MindGlassSurface,
                                border = BorderStroke(1.dp, MindEmerald.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MindEmerald, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Latest Release: ${updateInfo.tagName} (Installed & up to date)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MindTextPrimary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.checkForAppUpdate() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PastelSky),
                            border = BorderStroke(1.dp, PastelSky.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = "Check for updates",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check for MindLink Updates", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 7. About MindLink & User Guide Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MindSurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = PastelLavender,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About MindLink & User Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• All notes and vector embeddings are stored locally in Room SQLite.\n• Full-text search runs completely offline via SQLite FTS.\n• WorkManager guarantees background note enrichment without UI freezes.\n• Embeddings run via Fallback/ONNX 384-d semantic projection.\n• SLMs run on device GPU via Google MediaPipe Tasks GenAI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindTextSecondary,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showUserGuideDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PastelSky,
                            contentColor = MindBackgroundDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Read MindLink User Guide & Instructions", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showOnboardingDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PastelLavender),
                        border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Replay Onboarding Tour", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

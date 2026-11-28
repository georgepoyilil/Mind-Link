package com.mindlink.journal.ui.ladder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.data.capability.CapabilityItem
import com.mindlink.journal.data.capability.CapabilityLadderManager
import com.mindlink.journal.data.capability.TierInfo
import com.mindlink.journal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplexityLadderScreen(
    manager: CapabilityLadderManager,
    onNavigateBack: () -> Unit
) {
    val state by manager.state.collectAsState()
    val (nextTier, thoughtsNeeded) = remember(state) { manager.getNextTierInfo() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Journal Complexity Ladder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Text(
                            text = "Tier ${state.currentTier} Evolution · ${state.activeNotesCount} Thoughts",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MindBackgroundDark)
            )
        },
        containerColor = MindBackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
        ) {
            // 1. Hero Progression Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MindGlassSurfaceVariant),
                    border = BorderStroke(1.dp, MindGlassBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val currentTierInfo = CapabilityLadderManager.TIERS.firstOrNull { it.tier == state.currentTier }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CURRENT CAPABILITY TIER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelLavender,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${currentTierInfo?.badgeIcon ?: "🌿"} Tier ${state.currentTier}: ${currentTierInfo?.title ?: "Minimalist"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextPrimary
                                )
                                Text(
                                    text = currentTierInfo?.subtitle ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MindTextSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PastelMint.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, PastelMint.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = if (state.isGodMode) "GOD MODE" else "LEVEL ${state.currentTier}",
                                    color = PastelMint,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Next tier progress
                        if (nextTier != null && !state.isGodMode) {
                            val prevRequired = CapabilityLadderManager.TIERS.firstOrNull { it.tier == state.currentTier }?.requiredNotes ?: 0
                            val totalDelta = nextTier.requiredNotes - prevRequired
                            val progressInTier = (state.activeNotesCount - prevRequired).coerceAtLeast(0)
                            val fraction = if (totalDelta > 0) (progressInTier.toFloat() / totalDelta).coerceIn(0f, 1f) else 1f

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Next: ${nextTier.badgeIcon} ${nextTier.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MindTextSecondary
                                )
                                Text(
                                    text = if (thoughtsNeeded > 0) "$thoughtsNeeded more thoughts needed" else "Ready to unlock!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PastelSky,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = PastelSky,
                                trackColor = MindGlassBorder
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PastelLavender.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "👑 Pinnacle Unlocked: All 6 capability tiers are at your command.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PastelLavender,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MindGlassBorderSubtle)
                        Spacer(modifier = Modifier.height(12.dp))

                        // God Mode Override Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Power User 'Unlock All' (God Mode)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextPrimary
                                )
                                Text(
                                    text = "Bypass progression ladder and unlock all 6 tiers instantly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MindTextTertiary
                                )
                            }
                            Switch(
                                checked = state.isGodMode,
                                onCheckedChange = { manager.setGodMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MindIndigo
                                )
                            )
                        }
                    }
                }
            }

            // 2. Ladder Tier Cards
            items(CapabilityLadderManager.TIERS) { tier ->
                val isUnlocked = manager.isTierUnlocked(tier.tier)
                val tierCapabilities = CapabilityLadderManager.ALL_CAPABILITIES.filter { it.tier == tier.tier }

                TierCard(
                    tier = tier,
                    isUnlocked = isUnlocked,
                    capabilities = tierCapabilities,
                    state = state,
                    onToggleFeature = { key, enabled -> manager.toggleFeature(key, enabled) }
                )
            }
        }
    }
}

@Composable
fun TierCard(
    tier: TierInfo,
    isUnlocked: Boolean,
    capabilities: List<CapabilityItem>,
    state: com.mindlink.journal.data.capability.LadderState,
    onToggleFeature: (String, Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(isUnlocked) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) MindGlassSurfaceVariant else Color(0x111E293B)
        ),
        border = BorderStroke(
            1.dp,
            if (isUnlocked) MindGlassBorderSubtle else Color(0x22334155)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (isUnlocked) isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = tier.badgeIcon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tier ${tier.tier}: ${tier.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) MindTextPrimary else MindTextTertiary
                            )
                        }
                        Text(
                            text = tier.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MindTextTertiary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isUnlocked) PastelMint.copy(alpha = 0.15f) else Color(0x18F43F5E),
                    border = BorderStroke(0.5.dp, if (isUnlocked) PastelMint.copy(alpha = 0.5f) else Color(0x44F43F5E))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isUnlocked) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFFB7185), modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${tier.requiredNotes} Thoughts",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFB7185)
                            )
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Unlocked", tint = PastelMint, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "UNLOCKED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PastelMint
                            )
                        }
                    }
                }
            }

            // Expanded Capability Items
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    capabilities.forEach { item ->
                        val isEnabled = if (item.isCore) true else (state.enabledFeatures[item.key] ?: true)
                        CapabilityRow(
                            item = item,
                            isTierUnlocked = isUnlocked,
                            isEnabled = isEnabled,
                            onToggle = { enabled -> onToggleFeature(item.key, enabled) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CapabilityRow(
    item: CapabilityItem,
    isTierUnlocked: Boolean,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isEnabled && isTierUnlocked) MindGlassSurfaceVariant.copy(alpha = 0.7f) else Color(0x0C000000),
        border = BorderStroke(0.5.dp, if (isEnabled && isTierUnlocked) MindGlassBorderSubtle else Color(0x18334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = item.iconSymbol, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isTierUnlocked) MindTextPrimary else MindTextTertiary
                        )
                        Text(
                            text = item.shortDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MindTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isTierUnlocked) {
                    if (item.isCore) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PastelSky.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "CORE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PastelSky,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = onToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MindIndigo
                            )
                        )
                    }
                } else {
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MindTextTertiary, modifier = Modifier.size(16.dp))
                }
            }

            // Benefits & Drawbacks Toggle
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .clickable { showDetails = !showDetails }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.Info,
                    contentDescription = null,
                    tint = PastelLavender,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (showDetails) "Hide decision rationale" else "Why use this? (Pros & Trade-offs)",
                    style = MaterialTheme.typography.labelSmall,
                    color = PastelLavender,
                    fontWeight = FontWeight.Medium
                )
            }

            AnimatedVisibility(visible = showDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color(0x1A0F172A), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row {
                        Text(text = "✅ ", fontSize = 11.sp)
                        Text(
                            text = "Why use: ${item.whyUse}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MindTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Row {
                        Text(text = "⚠️ ", fontSize = 11.sp)
                        Text(
                            text = "Why not: ${item.whyNot}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MindTextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

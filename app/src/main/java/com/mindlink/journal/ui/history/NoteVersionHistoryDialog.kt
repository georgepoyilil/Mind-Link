package com.mindlink.journal.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mindlink.journal.data.local.entity.NoteVersionEntity
import com.mindlink.journal.ui.theme.MindBackgroundAmoled
import com.mindlink.journal.ui.theme.MindGlassBorderSubtle
import com.mindlink.journal.ui.theme.MindGlassButtonBorder
import com.mindlink.journal.ui.theme.MindGlassButtonContainer
import com.mindlink.journal.ui.theme.MindGlassSurface
import com.mindlink.journal.ui.theme.MindGlassSurfaceElevated
import com.mindlink.journal.ui.theme.MindTextPrimary
import com.mindlink.journal.ui.theme.MindTextSecondary
import com.mindlink.journal.ui.theme.MindTextTertiary
import com.mindlink.journal.ui.theme.PastelLavender
import com.mindlink.journal.ui.theme.PastelMint
import com.mindlink.journal.ui.theme.PastelRose

@Composable
fun NoteVersionHistoryDialog(
    currentTitle: String,
    currentContent: String,
    versions: List<NoteVersionEntity>,
    onDismiss: () -> Unit,
    onRestoreVersion: (NoteVersionEntity) -> Unit
) {
    var selectedVersion by remember { mutableStateOf<NoteVersionEntity?>(versions.firstOrNull()) }

    // When versions change or load, select the first one by default if not set
    LaunchedEffect(versions) {
        if (selectedVersion == null && versions.isNotEmpty()) {
            selectedVersion = versions.first()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = MindBackgroundAmoled,
            border = BorderStroke(1.2.dp, MindGlassBorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PastelLavender.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = PastelLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Version History & Diff",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MindTextPrimary
                            )
                            Text(
                                text = "${versions.size} past ${if (versions.size == 1) "revision" else "revisions"} recorded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextTertiary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MindTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MindGlassBorderSubtle)
                Spacer(modifier = Modifier.height(14.dp))

                if (versions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MindTextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No past revisions recorded yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MindTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "MindLink automatically snapshots revisions every time you edit and save an existing thought.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MindTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Version Selector Tabs / Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        versions.forEachIndexed { index, version ->
                            val isSelected = selectedVersion?.versionId == version.versionId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PastelLavender.copy(alpha = 0.25f) else MindGlassSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PastelLavender else MindGlassBorderSubtle
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedVersion = version }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = "Rev #${versions.size - index}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) PastelLavender else MindTextPrimary
                                    )
                                    Text(
                                        text = version.compactDate,
                                        fontSize = 10.5.sp,
                                        color = MindTextTertiary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    selectedVersion?.let { ver ->
                        // Diff Summary & Restore Action Bar
                        val diffSummary = remember(ver, currentContent) {
                            DiffUtils.computeLineDiff(ver.content, currentContent)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PastelMint.copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, PastelMint.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "+${diffSummary.additions} lines",
                                        color = PastelMint,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PastelRose.copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, PastelRose.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "-${diffSummary.deletions} lines",
                                        color = PastelRose,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Restore Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MindGlassButtonContainer,
                                border = BorderStroke(1.dp, MindGlassButtonBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onRestoreVersion(ver)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = "Restore",
                                        tint = PastelLavender,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Restore this Revision",
                                        color = PastelLavender,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Diff Lines View Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MindGlassSurfaceElevated),
                            border = BorderStroke(1.dp, MindGlassBorderSubtle)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            ) {
                                items(diffSummary.lines) { diffItem ->
                                    val (bgColor, textColor, prefix) = when (diffItem.operation) {
                                        DiffOperation.ADDED -> Triple(
                                            Color(0xFF143020),
                                            PastelMint,
                                            "+ "
                                        )
                                        DiffOperation.DELETED -> Triple(
                                            Color(0xFF351A20),
                                            PastelRose,
                                            "- "
                                        )
                                        DiffOperation.UNCHANGED -> Triple(
                                            Color.Transparent,
                                            MindTextSecondary,
                                            "  "
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bgColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = prefix,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = diffItem.text.ifEmpty { " " },
                                            fontFamily = FontFamily.Monospace,
                                            color = textColor,
                                            fontSize = 12.5.sp,
                                            textDecoration = if (diffItem.operation == DiffOperation.DELETED) TextDecoration.LineThrough else TextDecoration.None
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
}

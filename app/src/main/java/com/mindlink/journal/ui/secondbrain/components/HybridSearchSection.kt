package com.mindlink.journal.ui.secondbrain.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.domain.model.SearchResult
import com.mindlink.journal.ui.theme.*

@Composable
fun HybridSearchSection(
    results: List<SearchResult>,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }
    val initialPreviewCount = 3
    val displayedResults = if (isExpanded) results else results.take(initialPreviewCount)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FindInPage, contentDescription = null, tint = PastelLavender, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Matched Journal Entries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MindTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MindGlassSurfaceVariant,
                        border = BorderStroke(0.5.dp, MindGlassBorderSubtle)
                    ) {
                        Text(
                            text = "${results.size}",
                            color = MindTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = if (isExpanded) "Showing all ${results.size} matches" else "Top relevant matches (tap to expand)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MindTextTertiary
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MindGlassButtonContainer,
                border = BorderStroke(0.5.dp, MindGlassButtonBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Collapse" else "Expand (${results.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = PastelSky,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = PastelSky,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            displayedResults.forEach { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNoteClick(result.note.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MindGlassSurface
                    ),
                    border = BorderStroke(1.dp, MindGlassCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(PastelLavender, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = result.note.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MindTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (result.isSemanticMatch) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MindGlassButtonContainer,
                                        border = BorderStroke(0.5.dp, MindGlassButtonBorder)
                                    ) {
                                        Text(
                                            text = "Semantic",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PastelLavender,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                if (result.isKeywordMatch) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MindGlassButtonContainer,
                                        border = BorderStroke(0.5.dp, MindGlassButtonBorder)
                                    ) {
                                        Text(
                                            text = "FTS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PastelSky,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = result.note.previewText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MindTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result.note.formattedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextTertiary
                            )

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Note",
                                tint = MindTextTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Expand / Collapse Bar when more than 3 results
            if (results.size > initialPreviewCount) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MindGlassSurfaceVariant,
                    border = BorderStroke(0.8.dp, MindGlassBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) "Collapse Matched Entries" else "View All ${results.size} Matched Entries",
                            color = PastelLavender,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = PastelLavender,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

package com.mindlink.journal.ui.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.LocalTechMode
import com.mindlink.journal.domain.model.RelatedThought
import com.mindlink.journal.ui.theme.*

@Composable
fun RelatedThoughtsCard(
    relatedThoughts: List<RelatedThought>,
    isLoading: Boolean,
    onThoughtClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTechMode = LocalTechMode.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MindGlassSurface
        ),
        border = BorderStroke(1.dp, MindIndigo.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MindGlassButtonContainer,
                    border = BorderStroke(1.dp, MindGlassButtonBorder),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isTechMode) Icons.Default.Hub else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isTechMode) PastelSky else PastelMint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isTechMode) "Related Thoughts & Synapses" else "🌿 Reminds you of something...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MindTextPrimary
                    )
                    Text(
                        text = if (isTechMode) "384-d ONNX / Fallback vector space" else "Connected memories from your journal",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isTechMode) PastelSky.copy(alpha = 0.8f) else MindTextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = if (isTechMode) PastelSky else MindIndigo,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isTechMode) "Traversing 384-d vector embeddings..." else "Connecting the dots...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindTextSecondary
                    )
                }
            } else if (relatedThoughts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MindGlassSurfaceVariant,
                    border = BorderStroke(0.5.dp, MindGlassBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isTechMode) {
                            "No cosine vector neighbors found above threshold (>= 0.35). Add more thoughts to compute embeddings."
                        } else {
                            "No connected memories yet. As you write more, links will blossom here."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindTextSecondary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    relatedThoughts.forEach { thought ->
                        val score = thought.similarityScore
                        val percent = (score * 100).toInt().coerceIn(0, 100)
                        val distance = (1.0f - score).coerceAtLeast(0f)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MindGlassSurfaceVariant,
                            border = BorderStroke(0.5.dp, MindGlassBorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onThoughtClick(thought.note.id) }
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
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(if (isTechMode) PastelSky else PastelLavender, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = thought.note.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MindTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = thought.note.previewText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MindTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isTechMode) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = String.format("[384-d Cosine: %.3f | Distance: %.3f]", score, distance),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PastelSky,
                                                fontSize = 9.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isTechMode) PastelSky.copy(alpha = 0.12f) else MindGlassButtonContainer,
                                        border = BorderStroke(0.5.dp, if (isTechMode) PastelSky.copy(alpha = 0.35f) else MindGlassButtonBorder)
                                    ) {
                                        Text(
                                            text = if (isTechMode) {
                                                "$percent% match"
                                            } else if (score >= 0.70f) {
                                                "Strong connection"
                                            } else {
                                                "Related"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isTechMode) PastelSky else PastelLavender,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open",
                                        tint = MindTextTertiary,
                                        modifier = Modifier.size(16.dp)
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

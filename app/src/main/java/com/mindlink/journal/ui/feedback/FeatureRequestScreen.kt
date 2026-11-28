package com.mindlink.journal.ui.feedback

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.data.feedback.FeatureProposal
import com.mindlink.journal.data.feedback.FeatureRequestStore
import com.mindlink.journal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeatureRequestScreen(
    store: FeatureRequestStore,
    activeNotesCount: Int,
    currentTier: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FeatureRequestStore.CATEGORIES.first()) }
    var proposals by remember { mutableStateOf(store.getProposals()) }
    var isSubmitting by remember { mutableStateOf(false) }

    val (isEligible, eligibilityMessage) = remember(activeNotesCount, currentTier, proposals) {
        store.checkEligibility(activeNotesCount, currentTier)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Creator Portal & Proposals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Text(
                            text = "Tier $currentTier Thinker · Community Driven",
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
            // 1. Gate / Status Banner
            item {
                if (!isEligible) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x22F43F5E)),
                        border = BorderStroke(1.dp, Color(0x55F43F5E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Creative Gate Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFB7185)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = eligibilityMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MindTextSecondary
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MindGlassSurfaceVariant),
                        border = BorderStroke(1.dp, MindGlassBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = PastelSky, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Propose a New Feature or Direction",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Because of your consistent journaling, your suggestions go straight into MindLink's development queue. Limit: 1 thoughtful proposal per week.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MindTextTertiary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Category Chips
                            Text("CATEGORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MindTextTertiary)
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FeatureRequestStore.CATEGORIES.forEach { cat ->
                                    val isSelected = selectedCategory == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PastelSky.copy(alpha = 0.2f),
                                            selectedLabelColor = PastelSky
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Title Field
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Proposal Title") },
                                placeholder = { Text("e.g., Markdown table support in note editor") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MindBackgroundDark,
                                    unfocusedContainerColor = MindBackgroundDark,
                                    focusedBorderColor = PastelSky,
                                    unfocusedBorderColor = MindGlassBorderSubtle,
                                    focusedTextColor = MindTextPrimary,
                                    unfocusedTextColor = MindTextPrimary
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Description Field
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Detailed Vision (min 25 chars)") },
                                placeholder = { Text("Explain what problem this solves and how it should behave...") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MindBackgroundDark,
                                    unfocusedContainerColor = MindBackgroundDark,
                                    focusedBorderColor = PastelSky,
                                    unfocusedBorderColor = MindGlassBorderSubtle,
                                    focusedTextColor = MindTextPrimary,
                                    unfocusedTextColor = MindTextPrimary
                                ),
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                val length = description.trim().length
                                Text(
                                    text = "$length / 25 characters",
                                    fontSize = 10.sp,
                                    color = if (length >= 25) PastelMint else MindTextTertiary
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    isSubmitting = true
                                    val res = store.submitProposal(title, description, selectedCategory)
                                    isSubmitting = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Proposal submitted! Thank you for shaping MindLink.", Toast.LENGTH_LONG).show()
                                        title = ""
                                        description = ""
                                        proposals = store.getProposals()
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to submit", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = title.isNotBlank() && description.trim().length >= 25 && !isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = MindIndigo),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submit Proposal to Roadmap")
                            }
                        }
                    }
                }
            }

            // 2. Previous Proposals
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR SUBMITTED PROPOSALS (${proposals.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MindTextTertiary,
                        letterSpacing = 1.sp
                    )
                }
            }

            if (proposals.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.03f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No feature proposals submitted yet. When you submit ideas, track their engineering status here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MindTextTertiary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(proposals) { item ->
                    ProposalItemCard(item = item, onEmailDeveloper = {
                        val body = store.formatEmailBody(item, activeNotesCount, currentTier)
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:georgepoyilil@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "[MindLink Proposal] ${item.title}")
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send Proposal via Email"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun ProposalItemCard(
    item: FeatureProposal,
    onEmailDeveloper: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MindGlassSurfaceVariant),
        border = BorderStroke(1.dp, MindGlassBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PastelSky.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.category,
                        color = PastelSky,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PastelMint.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.status,
                        color = PastelMint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MindTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MindTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MindGlassBorderSubtle)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MindTextTertiary,
                    fontSize = 10.sp
                )

                TextButton(
                    onClick = onEmailDeveloper,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(13.dp), tint = PastelLavender)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Email to Dev", fontSize = 11.sp, color = PastelLavender)
                }
            }
        }
    }
}

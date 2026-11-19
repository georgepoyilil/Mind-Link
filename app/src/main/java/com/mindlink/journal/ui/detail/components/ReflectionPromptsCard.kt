package com.mindlink.journal.ui.detail.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.LocalTechMode
import com.mindlink.journal.ui.theme.*

@Composable
fun ReflectionPromptsCard(
    prompts: List<String>,
    onAnswerSubmitted: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (prompts.isEmpty()) return

    val isTechMode = LocalTechMode.current
    var activeAnsweringIndex by remember { mutableStateOf<Int?>(null) }
    var answerInputText by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MindGlassSurface
        ),
        border = BorderStroke(1.2.dp, MindAmber.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
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
                        Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = PastelAmber, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isTechMode) "Evolving Socratic Prompts" else "💭 Questions for Yourself",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PastelAmber.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, PastelAmber.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = if (isTechMode) "LIVE CONTEXT" else "REFLECT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PastelAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isTechMode) {
                            "Answer any prompt to evolve deeper questions and update your graph"
                        } else {
                            "Thoughtful prompts to help you reflect on this entry"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MindTextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                prompts.forEachIndexed { index, prompt ->
                    val isAnswering = activeAnsweringIndex == index

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isAnswering) MindGlassSurfaceElevated else MindGlassSurfaceVariant,
                        border = BorderStroke(if (isAnswering) 1.2.dp else 0.5.dp, if (isAnswering) PastelLavender else MindGlassBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = PastelLavender.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PastelLavender
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MindTextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 20.sp
                                    )

                                    if (!isAnswering) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MindGlassButtonContainer,
                                            border = BorderStroke(0.8.dp, MindGlassButtonBorder),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    activeAnsweringIndex = index
                                                    answerInputText = ""
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.QuestionAnswer,
                                                    contentDescription = null,
                                                    tint = PastelLavender,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "Answer & Deepen Thought",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PastelLavender
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Expandable Interactive Answer Area
                            AnimatedVisibility(visible = isAnswering) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    OutlinedTextField(
                                        value = answerInputText,
                                        onValueChange = { answerInputText = it },
                                        placeholder = {
                                            Text(
                                                text = "Your honest reflection or realization here...",
                                                color = MindTextTertiary,
                                                fontSize = 13.sp
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MindTextPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MindGlassSurface,
                                            unfocusedContainerColor = MindGlassSurface,
                                            focusedBorderColor = PastelLavender,
                                            unfocusedBorderColor = MindGlassBorderSubtle,
                                            focusedTextColor = MindTextPrimary,
                                            unfocusedTextColor = MindTextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 5
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { activeAnsweringIndex = null }) {
                                            Text("Cancel", color = MindTextTertiary, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (answerInputText.isNotBlank()) MindGlassButtonContainer else Color.Transparent,
                                            border = BorderStroke(1.dp, if (answerInputText.isNotBlank()) MindGlassButtonBorder else MindGlassBorderSubtle),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable(enabled = answerInputText.isNotBlank()) {
                                                    onAnswerSubmitted(prompt, answerInputText)
                                                    activeAnsweringIndex = null
                                                    answerInputText = ""
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (answerInputText.isNotBlank()) PastelLavender else MindTextTertiary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Submit & Evolve",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (answerInputText.isNotBlank()) PastelLavender else MindTextTertiary
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
    }
}

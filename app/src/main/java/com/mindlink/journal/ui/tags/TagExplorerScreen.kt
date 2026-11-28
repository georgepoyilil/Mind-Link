package com.mindlink.journal.ui.tags

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.domain.model.Note
import com.mindlink.journal.ui.theme.*
import java.util.Locale

enum class TagSortOrder {
    MOST_USED,
    ALPHABETICAL,
    RARE
}

data class NormalizedTag(
    val displayTag: String,
    val count: Int,
    val notes: List<Note>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagExplorerScreen(
    allNotes: List<Note>,
    initialSelectedTag: String? = null,
    onNavigateBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onFilterFeedByTag: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(TagSortOrder.MOST_USED) }
    var activeTagFilter by remember { mutableStateOf(initialSelectedTag) }

    // Normalize and aggregate tags case-insensitively
    val normalizedTags = remember(allNotes) {
        val tagToNotesMap = mutableMapOf<String, MutableList<Note>>()
        val canonicalNames = mutableMapOf<String, String>()

        allNotes.filter { !it.isDeleted }.forEach { note ->
            note.tags.forEach { rawTag ->
                val cleaned = rawTag.trim().trimStart('#')
                if (cleaned.isNotBlank()) {
                    val key = cleaned.lowercase(Locale.ROOT)
                    if (!canonicalNames.containsKey(key)) {
                        // Capitalize first char as canonical representation
                        canonicalNames[key] = cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    }
                    val list = tagToNotesMap.getOrPut(key) { mutableListOf() }
                    if (!list.contains(note)) {
                        list.add(note)
                    }
                }
            }
        }

        tagToNotesMap.map { (key, notes) ->
            NormalizedTag(
                displayTag = canonicalNames[key] ?: key,
                count = notes.size,
                notes = notes
            )
        }
    }

    // Filter by search query
    val filteredTags = remember(normalizedTags, searchQuery, sortOrder) {
        val list = if (searchQuery.isBlank()) {
            normalizedTags
        } else {
            normalizedTags.filter { it.displayTag.contains(searchQuery.trim(), ignoreCase = true) }
        }

        when (sortOrder) {
            TagSortOrder.MOST_USED -> list.sortedByDescending { it.count }
            TagSortOrder.ALPHABETICAL -> list.sortedBy { it.displayTag.lowercase(Locale.ROOT) }
            TagSortOrder.RARE -> list.sortedBy { it.count }
        }
    }

    val selectedTagData = remember(activeTagFilter, normalizedTags) {
        activeTagFilter?.let { target ->
            normalizedTags.firstOrNull { it.displayTag.equals(target, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hashtags & Tag Explorer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MindTextPrimary
                        )
                        Text(
                            text = "${normalizedTags.size} tags across ${allNotes.filter { !it.isDeleted }.size} thoughts",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search hashtags...", color = MindTextTertiary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MindTextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MindTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MindGlassSurfaceVariant,
                    unfocusedContainerColor = MindGlassSurfaceVariant,
                    focusedBorderColor = PastelLavender,
                    unfocusedBorderColor = MindGlassBorderSubtle,
                    focusedTextColor = MindTextPrimary,
                    unfocusedTextColor = MindTextPrimary
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sort Options Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort:", style = MaterialTheme.typography.labelSmall, color = MindTextTertiary)

                FilterChip(
                    selected = sortOrder == TagSortOrder.MOST_USED,
                    onClick = { sortOrder = TagSortOrder.MOST_USED },
                    label = { Text("🔥 Most Used", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PastelLavender.copy(alpha = 0.2f),
                        selectedLabelColor = PastelLavender
                    )
                )

                FilterChip(
                    selected = sortOrder == TagSortOrder.ALPHABETICAL,
                    onClick = { sortOrder = TagSortOrder.ALPHABETICAL },
                    label = { Text("🔤 A-Z", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PastelLavender.copy(alpha = 0.2f),
                        selectedLabelColor = PastelLavender
                    )
                )

                FilterChip(
                    selected = sortOrder == TagSortOrder.RARE,
                    onClick = { sortOrder = TagSortOrder.RARE },
                    label = { Text("✨ Rare", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PastelLavender.copy(alpha = 0.2f),
                        selectedLabelColor = PastelLavender
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tags FlowRow Area (or list of notes for selected tag)
            if (activeTagFilter == null) {
                Text(
                    text = "ALL TAGS (${filteredTags.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MindTextTertiary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredTags.forEach { tagItem ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MindGlassSurfaceVariant,
                                border = BorderStroke(1.dp, MindGlassBorderSubtle),
                                modifier = Modifier.clickable { activeTagFilter = tagItem.displayTag }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${tagItem.displayTag}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PastelLavender
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = "${tagItem.count}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MindTextSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Showing Notes for Selected Tag
                selectedTagData?.let { tagData ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { activeTagFilter = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close tag", tint = MindTextSecondary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "#${tagData.displayTag}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PastelLavender
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${tagData.count} thoughts)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MindTextTertiary
                            )
                        }

                        Button(
                            onClick = { onFilterFeedByTag(tagData.displayTag) },
                            colors = ButtonDefaults.buttonColors(containerColor = MindIndigo),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Filter in Feed", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(tagData.notes) { note ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MindGlassSurfaceVariant),
                                border = BorderStroke(1.dp, MindGlassBorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNoteClick(note.id) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = note.title.ifBlank { "Untitled Thought" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MindTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.content.take(150),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MindTextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = note.formattedDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MindTextTertiary,
                                            fontSize = 10.sp
                                        )
                                        if (!note.mood.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = PastelSky.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = note.mood!!,
                                                    color = PastelSky,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

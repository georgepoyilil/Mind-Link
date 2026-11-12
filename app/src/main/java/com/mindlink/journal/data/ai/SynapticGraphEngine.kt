package com.mindlink.journal.data.ai

import com.mindlink.journal.data.local.entity.NoteEntity
import com.mindlink.journal.data.local.entity.NoteLinkEntity
import java.util.Locale
import java.util.regex.Pattern

/**
 * Synaptic Graph Engine:
 * Analyzes journal notes to generate synaptic knowledge edges:
 * 1. Explicit wikilinks ([[Note Title]])
 * 2. Multi-dimensional tag intersections (#Burnout, #Work, #Love, etc.)
 * 3. Topic & thematic co-occurrence
 */
class SynapticGraphEngine {

    private val wikilinkPattern = Pattern.compile("\\[\\[([^\\]]+)\\]\\]")

    /**
     * Rebuilds all synaptic edges across the given collection of notes.
     */
    fun computeSynapticLinks(notes: List<NoteEntity>): List<NoteLinkEntity> {
        if (notes.size < 2) return emptyList()

        val generatedLinks = mutableListOf<NoteLinkEntity>()
        val notesById = notes.associateBy { it.id }
        val titleToIdMap = notes.associate { it.title.trim().lowercase(Locale.ROOT) to it.id }

        // Track existing edge pairs to avoid redundant reciprocal edges
        val connectedPairs = mutableSetOf<String>()

        fun pairKey(id1: Long, id2: Long): String {
            return if (id1 < id2) "$id1-$id2" else "$id2-$id1"
        }

        // 1. Explicit Wikilinks Detection ([[Title]])
        for (source in notes) {
            val matcher = wikilinkPattern.matcher(source.content)
            while (matcher.find()) {
                val targetTitle = matcher.group(1)?.trim()?.lowercase(Locale.ROOT) ?: continue
                val targetId = titleToIdMap[targetTitle]
                    ?: notes.firstOrNull { it.title.lowercase(Locale.ROOT).contains(targetTitle) }?.id

                if (targetId != null && targetId != source.id) {
                    val key = pairKey(source.id, targetId)
                    if (connectedPairs.add(key)) {
                        generatedLinks.add(
                            NoteLinkEntity(
                                sourceNoteId = source.id,
                                targetNoteId = targetId,
                                linkType = "WIKILINK",
                                weight = 1.0f,
                                label = "[[${matcher.group(1)}]]"
                            )
                        )
                    }
                }
            }
        }

        // 2. Shared Multi-dimensional Tags & Behavioral Patterns
        for (i in notes.indices) {
            val noteA = notes[i]
            val tagsA = noteA.tags.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }.toSet()

            for (j in i + 1 until notes.size) {
                val noteB = notes[j]
                val key = pairKey(noteA.id, noteB.id)
                if (connectedPairs.contains(key)) continue // Already linked via wikilink

                val tagsB = noteB.tags.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }.toSet()
                val sharedTags = tagsA.intersect(tagsB)

                if (sharedTags.isNotEmpty()) {
                    val weight = (0.4f + (sharedTags.size * 0.2f)).coerceAtMost(0.95f)
                    val label = sharedTags.take(2).joinToString(" & ") { "#$it" }
                    if (connectedPairs.add(key)) {
                        generatedLinks.add(
                            NoteLinkEntity(
                                sourceNoteId = noteA.id,
                                targetNoteId = noteB.id,
                                linkType = "TAG_CLUSTER",
                                weight = weight,
                                label = label
                            )
                        )
                    }
                } else if (noteA.mood != null && noteA.mood.equals(noteB.mood, ignoreCase = true) && noteA.tags.isNotEmpty() && noteB.tags.isNotEmpty()) {
                    // 3. Shared emotional polarity / mood continuity
                    val weight = 0.35f
                    val label = "Mood: ${noteA.mood}"
                    if (connectedPairs.add(key)) {
                        generatedLinks.add(
                            NoteLinkEntity(
                                sourceNoteId = noteA.id,
                                targetNoteId = noteB.id,
                                linkType = "EMOTION_FLOW",
                                weight = weight,
                                label = label
                            )
                        )
                    }
                }
            }
        }

        return generatedLinks
    }
}

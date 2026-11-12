package com.mindlink.journal.domain.model

/**
 * Knowledge Graph Node representing a thought in the synaptic network.
 */
data class ThoughtGraphNode(
    val id: Long,
    val title: String,
    val preview: String,
    val mood: String?,
    val moodEmoji: String,
    val tags: List<String>,
    val degree: Int = 0,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = (38f + (degree * 4f)).coerceIn(38f, 62f),
    val colorLong: Long = when (mood?.lowercase()?.trim()) {
        "reflective" -> 0xFF8B5CF6 // MindViolet
        "analytical" -> 0xFF06B6D4 // MindCyan
        "creative" -> 0xFFF59E0B   // MindAmber
        "energized" -> 0xFF10B981  // MindEmerald
        "stressed" -> 0xFF64748B   // MindTextTertiary
        "calm" -> 0xFF34D399       // Mint
        "vulnerable", "love" -> 0xFFEC4899 // MindPink
        "grateful" -> 0xFFF472B6
        else -> 0xFF6366F1         // MindIndigo
    }
)

/**
 * Knowledge Graph Edge representing a synaptic link between two thoughts.
 */
data class ThoughtGraphEdge(
    val sourceId: Long,
    val targetId: Long,
    val linkType: String,
    val weight: Float,
    val label: String?
)

/**
 * Cluster grouping for topic/domain categorization in the graph.
 */
data class ThoughtGraphCluster(
    val name: String,
    val nodeCount: Int,
    val colorLong: Long
)

/**
 * Complete Thought Graph structure ready for 2D force-directed simulation and rendering.
 */
data class ThoughtGraph(
    val nodes: List<ThoughtGraphNode> = emptyList(),
    val edges: List<ThoughtGraphEdge> = emptyList(),
    val clusters: List<ThoughtGraphCluster> = emptyList()
) {
    val totalThoughts: Int get() = nodes.size
    val totalSynapses: Int get() = edges.size
}

package com.mindlink.journal.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.domain.model.ThoughtGraph
import com.mindlink.journal.domain.model.ThoughtGraphNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class ThoughtGraphUiState(
    val graph: ThoughtGraph = ThoughtGraph(),
    val selectedNode: ThoughtGraphNode? = null,
    val selectedTagFilter: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class ThoughtGraphViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _selectedNode = MutableStateFlow<ThoughtGraphNode?>(null)
    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)

    // Raw graph stream from Room
    private val _rawGraph = repository.getThoughtGraph()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThoughtGraph())

    // Simulated layout nodes with 2D positions
    private val _layoutNodes = MutableStateFlow<List<ThoughtGraphNode>>(emptyList())

    val uiState: StateFlow<ThoughtGraphUiState> = combine(
        _rawGraph,
        _layoutNodes,
        _selectedNode,
        _selectedTagFilter,
        _searchQuery
    ) { rawGraph, layoutNodes, selectedNode, tagFilter, query ->
        // Sync layout coordinates back into the graph
        val activeNodes = if (layoutNodes.size == rawGraph.nodes.size) layoutNodes else rawGraph.nodes

        val filteredNodes = activeNodes.filter { node ->
            val matchesTag = tagFilter == null || node.tags.any { it.equals(tagFilter, ignoreCase = true) }
            val matchesQuery = query.isBlank() ||
                    node.title.contains(query, ignoreCase = true) ||
                    node.tags.any { it.contains(query, ignoreCase = true) }
            matchesTag && matchesQuery
        }

        val filteredNodeIds = filteredNodes.map { it.id }.toSet()
        val filteredEdges = rawGraph.edges.filter {
            filteredNodeIds.contains(it.sourceId) && filteredNodeIds.contains(it.targetId)
        }

        ThoughtGraphUiState(
            graph = rawGraph.copy(nodes = filteredNodes, edges = filteredEdges),
            selectedNode = selectedNode,
            selectedTagFilter = tagFilter,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThoughtGraphUiState())

    init {
        viewModelScope.launch {
            _rawGraph.collect { graph ->
                if (graph.nodes.isNotEmpty()) {
                    initializeGraphLayout(graph.nodes)
                }
            }
        }
    }

    private fun initializeGraphLayout(nodes: List<ThoughtGraphNode>) {
        val count = nodes.size
        val radius = 320f
        val layout = nodes.mapIndexed { index, node ->
            val angle = (2.0 * Math.PI * index / count.coerceAtLeast(1)).toFloat()
            val jitter = (Random.nextFloat() - 0.5f) * 60f
            node.copy(
                x = (radius + jitter) * cos(angle),
                y = (radius + jitter) * sin(angle),
                vx = 0f,
                vy = 0f
            )
        }

        // Run relaxation iterations to settle force-directed layout
        val settled = runForceRelaxation(layout, _rawGraph.value.edges, iterations = 80)
        _layoutNodes.value = settled
    }

    private fun runForceRelaxation(
        nodes: List<ThoughtGraphNode>,
        edges: List<com.mindlink.journal.domain.model.ThoughtGraphEdge>,
        iterations: Int
    ): List<ThoughtGraphNode> {
        val current = nodes.map { it.copy() }.toMutableList()
        val nodeMap = current.associateBy { it.id }

        val kRepulsion = 14000f
        val kSpring = 0.05f
        val restLength = 160f
        val kCenter = 0.008f
        val damping = 0.82f

        for (step in 0 until iterations) {
            // 1. Repulsion between all node pairs
            for (i in current.indices) {
                val nodeA = current[i]
                for (j in i + 1 until current.size) {
                    val nodeB = current[j]
                    val dx = nodeB.x - nodeA.x
                    val dy = nodeB.y - nodeA.y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(20f)
                    val force = kRepulsion / (dist * dist)
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force

                    nodeA.vx -= fx
                    nodeA.vy -= fy
                    nodeB.vx += fx
                    nodeB.vy += fy
                }
            }

            // 2. Spring attraction along connected edges
            for (edge in edges) {
                val source = nodeMap[edge.sourceId] ?: continue
                val target = nodeMap[edge.targetId] ?: continue
                val dx = target.x - source.x
                val dy = target.y - source.y
                val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val displacement = dist - restLength
                val force = kSpring * displacement * edge.weight
                val fx = (dx / dist) * force
                val fy = (dy / dist) * force

                source.vx += fx
                source.vy += fy
                target.vx -= fx
                target.vy -= fy
            }

            // 3. Center gravity & integrate positions
            for (node in current) {
                node.vx -= node.x * kCenter
                node.vy -= node.y * kCenter

                node.vx *= damping
                node.vy *= damping

                node.x += node.vx.coerceIn(-30f, 30f)
                node.y += node.vy.coerceIn(-30f, 30f)
            }
        }

        return current
    }

    fun selectNode(node: ThoughtGraphNode?) {
        _selectedNode.value = node
    }

    fun filterByTag(tag: String?) {
        _selectedTagFilter.value = if (_selectedTagFilter.value == tag) null else tag
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun rebuildGraph() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.rebuildSynapticGraph()
            _isLoading.value = false
        }
    }

    class Factory(private val repository: NoteRepository) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ThoughtGraphViewModel(repository) as T
        }
    }
}

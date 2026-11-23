package com.mindlink.journal.ui.graph

import android.graphics.Paint
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindlink.journal.LocalTechMode
import com.mindlink.journal.domain.model.ThoughtGraphNode
import com.mindlink.journal.ui.theme.*
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThoughtGraphScreen(
    viewModel: ThoughtGraphViewModel,
    onNavigateBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onSecondBrainClick: () -> Unit = {}
) {
    val isTechMode = LocalTechMode.current
    val uiState by viewModel.uiState.collectAsState()

    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val nativePaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(com.mindlink.journal.R.drawable.ic_thought_constellation),
                            contentDescription = "Thought Constellation",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isTechMode) "Synaptic Network (Inspector)" else "Thought Constellation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MindTextPrimary
                            )
                            Text(
                                text = if (isTechMode) {
                                    "${uiState.graph.totalThoughts} nodes · ${uiState.graph.totalSynapses} edges · 384-d Cosine"
                                } else {
                                    "${uiState.graph.totalThoughts} connected memories"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isTechMode) PastelSky else MindTextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MindTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scale = 1.0f
                        offset = Offset.Zero
                    }) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Center View",
                            tint = MindTextSecondary
                        )
                    }
                    IconButton(onClick = { viewModel.rebuildGraph() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reorganize Thoughts",
                            tint = if (isTechMode) PastelSky else MindIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MindBackgroundAmoled)
            )
        },
        containerColor = MindBackgroundAmoled
    ) { paddingValues ->
        val isThemeDark = LocalMindLinkColors.current.isDark
        val nodeCoreColor = MindSurfaceAmoled

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MindBackgroundAmoled)
        ) {
            val currentNodes by rememberUpdatedState(uiState.graph.nodes)
            val currentScale by rememberUpdatedState(scale)
            val currentOffset by rememberUpdatedState(offset)

            // Interactive 2D Canvas for Synaptic Graph
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val centerX = size.width / 2f + currentOffset.x
                            val centerY = size.height / 2f + currentOffset.y

                            // Test collision with nodes: select closest node within touch tolerance
                            var closestNode: ThoughtGraphNode? = null
                            var minDistance = Float.MAX_VALUE
                            for (node in currentNodes) {
                                val nodeScreenX = centerX + node.x * currentScale
                                val nodeScreenY = centerY + node.y * currentScale
                                val dx = tapOffset.x - nodeScreenX
                                val dy = tapOffset.y - nodeScreenY
                                val dist = sqrt(dx * dx + dy * dy)
                                val hitRadius = (node.radius * currentScale).coerceAtLeast(65f)

                                if (dist <= hitRadius && dist < minDistance) {
                                    minDistance = dist
                                    closestNode = node
                                }
                            }
                            viewModel.selectNode(closestNode)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.35f, 3.5f)
                            offset += pan
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2f + offset.x
                val centerY = canvasHeight / 2f + offset.y

                val nodeMap = uiState.graph.nodes.associateBy { it.id }
                val selectedId = uiState.selectedNode?.id

                // 1. Draw Edges (Synapses)
                for (edge in uiState.graph.edges) {
                    val source = nodeMap[edge.sourceId] ?: continue
                    val target = nodeMap[edge.targetId] ?: continue

                    val startX = centerX + source.x * scale
                    val startY = centerY + source.y * scale
                    val endX = centerX + target.x * scale
                    val endY = centerY + target.y * scale

                    val isConnectedToSelected = selectedId != null &&
                            (edge.sourceId == selectedId || edge.targetId == selectedId)

                    val edgeColor = if (selectedId == null) {
                        Color(0x3D818CF8) // Subtle neon indigo
                    } else if (isConnectedToSelected) {
                        Color(0xFF22D3EE) // Bright Cyan active highlight
                    } else {
                        Color(0x10818CF8) // Dimmed
                    }

                    val strokeWidth = if (isConnectedToSelected) 3.5f * scale else (1.5f * edge.weight * scale).coerceAtLeast(1f)

                    drawLine(
                        color = edgeColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeWidth
                    )
                }

                // 2. Draw Nodes
                for (node in uiState.graph.nodes) {
                    val nx = centerX + node.x * scale
                    val ny = centerY + node.y * scale
                    val nr = (node.radius * scale).coerceIn(16f, 70f)
                    val isSelected = node.id == selectedId

                    val baseColor = Color(node.colorLong)

                    // Outer glowing halo
                    val haloAlpha = if (isSelected) 0.45f else 0.18f
                    drawCircle(
                        color = baseColor.copy(alpha = haloAlpha),
                        radius = nr * 1.5f,
                        center = Offset(nx, ny)
                    )

                    // Solid obsidian core
                    drawCircle(
                        color = nodeCoreColor,
                        radius = nr,
                        center = Offset(nx, ny)
                    )

                    // Neon rim
                    val rimColor = if (isSelected) (if (isThemeDark) Color.White else Color.Black) else baseColor
                    drawCircle(
                        color = rimColor,
                        radius = nr,
                        center = Offset(nx, ny),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = if (isSelected) 3f * scale else 1.8f * scale
                        )
                    )

                    // Draw clean, readable title inside the node circle (without distracting emojis)
                    drawIntoCanvas { canvas ->
                        nativePaint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                        nativePaint.color = if (isThemeDark) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#0F172A")
                        val fontSize = (nr * 0.32f).coerceIn(11f, 20f)
                        nativePaint.textSize = fontSize

                        val words = node.title.trim().split(Regex("\\s+"))
                        val lineList = if (words.size >= 4) {
                            listOf(
                                "${words[0]} ${words[1]}",
                                "${words[2]} ${words.getOrNull(3) ?: ""}".trim()
                            )
                        } else if (words.size in 2..3) {
                            listOf(words[0], words.drop(1).joinToString(" "))
                        } else {
                            listOf(node.title)
                        }

                        if (lineList.size == 1) {
                            val raw = lineList[0]
                            val text = if (raw.length > 11) "${raw.take(9)}.." else raw
                            canvas.nativeCanvas.drawText(
                                text,
                                nx,
                                ny + (fontSize * 0.35f),
                                nativePaint
                            )
                        } else {
                            val line1 = if (lineList[0].length > 11) "${lineList[0].take(9)}.." else lineList[0]
                            val line2 = if (lineList[1].length > 11) "${lineList[1].take(9)}.." else lineList[1]
                            canvas.nativeCanvas.drawText(
                                line1,
                                nx,
                                ny - (fontSize * 0.15f),
                                nativePaint
                            )
                            canvas.nativeCanvas.drawText(
                                line2,
                                nx,
                                ny + (fontSize * 0.95f),
                                nativePaint
                            )
                        }
                    }
                }
            }

            // Top Clusters Filter Pills Carousel
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedTagFilter == null,
                        onClick = { viewModel.filterByTag(null) },
                        label = { Text("All (${uiState.graph.nodes.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MindIndigo,
                            selectedLabelColor = Color.White,
                            containerColor = MindGlassSurface,
                            labelColor = MindTextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    uiState.graph.clusters.forEach { cluster ->
                        val isSelected = uiState.selectedTagFilter.equals(cluster.name.removePrefix("#"), ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.filterByTag(cluster.name.removePrefix("#")) },
                            label = { Text("${cluster.name} (${cluster.nodeCount})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MindIndigo,
                                selectedLabelColor = Color.White,
                                containerColor = MindGlassSurface,
                                labelColor = MindTextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Selected Node Slide-in Frosted Glass Card
            AnimatedVisibility(
                visible = uiState.selectedNode != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                uiState.selectedNode?.let { node ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MindGlassSurfaceElevated),
                        border = BorderStroke(1.dp, MindPrismGlassCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color(node.colorLong), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = node.mood ?: "Thought Node",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(node.colorLong)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MindGlassButtonContainer,
                                        border = BorderStroke(0.5.dp, MindGlassButtonBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isTechMode) Icons.Default.Hub else Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = if (isTechMode) PastelSky else PastelLavender,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isTechMode) "k = ${node.degree} synapses" else "${node.degree} connections",
                                                fontSize = 10.sp,
                                                color = if (isTechMode) PastelSky else PastelLavender,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { viewModel.selectNode(null) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MindTextTertiary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Title in Calibri
                            Text(
                                text = node.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MindTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Preview
                            Text(
                                text = node.preview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MindTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Connected Insights Section (Interactive traversal)
                            val connectedNodeIds = remember(node.id, uiState.graph.edges) {
                                uiState.graph.edges
                                    .filter { it.sourceId == node.id || it.targetId == node.id }
                                    .map { if (it.sourceId == node.id) it.targetId else it.sourceId }
                                    .toSet()
                            }
                            val connectedNodes = remember(connectedNodeIds, uiState.graph.nodes) {
                                uiState.graph.nodes.filter { it.id in connectedNodeIds }
                            }

                            if (connectedNodes.isNotEmpty()) {
                                Text(
                                    text = "Connected Synaptic Links (${connectedNodes.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PastelLavender,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    connectedNodes.forEach { neighbor ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MindGlassSurfaceVariant,
                                            border = BorderStroke(0.5.dp, Color(neighbor.colorLong).copy(alpha = 0.5f)),
                                            modifier = Modifier.clickable { viewModel.selectNode(neighbor) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(Color(neighbor.colorLong), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = neighbor.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MindTextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Tags Row
                            if (node.tags.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    node.tags.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MindGlassSurfaceVariant,
                                            border = BorderStroke(0.5.dp, MindGlassBorderSubtle)
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MindTextSecondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Action Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onSecondBrainClick,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    border = BorderStroke(1.dp, MindIndigo.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = MindIndigo,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Synthesize", fontSize = 12.sp, color = MindIndigo, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onNoteClick(node.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MindIndigo),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Open Thought", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Overview Bottom Intelligence Panel (Active when no single node is selected)
            AnimatedVisibility(
                visible = uiState.selectedNode == null && uiState.graph.nodes.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MindGlassSurfaceElevated),
                    border = BorderStroke(1.dp, MindPrismGlassCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = PastelLavender,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Thought Constellation",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MindTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${uiState.graph.nodes.size} thoughts · ${uiState.graph.edges.size} synapses · Tap node to inspect",
                                style = MaterialTheme.typography.labelSmall,
                                color = MindTextSecondary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    scale = 1.0f
                                    offset = Offset.Zero
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = "Center View",
                                    tint = MindTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Button(
                                onClick = onSecondBrainClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MindIndigo),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Synthesize Brain", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

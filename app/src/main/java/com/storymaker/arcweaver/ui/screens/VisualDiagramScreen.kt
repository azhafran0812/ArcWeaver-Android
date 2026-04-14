package com.storymaker.arcweaver.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.data.repository.AppDatabase
import com.storymaker.arcweaver.data.repository.StoryRepository
import com.storymaker.arcweaver.viewmodel.VisualMapViewModel
import com.storymaker.arcweaver.viewmodel.VisualMapViewModelFactory
import kotlin.math.roundToInt

// Konstanta ukuran fisik Node di dalam Kanvas
private val NodeWidth = 240.dp
private val NodeHeight = 120.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualDiagramScreen(
    projectId: Int,
    onNavigateToEditor: (Int?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val viewModel: VisualMapViewModel = viewModel(
        factory = VisualMapViewModelFactory(projectId, StoryRepository(database.storyDao()))
    )

    // State Kamera (Viewport)
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    var cameraPan by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back to Project", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    // Kontrol Zoom di Top Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp)
                    ) {
                        IconButton(onClick = { cameraZoom = (cameraZoom - 0.1f).coerceAtLeast(0.3f) }) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                        }
                        Text("${(cameraZoom * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { cameraZoom = (cameraZoom + 0.1f).coerceAtMost(2f) }) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Node") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5)) // Warna latar grid
                // Deteksi Pan & Zoom pada latar belakang (Bukan pada Node)
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        cameraZoom = (cameraZoom * zoomChange).coerceIn(0.3f, 2f)
                        cameraPan += panChange / cameraZoom
                    }
                }
        ) {
            // 1. MENGGAMBAR GARIS GRID
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 40.dp.toPx() * cameraZoom
                val offsetX = (cameraPan.x * cameraZoom) % gridSpacing
                val offsetY = (cameraPan.y * cameraZoom) % gridSpacing

                val numLinesX = (size.width / gridSpacing).toInt() + 2
                val numLinesY = (size.height / gridSpacing).toInt() + 2

                for (i in 0..numLinesX) {
                    val x = offsetX + (i * gridSpacing)
                    drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                }
                for (i in 0..numLinesY) {
                    val y = offsetY + (i * gridSpacing)
                    drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                }
            }

            // 2. MENGGAMBAR GARIS PENGHUBUNG (EDGES / LINES)
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val nodeWidthPx = NodeWidth.toPx()
                val nodeHeightPx = NodeHeight.toPx()

                viewModel.nodes.forEach { parentNode ->
                    // Posisi absolut Node Parent di layar
                    val startX = (parentNode.canvasX + cameraPan.x) * cameraZoom + (nodeWidthPx * cameraZoom / 2)
                    val startY = (parentNode.canvasY + cameraPan.y) * cameraZoom + (nodeHeightPx * cameraZoom) // Titik bawah Node

                    val nodeChoices = viewModel.choices.filter { it.parentNodeId == parentNode.nodeId }

                    // Jika memiliki pilihan
                    nodeChoices.forEach { choice ->
                        val targetNode = viewModel.nodes.find { it.nodeId == choice.targetNodeId }
                        if (targetNode != null) {
                            val endX = (targetNode.canvasX + cameraPan.x) * cameraZoom + (nodeWidthPx * cameraZoom / 2)
                            val endY = (targetNode.canvasY + cameraPan.y) * cameraZoom // Titik atas Node Target

                            val path = Path().apply {
                                moveTo(startX, startY)
                                // Cubic Bezier Curve agar garis terlihat melengkung elegan
                                cubicTo(
                                    x1 = startX, y1 = startY + (100 * cameraZoom), // Control point 1
                                    x2 = endX, y2 = endY - (100 * cameraZoom),   // Control point 2
                                    x3 = endX, y3 = endY
                                )
                            }

                            // Jika ada syarat/kondisi, gambar garis putus-putus
                            val isConditional = !choice.requiredCondition.isNullOrBlank()
                            val pathEffect = if (isConditional) PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f) else null

                            drawPath(
                                path = path,
                                color = primaryColor.copy(alpha = 0.6f),
                                style = Stroke(width = 4f * cameraZoom, pathEffect = pathEffect)
                            )
                        }
                    }

                    // Jika ini cerita linear (tanpa pilihan tapi punya nextNodeId)
                    if (nodeChoices.isEmpty() && parentNode.nextNodeId != null) {
                        val targetNode = viewModel.nodes.find { it.nodeId == parentNode.nextNodeId }
                        if (targetNode != null) {
                            val endX = (targetNode.canvasX + cameraPan.x) * cameraZoom + (nodeWidthPx * cameraZoom / 2)
                            val endY = (targetNode.canvasY + cameraPan.y) * cameraZoom

                            val path = Path().apply {
                                moveTo(startX, startY)
                                cubicTo(startX, startY + (100 * cameraZoom), endX, endY - (100 * cameraZoom), endX, endY)
                            }
                            drawPath(path = path, color = primaryColor.copy(alpha = 0.6f), style = Stroke(width = 4f * cameraZoom))
                        }
                    }
                }
            }

            // 3. MERENDER KOTAK NODE DI ATAS KANVAS
            viewModel.nodes.forEach { node ->
                DraggableNodeCard(
                    node = node,
                    cameraZoom = cameraZoom,
                    cameraPan = cameraPan,
                    onPositionChanged = { newX, newY -> viewModel.updateNodePosition(node, newX, newY) }
                )
            }

            // 4. KOTAK LEGENDA / INFO
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .width(220.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendItem("Drag", "nodes to reposition")
                    LegendItem("Pinch", "to zoom in/out")
                    LegendItem("Swipe", "to pan canvas")
                    Divider()
                    LegendItem("Solid line", "= direct choice")
                    LegendItem("Dashed line", "= conditional")
                }
            }
        }
    }
}

// --- KOMPONEN NODE YANG BISA DISERET ---
@Composable
fun DraggableNodeCard(
    node: StoryNodeEntity,
    cameraZoom: Float,
    cameraPan: Offset,
    onPositionChanged: (Float, Float) -> Unit
) {
    // State lokal agar saat node digeser, UI update seketika tanpa menunggu sinkronisasi database
    var offsetX by remember(node.canvasX) { mutableFloatStateOf(node.canvasX) }
    var offsetY by remember(node.canvasY) { mutableFloatStateOf(node.canvasY) }

    Box(
        modifier = Modifier
            // 1. Kalkulasi posisi di layar berdasarkan koordinat X/Y, Pan Kamera, dan Zoom
            .offset {
                IntOffset(
                    x = ((offsetX + cameraPan.x) * cameraZoom).roundToInt(),
                    y = ((offsetY + cameraPan.y) * cameraZoom).roundToInt()
                )
            }
            // 2. Sesuaikan ukuran fisik node dengan tingkat Zoom
            .size(NodeWidth * cameraZoom, NodeHeight * cameraZoom)
            // 3. Deteksi geseran khusus pada Node
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // Simpan koordinat baru ke database saat jari dilepas
                        onPositionChanged(offsetX, offsetY)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    // Membalikkan nilai drag dengan faktor zoom agar kecepatan geser akurat
                    offsetX += dragAmount.x / cameraZoom
                    offsetY += dragAmount.y / cameraZoom
                }
            }
    ) {
        // Desain Visual Node Card
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp * cameraZoom),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = (4f * cameraZoom).dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = (1f * cameraZoom).dp,
                        color = if (node.nextNodeId == null) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp * cameraZoom)
                    )
                    .padding((12f * cameraZoom).dp)
            ) {
                Text(
                    text = node.characterName.ifBlank { "Node #${node.nodeId}" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp * cameraZoom,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height((4f * cameraZoom).dp))
                Text(
                    text = node.dialogueText,
                    fontSize = 12.sp * cameraZoom,
                    lineHeight = 16.sp * cameraZoom,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LegendItem(boldText: String, normalText: String) {
    Row {
        Text(boldText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(normalText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
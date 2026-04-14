package com.storymaker.arcweaver.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storymaker.arcweaver.viewmodel.PlaytestViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaytestScreen(
    viewModel: PlaytestViewModel,
    projectId: Int,
    onExit: () -> Unit
) {
    // State untuk Variable Inspector (Accordion)
    var isInspectorExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") }, // Dikosongkan agar bersih seperti desain
                navigationIcon = {
                    TextButton(onClick = onExit, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit", tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exit Playtest", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { viewModel.startGame() },
                        modifier = Modifier.padding(end = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            when {
                viewModel.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                viewModel.isStoryEnded -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("The End", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.startGame() }) { Text("Play Again") }
                    }
                }
                viewModel.currentNode != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))

                            // --- CARD ADEGAN (NARRATIVE) ---
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // LOGIKA GAMBAR KARAKTER
                                        val imageUri = viewModel.currentNode?.characterImageUri
                                        if (!imageUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = imageUri,
                                                contentDescription = "Character Portrait",
                                                contentScale = ContentScale.Crop, // Memotong gambar agar fit (proporsional)
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                        } else {
                                            // Fallback Icon jika tidak ada gambar
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Text(
                                            text = viewModel.currentNode?.characterName?.ifBlank { "Narrator" } ?: "",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = parseMarkdownForPlaytest(viewModel.currentNode?.dialogueText ?: ""),
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = 24.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // --- AREA PILIHAN PEMAIN ---
                            val availableChoices = viewModel.currentChoices.filter { viewModel.isConditionMet(it.requiredCondition) }

                            if (viewModel.currentChoices.isEmpty()) {
                                // JIKA LINEAR NODE (Tidak ada pilihan sama sekali)
                                OutlinedButton(
                                    onClick = { viewModel.continueLinear() },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Continue", fontWeight = FontWeight.Bold)
                                }
                            } else if (availableChoices.isEmpty()) {
                                // Ada pilihan, tapi tidak ada yang memenuhi syarat (Dead End / Logic Error)
                                Text("No choices available based on current variables.", color = MaterialTheme.colorScheme.error)
                            } else {
                                // Render tombol pilihan
                                availableChoices.forEach { choice ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.makeChoice(choice) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text(
                                            text = choice.choiceText,
                                            modifier = Modifier.padding(16.dp),
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // --- VARIABLE INSPECTOR ---
                        Column(modifier = Modifier.padding(vertical = 24.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { isInspectorExpanded = !isInspectorExpanded },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Variable Inspector", fontWeight = FontWeight.Bold)
                                        Icon(if (isInspectorExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                                    }

                                    AnimatedVisibility(visible = isInspectorExpanded) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                            Divider(modifier = Modifier.padding(bottom = 12.dp))
                                            InspectorRow("Current Node ID", "${viewModel.currentNode?.nodeId ?: "-"}")
                                            InspectorRow("Nodes Visited", "${viewModel.nodesVisited}")
                                            InspectorRow("Story Path", viewModel.storyPath.joinToString(" > ").takeLast(30))

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Live Variables:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(4.dp))

                                            if (viewModel.variablesState.isEmpty()) {
                                                Text("No variables tracking.", fontSize = 12.sp)
                                            } else {
                                                viewModel.variablesState.forEach { (key, value) ->
                                                    InspectorRow(key, value)
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
}

// Komponen Helper untuk Baris Inspector
@Composable
fun InspectorRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Helper Markdown untuk Playtest
fun parseMarkdownForPlaytest(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                        i = end + 2
                    } else { append(text[i]); i++ }
                }
                text.startsWith("*", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && !text.startsWith("**", i)) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                else -> { append(text[i]); i++ }
            }
        }
    }
}
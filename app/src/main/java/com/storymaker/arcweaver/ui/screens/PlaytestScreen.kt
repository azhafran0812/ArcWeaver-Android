package com.storymaker.arcweaver.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.storymaker.arcweaver.parseRichText
import com.storymaker.arcweaver.viewmodel.PlaytestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaytestScreen(
    viewModel: PlaytestViewModel,
    onExit: () -> Unit
) {
    val currentNode by viewModel.currentNode.collectAsState()
    val currentChoices by viewModel.currentChoices.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()

    // Memuat node pertama secara otomatis saat layar ini dibuka
    LaunchedEffect(Unit) {
        viewModel.startGame()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playtest Mode") },
                navigationIcon = {
                    TextButton(onClick = onExit) { Text("Exit") }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1E1E1E)) // Background gelap ala VN
        ) {
            if (isGameOver || currentNode == null) {
                // --- LAYAR TAMAT (THE END) ---
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("THE END", style = MaterialTheme.typography.displayMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onExit) { Text("Return to Editor") }
                }
            } else {
                // --- LAYAR BERMAIN (VISUAL NOVEL) ---
                val node = currentNode!!

                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. AREA GAMBAR KARAKTER
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (node.characterImageUri != null) {
                            AsyncImage(
                                model = Uri.parse(node.characterImageUri),
                                contentDescription = "Character Sprite",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            )
                        }
                    }

                    // 2. AREA DIALOG TEXT BOX
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Nama Karakter
                            Text(
                                text = node.characterName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF90CAF9) // Biru pastel
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Teks Dialog dengan Text Formatter kita!
                            Text(
                                text = parseRichText(node.dialogueText),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(16.dp))

                            // 3. AREA PILIHAN (CHOICES)
                            if (currentChoices.isEmpty()) {
                                // Jika adegan ini tidak memiliki pilihan, tampilkan tombol "Next" untuk mengakhiri cerita
                                Button(
                                    onClick = { viewModel.endGame() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Next")
                                }
                            } else {
                                currentChoices.forEach { choice ->
                                    OutlinedButton(
                                        onClick = { viewModel.makeChoice(choice) },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text(choice.choiceText)
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
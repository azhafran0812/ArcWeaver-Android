package com.storymaker.arcweaver.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.storymaker.arcweaver.viewmodel.PlaytestViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaytestScreen(
    viewModel: PlaytestViewModel,
    projectId: Int,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var isInspectorExpanded by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    // --- PERSISTENT MEDIA STATE (BGM & Background) ---
    var activeBgmUri by remember { mutableStateOf<String?>(null) }
    var activeBgUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel.currentNode) {
        // Cek BGM
        val nodeBgm = viewModel.currentNode?.bgmUri
        if (!nodeBgm.isNullOrBlank() && nodeBgm != activeBgmUri) activeBgmUri = nodeBgm

        // Cek Background
        val nodeBg = viewModel.currentNode?.bgImageUri
        if (!nodeBg.isNullOrBlank() && nodeBg != activeBgUri) activeBgUri = nodeBg
    }

    // Memutar Musik BGM
    DisposableEffect(activeBgmUri) {
        var bgmPlayer: android.media.MediaPlayer? = null
        if (!activeBgmUri.isNullOrBlank()) {
            try {
                bgmPlayer = android.media.MediaPlayer.create(context, android.net.Uri.parse(activeBgmUri))
                bgmPlayer?.isLooping = true
                bgmPlayer?.start()
            } catch (e: Exception) { e.printStackTrace() }
        }
        onDispose {
            bgmPlayer?.stop()
            bgmPlayer?.release()
        }
    }


    // Memutar Suara Dialog Karakter
    DisposableEffect(viewModel.currentNode?.voiceLineUri) {
        var voicePlayer: MediaPlayer? = null
        val uriStr = viewModel.currentNode?.voiceLineUri
        if (!uriStr.isNullOrBlank()) {
            try {
                voicePlayer = MediaPlayer.create(context, Uri.parse(uriStr))
                voicePlayer?.start()
            } catch (e: Exception) { e.printStackTrace() }
        }
        onDispose {
            voicePlayer?.stop()
            voicePlayer?.release()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("") },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                viewModel.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                viewModel.isStoryEnded -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("THE END", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.startGame() }) { Text("Play Again") }
                    }
                }
                viewModel.currentNode != null -> {
                    Box(modifier = Modifier.fillMaxSize()) {

                        // --- LAYER 1: BACKGROUND
                        Crossfade(
                            targetState = activeBgUri,
                            animationSpec = tween(800),
                            label = "bg_crossfade"
                        ) { bg ->
                            if (!bg.isNullOrBlank()) {
                                AsyncImage(
                                    model = bg,
                                    contentDescription = "Background",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))
                            }
                        }

                        // --- LAYER 2: KONTEN TEKS & PILIHAN
                        AnimatedContent(
                            targetState = viewModel.currentNode,
                            transitionSpec = {
                                (slideInHorizontally(animationSpec = tween(400)) { width -> width } + fadeIn(animationSpec = tween(400))).togetherWith(
                                    slideOutHorizontally(animationSpec = tween(400)) { width -> -width } + fadeOut(animationSpec = tween(400))
                                )
                            },
                            label = "scene_transition"
                        ) { targetNode ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {

                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // --- CARD ADEGAN (NARRATIVE) ---
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(24.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val imageUri = targetNode?.characterImageUri
                                                if (!imageUri.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = imageUri, contentDescription = null, contentScale = ContentScale.Crop,
                                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) { Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(text = targetNode?.characterName?.ifBlank { "Narrator" } ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))

                                            // --- TYPEWRITER EFFECT LOGIC ---
                                            var skipTyping by remember(targetNode) { mutableStateOf(false) }
                                            val fullText = parseMarkdownForPlaytest(targetNode?.dialogueText ?: "")
                                            var displayedText by remember(targetNode) { mutableStateOf(AnnotatedString("")) }

                                            LaunchedEffect(targetNode, skipTyping) {
                                                if (skipTyping) {
                                                    displayedText = fullText
                                                } else {
                                                    displayedText = AnnotatedString("")
                                                    for (i in 0..fullText.length) {
                                                        displayedText = fullText.subSequence(0, i)
                                                        delay(25)
                                                    }
                                                }
                                            }

                                            Text(
                                                text = displayedText,
                                                style = MaterialTheme.typography.bodyLarge,
                                                lineHeight = 24.sp,
                                                modifier = Modifier.fillMaxWidth().clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) { skipTyping = true }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // --- AREA PILIHAN ---
                                    val availableChoices = viewModel.currentChoices.filter { viewModel.isConditionMet(it.requiredCondition) }
                                    if (viewModel.currentChoices.isEmpty()) {
                                        OutlinedButton(
                                            onClick = { viewModel.continueLinear() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                        ) { Text("Continue", fontWeight = FontWeight.Bold) }
                                    } else if (availableChoices.isEmpty()) {
                                        Text("No choices available based on current variables.", color = MaterialTheme.colorScheme.error)
                                    } else {
                                        availableChoices.forEach { choice ->
                                            val effectStr = choice.effect ?: ""
                                            val isNegative = effectStr.contains("-")
                                            val isGolden = effectStr.contains("+") && (effectStr.contains("gold", ignoreCase = true) || effectStr.contains("coin", ignoreCase = true))
                                            val hasEffect = effectStr.isNotBlank()

                                            val borderColor = when {
                                                isNegative -> Color(0xFFBA1A1A).copy(alpha = 0.6f)
                                                isGolden -> Color(0xFFD4AF37)
                                                hasEffect -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                else -> Color.Transparent
                                            }

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 12.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        // Haptic
                                                        if (hasEffect) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        else haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                                        // Play Audio SFX
                                                        playChoiceSFX(context, choice.voiceLineUri)

                                                        // Lanjut Logika
                                                        viewModel.makeChoice(choice)
                                                    },
                                                border = if (borderColor != Color.Transparent) BorderStroke(1.5.dp, borderColor) else null,
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // --- CUSTOM CHOICE ICON ---
                                                    if (!choice.iconUri.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = choice.iconUri,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Fit,
                                                            modifier = Modifier.size(28.dp).padding(end = 12.dp)
                                                        )
                                                    }

                                                    Text(
                                                        text = choice.choiceText,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // --- DEV CONSOLE / VARIABLE INSPECTOR ---
                                Column(modifier = Modifier.padding(vertical = 24.dp)) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { isInspectorExpanded = !isInspectorExpanded },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Terminal, contentDescription = "Debug", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Developer Console", fontWeight = FontWeight.Bold)
                                                }
                                                Icon(if (isInspectorExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                                            }

                                            AnimatedVisibility(visible = isInspectorExpanded) {
                                                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                                    Divider(modifier = Modifier.padding(bottom = 12.dp))

                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Button(onClick = { showSaveDialog = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Save") }
                                                        Spacer(Modifier.width(8.dp))
                                                        Button(onClick = { showLoadDialog = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Load") }
                                                    }
                                                    Spacer(Modifier.height(8.dp))

                                                    var jumpInput by remember { mutableStateOf("") }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        OutlinedTextField(
                                                            value = jumpInput, onValueChange = { jumpInput = it },
                                                            label = { Text("Jump to Node ID") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.weight(1f).height(56.dp), singleLine = true
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Button(onClick = { viewModel.jumpToNode(jumpInput.toIntOrNull() ?: 0); jumpInput = "" }, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(8.dp)) { Text("Jump") }
                                                    }

                                                    Spacer(Modifier.height(16.dp))
                                                    Text("Live Variables Editor:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                                    Spacer(Modifier.height(8.dp))

                                                    if (viewModel.variablesState.isEmpty()) {
                                                        Text("No variables created in this project.", fontSize = 12.sp)
                                                    } else {
                                                        viewModel.variablesState.forEach { (key, value) ->
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                                                OutlinedTextField(
                                                                    value = value,
                                                                    onValueChange = { viewModel.updateVariable(key, it) },
                                                                    modifier = Modifier.width(120.dp).height(48.dp),
                                                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.End),
                                                                    singleLine = true
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
            }
        }
    }

    if (showSaveDialog) {
        var saveName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Create Save State", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = saveName, onValueChange = { saveName = it },
                    label = { Text("Save File Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { Button(onClick = { viewModel.saveProgress(saveName); showSaveDialog = false }) { Text("Save Progress") } },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
        )
    }

    if (showLoadDialog) {
        val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text("Load Game", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (viewModel.savedGames.isEmpty()) {
                        item { Text("No saved games found.", modifier = Modifier.padding(16.dp)) }
                    }
                    items(viewModel.savedGames) { save ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.loadProgress(save.saveId); showLoadDialog = false },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(save.saveName, fontWeight = FontWeight.Bold)
                                Text("Saved: ${formatter.format(Date(save.lastSavedAt))}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLoadDialog = false }) { Text("Close") } }
        )
    }
}

// Fungsi Helper untuk memutar suara
fun playChoiceSFX(context: Context, uriStr: String?) {
    if (uriStr.isNullOrBlank()) return
    try {
        val player = MediaPlayer.create(context, Uri.parse(uriStr))
        player.setOnCompletionListener { it.release() }
        player.start()
    } catch (e: Exception) { e.printStackTrace() }
}

fun parseMarkdownForPlaytest(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }; i = end + 2 } else { append(text[i]); i++ }
                }
                text.startsWith("*", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && !text.startsWith("**", i)) { withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }; i = end + 1 } else { append(text[i]); i++ }
                }
                else -> { append(text[i]); i++ }
            }
        }
    }
}
package com.storymaker.arcweaver.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storymaker.arcweaver.data.repository.AppDatabase
import com.storymaker.arcweaver.data.repository.VariableRepository
import com.storymaker.arcweaver.viewmodel.NodeViewModel
import com.storymaker.arcweaver.viewmodel.VariableViewModel
import com.storymaker.arcweaver.viewmodel.VariableViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeEditorScreen(
    viewModel: NodeViewModel,
    projectId: Int,
    nodeId: Int?,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Inisialisasi VariableViewModel untuk mengambil daftar variabel proyek
    val database = AppDatabase.getDatabase(context)
    val varViewModel: VariableViewModel = viewModel(
        factory = VariableViewModelFactory(VariableRepository(database.variableDao()))
    )
    val projectVariables by varViewModel.getVariables(projectId).collectAsState()

    // Launcher untuk mengambil gambar dari storage lokal HP
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { e.printStackTrace() }
            viewModel.characterImageUri = it.toString()
        }
    }

    LaunchedEffect(nodeId) {
        if (nodeId != null && nodeId != 0) viewModel.loadNodeForEdit(nodeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (nodeId == null || nodeId == 0) "New Story Node" else "Edit Node #$nodeId", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = {
                    Button(
                        onClick = { viewModel.saveStoryNode(projectId, nodeId) { onBack() } },
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { paddingValues ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // --- KARTU 1: NARRATIVE CONTENT ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Narrative Content", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = viewModel.characterName, onValueChange = { viewModel.characterName = it },
                                label = { Text("Character Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = viewModel.dialogueText, onValueChange = { viewModel.dialogueText = it },
                                label = { Text("Dialogue / Story Text") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp)
                            )

                            if (viewModel.dialogueText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(8.dp)) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                        Text("Live Preview:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = parseMarkdownBasic(viewModel.dialogueText), color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = viewModel.characterImageUri, onValueChange = { viewModel.characterImageUri = it },
                                label = { Text("Image URI / Path") }, leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = "Pick Local Image", tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // --- HEADER KARTU CHOICES ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Story Flow & Logic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FilledTonalButton(onClick = { viewModel.addEmptyChoice() }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Choice")
                        }
                    }
                }

                // --- KONDISI JIKA TIDAK ADA PILIHAN (LINEAR STORY) ---
                if (viewModel.choicesList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Linear Story (No Choices)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Because there are no choices, the player will automatically proceed to the target node below.", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = viewModel.nextNodeId, onValueChange = { viewModel.nextNodeId = it },
                                    label = { Text("Next Node ID (Destination)") }, leadingIcon = { Icon(Icons.Default.Adjust, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                                    singleLine = true, shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                // --- KARTU CHOICES DENGAN DROPDOWN VARIABEL ---
                itemsIndexed(viewModel.choicesList) { index, choice ->
                    var showConditionMenu by remember { mutableStateOf(false) }
                    var showActionMenu by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Teks Pilihan & Hapus
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = choice.choiceText, onValueChange = { viewModel.updateChoiceText(index, it) },
                                    label = { Text("Choice Text") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.removeChoice(index) },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                                ) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer) }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Target Node
                            OutlinedTextField(
                                value = choice.targetNodeId?.toString() ?: "", onValueChange = { viewModel.updateChoiceTarget(index, it) },
                                label = { Text("Target Node ID") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Kolom Condition (Dengan Dropdown Menu)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = choice.requiredCondition ?: "", onValueChange = { viewModel.updateChoiceCondition(index, it) },
                                    label = { Text("Condition (IF)") }, leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp)) },
                                    trailingIcon = { IconButton(onClick = { showConditionMenu = true }) { Icon(Icons.Default.List, null) } },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)
                                )
                                DropdownMenu(expanded = showConditionMenu, onDismissRequest = { showConditionMenu = false }) {
                                    projectVariables.forEach { variable ->
                                        DropdownMenuItem(
                                            text = { Text(variable.name) },
                                            onClick = {
                                                val existing = choice.requiredCondition ?: ""
                                                viewModel.updateChoiceCondition(index, "$existing ${variable.name} == ")
                                                showConditionMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Kolom Action (Dengan Dropdown Menu)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = choice.effect ?: "", onValueChange = { viewModel.updateChoiceEffect(index, it) },
                                    label = { Text("Variable Action (EFFECT)") }, leadingIcon = { Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp)) },
                                    trailingIcon = { IconButton(onClick = { showActionMenu = true }) { Icon(Icons.Default.List, null) } },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("e.g. gold + 10") }
                                )
                                DropdownMenu(expanded = showActionMenu, onDismissRequest = { showActionMenu = false }) {
                                    projectVariables.forEach { variable ->
                                        DropdownMenuItem(
                                            text = { Text(variable.name) },
                                            onClick = {
                                                val existing = choice.effect ?: ""
                                                val operator = if (variable.type == "Boolean") "= true" else "+ 1"
                                                viewModel.updateChoiceEffect(index, "$existing ${variable.name} $operator")
                                                showActionMenu = false
                                            }
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

// Helper: Basic Markdown Parser for Live Preview
fun parseMarkdownBasic(text: String): AnnotatedString {
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
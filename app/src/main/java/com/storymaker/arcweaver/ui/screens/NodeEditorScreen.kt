package com.storymaker.arcweaver.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.parseRichText
import com.storymaker.arcweaver.viewmodel.ChoiceDraft
import com.storymaker.arcweaver.viewmodel.NodeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeEditorScreen(
    viewModel: NodeViewModel,
    nodeIdToEdit: Int?,
    onNavigateBack: () -> Unit,
    onPlaytest: () -> Unit
) {
    val existingNodes by viewModel.allNodes.collectAsState()

    var characterName by remember { mutableStateOf("") }
    var dialogueText by remember { mutableStateOf("") }
    var choices by remember { mutableStateOf(listOf(ChoiceDraft())) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- FITUR LOAD DATA (AUTO-FILL) UNTUK MODE EDIT ---
    LaunchedEffect(nodeIdToEdit) {
        if (nodeIdToEdit != null) {
            val data = viewModel.loadNodeForEdit(nodeIdToEdit)
            if (data != null) {
                characterName = data.first.characterName
                dialogueText = data.first.dialogueText
                selectedImageUri = data.first.characterImageUri?.let { Uri.parse(it) }
                if (data.second.isNotEmpty()) {
                    choices = data.second
                } else {
                    choices = emptyList() // Jika end node
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Ganti judul dinamis sesuai mode
                title = { Text(if (nodeIdToEdit == null) "Create Story Node" else "Edit Story Node") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // --- PHOTO PICKER ---
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = "Add Photo", modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Character Sprite", style = MaterialTheme.typography.titleMedium)
                    if (selectedImageUri != null) {
                        TextButton(onClick = { selectedImageUri = null }, contentPadding = PaddingValues(0.dp)) {
                            Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // --- INPUT DASAR ---
            OutlinedTextField(
                value = characterName,
                onValueChange = { characterName = it },
                label = { Text("Character Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dialogueText,
                onValueChange = { dialogueText = it },
                label = { Text("Dialogue (Use **bold** or *italic*)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // --- LIVE PREVIEW TEXT FORMATTING ---
            if (dialogueText.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Live Preview:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        // Memanggil fungsi parseRichText kita!
                        Text(
                            text = parseRichText(dialogueText),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Text("Choices (Branching)", style = MaterialTheme.typography.titleMedium)

            // --- DAFTAR PILIHAN ---
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsIndexed(choices) { index, choiceDraft ->
                    var showAdvanced by remember { mutableStateOf(false) }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = choiceDraft.text,
                                    onValueChange = { newValue ->
                                        val newChoices = choices.toMutableList()
                                        newChoices[index] = choiceDraft.copy(text = newValue)
                                        choices = newChoices
                                    },
                                    label = { Text("Choice Text") },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { showAdvanced = !showAdvanced }) { Icon(Icons.Default.Build, "Advanced") }
                                IconButton(onClick = {
                                    val newChoices = choices.toMutableList()
                                    newChoices.removeAt(index)
                                    choices = newChoices
                                }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            NodeDropdownSelector(
                                nodes = existingNodes.filter { it.nodeId != nodeIdToEdit }, // Jangan bisa melink ke diri sendiri
                                selectedNodeId = choiceDraft.targetNodeId,
                                onNodeSelected = { targetId ->
                                    val newChoices = choices.toMutableList()
                                    newChoices[index] = choiceDraft.copy(targetNodeId = targetId)
                                    choices = newChoices
                                }
                            )

                            if (showAdvanced) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = choiceDraft.condition,
                                    onValueChange = { newValue ->
                                        val newChoices = choices.toMutableList()
                                        newChoices[index] = choiceDraft.copy(condition = newValue)
                                        choices = newChoices
                                    },
                                    label = { Text("Required Logic (e.g. hasKey == true)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { choices = choices + ChoiceDraft() }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Choice")
                    }
                }
            }

//            tombol playtest
            Button(
                onClick = { onPlaytest() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("▶ Playtest Story")
            }

            // --- TOMBOL SAVE ---
            Button(
                onClick = {
                    if (characterName.isNotBlank() && dialogueText.isNotBlank()) {
                        val validChoices = choices.filter { it.text.isNotBlank() }
                        // Panggil save dengan membawa ID (jika ada)
                        viewModel.saveStoryNode(nodeIdToEdit, characterName, dialogueText, selectedImageUri?.toString(), validChoices)
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (nodeIdToEdit == null) "Save New Node" else "Update Node")
            }
        }
    }
}

// Dropdown Selector (Tetap sama seperti sebelumnya)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDropdownSelector(nodes: List<StoryNodeEntity>, selectedNodeId: Int?, onNodeSelected: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedNodeText = if (selectedNodeId == null) "Unlinked (End of Story)" else nodes.find { it.nodeId == selectedNodeId }?.let { "Node #${it.nodeId} (${it.characterName})" } ?: "Unknown Node"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedNodeText, onValueChange = {}, readOnly = true,
            label = { Text("Connect to (Target Node)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Unlinked (End of Story)", fontWeight = FontWeight.Bold) }, onClick = { onNodeSelected(null); expanded = false })
            Divider()
            nodes.forEach { node ->
                DropdownMenuItem(text = { Text("Node #${node.nodeId}: ${node.dialogueText.take(20)}...") }, onClick = { onNodeSelected(node.nodeId); expanded = false })
            }
        }
    }
}
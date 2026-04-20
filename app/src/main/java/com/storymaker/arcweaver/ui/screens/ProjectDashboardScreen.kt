package com.storymaker.arcweaver.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.data.entity.VariableEntity
import com.storymaker.arcweaver.data.repository.AppDatabase
import com.storymaker.arcweaver.data.repository.ProjectRepository
import com.storymaker.arcweaver.data.repository.StoryRepository
import com.storymaker.arcweaver.data.repository.VariableRepository
import com.storymaker.arcweaver.viewmodel.ProjectDashboardViewModel
import com.storymaker.arcweaver.viewmodel.ProjectDashboardViewModelFactory
import com.storymaker.arcweaver.viewmodel.VariableViewModel
import com.storymaker.arcweaver.viewmodel.VariableViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDashboardScreen(
    projectId: Int,
    onNavigateToEditor: (Int?) -> Unit,
    onNavigateToVisual: () -> Unit,
    onNavigateToPlaytest: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)

    val variableRepo = VariableRepository(database.variableDao())

    val viewModel: ProjectDashboardViewModel = viewModel(
        factory = ProjectDashboardViewModelFactory(
            projectId = projectId,
            projectRepository = ProjectRepository(database.projectDao()),
            storyRepository = StoryRepository(database.storyDao()),
            variableRepository = variableRepo
        )
    )

    val varViewModel: VariableViewModel = viewModel(
        factory = VariableViewModelFactory(variableRepo)
    )

    val project by viewModel.project.collectAsState()
    val nodes by viewModel.nodes.collectAsState()
    val variables by varViewModel.getVariables(projectId).collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Story Nodes", "Variables", "Settings")
    var searchQuery by remember { mutableStateOf("") }

    // Dialog States
    var showAddVarDialog by remember { mutableStateOf(false) }
    var variableToEdit by remember { mutableStateOf<VariableEntity?>(null) }

    // --- LAUNCHER EXPORT & IMPORT JSON ---
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportProjectToJson(context, it, variables) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importProjectFromJson(context, it) }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(project?.title ?: "Loading...", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToVisual) { Icon(Icons.Default.AccountTree, contentDescription = "Visual Map") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
                TabRow(selectedTabIndex = selectedTabIndex, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 0 || selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTabIndex == 0) onNavigateToEditor(null)
                        if (selectedTabIndex == 1) showAddVarDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTabIndex) {
                0 -> { // TAB 1: STORY NODES
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search nodes...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }

                        item {
                            Button(
                                onClick = onNavigateToPlaytest,
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Playtest Simulator", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (nodes.isEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Text("No story nodes yet.", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        } else {
                            val filteredNodes = nodes.filter {
                                it.characterName.contains(searchQuery, ignoreCase = true) ||
                                        it.dialogueText.contains(searchQuery, ignoreCase = true)
                            }
                            items(filteredNodes, key = { it.nodeId }) { node ->
                                ModernNodeCard(
                                    node = node,
                                    onClick = { onNavigateToEditor(node.nodeId) },
                                    onDelete = { viewModel.deleteNode(node) }
                                )
                            }
                        }
                    }
                }
                1 -> { // TAB 2: VARIABLES
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        items(variables) { variable ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(variable.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Type: ${variable.type} | Initial: ${variable.initialValue}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { variableToEdit = variable }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { varViewModel.deleteVariable(variable) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // TAB 3: SETTINGS
                    var editTitle by remember(project) { mutableStateOf(project?.title ?: "") }
                    var editDesc by remember(project) { mutableStateOf(project?.description ?: "") }

                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Project Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Project Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = editDesc,
                            onValueChange = { editDesc = it },
                            label = { Text("Project Description") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.updateProjectDetails(editTitle, editDesc) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }

                        // --- EXPORT / IMPORT SECTION ---
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text("Backup & Share", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {

                                    val safeTitle = project?.title?.replace(" ", "_") ?: "ArcWeaver"
                                    exportLauncher.launch("${safeTitle}_Backup.json")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export JSON")
                            }

                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json")) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Import JSON")
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { viewModel.deleteCurrentProject { onBack() } },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Entire Project", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddVarDialog) {
        AddVariableDialog(
            onDismiss = { showAddVarDialog = false },
            onConfirm = { name, type, value ->
                varViewModel.addVariable(projectId, name, type, value)
                showAddVarDialog = false
            }
        )
    }

    variableToEdit?.let { variable ->
        EditVariableDialog(
            variable = variable,
            onDismiss = { variableToEdit = null },
            onConfirm = { updatedVar ->
                varViewModel.updateVariable(updatedVar)
                variableToEdit = null
            }
        )
    }
}

// [KOMPONEN ModernNodeCard, AddVariableDialog, EditVariableDialog] ...
@Composable
fun ModernNodeCard(node: StoryNodeEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "#${node.nodeId}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Text(text = node.characterName.ifBlank { "Narrator" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = node.dialogueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVariableDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Boolean") }
    var initialValue by remember { mutableStateOf("false") }
    var expanded by remember { mutableStateOf(false) }
    val types = listOf("Boolean", "Integer", "String")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Variable", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Variable Name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = type, onValueChange = {}, readOnly = true, label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        types.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    type = selectionOption; expanded = false
                                    initialValue = when (type) { "Boolean" -> "false"; "Integer" -> "0"; else -> "" }
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = initialValue, onValueChange = { initialValue = it }, label = { Text("Initial Value") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name.replace(" ", ""), type, initialValue) }, enabled = name.isNotBlank() && initialValue.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVariableDialog(variable: VariableEntity, onDismiss: () -> Unit, onConfirm: (VariableEntity) -> Unit) {
    var name by remember { mutableStateOf(variable.name) }
    var initialValue by remember { mutableStateOf(variable.initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Variable", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Variable Name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = variable.type, onValueChange = {}, readOnly = true, label = { Text("Type (Cannot be changed)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialValue, onValueChange = { initialValue = it }, label = { Text("Initial Value") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(variable.copy(name = name.replace(" ", ""), initialValue = initialValue)) }, enabled = name.isNotBlank() && initialValue.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
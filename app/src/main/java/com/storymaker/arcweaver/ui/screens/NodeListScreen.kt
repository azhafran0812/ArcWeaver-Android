package com.storymaker.arcweaver.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.storymaker.arcweaver.data.entity.StoryNodeEntity
import com.storymaker.arcweaver.viewmodel.ProjectDashboardViewModel // Menggunakan ViewModel Dashboard karena logic-nya sama

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeListScreen(
    viewModel: ProjectDashboardViewModel,
    projectId: Int,
    onNavigateToEditor: (Int?) -> Unit,
    onBack: () -> Unit
) {

    val nodes by viewModel.nodes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Story Nodes List") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToEditor(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Node")
            }
        }
    ) { paddingValues ->
        if (nodes.isEmpty()) {
            Text(
                text = "Belum ada adegan. Tekan + untuk membuat.",
                modifier = Modifier.padding(paddingValues).padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(nodes, key = { it.nodeId }) { node ->
                    OldNodeListItem(
                        node = node,
                        onClick = { onNavigateToEditor(node.nodeId) }
                    )
                }
            }
        }
    }
}

@Composable
fun OldNodeListItem(node: StoryNodeEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Node #${node.nodeId} - ${node.characterName}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = node.dialogueText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
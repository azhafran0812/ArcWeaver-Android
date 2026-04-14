package com.storymaker.arcweaver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storymaker.arcweaver.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.OutlinedTextFieldDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProjectClick: (Int) -> Unit
) {
    val projects by viewModel.projectList.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Search Bar (Sesuai Desain)
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("Search your projects...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // 2. Create New Project Card (Sesuai Desain)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AddBox,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Create New Project", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Start a new interactive story branch", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // 3. Recent Projects Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Projects", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                TextButton(onClick = { /* Navigasi ke tab Projects */ }) {
                    Text("See All")
                }
            }
        }

        // 4. Grid Recent Projects (2 Kolom)
        item {
            // Kita gunakan Box/Column manual karena LazyColumn tidak bisa menampung LazyGrid secara langsung
            val recentProjects = projects.take(4) // Tampilkan 4 terbaru saja
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                recentProjects.chunked(2).forEach { rowProjects ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowProjects.forEach { project ->
                            ProjectCard(
                                project = project,
                                onClick = { onProjectClick(project.projectId) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowProjects.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showDialog) {
        NewProjectDialog(
            onDismiss = { showDialog = false },
            onConfirm = { title, desc ->
                viewModel.createNewProject(title, desc)
                showDialog = false
            }
        )
    }
}

// --- KOMPONEN PROJECT CARD ---
@Composable
fun ProjectCard(
    project: com.storymaker.arcweaver.data.entity.ProjectEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Membuat proporsi card selalu persegi
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (project.description.isNotBlank()) {
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "${project.nodeCount} Nodes",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Updated: ${formatTimestamp(project.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- KOMPONEN DIALOG NEW PROJECT ---
@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Project", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onConfirm(title, description)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- FUNGSI UTILITAS WAKTU ---
fun formatTimestamp(timeInMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(timeInMillis))
}
package com.storymaker.arcweaver.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("arcweaver_prefs", Context.MODE_PRIVATE)

    // Membaca status Dark Mode dari SharedPreferences
    var isDarkMode by remember {
        mutableStateOf(sharedPreferences.getBoolean("dark_mode", false))
    }

    // State untuk memunculkan Dialog Tutorial
    var showTutorialDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Kategori 1: Appearance ---
            SettingsCategoryTitle(title = "Appearance")
            SettingsSwitchItem(
                icon = Icons.Default.Palette,
                title = "Dark Mode",
                subtitle = "Toggle dark theme for the app (Requires restart)",
                checked = isDarkMode,
                onCheckedChange = { checked ->
                    isDarkMode = checked
                    // Simpan preferensi ke penyimpanan lokal HP
                    sharedPreferences.edit().putBoolean("dark_mode", checked).apply()
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Kategori 2: Help & Guide ---
            SettingsCategoryTitle(title = "Help & Guide")
            SettingsActionItem(
                icon = Icons.Default.HelpOutline,
                title = "How to use ArcWeaver",
                subtitle = "Read the beginner's guide to create your first story",
                onClick = { showTutorialDialog = true }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Kategori 3: About ---
            SettingsCategoryTitle(title = "About")
            SettingsActionItem(
                icon = Icons.Default.Info,
                title = "ArcWeaver Version",
                subtitle = "v2.0.0 (Beta)",
                onClick = { }
            )
            SettingsActionItem(
                icon = Icons.Default.Code,
                title = "Developers",
                subtitle = "Aditya Ardian Syah & Abyan Zhafran",
                onClick = { }
            )
        }
    }

    // --- POP-UP DIALOG TUTORIAL ---
    if (showTutorialDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog = false },
            title = { Text("How to Use ArcWeaver", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TutorialStep("1. Create a Project", "Start by creating a new project in the Home Screen.")
                    TutorialStep("2. Setup Variables", "Go to the 'Variables' tab in your project to set up stats like HP, Gold, or specific items.")
                    TutorialStep("3. Story Nodes", "Create a 'Story Node' for each scene. You can add custom backgrounds, BGM, and character portraits.")
                    TutorialStep("4. Choices & Logic", "Add choices to your node. Use 'Condition (IF)' to lock choices (e.g. gold >= 50) and 'Action' to change stats (e.g. HP - 10). You can use commas for multiple logic.")
                    TutorialStep("5. Visual Map", "Use the Visual Map (tree icon on top right) to drag, drop, and view how your story nodes connect.")
                    TutorialStep("6. Playtest", "Hit the Playtest Simulator button to play your game, test variables live, and debug your logic!")
                }
            },
            confirmButton = {
                Button(onClick = { showTutorialDialog = false }) {
                    Text("Got it!")
                }
            }
        )
    }
}

// Komponen bantuan untuk merender teks tutorial
@Composable
fun TutorialStep(title: String, desc: String) {
    Column {
        Text(text = title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsCategoryTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
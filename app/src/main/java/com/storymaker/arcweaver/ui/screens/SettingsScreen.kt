package com.storymaker.arcweaver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    // State sementara untuk Switcher (Nantinya dihubungkan ke DataStore/ViewModel)
    var isDarkMode by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        ) {
            // --- Kategori 1: Appearance ---
            SettingsCategoryTitle(title = "Appearance")
            SettingsSwitchItem(
                icon = Icons.Default.Palette,
                title = "Dark Mode",
                subtitle = "Toggle dark theme for the app",
                checked = isDarkMode,
                onCheckedChange = { isDarkMode = it }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Kategori 2: Account ---
            SettingsCategoryTitle(title = "Account")
            SettingsActionItem(
                icon = Icons.Default.AccountCircle,
                title = "Account Configuration",
                subtitle = "Manage your profile and author details",
                onClick = { /* TODO: Aksi saat diklik */ }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Kategori 3: Notifications ---
            SettingsCategoryTitle(title = "Notifications")
            SettingsSwitchItem(
                icon = Icons.Default.Notifications,
                title = "Push Notifications",
                subtitle = "Receive updates and playtest reminders",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Kategori 4: About ---
            SettingsCategoryTitle(title = "About")
            SettingsActionItem(
                icon = Icons.Default.Info,
                title = "ArcWeaver Version",
                subtitle = "v1.0.0 (Beta)",
                onClick = { /* Tidak ada aksi, hanya info */ }
            )
        }
    }
}

// Komponen bantuan (Helper) untuk membuat Judul Kategori
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

// Komponen bantuan untuk baris pengaturan yang memiliki Switch (Tombol On/Off)
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
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
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

// Komponen bantuan untuk baris pengaturan yang bisa diklik (seperti tombol biasa)
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
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
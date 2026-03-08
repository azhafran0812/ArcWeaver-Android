package com.storymaker.arcweaver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Judul Aplikasi
        Text(
            text = "ArcWeaver",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Interactive Narrative Builder",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Tombol 1: Buat Cerita Baru (Mengarah ke Editor)
        Button(
            onClick = {
                // Perintah untuk pindah ke halaman editor
                navController.navigate("editor_screen")
            },
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
        ) {
            Text("Buat Cerita Baru", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tombol 2: Daftar Project (Dummy/Belum Aktif untuk sesi 15)
        OutlinedButton(
            onClick = {
                // Nanti bisa diarahkan ke halaman list project
            },
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
        ) {
            Text("Proyek Saya (Coming Soon)", fontSize = 18.sp)
        }
    }
}
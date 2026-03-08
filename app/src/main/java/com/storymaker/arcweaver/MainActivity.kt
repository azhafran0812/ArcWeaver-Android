package com.storymaker.arcweaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.storymaker.arcweaver.ui.screens.HomeScreen
import com.storymaker.arcweaver.ui.screens.NodeEditorScreen
import com.storymaker.arcweaver.ui.theme.ArcWeaverTheme // Sesuaikan nama theme-nya

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArcWeaverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 1. Buat Controller Navigasi
                    val navController = rememberNavController()

                    // 2. Atur Rute. Sekarang startDestination-nya adalah "home_screen"
                    NavHost(navController = navController, startDestination = "home_screen") {

                        // Rute ke Halaman Utama
                        composable("home_screen") {
                            HomeScreen(navController = navController)
                        }

                        // Rute ke Halaman Editor
                        composable("editor_screen") {
                            NodeEditorScreen()
                        }
                    }
                }
            }
        }
    }
}
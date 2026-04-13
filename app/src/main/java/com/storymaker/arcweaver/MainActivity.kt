package com.storymaker.arcweaver

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.storymaker.arcweaver.data.repository.AppDatabase
import com.storymaker.arcweaver.data.repository.StoryRepository
import com.storymaker.arcweaver.domain.engine.PlaytestEngine
import com.storymaker.arcweaver.domain.evaluator.ConditionalEvaluator
import com.storymaker.arcweaver.domain.parser.VariableParser
import com.storymaker.arcweaver.domain.repository.PlaytestRepository
import com.storymaker.arcweaver.ui.screens.NodeEditorScreen
import com.storymaker.arcweaver.ui.screens.NodeListScreen
import com.storymaker.arcweaver.ui.screens.PlaytestScreen
import com.storymaker.arcweaver.ui.theme.ArcWeaverTheme
import com.storymaker.arcweaver.viewmodel.NodeViewModel
import com.storymaker.arcweaver.viewmodel.NodeViewModelFactory
import com.storymaker.arcweaver.viewmodel.PlaytestViewModel
import com.storymaker.arcweaver.viewmodel.PlaytestViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val repository = StoryRepository(database.storyDao())
        val factory = NodeViewModelFactory(repository)
        val playtestRepository = PlaytestRepository(repository)
        val playtestFactory = PlaytestViewModelFactory(playtestRepository)

        setContent {
            ArcWeaverTheme {
                val nodeViewModel: NodeViewModel = viewModel(factory = factory)
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "dashboard") {

                    composable("dashboard") {
                        NodeListScreen(
                            viewModel = nodeViewModel,
                            onNavigateToEditor = {
                                navController.navigate("editor") // Rute Buat Baru
                            },
                            // Tambahan aksi untuk tombol edit di Dashboard
                            onNavigateToEditNode = { nodeId ->
                                navController.navigate("editor?nodeId=$nodeId") // Rute Edit
                            }
                        )
                    }


                    // Rute Editor untuk menerima argumen nodeId
                    composable(
                        route = "editor?nodeId={nodeId}",
                        arguments = listOf(navArgument("nodeId") {
                            type = NavType.StringType
                            nullable = true
                        })
                    ) { backStackEntry ->
                        // Ambil ID dari rute (jika ada)
                        val nodeIdStr = backStackEntry.arguments?.getString("nodeId")
                        val nodeId = nodeIdStr?.toIntOrNull()

                        NodeEditorScreen(
                            viewModel = nodeViewModel,
                            nodeIdToEdit = nodeId,
                            onNavigateBack = { navController.popBackStack() },
                            onPlaytest = {
                                navController.navigate("playtest")
                            }
                        )
                    }

                    composable("playtest") {
                        val playtestViewModel: PlaytestViewModel = viewModel(factory = playtestFactory)

                        PlaytestScreen(
                            viewModel = playtestViewModel,
                            onExit = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
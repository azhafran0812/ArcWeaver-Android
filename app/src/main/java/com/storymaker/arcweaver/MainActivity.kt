package com.storymaker.arcweaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.storymaker.arcweaver.data.repository.AppDatabase
import com.storymaker.arcweaver.data.repository.ProjectRepository
import com.storymaker.arcweaver.data.repository.StoryRepository
import com.storymaker.arcweaver.ui.screens.*
import com.storymaker.arcweaver.ui.theme.ArcWeaverTheme
import com.storymaker.arcweaver.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi Database dan Repository Central
        val database = AppDatabase.getDatabase(this)
        val projectRepo = ProjectRepository(database.projectDao())
        val storyRepo = StoryRepository(database.storyDao())
        val variableRepo = com.storymaker.arcweaver.data.repository.VariableRepository(database.variableDao())
        val playtestRepo = com.storymaker.arcweaver.domain.repository.PlaytestRepository(database.playtestDao(), database.storyDao())

        setContent {
            ArcWeaverTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Menentukan layar mana saja yang akan menampilkan Bottom Navigation Bar
                val showBottomBar = currentDestination?.route in listOf(
                    BottomNavItem.Home.route,
                    BottomNavItem.Projects.route,
                    BottomNavItem.Settings.route
                )

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                val items = listOf(
                                    BottomNavItem.Home,
                                    BottomNavItem.Projects,
                                    BottomNavItem.Settings
                                )
                                items.forEach { item ->
                                    NavigationBarItem(
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) },
                                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                // Menghindari penumpukan antrean navigasi
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // 1. Splash Screen
                        composable("splash") {
                            SplashScreen(onTimeout = {
                                navController.navigate(BottomNavItem.Home.route) {
                                    popUpTo("splash") { inclusive = true }
                                }
                            })
                        }

                        // 2. Tab Utama: Home
                        composable(BottomNavItem.Home.route) {
                            val homeViewModel: HomeViewModel = viewModel(
                                factory = HomeViewModelFactory(projectRepo)
                            )
                            HomeScreen(
                                viewModel = homeViewModel,
                                onProjectClick = { id -> navController.navigate("project_dashboard/$id") }
                            )
                        }

                        // 3. Tab Utama: All Projects
                        composable(BottomNavItem.Projects.route) {
                            val homeViewModel: HomeViewModel = viewModel(
                                factory = HomeViewModelFactory(projectRepo)
                            )
                            ProjectListTabScreen(
                                viewModel = homeViewModel,
                                onProjectClick = { id -> navController.navigate("project_dashboard/$id") }
                            )
                        }

                        // 4. Tab Utama: Settings
                        composable(BottomNavItem.Settings.route) {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }

                        // 5. Project Dashboard (Detail Proyek)
                        composable(
                            route = "project_dashboard/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getInt("projectId") ?: 0
                            ProjectDashboardScreen(
                                projectId = projectId,
                                onNavigateToEditor = { nodeId ->
                                    navController.navigate("editor/$projectId/${nodeId ?: 0}")
                                },
                                onNavigateToVisual = { navController.navigate("visual_map/$projectId") },
                                onNavigateToPlaytest = { navController.navigate("playtest/$projectId") },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 6. Storynode Editor
                        composable(
                            route = "editor/{projectId}/{nodeId}",
                            arguments = listOf(
                                navArgument("projectId") { type = NavType.IntType },
                                navArgument("nodeId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getInt("projectId") ?: 0
                            val nodeId = backStackEntry.arguments?.getInt("nodeId") ?: 0

                            val nodeViewModel: NodeViewModel = viewModel(
                                // TAMBAHKAN projectRepo DI SINI
                                factory = NodeViewModelFactory(storyRepo, projectRepo)
                            )
                            NodeEditorScreen(
                                viewModel = nodeViewModel,
                                projectId = projectId,
                                nodeId = if (nodeId == 0) null else nodeId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 7. Playtest Simulator
                        composable(
                            route = "playtest/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getInt("projectId") ?: 0

                            // UBAH FACTORY INI UNTUK MEMASUKKAN playtestRepo:
                            val playtestViewModel: PlaytestViewModel = viewModel(
                                factory = PlaytestViewModelFactory(projectId, storyRepo, variableRepo, playtestRepo)
                            )

                            PlaytestScreen(
                                viewModel = playtestViewModel,
                                projectId = projectId,
                                onExit = { navController.popBackStack() }
                            )
                        }

                        // 8. Visual Diagram Map
                        composable(
                            route = "visual_map/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getInt("projectId") ?: 0
                            VisualDiagramScreen(
                                projectId = projectId,
                                onNavigateToEditor = { nodeId ->
                                    navController.navigate("editor/$projectId/${nodeId ?: 0}")
                                }, // <--- TAMBAHKAN BARIS INI
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Definisi Menu Bottom Navigation
 */
sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home_tab", Icons.Default.Home, "Home")
    object Projects : BottomNavItem("projects_tab", Icons.Default.Folder, "Projects")
    object Settings : BottomNavItem("settings_tab", Icons.Default.Settings, "Settings")
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Timer untuk pindah layar otomatis setelah 2 detik
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onTimeout()
    }

    // Tampilan UI Splash Screen Sederhana
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ArcWeaver",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Interactive Narrative Builder",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
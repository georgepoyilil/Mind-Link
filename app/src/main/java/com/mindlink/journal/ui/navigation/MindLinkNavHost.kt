package com.mindlink.journal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mindlink.journal.di.ServiceLocator
import com.mindlink.journal.ui.detail.NoteDetailScreen
import com.mindlink.journal.ui.detail.NoteDetailViewModel
import com.mindlink.journal.ui.editor.NoteEditorScreen
import com.mindlink.journal.ui.editor.NoteEditorViewModel
import com.mindlink.journal.ui.graph.ThoughtGraphScreen
import com.mindlink.journal.ui.graph.ThoughtGraphViewModel
import com.mindlink.journal.ui.notes.NotesListScreen
import com.mindlink.journal.ui.notes.NotesViewModel
import com.mindlink.journal.ui.secondbrain.SecondBrainScreen
import com.mindlink.journal.ui.secondbrain.SecondBrainViewModel
import com.mindlink.journal.ui.settings.SettingsScreen
import com.mindlink.journal.ui.settings.SettingsViewModel

@Composable
fun MindLinkNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val serviceLocator = ServiceLocator.getInstance(context)
    val repository = serviceLocator.noteRepository

    NavHost(
        navController = navController,
        startDestination = Screen.NotesList.route
    ) {
        // Notes List (Home)
        composable(Screen.NotesList.route) {
            val notesViewModel: NotesViewModel = viewModel(
                factory = NotesViewModel.Factory(repository)
            )
            NotesListScreen(
                viewModel = notesViewModel,
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                },
                onCreateNoteClick = { mode ->
                    navController.navigate(Screen.NoteEditor.createRoute(null, mode))
                },
                onSecondBrainClick = {
                    navController.navigate(Screen.SecondBrain.route)
                },
                onThoughtGraphClick = {
                    navController.navigate(Screen.ThoughtGraph.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Note Editor (Create / Edit)
        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "text"
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId")?.takeIf { it > 0 }
            val mode = backStackEntry.arguments?.getString("mode") ?: "text"
            val editorViewModel: NoteEditorViewModel = viewModel(
                factory = NoteEditorViewModel.Factory(repository, noteId)
            )
            NoteEditorScreen(
                viewModel = editorViewModel,
                initialMode = mode,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { savedId ->
                    navController.popBackStack()
                    navController.navigate(Screen.NoteDetail.createRoute(savedId))
                }
            )
        }

        // Note Detail View
        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            val detailViewModel: NoteDetailViewModel = viewModel(
                factory = NoteDetailViewModel.Factory(repository, noteId)
            )
            NoteDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(Screen.NoteEditor.createRoute(id))
                },
                onThoughtClick = { relatedId ->
                    navController.navigate(Screen.NoteDetail.createRoute(relatedId))
                }
            )
        }

        // Second Brain Associative Query & Synthesis
        composable(Screen.SecondBrain.route) {
            val secondBrainViewModel: SecondBrainViewModel = viewModel(
                factory = SecondBrainViewModel.Factory(repository)
            )
            SecondBrainScreen(
                viewModel = secondBrainViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                }
            )
        }

        // Synaptic Knowledge Graph Visualizer
        composable(Screen.ThoughtGraph.route) {
            val graphViewModel: ThoughtGraphViewModel = viewModel(
                factory = ThoughtGraphViewModel.Factory(repository)
            )
            ThoughtGraphScreen(
                viewModel = graphViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                }
            )
        }

        // Settings & Engine Configuration
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(repository, serviceLocator.preferences)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

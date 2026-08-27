package com.quickreply.boards.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quickreply.boards.data.local.entity.FolderEntity
import com.quickreply.boards.ui.screens.AddAudioScreen
import com.quickreply.boards.ui.screens.AddContactScreen
import com.quickreply.boards.ui.screens.AddImageScreen
import com.quickreply.boards.ui.screens.AddLinkScreen
import com.quickreply.boards.ui.screens.AddLocationScreen
import com.quickreply.boards.ui.screens.AddPdfScreen
import com.quickreply.boards.ui.screens.AddSequenceScreen
import com.quickreply.boards.ui.screens.AddTextScreen
import com.quickreply.boards.ui.screens.BoardDetailScreen
import com.quickreply.boards.ui.screens.BoardsListScreen
import com.quickreply.boards.ui.screens.KeyboardSetupScreen
import com.quickreply.boards.ui.screens.SettingsScreen
import com.quickreply.boards.ui.theme.QuickReplyTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickReplyTheme {
                val navController = rememberNavController()
                val rootBoards by homeViewModel.rootBoards.collectAsState()

                NavHost(navController = navController, startDestination = "boards_list") {
                    // Pantalla Principal: Mis Boards
                    composable("boards_list") {
                        BoardsListScreen(
                            viewModel = homeViewModel,
                            onOpenBoard = { board ->
                                navController.navigate("board_detail/${board.id}")
                            },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }

                    // Pantalla de Ajustes y Configuración
                    composable("settings") {
                        SettingsScreen(
                            viewModel = homeViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToKeyboardSetup = { navController.navigate("setup") }
                        )
                    }

                    // Detalle del Tablero / Subcarpeta
                    composable(
                        route = "board_detail/{boardId}",
                        arguments = listOf(navArgument("boardId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val boardId = backStackEntry.arguments?.getLong("boardId") ?: 0L
                        val activeBoardState by remember(boardId) { homeViewModel.getBoardByIdFlow(boardId) }.collectAsState(initial = null)
                        val activeBoard = activeBoardState ?: rootBoards.find { it.id == boardId } ?: FolderEntity(id = boardId, name = "Board")

                        BoardDetailScreen(
                            board = activeBoard,
                            viewModel = homeViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onAddText = { bId -> navController.navigate("add_text/$bId") },
                            onAddAudio = { bId -> navController.navigate("add_audio/$bId") },
                            onAddSequence = { bId -> navController.navigate("add_sequence/$bId") },
                            onAddLocation = { bId -> navController.navigate("add_location/$bId") },
                            onAddContact = { bId -> navController.navigate("add_contact/$bId") },
                            onAddLink = { bId -> navController.navigate("add_link/$bId") },
                            onAddPdf = { bId -> navController.navigate("add_pdf/$bId") },
                            onAddImage = { bId -> navController.navigate("add_image/$bId") },
                            onEditText = { reply -> navController.navigate("add_text/${reply.folderId}?replyId=${reply.id}") },
                            onEditAudio = { reply -> navController.navigate("add_audio/${reply.folderId}?replyId=${reply.id}") },
                            onEditSequence = { reply -> navController.navigate("add_sequence/${reply.folderId}?replyId=${reply.id}") },
                            onEditLocation = { reply -> navController.navigate("add_location/${reply.folderId}?replyId=${reply.id}") },
                            onEditContact = { reply -> navController.navigate("add_contact/${reply.folderId}?replyId=${reply.id}") },
                            onEditLink = { reply -> navController.navigate("add_link/${reply.folderId}?replyId=${reply.id}") },
                            onEditPdf = { reply -> navController.navigate("add_pdf/${reply.folderId}?replyId=${reply.id}") },
                            onEditImage = { reply -> navController.navigate("add_image/${reply.folderId}?replyId=${reply.id}") },
                            onNavigateToSubfolder = { subfolder -> navController.navigate("board_detail/${subfolder.id}") }
                        )
                    }

                    // Editor de Texto
                    composable(
                        route = "add_text/{boardId}?replyId={replyId}",
                        arguments = listOf(
                            navArgument("boardId") { type = NavType.LongType },
                            navArgument("replyId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        )
                    ) { backStackEntry ->
                        val boardId = backStackEntry.arguments?.getLong("boardId") ?: 0L
                        val replyId = backStackEntry.arguments?.getLong("replyId") ?: 0L
                        val initialReply by remember(replyId) { homeViewModel.getReplyByIdFlow(replyId) }.collectAsState(initial = null)

                        AddTextScreen(
                            folderId = boardId,
                            initialReply = initialReply,
                            onNavigateBack = { navController.popBackStack() },
                            onSave = { reply -> homeViewModel.saveReply(reply) }
                        )
                    }

                    // Editor de Audio
                    composable(
                        route = "add_audio/{boardId}?replyId={replyId}",
                        arguments = listOf(
                            navArgument("boardId") { type = NavType.LongType },
                            navArgument("replyId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        )
                    ) { backStackEntry ->
                        val boardId = backStackEntry.arguments?.getLong("boardId") ?: 0L
                        val replyId = backStackEntry.arguments?.getLong("replyId") ?: 0L
                        val initialReply by remember(replyId) { homeViewModel.getReplyByIdFlow(replyId) }.collectAsState(initial = null)

                        AddAudioScreen(
                            folderId = boardId,
                            initialReply = initialReply,
                            onNavigateBack = { navController.popBackStack() },
                            onSave = { reply -> homeViewModel.saveReply(reply) }
                        )
                    }

                    // Editor de Secuencia de Ventas
                    composable(
                        route = "add_sequence/{boardId}?replyId={replyId}",
                        arguments = listOf(
                            navArgument("boardId") { type = NavType.LongType },
                            navArgument("replyId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        )
                    ) { backStackEntry ->
                        val boardId = backStackEntry.arguments?.getLong("boardId") ?: 0L
                        val replyId = backStackEntry.arguments?.getLong("replyId") ?: 0L
                        val initialReply by remember(replyId) { homeViewModel.getReplyByIdFlow(replyId) }.collectAsState(initial = null)

                        AddSequenceScreen(
                            folderId = boardId,
                            initialReply = initialReply,
                            onNavigateBack = { navController.popBackStack() },
                            onSave = { reply -> homeViewModel.saveReply(reply) }
                        )
                    }

                    // Editor de Ubicación / Sucursales
                    composable(
                        route = "add_location/{boardId}?replyId={replyId}",
                        arguments = listOf(
                            navArgument("boardId") { type = NavType.LongType },
                            navArgument("replyId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        )
                    ) { backStackEntry ->
                        val boardId = backStackEntry.arguments?.getLong("boardId") ?: 0L
                        val replyId = backStackEntry.arguments?.getLong("replyId") ?: 0L
                        val initialReply by remember(replyId) { homeViewModel.getReplyByIdFlow(replyId) }.collectAsState(initial = null)

                        AddLocationScreen(
                            folderId = boardId,
                            initialReply = initialReply,
                            onNavigateBack = { navController.popBackStack() },
                            onSave = { reply -> homeViewModel.saveReply(reply) }
                        )
                    }

                    // Editor de Contacto / VCard
                    composable(
                        route = "add_contact/{boardId}?replyId={replyId}",
                        arguments = listOf(
                            navArgument("boardId") { type = NavType.LongType },
                            navArgument("replyId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        )
                    ) { backStackEntry ->
                        val boardId = backStackEntry.arguments?.getLong("boardId") ?: 0L
                        val replyId = backStackEntry.arguments?.getLong("replyId") ?: 0L
                        val initialReply by remember(replyId) { homeViewModel.getReplyByIdFlow(replyId) }.collectAsState(initial = null)

                        AddContactScreen(
                            folderId = boardId,
                            initialReply = initialReply,
                            onNavigateBack = { navController.popBackStack() },
                            onSave = { reply -> homeViewModel.saveReply(reply) }
                        )
                    }

                    // Editor de Enlace
                    composable(
                        route = "add_link/{boardId}?replyId={replyId}",
                        arguments = listOf(
                            navArgument("boardId") { type = NavType.LongType },
                            navArgument("replyId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        )
                    ) { backStackEntry ->
                        val boardId = backStackEntry.arguments?.getLong("boardId") ?: 0L
                        val replyId = backStackEntry.arguments?.getLong("replyId") ?: 0L
                        val initialReply by remember(replyId) { homeViewModel.getReplyByIdFlow(replyId) }.collectAsState(initial = null)

                        AddLinkScreen(
                            folderId = boardId,
                            initialReply = initialReply,
                            onNavigateBack = { navController.popBackStack() },
                            onSave = { reply -> homeViewModel.saveReply(reply) }
                        )
                    }

                    // Editor de PDF
                    composable(
                        route = "add_pdf/{boardId}?replyId={replyId}",
                        arguments = listOf(
                            navArgument("boardId") { type = NavType.LongType },
                            navArgument("replyId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        )
                    ) { backStackEntry ->
                        val boardId = backStackEntry.arguments?.getLong("boardId") ?: 0L
                        val replyId = backStackEntry.arguments?.getLong("replyId") ?: 0L
                        val initialReply by remember(replyId) { homeViewModel.getReplyByIdFlow(replyId) }.collectAsState(initial = null)

                        AddPdfScreen(
                            folderId = boardId,
                            initialReply = initialReply,
                            onNavigateBack = { navController.popBackStack() },
                            onSave = { reply -> homeViewModel.saveReply(reply) }
                        )
                    }

                    // Editor de Imagen
                    composable(
                        route = "add_image/{boardId}?replyId={replyId}",
                        arguments = listOf(
                            navArgument("boardId") { type = NavType.LongType },
                            navArgument("replyId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        )
                    ) { backStackEntry ->
                        val boardId = backStackEntry.arguments?.getLong("boardId") ?: 0L
                        val replyId = backStackEntry.arguments?.getLong("replyId") ?: 0L
                        val initialReply by remember(replyId) { homeViewModel.getReplyByIdFlow(replyId) }.collectAsState(initial = null)

                        AddImageScreen(
                            folderId = boardId,
                            initialReply = initialReply,
                            onNavigateBack = { navController.popBackStack() },
                            onSave = { reply -> homeViewModel.saveReply(reply) }
                        )
                    }

                    // Configuración de activación de Teclado
                    composable("setup") {
                        KeyboardSetupScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

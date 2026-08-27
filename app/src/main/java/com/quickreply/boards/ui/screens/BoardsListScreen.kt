package com.quickreply.boards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.data.local.entity.FolderEntity
import com.quickreply.boards.ui.HomeScreenViewModel
import com.quickreply.boards.ui.components.BoardCardItem
import com.quickreply.boards.ui.components.EditBoardDialog
import com.quickreply.boards.ui.theme.BoardsBlue

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Sync

import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import com.quickreply.boards.ui.components.AuthDialog

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.CircularProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardsListScreen(
    viewModel: HomeScreenViewModel,
    onOpenBoard: (FolderEntity) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val rootBoards by viewModel.rootBoards.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val deletedReplies by viewModel.deletedReplies.collectAsState()

    var showCreateBoardDialog by remember { mutableStateOf(false) }
    var boardToEdit by remember { mutableStateOf<FolderEntity?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mis Boards",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C20)
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showTrashDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Papelera de Reciclaje",
                            tint = if (deletedReplies.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF707684)
                        )
                    }

                    // Botón único y consolidado de Sincronización
                    IconButton(
                        onClick = {
                            if (userSession == null) {
                                showAuthDialog = true
                            } else if (!isSyncing) {
                                viewModel.syncWithCloud { _, _ -> }
                            }
                        }
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF10B981)
                            )
                        } else {
                            Icon(
                                imageVector = if (userSession != null) Icons.Default.CloudDone else Icons.Default.AccountCircle,
                                contentDescription = "Sincronización en la Nube",
                                tint = if (userSession != null) Color(0xFF10B981) else BoardsBlue
                            )
                        }
                    }

                    // Botón de Configuraciones (Engranaje)
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuraciones",
                            tint = Color(0xFF191C20)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateBoardDialog = true },
                containerColor = BoardsBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuevo Board",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(userSession) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var totalDragY = 0f
                        var triggered = false

                        do {
                            val event = awaitPointerEvent()
                            val pressedPointers = event.changes.filter { it.pressed }

                            if (pressedPointers.size >= 2 && !triggered) {
                                val avgDeltaY = pressedPointers.map { it.positionChange().y }.average().toFloat()
                                totalDragY += avgDeltaY

                                if (totalDragY > 100f) {
                                    triggered = true
                                    if (userSession == null) {
                                        showAuthDialog = true
                                    } else {
                                        viewModel.syncWithCloud { _, _ -> }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                val displayedBoards = rootBoards

                Text(
                    text = "Tus Tableros (${displayedBoards.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF707684),
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

            if (displayedBoards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📋", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tienes ningún Board creado",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF191C20)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toca el botón + para crear tu primer tablero.",
                            fontSize = 13.sp,
                            color = Color(0xFF707684)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(displayedBoards) { board ->
                        BoardCardItem(
                            folder = board,
                            onClick = { onOpenBoard(board) },
                            onEdit = { boardToEdit = board },
                            onDelete = { viewModel.deleteFolder(board) }
                        )
                    }
                }
            }
        }
    }
}

    if (showCreateBoardDialog || boardToEdit != null) {
        EditBoardDialog(
            initialBoard = boardToEdit,
            parentId = null,
            onDismiss = {
                showCreateBoardDialog = false
                boardToEdit = null
            },
            onSave = { board ->
                viewModel.saveFolder(board)
                showCreateBoardDialog = false
                boardToEdit = null
            }
        )
    }

    if (showAuthDialog) {
        AuthDialog(
            currentSession = userSession,
            onDismiss = { showAuthDialog = false },
            onLogin = { email, pass, callback ->
                viewModel.login(email, pass) { ok, msg ->
                    callback(ok, msg)
                    if (ok) {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        showAuthDialog = false
                    }
                }
            },
            onSignUp = { email, pass, callback ->
                viewModel.signUp(email, pass) { ok, msg ->
                    callback(ok, msg)
                    if (ok) {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        showAuthDialog = false
                    }
                }
            },
            onResetPassword = { email, callback ->
                viewModel.resetPassword(email) { ok, msg ->
                    callback(ok, msg)
                }
            },
            onSignOut = {
                viewModel.signOut()
                Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showTrashDialog) {
        com.quickreply.boards.ui.components.TrashDialog(
            deletedReplies = deletedReplies,
            onDismiss = { showTrashDialog = false },
            onRestore = { replyToRestore ->
                viewModel.restoreReply(replyToRestore)
            },
            onRestoreAll = {
                viewModel.restoreAllReplies()
            },
            onPermanentDelete = { replyToDelete ->
                viewModel.permanentDeleteReply(replyToDelete)
            },
            onEmptyTrash = {
                viewModel.emptyTrash()
            }
        )
    }
}

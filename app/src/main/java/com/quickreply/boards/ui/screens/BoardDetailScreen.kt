package com.quickreply.boards.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.quickreply.boards.ui.components.TrashDialog
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.data.local.entity.FolderEntity
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import com.quickreply.boards.ui.HomeScreenViewModel
import com.quickreply.boards.ui.components.AddContentCategoryBottomSheet
import com.quickreply.boards.ui.components.DynamicFieldFillDialog
import com.quickreply.boards.ui.components.EditBoardDialog
import com.quickreply.boards.ui.components.EditFolderDialog
import com.quickreply.boards.ui.components.MoveReplyDialog
import com.quickreply.boards.ui.components.ReplyCardItem
import com.quickreply.boards.ui.components.hasDynamicFields
import com.quickreply.boards.ui.theme.BoardsBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDetailScreen(
    board: FolderEntity,
    viewModel: HomeScreenViewModel,
    onNavigateBack: () -> Unit,
    onAddText: (Long) -> Unit,
    onAddAudio: (Long) -> Unit,
    onAddSequence: (Long) -> Unit,
    onAddLocation: (Long) -> Unit,
    onAddContact: (Long) -> Unit,
    onAddLink: (Long) -> Unit,
    onAddPdf: (Long) -> Unit,
    onAddImage: (Long) -> Unit,
    onEditText: (QuickReplyEntity) -> Unit,
    onEditAudio: (QuickReplyEntity) -> Unit,
    onEditSequence: (QuickReplyEntity) -> Unit,
    onEditLocation: (QuickReplyEntity) -> Unit,
    onEditContact: (QuickReplyEntity) -> Unit,
    onEditLink: (QuickReplyEntity) -> Unit,
    onEditPdf: (QuickReplyEntity) -> Unit,
    onEditImage: (QuickReplyEntity) -> Unit,
    onNavigateToSubfolder: (FolderEntity) -> Unit = {}
) {
    val context = LocalContext.current

    val boardFlow = remember(board.id) { viewModel.getBoardByIdFlow(board.id) }
    val currentBoardState by boardFlow.collectAsState(initial = board)
    val currentBoard = currentBoardState ?: board

    val subfoldersFlow = remember(board.id) { viewModel.getSubfoldersForBoardFlow(board.id) }
    val subfolders by subfoldersFlow.collectAsState(initial = emptyList())

    val repliesFlow = remember(board.id) { viewModel.getRepliesForBoardFlow(board.id) }
    val replies by repliesFlow.collectAsState(initial = emptyList())

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val deletedReplies by viewModel.deletedReplies.collectAsState()

    var showSearchField by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showEditBoardDialog by remember { mutableStateOf(false) }
    var showAddSubfolderDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var testDynamicReply by remember { mutableStateOf<QuickReplyEntity?>(null) }
    var replyToMove by remember { mutableStateOf<QuickReplyEntity?>(null) }
    var allFoldersForMove by remember { mutableStateOf<List<FolderEntity>>(emptyList()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Modo Selección Múltiple
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedReplyIds = remember { mutableStateListOf<Long>() }

    // Emoji y nombre limpio con verificación Unicode
    val (iconEmoji, cleanBoardName) = remember(currentBoard.name) {
        com.quickreply.boards.util.EmojiUtils.parseEmojiAndTitle(currentBoard.name, defaultEmoji = "📋")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            text = "${selectedReplyIds.size} seleccionados",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C20)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode) {
                                isSelectionMode = false
                                selectedReplyIds.clear()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF191C20),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        if (selectedReplyIds.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val toDelete = replies.filter { it.id in selectedReplyIds }
                                    toDelete.forEach { viewModel.deleteReply(it) }
                                    selectedReplyIds.clear()
                                    isSelectionMode = false
                                    Toast.makeText(context, "${toDelete.size} respuestas eliminadas", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar seleccionados",
                                    tint = Color(0xFFE53935)
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { showSearchField = !showSearchField }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = Color(0xFF191C20),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Más opciones",
                                    tint = Color(0xFF191C20),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Editar Board") },
                                    onClick = {
                                        showOptionsMenu = false
                                        showEditBoardDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Selección múltiple") },
                                    onClick = {
                                        showOptionsMenu = false
                                        isSelectionMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Papelera de Reciclaje (${deletedReplies.size})") },
                                    onClick = {
                                        showOptionsMenu = false
                                        showTrashDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar Board", color = Color(0xFFE53935)) },
                                    onClick = {
                                        showOptionsMenu = false
                                        viewModel.deleteFolder(currentBoard)
                                        onNavigateBack()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showCategorySheet = true },
                    containerColor = BoardsBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar contenido",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
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
                                    viewModel.syncWithCloud { _, _ -> }
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
            // Buscador integrado
            if (showSearchField) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Buscar en este board...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF707684))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color(0xFF707684))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BoardsBlue,
                        unfocusedBorderColor = Color(0xFFE2E4E9)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            if (!isSelectionMode) {
                // Cabecera del Board
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = Color(0xFFEFF3FF)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = iconEmoji,
                                fontSize = 22.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cleanBoardName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C20),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Propietario • ${replies.size} elementos",
                            fontSize = 13.sp,
                            color = Color(0xFF707684)
                        )
                    }
                }

                // Píldoras de acción rápida
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF4F5F7),
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Enlace del Board copiado", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAddAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF191C20),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Compartir Board",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF191C20)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF4F5F7),
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "${replies.size} respuestas listas en tu teclado", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF191C20),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mensajes",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF191C20)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Seleccionar",
                        fontSize = 14.sp,
                        color = BoardsBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                isSelectionMode = true
                            }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Subcarpetas del Board
            if (subfolders.isNotEmpty()) {
                Text(
                    text = "Carpetas y Etapas (${subfolders.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C20),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subfolders.forEach { subfolder ->
                        SubfolderChipItem(
                            subfolder = subfolder,
                            viewModel = viewModel,
                            onClick = { onNavigateToSubfolder(subfolder) }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            // Grid de Respuestas Rápidas
            val displayedReplies = if (searchQuery.isNotBlank()) searchResults else replies

            if (displayedReplies.isEmpty() && subfolders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "✨ Este Board está listo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C20)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toca el botón '+' para agregar texto, audios de WhatsApp, secuencias, ubicaciones o PDFs.",
                            fontSize = 13.sp,
                            color = Color(0xFF707684),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedReplies, key = { it.id }) { reply ->
                        val isSelected = reply.id in selectedReplyIds

                        ReplyCardItem(
                            reply = reply,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (isSelected) {
                                    selectedReplyIds.remove(reply.id)
                                } else {
                                    selectedReplyIds.add(reply.id)
                                }
                            },
                            onClick = {
                                when (reply.contentType) {
                                    ContentType.TEXT -> onEditText(reply)
                                    ContentType.AUDIO -> onEditAudio(reply)
                                    ContentType.SEQUENCE -> onEditSequence(reply)
                                    ContentType.LOCATION -> onEditLocation(reply)
                                    ContentType.CONTACT -> onEditContact(reply)
                                    ContentType.LINK -> onEditLink(reply)
                                    ContentType.PDF -> onEditPdf(reply)
                                    ContentType.IMAGE -> onEditImage(reply)
                                }
                            },
                            onEdit = {
                                when (reply.contentType) {
                                    ContentType.TEXT -> onEditText(reply)
                                    ContentType.AUDIO -> onEditAudio(reply)
                                    ContentType.SEQUENCE -> onEditSequence(reply)
                                    ContentType.LOCATION -> onEditLocation(reply)
                                    ContentType.CONTACT -> onEditContact(reply)
                                    ContentType.LINK -> onEditLink(reply)
                                    ContentType.PDF -> onEditPdf(reply)
                                    ContentType.IMAGE -> onEditImage(reply)
                                }
                            },
                            onDelete = {
                                val replyToDelete = reply
                                viewModel.deleteReply(replyToDelete)
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Respuesta \"${replyToDelete.title}\" eliminada",
                                        actionLabel = "Deshacer",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreReply(replyToDelete)
                                    }
                                }
                            },
                            onMove = {
                                val selectedReply = reply
                                coroutineScope.launch {
                                    allFoldersForMove = viewModel.getAllFoldersList()
                                    replyToMove = selectedReply
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

    // Diálogo de Mover Respuesta Rápida
    replyToMove?.let { targetReply: QuickReplyEntity ->
        MoveReplyDialog(
            reply = targetReply,
            currentFolderId = currentBoard.id,
            allFolders = allFoldersForMove,
            onDismiss = { replyToMove = null },
            onSelectTargetFolder = { destinationFolder: FolderEntity ->
                val previousFolderId = targetReply.folderId
                viewModel.moveReply(targetReply, destinationFolder.id)
                replyToMove = null
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Mensaje movido a \"${destinationFolder.name}\"",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.moveReply(targetReply, previousFolderId)
                    }
                }
            }
        )
    }

    // Modal de Categorías
    if (showCategorySheet) {
        AddContentCategoryBottomSheet(
            onDismiss = { showCategorySheet = false },
            onSelectText = { onAddText(currentBoard.id) },
            onSelectAudio = { onAddAudio(currentBoard.id) },
            onSelectSequence = { onAddSequence(currentBoard.id) },
            onSelectLocation = { onAddLocation(currentBoard.id) },
            onSelectContact = { onAddContact(currentBoard.id) },
            onSelectLink = { onAddLink(currentBoard.id) },
            onSelectPdf = { onAddPdf(currentBoard.id) },
            onSelectImage = { onAddImage(currentBoard.id) },
            onSelectFolder = { showAddSubfolderDialog = true }
        )
    }

    // Diálogo de Agregar Subcarpeta
    if (showAddSubfolderDialog) {
        EditFolderDialog(
            parentId = currentBoard.id,
            onDismiss = { showAddSubfolderDialog = false },
            onSave = { newSubfolder ->
                viewModel.saveFolder(newSubfolder)
                showAddSubfolderDialog = false
            }
        )
    }

    // Diálogo de Edición de Tablero
    if (showEditBoardDialog) {
        EditBoardDialog(
            initialBoard = currentBoard,
            onDismiss = { showEditBoardDialog = false },
            onSave = { updatedBoard ->
                viewModel.saveFolder(updatedBoard)
                showEditBoardDialog = false
            }
        )
    }

    // Diálogo de Papelera de Reciclaje
    if (showTrashDialog) {
        TrashDialog(
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

    // Diálogo de Prueba de Plantilla Dinámica
    testDynamicReply?.let { dynamicReply ->
        DynamicFieldFillDialog(
            templateContent = dynamicReply.content,
            onDismiss = { testDynamicReply = null },
            onComplete = { processedText ->
                testDynamicReply = null
                Toast.makeText(context, "Respuesta generada:\n$processedText", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun SubfolderChipItem(
    subfolder: FolderEntity,
    viewModel: HomeScreenViewModel,
    onClick: () -> Unit
) {
    val subRepliesFlow = remember(subfolder.id) { viewModel.getRepliesForBoardFlow(subfolder.id) }
    val subReplies by subRepliesFlow.collectAsState(initial = emptyList())
    val folderColor = try {
        Color(android.graphics.Color.parseColor(subfolder.colorHex))
    } catch (_: Exception) {
        BoardsBlue
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = Color(0xFFF3F4F6),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = CircleShape,
                color = folderColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "📁", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = subfolder.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${subReplies.size} mensajes",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

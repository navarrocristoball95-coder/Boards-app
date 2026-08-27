package com.quickreply.boards.service.keyboard.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.quickreply.boards.ui.components.WhatsAppMarkdownHelper
import kotlinx.coroutines.delay
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.data.local.KeyboardPreferences
import com.quickreply.boards.data.local.entity.ClipboardItemEntity
import com.quickreply.boards.data.local.entity.FolderEntity
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import com.quickreply.boards.data.repository.QuickReplyRepository
import com.quickreply.boards.ui.components.LocalImagePreview
import com.quickreply.boards.ui.components.calculateFormulas
import com.quickreply.boards.ui.components.evaluateFormula
import com.quickreply.boards.ui.components.hasDynamicFields
import com.quickreply.boards.ui.components.parseDynamicVariables
import com.quickreply.boards.ui.components.processDynamicTemplate
import com.quickreply.boards.ui.screens.SEQUENCE_STEP_DELIMITER
import com.quickreply.boards.ui.theme.BoardsBlue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun KeyboardView(
    repository: QuickReplyRepository,
    scope: CoroutineScope,
    onCommitReply: (QuickReplyEntity, String?) -> Unit,
    onCommitMedia: (QuickReplyEntity, String, String) -> Unit,
    onDeleteCharacter: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    onHideKeyboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardPrefs = remember { KeyboardPreferences(context) }
    val dynamicHeight = keyboardPrefs.keyboardHeightDp.dp

    val boards by repository.rootFolders.collectAsState(initial = emptyList())
    val recentClips by repository.recentClips.collectAsState(initial = emptyList())

    var selectedBoardId by remember { mutableStateOf<Long?>(null) }
    var showBoardDrawer by remember { mutableStateOf(false) }
    var showClipboardTab by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var dynamicReplyToFill by remember { mutableStateOf<QuickReplyEntity?>(null) }
    var sequenceToPlay by remember { mutableStateOf<QuickReplyEntity?>(null) }
    var contextMenuReply by remember { mutableStateOf<QuickReplyEntity?>(null) }
    var inlineEditReply by remember { mutableStateOf<QuickReplyEntity?>(null) }

    // Board activo seleccionado por defecto
    val activeBoard = boards.find { it.id == selectedBoardId } ?: boards.firstOrNull()
    val activeBoardId = activeBoard?.id ?: 0L

    val subfoldersFlow = remember(activeBoardId) { repository.getSubfolders(activeBoardId) }
    val subfolders by subfoldersFlow.collectAsState(initial = emptyList())
    var selectedSubfolderId by remember(activeBoardId) { mutableStateOf<Long?>(null) }

    val currentRepliesFlow = remember(activeBoardId, selectedSubfolderId, searchQuery) {
        if (searchQuery.isNotBlank()) {
            repository.searchReplies(searchQuery)
        } else if (selectedSubfolderId != null) {
            repository.getRepliesByFolder(selectedSubfolderId!!)
        } else if (activeBoardId != 0L) {
            repository.getRepliesByFolderAndSubfolders(activeBoardId)
        } else {
            repository.mostUsedReplies
        }
    }
    val currentReplies by currentRepliesFlow.collectAsState(initial = emptyList())

    val iconEmoji = if (activeBoard?.name?.contains(" ") == true) {
        val parts = activeBoard.name.split(" ")
        if (parts[0].length <= 4) parts[0] else "📋"
    } else {
        "📋"
    }
    val cleanBoardName = if (activeBoard?.name?.contains(" ") == true && activeBoard.name.split(" ")[0].length <= 4) {
        activeBoard.name.substringAfter(" ")
    } else {
        activeBoard?.name ?: "Board"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(dynamicHeight),
        color = Color.White
    ) {
        val targetReply = dynamicReplyToFill
        val currentSequence = sequenceToPlay
        val activeMenuReply = contextMenuReply
        val currentEditReply = inlineEditReply

        if (targetReply != null) {
            KeyboardDynamicFieldFillView(
                reply = targetReply,
                recentClips = recentClips,
                onDismiss = { dynamicReplyToFill = null },
                onComplete = { processedText ->
                    if (targetReply.contentType == ContentType.LINK) {
                        val targetUrl = targetReply.mediaUri ?: if (targetReply.content.startsWith("http")) targetReply.content else ""
                        val formatted = WhatsAppMarkdownHelper.formatLinkMessage(processedText, targetReply.title, targetUrl)
                        onCommitReply(targetReply, formatted)
                    } else {
                        onCommitReply(targetReply, processedText)
                    }
                    dynamicReplyToFill = null
                }
            )
        } else if (currentEditReply != null) {
            // Editor Rápido Integrado en el Teclado
            KeyboardInlineEditView(
                reply = currentEditReply,
                onDismiss = { inlineEditReply = null },
                onSave = { updatedReply ->
                    scope.launch(Dispatchers.IO) {
                        repository.updateReply(updatedReply)
                    }
                    inlineEditReply = null
                }
            )
        } else if (activeMenuReply != null) {
            // Módulo Vertical de Opciones al Mantener Presionado
            KeyboardReplyActionVerticalSheet(
                reply = activeMenuReply,
                onDismiss = { contextMenuReply = null },
                onPinToggle = {
                    scope.launch(Dispatchers.IO) {
                        repository.toggleFavorite(activeMenuReply.id, !activeMenuReply.isFavorite)
                    }
                    contextMenuReply = null
                },
                onEdit = {
                    inlineEditReply = activeMenuReply
                    contextMenuReply = null
                },
                onPaste = {
                    when (activeMenuReply.contentType) {
                        ContentType.SEQUENCE -> {
                            sequenceToPlay = activeMenuReply
                        }
                        ContentType.AUDIO -> {
                            if (activeMenuReply.mediaUri != null) {
                                onCommitMedia(activeMenuReply, activeMenuReply.mediaUri, "audio/mp4")
                            } else {
                                onCommitReply(activeMenuReply, activeMenuReply.content)
                            }
                        }
                        ContentType.IMAGE, ContentType.PDF -> {
                            if (activeMenuReply.mediaUri != null) {
                                val mime = if (activeMenuReply.contentType == ContentType.PDF) "application/pdf" else "image/png"
                                onCommitMedia(activeMenuReply, activeMenuReply.mediaUri, mime)
                            } else {
                                onCommitReply(activeMenuReply, null)
                            }
                        }
                        ContentType.LINK -> {
                            val targetUrl = activeMenuReply.mediaUri ?: if (activeMenuReply.content.startsWith("http")) activeMenuReply.content else ""
                            val accompanying = if (!activeMenuReply.content.startsWith("http") && activeMenuReply.content != activeMenuReply.title) activeMenuReply.content else ""
                            val formatted = WhatsAppMarkdownHelper.formatLinkMessage(accompanying, activeMenuReply.title, targetUrl)
                            onCommitReply(activeMenuReply, formatted)
                        }
                        else -> {
                            onCommitReply(activeMenuReply, null)
                        }
                    }
                    contextMenuReply = null
                },
                onDelete = {
                    scope.launch(Dispatchers.IO) {
                        repository.deleteReply(activeMenuReply)
                    }
                    contextMenuReply = null
                }
            )
        } else if (currentSequence != null) {
            // Reproductor guiado de secuencias paso a paso
            KeyboardSequenceSenderView(
                reply = currentSequence,
                onDismiss = { sequenceToPlay = null },
                onSendStep = { stepText ->
                    onCommitReply(currentSequence, stepText)
                }
            )
        } else if (showClipboardTab) {
            // Vista de Portapapeles Inteligente
            KeyboardClipboardView(
                recentClips = recentClips,
                onClose = { showClipboardTab = false },
                onPasteClip = { clipText ->
                    onCommitReply(QuickReplyEntity(folderId = 0L, title = "Clip", content = clipText), clipText)
                },
                onTogglePin = { clipId, isPinned ->
                    scope.launch(Dispatchers.IO) {
                        repository.togglePinClip(clipId, isPinned)
                    }
                },
                onDeleteClip = { clip ->
                    scope.launch(Dispatchers.IO) {
                        repository.deleteClip(clip)
                    }
                },
                onClearAll = {
                    scope.launch(Dispatchers.IO) {
                        repository.clearUnpinnedClips()
                    }
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Fila Principal: Barra Lateral Izquierda + Área de Contenido
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Barra Lateral Izquierda (Sidebar con icono de Board activo y menú)
                    Column(
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFF9FAFB))
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Botón de Menú Hamburguesa
                        IconButton(
                            onClick = { showBoardDrawer = !showBoardDrawer },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Tableros",
                                tint = if (showBoardDrawer) BoardsBlue else Color(0xFF5F6570),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Lista rápida de Boards en el sidebar
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(boards) { boardItem ->
                                val isSelected = boardItem.id == activeBoardId
                                val bEmoji = if (boardItem.name.contains(" ")) {
                                    val p = boardItem.name.split(" ")
                                    if (p[0].length <= 4) p[0] else "📋"
                                } else "📋"

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFFEFF3FF) else Color.Transparent)
                                        .clickable {
                                            selectedBoardId = boardItem.id
                                            showBoardDrawer = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .width(3.dp)
                                                .height(24.dp)
                                                .background(BoardsBlue, RoundedCornerShape(2.dp))
                                        )
                                    }
                                    Text(text = bEmoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    // Divisor vertical
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFEAECF0))
                    )

                    // Área Central del Teclado
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (showBoardDrawer) {
                            // Selector desplegable de Boards
                            Text(
                                text = "Tus Boards",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191C20),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(boards) { b ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedBoardId = b.id
                                                showBoardDrawer = false
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (b.id == activeBoardId) Color(0xFFEFF3FF) else Color(0xFFF3F4F6)
                                    ) {
                                        Text(
                                            text = b.name,
                                            fontSize = 13.sp,
                                            fontWeight = if (b.id == activeBoardId) FontWeight.Bold else FontWeight.Normal,
                                            color = if (b.id == activeBoardId) BoardsBlue else Color(0xFF191C20),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Pestañas deslizantes horizontales entre Boards
                            if (boards.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    boards.forEach { b ->
                                        val isSel = b.id == activeBoardId
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSel) BoardsBlue else Color(0xFFF4F5F7),
                                            modifier = Modifier.clickable { selectedBoardId = b.id }
                                        ) {
                                            Text(
                                                text = b.name,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else Color(0xFF475467),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Cabecera del Board Activo y Buscador
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "$iconEmoji $cleanBoardName",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF191C20),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { showSearch = !showSearch },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "Buscar",
                                        tint = if (showSearch) BoardsBlue else Color(0xFF707684),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Chips de Etapas y Subcarpetas en el teclado
                            if (subfolders.isNotEmpty() && !showSearch) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isAll = selectedSubfolderId == null
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isAll) BoardsBlue.copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                                        border = if (isAll) androidx.compose.foundation.BorderStroke(1.dp, BoardsBlue) else null,
                                        modifier = Modifier.clickable { selectedSubfolderId = null }
                                    ) {
                                        Text(
                                            text = "Todas",
                                            fontSize = 11.sp,
                                            fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isAll) BoardsBlue else Color(0xFF4B5563),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    subfolders.forEach { sub ->
                                        val isSel = selectedSubfolderId == sub.id
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSel) BoardsBlue.copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, BoardsBlue) else null,
                                            modifier = Modifier.clickable { selectedSubfolderId = sub.id }
                                        ) {
                                            Text(
                                                text = "📁 ${sub.name}",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) BoardsBlue else Color(0xFF4B5563),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Campo de búsqueda predictiva con atajos
                            if (showSearch) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Escribe /atajo o texto...", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BoardsBlue,
                                        unfocusedBorderColor = Color(0xFFE2E4E9)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .padding(bottom = 2.dp)
                                )
                            }

                            // Grid de Respuestas Rápidas
                            if (currentReplies.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Sin respuestas en este board",
                                        fontSize = 12.sp,
                                        color = Color(0xFF8C9199)
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(currentReplies, key = { it.id }) { reply ->
                                        KeyboardReplyItemModern(
                                            reply = reply,
                                            onClick = {
                                                if (hasDynamicFields(reply.content)) {
                                                    dynamicReplyToFill = reply
                                                } else {
                                                    when (reply.contentType) {
                                                        ContentType.SEQUENCE -> {
                                                            sequenceToPlay = reply
                                                        }
                                                        ContentType.AUDIO -> {
                                                            if (reply.mediaUri != null) {
                                                                onCommitMedia(reply, reply.mediaUri, "audio/mp4")
                                                            } else {
                                                                onCommitReply(reply, reply.content)
                                                            }
                                                        }
                                                        ContentType.IMAGE, ContentType.PDF -> {
                                                            if (reply.mediaUri != null) {
                                                                val mime = if (reply.contentType == ContentType.PDF) "application/pdf" else "image/png"
                                                                onCommitMedia(reply, reply.mediaUri, mime)
                                                            } else {
                                                                onCommitReply(reply, null)
                                                            }
                                                        }
                                                        ContentType.LINK -> {
                                                            val targetUrl = reply.mediaUri ?: if (reply.content.startsWith("http")) reply.content else ""
                                                            val accompanying = if (!reply.content.startsWith("http") && reply.content != reply.title) reply.content else ""
                                                            val formatted = WhatsAppMarkdownHelper.formatLinkMessage(accompanying, reply.title, targetUrl)
                                                            onCommitReply(reply, formatted)
                                                        }
                                                        ContentType.CONTACT, ContentType.LOCATION -> {
                                                            onCommitReply(reply, reply.content)
                                                        }
                                                        ContentType.TEXT -> {
                                                            onCommitReply(reply, null)
                                                        }
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                contextMenuReply = reply
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Divisor horizontal
                HorizontalDivider(color = Color(0xFFEAECF0), thickness = 1.dp)

                // Barra Inferior de Navegación del Teclado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFFF9FAFB))
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón 'abc' para volver a Gboard
                    Surface(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSwitchKeyboard() },
                        color = Color.Transparent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "abc",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475467)
                            )
                        }
                    }

                    // Botón Portapapeles Inteligente
                    IconButton(
                        onClick = { showClipboardTab = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = "Portapapeles",
                            tint = Color(0xFF5F6570),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Botón Mensajes
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Mensajes",
                            tint = Color(0xFF5F6570),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Botón Borrar Continuo Acelerado (Backspace Holding)
                    RepeatingBackspaceButton(
                        onDelete = onDeleteCharacter,
                        modifier = Modifier.size(34.dp)
                    )

                    // Botón Ocultar Teclado
                    IconButton(
                        onClick = onHideKeyboard,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Ocultar teclado",
                            tint = Color(0xFF475467),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RepeatingBackspaceButton(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onDelete()
            delay(350)
            while (isPressed) {
                onDelete()
                delay(45)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Borrar",
            tint = Color(0xFF475467),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Módulo Vertical de Acciones que aparece al mantener presionado cualquier mensaje en el teclado:
 * 1. Fijar / Desfijar
 * 2. Editar
 * 3. Pegar
 * 4. Borrar (con Confirmación de Seguridad)
 */
@Composable
fun KeyboardReplyActionVerticalSheet(
    reply: QuickReplyEntity,
    onDismiss: () -> Unit,
    onPinToggle: () -> Unit,
    onEdit: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(12.dp)
    ) {
        // Cabecera con título del mensaje
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF191C20),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = reply.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF191C20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showDeleteConfirm) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFEBEE),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "¿Eliminar \"${reply.title}\"?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Esta acción quitará el mensaje de este tablero.",
                        color = Color(0xFF5F6570),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showDeleteConfirm = false },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E4E9), contentColor = Color(0xFF191C20)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Cancelar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onDelete,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Sí, borrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Módulo Vertical de Opciones
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Opción 1: Fijar / Desfijar
                VerticalActionItem(
                    icon = if (reply.isFavorite) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    iconColor = BoardsBlue,
                    bgColor = Color(0xFFEFF3FF),
                    title = if (reply.isFavorite) "Desfijar de arriba" else "Fijar mensaje arriba",
                    subtitle = if (reply.isFavorite) "Quitar del encabezado del tablero" else "Mostrar siempre de los primeros",
                    onClick = onPinToggle
                )

                // Opción 2: Editar
                VerticalActionItem(
                    icon = Icons.Default.Edit,
                    iconColor = Color(0xFF00897B),
                    bgColor = Color(0xFFE0F2F1),
                    title = "Editar mensaje",
                    subtitle = "Modificar el título o contenido al instante",
                    onClick = onEdit
                )

                // Opción 3: Pegar / Enviar
                VerticalActionItem(
                    icon = Icons.Default.ContentPaste,
                    iconColor = Color(0xFF3949AB),
                    bgColor = Color(0xFFE8EAF6),
                    title = "Pegar en el chat",
                    subtitle = "Insertar este contenido directamente",
                    onClick = onPaste
                )

                // Opción 4: Borrar
                VerticalActionItem(
                    icon = Icons.Default.Delete,
                    iconColor = Color(0xFFE53935),
                    bgColor = Color(0xFFFFEBEE),
                    title = "Borrar mensaje",
                    subtitle = "Eliminar de este tablero",
                    onClick = { showDeleteConfirm = true }
                )
            }
        }
    }
}

@Composable
private fun VerticalActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    bgColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF9FAFB),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAECF0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = bgColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C20)
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color(0xFF707684)
                )
            }
        }
    }
}

/**
 * Editor Rápido dentro del teclado para modificar un mensaje sin salir a la app principal
 */
@Composable
fun KeyboardInlineEditView(
    reply: QuickReplyEntity,
    onDismiss: () -> Unit,
    onSave: (QuickReplyEntity) -> Unit
) {
    var title by remember { mutableStateOf(reply.title) }
    var content by remember { mutableStateOf(reply.content) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar",
                        tint = Color(0xFF191C20),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Editar Mensaje",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C20)
                )
            }

            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSave(reply.copy(title = title.trim(), content = content.trim(), updatedAt = System.currentTimeMillis()))
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BoardsBlue),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Guardar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título", fontSize = 10.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardsBlue,
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Contenido", fontSize = 10.sp) },
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardsBlue,
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun KeyboardSequenceSenderView(
    reply: QuickReplyEntity,
    onDismiss: () -> Unit,
    onSendStep: (String) -> Unit
) {
    val steps = remember(reply.content) {
        if (reply.content.contains(SEQUENCE_STEP_DELIMITER)) {
            reply.content.split(SEQUENCE_STEP_DELIMITER).filter { it.isNotBlank() }
        } else {
            listOf(reply.content)
        }
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val totalSteps = steps.size
    val currentStep = steps.getOrNull(currentStepIndex) ?: steps.firstOrNull() ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(8.dp)
    ) {
        // Cabecera con título y progreso
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color(0xFF191C20),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "🔄 ${reply.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C20),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Paso ${currentStepIndex + 1} de $totalSteps",
                        fontSize = 10.sp,
                        color = Color(0xFF00897B),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (currentStepIndex > 0) {
                Text(
                    text = "Reiniciar",
                    fontSize = 11.sp,
                    color = BoardsBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { currentStepIndex = 0 }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Tarjeta del Mensaje Actual
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF4F5F7)
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Mensaje a enviar:",
                    fontSize = 10.sp,
                    color = Color(0xFF707684),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentStep,
                    fontSize = 12.sp,
                    color = Color(0xFF191C20),
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Botón Principal de Envío de Paso
        Button(
            onClick = {
                onSendStep(currentStep)
                if (currentStepIndex < totalSteps - 1) {
                    currentStepIndex++
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentStepIndex == totalSteps - 1) Color(0xFF00897B) else BoardsBlue
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentStepIndex == totalSteps - 1) {
                        "Enviar Paso Final (${currentStepIndex + 1}/$totalSteps)"
                    } else {
                        "Enviar Paso (${currentStepIndex + 1}/$totalSteps) y Siguiente ➔"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun KeyboardClipboardView(
    recentClips: List<ClipboardItemEntity>,
    onClose: () -> Unit,
    onPasteClip: (String) -> Unit,
    onTogglePin: (Long, Boolean) -> Unit,
    onDeleteClip: (ClipboardItemEntity) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color(0xFF191C20),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "📋 Portapapeles Reciente",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C20)
                )
            }

            if (recentClips.isNotEmpty()) {
                IconButton(onClick = onClearAll, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Limpiar no fijados",
                        tint = Color(0xFF707684),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (recentClips.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay textos copiados recientemente",
                    fontSize = 12.sp,
                    color = Color(0xFF8C9199)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(recentClips, key = { it.id }) { clip ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPasteClip(clip.text) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (clip.isPinned) Color(0xFFEFF3FF) else Color(0xFFF3F4F6)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = clip.text,
                                fontSize = 12.sp,
                                color = Color(0xFF191C20),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onTogglePin(clip.id, !clip.isPinned) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (clip.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = "Fijar",
                                        tint = if (clip.isPinned) BoardsBlue else Color(0xFF8C9199),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteClip(clip) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyboardReplyItemModern(
    reply: QuickReplyEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val isDynamic = hasDynamicFields(reply.content)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reply.isFavorite) Color(0xFFEFF3FF) else Color(0xFFF3F4F6)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (reply.isFavorite) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Fijado",
                            tint = BoardsBlue,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 2.dp)
                        )
                    }

                    if (reply.contentType == ContentType.CONTACT && reply.mediaUri != null) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .border(1.dp, BoardsBlue, CircleShape)
                        ) {
                            LocalImagePreview(uriString = reply.mediaUri, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                    }

                    Text(
                        text = reply.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF191C20),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    when (reply.contentType) {
                        ContentType.AUDIO -> {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        ContentType.SEQUENCE -> {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.AutoAwesomeMotion,
                                contentDescription = null,
                                tint = Color(0xFF00897B),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        ContentType.LOCATION -> {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        ContentType.CONTACT -> {
                            if (reply.mediaUri == null) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = null,
                                    tint = Color(0xFF3949AB),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        ContentType.LINK -> {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = Color(0xFF00ACC1),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        ContentType.PDF -> {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFFFB8C00),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        ContentType.IMAGE -> {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color(0xFF8E24AA),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        ContentType.TEXT -> {
                            if (isDynamic) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(BoardsBlue)
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text("{}", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            if (reply.contentType == ContentType.TEXT) {
                Text(
                    text = WhatsAppMarkdownHelper.parseToAnnotatedString(reply.content),
                    fontSize = 10.sp,
                    color = Color(0xFF5F6570),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
            } else if (reply.contentType == ContentType.IMAGE && reply.mediaUri != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    coil.compose.SubcomposeAsyncImage(
                        model = reply.mediaUri,
                        contentDescription = "Miniatura",
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🖼️", fontSize = 10.sp)
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFEDE9FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🖼️", fontSize = 10.sp)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (reply.content.isNotBlank()) reply.content else "🖼️ Foto adjunta",
                        fontSize = 10.sp,
                        color = Color(0xFF5F6570),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 13.sp
                    )
                }
            } else {
                val displayText = when (reply.contentType) {
                    ContentType.AUDIO -> "🎙️ Audio WhatsApp"
                    ContentType.SEQUENCE -> {
                        val count = if (reply.content.contains(SEQUENCE_STEP_DELIMITER)) {
                            reply.content.split(SEQUENCE_STEP_DELIMITER).size
                        } else 1
                        "🔄 $count mensajes: ${reply.content.split(SEQUENCE_STEP_DELIMITER).firstOrNull() ?: reply.content}"
                    }
                    ContentType.LOCATION -> "📍 ${reply.shortcut ?: reply.title}"
                    ContentType.CONTACT -> "👤 ${reply.content.lines().firstOrNull() ?: reply.title}"
                    ContentType.LINK -> {
                        val accompanying = if (!reply.content.startsWith("http") && reply.content != reply.title) reply.content else ""
                        if (accompanying.isNotBlank()) "🔗 ${reply.title} • $accompanying" else "🔗 ${reply.title}"
                    }
                    ContentType.PDF -> "📄 PDF"
                    ContentType.IMAGE -> "🖼️ Imagen"
                    ContentType.TEXT -> reply.content
                }

                Text(
                    text = displayText,
                    fontSize = 10.sp,
                    color = Color(0xFF5F6570),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
fun KeyboardDynamicFieldFillView(
    reply: QuickReplyEntity,
    recentClips: List<ClipboardItemEntity> = emptyList(),
    onDismiss: () -> Unit,
    onComplete: (processedText: String) -> Unit
) {
    val parsedVariables = remember(reply.content) {
        parseDynamicVariables(reply.content)
    }

    val fieldValues = remember {
        mutableStateMapOf<String, String>().apply {
            parsedVariables.forEach { variable ->
                if (!variable.isCalculated) {
                    this[variable.name] = variable.defaultValue
                }
            }
            calculateFormulas(parsedVariables, this)
        }
    }

    val nonCalculatedVariables = remember(parsedVariables) {
        parsedVariables.filter { !it.isCalculated }
    }

    var activeVarName by remember {
        mutableStateOf(nonCalculatedVariables.firstOrNull()?.name ?: "")
    }

    val context = LocalContext.current
    val keyboardPrefs = remember { KeyboardPreferences(context) }

    var isShifted by remember { mutableStateOf(true) } // Start with Capital letter for names
    var isSymbols by remember { mutableStateOf(false) }

    val handleAppendChar: (String) -> Unit = { char ->
        if (activeVarName.isNotBlank()) {
            val currentVal = fieldValues[activeVarName] ?: ""
            val nextVal = currentVal + char
            fieldValues[activeVarName] = nextVal
            calculateFormulas(parsedVariables, fieldValues)
            if (isShifted) isShifted = false
        }
    }

    val handleBackspace: () -> Unit = {
        if (activeVarName.isNotBlank()) {
            val currentVal = fieldValues[activeVarName] ?: ""
            if (currentVal.isNotEmpty()) {
                val nextVal = currentVal.dropLast(1)
                fieldValues[activeVarName] = nextVal
                calculateFormulas(parsedVariables, fieldValues)
            }
        }
    }

    val handleSelectSuggestion: (String) -> Unit = { suggestionText ->
        if (activeVarName.isNotBlank()) {
            val currentVal = fieldValues[activeVarName] ?: ""
            if (currentVal.isBlank() || !currentVal.contains(" ")) {
                fieldValues[activeVarName] = suggestionText
            } else {
                val words = currentVal.split(" ").toMutableList()
                if (currentVal.endsWith(" ")) {
                    words.add(suggestionText)
                } else {
                    words[words.lastIndex] = suggestionText
                }
                fieldValues[activeVarName] = words.joinToString(" ")
            }
            calculateFormulas(parsedVariables, fieldValues)
        }
    }

    val handleSend: () -> Unit = {
        calculateFormulas(parsedVariables, fieldValues)
        val activeVal = fieldValues[activeVarName] ?: ""
        if (activeVal.isNotBlank()) {
            keyboardPrefs.saveUsedName(activeVal)
        }
        val result = processDynamicTemplate(reply.content, parsedVariables, fieldValues)
        onComplete(result)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F7))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // 1. Cabecera superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onDismiss() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color(0xFF191C20),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Personalizar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C20)
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { handleSend() },
                color = BoardsBlue,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enviar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // 2. Selector de variables si hay más de 1
        if (nonCalculatedVariables.size > 1) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(nonCalculatedVariables) { variable ->
                    val isSelected = variable.name == activeVarName
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activeVarName = variable.name },
                        color = if (isSelected) BoardsBlue else Color.White,
                        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)) else null,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "[${variable.name}]",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF374151),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 3. Campo de texto activo (ancho completo 100%)
        val currentVal = fieldValues[activeVarName] ?: ""
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(vertical = 2.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, BoardsBlue),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (activeVarName.isNotBlank()) {
                        Text(
                            text = "$activeVarName: ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BoardsBlue
                        )
                    }
                    Text(
                        text = if (currentVal.isEmpty()) "Escribe el texto aquí..." else "$currentVal|",
                        fontSize = 14.sp,
                        fontWeight = if (currentVal.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (currentVal.isEmpty()) Color(0xFF9CA3AF) else Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (currentVal.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            fieldValues[activeVarName] = ""
                            calculateFormulas(parsedVariables, fieldValues)
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Borrar campo",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // 4. Franja de Sugerencias (Autocompletado, Predicción e Historial de Nombres)
        KeyboardSuggestionsBar(
            currentInput = currentVal,
            historyNames = keyboardPrefs.getUsedNamesHistory(),
            onSelectSuggestion = handleSelectSuggestion,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        // 5. Teclado QWERTY Virtual Completo con Teclas Altas Ergonómicas
        KeyboardQwertyPad(
            isShifted = isShifted,
            isSymbols = isSymbols,
            onChar = handleAppendChar,
            onBackspace = handleBackspace,
            onSpace = { handleAppendChar(" ") },
            onToggleShift = { isShifted = !isShifted },
            onToggleSymbols = { isSymbols = !isSymbols },
            onSend = handleSend
        )
    }
}

enum class SuggestionType {
    HISTORY,
    NAME,
    PREDICTION
}

data class SuggestionItem(
    val text: String,
    val type: SuggestionType
)

@Composable
fun KeyboardSuggestionsBar(
    currentInput: String,
    historyNames: List<String>,
    onSelectSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commonSpanishNames = remember {
        listOf(
            "Carlos", "María", "Juan", "Ana", "Cristóbal", "Pedro", "Claudia", "Sebastián",
            "Francisca", "Diego", "Valentina", "Matías", "Camila", "Rodrigo", "Sofía", "Felipe",
            "Andrea", "Gonzalo", "Paulina", "Javier", "Daniela", "Nicolás", "Fernanda", "Ignacio",
            "Constanza", "Alejandro", "Patricia", "Andrés", "Carolina", "Gabriel", "Natalia",
            "Tomás", "Bárbara", "Manuel", "Pía", "Vicente", "Paz", "Esteban", "Carmen",
            "Joaquín", "Consuelo", "Eduardo", "Verónica", "Jorge", "Loreto", "Ricardo",
            "Macarena", "Claudio", "Javiera", "Mauricio", "Catalina"
        )
    }

    val commonGreetingsAndPredictions = remember {
        listOf(
            "Estimado/a", "Hola", "Gracias", "Cliente", "Saludos", "Cotización", "Información",
            "Detalles", "Muchas gracias", "Atentamente", "Quedo atento", "Buenos días",
            "Buenas tardes", "Por favor", "Adjunto"
        )
    }

    val suggestions = remember(currentInput, historyNames) {
        val trimmed = currentInput.trim()
        val result = mutableListOf<SuggestionItem>()

        if (trimmed.isEmpty()) {
            historyNames.take(6).forEach { name ->
                result.add(SuggestionItem(text = name, type = SuggestionType.HISTORY))
            }
            commonSpanishNames.take(4).forEach { name ->
                if (result.none { it.text.equals(name, ignoreCase = true) }) {
                    result.add(SuggestionItem(text = name, type = SuggestionType.NAME))
                }
            }
            commonGreetingsAndPredictions.take(3).forEach { word ->
                if (result.none { it.text.equals(word, ignoreCase = true) }) {
                    result.add(SuggestionItem(text = word, type = SuggestionType.PREDICTION))
                }
            }
        } else {
            val lastWord = trimmed.split(" ").last()

            // 1. Coincidencias en Historial
            historyNames.filter { it.startsWith(lastWord, ignoreCase = true) }.take(4).forEach { name ->
                result.add(SuggestionItem(text = name, type = SuggestionType.HISTORY))
            }

            // 2. Coincidencias en Nombres Frecuentes
            commonSpanishNames.filter { it.startsWith(lastWord, ignoreCase = true) }.take(4).forEach { name ->
                if (result.none { it.text.equals(name, ignoreCase = true) }) {
                    result.add(SuggestionItem(text = name, type = SuggestionType.NAME))
                }
            }

            // 3. Coincidencias en Palabras y Frases
            commonGreetingsAndPredictions.filter { it.startsWith(lastWord, ignoreCase = true) }.take(3).forEach { word ->
                if (result.none { it.text.equals(word, ignoreCase = true) }) {
                    result.add(SuggestionItem(text = word, type = SuggestionType.PREDICTION))
                }
            }

            // 4. Si el usuario terminó una palabra con espacio, sugerir predicciones siguientes
            if (currentInput.endsWith(" ")) {
                historyNames.take(3).forEach { name ->
                    if (result.none { it.text.equals(name, ignoreCase = true) }) {
                        result.add(SuggestionItem(text = name, type = SuggestionType.HISTORY))
                    }
                }
                commonGreetingsAndPredictions.take(3).forEach { word ->
                    if (result.none { it.text.equals(word, ignoreCase = true) }) {
                        result.add(SuggestionItem(text = word, type = SuggestionType.PREDICTION))
                    }
                }
            }
        }

        result.distinctBy { it.text.lowercase() }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        color = Color(0xFFF3F4F6)
    ) {
        if (suggestions.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sugerencias y predicciones...",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Normal
                )
            }
        } else {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(suggestions) { item ->
                    val isPrimary = suggestions.firstOrNull() == item
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectSuggestion(item.text) },
                        color = if (isPrimary) Color(0xFFE0E7FF) else Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPrimary) BoardsBlue else Color(0xFFE5E7EB)
                        ),
                        shadowElevation = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (item.type == SuggestionType.HISTORY) {
                                Text("🕒 ", fontSize = 10.sp)
                            } else if (item.type == SuggestionType.PREDICTION) {
                                Text("✨ ", fontSize = 10.sp)
                            }
                            Text(
                                text = item.text,
                                fontSize = 12.sp,
                                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPrimary) BoardsBlue else Color(0xFF1F2937)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyboardQwertyPad(
    isShifted: Boolean,
    isSymbols: Boolean,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onToggleShift: () -> Unit,
    onToggleSymbols: () -> Unit,
    onSend: () -> Unit
) {
    val row1Letters = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2Letters = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ")
    val row3Letters = listOf("z", "x", "c", "v", "b", "n", "m")

    val row1Symbols = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val row2Symbols = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
    val row3Symbols = listOf("*", "\"", "'", ":", ";", "!", "?")

    val keyHeight = 48.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Fila 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val keys = if (isSymbols) row1Symbols else row1Letters
            keys.forEach { key ->
                val displayKey = if (!isSymbols && isShifted) key.uppercase() else key
                KeyboardVirtualKey(
                    text = displayKey,
                    modifier = Modifier.weight(1f),
                    height = keyHeight,
                    onClick = { onChar(displayKey) }
                )
            }
        }

        // Fila 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val keys = if (isSymbols) row2Symbols else row2Letters
            keys.forEach { key ->
                val displayKey = if (!isSymbols && isShifted) key.uppercase() else key
                KeyboardVirtualKey(
                    text = displayKey,
                    modifier = Modifier.weight(1f),
                    height = keyHeight,
                    onClick = { onChar(displayKey) }
                )
            }
        }

        // Fila 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tecla Shift / Mayúsculas
            Surface(
                modifier = Modifier
                    .weight(1.35f)
                    .height(keyHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleShift() },
                color = if (isShifted) Color(0xFFD1D5DB) else Color(0xFFE5E7EB),
                shadowElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "⇧",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isShifted) BoardsBlue else Color(0xFF1F2937)
                    )
                }
            }

            val keys = if (isSymbols) row3Symbols else row3Letters
            keys.forEach { key ->
                val displayKey = if (!isSymbols && isShifted) key.uppercase() else key
                KeyboardVirtualKey(
                    text = displayKey,
                    modifier = Modifier.weight(1f),
                    height = keyHeight,
                    onClick = { onChar(displayKey) }
                )
            }

            // Tecla Backspace (Borrar)
            Surface(
                modifier = Modifier
                    .weight(1.35f)
                    .height(keyHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBackspace() },
                color = Color(0xFFE5E7EB),
                shadowElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Borrar",
                        tint = Color(0xFF374151),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Fila 4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cambio Números / Letras (?123 / ABC)
            Surface(
                modifier = Modifier
                    .weight(1.4f)
                    .height(keyHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleSymbols() },
                color = Color(0xFFE5E7EB),
                shadowElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isSymbols) "ABC" else "?123",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                    )
                }
            }

            // Coma
            KeyboardVirtualKey(
                text = ",",
                modifier = Modifier.weight(0.9f),
                height = keyHeight,
                onClick = { onChar(",") }
            )

            // Barra Espaciadora
            Surface(
                modifier = Modifier
                    .weight(3.8f)
                    .height(keyHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSpace() },
                color = Color.White,
                shadowElevation = 1.5.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "espacio",
                        fontSize = 13.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }

            // Punto
            KeyboardVirtualKey(
                text = ".",
                modifier = Modifier.weight(0.9f),
                height = keyHeight,
                onClick = { onChar(".") }
            )

            // Enviar Directo
            Surface(
                modifier = Modifier
                    .weight(1.5f)
                    .height(keyHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSend() },
                color = BoardsBlue,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardVirtualKey(
    text: String,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 48.dp,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = Color.White,
        shadowElevation = 1.5.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827)
            )
        }
    }
}

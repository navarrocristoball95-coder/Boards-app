package com.quickreply.boards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import com.quickreply.boards.ui.components.WhatsAppListType
import com.quickreply.boards.ui.components.WhatsAppMarkdownHelper
import com.quickreply.boards.ui.theme.BoardsBlue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTextScreen(
    folderId: Long,
    initialReply: QuickReplyEntity? = null,
    onNavigateBack: () -> Unit,
    onSave: (QuickReplyEntity) -> Unit
) {
    var title by remember(initialReply?.id) { mutableStateOf(initialReply?.title ?: "") }
    var contentValue by remember(initialReply?.id) { mutableStateOf(TextFieldValue(initialReply?.content ?: "")) }
    var shortcut by remember(initialReply?.id) { mutableStateOf(initialReply?.shortcut ?: "") }

    LaunchedEffect(initialReply) {
        if (initialReply != null) {
            title = initialReply.title
            contentValue = TextFieldValue(initialReply.content, TextRange(initialReply.content.length))
            shortcut = initialReply.shortcut ?: ""
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (contentValue.text.isEmpty() && initialReply == null) {
            focusRequester.requestFocus()
        }
    }

    var showPreview by remember { mutableStateOf(false) }

    fun applyFormatting(prefix: String, suffix: String) {
        contentValue = WhatsAppMarkdownHelper.applyFormat(contentValue, prefix, suffix)
    }

    fun applyList(type: WhatsAppListType) {
        contentValue = WhatsAppMarkdownHelper.applyList(contentValue, type)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialReply == null) "Nuevo Texto" else "Editar Texto",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191C20)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF191C20),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    val isReady = contentValue.text.isNotBlank()
                    Button(
                        onClick = {
                            if (isReady) {
                                val cleanTitle = if (title.isNotBlank()) title.trim() else contentValue.text.take(20).trim()
                                val cleanShortcut = if (shortcut.isNotBlank()) {
                                    if (shortcut.startsWith("/")) shortcut.trim() else "/${shortcut.trim()}"
                                } else null

                                onSave(
                                    initialReply?.copy(
                                        title = cleanTitle,
                                        content = contentValue.text.trim(),
                                        shortcut = cleanShortcut,
                                        contentType = ContentType.TEXT,
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: QuickReplyEntity(
                                        folderId = folderId,
                                        title = cleanTitle,
                                        content = contentValue.text.trim(),
                                        shortcut = cleanShortcut,
                                        contentType = ContentType.TEXT
                                    )
                                )
                                onNavigateBack()
                            }
                        },
                        enabled = isReady,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BoardsBlue,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE2E4E9),
                            disabledContentColor = Color(0xFF9EACB9)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Listo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Título (opcional)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = "Título (opcional)",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5ABB7)
                        )
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C20)
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(BoardsBlue),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Barra de Formato Markdown Profesional de WhatsApp
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF4F5F7),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { applyFormatting("*", "*") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Negrita (*texto*)", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { applyFormatting("_", "_") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Cursiva (_texto_)", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { applyFormatting("~", "~") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.FormatStrikethrough, contentDescription = "Tachado (~texto~)", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { applyFormatting("```", "```") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Code, contentDescription = "Monoespacio (```texto```)", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { applyList(WhatsAppListType.BULLET) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Lista viñetas (•)", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { applyList(WhatsAppListType.NUMBERED) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = "Lista numerada (1.)", tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Botón para alternar Previsualización de WhatsApp
                    Surface(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { showPreview = !showPreview },
                        shape = RoundedCornerShape(8.dp),
                        color = if (showPreview) BoardsBlue else Color(0xFFE2E4E9)
                    ) {
                        Text(
                            text = if (showPreview) "👁️ Vista WhatsApp" else "✏️ Editar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showPreview) Color.White else Color(0xFF475467),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chips de variables y fórmulas dinámicas rápidas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Insertar:",
                    fontSize = 11.sp,
                    color = Color(0xFF707684),
                    fontWeight = FontWeight.Medium
                )
                val vars = listOf(
                    "{nombre}",
                    "{cliente}",
                    "{precio:1000}",
                    "{cantidad:1}",
                    "{total = precio * cantidad}",
                    "{iva = precio * 0.19}"
                )
                vars.forEach { v ->
                    SuggestionChip(
                        onClick = {
                            val current = contentValue.text
                            val newText = if (current.isEmpty()) v else "$current $v"
                            contentValue = TextFieldValue(newText, TextRange(newText.length))
                        },
                        label = { Text(v, fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFFEFF3FF),
                            labelColor = BoardsBlue
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de Texto Principal Multilínea / Vista Previa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (showPreview) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFEAE2) // Color de fondo típico de chat de WhatsApp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFD9FDD3), // Burbuja de chat WhatsApp
                                tonalElevation = 1.dp
                            ) {
                                Text(
                                    text = WhatsAppMarkdownHelper.parseToAnnotatedString(contentValue.text),
                                    fontSize = 15.sp,
                                    color = Color(0xFF111B21),
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                } else {
                    if (contentValue.text.isEmpty()) {
                        Text(
                            text = "Escribe aquí tu mensaje rápido para WhatsApp...",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = Color(0xFFA5ABB7),
                                lineHeight = 24.sp
                            )
                        )
                    }
                    BasicTextField(
                        value = contentValue,
                        onValueChange = { contentValue = it },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color(0xFF191C20),
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(BoardsBlue),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester)
                    )
                }
            }
        }
    }
}

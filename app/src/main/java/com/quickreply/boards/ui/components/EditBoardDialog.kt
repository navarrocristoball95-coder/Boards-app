package com.quickreply.boards.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.quickreply.boards.ui.theme.BoardsBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditBoardDialog(
    initialBoard: FolderEntity? = null,
    parentId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (FolderEntity) -> Unit
) {
    val emojiCategories = listOf(
        "💼 Ventas" to listOf("💼", "💰", "🚀", "🛒", "🏷️", "💳", "💵", "📈", "📊", "🤝", "📦", "🎯", "🏆", "💎", "🔥"),
        "💬 Chat" to listOf("💬", "📱", "📞", "✉️", "📢", "🔔", "❤️", "👍", "✨", "🤖", "💡", "⚡", "👋", "🎉", "🌟"),
        "🏢 Servicios" to listOf("🏡", "🏢", "🔑", "🛠️", "🚗", "✈️", "🏥", "📚", "🎨", "🍔", "☕", "🍕", "👔", "✂️", "⚽"),
        "🎱 Símbolos" to listOf("🎱", "⭐", "📌", "🔒", "✅", "⚠️", "🕒", "📁", "🗂️", "📑", "📋", "🌐", "🔗", "🎧", "🎬")
    )

    val initialName = initialBoard?.name ?: ""
    val defaultFallbackEmoji = if (initialBoard?.parentId != null || parentId != null) "📁" else "📋"
    val (initialEmoji, cleanInitialName) = remember(initialName) {
        if (initialName.isNotEmpty()) {
            com.quickreply.boards.util.EmojiUtils.parseEmojiAndTitle(initialName, defaultEmoji = defaultFallbackEmoji)
        } else {
            Pair(defaultFallbackEmoji, "")
        }
    }

    val presetColors = listOf("#4361EE", "#3A0CA3", "#7209B7", "#F72585", "#4CC9F0", "#10B981", "#F59E0B", "#EF4444")
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(cleanInitialName) }
    var selectedEmoji by remember { mutableStateOf(if (initialBoard != null) initialEmoji else (if (parentId != null) "📁" else "📋")) }
    var customEmojiInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(initialBoard?.colorHex ?: "#4361EE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialBoard == null) {
                    if (parentId != null) "Nueva Subcarpeta" else "Nuevo Board"
                } else {
                    if (initialBoard.parentId != null) "Editar Subcarpeta" else "Editar Board"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Previsualización del Emoji y Color seleccionado
                val previewColor = try {
                    Color(android.graphics.Color.parseColor(selectedColor))
                } catch (_: Exception) {
                    BoardsBlue
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = previewColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, previewColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = selectedEmoji, fontSize = 26.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Ícono y Color",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF191C20)
                        )
                        Text(
                            text = "Selecciona el color del ícono y el emoji identificador",
                            fontSize = 11.sp,
                            color = Color(0xFF707684)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de Color del Ícono
                Text(
                    text = "Color del Ícono",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF191C20)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { colorHex ->
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (_: Exception) {
                            BoardsBlue
                        }
                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)

                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { selectedColor = colorHex },
                            shape = CircleShape,
                            color = parsedColor,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFF191C20)) else null,
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de Categorías de Emojis
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    emojiCategories.forEachIndexed { index, (catName, _) ->
                        val isSelected = selectedCategoryIndex == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryIndex = index },
                            label = { Text(catName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEFF3FF),
                                selectedLabelColor = BoardsBlue
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grid/Flow de Emojis de la categoría seleccionada
                val currentCategoryEmojis = emojiCategories[selectedCategoryIndex].second
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currentCategoryEmojis.forEach { emoji ->
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    selectedEmoji = emoji
                                    customEmojiInput = ""
                                },
                            shape = CircleShape,
                            color = if (selectedEmoji == emoji) Color(0xFFEFF3FF) else Color(0xFFF4F5F7)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Entrada libre para cualquier emoji personalizado desde el teclado
                OutlinedTextField(
                    value = customEmojiInput,
                    onValueChange = { input ->
                        customEmojiInput = input
                        if (input.isNotBlank()) {
                            selectedEmoji = input.trim()
                        }
                    },
                    label = { Text("O escribe tu propio emoji aquí (ej. 👑, 🦄, ⚡)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BoardsBlue,
                        unfocusedBorderColor = Color(0xFFE2E4E9)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Nombre del Board / Subcarpeta
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (parentId != null || initialBoard?.parentId != null) "Nombre de la Subcarpeta" else "Nombre del Board") },
                    placeholder = { Text("ej. Precios, WhatsApp Clientes...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BoardsBlue,
                        unfocusedBorderColor = Color(0xFFE2E4E9)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val fullName = if (selectedEmoji.isNotBlank()) "$selectedEmoji ${name.trim()}" else name.trim()
                        onSave(
                            initialBoard?.copy(
                                name = fullName,
                                colorHex = selectedColor,
                                isSynced = false
                            ) ?: FolderEntity(
                                name = fullName,
                                colorHex = selectedColor,
                                parentId = parentId,
                                isSynced = false
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BoardsBlue),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF707684))
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

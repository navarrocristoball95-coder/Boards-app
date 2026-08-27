package com.quickreply.boards.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditReplyDialog(
    initialReply: QuickReplyEntity? = null,
    folderId: Long,
    onDismiss: () -> Unit,
    onSave: (QuickReplyEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialReply?.title ?: "") }
    var content by remember { mutableStateOf(initialReply?.content ?: "") }
    var shortcut by remember { mutableStateOf(initialReply?.shortcut ?: "") }

    val presetTags = listOf("nombre", "monto", "cliente", "producto", "banco", "fecha")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initialReply == null) "Nueva Respuesta Rápida" else "Editar Respuesta")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título identificador (ej. Cotización)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = shortcut,
                    onValueChange = { shortcut = it },
                    label = { Text("Atajo opcional (ej. /precio)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Insertar campo dinámico:",
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    presetTags.forEach { tag ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                content += " {$tag}"
                            },
                            label = { Text("{$tag}") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Texto de la respuesta") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        val reply = initialReply?.copy(
                            title = title.trim(),
                            content = content.trim(),
                            shortcut = if (shortcut.isNotBlank()) shortcut.trim() else null
                        ) ?: QuickReplyEntity(
                            folderId = folderId,
                            title = title.trim(),
                            content = content.trim(),
                            shortcut = if (shortcut.isNotBlank()) shortcut.trim() else null,
                            contentType = ContentType.TEXT
                        )
                        onSave(reply)
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

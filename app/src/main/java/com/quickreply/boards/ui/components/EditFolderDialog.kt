package com.quickreply.boards.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quickreply.boards.data.local.entity.FolderEntity

@Composable
fun EditFolderDialog(
    initialFolder: FolderEntity? = null,
    parentId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (FolderEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialFolder?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initialFolder == null) "Nueva Carpeta" else "Editar Carpeta")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la carpeta (ej. Ventas, Ofertas)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val folder = initialFolder?.copy(name = name.trim())
                            ?: FolderEntity(name = name.trim(), parentId = parentId)
                        onSave(folder)
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

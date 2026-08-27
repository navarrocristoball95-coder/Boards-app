package com.quickreply.boards.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import com.quickreply.boards.ui.screens.SEQUENCE_STEP_DELIMITER
import com.quickreply.boards.ui.theme.BoardsBlue

@Composable
fun ReplyCardItem(
    reply: QuickReplyEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDynamic = hasDynamicFields(reply.content)
    val uriHandler = LocalUriHandler.current

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar respuesta rápida?") },
            text = { Text("Se eliminará \"${reply.title}\" de este tablero.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable {
                if (isSelectionMode) onToggleSelect() else onClick()
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEFF3FF) else if (reply.isFavorite) Color(0xFFF0F4FF) else Color(0xFFF3F4F6)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    if (isSelectionMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = if (isSelected) BoardsBlue else Color(0xFF8C9199),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (reply.isFavorite) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Fijado",
                            tint = BoardsBlue,
                            modifier = Modifier
                                .size(15.dp)
                                .padding(end = 3.dp)
                        )
                    }

                    if (reply.contentType == ContentType.CONTACT && reply.mediaUri != null) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.dp, BoardsBlue, CircleShape)
                        ) {
                            LocalImagePreview(uriString = reply.mediaUri, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = reply.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF191C20),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    when (reply.contentType) {
                        ContentType.AUDIO -> {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Audio WhatsApp",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        ContentType.SEQUENCE -> {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.AutoAwesomeMotion,
                                contentDescription = "Secuencia",
                                tint = Color(0xFF00897B),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        ContentType.LOCATION -> {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Ubicación",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        ContentType.CONTACT -> {
                            if (reply.mediaUri == null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = "Contacto",
                                    tint = Color(0xFF3949AB),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        ContentType.LINK -> {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Enlace",
                                tint = Color(0xFF00ACC1),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        ContentType.PDF -> {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "PDF",
                                tint = Color(0xFFFB8C00),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        ContentType.IMAGE -> {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Imagen",
                                tint = Color(0xFF8E24AA),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        ContentType.TEXT -> {
                            if (isDynamic) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(BoardsBlue)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("{}", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (!isSelectionMode) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = Color(0xFF8C9199),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📁 Mover a...") },
                                onClick = {
                                    showMenu = false
                                    onMove()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = Color(0xFFE53935)) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (reply.contentType == ContentType.LINK) {
                val targetUrl = reply.mediaUri ?: if (reply.content.startsWith("http")) reply.content else ""
                val shortName = reply.title.ifBlank { "Enlace" }
                val accompanying = if (!reply.content.startsWith("http") && reply.content != reply.title) reply.content else ""

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (accompanying.isNotBlank()) {
                        Text(
                            text = WhatsAppMarkdownHelper.parseToAnnotatedString(accompanying),
                            fontSize = 11.sp,
                            color = Color(0xFF5F6570),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF3FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBE5FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (targetUrl.isNotBlank()) {
                                    try {
                                        var formatted = targetUrl.trim()
                                        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
                                            formatted = "https://$formatted"
                                        }
                                        uriHandler.openUri(formatted)
                                    } catch (_: Exception) {}
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = BoardsBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = shortName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BoardsBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (targetUrl.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Abrir enlace",
                                    tint = BoardsBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            } else if (reply.contentType == ContentType.TEXT) {
                Text(
                    text = WhatsAppMarkdownHelper.parseToAnnotatedString(reply.content),
                    fontSize = 12.sp,
                    color = Color(0xFF5F6570),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            } else {
                val previewText = when (reply.contentType) {
                    ContentType.AUDIO -> "🎙️ Nota de voz WhatsApp"
                    ContentType.SEQUENCE -> {
                        val count = if (reply.content.contains(SEQUENCE_STEP_DELIMITER)) {
                            reply.content.split(SEQUENCE_STEP_DELIMITER).size
                        } else 1
                        "🔄 $count mensajes: ${reply.content.split(SEQUENCE_STEP_DELIMITER).firstOrNull() ?: reply.content}"
                    }
                    ContentType.LOCATION -> "📍 ${reply.shortcut ?: reply.content.lines().firstOrNull() ?: reply.title}"
                    ContentType.CONTACT -> "👤 ${reply.content.lines().firstOrNull() ?: reply.title}"
                    ContentType.LINK -> "🔗 ${reply.content}"
                    ContentType.PDF -> "📄 Archivo PDF adjunto"
                    ContentType.IMAGE -> "🖼️ Imagen / Foto adjunta"
                    ContentType.TEXT -> reply.content
                }

                Text(
                    text = previewText,
                    fontSize = 12.sp,
                    color = Color(0xFF5F6570),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

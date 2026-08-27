package com.quickreply.boards.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.data.local.entity.FolderEntity
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.ui.theme.BoardsBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveReplyDialog(
    reply: QuickReplyEntity,
    currentFolderId: Long,
    allFolders: List<FolderEntity>,
    onDismiss: () -> Unit,
    onSelectTargetFolder: (FolderEntity) -> Unit
) {
    val rootFolders = allFolders.filter { it.parentId == null }
    val getSubfolders: (Long) -> List<FolderEntity> = { parentId ->
        allFolders.filter { it.parentId == parentId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DriveFileMove,
                        contentDescription = null,
                        tint = BoardsBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mover Mensaje",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF191C20)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF707684))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Selecciona el tablero o subcarpeta de destino para:",
                    fontSize = 12.sp,
                    color = Color(0xFF707684)
                )
                Text(
                    text = "\"${reply.title}\"",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BoardsBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rootFolders.forEach { root ->
                        val isCurrent = root.id == currentFolderId
                        val subfolders = getSubfolders(root.id)
                        val folderColor = try {
                            Color(android.graphics.Color.parseColor(root.colorHex))
                        } catch (_: Exception) {
                            BoardsBlue
                        }

                        // Tablero Raíz
                        item(key = "root_${root.id}") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isCurrent) {
                                        onSelectTargetFolder(root)
                                    },
                                color = if (isCurrent) Color(0xFFF3F4F6) else Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) Color(0xFFD1D5DB) else Color(0xFFE5E7EB)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(28.dp),
                                        shape = CircleShape,
                                        color = folderColor.copy(alpha = 0.2f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = "📋", fontSize = 14.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = root.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color(0xFF9CA3AF) else Color(0xFF191C20),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isCurrent) {
                                            Text(
                                                text = "Ubicación actual",
                                                fontSize = 11.sp,
                                                color = Color(0xFF9CA3AF),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Subcarpetas del Tablero Raíz
                        subfolders.forEach { sub ->
                            val isSubCurrent = sub.id == currentFolderId
                            val subColor = try {
                                Color(android.graphics.Color.parseColor(sub.colorHex))
                            } catch (_: Exception) {
                                folderColor
                            }

                            item(key = "sub_${sub.id}") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isSubCurrent) {
                                            onSelectTargetFolder(sub)
                                        },
                                    color = if (isSubCurrent) Color(0xFFF3F4F6) else Color(0xFFFAFAFA),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSubCurrent) Color(0xFFD1D5DB) else Color(0xFFE5E7EB)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(24.dp),
                                            shape = CircleShape,
                                            color = subColor.copy(alpha = 0.2f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = "📁", fontSize = 12.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sub.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSubCurrent) Color(0xFF9CA3AF) else Color(0xFF374151),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isSubCurrent) {
                                                Text(
                                                    text = "Ubicación actual",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF9CA3AF)
                                                )
                                            }
                                        }
                                        if (isSubCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF9CA3AF),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF707684))
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

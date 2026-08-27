package com.quickreply.boards.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import com.quickreply.boards.ui.theme.BoardsBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLocationScreen(
    folderId: Long,
    initialReply: QuickReplyEntity? = null,
    onNavigateBack: () -> Unit,
    onSave: (QuickReplyEntity) -> Unit
) {
    var branchName by remember(initialReply?.id) { mutableStateOf(initialReply?.title ?: "") }
    var address by remember(initialReply?.id) { mutableStateOf(initialReply?.shortcut ?: "") }
    var googleMapsUrl by remember(initialReply?.id) { mutableStateOf("") }
    var wazeUrl by remember(initialReply?.id) { mutableStateOf("") }
    var schedule by remember(initialReply?.id) { mutableStateOf("") }
    var note by remember(initialReply?.id) { mutableStateOf(initialReply?.content ?: "") }

    LaunchedEffect(initialReply) {
        if (initialReply != null) {
            branchName = initialReply.title
            address = initialReply.shortcut ?: ""
            note = initialReply.content
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialReply == null) "Nueva Ubicación" else "Editar Ubicación",
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
                    val canSave = branchName.isNotBlank() || address.isNotBlank()
                    Button(
                        onClick = {
                            if (canSave) {
                                val cleanTitle = branchName.trim().ifEmpty { "Ubicación / Sucursal" }
                                val cleanAddress = address.trim().ifEmpty { "Dirección" }

                                val encodedQuery = java.net.URLEncoder.encode(cleanAddress, "UTF-8")
                                val finalMapsUrl = if (googleMapsUrl.isNotBlank()) {
                                    googleMapsUrl.trim()
                                } else {
                                    "https://maps.google.com/?q=$encodedQuery"
                                }

                                val finalWazeUrl = if (wazeUrl.isNotBlank()) {
                                    wazeUrl.trim()
                                } else {
                                    "https://waze.com/ul?q=$encodedQuery"
                                }

                                val formattedContent = buildString {
                                    appendLine("📍 *$cleanTitle*")
                                    appendLine("🏢 $cleanAddress")
                                    if (schedule.isNotBlank()) appendLine("⏰ Horario: ${schedule.trim()}")
                                    if (note.isNotBlank()) appendLine("ℹ️ ${note.trim()}")
                                    appendLine()
                                    appendLine("🗺️ *Cómo llegar:*")
                                    appendLine("• Google Maps: $finalMapsUrl")
                                    appendLine("• Waze: $finalWazeUrl")
                                }

                                onSave(
                                    initialReply?.copy(
                                        title = cleanTitle,
                                        content = formattedContent.trim(),
                                        shortcut = cleanAddress,
                                        contentType = ContentType.LOCATION,
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: QuickReplyEntity(
                                        folderId = folderId,
                                        title = cleanTitle,
                                        content = formattedContent.trim(),
                                        shortcut = cleanAddress,
                                        contentType = ContentType.LOCATION
                                    )
                                )
                                onNavigateBack()
                            }
                        },
                        enabled = canSave,
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Nombre de la Sucursal / Oficina
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (branchName.isEmpty()) {
                    Text(
                        text = "Nombre (ej. Oficina Central)",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5ABB7)
                        )
                    )
                }
                BasicTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C20)
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(BoardsBlue),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dirección
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Dirección completa") },
                placeholder = { Text("ej. Av. Providencia 1234, Oficina 501, Santiago") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = BoardsBlue) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardsBlue,
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Horario de Atención
            OutlinedTextField(
                value = schedule,
                onValueChange = { schedule = it },
                label = { Text("Horario de atención") },
                placeholder = { Text("ej. Lun a Vie 09:00 - 18:00") },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = BoardsBlue) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardsBlue,
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Enlace de Google Maps (Opcional)
            OutlinedTextField(
                value = googleMapsUrl,
                onValueChange = { googleMapsUrl = it },
                label = { Text("Enlace Google Maps (opcional, se autogenera)") },
                leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = BoardsBlue) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardsBlue,
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Enlace de Waze (Opcional)
            OutlinedTextField(
                value = wazeUrl,
                onValueChange = { wazeUrl = it },
                label = { Text("Enlace Waze (opcional, se autogenera)") },
                leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = null, tint = BoardsBlue) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardsBlue,
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Indicaciones / Notas
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Indicaciones de llegada o estacionamiento") },
                placeholder = { Text("ej. Estacionamiento para clientes en subterráneo -1.") },
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardsBlue,
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

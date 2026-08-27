package com.quickreply.boards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import com.quickreply.boards.ui.components.WhatsAppMarkdownHelper
import com.quickreply.boards.ui.theme.BoardsBlue

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLinkScreen(
    folderId: Long,
    initialReply: QuickReplyEntity? = null,
    onNavigateBack: () -> Unit,
    onSave: (QuickReplyEntity) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    // 1. Nombre corto / Texto visible del link
    var shortName by remember(initialReply?.id) { 
        mutableStateOf(initialReply?.title ?: "") 
    }
    
    // 2. URL de destino real
    var url by remember(initialReply?.id) { 
        mutableStateOf(
            initialReply?.mediaUri 
                ?: (if (initialReply?.content?.startsWith("http") == true) initialReply.content else "")
        ) 
    }
    
    // 3. Texto de acompañamiento (mensaje que precede al link)
    var accompanyingText by remember(initialReply?.id) { 
        mutableStateOf(
            if (initialReply?.content?.startsWith("http") == false && initialReply.content != initialReply.title) {
                initialReply.content
            } else {
                ""
            }
        ) 
    }

    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = remember(shortName, url, accompanyingText, initialReply) {
        val origTitle = initialReply?.title.orEmpty()
        val origUrl = initialReply?.mediaUri ?: if (initialReply?.content?.startsWith("http") == true) initialReply.content else ""
        val origAccompanying = if (initialReply?.content?.startsWith("http") == false && initialReply.content != initialReply.title) initialReply.content else ""
        (shortName.trim() != origTitle.trim()) || (url.trim() != origUrl.trim()) || (accompanyingText.trim() != origAccompanying.trim())
    }

    val handleSafeBack = {
        if (hasUnsavedChanges && (shortName.isNotBlank() || url.isNotBlank() || accompanyingText.isNotBlank())) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = true) {
        handleSafeBack()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("¿Descartar cambios?", fontWeight = FontWeight.Bold) },
            text = { Text("Tienes cambios sin guardar. Si sales ahora, se perderán.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Descartar", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continuar editando")
                }
            }
        )
    }

    LaunchedEffect(initialReply) {
        if (initialReply != null) {
            shortName = initialReply.title
            url = initialReply.mediaUri ?: (if (initialReply.content.startsWith("http")) initialReply.content else "")
            accompanyingText = if (!initialReply.content.startsWith("http") && initialReply.content != initialReply.title) {
                initialReply.content
            } else {
                ""
            }
        }
    }

    var enableSmartTracking by remember { mutableStateOf(false) }
    var utmCampaign by remember { mutableStateOf("whatsapp_quickreply") }

    val formattedOutput = WhatsAppMarkdownHelper.formatLinkMessage(
        accompanyingText = accompanyingText,
        shortName = shortName,
        url = url
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialReply == null) "Nuevo Enlace" else "Editar Enlace",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191C20)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleSafeBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF191C20),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (url.isNotBlank()) {
                                var formattedUrl = url.trim()
                                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                    formattedUrl = "https://$formattedUrl"
                                }

                                if (enableSmartTracking) {
                                    val delimiter = if (formattedUrl.contains("?")) "&" else "?"
                                    formattedUrl = "$formattedUrl${delimiter}utm_source=quickreply&utm_medium=keyboard&utm_campaign=${utmCampaign.trim().ifBlank { "boards" }}"
                                }

                                val cleanShortName = shortName.trim().ifBlank { "Enlace" }
                                val cleanAccompanying = accompanyingText.trim()

                                onSave(
                                    initialReply?.copy(
                                        title = cleanShortName,
                                        content = cleanAccompanying,
                                        mediaUri = formattedUrl,
                                        shortcut = null,
                                        contentType = ContentType.LINK,
                                        isSynced = false,
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: QuickReplyEntity(
                                        folderId = folderId,
                                        title = cleanShortName,
                                        content = cleanAccompanying,
                                        mediaUri = formattedUrl,
                                        shortcut = null,
                                        contentType = ContentType.LINK,
                                        isSynced = false
                                    )
                                )
                                onNavigateBack()
                            }
                        },
                        enabled = url.isNotBlank(),
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
            // Campo 1: Título o Atajo
            OutlinedTextField(
                value = shortName,
                onValueChange = { shortName = it },
                label = { Text("Título o Atajo") },
                placeholder = { Text("Ej: 2° agendamiento, Saludo inicial, Catálogo 2026...") },
                leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF00897B)) },
                supportingText = { Text("Nombre o atajo que identifica a la respuesta rápida en tu tablero.") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00897B),
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Campo 2: URL de destino real
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL de destino real") },
                placeholder = { Text("https://calendar.app.google/... o tusitio.com") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = BoardsBlue) },
                supportingText = { Text("Dirección web de destino. Se validará automáticamente con https://.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardsBlue,
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Campo 3: Texto de acompañamiento (Opcional)
            OutlinedTextField(
                value = accompanyingText,
                onValueChange = { accompanyingText = it },
                label = { Text("Texto de acompañamiento (Opcional)") },
                placeholder = { Text("Ej: Hola {nombre}, te envío el enlace para agendar:") },
                leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFFE65100)) },
                supportingText = { Text("Mensaje introductorio que precederá al enlace al enviarlo por WhatsApp.") },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE65100),
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Variables dinámicas rápidas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("nombre", "empresa", "producto", "fecha").forEach { varName ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF3F4F6),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.clickable {
                            val spacer = if (accompanyingText.isNotEmpty() && !accompanyingText.endsWith(" ")) " " else ""
                            accompanyingText += "$spacer{$varName}"
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DataObject, contentDescription = null, tint = BoardsBlue, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "{$varName}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vista previa interactiva de salida hacia WhatsApp
            Text(
                text = "VISTA PREVIA DE SALIDA EN CHAT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF707684),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE7FCE3),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC3EBC0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(24.dp),
                                shape = CircleShape,
                                color = Color(0xFF25D366)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mensaje a enviar por WhatsApp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }

                        if (url.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.clickable {
                                    try {
                                        var target = url.trim()
                                        if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                            target = "https://$target"
                                        }
                                        uriHandler.openUri(target)
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Probar link", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = formattedOutput.ifBlank { "Escribe la URL para ver el formato de salida..." },
                        fontSize = 13.sp,
                        color = Color(0xFF191C20),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smart Link Tracking
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF4F5F7),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = BoardsBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Smart Link Tracking",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF191C20)
                                )
                                Text(
                                    text = "Añade etiquetas UTM para rastrear clics en analítica",
                                    fontSize = 11.sp,
                                    color = Color(0xFF707684)
                                )
                            }
                        }
                        Switch(
                            checked = enableSmartTracking,
                            onCheckedChange = { enableSmartTracking = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BoardsBlue)
                        )
                    }

                    if (enableSmartTracking) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = utmCampaign,
                            onValueChange = { utmCampaign = it },
                            label = { Text("Nombre de campaña (UTM Campaign)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BoardsBlue,
                                unfocusedBorderColor = Color(0xFFE2E4E9)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

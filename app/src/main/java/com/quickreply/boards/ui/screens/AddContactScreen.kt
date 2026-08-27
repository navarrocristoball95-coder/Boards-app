package com.quickreply.boards.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import com.quickreply.boards.ui.components.LocalImagePreview
import com.quickreply.boards.ui.theme.BoardsBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    folderId: Long,
    initialReply: QuickReplyEntity? = null,
    onNavigateBack: () -> Unit,
    onSave: (QuickReplyEntity) -> Unit
) {
    var name by remember(initialReply?.id) { mutableStateOf(initialReply?.title ?: "") }
    var phone by remember(initialReply?.id) { mutableStateOf(initialReply?.shortcut ?: "") }
    var email by remember(initialReply?.id) { mutableStateOf("") }
    var company by remember(initialReply?.id) { mutableStateOf("") }
    var note by remember(initialReply?.id) { mutableStateOf(initialReply?.content ?: "") }
    var profilePhotoUri by remember(initialReply?.id) { mutableStateOf<Uri?>(initialReply?.mediaUri?.let { Uri.parse(it) }) }

    LaunchedEffect(initialReply) {
        if (initialReply != null) {
            name = initialReply.title
            phone = initialReply.shortcut ?: ""
            note = initialReply.content
            profilePhotoUri = initialReply.mediaUri?.let { Uri.parse(it) }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            profilePhotoUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialReply == null) "Nuevo Contacto" else "Editar Contacto",
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
                    val canSave = name.isNotBlank() || phone.isNotBlank()
                    Button(
                        onClick = {
                            if (canSave) {
                                val cleanName = name.trim().ifEmpty { "Contacto" }
                                val formattedContent = buildString {
                                    appendLine("👤 *Contacto:* $cleanName")
                                    if (phone.isNotBlank()) appendLine("📞 *Teléfono:* ${phone.trim()}")
                                    if (email.isNotBlank()) appendLine("✉️ *Email:* ${email.trim()}")
                                    if (company.isNotBlank()) appendLine("🏢 *Empresa:* ${company.trim()}")
                                    if (note.isNotBlank()) {
                                        appendLine()
                                        append(note.trim())
                                    }
                                }

                                onSave(
                                    initialReply?.copy(
                                        title = cleanName,
                                        content = formattedContent.trim(),
                                        shortcut = phone.trim().ifEmpty { null },
                                        mediaUri = profilePhotoUri?.toString(),
                                        contentType = ContentType.CONTACT,
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: QuickReplyEntity(
                                        folderId = folderId,
                                        title = cleanName,
                                        content = formattedContent.trim(),
                                        shortcut = phone.trim().ifEmpty { null },
                                        mediaUri = profilePhotoUri?.toString(),
                                        contentType = ContentType.CONTACT
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
            // Sección de Foto de Perfil + Nombre
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar con selector de foto
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF3FF))
                        .border(2.dp, BoardsBlue, CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val currentPhoto = profilePhotoUri
                    if (currentPhoto != null) {
                        LocalImagePreview(
                            uriString = currentPhoto.toString(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = BoardsBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Insignia de cámara
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(BoardsBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Cambiar foto",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Nombre Completo
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (name.isEmpty()) {
                            Text(
                                text = "Nombre Completo",
                                style = TextStyle(
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA5ABB7)
                                )
                            )
                        }
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it },
                            textStyle = TextStyle(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191C20)
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(BoardsBlue),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = "Toca el avatar para elegir foto de perfil",
                        fontSize = 11.sp,
                        color = Color(0xFF707684),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Teléfono / WhatsApp
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono / WhatsApp") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BoardsBlue) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardsBlue,
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BoardsBlue) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardsBlue,
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Empresa
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Empresa o Cargo") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = BoardsBlue) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoardsBlue,
                    unfocusedBorderColor = Color(0xFFE2E4E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Nota / Mensaje adicional
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Mensaje o notas adicionales") },
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

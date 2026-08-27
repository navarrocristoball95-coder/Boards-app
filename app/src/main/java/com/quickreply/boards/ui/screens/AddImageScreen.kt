package com.quickreply.boards.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import com.quickreply.boards.ui.theme.BoardsBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddImageScreen(
    folderId: Long,
    initialReply: QuickReplyEntity? = null,
    onNavigateBack: () -> Unit,
    onSave: (QuickReplyEntity) -> Unit
) {
    var title by remember(initialReply?.id) { mutableStateOf(initialReply?.title ?: "") }
    var selectedImageUri by remember(initialReply?.id) { mutableStateOf<Uri?>(initialReply?.mediaUri?.let { Uri.parse(it) }) }
    var note by remember(initialReply?.id) { mutableStateOf(initialReply?.content ?: "") }

    LaunchedEffect(initialReply) {
        if (initialReply != null) {
            title = initialReply.title
            note = initialReply.content
            selectedImageUri = initialReply.mediaUri?.let { Uri.parse(it) }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var isCompressing by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isCompressing = true
                val localUri = com.quickreply.boards.data.local.FileStorageHelper.compressAndSaveImageLocally(context, uri)
                selectedImageUri = localUri
                if (title.isEmpty()) {
                    title = "Imagen"
                }
                isCompressing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialReply == null) "Nueva Imagen" else "Editar Imagen",
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
                    val canSave = selectedImageUri != null || note.isNotBlank() || title.isNotBlank()
                    Button(
                        onClick = {
                            if (canSave) {
                                val cleanTitle = if (title.isBlank()) "Imagen" else title.trim()
                                val contentText = note.trim()
                                onSave(
                                    initialReply?.copy(
                                        title = cleanTitle,
                                        content = contentText,
                                        mediaUri = selectedImageUri?.toString(),
                                        contentType = ContentType.IMAGE,
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: QuickReplyEntity(
                                        folderId = folderId,
                                        title = cleanTitle,
                                        content = contentText,
                                        mediaUri = selectedImageUri?.toString(),
                                        contentType = ContentType.IMAGE
                                    )
                                )
                                onNavigateBack()
                            }
                        },
                        enabled = canSave && !isCompressing,
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
                            text = if (isCompressing) "Optimizando..." else "Listo",
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
        ) {
            // Título (opcional)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = "Título de la Imagen",
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
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector y Preview con Coil de Imagen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(
                        width = 1.5.dp,
                        color = if (selectedImageUri != null) BoardsBlue else Color(0xFFE2E4E9),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { imagePickerLauncher.launch("image/*") },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedImageUri != null) Color(0xFFF0F4FF) else Color(0xFFF9FAFB)
            ) {
                if (selectedImageUri != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        coil.compose.AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Preview Imagen",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "📷 Cambiar foto",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = BoardsBlue,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isCompressing) "Optimizando con Compressor..." else "Elegir foto o imagen de la galería",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4A5060)
                        )
                        Text(
                            text = "Compresión inteligente automática para WhatsApp",
                            fontSize = 11.sp,
                            color = Color(0xFF9EACB9)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mensaje o pie de foto opcional
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Texto que acompaña a la imagen (opcional)") },
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

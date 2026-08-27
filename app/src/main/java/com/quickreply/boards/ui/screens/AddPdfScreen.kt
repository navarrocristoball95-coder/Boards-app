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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import com.quickreply.boards.ui.theme.BoardsBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPdfScreen(
    folderId: Long,
    initialReply: QuickReplyEntity? = null,
    onNavigateBack: () -> Unit,
    onSave: (QuickReplyEntity) -> Unit
) {
    var title by remember(initialReply?.id) { mutableStateOf(initialReply?.title ?: "") }
    var selectedPdfUri by remember(initialReply?.id) { mutableStateOf<Uri?>(initialReply?.mediaUri?.let { Uri.parse(it) }) }
    var note by remember(initialReply?.id) { mutableStateOf(initialReply?.content ?: "") }

    LaunchedEffect(initialReply) {
        if (initialReply != null) {
            title = initialReply.title
            note = initialReply.content
            selectedPdfUri = initialReply.mediaUri?.let { Uri.parse(it) }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localUri = com.quickreply.boards.data.local.FileStorageHelper.saveUriLocally(context, uri, "documents", "pdf")
            selectedPdfUri = localUri
            if (title.isEmpty()) {
                val lastPath = uri.lastPathSegment ?: "Documento.pdf"
                title = lastPath.substringAfterLast("/").replace(".pdf", "")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialReply == null) "Nuevo PDF" else "Editar PDF",
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
                    val canSave = selectedPdfUri != null || note.isNotBlank() || title.isNotBlank()
                    Button(
                        onClick = {
                            if (canSave) {
                                val cleanTitle = if (title.isNotBlank()) title.trim() else "Documento PDF"
                                val contentText = if (note.isNotBlank()) note.trim() else cleanTitle

                                onSave(
                                    initialReply?.copy(
                                        title = cleanTitle,
                                        content = contentText,
                                        mediaUri = selectedPdfUri?.toString(),
                                        contentType = ContentType.PDF,
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: QuickReplyEntity(
                                        folderId = folderId,
                                        title = cleanTitle,
                                        content = contentText,
                                        mediaUri = selectedPdfUri?.toString(),
                                        contentType = ContentType.PDF
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
        ) {
            // Título (opcional)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = "Título del Documento",
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

            // Selector de Archivo PDF
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(
                        width = 1.5.dp,
                        color = if (selectedPdfUri != null) Color(0xFFE53935) else Color(0xFFE2E4E9),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { pdfPickerLauncher.launch("application/pdf") },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedPdfUri != null) Color(0xFFFFEBEE) else Color(0xFFF9FAFB)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (selectedPdfUri != null) Icons.Default.Description else Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = if (selectedPdfUri != null) Color(0xFFE53935) else BoardsBlue,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedPdfUri != null) "PDF Seleccionado (Toca para cambiar)" else "Seleccionar archivo PDF del dispositivo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedPdfUri != null) Color(0xFFC62828) else Color(0xFF4A5060)
                    )
                    if (selectedPdfUri != null) {
                        Text(
                            text = selectedPdfUri?.lastPathSegment ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFF707684),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mensaje o descripción opcional
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Texto que acompaña al documento (opcional)") },
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

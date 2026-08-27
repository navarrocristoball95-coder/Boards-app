package com.quickreply.boards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

const val SEQUENCE_STEP_DELIMITER = "\n---PASO---\n"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSequenceScreen(
    folderId: Long,
    initialReply: QuickReplyEntity? = null,
    onNavigateBack: () -> Unit,
    onSave: (QuickReplyEntity) -> Unit
) {
    var title by remember(initialReply?.id) { mutableStateOf(initialReply?.title ?: "") }

    val stepsList = remember(initialReply?.id) {
        mutableStateListOf<String>().apply {
            if (initialReply != null && initialReply.content.isNotBlank()) {
                if (initialReply.content.contains(SEQUENCE_STEP_DELIMITER)) {
                    addAll(initialReply.content.split(SEQUENCE_STEP_DELIMITER))
                } else {
                    add(initialReply.content)
                }
            } else {
                addAll(
                    listOf(
                        "¡Hola {nombre}! Te comparto los detalles de nuestra propuesta 😊",
                        "Incluye soporte 24/7, acceso a todos los módulos y actualizaciones de por vida.",
                        "¿Te parece bien si coordinamos una llamada de 10 minutos para revisar dudas?"
                    )
                )
            }
        }
    }

    LaunchedEffect(initialReply) {
        if (initialReply != null) {
            title = initialReply.title
            stepsList.clear()
            if (initialReply.content.contains(SEQUENCE_STEP_DELIMITER)) {
                stepsList.addAll(initialReply.content.split(SEQUENCE_STEP_DELIMITER))
            } else {
                stepsList.add(initialReply.content)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialReply == null) "Nueva Secuencia de Mensajes" else "Editar Secuencia",
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
                    val canSave = stepsList.any { it.isNotBlank() }
                    Button(
                        onClick = {
                            if (canSave) {
                                val cleanTitle = if (title.isNotBlank()) title.trim() else "Secuencia de ${stepsList.count { it.isNotBlank() }} mensajes"
                                val validSteps = stepsList.filter { it.isNotBlank() }
                                val formattedContent = validSteps.joinToString(SEQUENCE_STEP_DELIMITER)

                                onSave(
                                    initialReply?.copy(
                                        title = cleanTitle,
                                        content = formattedContent,
                                        shortcut = "${validSteps.size} mensajes",
                                        contentType = ContentType.SEQUENCE,
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: QuickReplyEntity(
                                        folderId = folderId,
                                        title = cleanTitle,
                                        content = formattedContent,
                                        shortcut = "${validSteps.size} mensajes",
                                        contentType = ContentType.SEQUENCE
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
            // Título de la Secuencia
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = "Título (ej. Presentación de Propuesta)",
                        style = TextStyle(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5ABB7)
                        )
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C20)
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(BoardsBlue),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Explicación didáctica
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE0F2F1)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesomeMotion,
                        contentDescription = null,
                        tint = Color(0xFF00897B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Divide la información en mensajes cortos y naturales. En el teclado podrás enviarlos uno por uno con 1 toque para que no se vea como un texto automático masivo.",
                        fontSize = 12.sp,
                        color = Color(0xFF004D40),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lista dinámica de Mensajes Cortos
            stepsList.forEachIndexed { index, stepText ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4E9))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = BoardsBlue
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mensaje corto ${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF191C20)
                                )
                            }

                            if (stepsList.size > 1) {
                                IconButton(
                                    onClick = { stepsList.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar mensaje",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = stepText,
                            onValueChange = { newText -> stepsList[index] = newText },
                            placeholder = { Text("Escribe esta parte del mensaje...") },
                            minLines = 2,
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

            Spacer(modifier = Modifier.height(12.dp))

            // Botón para agregar otro mensaje
            OutlinedButton(
                onClick = { stepsList.add("") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = BoardsBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("＋ Agregar siguiente mensaje a la secuencia", fontWeight = FontWeight.SemiBold, color = BoardsBlue, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

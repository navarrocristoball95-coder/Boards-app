package com.quickreply.boards.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import com.quickreply.boards.ui.theme.BoardsBlue
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAudioScreen(
    folderId: Long,
    initialReply: QuickReplyEntity? = null,
    onNavigateBack: () -> Unit,
    onSave: (QuickReplyEntity) -> Unit
) {
    val context = LocalContext.current

    var title by remember(initialReply?.id) { mutableStateOf(initialReply?.title ?: "") }
    var note by remember(initialReply?.id) { mutableStateOf(initialReply?.content ?: "") }
    var audioUri by remember(initialReply?.id) { mutableStateOf<Uri?>(initialReply?.mediaUri?.let { Uri.parse(it) }) }

    LaunchedEffect(initialReply) {
        if (initialReply != null) {
            title = initialReply.title
            note = initialReply.content
            audioUri = initialReply.mediaUri?.let { Uri.parse(it) }
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentRecordedFile by remember { mutableStateOf<File?>(null) }

    // Temporizador de grabación
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSeconds = 0
            while (isRecording) {
                delay(1000L)
                recordingDurationSeconds++
            }
        }
    }

    // Limpieza de recursos al salir
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localUri = com.quickreply.boards.data.local.FileStorageHelper.saveUriLocally(context, uri, "audios", "audio")
            audioUri = localUri
            if (title.isEmpty()) {
                title = "Audio para cliente"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startRecording(
                context = context,
                onStarted = { recorder, file ->
                    mediaRecorder = recorder
                    currentRecordedFile = file
                    isRecording = true
                },
                onError = {
                    Toast.makeText(context, "Error al iniciar grabación", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Se requiere permiso de micrófono para grabar", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialReply == null) "Nuevo Audio" else "Editar Audio",
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
                    val canSave = audioUri != null || currentRecordedFile != null || title.isNotBlank()
                    Button(
                        onClick = {
                            if (canSave) {
                                val cleanTitle = if (title.isNotBlank()) title.trim() else "Audio de WhatsApp"
                                val finalUri = currentRecordedFile?.let {
                                    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it).toString()
                                } ?: audioUri?.toString()
                                val contentText = if (note.isNotBlank()) note.trim() else "Nota de voz / Audio"

                                onSave(
                                    initialReply?.copy(
                                        title = cleanTitle,
                                        content = contentText,
                                        mediaUri = finalUri,
                                        contentType = ContentType.AUDIO,
                                        updatedAt = System.currentTimeMillis()
                                    ) ?: QuickReplyEntity(
                                        folderId = folderId,
                                        title = cleanTitle,
                                        content = contentText,
                                        mediaUri = finalUri,
                                        contentType = ContentType.AUDIO
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
            // Título
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = "Título del Audio (ej. Saludo Inicial)",
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

            // Tarjeta Principal del Grabador / Reproductor de Audio
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = if (isRecording) Color(0xFFE53935) else if (audioUri != null || currentRecordedFile != null) Color(0xFF43A047) else Color(0xFFE2E4E9),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                color = if (isRecording) Color(0xFFFFF0F0) else if (audioUri != null || currentRecordedFile != null) Color(0xFFF1F8E9) else Color(0xFFF9FAFB)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isRecording) {
                        Text(
                            text = "Grabando nota de voz...",
                            fontSize = 14.sp,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatSeconds(recordingDurationSeconds),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Botón Detener Grabación
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable {
                                    try {
                                        mediaRecorder?.stop()
                                        mediaRecorder?.release()
                                        mediaRecorder = null
                                        isRecording = false
                                        if (title.isEmpty()) {
                                            title = "Nota de voz WhatsApp"
                                        }
                                        Toast.makeText(context, "Audio grabado con éxito", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        isRecording = false
                                    }
                                },
                            shape = CircleShape,
                            color = Color(0xFFE53935)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Detener",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    } else if (currentRecordedFile != null || audioUri != null) {
                        // Estado con audio listo para escuchar / eliminar
                        Text(
                            text = "🎙️ Audio Listo para WhatsApp",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Botón Play / Pause
                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        if (isPlaying) {
                                            mediaPlayer?.pause()
                                            isPlaying = false
                                        } else {
                                            try {
                                                mediaPlayer?.release()
                                                val uriToPlay = currentRecordedFile?.let { Uri.fromFile(it) } ?: audioUri!!
                                                mediaPlayer = MediaPlayer().apply {
                                                    setDataSource(context, uriToPlay)
                                                    prepare()
                                                    start()
                                                    setOnCompletionListener {
                                                        isPlaying = false
                                                    }
                                                }
                                                isPlaying = true
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "No se pudo reproducir el audio", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                shape = CircleShape,
                                color = BoardsBlue
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // Botón Borrar y Regrabar
                            Surface(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        currentRecordedFile?.delete()
                                        currentRecordedFile = null
                                        audioUri = null
                                        isPlaying = false
                                    },
                                shape = CircleShape,
                                color = Color(0xFFFFEBEE)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Estado Inicial: Botón Grabar con Micrófono
                        Surface(
                            modifier = Modifier
                                .size(64.dp)
                                .clickable {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        startRecording(
                                            context = context,
                                            onStarted = { recorder, file ->
                                                mediaRecorder = recorder
                                                currentRecordedFile = file
                                                isRecording = true
                                            },
                                            onError = {
                                                Toast.makeText(context, "Error al iniciar grabación", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                            shape = CircleShape,
                            color = BoardsBlue
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Grabar",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Toca para grabar nota de voz",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF191C20)
                        )
                        Text(
                            text = "Formato optimizado para WhatsApp y Telegram",
                            fontSize = 11.sp,
                            color = Color(0xFF707684)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Opción alternativa: Subir archivo de audio existente
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { audioFilePicker.launch("audio/*") }
                    .background(Color(0xFFF4F5F7))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = BoardsBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "O seleccionar archivo de audio del teléfono (.mp3, .m4a, .opus)",
                    fontSize = 12.sp,
                    color = Color(0xFF475467),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nota / Mensaje opcional
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Mensaje que acompaña al audio (opcional)") },
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

private fun startRecording(
    context: android.content.Context,
    onStarted: (MediaRecorder, File) -> Unit,
    onError: () -> Unit
) {
    try {
        val audioDir = File(context.filesDir, "audio_replies").apply { mkdirs() }
        val audioFile = File(audioDir, "audio_${System.currentTimeMillis()}.m4a")

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }
        onStarted(recorder, audioFile)
    } catch (e: Exception) {
        onError()
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

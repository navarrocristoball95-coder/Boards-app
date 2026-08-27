package com.quickreply.boards.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.data.local.KeyboardPreferences
import com.quickreply.boards.service.overlay.FloatingOverlayService
import com.quickreply.boards.ui.theme.BoardsBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardSetupScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val keyboardPrefs = remember { KeyboardPreferences(context) }

    var isHapticEnabled by remember { mutableStateOf(keyboardPrefs.hapticFeedbackEnabled) }
    var hapticIntensity by remember { mutableIntStateOf(keyboardPrefs.hapticIntensity) }
    var keyboardHeight by remember { mutableIntStateOf(keyboardPrefs.keyboardHeightDp) }
    var autoClipboard by remember { mutableStateOf(keyboardPrefs.autoCaptureClipboard) }
    var showShortcuts by remember { mutableStateOf(keyboardPrefs.showShortcutsBar) }
    var bubbleOpacity by remember { androidx.compose.runtime.mutableFloatStateOf(keyboardPrefs.bubbleOpacity) }
    val isBubbleActive by com.quickreply.boards.service.overlay.FloatingOverlayManager.isServiceActive.collectAsState()

    var testText by remember { mutableStateOf("") }

    // Detección del estado de activación
    var isImeEnabled by remember { mutableStateOf(false) }
    var isImeSelected by remember { mutableStateOf(false) }

    fun checkKeyboardStatus() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledMethods = imm.enabledInputMethodList
        val myPackage = context.packageName
        isImeEnabled = enabledMethods.any { it.packageName == myPackage }

        val defaultIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        isImeSelected = defaultIme?.contains(myPackage) == true
    }

    LaunchedEffect(Unit) {
        checkKeyboardStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configuración del Teclado",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C20)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF191C20),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // SECCIÓN 1: ESTADO Y ACTIVACIÓN
            SectionHeader(title = "ESTADO DEL SISTEMA", icon = Icons.Default.Settings)

            // Paso 1: Habilitar en Ajustes
            ActivationStepCard(
                stepNumber = 1,
                title = "Habilitar Teclado en Android",
                description = "Permite que el teclado aparezca en tu lista de métodos de entrada del sistema.",
                isCompleted = isImeEnabled,
                actionButtonText = if (isImeEnabled) "Configurado ✓" else "Habilitar en Ajustes",
                onClickAction = {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Paso 2: Seleccionar Teclado Activo
            ActivationStepCard(
                stepNumber = 2,
                title = "Seleccionar como Teclado Activo",
                description = "Activa el teclado para desplegar tus tableros en cualquier chat.",
                isCompleted = isImeSelected,
                actionButtonText = if (isImeSelected) "En Uso ✓" else "Cambiar Teclado",
                onClickAction = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Paso 3: Opción Pro Burbuja Flotante
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAECF0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = CircleShape,
                                color = if (isBubbleActive) Color(0xFFE8F5E9) else Color(0xFFEFF3FF)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = if (isBubbleActive) Color(0xFF2E7D32) else BoardsBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Burbuja Flotante en Pantalla", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF191C20))
                                Text(
                                    text = if (isBubbleActive) "● Activa en pantalla" else "○ Desactivada",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isBubbleActive) Color(0xFF2E7D32) else Color(0xFF707684)
                                )
                            }
                        }

                        Switch(
                            checked = isBubbleActive,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (!Settings.canDrawOverlays(context)) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        val serviceIntent = Intent(context, FloatingOverlayService::class.java)
                                        context.startService(serviceIntent)
                                        keyboardPrefs.bubbleEnabled = true
                                        Toast.makeText(context, "Burbuja flotante activada", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val serviceIntent = Intent(context, FloatingOverlayService::class.java)
                                    context.stopService(serviceIntent)
                                    com.quickreply.boards.service.overlay.FloatingOverlayManager.setServiceActive(false)
                                    keyboardPrefs.bubbleEnabled = false
                                    Toast.makeText(context, "Burbuja flotante desactivada", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2E7D32),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFD0D5DD)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botón explícito Activar / Desactivar
                    if (isBubbleActive) {
                        Button(
                            onClick = {
                                val serviceIntent = Intent(context, FloatingOverlayService::class.java)
                                context.stopService(serviceIntent)
                                com.quickreply.boards.service.overlay.FloatingOverlayManager.setServiceActive(false)
                                keyboardPrefs.bubbleEnabled = false
                                Toast.makeText(context, "Burbuja flotante desactivada", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFEE4E2),
                                contentColor = Color(0xFFD92D20)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Text("Desactivar Burbuja Flotante", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (!Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } else {
                                    val serviceIntent = Intent(context, FloatingOverlayService::class.java)
                                    context.startService(serviceIntent)
                                    keyboardPrefs.bubbleEnabled = true
                                    Toast.makeText(context, "Burbuja flotante activada", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BoardsBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Text("Activar Burbuja Flotante", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF2F4F7))

                    // Slider de Difuminación / Opacidad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Difuminación / Opacidad:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF191C20)
                        )
                        Text(
                            text = "${(bubbleOpacity * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BoardsBlue
                        )
                    }

                    androidx.compose.material3.Slider(
                        value = bubbleOpacity,
                        onValueChange = {
                            bubbleOpacity = it
                            keyboardPrefs.bubbleOpacity = it
                            com.quickreply.boards.service.overlay.FloatingOverlayManager.updateOpacity(it)
                            try {
                                val intent = Intent(context, FloatingOverlayService::class.java).apply {
                                    action = "ACTION_UPDATE_OPACITY"
                                    putExtra("opacity", it)
                                }
                                context.startService(intent)
                            } catch (_: Exception) {}
                        },
                        valueRange = 0.20f..1.0f,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = BoardsBlue,
                            activeTrackColor = BoardsBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Ajusta la transparencia para no tapar información de la pantalla mientras navegas en otras apps.",
                        fontSize = 10.sp,
                        color = Color(0xFF707684),
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECCIÓN 2: PERSONALIZACIÓN Y EXPERIENCIA
            SectionHeader(title = "PREFERENCIAS DEL TECLADO", icon = Icons.Default.Vibration)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAECF0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Vibración / Respuesta Háptica
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Respuesta Háptica (Vibración)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF191C20))
                            Text("Vibra suavemente al pulsar tarjetas y botones", fontSize = 11.sp, color = Color(0xFF707684))
                        }
                        Switch(
                            checked = isHapticEnabled,
                            onCheckedChange = {
                                isHapticEnabled = it
                                keyboardPrefs.hapticFeedbackEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BoardsBlue)
                        )
                    }

                    if (isHapticEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(10 to "Suave", 15 to "Normal", 25 to "Fuerte").forEach { (intensity, label) ->
                                val isSel = hapticIntensity == intensity
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        hapticIntensity = intensity
                                        keyboardPrefs.hapticIntensity = intensity
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEFF3FF),
                                        selectedLabelColor = BoardsBlue
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF2F4F7))

                    // Altura del Teclado
                    Column {
                        Text("Altura del Teclado", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF191C20))
                        Text("Ajusta el tamaño del panel en pantalla", fontSize = 11.sp, color = Color(0xFF707684))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(260 to "Compacto (260dp)", 290 to "Estándar (290dp)", 330 to "Alto (330dp)").forEach { (height, label) ->
                                val isSel = keyboardHeight == height
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        keyboardHeight = height
                                        keyboardPrefs.keyboardHeightDp = height
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEFF3FF),
                                        selectedLabelColor = BoardsBlue
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF2F4F7))

                    // Barra de Atajos Rápidos (/shortcuts)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Barra de Atajos Predictivos", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF191C20))
                            Text("Sugerencias de comandos /precio al tipear", fontSize = 11.sp, color = Color(0xFF707684))
                        }
                        Switch(
                            checked = showShortcuts,
                            onCheckedChange = {
                                showShortcuts = it
                                keyboardPrefs.showShortcutsBar = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BoardsBlue)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECCIÓN 3: PORTAPAPELES INTELIGENTE
            SectionHeader(title = "PORTAPAPELES INTELIGENTE", icon = Icons.Default.Assignment)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAECF0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Captura Automática de Copiado", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF191C20))
                            Text("Guarda textos y datos que copias en el móvil", fontSize = 11.sp, color = Color(0xFF707684))
                        }
                        Switch(
                            checked = autoClipboard,
                            onCheckedChange = {
                                autoClipboard = it
                                keyboardPrefs.autoCaptureClipboard = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BoardsBlue)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECCIÓN 4: ZONA DE PRUEBA EN VIVO
            SectionHeader(title = "ZONA DE PRUEBA EN VIVO", icon = Icons.Default.Keyboard)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAECF0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Toca el campo de abajo para probar tu teclado y enviar respuestas en tiempo real:",
                        fontSize = 12.sp,
                        color = Color(0xFF707684)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        placeholder = { Text("Escribe o envía una respuesta rápida aquí...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BoardsBlue,
                            unfocusedBorderColor = Color(0xFFE2E4E9)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BoardsBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475467),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ActivationStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    isCompleted: Boolean,
    actionButtonText: String,
    onClickAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isCompleted) Color(0xFFD1FADF) else Color(0xFFEAECF0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = if (isCompleted) Color(0xFFD1FADF) else Color(0xFFEFF3FF)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF12B76A), modifier = Modifier.size(16.dp))
                            } else {
                                Text(text = "$stepNumber", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BoardsBlue)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF191C20)
                    )
                }

                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFECFDF3)
                    ) {
                        Text(
                            text = "ACTIVO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF027A48),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF707684),
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClickAction,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) Color(0xFFF2F4F7) else BoardsBlue,
                    contentColor = if (isCompleted) Color(0xFF344054) else Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actionButtonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

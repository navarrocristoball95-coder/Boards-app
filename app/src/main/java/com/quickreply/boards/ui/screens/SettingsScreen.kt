package com.quickreply.boards.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.quickreply.boards.ui.HomeScreenViewModel
import com.quickreply.boards.ui.components.AuthDialog
import com.quickreply.boards.ui.theme.BoardsBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HomeScreenViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToKeyboardSetup: () -> Unit
) {
    val context = LocalContext.current
    val userSession by viewModel.userSession.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showAuthDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configuraciones",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C20)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF191C20)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ==========================================
            // SECCIÓN: USUARIO Y SESIÓN
            // ==========================================
            Text(
                text = "Usuario y Sesión",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (userSession != null) Color(0xFFECFDF5) else Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (userSession != null) Icons.Default.CloudDone else Icons.Default.AccountCircle,
                                contentDescription = "Avatar",
                                tint = if (userSession != null) Color(0xFF10B981) else BoardsBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (userSession != null) "Sesión Activa" else "Modo Local",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191C20)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userSession?.email ?: "Sin cuenta conectada",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (userSession != null) {
                        OutlinedButton(
                            onClick = { showLogoutConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEF4444)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Cerrar sesión",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cerrar Sesión", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = { showAuthDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BoardsBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Iniciar sesión",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Iniciar Sesión / Registrarse", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SECCIÓN: TECLADO RÁPIDO
            // ==========================================
            Text(
                text = "Teclado",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToKeyboardSetup() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF3FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Teclado",
                            tint = BoardsBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Configuración del Teclado",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF191C20)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Activar y seleccionar el teclado en Android",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ir",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SECCIÓN: SINCRONIZACIÓN Y NUBE
            // ==========================================
            Text(
                text = "Sincronización en Tiempo Real",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF10B981)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sync",
                                        tint = if (userSession != null) Color(0xFF10B981) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = if (isSyncing) "Sincronizando..." else "Sincronización en la Nube",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF191C20)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (userSession != null) "Sincronización bidireccional activa" else "Inicia sesión para sincronizar",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (userSession == null) {
                                    showAuthDialog = true
                                } else {
                                    viewModel.syncWithCloud { success, msg ->
                                        if (success) {
                                            Toast.makeText(context, "Sincronización completada", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (userSession != null) Color(0xFF10B981) else BoardsBlue),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isSyncing) "..." else "Sincronizar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SECCIÓN: ACERCA DE
            // ==========================================
            Text(
                text = "Información",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Versión de la App",
                            fontSize = 14.sp,
                            color = Color(0xFF191C20)
                        )
                        Text(
                            text = "v1.2.0 HD",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Estado del Servicio",
                            fontSize = 14.sp,
                            color = Color(0xFF191C20)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Online",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Operativo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Autenticación
    if (showAuthDialog) {
        AuthDialog(
            currentSession = userSession,
            onDismiss = { showAuthDialog = false },
            onLogin = { email, pass, cb ->
                viewModel.login(email, pass) { ok, msg ->
                    cb(ok, msg)
                    if (ok) showAuthDialog = false
                }
            },
            onSignUp = { email, pass, cb ->
                viewModel.signUp(email, pass) { ok, msg ->
                    cb(ok, msg)
                    if (ok) showAuthDialog = false
                }
            },
            onSignOut = {
                viewModel.signOut()
                showAuthDialog = false
            }
        )
    }

    // Diálogo de Confirmación para Cerrar Sesión
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = "Cerrar Sesión",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas desconectar tu cuenta (${userSession?.email ?: ""}) de este dispositivo?",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signOut()
                        showLogoutConfirmDialog = false
                        Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            }
        )
    }
}

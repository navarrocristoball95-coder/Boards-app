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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quickreply.boards.data.sync.UserSession
import com.quickreply.boards.ui.theme.BoardsBlue

@Composable
fun AuthDialog(
    currentSession: UserSession?,
    onDismiss: () -> Unit,
    onLogin: (String, String, (Boolean, String) -> Unit) -> Unit,
    onSignUp: (String, String, (Boolean, String) -> Unit) -> Unit,
    onResetPassword: ((String, (Boolean, String) -> Unit) -> Unit)? = null,
    onSignOut: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var isForgotPassword by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = BoardsBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentSession != null) "Tu Cuenta Cloud"
                            else if (isForgotPassword) "Recuperar Clave"
                            else if (isSignUp) "Crear Cuenta"
                            else "Iniciar Sesión",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF191C20)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isForgotPassword) "Te enviaremos un enlace a tu correo para restablecer tu clave."
                    else "Sincroniza tus tableros y respuestas con tu PC y la Web en tiempo real.",
                    fontSize = 13.sp,
                    color = Color(0xFF707684)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (currentSession != null) {
                    // Vista de Usuario Autenticado
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFECFDF5), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Conectado como:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = currentSession.email,
                                    fontSize = 14.sp,
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onSignOut()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar Sesión en este Teléfono", fontWeight = FontWeight.Bold)
                    }
                } else if (isForgotPassword) {
                    // Vista de Recuperación de Contraseña
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Correo Electrónico") },
                        placeholder = { Text("tu@correo.com") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Mail, contentDescription = null, tint = Color(0xFF94A3B8))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (successMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = successMessage ?: "",
                            color = Color(0xFF059669),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (email.isBlank()) {
                                errorMessage = "Ingresa tu correo electrónico"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            successMessage = null
                            onResetPassword?.invoke(email) { ok, msg ->
                                isLoading = false
                                if (ok) {
                                    successMessage = "¡Enlace enviado! Revisa tu correo (incluida la carpeta spam)."
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = BoardsBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Enviar Enlace de Recuperación",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            isForgotPassword = false
                            errorMessage = null
                            successMessage = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = BoardsBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Volver a Iniciar Sesión",
                                color = BoardsBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    // Toggle Iniciar Sesión / Registrarse
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (!isSignUp) Color.White else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    isSignUp = false
                                    errorMessage = null
                                    successMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Iniciar Sesión",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (!isSignUp) BoardsBlue else Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSignUp) Color.White else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    isSignUp = true
                                    errorMessage = null
                                    successMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Registrarse",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSignUp) BoardsBlue else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Correo Electrónico") },
                        placeholder = { Text("tu@correo.com") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Mail, contentDescription = null, tint = Color(0xFF94A3B8))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Contraseña") },
                        placeholder = { Text("Mínimo 6 caracteres") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Mostrar contraseña",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isSignUp) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "¿Olvidaste tu contraseña?",
                                color = BoardsBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    isForgotPassword = true
                                    errorMessage = null
                                    successMessage = null
                                }
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (successMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = successMessage ?: "",
                            color = Color(0xFF059669),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Ingresa tu correo y contraseña"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            if (isSignUp) {
                                onSignUp(email, password) { ok, msg ->
                                    isLoading = false
                                    if (ok) {
                                        successMessage = "¡Cuenta creada! Sincronizando..."
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            } else {
                                onLogin(email, password) { ok, msg ->
                                    isLoading = false
                                    if (ok) {
                                        successMessage = "¡Sesión iniciada con éxito!"
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = BoardsBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "Registrarme y Sincronizar" else "Entrar y Sincronizar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

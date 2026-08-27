package com.quickreply.boards.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickreply.boards.ui.theme.BoardsBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContentCategoryBottomSheet(
    onDismiss: () -> Unit,
    onSelectText: () -> Unit,
    onSelectAudio: () -> Unit,
    onSelectSequence: () -> Unit,
    onSelectLocation: () -> Unit,
    onSelectContact: () -> Unit,
    onSelectLink: () -> Unit,
    onSelectPdf: () -> Unit,
    onSelectImage: () -> Unit,
    onSelectFolder: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp, top = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Agregar a este Board",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF191C20),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            CategoryOptionItem(
                icon = Icons.Default.TextFields,
                iconColor = BoardsBlue,
                bgColor = Color(0xFFEFF3FF),
                title = "Texto",
                subtitle = "Mensajes, saludos y respuestas dinámicas con fórmulas",
                onClick = {
                    onDismiss()
                    onSelectText()
                }
            )

            CategoryOptionItem(
                icon = Icons.Default.Mic,
                iconColor = Color(0xFFE53935),
                bgColor = Color(0xFFFFEBEE),
                title = "Audio / Nota de Voz",
                subtitle = "Graba audios listos para enviar por WhatsApp",
                onClick = {
                    onDismiss()
                    onSelectAudio()
                }
            )

            CategoryOptionItem(
                icon = Icons.Default.AutoAwesomeMotion,
                iconColor = Color(0xFF00897B),
                bgColor = Color(0xFFE0F2F1),
                title = "Secuencia de Ventas",
                subtitle = "Pasos guiados de seguimiento (Paso 1, 2, 3)",
                onClick = {
                    onDismiss()
                    onSelectSequence()
                }
            )

            CategoryOptionItem(
                icon = Icons.Default.LocationOn,
                iconColor = Color(0xFFE65100),
                bgColor = Color(0xFFFFF3E0),
                title = "Ubicación / Sucursal",
                subtitle = "Oficinas y tiendas con enlace a Google Maps y Waze",
                onClick = {
                    onDismiss()
                    onSelectLocation()
                }
            )

            CategoryOptionItem(
                icon = Icons.Default.ContactPhone,
                iconColor = Color(0xFF3949AB),
                bgColor = Color(0xFFE8EAF6),
                title = "Contacto / VCard",
                subtitle = "Comparte datos de contacto telefónico y empresa",
                onClick = {
                    onDismiss()
                    onSelectContact()
                }
            )

            CategoryOptionItem(
                icon = Icons.Default.Link,
                iconColor = Color(0xFF00ACC1),
                bgColor = Color(0xFFE0F7FA),
                title = "Enlace",
                subtitle = "Páginas web con Smart Tracking o calendarios",
                onClick = {
                    onDismiss()
                    onSelectLink()
                }
            )

            CategoryOptionItem(
                icon = Icons.Default.Description,
                iconColor = Color(0xFFFB8C00),
                bgColor = Color(0xFFFFF3E0),
                title = "PDF / Documento",
                subtitle = "Catálogos, listas de precios o contratos",
                onClick = {
                    onDismiss()
                    onSelectPdf()
                }
            )

            CategoryOptionItem(
                icon = Icons.Default.Image,
                iconColor = Color(0xFF8E24AA),
                bgColor = Color(0xFFF3E5F5),
                title = "Imágenes / Fotos",
                subtitle = "Fotos de productos, comprobantes o banners",
                onClick = {
                    onDismiss()
                    onSelectImage()
                }
            )

            CategoryOptionItem(
                icon = Icons.Default.Folder,
                iconColor = Color(0xFF546E7A),
                bgColor = Color(0xFFECEFF1),
                title = "Carpeta",
                subtitle = "Subcategoría para organizar respuestas",
                onClick = {
                    onDismiss()
                    onSelectFolder()
                }
            )
        }
    }
}

@Composable
private fun CategoryOptionItem(
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = bgColor
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .padding(10.dp)
                    .size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF191C20)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF707684)
            )
        }
    }
}

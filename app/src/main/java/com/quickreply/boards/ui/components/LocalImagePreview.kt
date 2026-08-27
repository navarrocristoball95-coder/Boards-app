package com.quickreply.boards.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

@Composable
fun LocalImagePreview(
    uriString: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, uriString) {
        value = try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    bitmap?.let { b ->
        Image(
            bitmap = b.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier.fillMaxSize(),
            contentScale = contentScale
        )
    }
}

package com.quickreply.boards.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FileStorageHelper {

    /**
     * Copia y comprime automáticamente imágenes utilizando Compressor
     * reduciendo peso a <250KB manteniendo nitidez para envíos rápidos por WhatsApp.
     */
    suspend fun compressAndSaveImageLocally(context: Context, sourceUri: Uri): Uri = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(sourceUri)
            if (mimeType?.contains("gif") == true) {
                // Preservar formato y animación en GIFs sin compresión destructiva JPEG
                return@withContext saveUriLocally(context, sourceUri, "images", "gif")
            }

            val dir = File(context.filesDir, "images")
            if (!dir.exists()) dir.mkdirs()

            // 1. Guardar temporal sin comprimir
            val tempFile = File(dir, "temp_raw_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 2. Comprimir con Compressor
            val compressedFile = try {
                Compressor.compress(context, tempFile) {
                    resolution(1280, 1280)
                    quality(80)
                }
            } catch (_: Exception) {
                tempFile
            }

            // 3. Archivo final optimizado
            val finalFile = File(dir, "img_${System.currentTimeMillis()}.jpg")
            compressedFile.copyTo(finalFile, overwrite = true)
            tempFile.delete()
            if (compressedFile.absolutePath != finalFile.absolutePath) {
                compressedFile.delete()
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", finalFile)
        } catch (_: Exception) {
            saveUriLocally(context, sourceUri, "images", "img")
        }
    }

    /**
     * Copia de forma segura un URI externo de Galería o Descargas a la memoria interna de la app
     * y genera una URI segura con FileProvider compatible con WhatsApp / Telegram.
     */
    fun saveUriLocally(context: Context, sourceUri: Uri, subDir: String, filePrefix: String): Uri {
        try {
            val dir = File(context.filesDir, subDir)
            if (!dir.exists()) dir.mkdirs()

            val mimeType = context.contentResolver.getType(sourceUri)
            val extension = when {
                mimeType?.contains("pdf") == true -> "pdf"
                mimeType?.contains("png") == true -> "png"
                mimeType?.contains("jpeg") == true || mimeType?.contains("jpg") == true -> "jpg"
                mimeType?.contains("mp4") == true -> "mp4"
                mimeType?.contains("ogg") == true || mimeType?.contains("opus") == true -> "opus"
                else -> sourceUri.lastPathSegment?.substringAfterLast(".", "dat") ?: "dat"
            }

            val destFile = File(dir, "${filePrefix}_${System.currentTimeMillis()}.$extension")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
        } catch (_: Exception) {
            return sourceUri
        }
    }
}

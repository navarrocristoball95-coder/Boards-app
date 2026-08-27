package com.quickreply.boards.ui.components

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object VCardHelper {

    /**
     * Genera un archivo vCard 3.0 estándar (.vcf) y retorna la URI segura de FileProvider
     */
    fun createVCardFile(
        context: Context,
        fullName: String,
        phone: String,
        email: String? = null,
        organization: String? = null,
        jobTitle: String? = null,
        notes: String? = null
    ): android.net.Uri? {
        try {
            val vcfDir = File(context.filesDir, "vcards")
            if (!vcfDir.exists()) vcfDir.mkdirs()

            val sanitizedName = fullName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val vcfFile = File(vcfDir, "contact_${sanitizedName}_${System.currentTimeMillis()}.vcf")

            val nameParts = fullName.trim().split(" ")
            val lastName = if (nameParts.size > 1) nameParts.last() else ""
            val firstName = if (nameParts.size > 1) nameParts.dropLast(1).joinToString(" ") else fullName

            val vcfContent = buildString {
                append("BEGIN:VCARD\r\n")
                append("VERSION:3.0\r\n")
                append("N:$lastName;$firstName;;;\r\n")
                append("FN:$fullName\r\n")
                if (!organization.isNullOrBlank()) append("ORG:$organization\r\n")
                if (!jobTitle.isNullOrBlank()) append("TITLE:$jobTitle\r\n")
                if (!phone.isNullOrBlank()) append("TEL;TYPE=CELL,VOICE:$phone\r\n")
                if (!email.isNullOrBlank()) append("EMAIL;TYPE=INTERNET,HOME:$email\r\n")
                if (!notes.isNullOrBlank()) append("NOTE:$notes\r\n")
                append("END:VCARD\r\n")
            }

            FileOutputStream(vcfFile).use { it.write(vcfContent.toByteArray()) }

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", vcfFile)
        } catch (_: Exception) {
            return null
        }
    }
}

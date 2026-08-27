package com.quickreply.boards.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration

enum class WhatsAppListType {
    BULLET,
    NUMBERED,
    QUOTE
}

object WhatsAppMarkdownHelper {

    /**
     * Formatea un mensaje con enlace unificado para WhatsApp, Telegram y apps de mensajería:
     * [Texto de acompañamiento] [Nombre corto/Texto visible](URL)
     * Ejemplo: "Hola David, te envío el enlace: Enlace (https://calendar.app.google/...)"
     */
    fun formatLinkMessage(accompanyingText: String?, shortName: String?, url: String?): String {
        var extractedUrl = url?.trim().orEmpty()
        val rawText = accompanyingText?.trim().orEmpty()

        if (extractedUrl.isBlank() && rawText.isNotBlank()) {
            val urlMatch = Regex("""https?://[^\s)]+""").find(rawText)
            if (urlMatch != null) {
                extractedUrl = urlMatch.value
            }
        }

        if (extractedUrl.isBlank() && rawText.isBlank()) {
            return ""
        }

        // Limpiar cualquier residuo de [Nombre](URL) o Nombre (URL)
        var cleanAccompanying = rawText
            .replace(Regex("""\[?([^\]\n]+)\]?\s*\((https?://[^\)]+)\)"""), "$1")

        if (extractedUrl.isNotBlank()) {
            cleanAccompanying = cleanAccompanying.replace(extractedUrl, "")
        }

        // Remover nombre de atajo o texto residual si quedó concatenado al final
        if (!shortName.isNullOrBlank()) {
            cleanAccompanying = cleanAccompanying.replace(Regex("""\s*${Regex.escape(shortName.trim())}\s*$""", RegexOption.IGNORE_CASE), "")
        }

        cleanAccompanying = cleanAccompanying.trim()

        if (cleanAccompanying.isNotBlank() && extractedUrl.isNotBlank()) {
            return "$cleanAccompanying\n$extractedUrl"
        } else if (extractedUrl.isNotBlank()) {
            return extractedUrl
        } else {
            return cleanAccompanying
        }
    }

    /**
     * Aplica formato Markdown compatible al 100% con WhatsApp:
     * - Si hay texto seleccionado: aísla los espacios en blanco iniciales/finales para no romper las reglas de WhatsApp.
     * - Toggle off: Si el texto ya tiene el formato, lo remueve.
     * - Si no hay selección: Inserta los delimitadores y coloca el cursor en medio (ej. *|*).
     */
    fun applyFormat(textFieldValue: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
        val text = textFieldValue.text
        val selection = textFieldValue.selection

        if (selection.min < selection.max) {
            val start = selection.min
            val end = selection.max
            val selectedText = text.substring(start, end)

            val leadingWhitespace = selectedText.takeWhile { it.isWhitespace() }
            val trailingWhitespace = selectedText.takeLastWhile { it.isWhitespace() }
            val coreText = selectedText.substring(
                leadingWhitespace.length,
                selectedText.length - trailingWhitespace.length
            )

            if (coreText.isEmpty()) {
                val newText = text.substring(0, start) + prefix + suffix + text.substring(end)
                val newCursor = start + prefix.length
                return TextFieldValue(newText, TextRange(newCursor))
            }

            // Verificar si ya está formateado para hacer Toggle Off (quitar formato)
            val isAlreadyFormatted = coreText.startsWith(prefix) && coreText.endsWith(suffix) && coreText.length >= prefix.length + suffix.length
            val formattedCore = if (isAlreadyFormatted) {
                coreText.substring(prefix.length, coreText.length - suffix.length)
            } else {
                prefix + coreText + suffix
            }

            val replacement = leadingWhitespace + formattedCore + trailingWhitespace
            val newText = text.substring(0, start) + replacement + text.substring(end)
            val newSelection = TextRange(start + leadingWhitespace.length, start + leadingWhitespace.length + formattedCore.length)
            return TextFieldValue(newText, newSelection)
        } else {
            // Sin selección: insertar delimitadores en la posición del cursor
            val cursor = selection.start.coerceIn(0, text.length)
            val newText = text.substring(0, cursor) + prefix + suffix + text.substring(cursor)
            val newCursor = cursor + prefix.length
            return TextFieldValue(newText, TextRange(newCursor))
        }
    }

    /**
     * Aplica listas o citas de WhatsApp por línea
     */
    fun applyList(textFieldValue: TextFieldValue, type: WhatsAppListType): TextFieldValue {
        val text = textFieldValue.text
        val selection = textFieldValue.selection

        if (text.isEmpty()) {
            val prefix = when (type) {
                WhatsAppListType.BULLET -> "• "
                WhatsAppListType.NUMBERED -> "1. "
                WhatsAppListType.QUOTE -> "> "
            }
            return TextFieldValue(prefix, TextRange(prefix.length))
        }

        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)

        // Encontrar inicio de la primera línea y fin de la última línea seleccionada
        val lineStart = if (start <= 0) 0 else {
            val prevNl = text.lastIndexOf('\n', start - 1)
            if (prevNl == -1) 0 else prevNl + 1
        }
        val lineEnd = text.indexOf('\n', end).let { if (it == -1) text.length else it }

        val targetBlock = text.substring(lineStart, lineEnd)
        val lines = targetBlock.split("\n")

        var numberIndex = 1
        val newLines = lines.map { line ->
            when (type) {
                WhatsAppListType.BULLET -> {
                    if (line.startsWith("• ") || line.startsWith("- ")) {
                        line.substring(2)
                    } else {
                        "• $line"
                    }
                }
                WhatsAppListType.NUMBERED -> {
                    val regex = Regex("^\\d+\\.\\s*")
                    if (regex.containsMatchIn(line)) {
                        line.replace(regex, "")
                    } else {
                        val prefix = "${numberIndex++}. "
                        prefix + line
                    }
                }
                WhatsAppListType.QUOTE -> {
                    if (line.startsWith("> ")) {
                        line.substring(2)
                    } else {
                        "> $line"
                    }
                }
            }
        }

        val formattedBlock = newLines.joinToString("\n")
        val newText = text.substring(0, lineStart) + formattedBlock + text.substring(lineEnd)
        val newSelection = TextRange(lineStart, lineStart + formattedBlock.length)
        return TextFieldValue(newText, newSelection)
    }

    /**
     * Parsea texto con Markdown de WhatsApp a un AnnotatedString para previsualización visual real
     */
    fun parseToAnnotatedString(text: String): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        return buildAnnotatedString {
            var i = 0
            val len = text.length

            while (i < len) {
                val c = text[i]

                // Monoespacio ```texto```
                if (c == '`' && i + 2 < len && text[i + 1] == '`' && text[i + 2] == '`') {
                    val closeIndex = text.indexOf("```", i + 3)
                    if (closeIndex != -1 && closeIndex > i + 3) {
                        val content = text.substring(i + 3, closeIndex)
                        val startPos = length
                        append(content)
                        addStyle(
                            SpanStyle(fontFamily = FontFamily.Monospace),
                            startPos,
                            length
                        )
                        i = closeIndex + 3
                        continue
                    }
                }

                // Negrita *texto*
                if (c == '*' && i + 1 < len && !text[i + 1].isWhitespace()) {
                    val closeIndex = text.indexOf('*', i + 1)
                    if (closeIndex > i + 1 && !text[closeIndex - 1].isWhitespace()) {
                        val content = text.substring(i + 1, closeIndex)
                        val startPos = length
                        append(content)
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            startPos,
                            length
                        )
                        i = closeIndex + 1
                        continue
                    }
                }

                // Cursiva _texto_
                if (c == '_' && i + 1 < len && !text[i + 1].isWhitespace()) {
                    val closeIndex = text.indexOf('_', i + 1)
                    if (closeIndex > i + 1 && !text[closeIndex - 1].isWhitespace()) {
                        val content = text.substring(i + 1, closeIndex)
                        val startPos = length
                        append(content)
                        addStyle(
                            SpanStyle(fontStyle = FontStyle.Italic),
                            startPos,
                            length
                        )
                        i = closeIndex + 1
                        continue
                    }
                }

                // Tachado ~texto~
                if (c == '~' && i + 1 < len && !text[i + 1].isWhitespace()) {
                    val closeIndex = text.indexOf('~', i + 1)
                    if (closeIndex > i + 1 && !text[closeIndex - 1].isWhitespace()) {
                        val content = text.substring(i + 1, closeIndex)
                        val startPos = length
                        append(content)
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.LineThrough),
                            startPos,
                            length
                        )
                        i = closeIndex + 1
                        continue
                    }
                }

                append(c)
                i++
            }
        }
    }
}

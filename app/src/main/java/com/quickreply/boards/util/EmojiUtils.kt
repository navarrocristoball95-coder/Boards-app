package com.quickreply.boards.util

object EmojiUtils {

    /**
     * Accurately parses a board or folder name into an (emoji, cleanTitle) pair.
     * Uses Unicode code point inspection to verify if the leading characters are truly an emoji.
     * If no emoji is present, returns defaultEmoji and the full original trimmed name.
     */
    fun parseEmojiAndTitle(rawName: String, defaultEmoji: String = "📋"): Pair<String, String> {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return Pair(defaultEmoji, "")

        // Check first Unicode code point
        val firstCodePoint = trimmed.codePointAt(0)
        val charCount = Character.charCount(firstCodePoint)
        val type = Character.getType(firstCodePoint)

        val isEmoji = firstCodePoint in 0x1F000..0x1FAFF ||  // Misc Symbols, Pictographs, Emoticons, Transport
                      firstCodePoint in 0x2600..0x27BF ||    // Misc Symbols & Dingbats
                      firstCodePoint in 0x2300..0x23FF ||    // Misc Technical (watch, hourglass, etc)
                      firstCodePoint in 0x2B50..0x2B55 ||    // Star and other symbols
                      type == Character.OTHER_SYMBOL.toInt() ||
                      type == Character.SURROGATE.toInt()

        if (isEmoji) {
            // Include possible variation selectors or modifiers (like skin tone or zero-width joiners)
            var totalEmojiChars = charCount
            while (totalEmojiChars < trimmed.length) {
                val nextCodePoint = trimmed.codePointAt(totalEmojiChars)
                if (nextCodePoint == 0xFE0F || nextCodePoint == 0xFE0E || nextCodePoint in 0x1F3FB..0x1F3FF || nextCodePoint == 0x200D) {
                    totalEmojiChars += Character.charCount(nextCodePoint)
                    if (nextCodePoint == 0x200D && totalEmojiChars < trimmed.length) {
                        // ZWJ followed by another emoji component
                        val joinedCodePoint = trimmed.codePointAt(totalEmojiChars)
                        totalEmojiChars += Character.charCount(joinedCodePoint)
                    }
                } else {
                    break
                }
            }

            val emoji = trimmed.substring(0, totalEmojiChars)
            val rest = trimmed.substring(totalEmojiChars).trimStart()
            return Pair(emoji, if (rest.isNotEmpty()) rest else trimmed)
        }

        return Pair(defaultEmoji, trimmed)
    }
}

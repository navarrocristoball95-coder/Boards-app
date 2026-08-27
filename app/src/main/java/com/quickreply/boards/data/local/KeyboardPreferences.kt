package com.quickreply.boards.data.local

import android.content.Context
import android.content.SharedPreferences

class KeyboardPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("quickreply_keyboard_prefs", Context.MODE_PRIVATE)

    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean("haptic_feedback_enabled", true)
        set(value) = prefs.edit().putBoolean("haptic_feedback_enabled", value).apply()

    var hapticIntensity: Int
        get() = prefs.getInt("haptic_intensity", 15) // 10ms (suave), 15ms (normal), 25ms (fuerte)
        set(value) = prefs.edit().putInt("haptic_intensity", value).apply()

    var keyboardHeightDp: Int
        get() = prefs.getInt("keyboard_height_dp", 360) // 340, 360, 390
        set(value) = prefs.edit().putInt("keyboard_height_dp", value).apply()

    var autoCaptureClipboard: Boolean
        get() = prefs.getBoolean("auto_capture_clipboard", true)
        set(value) = prefs.edit().putBoolean("auto_capture_clipboard", value).apply()

    var showShortcutsBar: Boolean
        get() = prefs.getBoolean("show_shortcuts_bar", true)
        set(value) = prefs.edit().putBoolean("show_shortcuts_bar", value).apply()

    var directSendWithoutPreview: Boolean
        get() = prefs.getBoolean("direct_send_without_preview", true)
        set(value) = prefs.edit().putBoolean("direct_send_without_preview", value).apply()

    var bubbleOpacity: Float
        get() = prefs.getFloat("bubble_opacity", 0.85f) // 0.20f a 1.0f
        set(value) = prefs.edit().putFloat("bubble_opacity", value).apply()

    var bubbleEnabled: Boolean
        get() = prefs.getBoolean("bubble_enabled", false)
        set(value) = prefs.edit().putBoolean("bubble_enabled", value).apply()

    fun getUsedNamesHistory(): List<String> {
        val raw = prefs.getString("used_names_history", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|||").filter { it.isNotBlank() }
    }

    fun saveUsedName(name: String) {
        val trimmed = name.trim()
        if (trimmed.length < 2) return
        val current = getUsedNamesHistory().toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val capped = current.take(30).joinToString("|||")
        prefs.edit().putString("used_names_history", capped).apply()
    }
}

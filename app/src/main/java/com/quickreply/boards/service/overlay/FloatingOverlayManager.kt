package com.quickreply.boards.service.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FloatingOverlayManager {
    private val _opacityFlow = MutableStateFlow(0.85f)
    val opacityFlow: StateFlow<Float> = _opacityFlow.asStateFlow()

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    fun updateOpacity(opacity: Float) {
        _opacityFlow.value = opacity.coerceIn(0.20f, 1.0f)
    }

    fun setServiceActive(active: Boolean) {
        _isServiceActive.value = active
    }
}

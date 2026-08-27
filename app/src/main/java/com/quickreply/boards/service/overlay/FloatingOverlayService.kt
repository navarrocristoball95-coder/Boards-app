package com.quickreply.boards.service.overlay

import android.animation.ValueAnimator
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.quickreply.boards.data.local.AppDatabase
import com.quickreply.boards.data.local.KeyboardPreferences
import com.quickreply.boards.data.repository.QuickReplyRepository
import com.quickreply.boards.service.keyboard.ui.KeyboardView
import com.quickreply.boards.ui.theme.BoardsBlue
import com.quickreply.boards.ui.theme.QuickReplyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloatingOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: QuickReplyRepository
    private lateinit var keyboardPrefs: KeyboardPreferences
    private var vibrator: Vibrator? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowParams: WindowManager.LayoutParams? = null

    // Estado reactivo de expansión
    private var isExpandedState by mutableStateOf(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_UPDATE_OPACITY") {
            val newOpacity = intent.getFloatExtra("opacity", keyboardPrefs.bubbleOpacity).coerceIn(0.20f, 1.0f)
            FloatingOverlayManager.updateOpacity(newOpacity)
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        keyboardPrefs = KeyboardPreferences(this)
        val database = AppDatabase.getDatabase(this, serviceScope)
        repository = QuickReplyRepository(database.folderDao(), database.quickReplyDao(), database.clipboardDao())

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        FloatingOverlayManager.updateOpacity(keyboardPrefs.bubbleOpacity)
        FloatingOverlayManager.setServiceActive(true)

        serviceScope.launch(Dispatchers.IO) {
            repository.seedDefaultDataIfEmpty()
        }

        createFloatingOverlayView()
    }

    private fun updateWindowPosition(deltaX: Float, deltaY: Float) {
        val params = windowParams ?: return
        val view = overlayView ?: return

        val screenSize = Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(screenSize)
        val screenWidth = screenSize.x
        val screenHeight = screenSize.y

        val viewW = if (view.width > 0) view.width else 340
        val viewH = if (view.height > 0) view.height else 300
        val maxX = (screenWidth - viewW).coerceAtLeast(0)
        val maxY = (screenHeight - viewH - 40).coerceAtLeast(100)

        params.x = (params.x + deltaX.toInt()).coerceIn(0, maxX)
        params.y = (params.y + deltaY.toInt()).coerceIn(50, maxY)
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun snapBubbleToEdge() {
        val params = windowParams ?: return
        val view = overlayView ?: return

        val screenSize = Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(screenSize)
        val screenWidth = screenSize.x

        val targetX = if (params.x + 28 < screenWidth / 2) 20 else screenWidth - 170
        val startX = params.x
        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
        }
        animator.start()
    }

    private fun createFloatingOverlayView() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val initialOpacity = keyboardPrefs.bubbleOpacity.coerceIn(0.20f, 1.0f)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
            alpha = initialOpacity
        }
        windowParams = params

        val composeView = ComposeView(this)
        overlayView = composeView
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycleRegistry))
        composeView.setContent {
            QuickReplyTheme {
                val liveOpacity by FloatingOverlayManager.opacityFlow.collectAsState()
                var showOpacitySlider by remember { mutableStateOf(false) }
                var copiedNotificationText by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(copiedNotificationText) {
                    if (copiedNotificationText != null) {
                        delay(2200)
                        copiedNotificationText = null
                    }
                }

                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = liveOpacity
                    }
                ) {
                    if (!isExpandedState) {
                        // 1. BURBUJA FLOTANTE COMPACTA (Toque instantáneo para abrir y arrastre fluido con imantación)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(BoardsBlue)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown()
                                        var isDrag = false
                                        var totalDrag = Offset.Zero
                                        val pointerId = down.id

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                            if (change.changedToUp()) {
                                                if (!isDrag) {
                                                    performHaptic()
                                                    isExpandedState = true
                                                } else {
                                                    snapBubbleToEdge()
                                                }
                                                break
                                            }

                                            val drag = change.position - change.previousPosition
                                            totalDrag += drag
                                            if (!isDrag && totalDrag.getDistance() > 8f) {
                                                isDrag = true
                                            }

                                            if (isDrag) {
                                                change.consume()
                                                updateWindowPosition(drag.x, drag.y)
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Abrir Boards",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        // 2. VENTANA FLOTANTE EXPANDIDA (Paridad Total con Teclado, Arrastre Superior y Clics 100% Fluidos)
                        Surface(
                            modifier = Modifier
                                .width(340.dp)
                                .height((keyboardPrefs.keyboardHeightDp + 48).dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            tonalElevation = 12.dp,
                            shadowElevation = 16.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAECF0))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Barra Superior Header: Arrastre, Ajuste de Opacidad en Vivo y Botones
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .background(Color(0xFFF9FAFB))
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .pointerInput(Unit) {
                                                detectDragGestures(
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        updateWindowPosition(dragAmount.x, dragAmount.y)
                                                    }
                                                )
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Arrastrar ventana",
                                            tint = Color(0xFF8C9199),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Boards Flotante",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF191C20)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Botón para ajustar difuminación en vivo
                                        IconButton(
                                            onClick = {
                                                performHaptic()
                                                showOpacitySlider = !showOpacitySlider
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Opacity,
                                                contentDescription = "Ajustar Difuminación",
                                                tint = if (showOpacitySlider) BoardsBlue else Color(0xFF5F6570),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // Minimizar a burbuja
                                        IconButton(
                                            onClick = {
                                                performHaptic()
                                                isExpandedState = false
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Minimizar",
                                                tint = Color(0xFF5F6570),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Cerrar servicio
                                        IconButton(
                                            onClick = {
                                                performHaptic()
                                                stopSelf()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cerrar",
                                                tint = Color(0xFFE53935),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Barra Rápida de Difuminación / Opacidad en Tiempo Real (Toggleable)
                                if (showOpacitySlider) {
                                    Surface(
                                        color = Color(0xFFEFF3FF),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Opacidad: ${(liveOpacity * 100).toInt()}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BoardsBlue,
                                                modifier = Modifier.width(80.dp)
                                            )
                                            Slider(
                                                value = liveOpacity,
                                                onValueChange = {
                                                    FloatingOverlayManager.updateOpacity(it)
                                                    keyboardPrefs.bubbleOpacity = it
                                                },
                                                valueRange = 0.20f..1.0f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = BoardsBlue,
                                                    activeTrackColor = BoardsBlue
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // Notificación Visual Flotante cuando se copia un mensaje
                                AnimatedVisibility(
                                    visible = copiedNotificationText != null,
                                    enter = fadeIn() + slideInVertically(),
                                    exit = fadeOut() + slideOutVertically()
                                ) {
                                    Surface(
                                        color = Color(0xFF10B981),
                                        shape = RoundedCornerShape(0.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = copiedNotificationText ?: "",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // CUERPO PRINCIPAL DEL TECLADO CON PARIDAD TOTAL
                                Box(modifier = Modifier.weight(1f)) {
                                    KeyboardView(
                                        repository = repository,
                                        scope = serviceScope,
                                        onCommitReply = { reply, processedText ->
                                            performHaptic()
                                            val textToCopy = processedText ?: reply.content
                                            copyToClipboard(reply.title, textToCopy)
                                            copiedNotificationText = "✓ \"${reply.title}\" copiado para pegar"
                                            serviceScope.launch(Dispatchers.IO) {
                                                repository.incrementUsage(reply.id)
                                            }
                                        },
                                        onCommitMedia = { reply, uriString, _ ->
                                            performHaptic()
                                            copyToClipboard(reply.title, uriString)
                                            copiedNotificationText = "✓ Adjunto \"${reply.title}\" listo para enviar"
                                            serviceScope.launch(Dispatchers.IO) {
                                                repository.incrementUsage(reply.id)
                                            }
                                        },
                                        onDeleteCharacter = {
                                            performHaptic()
                                        },
                                        onSwitchKeyboard = {
                                            performHaptic()
                                            isExpandedState = false
                                        },
                                        onHideKeyboard = {
                                            isExpandedState = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        overlayView = composeView
        windowManager.addView(overlayView, params)
    }

    private fun performHaptic() {
        if (!keyboardPrefs.hapticFeedbackEnabled) return
        val duration = keyboardPrefs.hapticIntensity.toLong().coerceIn(5L, 50L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(applicationContext, "✓ Copiado: $label (Listo para pegar)", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        FloatingOverlayManager.setServiceActive(false)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
    }
}

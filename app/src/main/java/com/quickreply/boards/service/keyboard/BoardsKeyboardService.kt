package com.quickreply.boards.service.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
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
import com.quickreply.boards.data.sync.SupabaseSyncManager
import com.quickreply.boards.service.keyboard.ui.KeyboardView
import com.quickreply.boards.ui.theme.QuickReplyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BoardsKeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: QuickReplyRepository
    private var vibrator: Vibrator? = null

    // Gestión del ciclo de vida requerida por Compose dentro del InputMethodService
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var keyboardPrefs: KeyboardPreferences
    private lateinit var syncManager: SupabaseSyncManager

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        keyboardPrefs = KeyboardPreferences(this)
        val database = AppDatabase.getDatabase(this, serviceScope)
        repository = QuickReplyRepository(database.folderDao(), database.quickReplyDao(), database.clipboardDao())
        syncManager = SupabaseSyncManager(this, database.folderDao(), database.quickReplyDao())

        // Garantiza que los tableros y respuestas existan
        serviceScope.launch(Dispatchers.IO) {
            repository.seedDefaultDataIfEmpty()
            try {
                syncManager.performFullSync()
            } catch (_: Exception) {}
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        initClipboardListener()
    }

    private fun initClipboardListener() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.addPrimaryClipChangedListener {
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text?.toString()
                    if (!text.isNullOrBlank()) {
                        serviceScope.launch(Dispatchers.IO) {
                            repository.insertClip(text)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun checkCurrentClipboard() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank()) {
                    serviceScope.launch(Dispatchers.IO) {
                        repository.insertClip(text)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        serviceScope.launch(Dispatchers.IO) {
            try {
                syncManager.performFullSync()
            } catch (_: Exception) {}
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        checkCurrentClipboard()
        serviceScope.launch(Dispatchers.IO) {
            try {
                syncManager.performFullSync()
            } catch (_: Exception) {}
        }
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycleRegistry))
            setContent {
                QuickReplyTheme {
                    KeyboardView(
                        repository = repository,
                        scope = serviceScope,
                        onCommitReply = { reply, processedText ->
                            performHapticFeedback()
                            commitTextToApp(processedText ?: reply.content)
                            serviceScope.launch(Dispatchers.IO) {
                                repository.incrementUsage(reply.id)
                            }
                        },
                        onCommitMedia = { reply, uriString, mimeType ->
                            performHapticFeedback()
                            commitMediaToApp(uriString, mimeType)
                            serviceScope.launch(Dispatchers.IO) {
                                repository.incrementUsage(reply.id)
                            }
                        },
                        onDeleteCharacter = {
                            performHapticFeedback()
                            deletePreviousCharacter()
                        },
                        onSwitchKeyboard = {
                            performHapticFeedback()
                            switchToSystemKeyboard()
                        },
                        onHideKeyboard = {
                            requestHideSelf(0)
                        }
                    )
                }
            }
        }

        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        return composeView
    }

    private fun performHapticFeedback() {
        if (!::keyboardPrefs.isInitialized || !keyboardPrefs.hapticFeedbackEnabled) return
        val duration = keyboardPrefs.hapticIntensity.toLong().coerceIn(5L, 50L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    private fun commitTextToApp(text: String) {
        val inputConnection: InputConnection = currentInputConnection ?: return
        inputConnection.commitText(text, 1)
    }

    private fun commitMediaToApp(uriString: String, mimeType: String) {
        val inputConnection: InputConnection = currentInputConnection ?: return
        val editorInfo: EditorInfo = currentInputEditorInfo ?: return

        try {
            val contentUri: android.net.Uri = if (uriString.startsWith("file://")) {
                val file = java.io.File(android.net.Uri.parse(uriString).path ?: "")
                androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            } else if (uriString.startsWith("/")) {
                val file = java.io.File(uriString)
                androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            } else {
                android.net.Uri.parse(uriString)
            }

            editorInfo.packageName?.let { targetPackage ->
                grantUriPermission(
                    targetPackage,
                    contentUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val inputContentInfo = InputContentInfoCompat(
                contentUri,
                android.content.ClipDescription("Respuesta Multimedia", arrayOf(mimeType)),
                null
            )

            var flags = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                flags = flags or InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            }

            val committed = InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                inputContentInfo,
                flags,
                Bundle.EMPTY
            )
            if (!committed) {
                commitTextToApp(contentUri.toString())
            }
        } catch (e: Exception) {
            commitTextToApp(uriString)
        }
    }

    private fun deletePreviousCharacter() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    private fun switchToSystemKeyboard() {
        var switched = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switched = switchToPreviousInputMethod()
        }
        if (!switched) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showInputMethodPicker()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
    }
}

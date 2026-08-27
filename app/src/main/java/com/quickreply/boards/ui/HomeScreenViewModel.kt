package com.quickreply.boards.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickreply.boards.data.local.AppDatabase
import com.quickreply.boards.data.local.entity.FolderEntity
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.repository.QuickReplyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.quickreply.boards.data.sync.SupabaseSyncManager
import com.quickreply.boards.data.sync.UserSession
import kotlinx.coroutines.withContext

class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {

    val repository: QuickReplyRepository
    private val syncManager: SupabaseSyncManager

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _userSession = MutableStateFlow<UserSession?>(null)
    val userSession: StateFlow<UserSession?> = _userSession.asStateFlow()

    private val _selectedBoardId = MutableStateFlow<Long?>(null)
    val selectedBoardId: StateFlow<Long?> = _selectedBoardId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = QuickReplyRepository(database.folderDao(), database.quickReplyDao(), database.clipboardDao())
        syncManager = SupabaseSyncManager(application, database.folderDao(), database.quickReplyDao())
        _userSession.value = syncManager.getSession()

        viewModelScope.launch(Dispatchers.IO) {
            repository.seedDefaultDataIfEmpty()
            var backoffDelay = 3000L
            while (true) {
                try {
                    val result = syncManager.performFullSync()
                    if (result.isSuccess) {
                        backoffDelay = 3000L
                    } else {
                        backoffDelay = (backoffDelay * 2).coerceAtMost(30000L)
                    }
                } catch (_: Exception) {
                    backoffDelay = (backoffDelay * 2).coerceAtMost(30000L)
                }
                kotlinx.coroutines.delay(backoffDelay)
            }
        }
    }

    fun login(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            val result = syncManager.signIn(email, pass)
            if (result.isSuccess) {
                _userSession.value = result.getOrNull()
                syncManager.performFullSync()
            }
            _isSyncing.value = false
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    onResult(true, "¡Sesión iniciada con éxito!")
                }.onFailure { err ->
                    onResult(false, err.localizedMessage ?: "Error al autenticar")
                }
            }
        }
    }

    fun signUp(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            val result = syncManager.signUp(email, pass)
            if (result.isSuccess) {
                _userSession.value = result.getOrNull()
                syncManager.performFullSync()
            }
            _isSyncing.value = false
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    onResult(true, "¡Cuenta creada exitosamente!")
                }.onFailure { err ->
                    onResult(false, err.localizedMessage ?: "Error al registrar cuenta")
                }
            }
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = syncManager.resetPassword(email)
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    onResult(true, "¡Enlace de recuperación enviado! Revisa tu correo.")
                }.onFailure { err ->
                    onResult(false, err.localizedMessage ?: "Error al solicitar recuperación")
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch(Dispatchers.IO) {
            syncManager.signOut()
            repository.clearLocalData()
            _userSession.value = null
        }
    }

    fun syncWithCloud(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            val result = syncManager.performFullSync()
            _isSyncing.value = false
            withContext(Dispatchers.Main) {
                result.onSuccess { summary ->
                    onResult(true, "Sincronizado: ${summary.foldersSynced} tableros y ${summary.repliesSynced} respuestas actualizadas")
                }.onFailure { err ->
                    onResult(false, "Aviso de sincronización: ${err.localizedMessage ?: "Sin conexión"}")
                }
            }
        }
    }

    // Flujo estable de Tableros Principales (Siempre contiene los tableros raíz)
    val rootBoards: StateFlow<List<FolderEntity>> = repository.rootFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentReplies: StateFlow<List<QuickReplyEntity>> = _selectedBoardId
        .flatMapLatest { id ->
            if (id == null) MutableStateFlow(emptyList())
            else repository.getRepliesByFolder(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<QuickReplyEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) MutableStateFlow(emptyList())
            else repository.searchReplies(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedReplies: StateFlow<List<QuickReplyEntity>> = repository.deletedReplies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedBoard(boardId: Long?) {
        _selectedBoardId.value = boardId
    }

    fun getBoardByIdFlow(boardId: Long): Flow<FolderEntity?> {
        return repository.getFolderByIdFlow(boardId)
    }

    fun getSubfoldersForBoardFlow(boardId: Long): Flow<List<FolderEntity>> {
        return repository.getSubfolders(boardId)
    }

    fun getRepliesForBoardFlow(boardId: Long): Flow<List<QuickReplyEntity>> {
        return repository.getRepliesByFolderAndSubfolders(boardId)
    }

    fun getReplyByIdFlow(id: Long): Flow<QuickReplyEntity?> {
        return repository.getReplyByIdFlow(id)
    }

    suspend fun getReplyById(id: Long): QuickReplyEntity? {
        return repository.getReplyById(id)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveFolder(folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (folder.id == 0L) {
                repository.insertFolder(folder)
            } else {
                repository.updateFolder(folder)
            }
            try {
                syncManager.performFullSync()
            } catch (_: Exception) {}
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFolder(folder)
            syncManager.deleteFolderRemote(folder)
            try {
                syncManager.performFullSync()
            } catch (_: Exception) {}
        }
    }

    fun saveReply(reply: QuickReplyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (reply.id == 0L) {
                repository.insertReply(reply)
            } else {
                repository.updateReply(reply)
            }
            try {
                syncManager.performFullSync()
            } catch (_: Exception) {}
        }
    }

    fun deleteReply(reply: QuickReplyEntity, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.softDeleteReply(reply)
                syncManager.deleteReplyRemote(reply)
                syncManager.performFullSync()
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, "Mensaje eliminado")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Error al eliminar: ${e.message}")
                }
            }
        }
    }

    fun restoreReply(reply: QuickReplyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreReply(reply)
            try {
                syncManager.performFullSync()
            } catch (_: Exception) {}
        }
    }

    fun moveReply(reply: QuickReplyEntity, targetFolderId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveReply(reply.id, targetFolderId)
            try {
                syncManager.performFullSync()
            } catch (_: Exception) {}
        }
    }

    suspend fun getAllFoldersList(): List<FolderEntity> {
        return repository.getAllFoldersList()
    }

    fun restoreAllReplies() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreAllReplies()
            try {
                syncManager.performFullSync()
            } catch (_: Exception) {}
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.emptyTrash()
            try {
                syncManager.performFullSync()
            } catch (_: Exception) {}
        }
    }

    fun permanentDeleteReply(reply: QuickReplyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.permanentDeleteReply(reply)
            if (reply.remoteId != null) {
                syncManager.permanentDeleteReplyRemote(reply.remoteId)
            }
            try {
                syncManager.performFullSync()
            } catch (_: Exception) {}
        }
    }
}

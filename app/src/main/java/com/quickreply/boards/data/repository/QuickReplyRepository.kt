package com.quickreply.boards.data.repository

import com.quickreply.boards.data.local.dao.ClipboardDao
import com.quickreply.boards.data.local.dao.FolderDao
import com.quickreply.boards.data.local.dao.QuickReplyDao
import com.quickreply.boards.data.local.entity.ClipboardItemEntity
import com.quickreply.boards.data.local.entity.FolderEntity
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.map

class QuickReplyRepository(
    private val folderDao: FolderDao,
    private val quickReplyDao: QuickReplyDao,
    private val clipboardDao: ClipboardDao? = null
) {
    val rootFolders: Flow<List<FolderEntity>> = folderDao.getRootFolders()
    val favoriteReplies: Flow<List<QuickReplyEntity>> = quickReplyDao.getFavoriteReplies()
    val mostUsedReplies: Flow<List<QuickReplyEntity>> = quickReplyDao.getMostUsedReplies()
    val deletedReplies: Flow<List<QuickReplyEntity>> = quickReplyDao.getDeletedRepliesFlow()
    val recentClips: Flow<List<ClipboardItemEntity>> = clipboardDao?.getRecentClips() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getSubfolders(parentId: Long): Flow<List<FolderEntity>> = folderDao.getSubfolders(parentId)

    fun getFolderByIdFlow(id: Long): Flow<FolderEntity?> = folderDao.getFolderByIdFlow(id)

    suspend fun getFolderById(id: Long): FolderEntity? = folderDao.getFolderById(id)

    fun getRepliesByFolder(folderId: Long): Flow<List<QuickReplyEntity>> =
        quickReplyDao.getRepliesByFolder(folderId)

    fun getRepliesByFolderAndSubfolders(folderId: Long): Flow<List<QuickReplyEntity>> =
        quickReplyDao.getRepliesByFolderAndSubfolders(folderId)

    suspend fun getRepliesByFolderSync(folderId: Long): List<QuickReplyEntity> =
        quickReplyDao.getRepliesByFolderSync(folderId)

    fun searchReplies(query: String): Flow<List<QuickReplyEntity>> {
        val normalizedQuery = removeDiacritics(query.trim().lowercase())
        return quickReplyDao.getAllRepliesFlow().map { list: List<QuickReplyEntity> ->
            if (normalizedQuery.isBlank()) list else {
                list.filter { reply ->
                    removeDiacritics(reply.title.lowercase()).contains(normalizedQuery) ||
                    removeDiacritics(reply.content.lowercase()).contains(normalizedQuery) ||
                    (reply.shortcut != null && removeDiacritics(reply.shortcut.lowercase()).contains(normalizedQuery))
                }
            }
        }
    }

    private fun removeDiacritics(input: String): String {
        val nfdNormalizedString = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
        val pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfdNormalizedString).replaceAll("")
    }

    suspend fun getByShortcut(shortcut: String): QuickReplyEntity? =
        quickReplyDao.getByShortcut(shortcut)

    suspend fun incrementUsage(id: Long) =
        quickReplyDao.incrementUsage(id)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        quickReplyDao.toggleFavorite(id, isFavorite)

    suspend fun insertFolder(folder: FolderEntity): Long = folderDao.insertFolder(folder)

    suspend fun updateFolder(folder: FolderEntity) = folderDao.updateFolder(folder)

    suspend fun deleteFolder(folder: FolderEntity) = folderDao.deleteFolder(folder)

    fun getReplyByIdFlow(id: Long): Flow<QuickReplyEntity?> = quickReplyDao.getReplyByIdFlow(id)
    suspend fun getReplyById(id: Long): QuickReplyEntity? = quickReplyDao.getReplyById(id)

    suspend fun insertReply(reply: QuickReplyEntity): Long = quickReplyDao.insertReply(reply)

    suspend fun updateReply(reply: QuickReplyEntity) = quickReplyDao.updateReply(reply)

    suspend fun softDeleteReply(reply: QuickReplyEntity) {
        quickReplyDao.softDeleteReply(reply.id)
    }

    suspend fun restoreReply(reply: QuickReplyEntity) {
        quickReplyDao.restoreReply(reply.id)
    }

    suspend fun moveReply(id: Long, targetFolderId: Long) {
        quickReplyDao.moveReply(id, targetFolderId)
    }

    suspend fun getAllFoldersList(): List<FolderEntity> {
        return folderDao.getAllFoldersList()
    }

    suspend fun restoreAllReplies() {
        quickReplyDao.restoreAllReplies()
    }

    suspend fun emptyTrash() {
        quickReplyDao.emptyTrash()
    }

    suspend fun permanentDeleteReply(reply: QuickReplyEntity) {
        quickReplyDao.permanentDelete(reply.id)
    }

    suspend fun deleteReply(reply: QuickReplyEntity) = quickReplyDao.deleteReply(reply)

    // Métodos de Portapapeles Inteligente
    suspend fun insertClip(text: String): Long {
        if (text.isBlank()) return 0L
        return clipboardDao?.insertClip(ClipboardItemEntity(text = text.trim())) ?: 0L
    }

    suspend fun togglePinClip(id: Long, isPinned: Boolean) {
        clipboardDao?.togglePin(id, isPinned)
    }

    suspend fun clearUnpinnedClips() {
        clipboardDao?.clearUnpinned()
    }

    suspend fun deleteClip(clip: ClipboardItemEntity) {
        clipboardDao?.deleteClip(clip)
    }

    suspend fun clearLocalData() {
        quickReplyDao.deleteAllReplies()
        folderDao.deleteAllFolders()
    }

    suspend fun seedDefaultDataIfEmpty() {
        val allFolders = folderDao.getAllFoldersList()
        var fiBoard = allFolders.find { it.name.contains("Fi Corredores", ignoreCase = true) && it.parentId == null }
        
        if (fiBoard == null) {
            val rootId = folderDao.insertFolder(
                FolderEntity(
                    name = "🏢 Fi Corredores",
                    colorHex = "#4361EE",
                    sortOrder = 0,
                    isSynced = false
                )
            )
            fiBoard = folderDao.getFolderById(rootId)
        }

        // Asegurar Bice Seguros y Rocanegra Propiedades
        if (allFolders.none { it.name.contains("Bice Seguros", ignoreCase = true) && it.parentId == null }) {
            folderDao.insertFolder(
                FolderEntity(
                    name = "🏢 Bice Seguros",
                    colorHex = "#4361EE",
                    sortOrder = 1,
                    isSynced = false
                )
            )
        }

        if (allFolders.none { it.name.contains("Rocanegra", ignoreCase = true) && it.parentId == null }) {
            folderDao.insertFolder(
                FolderEntity(
                    name = "🏢 Rocanegra Propiedades",
                    colorHex = "#3A0CA3",
                    sortOrder = 2,
                    isSynced = false
                )
            )
        }

        // Purgar cualquier mensaje 'Prueba' residual
        quickReplyDao.getAllRepliesList().forEach { reply ->
            if (reply.title.trim().equals("prueba", ignoreCase = true) || reply.title.trim().equals("mensaje de prueba", ignoreCase = true)) {
                quickReplyDao.permanentDelete(reply.id)
            }
        }

        if (fiBoard != null) {
            val currentSubfolders = folderDao.getAllFoldersList().filter { it.parentId == fiBoard.id }
            
            var subFacebook = currentSubfolders.find { it.name.contains("Clientes facebook", ignoreCase = true) }
            if (subFacebook == null) {
                val subFbId = folderDao.insertFolder(
                    FolderEntity(
                        name = "Clientes facebook",
                        colorHex = "#3B82F6",
                        parentId = fiBoard.id,
                        sortOrder = 0,
                        isSynced = false
                    )
                )
                subFacebook = folderDao.getFolderById(subFbId)
            }

            var subAgendamiento = currentSubfolders.find { it.name.contains("Agendamiento", ignoreCase = true) }
            if (subAgendamiento == null) {
                val subAgId = folderDao.insertFolder(
                    FolderEntity(
                        name = "Agendamiento",
                        colorHex = "#10B981",
                        parentId = fiBoard.id,
                        sortOrder = 1,
                        isSynced = false
                    )
                )
                subAgendamiento = folderDao.getFolderById(subAgId)
            }

            // Normalizar y reubicar respuestas existentes en sus subcarpetas correspondientes
            val existingReplies = quickReplyDao.getAllRepliesList()
            existingReplies.forEach { reply ->
                var targetFolderId = reply.folderId
                if (reply.folderId == fiBoard.id) {
                    if (reply.title.contains("facebook", ignoreCase = true) && subFacebook != null) {
                        targetFolderId = subFacebook.id
                    } else if ((reply.title.contains("agendamiento", ignoreCase = true) || reply.title.contains("calendario", ignoreCase = true)) && subAgendamiento != null) {
                        targetFolderId = subAgendamiento.id
                    }
                }

                val cleanTitle = reply.title
                    .replace(" de facebook", "", ignoreCase = true)
                    .replace(" facebook", "", ignoreCase = true)
                    .trim()

                if (cleanTitle != reply.title || targetFolderId != reply.folderId) {
                    quickReplyDao.updateReply(reply.copy(title = cleanTitle, folderId = targetFolderId))
                }
            }

            // Si las subcarpetas no tienen respuestas, crearlas con sus datos completos
            if (subFacebook != null && quickReplyDao.getRepliesByFolderSync(subFacebook.id).isEmpty()) {
                quickReplyDao.insertReply(
                    QuickReplyEntity(
                        folderId = subFacebook.id,
                        title = "1° mensaje",
                        content = "Hola! Cómo estás? Hablas con Cristóbal de FI Corredores. Nosotros nos dedicamos a ayudar a las personas para que puedan comprar la propiedad que tanto han querido. Los llevamos de la mano en todo el camino, desde la búsqueda y aprobación del crédito hasta la compra de la propiedad",
                        contentType = ContentType.TEXT,
                        sortOrder = 0
                    )
                )
                quickReplyDao.insertReply(
                    QuickReplyEntity(
                        folderId = subFacebook.id,
                        title = "2° mensaje",
                        content = "Nosotros trabajamos con varios bancos y mutuarias, por lo que te podríamos ayudar a obtener el crédito.\n\nPara poder ayudarte con el crédito, tenemos que saber si hay factibilidad crediticia, y para eso, necesitamos conocer tu situación actual",
                        contentType = ContentType.TEXT,
                        sortOrder = 1
                    )
                )
                quickReplyDao.insertReply(
                    QuickReplyEntity(
                        folderId = subFacebook.id,
                        title = "3° mensaje",
                        content = "Y para eso, necesitamos que nos puedas responder estas preguntas:\n\n• ¿Estado civil?\n• ¿Cuál es tú profesión?\n• ¿Es tú primera propiedad?",
                        contentType = ContentType.TEXT,
                        sortOrder = 2
                    )
                )
                quickReplyDao.insertReply(
                    QuickReplyEntity(
                        folderId = subFacebook.id,
                        title = "4° mensaje",
                        content = "Con eso, podríamos avanzar y ayudarlos a conseguir una aprobación",
                        contentType = ContentType.TEXT,
                        sortOrder = 3
                    )
                )
            }

            if (subAgendamiento != null && quickReplyDao.getRepliesByFolderSync(subAgendamiento.id).isEmpty()) {
                quickReplyDao.insertReply(
                    QuickReplyEntity(
                        folderId = subAgendamiento.id,
                        title = "1° agendamiento",
                        content = "Mira, el primer paso, es que nos podamos reunir en una llamada de 15 minutos para ver los detalles.",
                        contentType = ContentType.TEXT,
                        sortOrder = 0
                    )
                )
                quickReplyDao.insertReply(
                    QuickReplyEntity(
                        folderId = subAgendamiento.id,
                        title = "2° agendamiento (Link calendario)",
                        content = "En este link puedes ver qué horarios tenemos disponibles\n-",
                        mediaUri = "https://calendar.app.google/Q6RUWtL2dFhAfjvA8",
                        contentType = ContentType.LINK,
                        sortOrder = 1
                    )
                )
            }

            // Asegurar Gastos operacionales en el tablero principal Fi Corredores
            val allExisting = quickReplyDao.getAllRepliesList()
            if (allExisting.none { it.title.contains("Gastos operacionales", ignoreCase = true) }) {
                quickReplyDao.insertReply(
                    QuickReplyEntity(
                        folderId = fiBoard.id,
                        title = "Gastos operacionales",
                        content = "Los gastos operacionales contemplan la tasación bancaria, estudio de títulos, redacción de escritura, gastos notariales, impuestos e inscripción en el conservador de bienes raíces.\n\nEl banco que te apruebe el crédito, te va a pedir que crees una cuenta, y que ahí deposites cierto monto para esos gastos, y a medida que se vayan utilizando te van a ir enviando los comprobantes.",
                        contentType = ContentType.TEXT,
                        sortOrder = 2
                    )
                )
            }
        }
    }
}

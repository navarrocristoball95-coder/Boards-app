package com.quickreply.boards.data.sync

import com.quickreply.boards.data.local.entity.FolderEntity
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Abstracción de Sincronización Nube (Supabase / Firebase Ready)
 * Permite alternar entre proveedores de nube manteniendo la arquitectura intacta.
 */
interface SyncProvider {
    suspend fun syncFolders(localFolders: List<FolderEntity>): Result<Unit>
    suspend fun syncReplies(localReplies: List<QuickReplyEntity>): Result<Unit>
    fun observeRemoteChanges(): Flow<Boolean>
}

class DefaultSyncManager : SyncProvider {
    override suspend fun syncFolders(localFolders: List<FolderEntity>): Result<Unit> {
        // Listo para conectar cliente Supabase (Ktor / Postgrest) o Firebase Firestore
        return Result.success(Unit)
    }

    override suspend fun syncReplies(localReplies: List<QuickReplyEntity>): Result<Unit> {
        return Result.success(Unit)
    }

    override fun observeRemoteChanges(): Flow<Boolean> {
        return flowOf(false)
    }
}

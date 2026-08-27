package com.quickreply.boards.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickReplyDao {

    @Query("SELECT * FROM quick_replies WHERE folderId = :folderId AND isDeleted = 0 ORDER BY isFavorite DESC, sortOrder ASC, title ASC, createdAt ASC")
    fun getRepliesByFolder(folderId: Long): Flow<List<QuickReplyEntity>>

    @Query("SELECT * FROM quick_replies WHERE (folderId = :folderId OR folderId IN (SELECT id FROM folders WHERE parentId = :folderId)) AND isDeleted = 0 ORDER BY isFavorite DESC, sortOrder ASC, title ASC, createdAt ASC")
    fun getRepliesByFolderAndSubfolders(folderId: Long): Flow<List<QuickReplyEntity>>

    @Query("SELECT * FROM quick_replies WHERE folderId = :folderId AND isDeleted = 0 ORDER BY isFavorite DESC, sortOrder ASC, title ASC, createdAt ASC")
    suspend fun getRepliesByFolderSync(folderId: Long): List<QuickReplyEntity>

    @Query("SELECT * FROM quick_replies WHERE isFavorite = 1 AND isDeleted = 0 ORDER BY usageCount DESC, title ASC")
    fun getFavoriteReplies(): Flow<List<QuickReplyEntity>>

    @Query("SELECT * FROM quick_replies WHERE isDeleted = 0 ORDER BY isFavorite DESC, usageCount DESC, lastUsedAt DESC LIMIT 20")
    fun getMostUsedReplies(): Flow<List<QuickReplyEntity>>

    @Query("SELECT * FROM quick_replies WHERE isDeleted = 0 ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllRepliesFlow(): Flow<List<QuickReplyEntity>>

    @Query("SELECT * FROM quick_replies WHERE isDeleted = 0 ORDER BY isFavorite DESC, createdAt DESC")
    suspend fun getAllRepliesList(): List<QuickReplyEntity>

    @Query("SELECT * FROM quick_replies WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedRepliesFlow(): Flow<List<QuickReplyEntity>>

    @Query("SELECT * FROM quick_replies WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR shortcut LIKE '%' || :query || '%') AND isDeleted = 0 ORDER BY isFavorite DESC, usageCount DESC")
    fun searchReplies(query: String): Flow<List<QuickReplyEntity>>

    @Query("SELECT * FROM quick_replies WHERE shortcut = :shortcut AND isDeleted = 0 LIMIT 1")
    suspend fun getByShortcut(shortcut: String): QuickReplyEntity?

    @Query("SELECT * FROM quick_replies WHERE id = :id")
    suspend fun getReplyById(id: Long): QuickReplyEntity?

    @Query("SELECT * FROM quick_replies WHERE id = :id")
    fun getReplyByIdFlow(id: Long): Flow<QuickReplyEntity?>

    @Query("SELECT * FROM quick_replies WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getReplyByRemoteId(remoteId: String): QuickReplyEntity?

    @Query("UPDATE quick_replies SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE quick_replies SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE quick_replies SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteReply(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE quick_replies SET folderId = :targetFolderId, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun moveReply(id: Long, targetFolderId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE quick_replies SET isDeleted = 0, isSynced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restoreReply(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE quick_replies SET isDeleted = 0, isSynced = 0, updatedAt = :timestamp WHERE isDeleted = 1")
    suspend fun restoreAllReplies(timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM quick_replies WHERE isSynced = 0")
    suspend fun getUnsyncedReplies(): List<QuickReplyEntity>

    @Query("SELECT * FROM quick_replies")
    suspend fun getAllRepliesIncludingDeletedList(): List<QuickReplyEntity>

    @Query("DELETE FROM quick_replies WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM quick_replies WHERE id = :id")
    suspend fun permanentDelete(id: Long)

    @Query("DELETE FROM quick_replies")
    suspend fun deleteAllReplies()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReply(reply: QuickReplyEntity): Long

    @Update
    suspend fun updateReply(reply: QuickReplyEntity)

    @Delete
    suspend fun deleteReply(reply: QuickReplyEntity)
}

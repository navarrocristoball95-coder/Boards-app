package com.quickreply.boards.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quickreply.boards.data.local.entity.ClipboardItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {

    @Query("SELECT * FROM clipboard_history ORDER BY isPinned DESC, copiedAt DESC LIMIT 30")
    fun getRecentClips(): Flow<List<ClipboardItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipboardItemEntity): Long

    @Query("UPDATE clipboard_history SET isPinned = :isPinned WHERE id = :id")
    suspend fun togglePin(id: Long, isPinned: Boolean)

    @Query("DELETE FROM clipboard_history WHERE isPinned = 0")
    suspend fun clearUnpinned()

    @Delete
    suspend fun deleteClip(clip: ClipboardItemEntity)
}

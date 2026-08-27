package com.quickreply.boards.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quickreply.boards.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun getRootFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY sortOrder ASC, name ASC")
    fun getSubfolders(parentId: Long): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): FolderEntity?

    @Query("SELECT * FROM folders WHERE id = :id")
    fun getFolderByIdFlow(id: Long): Flow<FolderEntity?>

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    suspend fun getFolderByName(name: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getFolderByRemoteId(remoteId: String): FolderEntity?

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC")
    suspend fun getAllFoldersList(): List<FolderEntity>

    @Query("SELECT COUNT(*) FROM folders")
    suspend fun getFolderCount(): Int

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)
}

package com.quickreply.boards.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "folder", // Icon key e.g. "folder", "shopping", "support", "link"
    val colorHex: String = "#4285F4",
    val parentId: Long? = null, // Null indicates root folder
    val sortOrder: Int = 0,
    val isSynced: Boolean = false,
    val remoteId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

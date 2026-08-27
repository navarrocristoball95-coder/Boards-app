package com.quickreply.boards.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quickreply.boards.data.model.ContentType

@Entity(
    tableName = "quick_replies",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("folderId"),
        Index("shortcut"),
        Index("isFavorite"),
        Index("usageCount")
    ]
)
data class QuickReplyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long,
    val title: String,
    val content: String, // Soporta variables como {nombre} o {monto:1000} con valor por defecto
    val contentType: ContentType = ContentType.TEXT,
    val shortcut: String? = null, // e.g. "/precio"
    val mediaUri: String? = null,
    val isFavorite: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAt: Long = 0,
    val isSynced: Boolean = false,
    val remoteId: String? = null,
    val isDeleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

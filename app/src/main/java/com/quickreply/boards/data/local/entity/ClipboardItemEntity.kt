package com.quickreply.boards.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clipboard_history")
data class ClipboardItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isPinned: Boolean = false,
    val copiedAt: Long = System.currentTimeMillis()
)

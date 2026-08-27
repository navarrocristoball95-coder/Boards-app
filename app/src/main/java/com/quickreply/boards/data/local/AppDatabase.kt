package com.quickreply.boards.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quickreply.boards.data.local.dao.ClipboardDao
import com.quickreply.boards.data.local.dao.FolderDao
import com.quickreply.boards.data.local.dao.QuickReplyDao
import com.quickreply.boards.data.local.entity.ClipboardItemEntity
import com.quickreply.boards.data.local.entity.FolderEntity
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [FolderEntity::class, QuickReplyEntity::class, ClipboardItemEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao
    abstract fun quickReplyDao(): QuickReplyDao
    abstract fun clipboardDao(): ClipboardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quick_reply_boards_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

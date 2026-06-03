package com.hire.smartcompress.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hire.smartcompress.data.database.dao.CompressionHistoryDao
import com.hire.smartcompress.data.database.entities.CompressionHistoryEntity

@Database(
    entities = [CompressionHistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun compressionHistoryDao(): CompressionHistoryDao

    companion object {
        const val DATABASE_NAME = "smart_compress.db"
    }
}

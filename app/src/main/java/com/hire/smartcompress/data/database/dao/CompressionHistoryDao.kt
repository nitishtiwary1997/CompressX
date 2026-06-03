package com.hire.smartcompress.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hire.smartcompress.data.database.entities.CompressionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompressionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CompressionHistoryEntity): Long

    @Query("SELECT * FROM compression_history ORDER BY compressionDate DESC")
    fun getAll(): Flow<List<CompressionHistoryEntity>>

    @Query("SELECT * FROM compression_history WHERE fileType = :fileType ORDER BY compressionDate DESC")
    fun getByType(fileType: String): Flow<List<CompressionHistoryEntity>>

    @Query("SELECT * FROM compression_history WHERE fileName LIKE '%' || :query || '%' ORDER BY compressionDate DESC")
    fun search(query: String): Flow<List<CompressionHistoryEntity>>

    @Query("DELETE FROM compression_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM compression_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM compression_history")
    suspend fun getTotalCount(): Int

    @Query("SELECT COALESCE(SUM(savedSize), 0) FROM compression_history")
    suspend fun getTotalSaved(): Long

    @Query("SELECT COUNT(*) FROM compression_history WHERE fileType = :fileType")
    suspend fun getCountByType(fileType: String): Int

    @Query("SELECT * FROM compression_history ORDER BY compressionDate DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<CompressionHistoryEntity>
}

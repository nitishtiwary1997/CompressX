package com.hire.smartcompress.domain.repository

import com.hire.smartcompress.domain.model.CompressionHistory
import com.hire.smartcompress.domain.model.DashboardStats
import com.hire.smartcompress.domain.model.FileType
import kotlinx.coroutines.flow.Flow

interface ICompressionRepository {
    fun getAllHistory(): Flow<List<CompressionHistory>>
    fun getHistoryByType(fileType: FileType): Flow<List<CompressionHistory>>
    suspend fun saveHistory(history: CompressionHistory)
    suspend fun deleteHistory(id: Long)
    suspend fun deleteAllHistory()
    suspend fun getDashboardStats(): DashboardStats
    fun searchHistory(query: String): Flow<List<CompressionHistory>>
}

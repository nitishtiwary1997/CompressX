package com.hire.smartcompress.data.repository

import com.hire.smartcompress.data.database.dao.CompressionHistoryDao
import com.hire.smartcompress.data.database.entities.CompressionHistoryEntity
import com.hire.smartcompress.domain.model.CompressionHistory
import com.hire.smartcompress.domain.model.DashboardStats
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.repository.ICompressionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CompressionRepositoryImpl @Inject constructor(
    private val dao: CompressionHistoryDao
) : ICompressionRepository {

    override fun getAllHistory(): Flow<List<CompressionHistory>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getHistoryByType(fileType: FileType): Flow<List<CompressionHistory>> =
        dao.getByType(fileType.name).map { list -> list.map { it.toDomain() } }

    override suspend fun saveHistory(history: CompressionHistory) {
        dao.insert(CompressionHistoryEntity.fromDomain(history))
    }

    override suspend fun deleteHistory(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun deleteAllHistory() {
        dao.deleteAll()
    }

    override suspend fun getDashboardStats(): DashboardStats {
        val total = dao.getTotalCount()
        val saved = dao.getTotalSaved()
        val imageCount = dao.getCountByType(FileType.IMAGE.name)
        val videoCount = dao.getCountByType(FileType.VIDEO.name)
        val pdfCount = dao.getCountByType(FileType.PDF.name)
        val recent = dao.getRecent(5).map { it.toDomain() }
        return DashboardStats(
            totalFilesCompressed = total,
            totalStorageSaved = saved,
            imageCount = imageCount,
            videoCount = videoCount,
            pdfCount = pdfCount,
            recentHistory = recent
        )
    }

    override fun searchHistory(query: String): Flow<List<CompressionHistory>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }
}

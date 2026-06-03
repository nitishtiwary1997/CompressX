package com.hire.smartcompress.domain.usecase

import com.hire.smartcompress.domain.model.CompressionHistory
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.repository.ICompressionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCompressionHistoryUseCase @Inject constructor(
    private val repository: ICompressionRepository
) {
    fun getAll(): Flow<List<CompressionHistory>> = repository.getAllHistory()
    fun getByType(type: FileType): Flow<List<CompressionHistory>> = repository.getHistoryByType(type)
    fun search(query: String): Flow<List<CompressionHistory>> = repository.searchHistory(query)
}

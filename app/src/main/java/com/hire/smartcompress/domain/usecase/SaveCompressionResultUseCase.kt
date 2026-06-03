package com.hire.smartcompress.domain.usecase

import com.hire.smartcompress.domain.model.CompressionHistory
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.repository.ICompressionRepository
import com.hire.smartcompress.utils.FileUtils
import javax.inject.Inject

class SaveCompressionResultUseCase @Inject constructor(
    private val repository: ICompressionRepository,
    private val fileUtils: FileUtils
) {
    suspend operator fun invoke(result: CompressionResult.Success) {
        val history = CompressionHistory(
            fileName = fileUtils.getFileName(result.compressedUri) ?: "unknown",
            fileType = result.fileType,
            originalSize = result.originalSize,
            compressedSize = result.compressedSize,
            savedSize = result.savedBytes,
            compressionDate = System.currentTimeMillis(),
            originalPath = result.originalUri.toString(),
            compressedPath = result.compressedUri.toString(),
            processingTimeMs = result.processingTimeMs
        )
        repository.saveHistory(history)
    }
}

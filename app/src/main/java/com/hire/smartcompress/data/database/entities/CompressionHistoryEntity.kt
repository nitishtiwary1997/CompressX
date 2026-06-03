package com.hire.smartcompress.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hire.smartcompress.domain.model.CompressionHistory
import com.hire.smartcompress.domain.model.FileType

@Entity(
    tableName = "compression_history",
    indices = [Index("compressionDate"), Index("fileType")]
)
data class CompressionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileType: String,
    val originalSize: Long,
    val compressedSize: Long,
    val savedSize: Long,
    val compressionDate: Long,
    val originalPath: String,
    val compressedPath: String,
    val processingTimeMs: Long
) {
    fun toDomain(): CompressionHistory = CompressionHistory(
        id = id,
        fileName = fileName,
        fileType = runCatching { FileType.valueOf(fileType) }.getOrDefault(FileType.UNKNOWN),
        originalSize = originalSize,
        compressedSize = compressedSize,
        savedSize = savedSize,
        compressionDate = compressionDate,
        originalPath = originalPath,
        compressedPath = compressedPath,
        processingTimeMs = processingTimeMs
    )

    companion object {
        fun fromDomain(history: CompressionHistory): CompressionHistoryEntity =
            CompressionHistoryEntity(
                id = history.id,
                fileName = history.fileName,
                fileType = history.fileType.name,
                originalSize = history.originalSize,
                compressedSize = history.compressedSize,
                savedSize = history.savedSize,
                compressionDate = history.compressionDate,
                originalPath = history.originalPath,
                compressedPath = history.compressedPath,
                processingTimeMs = history.processingTimeMs
            )
    }
}

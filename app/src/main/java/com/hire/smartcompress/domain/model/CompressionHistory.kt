package com.hire.smartcompress.domain.model

data class CompressionHistory(
    val id: Long = 0,
    val fileName: String,
    val fileType: FileType,
    val originalSize: Long,
    val compressedSize: Long,
    val savedSize: Long,
    val compressionDate: Long,
    val originalPath: String,
    val compressedPath: String,
    val processingTimeMs: Long
) {
    val savedPercent: Float
        get() = if (originalSize > 0) (savedSize.toFloat() / originalSize) * 100f else 0f
}

data class DashboardStats(
    val totalFilesCompressed: Int = 0,
    val totalStorageSaved: Long = 0,
    val imageCount: Int = 0,
    val videoCount: Int = 0,
    val pdfCount: Int = 0,
    val recentHistory: List<CompressionHistory> = emptyList()
)

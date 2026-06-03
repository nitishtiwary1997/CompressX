package com.hire.smartcompress.domain.model

import android.net.Uri

sealed class CompressionResult {
    data class Success(
        val originalUri: Uri,
        val compressedUri: Uri,
        val originalSize: Long,
        val compressedSize: Long,
        val processingTimeMs: Long,
        val fileType: FileType
    ) : CompressionResult() {
        val savedBytes: Long get() = originalSize - compressedSize
        val savedPercent: Float get() = if (originalSize > 0) (savedBytes.toFloat() / originalSize) * 100f else 0f
        val compressionRatio: Float get() = if (compressedSize > 0) originalSize.toFloat() / compressedSize else 1f
    }

    data class Error(
        val originalUri: Uri,
        val message: String,
        val cause: Throwable? = null
    ) : CompressionResult()

    data class Progress(
        val fileUri: Uri,
        val progressPercent: Int,
        val estimatedRemainingMs: Long = 0
    ) : CompressionResult()
}

data class BatchCompressionState(
    val items: List<BatchItem> = emptyList(),
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val totalProgress: Int = 0
) {
    val completedCount: Int get() = items.count { it.status == BatchItemStatus.COMPLETED }
    val failedCount: Int get() = items.count { it.status == BatchItemStatus.FAILED }
    val pendingCount: Int get() = items.count { it.status == BatchItemStatus.PENDING }
}

data class BatchItem(
    val fileItem: FileItem,
    val status: BatchItemStatus = BatchItemStatus.PENDING,
    val progress: Int = 0,
    val result: CompressionResult? = null
)

enum class BatchItemStatus { PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED }

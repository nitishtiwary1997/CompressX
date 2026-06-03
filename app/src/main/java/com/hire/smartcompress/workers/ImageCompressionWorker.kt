package com.hire.smartcompress.workers

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.model.ImageCompressionConfig
import com.hire.smartcompress.domain.usecase.CompressImageUseCase
import com.hire.smartcompress.domain.usecase.SaveCompressionResultUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

@HiltWorker
class ImageCompressionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val compressImageUseCase: CompressImageUseCase,
    private val saveCompressionResultUseCase: SaveCompressionResultUseCase
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URI = "uri"
        const val KEY_QUALITY = "quality"
        const val KEY_MAX_WIDTH = "max_width"
        const val KEY_MAX_HEIGHT = "max_height"
        const val KEY_KEEP_ASPECT = "keep_aspect"
        const val KEY_OUTPUT_URI = "output_uri"
        const val KEY_ORIGINAL_SIZE = "original_size"
        const val KEY_COMPRESSED_SIZE = "compressed_size"
        const val KEY_SAVED_BYTES = "saved_bytes"
    }

    override suspend fun doWork(): Result {
        val uriStr = inputData.getString(KEY_URI) ?: return Result.failure()
        val uri = Uri.parse(uriStr)
        val quality = inputData.getInt(KEY_QUALITY, 80)
        val maxWidth = inputData.getInt(KEY_MAX_WIDTH, -1).takeIf { it > 0 }
        val maxHeight = inputData.getInt(KEY_MAX_HEIGHT, -1).takeIf { it > 0 }
        val keepAspect = inputData.getBoolean(KEY_KEEP_ASPECT, true)

        val fileItem = FileItem(
            uri = uri,
            name = uri.lastPathSegment ?: "image",
            extension = "jpg",
            size = 0L,
            lastModified = 0L,
            mimeType = "image/jpeg",
            fileType = FileType.IMAGE
        )

        val config = ImageCompressionConfig(quality, maxWidth, maxHeight, keepAspect)

        return try {
            val result = compressImageUseCase(fileItem, config)
                .filterIsInstance<CompressionResult.Success>()
                .first()

            saveCompressionResultUseCase(result)

            Result.success(
                Data.Builder()
                    .putString(KEY_OUTPUT_URI, result.compressedUri.toString())
                    .putLong(KEY_ORIGINAL_SIZE, result.originalSize)
                    .putLong(KEY_COMPRESSED_SIZE, result.compressedSize)
                    .putLong(KEY_SAVED_BYTES, result.savedBytes)
                    .build()
            )
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

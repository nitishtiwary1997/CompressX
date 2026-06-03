package com.hire.smartcompress.workers

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.model.VideoCompressionConfig
import com.hire.smartcompress.domain.model.VideoQuality
import com.hire.smartcompress.domain.model.VideoResolution
import com.hire.smartcompress.domain.usecase.CompressVideoUseCase
import com.hire.smartcompress.domain.usecase.SaveCompressionResultUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

@HiltWorker
class VideoCompressionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val compressVideoUseCase: CompressVideoUseCase,
    private val saveCompressionResultUseCase: SaveCompressionResultUseCase
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URI = "uri"
        const val KEY_RESOLUTION = "resolution"
        const val KEY_QUALITY = "quality"
    }

    override suspend fun doWork(): Result {
        val uriStr = inputData.getString(KEY_URI) ?: return Result.failure()
        val uri = Uri.parse(uriStr)
        val resolutionName = inputData.getString(KEY_RESOLUTION) ?: VideoResolution.P720.name
        val qualityName = inputData.getString(KEY_QUALITY) ?: VideoQuality.MEDIUM.name

        val resolution = runCatching { VideoResolution.valueOf(resolutionName) }
            .getOrDefault(VideoResolution.P720)
        val quality = runCatching { VideoQuality.valueOf(qualityName) }
            .getOrDefault(VideoQuality.MEDIUM)

        val fileItem = FileItem(
            uri = uri,
            name = uri.lastPathSegment ?: "video",
            extension = "mp4",
            size = 0L,
            lastModified = 0L,
            mimeType = "video/mp4",
            fileType = FileType.VIDEO
        )

        return try {
            val result = compressVideoUseCase(fileItem, VideoCompressionConfig(resolution, quality))
                .filterIsInstance<CompressionResult.Success>()
                .first()
            saveCompressionResultUseCase(result)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

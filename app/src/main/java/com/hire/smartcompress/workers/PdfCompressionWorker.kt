package com.hire.smartcompress.workers

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.model.PdfCompressionConfig
import com.hire.smartcompress.domain.model.PdfCompressionLevel
import com.hire.smartcompress.domain.usecase.CompressPdfUseCase
import com.hire.smartcompress.domain.usecase.SaveCompressionResultUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

@HiltWorker
class PdfCompressionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val compressPdfUseCase: CompressPdfUseCase,
    private val saveCompressionResultUseCase: SaveCompressionResultUseCase
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URI = "uri"
        const val KEY_LEVEL = "level"
    }

    override suspend fun doWork(): Result {
        val uriStr = inputData.getString(KEY_URI) ?: return Result.failure()
        val uri = Uri.parse(uriStr)
        val levelName = inputData.getString(KEY_LEVEL) ?: PdfCompressionLevel.MEDIUM.name
        val level = runCatching { PdfCompressionLevel.valueOf(levelName) }
            .getOrDefault(PdfCompressionLevel.MEDIUM)

        val fileItem = FileItem(
            uri = uri,
            name = uri.lastPathSegment ?: "document.pdf",
            extension = "pdf",
            size = 0L,
            lastModified = 0L,
            mimeType = "application/pdf",
            fileType = FileType.PDF
        )

        return try {
            val result = compressPdfUseCase(fileItem, PdfCompressionConfig(level))
                .filterIsInstance<CompressionResult.Success>()
                .first()
            saveCompressionResultUseCase(result)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

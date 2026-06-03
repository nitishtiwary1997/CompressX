package com.hire.smartcompress.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.model.PdfCompressionConfig
import com.hire.smartcompress.domain.model.PdfCompressionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfCompressor @Inject constructor(
    private val fileUtils: FileUtils
) {
    fun compress(
        context: Context,
        fileItem: FileItem,
        config: PdfCompressionConfig
    ): Flow<CompressionResult> = flow {
        val startTime = System.currentTimeMillis()
        val originalSize = fileUtils.getFileSizeFromUri(context, fileItem.uri)
        emit(CompressionResult.Progress(fileItem.uri, 5))

        val tempOutput = File.createTempFile("sc_pdf_", ".pdf", context.cacheDir)

        try {
            val fd = context.contentResolver.openFileDescriptor(fileItem.uri, "r")
                ?: throw IllegalStateException("Cannot open PDF file")

            val renderer = PdfRenderer(fd)
            val pageCount = renderer.pageCount

            val pdfDocument = PdfDocument()
            val quality = config.level.imageQuality
            val scaleFactor = when (config.level) {
                PdfCompressionLevel.LOW    -> 0.5f
                PdfCompressionLevel.MEDIUM -> 0.7f
                PdfCompressionLevel.HIGH   -> 0.9f
            }

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = (page.width * scaleFactor).toInt().coerceAtLeast(100)
                val height = (page.height * scaleFactor).toInt().coerceAtLeast(100)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val docPage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                )
                docPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(docPage)
                bitmap.recycle()

                val progress = (5 + ((i + 1).toFloat() / pageCount * 85)).toInt()
                emit(CompressionResult.Progress(fileItem.uri, progress))
            }

            renderer.close()
            fd.close()

            tempOutput.outputStream().use { pdfDocument.writeTo(it) }
            pdfDocument.close()

            emit(CompressionResult.Progress(fileItem.uri, 95))

            val baseName = (fileUtils.getFileNameFromUri(context, fileItem.uri) ?: "document")
                .substringBeforeLast('.')
            val outputName = "${baseName}_compressed.pdf"
            val savedUri = fileUtils.saveToMediaStore(context, tempOutput, "application/pdf", outputName)
                ?: throw IllegalStateException("Failed to save compressed PDF")

            tempOutput.delete()
            val compressedSize = fileUtils.getFileSizeFromUri(context, savedUri)
            val elapsed = System.currentTimeMillis() - startTime

            emit(CompressionResult.Progress(fileItem.uri, 100))
            emit(CompressionResult.Success(
                originalUri = fileItem.uri,
                compressedUri = savedUri,
                originalSize = originalSize,
                compressedSize = compressedSize,
                processingTimeMs = elapsed,
                fileType = FileType.PDF
            ))

        } catch (e: Exception) {
            tempOutput.delete()
            emit(CompressionResult.Error(fileItem.uri, e.message ?: "PDF compression failed", e))
        }
    }.flowOn(Dispatchers.IO)
}

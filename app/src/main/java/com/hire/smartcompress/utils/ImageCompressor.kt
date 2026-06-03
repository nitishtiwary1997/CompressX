package com.hire.smartcompress.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.model.ImageCompressionConfig
import com.hire.smartcompress.domain.model.ImageOutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageCompressor @Inject constructor(
    private val fileUtils: FileUtils
) {
    fun compress(
        context: Context,
        fileItem: FileItem,
        config: ImageCompressionConfig
    ): Flow<CompressionResult> = flow {
        emit(CompressionResult.Progress(fileItem.uri, 10))
        val startTime = System.currentTimeMillis()
        try {
            val originalSize = fileUtils.getFileSizeFromUri(context, fileItem.uri)
            emit(CompressionResult.Progress(fileItem.uri, 20))

            val bitmap = loadBitmap(context, fileItem.uri)
                ?: throw IllegalStateException("Failed to decode bitmap")
            emit(CompressionResult.Progress(fileItem.uri, 40))

            val resized = resizeBitmap(bitmap, config)
            emit(CompressionResult.Progress(fileItem.uri, 60))

            val compressFormat = when (config.outputFormat) {
                ImageOutputFormat.PNG -> Bitmap.CompressFormat.PNG
                ImageOutputFormat.WEBP -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                    Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }
            val ext = when (config.outputFormat) {
                ImageOutputFormat.PNG -> ".png"
                ImageOutputFormat.WEBP -> ".webp"
                else -> ".jpg"
            }
            val mimeType = when (config.outputFormat) {
                ImageOutputFormat.PNG -> "image/png"
                ImageOutputFormat.WEBP -> "image/webp"
                else -> "image/jpeg"
            }

            val tempFile = File.createTempFile("sc_img_", ext, context.cacheDir)
            tempFile.outputStream().use { out ->
                resized.compress(compressFormat, config.quality, out)
            }
            emit(CompressionResult.Progress(fileItem.uri, 80))

            val baseName = (fileUtils.getFileNameFromUri(context, fileItem.uri) ?: "image")
                .substringBeforeLast('.')
            val outputName = "${baseName}_compressed$ext"
            val savedUri = fileUtils.saveToMediaStore(context, tempFile, mimeType, outputName)
                ?: throw IllegalStateException("Failed to save compressed image")

            tempFile.delete()
            bitmap.recycle()
            resized.recycle()

            val compressedSize = fileUtils.getFileSizeFromUri(context, savedUri)
            val elapsed = System.currentTimeMillis() - startTime
            emit(CompressionResult.Progress(fileItem.uri, 100))
            emit(CompressionResult.Success(
                originalUri = fileItem.uri,
                compressedUri = savedUri,
                originalSize = originalSize,
                compressedSize = compressedSize,
                processingTimeMs = elapsed,
                fileType = FileType.IMAGE
            ))
        } catch (e: Exception) {
            emit(CompressionResult.Error(fileItem.uri, e.message ?: "Image compression failed", e))
        }
    }.flowOn(Dispatchers.IO)

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            val sampleSize = calculateInSampleSize(opts, 4096, 4096)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null
            applyExifRotation(context, uri, bitmap)
        } catch (e: Exception) { null }
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val exif = context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it)
            } ?: return bitmap
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            rotated
        } catch (e: Exception) { bitmap }
    }

    private fun resizeBitmap(bitmap: Bitmap, config: ImageCompressionConfig): Bitmap {
        val targetW = config.maxWidth
        val targetH = config.maxHeight
        if (targetW == null && targetH == null) return bitmap

        val origW = bitmap.width.toFloat()
        val origH = bitmap.height.toFloat()

        val newW: Int
        val newH: Int

        if (config.keepAspectRatio) {
            // Fit within the target box while preserving aspect ratio
            val scaleW = targetW?.let { it / origW } ?: Float.MAX_VALUE
            val scaleH = targetH?.let { it / origH } ?: Float.MAX_VALUE
            val scale = minOf(scaleW, scaleH, 1f)
            if (scale >= 1f) return bitmap
            newW = (origW * scale).toInt().coerceAtLeast(1)
            newH = (origH * scale).toInt().coerceAtLeast(1)
        } else {
            // Apply each dimension independently; only shrink, never enlarge
            newW = targetW?.coerceAtMost(bitmap.width) ?: bitmap.width
            newH = targetH?.coerceAtMost(bitmap.height) ?: bitmap.height
            if (newW == bitmap.width && newH == bitmap.height) return bitmap
        }

        val resized = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        bitmap.recycle()
        return resized
    }

    private fun calculateInSampleSize(opts: BitmapFactory.Options, maxW: Int, maxH: Int): Int {
        var sample = 1
        val (h, w) = opts.outHeight to opts.outWidth
        if (h > maxH || w > maxW) {
            while ((h / sample) > maxH || (w / sample) > maxW) sample *= 2
        }
        return sample
    }
}

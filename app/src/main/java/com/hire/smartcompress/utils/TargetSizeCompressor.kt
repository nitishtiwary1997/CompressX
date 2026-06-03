package com.hire.smartcompress.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.hire.smartcompress.domain.model.SmartCompressResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TargetSizeCompressor @Inject constructor(
    private val fileUtils: FileUtils
) {
    // Scale steps tried from largest to smallest before giving up
    private val SCALE_STEPS = listOf(1.0f, 0.85f, 0.70f, 0.55f, 0.40f, 0.30f, 0.20f, 0.12f, 0.07f)

    fun compress(
        context: Context,
        inputUri: Uri,
        targetBytes: Long
    ): Flow<SmartCompressResult> = flow {
        val startMs = System.currentTimeMillis()
        val inputName = fileUtils.getFileNameFromUri(context, inputUri) ?: "image"
        val originalBytes = fileUtils.getFileSizeFromUri(context, inputUri)
        emit(SmartCompressResult.Progress(5, "Loading image…"))

        try {
            // Short-circuit: already within target
            if (originalBytes > 0 && originalBytes <= targetBytes) {
                val outUri = copyToOutput(context, inputUri, inputName)
                emit(SmartCompressResult.Progress(100))
                emit(SmartCompressResult.ItemSuccess(
                    outputUri = outUri,
                    inputName = inputName,
                    originalBytes = originalBytes,
                    achievedBytes = originalBytes,
                    targetBytes = targetBytes,
                    qualityUsed = 100,
                    scalePercent = 100,
                    targetAchieved = true,
                    processingTimeMs = System.currentTimeMillis() - startMs
                ))
                return@flow
            }

            val original = loadBitmap(context, inputUri)
                ?: throw IllegalStateException("Cannot decode image")
            val origW = original.width
            val origH = original.height
            emit(SmartCompressResult.Progress(15, "Image loaded (${origW}×${origH})…"))

            var bestBitmap: Bitmap? = null
            var bestQuality = 1
            var bestScalePct = 5
            var targetAchieved = false

            for ((stepIdx, scale) in SCALE_STEPS.withIndex()) {
                val scaledW = (origW * scale).toInt().coerceAtLeast(1)
                val scaledH = (origH * scale).toInt().coerceAtLeast(1)
                val bmp = if (scale == 1.0f) original
                          else Bitmap.createScaledBitmap(original, scaledW, scaledH, true)

                val q = binarySearchQuality(bmp, targetBytes)
                val achieved = estimateJpegSize(bmp, q)

                val pct = 20 + (stepIdx.toFloat() / SCALE_STEPS.size * 65).toInt()
                emit(SmartCompressResult.Progress(
                    pct,
                    "Scale ${(scale * 100).toInt()}% · Quality $q% → ${formatBytes(achieved)}"
                ))

                if (achieved <= targetBytes) {
                    if (bmp !== original) bestBitmap?.let { if (it !== original) it.recycle() }
                    bestBitmap = bmp
                    bestQuality = q
                    bestScalePct = (scale * 100).toInt()
                    targetAchieved = true
                    break
                }

                // Keep the smallest so far for best-effort fallback
                if (stepIdx == SCALE_STEPS.lastIndex) {
                    bestBitmap = bmp
                    bestQuality = q
                    bestScalePct = (scale * 100).toInt()
                }
                // Recycle intermediates we don't keep
                if (bmp !== original && bmp !== bestBitmap) bmp.recycle()
            }

            emit(SmartCompressResult.Progress(88, "Saving compressed image…"))

            val useBitmap = bestBitmap ?: original
            val ext = when {
                inputName.endsWith(".png", true) && targetAchieved && bestQuality > 80 -> "png"
                else -> "jpg"
            }
            val outputName = "${inputName.substringBeforeLast('.')}_${formatBytes(targetBytes).replace(" ", "")}.${ext}"
            val mime = if (ext == "png") "image/png" else "image/jpeg"
            val fmt = if (ext == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

            val tmp = File.createTempFile("sc_target_", ".$ext", context.cacheDir)
            tmp.outputStream().use { useBitmap.compress(fmt, bestQuality, it) }

            if (useBitmap !== original) useBitmap.recycle()
            original.recycle()

            val outUri = fileUtils.saveToMediaStore(context, tmp, mime, outputName)
                ?: throw IllegalStateException("Failed to save compressed image")
            tmp.delete()

            val achievedBytes = fileUtils.getFileSizeFromUri(context, outUri)
            emit(SmartCompressResult.Progress(100))
            emit(SmartCompressResult.ItemSuccess(
                outputUri = outUri,
                inputName = inputName,
                originalBytes = originalBytes,
                achievedBytes = achievedBytes,
                targetBytes = targetBytes,
                qualityUsed = bestQuality,
                scalePercent = bestScalePct,
                targetAchieved = targetAchieved,
                processingTimeMs = System.currentTimeMillis() - startMs
            ))
        } catch (e: Exception) {
            emit(SmartCompressResult.Error(e.message ?: "Smart compression failed"))
        }
    }.flowOn(Dispatchers.IO)

    // Binary-searches JPEG quality [1..95] to find the HIGHEST quality whose output ≤ targetBytes
    private fun binarySearchQuality(bitmap: Bitmap, targetBytes: Long): Int {
        var lo = 1; var hi = 95; var best = 1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val size = estimateJpegSize(bitmap, mid)
            if (size <= targetBytes) { best = mid; lo = mid + 1 } else hi = mid - 1
        }
        return best
    }

    private fun estimateJpegSize(bitmap: Bitmap, quality: Int): Long {
        val boas = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, boas)
        return boas.size().toLong()
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        // Sample down large images to avoid OOM while preserving full detail for scale steps
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val maxDim = maxOf(opts.outWidth, opts.outHeight)
        val sample = when { maxDim > 12000 -> 8; maxDim > 6000 -> 4; maxDim > 3000 -> 2; else -> 1 }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
    }

    private fun copyToOutput(context: Context, uri: Uri, name: String): Uri {
        val ext = name.substringAfterLast('.', "jpg")
        val tmp = File.createTempFile("sc_target_", ".$ext", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { it.copyTo(tmp.outputStream()) }
        val mime = fileUtils.getMimeType(context, uri)
        return fileUtils.saveToMediaStore(context, tmp, mime, name).also { tmp.delete() }
            ?: uri
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576 -> "${"%.1f".format(bytes / 1_048_576f)} MB"
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}

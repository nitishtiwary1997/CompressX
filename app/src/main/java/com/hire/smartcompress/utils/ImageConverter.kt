package com.hire.smartcompress.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.caverock.androidsvg.SVG
import com.hire.smartcompress.domain.model.ConversionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageConverter @Inject constructor(
    private val fileUtils: FileUtils
) {

    enum class OutputFormat(val ext: String, val mime: String) {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp");

        val compressFormat: Bitmap.CompressFormat
            get() = when (this) {
                JPEG -> Bitmap.CompressFormat.JPEG
                PNG -> Bitmap.CompressFormat.PNG
                WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    Bitmap.CompressFormat.WEBP_LOSSY
                else
                    @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            }
    }

    fun convertFormat(
        context: Context,
        inputUri: Uri,
        output: OutputFormat,
        quality: Int = 90
    ): Flow<ConversionResult> = flow {
        val startMs = System.currentTimeMillis()
        emit(ConversionResult.Progress(10, "Reading image…"))
        try {
            val bitmap = context.contentResolver.openInputStream(inputUri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: throw IllegalStateException("Cannot decode image")
            emit(ConversionResult.Progress(50, "Converting…"))

            val inputName = fileUtils.getFileNameFromUri(context, inputUri) ?: "image"
            val outputName = "${inputName.substringBeforeLast('.')}.${output.ext}"
            val tmp = File.createTempFile("sc_conv_", ".${output.ext}", context.cacheDir)
            tmp.outputStream().use { bitmap.compress(output.compressFormat, quality, it) }
            bitmap.recycle()
            emit(ConversionResult.Progress(80, "Saving…"))

            val outUri = fileUtils.saveToMediaStore(context, tmp, output.mime, outputName)
                ?: throw IllegalStateException("Failed to save output file")
            tmp.delete()
            emit(ConversionResult.Progress(100))
            emit(ConversionResult.Success(
                outputUris = listOf(outUri),
                outputNames = listOf(outputName),
                inputSizeBytes = fileUtils.getFileSizeFromUri(context, inputUri),
                outputSizeBytes = fileUtils.getFileSizeFromUri(context, outUri),
                processingTimeMs = System.currentTimeMillis() - startMs
            ))
        } catch (e: Exception) {
            emit(ConversionResult.Error(e.message ?: "Image conversion failed"))
        }
    }.flowOn(Dispatchers.IO)

    fun svgToPng(context: Context, inputUri: Uri): Flow<ConversionResult> = flow {
        val startMs = System.currentTimeMillis()
        emit(ConversionResult.Progress(10, "Parsing SVG…"))
        try {
            val svg = context.contentResolver.openInputStream(inputUri)?.use {
                SVG.getFromInputStream(it)
            } ?: throw IllegalStateException("Cannot read SVG file")

            val w = svg.documentWidth.takeIf { it > 0f }?.toInt() ?: 1024
            val h = svg.documentHeight.takeIf { it > 0f }?.toInt() ?: 1024
            emit(ConversionResult.Progress(40, "Rendering SVG…"))

            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            svg.renderToCanvas(canvas)
            emit(ConversionResult.Progress(70, "Saving PNG…"))

            val inputName = fileUtils.getFileNameFromUri(context, inputUri) ?: "image"
            val outputName = "${inputName.substringBeforeLast('.')}.png"
            val tmp = File.createTempFile("sc_svg_", ".png", context.cacheDir)
            tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()

            val outUri = fileUtils.saveToMediaStore(context, tmp, "image/png", outputName)
                ?: throw IllegalStateException("Failed to save PNG")
            tmp.delete()
            emit(ConversionResult.Progress(100))
            emit(ConversionResult.Success(
                outputUris = listOf(outUri),
                outputNames = listOf(outputName),
                inputSizeBytes = fileUtils.getFileSizeFromUri(context, inputUri),
                outputSizeBytes = fileUtils.getFileSizeFromUri(context, outUri),
                processingTimeMs = System.currentTimeMillis() - startMs
            ))
        } catch (e: Exception) {
            emit(ConversionResult.Error(e.message ?: "SVG to PNG conversion failed"))
        }
    }.flowOn(Dispatchers.IO)

    fun imageToSvg(context: Context, inputUri: Uri): Flow<ConversionResult> = flow {
        val startMs = System.currentTimeMillis()
        emit(ConversionResult.Progress(10, "Reading image…"))
        try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(inputUri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            val w = opts.outWidth
            val h = opts.outHeight
            if (w <= 0 || h <= 0) throw IllegalStateException("Cannot read image dimensions")

            emit(ConversionResult.Progress(30, "Encoding image…"))
            val bytes = context.contentResolver.openInputStream(inputUri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Cannot read image data")
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val mime = fileUtils.getMimeType(context, inputUri)

            emit(ConversionResult.Progress(60, "Building SVG…"))
            val svgContent = buildString {
                appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
                appendLine("""<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"""")
                appendLine("""     width="$w" height="$h" viewBox="0 0 $w $h">""")
                appendLine("""  <image width="$w" height="$h" xlink:href="data:$mime;base64,$b64"/>""")
                append("</svg>")
            }

            val inputName = fileUtils.getFileNameFromUri(context, inputUri) ?: "image"
            val outputName = "${inputName.substringBeforeLast('.')}.svg"
            val tmp = File(context.cacheDir, "sc_$outputName")
            tmp.writeText(svgContent)
            emit(ConversionResult.Progress(85, "Saving SVG…"))

            val outUri = fileUtils.saveToMediaStore(context, tmp, "image/svg+xml", outputName)
                ?: throw IllegalStateException("Failed to save SVG")
            tmp.delete()
            emit(ConversionResult.Progress(100))
            emit(ConversionResult.Success(
                outputUris = listOf(outUri),
                outputNames = listOf(outputName),
                inputSizeBytes = fileUtils.getFileSizeFromUri(context, inputUri),
                outputSizeBytes = fileUtils.getFileSizeFromUri(context, outUri),
                processingTimeMs = System.currentTimeMillis() - startMs
            ))
        } catch (e: Exception) {
            emit(ConversionResult.Error(e.message ?: "Image to SVG conversion failed"))
        }
    }.flowOn(Dispatchers.IO)
}

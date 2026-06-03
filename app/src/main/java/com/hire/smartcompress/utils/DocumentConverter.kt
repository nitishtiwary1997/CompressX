package com.hire.smartcompress.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.hire.smartcompress.domain.model.ConversionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentConverter @Inject constructor(
    private val fileUtils: FileUtils
) {
    // A4 page dimensions in points (72 dpi)
    private val PAGE_W = 595
    private val PAGE_H = 842
    private val MARGIN = 56

    // ── PDF → Images ─────────────────────────────────────────────────────────

    fun pdfToImages(context: Context, inputUri: Uri): Flow<ConversionResult> = flow {
        val startMs = System.currentTimeMillis()
        emit(ConversionResult.Progress(5, "Opening PDF…"))
        val outUris = mutableListOf<Uri>()
        val outNames = mutableListOf<String>()
        try {
            val pfd = context.contentResolver.openFileDescriptor(inputUri, "r")
                ?: throw IllegalStateException("Cannot open PDF")
            pfd.use {
                val renderer = PdfRenderer(it)
                val pageCount = renderer.pageCount
                val baseName = (fileUtils.getFileNameFromUri(context, inputUri) ?: "page")
                    .substringBeforeLast('.')
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    val scale = 2f  // 2× resolution for crisp output
                    val bmpW = (page.width * scale).toInt()
                    val bmpH = (page.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val name = "${baseName}_page${i + 1}.png"
                    val tmp = File.createTempFile("sc_pdf_", ".png", context.cacheDir)
                    tmp.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                    bitmap.recycle()

                    val uri = fileUtils.saveToMediaStore(context, tmp, "image/png", name)
                    tmp.delete()
                    if (uri != null) { outUris += uri; outNames += name }

                    val pct = 10 + ((i + 1).toFloat() / pageCount * 85).toInt()
                    emit(ConversionResult.Progress(pct, "Page ${i + 1} of $pageCount…"))
                }
                renderer.close()
            }
            if (outUris.isEmpty()) throw IllegalStateException("No pages extracted")
            val inSize = fileUtils.getFileSizeFromUri(context, inputUri)
            val outSize = outUris.sumOf { fileUtils.getFileSizeFromUri(context, it) }
            emit(ConversionResult.Progress(100))
            emit(ConversionResult.Success(outUris, outNames, inSize, outSize, System.currentTimeMillis() - startMs))
        } catch (e: Exception) {
            emit(ConversionResult.Error(e.message ?: "PDF to images failed"))
        }
    }.flowOn(Dispatchers.IO)

    // ── Images → PDF ─────────────────────────────────────────────────────────

    fun imagesToPdf(context: Context, inputUris: List<Uri>): Flow<ConversionResult> = flow {
        val startMs = System.currentTimeMillis()
        emit(ConversionResult.Progress(5, "Building PDF…"))
        try {
            val pdf = PdfDocument()
            inputUris.forEachIndexed { idx, uri ->
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return@forEachIndexed

                // Scale bitmap to fit A4 while keeping aspect ratio
                val scale = minOf(
                    (PAGE_W - 2 * MARGIN).toFloat() / bitmap.width,
                    (PAGE_H - 2 * MARGIN).toFloat() / bitmap.height,
                    1f
                )
                val dstW = (bitmap.width * scale).toInt()
                val dstH = (bitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmap, dstW, dstH, true)
                bitmap.recycle()

                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, idx + 1).create()
                val page = pdf.startPage(pageInfo)
                val x = (PAGE_W - dstW) / 2f
                val y = (PAGE_H - dstH) / 2f
                page.canvas.drawBitmap(scaled, x, y, null)
                scaled.recycle()
                pdf.finishPage(page)

                val pct = 10 + ((idx + 1).toFloat() / inputUris.size * 80).toInt()
                emit(ConversionResult.Progress(pct, "Image ${idx + 1} of ${inputUris.size}…"))
            }

            val outputName = "images_combined_${System.currentTimeMillis()}.pdf"
            val tmp = File.createTempFile("sc_imgs_", ".pdf", context.cacheDir)
            tmp.outputStream().use { pdf.writeTo(it) }
            pdf.close()
            emit(ConversionResult.Progress(95, "Saving…"))

            val outUri = fileUtils.saveToMediaStore(context, tmp, "application/pdf", outputName)
                ?: throw IllegalStateException("Failed to save PDF")
            tmp.delete()
            val inSize = inputUris.sumOf { fileUtils.getFileSizeFromUri(context, it) }
            val outSize = fileUtils.getFileSizeFromUri(context, outUri)
            emit(ConversionResult.Progress(100))
            emit(ConversionResult.Success(listOf(outUri), listOf(outputName), inSize, outSize, System.currentTimeMillis() - startMs))
        } catch (e: Exception) {
            emit(ConversionResult.Error(e.message ?: "Images to PDF failed"))
        }
    }.flowOn(Dispatchers.IO)

    // ── DOCX → PDF ───────────────────────────────────────────────────────────

    fun docxToPdf(context: Context, inputUri: Uri): Flow<ConversionResult> = flow {
        val startMs = System.currentTimeMillis()
        emit(ConversionResult.Progress(5, "Reading document…"))
        try {
            val paragraphs = parseDocx(context, inputUri)
            emit(ConversionResult.Progress(50, "Building PDF…"))

            val pdf = PdfDocument()
            var pageNum = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            var canvas = page.canvas
            val contentW = PAGE_W - 2 * MARGIN
            var yPos = MARGIN.toFloat()

            for (para in paragraphs) {
                val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = para.fontSize.toFloat()
                    isFakeBoldText = para.bold
                    textSkewX = if (para.italic) -0.25f else 0f
                    color = Color.BLACK
                }
                val align = when (para.alignment) {
                    "center" -> Layout.Alignment.ALIGN_CENTER
                    "right" -> Layout.Alignment.ALIGN_OPPOSITE
                    else -> Layout.Alignment.ALIGN_NORMAL
                }
                val displayText = para.text.ifEmpty { " " }
                val layout = StaticLayout.Builder
                    .obtain(displayText, 0, displayText.length, textPaint, contentW)
                    .setAlignment(align)
                    .setLineSpacing(2f, 1f)
                    .build()

                val blockH = layout.height.toFloat() + para.spacingAfter
                if (yPos + blockH > PAGE_H - MARGIN && yPos > MARGIN) {
                    pdf.finishPage(page)
                    pageNum++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
                    canvas = page.canvas
                    yPos = MARGIN.toFloat()
                }
                canvas.save()
                canvas.translate(MARGIN.toFloat(), yPos)
                layout.draw(canvas)
                canvas.restore()
                yPos += blockH
            }
            pdf.finishPage(page)

            val inputName = fileUtils.getFileNameFromUri(context, inputUri) ?: "document"
            val outputName = "${inputName.substringBeforeLast('.')}.pdf"
            val tmp = File.createTempFile("sc_docx_", ".pdf", context.cacheDir)
            tmp.outputStream().use { pdf.writeTo(it) }
            pdf.close()
            emit(ConversionResult.Progress(90, "Saving…"))

            val outUri = fileUtils.saveToMediaStore(context, tmp, "application/pdf", outputName)
                ?: throw IllegalStateException("Failed to save PDF")
            tmp.delete()
            val inSize = fileUtils.getFileSizeFromUri(context, inputUri)
            val outSize = fileUtils.getFileSizeFromUri(context, outUri)
            emit(ConversionResult.Progress(100))
            emit(ConversionResult.Success(
                listOf(outUri), listOf(outputName), inSize, outSize,
                System.currentTimeMillis() - startMs
            ))
        } catch (e: Exception) {
            emit(ConversionResult.Error(e.message ?: "Word to PDF conversion failed"))
        }
    }.flowOn(Dispatchers.IO)

    // ── DOCX parser ──────────────────────────────────────────────────────────

    private data class DocParagraph(
        val text: String,
        val fontSize: Int = 12,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val alignment: String = "left",
        val spacingAfter: Float = 8f
    )

    private fun parseDocx(context: Context, uri: Uri): List<DocParagraph> {
        val paragraphs = mutableListOf<DocParagraph>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        parseDocumentXml(zip.readBytes().inputStream(), paragraphs)
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return paragraphs.ifEmpty { listOf(DocParagraph("(Empty document)")) }
    }

    private fun parseDocumentXml(stream: InputStream, out: MutableList<DocParagraph>) {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var inPara = false
        var inRun = false
        var paraBold = false
        var paraItalic = false
        var paraSize = 12
        var paraAlign = "left"
        val paraText = StringBuilder()

        var runBold = false
        var runItalic = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name ?: ""
            when (event) {
                XmlPullParser.START_TAG -> when (tag) {
                    "w:p" -> {
                        inPara = true
                        paraBold = false; paraItalic = false; paraSize = 12; paraAlign = "left"
                        paraText.clear()
                    }
                    "w:r" -> { inRun = true; runBold = paraBold; runItalic = paraItalic }
                    "w:b" -> if (inRun) runBold = true else paraBold = true
                    "w:i" -> if (inRun) runItalic = true else paraItalic = true
                    "w:sz" -> {
                        val v = parser.getAttributeValue(null, "w:val")?.toIntOrNull() ?: 24
                        val pt = (v / 2).coerceIn(8, 72)
                        if (!inRun) paraSize = pt
                    }
                    "w:jc" -> {
                        val v = parser.getAttributeValue(null, "w:val") ?: "left"
                        paraAlign = v
                    }
                    "w:br" -> if (inPara) paraText.append('\n')
                    "w:t" -> {
                        // preserve xml:space="preserve" leading/trailing spaces
                        val space = parser.getAttributeValue(
                            "http://www.w3.org/XML/1998/namespace", "space"
                        )
                        val txt = parser.nextText()
                        paraText.append(if (space == "preserve") txt else txt.trim())
                        event = parser.eventType  // nextText already advanced parser
                        continue
                    }
                }
                XmlPullParser.END_TAG -> when (tag) {
                    "w:r" -> inRun = false
                    "w:p" -> {
                        if (inPara) {
                            out += DocParagraph(
                                text = paraText.toString(),
                                fontSize = paraSize,
                                bold = paraBold,
                                italic = paraItalic,
                                alignment = paraAlign
                            )
                        }
                        inPara = false
                    }
                }
            }
            event = parser.next()
        }
    }
}

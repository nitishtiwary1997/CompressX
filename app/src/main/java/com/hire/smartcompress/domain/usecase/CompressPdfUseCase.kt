package com.hire.smartcompress.domain.usecase

import android.content.Context
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.PdfCompressionConfig
import com.hire.smartcompress.utils.PdfCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CompressPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfCompressor: PdfCompressor
) {
    operator fun invoke(
        fileItem: FileItem,
        config: PdfCompressionConfig
    ): Flow<CompressionResult> = pdfCompressor.compress(context, fileItem, config)
}

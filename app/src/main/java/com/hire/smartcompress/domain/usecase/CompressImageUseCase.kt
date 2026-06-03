package com.hire.smartcompress.domain.usecase

import android.content.Context
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.ImageCompressionConfig
import com.hire.smartcompress.utils.ImageCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CompressImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCompressor: ImageCompressor
) {
    operator fun invoke(
        fileItem: FileItem,
        config: ImageCompressionConfig
    ): Flow<CompressionResult> = imageCompressor.compress(context, fileItem, config)
}

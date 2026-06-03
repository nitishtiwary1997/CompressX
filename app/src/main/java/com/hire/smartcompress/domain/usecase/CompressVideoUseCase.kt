package com.hire.smartcompress.domain.usecase

import android.content.Context
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.VideoCompressionConfig
import com.hire.smartcompress.utils.VideoCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CompressVideoUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoCompressor: VideoCompressor
) {
    operator fun invoke(
        fileItem: FileItem,
        config: VideoCompressionConfig
    ): Flow<CompressionResult> = videoCompressor.compress(context, fileItem, config)
}

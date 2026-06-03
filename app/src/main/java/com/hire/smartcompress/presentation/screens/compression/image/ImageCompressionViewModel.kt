package com.hire.smartcompress.presentation.screens.compression.image

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.model.ImageCompressionConfig
import com.hire.smartcompress.domain.model.ImageOutputFormat
import com.hire.smartcompress.domain.usecase.CompressImageUseCase
import com.hire.smartcompress.domain.usecase.SaveCompressionResultUseCase
import com.hire.smartcompress.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImageCompressionUiState(
    val fileName: String = "",
    val originalSize: Long = 0,
    val quality: Int = 80,
    val maxWidth: String = "",
    val maxHeight: String = "",
    val keepAspectRatio: Boolean = true,
    val outputFormat: ImageOutputFormat = ImageOutputFormat.JPEG,
    val isCompressing: Boolean = false,
    val progress: Int = 0,
    val result: CompressionResult.Success? = null,
    val error: String? = null
)

@HiltViewModel
class ImageCompressionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val compressImageUseCase: CompressImageUseCase,
    private val saveCompressionResultUseCase: SaveCompressionResultUseCase,
    private val fileUtils: FileUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageCompressionUiState())
    val uiState: StateFlow<ImageCompressionUiState> = _uiState.asStateFlow()

    fun loadFile(uri: Uri) {
        try {
            val name = fileUtils.getFileNameFromUri(context, uri) ?: "image"
            val size = fileUtils.getFileSizeFromUri(context, uri)
            _uiState.value = _uiState.value.copy(fileName = name, originalSize = size)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(fileName = uri.lastPathSegment ?: "image", originalSize = 0)
        }
    }

    fun setQuality(quality: Int) { _uiState.value = _uiState.value.copy(quality = quality) }
    fun setMaxWidth(w: String) { _uiState.value = _uiState.value.copy(maxWidth = w) }
    fun setMaxHeight(h: String) { _uiState.value = _uiState.value.copy(maxHeight = h) }
    fun setKeepAspectRatio(keep: Boolean) { _uiState.value = _uiState.value.copy(keepAspectRatio = keep) }
    fun setOutputFormat(fmt: ImageOutputFormat) { _uiState.value = _uiState.value.copy(outputFormat = fmt) }

    fun compress(uri: Uri) {
        val state = _uiState.value
        _uiState.value = state.copy(isCompressing = true, progress = 0, error = null, result = null)

        val fileItem = FileItem(
            uri = uri,
            name = state.fileName,
            extension = "jpg",
            size = state.originalSize,
            lastModified = 0L,
            mimeType = "image/jpeg",
            fileType = FileType.IMAGE
        )
        val config = ImageCompressionConfig(
            quality = state.quality,
            maxWidth = state.maxWidth.toIntOrNull(),
            maxHeight = state.maxHeight.toIntOrNull(),
            keepAspectRatio = state.keepAspectRatio,
            outputFormat = state.outputFormat
        )

        viewModelScope.launch {
            compressImageUseCase(fileItem, config).collect { result ->
                when (result) {
                    is CompressionResult.Progress ->
                        _uiState.value = _uiState.value.copy(progress = result.progressPercent)
                    is CompressionResult.Success -> {
                        saveCompressionResultUseCase(result)
                        _uiState.value = _uiState.value.copy(
                            isCompressing = false, result = result, progress = 100
                        )
                    }
                    is CompressionResult.Error ->
                        _uiState.value = _uiState.value.copy(
                            isCompressing = false, error = result.message
                        )
                }
            }
        }
    }
}

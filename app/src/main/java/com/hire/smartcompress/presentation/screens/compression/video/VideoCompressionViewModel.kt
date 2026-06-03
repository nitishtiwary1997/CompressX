package com.hire.smartcompress.presentation.screens.compression.video

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.model.VideoCompressionConfig
import com.hire.smartcompress.domain.model.VideoQuality
import com.hire.smartcompress.domain.model.VideoResolution
import com.hire.smartcompress.domain.usecase.CompressVideoUseCase
import com.hire.smartcompress.domain.usecase.SaveCompressionResultUseCase
import com.hire.smartcompress.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoCompressionUiState(
    val fileName: String = "",
    val originalSize: Long = 0,
    val resolution: VideoResolution = VideoResolution.P720,
    val quality: VideoQuality = VideoQuality.MEDIUM,
    val isCompressing: Boolean = false,
    val isPaused: Boolean = false,
    val progress: Int = 0,
    val estimatedRemainingMs: Long = 0,
    val result: CompressionResult.Success? = null,
    val error: String? = null
)

@HiltViewModel
class VideoCompressionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val compressVideoUseCase: CompressVideoUseCase,
    private val saveCompressionResultUseCase: SaveCompressionResultUseCase,
    private val fileUtils: FileUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoCompressionUiState())
    val uiState: StateFlow<VideoCompressionUiState> = _uiState.asStateFlow()
    private var compressionJob: Job? = null

    fun loadFile(uri: Uri) {
        try {
            val name = fileUtils.getFileNameFromUri(context, uri) ?: "video"
            val size = fileUtils.getFileSizeFromUri(context, uri)
            _uiState.value = _uiState.value.copy(fileName = name, originalSize = size)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(fileName = uri.lastPathSegment ?: "video", originalSize = 0)
        }
    }

    fun setResolution(r: VideoResolution) { _uiState.value = _uiState.value.copy(resolution = r) }
    fun setQuality(q: VideoQuality) { _uiState.value = _uiState.value.copy(quality = q) }

    fun compress(uri: Uri) {
        val state = _uiState.value
        _uiState.value = state.copy(isCompressing = true, progress = 0, error = null, result = null)

        val fileItem = FileItem(uri, state.fileName, "mp4", state.originalSize, 0L, "video/mp4", FileType.VIDEO)
        val config = VideoCompressionConfig(state.resolution, state.quality)

        compressionJob = viewModelScope.launch {
            compressVideoUseCase(fileItem, config).collect { result ->
                when (result) {
                    is CompressionResult.Progress ->
                        _uiState.value = _uiState.value.copy(
                            progress = result.progressPercent,
                            estimatedRemainingMs = result.estimatedRemainingMs
                        )
                    is CompressionResult.Success -> {
                        saveCompressionResultUseCase(result)
                        _uiState.value = _uiState.value.copy(isCompressing = false, result = result)
                    }
                    is CompressionResult.Error ->
                        _uiState.value = _uiState.value.copy(isCompressing = false, error = result.message)
                }
            }
        }
    }

    fun cancel() {
        compressionJob?.cancel()
        _uiState.value = _uiState.value.copy(isCompressing = false, progress = 0)
    }
}

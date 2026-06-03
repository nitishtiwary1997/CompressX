package com.hire.smartcompress.presentation.screens.compression.pdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.model.PdfCompressionConfig
import com.hire.smartcompress.domain.model.PdfCompressionLevel
import com.hire.smartcompress.domain.usecase.CompressPdfUseCase
import com.hire.smartcompress.domain.usecase.SaveCompressionResultUseCase
import com.hire.smartcompress.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PdfCompressionUiState(
    val fileName: String = "",
    val originalSize: Long = 0,
    val level: PdfCompressionLevel = PdfCompressionLevel.MEDIUM,
    val isCompressing: Boolean = false,
    val progress: Int = 0,
    val result: CompressionResult.Success? = null,
    val error: String? = null
)

@HiltViewModel
class PdfCompressionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val compressPdfUseCase: CompressPdfUseCase,
    private val saveCompressionResultUseCase: SaveCompressionResultUseCase,
    private val fileUtils: FileUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfCompressionUiState())
    val uiState: StateFlow<PdfCompressionUiState> = _uiState.asStateFlow()

    fun loadFile(uri: Uri) {
        try {
            val name = fileUtils.getFileNameFromUri(context, uri) ?: "document.pdf"
            val size = fileUtils.getFileSizeFromUri(context, uri)
            _uiState.value = _uiState.value.copy(fileName = name, originalSize = size)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(fileName = uri.lastPathSegment ?: "document.pdf", originalSize = 0)
        }
    }

    fun setLevel(level: PdfCompressionLevel) { _uiState.value = _uiState.value.copy(level = level) }

    fun compress(uri: Uri) {
        val state = _uiState.value
        _uiState.value = state.copy(isCompressing = true, progress = 0, error = null, result = null)

        val fileItem = FileItem(uri, state.fileName, "pdf", state.originalSize, 0L, "application/pdf", FileType.PDF)

        viewModelScope.launch {
            compressPdfUseCase(fileItem, PdfCompressionConfig(state.level)).collect { result ->
                when (result) {
                    is CompressionResult.Progress ->
                        _uiState.value = _uiState.value.copy(progress = result.progressPercent)
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
}

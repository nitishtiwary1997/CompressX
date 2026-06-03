package com.hire.smartcompress.presentation.screens.converter

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hire.smartcompress.domain.model.ConversionResult
import com.hire.smartcompress.domain.model.ConversionType
import com.hire.smartcompress.utils.DocumentConverter
import com.hire.smartcompress.utils.FileUtils
import com.hire.smartcompress.utils.ImageConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConverterUiState(
    val selectedType: ConversionType? = null,
    val inputUris: List<Uri> = emptyList(),
    val inputNames: List<String> = emptyList(),
    val quality: Int = 90,
    val isConverting: Boolean = false,
    val progress: Int = 0,
    val progressMessage: String = "",
    val result: ConversionResult.Success? = null,
    val error: String? = null
)

@HiltViewModel
class FormatConverterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageConverter: ImageConverter,
    private val documentConverter: DocumentConverter,
    private val fileUtils: FileUtils
) : ViewModel() {

    private val _state = MutableStateFlow(ConverterUiState())
    val state: StateFlow<ConverterUiState> = _state.asStateFlow()

    fun selectType(type: ConversionType) {
        _state.value = ConverterUiState(selectedType = type)
    }

    fun setInputUris(uris: List<Uri>) {
        val names = uris.map { fileUtils.getFileNameFromUri(context, it) ?: it.lastPathSegment ?: "file" }
        _state.value = _state.value.copy(
            inputUris = uris,
            inputNames = names,
            result = null,
            error = null,
            progress = 0
        )
    }

    fun setQuality(q: Int) { _state.value = _state.value.copy(quality = q) }

    fun convert() {
        val type = _state.value.selectedType ?: return
        val uris = _state.value.inputUris
        if (uris.isEmpty()) return

        _state.value = _state.value.copy(isConverting = true, result = null, error = null, progress = 0)
        viewModelScope.launch {
            val flow = when (type) {
                ConversionType.JPEG_TO_PNG ->
                    imageConverter.convertFormat(context, uris[0], ImageConverter.OutputFormat.PNG, 100)
                ConversionType.PNG_TO_JPEG ->
                    imageConverter.convertFormat(context, uris[0], ImageConverter.OutputFormat.JPEG, _state.value.quality)
                ConversionType.IMAGE_TO_WEBP ->
                    imageConverter.convertFormat(context, uris[0], ImageConverter.OutputFormat.WEBP, _state.value.quality)
                ConversionType.WEBP_TO_PNG ->
                    imageConverter.convertFormat(context, uris[0], ImageConverter.OutputFormat.PNG, 100)
                ConversionType.SVG_TO_PNG ->
                    imageConverter.svgToPng(context, uris[0])
                ConversionType.IMAGE_TO_SVG ->
                    imageConverter.imageToSvg(context, uris[0])
                ConversionType.PDF_TO_IMAGES ->
                    documentConverter.pdfToImages(context, uris[0])
                ConversionType.IMAGES_TO_PDF ->
                    documentConverter.imagesToPdf(context, uris)
                ConversionType.DOCX_TO_PDF ->
                    documentConverter.docxToPdf(context, uris[0])
            }
            flow.collect { result ->
                when (result) {
                    is ConversionResult.Progress ->
                        _state.value = _state.value.copy(
                            progress = result.percent,
                            progressMessage = result.message
                        )
                    is ConversionResult.Success ->
                        _state.value = _state.value.copy(
                            isConverting = false,
                            progress = 100,
                            result = result
                        )
                    is ConversionResult.Error ->
                        _state.value = _state.value.copy(
                            isConverting = false,
                            error = result.message
                        )
                }
            }
        }
    }

    fun reset() {
        _state.value = ConverterUiState(selectedType = _state.value.selectedType)
    }
}

package com.hire.smartcompress.presentation.screens.smartcompress

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hire.smartcompress.domain.model.SmartCompressResult
import com.hire.smartcompress.utils.FileUtils
import com.hire.smartcompress.utils.TargetSizeCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SizeUnit(val label: String, val multiplier: Long) {
    KB("KB", 1_024L),
    MB("MB", 1_048_576L)
}

data class SmartCompressUiState(
    val inputUris: List<Uri> = emptyList(),
    val inputNames: List<String> = emptyList(),
    val inputSizes: List<Long> = emptyList(),
    val targetText: String = "100",
    val targetUnit: SizeUnit = SizeUnit.KB,
    val isProcessing: Boolean = false,
    val currentIndex: Int = 0,
    val progress: Int = 0,
    val progressMessage: String = "",
    val results: List<SmartCompressResult.ItemSuccess> = emptyList(),
    val error: String? = null
) {
    val targetBytes: Long get() = (targetText.toLongOrNull() ?: 0L) * targetUnit.multiplier
    val isReady: Boolean get() = inputUris.isNotEmpty() && targetBytes > 0
    val isDone: Boolean get() = results.size == inputUris.size && inputUris.isNotEmpty()
}

@HiltViewModel
class SmartCompressViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val compressor: TargetSizeCompressor,
    private val fileUtils: FileUtils
) : ViewModel() {

    private val _state = MutableStateFlow(SmartCompressUiState())
    val state: StateFlow<SmartCompressUiState> = _state.asStateFlow()

    fun setTargetText(text: String) {
        if (text.all { it.isDigit() } && text.length <= 6)
            _state.value = _state.value.copy(targetText = text, error = null)
    }

    fun setTargetUnit(unit: SizeUnit) { _state.value = _state.value.copy(targetUnit = unit) }

    fun setInputUris(uris: List<Uri>) {
        val names = uris.map { fileUtils.getFileNameFromUri(context, it) ?: it.lastPathSegment ?: "file" }
        val sizes = uris.map { fileUtils.getFileSizeFromUri(context, it) }
        _state.value = _state.value.copy(
            inputUris = uris, inputNames = names, inputSizes = sizes,
            results = emptyList(), error = null, progress = 0
        )
    }

    fun startCompression() {
        val s = _state.value
        if (!s.isReady) return
        _state.value = s.copy(isProcessing = true, results = emptyList(), error = null, progress = 0)

        viewModelScope.launch {
            val allResults = mutableListOf<SmartCompressResult.ItemSuccess>()
            for ((idx, uri) in s.inputUris.withIndex()) {
                _state.value = _state.value.copy(currentIndex = idx)
                compressor.compress(context, uri, s.targetBytes).collect { result ->
                    when (result) {
                        is SmartCompressResult.Progress -> {
                            val overall = ((idx.toFloat() / s.inputUris.size) +
                                    (result.percent / 100f / s.inputUris.size)) * 100
                            _state.value = _state.value.copy(
                                progress = overall.toInt(),
                                progressMessage = if (s.inputUris.size > 1)
                                    "Image ${idx + 1}/${s.inputUris.size}: ${result.message}"
                                else result.message
                            )
                        }
                        is SmartCompressResult.ItemSuccess -> allResults.add(result)
                        is SmartCompressResult.Error ->
                            _state.value = _state.value.copy(error = result.message)
                    }
                }
            }
            _state.value = _state.value.copy(
                isProcessing = false,
                results = allResults,
                progress = 100
            )
        }
    }

    fun reset() { _state.value = SmartCompressUiState() }
}

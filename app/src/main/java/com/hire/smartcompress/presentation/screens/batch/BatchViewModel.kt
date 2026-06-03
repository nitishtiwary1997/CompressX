package com.hire.smartcompress.presentation.screens.batch

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hire.smartcompress.domain.model.BatchCompressionState
import com.hire.smartcompress.domain.model.BatchItem
import com.hire.smartcompress.domain.model.BatchItemStatus
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.domain.model.ImageCompressionConfig
import com.hire.smartcompress.domain.model.PdfCompressionConfig
import com.hire.smartcompress.domain.model.VideoCompressionConfig
import com.hire.smartcompress.domain.usecase.CompressImageUseCase
import com.hire.smartcompress.domain.usecase.CompressPdfUseCase
import com.hire.smartcompress.domain.usecase.CompressVideoUseCase
import com.hire.smartcompress.domain.usecase.SaveCompressionResultUseCase
import com.hire.smartcompress.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BatchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val compressImageUseCase: CompressImageUseCase,
    private val compressVideoUseCase: CompressVideoUseCase,
    private val compressPdfUseCase: CompressPdfUseCase,
    private val saveCompressionResultUseCase: SaveCompressionResultUseCase,
    private val fileUtils: FileUtils
) : ViewModel() {

    private val _state = MutableStateFlow(BatchCompressionState())
    val state: StateFlow<BatchCompressionState> = _state.asStateFlow()
    private var batchJob: Job? = null

    fun addFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val newItems = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    val name = fileUtils.getFileNameFromUri(context, uri) ?: "file"
                    val mimeType = fileUtils.getMimeType(context, uri)
                    val ext = name.substringAfterLast('.', "")
                    val fileType = FileType.fromMimeType(mimeType)
                    val cached = fileUtils.copyUriToCacheFile(context, uri, name)
                    val localUri = cached?.let { Uri.fromFile(it) } ?: uri
                    val size = cached?.length() ?: fileUtils.getFileSizeFromUri(context, uri)
                    BatchItem(FileItem(localUri, name, ext, size, cached?.lastModified() ?: 0L, mimeType, fileType))
                }
            }
            _state.value = _state.value.copy(items = _state.value.items + newItems)
        }
    }

    fun removeItem(index: Int) {
        val items = _state.value.items.toMutableList()
        if (index in items.indices) {
            items.removeAt(index)
            _state.value = _state.value.copy(items = items)
        }
    }

    fun startBatch() {
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(isRunning = true, isPaused = false)

        batchJob = viewModelScope.launch {
            val items = _state.value.items.toMutableList()
            items.forEachIndexed { index, batchItem ->
                if (batchItem.status == BatchItemStatus.COMPLETED) return@forEachIndexed
                updateItem(index, batchItem.copy(status = BatchItemStatus.PROCESSING))

                try {
                    val result = when (batchItem.fileItem.fileType) {
                        FileType.IMAGE -> compressImage(batchItem.fileItem)
                        FileType.VIDEO -> compressVideo(batchItem.fileItem)
                        FileType.PDF -> compressPdf(batchItem.fileItem)
                        else -> null
                    }
                    if (result != null) {
                        saveCompressionResultUseCase(result)
                        updateItem(index, batchItem.copy(status = BatchItemStatus.COMPLETED, progress = 100, result = result))
                    } else {
                        updateItem(index, batchItem.copy(status = BatchItemStatus.FAILED))
                    }
                } catch (e: Exception) {
                    updateItem(index, batchItem.copy(status = BatchItemStatus.FAILED))
                }

                val completed = _state.value.items.count { it.status == BatchItemStatus.COMPLETED }
                val total = _state.value.items.size
                _state.value = _state.value.copy(totalProgress = if (total > 0) (completed * 100 / total) else 0)
            }
            _state.value = _state.value.copy(isRunning = false)
        }
    }

    fun cancelBatch() {
        batchJob?.cancel()
        _state.value = _state.value.copy(isRunning = false, isPaused = false)
        val items = _state.value.items.map {
            if (it.status == BatchItemStatus.PROCESSING) it.copy(status = BatchItemStatus.CANCELLED)
            else it
        }
        _state.value = _state.value.copy(items = items)
    }

    fun clearAll() {
        cancelBatch()
        _state.value = BatchCompressionState()
    }

    private suspend fun compressImage(fileItem: FileItem): CompressionResult.Success? {
        var result: CompressionResult.Success? = null
        compressImageUseCase(fileItem, ImageCompressionConfig()).collect {
            if (it is CompressionResult.Success) result = it
        }
        return result
    }

    private suspend fun compressVideo(fileItem: FileItem): CompressionResult.Success? {
        var result: CompressionResult.Success? = null
        compressVideoUseCase(fileItem, VideoCompressionConfig()).collect {
            if (it is CompressionResult.Success) result = it
        }
        return result
    }

    private suspend fun compressPdf(fileItem: FileItem): CompressionResult.Success? {
        var result: CompressionResult.Success? = null
        compressPdfUseCase(fileItem, PdfCompressionConfig()).collect {
            if (it is CompressionResult.Success) result = it
        }
        return result
    }

    private fun updateItem(index: Int, item: BatchItem) {
        val items = _state.value.items.toMutableList()
        if (index in items.indices) {
            items[index] = item
            _state.value = _state.value.copy(items = items.toList())
        }
    }
}

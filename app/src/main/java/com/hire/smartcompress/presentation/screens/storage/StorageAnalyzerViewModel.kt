package com.hire.smartcompress.presentation.screens.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class StorageAnalyzerUiState(
    val isScanning: Boolean = false,
    val largeImages: List<FileItem> = emptyList(),
    val largeVideos: List<FileItem> = emptyList(),
    val largePdfs: List<FileItem> = emptyList(),
    val totalImages: Int = 0,
    val totalVideos: Int = 0,
    val totalImageSize: Long = 0,
    val totalVideoSize: Long = 0,
    val hasScanned: Boolean = false
)

@HiltViewModel
class StorageAnalyzerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageAnalyzerUiState())
    val uiState: StateFlow<StorageAnalyzerUiState> = _uiState.asStateFlow()

    fun scanStorage() {
        _uiState.value = _uiState.value.copy(isScanning = true)
        viewModelScope.launch {
            val (images, imgSize) = withContext(Dispatchers.IO) { queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/") }
            val (videos, vidSize) = withContext(Dispatchers.IO) { queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/") }

            val largeImages = images.sortedByDescending { it.size }.take(20)
            val largeVideos = videos.sortedByDescending { it.size }.take(20)

            _uiState.value = StorageAnalyzerUiState(
                isScanning = false,
                largeImages = largeImages,
                largeVideos = largeVideos,
                totalImages = images.size,
                totalVideos = videos.size,
                totalImageSize = imgSize,
                totalVideoSize = vidSize,
                hasScanned = true
            )
        }
    }

    private fun queryMedia(uri: Uri, mimePrefix: String): Pair<List<FileItem>, Long> {
        val items = mutableListOf<FileItem>()
        var totalSize = 0L
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        context.contentResolver.query(uri, projection, null, null, "${MediaStore.MediaColumns.SIZE} DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val size = cursor.getLong(sizeCol)
                val mime = cursor.getString(mimeCol) ?: "$mimePrefix*"
                val date = cursor.getLong(dateCol)
                val fileUri = Uri.withAppendedPath(uri, id.toString())
                val ext = name.substringAfterLast('.', "")
                totalSize += size
                items.add(FileItem(fileUri, name, ext, size, date * 1000L, mime, FileType.fromMimeType(mime)))
            }
        }
        return items to totalSize
    }
}

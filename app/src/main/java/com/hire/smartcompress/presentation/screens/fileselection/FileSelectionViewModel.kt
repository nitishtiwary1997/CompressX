package com.hire.smartcompress.presentation.screens.fileselection

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FileSelectionUiState(
    val selectedFiles: List<FileItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FileSelectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileUtils: FileUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileSelectionUiState())
    val uiState: StateFlow<FileSelectionUiState> = _uiState.asStateFlow()

    fun processPickedUris(uris: List<Uri>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val items = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    try {
                        val name = fileUtils.getFileNameFromUri(context, uri)
                            ?: uri.lastPathSegment
                            ?: return@mapNotNull null
                        val mimeType = fileUtils.getMimeType(context, uri)
                        val ext = name.substringAfterLast('.', "")
                        // Copy to cache while the temporary URI permission is active.
                        // All subsequent operations use the local file:// URI — no URI
                        // permissions required after this point.
                        val cached = fileUtils.copyUriToCacheFile(context, uri, name)
                        val localUri = cached?.let { Uri.fromFile(it) } ?: uri
                        val size = cached?.length() ?: fileUtils.getFileSizeFromUri(context, uri)
                        FileItem(
                            uri = localUri,
                            name = name,
                            extension = ext,
                            size = size,
                            lastModified = cached?.lastModified() ?: 0L,
                            mimeType = mimeType,
                            fileType = FileType.fromMimeType(mimeType)
                        )
                    } catch (e: Exception) { null }
                }
            }
            _uiState.value = FileSelectionUiState(selectedFiles = items, isLoading = false)
        }
    }

    fun removeFile(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            selectedFiles = _uiState.value.selectedFiles.filter { it.uri != uri }
        )
    }
}

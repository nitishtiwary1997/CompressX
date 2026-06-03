package com.hire.smartcompress.presentation.screens.fileselection

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.presentation.components.AppTopBar
import com.hire.smartcompress.presentation.components.EmptyStateView
import com.hire.smartcompress.presentation.components.FileTypeBadge
import com.hire.smartcompress.presentation.components.PrimaryButton
import com.hire.smartcompress.utils.FormatUtils

@Composable
fun FileSelectionScreen(
    fileTypeFilter: String,
    onFileSelected: (Uri, String) -> Unit,
    onBack: () -> Unit,
    viewModel: FileSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val mimeTypes = when (fileTypeFilter) {
        "image" -> arrayOf("image/jpeg", "image/png", "image/webp")
        "video" -> arrayOf("video/mp4", "video/quicktime", "video/x-matroska", "video/x-msvideo")
        "pdf" -> arrayOf("application/pdf")
        else -> arrayOf("image/*", "video/*", "application/pdf")
    }

    val singleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) {}
            viewModel.processPickedUris(listOf(it))
        }
    }

    val multiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                catch (_: SecurityException) {}
            }
            viewModel.processPickedUris(uris)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = when (fileTypeFilter) {
                    "image" -> "Select Images"
                    "video" -> "Select Videos"
                    "pdf" -> "Select PDFs"
                    else -> "Select Files"
                },
                onBack = onBack
            )
        },
        bottomBar = {
            if (state.selectedFiles.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                        Text(
                            "${state.selectedFiles.size} file(s) selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (state.selectedFiles.size == 1) {
                            val file = state.selectedFiles.first()
                            PrimaryButton(
                                text = "Compress File",
                                onClick = { onFileSelected(file.uri, file.mimeType) },
                                icon = Icons.Default.Compress
                            )
                        } else {
                            PrimaryButton(
                                text = "Compress All (${state.selectedFiles.size})",
                                onClick = {
                                    val first = state.selectedFiles.first()
                                    onFileSelected(first.uri, first.mimeType)
                                },
                                icon = Icons.Default.Compress
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Pick buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { singleLauncher.launch(mimeTypes) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Single File")
                }
                OutlinedButton(
                    onClick = { multiLauncher.launch(mimeTypes) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Multiple Files")
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.selectedFiles.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.FolderOpen,
                    title = "No files selected",
                    subtitle = "Tap 'Single File' or 'Multiple Files' to pick files for compression",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.selectedFiles, key = { it.uri.toString() }) { file ->
                        FileItemRow(
                            file = file,
                            onRemove = { viewModel.removeFile(file.uri) },
                            onSelect = { onFileSelected(file.uri, file.mimeType) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItemRow(
    file: FileItem,
    onRemove: () -> Unit,
    onSelect: () -> Unit
) {
    ListItem(
        headlineContent = { Text(file.name, maxLines = 1, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Column {
                Text(FormatUtils.formatSize(file.size), style = MaterialTheme.typography.bodySmall)
                FileTypeBadge(file.extension.uppercase())
            }
        },
        leadingContent = {
            when (file.fileType) {
                FileType.IMAGE -> AsyncImage(
                    model = file.uri,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                FileType.VIDEO -> Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VideoFile, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(28.dp))
                    }
                }
                FileType.PDF -> Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(28.dp))
                    }
                }
                else -> Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onSelect) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Select")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

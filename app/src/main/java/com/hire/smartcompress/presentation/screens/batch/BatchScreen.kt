package com.hire.smartcompress.presentation.screens.batch

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.hire.smartcompress.domain.model.BatchItemStatus
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.presentation.components.*
import com.hire.smartcompress.ui.theme.ImageTypeColor
import com.hire.smartcompress.ui.theme.PdfTypeColor
import com.hire.smartcompress.ui.theme.SuccessGreen
import com.hire.smartcompress.ui.theme.VideoTypeColor
import com.hire.smartcompress.utils.FormatUtils

@Composable
fun BatchScreen(
    onBack: () -> Unit,
    viewModel: BatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                catch (_: SecurityException) {}
            }
            viewModel.addFiles(uris)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Batch Compression",
                onBack = onBack,
                actions = {
                    if (state.items.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearAll) {
                            Icon(Icons.Default.Clear, "Clear All")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.items.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.isRunning) {
                            CompressionProgressBar(
                                progress = state.totalProgress / 100f,
                                label = "Overall progress: ${state.completedCount}/${state.items.size}"
                            )
                            OutlinedButton(onClick = viewModel::cancelBatch, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cancel Batch")
                            }
                        } else {
                            PrimaryButton(
                                text = "Start Batch (${state.items.size} files)",
                                onClick = viewModel::startBatch,
                                icon = Icons.Default.PlayArrow
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Add files button
            OutlinedButton(
                onClick = { launcher.launch(arrayOf("image/*", "video/*", "application/pdf")) },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Files")
            }

            if (state.items.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.LibraryAdd,
                    title = "No files in queue",
                    subtitle = "Add images, videos, or PDFs to compress them all at once",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(state.items) { index, item ->
                        val (icon, color) = when (item.fileItem.fileType) {
                            FileType.IMAGE -> Icons.Default.Image to ImageTypeColor
                            FileType.VIDEO -> Icons.Default.VideoFile to VideoTypeColor
                            FileType.PDF -> Icons.Default.PictureAsPdf to PdfTypeColor
                            else -> Icons.AutoMirrored.Filled.InsertDriveFile to MaterialTheme.colorScheme.outline
                        }
                        ListItem(
                            headlineContent = {
                                Text(item.fileItem.name, maxLines = 1, fontWeight = FontWeight.Medium)
                            },
                            supportingContent = {
                                Column {
                                    Text(FormatUtils.formatSize(item.fileItem.size),
                                        style = MaterialTheme.typography.bodySmall)
                                    if (item.status == BatchItemStatus.PROCESSING) {
                                        Spacer(Modifier.height(4.dp))
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            },
                            leadingContent = {
                                Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
                                    Icon(icon, null, tint = color,
                                        modifier = Modifier.padding(8.dp).size(24.dp))
                                }
                            },
                            trailingContent = {
                                when (item.status) {
                                    BatchItemStatus.PENDING -> {
                                        if (!state.isRunning) {
                                            IconButton(onClick = { viewModel.removeItem(index) }) {
                                                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                    BatchItemStatus.PROCESSING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    BatchItemStatus.COMPLETED -> Icon(Icons.Default.CheckCircle, "Done", tint = SuccessGreen)
                                    BatchItemStatus.FAILED -> Icon(Icons.Default.Error, "Failed", tint = MaterialTheme.colorScheme.error)
                                    BatchItemStatus.CANCELLED -> Icon(Icons.Default.Cancel, "Cancelled", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

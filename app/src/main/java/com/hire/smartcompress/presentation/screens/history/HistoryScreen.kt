package com.hire.smartcompress.presentation.screens.history

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
import com.hire.smartcompress.domain.model.CompressionHistory
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.presentation.components.*
import com.hire.smartcompress.ui.theme.ImageTypeColor
import com.hire.smartcompress.ui.theme.PdfTypeColor
import com.hire.smartcompress.ui.theme.VideoTypeColor
import com.hire.smartcompress.utils.FormatUtils

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Clear History") },
            text = { Text("Delete all compression history? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAll(); showDeleteAllDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete All")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "History",
                onBack = onBack,
                actions = {
                    if (state.items.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, "Clear All")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search files…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter chips
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = state.filterType == null, onClick = { viewModel.setFilter(null) }, label = { Text("All") })
                FilterChip(selected = state.filterType == FileType.IMAGE, onClick = { viewModel.setFilter(FileType.IMAGE) }, label = { Text("Images") })
                FilterChip(selected = state.filterType == FileType.VIDEO, onClick = { viewModel.setFilter(FileType.VIDEO) }, label = { Text("Videos") })
                FilterChip(selected = state.filterType == FileType.PDF, onClick = { viewModel.setFilter(FileType.PDF) }, label = { Text("PDFs") })
            }

            Spacer(Modifier.height(8.dp))

            if (state.isLoading) {
                LazyColumn {
                    items(5) { SkeletonHistoryItem() }
                }
            } else if (state.items.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.History,
                    title = "No history found",
                    subtitle = if (state.searchQuery.isNotEmpty()) "No results for \"${state.searchQuery}\"" else "Compressed files will appear here",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.items, key = { it.id }) { history ->
                        HistoryItemRow(history, onDelete = { viewModel.deleteItem(it) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItemRow(
    history: CompressionHistory,
    onDelete: (Long) -> Unit
) {
    val (icon, color) = when (history.fileType) {
        FileType.IMAGE -> Icons.Default.Image to ImageTypeColor
        FileType.VIDEO -> Icons.Default.VideoFile to VideoTypeColor
        FileType.PDF -> Icons.Default.PictureAsPdf to PdfTypeColor
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to MaterialTheme.colorScheme.outline
    }
    ListItem(
        headlineContent = { Text(history.fileName, maxLines = 1, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Column {
                Text(
                    "${FormatUtils.formatSize(history.originalSize)} → ${FormatUtils.formatSize(history.compressedSize)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(FormatUtils.formatDate(history.compressionDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        leadingContent = {
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
                Icon(icon, null, tint = color, modifier = Modifier.padding(8.dp).size(24.dp))
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "-${FormatUtils.formatPercent(history.savedPercent)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = { onDelete(history.id) }) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp))
                }
            }
        }
    )
}

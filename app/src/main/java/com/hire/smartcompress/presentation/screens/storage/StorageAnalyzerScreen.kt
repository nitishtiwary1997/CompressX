package com.hire.smartcompress.presentation.screens.storage

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
import com.hire.smartcompress.domain.model.FileItem
import com.hire.smartcompress.domain.model.FileType
import com.hire.smartcompress.presentation.components.*
import com.hire.smartcompress.ui.theme.ImageTypeColor
import com.hire.smartcompress.ui.theme.VideoTypeColor
import com.hire.smartcompress.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(
    onBack: () -> Unit,
    viewModel: StorageAnalyzerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { AppTopBar("Storage Analyzer", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (!state.hasScanned) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Storage Analyzer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Scan your device to find large files that can be compressed to save storage space.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PrimaryButton(
                            text = if (state.isScanning) "Scanning…" else "Scan Storage",
                            onClick = viewModel::scanStorage,
                            enabled = !state.isScanning,
                            icon = Icons.Default.Search
                        )
                        if (state.isScanning) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            } else {
                // Stats overview
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Images",
                            value = "${state.totalImages} (${FormatUtils.formatSize(state.totalImageSize)})",
                            icon = Icons.Default.Image,
                            tint = ImageTypeColor,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Videos",
                            value = "${state.totalVideos} (${FormatUtils.formatSize(state.totalVideoSize)})",
                            icon = Icons.Default.VideoFile,
                            tint = VideoTypeColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Rescan button
                item {
                    OutlinedButton(
                        onClick = viewModel::scanStorage,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rescan")
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Large Images
                if (state.largeImages.isNotEmpty()) {
                    item { SectionHeader("Largest Images (Top 20)") }
                    items(state.largeImages) { file ->
                        StorageFileRow(file)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                // Large Videos
                if (state.largeVideos.isNotEmpty()) {
                    item { SectionHeader("Largest Videos (Top 20)") }
                    items(state.largeVideos) { file ->
                        StorageFileRow(file)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageFileRow(file: FileItem) {
    val (icon, color) = when (file.fileType) {
        FileType.IMAGE -> Icons.Default.Image to ImageTypeColor
        FileType.VIDEO -> Icons.Default.VideoFile to VideoTypeColor
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to MaterialTheme.colorScheme.outline
    }
    ListItem(
        headlineContent = { Text(file.name, maxLines = 1, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(FormatUtils.formatSize(file.size), style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = {
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
                Icon(icon, null, tint = color, modifier = Modifier.padding(8.dp).size(24.dp))
            }
        },
        trailingContent = {
            FileTypeBadge(file.extension.uppercase())
        }
    )
}

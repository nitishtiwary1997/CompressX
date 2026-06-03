package com.hire.smartcompress.presentation.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToFileSelection: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBatch: () -> Unit,
    onNavigateToConverter: () -> Unit = {},
    onNavigateToImagePdf: () -> Unit = {},
    onNavigateToSmartCompress: () -> Unit = {},
    onNavigateToCrop: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadStats() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SmartCompress", fontWeight = FontWeight.Bold)
                        Text("File Resizer Pro", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToStorage) {
                        Icon(Icons.Default.Storage, contentDescription = "Storage Analyzer")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Quick actions
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader("Compress Files", modifier = Modifier.padding(0.dp))
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                QuickActionCard("Images", Icons.Default.Image, ImageTypeColor) {
                                    onNavigateToFileSelection("image")
                                }
                            }
                            item {
                                QuickActionCard("Videos", Icons.Default.VideoFile, VideoTypeColor) {
                                    onNavigateToFileSelection("video")
                                }
                            }
                            item {
                                QuickActionCard("PDFs", Icons.Default.PictureAsPdf, PdfTypeColor) {
                                    onNavigateToFileSelection("pdf")
                                }
                            }
                            item {
                                QuickActionCard("Batch", Icons.Default.LibraryAdd,
                                    MaterialTheme.colorScheme.tertiary) {
                                    onNavigateToBatch()
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        SectionHeader("Convert Files", modifier = Modifier.padding(0.dp))
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                QuickActionCard("Format", Icons.Default.SwapHoriz,
                                    MaterialTheme.colorScheme.secondary) { onNavigateToConverter() }
                            }
                            item {
                                QuickActionCard("SVG↔PNG", Icons.Default.AutoAwesome,
                                    MaterialTheme.colorScheme.tertiary) { onNavigateToConverter() }
                            }
                            item {
                                QuickActionCard("Word→PDF", Icons.Default.Description,
                                    PdfTypeColor) { onNavigateToConverter() }
                            }
                            item {
                                QuickActionCard("Image→PDF", Icons.Default.PictureAsPdf,
                                    PdfTypeColor) { onNavigateToImagePdf() }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        SectionHeader("Smart Tools", modifier = Modifier.padding(0.dp))
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                QuickActionCard(
                                    "Target Size",
                                    Icons.Default.Tune,
                                    MaterialTheme.colorScheme.error
                                ) { onNavigateToSmartCompress() }
                            }
                            item {
                                QuickActionCard(
                                    "Crop Image",
                                    Icons.Default.Crop,
                                    MaterialTheme.colorScheme.secondary
                                ) { onNavigateToCrop() }
                            }
                        }
                    }
                }
            }

            // Stats
            item {
                SectionHeader("Compression Stats")
                if (state.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(2) { SkeletonStatCard(modifier = Modifier.weight(1f)) }
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Files Compressed",
                                value = "${state.stats.totalFilesCompressed}",
                                icon = Icons.Default.Compress,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Storage Saved",
                                value = FormatUtils.formatSize(state.stats.totalStorageSaved),
                                icon = Icons.Default.Save,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Images",
                                value = "${state.stats.imageCount}",
                                icon = Icons.Default.Image,
                                tint = ImageTypeColor,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Videos",
                                value = "${state.stats.videoCount}",
                                icon = Icons.Default.VideoFile,
                                tint = VideoTypeColor,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "PDFs",
                                value = "${state.stats.pdfCount}",
                                icon = Icons.Default.PictureAsPdf,
                                tint = PdfTypeColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Recent Activity
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onNavigateToHistory) { Text("See All") }
                }
            }

            if (state.isLoading) {
                items(3) { SkeletonHistoryItem() }
            } else if (state.stats.recentHistory.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.History,
                        title = "No compression history",
                        subtitle = "Start compressing files to see your activity here"
                    )
                }
            } else {
                items(state.stats.recentHistory) { history ->
                    RecentHistoryItem(history)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(width = 100.dp, height = 100.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = color)
        }
    }
}

@Composable
private fun RecentHistoryItem(history: CompressionHistory) {
    val (icon, color) = when (history.fileType) {
        FileType.IMAGE -> Icons.Default.Image to ImageTypeColor
        FileType.VIDEO -> Icons.Default.VideoFile to VideoTypeColor
        FileType.PDF -> Icons.Default.PictureAsPdf to PdfTypeColor
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to MaterialTheme.colorScheme.outline
    }
    ListItem(
        headlineContent = { Text(history.fileName, maxLines = 1) },
        supportingContent = {
            Text(
                "${FormatUtils.formatSize(history.originalSize)} → ${FormatUtils.formatSize(history.compressedSize)}",
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(8.dp).size(24.dp))
            }
        },
        trailingContent = {
            Text(
                "-${FormatUtils.formatPercent(history.savedPercent)}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    )
}

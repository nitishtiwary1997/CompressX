package com.hire.smartcompress.presentation.screens.compression.video

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.VideoQuality
import com.hire.smartcompress.domain.model.VideoResolution
import com.hire.smartcompress.presentation.components.*
import com.hire.smartcompress.utils.FormatUtils

@Composable
fun VideoCompressionScreen(
    fileUri: Uri,
    onCompressionComplete: (CompressionResult.Success) -> Unit,
    onBack: () -> Unit,
    viewModel: VideoCompressionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(fileUri) { viewModel.loadFile(fileUri) }
    LaunchedEffect(state.result) { state.result?.let { onCompressionComplete(it) } }

    Scaffold(
        topBar = { AppTopBar("Compress Video", onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isCompressing) {
                        OutlinedButton(onClick = viewModel::cancel, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel")
                        }
                    } else {
                        PrimaryButton(
                            text = "Compress Video",
                            onClick = { viewModel.compress(fileUri) },
                            icon = Icons.Default.VideoFile
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // File info card
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Icon(Icons.Default.VideoFile, null, modifier = Modifier.padding(12.dp).size(28.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(state.fileName, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(FormatUtils.formatSize(state.originalSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Resolution
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Target Resolution", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    VideoResolution.entries.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { res ->
                                FilterChip(
                                    selected = state.resolution == res,
                                    onClick = { viewModel.setResolution(res) },
                                    label = { Text(res.label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            // Quality preset
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quality Preset", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoQuality.entries.forEach { q ->
                            FilterChip(
                                selected = state.quality == q,
                                onClick = { viewModel.setQuality(q) },
                                label = { Text(q.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val desc = when (state.quality) {
                        VideoQuality.LOW -> "Smallest file, lower quality"
                        VideoQuality.MEDIUM -> "Balanced size and quality"
                        VideoQuality.HIGH -> "Good quality, larger file"
                        VideoQuality.ULTRA -> "Best quality, largest file"
                    }
                    Text(desc, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Progress
            if (state.isCompressing) {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompressionProgressBar(
                            progress = state.progress / 100f,
                            label = "Compressing video…"
                        )
                        if (state.estimatedRemainingMs > 0) {
                            Text(
                                "Estimated: ${FormatUtils.formatDuration(state.estimatedRemainingMs)} remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            state.error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

        }
    }
}

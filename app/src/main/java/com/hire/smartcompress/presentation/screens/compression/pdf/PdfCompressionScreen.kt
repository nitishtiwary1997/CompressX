package com.hire.smartcompress.presentation.screens.compression.pdf

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
import com.hire.smartcompress.domain.model.PdfCompressionLevel
import com.hire.smartcompress.presentation.components.*
import com.hire.smartcompress.utils.FormatUtils

@Composable
fun PdfCompressionScreen(
    fileUri: Uri,
    onCompressionComplete: (CompressionResult.Success) -> Unit,
    onBack: () -> Unit,
    viewModel: PdfCompressionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(fileUri) { viewModel.loadFile(fileUri) }
    LaunchedEffect(state.result) { state.result?.let { onCompressionComplete(it) } }

    Scaffold(
        topBar = { AppTopBar("Compress PDF", onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    PrimaryButton(
                        text = if (state.isCompressing) "Compressing…" else "Compress PDF",
                        onClick = { viewModel.compress(fileUri) },
                        enabled = !state.isCompressing,
                        icon = Icons.Default.Compress
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.padding(12.dp).size(28.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer)
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

            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Compression Level", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(16.dp))
                    PdfCompressionLevel.entries.forEach { level ->
                        val (icon, desc) = when (level) {
                            PdfCompressionLevel.LOW -> Icons.Default.HighQuality to "Preserves quality, moderate size reduction"
                            PdfCompressionLevel.MEDIUM -> Icons.Default.Tune to "Balanced quality and compression"
                            PdfCompressionLevel.HIGH -> Icons.Default.Compress to "Maximum compression, reduced quality"
                        }
                        Card(
                            onClick = { viewModel.setLevel(level) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.level == level)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(icon, null, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(level.label, fontWeight = FontWeight.Medium)
                                    Text(desc, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (state.level == level) {
                                    Icon(Icons.Default.CheckCircle, null,
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            if (state.isCompressing) {
                CompressionProgressBar(progress = state.progress / 100f, label = "Compressing PDF…")
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

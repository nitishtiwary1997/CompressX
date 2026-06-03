package com.hire.smartcompress.presentation.screens.compression.image

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.hire.smartcompress.domain.model.CompressionResult
import com.hire.smartcompress.domain.model.ImageOutputFormat
import com.hire.smartcompress.presentation.components.*
import com.hire.smartcompress.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCompressionScreen(
    fileUri: Uri,
    onCompressionComplete: (CompressionResult.Success) -> Unit,
    onBack: () -> Unit,
    viewModel: ImageCompressionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(fileUri) { viewModel.loadFile(fileUri) }
    LaunchedEffect(state.result) {
        state.result?.let { onCompressionComplete(it) }
    }

    Scaffold(
        topBar = { AppTopBar("Compress Image", onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    PrimaryButton(
                        text = if (state.isCompressing) "Compressing…" else "Compress Image",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview
            Card(shape = RoundedCornerShape(16.dp)) {
                AsyncImage(
                    model = fileUri,
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            // File info
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("File", state.fileName)
                    InfoRow("Original Size", FormatUtils.formatSize(state.originalSize))
                }
            }

            // Quality
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Quality", fontWeight = FontWeight.Medium)
                        Text("${state.quality}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = state.quality.toFloat(),
                        onValueChange = { viewModel.setQuality(it.toInt()) },
                        valueRange = 1f..100f,
                        steps = 98
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("100%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Dimensions
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Resize (Optional)", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.maxWidth,
                            onValueChange = viewModel::setMaxWidth,
                            label = { Text("Max Width") },
                            placeholder = { Text("e.g. 1920") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.maxHeight,
                            onValueChange = viewModel::setMaxHeight,
                            label = { Text("Max Height") },
                            placeholder = { Text("e.g. 1080") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.keepAspectRatio, onCheckedChange = viewModel::setKeepAspectRatio)
                        Text("Keep Aspect Ratio")
                    }
                }
            }

            // Output format
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Output Format", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageOutputFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = state.outputFormat == fmt,
                                onClick = { viewModel.setOutputFormat(fmt) },
                                label = { Text(fmt.name) }
                            )
                        }
                    }
                }
            }

            if (state.isCompressing) {
                CompressionProgressBar(
                    progress = state.progress / 100f,
                    label = "Compressing…"
                )
            }

            state.error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

        }
    }
}

package com.hire.smartcompress.presentation.screens.converter

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hire.smartcompress.domain.model.ConversionCategory
import com.hire.smartcompress.domain.model.ConversionResult
import com.hire.smartcompress.domain.model.ConversionType
import com.hire.smartcompress.presentation.components.AppTopBar
import com.hire.smartcompress.presentation.components.CompressionProgressBar
import com.hire.smartcompress.presentation.components.PrimaryButton
import com.hire.smartcompress.ui.theme.ImageTypeColor
import com.hire.smartcompress.ui.theme.PdfTypeColor
import com.hire.smartcompress.ui.theme.VideoTypeColor
import com.hire.smartcompress.utils.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun FormatConverterScreen(
    initialType: ConversionType? = null,
    onBack: () -> Unit,
    viewModel: FormatConverterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val singleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) {}
            viewModel.setInputUris(listOf(it))
        }
    }
    val multiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                catch (_: SecurityException) {}
            }
            viewModel.setInputUris(uris)
        }
    }

    fun launchPicker(type: ConversionType) {
        viewModel.selectType(type)
        if (type.multipleInput) multiLauncher.launch(type.inputMimes)
        else singleLauncher.launch(type.inputMimes)
    }

    // Auto-select and open picker when navigated with a pre-selected type (e.g. Image→PDF shortcut)
    var autoLaunched by remember { mutableStateOf(false) }
    if (initialType != null && !autoLaunched) {
        SideEffect {
            autoLaunched = true
            launchPicker(initialType)
        }
    }

    Scaffold(
        topBar = { AppTopBar("Format Converter", onBack) },
        bottomBar = {
            if (state.inputUris.isNotEmpty() && !state.isConverting && state.result == null) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.selectedType in listOf(
                                ConversionType.PNG_TO_JPEG,
                                ConversionType.IMAGE_TO_WEBP
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Quality", fontWeight = FontWeight.Medium)
                                Text("${state.quality}%",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = state.quality.toFloat(),
                                onValueChange = { viewModel.setQuality(it.toInt()) },
                                valueRange = 50f..100f
                            )
                        }
                        PrimaryButton(
                            text = "Convert — ${state.selectedType?.label ?: ""}",
                            onClick = { viewModel.convert() },
                            icon = Icons.Default.SwapHoriz
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Conversion type picker ──────────────────────────────────────
            if (state.result == null && !state.isConverting) {
                ConversionCategory.entries.forEach { category ->
                    item { SectionHeader(category.label) }
                    val types = ConversionType.entries.filter { it.category == category }
                    items(types.chunked(2)) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { type ->
                                ConversionTypeCard(
                                    type = type,
                                    isSelected = state.selectedType == type && state.inputUris.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    onClick = { launchPicker(type) }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Selected file(s) info ───────────────────────────────────────
            if (state.inputUris.isNotEmpty() && state.result == null) {
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FilePresent, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (state.inputUris.size == 1) "Selected File"
                                    else "${state.inputUris.size} Files Selected",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { viewModel.reset() }) { Text("Change") }
                            }
                            state.inputNames.take(3).forEach { name ->
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (state.inputNames.size > 3) {
                                Text(
                                    "+${state.inputNames.size - 3} more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Progress ────────────────────────────────────────────────────
            if (state.isConverting) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Converting…", fontWeight = FontWeight.SemiBold)
                            CompressionProgressBar(
                                progress = state.progress / 100f,
                                label = state.progressMessage.ifEmpty { "Processing…" }
                            )
                        }
                    }
                }
            }

            // ── Error ───────────────────────────────────────────────────────
            state.error?.let { err ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Column(Modifier.weight(1f)) {
                                Text("Conversion Failed", fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(err, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) { Text("Try Again") }
                }
            }

            // ── Success result ──────────────────────────────────────────────
            state.result?.let { result ->
                item { ResultCard(result = result, type = state.selectedType) }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.reset() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Convert Another") }
                        if (result.outputUris.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "*/*"
                                        putExtra(Intent.EXTRA_STREAM, result.outputUris[0])
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share file"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp)); Text("Share")
                            }
                        }
                    }
                    // Download button
                    if (result.outputUris.isNotEmpty()) {
                        val scope = androidx.compose.runtime.rememberCoroutineScope()
                        val snack = androidx.compose.material3.SnackbarHostState()
                        val dlUri = result.outputUris[0]
                        val dlName = result.outputNames.firstOrNull() ?: "converted_file"
                        OutlinedButton(
                            onClick = {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val ok = runCatching {
                                        context.contentResolver.openInputStream(dlUri)?.use { input ->
                                            val values = android.content.ContentValues().apply {
                                                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, dlName)
                                                put(android.provider.MediaStore.Downloads.MIME_TYPE, "*/*")
                                                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                                            }
                                            val dest = context.contentResolver.insert(
                                                android.provider.MediaStore.Downloads.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY), values
                                            )!!
                                            context.contentResolver.openOutputStream(dest)?.use { input.copyTo(it) }
                                            values.clear(); values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                                            context.contentResolver.update(dest, values, null, null)
                                        }
                                        true
                                    }.isSuccess
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        snack.showSnackbar(if (ok) "Saved to Downloads ✓" else "Save failed")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding()
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Save to Downloads")
                        }
                    }
                }
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun ConversionTypeCard(
    type: ConversionType,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (icon, color) = conversionIcon(type)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    icon, contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .padding(6.dp)
                        .fillMaxSize()
                )
            }
            Text(
                type.fromLabel + " →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(type.toLabel, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium)
            Text(
                type.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ResultCard(result: ConversionResult.Success, type: ConversionType?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Conversion Complete!", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            if (result.outputUris.size == 1) {
                ConverterInfoRow("Output", result.outputNames.firstOrNull() ?: "file")
                ConverterInfoRow("Input Size", FormatUtils.formatSize(result.inputSizeBytes))
                ConverterInfoRow("Output Size", FormatUtils.formatSize(result.outputSizeBytes))
                val diff = result.inputSizeBytes - result.outputSizeBytes
                if (diff != 0L) {
                    ConverterInfoRow(
                        if (diff > 0) "Size Reduced" else "Size Increased",
                        "${FormatUtils.formatSize(kotlin.math.abs(diff))} " +
                                "(${if (diff > 0) "-" else "+"}${
                                    if (result.inputSizeBytes > 0)
                                        "%.1f".format(kotlin.math.abs(diff).toFloat() / result.inputSizeBytes * 100)
                                    else "0"
                                }%)"
                    )
                }
            } else {
                ConverterInfoRow("Pages Extracted", "${result.outputUris.size} images")
                ConverterInfoRow("Input Size", FormatUtils.formatSize(result.inputSizeBytes))
                ConverterInfoRow("Total Output Size", FormatUtils.formatSize(result.outputSizeBytes))
            }
            ConverterInfoRow("Time", "${result.processingTimeMs}ms")
        }
    }
}

@Composable
private fun ConverterInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

private fun conversionIcon(type: ConversionType): Pair<ImageVector, Color> = when (type) {
    ConversionType.JPEG_TO_PNG,
    ConversionType.PNG_TO_JPEG,
    ConversionType.IMAGE_TO_WEBP,
    ConversionType.WEBP_TO_PNG -> Icons.Default.Image to ImageTypeColor
    ConversionType.SVG_TO_PNG,
    ConversionType.IMAGE_TO_SVG -> Icons.Default.AutoAwesome to VideoTypeColor
    ConversionType.PDF_TO_IMAGES -> Icons.Default.PictureAsPdf to PdfTypeColor
    ConversionType.IMAGES_TO_PDF -> Icons.Default.LibraryBooks to PdfTypeColor
    ConversionType.DOCX_TO_PDF -> Icons.Default.Description to PdfTypeColor
}

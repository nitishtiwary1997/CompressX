package com.hire.smartcompress.presentation.screens.smartcompress

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hire.smartcompress.presentation.components.AppTopBar
import com.hire.smartcompress.presentation.components.CompressionProgressBar
import com.hire.smartcompress.presentation.components.PrimaryButton
import com.hire.smartcompress.utils.FormatUtils
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SmartCompressScreen(
    onBack: () -> Unit,
    viewModel: SmartCompressViewModel = hiltViewModel()
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

    val imageMimes = arrayOf("image/jpeg", "image/png", "image/webp")

    Scaffold(
        topBar = { AppTopBar("Smart Compress", onBack) },
        bottomBar = {
            if (state.isReady && !state.isProcessing && !state.isDone) {
                Surface(shadowElevation = 8.dp) {
                    PrimaryButton(
                        text = "Compress to ${state.targetText} ${state.targetUnit.label}",
                        onClick = { viewModel.startCompression() },
                        icon = Icons.Default.Compress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── How it works banner ───────────────────────────────────────
            if (state.inputUris.isEmpty() && !state.isDone) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Lightbulb, null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp))
                            Text(
                                "Set a target file size and let the app automatically find " +
                                "the best quality + resolution combo to hit that target. " +
                                "Works on images up to 100 MB+.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // ── Target size card ─────────────────────────────────────────
            if (!state.isDone) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Target File Size", fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = state.targetText,
                                    onValueChange = viewModel::setTargetText,
                                    label = { Text("Size") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                // Unit toggle
                                SizeUnit.entries.forEach { unit ->
                                    FilterChip(
                                        selected = state.targetUnit == unit,
                                        onClick = { viewModel.setTargetUnit(unit) },
                                        label = { Text(unit.label) }
                                    )
                                }
                            }
                            if (state.targetBytes > 0) {
                                Text(
                                    "Target: ${FormatUtils.formatSize(state.targetBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // ── File picker ───────────────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Select Images", fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { singleLauncher.launch(imageMimes) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Single")
                                }
                                OutlinedButton(
                                    onClick = { multiLauncher.launch(imageMimes) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Multiple")
                                }
                            }
                        }
                    }
                }
            }

            // ── Selected files list ───────────────────────────────────────
            if (state.inputUris.isNotEmpty() && !state.isDone && !state.isProcessing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${state.inputUris.size} image${if (state.inputUris.size > 1) "s" else ""} selected",
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.reset() }) { Text("Clear") }
                            }
                            state.inputNames.zip(state.inputSizes).forEach { (name, size) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f), maxLines = 1)
                                    Text(FormatUtils.formatSize(size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // ── Progress ──────────────────────────────────────────────────
            if (state.isProcessing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                if (state.inputUris.size > 1)
                                    "Compressing ${state.currentIndex + 1} of ${state.inputUris.size}…"
                                else "Compressing…",
                                fontWeight = FontWeight.SemiBold
                            )
                            CompressionProgressBar(
                                progress = state.progress / 100f,
                                label = state.progressMessage
                            )
                        }
                    }
                }
            }

            // ── Results ───────────────────────────────────────────────────
            if (state.isDone) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Compression Complete!",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                itemsIndexed(state.results) { _, result ->
                    ResultItemCard(result = result, context = context)
                }
                item {
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    val snack = androidx.compose.material3.SnackbarHostState()
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.weight(1f)) {
                                Text("Compress More")
                            }
                            if (state.results.size == 1) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/*"
                                            putExtra(Intent.EXTRA_STREAM, state.results[0].outputUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share image"))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp)); Text("Share")
                                }
                            }
                        }
                        // Save to Downloads
                        OutlinedButton(
                            onClick = {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    var allOk = true
                                    state.results.forEach { r ->
                                        val ok = runCatching {
                                            context.contentResolver.openInputStream(r.outputUri)?.use { input ->
                                                val values = android.content.ContentValues().apply {
                                                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, r.inputName.substringBeforeLast('.') + "_compressed.jpg")
                                                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "image/jpeg")
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
                                        if (!ok) allOk = false
                                    }
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        snack.showSnackbar(if (allOk) "Saved ${state.results.size} file(s) to Downloads ✓" else "Some files failed to save")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Save to Downloads")
                        }
                    }
                }
            }

            // ── Error ─────────────────────────────────────────────────────
            state.error?.let { err ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text(err, color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultItemCard(
    result: com.hire.smartcompress.domain.model.SmartCompressResult.ItemSuccess,
    context: android.content.Context
) {
    val achieved = result.achievedBytes
    val saved = result.originalBytes - achieved
    val savedPct = if (result.originalBytes > 0) saved * 100f / result.originalBytes else 0f

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (result.targetAchieved)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (result.targetAchieved) Icons.Default.CheckCircle else Icons.Default.Warning,
                    null,
                    tint = if (result.targetAchieved) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    if (result.targetAchieved) "Target achieved ✓" else "Best effort (target not reached)",
                    fontWeight = FontWeight.SemiBold,
                    color = if (result.targetAchieved) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                )
            }
            Text(result.inputName, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            HorizontalDivider()
            SmallRow("Original Size", FormatUtils.formatSize(result.originalBytes))
            SmallRow("Achieved Size", FormatUtils.formatSize(achieved))
            SmallRow("Target Size", FormatUtils.formatSize(result.targetBytes))
            SmallRow("Size Reduced", "${FormatUtils.formatSize(abs(saved))} (${"%.1f".format(savedPct)}%)")
            SmallRow("Quality Used", "${result.qualityUsed}%")
            if (result.scalePercent < 100) {
                SmallRow("Resolution", "${result.scalePercent}% of original")
            }
            SmallRow("Time", "${result.processingTimeMs} ms")
        }
    }
}

@Composable
private fun SmallRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

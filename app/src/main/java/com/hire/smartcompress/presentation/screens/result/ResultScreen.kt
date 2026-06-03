package com.hire.smartcompress.presentation.screens.result

import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hire.smartcompress.presentation.components.AppTopBar
import com.hire.smartcompress.presentation.components.InfoRow
import com.hire.smartcompress.presentation.components.PrimaryButton
import com.hire.smartcompress.ui.theme.SuccessGreen
import com.hire.smartcompress.utils.FormatUtils

@Composable
fun ResultScreen(
    originalSize: Long,
    compressedSize: Long,
    savedBytes: Long,
    savedPercent: Float,
    processingTimeMs: Long,
    fileType: String,
    compressedUri: Uri,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var animStarted by remember { mutableStateOf(false) }
    val animProgress by animateFloatAsState(
        targetValue = if (animStarted) savedPercent / 100f else 0f,
        animationSpec = tween(1500),
        label = "saved_percent"
    )

    LaunchedEffect(Unit) { animStarted = true }

    Scaffold(
        topBar = { AppTopBar("Compression Result") },
        snackbarHost = { SnackbarHost(snackbarState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success indicator
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animProgress },
                    modifier = Modifier.size(140.dp),
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round,
                    color = SuccessGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "-${FormatUtils.formatPercent(savedPercent)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                    Text("Saved", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                "Compression Complete!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "You saved ${FormatUtils.formatSize(savedBytes)} of storage",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Stats
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Compression Details", fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()
                    InfoRow("Original Size", FormatUtils.formatSize(originalSize))
                    InfoRow("Compressed Size", FormatUtils.formatSize(compressedSize))
                    InfoRow("Space Saved", FormatUtils.formatSize(savedBytes))
                    InfoRow("Compression Ratio",
                        "%.2fx".format(if (compressedSize > 0) originalSize.toFloat() / compressedSize else 1f))
                    InfoRow("Processing Time", FormatUtils.formatDuration(processingTimeMs))
                    InfoRow("File Type", fileType)
                }
            }

            // Actions
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Actions", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { openFile(context, compressedUri, fileType) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open")
                        }
                        OutlinedButton(
                            onClick = { shareFile(context, compressedUri, fileType) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                    val mimeForDownload = when (fileType.uppercase()) {
                        "IMAGE" -> "image/jpeg"; "VIDEO" -> "video/mp4"; "PDF" -> "application/pdf"; else -> "*/*"
                    }
                    val fileNameForDownload = compressedUri.lastPathSegment ?: "compressed_file"
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val saved = saveToDownloads(context, compressedUri, fileNameForDownload, mimeForDownload)
                                snackbarState.showSnackbar(
                                    if (saved) "Saved to Downloads folder ✓" else "Failed to save to Downloads"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save to Downloads")
                    }
                }
            }

            PrimaryButton(
                text = "Compress Another File",
                onClick = onBack,
                icon = Icons.Default.Add
            )
        }
    }
}

private fun openFile(context: Context, uri: Uri, fileType: String) {
    val mimeType = when (fileType.uppercase()) {
        "IMAGE" -> "image/*"
        "VIDEO" -> "video/*"
        "PDF" -> "application/pdf"
        else -> "*/*"
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Open with"))
}

private suspend fun saveToDownloads(context: Context, uri: Uri, name: String, mime: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val destUri = context.contentResolver.insert(
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values
                ) ?: return@withContext false
                context.contentResolver.openInputStream(uri)?.use { input ->
                    context.contentResolver.openOutputStream(destUri)?.use { input.copyTo(it) }
                }
                values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(destUri, values, null, null)
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).also { it.mkdirs() }
                context.contentResolver.openInputStream(uri)?.use { it.copyTo(File(dir, name).outputStream()) }
            }
            true
        } catch (_: Exception) { false }
    }

private fun shareFile(context: Context, uri: Uri, fileType: String) {
    val mimeType = when (fileType.uppercase()) {
        "IMAGE" -> "image/*"
        "VIDEO" -> "video/*"
        "PDF" -> "application/pdf"
        else -> "*/*"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

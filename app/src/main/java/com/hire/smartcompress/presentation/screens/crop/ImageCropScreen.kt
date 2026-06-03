package com.hire.smartcompress.presentation.screens.crop

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hire.smartcompress.presentation.components.AppTopBar
import com.hire.smartcompress.presentation.components.PrimaryButton

private enum class CropHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

@Composable
fun ImageCropScreen(
    onBack: () -> Unit,
    viewModel: ImageCropViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // crop rect in image-normalized coords [0,1]
    var cropRect by remember { mutableStateOf(Rect(0.05f, 0.05f, 0.95f, 0.95f)) }

    val imageMimes = arrayOf("image/jpeg", "image/png", "image/webp")
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) {}
            cropRect = Rect(0.05f, 0.05f, 0.95f, 0.95f)  // reset crop on new image
            viewModel.loadBitmap(it)
        }
    }

    // Auto-open picker on first load
    var pickerOpened by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!pickerOpened) {
            pickerOpened = true
            launcher.launch(imageMimes)
        }
    }

    // Adjust crop rect when aspect ratio preset changes
    LaunchedEffect(state.selectedPreset) {
        val ratio = state.selectedPreset.ratio ?: return@LaunchedEffect
        val cx = (cropRect.left + cropRect.right) / 2f
        val cy = (cropRect.top + cropRect.bottom) / 2f
        val w = cropRect.right - cropRect.left
        var newW = w
        var newH = w / ratio
        if (cy - newH / 2f < 0f || cy + newH / 2f > 1f) {
            newH = (cropRect.bottom - cropRect.top).coerceAtMost(1f)
            newW = newH * ratio
        }
        cropRect = Rect(
            (cx - newW / 2f).coerceIn(0f, 1f - newW),
            (cy - newH / 2f).coerceIn(0f, 1f - newH),
            (cx + newW / 2f).coerceIn(newW, 1f),
            (cy + newH / 2f).coerceIn(newH, 1f)
        )
    }

    Scaffold(
        topBar = { AppTopBar("Crop Image", onBack) },
        bottomBar = {
            if (state.bitmap != null && state.outputUri == null && !state.isApplying) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(AspectRatioPreset.entries) { preset ->
                                FilterChip(
                                    selected = state.selectedPreset == preset,
                                    onClick = { viewModel.setPreset(preset) },
                                    label = { Text(preset.label) }
                                )
                            }
                        }
                        PrimaryButton(
                            text = "Apply Crop",
                            onClick = { viewModel.applyCropFromState(cropRect) },
                            icon = Icons.Default.Crop,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(color = Color.White)

                state.bitmap == null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.Image, null, tint = Color.White.copy(0.4f),
                            modifier = Modifier.size(72.dp))
                        Text("No image selected", color = Color.White.copy(0.6f))
                        Button(onClick = { launcher.launch(imageMimes) }) { Text("Select Image") }
                    }
                }

                state.isApplying -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = Color.White)
                        Text("Applying crop…", color = Color.White)
                    }
                }

                state.outputUri != null -> CropResultView(
                    outputUri = state.outputUri!!,
                    context = context,
                    onCropAnother = {
                        viewModel.reset()
                        launcher.launch(imageMimes)
                    }
                )

                else -> {
                    val bitmap = state.bitmap!!
                    var containerSize by remember { mutableStateOf(Size.Zero) }

                    val imageRect = remember(containerSize, bitmap) {
                        if (containerSize == Size.Zero) return@remember Rect.Zero
                        val br = bitmap.width.toFloat() / bitmap.height
                        val cr = containerSize.width / containerSize.height
                        if (br > cr) {
                            val h = containerSize.width / br
                            val top = (containerSize.height - h) / 2f
                            Rect(0f, top, containerSize.width, top + h)
                        } else {
                            val w = containerSize.height * br
                            val left = (containerSize.width - w) / 2f
                            Rect(left, 0f, left + w, containerSize.height)
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize()
                            .onSizeChanged { containerSize = it.toSize() }
                            .clipToBounds()
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Image to crop",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        if (imageRect != Rect.Zero) {
                            CropOverlay(
                                cropRect = cropRect,
                                imageRect = imageRect,
                                modifier = Modifier.fillMaxSize(),
                                onCropChange = { cropRect = it }
                            )
                        }
                    }

                    // Tap-to-reselect hint
                    TextButton(
                        onClick = { launcher.launch(imageMimes) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Change", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            state.error?.let { err ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = { viewModel.reset() }) { Text("OK") } }
                ) { Text(err) }
            }
        }
    }
}

@Composable
private fun CropOverlay(
    cropRect: Rect,
    imageRect: Rect,
    modifier: Modifier = Modifier,
    onCropChange: (Rect) -> Unit
) {
    val density = LocalDensity.current
    val touchRadiusPx = with(density) { 40.dp.toPx() }
    val minCrop = 0.04f
    var activeHandle by remember { mutableStateOf<CropHandle?>(null) }

    fun normToScreen(nx: Float, ny: Float) = Offset(
        imageRect.left + nx * imageRect.width,
        imageRect.top + ny * imageRect.height
    )

    Canvas(
        modifier = modifier.pointerInput(cropRect, imageRect) {
            detectDragGestures(
                onDragStart = { offset ->
                    val tl = normToScreen(cropRect.left, cropRect.top)
                    val tr = normToScreen(cropRect.right, cropRect.top)
                    val bl = normToScreen(cropRect.left, cropRect.bottom)
                    val br = normToScreen(cropRect.right, cropRect.bottom)
                    activeHandle = when {
                        (offset - tl).getDistance() <= touchRadiusPx -> CropHandle.TOP_LEFT
                        (offset - tr).getDistance() <= touchRadiusPx -> CropHandle.TOP_RIGHT
                        (offset - bl).getDistance() <= touchRadiusPx -> CropHandle.BOTTOM_LEFT
                        (offset - br).getDistance() <= touchRadiusPx -> CropHandle.BOTTOM_RIGHT
                        imageRect.width > 0f && imageRect.height > 0f &&
                        offset.x in normToScreen(cropRect.left, 0f).x..normToScreen(cropRect.right, 0f).x &&
                        offset.y in normToScreen(0f, cropRect.top).y..normToScreen(0f, cropRect.bottom).y ->
                            CropHandle.MOVE
                        else -> null
                    }
                },
                onDrag = { change, drag ->
                    change.consume()
                    if (imageRect.width == 0f || imageRect.height == 0f) return@detectDragGestures
                    val dx = drag.x / imageRect.width
                    val dy = drag.y / imageRect.height
                    val c = cropRect
                    onCropChange(
                        when (activeHandle) {
                            CropHandle.TOP_LEFT -> Rect(
                                (c.left + dx).coerceIn(0f, c.right - minCrop),
                                (c.top + dy).coerceIn(0f, c.bottom - minCrop),
                                c.right, c.bottom
                            )
                            CropHandle.TOP_RIGHT -> Rect(
                                c.left,
                                (c.top + dy).coerceIn(0f, c.bottom - minCrop),
                                (c.right + dx).coerceIn(c.left + minCrop, 1f),
                                c.bottom
                            )
                            CropHandle.BOTTOM_LEFT -> Rect(
                                (c.left + dx).coerceIn(0f, c.right - minCrop),
                                c.top, c.right,
                                (c.bottom + dy).coerceIn(c.top + minCrop, 1f)
                            )
                            CropHandle.BOTTOM_RIGHT -> Rect(
                                c.left, c.top,
                                (c.right + dx).coerceIn(c.left + minCrop, 1f),
                                (c.bottom + dy).coerceIn(c.top + minCrop, 1f)
                            )
                            CropHandle.MOVE -> {
                                val nl = (c.left + dx).coerceIn(0f, 1f - c.width)
                                val nt = (c.top + dy).coerceIn(0f, 1f - c.height)
                                Rect(nl, nt, nl + c.width, nt + c.height)
                            }
                            null -> c
                        }
                    )
                },
                onDragEnd = { activeHandle = null }
            )
        }
    ) {
        val sl = imageRect.left + cropRect.left * imageRect.width
        val st = imageRect.top + cropRect.top * imageRect.height
        val sr = imageRect.left + cropRect.right * imageRect.width
        val sb = imageRect.top + cropRect.bottom * imageRect.height
        val cw = sr - sl;  val ch = sb - st

        // Dark overlay outside crop area
        val dimColor = Color.Black.copy(alpha = 0.6f)
        drawRect(dimColor, Offset.Zero, Size(size.width, st))
        drawRect(dimColor, Offset(0f, sb), Size(size.width, size.height - sb))
        drawRect(dimColor, Offset(0f, st), Size(sl, ch))
        drawRect(dimColor, Offset(sr, st), Size(size.width - sr, ch))

        // Rule-of-thirds grid lines
        val grid = Color.White.copy(alpha = 0.3f)
        for (i in 1..2) {
            drawLine(grid, Offset(sl + cw / 3 * i, st), Offset(sl + cw / 3 * i, sb), 1.5f)
            drawLine(grid, Offset(sl, st + ch / 3 * i), Offset(sr, st + ch / 3 * i), 1.5f)
        }

        // Border
        drawRect(Color.White, Offset(sl, st), Size(cw, ch), style = Stroke(2.5f))

        // L-shaped corner handles
        val hs = 52f; val ht = 5f
        listOf(
            Triple(Offset(sl, st), Offset(sl + hs, st), Offset(sl, st + hs)),   // TL
            Triple(Offset(sr, st), Offset(sr - hs, st), Offset(sr, st + hs)),   // TR
            Triple(Offset(sl, sb), Offset(sl + hs, sb), Offset(sl, sb - hs)),   // BL
            Triple(Offset(sr, sb), Offset(sr - hs, sb), Offset(sr, sb - hs))    // BR
        ).forEach { (corner, h, v) ->
            drawLine(Color.White, corner, h, ht, StrokeCap.Square)
            drawLine(Color.White, corner, v, ht, StrokeCap.Square)
        }
    }
}

@Composable
private fun CropResultView(
    outputUri: android.net.Uri,
    context: android.content.Context,
    onCropAnother: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(56.dp))
        Text("Crop Applied!", color = Color.White, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge)
        Text("Saved to Pictures/SmartCompress", color = Color.White.copy(0.6f),
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(outputUri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open with"))
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp)); Text("Open")
            }
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, outputUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share"))
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(0.5f))
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp)); Text("Share")
            }
        }
        TextButton(onClick = onCropAnother) {
            Text("Crop Another", color = Color.White.copy(0.6f))
        }
    }
}

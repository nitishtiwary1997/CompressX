package com.hire.smartcompress.presentation.screens.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hire.smartcompress.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class AspectRatioPreset(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    PORTRAIT_9_16("9:16", 9f / 16f)
}

data class CropUiState(
    val sourceUri: Uri? = null,
    val bitmap: Bitmap? = null,
    val isLoading: Boolean = true,
    val isApplying: Boolean = false,
    val selectedPreset: AspectRatioPreset = AspectRatioPreset.FREE,
    val outputUri: Uri? = null,
    val error: String? = null
)

@HiltViewModel
class ImageCropViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileUtils: FileUtils
) : ViewModel() {

    private val _state = MutableStateFlow(CropUiState())
    val state: StateFlow<CropUiState> = _state.asStateFlow()

    fun loadBitmap(uri: Uri) {
        _state.value = _state.value.copy(sourceUri = uri, isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                val maxDim = maxOf(opts.outWidth, opts.outHeight)
                val sample = when { maxDim > 8000 -> 4; maxDim > 4000 -> 2; else -> 1 }
                val decode = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decode) }
                _state.value = _state.value.copy(bitmap = bmp, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Cannot load image: ${e.message}")
            }
        }
    }

    fun setPreset(preset: AspectRatioPreset) {
        _state.value = _state.value.copy(selectedPreset = preset)
    }

    fun applyCrop(sourceUri: Uri, cropNorm: Rect) {
        val bitmap = _state.value.bitmap ?: return
        _state.value = _state.value.copy(isApplying = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bw = bitmap.width
                val bh = bitmap.height
                val x = (cropNorm.left * bw).toInt().coerceIn(0, bw - 1)
                val y = (cropNorm.top * bh).toInt().coerceIn(0, bh - 1)
                val w = ((cropNorm.right - cropNorm.left) * bw).toInt().coerceIn(1, bw - x)
                val h = ((cropNorm.bottom - cropNorm.top) * bh).toInt().coerceIn(1, bh - y)

                val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                val inputName = fileUtils.getFileNameFromUri(context, sourceUri) ?: "image"
                val outputName = "${inputName.substringBeforeLast('.')}_cropped.jpg"

                val tmp = File.createTempFile("sc_crop_", ".jpg", context.cacheDir)
                tmp.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                cropped.recycle()

                val outUri = fileUtils.saveToMediaStore(context, tmp, "image/jpeg", outputName)
                tmp.delete()

                _state.value = _state.value.copy(isApplying = false, outputUri = outUri)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isApplying = false, error = "Crop failed: ${e.message}")
            }
        }
    }

    fun applyCropFromState(cropNorm: Rect) {
        val uri = _state.value.sourceUri ?: return
        applyCrop(uri, cropNorm)
    }

    fun reset() { _state.value = CropUiState() }
}

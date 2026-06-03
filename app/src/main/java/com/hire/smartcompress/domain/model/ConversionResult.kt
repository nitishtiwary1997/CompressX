package com.hire.smartcompress.domain.model

import android.net.Uri

sealed class ConversionResult {
    data class Progress(val percent: Int, val message: String = "") : ConversionResult()
    data class Success(
        val outputUris: List<Uri>,
        val outputNames: List<String>,
        val inputSizeBytes: Long,
        val outputSizeBytes: Long,
        val processingTimeMs: Long
    ) : ConversionResult()
    data class Error(val message: String) : ConversionResult()
}

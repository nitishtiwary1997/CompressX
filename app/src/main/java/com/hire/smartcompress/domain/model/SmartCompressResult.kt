package com.hire.smartcompress.domain.model

import android.net.Uri

sealed class SmartCompressResult {
    data class Progress(val percent: Int, val message: String = "") : SmartCompressResult()
    data class ItemSuccess(
        val outputUri: Uri,
        val inputName: String,
        val originalBytes: Long,
        val achievedBytes: Long,
        val targetBytes: Long,
        val qualityUsed: Int,
        val scalePercent: Int,
        val targetAchieved: Boolean,
        val processingTimeMs: Long
    ) : SmartCompressResult()
    data class Error(val message: String) : SmartCompressResult()
}

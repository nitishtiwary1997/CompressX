package com.hire.smartcompress.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    fun formatSize(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> "%.2f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDuration(ms: Long): String = when {
        ms >= 3_600_000 -> "%d h %d m".format(ms / 3_600_000, (ms % 3_600_000) / 60_000)
        ms >= 60_000 -> "%d m %d s".format(ms / 60_000, (ms % 60_000) / 1_000)
        ms >= 1_000 -> "%.1f s".format(ms / 1_000.0)
        else -> "${ms}ms"
    }

    fun formatPercent(value: Float): String = "%.1f%%".format(value)
}

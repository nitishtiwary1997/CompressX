package com.hire.smartcompress.domain.model

import android.net.Uri

data class FileItem(
    val uri: Uri,
    val name: String,
    val extension: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
    val fileType: FileType
)

enum class FileType {
    IMAGE, VIDEO, PDF, UNKNOWN;

    companion object {
        fun fromMimeType(mimeType: String): FileType = when {
            mimeType.startsWith("image/") -> IMAGE
            mimeType.startsWith("video/") -> VIDEO
            mimeType == "application/pdf" -> PDF
            else -> UNKNOWN
        }

        fun fromExtension(ext: String): FileType = when (ext.lowercase()) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> IMAGE
            "mp4", "mov", "mkv", "avi", "3gp", "wmv" -> VIDEO
            "pdf" -> PDF
            else -> UNKNOWN
        }
    }
}

package com.hire.smartcompress.domain.model

data class ImageCompressionConfig(
    val quality: Int = 80,
    val maxWidth: Int? = null,
    val maxHeight: Int? = null,
    val keepAspectRatio: Boolean = true,
    val outputFormat: ImageOutputFormat = ImageOutputFormat.JPEG
)

enum class ImageOutputFormat { JPEG, PNG, WEBP }

data class VideoCompressionConfig(
    val resolution: VideoResolution = VideoResolution.P720,
    val quality: VideoQuality = VideoQuality.MEDIUM,
    val bitrateMbps: Float? = null
)

enum class VideoResolution(val width: Int, val height: Int, val label: String) {
    P240(426, 240, "240p"),
    P360(640, 360, "360p"),
    P480(854, 480, "480p"),
    P720(1280, 720, "720p"),
    P1080(1920, 1080, "1080p")
}

enum class VideoQuality(val crf: Int, val label: String) {
    LOW(35, "Low"),
    MEDIUM(28, "Medium"),
    HIGH(23, "High"),
    ULTRA(18, "Ultra")
}

data class PdfCompressionConfig(
    val level: PdfCompressionLevel = PdfCompressionLevel.MEDIUM
)

enum class PdfCompressionLevel(val imageQuality: Int, val label: String) {
    LOW(40, "Low"),
    MEDIUM(60, "Medium"),
    HIGH(80, "High")
}

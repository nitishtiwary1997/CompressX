package com.hire.smartcompress.presentation.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object FileSelection : Screen("file_selection/{type}") {
        fun createRoute(type: String) = "file_selection/$type"
    }
    object ImageCompression : Screen("image_compression/{uri}") {
        fun createRoute(encodedUri: String) = "image_compression/$encodedUri"
    }
    object VideoCompression : Screen("video_compression/{uri}") {
        fun createRoute(encodedUri: String) = "video_compression/$encodedUri"
    }
    object PdfCompression : Screen("pdf_compression/{uri}") {
        fun createRoute(encodedUri: String) = "pdf_compression/$encodedUri"
    }
    object Batch : Screen("batch")
    object Result : Screen("result/{originalSize}/{compressedSize}/{savedBytes}/{savedPercent}/{processingTime}/{fileType}/{compressedUri}") {
        fun createRoute(
            originalSize: Long, compressedSize: Long, savedBytes: Long,
            savedPercent: Float, processingTime: Long, fileType: String, encodedUri: String
        ) = "result/$originalSize/$compressedSize/$savedBytes/$savedPercent/$processingTime/$fileType/$encodedUri"
    }
    object History : Screen("history")
    object Settings : Screen("settings")
    object StorageAnalyzer : Screen("storage_analyzer")
    object FormatConverter : Screen("format_converter?type={type}") {
        fun createRoute(type: String? = null) =
            if (type != null) "format_converter?type=$type" else "format_converter?type="
    }
    object SmartCompress : Screen("smart_compress")
    object ImageCrop : Screen("image_crop")
}

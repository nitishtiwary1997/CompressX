package com.hire.smartcompress.domain.model

enum class ConversionType(
    val label: String,
    val fromLabel: String,
    val toLabel: String,
    val description: String,
    val inputMimes: Array<String>,
    val multipleInput: Boolean = false,
    val category: ConversionCategory
) {
    JPEG_TO_PNG(
        label = "JPEG → PNG",
        fromLabel = "JPEG",
        toLabel = "PNG",
        description = "Lossless, supports transparency",
        inputMimes = arrayOf("image/jpeg", "image/jpg"),
        category = ConversionCategory.IMAGE
    ),
    PNG_TO_JPEG(
        label = "PNG → JPEG",
        fromLabel = "PNG",
        toLabel = "JPEG",
        description = "Smaller size, adjustable quality",
        inputMimes = arrayOf("image/png"),
        category = ConversionCategory.IMAGE
    ),
    IMAGE_TO_WEBP(
        label = "Image → WebP",
        fromLabel = "JPEG/PNG",
        toLabel = "WebP",
        description = "Modern format, excellent compression",
        inputMimes = arrayOf("image/jpeg", "image/png"),
        category = ConversionCategory.IMAGE
    ),
    WEBP_TO_PNG(
        label = "WebP → PNG",
        fromLabel = "WebP",
        toLabel = "PNG",
        description = "Convert WebP to universal PNG",
        inputMimes = arrayOf("image/webp"),
        category = ConversionCategory.IMAGE
    ),
    SVG_TO_PNG(
        label = "SVG → PNG",
        fromLabel = "SVG",
        toLabel = "PNG",
        description = "Render vector graphics to raster",
        inputMimes = arrayOf("image/svg+xml", "*/*"),
        category = ConversionCategory.VECTOR
    ),
    IMAGE_TO_SVG(
        label = "Image → SVG",
        fromLabel = "JPEG/PNG",
        toLabel = "SVG",
        description = "Embed image in SVG container",
        inputMimes = arrayOf("image/jpeg", "image/png"),
        category = ConversionCategory.VECTOR
    ),
    PDF_TO_IMAGES(
        label = "PDF → Images",
        fromLabel = "PDF",
        toLabel = "PNG",
        description = "Extract each page as a PNG image",
        inputMimes = arrayOf("application/pdf"),
        category = ConversionCategory.DOCUMENT
    ),
    IMAGES_TO_PDF(
        label = "Images → PDF",
        fromLabel = "JPEG/PNG",
        toLabel = "PDF",
        description = "Combine images into one PDF",
        inputMimes = arrayOf("image/jpeg", "image/png"),
        multipleInput = true,
        category = ConversionCategory.DOCUMENT
    ),
    DOCX_TO_PDF(
        label = "Word → PDF",
        fromLabel = "DOCX",
        toLabel = "PDF",
        description = "Convert Word document to PDF",
        inputMimes = arrayOf(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
        ),
        category = ConversionCategory.DOCUMENT
    );
}

enum class ConversionCategory(val label: String) {
    IMAGE("Image Conversion"),
    VECTOR("Vector Conversion"),
    DOCUMENT("Document Conversion")
}

package com.hire.smartcompress.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileUtils @Inject constructor() {

    fun getFileName(uri: Uri): String? = uri.path?.substringAfterLast('/')

    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return cursor.getString(idx)
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.path?.substringAfterLast('/')
    }

    fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (idx >= 0) return cursor.getLong(idx)
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.path?.let { File(it).length() } ?: 0L
    }

    fun getMimeType(context: Context, uri: Uri): String =
        try { context.contentResolver.getType(uri) ?: "application/octet-stream" }
        catch (_: Exception) { "application/octet-stream" }

    /**
     * Copies a content URI to the app's cache directory using the original filename.
     * Must be called while the temporary URI permission is still active (i.e. on the
     * same call-stack or coroutine as the picker result callback).
     */
    fun copyUriToCacheFile(context: Context, uri: Uri, name: String): File? {
        return try {
            val dest = File(context.cacheDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest
        } catch (_: Exception) { null }
    }

    fun copyUriToTempFile(context: Context, uri: Uri, suffix: String): File? {
        return try {
            val tempFile = File.createTempFile("sc_", suffix, context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            tempFile
        } catch (e: Exception) { null }
    }

    fun saveToMediaStore(
        context: Context,
        sourceFile: File,
        mimeType: String,
        displayName: String,
        subFolder: String = "SmartCompress"
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = when {
                    mimeType.startsWith("image/") -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    mimeType.startsWith("video/") -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    else -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }
                val relPath = when {
                    mimeType.startsWith("image/") -> "${Environment.DIRECTORY_PICTURES}/$subFolder"
                    mimeType.startsWith("video/") -> "${Environment.DIRECTORY_MOVIES}/$subFolder"
                    else -> "${Environment.DIRECTORY_DOWNLOADS}/$subFolder"
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(collection, values) ?: return null
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                uri
            } else {
                val dir = when {
                    mimeType.startsWith("image/") -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    mimeType.startsWith("video/") -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                }
                val outDir = File(dir, subFolder).also { it.mkdirs() }
                val outFile = File(outDir, displayName)
                sourceFile.copyTo(outFile, overwrite = true)
                Uri.fromFile(outFile)
            }
        } catch (e: Exception) { null }
    }

    /**
     * Explicitly saves a file to the device Downloads folder.
     * Returns the saved Uri on success, null on failure.
     */
    fun saveToDownloads(context: Context, sourceUri: Uri, displayName: String, mimeType: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val destUri = context.contentResolver.insert(collection, values) ?: return null
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    context.contentResolver.openOutputStream(destUri)?.use { output -> input.copyTo(output) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(destUri, values, null, null)
                destUri
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .also { it.mkdirs() }
                val dest = File(dir, displayName)
                context.contentResolver.openInputStream(sourceUri)?.use { it.copyTo(dest.outputStream()) }
                Uri.fromFile(dest)
            }
        } catch (e: Exception) { null }
    }

    fun deleteTempFiles(context: Context) {
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("sc_")) file.delete()
        }
    }
}

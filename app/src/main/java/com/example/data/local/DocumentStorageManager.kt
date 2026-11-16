package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.DocumentSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DocumentStorageManager(private val context: Context) {

    private val docsDir: File
        get() = File(context.filesDir, "student_docs").apply { if (!exists()) mkdirs() }

    private val cameraCacheDir: File
        get() = File(context.cacheDir, "camera_captures").apply { if (!exists()) mkdirs() }

    fun createTempCameraUri(): Pair<Uri, File> {
        val tempFile = File(cameraCacheDir, "capture_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return uri to tempFile
    }

    suspend fun saveImageFromUri(sourceUri: Uri, slot: DocumentSlot): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeScaledBitmapFromUri(sourceUri, maxDimension = 1600) ?: return@withContext null
            val destFile = File(docsDir, "${slot.filePrefix}_${UUID.randomUUID()}.jpg")
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            bitmap.recycle()
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveImageFromFile(sourceFile: File, slot: DocumentSlot): String? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null
        try {
            val uri = Uri.fromFile(sourceFile)
            val savedPath = saveImageFromUri(uri, slot)
            sourceFile.delete()
            savedPath
        } catch (_: Exception) {
            null
        }
    }

    fun deleteFileIfExists(path: String) {
        if (path.isBlank()) return
        try {
            val file = File(path)
            if (file.exists() && file.canonicalPath.startsWith(docsDir.canonicalPath)) {
                file.delete()
            }
        } catch (_: Exception) {
            // Ignore deletion failure
        }
    }

    fun loadBitmap(path: String, maxDimension: Int = 1200): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeScaledBitmapFromUri(uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }

        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        } ?: return null

        // Scale down precisely if still larger than maxDimension
        val maxSide = maxOf(decoded.width, decoded.height)
        return if (maxSide > maxDimension) {
            val scale = maxDimension.toFloat() / maxSide.toFloat()
            val matrix = Matrix().apply { postScale(scale, scale) }
            val scaled = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (scaled !== decoded) decoded.recycle()
            scaled
        } else {
            decoded
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        var currentMax = maxOf(width, height)
        while (currentMax / 2 >= maxDim) {
            currentMax /= 2
            inSampleSize *= 2
        }
        return inSampleSize
    }
}

package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object CustomArtworkManager {
    private const val FILE_NAME = "sonora_custom_cover.png"
    private const val COVER_WIDGET_FILE_NAME = "sonora_cover_widget_cover.png"
    private const val MAX_SIDE = 1080

    private data class Cached(val modified: Long, val bitmap: Bitmap)

    private val cache = HashMap<String, Cached>()

    // ---------------- main widget / app cover ----------------

    fun getCustomCoverFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    fun hasCustomCover(context: Context): Boolean {
        val file = getCustomCoverFile(context)
        return file.exists() && file.length() > 0
    }

    fun saveCustomArtworkFromUri(context: Context, uri: Uri): Boolean =
        copyUri(context, uri, getCustomCoverFile(context))

    fun loadCustomCoverBitmap(context: Context): Bitmap? =
        loadScaled(getCustomCoverFile(context))

    fun removeCustomArtwork(context: Context): Boolean = deleteFile(getCustomCoverFile(context))

    // ---------------- Cover widget has its OWN image ----------------

    fun getCoverWidgetFile(context: Context): File {
        return File(context.filesDir, COVER_WIDGET_FILE_NAME)
    }

    fun hasCoverWidgetCover(context: Context): Boolean {
        val file = getCoverWidgetFile(context)
        return file.exists() && file.length() > 0
    }

    fun saveCoverWidgetArtworkFromUri(context: Context, uri: Uri): Boolean =
        copyUri(context, uri, getCoverWidgetFile(context))

    fun loadCoverWidgetBitmap(context: Context): Bitmap? =
        loadScaled(getCoverWidgetFile(context))

    fun removeCoverWidgetArtwork(context: Context): Boolean = deleteFile(getCoverWidgetFile(context))

    // ---------------- helpers ----------------

    private fun copyUri(context: Context, uri: Uri, destination: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            synchronized(cache) { cache.remove(destination.absolutePath) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun deleteFile(file: File): Boolean {
        synchronized(cache) { cache.remove(file.absolutePath) }
        return if (file.exists()) file.delete() else true
    }

    /** Decodes (and caches) the image, scaled down so widgets stay light. */
    private fun loadScaled(file: File): Bitmap? {
        if (!file.exists() || file.length() == 0L) return null
        val key = file.absolutePath
        val modified = file.lastModified()
        synchronized(cache) {
            val hit = cache[key]
            if (hit != null && hit.modified == modified) return hit.bitmap
        }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / sample > MAX_SIDE * 2 || bounds.outHeight / sample > MAX_SIDE * 2) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = BitmapFactory.decodeFile(file.absolutePath, opts)
            if (bmp != null) {
                synchronized(cache) { cache[key] = Cached(modified, bmp) }
            }
            bmp
        } catch (e: Throwable) {
            null
        }
    }
}

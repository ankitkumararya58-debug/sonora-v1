package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object CustomArtworkManager {
    private const val FILE_NAME = "sonora_custom_cover.png"

    fun getCustomCoverFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    fun hasCustomCover(context: Context): Boolean {
        val file = getCustomCoverFile(context)
        return file.exists() && file.length() > 0
    }

    fun saveCustomArtworkFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val destinationFile = getCustomCoverFile(context)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadCustomCoverBitmap(context: Context): Bitmap? {
        val file = getCustomCoverFile(context)
        if (!file.exists() || file.length() == 0L) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    fun removeCustomArtwork(context: Context): Boolean {
        val file = getCustomCoverFile(context)
        return if (file.exists()) {
            file.delete()
        } else {
            true
        }
    }
}

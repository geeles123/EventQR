package com.thedavelopers.eventqr.core.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object BitmapSaver {

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val imageOutStream: OutputStream?
        var uri: Uri? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "EventQR")
            }
            context.contentResolver.run {
                uri = insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                imageOutStream = uri?.let { openOutputStream(it) }
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + "EventQR"
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdir()
            }
            val image = File(imagesDir, "$fileName.png")
            imageOutStream = FileOutputStream(image)
            uri = Uri.fromFile(image)
        }

        imageOutStream?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        return uri
    }
}

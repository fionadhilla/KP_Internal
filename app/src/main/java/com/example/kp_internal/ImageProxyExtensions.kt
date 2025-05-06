// ImageProxyExtensions.kt
package com.example.kp_internal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize).also {
        yBuffer.get(it, 0, ySize)
        vBuffer.get(it, ySize, vSize)
        uBuffer.get(it, ySize + vSize, uSize)
    }

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream().apply {
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, this)
    }
    val bitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())

    val rotation = imageInfo.rotationDegrees
    return if (rotation == 0) {
        bitmap
    } else {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

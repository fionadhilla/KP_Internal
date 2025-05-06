package com.example.kp_internal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    private var results: FaceDetectorResult? = null
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f
    private var isFrontCamera: Boolean = false
    private var bounds = Rect()

    init {
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            textSize = 50f
        }
        textPaint.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = 50f
        }
        boxPaint.apply {
            color = ContextCompat.getColor(context!!, android.R.color.holo_green_light)
            strokeWidth = 8f
            style = Paint.Style.STROKE
        }
    }

    @SuppressLint("DefaultLocale")
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.detections()?.forEach { detection ->
            val box = detection.boundingBox()

            val left = if (isFrontCamera) (imageWidth - box.right) else box.left
            val right = if (isFrontCamera) (imageWidth - box.left) else box.right
            val top = box.top
            val bottom = box.bottom

            var l = left * scaleFactor + offsetX
            var t = box.top * scaleFactor + offsetY
            var r = right * scaleFactor + offsetX
            var b = box.bottom * scaleFactor + offsetY

            val expandRatio = 0.5f
            val boxW = r - 2f
            val boxH = b - 2f
            val padW = boxW * expandRatio / 2f
            val padH = boxH * expandRatio / 2f

            l = (l - padW).coerceAtLeast(0f)
            t = (t - padH).coerceAtLeast(0f)
            r = (r + padW).coerceAtMost(width.toFloat())
            b = (b + padH).coerceAtMost(height.toFloat())


            val rect = RectF(l, t, r, b)
            canvas.drawRect(rect, boxPaint)

            val text = "${detection.categories()[0].categoryName()} " +
                    String.format("%.2f", detection.categories()[0].score())
            textBackgroundPaint.getTextBounds(text, 0, text.length, bounds)
            canvas.drawRect(
                l,
                t,
                l + bounds.width() + BOUNDING_RECT_TEXT_PADDING,
                t + bounds.height() + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            canvas.drawText(text, l, t + bounds.height(), textPaint)
        }
    }

    fun setResults(
        detectionResults: FaceDetectorResult,
        imageW: Int,
        imageH: Int,
        isFront: Boolean
    ) {
        results = detectionResults
        isFrontCamera = isFront

        imageWidth = imageW
        imageHeight = imageH

        scaleFactor = min(width.toFloat() / imageWidth, height.toFloat() / imageHeight)

        val scaledW = imageWidth * scaleFactor
        val scaledH = imageHeight * scaleFactor
        offsetX = (width - scaledW) / 2f
        offsetY = (height - scaledH) / 2f

        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
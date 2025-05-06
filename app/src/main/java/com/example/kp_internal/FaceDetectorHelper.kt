package com.example.kp_internal

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

class FaceDetectorHelper(
    context: Context,
    private val resultListener: (FaceDetectorResult) -> Unit
) {
    private val detector: FaceDetector

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("blaze_face_short_range.tflite")
            .build()

        val options = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setMinDetectionConfidence(0.5f)
            .setResultListener { result: FaceDetectorResult, _ ->
                resultListener(result)
            }
            .build()

        detector = FaceDetector.createFromOptions(context, options)
    }

    fun detect(bitmap: Bitmap) {
        val mpImage = BitmapImageBuilder(bitmap).build()
        detector.detectAsync(mpImage, System.currentTimeMillis())
    }

    fun close() {
        detector.close()
    }
}

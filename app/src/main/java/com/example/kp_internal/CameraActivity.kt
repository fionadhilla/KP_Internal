package com.example.kp_internal

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.widget.Toast
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.kp_internal.databinding.ActivityCameraBinding
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var faceRecognitionHelper: FaceRecognitionHelper
    private lateinit var binding: ActivityCameraBinding
    private lateinit var faceDetectorHelper: FaceDetectorHelper

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var lastBitmapWidth = 0
    private var lastBitmapHeight = 0

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        faceRecognitionHelper = FaceRecognitionHelper(this)

        requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)

        setupFaceDetector()
        startCamera()

        binding.btnSwitchCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT
            else
                CameraSelector.LENS_FACING_BACK
            startCamera()
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }


    private fun setupFaceDetector() {
        faceDetectorHelper = FaceDetectorHelper(this) { result ->
            runOnUiThread {
                val faceBoundingBox = result.detectedFaces[0].boundingBox
                val croppedBitmap = cropFaceBitmap(faceBoundingBox)

                // Kirim ke model Face Recognition
                val faceRecognitionInput = preprocessFaceForRecognition(croppedBitmap)
                val recognizedFace = faceRecognitionHelper.recognizeFace(faceRecognitionInput)

                // Lakukan pencocokan atau verifikasi
                matchFace(recognizedFace)


                binding.overlayView.setResults(
                    result,
                    lastBitmapWidth,
                    lastBitmapHeight,
                    lensFacing == CameraSelector.LENS_FACING_FRONT
                )
            }
        }
    }

    fun preprocessFaceForRecognition(bitmap: Bitmap): Array<Array<FloatArray>> {
        val inputSize = 112
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val input = Array(1) { Array(inputSize) { FloatArray(inputSize * 3) } }

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = scaledBitmap.getPixel(x, y)

                input[0][y][x * 3]     = ((pixel shr 16 and 0xFF) - 127.5f) / 128.0f // R
                input[0][y][x * 3 + 1] = ((pixel shr 8 and 0xFF) - 127.5f) / 128.0f  // G
                input[0][y][x * 3 + 2] = ((pixel and 0xFF) - 127.5f) / 128.0f        // B
            }
        }

        return input
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(executor) { imageProxy ->
                val bitmap = imageProxy.toBitmap()
                lastBitmapWidth = bitmap.width
                lastBitmapHeight = bitmap.height

                faceDetectorHelper.detect(bitmap)
                imageProxy.close()
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, analysis
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Gagal memunculkan kamera.", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }


}

fun cropFaceBitmap(bitmap: Bitmap, boundingBox: Rect): Bitmap {
    val safeRect = Rect(
        boundingBox.left.coerceAtLeast(0),
        boundingBox.top.coerceAtLeast(0),
        boundingBox.right.coerceAtMost(bitmap.width),
        boundingBox.bottom.coerceAtMost(bitmap.height)
    )

    return Bitmap.createBitmap(
        bitmap,
        safeRect.left,
        safeRect.top,
        safeRect.width(),
        safeRect.height()
    )
}
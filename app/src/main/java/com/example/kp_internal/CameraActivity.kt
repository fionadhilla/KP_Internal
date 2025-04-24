package com.example.kp_internal

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CameraActivity : AppCompatActivity() {
    lateinit var capReq: CaptureRequest.Builder
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    lateinit var captureRequest: CaptureRequest
    private var isBackCamera = true
    private lateinit var cameraId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera)
        getPermission()
        textureView = findViewById(R.id.TextureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler((handlerThread).looper)

        findViewById<ImageButton>(R.id.SwitchCamera).setOnClickListener {
            cameraDevice.close()
            isBackCamera = !isBackCamera
            openCamera()
        }


        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }
        }

        val btnBack: ImageButton = findViewById(R.id.Back)

        btnBack.setOnClickListener{
            val bukaDashboard = Intent(this, MainActivity::class.java)
            startActivity(bukaDashboard)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }



    fun getCameraId(isBack: Boolean): String {
        cameraManager.cameraIdList.forEach { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
            if (isBack && lensFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK)
                return id
            else if (!isBack && lensFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT)
                return id
        }
        throw RuntimeException("Camera not found!")
    }


    @SuppressLint("MissingPermission")
    fun openCamera() {
        cameraId = getCameraId(isBackCamera)
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val surface = Surface(textureView.surfaceTexture)
                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                capReq.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        captureRequest = capReq.build()
                        cameraCaptureSession.setRepeatingRequest(captureRequest, null, handler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, handler)
            }

            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {}
        }, handler)
    }


    private fun getPermission(){
        val permissionList = mutableListOf<String>()

        if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(android.Manifest.permission.CAMERA)
        if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(android.Manifest.permission.CAMERA)
        if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(android.Manifest.permission.CAMERA)

        if(permissionList.size > 0) {
            requestPermissions(permissionList.toTypedArray(), 101)

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if(it != PackageManager.PERMISSION_GRANTED){
                getPermission()
            }
        }
    }

}
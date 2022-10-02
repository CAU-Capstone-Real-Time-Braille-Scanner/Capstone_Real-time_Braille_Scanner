package com.example.realtimebraillescanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import java.util.*
import java.util.concurrent.ExecutorService
import android.view.ScaleGestureDetector
import com.example.realtimebraillescanner.databinding.ActivityCameraBinding

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding

    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initClickListener()

        startCamera()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initClickListener(){
        // Set up the listeners for take photo and video capture buttons
//        binding.imageCaptureButton.setOnClickListener { takePhoto() }
//        binding.videoCaptureButton.setOnClickListener { captureVideo() }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val cameraControl = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

                // Zoom settings
                val scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener(){
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val scale = cameraControl.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                        cameraControl.cameraControl.setZoomRatio(scale)
                        return true
                    }
                })

                binding.viewFinder.setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    return@setOnTouchListener true
                }

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }




    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "RealTimeBrailleScanner"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
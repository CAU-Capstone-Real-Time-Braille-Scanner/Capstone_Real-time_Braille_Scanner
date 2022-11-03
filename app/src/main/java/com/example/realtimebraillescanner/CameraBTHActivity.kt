package com.example.realtimebraillescanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.Camera
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.View
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.res.ResourcesCompat
import com.example.realtimebraillescanner.databinding.CameraBthActivityBinding
import com.example.realtimebraillescanner.util.ScopedExecutor
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

typealias BrailleListener = (temp: String) -> Unit

class CameraBTHActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CameraBTHActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        /* ######################################################### */
        var recognizedText : String = ""

        // We only need to analyze the part of the image that has text, so we set crop percentages
        // to avoid analyze the entire image from the live camera feed.
        const val DESIRED_WIDTH_CROP_PERCENT = 8
        const val DESIRED_HEIGHT_CROP_PERCENT = 84

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        /* ######################################################### */
    }

    private lateinit var binding: CameraBthActivityBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    /* ######################################################### */
    private var displayId: Int = -1
    // private val viewModel: MainViewModel by viewModels()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageAnalyzer: ImageAnalysis
    private lateinit var viewFinder: PreviewView
    // private lateinit var textAnalyzer: TextAnalyzer
    private lateinit var scopedExecutor: ScopedExecutor
    /* ######################################################### */

    // 모든 작업을 수행할 인터프리터 객체
    private lateinit var tflite: Interpreter

    // 인터프리터에 전달할 모델
    private lateinit var tflitemodel: ByteBuffer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CameraBthActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 인터프리터를 초기화하고 model.tflite 을 로드하는 코드
        try {
            tflitemodel = loadModelFile(this.assets, "model.tflite")
            tflite = Interpreter(tflitemodel)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        initClickListener()
        setIconBackground(0, 1, 0)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Shut down our background executor
        cameraExecutor.shutdown()
        scopedExecutor.shutdown()
    }

    override fun onStart() {
        super.onStart()
        viewFinder = binding.viewfinder
        cameraExecutor = Executors.newSingleThreadExecutor()
        scopedExecutor = ScopedExecutor(cameraExecutor)

        // Request camera permissions
        if (allPermissionsGranted()) {
            // Wait for the views to be properly laid out
            viewFinder.post {
                // Keep track of the display in which this view is attached
                displayId = viewFinder.display.displayId
                startCamera()
            }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.overlay.apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSPARENT)
            holder.addCallback(object: SurfaceHolder.Callback {
                override fun surfaceChanged(
                    p0: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) = Unit

                override fun surfaceDestroyed(p0: SurfaceHolder) {}
                override fun surfaceCreated(p0: SurfaceHolder) {
                    holder?.let { drawOverlay(it) }
                }
            })
        }
    }

    private fun initClickListener() {
        // Set up the listeners for take photo and video capture buttons
        binding.imageCaptureButton.setOnClickListener { takePhoto() }

        binding.play.setOnClickListener {
            binding.srcText.visibility = View.VISIBLE
            binding.recognizedText.visibility = View.GONE
            binding.mode.text = "1"

            setIconBackground(1, 2, 0)
        }
        binding.pause.setOnClickListener {
            binding.srcText.visibility = View.VISIBLE
            binding.recognizedText.visibility = View.GONE
            binding.mode.text = "2"

            binding.srcText.text = binding.srcText.text.toString()
            binding.translatedText.text = binding.translatedText.text.toString()
            binding.srcText.text.trim()
            binding.translatedText.text.trim()

            setIconBackground(2, 1, 1)
        }
        binding.voice.setOnClickListener {
            binding.mode.text = "2"

            setIconBackground(1, 0, 2)
        }
    }

    private fun setIconBackground(pause : Int, play : Int, voice : Int){
        when (pause) {
            1 -> {
                binding.pause.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.pause_click, null))
                binding.pause.isClickable = true
            }
            0 -> {
                binding.pause.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.pause_d, null))
                binding.pause.isClickable = false
            }
            else -> {
                binding.pause.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.pause_g, null))
                binding.pause.isClickable = false
            }
        }

        when (play) {
            1 -> {
                binding.play.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.play_click, null))
                binding.play.isClickable = true
            }
            0 -> {
                binding.play.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.play_d, null))
                binding.play.isClickable = false
            }
            else -> {
                binding.play.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.play_g, null))
                binding.play.isClickable = false
            }
        }

        when (voice) {
            1 -> {
                binding.voice.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.voice_click, null))
                binding.voice.isClickable = true
            }
            0 -> {
                binding.voice.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.voice_d, null))
                binding.voice.isClickable = false
            }
            else -> {
                binding.voice.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.voice_g, null))
                binding.voice.isClickable = false
            }
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get() ?: throw IllegalStateException("Camera initialization failed")

            val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
            Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

            val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

            val rotation = viewFinder.display.rotation

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()

            /*
            TODO: 점자 분석기가 필요할 것.
            textAnalyzer = TextAnalyzer(
                requireContext(),
                lifecycle,
                viewModel.sourceText,
                viewModel.translatedText,
                viewModel.braille,
                viewModel.imageCropPercentages,
                binding
            )
            */

            imageCapture = ImageCapture.Builder().build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        // TODO: textAnalyzer
                        BrailleAnalyzer { temp ->
                        runOnUiThread {
                            binding.recognizedText.text = "인식된 글자: $temp"
                        }
                    })
                }

            /*
            TODO: make own viewModel
            viewModel.sourceText.observe(viewLifecycleOwner, Observer {
                binding.srcText.text = it
            })
            viewModel.translatedText.observe(viewLifecycleOwner, Observer {
                binding.translatedText.text = it
            })
            viewModel.braille.observe(viewLifecycleOwner, Observer {
                CameraHTBFragment.braille = it
            })
            viewModel.imageCropPercentages.observe(viewLifecycleOwner,
                Observer { drawOverlay(binding.overlay.holder, it.first, it.second) })
            */

            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                // Zoom settings
                val scaledGestureDetector = ScaleGestureDetector(this,
                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            val scale = camera.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                            camera.cameraControl.setZoomRatio(scale)
                            return true
                        }
                    }
                )

                viewFinder.setOnTouchListener { _, event ->
                    scaledGestureDetector.onTouchEvent(event)
                    return@setOnTouchListener true
                }

                preview.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun drawOverlay(holder: SurfaceHolder) {
        val canvas = holder.lockCanvas()
        val bgPaint = Paint().apply {
            alpha = 140
        }
        canvas.drawPaint(bgPaint)
        val rectPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        val outLinePaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = 4f
        }
        val surfaceWidth = holder.surfaceFrame.width()
        val surfaceHeight = holder.surfaceFrame.height()
        val cornerRadius = 25f

        // Set rect centered in frame
        val rectTop = surfaceHeight * 70 / 2 / 100f
        val rectLeft = surfaceWidth * DESIRED_WIDTH_CROP_PERCENT / 2 / 100f
        val rectRight = surfaceWidth * (1 - DESIRED_WIDTH_CROP_PERCENT / 2 / 100f)
        val rectBottom = surfaceHeight * (1 - 70 / 2 / 100f)
        val rect = RectF(rectLeft, rectTop, rectRight, rectBottom)

        canvas.drawRoundRect(
            rect, cornerRadius, cornerRadius, rectPaint
        )
        canvas.drawRoundRect(
            rect, cornerRadius, cornerRadius, outLinePaint
        )

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 50F
        }

        // set text rect centered in frame
        val overlayText = "점자를 박스에 비춰주세요"
        val textBounds = Rect()
        textPaint.getTextBounds(overlayText, 0, overlayText.length, textBounds)
        val textX = (surfaceWidth - textBounds.width()) / 2f
        val textY = rectBottom + textBounds.height() + 15f // put text below rect and 15f padding
        canvas.drawText(overlayText, textX, textY, textPaint)
        holder.unlockCanvasAndPost(canvas)
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = ln(max(width, height).toDouble() / min(width, height))
        if (abs(previewRatio - ln(RATIO_4_3_VALUE)) <= abs(previewRatio - ln(RATIO_16_9_VALUE))) {
            return AspectRatio.RATIO_4_3
        } else {
            return AspectRatio.RATIO_16_9
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post {
                    // Keep track of the display in which this view is attached
                    displayId = viewFinder.display.displayId
                    startCamera()
                }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // This helper function loads tensorflow lite model from "assets" directory.
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun doInference(bitmap: Bitmap): String {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 28 * 28 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(28 * 28)  // 28 * 28 크기의 점자 이미지의 픽셀 값을 저장하는 배열
        val result = Array(1) { FloatArray(64) }  // 해당 점자 이미지가 어떤 글자에 해당하는지를 나타내는 확률 배열. 이 중 값이 가장 높은 인덱스가 결과값이 된다.

        val scaledBitmap: Bitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, false)
        // binding.imageView.setImageBitmap(scaledBitmap)
        scaledBitmap.getPixels(intValues, 0, 28, 0, 0, 28, 28)

        var pixel = 0
        for (i in 0 until 28) {
            for (j in 0 until 28) {
                val input = intValues[pixel++]
                byteBuffer.putFloat(((input.shr(16) and 0xFF) / 1.0f))
                byteBuffer.putFloat(((input.shr(8) and 0xFF) / 1.0f))
                byteBuffer.putFloat(((input and 0xFF) / 1.0f))
            }
        }

        tflite.run(byteBuffer, result)
        var maxValue = 0.0f
        var maxIndex = 0
        for (i in result[0].indices) {
            if (maxValue < result[0][i]) {
                maxValue = result[0][i]
                maxIndex = i
            }
        }

        val answer = if (maxValue >= 0.5f) {
            StringBuilder().apply {
                for (i in 0 until 6) {
                    if (maxIndex % 2 == 1) {
                        maxIndex = (maxIndex - 1) / 2
                        append("1")
                    } else {
                        maxIndex /= 2
                        append("0")
                    }
                }
            }.toString().reversed()
        } else {
            "인식 불가"
        }

        return answer
    }

    private inner class BrailleAnalyzer(private val listener: BrailleListener) : ImageAnalysis.Analyzer {
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: return
            val bitmap = toBitmap(mediaImage)
            listener(doInference(bitmap))

            imageProxy.close()
        }

        private fun toBitmap(image: Image): Bitmap {
            val planes: Array<Image.Plane> = image.planes
            val yBuffer: ByteBuffer = planes[0].buffer
            val uBuffer: ByteBuffer = planes[1].buffer
            val vBuffer: ByteBuffer = planes[2].buffer

            val ySize: Int = yBuffer.remaining()
            val uSize: Int = uBuffer.remaining()
            val vSize: Int = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage  = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)

            val imageBytes: ByteArray = out.toByteArray()
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
    }
}
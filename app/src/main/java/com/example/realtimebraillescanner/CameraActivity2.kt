package com.example.realtimebraillescanner

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.*
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import com.example.realtimebraillescanner.databinding.ActivityCamera2Binding
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Locale

typealias BrailleListener = (temp: String) -> Unit

class CameraActivity2 : AppCompatActivity() {
    private lateinit var binding: ActivityCamera2Binding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // 모든 작업을 수행할 인터프리터 객체
    private lateinit var tflite: Interpreter

    // 인터프리터에 전달할 모델
    private lateinit var tflitemodel: ByteBuffer

    private val localModel = LocalModel.Builder()
        .setAssetFilePath("model.tflite")
        .build()

    // Live multiple object detection and tracking
    private val customObjectDetectorOptions =
        CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f)
            .setMaxPerObjectLabelCount(3)
            .build()

    private val objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamera2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // 인터프리터를 초기화하고 model.tflite 을 로드하는 코드
        try {
            tflitemodel = loadModelFile(this.assets, "model.tflite")
            tflite = Interpreter(tflitemodel)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        // 추론 버튼의 리스너 추가
        binding.inferButton.setOnClickListener {
            doInferenceFromSavedImage()
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listeners for take photo and video capture buttons
        binding.imageCaptureButton.setOnClickListener { takePhoto() }
        binding.inferButton.setOnClickListener { doInferenceFromSavedImage() }

        cameraExecutor = Executors.newSingleThreadExecutor()
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

            imageCapture = ImageCapture.Builder().build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BrailleAnalyzer { temp ->
                        runOnUiThread {
                            binding.textView.text = "인식된 글자: $temp"
                        }
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
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

    // assets 폴더에 저장된 이미지에서 추론을 수행하는 함수
    private fun doInferenceFromSavedImage() {
        // assets 폴더에 저장된 점자 이미지 로드하기
        val bitmap: Bitmap
        var inputStream: InputStream? = null
        val scaledBitmap: Bitmap
        val assetManager: AssetManager = this.assets
        val byteBuffer = ByteBuffer.allocateDirect(4 * 28 * 28 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(28 * 28)  // 28 * 28 크기의 점자 이미지의 픽셀 값을 저장하는 배열
        val result = Array(1) { FloatArray(64) }  // 해당 점자 이미지가 어떤 글자에 해당하는지를 나타내는 확률 배열. 이 중 값이 가장 높은 인덱스가 결과값이 된다.

        try {
            inputStream = assetManager.open("Braille_dataset/111101/111101_0_3.jpg", AssetManager.ACCESS_UNKNOWN)
            bitmap = BitmapFactory.decodeStream(inputStream, null, null) ?: throw Exception("fail to load bitmap from InputStream")
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, false)
            binding.imageView.setImageBitmap(scaledBitmap)
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

            inputStream.close()
            inputStream = null
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {}
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

        // 추론 결과를 표시하는 AlertDialog
        val builder = AlertDialog.Builder(this)
        with (builder) {
            setTitle("TFLite Interpreter")
            setMessage("추론 결과: $answer")
            setNeutralButton("OK") { dialog, _ ->
                dialog.cancel()
            }
            show()
        }
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

    companion object {
        private const val TAG = "CurrentApp"
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
    }

    private inner class BrailleAnalyzer(private val listener: BrailleListener) : ImageAnalysis.Analyzer {
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: return

            /*
            val str = StringBuilder()

            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                objectDetector.process(image)
                    .addOnSuccessListener { detectedObjects ->
                        // Task completed successfully
                        // process() 호출이 성공하면 DetectedObject 목록이 성공 리스너에 전달됨.
                        for (detectedObject in detectedObjects) {
                            val boundingBox = detectedObject.boundingBox
                            val trackingId = detectedObject.trackingId
                            for (label in detectedObject.labels) {
                                val text = label.text
                                val index = label.index
                                val confidence = label.confidence

                                str.append("Detected object: ${index}, ")
                                str.append("confidence: ${confidence}\n")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                    }
            }
            */

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

            val nv21: ByteArray = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage: YuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out: ByteArrayOutputStream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)

            val imageBytes: ByteArray = out.toByteArray()
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
    }
}
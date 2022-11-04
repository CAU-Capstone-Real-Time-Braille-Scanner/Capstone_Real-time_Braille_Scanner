package com.example.realtimebraillescanner

import android.content.Context
import android.content.res.AssetManager
import android.graphics.*
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.example.realtimebraillescanner.databinding.CameraBthFragmentBinding
import com.example.realtimebraillescanner.util.ImageUtils
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Analyze the frames passed in from the camera and
 * return any detected braille within the requested crop region
 */
class BrailleAnalyzer(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val srcText: MutableLiveData<String>,
    private val translatedText: MutableLiveData<String>,
    private val koreanText: MutableLiveData<String>,
    private val imageCropPercentages: MutableLiveData<Pair<Int, Int>>,
    private val binding: CameraBthFragmentBinding
) : ImageAnalysis.Analyzer {
    companion object {
        private const val TAG = "BrailleAnalyzer"
    }

    // 모든 작업을 수행할 인터프리터 객체
    private lateinit var tflite: Interpreter

    // 인터프리터에 전달할 모델
    private lateinit var tflitemodel: ByteBuffer

    private val localModel = LocalModel.Builder()
        .setAssetFilePath("model_with_metadata.tflite")
        .build()

    private val customObjectDetectorOptions =
        CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f)
            .setMaxPerObjectLabelCount(3)
            .build()

    private val objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)

    var currentTimestamp: Long = 0L

    init {
        // 인터프리터를 초기화하고 model.tflite 을 로드하는 코드
        try {
            tflitemodel = loadModelFile(context.assets, "model.tflite")
            tflite = Interpreter(tflitemodel)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        // TODO
        // lifecycle.addObserver(detector)
    }

    // camera frame rate 에 맞게 호출되어 이미지 분석
    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return

        currentTimestamp = System.currentTimeMillis()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        // We requested a setTargetAspectRatio, but it's not guaranteed that's what the camera
        // stack is able to support, so we calculate the actual ratio from the first frame to
        // know how to appropriately crop the image we want to analyze.
        val imageHeight = mediaImage.height
        val imageWidth = mediaImage.width

        val actualAspectRatio = imageWidth / imageHeight

        val convertImageToBitmap = ImageUtils.convertYuv420888ImageToBitmap(mediaImage)
        val cropRect = Rect(0, 0, imageWidth, imageHeight)

        // If the image has a way wider aspect ratio than expected, crop less of the height so we
        // don't end up cropping too much of the image. If the image has a way taller aspect ratio
        // than expected, we don't have to make any changes to our cropping so we don;t handle it here.
        val currentCropPercentages = imageCropPercentages.value ?: return
        if (actualAspectRatio > 3) {
            val originalHeightCropPercentage = currentCropPercentages.first
            val orignalWidthCropPercentage = currentCropPercentages.second
            imageCropPercentages.value = Pair(originalHeightCropPercentage / 2, orignalWidthCropPercentage)
        }

        // If the image is rotated by 90 (or 270) degrees, swap height and width when calculating
        // the crop.
        val cropPercentages = imageCropPercentages.value ?: return
        val heightCropPercent = cropPercentages.first
        val widthCropPercent = cropPercentages.second
        val (widthCrop, heightCrop) = when (rotationDegrees) {
            90, 270 -> Pair(heightCropPercent / 100f, widthCropPercent / 100f)
            else -> Pair(widthCropPercent / 100f, heightCropPercent / 100f)
        }

        cropRect.inset(
            (imageWidth * widthCrop / 2).toInt(),
            (imageHeight * heightCrop / 2).toInt()
        )
        val croppedBitmap =
            ImageUtils.rotateAndCrop(convertImageToBitmap, rotationDegrees, cropRect)

        // TODO: recognizeBraille
        val image = InputImage.fromBitmap(croppedBitmap, 0)
        objectDetector
            .process(image)
            .addOnFailureListener { exception ->
                Log.e(TAG, "Braille recognition error", exception)
                val message = getErrorMessage(exception)
                message?.let {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
            .addOnSuccessListener { results ->
                when (binding.mode.text) {
                    "1" -> { // 재생 버튼
                        val recognizedText = StringBuilder()

                        for (detectedObject in results) {
                            val boundingBox = detectedObject.boundingBox
                            val trackingId = detectedObject.trackingId
                            for (label in detectedObject.labels) {
                                val index = label.index // 0 ~ 63
                                val confidence = label.confidence

                                when (index) {
                                    0 -> recognizedText.append("#")
                                    1 -> recognizedText.append("⠠")
                                    2 -> recognizedText.append("⠐")
                                    3 -> recognizedText.append("⠰")
                                    4 -> recognizedText.append("⠈")
                                    5 -> recognizedText.append("⠨")
                                    6 -> recognizedText.append("⠘")
                                    7 -> recognizedText.append("⠸")
                                    8 -> recognizedText.append("⠄")
                                    9 -> recognizedText.append("⠤")
                                    10 -> recognizedText.append("⠔")
                                    11 -> recognizedText.append("⠴")
                                    12 -> recognizedText.append("⠌")
                                    13 -> recognizedText.append("⠬")
                                    14 -> recognizedText.append("⠜")
                                    15 -> recognizedText.append("⠼")
                                    16 -> recognizedText.append("⠂")
                                    17 -> recognizedText.append("⠢")
                                    18 -> recognizedText.append("⠒")
                                    19 -> recognizedText.append("⠲")
                                    20 -> recognizedText.append("⠊")
                                    21 -> recognizedText.append("⠪")
                                    22 -> recognizedText.append("⠚")
                                    23 -> recognizedText.append("⠺")
                                    24 -> recognizedText.append("⠆")
                                    25 -> recognizedText.append("⠦")
                                    26 -> recognizedText.append("⠖")
                                    27 -> recognizedText.append("⠶")
                                    28 -> recognizedText.append("⠎")
                                    29 -> recognizedText.append("⠮")
                                    30 -> recognizedText.append("⠞")
                                    31 -> recognizedText.append("⠾")
                                    32 -> recognizedText.append("⠁")
                                    33 -> recognizedText.append("⠡")
                                    34 -> recognizedText.append("⠑")
                                    35 -> recognizedText.append("⠱")
                                    36 -> recognizedText.append("⠉")
                                    37 -> recognizedText.append("⠩")
                                    38 -> recognizedText.append("⠙")
                                    39 -> recognizedText.append("⠹")
                                    40 -> recognizedText.append("⠅")
                                    41 -> recognizedText.append("⠥")
                                    42 -> recognizedText.append("⠕")
                                    43 -> recognizedText.append("⠵")
                                    44 -> recognizedText.append("⠍")
                                    45 -> recognizedText.append("⠭")
                                    46 -> recognizedText.append("⠝")
                                    47 -> recognizedText.append("⠽")
                                    48 -> recognizedText.append("⠃")
                                    49 -> recognizedText.append("⠣")
                                    50 -> recognizedText.append("⠓")
                                    51 -> recognizedText.append("⠳")
                                    52 -> recognizedText.append("⠋")
                                    53 -> recognizedText.append("⠫")
                                    54 -> recognizedText.append("⠛")
                                    55 -> recognizedText.append("⠻")
                                    56 -> recognizedText.append("⠇")
                                    57 -> recognizedText.append("⠧")
                                    58 -> recognizedText.append("⠗")
                                    59 -> recognizedText.append("⠷")
                                    60 -> recognizedText.append("⠏")
                                    61 -> recognizedText.append("⠯")
                                    62 -> recognizedText.append("⠟")
                                    63 -> recognizedText.append("⠿")
                                }
                            }
                        }
                        srcText.value = recognizedText.toString()
                    }
                    "2" -> { // 일시정지 버튼
                        // Do nothing.
                    }
                    else -> {
                        // TODO
                        binding.mode.text = "1"
                    }
                }
            }

        imageProxy.close()
    }

    /*
    private fun imageToBitmap(image: Image): Bitmap {
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
    */

    private fun getErrorMessage(exception: Exception): String? {
        val mlKitException = exception as? MlKitException ?: return exception.message
        return if (mlKitException.errorCode == MlKitException.UNAVAILABLE) {
            "종료중..."
        } else {
            exception.message
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
}
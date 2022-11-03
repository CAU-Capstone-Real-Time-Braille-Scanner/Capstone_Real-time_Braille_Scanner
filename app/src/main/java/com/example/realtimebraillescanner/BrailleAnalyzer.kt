package com.example.realtimebraillescanner

import android.content.Context
import android.content.res.AssetManager
import android.graphics.*
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.example.realtimebraillescanner.databinding.CameraBthFragmentBinding
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
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

    // TODO
    // private val detector = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

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

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val bitmap = imageToBitmap(mediaImage)
        //listener(doInference(bitmap))

        imageProxy.close()
    }

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
}
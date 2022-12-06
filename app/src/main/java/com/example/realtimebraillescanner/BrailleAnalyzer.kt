package com.example.realtimebraillescanner

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.example.realtimebraillescanner.databinding.CameraBthFragmentBinding
import com.example.realtimebraillescanner.retrofit_util.DataModel
import com.example.realtimebraillescanner.retrofit_util.RetrofitClient
import com.example.realtimebraillescanner.retrofit_util.RetrofitInterface
import com.example.realtimebraillescanner.util.ImageUtils
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*

/**
 * Analyze the frames passed in from the camera and
 * return any detected braille within the requested crop region
 */
class BrailleAnalyzer(
    private val context: Context,
    private val srcText: MutableLiveData<String>,
    private val translatedText: MutableLiveData<String>,
    private val koreanText: MutableLiveData<String>,
    private val imageCropPercentages: MutableLiveData<Pair<Int, Int>>,
    private val binding: CameraBthFragmentBinding,
) : ImageAnalysis.Analyzer, AppCompatActivity() {
    companion object {
        private const val TAG = "BrailleAnalyzer"
    }

    /*
    private val python: Python
    private val pythonFile: PyObject
    */

    private val service = RetrofitClient.getApiService()

    private var num = 1 // 프레임 속도 측정을 위한 임시 코드

    init {
        /*
        // "Context" must be an Activity, Service or Application object from your app.
        // 1. Start the Python instance if it isn't already running.
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        // 2. Obtain the python instance
        python = Python.getInstance()
        pythonFile = python.getModule("braille_ocr_from_image")
        pythonFile.callAttr("loadModel")
        */
        service.loadModel().enqueue(object : Callback<String>{
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "####################\n\n\nloadModel() 성공\n\n\n######################")
                } else {
                    // 통신이 실패한 경우
                    Log.d(TAG, "####################\n\n\nloadModel() 실패1\n\n\n#####################")
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                // 통신 실패 (인터넷 끊김, 예외 발생 등 시스템적인 이유)
                Log.d(TAG, "###########################\n\n\nloadModel() 실패2\n\n\n###################3")
            }
        })
    }

    // camera frame rate 에 맞게 호출되어 이미지 분석
    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return

        //currentTimestamp = System.currentTimeMillis()
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

        takePhoto(croppedBitmap)

        /*
        var result: DataModel? = null
        val file = File("/data/data/com.example.realtimebraillescanner/files/pic.png")
        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val body = MultipartBody.Part.createFormData("uploaded_file", file.name, requestFile)

        service.getResult(body).enqueue(object : Callback<DataModel> {
            override fun onResponse(call: Call<DataModel>, response: Response<DataModel>) {
                if (response.isSuccessful) {
                    result = response.body()
                } else {
                    // 통신이 실패한 경우
                    Log.d(TAG, "onResponse 실패")
                }
            }

            override fun onFailure(call: Call<DataModel>, t: Throwable) {
                // 통신 실패 (인터넷 끊김, 예외 발생 들 시스템적인 이유)
                Log.d(TAG, "onFailure 에러: " + t.message.toString())
            }
        })

        runOnUiThread {
            srcText.value = result?.srcText ?: ""
            translatedText.value = num.toString() // TODO: 수정 필요
            num++
        }
        */

        /*
        val obj: List<PyObject> = pythonFile.callAttr(
            "getBrailleText",
            "/data/data/com.example.realtimebraillescanner/files/pic.png")
            .asList()
        val result1 = obj[0].toJava(Array<String>::class.java)
        val result2 = obj[1].toJava(Array<String>::class.java)

        runOnUiThread {
            srcText.value = StringBuilder().apply {
                for (line in result1) {
                    append(line)
                }
            }.toString()

            translatedText.value = StringBuilder().apply {
                for (line in result2) {
                    append(line)
                }
                append("\n")
                append("프레임: $num")
            }.toString()
            num++
        }
        */

        imageProxy.close()
    }

    private fun takePhoto(photo: Bitmap) {
        val bytes = ByteArrayOutputStream()
        photo.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val photoFile = File(
            "/data/data/com.example.realtimebraillescanner/files",
            "pic.png"
        )
        photoFile.createNewFile()
        val fo = FileOutputStream(photoFile)
        fo.write(bytes.toByteArray())
        fo.close()
        Log.d(TAG, "Capture image successfully")
    }
}
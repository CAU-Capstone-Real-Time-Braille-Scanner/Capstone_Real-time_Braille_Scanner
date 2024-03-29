package com.example.realtimebraillescanner

import android.app.Activity
import android.graphics.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.example.realtimebraillescanner.databinding.CameraBthFragmentBinding
import com.example.realtimebraillescanner.retrofit_util.DataModel
import com.example.realtimebraillescanner.retrofit_util.RetrofitClient
import com.example.realtimebraillescanner.util.ImageUtils
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.*
import java.util.UUID

/**
 * Analyze the frames passed in from the camera and
 * return any detected braille within the requested crop region
 */
class BrailleAnalyzer(
    private val srcText: MutableLiveData<String>,
    private val translatedText: MutableLiveData<String>,
    private val imageCropPercentages: MutableLiveData<Pair<Int, Int>>,
    private val binding: CameraBthFragmentBinding,
    private val hashValue: String
) : ImageAnalysis.Analyzer, AppCompatActivity() {
    companion object {
        private const val TAG = "BrailleAnalyzer"
    }

    private val service1 = RetrofitClient.getApiService1()
    private val service2 = RetrofitClient.getApiService2()

    init {
        Thread {
            try {
                val response = service1.loadModel().execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "loadModel() 성공")
                } else {
                    // 통신이 실패한 경우
                    Log.d(TAG, "loadModel() 실패1: ${response.message()}")
                    print(response.message().toString())

                }
            } catch (e: Exception) {
                // 통신 실패 (인터넷 끊김, 예외 발생 등 시스템적인 이유)
                Log.d(TAG, "loadModel() 실패2: ${e.message.toString()}")
                e.printStackTrace()
            }
        }.start()
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

        // 재생 버튼
        if (binding.mode.text.equals("1")) {
            var result: DataModel? = null
            val file = File("/data/data/com.example.realtimebraillescanner/${hashValue}.png")
            val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            try {
                val response = service2.getResult(body).execute()
                if (response.isSuccessful) {
                    result = response.body()
                } else {
                    // 통신이 실패한 경우
                    Log.d(TAG, "onResponse 실패: ${response.message()}")
                }
            } catch (e: Exception) {
                // 통신 실패 (인터넷 끊김, 예외 발생 등 시스템적인 이유)
                Log.d(TAG, "onFailure 에러: " + e.message.toString())
                e.printStackTrace()
            }

            if (binding.mode.text.equals("1")) {
                runOnUiThread {
                    srcText.value = result?.srcText ?: ""
                    translatedText.value = result?.translatedText ?: ""
                }
            }
        }

        imageProxy.close()
    }

    private fun takePhoto(photo: Bitmap) {
        val bytes = ByteArrayOutputStream()
        photo.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val photoFile = File(
            "/data/data/com.example.realtimebraillescanner/",
            "${hashValue}.png"
        )
        photoFile.createNewFile()
        val fo = FileOutputStream(photoFile)
        fo.write(bytes.toByteArray())
        fo.close()
        Log.d(TAG, "Capture image successfully")
    }
}
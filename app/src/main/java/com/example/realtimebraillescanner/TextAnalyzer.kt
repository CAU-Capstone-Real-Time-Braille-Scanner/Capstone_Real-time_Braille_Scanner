/*
 * Copyright 2019 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.example.realtimebraillescanner

import android.content.Context
import android.graphics.Rect
import android.opengl.Visibility
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.example.realtimebraillescanner.databinding.CameraHtbFragmentBinding
import com.example.realtimebraillescanner.util.ImageUtils
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * Analyzes the frames passed in from the camera and returns any detected text within the requested
 * crop region.
 */
class TextAnalyzer(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val srcText: MutableLiveData<String>,
    private val translatedText: MutableLiveData<String>,
    private val brailleText: MutableLiveData<String>,
    private val imageCropPercentages: MutableLiveData<Pair<Int, Int>>,
    private val binding: CameraHtbFragmentBinding
) : ImageAnalysis.Analyzer {

    // TODO: Instantiate TextRecognition detector
    private val detector = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    var currentTimestamp: Long = 0

    init {
        lifecycle.addObserver(detector)
        setEditText()
    }

    // TODO: Add lifecycle observer to properly close ML Kit detectors

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {  //camera frame rate 에 맞게 호출되어 이미지 분석
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
        // than expected, we don't have to make any changes to our cropping so we don't handle it
        // here.
        val currentCropPercentages = imageCropPercentages.value ?: return
        if (actualAspectRatio > 3) {
            val originalHeightCropPercentage = currentCropPercentages.first
            val originalWidthCropPercentage = currentCropPercentages.second
            imageCropPercentages.value =
                Pair(originalHeightCropPercentage / 2, originalWidthCropPercentage)
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

        // TODO call recognizeText() once implemented
        recognizeText(InputImage.fromBitmap(croppedBitmap, 0)).addOnCompleteListener {
            CoroutineScope(Dispatchers.IO).launch {
                delay(500 - (System.currentTimeMillis() - currentTimestamp))
                imageProxy.close()
            }
        }
    }

    private fun recognizeText(image: InputImage): Task<Text> {
        // 매 frame 별 image 받아와 text 인식기 실행
        // TODO Use ML Kit's TextRecognition to analyze frames from the camera live feed.
        // Pass image to an ML Kit Vision API
        return detector.process(image)      //매 프레임마다 실행되는 과정 속에서 mode를 기준으로 특정 작업 수행 반복
            .addOnSuccessListener { text ->
                // Task completed successfully
                if(binding.mode.text.equals("1")){  //재생 버튼
                    val result : String = leaveOnlyAvailableText(text.text)    //실시간 번역 수행
                    translateKorToBraille(result)
                }
                else if(binding.mode.text.equals("2")){ //일시정지 버튼
                    //아무것도 안함
                }
                else if(binding.mode.text.equals("3")){   //수정 버튼
                    binding.mode.setText("0")   //editText 보이도록 하고 detector가 일시정지된 것처럼 하기 위함 (위 문구 중 어느 곳에도 안 들어감)
                    //위 문구 없으면 해당 문단 계속 반복되면서 editText 반복 갱신 --> 제대로 수정 불가
                }
            }
            .addOnFailureListener { exception ->
                // Task failed with an exception
                Log.e(TAG, "Text recognition error", exception)
                val message = getErrorMessage(exception)
                message?.let {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun leaveOnlyAvailableText(text : String): String {
        var result = ""



        for(i in text.indices){
            val translator = KorToBrailleConverter()

            if(text[i].toString() == " " || text[i].toString() == "\n"){
                result += text[i]
            }

            if (translator.checkValidity(text[i].toString())){
                result += text[i]
            }
        }
        srcText.value = result

        return result
    }

    private fun translateKorToBraille(text : String){

        val translator = KorToBrailleConverter()

        translatedText.value = translator.translate(text)
        brailleText.value = translatedText.value
    }

    private fun setEditText(){
        binding.editSrcText.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val result : String = leaveOnlyAvailableText(binding.editSrcText.text.toString())
                translateKorToBraille(result)
            }

            override fun afterTextChanged(p0: Editable?) {
                val result : String = leaveOnlyAvailableText(binding.editSrcText.text.toString())
                translateKorToBraille(result)
            }

        })
    }

    private fun getErrorMessage(exception: Exception): String? {
        val mlKitException = exception as? MlKitException ?: return exception.message
        return if (mlKitException.errorCode == MlKitException.UNAVAILABLE) {
//            "Waiting for text recognition model to be downloaded"
            "종료중..."
        } else exception.message
    }

    companion object {
        private const val TAG = "TextAnalyzer"
    }
}
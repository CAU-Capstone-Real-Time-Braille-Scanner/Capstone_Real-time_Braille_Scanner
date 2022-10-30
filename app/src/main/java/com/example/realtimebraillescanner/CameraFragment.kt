/*
 * Copyright 2020 Google Inc. All Rights Reserved.
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

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.realtimebraillescanner.databinding.CameraFragmentBinding
import com.example.realtimebraillescanner.util.Language
import com.example.realtimebraillescanner.util.ScopedExecutor
import kotlinx.android.synthetic.main.camera_fragment.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

class CameraFragment : Fragment() {

    companion object {
        fun newInstance() = CameraFragment()
        var braille : String = ""

        // We only need to analyze the part of the image that has text, so we set crop percentages
        // to avoid analyze the entire image from the live camera feed.
        const val DESIRED_WIDTH_CROP_PERCENT = 8
        const val DESIRED_HEIGHT_CROP_PERCENT = 84

        // This is an arbitrary number we are using to keep tab of the permission
        // request. Where an app has multiple context for requesting permission,
        // this can help differentiate the different contexts
        private const val REQUEST_CODE_PERMISSIONS = 10

        // This is an array of all the permission specified in the manifest
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val TAG = "CameraFragment"
    }

    private var displayId: Int = -1
    private val viewModel: MainViewModel by viewModels()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: PreviewView
    private lateinit var textAnalyzer: TextAnalyzer
    private lateinit var binding : CameraFragmentBinding

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var scopedExecutor: ScopedExecutor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = CameraFragmentBinding.inflate(inflater, container, false)
        initClickListener()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
        scopedExecutor.shutdown()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Test Case
//        val translator = KorToBrailleConverter()
//        Log.d("점자", (""))
//        Log.d("점자", translator.translate("가\"나다라\"마 가'나다라'마 "))
//        Log.d("점자", translator.translate("나무위키, 여러분이 가꾸어 나가는 지식의 나무."))
//        Log.d("점자", translator.translate("불어 되는 따뜻한 사람은 피고 하는 과실이 싶이 그리하였는가? 내는 이상 끝까지 속에 장식하는 것이다. 얼마나 힘차게 위하여 길지 장식하는 살 밥을 그들을 우리의 봄바람이다."))
//        Log.d("점자", translator.translate("밤이 계집애들의 하나에 당신은 자랑처럼 멀듯이, 지나고 아스라히 거외다. 위에도 어머니, 걱정도 너무나 것은 버리었습니다. 슬퍼하는 지나고 잠, 말 내일 이웃 이름과, 까닭입니다. 비둘기, 지나가는 하나의 하나에 때 마리아 이제 내린 듯합니다."))
//        Log.d("점자", translator.translate("나는 노루, 않은 우는 불러 별빛이 애기 멀리 거외다. 이름과, 그리고 이름을 이제 파란 계절이 라이너 밤이 옥 거외다. 같이 이름과 비둘기, 멀듯이, 차 봄이 아무 남은 듯합니다."))

        super.onViewCreated(view, savedInstanceState)

//        container = view as ConstraintLayout
//        viewFinder = container.findViewById(R.id.viewfinder)
        viewFinder = binding.viewfinder

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        scopedExecutor = ScopedExecutor(cameraExecutor)

        // Request camera permissions
        if (allPermissionsGranted()) {
            // Wait for the views to be properly laid out
            viewFinder.post {
                // Keep track of the display in which this view is attached
                displayId = viewFinder.display.displayId

                // Set up the camera and its use cases
                setUpCamera()
            }
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

//        // Get available language list and set up the target language spinner
//        // with default selections.
//        val adapter = ArrayAdapter(
//            requireContext(),
//            android.R.layout.simple_spinner_dropdown_item, viewModel.availableLanguages
//        )

//        targetLangSelector.adapter = adapter
//        targetLangSelector.setSelection(adapter.getPosition(Language("en")))
//        targetLangSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {
//                viewModel.targetLang.value = adapter.getItem(position)
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {}
//        }

//        viewModel.sourceLang.observe(viewLifecycleOwner, Observer { srcLang.text = it.displayName })
//        viewModel.translatedText.observe(viewLifecycleOwner, Observer { resultOrError ->
//            resultOrError?.let {
//                if (it.error != null) {
//                    translatedText.error = resultOrError.error?.localizedMessage
//                } else {
//                    translatedText.text = resultOrError.result
//                }
//            }
//        })
//        viewModel.modelDownloading.observe(viewLifecycleOwner, Observer { isDownloading ->
//            progressBar.visibility = if (isDownloading) {
//                View.VISIBLE
//            } else {
//                View.INVISIBLE
//            }
//            progressText.visibility = progressBar.visibility
//        })

        overlay.apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSPARENT)
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceChanged(
                    p0: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) = Unit

                override fun surfaceDestroyed(p0: SurfaceHolder) {
                }

                override fun surfaceCreated(p0: SurfaceHolder) {
                    holder?.let { drawOverlay(it, DESIRED_HEIGHT_CROP_PERCENT, DESIRED_WIDTH_CROP_PERCENT) }
                }

            })
        }

    }

    private fun initClickListener(){
        //각 버튼 케이스별 text 처리는 CameraFragment layout객체 TextAnalyzer로 넘겨줘서 처리
        binding.play.setOnClickListener {
            Toast.makeText(context, "번역을 실행합니다", Toast.LENGTH_SHORT).show()
            binding.mode.setText("1")
        }
        binding.pause.setOnClickListener {
            Toast.makeText(context, "일시정지", Toast.LENGTH_SHORT).show()
            binding.mode.setText("2")

            Handler().postDelayed({     //인식된 텍스트 반영 멈춘 뒤 실행위함
                setTextHighlight()    //텍스트 하이라이트
            }, 200)
        }
        binding.edit.setOnClickListener {
            Toast.makeText(context, "텍스트 수정이 가능합니다", Toast.LENGTH_SHORT).show()
            binding.mode.setText("3")
        }
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        val preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        textAnalyzer = TextAnalyzer(
            requireContext(),
            lifecycle,
            viewModel.sourceText,
            viewModel.translatedText,
            viewModel.braille,
            viewModel.imageCropPercentages,
            binding
        )

        // Build the image analysis use case and instantiate our analyzer
        imageAnalyzer = ImageAnalysis.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor
                    , textAnalyzer
                )
            }
        viewModel.sourceText.observe(viewLifecycleOwner, Observer {
            srcText.text = it
        })
        viewModel.translatedText.observe(viewLifecycleOwner, Observer {
            translatedText.text = it
        })
        viewModel.braille.observe(viewLifecycleOwner, Observer {
            braille = it
        })
        viewModel.imageCropPercentages.observe(viewLifecycleOwner,
            Observer { drawOverlay(overlay.holder, it.first, it.second) })

        // Select back camera since text detection does not work with front camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            val camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            //Zoom settings
            val scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener(){
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scale = camera.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                    camera.cameraControl.setZoomRatio(scale)
                    return true
                }
            })

            viewFinder.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)
                return@setOnTouchListener true
            }

            preview.setSurfaceProvider(viewFinder.createSurfaceProvider())
        } catch (exc: IllegalStateException) {
            Log.e(TAG, "Use case binding failed. This must be running on main thread.", exc)
        }
    }

    private fun drawOverlay(
        holder: SurfaceHolder,
        heightCropPercent: Int,
        widthCropPercent: Int
    ) {
        val canvas = holder.lockCanvas()
        val bgPaint = Paint().apply {
            alpha = 140
        }
        canvas.drawPaint(bgPaint)
        val rectPaint = Paint()
        rectPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        rectPaint.style = Paint.Style.FILL
        rectPaint.color = Color.WHITE
        val outlinePaint = Paint()
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.color = Color.WHITE
        outlinePaint.strokeWidth = 4f
        val surfaceWidth = holder.surfaceFrame.width()
        val surfaceHeight = holder.surfaceFrame.height()

        val cornerRadius = 25f
        // Set rect centered in frame
        val rectTop = surfaceHeight * 70 / 2 / 100f
        val rectLeft = surfaceWidth * widthCropPercent / 2 / 100f
        val rectRight = surfaceWidth * (1 - widthCropPercent / 2 / 100f)
        val rectBottom = surfaceHeight * (1 - 70 / 2 / 100f)
        val rect = RectF(rectLeft, rectTop, rectRight, rectBottom)
        canvas.drawRoundRect(
            rect, cornerRadius, cornerRadius, rectPaint
        )
        canvas.drawRoundRect(
            rect, cornerRadius, cornerRadius, outlinePaint
        )
        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 50F

        // Set text rect centered in frame
        val overlayText = "텍스트를 박스에 비춰주세요"
        val textBounds = Rect()
        textPaint.getTextBounds(overlayText, 0, overlayText.length, textBounds)
        val textX = (surfaceWidth - textBounds.width()) / 2f
        val textY = rectBottom + textBounds.height() + 15f // put text below rect and 15f padding
        canvas.drawText("텍스트를 박스에 비춰주세요", textX, textY, textPaint)
        holder.unlockCanvasAndPost(canvas)
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by comparing absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = ln(max(width, height).toDouble() / min(width, height))
        if (abs(previewRatio - ln(RATIO_4_3_VALUE))
            <= abs(previewRatio - ln(RATIO_16_9_VALUE))
        ) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun setTextHighlight(){      //문장의 각 어절 클릭 시 대응하는 점자 highlight
        val wordsTokens : List<String> = srcText.text.split("\n")   //줄바꿈 기준으로 문장 나누기

        var start = 0
        var end = 0
        var lenSentence = 0
        var lenBraille = 0
        var lenBrailleList = ArrayList<Int>()
        lenBrailleList.add(0)

            for(j in wordsTokens.indices){
            val tokens : List<String> = wordsTokens[j].split(" ") //한 문장에서 각 단어 색출
            var idx = 0 //문장에서 특정 단어의 인덱스를 찾을 때 사용하는 indexOf의 중복 케이스를 구분하기 위한 subString의 누적 idx
            var subSentence = wordsTokens[j].substring(wordsTokens[j].indices)   //찾은 단어를 배제하여 subSentence를 계속 줄여나감

            var sentenceBraille = KorToBrailleConverter().translate(wordsTokens[j])
            var startIndexForBraille = 0

//            var posB = ArrayList<Array<Int>>() //matching Braille position 저장
//            var startB = 0  //시작점
//            var endB = 0    //끝점

            for(i in tokens.indices){

                start = subSentence.indexOf(tokens[i]) + idx
                end = start + tokens[i].length

                var oneBraille = KorToBrailleConverter().translate(tokens[i]).trim()
//                var oneBraille = KorToBrailleConverter().translate(sentence.substring(start, end)).trim()
                var startBraille = sentenceBraille.indexOf(oneBraille, startIndexForBraille)
                var idxBraille = startBraille + lenBrailleList[j]

//                endB = startB + oneBraille.length
//                var arr = Array<Int>(2) { 0 }
//                arr[0] = startB
//                arr[1] = endB
//                posB.add(arr)

                Log.d("subString", subSentence)
                Log.d("subString sentence ", sentenceBraille)
                Log.d("subString token", tokens[i])
                Log.d("subString oneBraille", oneBraille)
    //            Log.d("subString idx", i.toString())
    //            Log.d("subString idx", idx.toString())
                Log.d("subString st ", start.toString())
                Log.d("subString ed ", end.toString())
                Log.d("subString Braille ", startBraille.toString())
    //            Log.d("subString stB ", posB.get(i)[0].toString())
    //            Log.d("subString edB ", posB.get(i)[1].toString())
                Log.d("subString startIdx ", startIndexForBraille.toString())
                Log.d("subString ", "")

                srcText.text.toSpannable().setSpan(object : ClickableSpan() {
                    override fun onClick(p0: View) {

                        var builder = SpannableStringBuilder(translatedText.text)

                        Log.d("subString sentence ", sentenceBraille)
                        Log.d("subString token", tokens[i])
                        Log.d("subString oneBraille", oneBraille)
                        Log.d("subString startBraille ", startBraille.toString())
                        Log.d("subString lenBraille ", lenBraille.toString())
//                        Log.d("subString lenBraille ", lenBrailleList[i].toString())
                        Log.d("subString sentence ", sentenceBraille)
                        Log.d("subString startIdx ", startIndexForBraille.toString())
                        Log.d("subString ", "idxBraille")
                        Log.d("subString ", "")

                        try {
                            builder.setSpan(UnderlineSpan(), idxBraille, idxBraille + oneBraille.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }catch (e : Exception){

                        }
                        translatedText.text = builder
                    }
                }, start + lenSentence, end + lenSentence, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

//                startB = endB + 1
                startIndexForBraille += oneBraille.length + 1
                idx = end + 1
                if (idx < wordsTokens[j].length)
                    subSentence = subSentence.substring((end - start + 1) until subSentence.length)
            }
                lenSentence += wordsTokens[j].length + 1
                lenBraille += KorToBrailleConverter().translate(wordsTokens[j]).length
                lenBrailleList.add(lenBraille)
        }

        srcText.linksClickable = true
        srcText.movementMethod = LinkMovementMethod.getInstance()
    }
    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post {
                    // Keep track of the display in which this view is attached
                    displayId = viewFinder.display.displayId

                    // Set up the camera and its use cases
                    setUpCamera()
                }
            } else {
                Toast.makeText(
                    context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }
}


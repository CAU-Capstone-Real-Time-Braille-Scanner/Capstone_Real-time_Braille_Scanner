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
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.example.realtimebraillescanner.CameraBTHFragment.Companion.text
import com.example.realtimebraillescanner.databinding.CameraHtbFragmentBinding
import com.example.realtimebraillescanner.util.ScopedExecutor
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

class CameraHTBFragment : Fragment() {

    companion object {
        fun newInstance() = CameraHTBFragment()
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
    private lateinit var binding : CameraHtbFragmentBinding
    private var tts: TextToSpeech? = null

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var scopedExecutor: ScopedExecutor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CameraHtbFragmentBinding.inflate(inflater, container, false)
        initTTS()
        initSettings()
        initClickListener()
        setIconBackground(0, 1, 1, 0, 1, 0)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
        scopedExecutor.shutdown()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

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

        binding.overlay.apply {
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
    private fun initTTS(){
        tts = TextToSpeech(requireContext(),
            TextToSpeech.OnInitListener {
                if (it == TextToSpeech.SUCCESS) {
                    // set US English as language for tts
                    val result = tts!!.setLanguage(Locale.KOREA)

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        //doSomething
                    } else {
                        //doSomething
                    }
                } else {
                    //doSomething
                }
            })
    }

    private fun initSettings(){
        val sizePreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val speedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val size = sizePreferences.getString("font_size", "")
        val speed = speedPreferences.getString("speed_of_voice", "")

        if (size.equals("작게")){
            setSize(14f)
        }
        else if (size.equals("크게")){
            setSize(30f)
        }
        else{
            setSize(22f)
        }

        if (speed.equals("느림")){
            setSpeed(0.5f)
        }
        else if (speed.equals("빠름")){
            setSpeed(1.5f)
        }
        else{
            setSpeed(1.0f)
        }
    }

    private fun setSize(size : Float){
        binding.srcText.textSize = size
        binding.translatedText.textSize = size
        binding.editSrcText.textSize = size
    }

    private fun setSpeed(size : Float){
        tts!!.setSpeechRate(size)
    }

    private fun srcTextWithoutLines() : String{
        return binding.srcText.text.toString().replace("\n", " ")
    }

    private fun initClickListener(){
        //각 버튼 케이스별 text 처리는 CameraFragment layout객체 TextAnalyzer로 넘겨줘서 처리
        binding.play.setOnClickListener {
            binding.srcText.visibility = View.VISIBLE
            binding.editSrcText.visibility = View.GONE

            binding.mode.setText("1")

            setIconBackground(1, 2, 0, 0, 0,0)
        }
        binding.pause.setOnClickListener {
            binding.srcText.visibility = View.VISIBLE
            binding.editSrcText.visibility = View.GONE

            binding.mode.setText("2")

            binding.srcText.setText(binding.srcText.text.toString() + " ")
            binding.translatedText.setText(binding.translatedText.text.toString() + " ")
            binding.srcText.text.trim()
            binding.translatedText.text.trim()

            setIconBackground(2, 1, 1, 1, 1,1)
        }
        binding.edit.setOnClickListener {
            if (binding.srcText.text.equals(null)){
                binding.editSrcText.setText("")
            }
            else{
                binding.editSrcText.setText(binding.srcText.text.toString())
            }

            binding.srcText.visibility = View.GONE
            binding.editSrcText.visibility = View.VISIBLE

            binding.mode.setText("3")

            setIconBackground(1, 0, 2, 0, 0,0)
        }
        binding.highlight.setOnClickListener {
            binding.mode.setText("2")

            Handler().postDelayed({     //인식된 텍스트 반영 멈춘 뒤 실행위함
                setTextHighlight()    //텍스트 하이라이트
            }, 100)

            if (binding.srcText.text.toString().trim().replace(" ", "").equals("")){
                Toast.makeText(requireContext(), "하이라이트할 텍스트를 먼저 촬영해주세요", Toast.LENGTH_SHORT).show()
            }
            else{
                setIconBackground(1, 0, 0, 2, 0,
                    0)
            }
        }
        binding.voice.setOnClickListener {
            binding.mode.setText("2")
            val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")} //한국어로 녹음
            startActivityForResult(speechRecognizerIntent, 100)
        }
        binding.speak.setOnClickListener{
            var tts_bundle = Bundle()
            tts_bundle.putFloat(KEY_PARAM_VOLUME, 1.0F) //음량 최대로 설정
            tts!!.speak(srcTextWithoutLines(), TextToSpeech.QUEUE_FLUSH, tts_bundle, "") //tts 소리 출력
            if (binding.srcText.text.toString().trim().replace(" ", "").equals(""))
                Toast.makeText(requireContext(), "음성 재생할 텍스트를 먼저 촬영해주세요", Toast.LENGTH_SHORT).show()
            setIconBackground(2, 1, 1, 1, 1,1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 100) {
            binding.srcText.text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!![0]
            binding.editSrcText.setText(binding.srcText.text.toString())
            binding.mode.setText("2")

            setIconBackground(2, 1, 1, 1, 1,1)
        }

    }

    private fun setIconBackground(pause : Int, play : Int, edit : Int, highlight : Int, voice : Int, speak : Int){

        if (pause == 1){
            binding.pause.setImageDrawable(resources.getDrawable(R.drawable.pause_click))
            binding.pause.isClickable = true
        }
        else if (pause == 0){
            binding.pause.setImageDrawable(resources.getDrawable(R.drawable.pause_d))
            binding.pause.isClickable = false
        }
        else{
            binding.pause.setImageDrawable(resources.getDrawable(R.drawable.pause_g))
            binding.pause.isClickable = false
        }

        if (play == 1){
            binding.play.setImageDrawable(resources.getDrawable(R.drawable.play_click))
            binding.play.isClickable = true
        }
        else if (play == 0){
            binding.play.setImageDrawable(resources.getDrawable(R.drawable.play_d))
            binding.play.isClickable = false
        }
        else{
            binding.play.setImageDrawable(resources.getDrawable(R.drawable.play_g))
            binding.play.isClickable = false
        }

        if (edit == 1){
            binding.edit.setImageDrawable(resources.getDrawable(R.drawable.edit_click))
            binding.edit.isClickable = true
        }
        else if (edit == 0){
            binding.edit.setImageDrawable(resources.getDrawable(R.drawable.edit_d))
            binding.edit.isClickable = false
        }
        else{
            binding.edit.setImageDrawable(resources.getDrawable(R.drawable.edit_g))
            binding.edit.isClickable = false
        }

        if (highlight == 1){
            binding.highlight.setImageDrawable(resources.getDrawable(R.drawable.highlight_click))
            binding.highlight.isClickable = true
        }
        else if (highlight == 0){
            binding.highlight.setImageDrawable(resources.getDrawable(R.drawable.highlight_d))
            binding.highlight.isClickable = false
        }
        else{
            binding.highlight.setImageDrawable(resources.getDrawable(R.drawable.highlight_g))
            binding.highlight.isClickable = false
        }

        if (voice == 1){
            binding.voice.setImageDrawable(resources.getDrawable(R.drawable.voice_click))
            binding.voice.isClickable = true
        }
        else if (voice == 0){
            binding.voice.setImageDrawable(resources.getDrawable(R.drawable.voice_d))
            binding.voice.isClickable = false
        }
        else{
            binding.voice.setImageDrawable(resources.getDrawable(R.drawable.voice_g))
            binding.voice.isClickable = false
        }
        if (speak == 1){
            binding.speak.setImageDrawable(resources.getDrawable(R.drawable.speak_click))
            binding.speak.isClickable = true
        }
        else if (speak == 0){
            binding.speak.setImageDrawable(resources.getDrawable(R.drawable.speak_d))
            binding.speak.isClickable = false
        }
        else{
            binding.speak.setImageDrawable(resources.getDrawable(R.drawable.speak_g))
            binding.speak.isClickable = false
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
            binding.srcText.text = it
        })
        viewModel.translatedText.observe(viewLifecycleOwner, Observer {
            binding.translatedText.text = it
        })
        viewModel.braille.observe(viewLifecycleOwner, Observer {
            braille = it
        })
        viewModel.imageCropPercentages.observe(viewLifecycleOwner,
            Observer { drawOverlay(binding.overlay.holder, it.first, it.second) })

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

            preview.setSurfaceProvider(viewFinder.surfaceProvider)
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
        val wordsTokens : List<String> = binding.srcText.text.split("\n")   //줄바꿈 기준으로 문장 나누기

        var lenSentence = 0
        var lenSentenceList = ArrayList<Int>()
        val startIdxList = Array(wordsTokens.size) { Array(100) { 0 } }
        val lenWordList = Array(wordsTokens.size) { Array(100) { 0 } }
        val statePressed = Array(wordsTokens.size) { Array(100) { false } }
        lenSentenceList.add(lenSentence)

        var lenBSentence = 0
        var lenBSentenceList = ArrayList<Int>()
        val startBIdxList = Array(wordsTokens.size) { Array(100) { 0 } }
        val lenBWordList = Array(wordsTokens.size) { Array(100) { 0 } }
        val stateBPressed = Array(wordsTokens.size) { Array(100) { false } }
        lenBSentenceList.add(lenBSentence)

        for(i in wordsTokens.indices){

            var startIdx = 0
            var startBIdx = 0
            val tokens : List<String> = wordsTokens[i].split(" ") //한 문장에서 각 단어 색출

            for(j in tokens.indices){

                val lenWord = tokens[j].length
                lenWordList[i][j] = lenWord
                if (j > 0){
                    startIdx += tokens[j-1].length + 1
                    startIdxList[i][j] = startIdx
                }

                val lenBWord = KorToBrailleConverter().translate(tokens[j]).trim().length
                lenBWordList[i][j] = lenBWord
                if (j > 0){
                    startBIdx += KorToBrailleConverter().translate(tokens[j-1]).trim().length + 1
                    startBIdxList[i][j] = startBIdx
                }

                binding.srcText.text.toSpannable().setSpan(object : ClickableSpan() {

                    override fun onClick(p0: View) {
                        //srcText 클릭 시 srcText 및 translatedText highlight 하기
                        binding.srcText.text = setHighlightWhenClicked(i, j, binding.srcText.text, statePressed, startIdxList, lenSentenceList, lenWordList)
                        binding.translatedText.text = setHighlightWhenClicked(i, j, binding.translatedText.text, stateBPressed, startBIdxList, lenBSentenceList, lenBWordList)

                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = Color.BLACK
                        ds.isUnderlineText = false
                    }

                },  startIdx + lenSentence, startIdx + lenWord + lenSentence, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            }
            lenSentence += wordsTokens[i].length + 1
            lenSentenceList.add(lenSentence)

            lenBSentence += KorToBrailleConverter().translate(wordsTokens[i]).length
            lenBSentenceList.add(lenBSentence)
        }

        binding.srcText.linksClickable = true
        binding.srcText.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setHighlightWhenClicked(i:Int, j:Int, text:CharSequence, statePressed:Array<Array<Boolean>>, startIdxList:Array<Array<Int>>, lenSentenceList:ArrayList<Int>, lenWordList:Array<Array<Int>>):SpannableStringBuilder{
        val underLineRemoveSpan = object : UnderlineSpan(){
            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
            }
        }

        var builderSrc = SpannableStringBuilder(text)
        try {
            if (!statePressed[i][j]){
                builderSrc.setSpan(UnderlineSpan(), startIdxList[i][j] + lenSentenceList[i], startIdxList[i][j] + lenWordList[i][j] + lenSentenceList[i], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                statePressed[i][j] = true
            }
            else{
                builderSrc.setSpan(underLineRemoveSpan, startIdxList[i][j] + lenSentenceList[i], startIdxList[i][j] + lenWordList[i][j] + lenSentenceList[i], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                statePressed[i][j] = false
            }
        }catch (e : Exception){
            Toast.makeText(requireContext(), "인식할 수 없음", Toast.LENGTH_SHORT).show()
        }
        return builderSrc
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

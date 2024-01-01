package com.example.realtimebraillescanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import android.util.Log
import android.view.*
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.realtimebraillescanner.databinding.CameraBthFragmentBinding
import com.example.realtimebraillescanner.util.ScopedExecutor
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.camera_bth_fragment.*
import java.util.*

class CameraBTHFragment : Fragment() {
    companion object {
        fun newInstance() = CameraBTHFragment()

        private const val TAG = "CameraBTHActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        // We only need to analyze the part of the image that has text, so we set crop percentages
        // to avoid analyze the entire image from the live camera feed.
        const val DESIRED_WIDTH_CROP_PERCENT = 8
        const val DESIRED_HEIGHT_CROP_PERCENT = 84

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    private var imageCapture: ImageCapture? = null
    private var displayId: Int = -1
    private val viewModel: BTHViewModel by viewModels()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageAnalyzer: ImageAnalysis
    private lateinit var viewFinder: PreviewView
    private lateinit var brailleAnalyzer: BrailleAnalyzer
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var scopedExecutor: ScopedExecutor
    private lateinit var binding: CameraBthFragmentBinding
    private var tts: TextToSpeech? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CameraBthFragmentBinding.inflate(inflater, container, false)
        initTTS()
        initSettings()
        initClickListener()
        setIconBackground(0, 1, 0)
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
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
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
    }

    private fun setSpeed(size : Float){
        tts!!.setSpeechRate(size)
    }

    private fun initClickListener() {
        binding.play.setOnClickListener {
            binding.srcText.visibility = View.VISIBLE
            binding.recognizedText.visibility = View.GONE
            binding.mode.text = "1"

            setIconBackground(1, 2, 0)
        }
        binding.pause.setOnClickListener {
            binding.srcText.visibility = View.VISIBLE
            binding.recognizedText.visibility = View.GONE
            binding.mode.text = "0"

            binding.srcText.text = binding.srcText.text.toString()
            binding.translatedText.text = binding.translatedText.text.toString()
            binding.srcText.text.trim()
            binding.translatedText.text.trim()

            setIconBackground(2, 1, 1)
        }
        binding.speak.setOnClickListener{
            var tts_bundle = Bundle()
            tts_bundle.putFloat(KEY_PARAM_VOLUME, 1.0F) //음량 최대로 설정
            tts!!.speak(translatedText.text, TextToSpeech.QUEUE_FLUSH, tts_bundle, "") //tts 소리 출력
            if (binding.translatedText.text.toString().trim().replace(" ", "").equals(""))
                Toast.makeText(requireContext(), "음성 재생할 텍스트를 먼저 촬영해주세요", Toast.LENGTH_SHORT).show()
            setIconBackground(2, 1, 1)
        }
    }

    private fun setIconBackground(pause : Int, play : Int, speak : Int){
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

    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

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

            imageCapture = ImageCapture.Builder()
                .build()

            brailleAnalyzer = BrailleAnalyzer(
                viewModel.sourceText,
                viewModel.translatedText,
                viewModel.imageCropPercentages,
                binding,
                arguments?.getString("hashValue") ?: "uniqueIDNotFound"
            )

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        brailleAnalyzer
                    )
                }

            viewModel.sourceText.observe(viewLifecycleOwner) {
                binding.srcText.text = it
            }
            viewModel.translatedText.observe(viewLifecycleOwner) {
                binding.translatedText.text = it
            }
            viewModel.imageCropPercentages.observe(viewLifecycleOwner) {
                drawOverlay(binding.overlay.holder, it.first, it.second)
            }

            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer, imageCapture
                )

                // Zoom settings
                val scaledGestureDetector = ScaleGestureDetector(requireContext(),
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

        }, ContextCompat.getMainExecutor(requireContext()))
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

        // Set rect centered in frame
        val rectTop = surfaceHeight * 70 / 2 / 100f
        val rectLeft = surfaceWidth * widthCropPercent / 2 / 100f
        val rectRight = surfaceWidth * (1 - widthCropPercent / 2 / 100f)
        val rectBottom = surfaceHeight * (1 - 70 / 2 / 100f)
        val rect = RectF(rectLeft, rectTop, rectRight, rectBottom)

        canvas.drawRect(
            rect, rectPaint
        )
        canvas.drawRect(
            rect, outLinePaint
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
        return if (abs(previewRatio - ln(RATIO_4_3_VALUE)) <= abs(previewRatio - ln(RATIO_16_9_VALUE))) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post {
                    // Keep track of the display in which this view is attached
                    displayId = viewFinder.display.displayId
                    startCamera()
                }
            } else {
                Toast.makeText(context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }
}
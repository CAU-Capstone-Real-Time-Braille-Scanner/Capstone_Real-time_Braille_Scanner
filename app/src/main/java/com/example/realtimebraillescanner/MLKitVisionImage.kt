package com.example.realtimebraillescanner

import android.content.Context
import android.graphics.Rect
import android.view.ContextMenu
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import com.example.realtimebraillescanner.util.ImageUtils
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class MLKitVisionImage(
    private val context: Context,
    private val lifecycle: Lifecycle,
) : ImageAnalysis.Analyzer {
    val localModel = LocalModel.Builder()
        .setAssetFilePath("model.tflite")
        .build()

    // Live multiple object detection and tracking
    val customObjectDetectorOptions =
        CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f)
            .setMaxPerObjectLabelCount(3)
            .build()

    val objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)

    init {
        lifecycle.addObserver(objectDetector)
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            // Pass image to an ML Kit Vision API

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
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                }
        }
        // val convertedImageToBitmap = ImageUtils.convertYuv420888ImageToBitmap(mediaImage)

    }
}
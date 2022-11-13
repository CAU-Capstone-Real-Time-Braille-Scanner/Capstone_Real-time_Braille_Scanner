package com.example.realtimebraillescanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.realtimebraillescanner.CameraHTBFragment.Companion.DESIRED_HEIGHT_CROP_PERCENT
import com.example.realtimebraillescanner.CameraHTBFragment.Companion.DESIRED_WIDTH_CROP_PERCENT
import com.example.realtimebraillescanner.util.SmoothedMutableLiveData

class BTHViewModel(application: Application): AndroidViewModel(application) {
    companion object {
        // Amount of time (in milliseconds) to wait for detected text to settle
        private const val SMOOTHING_DURATION = 50L
    }

    val sourceText = SmoothedMutableLiveData<String>(SMOOTHING_DURATION)

    // We set desired crop percentages to avoid having to analyze the whole image from the live
    // camera feed. However, we are not guaranteed what aspect ratio we will get from the camera, so
    // we use the first frame we get back from the camera to update these crop percentages based on
    // the actual aspect ratio of images.
    val imageCropPercentages = MutableLiveData<Pair<Int, Int>>()
        .apply { value = Pair(DESIRED_HEIGHT_CROP_PERCENT, DESIRED_WIDTH_CROP_PERCENT) }
    val translatedText = SmoothedMutableLiveData<String>(SMOOTHING_DURATION)
    val koreanText = SmoothedMutableLiveData<String>(SMOOTHING_DURATION)
}
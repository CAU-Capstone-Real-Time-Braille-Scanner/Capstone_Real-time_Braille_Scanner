package com.example.realtimebraillescanner.retrofit_util

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class DataModel(
    @SerializedName("srcText") @Expose
    val srcText: String,
    @SerializedName("translatedText") @Expose
    val translatedText: String
)
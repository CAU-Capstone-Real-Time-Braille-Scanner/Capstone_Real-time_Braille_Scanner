package com.example.realtimebraillescanner.retrofit_util

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class DataModel {
    @SerializedName("srcText")
    @Expose
    lateinit var srcText: String

    @SerializedName("translatedText")
    @Expose
    lateinit var translatedText: String
}
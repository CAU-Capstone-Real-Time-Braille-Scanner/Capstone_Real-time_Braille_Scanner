package com.example.realtimebraillescanner.retrofit_util

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RetrofitInterface {
    @POST("loadModel")
    fun loadModel(): Call<String>

    @Multipart
    @POST("inference")
    fun getResult(
        @Part imageFile: MultipartBody.Part,
    ): Call<DataModel>
}
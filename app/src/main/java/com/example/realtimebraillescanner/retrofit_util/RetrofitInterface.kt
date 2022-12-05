package com.example.realtimebraillescanner.retrofit_util

import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Path

interface RetrofitInterface {
    @POST("inference")
    fun testApiGet(
        @Path("srcText") srcText: String,
        @Path("translatedText") translatedText: String
    ): Call<DataModel>
}
package com.example.realtimebraillescanner.retrofit_util

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    /**
     * TODO: 아래 URL 을 컴퓨터 IP 를 확인한 후 수정해야 함.
     * TODO: 아래 URL 은 208-415에서 Wi-Fi 를 Smart-CAU 로 설정했을 때 나온 결과  
     */
    const val BASE_URL = "http://10.210.61.70:8080/"

    fun getInstance(): Retrofit {
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun getApiService() = getInstance().create(RetrofitInterface::class.java)
}
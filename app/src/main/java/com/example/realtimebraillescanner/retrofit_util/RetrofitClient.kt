package com.example.realtimebraillescanner.retrofit_util

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient {
    companion object {
        // TODO: 아래 URL 은 컴퓨터 IP 를 확인한 후 수정해야 함
        const val BASE_URL = "http://192.168.1.15:8080/"

        fun getInstance(): Retrofit {
            val gson = GsonBuilder().setLenient().create()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }

        fun getApiService() = getInstance().create(RetrofitInterface::class.java)
    }
}
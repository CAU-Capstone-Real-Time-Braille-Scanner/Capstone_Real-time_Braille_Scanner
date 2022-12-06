package com.example.realtimebraillescanner.retrofit_util

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    /**
     * IP주소: 52.79.233.83
     */
    const val BASE_URL = "http://52.79.233.83:8080/"

    fun getInstance(): Retrofit {
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun getApiService() = getInstance().create(RetrofitInterface::class.java)
}
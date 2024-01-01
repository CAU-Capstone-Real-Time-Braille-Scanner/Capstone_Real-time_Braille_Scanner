package com.example.realtimebraillescanner.retrofit_util

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitClient {
    /**
     * IP 주소: IPAddress.kt 파일에 정의되어 있는 BASE_URL 참조.
     */
    //private const val BASE_URL = "http://???.???.??.???:????/"

    private fun getInstance1(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    private fun getInstance2(): Retrofit {
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun getApiService1(): RetrofitInterface = getInstance1().create(RetrofitInterface::class.java)

    fun getApiService2(): RetrofitInterface = getInstance2().create(RetrofitInterface::class.java)
}
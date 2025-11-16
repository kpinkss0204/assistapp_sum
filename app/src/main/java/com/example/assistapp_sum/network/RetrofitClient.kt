package com.example.assistapp_sum.network

import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

object RetrofitClient {
    val instance: FoodSafetyApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://openapi.foodsafetykorea.go.kr/api/")
            .addConverterFactory(SimpleXmlConverterFactory.createNonStrict()) // ✅ NonStrict 필수
            .build()
            .create(FoodSafetyApi::class.java)
    }
}

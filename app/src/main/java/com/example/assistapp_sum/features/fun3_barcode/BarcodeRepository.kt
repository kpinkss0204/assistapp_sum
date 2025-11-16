package com.example.assistapp_sum.features.fun3_barcode

import android.util.Log
import com.example.assistapp_sum.model.C005Response
import com.example.assistapp_sum.network.FoodSafetyApi
import com.example.assistapp_sum.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BarcodeRepository {

    fun fetchProduct(
        barcode: String,
        onSuccess: (C005Response?) -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        val api: FoodSafetyApi = RetrofitClient.instance
        val call = api.getProductByBarcode(
            keyId = "7798fd698f1f456a9988",
            serviceId = "C005",
            dataType = "xml",
            startIdx = 1,
            endIdx = 5,
            barcode = barcode
        )

        Log.d("BarcodeRepo", "📡 요청 URL = ${call.request().url}")

        call.enqueue(object : Callback<C005Response> {
            override fun onResponse(call: Call<C005Response>, response: Response<C005Response>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("BarcodeRepo", "✅ 응답 성공: $body")
                    onSuccess(body)
                } else {
                    Log.e("BarcodeRepo", "❌ 응답 실패 코드: ${response.code()}")
                    onError(null)
                }
            }

            override fun onFailure(call: Call<C005Response>, t: Throwable) {
                Log.e("BarcodeRepo", "❌ API 통신 오류: ${t.message}")
                onError(t)
            }
        })
    }
}

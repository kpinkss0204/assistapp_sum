package com.example.assistapp_sum.features.fun6_location

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class LocationSharingWithCodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocationSharingWithCodeScreen() // 패키지 맞게 호출
        }
    }
}

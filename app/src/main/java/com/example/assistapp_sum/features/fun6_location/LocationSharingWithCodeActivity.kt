package com.example.assistapp_sum.features.fun6_location


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.assistapp_sum.features.LocationSharing.LocationSharingWithCodeScreen

class LocationSharingWithCodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocationSharingWithCodeScreen() // 기존 Composable 호출
        }
    }
}

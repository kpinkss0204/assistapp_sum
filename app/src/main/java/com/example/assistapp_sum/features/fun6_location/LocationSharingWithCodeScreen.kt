package com.example.assistapp_sum.features.fun6_location

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.example.assistapp_sum.services.LocationTrackingService
import java.security.MessageDigest

@Composable
fun LocationSharingWithCodeScreen() {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val realtimeDB = FirebaseDatabase.getInstance().reference.child("shared_locations")

    val sharedPreferences = remember {
        context.getSharedPreferences("location_sending_prefs", Context.MODE_PRIVATE)
    }

    var generatedKey by remember {
        mutableStateOf(sharedPreferences.getString("generated_key", "") ?: "")
    }

    // 권한 요청 상태
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (!granted) {
            Toast.makeText(context, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        } else {
            // 권한 허용 시 서비스 시작
            if (generatedKey.isNotEmpty()) {
                val initialData = mapOf(
                    "lat" to 0.0,
                    "lon" to 0.0,
                    "timestamp" to System.currentTimeMillis()
                )
                realtimeDB.child(generatedKey!!).setValue(initialData)
                LocationTrackingService.startService(context, generatedKey!!)
                Toast.makeText(context, "🔔 위치 공유 서비스 시작", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 최초 암호 생성 및 Firestore 저장
    LaunchedEffect(Unit) {
        if (generatedKey.isEmpty()) {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()-_=+"
            val newKey = (1..12).map { chars.random() }.joinToString("")
            generatedKey = newKey
            sharedPreferences.edit().putString("generated_key", newKey).apply()

            val hashBytes = MessageDigest.getInstance("SHA-256").digest(newKey.toByteArray())
            val docId = hashBytes.joinToString("") { "%02x".format(it) }.take(32)

            val data = hashMapOf(
                "originalCode" to newKey,
                "docId" to docId,
                "createdAt" to Timestamp.now()
            )
            firestore.collection("location_keys")
                .document(docId)
                .set(data)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📍 내 위치 공유", style = MaterialTheme.typography.titleMedium)

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("내 고유 암호코드", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (generatedKey.isNotEmpty()) generatedKey else "(생성 중...)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "이 코드를 상대방에게 공유하세요",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Button(
            onClick = {
                if (generatedKey.isNotEmpty()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("암호코드", generatedKey))
                    Toast.makeText(context, "클립보드에 복사되었습니다", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = generatedKey.isNotEmpty()
        ) {
            Text("📋 클립보드에 복사")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 권한 요청 버튼
        Button(
            onClick = {
                val permissionsToRequest = mutableListOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissionsToRequest.add(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                }
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⚠️ 위치 권한 요청 및 서비스 시작")
        }
    }
}

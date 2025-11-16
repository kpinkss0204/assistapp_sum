package com.example.assistapp_sum.features.LocationSharing

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.location.*

@Composable
fun LocationSharingWithCodeScreen() {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val realtimeDb = FirebaseDatabase.getInstance().reference
    val sharedPreferences = context.getSharedPreferences("location_sharing_prefs", Context.MODE_PRIVATE)

    var generatedKey by remember { mutableStateOf(sharedPreferences.getString("generated_key", "") ?: "") }
    var isCodeVisible by remember { mutableStateOf(false) }
    var isBeingTracked by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // 위치 권한 요청
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "위치 권한 허용됨", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "위치 권한 거부됨", Toast.LENGTH_SHORT).show()
        }
    }

    // 권한 체크
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 최초 암호 생성 및 Firestore 저장
    LaunchedEffect(Unit) {
        if (generatedKey.isEmpty()) {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()-_=+"
            val newKey = (1..12).map { chars.random() }.joinToString("")
            generatedKey = newKey
            sharedPreferences.edit().putString("generated_key", newKey).apply()

            // Firestore 저장
            val messageDigest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = messageDigest.digest(newKey.toByteArray())
            val docId = hashBytes.joinToString("") { "%02x".format(it) }.take(32)
            val firestoreData = hashMapOf(
                "originalCode" to newKey,
                "docId" to docId,
                "createdAt" to Timestamp.now()
            )
            firestore.collection("location_keys")
                .document(docId)
                .set(firestoreData)

            // Realtime Database 초기값 저장
            val realtimeData = mapOf(
                "originalCode" to newKey,
                "lat" to null,
                "lon" to null,
                "timestamp" to System.currentTimeMillis()
            )
            realtimeDb.child("shared_locations").child(newKey).setValue(realtimeData)
        }
    }

    // tracking_requests 감시 → 누군가 내 코드를 입력하면 위치 공유 시작
    DisposableEffect(generatedKey, hasLocationPermission) {
        if (generatedKey.isEmpty()) return@DisposableEffect onDispose {}

        val trackingRef = realtimeDb.child("tracking_requests").child(generatedKey)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isBeingTracked = snapshot.exists()
                if (isBeingTracked) {
                    Toast.makeText(context, "🔔 위치 공유 시작", Toast.LENGTH_SHORT).show()
                    if (hasLocationPermission) {
                        sendLocation(context, generatedKey)
                    }
                } else {
                    Toast.makeText(context, "📴 위치 공유 중단", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        trackingRef.addValueEventListener(listener)
        onDispose { trackingRef.removeEventListener(listener) }
    }

    // UI
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("📍 내 암호코드", style = MaterialTheme.typography.titleMedium) }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("암호코드")
                        TextButton(onClick = { isCodeVisible = !isCodeVisible }) {
                            Text(if (isCodeVisible) "숨기기" else "보기")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(if (isCodeVisible) generatedKey else "••••••••••••", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))

                    Button(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("암호코드", generatedKey))
                        Toast.makeText(context, "클립보드 복사 완료", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("📋 복사")
                    }
                }
            }
        }

        item {
            Text("위치 공유 상태: ${if (isBeingTracked) "실행 중" else "중단"}")
        }

        if (!hasLocationPermission) {
            item {
                Button(onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                    Text("위치 권한 요청")
                }
            }
        }
    }
}

// GPS에서 위치 받아서 Realtime Database에 전송
fun sendLocation(context: Context, key: String) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val data = mapOf(
                    "lat" to location.latitude,
                    "lon" to location.longitude,
                    "timestamp" to System.currentTimeMillis()
                )
                FirebaseDatabase.getInstance().reference.child("shared_locations").child(key)
                    .updateChildren(data)
                    .addOnSuccessListener { android.util.Log.d("LocationSharing", "위치 업데이트 성공") }
                    .addOnFailureListener { android.util.Log.e("LocationSharing", "위치 업데이트 실패", it) }
            }
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
    }
}

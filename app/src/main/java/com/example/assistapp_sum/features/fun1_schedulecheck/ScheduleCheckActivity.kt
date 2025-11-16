package com.example.assistapp_sum.features.fun1_schedulecheck

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.collections.mapNotNull

data class Schedule(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val place: String
)

class ScheduleCheckActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {   // 기존 AssistAppTheme -> MaterialTheme
                ScheduleCheckScreen()
            }
        }
    }
}

@Composable
fun ScheduleCheckScreen() {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    val sharedPreferences = context.getSharedPreferences("settings", 0)
    val generatedKey = sharedPreferences.getString("generated_key", "") ?: ""

    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf<Schedule?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    // Firestore 실시간 데이터 감시
    LaunchedEffect(generatedKey) {
        if (generatedKey.isEmpty()) {
            Toast.makeText(context, "❌ 고유 코드가 설정되지 않았습니다.", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }

        firestore.collection("shared_schedules")
            .document(generatedKey)
            .collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "❌ 오류: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                schedules = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Schedule(
                        id = doc.id,
                        title = data["title"]?.toString() ?: "제목 없음",
                        date = data["date"]?.toString() ?: "날짜 미정",
                        time = data["time"]?.toString() ?: "시간 미정",
                        place = data["place"]?.toString() ?: "장소 미정"
                    )
                } ?: emptyList()
            }
    }

    // 삭제 다이얼로그
    showDeleteDialog?.let { schedule ->
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = null },
            title = { Text("일정 삭제") },
            text = { Text("'${schedule.title}' 일정을 삭제하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        firestore.collection("shared_schedules")
                            .document(generatedKey)
                            .collection("items")
                            .document(schedule.id)
                            .delete()
                            .addOnSuccessListener {
                                isDeleting = false
                                showDeleteDialog = null
                                Toast.makeText(context, "✅ 일정이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { ex ->
                                isDeleting = false
                                Toast.makeText(context, "❌ 삭제 실패: ${ex.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isDeleting) "삭제중..." else "삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }, enabled = !isDeleting) {
                    Text("취소")
                }
            }
        )
    }

    // UI
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("📥 받은 일정 목록", style = MaterialTheme.typography.titleLarge)
        }

        if (schedules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("받은 일정이 없습니다", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        items(schedules) { schedule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(schedule.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("📅 ${schedule.date} ${schedule.time}\n📍 ${schedule.place}", style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { showDeleteDialog = schedule }) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제")
                    }
                }
            }
        }
    }
}

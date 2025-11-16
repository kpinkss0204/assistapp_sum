package com.example.assistapp_sum.features.fun1_schedulecheck

import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.assistapp_sum.R
import com.example.assistapp_sum.core.GestureTapCounter
import com.example.assistapp_sum.core.TTSManager
import com.google.firebase.database.*

class ScheduleCheckActivity : AppCompatActivity() {

    private lateinit var tv: TextView
    private lateinit var gestureDetector: GestureDetector
    private val taps = GestureTapCounter(800)
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_check)

        tv = findViewById(R.id.tvSchedule)
        TTSManager.ensureInit(this)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val count = taps.onTap()
                if (count == 3) {
                    TTSManager.speak(this@ScheduleCheckActivity, "메인화면으로 돌아갑니다.")
                    finish()
                }
                return true
            }
        })

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val generatedKey = prefs.getString("generated_key", null)

        if (generatedKey.isNullOrEmpty()) {
            tv.text = "고유 코드가 설정되지 않았습니다.\n기능 관리에서 코드를 입력해주세요."
            TTSManager.speak(this, "고유 코드가 설정되지 않았습니다. 기능 관리에서 코드를 입력해주세요.")
            return
        }

        dbRef = FirebaseDatabase.getInstance()
            .getReference("shared_schedules")
            .child(generatedKey)
            .child("items")

        loadSchedules()
    }

    private fun loadSchedules() {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    tv.text = "등록된 일정이 없습니다."
                    TTSManager.speak(this@ScheduleCheckActivity, "등록된 일정이 없습니다.")
                    return
                }

                val builder = StringBuilder()
                val ttsText = StringBuilder("등록된 일정이 있습니다. ")

                for (child in snapshot.children) {
                    val title = child.child("title").getValue(String::class.java) ?: "제목 없음"
                    val time = child.child("time").getValue(String::class.java) ?: "시간 미정"
                    val place = child.child("place").getValue(String::class.java) ?: "장소 미정"

                    builder.append("제목: $title\n시간: $time\n장소: $place\n\n")
                    ttsText.append("$title, $time, $place. ")
                }

                tv.text = builder.toString().trim()
                TTSManager.speak(this@ScheduleCheckActivity, ttsText.toString() + "세 번 탭하면 메인화면으로 돌아갑니다.")
            }

            override fun onCancelled(error: DatabaseError) {
                tv.text = "데이터를 불러오는 중 오류가 발생했습니다."
                TTSManager.speak(this@ScheduleCheckActivity, "데이터를 불러오는 중 오류가 발생했습니다.")
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onDestroy() {
        TTSManager.shutdown()
        super.onDestroy()
    }
}

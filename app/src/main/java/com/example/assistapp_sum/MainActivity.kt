package com.example.assistapp_sum

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.assistapp_sum.databinding.ActivityMainBinding
import com.example.assistapp_sum.core.TTSManager
import com.example.assistapp_sum.features.feature_manager.FeatureManagerActivity
import com.example.assistapp_sum.features.fun1_schedulecheck.ScheduleCheckActivity
import com.example.assistapp_sum.features.fun2_navigation.NavigationActivity
import com.example.assistapp_sum.features.fun3_barcode.BarcodeRecognitionActivity
import com.example.assistapp_sum.features.fun4_hand.HandTrainingActivity
import com.example.assistapp_sum.features.fun5_bill.BillRecognitionActivity
import com.example.assistapp_sum.features.fun6_location.LocationSharingWithCodeActivity   // ✅ 추가
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gestureDetector: GestureDetector
    private var currentIndex = 0

    // ✅ 위치 공유 추가됨
    private val screens = listOf(
        "메인화면",
        "일정표 확인",
        "도보 네비게이션",
        "상품 인식",
        "손 감각 훈련",
        "지폐 인식",
        "위치 공유"          // <-- 여기 추가됨
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        TTSManager.ensureInit(this)

        gestureDetector = GestureDetector(this, GestureListener())

        speakCurrent()

        binding.btnMenu.setOnClickListener {
            startActivity(Intent(this, FeatureManagerActivity::class.java))
        }

        binding.btnManageFeatures.setOnClickListener {
            TTSManager.speak(this, "기능 관리 화면으로 이동합니다.")
            startActivity(Intent(this, FeatureManagerActivity::class.java))
        }
    }

    private fun moveNext() {
        currentIndex = (currentIndex + 1) % screens.size
        speakCurrent()
    }

    private fun movePrev() {
        currentIndex = if (currentIndex - 1 < 0) screens.size - 1 else currentIndex - 1
        speakCurrent()
    }

    private fun speakCurrent() {
        val msg = "${screens[currentIndex]}입니다. 더블탭하면 실행합니다."
        binding.textView.text = msg
        TTSManager.speak(this, msg)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (e1 == null) return false
            val diffX = e2.x - e1.x
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(vx) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) movePrev() else moveNext()
                return true
            }
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            when (screens[currentIndex]) {
                "일정표 확인" ->
                    startActivity(Intent(this@MainActivity, ScheduleCheckActivity::class.java))

                "도보 네비게이션" ->
                    startActivity(Intent(this@MainActivity, NavigationActivity::class.java))

                "상품 인식" ->
                    startActivity(Intent(this@MainActivity, BarcodeRecognitionActivity::class.java))

                "손 감각 훈련" ->
                    startActivity(Intent(this@MainActivity, HandTrainingActivity::class.java))

                "지폐 인식" ->
                    startActivity(Intent(this@MainActivity, BillRecognitionActivity::class.java))

                "위치 공유" ->
                    startActivity(Intent(this@MainActivity, LocationSharingWithCodeActivity::class.java)) // ✅ 추가
            }
            return true
        }
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

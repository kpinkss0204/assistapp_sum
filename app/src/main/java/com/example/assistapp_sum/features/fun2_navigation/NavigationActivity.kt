package com.example.assistapp_sum.features.fun2_navigation

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.assistapp_sum.R
import java.util.*

class NavigationActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var gestureDetector: GestureDetector
    private var tapCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // 간단히 재활용
        tts = TextToSpeech(this, this)
        gestureDetector = GestureDetector(this, GestureListener())

        speak("도보 네비게이션 기능입니다. 두 번 탭하면 경로 안내를 시작합니다.")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.KOREAN
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            speak("현재 위치에서 목적지까지 경로를 안내합니다.")
            return true
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            tapCount++
            val handler = android.os.Handler(mainLooper)
            handler.postDelayed({
                if (tapCount == 3) {
                    speak("메인화면으로 돌아갑니다.")
                    finish()
                }
                tapCount = 0
            }, 1000)
        }
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun speak(msg: String) {
        tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}

package com.example.assistapp_sum.features.fun4_hand

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.assistapp_sum.R
import com.example.assistapp_sum.core.TTSManager
import kotlin.math.abs

class LevelSelectActivity : AppCompatActivity() {
    private lateinit var tv: TextView
    private lateinit var detector: GestureDetector
    private var level = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TTSManager.ensureInit(this)
        setContentView(R.layout.activity_level_select)

        tv = findViewById(R.id.tvLevel)
        speakAndShow()

        detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val TH = 100; private val VT = 100
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                if (abs(dx) > TH && abs(vx) > VT) {
                    if (dx > 0) level = if (level == 1) 4 else level - 1
                    else level = if (level == 4) 1 else level + 1
                    speakAndShow()
                    return true
                }
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val prompt = RandomLetterManager(level).next()
                TTSManager.speak(this@LevelSelectActivity,
                    "손감각 훈련 ${level}단계를 시작합니다. 제시어는 $prompt, 박스찾기 모드입니다.")
                startActivity(Intent(this@LevelSelectActivity, HandTrainingActivity::class.java).apply {
                    putExtra("level", level)
                    putExtra("prompt", prompt)
                })
                return true
            }
        })
    }

    private fun speakAndShow() {
        tv.text = "현재 단계: ${level}단계\n오른쪽/왼쪽 슬라이드로 변경, 더블탭으로 시작"
        TTSManager.speak(this, "현재 ${level}단계. 슬라이드로 변경, 더블탭으로 시작합니다.")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        detector.onTouchEvent(event)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

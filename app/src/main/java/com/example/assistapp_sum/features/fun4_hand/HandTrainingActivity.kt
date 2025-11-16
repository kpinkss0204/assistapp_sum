package com.example.assistapp_sum.features.fun4_hand

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.assistapp_sum.R
import com.example.assistapp_sum.core.TTSManager
import com.google.mlkit.vision.digitalink.*

class HandTrainingActivity : AppCompatActivity() {

    private lateinit var drawView: DrawView
    private lateinit var tvMode: TextView
    private lateinit var tvPrompt: TextView

    private lateinit var randomManager: RandomLetterManager

    private var currentMode = false
    private var prompt = ""
    private var isResultShowing = false

    private var tripleTapCount = 0
    private var lastTapTime = 0L

    private val doubleTapThreshold = 350L

    private var recognizer: DigitalInkRecognizer? = null
    private var modelReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hand_training)

        // 레이아웃 연결
        drawView = findViewById(R.id.drawView)
        tvMode = findViewById(R.id.tvMode)
        tvPrompt = findViewById(R.id.tvPrompt)

        val level = intent.getIntExtra("level", 1)
        randomManager = RandomLetterManager(level)

        // 제시어 표시
        prompt = randomManager.next()
        tvPrompt.text = "제시어: $prompt"
        updateModeText()

        // recognizer 초기화해도 OK / 실패해도 crash 안 나도록 처리
        initRecognizerSafe()

        // DrawView 기본 콜백 안전 처리
        drawView.onBoxTouched = { /* 아무 동작 안함 */ }
        drawView.onRecognitionFinished = {
            // 인식을 아예 수행하지 않아도 crash 방지
            isResultShowing = true
            tvPrompt.text = "인식 결과 창입니다.\n세 번 탭하면 메인으로 돌아갑니다."
            TTSManager.speak(this, "세 번 탭하면 메인으로 돌아갑니다.")
        }

        // 터치 핸들러 — 동작은 비활성화 / 안전하게만 처리
        drawView.setOnTouchListener { _, event ->
            if (event.action != MotionEvent.ACTION_DOWN) return@setOnTouchListener false

            if (isResultShowing) {
                tripleTapCount++
                if (tripleTapCount == 3) {
                    TTSManager.speak(this, "메인화면으로 돌아갑니다.")
                    finish()
                }
                return@setOnTouchListener true
            }

            // 글씨쓰기 모드였던 부분은 동작 무시하되 crash X
            lastTapTime = System.currentTimeMillis()
            return@setOnTouchListener true
        }

        TTSManager.speak(
            this,
            "손감각 훈련 ${level}단계를 시작합니다. 제시어는 $prompt 입니다."
        )
    }

    // 🔒 안전한 recognizer 초기화 (동작 안해도 crash 없음)
    private fun initRecognizerSafe() {
        try {
            val modelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag("ko")
            if (modelId == null) {
                return
            }

            val model = DigitalInkRecognitionModel.builder(modelId).build()
            val options = DigitalInkRecognizerOptions.builder(model).build()

            // recognizer 생성만 하고, 실제 사용은 안함
            recognizer = DigitalInkRecognition.getClient(options)
            drawView.setRecognizer(recognizer!!)
            modelReady = true
        } catch (e: Exception) {
            // crash 방지
        }
    }

    private fun toggleMode() {
        currentMode = !currentMode
        drawView.isWritingMode = currentMode
        updateModeText()
        val modeName = if (currentMode) "글씨쓰기 모드" else "박스찾기 모드"
        TTSManager.speak(this, modeName)
    }

    private fun updateModeText() {
        tvMode.text = if (currentMode) "✍️ 글씨쓰기 모드" else "🔲 박스찾기 모드"
    }

    override fun onDestroy() {
        TTSManager.shutdown()
        super.onDestroy()
    }
}

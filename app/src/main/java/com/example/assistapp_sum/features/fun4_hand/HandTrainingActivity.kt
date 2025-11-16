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

    private var currentMode = false // false=박스찾기, true=글씨쓰기
    private var prompt = ""
    private var isResultShowing = false
    private var tripleTapCount = 0
    private var lastTapTime = 0L
    private val doubleTapThreshold = 350L

    private var recognizer: DigitalInkRecognizer? = null
    private var modelReady = false
    private var initAttempt = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hand_training)

        drawView = findViewById(R.id.drawView)
        tvMode = findViewById(R.id.tvMode)
        tvPrompt = findViewById(R.id.tvPrompt)

        val level = intent.getIntExtra("level", 1)
        randomManager = RandomLetterManager(level)

        // ✅ 제시어 랜덤 표시
        prompt = randomManager.next()
        tvPrompt.text = "제시어: $prompt"
        updateModeText()

        initRecognizer() // ✳️ 모델 초기화 (자동 재시도 내장)

        drawView.onBoxTouched = { }

        drawView.onRecognitionFinished = { result ->
            isResultShowing = true
            tripleTapCount = 0

            val msg = if (result == prompt) "제시어와 일치합니다." else "제시어와 다릅니다."
            val speechText = "인식된 글자는 ${result}입니다. ${msg} 세 번 탭하면 메인으로 돌아갑니다."
            TTSManager.speak(this, speechText)
            tvPrompt.text = "인식 결과: $result\n($msg)\n\n세 번 탭하면 메인으로 돌아갑니다"
        }

        drawView.setOnTouchListener(object : View.OnTouchListener {
            private var tapCount = 0
            private var singleTapHandler: Runnable? = null

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                if (event.action != MotionEvent.ACTION_DOWN) return false
                val currentTime = System.currentTimeMillis()

                // ✅ 결과창일 때 → 세 번 탭하면 복귀
                if (isResultShowing) {
                    tripleTapCount++
                    if (tripleTapCount == 3) {
                        TTSManager.speak(this@HandTrainingActivity, "메인화면으로 돌아갑니다.")
                        finish()
                    }
                    return true
                }

                // ✅ 글씨쓰기 모드에서는 DrawView가 직접 처리
                if (drawView.isWritingMode && drawView.boxContains(event.x, event.y)) {
                    drawView.onTouchEvent(event)
                    return true
                }

                // ✅ 박스 밖 탭 로직
                if (!drawView.boxContains(event.x, event.y)) {
                    val delta = currentTime - lastTapTime

                    if (delta < doubleTapThreshold) {
                        // 두 번 탭 → 인식
                        if (currentMode && modelReady) {
                            TTSManager.speak(this@HandTrainingActivity, "인식을 시작합니다.")
                            drawView.startRecognition()
                        } else if (!modelReady) {
                            TTSManager.speak(this@HandTrainingActivity, "모델을 준비 중입니다. 잠시만 기다려주세요.")
                        } else {
                            TTSManager.speak(this@HandTrainingActivity, "지금은 박스찾기 모드입니다.")
                        }
                        tapCount = 0
                    } else {
                        // 한 번 탭 → 모드 전환
                        tapCount = 1
                        singleTapHandler?.let { drawView.removeCallbacks(it) }
                        singleTapHandler = Runnable {
                            if (tapCount == 1) toggleMode()
                            tapCount = 0
                        }
                        drawView.postDelayed(singleTapHandler, doubleTapThreshold)
                    }

                    lastTapTime = currentTime
                    return true
                }

                return false
            }
        })

        TTSManager.speak(
            this,
            "손감각 훈련 ${level}단계 훈련을 시작합니다. 제시어는 $prompt 입니다. 박스찾기 모드입니다."
        )
    }

    // ✅ 박스찾기 <-> 글씨쓰기 전환
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

    // ✅ Firebase 없이, ModelManager 없이 자동 재시도 포함
    private fun initRecognizer() {
        TTSManager.speak(this, "필기 인식 모델을 준비 중입니다.")

        try {
            val modelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag("ko")
            if (modelId == null) {
                TTSManager.speak(this, "한국어 모델을 찾을 수 없습니다.")
                return
            }
            val model = DigitalInkRecognitionModel.builder(modelId).build()
            val manager = RemoteModelManager.getInstance()
            val conditions = DownloadConditions.Builder()
                // .requireWifi()  // 필요하면 Wi-Fi 강제
                .build()

            // 1) 모델 다운로드 여부 확인
            manager.isModelDownloaded(model)
                .addOnSuccessListener { downloaded ->
                    if (downloaded == true) {
                        // 2-A) 이미 다운로드됨 → 바로 recognizer 생성
                        val options = DigitalInkRecognizerOptions.builder(model).build()
                        recognizer = DigitalInkRecognition.getClient(options)
                        drawView.setRecognizer(recognizer!!)
                        modelReady = true
                        TTSManager.speak(this, "필기 인식 준비 완료. 훈련을 시작할 수 있습니다.")
                    } else {
                        // 2-B) 다운로드 필요 → 다운로드 후 recognizer 생성
                        manager.download(model, conditions)
                            .addOnSuccessListener {
                                val options = DigitalInkRecognizerOptions.builder(model).build()
                                recognizer = DigitalInkRecognition.getClient(options)
                                drawView.setRecognizer(recognizer!!)
                                modelReady = true
                                TTSManager.speak(this, "필기 인식 모델 다운로드 완료. 훈련을 시작할 수 있습니다.")
                            }
                            .addOnFailureListener { e ->
                                TTSManager.speak(this, "모델 다운로드 실패: ${e.message ?: "알 수 없는 오류"}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    TTSManager.speak(this, "모델 상태 확인 실패: ${e.message ?: "알 수 없는 오류"}")
                }
        } catch (e: Exception) {
            TTSManager.speak(this, "모델 초기화 오류: ${e.message ?: "알 수 없는 오류"}")
        }
    }

    override fun onDestroy() {
        TTSManager.shutdown()
        super.onDestroy()
    }
}

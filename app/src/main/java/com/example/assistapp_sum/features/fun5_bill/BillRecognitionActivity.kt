package com.example.assistapp_sum.features.fun5_bill

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.assistapp_sum.R
import java.util.Locale
import java.util.concurrent.Executors

class BillRecognitionActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var previewView: PreviewView
    private lateinit var tts: TextToSpeech
    private lateinit var gestureDetector: GestureDetector
    private var cameraProvider: ProcessCameraProvider? = null
    private val executor = Executors.newSingleThreadExecutor()

    private var isCameraActive = false
    private var tripleTapCount = 0
    private var lastTapTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewView = PreviewView(this)
        previewView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        setContentView(previewView)

        tts = TextToSpeech(this, this)
        gestureDetector = GestureDetector(this, GestureListener())

        speak("지폐 인식 기능이 활성화되었습니다. 두 번 탭하면 카메라가 켜집니다.")
    }

    override fun onResume() {
        super.onResume()
        if (isCameraActive) startCamera()
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (!isCameraActive) {
                requestCameraPermission()
            }
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return true
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        handleTripleTap(event)
        return true
    }

    // ✅ 3번 탭 감지 → 메인 복귀
    private fun handleTripleTap(e: MotionEvent) {
        if (e.action != MotionEvent.ACTION_DOWN) return
        val now = SystemClock.uptimeMillis()
        if (now - lastTapTime < 600) tripleTapCount++ else tripleTapCount = 1
        lastTapTime = now
        if (tripleTapCount >= 3) {
            speak("메인화면으로 돌아갑니다.")
            stopCamera()
            finish()
        }
    }

    // ✅ 카메라 권한 요청
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            speak("카메라 권한이 필요합니다.")
        }
    }

    // ✅ 카메라 시작
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
                isCameraActive = true
                speak("카메라가 활성화되었습니다. 지폐를 화면 중앙에 비춰주세요.")
            } catch (e: Exception) {
                Log.e("BillRecognition", "카메라 초기화 오류: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analyzer = ImageAnalysis.Builder().build().also {
            it.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
                processImage(image)
            })
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        provider.bindToLifecycle(this, cameraSelector, preview, analyzer)
    }

    // ✅ 카메라 중지
    private fun stopCamera() {
        cameraProvider?.unbindAll()
        isCameraActive = false
    }

    // ✅ 진동 (API 24~35 호환)
    private fun vibrateFeedback() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    300,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    // ✅ 이미지 처리 (지폐 인식)
    private fun processImage(image: ImageProxy) {
        try {
            // TODO: 여기에 YOLO 모델 인식 붙이기 (best.onnx 로드)
            // 지금은 예시로 더미 인식
            val recognized = false

            if (recognized) {
                vibrateFeedback()
                speak("지폐가 인식되었습니다. 만원입니다.")
            }
        } catch (e: Exception) {
            Log.e("BillRecognition", "분석 오류: ${e.message}")
        } finally {
            image.close()
        }
    }

    private fun speak(text: String) {
        if (::tts.isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt-${System.currentTimeMillis()}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.KOREAN
    }

    override fun onDestroy() {
        stopCamera()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}

package com.example.assistapp_sum.features.fun3_barcode

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.assistapp_sum.MainActivity
import com.example.assistapp_sum.R
import com.example.assistapp_sum.core.GestureTapCounter
import com.example.assistapp_sum.core.TTSManager
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
class BarcodeRecognitionActivity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tv: TextView
    private val taps = GestureTapCounter(800)
    private val repo = BarcodeRepository()

    private var isDetecting = false
    private var cameraStarted = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var isResultShown = false   // ✅ 결과 화면이 떠 있는지 여부

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_recognition)

        // ✅ TTS 초기화
        TTSManager.ensureInit(this)

        tv = findViewById(R.id.resultText)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 첫 음성 안내
        TTSManager.speak(
            this,
            "상품 인식 기능이 실행되었습니다. 두 번 탭하면 카메라가 켜집니다. 세 번 탭하면 메인으로 돌아갑니다."
        )

        gestureDetector = GestureDetector(this, GestureListener())
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // ✅ 결과가 이미 표시되었다면 더블탭 동작 무시
            if (isResultShown) return true

            if (!cameraStarted) {
                if (ContextCompat.checkSelfPermission(
                        this@BarcodeRecognitionActivity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startCamera()
                    TTSManager.speak(this@BarcodeRecognitionActivity, "카메라가 켜졌습니다. 바코드를 비춰주세요.")
                    cameraStarted = true
                } else {
                    ActivityCompat.requestPermissions(
                        this@BarcodeRecognitionActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        1001
                    )
                }
            }
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val c = taps.onTap()
            if (c >= 3) {
                TTSManager.speak(this@BarcodeRecognitionActivity, "메인화면으로 돌아갑니다.")
                startActivity(Intent(this@BarcodeRecognitionActivity, MainActivity::class.java))
                finish()
            }
            return true
        }
    }

    private fun beepAndVibrate() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        } catch (_: Exception) { }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.previewView).surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            cameraStarted = false
        } catch (_: Exception) {}
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty() && !isDetecting) {
                    isDetecting = true
                    val barcodeValue = barcodes.first().rawValue ?: ""
                    beepAndVibrate()
                    runOnUiThread {
                        tv.text = "바코드 인식됨: $barcodeValue\n상품 정보를 불러오는 중..."
                    }

                    // ✅ 인식 성공 → 카메라 즉시 종료
                    stopCamera()

                    // ✅ API 요청
                    repo.fetchProduct(barcodeValue,
                        onSuccess = { res ->
                            val item = res?.rows?.firstOrNull()
                            if (item != null) {
                                val leftDays = item.expiration?.toIntOrNull() ?: 0
                                val msg = "상품명 ${item.productName}, 제조사 ${item.manufacturer}, 유통기한 ${leftDays}일입니다."
                                runOnUiThread {
                                    tv.text = msg
                                    TTSManager.speak(this, msg)
                                    isResultShown = true // ✅ 결과 화면 표시 상태로 전환
                                }
                            } else {
                                runOnUiThread {
                                    tv.text = "등록되지 않은 상품입니다."
                                    TTSManager.speak(this, "등록되지 않은 상품입니다.")
                                    isResultShown = true
                                }
                            }
                            imageProxy.close()
                            isDetecting = false
                        },
                        onError = {
                            runOnUiThread {
                                tv.text = "상품 정보를 불러올 수 없습니다."
                                TTSManager.speak(this, "상품 정보를 불러올 수 없습니다.")
                                isResultShown = true
                            }
                            imageProxy.close()
                            isDetecting = false
                        }
                    )
                } else imageProxy.close()
            }
            .addOnFailureListener { imageProxy.close() }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        TTSManager.stop()
        super.onDestroy()
    }
}

package com.example.assistapp_sum.features.fun4_hand

import android.content.Context
import android.graphics.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.mlkit.vision.digitalink.*

class DrawView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    var onBoxTouched: (() -> Unit)? = null
    var onRecognitionFinished: ((String) -> Unit)? = null
    var isWritingMode: Boolean = false

    private val boxPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val pathPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 12f
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var boxRect = RectF(160f, 280f, 920f, 1040f)
    private var currentPath = Path()
    private val paths = mutableListOf<Path>()
    private var inkBuilder = Ink.builder()
    private var strokeBuilder = Ink.Stroke.builder()
    private var recognizer: DigitalInkRecognizer? = null
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    private var lastBoxTouchTime = 0L
    private val beepCooldown = 500L

    fun setRecognizer(recognizer: DigitalInkRecognizer) {
        this.recognizer = recognizer
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        canvas.drawRect(boxRect, boxPaint)
        for (p in paths) canvas.drawPath(p, pathPaint)
        canvas.drawPath(currentPath, pathPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val insideBox = boxRect.contains(x, y)

        // 🟩 박스찾기 모드
        if (!isWritingMode) {
            if (event.action == MotionEvent.ACTION_MOVE && insideBox) {
                val now = System.currentTimeMillis()
                if (now - lastBoxTouchTime > beepCooldown) {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                    onBoxTouched?.invoke()
                    lastBoxTouchTime = now
                }
            }
            return true
        }

        // ✍️ 글씨쓰기 모드
        if (!insideBox) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.reset()
                currentPath.moveTo(x, y)
                strokeBuilder = Ink.Stroke.builder()
                strokeBuilder.addPoint(Ink.Point.create(x, y, System.currentTimeMillis()))
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y)
                strokeBuilder.addPoint(Ink.Point.create(x, y, System.currentTimeMillis()))
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(x, y)
                strokeBuilder.addPoint(Ink.Point.create(x, y, System.currentTimeMillis()))
                inkBuilder.addStroke(strokeBuilder.build())
                paths.add(Path(currentPath))
                currentPath = Path()
                invalidate()
            }
        }
        return true
    }

    fun startRecognition() {
        val ink = inkBuilder.build()
        if (ink.strokes.isEmpty()) {
            onRecognitionFinished?.invoke("글씨가 없습니다")
            return
        }

        recognizer?.recognize(ink)
            ?.addOnSuccessListener { result ->
                val text = result.candidates.firstOrNull()?.text ?: "인식 실패"
                onRecognitionFinished?.invoke(text)
            }
            ?.addOnFailureListener { e ->
                onRecognitionFinished?.invoke("인식 오류: ${e.message}")
            }
    }

    fun clearCanvas() {
        paths.clear()
        currentPath.reset()
        inkBuilder = Ink.builder()
        invalidate()
    }

    fun boxContains(x: Float, y: Float): Boolean = boxRect.contains(x, y)
}

package com.example.assistapp_sum.features.fun5_bill

import ai.onnxruntime.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

data class Detection(
    val className: String,
    val confidence: Float,
    val box: RectF
)

class Yolov8Onnx(
    context: Context,
    modelAssetName: String = "best.onnx",
    private val confThreshold: Float = 0.25f,
    private val iouThreshold: Float = 0.45f
) {
    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    private val inputW = 640
    private val inputH = 640
    private var srcW = 0
    private var srcH = 0

    // 클래스 라벨: 예시
    private val labels = listOf("1000원", "5000원", "10000원", "50000원")

    init {
        val bytes = context.assets.open(modelAssetName).readBytes()
        session = env.createSession(bytes)
    }

    private fun preprocess(bitmap: Bitmap): OnnxTensor {
        srcW = bitmap.width; srcH = bitmap.height
        val resized = Bitmap.createScaledBitmap(bitmap, inputW, inputH, true)
        val data = Array(1) { Array(3) { Array(inputH) { FloatArray(inputW) } } }
        for (y in 0 until inputH) for (x in 0 until inputW) {
            val p = resized.getPixel(x, y)
            data[0][0][y][x] = ((p shr 16) and 0xFF) / 255f
            data[0][1][y][x] = ((p shr 8) and 0xFF) / 255f
            data[0][2][y][x] = (p and 0xFF) / 255f
        }
        return OnnxTensor.createTensor(env, data)
    }

    private fun xywh2xyxy(b: FloatArray): RectF {
        val x1 = b[0] - b[2] / 2f
        val y1 = b[1] - b[3] / 2f
        val x2 = b[0] + b[2] / 2f
        val y2 = b[1] + b[3] / 2f
        return RectF(x1, y1, x2, y2)
    }

    private fun scale(r: RectF): RectF {
        val sx = srcW / inputW.toFloat()
        val sy = srcH / inputH.toFloat()
        return RectF(
            max(0f, r.left * sx), max(0f, r.top * sy),
            min(srcW.toFloat(), r.right * sx), min(srcH.toFloat(), r.bottom * sy)
        )
    }

    private fun iou(a: RectF, b: RectF): Float {
        val l = max(a.left, b.left)
        val t = max(a.top, b.top)
        val r = min(a.right, b.right)
        val bt = min(a.bottom, b.bottom)
        val inter = max(0f, r - l) * max(0f, bt - t)
        val uni = a.width() * a.height() + b.width() * b.height() - inter
        return if (uni <= 0) 0f else inter / uni
    }

    private fun nms(boxes: List<RectF>, scores: List<Float>): List<Int> {
        val out = mutableListOf<Int>()
        val order = scores.indices.sortedByDescending { scores[it] }
        for (i in order) {
            var keep = true
            for (j in out) if (iou(boxes[i], boxes[j]) > iouThreshold) { keep = false; break }
            if (keep) out += i
        }
        return out
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val input = preprocess(bitmap)
        val inName = session.inputNames.first()
        val res = session.run(mapOf(inName to input))
        @Suppress("UNCHECKED_CAST")
        val out = res[0].value as Array<Array<FloatArray>> // [1,85,N] -> [85,N]
        val p = out[0]

        val boxes = mutableListOf<RectF>()
        val scores = mutableListOf<Float>()
        val clsIds = mutableListOf<Int>()

        val n = p[0].size
        for (i in 0 until n) {
            val x = p[0][i]; val y = p[1][i]; val w = p[2][i]; val h = p[3][i]
            val cls = FloatArray(labels.size) { c -> p[4 + c][i] }
            var best = 0; var sc = cls[0]
            for (c in 1 until labels.size) if (cls[c] > sc) { best = c; sc = cls[c] }
            if (sc > confThreshold) {
                boxes += scale(xywh2xyxy(floatArrayOf(x, y, w, h)))
                scores += sc
                clsIds += best
            }
        }

        val keep = nms(boxes, scores)
        return keep.map { i -> Detection(labels[clsIds[i]], scores[i], boxes[i]) }
    }

    fun close() { session.close(); env.close() }
}

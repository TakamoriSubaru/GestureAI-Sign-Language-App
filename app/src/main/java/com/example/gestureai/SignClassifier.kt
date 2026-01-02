package com.example.gestureai

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.scale
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SignClassifier(context: Context) {

    companion object {
        private const val INPUT_SIZE = 224
        private const val CHANNELS = 3
    }

    private val interpreter: Interpreter
    private val labels: List<String>

    init {
        interpreter = Interpreter(
            FileUtil.loadMappedFile(context, "model.tflite")
        )
        labels = FileUtil.loadLabels(context, "labels.txt")
    }

    fun classify(bitmap: Bitmap): String {
        val resized = bitmap.scale(INPUT_SIZE, INPUT_SIZE)
        val inputBuffer = bitmapToBuffer(resized)

        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(inputBuffer, output)

        val index = output[0]
            .indices
            .maxByOrNull { output[0][it] } ?: 0

        return labels.getOrElse(index) { "Unknown" }
    }

    private fun bitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(
            4 * INPUT_SIZE * INPUT_SIZE * CHANNELS
        ).apply {
            order(ByteOrder.nativeOrder())
        }

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(
            pixels,
            0,
            INPUT_SIZE,
            0,
            0,
            INPUT_SIZE,
            INPUT_SIZE
        )

        var i = 0
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val p = pixels[i++]
                buffer.putFloat(((p shr 16) and 0xFF) / 255f)
                buffer.putFloat(((p shr 8) and 0xFF) / 255f)
                buffer.putFloat((p and 0xFF) / 255f)
            }
        }

        buffer.rewind()
        return buffer
    }
}

package com.example.assistapp_sum.core

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TTSManager {
    private var tts: TextToSpeech? = null
    private var ready = false

    fun ensureInit(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = (status == TextToSpeech.SUCCESS)
            if (ready) tts?.language = Locale.KOREAN
        }
    }

    fun speak(context: Context, text: String, flush: Boolean = true) {
        ensureInit(context)
        if (!ready) return
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, mode, null, "utt-${System.currentTimeMillis()}")
    }

    fun stop() { tts?.stop() }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}

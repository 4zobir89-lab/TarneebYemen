package com.example.game

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

class SoundManager(context: Context) {
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
    private var enabled = true

    fun setEnabled(on: Boolean) { enabled = on }

    fun playCardDeal() {
        if (!enabled) return
        toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 60)
    }

    fun playCardSlap() {
        if (!enabled) return
        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 80)
    }

    fun playTrumpReveal() {
        if (!enabled) return
        toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
    }

    fun playWin() {
        if (!enabled) return
        toneGen.startTone(ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE, 400)
    }

    fun playLose() {
        if (!enabled) return
        toneGen.startTone(ToneGenerator.TONE_CDMA_SIGNAL_OFF, 400)
    }

    fun playBid() {
        if (!enabled) return
        toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
    }

    fun release() {
        toneGen.release()
    }
}

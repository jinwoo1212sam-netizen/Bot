package com.ultron.assistant.actions

import android.content.Context
import android.media.AudioManager

class VolumeAction(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setVolumePercent(percent: Int, streamType: Int = AudioManager.STREAM_MUSIC): ActionResult {
        val clampedPercent = percent.coerceIn(0, 100)
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val targetIndex = (clampedPercent * maxVolume / 100f).toInt().coerceIn(0, maxVolume)

        return try {
            audioManager.setStreamVolume(streamType, targetIndex, AudioManager.FLAG_SHOW_UI)
            ActionResult.Success("Boss, media volume $clampedPercent% par set kar diya.")
        } catch (e: Exception) {
            ActionResult.Failure("Volume set nahi ho saka: ${e.message}")
        }
    }

    fun increaseVolume(step: Int = 1, streamType: Int = AudioManager.STREAM_MUSIC): ActionResult {
        return try {
            for (i in 0 until step) {
                audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            }
            val current = getCurrentVolumePercent(streamType)
            ActionResult.Success("Boss, volume badha diya ($current%).")
        } catch (e: Exception) {
            ActionResult.Failure("Volume adjust nahi ho paya.")
        }
    }

    fun decreaseVolume(step: Int = 1, streamType: Int = AudioManager.STREAM_MUSIC): ActionResult {
        return try {
            for (i in 0 until step) {
                audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            }
            val current = getCurrentVolumePercent(streamType)
            ActionResult.Success("Boss, volume kam kar diya ($current%).")
        } catch (e: Exception) {
            ActionResult.Failure("Volume adjust nahi ho paya.")
        }
    }

    fun setFullVolume(streamType: Int = AudioManager.STREAM_MUSIC): ActionResult {
        return setVolumePercent(100, streamType)
    }

    fun muteVolume(streamType: Int = AudioManager.STREAM_MUSIC): ActionResult {
        return setVolumePercent(0, streamType)
    }

    private fun getCurrentVolumePercent(streamType: Int = AudioManager.STREAM_MUSIC): Int {
        val max = audioManager.getStreamMaxVolume(streamType)
        val current = audioManager.getStreamVolume(streamType)
        return if (max > 0) (current * 100 / max) else 0
    }
}

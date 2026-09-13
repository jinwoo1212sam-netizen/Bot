package com.ultron.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import com.ultron.assistant.service.UltronForegroundService

class WakeWordManager(private val context: Context) {

    fun startWakeWordListening() {
        val intent = Intent(context, UltronForegroundService::class.java).apply {
            action = UltronForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopWakeWordListening() {
        val intent = Intent(context, UltronForegroundService::class.java).apply {
            action = UltronForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun matchesWakeWord(phrase: String, targetWord: String = "ultron"): Boolean {
        val clean = phrase.lowercase().trim()
        return clean.startsWith(targetWord.lowercase()) || clean.contains(targetWord.lowercase())
    }
}

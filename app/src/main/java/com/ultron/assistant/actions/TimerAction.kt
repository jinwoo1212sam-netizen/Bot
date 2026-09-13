package com.ultron.assistant.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

class TimerAction(private val context: Context) {

    fun setTimer(durationSeconds: Int, message: String = "Ultron Timer"): ActionResult {
        if (durationSeconds <= 0) {
            return ActionResult.Failure("Boss, timer ka duration sahi nahi hai.")
        }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            val timeText = if (minutes > 0) "$minutes minute ${if (seconds > 0) "$seconds second" else ""}" else "$seconds second"
            ActionResult.Success("Boss, $timeText ka timer set kar diya.")
        } catch (e: Exception) {
            ActionResult.Failure("Timer set karne mein dikkat aayi: ${e.message}")
        }
    }
}

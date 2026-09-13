package com.ultron.assistant.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

class AlarmAction(private val context: Context) {

    fun setAlarm(hour: Int, minute: Int, message: String = "Ultron Alarm"): ActionResult {
        if (hour !in 0..23 || minute !in 0..59) {
            return ActionResult.Failure("Boss, alarm ka time invalid hai.")
        }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val formattedTime = String.format("%02d:%02d", hour, minute)
            ActionResult.Success("Boss, subah/shaam $formattedTime baje ka alarm set kar diya.")
        } catch (e: Exception) {
            ActionResult.Failure("Alarm set karne mein problem aayi: ${e.message}")
        }
    }
}

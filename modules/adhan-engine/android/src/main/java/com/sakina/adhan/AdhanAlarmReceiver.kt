package com.sakina.adhan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Fired by AlarmManager at a prayer time; hands off to the playback service. */
class AdhanAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val service = Intent(context, AdhanPlaybackService::class.java)
      .setAction(AdhanPlaybackService.ACTION_PLAY)
      .putExtras(intent)
    // Exact alarms are allowed to start a foreground service from the background.
    ContextCompat.startForegroundService(context, service)

    // Drop the fired event from the stored schedule.
    val id = intent.getIntExtra(AdhanScheduler.EXTRA_ID, -1)
    val store = AdhanStore(context)
    store.events = store.events.filter { it.id != id }
  }
}

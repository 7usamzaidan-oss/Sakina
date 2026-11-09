package com.sakina.adhan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Owns every AlarmManager call. The rest of the app only hands it a full list of events. */
object AdhanScheduler {
  const val EXTRA_ID = "id"
  const val EXTRA_TITLE = "title"
  const val EXTRA_BODY = "body"

  fun canScheduleExact(context: Context): Boolean {
    val am = context.getSystemService(AlarmManager::class.java)
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
  }

  /** Cancels everything previously scheduled, then schedules the future events in [events]. */
  fun replaceAll(context: Context, events: List<AdhanEvent>): Int {
    val store = AdhanStore(context)
    cancel(context, store.events)
    val now = System.currentTimeMillis()
    val future = events.filter { it.triggerAtMillis > now }.sortedBy { it.triggerAtMillis }
    store.events = future
    future.forEach { schedule(context, it) }
    return future.size
  }

  /** Re-arms whatever is stored (after reboot, time change, app update). */
  fun restore(context: Context): Int = replaceAll(context, AdhanStore(context).events)

  private fun schedule(context: Context, event: AdhanEvent) {
    val am = context.getSystemService(AlarmManager::class.java)
    val fire = pendingIntent(context, event)
    if (canScheduleExact(context)) {
      // setAlarmClock is the most reliable exact alarm: exempt from Doze and app standby.
      val show = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
        PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
      }
      am.setAlarmClock(AlarmManager.AlarmClockInfo(event.triggerAtMillis, show), fire)
    } else {
      // Without the exact-alarm permission this may be delayed by a few minutes.
      am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.triggerAtMillis, fire)
    }
  }

  private fun cancel(context: Context, events: List<AdhanEvent>) {
    val am = context.getSystemService(AlarmManager::class.java)
    events.forEach { am.cancel(pendingIntent(context, it)) }
  }

  private fun pendingIntent(context: Context, event: AdhanEvent): PendingIntent {
    val intent = Intent(context, AdhanAlarmReceiver::class.java)
      .setAction("com.sakina.adhan.FIRE.${event.id}")
      .putExtra(EXTRA_ID, event.id)
      .putExtra(EXTRA_TITLE, event.title)
      .putExtra(EXTRA_BODY, event.body)
    return PendingIntent.getBroadcast(
      context, event.id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
  }
}

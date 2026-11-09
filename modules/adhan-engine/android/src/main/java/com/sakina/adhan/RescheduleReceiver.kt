package com.sakina.adhan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms are wiped on reboot; this re-arms the stored schedule. */
class RescheduleReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    AdhanScheduler.restore(context)
  }
}

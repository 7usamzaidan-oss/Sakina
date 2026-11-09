package com.sakina.adhan

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

class AdhanEventRecord : Record {
  @Field var id: Int = 0
  @Field var triggerAtMillis: Double = 0.0
  @Field var title: String = ""
  @Field var body: String = ""
}

class AdhanEngineModule : Module() {
  private val context: Context
    get() = appContext.reactContext ?: throw Exceptions.ReactContextLost()

  override fun definition() = ModuleDefinition {
    Name("AdhanEngine")

    Function("replaceSchedule") { events: List<AdhanEventRecord> ->
      AdhanPlaybackService.ensureChannel(context)
      AdhanScheduler.replaceAll(context, events.map {
        AdhanEvent(it.id, it.triggerAtMillis.toLong(), it.title, it.body)
      })
    }

    Function("getSchedule") {
      AdhanStore(context).events.map {
        mapOf("id" to it.id, "triggerAtMillis" to it.triggerAtMillis.toDouble(), "title" to it.title, "body" to it.body)
      }
    }

    Function("setSoundUri") { uri: String? -> AdhanStore(context).soundUri = uri }
    Function("getSoundUri") { AdhanStore(context).soundUri }

    Function("playNow") { title: String ->
      val i = Intent(context, AdhanPlaybackService::class.java)
        .setAction(AdhanPlaybackService.ACTION_PLAY)
        .putExtra(AdhanScheduler.EXTRA_TITLE, title)
      androidx.core.content.ContextCompat.startForegroundService(context, i)
    }

    Function("stop") {
      context.startService(Intent(context, AdhanPlaybackService::class.java).setAction(AdhanPlaybackService.ACTION_STOP))
    }

    Function("getPermissionStatus") {
      val pm = context.getSystemService(PowerManager::class.java)
      val nm = context.getSystemService(NotificationManager::class.java)
      mapOf(
        "notifications" to NotificationManagerCompat.from(context).areNotificationsEnabled(),
        "exactAlarms" to AdhanScheduler.canScheduleExact(context),
        "ignoringBatteryOptimizations" to pm.isIgnoringBatteryOptimizations(context.packageName),
        "fullScreenIntent" to (Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()),
        "manufacturer" to Build.MANUFACTURER,
        "sdkInt" to Build.VERSION.SDK_INT
      )
    }

    Function("openSettings") { kind: String ->
      val pkg = Uri.parse("package:${context.packageName}")
      val intent = when (kind) {
        "notifications" -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
          .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        "exactAlarms" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
          Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg) else null
        "battery" -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
        "fullScreenIntent" -> if (Build.VERSION.SDK_INT >= 34)
          Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg) else null
        else -> null
      } ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      try {
        context.startActivity(intent)
      } catch (e: Exception) {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      }
    }
  }
}

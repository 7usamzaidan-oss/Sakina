package com.sakina.adhan

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.io.File

/**
 * Plays the full adhan as a mediaPlayback foreground service so it keeps running with the
 * screen locked. Stops on: the notification's Stop button, swiping the notification,
 * either volume key, or the audio finishing.
 */
class AdhanPlaybackService : Service() {
  private var player: MediaPlayer? = null
  private var session: MediaSession? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopAdhan()
        return START_NOT_STICKY
      }
      ACTION_PLAY -> {
        val title = intent.getStringExtra(AdhanScheduler.EXTRA_TITLE) ?: "حان وقت الصلاة"
        val body = intent.getStringExtra(AdhanScheduler.EXTRA_BODY) ?: ""
        goForeground(title, body)
        play()
        return START_NOT_STICKY
      }
    }
    stopSelf()
    return START_NOT_STICKY
  }

  private fun goForeground(title: String, body: String) {
    ensureChannel(this)
    val stop = PendingIntent.getService(
      this, 1, Intent(this, AdhanPlaybackService::class.java).setAction(ACTION_STOP),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
      PendingIntent.getActivity(this, 2, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(applicationInfo.icon)
      .setContentTitle(title)
      .setContentText(body)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setContentIntent(open)
      .setDeleteIntent(stop)
      .addAction(0, "إيقاف الأذان", stop)
      .build()
    ServiceCompat.startForeground(
      this, NOTIFICATION_ID, notification,
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
    )
  }

  private fun play() {
    releasePlayer()
    val attrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_ALARM)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
    val mp = MediaPlayer()
    try {
      mp.setAudioAttributes(attrs)
      mp.setDataSource(this, resolveSound())
      mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
      mp.setOnCompletionListener { stopAdhan() }
      mp.setOnErrorListener { _, what, extra ->
        Log.e(TAG, "MediaPlayer error $what/$extra"); stopAdhan(); true
      }
      mp.prepare()
      mp.start()
      player = mp
      startVolumeKeySession()
    } catch (e: Exception) {
      Log.e(TAG, "Could not play adhan", e)
      mp.release()
      stopAdhan()
    }
  }

  /** The user's chosen adhan file if it still exists, otherwise the system alarm sound. */
  private fun resolveSound(): Uri {
    val chosen = AdhanStore(this).soundUri
    if (chosen != null) {
      val uri = Uri.parse(chosen)
      if (uri.scheme != "file" || File(uri.path ?: "").exists()) return uri
    }
    return RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
  }

  /**
   * An active media session with remote volume handling receives the volume keys,
   * even on the lock screen, so either key stops the adhan.
   */
  private fun startVolumeKeySession() {
    val s = MediaSession(this, "SakinaAdhan")
    s.setPlaybackToRemote(object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 1, 1) {
      override fun onAdjustVolume(direction: Int) { stopAdhan() }
    })
    s.setPlaybackState(
      PlaybackState.Builder()
        .setState(PlaybackState.STATE_PLAYING, 0, 1f)
        .setActions(PlaybackState.ACTION_STOP)
        .build()
    )
    s.setCallback(object : MediaSession.Callback() {
      override fun onStop() { stopAdhan() }
      override fun onPause() { stopAdhan() }
    })
    s.isActive = true
    session = s
  }

  private fun stopAdhan() {
    releasePlayer()
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun releasePlayer() {
    player?.let { runCatching { if (it.isPlaying) it.stop() }; it.release() }
    player = null
    session?.let { it.isActive = false; it.release() }
    session = null
  }

  override fun onDestroy() {
    releasePlayer()
    super.onDestroy()
  }

  companion object {
    const val ACTION_PLAY = "com.sakina.adhan.PLAY"
    const val ACTION_STOP = "com.sakina.adhan.STOP"
    private const val CHANNEL_ID = "adhan_playback"
    private const val NOTIFICATION_ID = 7001
    private const val TAG = "SakinaAdhan"

    fun ensureChannel(context: Context) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
      val nm = context.getSystemService(NotificationManager::class.java)
      if (nm.getNotificationChannel(CHANNEL_ID) != null) return
      val ch = NotificationChannel(CHANNEL_ID, "الأذان", NotificationManager.IMPORTANCE_HIGH).apply {
        description = "تشغيل الأذان عند دخول وقت الصلاة"
        setSound(null, null) // the service plays the audio itself
        enableVibration(true)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        setBypassDnd(true)
      }
      nm.createNotificationChannel(ch)
    }
  }
}

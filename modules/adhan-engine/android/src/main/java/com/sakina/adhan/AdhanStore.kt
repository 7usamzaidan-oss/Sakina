package com.sakina.adhan

import android.content.Context

/**
 * Persists the schedule and the chosen sound so alarms can be restored after a reboot
 * without the JS side running. Uses device-protected storage so it is readable before unlock.
 */
class AdhanStore(context: Context) {
  private val prefs = context.createDeviceProtectedStorageContext()
    .getSharedPreferences("sakina_adhan", Context.MODE_PRIVATE)

  var events: List<AdhanEvent>
    get() = AdhanEvent.listFromJson(prefs.getString(KEY_EVENTS, null))
    set(value) = prefs.edit().putString(KEY_EVENTS, AdhanEvent.listToJson(value)).apply()

  /** file:// or content:// URI of the adhan audio, or null to use the system alarm sound. */
  var soundUri: String?
    get() = prefs.getString(KEY_SOUND, null)
    set(value) = prefs.edit().putString(KEY_SOUND, value).apply()

  companion object {
    private const val KEY_EVENTS = "events"
    private const val KEY_SOUND = "sound_uri"
  }
}

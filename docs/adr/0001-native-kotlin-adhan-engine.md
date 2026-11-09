# Adhan engine is a custom Kotlin Expo module, not Notifee

The previous version of this app failed because adhan notifications did not fire reliably. Full adhan playback, the Fajr alarm over the lock screen, rescheduling after reboot and stop-on-volume-down all need native Android code regardless, so we write one Expo native module in Kotlin that owns scheduling (`AlarmManager.setAlarmClock`), playback (a `mediaPlayback` foreground service with a Stop action), the `BOOT_COMPLETED` receiver and the full-screen-intent alarm. JS only computes prayer times and hands the module a list of upcoming events.

## Considered Options

- **Notifee**: designed for notifications, not long audio; the features above would still need native code, leaving two systems that both schedule alarms.
- **expo-notifications alone**: ruled out by the brief: notification sounds are capped and cannot be stopped or controlled during playback.

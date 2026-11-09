import { requireNativeModule } from 'expo-modules-core';

export type AdhanEvent = {
  /** Stable, unique per event; also the Android PendingIntent request code. */
  id: number;
  triggerAtMillis: number;
  title: string;
  body: string;
};

export type PermissionStatus = {
  notifications: boolean;
  exactAlarms: boolean;
  ignoringBatteryOptimizations: boolean;
  fullScreenIntent: boolean;
  manufacturer: string;
  sdkInt: number;
};

export type SettingsKind = 'notifications' | 'exactAlarms' | 'battery' | 'fullScreenIntent' | 'app';

type NativeAdhanEngine = {
  replaceSchedule(events: AdhanEvent[]): number;
  getSchedule(): AdhanEvent[];
  setSoundUri(uri: string | null): void;
  getSoundUri(): string | null;
  playNow(title: string): void;
  stop(): void;
  getPermissionStatus(): PermissionStatus;
  openSettings(kind: SettingsKind): void;
};

/**
 * Native adhan engine (see docs/adr/0001). JS computes prayer times and hands the
 * engine the complete list of upcoming events; the engine owns alarms, playback and
 * restoring the schedule after a reboot.
 */
export const AdhanEngine = requireNativeModule<NativeAdhanEngine>('AdhanEngine');

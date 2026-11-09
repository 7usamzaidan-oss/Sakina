import { CalculationMethod, Coordinates, Madhab, PrayerTimes } from 'adhan';

export type PrayerName = 'fajr' | 'dhuhr' | 'asr' | 'maghrib' | 'isha';

export const PRAYERS: PrayerName[] = ['fajr', 'dhuhr', 'asr', 'maghrib', 'isha'];

export const PRAYER_LABELS: Record<PrayerName | 'sunrise', string> = {
  fajr: 'الفجر',
  sunrise: 'الشروق',
  dhuhr: 'الظهر',
  asr: 'العصر',
  maghrib: 'المغرب',
  isha: 'العشاء',
};

/** Tripoli, Libya — the default location when GPS is unavailable. */
export const TRIPOLI = { name: 'طرابلس', latitude: 32.8872, longitude: 13.1913 };

export const DEFAULT_METHOD_LABEL = 'الهيئة المصرية العامة للمساحة (الفجر 19.5°، العشاء 17.5°)';

export type DayTimes = Record<PrayerName | 'sunrise', Date> & { date: Date };

/** Prayer times for one calendar day. Default: Egyptian method, standard (Shafi'i/Maliki) Asr. */
export function timesForDay(date: Date, latitude = TRIPOLI.latitude, longitude = TRIPOLI.longitude): DayTimes {
  const params = CalculationMethod.Egyptian();
  params.madhab = Madhab.Shafi;
  const t = new PrayerTimes(new Coordinates(latitude, longitude), date, params);
  return { date, fajr: t.fajr, sunrise: t.sunrise, dhuhr: t.dhuhr, asr: t.asr, maghrib: t.maghrib, isha: t.isha };
}

export function timesForDays(start: Date, days: number): DayTimes[] {
  return Array.from({ length: days }, (_, i) => {
    const d = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
    return timesForDay(d);
  });
}

/**
 * Stable event id: yyyymmdd * 10 + prayer index, kept within a 32-bit int by
 * using days since 2020-01-01 instead of the full date.
 */
export function eventId(day: Date, prayer: PrayerName): number {
  const epoch = Date.UTC(2020, 0, 1);
  const dayIndex = Math.floor((Date.UTC(day.getFullYear(), day.getMonth(), day.getDate()) - epoch) / 86_400_000);
  return dayIndex * 10 + PRAYERS.indexOf(prayer) + 1;
}

export function formatTime(d: Date): string {
  const h = d.getHours();
  const m = d.getMinutes();
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

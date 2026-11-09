import { AdhanEngine, AdhanEvent } from '../modules/adhan-engine';
import { PRAYER_LABELS, PRAYERS, TRIPOLI, eventId, formatTime, timesForDays } from './prayerTimes';

const DAYS_AHEAD = 7;
/** Ids below this are one-off test events and survive a schedule rebuild. */
const TEST_ID_LIMIT = 1000;

/** Rebuilds the next 7 days of adhan alarms. Safe to call on every app open. */
export function rescheduleWeek(now = new Date()): number {
  const prayerEvents: AdhanEvent[] = timesForDays(now, DAYS_AHEAD).flatMap((day) =>
    PRAYERS.map((p) => ({
      id: eventId(day.date, p),
      triggerAtMillis: day[p].getTime(),
      title: `حان الآن موعد أذان ${PRAYER_LABELS[p]}`,
      body: `${TRIPOLI.name} — ${formatTime(day[p])}`,
    })),
  );
  const pendingTests = AdhanEngine.getSchedule().filter(
    (e) => e.id < TEST_ID_LIMIT && e.triggerAtMillis > now.getTime(),
  );
  return AdhanEngine.replaceSchedule([...pendingTests, ...prayerEvents]);
}

/** Schedules a test adhan [minutes] from now, keeping the real schedule. */
export function scheduleTest(minutes = 2): Date {
  const at = new Date(Date.now() + minutes * 60_000);
  at.setSeconds(0, 0);
  if (at.getTime() <= Date.now() + 30_000) at.setMinutes(at.getMinutes() + 1);
  const others = AdhanEngine.getSchedule().filter((e) => e.id !== 1);
  AdhanEngine.replaceSchedule([
    { id: 1, triggerAtMillis: at.getTime(), title: 'اختبار الأذان', body: `أذان تجريبي — ${formatTime(at)}` },
    ...others,
  ]);
  return at;
}

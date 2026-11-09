import * as DocumentPicker from 'expo-document-picker';
import { Directory, File, Paths } from 'expo-file-system';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  AppState,
  PermissionsAndroid,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { AdhanEngine, AdhanEvent, PermissionStatus, SettingsKind } from './modules/adhan-engine';
import { rescheduleWeek, scheduleTest } from './services/adhanSchedule';
import {
  DEFAULT_METHOD_LABEL,
  PRAYER_LABELS,
  PRAYERS,
  TRIPOLI,
  formatTime,
  timesForDay,
} from './services/prayerTimes';

const C = {
  navy: '#061B2B',
  teal: '#063B43',
  emerald: '#12A878',
  gold: '#E8B85B',
  white: '#FFFFFF',
  gray: '#E8EDF0',
  danger: '#E5484D',
};

const PERMISSIONS: { key: keyof PermissionStatus; kind: SettingsKind; label: string; why: string }[] = [
  { key: 'notifications', kind: 'notifications', label: 'الإشعارات', why: 'لإظهار إشعار الأذان وزر الإيقاف.' },
  { key: 'exactAlarms', kind: 'exactAlarms', label: 'المنبهات الدقيقة', why: 'ليُرفع الأذان في الدقيقة نفسها دون تأخير.' },
  {
    key: 'ignoringBatteryOptimizations',
    kind: 'battery',
    label: 'استثناء من توفير البطارية',
    why: 'حتى لا يوقف النظام التطبيق في الخلفية.',
  },
];

export default function App() {
  const [status, setStatus] = useState<PermissionStatus | null>(null);
  const [schedule, setSchedule] = useState<AdhanEvent[]>([]);
  const [soundUri, setSoundUri] = useState<string | null>(null);
  const [now, setNow] = useState(new Date());

  const refresh = useCallback(() => {
    rescheduleWeek();
    setStatus(AdhanEngine.getPermissionStatus());
    setSchedule(AdhanEngine.getSchedule());
    setSoundUri(AdhanEngine.getSoundUri());
    setNow(new Date());
  }, []);

  useEffect(() => {
    if (Platform.OS === 'android' && Platform.Version >= 33) {
      PermissionsAndroid.request('android.permission.POST_NOTIFICATIONS').finally(refresh);
    } else {
      refresh();
    }
    const sub = AppState.addEventListener('change', (s) => s === 'active' && refresh());
    const tick = setInterval(() => setNow(new Date()), 30_000);
    return () => {
      sub.remove();
      clearInterval(tick);
    };
  }, [refresh]);

  const today = timesForDay(now);
  const next = schedule.find((e) => e.triggerAtMillis > now.getTime());

  async function pickSound() {
    const res = await DocumentPicker.getDocumentAsync({ type: 'audio/*', copyToCacheDirectory: true });
    if (res.canceled || !res.assets?.[0]) return;
    const dir = new Directory(Paths.document, 'adhan');
    if (!dir.exists) dir.create();
    const dest = new File(dir, 'custom.mp3');
    if (dest.exists) dest.delete();
    new File(res.assets[0].uri).copy(dest);
    AdhanEngine.setSoundUri(dest.uri);
    setSoundUri(dest.uri);
    Alert.alert('تم', `اخترت: ${res.assets[0].name}`);
  }

  function onTest() {
    const at = scheduleTest(2);
    setSchedule(AdhanEngine.getSchedule());
    Alert.alert(
      'تمت جدولة أذان تجريبي',
      `سيُرفع الأذان الساعة ${formatTime(at)}.\nاقفل الهاتف الآن وانتظر.`,
    );
  }

  return (
    <View style={s.root}>
      <StatusBar style="light" />
      <ScrollView contentContainerStyle={s.content}>
        <Text style={s.brand}>سَكِينَة</Text>
        <Text style={s.subtitle}>نسخة اختبار الأذان (APK #0)</Text>

        <View style={s.card}>
          <Text style={s.cardTitle}>مواقيت اليوم — {TRIPOLI.name}</Text>
          {(['fajr', 'sunrise', ...PRAYERS.slice(1)] as const).map((p) => (
            <View key={p} style={s.row}>
              <Text style={s.label}>{PRAYER_LABELS[p]}</Text>
              <Text style={s.time}>{formatTime(today[p])}</Text>
            </View>
          ))}
          <Text style={s.note}>طريقة الحساب: {DEFAULT_METHOD_LABEL}</Text>
        </View>

        <View style={s.card}>
          <Text style={s.cardTitle}>الأذونات</Text>
          {status &&
            PERMISSIONS.map((p) => {
              const ok = Boolean(status[p.key]);
              return (
                <View key={p.key} style={s.permRow}>
                  <View style={{ flex: 1 }}>
                    <Text style={s.label}>
                      {ok ? '✅' : '⚠️'} {p.label}
                    </Text>
                    <Text style={s.note}>{p.why}</Text>
                  </View>
                  {!ok && (
                    <Pressable style={s.smallBtn} onPress={() => AdhanEngine.openSettings(p.kind)}>
                      <Text style={s.smallBtnText}>تفعيل</Text>
                    </Pressable>
                  )}
                </View>
              );
            })}
          {status && (
            <Text style={s.note}>
              الهاتف: {status.manufacturer} — Android API {status.sdkInt}
            </Text>
          )}
        </View>

        <View style={s.card}>
          <Text style={s.cardTitle}>صوت الأذان</Text>
          <Text style={s.note}>
            {soundUri
              ? 'تم اختيار ملف أذان من هاتفك.'
              : 'لم يُختر ملف أذان بعد؛ سيُستخدم صوت المنبه الافتراضي في الهاتف.'}
          </Text>
          <Pressable style={s.btn} onPress={pickSound}>
            <Text style={s.btnText}>اختيار ملف أذان (MP3)</Text>
          </Pressable>
        </View>

        <View style={s.card}>
          <Text style={s.cardTitle}>الاختبار</Text>
          <Pressable style={[s.btn, { backgroundColor: C.gold }]} onPress={onTest}>
            <Text style={[s.btnText, { color: C.navy }]}>أذان تجريبي بعد دقيقتين</Text>
          </Pressable>
          <View style={s.btnRow}>
            <Pressable style={[s.btn, { flex: 1 }]} onPress={() => AdhanEngine.playNow('تجربة صوت الأذان')}>
              <Text style={s.btnText}>تشغيل الآن</Text>
            </Pressable>
            <Pressable style={[s.btn, { flex: 1, backgroundColor: C.danger }]} onPress={() => AdhanEngine.stop()}>
              <Text style={s.btnText}>إيقاف</Text>
            </Pressable>
          </View>
          <Text style={s.note}>
            لإيقاف الأذان أثناء تشغيله: زر «إيقاف الأذان» في الإشعار، أو أحد زرّي الصوت.
          </Text>
        </View>

        <View style={s.card}>
          <Text style={s.cardTitle}>المجدول ({schedule.length} أذانًا خلال 7 أيام)</Text>
          {next && (
            <Text style={s.label}>
              القادم: {next.title.replace('حان الآن موعد ', '')} —{' '}
              {new Date(next.triggerAtMillis).toLocaleDateString('ar-LY')} {formatTime(new Date(next.triggerAtMillis))}
            </Text>
          )}
          {schedule.slice(0, 6).map((e) => (
            <Text key={e.id} style={s.note}>
              • {e.body} ({e.title.replace('حان الآن موعد ', '')})
            </Text>
          ))}
        </View>
      </ScrollView>
    </View>
  );
}

const s = StyleSheet.create({
  root: { flex: 1, backgroundColor: C.navy },
  content: { padding: 16, paddingTop: 56, gap: 14 },
  brand: { color: C.gold, fontSize: 40, fontWeight: '700', textAlign: 'center' },
  subtitle: { color: C.gray, fontSize: 14, textAlign: 'center', marginBottom: 4 },
  card: {
    backgroundColor: 'rgba(6,59,67,0.85)',
    borderRadius: 18,
    padding: 16,
    gap: 10,
    borderWidth: 1,
    borderColor: 'rgba(232,184,91,0.25)',
  },
  cardTitle: { color: C.gold, fontSize: 17, fontWeight: '700' },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  permRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  label: { color: C.white, fontSize: 16, textAlign: 'left' },
  time: { color: C.white, fontSize: 16, fontVariant: ['tabular-nums'] },
  note: { color: C.gray, fontSize: 13, opacity: 0.8, textAlign: 'left' },
  btn: { backgroundColor: C.emerald, borderRadius: 12, paddingVertical: 12, alignItems: 'center' },
  btnRow: { flexDirection: 'row', gap: 10 },
  btnText: { color: C.white, fontSize: 16, fontWeight: '700' },
  smallBtn: { backgroundColor: C.emerald, borderRadius: 10, paddingVertical: 6, paddingHorizontal: 14 },
  smallBtnText: { color: C.white, fontWeight: '700' },
});

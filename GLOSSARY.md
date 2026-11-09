# سكينة (SAKINA)

An offline-first Islamic companion app for Android, centred on accurate prayer times and a reliable adhan.

## Prayer times

**Prayer** (صلاة):
One of the five daily obligatory prayers: Fajr, Dhuhr, Asr, Maghrib, Isha.
_Avoid_: Salah time, slot

**Sunrise** (الشروق):
The end of Fajr's time. Shown alongside the prayers but is not a Prayer and never gets an adhan.

**Prayer time** (وقت الصلاة):
The computed moment a Prayer's time begins for a given location and date, after the user's Manual offset.
_Avoid_: Adhan time

**Calculation method** (طريقة الحساب):
A named set of sun-angle rules (e.g. Egyptian General Authority of Survey) used to compute Prayer times.

**Asr school** (المذهب):
Either Standard (Shafi'i/Maliki/Hanbali) or Hanafi; only affects Asr.
_Avoid_: Madhab setting, juristic method

**Manual offset** (التعديل اليدوي):
Minutes the user adds or subtracts from one Prayer's computed time.

**Iqama time** (وقت الإقامة):
When the congregation actually stands to pray; entered by the user, never computed.

## Adhan and alerts

**Adhan** (أذان):
The full call to prayer played as audio when a Prayer time arrives.

**Fajr adhan** (أذان الفجر):
The Fajr variant of an Adhan that includes «الصلاة خير من النوم».

**Muezzin** (مؤذن):
A named Adhan voice the user picks per Prayer (e.g. Makkah, Madinah, Libyan).
_Avoid_: Reciter (reserved for Quran recitation), voice, sound

**Short alert** (تنبيه مختصر):
A brief tone used instead of a full Adhan.

**Pre-prayer reminder** (تنبيه قبل الصلاة):
A notification a chosen number of minutes before a Prayer time.

**Fajr alarm** (منبه الفجر):
A true alarm before Fajr that rings over the lock screen with snooze and gradual volume; distinct from the Fajr adhan.
_Avoid_: Fajr notification

## Quran

**Reciter** (قارئ):
A Quran reciter whose recitation audio can be streamed or downloaded.
_Avoid_: Muezzin, sheikh

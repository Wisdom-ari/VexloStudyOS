# Vexlo StudyOS

Native Android (Kotlin + Jetpack Compose) exam study and alarm app for the Kabale University
Semester 1 exams (first paper 27 Nov 2026). Developer: **Vexlo Digital**.
Offline, no account, all data on the phone. Android 8.0+ (built for Android 14 / Tecno Pop 9).

## Project structure

```
app/src/main/java/com/vexlo/studyos/
  StudyApp.kt            Application: channels, first-run seed, re-arms alarms on every start
  MainActivity.kt        Edge-to-edge Compose host, asks for notification permission
  data/                  Room entities + DAO + database, DataStore settings, seed data
  alarm/                 AlarmScheduler (AlarmManager), AlarmReceiver, AlarmService (sound,
                         snooze/dismiss), BootReceiver (reboot), DailyWorker (WorkManager)
  domain/                Logic (streaks, readiness, schedule), TimerEngine, Backup
  ui/                    Theme, components, 5 tabs + Exam mode + Timer, ViewModel
```

## How it works

| Requirement | Implementation |
|---|---|
| Alarms | `AlarmManager.setAlarmClock()` (works in Doze, no exact-alarm restriction); each fire re-arms the next weekly occurrence; days of week selectable per alarm |
| Sound / snooze / dismiss | `AlarmService` (foreground, `mediaPlayback`) plays the chosen audio file (or default alarm sound), notification has Snooze and Dismiss |
| Session-start notification | A normal notification "Session started: ..." stays in the shade after the alarm is dismissed |
| Reboot / time change | `BootReceiver` on BOOT_COMPLETED, QUICKBOOT_POWERON, MY_PACKAGE_REPLACED, TIME_SET, TIMEZONE_CHANGED |
| Battery restrictions | Settings screen shows status of notifications / exact alarms / battery optimisation with buttons to the right system pages; `DailyWorker` (every 6 h) re-arms alarms as a safety net |
| Database | Room (`studyos.db`): courses, topics+subtopics, notes, assignments, questions, sessions, alarms, timetable, mocks, exam papers, checklists, tasks |
| Settings | DataStore (targets, dates, snooze, pomodoro defaults) |
| Progress | Every number is computed from saved records; readiness = topics 40% + checklist 20% + last 3 mocks 30% + question accuracy 10% (only parts that have data) |
| Timer | Wall-clock based, survives screen-off and app kill, rings via AlarmManager, saves actual focus time as a session |
| Backup / restore / export | Copy of the database file via the system file picker; CSV progress report |

Default data: the six courses with a **starter topic outline (generic, edit it to match your lecturer's syllabus)**,
the 10 requested alarms (Mon-Sat; 7 PM revision every day) and the weekly timetable you specified.

## Build the debug APK

### Option A - GitHub (no PC needed)
1. Create a GitHub repository and upload this whole folder (keep `.github/workflows/build-apk.yml`).
2. Open the **Actions** tab > **Build debug APK** > *Run workflow* (it also runs on every push).
3. When it finishes, open the run and download the artifact **VexloStudyOS-debug-apk** (contains `app-debug.apk`).

### Option B - Android Studio (Ladybug or newer, JDK 17)
1. *File > Open* this folder, let Gradle sync (needs internet once).
2. *Build > Build APK(s)*. Output: `app/build/outputs/apk/debug/app-debug.apk`.

### Option C - command line
```
./gradlew assembleDebug
```
(needs JDK 17 and the Android SDK, platform 34.)

## Install on the Tecno Pop 9
1. Copy `app-debug.apk` to the phone, open it, allow "Install unknown apps" for your file manager/browser.
2. Open Vexlo StudyOS and allow notifications when asked.

## Make alarms reliable (do this once)
Open **Settings > Alarm reliability** in the app; each row must show OK.
1. **Notifications**: allow.
2. **Exact alarms**: tap *Exact alarms* and allow "Alarms & reminders" (Android 12+).
3. **Battery**: tap *Battery* > Allow, then in system settings set Vexlo StudyOS to *Unrestricted*.
4. **Tecno / HiOS**: Phone Master > Auto-start management > enable Vexlo StudyOS. Open Recent apps and lock the app card (lock icon) so the cleaner does not kill it.
5. Turn on volume for **Alarms** (not just media volume) and make sure Do Not Disturb allows alarms.

## Test plan
1. **Test alarm**: Settings > *Test alarm in 20 seconds*, lock the screen. Expect sound + notification with Snooze/Dismiss.
2. **Real alarm**: edit any alarm time to 2 minutes ahead (tap the time), wait; check it rings, then Snooze (rings again after the snooze minutes) and Dismiss.
3. **Weekly repeat**: after it rings, Home > *Next alarm* shows the following occurrence.
4. **Reboot**: restart the phone *without opening the app*, then confirm the next alarm still rings.
5. **Timer**: Home > START STUDY > set 5/1 minutes, start, lock the screen; it rings at 5 min and the session appears in Progress.
6. **Offline**: switch on airplane mode, force-stop the app, reopen. All data and screens work; alarms still ring.
7. **Backup**: Settings > Back up all data, uninstall/reinstall, Restore, reopen the app.

## Known limits
- Course/topic content is yours to maintain; starter topics are placeholders.
- Timetable items can be added and removed but not re-ordered (remove and re-add).
- If the phone is switched fully off at an alarm time, nothing can ring; alarms resume after boot.

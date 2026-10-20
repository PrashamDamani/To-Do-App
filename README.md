# To Do app - C030 Prasham Damani - Assignment 2

| Item | Value |
|---|---|
| Student | **Prasham Damani** |
| Roll No. | **C030** |
| Assignment | 2 - Android To-Do / Priority Management with Firebase Realtime Database |
| App display name | To Do app |
| Project / root folder | `C030-PrashamDamani-assignment2` |
| Application ID & Kotlin package | `com.example.c030.todo` |

> **Why the package has underscores, not hyphens:** `C030-PrashamDamani-assignment2` is not a legal Kotlin/Java package identifier (hyphens are not allowed). The project folder and Gradle project name use the exact name you asked for. The package/application ID uses the closest valid form. Register this exact ID in Firebase.

> **Honest status:** This source was written without access to an Android SDK, Gradle or a device, so it has **not been compiled, run or tested by the author**. Follow the steps below; if Android Studio reports a compile error, see Troubleshooting. Nothing in this README claims a passing build.

## 1. Overview
Material 3 / Jetpack Compose task manager: Firebase Auth login, per-user tasks in Realtime Database with live updates, priority sorting, calendar (month/week/agenda), local reminders (AlarmManager), countdown timer, insights, offline cache.

The app name shown on screen is **To Do app**. Headings use the signed-in user's name, e.g. **Prasham's Tasks**, **Prasham's Reminders**, **Prasham's Timer**, **Prasham's Insights**, and the calendar heading **C030 - Prasham Damani Schedule** (built from the profile's roll number + name; other users with no roll number see "<Name>'s Schedule").

## 2. Features
- Registration (name, optional roll number, email, password, confirm), login, forgot-password email, session restore, logout confirmation, profile editing. Passwords are only in Firebase Auth, never in the database.
- Tasks: create / read (live `callbackFlow` + `ValueEventListener` -> `StateFlow` -> `collectAsStateWithLifecycle` -> `LazyColumn`) / update (`updateChildren`) / delete (swipe via `SwipeToDismissBox`, button, detail screen; Undo snackbar restores from an in-memory copy, this session only).
- Complete / reopen with animated checkbox. Swipe right = complete/reopen, swipe left = delete.
- Priority HIGH/MEDIUM/LOW, separate Importance CRITICAL/IMPORTANT/NORMAL, categories (7 defaults + custom text categories stored in Firebase).
- Smart sort: High > Medium > Low; inside each priority overdue first, then earliest deadline/start, undated last, then creation time. Separate **Overdue** section. Optional sorts: priority, deadline, creation date, A-Z.
- Search, status chips, advanced filter bottom sheet (priority, importance, category, date, overdue).
- Dashboard: greeting by local time + user name, date, C030 identity (for accounts with roll number), stat cards (tap -> filtered list), progress, quick filters, today / overdue / upcoming / recent, quick-add.
- Calendar: month, week, agenda; today highlight; selected day; priority dots; deadlines vs time blocks vs all-day vs unscheduled; start/end/duration; overlap warnings; reschedule action. All derived from Firebase tasks.
- Reminders: per-task on/off, at time / 5 / 15 / 30 min / 1 h / 1 day before / custom; relative offsets use the start time, else the due time. A deadline alone never notifies. Cancelled on complete/delete/disable; stable request ids; notification opens the task.
- Countdown timer: presets/custom, start/pause/resume/reset, monotonic clock (`SystemClock.elapsedRealtime`), completion notification, optional task link, "Start Timer" from task details.
- Recurring tasks: daily / weekly / monthly / custom weekdays. Completing an occurrence creates the next one (dates shifted from the due date, else start date).
- Insights: totals, completion %, by priority, by category, overdue, completed today, 7-day trend.
- Offline: persistence enabled in `ToDoApplication`, offline banner, `keepSynced`, malformed records skipped.
- Settings (DataStore): reduce animations, default sort. System "animation scale = 0" is also honored.

## 3. Screens
Splash, Login, Register, Home, Tasks, Add Task, Edit Task, Task Details, Calendar, Reminders, Timer, Insights, Profile & Settings. Bottom bar: Home, Tasks, Calendar, Reminders, Profile. FAB (Home/Tasks/Calendar/Reminders) adds a task. Insights and Timer are reachable from Home (insights icon) and Profile. Android Back works through the Navigation back stack.

## 4. Stack & architecture
Kotlin 2.0.21, Compose BoM 2024.10.01 (Material 3 1.3), Navigation Compose 2.8.3, Lifecycle 2.8.7, DataStore 1.1.1, Coroutines 1.9.0, Firebase BoM 33.5.1 (Auth, Realtime Database), Google Services plugin 4.4.2, AGP 8.7.2, Gradle 8.9, JDK 17, minSdk 26, compile/target SDK 35. Versions are in `gradle/libs.versions.toml`.

```
data/model        Task, UserProfile
data/repository   TaskRepository, AuthRepository (Firebase)
data/prefs        SettingsStore (DataStore)
viewmodel         AuthViewModel, TaskViewModel, TimerViewModel
reminders         ReminderScheduler, NotificationHelper, TaskNotificationReceiver, BootReceiver
util              TaskLogic (sorting/filter/validation/calendar/recurrence - pure, unit-tested), DateFmt, Permissions
ui/*              Compose screens; ui/components shared widgets; ui/theme
navigation        AppNavigation (routes, bottom bar, transitions, snackbars)
```
WorkManager is intentionally **not** used: reminders are clock-time alarms (AlarmManager), and no deferrable background work exists in this app.

## 5. Prerequisites
Android Studio Ladybug (2024.2) or newer, JDK 17 (bundled), Android SDK 35, an emulator (API 26+; API 33+ to test the notification permission) or a device, a Google account for Firebase.

## 6. Open the project
1. Unzip. Android Studio -> **Open** -> choose the `C030-PrashamDamani-assignment2` folder.
2. Let Gradle sync (the included Gradle 8.9 wrapper downloads Gradle automatically).
3. The build will **fail until you add `google-services.json`** (next section).

## 7. Firebase setup (required)
1. Go to <https://console.firebase.google.com> -> **Add project** (any name, Analytics optional).
2. Project overview -> **Add app -> Android**. Package name must be exactly: `com.example.c030.todo`. SHA-1 is not needed for email/password.
3. **Download `google-services.json`** and place it at:
   `C030-PrashamDamani-assignment2/app/google-services.json` (inside the `app` folder, next to `app/build.gradle.kts`). **This file is supplied by you; it is not included and is git-ignored.** `app/google-services.json.example` only shows the shape.
4. Build -> **Authentication -> Get started -> Sign-in method -> Email/Password -> Enable**.
5. Build -> **Realtime Database -> Create database**. Pick a region, start in **locked mode**.
6. Realtime Database -> **Rules** tab -> paste the contents of `database.rules.json` -> **Publish**.
   - Only the signed-in user can read/write `/users/{uid}` and `/tasks/{uid}`; task fields are validated (title length, priority/importance/recurrence enums, ownerUid == auth uid, etc.).
   - If your database region is not us-central1, `google-services.json` already contains your `firebase_url`; re-download the file after creating the database so the URL is included.
7. Android Studio: **File -> Sync Project with Gradle Files**, then **Run**.
8. In the app: Register (name/roll are prefilled with Prasham Damani / C030; edit for other users) -> add a task -> in the console, Realtime Database -> Data shows `/users/<uid>` and `/tasks/<uid>/<taskId>`.

Data layout: `/users/{uid}/{fullName,email,rollNumber,createdAt,categories/*}`, `/tasks/{uid}/{taskId}/{...task fields...}`. Timestamps are UTC epoch milliseconds shown in the device time zone.

## 8. Run
Select an emulator/device -> Run `app`. On Android 13+ allow the notification permission when asked. For exact reminders on Android 12+ use Profile -> "Exact alarm permission" (or the banner on Reminders). Without it reminders still fire but may be delayed.

## 9. Unit tests
`app/src/test/.../TaskLogicTest.kt` covers priority/deadline ordering, overdue rules, filters, stats, validation, reminder computation, end time, overlap detection, calendar entry kinds, reschedule shift, recurrence, weekly trend, greeting, email/password checks and profile headings. Run: right-click the test class -> Run, or `./gradlew test` . Not yet executed by the author.

## 10. Demo steps
1. Register as Prasham Damani / C030. Home greets "Good <time>, Prasham Damani" with the C030 identity.
2. Add three tasks (High, Medium, Low) with deadlines; open Tasks and verify order and the Overdue section (use a past due date by editing before saving? past due is allowed, past reminder is not).
3. Open the Firebase console, change a task value: the app updates live.
4. Swipe a task right (complete) and left (delete -> Undo).
5. Calendar: see "C030 - Prasham Damani Schedule"; add two overlapping timed tasks to see the warning; reschedule one.
6. Add a task with a reminder 2 minutes ahead; lock the screen; tap the notification to open the task.
7. Timer: start, pause, resume, reset; let one finish.
8. Turn on airplane mode: banner appears, cached tasks stay visible, edits sync after reconnecting.

## 11. Troubleshooting
- **"File google-services.json is missing"**: place the file in `app/` (see section 7).
- **"Package name ... not found in google-services.json"**: the Firebase Android app was registered with a different package; register `com.example.c030_prashamdamani_assignment2`.
- **Permission denied**: rules not published, or you are not signed in.
- **Sign-up error / "operation not allowed"**: enable Email/Password in Authentication.
- **Database never loads**: `google-services.json` lacks `firebase_url`; create the database, then re-download the file. First load needs internet.
- **Reminder never shows**: notifications blocked (Reminders screen shows a banner), battery optimization/OEM task killers, or app force-stopped.
- **Gradle/AGP errors**: use JDK 17, Android Studio Ladybug+, SDK 35 installed. Dependency versions are pinned in `gradle/libs.versions.toml`.
- **Compile errors**: this code was not compiled by its author; please report the first error line and fix the import/signature it names.

## 12. Known limitations (honest list)
- Not compiled/run/tested by the author (no SDK available).
- Reminders: exact delivery is not guaranteed. Without exact-alarm permission they are inexact; Doze, battery savers and OEM restrictions can delay them; **force-stop cancels all alarms** until the app is opened; permission denial means no notification is shown. After reboot alarms are restored from locally stored metadata (missed ones are dropped). Alarms are re-synchronised with Firebase each time the app loads tasks, so reminders created on another device appear once this app is opened.
- Logging out cancels this device's pending reminders.
- Undo-delete only works in the same app session (task copy kept in memory).
- Offline cache only contains data previously loaded; sign-up/sign-in and first load need network. Queued offline writes sync on reconnect. Offline login after sign-out is not possible.
- Calendar: a timed block is shown on its start date only (multi-day/overnight blocks are not split); week starts per device locale. Custom categories are text-only (no per-category icon picker). Dashboard "Due today" uses due date, else start date.
- Recurrence: next occurrence is created on completion (not pre-generated); reopening a completed recurring task does not delete the occurrence already created.
- Dark theme colors are basic; light theme is the designed one.
- No screenshots are included (the app could not be launched in the build environment). The Gradle wrapper files are copied from the official Gradle 8.9.0 sources.

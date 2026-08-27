# Social Time Lock

A simple Android app that keeps selected apps (Instagram, Telegram, etc.)
locked except during time windows you define, and blocks them the rest of
the day.

## How it works
1. On the home screen, tap **"+ Create New Rule"**.
2. Check one or more apps from the list:
   - Checking **just one** creates an individual restriction for that app alone.
   - Checking **several at once** groups them into a shared "group rule" that
     follows the same list of time windows.
3. Add the allowed time window(s) for that rule (morning, noon, night —
   as many as you like).
4. Tap **"Save Rule"**. You'll now see it listed under "My Rules" on the
   home screen.
5. You can create as many separate rules as you want — for example, a group
   rule for Instagram + Telegram with an evening window, and a separate
   individual rule for YouTube with morning and noon windows.
6. Each app can only belong to **one** rule at a time; if you select an app
   that was already in another rule, it's automatically moved out of the old
   rule and into the new one.
7. Tap **"Enable Accessibility Service"** and turn the service on in Android
   settings (this permission is required so the app can detect which app is
   being opened).
8. From then on, if you try to open one of a rule's apps outside its allowed
   windows, you'll immediately see the "This app is locked 🔒" screen and get
   sent back to the home screen. Apps that aren't part of any rule are left
   completely untouched.

## Extra features
- **Temporary access ("wait it out"):** on the locked screen there's a
  "⏳ Wait and Get 5 Minutes" button. Tapping it starts a deliberate 30-second
  countdown — designed to interrupt the impulsive urge to open the app —
  after which you get 5 minutes of temporary access to that specific app.
- **Usage stats:** tap "📊 Usage Stats" on the home screen to see how much
  each app has been used today or this week, with a comparison bar between
  apps. This needs a separate "Usage Access" permission (different from
  Accessibility), with its own button to jump to the right settings screen.
  Apps that are part of an active rule show a 🔒 badge.
- **Backup:** the "Rules Backup" card on the home screen lets you export all
  your rules to a JSON file, or restore them later — either merging with your
  current rules or replacing them entirely.

## How to build and install it on your phone (step by step for beginners)

### Step 1: Install Android Studio
1. Download and install it from the official site (developer.android.com/studio).
2. The first time you open it, let it download the default SDK (takes a few minutes).

### Step 2: Open the project
1. Extract the `SocialTimeLock` zip folder.
2. In Android Studio: File → Open → select the `SocialTimeLock` folder.
3. Wait for "Gradle Sync" to finish (a progress bar shows at the bottom).
   - If it shows a message about the "Gradle Wrapper" and offers to create
     it, accept — Android Studio will set it up automatically.

### Step 3: Connect your phone or run on an emulator
- **Real phone (recommended, since Accessibility can be limited on emulators):**
  1. On your phone: Settings → About phone → tap "Build Number" seven times
     in a row to enable Developer Mode.
  2. Settings → Developer options → turn on "USB debugging".
  3. Connect your phone to your computer with a USB cable and confirm the
     connection prompt.
- At the top of Android Studio, select your device from the list and click
  the green ▶ (Run) button.

### Step 4: Set up the app on your phone
1. Once installed and opened, check the apps you want and set your time window.
2. Tap "Save Rule".
3. Tap "Enable Accessibility Service"; on the screen that opens, find this
   app's name ("Social Time Lock") and turn it on. Android will show a
   security warning — that's expected since this permission is powerful;
   confirm it.

From then on, the app runs in the background and locks the selected apps
outside their allowed hours.

## Important technical notes
- This app uses **AccessibilityService**, which Google Play restricts
  heavily. It's fine for personal use and direct APK installs, but if you
  ever want to publish it on Google Play, you'll need to clearly justify
  this permission's use in the Play Console form.
- Settings (selected apps and time windows) are stored locally on the phone
  (SharedPreferences).
- To prevent someone from simply disabling or uninstalling the app to bypass
  the lock, a PIN/password to disable it could be added later — let me know
  if you want this feature too.

## Project structure
```
SocialTimeLock/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/socialtimelock/
│       │   ├── MainActivity.kt              ← Home screen (rules list)
│       │   ├── GroupEditActivity.kt          ← Create/edit a rule (individual or group)
│       │   ├── BlockedScreenActivity.kt      ← Lock screen
│       │   ├── AppBlockerAccessibilityService.kt ← Core locking engine
│       │   ├── UsageStatsActivity.kt         ← Usage stats screen
│       │   ├── UsageStatsHelper.kt           ← Reads usage stats from Android
│       │   ├── PrefsHelper.kt                ← Saves/loads rules & backups
│       │   ├── LockGroup.kt                  ← Model for a rule (apps + ranges)
│       │   ├── TimeRange.kt                  ← Model for a time window
│       │   ├── AppInfo.kt
│       │   └── AppListAdapter.kt
│       └── res/                              ← Colors, strings, layouts
├── build.gradle
└── settings.gradle
```

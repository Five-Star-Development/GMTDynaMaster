# GMT DynaMaster

An analog Wear OS watch face with a 24-hour GMT hand and a live, location-based sunrise/sunset bezel.

## What makes it special

- **24h GMT hand** — on top of the regular hour/minute/second hands, a dedicated GMT arrow makes one full rotation every 24 hours against a 24h bezel scale (1–23 plus a triangle at 24), just like a classic pilot/GMT watch.
- **Live sun-tracking bezel** — the bezel's day/night split isn't fixed: it's calculated from your last known location using real sunrise/sunset times, so the red "day" arc and blue "night" ring actually match daylight where you are.
- **Works without location too** — if location access isn't available or granted, it falls back to a static 6:00–18:00 day arc, so the watch face is always fully functional.
- **Battery-conscious** — location is refreshed every minute only until a first fix is found, then just once every 3 hours. No continuous GPS polling.
- **Proper ambient mode** — dimmed colors, a thinner bezel, and no seconds hand in always-on/ambient display, alongside full-color interactive rendering.
- **Minimal by design** — a standalone watch face with no phone companion app and no complications, just the dial.

## Requirements

- A Wear OS device (or Wear OS emulator) running API 30+ (Wear OS 3+)
- Android Studio (current stable). The Gradle wrapper is checked in, so it will automatically use Gradle 9.6.0, AGP 9.4.1, and Kotlin 2.2.10 — no manual setup needed.

## Run it from Android Studio

1. Open the project in Android Studio and let Gradle sync.
2. Connect a Wear OS device (enable Wi-Fi debugging on the watch and connect via Android Studio's device pairing) or start a Wear OS emulator.
3. Run the `app` configuration.
4. On the watch, long-press an empty spot on the watch face and pick **GMT DynaMaster** from the list to activate it.
5. On first launch you'll be asked for location permission — grant it for the live sunrise/sunset bezel, or deny it to keep the static 6–18 fallback.

## Just want the APK? (sideloading)

The simplest way to get it onto a watch without building from an IDE:

1. Build the APK:
   ```
   ./gradlew assembleDebug
   ```
   This produces `app/build/outputs/apk/debug/app-debug.apk`.
2. On the watch, enable Developer options: **Settings → System → About → tap "Build number" 7 times**.
3. In **Developer options**, enable **ADB debugging** (and **Debug over Wi-Fi** if you're not using a USB cradle).
4. Connect to the watch:
   ```
   adb connect <watch-ip>:5555
   ```
5. Install the APK:
   ```
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
6. On the watch, long-press an empty spot on the watch face and choose **GMT DynaMaster**.
7. Grant the location permission prompt if you want the live sunrise/sunset bezel; denying it just keeps the fixed 6–18 day arc.

Note: there's no release signing configured yet, so this is a debug build — fine for personal sideloading, not for store distribution.

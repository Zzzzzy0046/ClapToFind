# Clap To Find Phone — Project Context

## What this is
Android app that lets users find their phone by clapping or whistling.
Uses on-device ML (MediaPipe AudioClassifier + YAMNet) for sound detection.

## Tech stack
- Kotlin + Jetpack Compose (Material 3)
- MediaPipe AudioClassifier 0.10.35 (AUDIO_CLIPS mode)
- YAMNet TFLite model (AudioSet 521 classes)
- DataStore Preferences for persistence
- No DI framework (manual dependency via Application class)
- No network layer (offline-only app)

## Architecture
```
app/
  ClapToFindApp.kt          — Application class (PreferencesManager, SoundEngine singletons)
  data/
    PreferencesManager.kt   — All settings via DataStore
    Models.kt               — SoundEffect, FlashlightMode, VibrateMode, SoundSensitivity, SupportedLanguage enums
  service/
    ClapDetector.kt         — MediaPipe AudioClassifier wrapper, YAMNet inference (500ms polling, AUDIO_CLIPS)
    DetectionService.kt     — Foreground service, mic recording, schedule handling, alert triggering
  audio/
    SoundEngine.kt          — Vibration + flashlight + ToneGenerator. Sound output DISABLED (line ~63)
    FlashlightController.kt — Camera2 torch control
  receiver/
    BootReceiver.kt         — Restarts service after device reboot
    ScheduleReceiver.kt     — Time-based schedule enforcement (NOT wired to AlarmManager yet)
    FindPhoneWidget.kt      — 2×2 home screen widget
  ui/
    MainActivity.kt         — Single activity, Compose host
    navigation/             — NavHost with 14 routes
    screens/                — 16 screens (Splash, LanguageSelect, Home, Settings, Sound/Flash/Vibrate settings, etc.)
    theme/                  — Light + dark Material3 theme
  util/
    ToneGenerator.kt        — 24 synthesized sound effects (not wired into alert path yet)
  assets/
    yamnet.tflite           — 4.1MB, extracted from reference APK (fmp_mediapipe.apk)

## Key design decisions
1. YAMNet model: AUDIO_CLIPS mode (synchronous classify()), not STREAM. Mirrors reference APK pattern.
2. Sound output intentionally disabled — only vibrate + flashlight on trigger.
3. Subscription/payment layer not implemented — Pro gating logic exists but subscribe buttons are dead ends.
4. No dependency injection — ClapToFindApp holds singletons, screens access via ClapToFindApp.instance.
5. DataStore flow-based reads everywhere, runBlocking in receivers (known ANR risk).

## Known issues from code review (136 findings, not yet fixed)
- Critical: BootReceiver just added, needs testing
- Critical: SoundSensitivity parameter ignored by ClapDetector (hardcoded 0.25 threshold)
- Critical: No battery optimization exemption request at runtime
- Critical: No AudioFocus handling (phone call kills detection silently)
- Critical: ScheduleReceiver has no AlarmManager wiring
- High: Sound output disabled (playAlert() sound path commented out)
- High: Widget/receiver use runBlocking on main thread (ANR risk)
- High: Missing TalkBack content descriptions throughout UI
- Full review findings at ~/.claude/projects/.../workflows/wf_cff1eac7-68d/journal.jsonl

## Build
- Gradle 8.14.3, AGP 8.2.0, Kotlin 1.9.20, Compose BOM 2023.10.01
- JDK 21 (bundled with Android Studio at D:/Program Files/Android Studio/jbr)
- compileSdk 34, minSdk 26, targetSdk 34
- Build: `./gradlew assembleDebug`
- Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

## Environment
- Windows 11, Android SDK at C:\Users\simon\AppData\Local\Android\Sdk
- Test device: R9DMC057PKJ (adb)
- GitHub: https://github.com/Zzzzzy0046/ClapToFind (private)

## Why YAMNet (not ML Kit or custom model)
ML Kit's Sound Notifications API only detects pre-defined event types (baby sounds, smoke alarm, etc.) — no "clap" or "whistle" class. MediaPipe AudioClassifier + YAMNet was the only Google-provided on-device solution that directly classifies "Clapping" and "Whistling" from the AudioSet ontology.
Reference APK (fmp_mediapipe.apk) confirmed this approach works in production with 1M+ downloads.

## Next steps (priority order)
1. Fix all 18 critical issues from code review
2. Wire SoundEngine to actually play sound on alert
3. Implement sensitivity-to-threshold mapping in ClapDetector
4. Add AlarmManager for schedule enforcement
5. Add AudioFocus handling
6. Request battery optimization exemption at runtime
7. Fix accessibility (content descriptions)
8. Add subscription/payment integration
9. Implement missing PRD features (rating timing, widget trial gating, etc.)

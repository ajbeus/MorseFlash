<p align="center">
  <img src="app/src/main/res/drawable-nodpi/splash_art.webp" alt="Morse Flash" width="320">
</p>

# Morse Flash

An Android app that turns any message into Morse code and sends it with your phone's flashlight, its screen, or a beep.

## Features

- **Live translation:** type a message and see the Morse code (`.-` / `-...`) as you type.
- **Three ways to send:**
  - **Flashlight:** blinks the rear camera flash. This is the default.
  - **Screen:** flashes the whole display white and black at full brightness. Tap anywhere to stop. It works on phones without a flashlight.
  - **Beep:** plays the message as a 700 Hz tone.
- **Adjustable speed:** 5 to 25 words per minute.
- **Standard Morse timing:** dot = 1 unit, dash = 3, gap between symbols = 1, between letters = 3, between words = 7.
- **Dark mode:** follows your system setting by default, with a toggle in the app.
- **Supports** letters, numbers and common punctuation (`. , ? ! ' " / ( ) & : ; = + - _ $ @`).

## Requirements

- Android 8.0 (API 26) or newer
- A flashlight is optional. Without one, the app uses Screen mode.
- No permissions are needed. The flashlight is controlled with Android's torch API, which doesn't require camera access.

## Build and run

1. Clone the repo:
   ```bash
   git clone https://github.com/ajbeus/MorseFlash.git
   ```
2. In Android Studio, choose **File → Open** and select the `MorseFlash` folder.
3. Let Gradle sync, choose a device, and press **Run** ▶.

Or build from the command line:

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

> The emulator has no real flashlight. Use Screen or Beep mode there, or test the flashlight on a physical phone.

## Project structure

```
app/src/main/java/com/example/morseflash/
├── MainActivity.kt   # UI (Jetpack Compose), splash screen, flashlight and screen output
├── Morse.kt          # Character table, text → Morse, and on/off pulse timing
└── Beeper.kt         # Tone generation with AudioTrack for Beep mode
```

## Built with

- Kotlin
- Jetpack Compose and Material 3
- AndroidX Core SplashScreen
- Camera2 `CameraManager.setTorchMode` for the flashlight
- `AudioTrack` for the tones

## Roadmap

- [ ] Decode Morse from sound with the microphone
- [ ] Decode Morse from flashes with the camera

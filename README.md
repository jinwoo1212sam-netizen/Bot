# ULTRON — Futuristic Native Android Personal Assistant

ULTRON is a native Android voice + text personal assistant engineered in **Kotlin** and **Jetpack Compose**. Built with clean architecture, strict permission verification, an offline-first local command engine, and optional OpenRouter AI integration.

Always addresses the user with respect as: **"Boss"**.

---

## ⚡ Core Architecture

- **Platform:** Native Android (minSdk 26, targetSdk 34)
- **UI Framework:** Jetpack Compose + Material 3
- **Language:** Kotlin with Coroutines & StateFlow
- **Persistence:**
  - **Room Database:** Complete conversation history and status tracking (`AppDatabase`, `ConversationEntity`, `ConversationDao`)
  - **DataStore:** Contact nicknames, social messaging accounts, custom multi-step routines, app aliases, and preferences
  - **EncryptedSharedPreferences:** Secure Android Keystore storage for OpenRouter API keys
- **Hardware & System Integrations:**
  - **SpeechRecognizer:** Live continuous microphone speech-to-text with audio RMS visualizer
  - **TextToSpeech:** Offline-capable speech synthesis engine
  - **AudioManager:** System stream volume controls (percentages, steps, mute, max)
  - **AlarmManager / AlarmClock:** Native timers and alarms
  - **Contacts & Telecom:** Official `ACTION_CALL` and `ACTION_DIAL` intents
  - **Messaging:** Safe official intents for WhatsApp, Telegram, SMS (`ACTION_VIEW`, `ACTION_SENDTO`)
  - **Settings Provider:** Standard Android deep-links for Wi-Fi, Bluetooth, Sound, Display, Battery, and Apps
  - **WindowManager Overlay:** Floating Assistant button with `SYSTEM_ALERT_WINDOW`
  - **Foreground Service:** Persistent microphone listening notification with required Android 14 foreground-service permissions

---

## 🚀 Opening in Android Studio

1. Open **Android Studio** (Hedgehog, Iguana, Jellyfish, or Koala).
2. Select **File > Open...** and choose this project root folder.
3. Allow Gradle to sync. The project uses Gradle 8.4 and AGP 8.2.2.
4. Connect an Android phone via USB or start an Android Virtual Device (AVD).
5. Click **Run > Run 'app'** (`Shift + F10`).

To build from command-line:
```bash
# Debug APK
./gradlew assembleDebug

# Run unit test suite
./gradlew test
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧠 Local Mode (AI OFF) — 100% Offline

When AI is switched OFF in Settings, ULTRON relies on the high-performance `LocalCommandParser`:
- **YouTube Play & Search:** Variable query extraction for any song or trailer (e.g., *"Kesariya chalao"*, *"youtube par arijit singh ka song search karo"*).
- **App Launches:** Resolves installed packages and custom aliases (e.g., *"youtube kholo"*, *"instagram kholo"*, *"YT" -> YouTube*).
- **Voice Calling:** Resolves voice nicknames to phone numbers (e.g., *"Mummy ko call karo"*, *"meri mummy ko call kar"*).
- **Social Messaging:** Resolves contacts and platforms (e.g., *"Tital ko WhatsApp par message karo: VC aa"*).
- **Volume Adjustments:** Percentages and modes (e.g., *"volume 50 percent karo"*, *"volume full karo"*, *"volume badhao"*).
- **Timers & Alarms:** Native clock intents (e.g., *"10 minute ka timer lagao"*, *"7 baje alarm lagao"*).
- **Multi-Action Sequencing:** Executes chained requests sequentially (e.g., *"YouTube kholo, Kesariya search karo aur volume 50 percent karo"*).
- **Device Security:** Explains physical limits safely (e.g., *"Ultron phone on kar de"*).

---

## 🌐 OpenRouter AI Provider (Optional)

Configure via **Settings > AI Provider**:
- **Default Base URL:** `https://openrouter.ai/api/v1`
- **Default Endpoint:** `https://openrouter.ai/api/v1/chat/completions`
- **Default Model:** `ling-3.0-flash-vl:free` (customizable to any OpenAI-compatible model)
- **Security:** The AI model is strictly restricted to an allowlist of structured tools (`ToolRegistry`). It **never** receives raw terminal or shell execution rights.

---

## 🛡️ Android Security & Permissions

ULTRON only uses official Android public APIs:
- `RECORD_AUDIO` — Voice input
- `CALL_PHONE` & `READ_CONTACTS` — Optional direct phone dialing
- `SYSTEM_ALERT_WINDOW` — Optional floating assistant pill
- `FOREGROUND_SERVICE_MICROPHONE` — Continuous wake word listener

No root, no hidden APIs, no screen scraping, no credential harvesting.

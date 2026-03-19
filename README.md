# Emotion-Aware AI Android

An offline, emotion-aware AI conversational assistant for Android. The app uses on-device LLM inference (llama.cpp via JNI), MediaPipe facial emotion detection, Android SpeechRecognizer for voice input, and TextToSpeech for spoken responses — all running fully on-device with no cloud dependency.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development Build](#local-development-build)
3. [Running Tests](#running-tests)
4. [Generating a Release Keystore](#generating-a-release-keystore)
5. [Building a Signed Release AAB Locally](#building-a-signed-release-aab-locally)
6. [Publishing to Google Play Store](#publishing-to-google-play-store)
   - [One-Time Play Console Setup](#one-time-play-console-setup)
   - [Automated Publishing via GitHub Actions](#automated-publishing-via-github-actions)
7. [CI/CD Workflows](#cicd-workflows)
8. [Project Architecture](#project-architecture)

---

## Prerequisites

| Tool | Minimum version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or later |
| JDK | 17 |
| Android SDK | API 34 (compileSdk) |
| Android NDK | r25c or later |
| CMake | 3.22.1 |
| Gradle | 8.7 (via wrapper — no installation needed) |

> The Gradle wrapper (`./gradlew`) downloads the correct Gradle version automatically.

---

## Local Development Build

```bash
# Clone the repository
git clone https://github.com/upamanyu92/Emotion-aware-ai-android.git
cd Emotion-aware-ai-android

# Build a debug APK (installs nothing, just produces the file)
./gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device or emulator:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or use Android Studio → **Run ▶** to build and launch directly.

---

## Running Tests

```bash
# Unit tests (runs on the JVM, no device needed)
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# HTML reports are written to:
# app/build/reports/tests/
```

---

## Generating a Release Keystore

You need a keystore file **once**. Keep it secret — never commit it to source control.

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias emotionawareai \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

You will be prompted for:
- **Keystore password** — remember this as `KEYSTORE_PASSWORD`
- **Key alias** — use `emotionawareai` (or any name) — remember as `KEY_ALIAS`
- **Key password** — remember this as `KEY_PASSWORD`
- Distinguished-name fields (name, organisation, country)

> **Important:** Store your `release.keystore`, passwords, and alias somewhere secure (e.g. a password manager). If you lose the keystore you cannot update an app already published on the Play Store.

---

## Building a Signed Release AAB Locally

Android App Bundles (`.aab`) are required for new Play Store submissions.

```bash
export KEYSTORE_FILE=/path/to/release.keystore
export KEYSTORE_PASSWORD=your_keystore_password
export KEY_ALIAS=emotionawareai
export KEY_PASSWORD=your_key_password

./gradlew bundleRelease

# Output:
# app/build/outputs/bundle/release/app-release.aab
```

To build a signed APK instead (for sideloading):

```bash
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

---

## Publishing to Google Play Store

### One-Time Play Console Setup

1. **Create a Google Play Developer account** at <https://play.google.com/console> (one-time $25 USD registration fee).
2. **Create a new application** in the Play Console:
   - Choose your default language and enter an app title.
   - Fill in the store listing (description, screenshots, feature graphic — minimum 2 phone screenshots required).
   - Complete the content rating questionnaire.
   - Set up pricing and distribution (free/paid, countries).
3. **Upload your first AAB manually** to the *Internal Testing* track (the Play Console requires at least one manual upload before the API can be used).
4. **Create a Service Account** for API access:
   - In the Play Console go to **Setup → API access**.
   - Link to a Google Cloud project (create one if needed).
   - Click **Create new service account**, follow the link to Google Cloud Console.
   - In Google Cloud Console: **IAM & Admin → Service Accounts → Create Service Account**.
   - Grant it the role **Service Account User**; click **Done**.
   - Create a JSON key for the service account and download it — this is your `PLAY_STORE_SERVICE_ACCOUNT_JSON`.
   - Back in Play Console, click **Grant access** next to the new service account and assign the **Release manager** (or **Release to selected tracks**) permission.

### Automated Publishing via GitHub Actions

The release workflow (`.github/workflows/release.yml`) triggers automatically when you push a version tag (e.g. `v1.0.0`) and:

1. Runs unit tests
2. Builds a signed release AAB using keystore credentials from GitHub Secrets
3. Uploads the AAB to the **Internal Testing** track on Google Play

#### 1 — Encode your keystore as Base64

```bash
base64 -i release.keystore | tr -d '\n'   # macOS / Linux
# Copy the entire output string
```

#### 2 — Add GitHub Secrets

In your GitHub repository go to **Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded content of `release.keystore` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (e.g. `emotionawareai`) |
| `KEY_PASSWORD` | Key password |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | Full contents of the service account JSON file |

#### 3 — Trigger a release

```bash
# Bump versionCode and versionName in app/build.gradle.kts, then:
git tag v1.0.0
git push origin v1.0.0
```

The workflow will run, and the new AAB will appear in the **Internal Testing** track within a few minutes. Promote it to **Alpha → Beta → Production** from the Play Console when ready.

> **Changing the track:** Edit the `track` field in `.github/workflows/release.yml`.  
> Valid values: `internal`, `alpha`, `beta`, `production`.

---

## CI/CD Workflows

| Workflow | File | Trigger | What it does |
|---|---|---|---|
| CI | `.github/workflows/ci.yml` | Push / PR to `main` | Builds debug APK, runs unit tests, uploads artifacts |
| Release | `.github/workflows/release.yml` | Push of `v*.*.*` tag | Builds signed release AAB, publishes to Play Store internal track |

---

## Project Architecture

```
app/src/main/
├── cpp/
│   ├── CMakeLists.txt          # NDK/CMake build for JNI library
│   └── llm_engine.cpp          # JNI bridge (llama.cpp integration point)
└── java/com/example/emotionawareai/
    ├── engine/
    │   ├── LLMEngine.kt         # Kotlin JNI wrapper, streaming token Flow
    │   └── EmotionDetector.kt   # MediaPipe FaceLandmarker (10fps, 8 emotions)
    ├── voice/
    │   └── VoiceProcessor.kt    # SpeechRecognizer → SharedFlow<String>
    ├── manager/
    │   ├── ConversationManager.kt  # Builds emotion-aware LLM prompts
    │   ├── MemoryManager.kt        # Room-backed conversation memory
    │   └── ResponseEngine.kt       # LLM inference → TextToSpeech
    ├── data/
    │   ├── database/            # Room DB, DAOs
    │   └── model/               # Room entities
    ├── domain/
    │   ├── model/               # ChatMessage, Emotion, ConversationContext
    │   └── repository/          # ConversationRepository
    ├── ui/
    │   ├── ChatViewModel.kt     # @HiltViewModel, StateFlow-driven
    │   ├── screen/ChatScreen.kt # Compose chat UI
    │   ├── component/           # MessageBubble, EmotionIndicator, VoiceInputButton
    │   └── theme/               # Material3 colours, typography, theme
    ├── di/AppModule.kt          # Hilt singleton providers
    ├── MainActivity.kt
    └── EmotionAwareApp.kt       # @HiltAndroidApp, onLowMemory handler
```

### Data Flow

```
User speaks
  └─► SpeechRecognizer (VoiceProcessor)
        └─► Transcribed text
              └─► ConversationManager
                    ├── detected Emotion (EmotionDetector ← CameraX)
                    └── recent memory (MemoryManager ← Room)
                          └─► LLM prompt
                                └─► LLMEngine (JNI → llama.cpp)
                                      └─► token stream
                                            ├─► Chat UI (Compose)
                                            └─► TextToSpeech
```

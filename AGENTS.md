# AGENTS.md
## Purpose
- This project is an offline Android assistant: Compose UI + Hilt DI + Room memory + Camera/MediaPipe signals + JNI LLM inference.
- Start by reading `README.md`, then `app/src/main/java/com/example/emotionawareai/ui/ChatViewModel.kt` (runtime orchestrator).
## Architecture Map (What Talks to What)
- UI layer: `MainActivity.kt` requests camera/audio permissions, gates boot flow (`SplashScreen` -> `LlmSetupScreen` -> `LoginScreen`), then hosts tabbed `ui/navigation/AppNavigation.kt` (`ChatScreen`, `DiaryScreen`, `InsightsScreen`, `GoalsScreen`, `EvaluationScreen`, `SettingsScreen`).
- State/orchestration: `ui/ChatViewModel.kt` owns session state, message list, generation lifecycle, and permission-triggered analyzer init.
- Managers: `manager/ConversationManager.kt` builds prompt context; `manager/ResponseEngine.kt` streams model output + optional TTS (system/neural fallback); `manager/MemoryManager.kt` wraps repository/preferences helpers; `manager/InsightsGenerator.kt` computes weekly insight summaries.
- Data layer: `domain/repository/ConversationRepository.kt` maps Room entities <-> domain models and controls active conversation preference.
- Persistence: Room DB in `data/database/*` + entities in `data/model/*`; DB is provided in `di/AppModule.kt` with `addMigrations(...)` and `fallbackToDestructiveMigration()`.
- Inference boundary: `engine/LLMEngine.kt` wraps JNI (`app/src/main/cpp/llm_engine.cpp`, `CMakeLists.txt`).
- Startup download boundary: `EmotionAwareApp.kt` triggers `engine/ModelDownloader.kt` in `Application.onCreate`; `ChatViewModel` observes downloader state and loads model when available.
## End-to-End Data Flow
- Text path: `ChatScreen` -> `ChatViewModel.sendMessage` -> `ConversationManager.buildContext` -> `ConversationContext.buildPrompt()` -> `ResponseEngine.generateResponse()` -> `LLMEngine.generateResponse()` -> UI token updates + `ConversationManager.saveMessage` -> Langfuse trace/evaluation (`evaluation/*`).
- Voice path: `voice/VoiceProcessor.kt` emits recognized text via `recognizedTextFlow`; ViewModel forwards it into `sendMessage` and handles continuous-mode restart/error flows.
- Camera path: `ChatScreen.CameraPreviewOverlay` sends bitmap frames to `ChatViewModel.onCameraFrame`, which fans out to `EmotionDetector` and `ActivityAnalyzer`.
## Project-Specific Conventions
- Prompt format is tag-based (`[SYSTEM]`, `[LONG-TERM MEMORY]`, `[CONTEXT]`, `[USER]`, `[ASSISTANT]`) in `domain/model/ConversationContext.kt`; keep this stable if changing prompt logic.
- Recent-message ordering is intentional: DAO returns DESC, repository reverses to chronological (`ConversationRepository.getRecentMessages`).
- In `ChatViewModel.sendMessage`, context is built before saving the current user message; preserve this ordering to avoid duplicating the same turn in both `[CONTEXT]` and `[USER]`.
- Emotion/activity detectors rate-limit internally (`MAX_FPS` and `FRAME_INTERVAL_MS`) and publish via `SharedFlow` with replay/drop-oldest.
- Model download starts at process startup (`EmotionAwareApp.onCreate`); setup/load state is driven by `ChatViewModel.initializeSession` + `ModelDownloader` flows, and UI shows stub mode when not loaded (`ChatScreen` top bar).
- Native layer currently ships a stub word-by-word callback in `llm_engine.cpp`; Kotlin currently re-emits one combined chunk in `LLMEngine.generateResponse`.
## Build/Test/Release Workflows
- Local debug build: `./gradlew assembleDebug`
- Unit tests: `./gradlew test`
- Instrumented tests: `./gradlew connectedAndroidTest`
- CI mirrors local `assembleDebug`/`test` inside Docker in `.github/workflows/ci.yml` (with `--stacktrace`) and uploads APK/test artifacts.
- Release tag `v*.*.*` triggers `.github/workflows/release.yml` to run tests, build `assembleRelease` + `bundleRelease`, and upload AAB to Play internal track.
- Push to `main` can also trigger internal-track deployment via `.github/workflows/pr-deploy.yml`.
- Release signing reads env vars in `app/build.gradle.kts`: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `KEYSTORE_TYPE`.
## Integration Notes and Change Traps
- MediaPipe assets are expected by code (`face_landmarker.task`, `pose_landmarker_lite.task` in analyzers) under `app/src/main/assets/`; ensure they are provisioned locally before camera analysis.
- `ChatViewModel` constructor dependencies are tightly coupled to tests; when adding/removing deps, update tests in `app/src/test` and `app/src/androidTest` mocks.
- If changing camera frame handling, preserve bitmap ownership rules (`processBitmapFrame` does not recycle shared bitmaps).
- If changing schema/entities, plan migration strategy; current setup includes explicit migrations plus destructive fallback, so version changes can still wipe local DB when migration coverage is incomplete.

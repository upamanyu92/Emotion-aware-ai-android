# AGENTS.md
## Purpose
- This project is an offline Android assistant: Compose UI + Hilt DI + Room memory + Camera/MediaPipe signals + JNI LLM inference.
- Start by reading `README.md`, then `app/src/main/java/com/example/emotionawareai/ui/ChatViewModel.kt` (runtime orchestrator).
## Architecture Map (What Talks to What)
- UI layer: `MainActivity.kt` requests camera/audio permissions, then hosts `ui/screen/ChatScreen.kt`.
- State/orchestration: `ui/ChatViewModel.kt` owns session state, message list, generation lifecycle, and permission-triggered analyzer init.
- Managers: `manager/ConversationManager.kt` builds prompt context; `manager/ResponseEngine.kt` streams model output + optional TTS; `manager/MemoryManager.kt` wraps repository/preferences helpers.
- Data layer: `domain/repository/ConversationRepository.kt` maps Room entities <-> domain models and controls active conversation preference.
- Persistence: Room DB in `data/database/*` + entities in `data/model/*`; DB is provided in `di/AppModule.kt` with `fallbackToDestructiveMigration()`.
- Inference boundary: `engine/LLMEngine.kt` wraps JNI (`app/src/main/cpp/llm_engine.cpp`, `CMakeLists.txt`).
## End-to-End Data Flow
- Text path: `ChatScreen` -> `ChatViewModel.sendMessage` -> `ConversationManager.buildContext` -> `ConversationContext.buildPrompt()` -> `ResponseEngine.generateResponse()` -> `LLMEngine.generateResponse()` -> UI token updates + `ConversationManager.saveMessage`.
- Voice path: `voice/VoiceProcessor.kt` emits recognized text via `recognizedTextFlow`; ViewModel forwards it into `sendMessage`.
- Camera path: `ChatScreen.CameraPreviewOverlay` sends bitmap frames to `ChatViewModel.onCameraFrame`, which fans out to `EmotionDetector` and `ActivityAnalyzer`.
## Project-Specific Conventions
- Prompt format is tag-based (`[SYSTEM]`, `[CONTEXT]`, `[USER]`, `[ASSISTANT]`) in `domain/model/ConversationContext.kt`; keep this stable if changing prompt logic.
- Recent-message ordering is intentional: DAO returns DESC, repository reverses to chronological (`ConversationRepository.getRecentMessages`).
- Emotion/activity detectors rate-limit internally (`MAX_FPS` and `FRAME_INTERVAL_MS`) and publish via `SharedFlow` with replay/drop-oldest.
- Model loading is async at startup (`ChatViewModel.initializeSession`); UI shows stub mode when not loaded (`ChatScreen` top bar).
- Native layer currently ships a stub word-by-word callback in `llm_engine.cpp`; Kotlin currently re-emits one combined chunk in `LLMEngine.generateResponse`.
## Build/Test/Release Workflows
- Local debug build: `./gradlew assembleDebug`
- Unit tests: `./gradlew test`
- Instrumented tests: `./gradlew connectedAndroidTest`
- CI mirrors local commands with stacktraces in `.github/workflows/ci.yml` (`assembleDebug`, `test`).
- Release tag `v*.*.*` triggers `.github/workflows/release.yml` to run tests, build `bundleRelease`, and upload to Play internal track.
- Release signing reads env vars in `app/build.gradle.kts`: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
## Integration Notes and Change Traps
- MediaPipe assets are expected by code (`face_landmarker.task`, `pose_landmarker_lite.task` in analyzers) under `app/src/main/assets/`.
- `ChatViewModel` constructor dependencies are tightly coupled to tests; when adding/removing deps, update tests in `app/src/test` and `app/src/androidTest` mocks.
- If changing camera frame handling, preserve bitmap ownership rules (`processBitmapFrame` does not recycle shared bitmaps).
- If changing schema/entities, plan migration strategy; current destructive fallback will wipe local DB on version changes.

# MiCTS Architecture

This document describes the maintained architecture of the personal MiCTS derivative at [rungruch/MiCTS](https://github.com/rungruch/MiCTS). The project is based on [parallelcc/MiCTS](https://github.com/parallelcc/MiCTS) and remains licensed under GPL-3.0.

## Project layout

MiCTS is a single Android application module with two product flavors:

| Flavor | Purpose |
| --- | --- |
| `MiCTS` | Full app with native Circle to Search triggering, MediaProjection fallback, smart editor, bundled OCR, Lens sharing, and optional AI chat. |
| `VISTrigger` | Minimal Voice Interaction Service trigger without the capture/editor, ML Kit models, OkHttp client, or AI configuration. |

Shared trigger, settings, and domain code lives in `app/src/main`. Flavor-specific implementations live in `app/src/MiCTS` and `app/src/VISTrigger`. Dependencies that process screenshots or access AI endpoints are added only to the MiCTS flavor.

Both flavors target API 36, support API 28 and later, and use JDK 17. The application IDs remain `com.parallelc.micts` and `com.parallelc.vistrigger` for compatibility with existing installs.

## Trigger flow

`MainActivity` is a transparent trigger trampoline. It reads the selected strategy and asks `TriggerCoordinator` for the next action.

### Native trigger

`AndroidNativeTriggerGateway` chooses the available Android system path:

- VIS is available across Android 9–16 and requires Google as the active assistant.
- CSHelper is available on Android 14 QPR3 and later when its LSPosed scope is active.
- CSService is available on Android 15 and later when its LSPosed scope is active.

A native invocation can report that Android accepted the request, rejected it, or raised an error. Acceptance does not prove that Google's UI appeared. Auto mode therefore records a pending confirmation and asks the user on the next launch.

### Strategy routing

- `AUTO` tries native first. A native rejection immediately selects fallback; an accepted request is confirmed by the user on the next launch.
- `NATIVE_ONLY` invokes the native service and exits.
- `LENS_FALLBACK` requests capture and opens the smart editor.
- `DIRECT_LENS` requests capture and attempts a full-frame Lens handoff after `CropActivity` reaches the foreground. If Lens fails, the smart editor remains available.

The selected strategy and Auto resolution are stored in normal app-private preferences.

## Screen-capture permission flow

The fallback uses Android MediaProjection and does not declare an accessibility service.

`CapturePermissionCoordinator` routes by API level and capture mode:

| API level | Behavior |
| --- | --- |
| 28–33 | The user can choose `REMEMBER_CONSENT` or `ASK_EVERY_TIME`. Remembered consent is held only in the process-wide memory of `ProjectionConsentStore`; it is not serialized to disk. |
| 34–36 | Tokens are treated as single-use. After a one-time explanation, every capture requests fresh Android approval. |

On API 28–33, process death, reboot, Android token invalidation, or OEM policy can remove remembered consent. The coordinator then requests permission again. Capture preferences retain only the selected mode, explanation state, and schema version.

After consent, `MainActivity` starts a one-shot `ScreenCaptureService`:

1. Clear and hide the transparent permission or trigger UI.
2. Start a foreground MediaProjection session.
3. Create an `ImageReader` and virtual display for the default display.
4. Wait for one frame, remove row-padding pixels, and reject an empty or protected frame.
5. Replace the single cached PNG through `BitmapCaptureWriter`.
6. Release the image reader, virtual display, callback, and projection, then stop the service.
7. Open `CropActivity` with either the captured frame or a typed failure reason.

The flow handles invalid permission results, service startup failures, projection shutdown, timeout, empty images, write failures, expired consent, and unknown failures.

## Smart editor and local OCR

`CropViewModel` owns the editor state: decoded capture, normalized selection, viewport transform, recognized lines, selected text, OCR state, external-action state, and AI chat state.

The editor supports:

- A normalized rectangular selection mapped between screen, viewport, and bitmap coordinates.
- Drawing a new selection, moving it, resizing from corners, and enforcing minimum touch and selection sizes.
- Panning and 1×–5× zoom while keeping the image reachable.
- Selecting an OCR line by tapping it.
- Extracting selected text from line centers inside the selection in reading order.
- Copy, Search, Translate, selected Lens, full-screen Lens, Retake, and Close actions.

The MiCTS flavor packages ML Kit's Latin and Chinese recognizers. `MlKitTextRecognitionGateway` downsizes only the analysis copy to a maximum 2048-pixel edge, runs the two recognizers sequentially, maps bounds back to the original bitmap, and merges overlapping output. The original bitmap remains available for cropping and sharing. No model download or network request is required for this OCR path.

VISTrigger supplies no-op factories and does not package the editor-specific OCR or AI dependencies.

## Temporary files and external actions

`CaptureFiles` maintains exactly one file at `cache/lens_capture/capture.png`. Preparing a new capture deletes the old file first.

- Closing, retaking, and successful text-only handoffs delete the capture.
- Search uses `ACTION_WEB_SEARCH`; if unavailable, the user can approve a Google web-search fallback.
- Translate uses `android.intent.action.TRANSLATE`; if unavailable, the user can approve a Google Translate web fallback.
- Lens shares a FileProvider URI only to `com.google.android.googlequicksearchbox` with temporary read permission.
- A selected-region Lens action replaces the cached full frame with the crop before sharing. If launch fails, the full frame is restored when possible.
- A successful Lens handoff may leave the temporary file available for the receiver. It is replaced by the next capture.

External launch failures preserve the editor and current selection. Search, Translate, browser, Lens, and Google apply their own privacy policies after a handoff.

## Optional AI flow

The AI feature exists only in the MiCTS flavor and is disabled by default.

When enabled, the user supplies an OpenAI-compatible base URL, model, and API key. `OpenAiCompatibleAiGateway` calls the endpoint's `/models` and `/chat/completions` routes. An initial prompt can include recognized text and a JPEG representation of the selected region; image sending can be disabled. Follow-up messages stay in memory for the current editor session and are not written as chat history.

The API key is stored through `AiKeyStorageFactory`. It attempts to use AndroidX `EncryptedSharedPreferences` backed by an AES-256-GCM master key. If encrypted storage initialization fails, it falls back to ordinary app-private `SharedPreferences`; documentation and UI must not claim encryption is guaranteed.

The MiCTS manifest permits cleartext traffic so users can connect to local or legacy compatible endpoints. Consequently, a user-configured `http://` endpoint can expose requests in transit. HTTPS is the safe default for remote endpoints.

No screenshot, recognized text, or chat content is sent to an AI endpoint until the user enables the feature, accepts the privacy notice, and explicitly requests an AI action.

## Privacy and security boundaries

- MediaProjection is requested only for fallback actions initiated by the user.
- No accessibility service, overlay permission, broad storage permission, or exported capture component is declared.
- The capture activity, foreground service, and FileProvider are not exported.
- Local OCR runs on the captured bitmap in the app process.
- The capture cache is private to the app except for a temporary FileProvider grant used for Lens.
- Clipboard data becomes available to Android and other components according to platform clipboard behavior.
- Secure-window content cannot be captured and normally produces a protected or blank-capture result.
- Avoiding accessibility does not guarantee compatibility with every banking, enterprise, or security-sensitive app.
- Native Circle to Search eligibility belongs to Google and cannot be inferred from Gemini, Android Auto, visible system features, or a successfully accepted invocation request.

## Configuration and migrations

`CapturePreferenceStore` uses schema version 2. Upgrading removes obsolete serialized-projection and armed-service keys. A legacy `FAST_ACCESSIBILITY` mode migrates to remembered consent on API 28–33 and ask-every-time on API 34 and later.

App language resources and Crowdin configuration remain part of the inherited application. Repository documentation is maintained in English only; untranslated strings for newer app features fall back to the default English resources.

## Build and verification

Common Gradle targets are:

```bash
./gradlew :app:assembleMiCTSDebug
./gradlew :app:assembleVISTriggerDebug
./gradlew :app:testMiCTSDebugUnitTest
./gradlew :app:testVISTriggerDebugUnitTest
```

CI assembles both release flavors with JDK 17. Unit tests cover trigger coordination, capture-mode migration and routing, OCR result merging, geometry, viewport and selection behavior, and AI request models. Instrumented Compose tests cover the primary fallback screens and editor states.

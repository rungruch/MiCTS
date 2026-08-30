# MiCTS Architecture

This document describes the maintained implementation of the personal MiCTS derivative at [rungruch/MiCTS](https://github.com/rungruch/MiCTS). Historical design plans and migration notes are intentionally consolidated here.

## Build and source-set layout

The repository contains one Android application module with two product flavors:

| Flavor | Application ID | Maintained role |
| --- | --- | --- |
| `MiCTS` | `com.parallelc.micts` | Standalone, no-root native Circle to Search trigger with a direct Google Lens fallback. |
| `VISTrigger` | `com.parallelc.vistrigger` | Separate non-root direct Voice Interaction Service trigger. |

Both flavors use a minimum SDK of 28, compile and target SDK 37, and a JDK 17 toolchain. The documented device-support range remains Android 9–16 because Android 17 behavior still requires physical-device validation.

Source ownership is explicit:

- `app/src/main` contains the shared language model, native trigger gateway, native-result type, theme, manifest shell, and Quick Settings tile.
- `app/src/MiCTS` contains the strategy settings, native-confirmation flow, one-shot capture implementation, encoding and temporary-file helpers, and direct Lens trampoline.
- `app/src/VISTrigger` contains only its direct VIS launch flow, lean settings repository and UI, and obsolete-preference migration.
- Flavor-specific unit and instrumented tests live under the corresponding `testMiCTS`, `testVISTrigger`, `androidTestMiCTS`, and `androidTestVISTrigger` source sets.

`AppConfig`, `MainApplication`, and activities are flavor-specific because the two apps have different settings and lifecycle needs. The shared `hiddenapibypass` dependency allows both ordinary app processes to call Android's hidden voice-interaction binder interface. It does not require root, module scopes, or an LSPosed service.

## Standalone MiCTS trigger flow

MiCTS `MainActivity` is a transparent trigger trampoline. It reads `TriggerPreferenceStore`, applies the configured launch or tile delay via non-blocking lifecycle coroutines, and asks `TriggerCoordinator` for one of four actions: invoke native, request native confirmation, request Lens capture, or finish.

### Auto: native first

1. With no remembered result, MiCTS calls `AndroidNativeTriggerGateway`.
2. The gateway invokes `IVoiceInteractionManagerService.showSessionFromSession` through `HiddenApiBypass`.
3. A rejected request routes directly to Lens capture and records the fallback result.
4. An accepted request is only `AcceptedUnverified`; MiCTS cannot observe Google's private eligibility or UI state.
5. On the next launch, MiCTS asks the user whether Circle to Search appeared.
6. The answer is stored as native-confirmed or fallback-confirmed and controls later Auto launches.

Native invocation errors fall back in Auto mode. Resetting Auto clears only the remembered resolution and causes the confirmation cycle to run again.

### Native only

MiCTS invokes the same binder path and exits. It shows a short failure notification when Android rejects the request or the hidden-API call fails. This strategy never starts MediaProjection.

### Google Lens fallback

MiCTS skips the native request and routes directly to capture consent. A successful capture opens `LensFallbackActivity`, which does not decode, crop, recognize, or edit the image. It grants the Google app temporary read access to the complete cached JPEG through `LensShareGateway` and an app-private FileProvider.

The retired `DIRECT_LENS` preference value is retained only for settings migration and maps to `LENS_FALLBACK`.

## MediaProjection consent and capture

`CapturePermissionCoordinator` implements the platform split:

| API level | Routing |
| --- | --- |
| 28–33 | Reuse a valid consent result from `ProjectionConsentStore`; otherwise show Android's MediaProjection dialog. |
| 34 and later | Always show Android's dialog because the consent token is single-use. |

`ProjectionConsentStore` is process memory only. No result code, permission `Intent`, virtual display, or projection instance is persisted to disk. Process death, reboot, token invalidation, or OEM policy therefore causes a new permission request. When a reused token fails immediately, the service clears it so the next trigger can recover by asking again.

Standalone MiCTS registers a non-exported `ScreenCaptureService` as a media-projection foreground service. Each invocation:

1. Clears and hides the transparent trigger UI.
2. Starts a fresh MediaProjection session for the default display.
3. Creates an `ImageReader` and virtual display.
4. Acquires one RGBA frame and extracts compact bitmap pixels via `PixelBufferExtractor` (single-pass direct copy when row padding is zero, and row-by-row buffer transfer when row padding is nonzero to avoid intermediate bitmap allocations).
5. Encodes the frame and checks for an empty or probably protected result.
6. Releases the reader, virtual display, callback, and MediaProjection.
7. Stops the foreground service and routes to the flavor's fallback activity.

The operation times out after seven seconds. Typed failures cover invalid permission data, foreground-service startup, projection shutdown, expired consent, timeout, empty images, write failure, and unknown errors.

`CapturePreferenceMigration` currently uses schema version 3. It deletes obsolete capture-mode, explanation, serialized projection-result, and armed-service keys while preserving unrelated preferences.

## Encoding and temporary storage

MiCTS `CaptureEncoding` keeps its JPEG format, filename, MIME type, and cleanup policy together: quality 90 at `cache/lens_capture/capture.jpg`.

Preparing a capture removes the previous file. MiCTS also removes the old PNG filename left by earlier versions. Cancel and retry actions delete the current capture. A successful Lens handoff may retain the app-private file long enough for the Google app to read it; the next capture replaces it.

Lens sharing uses `ACTION_SEND`, restricts the target package to `com.google.android.googlequicksearchbox`, and grants temporary read access only to the FileProvider URI. Standalone MiCTS does not declare Internet permission and does not upload the capture itself.

## VISTrigger direct VIS flow

VISTrigger remains a separate APK rather than a mode inside MiCTS. Its transparent `MainActivity` reads typed settings, selects the app or tile delay, waits in a lifecycle coroutine, and calls the shared native trigger gateway with entry point 0 (pure Voice Interaction Service request for Gemini Assistant without Circle to Search flags). Android acceptance optionally produces vibration; rejection or reflection failure produces a short failure message. The activity then exits.

VISTrigger has no strategy coordinator, native-result confirmation, MediaProjection permission, capture service, FileProvider, Lens gateway, crop editor, OCR/AI interface, system hooks, device spoofing, or Xposed metadata. Its settings are limited to app delay, tile delay, vibration, and language.

`VisTriggerPreferenceMigration` preserves those four settings when upgrading older installations and removes obsolete app-local strategy, capture, editor, and AI preferences. Former remote module configuration belongs to the removed LSPosed service boundary and is never accessed by the non-root app.

CI inspects both built APKs for Xposed metadata and libxposed classes. It additionally verifies that VISTrigger contains none of the removed module, editor, capture, or fallback classes and that only MiCTS registers MediaProjection and FileProvider components.

## Privacy and security boundaries

- Capture is initiated by a visible user action and Android consent when required.
- No flavor declares an accessibility service, overlay permission, broad storage permission, or Internet permission.
- MiCTS capture components and FileProvider are not exported.
- Secure-window content cannot be captured and normally appears as an empty or protected frame.
- Google receives a screenshot only through the explicit Lens fallback handoff and applies its own privacy and retention policies.
- Native request acceptance does not prove Circle to Search eligibility or successful UI display.
- Avoiding accessibility does not guarantee acceptance by banking, enterprise, or security-sensitive applications.
- VISTrigger executes only in its own ordinary application process and never receives a captured frame.
- Google and OEM updates can change hidden native services, eligibility, and Lens behavior independently of this project.

## Build and continuous verification

Common local checks are:

```bash
./gradlew \
  :app:testMiCTSDebugUnitTest \
  :app:testVISTriggerDebugUnitTest \
  :app:lintMiCTSDebug \
  :app:lintVISTriggerDebug \
  :app:assembleMiCTSDebug \
  :app:assembleVISTriggerDebug \
  --no-parallel
```

The GitHub Actions verification job uses JDK 17, runs both unit-test and lint suites, assembles both debug APKs, validates their package identities and non-root boundaries, and checks their manifest separation. A manually dispatched connected-test job supports emulator API levels 28, 33, 34, and 37.

Push and tag workflows also attempt minified release builds. They require all signing secrets, verify both APK signatures with `apksigner`, and upload workflow artifacts. Local release builds without the four Gradle signing properties are intentionally unsigned; the `sideload` build type is debug-signed.

# MiCTS Architecture

This document describes the maintained implementation of the personal MiCTS derivative at [rungruch/MiCTS](https://github.com/rungruch/MiCTS). Historical design plans and migration notes are intentionally consolidated here.

## Build and source-set layout

The repository contains one Android application module with two product flavors:

| Flavor | Application ID | Maintained role |
| --- | --- | --- |
| `MiCTS` | `com.parallelc.micts` | Standalone, no-root native Circle to Search trigger with a direct Google Lens fallback. |
| `VISTrigger` | `com.parallelc.vistrigger` | Separate legacy LSPosed/Xposed module and inherited compatibility code. |

Both flavors use a minimum SDK of 28, compile and target SDK 37, and a JDK 17 toolchain. The documented device-support range remains Android 9–16 because Android 17 and current libxposed behavior still require physical-device validation.

Source ownership is explicit:

- `app/src/main` contains the shared trigger models, preference stores, one-shot capture implementation, encoding policy, temporary-file helpers, theme, and Quick Settings tile.
- `app/src/MiCTS` contains the lean settings UI, no-root trigger gateway, native-confirmation flow, and direct Lens trampoline.
- `app/src/VISTrigger` contains the LSPosed module entry point, hook implementations, legacy settings, and editor-era code.
- Flavor-specific unit and instrumented tests live under the corresponding `testMiCTS`, `testVISTrigger`, `androidTestMiCTS`, and `androidTestVISTrigger` source sets.

`AppConfig` and several activities are flavor-specific so legacy root/editor configuration is not compiled into standalone MiCTS. The `hiddenapibypass` dependency remains shared because both native trigger gateways use Android's hidden voice-interaction binder interface. libxposed dependencies are added only to VISTrigger.

## Standalone MiCTS trigger flow

`MainActivity` is a transparent trigger trampoline. It reads `TriggerPreferenceStore`, applies the configured launch or tile delay, and asks `TriggerCoordinator` for one of four actions: invoke native, request native confirmation, request Lens capture, or finish.

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
4. Acquires one RGBA frame and removes row-padding pixels.
5. Encodes the frame and checks for an empty or probably protected result.
6. Releases the reader, virtual display, callback, and MediaProjection.
7. Stops the foreground service and routes to the flavor's fallback activity.

The operation times out after seven seconds. Typed failures cover invalid permission data, foreground-service startup, projection shutdown, expired consent, timeout, empty images, write failure, and unknown errors.

`CapturePreferenceMigration` currently uses schema version 3. It deletes obsolete capture-mode, explanation, serialized projection-result, and armed-service keys while preserving unrelated preferences.

## Encoding and temporary storage

`CaptureEncoding` derives the format, filename, MIME type, and cleanup policy from a flavor build flag:

| Flavor | Format | Quality | Cache file |
| --- | --- | --- | --- |
| MiCTS | JPEG | 90 | `cache/lens_capture/capture.jpg` |
| VISTrigger | PNG | Lossless | `cache/lens_capture/capture.png` |

Preparing a capture removes the previous file. MiCTS also removes the old PNG filename left by earlier versions. Cancel and retry actions delete the current capture. A successful Lens handoff may retain the app-private file long enough for the Google app to read it; the next capture replaces it.

Lens sharing uses `ACTION_SEND`, restricts the target package to `com.google.android.googlequicksearchbox`, and grants temporary read access only to the FileProvider URI. Standalone MiCTS does not declare Internet permission and does not upload the capture itself.

## VISTrigger isolation and legacy behavior

VISTrigger is a separate APK, not a root mode inside standalone MiCTS. Its flavor contains:

- libxposed metadata with API range 101–102.
- Scopes for the Android system, Xiaomi/POCO launchers, and the Google app.
- Inherited Xiaomi and Meizu navigation/Home hooks and Google-app device spoofing.
- Legacy trigger-service settings and a direct native binder gateway.
- Inherited crop/editor models, screens, and tests.

The legacy source tree should not be confused with a supported OCR or AI implementation. `TextRecognitionGatewayFactory` returns a successful empty result, while `AiGatewayFactory` fails with “AI is not supported in VISTrigger.” Neither ML Kit nor an HTTP client is included in the current dependency graph. Documentation therefore treats these editor-era interfaces as compatibility code rather than active product features.

CI enforces the important boundary in the other direction: the built MiCTS APK must contain no Xposed metadata and no VISTrigger hook, AI, OCR, crop-activity, crop-view-model, geometry, or recognition classes. The VISTrigger APK must retain its Xposed initialization metadata.

## Privacy and security boundaries

- Capture is initiated by a visible user action and Android consent when required.
- No flavor declares an accessibility service, overlay permission, broad storage permission, or Internet permission.
- MiCTS capture components and FileProvider are not exported.
- Secure-window content cannot be captured and normally appears as an empty or protected frame.
- Google receives a screenshot only through the explicit Lens fallback handoff and applies its own privacy and retention policies.
- Native request acceptance does not prove Circle to Search eligibility or successful UI display.
- Avoiding accessibility does not guarantee acceptance by banking, enterprise, or security-sensitive applications.
- VISTrigger executes hooks inside explicitly scoped processes and therefore carries the security and stability risks of root and LSPosed modules.
- Google and OEM updates can change native services, hook targets, eligibility, and Lens behavior independently of this project.

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

The GitHub Actions verification job uses JDK 17, runs both unit-test and lint suites, assembles both debug APKs, inspects MiCTS isolation, and confirms VISTrigger's Xposed metadata. A manually dispatched connected-test job supports emulator API levels 28, 33, 34, and 37.

Push and tag workflows also attempt minified release builds. They require all signing secrets, verify both APK signatures with `apksigner`, and upload workflow artifacts. Local release builds without the four Gradle signing properties are intentionally unsigned; the `sideload` build type is debug-signed.

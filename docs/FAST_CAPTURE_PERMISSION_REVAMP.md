# MiCTS Fast Capture Permission Revamp

This document records the Fast Capture implementation completed on 2026-08-27,
including its architecture, privacy boundaries, and real-device verification.

## Outcome

MiCTS now offers two persistent fallback capture methods:

- **Fast capture** uses Android's Accessibility screenshot API on Android 11+
  after the user enables the MiCTS service once.
- **Ask every time** creates a fresh, one-shot MediaProjection session for every
  fallback trigger.

Android 9–10 cannot use Accessibility screenshot capture, so MiCTS displays a
  one-time platform explanation and then uses MediaProjection on every trigger.
Native Circle to Search remains the primary Auto path. VisTrigger and LSPosed
hooks remain native-only.

## User flow

Existing and new installations start with `CaptureMode.UNSET`. The first Lens
fallback trigger opens the capture setup screen instead of immediately opening
Android's projection dialog.

On Android 11+ the setup screen:

1. Explains exactly what the screenshot-only service can and cannot do.
2. Recommends **Enable Fast capture**.
3. Offers **Ask every time instead** as a persistent privacy alternative.
4. On Android 13+, explains **Allow restricted settings** for direct installs
   and provides an App info shortcut.

After returning from Accessibility settings, MiCTS waits up to five seconds for
the service connection and captures immediately when ready. If the service is
later disabled or disconnected, MiCTS presents **Re-enable**, **Use once**, and
**Change capture method**. It never opens MediaProjection unexpectedly.

## Accessibility privacy boundary

`MiCTSAccessibilityCaptureService` is included only in the MiCTS flavor. Its
manifest service is protected by `BIND_ACCESSIBILITY_SERVICE`, and its metadata
declares:

- `canTakeScreenshot=true`
- `canRetrieveWindowContent=false`
- generic feedback only
- no Accessibility event types
- no gesture capability
- no Accessibility button
- no overlay or interactive-window flags

The connected service also forces empty event types and flags at runtime. It
takes a screenshot only after an explicit MiCTS launcher or Quick Settings
trigger.

## Capture implementation

The permission and routing behavior is separated into pure domain models:

- `CaptureMode`
- `FastCaptureAvailability`
- `CapturePermissionAction`
- `CapturePermissionCoordinator`

Android behavior is behind `FastCaptureGateway`. MiCTS wires this interface to
the Accessibility service; VisTrigger wires it to an unsupported stub so it
packages neither the service nor its metadata.

For each Fast Capture request, MiCTS:

1. Hides and moves the transparent trigger task behind the visible app.
2. Calls `AccessibilityService.takeScreenshot()` for the default display.
3. Retries `ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT` once after 500 ms.
4. Copies the hardware buffer into a software bitmap and closes all buffers and
   temporary bitmaps.
5. Reuses the one-PNG cache writer and protected/blank-frame detection.
6. Opens the existing smart editor with offline Latin and Chinese OCR.

The capture job is owned by a process-level supervised scope. This prevents
EMUI from cancelling a capture or its rate-limit retry when it trims the
transparent launcher task after that task is moved behind the current app.

The MediaProjection alternative remains one-shot. It always requests new
consent, uses one virtual display, captures one frame, releases the projection,
and stops its foreground service. Android 14+ requests default-display capture
and never reuses a consent intent or projection token.

## Preference migration

`CapturePreferenceStore` performs a one-time schema migration that:

- removes obsolete projection result-code, projection-intent, and armed-service
  keys;
- initializes the capture mode to `UNSET`;
- preserves trigger strategy, Auto resolution, OCR, and unrelated settings.

Settings now shows the selected capture method and whether Fast Capture is
supported, enabled, and connected. The compatibility report exposes these
states without claiming access to Google's private Circle to Search entitlement.

## Failure handling

Fast Capture maps Android failures to explicit app states for:

- Accessibility disabled or disconnected;
- screenshot rate limiting;
- secure windows;
- invalid displays/windows;
- internal screenshot errors;
- empty or unwritable captures.

Partial cache files are removed on failure. Secure or blank captures retain the
existing protected-content explanation.

## Tests and build verification

Added unit coverage for API 28, 29, 30, 31, 33, 34, and 36 routing, persistent
capture choices, preference migration, and restoration. Added Compose coverage
for initial setup, Android 9–10 explanation, connecting, recovery, restricted
settings help, and explicit action callbacks.

The following completed successfully with JDK 17:

- MiCTS and VisTrigger unit tests
- MiCTS and VisTrigger Android-test Kotlin compilation
- MiCTS and VisTrigger lint
- MiCTS and VisTrigger release minification and assembly

APK inspection confirmed:

- MiCTS contains one Accessibility screenshot service and bundled Latin/Chinese
  OCR models.
- VisTrigger contains zero Accessibility service entries and zero OCR model
  entries.
- Both flavors continue to compile their native trigger paths.

## Huawei verification

Validated on a Huawei SGT-LX9 running EMUI 15 / Android API 31:

- enabled Fast Capture once through Huawei Accessibility settings;
- confirmed the bound MiCTS service exposes only
  `CAPABILITY_CAN_TAKE_SCREENSHOT`, with empty event types and no retrieval
  flags;
- captured launcher content repeatedly without another projection dialog;
- opened the smart editor with offline Latin and Chinese OCR results;
- validated normal launcher and tile-style triggers;
- confirmed `dumpsys media_projection` remained `null` after every Fast Capture;
- found and fixed EMUI retaining MiCTS's confirmation dialog in the captured
  compositor frame;
- installed the signed, minified MiCTS `1.0 (5)` release with the already
  enabled service preserved;
- left the installed VisTrigger `2.6 (66)` package untouched.

The installed and release MiCTS certificates matched SHA-256 signer digest
`964e19f724c911f87c1e8abcec8f6a3230aef6ac3a2d8a672d9e78b8541636ee`.

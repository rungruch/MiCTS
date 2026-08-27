# MiCTS Screen Capture Architecture & Consent Revamp

This document records the screen capture architecture refactor completed on 2026-08-27, replacing the experimental Accessibility Service approach with a clean, no-root MediaProjection consent model.

## Rationale: Removal of Accessibility Service

An Accessibility Service approach (`takeScreenshot()`) was previously explored to bypass repeated MediaProjection prompts. However, this introduced critical drawbacks:
1. **Security & Banking App Conflicts**: Many banking, fintech, and enterprise apps detect active Accessibility Services as potential keyloggers/screen-scrapers and refuse to run or block transactions.
2. **Platform Inconsistencies**: Android 9–10 did not support the Accessibility screenshot API, creating fragmented behavior.
3. **Restricted Settings Barriers**: On Android 13+, side-loaded/direct-install apps require complex user navigation through "Allow restricted settings".

By completely eliminating Accessibility Services from the project, MiCTS guarantees **zero banking app detection** while preserving seamless screen capture.

## Capture Architecture

MiCTS separates capture routing cleanly by Android platform capabilities:

| Android Version | Capture Strategy | User Experience |
| --- | --- | --- |
| **Android 9–13 (API 28–33)** | `REMEMBER_CONSENT` (`ProjectionConsentStore`) | Consent dialog shown **once** on setup. Subsequent triggers capture silently without prompts. |
| **Android 14+ (API 34+)** | `ASK_EVERY_TIME` | Platform enforces single-use tokens by design. Prompts dialog before each capture after a one-time explanation. |

### 1. Reusable Consent without Fragile Armed Services (Android ≤ 13)
Earlier iterations kept a long-lived foreground service with a persistent `MediaProjection` instance ("armed service"). This suffered from race conditions, state drift, and process death.

The current architecture avoids that fragility:
- When permission is granted, `MainActivity` serializes the consent `Intent` via `Parcel` (marshalled to Base64) into private `SharedPreferences` (`ProjectionConsentStore`).
- On each capture trigger, MiCTS starts a fresh, **one-shot** `ScreenCaptureService` using the stored consent.
- `ScreenCaptureService` acquires a single frame, writes it to temporary cache, unregisters callbacks, releases the `VirtualDisplay` and `MediaProjection`, and terminates itself immediately (`stopSelf()`).
- **Self-Healing Token Expiry**: If Android invalidates the stored token (e.g. following a device reboot or OEM policy), `ScreenCaptureService` catches the failure or early `onStop()`, clears `ProjectionConsentStore`, and emits `CaptureFailureReason.CONSENT_EXPIRED`. The next trigger cleanly prompts for consent again.

### 2. Android 14+ Handling (API ≥ 34)
Android 14 explicitly throws a `SecurityException` if a MediaProjection token is reused across sessions.
- `CapturePreferenceStore` and `CapturePermissionCoordinator` recognize API ≥ 34 and route requests to `CaptureMode.ASK_EVERY_TIME`.
- `ConsentExplanationScreen` is presented once to inform the user why the prompt appears on each trigger, followed by the native system screen-capture dialog.

## Preference Schema Migration (Schema Version 2)

`CapturePreferenceStore` implements an automatic migration when upgrading from older schemas:
- Obsolete keys (`projection_result_code`, `projection_result_data`, `capture_armed`) are purged.
- Any legacy `FAST_ACCESSIBILITY` mode setting is migrated automatically:
  - On API < 34: Migrated to `CaptureMode.REMEMBER_CONSENT`.
  - On API ≥ 34: Migrated to `CaptureMode.ASK_EVERY_TIME`.
- Schema is bumped to version 2 (`KEY_CAPTURE_PERMISSION_SCHEMA = 2`).

## Verification & Compatibility

- **Zero Accessibility Footprint**: Verified that neither `MiCTS` nor `VISTrigger` manifests or source sets contain accessibility services, permissions (`BIND_ACCESSIBILITY_SERVICE`), or resource descriptors.
- **Unit & Instrumented Tests**:
  - `CapturePreferenceStoreTest`: Validates initial defaults, schema migration (mapping legacy modes on API 33 vs API 34), and cleanup of deprecated keys.
  - `CapturePermissionCoordinatorTest`: Validates permission actions for API 28–36, stored token handling, and single-use enforcement on Android 14+.
  - `FallbackUiTest`: Validates Compose UI flows for `CaptureSetupScreen` and `ConsentExplanationScreen`.

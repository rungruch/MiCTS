# MiCTS Screen Capture Architecture & Consent Revamp

This document records the screen capture architecture refactor completed on 2026-08-27, replacing the experimental Accessibility Service approach with a clean, no-root MediaProjection consent model. The standalone `MiCTS` build uses this capture only for its full-screen Google Lens fallback; the separate `VISTrigger` build remains the legacy LSPosed module.

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
| **Android 9–13 (API 28–33)** | Automatic reuse (`ProjectionConsentStore`) | Consent dialog shown **once per app process**. Subsequent triggers capture silently while the process remains alive. |
| **Android 14+ (API 34+)** | Fresh system consent | Platform enforces single-use tokens by design. The system dialog opens before each capture. |

### 1. Reusable Consent without Fragile Armed Services (Android ≤ 13)
Earlier iterations kept a long-lived foreground service with a persistent `MediaProjection` instance ("armed service"). This suffered from race conditions, state drift, and process death.

The current architecture avoids that fragility:
- When permission is granted, `MainActivity` keeps the consent `Intent` in the process-scoped `ProjectionConsentStore`; it is never written to disk.
- On each capture trigger while that process is alive, MiCTS starts a fresh, **one-shot** `ScreenCaptureService` using the in-memory consent.
- `ScreenCaptureService` acquires a single frame, writes it to temporary cache, unregisters callbacks, releases the `VirtualDisplay` and `MediaProjection`, and terminates itself immediately (`stopSelf()`).
- **Self-Healing Token Expiry**: Process death naturally discards the token. If Android invalidates a live token (for example through OEM policy), `ScreenCaptureService` catches the failure or early `onStop()`, clears `ProjectionConsentStore`, and emits `CaptureFailureReason.CONSENT_EXPIRED`. The next trigger cleanly prompts for consent again.

### 2. Android 14+ Handling (API ≥ 34)
Android 14 explicitly throws a `SecurityException` if a MediaProjection token is reused across sessions.
- `CapturePermissionCoordinator` recognizes API ≥ 34 and always routes directly to the native system screen-capture dialog.
- No app-owned setup or explanation screen appears before Android's consent UI.

## Preference Schema Migration (Schema Version 3)

`CapturePreferenceMigration` runs at application startup when upgrading from older schemas:
- Obsolete keys (`capture_mode`, `legacy_capture_explainer_seen`, `projection_result_code`, `projection_result_data`, and `capture_armed`) are purged.
- Unrelated application settings are preserved.
- The private capture-permission schema is bumped to version 3.

## Verification & Compatibility

- **Zero Accessibility Footprint**: Verified that neither `MiCTS` nor `VISTrigger` manifests or source sets contain accessibility services, permissions (`BIND_ACCESSIBILITY_SERVICE`), or resource descriptors. Only `VISTrigger` contains LSPosed/Xposed metadata and hooks.
- **Unit & Instrumented Tests**:
  - `CapturePreferenceMigrationTest`: Validates schema migration and cleanup of deprecated keys without affecting unrelated settings.
  - `CapturePermissionCoordinatorTest`: Validates permission actions for API 28–37, in-memory consent reuse, and single-use enforcement on Android 14+.
  - `FallbackUiTest`: Validates the remaining denial/retry and fallback UI flows.

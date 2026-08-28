# MiCTS Lean Build

## Summary

- `MiCTS` is the standalone build: native Circle to Search first, then a full-screen Google Lens fallback.
- `VISTrigger` remains the separate legacy LSPosed/Xposed build.
- `MiCTS` keeps the `com.parallelc.micts` package; `VISTrigger` uses `com.parallelc.vistrigger`.
- The standalone build has no local OCR, AI assistant, crop editor, root hooks, device spoofing, or Xposed metadata.

## Standalone behavior

- The app invokes the existing direct voice-interaction binder path for native Circle to Search.
- Auto mode asks whether native Circle to Search appeared and remembers the answer.
- Lens fallback uses one MediaProjection capture and shares the complete temporary PNG directly with the Google app.
- Android 14+ capture consent remains per-trigger; Android 9–13 may reuse remembered consent.

## Verification

- Build both `MiCTS` and `VISTrigger` variants with JDK 17.
- Confirm the MiCTS APK contains no Xposed metadata/classes, ML Kit, OCR gateways, AI gateways, or accessibility components.
- Confirm VISTrigger still contains its module metadata and hook implementations.
- Test native confirmation, Lens fallback, permission denial, protected content, Lens unavailability, retry, cancellation, and temporary-file cleanup.

## Status (2026-08-28)

- Both debug flavors build with JDK 17 (Corretto 17); unit tests pass for both flavors.
- MiCTS release APK builds (`assembleMiCTSRelease`), R8-minified, verified Xposed-free.
- Release APK `MiCTS_1.0_9_MiCTSRelease.apk` installed on Samsung Galaxy Tab S5e (SGT-L29) via adb.
- Note: release APK is currently signed with the debug certificate fallback (no keystore configured).

## Measured trigger-to-Lens latency (Tab S5e, 2026-08-28)

Timeline captured from logcat on a real trigger:

| Stage | Duration | Owner |
| --- | --- | --- |
| Trigger → MediaProjection ready | ~194 ms | MiCTS (service start + projection setup) |
| First frame + full-res PNG encode + write | ~276 ms | MiCTS |
| Trampoline activity → `ACTION_SEND` | ~23 ms | MiCTS |
| Google app → Lens first frame drawn | ~134 ms | Google app (warm process) |
| **Total trigger → Lens visible** | **~650 ms** | |

Findings:

- There is no artificial delay between capture and opening Lens anywhere in the code path.
- The pre-trigger delay slider (0–2000 ms) runs before the trigger and is skipped for the forced-Lens path; it does not apply here.
- The dominant variable is the Google app process state: warm process draws Lens in ~150 ms; a cold process (OEM battery managers often kill it) adds seconds. That portion is app-side and not controllable from MiCTS.

## Plan 1: Faster capture encoding (JPEG)

PNG at quality 100 for a full-resolution frame is the slowest MiCTS-side step and scales with screen size. Switch the fallback capture to JPEG.

- `app/src/main/java/com/parallelc/micts/capture/BitmapCaptureWriter.kt`: `Bitmap.CompressFormat.PNG` → `Bitmap.CompressFormat.JPEG`, quality ~90.
- `app/src/main/java/com/parallelc/micts/data/LensShareGateway.kt`: share intent MIME `image/png` → `image/jpeg`.
- `app/src/main/java/com/parallelc/micts/data/CaptureFiles.kt`: capture file extension `.png` → `.jpg` (and any cleanup paths referencing it).
- Check `app/src/test/` for tests asserting the capture file name/MIME and update.
- Expected result: capture stage drops from ~276 ms to roughly 60–100 ms; JPEG quality 90 is sufficient for Lens OCR and visual search.
- Risk: low. If a `Bitmap.CompressFormat` nullability warning appears on some API levels, suppress explicitly (the API is null-safe in practice when passed a valid format).

## Plan 2: Release signing

The `apksign` plugin falls back to the debug certificate when signing properties are missing.

- Add to `~/.gradle/gradle.properties` (do not commit): `androidStoreFile`, `androidStorePassword`, `androidKeyAlias`, `androidKeyPassword`.
- Re-run `./gradlew :app:assembleMiCTSRelease` and verify with `apksigner verify --print-certs`.
- Uninstall the debug-signed build before installing the release-keyed build (signature mismatch blocks upgrade and resets preferences).

## Plan 3: Instrumented tests

Not yet run; require a connected device/emulator.

- `./gradlew :app:connectedMiCTSDebugAndroidTest :app:connectedVISTriggerDebugAndroidTest`
- Cover: native confirmation dialog, Lens fallback happy path, permission denial, protected-content capture, Lens-unavailable dialog, retake/cancel cleanup of the temporary capture file.

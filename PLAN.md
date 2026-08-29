# MiCTS Lean Build

## Summary

- `MiCTS` is the standalone build: native Circle to Search first, then a full-screen Google Lens fallback.
- `VISTrigger` remains the separate legacy LSPosed/Xposed build.
- `MiCTS` keeps the `com.parallelc.micts` package; `VISTrigger` uses `com.parallelc.vistrigger`.
- The standalone build has no local OCR, AI assistant, crop editor, root hooks, device spoofing, or Xposed metadata.

## Standalone behavior

- The app invokes the existing direct voice-interaction binder path for native Circle to Search.
- Auto mode asks whether native Circle to Search appeared and remembers the answer.
- Lens fallback uses one MediaProjection capture and shares the complete temporary JPEG directly with the Google app.
- Android 14+ capture consent remains per-trigger; Android 9–13 may reuse consent only while the app process remains alive.

## Verification

- Build both `MiCTS` and `VISTrigger` variants with JDK 17.
- Confirm the MiCTS APK contains no Xposed metadata/classes, ML Kit, OCR gateways, AI gateways, or accessibility components.
- Confirm VISTrigger still contains its module metadata and hook implementations.
- Test native confirmation, Lens fallback, permission denial, protected content, Lens unavailability, retry, cancellation, and temporary-file cleanup.

## Status (2026-08-28)

- Both debug flavors build with JDK 17; unit tests pass for both flavors.
- MiCTS release APK builds (`assembleMiCTSRelease`), is R8-minified, and is verified Xposed-free.
- Release signing uses standard AGP credentials from Gradle properties or CI environment variables. A local release build without those credentials is intentionally unsigned; tag CI fails instead of falling back to a debug certificate.

## Measured trigger-to-Lens latency (Huawei SGT-LX9, 2026-08-28)

Timeline captured from logcat on a real trigger:

| Stage | Duration | Owner |
| --- | --- | --- |
| Trigger → MediaProjection ready | ~194 ms | MiCTS (service start + projection setup) |
| First frame + full-res JPEG encode + write | ~276 ms | MiCTS |
| Trampoline activity → `ACTION_SEND` | ~23 ms | MiCTS |
| Google app → Lens first frame drawn | ~134 ms | Google app (warm process) |
| **Total trigger → Lens visible** | **~650 ms** | |

Findings:

- There is no artificial delay between capture and opening Lens anywhere in the code path.
- The pre-trigger delay slider (0–2000 ms) runs before the trigger and is skipped for the forced-Lens path; it does not apply here.
- The dominant variable is the Google app process state: warm process draws Lens in ~150 ms; a cold process (OEM battery managers often kill it) adds seconds. That portion is app-side and not controllable from MiCTS.

## Plan 1: Faster capture encoding (JPEG) — implemented

MiCTS now encodes the Lens-only fallback as JPEG at quality 90. VISTrigger keeps its lossless PNG editor/OCR input.

- A flavor flag selects one shared capture policy so the bitmap format, quality, cache filename, and share MIME remain consistent.
- MiCTS writes `capture.jpg`, shares `image/jpeg`, and removes both the current JPEG and a legacy `capture.png` during cleanup.
- VISTrigger continues to write `capture.png` and share `image/png`.
- Flavor unit tests and focused encoding/cleanup instrumentation tests pass, including JPEG/PNG signature and decode checks.
- Android ignores the quality argument for lossless PNG; the speedup comes from switching MiCTS to lossy JPEG, not from changing PNG quality.
- Ten complete warm physical-device trials measured median stages of 173 ms trigger → MediaProjection start, 123 ms projection start → JPEG `ACTION_SEND`, 118 ms send → Lens displayed, and 413 ms total. The middle stage includes the prior ~23 ms trampoline/routing cost, putting capture/encoding near 100 ms.
- Compared with the earlier ~650 ms end-to-end measurement, the new 413 ms median is about 36% faster and passes the 30% acceptance gate. Google Lens displayed its post-capture “Select text” UI, confirming JPEG acceptance and processing.

## Plan 2: Release signing (completed)

Standard AGP signing replaces the former `apksign` plugin. Provide `androidStoreFile`, `androidStorePassword`, `androidKeyAlias`, and `androidKeyPassword` through uncommitted Gradle properties or CI environment variables.

- Local release builds without all four properties are intentionally unsigned; no debug-certificate fallback exists.
- Main and tag CI builds require the credentials, verify both APKs with `apksigner`, rename from `output-metadata.json`, and upload the artifacts.
- A test release keystore has verified the standard AGP signing path and both flavor signatures.

## Plan 3: Instrumented tests

- The MiCTS JPEG and VISTrigger PNG encoding tests pass on the Pixel Android 17 preview emulator.
- The MiCTS JPEG encoding, decode, MIME, and legacy-cleanup tests also pass on the physical Huawei SGT-LX9 (Android 12/API 31), and the release build completes a real JPEG handoff to Google Lens.
- The flavor-specific test directories use Gradle's `androidTest<Flavor>`/`test<Flavor>` convention, so both flavor test suites execute rather than silently producing zero-test reports.
- MiCTS has Compose coverage for native confirmation, permission denial, protected-content messaging, and Lens-unavailable fallback. VISTrigger retains its crop/editor coverage.
- The manual emulator workflow runs connected tests at API 28, 33, 34, or 37; real-device Android 17 and libxposed 101/102 validation remains required before documenting Android 17 support.

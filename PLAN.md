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

# MiCTS Lean Build Handover

Date: 2026-08-29
Branch: `lean`

## Status

The lean implementation and Android 17/API 37 toolchain upgrade are complete locally. Both flavors assemble with JDK 17; CI now verifies tests, lint, APK isolation, signed release artifacts, and manual emulator runs.

## Build matrix

- `MiCTS` (`com.parallelc.micts`): standalone, no-root build.
- `VISTrigger` (`com.parallelc.vistrigger`): separate legacy LSPosed/Xposed module build.

Installing the lean `MiCTS` APK over an older MiCTS module replaces the old `com.parallelc.micts` package. Install `VISTrigger` separately when the legacy hooks are still needed.

## MiCTS behavior

- Uses the existing direct voice-interaction binder path for native Circle to Search.
- `Auto` tries native first, asks for confirmation, and remembers the result.
- `Native only` never captures the screen.
- `Lens fallback` uses one MediaProjection capture and sends the complete temporary JPEG directly to Google Lens.
- The fallback has no local OCR, crop editor, AI assistant, root trigger hooks, device spoofing, or Xposed metadata.
- The direct hidden-API binder implementation and `hiddenapibypass` dependency are intentionally retained.

## Source layout

- Lean app code is under `app/src/MiCTS/`.
- Legacy module code, Xposed metadata, and AI/OCR editor interfaces are under `app/src/VISTrigger/`; its tests are under `app/src/testVISTrigger/` and `app/src/androidTestVISTrigger/`.
- MiCTS flavor tests are under `app/src/testMiCTS/` and `app/src/androidTestMiCTS/`.
- Shared capture and preference code remains under `app/src/main/`.
- `AppConfig` is flavor-specific so legacy AI/OCR settings are not compiled into MiCTS.

## Verification completed

- `git diff --check` passes.
- `:app:testMiCTSDebugUnitTest` and `:app:testVISTriggerDebugUnitTest` pass (25 and 50 tests respectively).
- Both flavor lint tasks report no new issues, and both debug and minified release APKs assemble successfully.
- APK inspection confirms MiCTS contains no Xposed metadata/classes or VISTrigger editor code, while VISTrigger retains its Xposed entry metadata.
- Standard AGP release signing has been verified with `apksigner`; local unsigned releases are intentional when signing properties are absent.

```text
./gradlew :app:testMiCTSDebugUnitTest :app:testVISTriggerDebugUnitTest :app:lintMiCTSDebug :app:lintVISTriggerDebug :app:assembleMiCTSDebug :app:assembleVISTriggerDebug --no-parallel
```

## Next steps

1. Run the manual emulator workflow for API 28, 33, 34, and 37.
2. Complete the real-device Android 17 and libxposed 101/102 framework checks before documenting Android 17 support.

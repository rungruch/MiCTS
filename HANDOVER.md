# MiCTS Lean Build Handover

Date: 2026-08-28
Branch: `lean`

## Status

The lean implementation is complete. APK assembly is not verified because this host has no Java/JDK runtime; Gradle exits before project configuration with `Unable to locate a Java Runtime`.

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
- Legacy module code, Xposed metadata, AI/OCR editor interfaces, and legacy tests are under `app/src/VISTrigger/`, `app/src/VISTriggerTest/`, and `app/src/VISTriggerAndroidTest/`.
- Shared capture and preference code remains under `app/src/main/`.
- `AppConfig` is flavor-specific so legacy AI/OCR settings are not compiled into MiCTS.

## Verification completed

- All XML files pass `xmllint --noout`.
- `git diff --check` passes.
- Static checks confirm MiCTS has no Xposed/libxposed, ML Kit, OCR gateway, AI gateway, accessibility component, internet permission, or Xposed metadata references.
- The attempted build command was:

```text
./gradlew :app:assembleMiCTSDebug :app:assembleVISTriggerDebug
```

## Next steps

1. Install/configure JDK 17.
2. Run the build command above.
3. Run the relevant unit and instrumented tests for both flavors.
4. Inspect both APKs to confirm only VISTrigger contains Xposed metadata and hook classes.

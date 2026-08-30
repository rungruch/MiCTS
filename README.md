# MiCTS

[![Build, verify, and test](https://github.com/rungruch/MiCTS/actions/workflows/ci_build.yml/badge.svg)](https://github.com/rungruch/MiCTS/actions/workflows/ci_build.yml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Android 9–16](https://img.shields.io/badge/Android-9–16-3DDC84?logo=android&logoColor=white)](#requirements)

MiCTS is a small Android utility that tries to open native Circle to Search without root. When Google does not enable the native interface, the standalone app can capture one full-screen frame and hand it directly to Google Lens.

> [!IMPORTANT]
> This repository is an independently maintained personal derivative of [parallelcc/MiCTS](https://github.com/parallelcc/MiCTS). It is not an official upstream build, is not affiliated with Google, and is maintained for this personal project rather than for upstream pull requests.

## Highlights

- Supports Android 9–16 (API 28–36) as the documented device range.
- Offers Auto, native-only, and full-screen Google Lens fallback strategies.
- Can be launched from the app icon, a Quick Settings tile, or an external automation that opens the app.
- Uses Android's voice-interaction binder path for the standalone native trigger.
- Uses a one-shot MediaProjection service for Lens fallback capture.
- Declares no accessibility service.
- Keeps root and LSPosed functionality in a separate `VISTrigger` flavor.

The standalone MiCTS flavor deliberately does **not** include a crop editor, local OCR, an AI assistant, root hooks, device spoofing, or Xposed metadata. Native Circle to Search eligibility remains controlled by Google, the installed Google app, device configuration, region, account, and OEM software.

## Builds

| Flavor | Application ID | Purpose |
| --- | --- | --- |
| `MiCTS` | `com.parallelc.micts` | Standalone no-root native trigger with a direct, full-screen Lens fallback. |
| `VISTrigger` | `com.parallelc.vistrigger` | Separate legacy LSPosed/Xposed module for users who still need root hooks. |

Installing the standalone flavor over an older MiCTS module replaces that installation because the package name is unchanged. Install `VISTrigger` separately if you still need the legacy hooks.

This personal repository currently does not publish release APKs. Build from source using [Building from source](#building-from-source).

## Requirements

For standalone MiCTS:

- Android 9–16.
- The latest available [Google app](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox).
- Google configured as the default assistant for the native voice-interaction trigger.
- Background and battery restrictions disabled for the Google app when the device vendor would otherwise delay or stop it.

For the separate VISTrigger module, root and a compatible LSPosed installation are also required. Hook behavior depends on Android, the OEM framework, the Google app, launcher versions, and the installed LSPosed framework.

Android 17/API 37 is used by the build toolchain, but Android 17 device support is not claimed until it has been validated on physical devices and current LSPosed frameworks.

## Getting started

1. Install the MiCTS flavor and open it once.
2. Keep **Auto: native first** selected unless you already know that you want native-only or Lens-only behavior.
3. MiCTS tries the native request. On the next launch, it asks whether the native interface appeared.
4. Choose **Yes, keep native** if Circle to Search worked, or **No, use Lens fallback** if it did not.
5. Approve Android's screen-capture dialog when a Lens fallback needs it.
6. Optionally add the MiCTS tile to Quick Settings or configure another automation to launch MiCTS.

Open settings by long-pressing the app icon and choosing **Settings**, or by long-pressing the Quick Settings tile.

## Trigger strategies

| Strategy | Behavior |
| --- | --- |
| **Auto: native first** | Tries native Circle to Search. If Android rejects the request, MiCTS falls back immediately. If Android accepts the request, MiCTS asks on the next launch whether Google's interface actually appeared and remembers the answer. |
| **Native Circle to Search only** | Requests native Circle to Search and exits. It never captures the screen as a fallback. |
| **Google Lens fallback** | Skips the native request, captures one complete screen frame, and shares it directly with the Google app's Lens interface. |

MiCTS cannot inspect Google's private Circle to Search eligibility or reliably detect whether Google's UI appeared. An accepted binder request is therefore treated as unverified until the user confirms the result.

## Capture permission behavior

The Lens fallback uses Android MediaProjection and a short-lived foreground service. It captures one frame, writes the temporary image, releases the virtual display and projection resources, and stops.

| Android version | Consent behavior |
| --- | --- |
| Android 9–13 (API 28–33) | A successful consent token can be reused for later one-shot captures while the MiCTS process and token remain valid. Process death, reboot, or platform/OEM invalidation requires approval again. |
| Android 14–16 (API 34–36) | MediaProjection consent tokens are single-use, so Android shows the system approval dialog before every fallback capture. |

MiCTS stores consent only in process memory; it does not serialize MediaProjection permission data to disk or keep a permanently armed capture service. Permission denial, expired consent, capture timeout, protected content, write failure, and unavailable Lens produce a retry or explanatory screen.

## Standalone settings

- Default app-launch trigger delay.
- Quick Settings tile trigger delay.
- Vibration on a successful native request.
- Auto, native-only, or Lens fallback strategy.
- Resetting Auto's remembered result.
- App interface language with Android 13+ Per-App Language integration.

## VISTrigger legacy module

`VISTrigger` keeps the inherited LSPosed/Xposed entry point and legacy hooks isolated from the standalone APK. Its module scopes include the Android system framework, Xiaomi/POCO launchers, and the Google app. Depending on the device and Android version, the legacy settings expose VIS, contextual-search service helpers, Xiaomi gesture hooks, Home-button hooks, and Google-app device spoofing.

Use only the scopes needed for the selected feature. Root hooks and spoofing can break after Android, OEM, Google app, launcher, or LSPosed updates, and this project cannot guarantee that Google will enable Circle to Search.

The VISTrigger source set also retains legacy crop/editor interfaces for compatibility. Its local-recognition gateway currently returns no recognized text and its AI gateway reports that AI is unsupported. Those paths are not advertised as maintained VISTrigger features.

## Privacy and security boundaries

- Standalone MiCTS captures only after a user trigger and Android approval when required.
- It keeps at most one temporary JPEG at `cache/lens_capture/capture.jpg`; a new capture removes the previous JPEG and legacy PNG.
- MiCTS does not upload the image itself. A successful fallback grants the Google app temporary read access to the captured frame, after which Google's privacy and retention policies apply.
- Cancel and retry flows remove the temporary capture. A successful Lens handoff may leave it in app-private cache until the next capture replaces it.
- MiCTS declares no accessibility service, overlay permission, broad storage permission, or Internet permission.
- Android secure-window protection can produce an empty or blocked capture that MiCTS cannot bypass.
- Avoiding accessibility does not guarantee acceptance by every banking, enterprise, or security-sensitive app.
- The VISTrigger flavor has additional root/Xposed trust boundaries because its configured hooks execute inside scoped processes.

## Troubleshooting

### “Trigger failed!” appears

Confirm that the Google app is installed, updated, allowed to run in the background, and configured as the default assistant. If you are using VISTrigger, also verify that LSPosed is active and the required scopes are selected.

### Google Assistant opens instead of Circle to Search

The native request reached Google's assistant path, but Google did not expose Circle to Search for the current configuration. Update the Google app, then use the Lens fallback if native eligibility remains unavailable.

### Logcat contains `Omni invocation failed: not enabled`

Google received the native request but declined to enable its native interface. Standalone MiCTS cannot override that decision. Select the Lens fallback, or review VISTrigger's root-based options with an understanding of their risks.

### Native UI appears only after opening the Google app manually

Remove background and battery restrictions from the Google app. Vendor process management can make a cold Google app launch much slower.

### Android asks for capture permission again

This is expected on Android 14 and later. On Android 9–13, approval lasts only while Android keeps the in-memory token valid; process death, reboot, or OEM policy can invalidate it.

### The captured image is blank

The visible app may protect its window from screenshots. MiCTS cannot capture protected content.

### Lens does not open

Install or update the Google app and confirm that it can receive image shares. MiCTS keeps the failure screen available so you can retry or cancel.

## Building from source

Prerequisites:

- JDK 17.
- Android SDK with API 37 installed.
- ADB if you want to install from the command line.

Clone this personal repository and build both debug flavors:

```bash
git clone https://github.com/rungruch/MiCTS.git
cd MiCTS
./gradlew :app:assembleMiCTSDebug :app:assembleVISTriggerDebug --no-parallel
```

The APKs are written under:

- `app/build/outputs/apk/MiCTS/debug/`
- `app/build/outputs/apk/VISTrigger/debug/`

Run the flavor unit tests and lint checks with:

```bash
./gradlew \
  :app:testMiCTSDebugUnitTest \
  :app:testVISTriggerDebugUnitTest \
  :app:lintMiCTSDebug \
  :app:lintVISTriggerDebug \
  --no-parallel
```

Release builds use standard Android Gradle Plugin signing properties: `androidStoreFile`, `androidStorePassword`, `androidKeyAlias`, and `androidKeyPassword`. Without all four values, local release APKs are intentionally unsigned. The `sideload` build type uses the debug certificate and is not a production release signature.

For implementation details, see [Architecture](docs/ARCHITECTURE.md).

## Project origin and license

Thanks to the original project and its contributors at [parallelcc/MiCTS](https://github.com/parallelcc/MiCTS). This derivative retains the inherited MiCTS name, application IDs, package names, and GPL-3.0 license while maintaining its own direction at [rungruch/MiCTS](https://github.com/rungruch/MiCTS).

Distributed under the [GNU General Public License v3.0](LICENSE).

# MiCTS

[![CI Build](https://github.com/rungruch/MiCTS/actions/workflows/ci_build.yml/badge.svg)](https://github.com/rungruch/MiCTS/actions/workflows/ci_build.yml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Android 9–16](https://img.shields.io/badge/Android-9–16-3DDC84?logo=android&logoColor=white)](#requirements)

MiCTS is an Android utility that tries to launch native Circle to Search and provides a private, user-triggered screen-search fallback when native support is unavailable. The fallback combines a one-frame screen capture, local text recognition, region selection, Google Lens handoff, and an optional user-configured AI assistant.

> [!IMPORTANT]
> This repository is an independently maintained personal derivative of [parallelcc/MiCTS](https://github.com/parallelcc/MiCTS). It is not an official upstream build and is not affiliated with Google. Development here is intended for this personal project rather than for upstream pull requests.

## Highlights

- Targets Android 9–16 (API 28–36).
- Tries native Circle to Search first, with a smart fallback for devices that Google does not enable.
- Offers launcher, Quick Settings tile, and optional LSPosed/Xiaomi gesture triggers.
- Recognizes Latin and Chinese text locally with bundled ML Kit models.
- Supports text selection, Copy, Search, Translate, selected-region Lens, and full-screen Lens.
- Includes an optional OpenAI-compatible AI chat using an endpoint and API key supplied by the user.
- Uses a Gemini-inspired gradient and glass-style smart-editor interface.
- Does not declare an Android accessibility service.

Native Circle to Search remains controlled by Google, the installed Google app, device configuration, region, account eligibility, and OEM software. MiCTS can request the feature but cannot inspect or change Google's private eligibility decision without root-based modifications.

## Requirements

- Android 9–16.
- The latest available [Google app](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox) for native Circle to Search and Google Lens actions.
- Google configured as the default assistant when using the VIS trigger service.
- Background launch and battery restrictions disabled for the Google app when the OEM would otherwise delay it.
- Optional: root, LSPosed, and the appropriate module scopes for CSHelper, CSService, Xiaomi gesture hooks, or Google device spoofing.

This personal repository currently does not publish release APKs. Build the app from source using [Building from source](#building-from-source).

## Getting started

1. Install a MiCTS build and open it once.
2. Leave the trigger strategy on **Auto: native first** unless you already know which mode you need.
3. If native Circle to Search is accepted, MiCTS asks on the next launch whether the interface appeared:
   - Choose **Yes, keep native** to continue using the native trigger.
   - Choose **No, use Lens fallback** to use screen capture and the smart editor.
4. If a fallback is selected, approve Android's screen-capture dialog as described in [Capture permission behavior](#capture-permission-behavior).
5. Add the MiCTS tile to Quick Settings or configure another launcher or automation action if you want a trigger other than the app icon.

Settings can be opened by:

- Long-pressing the app icon and selecting **Settings**.
- Long-pressing the MiCTS Quick Settings tile.
- Opening MiCTS from the LSPosed Modules page and selecting its settings entry.

## Trigger strategies

| Strategy | Behavior |
| --- | --- |
| **Auto: native first** | Tries native Circle to Search, asks the user to confirm whether it appeared, and remembers whether to use native or fallback on later launches. An immediate native rejection falls back automatically. |
| **Native Circle to Search only** | Requests native Circle to Search and never captures the screen as a fallback. |
| **Smart screen editor (local OCR)** | Captures one frame, opens the local editor, runs optional local OCR, and lets the user select text or an image region. |
| **Google Lens directly** | Captures one frame and hands the full image to the Google app's Lens screen without first showing the editor. If Lens cannot be launched, the editor remains available and explains the failure. |

MiCTS cannot reliably observe whether Google's native interface appeared after a request. Auto mode therefore uses the user's confirmation rather than claiming to detect private Google eligibility.

## Smart screen editor

The smart editor operates on one captured frame and provides:

- Tap-to-select recognized text.
- Drag-to-create, move, and resize a rectangular selection.
- Pinch-to-zoom and pan.
- Copy, Search, and Translate actions for selected text.
- Google Lens handoff for the selected region or the full screen.
- Retry and retake flows for recognition or capture failures.
- Detection of blank captures that may result from Android secure-window protection.

Latin and Chinese recognition runs locally using models packaged only in the MiCTS app flavor. OCR can be disabled without disabling image selection or Lens.

## Capture permission behavior

MiCTS uses Android MediaProjection and a short-lived foreground service to capture one frame. It does not keep a continuously armed capture service.

| Android version | Available behavior |
| --- | --- |
| Android 9–13 (API 28–33) | **Approve once** can reuse the consent token while the app process and token remain valid. **Ask every time** requests fresh approval for each trigger. Process death, reboot, or OEM invalidation can require approval again. |
| Android 14–16 (API 34–36) | Android treats MediaProjection consent tokens as single-use, so the system approval dialog appears before every fallback capture. MiCTS shows a one-time explanation before the first request. |

Each trigger starts a one-shot `ScreenCaptureService`, captures one frame, releases the virtual display and MediaProjection resources, and stops. Denied, expired, protected, empty, and timed-out captures return to an explanatory error screen instead of silently continuing.

## Optional AI assistant

The AI assistant is disabled by default. When enabled, the user configures an OpenAI-compatible base URL, model, and API key, then explicitly taps **Ask AI** in the smart editor.

- The initial request can include the selected screenshot region and recognized text.
- Image sending can be disabled for text-only models.
- Follow-up chat remains within the current editor session.
- MiCTS does not operate or control the configured endpoint.
- A user-configured `http://` endpoint is not transport-encrypted; use HTTPS unless a trusted local endpoint specifically requires HTTP.

The API key is stored in app-private preferences. MiCTS attempts to protect it with AndroidX encrypted preferences and falls back to ordinary app-private preferences if encrypted storage cannot be initialized on the device.

## Privacy and data handling

- Screen capture occurs only after a user trigger and Android approval when required.
- The app keeps at most one PNG at `cache/lens_capture/capture.png`; a new capture replaces it.
- Local OCR does not upload the screenshot or recognized text.
- Copy stays on the Android clipboard.
- Search, Translate, browser fallback, Lens, and AI actions send data only after the user selects the corresponding action.
- External apps, websites, and AI endpoints apply their own privacy and retention policies.
- Captures are deleted after cancellation and text-only handoffs. A Lens handoff may retain the temporary file long enough for the receiving Google app to read it; the next capture replaces it.
- MiCTS requests internet access for the optional AI assistant and browser or external-service workflows.

The absence of an accessibility service avoids that specific class of permission and compatibility concern, but it does not guarantee acceptance by every banking, enterprise, or security-sensitive app.

## App and module settings

### App settings

- Default and Quick Settings tile trigger delays.
- Vibration and asynchronous trigger options.
- Trigger strategy and Auto-mode reset.
- Capture method where Android permits a choice.
- Local text recognition toggle.
- Compatibility report for the device, Google app, assistant, system CTS signals, Lens sharing, trigger service, and capture mode.
- Optional AI endpoint, model, API key, image sending, and connection test.

### LSPosed module settings

Root and LSPosed are required for these settings. Configure only the scopes needed by the selected feature.

- **VIS**: Android 9–16; requires Google as the default assistant. It is also the only native trigger used without the module.
- **CSHelper**: Android 14 QPR3 and later; requires the System Framework scope and generally does not require Google as the default assistant.
- **CSService**: Android 15 and later; requires the System Framework scope and uses Android's dedicated contextual-search service.
- **Long-press gesture handle**: Xiaomi devices; requires the System Launcher or POCO Launcher scope.
- **Long-press Home button**: Xiaomi devices; requires the System Framework scope.
- **Device spoof for Google**: Changes the manufacturer, brand, model, and device values observed by the Google app; requires the Google app scope.

Use root-based spoofing and third-party flag tools at your own risk. Google can change eligibility checks or service behavior independently of this project.

## Troubleshooting

### “Trigger failed!” appears

For VIS, confirm that the Google app is installed, updated, allowed to run in the background, and configured as the default assistant. If LSPosed is enabled, verify the selected service is supported by the Android version and that its required scope is active.

### Google Assistant opens instead of Circle to Search

Update the Google app and confirm that Circle to Search is available for the device or account. MiCTS cannot force Google's private entitlement. If native support remains unavailable, select the smart editor or direct Lens strategy.

### Logcat reports `Omni invocation failed: not enabled`

The Google app received the native request but did not enable the native interface for the current configuration. Use a Lens fallback, or—with root and an understanding of the risks—review the LSPosed device-spoof settings.

### Native UI appears only after opening the Google app manually

Remove background and battery restrictions for the Google app. On supported rooted devices, CSHelper or CSService may avoid behavior tied to VIS and the default-assistant path.

### Android asks for screen-capture approval again

This is expected on Android 14 and later. On Android 9–13, **Approve once** lasts only while Android keeps the in-memory consent token valid; app process death, reboot, or OEM policy can invalidate it.

### The captured image is blank

The visible app may use Android's secure-window protection. MiCTS cannot capture protected content.

## Building from source

Prerequisites:

- JDK 17.
- Android SDK with API 36 installed.
- ADB if you want to install from the command line.

Clone this personal repository and build the full MiCTS debug flavor:

```bash
git clone https://github.com/rungruch/MiCTS.git
cd MiCTS
./gradlew :app:assembleMiCTSDebug
```

The APK is written under `app/build/outputs/apk/MiCTS/debug/`. Install the generated file with Android Studio or `adb install -r <apk-path>`.

The project also provides a smaller VISTrigger flavor containing only the native Voice Interaction Service trigger path:

```bash
./gradlew :app:assembleVISTriggerDebug
```

Release builds use the repository's signing-property configuration. CI builds both release flavors on pushes to `main`, version tags, and pull requests.

For implementation details, see [Architecture](docs/ARCHITECTURE.md).

## Project origin and license

This derivative retains the MiCTS name, package identifiers, and GPL-3.0 licensing of the original project. The upstream project and its authors remain credited at [parallelcc/MiCTS](https://github.com/parallelcc/MiCTS). Changes specific to this personal derivative are maintained at [rungruch/MiCTS](https://github.com/rungruch/MiCTS).

Distributed under the [GNU General Public License v3.0](LICENSE).

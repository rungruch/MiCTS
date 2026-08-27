# MiCTS

[![Stars](https://img.shields.io/github/stars/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS) [![Downloads](https://img.shields.io/github/downloads/parallelcc/MiCTS/total)](https://github.com/parallelcc/MiCTS/releases) [![Release](https://img.shields.io/github/v/release/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS/releases/latest)  [![DeepWiki](https://img.shields.io/badge/DeepWiki-parallelcc%2FMiCTS-blue.svg?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACwAAAAyCAYAAAAnWDnqAAAAAXNSR0IArs4c6QAAA05JREFUaEPtmUtyEzEQhtWTQyQLHNak2AB7ZnyXZMEjXMGeK/AIi+QuHrMnbChYY7MIh8g01fJoopFb0uhhEqqcbWTp06/uv1saEDv4O3n3dV60RfP947Mm9/SQc0ICFQgzfc4CYZoTPAswgSJCCUJUnAAoRHOAUOcATwbmVLWdGoH//PB8mnKqScAhsD0kYP3j/Yt5LPQe2KvcXmGvRHcDnpxfL2zOYJ1mFwrryWTz0advv1Ut4CJgf5uhDuDj5eUcAUoahrdY/56ebRWeraTjMt/00Sh3UDtjgHtQNHwcRGOC98BJEAEymycmYcWwOprTgcB6VZ5JK5TAJ+fXGLBm3FDAmn6oPPjR4rKCAoJCal2eAiQp2x0vxTPB3ALO2CRkwmDy5WohzBDwSEFKRwPbknEggCPB/imwrycgxX2NzoMCHhPkDwqYMr9tRcP5qNrMZHkVnOjRMWwLCcr8ohBVb1OMjxLwGCvjTikrsBOiA6fNyCrm8V1rP93iVPpwaE+gO0SsWmPiXB+jikdf6SizrT5qKasx5j8ABbHpFTx+vFXp9EnYQmLx02h1QTTrl6eDqxLnGjporxl3NL3agEvXdT0WmEost648sQOYAeJS9Q7bfUVoMGnjo4AZdUMQku50McDcMWcBPvr0SzbTAFDfvJqwLzgxwATnCgnp4wDl6Aa+Ax283gghmj+vj7feE2KBBRMW3FzOpLOADl0Isb5587h/U4gGvkt5v60Z1VLG8BhYjbzRwyQZemwAd6cCR5/XFWLYZRIMpX39AR0tjaGGiGzLVyhse5C9RKC6ai42ppWPKiBagOvaYk8lO7DajerabOZP46Lby5wKjw1HCRx7p9sVMOWGzb/vA1hwiWc6jm3MvQDTogQkiqIhJV0nBQBTU+3okKCFDy9WwferkHjtxib7t3xIUQtHxnIwtx4mpg26/HfwVNVDb4oI9RHmx5WGelRVlrtiw43zboCLaxv46AZeB3IlTkwouebTr1y2NjSpHz68WNFjHvupy3q8TFn3Hos2IAk4Ju5dCo8B3wP7VPr/FGaKiG+T+v+TQqIrOqMTL1VdWV1DdmcbO8KXBz6esmYWYKPwDL5b5FA1a0hwapHiom0r/cKaoqr+27/XcrS5UwSMbQAAAABJRU5ErkJggg==)](https://deepwiki.com/parallelcc/MiCTS)

[简体中文](/README.md)&nbsp;&nbsp;|&nbsp;&nbsp;English&nbsp;&nbsp;|&nbsp;&nbsp;[Русский](/README_ru.md)&nbsp;&nbsp;|&nbsp;&nbsp;[Türkçe](/README_tr.md)&nbsp;&nbsp;|&nbsp;&nbsp;[فارسی](/README_fa.md)

Trigger Circle to Search on any Android 9–16 device

*This app only aims to trigger Circle to Search and cannot handle issues that may occur after triggering successfully*

## How to Use

1. Install the latest version of [Google](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox), enable auto-start, disable background restrictions, and set Google as the default assistant app


2. Install and launch MiCTS
   - If you're lucky, Circle to Search will be triggered directly without root when launching MiCTS
   - If nothing happened, most likely it's because Google disabled Circle to Search for your device (you can confirm by checking the message `Omni invocation failed: not enabled` in Logcat). Try the following **with root**:
     - Activate the module in LSPosed, enable `Device spoof for Google` in the [MiCTS settings](#how-to-enter-settings), and force restart Google
     - If it still doesn't work, then change `com.google.android.apps.search.omnient.device` flag `45631784` to true using [GMS-Flags](https://github.com/polodarb/GMS-Flags)


3. Set up the trigger method
   - Launching MiCTS will trigger, so you can use other apps like Quick Ball, Xposed Edge, ShortX, etc., set launching MiCTS as the action to customize the trigger method
   - MiCTS provides a trigger tile, so you can add it to the Quick Settings panel and trigger by clicking it
   - For Xiaomi devices, MiCTS has built-in support for `Trigger by long press gesture handle` and `Trigger by long press home button`, which can be enabled in the MiCTS settings (need to activate the module and restart the phone after installing MiCTS)
   - For Samsung devices running Android 13 and above, you can download and install "Routines+" from the [Galaxy Store](https://galaxystore.samsung.com/detail/com.samsung.android.app.routineplus) or [Good Lock](https://galaxystore.samsung.com/detail/com.samsung.android.goodlock). Then, go to Settings > Modes and Routines to create routines that launch MiCTS by Button action such as long-pressing the power button.

### Google Lens fallback

MiCTS defaults to `Auto: native first`. The first launch tries real Circle to Search. On the next launch, MiCTS asks whether the native interface appeared:

- Choose `Yes, keep native` to continue using real Circle to Search.
- Choose `No, use Lens fallback` if Google disables Circle to Search. The first fallback trigger asks you to choose a capture method.

Settings offers four trigger strategies:

- **Auto: native first** - try real Circle to Search, remember what works, fall back automatically if the system rejects it.
- **Native Circle to Search only** - never use a fallback.
- **Smart screen editor (local OCR)** - after capturing one frame, open MiCTS's private editor with bundled offline Latin/Chinese text recognition, selection, Copy/Search/Translate, and a Lens handoff for the selected region or full screen.
- **Google Lens directly** - share the full captured frame straight into the Google app's Lens screen without opening the local editor. Refine the region inside Lens itself.

The fallback recognizes Latin and Chinese text locally with models bundled in MiCTS. You can tap detected text or draw a rectangle, pinch to zoom, then Copy, Search, Translate, or send the selected region to Google Lens. Full-screen Lens is available from the editor's overflow menu. Text recognition can be disabled in Settings without disabling Lens.

The Lens path is a fallback, not native Circle to Search. At most one temporary capture is kept in the app cache, and MiCTS does not upload the image or recognized text itself. Search, Translate, and Lens receive content only after you explicitly tap their action, and those external apps or websites apply their own privacy policies.

### Capture permission and privacy

| Android version | Fast capture | Ask every time |
| --- | --- | --- |
| Android 9–10 (API 28–29) | Not available because Android has no Accessibility screenshot API | Required for each fallback trigger |
| Android 11–12 (API 30–32) | Recommended; enable the screenshot-only MiCTS service once | Optional; new MediaProjection consent for every trigger |
| Android 13+ (API 33+) | Recommended; direct installs may first need App info → More → Allow restricted settings | Optional; new MediaProjection consent for every trigger, including Android 14+ |

Fast capture uses Android's Accessibility screenshot API only after an explicit launcher or Quick Settings trigger. The MiCTS service does not retrieve window content, receive Accessibility events, perform gestures, use the Accessibility button, or display overlays. It remains enabled until you disable it in Android Accessibility settings. If it is disabled or disconnected, MiCTS shows recovery choices instead of unexpectedly opening a screen-capture dialog.

`Ask every time` is the privacy-focused alternative. It creates one MediaProjection session, captures one frame, releases every projection resource, and discards the consent token. MiCTS never reuses a projection consent intent or token; Android 14 and later explicitly require fresh consent for each session.
   

## Settings

### How to enter Settings
- Long press the MiCTS app icon to show the Settings option, then click to enter
- From the Modules page in LSPosed, click MiCTS, then click the settings icon to enter
- Long press the Quick Settings panel tile to enter

### App Settings
- Default trigger delay: The delay when triggering by launching MiCTS
- Tile trigger delay: The delay when triggering by the Quick Settings panel tile
- Trigger strategy: Choose Auto, native Circle to Search only, the smart screen editor, or Google Lens directly
- Capture method: On Android 11+, choose Fast capture or Ask every time and inspect whether the screenshot service is enabled and connected. Android 9–10 always asks every time.
- Reset Auto detection: Ask again whether the native trigger works
- Recognize text locally: Enable the bundled offline Latin and Chinese models used by Copy, Search, and Translate
- Compatibility report: Shows the Google app, assistant, Lens, Android framework, and selected trigger-service status without claiming access to Google's private device eligibility

### Module Settings
Need to activate the module in LSPosed
- System trigger service: The system service used by triggering. Only the services supported will be shown. Need to add System Framework to the scope in LSPosed
   - VIS: Supports on Android 9-16. Need to set Google as the default assistant app and the screen edge will flash when triggering for some devices. If the module is not activated, only this service will be used
   - CSHelper: Supports on Android 14 QPR3 and above. Don’t need to set Google as the default assistant app and the screen edge will not flash when triggering
   - CSService: Supports on Android 15 and above. A dedicated service for Circle to Search, same effect as CSHelper


- Trigger by long press gesture handle: Only supports on Xiaomi devices. Need to add System Launcher/POCO Launcher to the scope in LSPosed


- Trigger by long press home button: Only supports on Xiaomi devices. Need to add System Framework to the scope in LSPosed


- Device spoof for Google: Need to add Google to the scope in LSPosed
   - Manufacturer: Modify the `ro.product.manufacturer` value that Google reads
   - Brand: Modify the `ro.product.brand` value that Google reads
   - Model: Modify the `ro.product.model` value that Google reads
   - Device: Modify the `ro.product.device` value that Google reads

## FAQ

### Prompt "Trigger failed!"

Most likely because Google is not set as the default assistant, check it

### Google assistant appears instead of Circle to Search

Ensure that Google is the latest version

### Gemini or Android Auto works, but Circle to Search does not

These features use different Google services and eligibility checks. If Logcat contains `Omni invocation failed: not enabled`, Google received the native request but disabled Circle to Search for the device. Without root, use MiCTS's Google Lens fallback.

### Sometimes it doesn't trigger successfully, and the interface appears only after opening Google

This is likely due to the tombstone mechanism. Check if your device has related settings and add Google to the whitelist, such as selecting "No restrictions" in battery saver

This issue should not occur when the `System trigger service` is set to `CSHelper` in the Module Settings

## Translation Contributing

You can contribute translation through [Crowdin](https://crowdin.com/project/micts)

If you need to contribute a new language, please submit an issue first

## Star History

<a href="https://star-history.com/#parallelcc/micts&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />
 </picture>
</a>

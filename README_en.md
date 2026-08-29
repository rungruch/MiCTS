# MiCTS

[![Stars](https://img.shields.io/github/stars/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS) [![Downloads](https://img.shields.io/github/downloads/parallelcc/MiCTS/total)](https://github.com/parallelcc/MiCTS/releases) [![Release](https://img.shields.io/github/v/release/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS/releases/latest)  [![DeepWiki](https://img.shields.io/badge/DeepWiki-parallelcc%2FMiCTS-blue.svg?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACwAAAAyCAYAAAAnWDnqAAAAAXNSR0IArs4c6QAAA05JREFUaEPtmUtyEzEQhtWTQyQLHNak2AB7ZnyXZMEjXMGeK/AIi+QuHrMnbChYY7MIh8g01fJoopFb0uhhEqqcbWTp06/uv1saEDv4O3n3dV60RfP947Mm9/SQc0ICFQgzfc4CYZoTPAswgSJCCUJUnAAoRHOAUOcATwbmVLWdGoH//PB8mnKqScAhsD0kYP3j/Yt5LPQe2KvcXmGvRHcDnpxfL2zOYJ1mFwrryWTz0advv1Ut4CJgf5uhDuDj5eUcAUoahrdY/56ebRWeraTjMt/00Sh3UDtjgHtQNHwcRGOC98BJEAEymycmYcWwOprTgcB6VZ5JK5TAJ+fXGLBm3FDAmn6oPPjR4rKCAoJCal2eAiQp2x0vxTPB3ALO2CRkwmDy5WohzBDwSEFKRwPbknEggCPB/imwrycgxX2NzoMCHhPkDwqYMr9tRcP5qNrMZHkVnOjRMWwLCcr8ohBVb1OMjxLwGCvjTikrsBOiA6fNyCrm8V1rP93iVPpwaE+gO0SsWmPiXB+jikdf6SizrT5qKasx5j8ABbHpFTx+vFXp9EnYQmLx02h1QTTrl6eDqxLnGjporxl3NL3agEvXdT0WmEost648sQOYAeJS9Q7bfUVoMGnjo4AZdUMQku50McDcMWcBPvr0SzbTAFDfvJqwLzgxwATnCgnp4wDl6Aa+Ax283gghmj+vj7feE2KBBRMW3FzOpLOADl0Isb5587h/U4gGvkt5v60Z1VLG8BhYjbzRwyQZemwAd6cCR5/XFWLYZRIMpX39AR0tjaGGiGzLVyhse5C9RKC6ai42ppWPKiBagOvaYk8lO7DajerabOZP46Lby5wKjw1HCRx7p9sVMOWGzb/vA1hwiWc6jm3MvQDTogQkiqIhJV0nBQBTU+3okKCFDy9WwferkHjtxib7t3xIUQtHxnIwtx4mpg26/HfwVNVDb4oI9RHmx5WGelRVlrtiw43zboCLaxv46AZeB3IlTkwouebTr1y2NjSpHz68WNFjHvupy3q8TFn3Hos2IAk4Ju5dCo8B3wP7VPr/FGaKiG+T+v+TQqIrOqMTL1VdWV1DdmcbO8KXBz6esmYWYKPwDL5b5FA1a0hwapHiom0r/cKaoqr+27/XcrS5UwSMbQAAAABJRU5ErkJggg==)](https://deepwiki.com/parallelcc/MiCTS)

[简体中文](/README.md)&nbsp;&nbsp;|&nbsp;&nbsp;English&nbsp;&nbsp;|&nbsp;&nbsp;[Русский](/README_ru.md)&nbsp;&nbsp;|&nbsp;&nbsp;[Türkçe](/README_tr.md)&nbsp;&nbsp;|&nbsp;&nbsp;[فارسی](/README_fa.md)

Trigger Circle to Search without root on Android 9–16

*This app only aims to trigger Circle to Search and cannot handle issues that may occur after triggering successfully*

## How to Use

1. Install the latest version of [Google](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox), enable auto-start, disable background restrictions, and set Google as the default assistant app


2. Install and launch MiCTS
   - MiCTS invokes the native Circle to Search interface directly.
   - If Google rejects the native request, MiCTS can use the Google Lens fallback described below. Native eligibility is controlled by Google and cannot be changed by MiCTS.


3. Set up the trigger method
   - Launching MiCTS will trigger, so you can use other automation tools like Quick Ball or ShortX and set launching MiCTS as the action to customize the trigger method
   - MiCTS provides a trigger tile, so you can add it to the Quick Settings panel and trigger by clicking it
   - For Samsung devices running Android 13 and above, you can download and install "Routines+" from the [Galaxy Store](https://galaxystore.samsung.com/detail/com.samsung.android.app.routineplus) or [Good Lock](https://galaxystore.samsung.com/detail/com.samsung.android.goodlock). Then, go to Settings > Modes and Routines to create routines that launch MiCTS by Button action such as long-pressing the power button.

The repository also produces a separate `VISTrigger` APK for users who still need the legacy LSPosed module. It is not required by the standalone MiCTS APK.

The standalone build keeps the `com.parallelc.micts` package name. Installing it over an older MiCTS module replaces that old module build; install `VISTrigger` separately if you still need the legacy hooks.

### Google Lens fallback

MiCTS defaults to `Auto: native first`. The first launch tries real Circle to Search. On the next launch, MiCTS asks whether the native interface appeared:

- Choose `No, use Lens fallback` if Google disables Circle to Search. MiCTS requests Android screen-capture permission, captures one full frame, and sends it directly to Google Lens.

Settings offers three trigger strategies:

- **Auto: native first** - try real Circle to Search, remember what works, fall back automatically if the system rejects it.
- **Native Circle to Search only** - never use a fallback.
- **Google Lens fallback** - capture one full frame and send it directly to the Google app's Lens screen.

The Lens path is a fallback, not native Circle to Search. At most one temporary capture is kept in the app cache. MiCTS does not upload the capture itself; Google Lens receives the image only after the explicit fallback action and applies its own privacy policy.

### Capture permission and privacy

| Android version | Approve once (Remember consent) | Ask every time |
| --- | --- | --- |
| Android 9–13 (API 28–33) | Recommended; approve Android's screen-capture prompt once per app process to capture silently without repeated dialogs | Optional; prompt for fresh MediaProjection consent before every trigger |
| Android 14+ (API 34+) | Automatically degrades to Ask every time; tokens are single-use by platform design | Required for each fallback trigger |

Unlike accessibility-based capture tools, MiCTS uses **zero accessibility services**, ensuring complete compatibility with banking and security-conscious applications that restrict accessibility tools.

- **Approve once (Android 9–13)**: Prompts for Android's standard MediaProjection screen-capture permission once and keeps the consent token only while the app process is alive. Each trigger starts a fresh, one-shot foreground service that captures a single frame and immediately stops and releases all resources—avoiding background battery drain and avoiding the need for an accessibility service or persistent "armed" service. After process death, a reboot, or platform token invalidation, MiCTS prompts again.
- **Ask every time (Android 14+ and optional for older versions)**: Android 14 and newer enforce single-use tokens by platform design (`SecurityException` on reuse). On Android 14+, MiCTS explains this once and requests fresh consent for each capture.
   

## Settings

### How to enter Settings
- Long press the MiCTS app icon to show the Settings option, then click to enter
- Long press the Quick Settings panel tile to enter

### App Settings
- Default trigger delay: The delay when triggering by launching MiCTS
- Tile trigger delay: The delay when triggering by the Quick Settings panel tile
- Trigger strategy: Choose Auto, native Circle to Search only, or Google Lens fallback
- Capture method: On Android 9–13, choose between "Approve once" (reuses consent while MiCTS remains running) and "Ask every time". On Android 14+, Android requires asking before every capture.
- Reset Auto detection: Ask again whether the native trigger works

## FAQ

### Prompt "Trigger failed!"

Most likely because Google is not set as the default assistant, check it

### Google assistant appears instead of Circle to Search

Ensure that Google is the latest version

### Gemini or Android Auto works, but Circle to Search does not

These features use different Google services and eligibility checks. If Logcat contains `Omni invocation failed: not enabled`, Google received the native request but disabled Circle to Search for the device. Use MiCTS's Google Lens fallback.

### Sometimes it doesn't trigger successfully, and the interface appears only after opening Google

This is likely due to the tombstone mechanism. Check if your device has related settings and add Google to the whitelist, such as selecting "No restrictions" in battery saver

If the native interface is unreliable, select Google Lens fallback in MiCTS settings.

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

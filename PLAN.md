# MiCTS Smart Search Editor Upgrade

## Summary

- Keep native Circle to Search as MiCTS’s primary path; enhance only the rootless fallback used after CTS rejection or manual selection.
- Turn the fallback into a private smart editor with bundled, offline Latin and Chinese OCR, precise region selection, and Copy, Search, Translate, and Lens actions.
- Preserve MediaProjection consent on every capture. Keep VisTrigger and LSPosed gesture hooks unchanged and native-only.

## Implementation Changes

- Run bundled [ML Kit Text Recognition V2](https://developers.google.com/ml-kit/vision/text-recognition/v2/android) automatically after capture, controlled by a default-on “Recognize text locally” setting. Process a downscaled image capped at 2048px on its longest edge while retaining the original bitmap for sharing.
- Run Latin and Chinese recognition sequentially to limit memory use. Map line bounds back to original pixels and deduplicate overlaps: prefer Chinese output when either result contains CJK characters, otherwise prefer Latin.
- Replace the current crop state with a lifecycle-aware editor state containing normalized selection, viewport transform, OCR status, recognized lines, selected text, and action availability. Restore selection after rotation or process recreation and rerun OCR from the cached capture if necessary.
- Upgrade selection controls:
  - Start with the existing centered 72% rectangle.
  - Tap a recognized line to select its padded bounds.
  - Drag outside the selection to create a new rectangle; drag inside to move it; use corner handles to resize it.
  - Use two fingers to pan and zoom from 1×–5×, clamped to keep the screenshot reachable.
  - Derive selected text from recognized lines whose centers lie inside the rectangle, ordered top-to-bottom and left-to-right.
  - Maintain correct bitmap mapping through zoom, density, orientation, and letterboxing, with 48dp touch targets and minimum selection size.
- Present nonblocking OCR states such as “Finding text,” “No text found,” and retryable recognition failure. Lens selection remains usable while OCR is running or disabled.
- Use a responsive Material 3 editor:
  - Close and Retake in the top bar.
  - A two-line selected-text preview.
  - Copy, Search, and Translate contextual actions.
  - A prominent Lens button that shares the selected rectangle.
  - “Search full screen with Lens” in overflow.
  - Use a bottom action bar on compact screens and a right-side action rail at widths of 600dp or greater.
- Implement action behavior through testable gateways:
  - Copy places only selected text on the clipboard.
  - Search uses `ACTION_WEB_SEARCH`, then offers a browser search if unavailable.
  - Translate uses `ACTION_TRANSLATE` when resolvable; otherwise it offers a clearly explained Google Translate browser handoff.
  - Lens continues targeting only the host Google app using the existing FileProvider URI and temporary read permission.
  - Failed external launches keep the editor and selection visible. Successful Search, Translate, or Lens handoffs close the editor; Copy does not.
- Keep exactly one cached PNG. Delete it immediately on Cancel and after text-only handoffs; retain it temporarily after Lens so the receiving app can read it, then replace or clean it at the next fallback launch. Never persist OCR text or capture history.
- Add package visibility queries for web search, translation, and HTTPS browser fallback without adding exported components, broad storage access, accessibility, or overlay permissions.
- Add `TextRecognitionGateway`, `RecognizedTextLine`, `RecognitionResult`, `EditorState`, `ViewportTransform`, `SelectionGesture`, and `ExternalActionGateway`. Keep geometry, deduplication, selection, and coordinator logic as pure Kotlin.
- Place ML Kit implementations and dependencies in the MiCTS flavor only, with a no-op flavor factory for VisTrigger compilation. VisTrigger must not package the OCR models or expose fallback UI.
- Add English strings and documentation first; existing locales fall back to English. Document that OCR is local, external Search/Translate/Lens apps may transmit explicitly shared content under their own policies, and working Gemini or Android Auto does not imply native CTS eligibility.

## Test Plan

- Unit-test OCR coordinate scaling, Latin/Chinese deduplication, reading order, text extraction, normalized state restoration, zoom/pan clamping, rectangle creation/move/resize, 48dp minimum sizing, and portrait/landscape letterboxing.
- Add Compose tests for OCR loading, disabled OCR, line tapping, free rectangle creation, text preview, action enablement, recognition failure/retry, no-text captures, protected content, external-app failure, and selected versus full-screen Lens.
- Build, test, lint, and minify both MiCTS and VisTrigger with JDK 17; verify the VisTrigger artifact contains neither ML Kit models nor fallback components.
- Validate APIs 28, 31, 34, and 36 for rotation, process death, projection denial, secure windows, clipboard behavior, intent resolution, cache cleanup, and temporary URI access.
- On the Huawei SGT-LX9, verify offline Latin and Chinese OCR, pan/zoom selection, Copy, Google/GBox Search and Translate resolution, selected-region Lens, full-screen Lens, per-trigger capture consent, and projection/grant cleanup.
- Assemble and reinstall the release build with `adb install -r`; do not uninstall or erase app data if a signing mismatch occurs without separate user authorization.

## Assumptions

- Bundled Latin and Chinese OCR increases only the MiCTS artifact size; no Play Services model download is required.
- Object detection, barcode scanning, freehand lasso, in-app translation, screenshot history, analytics, root spoofing, and private Google entitlement changes remain out of scope.
- The fallback shares the selected region by default; full-screen Lens is always an explicit secondary action.
- MiCTS itself performs no network upload. Network use occurs only after the user explicitly hands text or an image to an external Search, Translate, browser, or Lens app.

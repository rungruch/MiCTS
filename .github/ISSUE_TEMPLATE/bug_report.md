---
name: Bug report
about: Report a reproducible problem in this personal MiCTS derivative
title: ""
labels: ""
assignees: ""
---

## Before submitting

- Confirm that the APK came from `rungruch/MiCTS`, not another MiCTS repository.
- Update the Google app and remove background or battery restrictions that may delay it.
- For the native standalone trigger, confirm that Google is the default assistant.
- For VISTrigger, confirm that LSPosed is active and only the required scopes are selected.
- Remove private screenshot content, account details, signing material, tokens, and other secrets from logs and attachments.

## Problem

Describe what happened and what you expected to happen.

## Reproduction steps

1.
2.
3.

## Build and trigger configuration

- Flavor: MiCTS / VISTrigger
- Version and commit:
- APK filename or build command:
- Trigger strategy: Auto / Native only / Google Lens fallback
- Trigger method: App icon / Quick Settings tile / Automation / Xiaomi gesture / Home button / Other
- Did Android accept the native request?
- Did the Circle to Search interface appear?
- Does another trigger method work?

## Capture configuration

- Capture involved: Yes / No
- Android showed the capture-consent dialog: Every time / Once this process / Not applicable
- Permission result: Approved / Denied / Not applicable
- Result: Lens opened / Blank capture / Protected-content message / Timeout / Lens unavailable / Other

## Environment

- Device manufacturer and model:
- Android version and API level:
- OEM OS version:
- Google app version:
- Default assistant:
- Rooted: Yes / No
- LSPosed version:
- Active LSPosed scopes:
- Launcher and version, if a launcher hook is involved:

## Evidence

Attach the smallest relevant Logcat section, screenshots, a screen recording, or the ZIP exported from LSPosed's Logs page. Include timestamps and reproduce the problem once before collecting logs when possible.

Redact API keys, authorization headers, signing credentials, private screenshots, account identifiers, and other sensitive data. Do not upload an entire unreviewed device log.

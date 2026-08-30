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
- Confirm that Google is the default assistant for native MiCTS and VISTrigger requests.
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
- Trigger strategy: MiCTS Auto / MiCTS native only / MiCTS Google Lens fallback / VISTrigger direct VIS
- Trigger method: App icon / Quick Settings tile / Automation / Other
- Did Android accept the native request?
- Did the Circle to Search interface appear?
- Does another trigger method work?

## Capture configuration (MiCTS Lens fallback only)

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
- Rooted: Yes / No (neither app requires root)
- Launcher and version:

## Evidence

Attach the smallest relevant Logcat section, screenshots, or a screen recording. Include timestamps and reproduce the problem once before collecting logs when possible.

Redact API keys, authorization headers, signing credentials, private screenshots, account identifiers, and other sensitive data. Do not upload an entire unreviewed device log.

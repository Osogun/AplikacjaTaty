# Forensic Signature Acquisition (Android, Views)

This repository contains an Android (Kotlin, XML Views) application for dynamic biometric signature capture with a focus on forensic reproducibility and evidential integrity.

## Core features

- Full-screen white canvas with black stroke rendering.
- Captures stylus and finger input, with tool type distinguished in records.
- Captures hover events from stylus only.
- Captures historical `MotionEvent` points as `MOVE_HIST`.
- Normalizes coordinates into a fixed logical space:
  - Width: `960`
  - Height: `540`
- Stores per-point biometric and dynamic data:
  - `eventTime`
  - normalized `x`, `y`
  - `pressure`
  - `eventType`
  - `toolType`
  - `tilt`
  - `orientation`
  - `distance`
- Adds forensic metadata (device/build/version/timezone/session IDs).
- Exports both JSON and CSV for each session and computes SHA-256 digests.

## Output location

The app writes output into app-specific external storage:

`Android/data/com.example.forensicsignature/files/forensic_sessions/`

Each save creates:

- `<base>.json`
- `<base>.csv`

## Integrity notes

The JSON payload includes:

- `samplesSha256` (hash over canonical sample structure)
- `payloadSha256` (hash over payload structure)

In addition, file-level SHA-256 values are computed after write and surfaced in UI toast.

## Gradle wrapper note

This repository intentionally excludes binary artifacts. If your environment requires `gradle/wrapper/gradle-wrapper.jar`, regenerate it locally with:

```bash
gradle wrapper --no-validate-url
```

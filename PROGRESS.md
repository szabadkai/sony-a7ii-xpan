# Progress — 2026-10-25

## Completed locally

- Cloned and inspected PMCADemo, Recipe Lab, Sony-PMCA-RE, OpenMemories-Framework and TimeLapse; exact revisions in research/sources.lock.json.
- Recorded platform/licensing/API evidence in RESEARCH.md and retained a selected MIT SDK declaration reference.
- Implemented Java-only API-10 app, real CameraEx bridge, native SurfaceView, physical half/full shutter, RAW+JPEG/full-size/3:2/single-shot negotiation, APS-C off, output restoration, AF and exposure controls.
- Added 65:24 black mask and four other ratios, persistence, physical settings UI, display/layout listeners, RGBA4444 mode and magenta EVF probe.
- Added callback generation fencing, duplicate-shutter suppression, preview resume/release, explicit error paths and bounded diagnostics.
- Built signed APK with local pinned tools; verified API 10 manifest, DEX 035, v1 RSA signing, ZIP alignment, absence of packaged Sony stubs/native libraries.
- Passed Java core checks (fixed/randomized geometry, display pixel aspect, capture lifecycle) and six Python tests (API declarations, crop export, EXIF rotation, source preservation).
- Added non-destructive JPEG/TIFF/PNG companion exporter and Linux/macOS bootstrap/build scripts.
- Wrote installation/recovery, user guide, limitations and owner-run hardware checklist.

## Current validation

- Separate clean-source build and byte-reproducibility check are being performed; final result goes in docs/VALIDATION.md.

## Verified source findings

- PMCA-RE documents Android 2.3.7; the OpenMemories camera table lists A7M2 Scalar API 2.10 (different version scheme).
- CameraEx.getNormalCamera supports PMCADemo's native preview/AF/null-callback capture path.
- Scalar avio exposes finder/panel display IDs, active-display notifications, video layout/rectangle and physical aspect.
- Recipe Lab lists ILCE-7M2 compatible without specifying firmware/EVF results; its RAW-only capture failure report is A6000 evidence only.
- Newer CaptureStatus/StoreImage APIs are optional; older Sony ShutterListener provides a source-supported fallback.

## Unverified assumptions and outstanding camera tests

- APK acceptance/launch on actual ILCE-7M2; exact body firmware/runtime/API capabilities.
- Live preview, AF-S/AF-C, exposure units/dials, native output negotiation, 6000×4000 active RAW and JPEG files actually reaching the card.
- EVF overlay, eye-sensor switching, physical pixel aspect, VideoRect coordinates, preview field of view and RAW/lens-correction alignment.
- Callback timing, automatic preview return, full/missing card errors, pause/resume and normal output restoration.
- Diagnostic external-storage mapping and optional file callback availability.

**No camera has been connected to or operated by this project; no installation was attempted.** All hardware acceptance rows remain NOT RUN. Native extra panoramic JPEG and reliable per-file ratio metadata are deferred. This is an installable-format prototype, not a physically validated camera release.

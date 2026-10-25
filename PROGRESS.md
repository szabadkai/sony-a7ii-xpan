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

## Final local validation

- Separate clean-source build passed: unsigned and same-key signed APKs are byte-identical. Evidence and hashes are in docs/VALIDATION.md.

## Verified source findings

- PMCA-RE documents Android 2.3.7; the OpenMemories camera table lists A7M2 Scalar API 2.10 (different version scheme).
- CameraEx.getNormalCamera supports PMCADemo's native preview/AF/null-callback capture path.
- Scalar avio exposes finder/panel display IDs, active-display notifications, video layout/rectangle and physical aspect.
- Recipe Lab lists ILCE-7M2 compatible without specifying firmware/EVF results; its RAW-only capture failure report is A6000 evidence only.
- Newer CaptureStatus/StoreImage APIs are optional; older Sony ShutterListener provides a source-supported fallback.

## Unverified assumptions and outstanding camera tests

- Launch on actual ILCE-7M2 and runtime/API capabilities. Installation and firmware 4.01 are now confirmed.
- Live preview, AF-S/AF-C, exposure units/dials, native output negotiation, 6000×4000 active RAW and JPEG files actually reaching the card.
- EVF overlay, eye-sensor switching, physical pixel aspect, VideoRect coordinates, preview field of view and RAW/lens-correction alignment.
- Callback timing, automatic preview return, full/missing card errors, pause/resume and normal output restoration.
- Diagnostic external-storage mapping and optional file callback availability.

**The user-authorized installation succeeded on ILCE-7M2 firmware 4.01 via MTP/libusb on macOS. See docs/INSTALLATION_RESULT.json.** Installation is confirmed; launch, capture and EVF acceptance rows remain NOT RUN. Native extra panoramic JPEG and reliable per-file ratio metadata are deferred. This is an installable-format prototype, not a physically validated camera release.

## Physical installation update

- Installed org.xpan.camera 0.1.0-prototype successfully after the user requested installation.
- Camera returned model ILCE-7M2, firmware 4.01, and the installed package in its application inventory.
- Mass Storage access failed on this Mac. Switching to MTP and using libusb with normal macOS administrator authorization succeeded. No driver installation or firmware modification was performed.
- The inventory reports the Android package version as 4.1.2-323; the earlier Android 2.3.7 statement is the upstream compatibility baseline, not a measurement of this body. Build.VERSION.SDK_INT still needs an app diagnostic.

## Interface polish — 0.2.0-prototype

- Added a project-owned SVG launcher icon and six packaged PNG density variants.
- Refined shooting information, RAW/JPEG badge, selected settings rows, contextual help, wrapped errors and preview-alignment fallback labeling.
- Added persisted clean view on UP; error and capture-in-progress messages stay visible.
- Shared drawing logic with a host Java2D preview tool; generated 32 layouts across four viewport sizes. These are layout checks, not camera screenshots.
- Built versionCode 2 with the retained signing key; legacy API 10, DEX 035 and v1 signing checks passed. Existing Java and Python suites passed.
- Owner-authorized 0.2.0 update succeeded via the same MTP/libusb route. The camera returned version 0.2.0-prototype in its application inventory; sanitized evidence is in docs/INSTALLATION_RESULT_0.2.0.json. Launch, preview, capture and EVF tests remain pending.

## Panoramic RAW follow-up

- Owner confirms 0.2.0 live preview and normal shutter capture work. This supersedes earlier NOT RUN entries for those two observations; EVF and precise framing remain unconfirmed.
- Added an optional desktop RAW DNG workflow, keeping original ARWs and verifying unchanged decoded RAW pixels. Native arbitrary-aspect ARW capture is not established.
- Tested one ARW read from the connected ILCE-7M2: 6000×2215 default crop, unchanged RAW pixel checksum, matching encoded RAW segments and byte-identical recovery of the embedded original. macOS ImageIO renders the panoramic crop correctly.
- Added four RAW workflow tests; all ten Python tests and 125,022 Java checks pass. Personal sample files are excluded from Git.
- Camera APK remains 0.2.0. The new conversion runs on the computer after import; in-camera JPEG export is still unfinished.

## On-camera JPEG implementation — 0.3.0

- Added an enabled-by-default Panorama JPEG setting, per-shot ratio snapshot, new-image identification and automatic additional JPEG export after native capture.
- Selected Android region decoding and strip encoding instead of relying on undocumented Sony image-filter ownership. No full-frame Android bitmap or new native library is required.
- Added tested baseline JPEG strip assembly with restart markers, output dimension validation, unique files, cancellation and preview/capture coordination.
- Output is under XPAN/PANORAMAS in reported external storage. Sony playback indexing is not implemented; actual file visibility needs testing.
- API-10 build, DEX/v1 signature/alignment, existing tests and JPEG strip tests pass. 0.3.0 is not installed or hardware-validated yet. See docs/ON_CAMERA_JPEG.md.

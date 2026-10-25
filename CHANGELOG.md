# Changelog

## 0.3.0-prototype

- Optional on-camera cropped JPEG after native capture, enabled by default; full RAW/JPEG originals remain intact.
- Memory-bounded strip processing, quality-95 output, orientation and supported EXIF copying.
- New uniquely named files under XPAN/PANORAMAS, progress display, cancellation and capture blocking during export.
- Host tests pass; camera installation, crop output, storage visibility and playback remain unverified.

## Desktop RAW companion

- Create a new panoramic CFA RAW DNG from an A7 II ARW or fresh full-frame DNG.
- Retain all sensor pixels, embed the original ARW and verify decoded pixel checksums.
- Preserve source files; refuse conflicting crops and existing outputs.
- Tested one real camera ARW and macOS panoramic rendering. Camera APK remains 0.2.0.

## 0.2.0-prototype

- Panoramic launcher icon in six density variants, with editable SVG source.
- Ivory/amber shooting information, clearer RAW/JPEG badge and settings selection.
- Contextual settings help and measured, wrapped status/error messages.
- UP toggles a persisted clean shooting view; errors and capture progress stay visible.
- Explicit Sony-rectangle fallback label in the diagnostic probe.
- Shared production drawing code and a host layout-preview script.
- Versioned APK filenames derived from the manifest; same signing identity as 0.1.0.

Host checks pass. Camera installation and operation for this version remain untested.

## 0.1.0-prototype

Initial native CameraEx preview/capture implementation, five framing ratios, RAW+JPEG negotiation, display diagnostics, desktop crop utility and reproducible legacy Android build. Installed on ILCE-7M2 firmware 4.01; launch, capture and EVF behavior still await physical testing.

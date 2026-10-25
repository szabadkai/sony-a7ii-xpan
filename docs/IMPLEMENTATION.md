# Implementation and known limits

## Structure

- `Sony.java`: a small reflective adapter to real CameraEx APIs, parameter capability negotiation, optional listeners and release. Required signature declarations are retained under `third_party/api-reference/`; no Sony implementations are bundled.
- `MainActivity.java`: SurfaceView, Android Canvas mask, physical controls, parameter snapshot/restore, callbacks, app preferences, bounded diagnostic logs and display listener.
- `UiRenderer.java`: shared mask/HUD/settings drawing with measured text wrapping and truncation, warm high-contrast colors and clean view. Android Canvas is the production adapter; Java2D is only a host layout preview tool.
- `Geometry.java`: integer sensor crop and mapping into a fitted live-image rectangle, with physical display pixel aspect correction.
- `CaptureGate.java`: single capture at a time, generation numbers that reject callbacks from an old camera session.
- `tools/crop.py`: separate desktop export; never reads/writes ARW or installs anything.

## Geometry contract

`Geometry.crop` retains maximal width or height, rounds the other to nearest integer (half up), and floor-centers it. `Geometry.map` maps sensor coordinates into the image rectangle. `Geometry.fit` fits the **full sensor image**, rather than computing black bars from the screen alone. For a square-pixel 640×480 Android viewport, a 3:2 source uses approximately 640×427 at y=26; the panorama is then cut inside that image, not inside the full 480-pixel display. Sony-reported physical display aspect corrects nonsquare pixels.

The implementation assumes that the live surface represents the complete configured 3:2 active image. The real preview buffer may have padding, a different field of view, lens corrections or compositor behavior. Source code alone cannot prove that assumption. The experimental VideoRect mode scales device coordinates into view coordinates, but its coordinate system must be established by the hardware chart test. Pixel-right/bottom endpoint convention is also unverified. No claim of calibrated EVF framing is made yet.

A7 II output size is verified by runtime parameters where possible: standard `getPictureSize()` first, then a read-only `getImagePixelSize` query. The aggregated stubs expose two String arguments without names; both size/aspect orders are probed, and only 6000×4000 is accepted. This checks what the API reports, not the contents of the actual ARW. No resolution is fabricated if those queries fail.

## Camera and callbacks

The app snapshots output format/aspect/size/drive/APS-C, selects large 3:2 single-shot RAW+JPEG (or explicit JPEG), sets APS-C off where supported, and verifies returned settings. Exposure, white balance and AF mode remain inherited. Digital zoom must be 1x. The existing native still engine owns image encoding, naming, DCF/database updates and card writes via `takePicture(null,null,null)` — the PMCADemo pattern. The app never supplies an empty file writer or overwrites an existing photograph.

CaptureStatus `onEnd` is preferred where available. Older firmware falls back to Sony ShutterListener, as used in the MIT TimeLapse project; its timing relative to card writes is unverified. Neither is reported as a saved-file guarantee. Preview is restarted after the completion event; the actual A7 II may also restart itself. Optional StoreImageComplete callbacks are logged only; absence is allowed. No JPEG pixel callback is installed, avoiding 24 MP bitmap allocation and possible changes to native storage behavior.

Repeated shutter-down events are suppressed. Key-up follows PMCADemo's cancelTakePicture convention. A 60-second watchdog reports waiting; it does not start another capture, force a preview restart or assume a long exposure has finished. Unexpected system pause still releases camera resources; output restoration is skipped while capture is pending rather than changing parameters mid-write. After abnormal exit verify native output settings yourself. Normal restoration is best effort and needs hardware confirmation.

The Android surface can survive activity pause; on resume the app explicitly attempts preview whenever both the camera and surface exist. It does not rely exclusively on a future surfaceCreated event. Generation checks protect against queued callbacks from a previous session.

## EVF and UI

The SurfaceView is beneath the Android mask View (no setZOrderOnTop). The app requests RGBA4444, matching PMCADemo's live-view path. It listens for Sony active-display events and recomputes geometry on view size/display changes. Automatic eye-sensor routing remains Sony-controlled. No forced LCD/EVF switch or persistent display tweak is used. C1 renders a solid magenta patch for the critical EVF composition test.

UI has no touchscreen dependency. Five aspect ratios and output preferences persist. The native autofocus callback supplies a simple status indicator, not Sony's focus-box rendering. P/A/S/M and dials depend on what the native camera preserves on opening; ISO/WB/AF-area menus and AF/MF–AEL handling remain outside this prototype. Exposure units (aperture /100; shutter numerator/denominator) and live callback refresh require comparison with EXIF. A 3:2 image has no panorama bars, so the small status text overlays the image in that mode.

## Known limits

1. Versions 0.1.0 and 0.2.0 installation are verified on ILCE-7M2 firmware 4.01. Launch, preview, capture, EVF overlays, eye sensor and release remain unverified.
2. No firmware-wide compatibility claim. Third-party A7 II reports do not identify every firmware/API revision.
3. Native extra cropped JPEG is investigated but not implemented; desktop export is the safe first phase.
4. No automatic per-file ratio metadata association. Diagnostic capture intentions are not authoritative file metadata.
5. Full native camera UI, playback, movies, burst, HDR/bracketing and touch control are not implemented. Single-shot is intentional.
6. Lens corrections and RAW development can change field of view; crop geometry alone cannot undo them.
7. Automatic preview-return behavior, errors on missing/full cards and callbacks during eye-sensor changes need testing.
8. Normal parameter restoration is best effort. Do not assume configuration is restored after power loss or forced exit.
9. A signature/DEX inspection proves package construction, not successful PMCA installation or hardware reliability.

## Non-goals enforced in code

No firmware partitions, bootloaders, native memory, sensor calibration, protected backup parameters, system patches, network services, installer automation, or photo deletion. External file writes are uniquely named app diagnostic reports only; photo capture is delegated to Sony's normal camera API.

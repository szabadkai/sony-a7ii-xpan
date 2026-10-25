# Platform research — 2026-10-25

This repository targets **ILCE-7M2**, not other A7 generations. Findings below distinguish source evidence from hardware results. No physical camera has been tested.

## Platform and starting point

Sony-PMCA-RE is an installer/reverse-engineering tool, not an Android SDK. Its `pmca-gui` is a Python UI. Actual API declarations are the Jasmin class stubs in [OpenMemories-Framework](https://github.com/ma1co/OpenMemories-Framework/tree/master/stubs/src/main/jasmin). Its camera table identifies A7M2 with **Scalar API 2.10**, distinct from **Android API 10 / Android 2.3.7** required by PMCA-RE's README.

[PMCADemo](https://github.com/ma1co/PMCADemo) is MIT and provides the simplest camera example: CameraEx.open(0, null), getNormalCamera(), SurfaceHolder, autoFocus(null), takePicture(null, null, null), cancelTakePicture(), release(). It is the basis of this independent minimal app. OpenMemories is MIT. [Recipe Lab](https://github.com/voxivoid/recipe-lab-sony-pmca) currently uses GPL-3.0 plus attribution terms; inspected as research only, no code copied. Its MainActivity opens CameraEx reflectively and its Params documents runtime RAW/JPEG formats. Its native backup-setting writes are unnecessary here and are excluded. PMCA-RE is MIT and is not bundled into the APK. Exact inspected commits: research/sources.lock.json.

## Compatibility and firmware

Recipe Lab's README lists **A7 II / ILCE-7M2** as compatible; the row gives neither firmware nor EVF-overlay results. This is evidence for PMCA application execution, not validation of this APK. [Sony's model-specific firmware page](https://www.sony.com/electronics/support/e-mount-body-ilce-7-series/ilce-7m2/downloads/00016064) lists firmware **4.01**, released May 16, 2019. No source examined establishes a complete firmware-by-firmware application compatibility matrix. Record the owner's version and runtime in the hardware checklist; do not upgrade firmware for this prototype.

## Capture and display architecture

CameraEx wraps Sony's native still-camera engine through android.hardware.Camera. The live video goes to a SurfaceView via setPreviewDisplay. Android Views placed above that surface are the initial overlay architecture. PMCADemo switches the graphics framebuffer to RGBA4444 during live view; the app follows that path. Sony avio.DisplayManager exposes DEVICE_ID_PANEL/FINDER, getActiveDevice, getDeviceInfo, getDisplayedVideoRect, setDisplayStatusListener, switchDisplayOutputTo, and setOSDOutput. The framework's DisplayInfo notes that physical display aspect can differ from pixel-grid aspect. This is evidence that display routing and graphics composition exist; it does **not** prove a custom View is visible in the A7 II EVF.

S1_1 scan 516 starts autofocus; S1_2 517 is consumed; S2 518 triggers capture and its release calls cancelTakePicture (PMCADemo convention). Normal Sony metering/AF runs behind these APIs, but native menus and all button actions are not automatically inherited by an Android Activity. P/A/S/M behavior, AF-C, stabilization, auto review and lens correction need camera tests.

ParametersModifier declares supported-format queries, RAW/RAWJPEG/JPEG, large image size, 3:2 aspect, drive mode, aperture, shutter and ISO getters. This prototype negotiates full-size 3:2 single-shot RAW+JPEG where advertised, never substitutes JPEG silently, and leaves exposure/WB/focus settings inherited. Recipe Lab documents a RAW-only shutter reboot on A6000 (issue #100); that is **not an A7 II finding**, but justifies RAW+JPEG as the conservative RAW-preserving default.

Sony's store-completion listener is marked added in V6 in the aggregated stubs and may be absent on A7 II. Optional callbacks must be feature detected. Capture completion and storage completion are different events. No host test proves a file reached the card.

## EVF investigation

The production overlay doubles as the smallest useful proof of concept: C1 adds an opaque magenta rectangle and text over the live surface. Inspect on LCD, EVF, half press, capture return and repeated eye-sensor switches. Log active display, display dimensions/aspect, surface bounds and Sony VideoRect. These results remain unverified.

If the live view appears in EVF but UI does not: first compare RGBA4444 graphics with the default and inspect display/OSD listener state. The stub-declared setOSDOutput(FINDER, OSD_OUTPUT_ON) and controlGraphicsOutputAll(true) are candidates for a narrowly scoped experiment; semantics/restoration are not established, so this build does not invoke them. Next inspect Sony native apps' Surface and graphics-layer setup, VideoLayoutStatusListener and Gpelibrary. A camera-owned graphics surface is a more credible next architecture than CPU copying each preview frame. A video-frame callback/GL path would require proving availability, latency and EVF routing. No firmware, bootloader, calibration or persistent system changes are needed or authorized.

## Output strategy

The black bars change composition only. The camera stores full frames. Desktop crop utility uses actual oriented image dimensions and the same integer rounding rule as the mask. At 6000×4000, 65:24 gives 6000×2215 (13.29 MP), y=892. Exact 65:24 with integer pixels would require giving up width; this app keeps full width and rounds height to nearest pixel.

Native extra panoramic JPEG is deferred: CameraEx JpegListener and MediaManager expose candidate paths, but lifetime, file allocation and native indexing need investigation. A full 24 MP ARGB bitmap alone costs about 96 MB, unsuitable as an assumed safe path on this legacy camera. Desktop export leaves RAW untouched and preserves practical JPEG EXIF/ICC metadata.

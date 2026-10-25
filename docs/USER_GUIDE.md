# Shooting with XPan Mode

This prototype is intended for the **A7 II / ILCE-7M2**. The owner reports live preview and shutter capture working in 0.2.0. Autofocus/exposure details, mask alignment and EVF behavior still need focused validation.

## First launch

Use a full-frame lens and start in P mode with digital zoom at 1x. The app requests **RAW+JPEG, large, 3:2, single-shot**, disables APS-C mode when the API supports it, and otherwise inherits exposure, white balance and focus settings. It queries the result and expects 6000×4000. If configuration is rejected, preview can still be tested but shutter capture is disabled; there is no silent fallback to JPEG.

The 65:24 mask is on by default. Full sensor frames are still recorded. The 3:2 mode shows the whole sensor image; it is useful for checking framing before comparing with a saved photograph.

## Physical controls

| Control | Action |
| --- | --- |
| Shutter half-press | Native autofocus; release cancels autofocus |
| Shutter full-press | Native single photo; release cancels any continued shutter request using Sony's API |
| Fn | Cycle the five aspect ratios |
| Left / right | Previous / next aspect ratio |
| Centre | Open/close settings; activate export/retry rows |
| Up in shooting view | Toggle clean view (hide exposure, ratio and hints); choice persists |
| Up / down in settings | Select row |
| Left / right in settings | Change ratio, RAW+JPEG/JPEG output, or alignment mode |
| C1 | Toggle diagnostic magenta rectangle, image boundary and display label |
| Dial 1 | Increment/decrement aperture through CameraEx |
| Dial 2 | Increment/decrement shutter speed through CameraEx |
| MENU | Close settings, or exit from shooting view |

Physical button/dial mappings come from ScalarInput, but their delivery on the owner's body is unverified. No touch input is required. Mode changes, ISO/WB menus, AF area selection, playback, movie recording and the AF/MF–AEL lever are not implemented. Set those in the native camera before launching. This is not a complete replacement for Sony's shooting UI. The native camera may reject aperture/shutter changes depending on its current exposure mode.

Exposure values are read from Sony parameters and refreshed on optional callbacks, AF completion and dial changes. Units and Auto ISO reporting need hardware checks. AF OK reports Sony's callback; it is not a custom focus detector or a focus-point location display. Capture callbacks report status, not an independently verified card write.

## Settings and diagnostics

Ratio, output, alignment and clean-view choices persist. Clean view keeps the black framing mask; capture progress and error messages remain visible. Press UP again to restore the shooting information. **JPEG** is an explicit option and does not retain RAW; leave **RAW + JPEG** selected for the intended non-destructive workflow. The app never selects RAW-only because another PMCA camera has a documented capture problem with that combination.

Alignment defaults to the fitted SurfaceView. **Sony rectangle (experimental)** maps the reported video bounds from device pixels into the Android view. The API's coordinate convention is unverified; compare against the test chart before trusting this option. If no valid rectangle is reported, it falls back to the surface geometry. A difference between the green image boundary and visible live-image edges is a failed alignment test, not something to ignore.

Settings → Export diagnostics → Centre writes a new uniquely named `XPAN/diagnostics-*.txt` under Android's reported external-storage directory. Its actual mapping to the removable card is device-dependent and unverified. The path appears in status (long paths may be shortened to fit the screen). Private app storage also retains `diagnostics.txt`, with logcat tag `XPan`; ADB retrieval is optional only if the owner already has authorized debugging configured. This project does not enable ADB, network services or system patches.

Restart camera releases/reopens the camera and renegotiates the settings. It is disabled while a capture is pending. If a callback is missing, the app waits and shows a timeout message rather than repeatedly triggering the shutter.

## Panoramic RAW files

Use the [RAW DNG companion](RAW_WORKFLOW.md) to make a new RAW file that opens with the selected panoramic crop, preserving the full sensor mosaic and the original ARW. This is a desktop conversion after import; the app does not record cropped ARWs on the card.

## Crop the full-frame image later

```sh
.venv/bin/python tools/crop.py DSC00001.JPG DSC00001-xpan.jpg --ratio 65:24
.venv/bin/python tools/crop.py developed.tif cinema.jpg --ratio 2.39:1 --plan
```

The utility takes the actual stored pixel dimensions, rounds the maximal centered crop to the nearest pixel (half up), and places any odd leftover pixel on the bottom/right. For 6000×4000, 65:24 gives 6000×2215 with rows 892–3106 inclusive. Output is a new file; existing paths and RAW inputs are refused. JPEG exports are recompressed at quality 95. Practical EXIF fields and ICC profile are retained; dimensions/orientation are updated; offset-sensitive MakerNotes and thumbnails are omitted. Metadata is not guaranteed byte-identical.

Crop happens **before EXIF orientation** to match the camera's sensor-space mask. A portrait camera position consequently yields a vertical panorama. For a portrait TIFF already rotated by a RAW developer (orientation 1), add `--vertical-frame` to apply the inverse ratio in that stored raster.

The app does not yet embed a reliable per-frame ratio in ARW/JPEG metadata. Record which ratio belongs to each frame; the log records shutter intentions and optional Sony file callbacks, but those callbacks are not guaranteed available. Select the same ratio in the utility.

Use a full-frame render from the RAW developer, with equivalent lens/distortion correction. ARW raw mosaics may include optical-black margins and dimensions unlike the 6000×4000 active image; do not blindly crop the mosaic or assume an uncorrected RAW render matches Sony's corrected live view. Confirm composition with the calibration checklist first.

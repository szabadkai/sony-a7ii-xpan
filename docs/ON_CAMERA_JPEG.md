# On-camera panoramic JPEG export — 0.3.0 prototype

Version 0.3.0 adds **Panorama JPEG** to settings, enabled by default. After a native shot, the app attempts to save an additional JPEG matching the selected frame. A 65:24 frame produces 6000×2215 pixels. Full-frame RAW and JPEG originals remain in Sony's normal storage. The 3:2 selection uses the original full-frame JPEG and does not create a duplicate.

**Status: implemented and host-tested; not yet installed or verified on the A7 II.** Earlier owner-confirmed preview/capture results concern version 0.2.0.

## How to use

1. Install the 0.3.0 APK using the normal app installer.
2. Open XPan Mode. Select **RAW + JPEG** and **Panorama JPEG → On** in settings.
3. Choose 65:24 and take one photograph. Keep the app open while **Saving panorama** is displayed.
4. On success, the message gives the output dimensions and path. Files are newly named `XPAN/PANORAMAS/XPAN-<image-id>-<unique-id>.jpg` under Android's reported external storage.
5. Check the card through a card reader or file transfer. Compare the JPEG with the full-frame original, and confirm that the matching ARW remains present.

The actual external-storage mapping and file visibility need a device test. We have not established a supported Sony playback-index insertion API. These additional JPEGs may therefore be absent from normal camera playback or MTP photo listings even when accessible through the card filesystem. The app does not write to DCIM or modify Sony's index.

To return to the original capture-only behavior, switch **Panorama JPEG → Off**. Export errors show a message; the app does not edit or delete originals to recover. MENU exits normally and requests cancellation of an in-progress export. A forced shutdown may leave an app-owned `.part` or `.source-` temporary file in the export folder.

## Capture and processing

- Before the shutter request, the app snapshots Sony image-provider IDs (bounded to 100,000 records). This query can add shutter latency on a large card; timing is unmeasured on hardware.
- After native capture completion it waits up to 20 seconds for exactly one new JPEG record and a complete JPEG stream. IDs need not increase monotonically. Multiple new JPEGs are treated as ambiguous, so no panorama is guessed from an older photo.
- The shot's ratio is frozen before capture. Preview is paused and further shutter requests are blocked during export.
- Android API-10 `BitmapRegionDecoder` decodes 16-row bands from the exact sensor-aligned crop. Each band is encoded at JPEG quality 95 and joined with JPEG restart markers. At 6000 pixels wide, one ARGB band is about 384 KB; no full 24 MP Android bitmap is allocated. Decoder/index/encoder overhead is additional and must be measured on the camera.
- The assembler accepts only baseline sequential color JPEGs with identical sampling, quantization and Huffman tables across strips. Unsupported encoder behavior fails explicitly. Output dimensions are checked before publication.
- Files are written under new temporary names and finalized only after encoding and metadata handling succeed. Cleanup touches only files created by that export.
- A process-wide export lock and cancellation on pause prevent overlapping export jobs and camera reopening while a previous job is still finishing.

## Image quality and metadata

The output is re-encoded at quality 95; this is not a lossless JPEG transform. The crop preserves the original orientation tag, so a portrait exposure produces a vertical panorama. EXIF fields supported by the old Android ExifInterface are copied where available. MakerNotes, complete metadata and ICC profiles are not guaranteed to survive; use sRGB JPEG capture for initial testing and retain the originals for archival/editing work.

The app still records full-frame ARWs. Panoramic RAW DNG conversion remains the separate [desktop workflow](RAW_WORKFLOW.md).

## Validation and remaining checks

Host checks cover 1,035,508 decoded grayscale pixel samples across odd dimensions, small images and a 6000×2215 panorama, including more than eight restart intervals. Tests reject truncated streams, wrong strip dimensions, incomplete output, mismatched quantization tables and ambiguous new image IDs. A real photograph was also re-encoded in strips and decoded by Java ImageIO to check color preservation. Android's actual encoder/provider behavior is not simulated by those tests.

API-10 compilation, DEX 035, v1 signing, package inspection, ZIP alignment and the existing geometry/RAW-export tests pass. Host renders check the new settings and saving-progress layouts.

On the body, verify: provider access, storage location, correct 6000×2215 output, intact original ARW/JPEG, visual crop alignment, orientation, encode time, preview return, cancellation, missing/full card behavior, LCD and EVF. Do not call this feature hardware-verified until a cropped JPEG is retrieved and inspected.

API sources: [Android BitmapRegionDecoder](https://developer.android.com/reference/android/graphics/BitmapRegionDecoder), [OpenMemories Sony image provider declarations](https://github.com/ma1co/OpenMemories-Framework/blob/f8df3504d8684433a331ee869e64f67cc8be9730/stubs/src/main/jasmin/com/sony/scalar/provider/AvindexStore%24Images%24Media.j). The selected MIT provider declaration is retained under `third_party/api-reference/`; no firmware implementation is bundled.

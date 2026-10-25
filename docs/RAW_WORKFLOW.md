# Panoramic RAW workflow

The camera records a full-frame Sony ARW. The desktop companion creates a **new RAW DNG with a 65:24 default crop**, preserving the entire sensor mosaic. On a 6000×4000 image, the displayed crop is 6000×2215. This is a RAW file with crop metadata, not a JPEG stored in a DNG container.

This conversion runs on the computer after importing photos. Version 0.2.0 does not write cropped ARWs or DNGs on the camera. We have not found a supported arbitrary-65:24 ARW capture setting in the inspected Sony APIs. On-camera panoramic JPEG export is implemented separately in 0.3.0 and awaits a device test; see ON_CAMERA_JPEG.md.

## Setup

Python 3.12+ is required for the optional RAW dependencies. The camera APK build does not need these packages.

```sh
python3 -m venv .venv
.venv/bin/pip install -r tools/requirements-raw.txt
python3 scripts/bootstrap-raw.py
```

The bootstrap downloads checksum-pinned DNGLab 0.8.0 into `.toolchain/dnglab/`. macOS arm64 was tested. Linux x86_64/arm64 binaries are pinned but not run here. On other hosts, provide a compatible DNGLab 0.8.0 binary using `--dnglab /path/to/dnglab` or `XPAN_DNGLAB`.

DNGLab is an external LGPL-2.1 tool, downloaded separately from its [official release](https://github.com/dnglab/dnglab/releases/tag/v0.8.0); no converter code or binaries are bundled in the app. Its [A7 II camera definition](https://github.com/dnglab/dnglab/blob/v0.8.0/rawler/data/cameras/sony/a7m2.toml) identifies sensor margins and the normal full-frame image area.

## Convert a photograph

Copy the ARW from the camera/card first. Keep the original and choose a new output filename:

```sh
.venv/bin/python tools/raw_panorama.py /path/to/DSC00001.ARW /path/to/DSC00001-xpan.dng
```

Add `--ratio 2.39:1`, `3:1`, `16:9`, or `3:2` to match the frame used when shooting. Version 0.2.0 does not reliably associate the selected ratio with each file, so choose it explicitly when exporting. The default is 65:24.

The tool also accepts a fresh, full-frame A7 II RAW DNG. It rejects rendered/linear DNGs, other camera models, non-square pixel scaling, unexpected dimensions and an existing crop. Existing output DNGs and crop reports are never overwritten.

The output includes a `.dng.crop.json` audit record with the source/output hashes, crop coordinates and verification results. `exports/` is excluded from Git for personal outputs.

## What is preserved

- The source ARW is opened for reading only and its hash is checked again after conversion.
- DNGLab uses lossless DNG compression. The complete decoded RAW pixel checksum is compared between ARW and DNG; a mismatch rejects the output.
- Only `DefaultCropOrigin` and `DefaultCropSize` are changed after conversion. Encoded RAW segment hashes are compared before and after this edit.
- Sensor margins are respected. For the tested file, the normal DNG origin `(12,12)` becomes `(12,904)`, with size `(6000,2215)`. Cropping the sensor's entire stored 6048×4024 mosaic would be incorrect.
- Orientation is retained: a portrait exposure remains a vertical panorama.
- The original ARW is embedded in newly converted DNGs, in addition to remaining untouched as a separate source. This makes files larger; the crop does not discard pixels or reduce RAW storage.

To recover the embedded original into a new directory:

```sh
mkdir recovered
.toolchain/dnglab/dnglab extract DSC00001-xpan.dng recovered
```

## Compatibility and limits

DNG defines crop origin and size in RAW coordinates relative to the active area; see [Apple's DNG property definitions](https://developer.apple.com/documentation/imageio/dng-image-properties) and the [Adobe DNG SDK's validation implementation](https://android.googlesource.com/platform/external/dng_sdk/+/80a267ed1ac714acff455e85ae28c1732777d5b6/source/dng_ifd.cpp). Editors differ in how they expose or override the default crop. We have verified macOS ImageIO rendering, not every RAW editor. Recovering the full frame may require the untouched/embedded ARW rather than an editor's Reset Crop button.

ARW conversion disables embedded rendered previews and thumbnails so a stale full-frame preview does not misrepresent the panorama. DNG input retains its existing previews; a thumbnail-only viewer may therefore show the full frame until it renders the RAW. The tool does not import edits from an adjacent XMP sidecar. Lens profiles, distortion correction and the chosen RAW renderer can affect field of view relative to the camera's preview; physical mask alignment still needs comparison.

## Validation on 2026-10-25

- Owner reports that 0.2.0 live preview and shutter capture work on the ILCE-7M2. EVF behavior remains unconfirmed.
- Read one recent 24,903,680-byte ARW from the connected ILCE-7M2 through standard, read-only PTP object commands. No card files were modified.
- Converted it to a CFA RAW DNG with default crop 6000×2215. ARW and DNG decoded RAW pixel checksums matched.
- Encoded RAW segment hashes were unchanged by the crop metadata edit.
- macOS `sips`/ImageIO read and rendered the DNG at exactly 6000×2215; visually inspected the rendered panorama.
- Extracted the embedded ARW and verified its SHA-256 matches the source byte-for-byte.
- Four additional Python tests cover integer/rational tags, little/big endian files, source preservation, orientation, margins, alternate ratio, overwrite refusal and rejection of conflicting crops. These use synthetic TIFF/DNG fixtures and are not camera evidence.

Personal ARW/DNG photographs and rendered previews remain local and are excluded from the repository.

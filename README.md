# XPan Mode for Sony A7 II

A small, buildable PlayMemories camera app for **Sony ILCE-7M2**, with a black panoramic live-view mask and native camera capture. **Prototype: APK/build verified; camera installation, capture and EVF operation have not been physically verified.**

- [Signed APK](dist/XPanMode-0.1.0.apk) · [SHA-256](dist/XPanMode-0.1.0.apk.sha256)
- [Build instructions](docs/BUILD.md)
- [Install / remove / recover](docs/INSTALL.md)
- [User guide](docs/USER_GUIDE.md)
- [Hardware test checklist](docs/HARDWARE_TEST.md)
- [Research](RESEARCH.md) · [Implementation and limitations](docs/IMPLEMENTATION.md) · [Progress](PROGRESS.md)

## Implemented

Native Sony CameraEx preview/capture, physical half/full shutter, full-size 3:2 RAW+JPEG negotiation, single-shot mode, autofocus requests, inherited exposure/WB, aperture/shutter controls, return-to-preview handling, lifecycle release, persisted aspect/output selection, display diagnostics and a visible EVF probe. Five composition ratios: **65:24**, 2.39:1, 3:1, 16:9, 3:2. The black mask is mapped from sensor coordinates into the preview surface, with an experimental Sony VideoRect mapping option for hardware calibration.

The app requests full-size RAW+JPEG by default and refuses capture if that configuration cannot be established. JPEG-only is an explicit menu choice. It never crops or edits RAW files, accesses backup-setting stores, patches firmware, or automatically installs itself.

## Build

Requires macOS (Apple Silicon with Rosetta 2, or Intel) or Linux x86_64, Python 3, curl, unzip and tar.

```sh
python3 scripts/bootstrap.py
./scripts/build.sh
```

The bootstrap installs a pinned, checksum-verified toolchain under `.toolchain/`, not globally. The APK uses API 10, DEX 035 and v1 signing. No Gradle, NDK, AndroidX or firmware libraries are bundled. A reusable local signing key is created in `.keys/`; keep it for future updates.

## Desktop panorama export

```sh
python3 -m venv .venv
.venv/bin/pip install -r tools/requirements.txt
.venv/bin/python tools/crop.py DSC00001.JPG DSC00001-xpan.jpg --ratio 65:24
XPAN_PYTHON=.venv/bin/python ./scripts/test.sh
```

Export operates on a full-frame JPEG or developed TIFF/PNG, never an ARW. It refuses to overwrite files and writes a crop-record JSON beside the result. On a 6000×4000 landscape image the crop is `(0, 892, 6000, 3107)` → **6000×2215**, retaining the original full frame. See the user guide for portrait orientation and RAW-development alignment.

## Evidence boundary

Host tests verify the crop math, lifecycle capture gate, reflection names against upstream SDK declarations, non-overwrite export and APK structure/signing. They cannot validate Sony firmware behavior. The next step is the owner-run LCD/EVF and capture checklist, beginning with a spare card and the magenta C1 probe. No camera has been accessed during development.

MIT license; third-party notices are in `third_party/`.

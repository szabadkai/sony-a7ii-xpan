# Local validation — 2026-10-25

## 0.2.0-prototype interface update

**Host validation and owner-authorized 0.2.0 installation passed.** PMCA-RE completed the update and returned `org.xpan.camera` version `0.2.0-prototype` in the ILCE-7M2 firmware 4.01 application inventory. Sanitized evidence: [INSTALLATION_RESULT_0.2.0.json](INSTALLATION_RESULT_0.2.0.json). Launch, preview, capture and EVF behavior remain untested. The historical installation report below is specific to 0.1.0.

- Package `org.xpan.camera`, versionCode 2, min/target API 10.
- DEX 035, 37 application classes, no packaged Sony implementations or native libraries.
- Six launcher-icon density variants resolve through the application manifest.
- v1 signature and ZIP alignment verified; certificate SHA-256 matches 0.1.0: `7d1a50a82edca3eb91ddd3b26f9ace48c2bd4ba18a4549300b90e57f71f359b2`.
- Java core: 125,022 checks passed; all six Python tests passed.
- Generated 32 host layouts from production drawing code at 320×240, 640×360, 640×480 and 960×720. Visually reviewed shooting, settings, clean-view error, full-frame and diagnostic-fallback examples, including small and widescreen settings layouts. Android fonts and hardware composition remain untested.
- Copied only `app/` and `scripts/` into a fresh temporary directory, with no compiled classes. Rebuilt using the same pinned toolchain and retained key through explicit paths. Both unsigned and signed APKs matched byte-for-byte.

| Artifact | SHA-256 |
| --- | --- |
| `dist/XPanMode-0.2.0-prototype.apk` | `1b4d53483abd0b303e254475bed57771a637522e1d7b2724f5c0e7aa0954e95e` |
| Aligned unsigned APK | `12fc04add27f6859ca40755590c790a88dd94f3367a1f8b5c27a4d0dc285b097` |

## Historical 0.1.0 validation and installation


**Result: signed prototype APK built and inspected. Installation has subsequently been confirmed on ILCE-7M2 firmware 4.01; app operation remains unverified.**

## Artifact

| Item | Observed result |
| --- | --- |
| APK | `dist/XPanMode-0.1.0.apk` |
| Package / version | `org.xpan.camera` / `0.1.0-prototype`, versionCode 1 |
| API | min 10, target 10; actual API-10 compilation bootclasspath |
| DEX | 035, 33 application classes |
| Sony implementation classes in APK | None; reflection binds real firmware classes |
| Native libraries | None |
| Signature | apksigner verification passed; v1 true, v2/v3/v4 false; RSA 2048 |
| Alignment | zipalign 4-byte verification passed |
| Touchscreen | Explicitly not required |
| Camera access / install | Confirmed via MTP/libusb; see INSTALLATION_RESULT.json |

APK SHA-256:

```text
997a0316b6d45d57dd4d0c6345b985dc7e8b1b34472277afced73c1ff0653835
```

Unsigned aligned payload SHA-256:

```text
bae52733f90d61a07f31bc81b361885561d79fd68025c576822e84d550f2fb1f
```

Signer certificate SHA-256 (identity of the delivered build, not a public trust assertion):

```text
7d1a50a82edca3eb91ddd3b26f9ace48c2bd4ba18a4549300b90e57f71f359b2
```

## Reproduction performed

On macOS arm64, exported Git source checkpoint `8cbbe23` to a new temporary directory, with no precompiled classes. Seeded only the downloaded tool archives, then ran the bootstrap, which verified their hashes and extracted a fresh toolchain. Ran the build with the retained local signing key. Compared SHA-256 of both unsigned and signed APKs with the workspace build: **identical bytes for both**. Later changes to this report/README/progress/license do not affect APK compilation. Linux/Intel tool execution was not tested.

This proves rebuilding from clean source with the pinned archives and same key on this host. It does not claim that a fresh random signing key gives identical signed output. Initial archive downloads were retrieved over the network successfully during this session; no camera firmware files were downloaded or modified.

## Host tests passed

- Java core suite: 125,022 assertions across fixed crop examples, 5,000 deterministic dimension cases × five ratios, bounds, centering, nonsquare pixels, letterboxing and capture-session generation handling.
- Six Python unittest tests: reflective API names/descriptors/key codes against pinned upstream declarations; output geometry; input hash preservation, overwrite refusal, RAW refusal; EXIF portrait composition.
- APK inspection: manifest/resources/classes, real legacy DEX header, package-owned class definitions only, no native libraries, v1 signature entry.
- External verifier: `apksigner verify --verbose --print-certs --min-sdk-version 10` and `zipalign -c 4` passed.

No simulator camera, mocks or fabricated screenshots were used as hardware evidence. The tests cannot prove API semantics, live field of view, actual card writes or EVF composition.

## Definition-of-done status

| Requirement | Status |
| --- | --- |
| Valid reproducible APK build | **PASS locally** |
| Installs on A7 II | **PASS: ILCE-7M2 firmware 4.01**, package returned in installed-app inventory |
| Functioning camera preview | **Implemented, NOT RUN on body** |
| Correctly aligned 65:24 mask | **Math tested; physical alignment NOT RUN** |
| Physical shutter controls | **Implemented, NOT RUN on body** |
| Full RAW preserved / new files saved | **Implemented intent; card output NOT RUN** |
| EVF overlay verified | **NOT RUN — primary remaining milestone** |
| Safe exit / resource release | **Implemented; physical behavior NOT RUN** |

Use `HARDWARE_TEST.md` to record the missing results. This release remains a prototype until those checks pass.

## Subsequent physical installation

The owner connected the camera and explicitly requested installation. PMCA-RE completed the app install and returned org.xpan.camera 0.1.0-prototype in the device application inventory. Model ILCE-7M2 and firmware 4.01 were returned. The same APK hash above was installed. The inventory labels the Android package 4.1.2-323; this is device-reported package metadata, not a direct Build.VERSION.SDK_INT reading. No firmware, bootloader, calibration, or protected settings were changed. Preview/capture/EVF tests remain pending. Sanitized machine-readable evidence is in INSTALLATION_RESULT.json.

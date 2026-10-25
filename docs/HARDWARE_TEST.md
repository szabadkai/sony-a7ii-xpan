# A7 II hardware acceptance checklist

**Installation is confirmed on ILCE-7M2 firmware 4.01; all app-operation rows below are NOT RUN.** Complete this on a physical **ILCE-7M2**. An emulator, desktop crop preview or successful build is not evidence of EVF operation. Do not install automatically from a computer script.

Record: tester/date, body model, firmware, lens, native exposure/AF/WB settings, card, APK SHA-256, report filename. Keep test images and note filenames. Start with a spare card, a charged battery, a full-frame lens and a short exposure in P mode. Never erase existing images as part of testing.

## 1. Install and initial preview

- [x] Normal PMCA install accepted the signed APK on ILCE-7M2 firmware 4.01 via MTP/libusb on macOS 26.6.2. See INSTALLATION_RESULT.json.
- [ ] Launch from Application List. Record runtime SDK/model from diagnostic report.
- [ ] LCD shows a responsive sensor preview, with no stretching or unexpected crop.
- [ ] Status indicates ready RAWJPEG. If disabled, export the actual error; do not bypass it or unlock protected settings.
- [ ] 3:2 shows the whole active frame. Compare all four scene edges with Sony native shooting preview.

## 2. The critical EVF experiment

Press **C1**: an opaque magenta rectangle reading **EVF PROBE** should appear inside the live image, plus a green outer-image boundary. Record each row separately, with a phone photograph through the finder if practical.

| Situation | Live image | Magenta rectangle/text | Black bars aligned | Status |
| --- | --- | --- | --- | --- |
| LCD, no keys pressed | — | — | — | NOT RUN |
| EVF, eye sensor active | — | — | — | NOT RUN |
| LCD during half-press AF | — | — | — | NOT RUN |
| EVF during half-press AF | — | — | — | NOT RUN |
| LCD after first capture returns | — | — | — | NOT RUN |
| EVF after first capture returns | — | — | — | NOT RUN |
| LCD → EVF → LCD, ten cycles | — | — | — | NOT RUN |
| EVF → LCD → EVF, ten cycles | — | — | — | NOT RUN |
| Pause/exit and relaunch | — | — | — | NOT RUN |

Capture an exported report both before and after switching displays. Compare PANEL/FINDER IDs, `res_w/res_h`, physical aspect, Android dimensions, surface dimensions and VideoRect. Check whether display switching changes Android dimensions or just stretches a shared framebuffer. Verify reported pixel aspect against a circular target.

If the rectangle appears on LCD but not EVF, this is an LCD-only result. Check whether the preview itself switches correctly. Do not call it EVF compatible or try firmware patches. See the non-invasive follow-up architecture in RESEARCH.md.

## 3. Geometric calibration

Use a tripod and a planar chart/wall with numbered horizontal/vertical lines reaching all frame edges. Keep subject, focal length, focus distance and lens-correction settings unchanged.

- [ ] Start with 3:2. The green image boundary matches the actual live image on each display.
- [ ] Switch to 65:24. Record which chart lines are immediately inside/outside the black bars and the left/right edges.
- [ ] Capture RAW+JPEG. Confirm card output is new files; previous photographs remain intact.
- [ ] Confirm JPEG pixel dimensions 6000×4000 and RAW active image is full frame using a RAW viewer. ARW file size alone is not proof.
- [ ] Apply desktop crop `(0,892,6000,3107)` to the full-frame JPEG. Compare its boundaries to recorded live framing.
- [ ] Repeat on EVF, all five ratios, multiple lens focal lengths and after display switches.
- [ ] If necessary compare Surface alignment versus experimental Sony rectangle; record both results. Do not choose based on appearance alone.
- [ ] Verify lens corrections, stabilization, AF magnification and APS-C mode do not silently change the preview field of view. Disable magnification for calibration.
- [ ] Compare a full-frame developed RAW with identical correction settings; document any warp/field-of-view difference. A single affine mask cannot compensate for different nonlinear distortion corrections.

Acceptance: a repeatable crop boundary match on both displays (within preview raster rounding), no stretched scene, and known correspondence to the active image area. If this fails, the mask is not validated even when the bars look correct.

## 4. Camera behavior

- [ ] Half-press initiates autofocus; release cancels it. Test AF-S, AF-C and manual separately; record limitations.
- [ ] Full-press once saves one photograph. Holding full-press does not issue repeated app calls.
- [ ] Rapid repeated presses never overlap captures; both RAW and JPEG are usable.
- [ ] Capture returns promptly to live view; probe and mask remain. Record timing and callbacks.
- [ ] Test focus-and-recompose, AE response to lighting, P/A/S/M separately, and aperture/shutter dials. Compare displayed exposure to EXIF.
- [ ] Test RAW+JPEG with short exposure first, then slower shutter; do not remove power while writing. The watchdog only reports waiting and does not force completion.
- [ ] Explicit JPEG option saves JPEG only. Returning to RAWJPEG restores RAW-preserving capture.
- [ ] Missing/full card errors are visible and do not cause retry loops. Do not fill/format the user's existing card for this test; use a prepared test card.
- [ ] Pause/resume, repeated launches, lens detach/reattach while safely idle, and display switch during capture do not leak resources or lose the mask.
- [ ] MENU exit returns to native camera. Verify output format, size, aspect, drive and APS-C restored; photograph normally afterwards.
- [ ] Aspect/output preferences survive relaunch. Diagnostic export produces only a new XPAN report file.
- [ ] Uninstall removes the app and leaves photographs intact.

## Results / promotion decision

Attach the completed table, diagnostic report, example full-frame JPEG/ARW, cropped export and exact build hash. Promote from prototype only after installation, preview, physical shutter, full-resolution RAW preservation, EVF mask and clean exit are all observed. If EVF fails, label the release LCD-only and retain the failed test evidence for the next investigation.

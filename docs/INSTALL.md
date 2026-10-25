# Install, remove, recover — ILCE-7M2

This is a normal removable PMCA Android application. Versions 0.1.0 and 0.2.0 were successfully installed on the owner’s ILCE-7M2, firmware 4.01. Version 0.2.0 updated the same package with the retained signing certificate. Preview, capture and EVF behavior still await testing. See [0.2.0 installation evidence](INSTALLATION_RESULT_0.2.0.json) and the historical INSTALLATION_RESULT.json.

1. Confirm the label is **ILCE-7M2** and that `MENU → Application` exists. Record `MENU → Setup → Version`. Firmware 4.01 is listed by Sony, but do not update firmware just to run this prototype.
2. Start with a charged battery and a spare/test memory card. Keep existing photographs backed up. Set P mode, a full-frame lens, digital zoom 1x, and short exposure for the first test. Exit other camera apps.
3. Obtain [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE/releases). Use **App Installer**, not Tweaks, updater mode or service mode. No OpenMemories-Tweak installation or protected-setting unlock is required.
4. Set the camera's USB connection to **Mass Storage**, connect a data-capable USB cable, and close Photos/other software claiming the device.
5. In pmca-gui's **Install app** tab choose **Select an apk**, click **Open apk...**, select `dist/XPanMode-0.2.0-prototype.apk`, then click **Install selected app**. Alternatively, from PMCA-RE's directory:

   ```sh
   python3 pmca-console.py install -f '/absolute/path/to/XPanMode-0.2.0-prototype.apk'
   ```

6. Wait for the normal app installation to finish. Disconnect safely. Open `MENU → Application → Application List → XPan Mode`.
7. Run the [hardware checklist](HARDWARE_TEST.md), beginning with preview and C1's magenta probe. A signing/package pass on the computer does not establish device installation success.

PMCA-RE source setup on Linux: create a virtual environment, install that checkout's `requirements.txt`, then use `pmca-console.py`. Its README documents libusb on Linux. macOS release binaries are less tested upstream; Mass Storage communication may require Sony's Camera Driver and OS-specific permissions. If the installer cannot find the body, use the upstream instructions or a supported Windows/Linux computer; do not switch to firmware/service modifications to force installation.

Official PlayMemories online services are not the delivery path here; PMCA-RE transfers the local APK.

## Update and remove

Use the same application ID and signing key to install an update over this prototype. If signing keys differ, remove the old app first, then install; that removes app preferences/private logs but must not remove camera photographs. Export diagnostics before removal if needed.

Remove using `MENU → Application → Application Management → Manage and Remove → XPan Mode` (menu wording may vary with camera language). The export folder `XPAN` contains app diagnostics and is not automatically removed. The app does not create or delete folders under DCIM; Sony's capture engine writes new photographs normally.

## Exit or frozen app

Press MENU from shooting view to exit (from the settings screen, MENU closes settings first). The app stops preview, unregisters listeners and releases CameraEx. It attempts to restore the native output settings it changed; if interrupted during capture it skips those writes. Check Quality, Image Size, Aspect Ratio, Drive and APS-C mode in native camera menus after an abnormal exit.

If unresponsive, wait for the card-access indicator to stop, then try powering the camera off. Do not remove the card or battery during active writing. If the body remains frozen after waiting for writing to cease, power off and remove/reinsert the battery as a last resort. Reopen the native camera first and verify its settings/files. Do not reset/format the card, update firmware or touch calibration data to recover this app. Record the last visible status and diagnostics before trying again where possible.

## Verified route on this Mac

On macOS 26.6.2 arm64, Mass Storage failed with USB access/claim errors. The successful route was **MTP**, the source PMCA-RE installer with `-d libusb`, and normal macOS administrator authentication. No Sony driver was installed. The interpreter, dependencies, installer and APK were copied to a private temporary directory because the privileged process could not access the Documents project folder. The returned camera inventory confirms XPan Mode installed. This does not establish that every macOS setup requires or supports the same route.

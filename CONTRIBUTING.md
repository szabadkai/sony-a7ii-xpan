# Contributing

The most useful contribution right now is a reproducible **ILCE-7M2 hardware report**, especially whether the Android mask reaches the EVF. Follow [the checklist](docs/HARDWARE_TEST.md) and use the hardware-test issue template. Installation on firmware 4.01 is confirmed; preview, capture and EVF claims must remain unverified until observed.

For code changes, explain the problem, the Sony API evidence and the validation you performed. Keep production code on the real Scalar APIs, with no emulator substitutes packaged in the APK. Run the relevant host tests and APK build described in [BUILD.md](docs/BUILD.md). A host test is not a camera test.

Preserve the upstream MIT notices. Do not copy GPL implementation code into this MIT project without resolving the licensing of the resulting work. Avoid firmware, bootloader, calibration or protected-setting changes; the intended architecture is a removable camera application.

Reports are public. Redact serial numbers, account identifiers and credentials, and avoid uploading complete PMCA installer result JSON. Do not upload personal photographs unless you intend to publish them. APK signing keys belong outside version control.

# Third-party notices

XPan Mode is MIT licensed. Camera initialization/lifecycle and physical shutter sequence derive from PMCADemo (MIT, ma1co 2015). Display API behavior was studied in OpenMemories-Framework (MIT, ma1co). Legacy ShutterListener handling was studied in TimeLapse (MIT, Jonas Juffinger 2017). License texts accompany this file.

`api-reference/*.j` are unmodified selected Jasmin declarations from OpenMemories-Framework, at the revision in `research/sources.lock.json`. They are documentation/host-audit inputs only; they are neither assembled nor packaged. Production binds to the camera firmware's real implementation through Java reflection.

Recipe Lab was inspected, but no source, assets, fonts, native code or build scripts were copied. Its current GPL-3.0 code is not a dependency. Sony-PMCA-RE is a separate installer, not an APK dependency.

The bootstrap downloads Google Android SDK tools and Eclipse Temurin JDK under their own licenses into an ignored local directory. The desktop crop utility depends on Pillow (HPND license). No toolchain binaries or Pillow are redistributed in the source package or APK.

# Build from a clean checkout

## Pinned toolchain

| Component | Version / purpose |
| --- | --- |
| JDK | Eclipse Temurin 17.0.18+8; compiler host only |
| Java source / class target | 7; compiled against the Android API 10 boot classpath |
| Android SDK platform | 2.3.3 / API 10, revision 2 |
| Camera runtime | PMCA-RE documents Android 2.3.7, also API 10; verify on the actual body |
| Android build tools | 30.0.3: aapt, D8, zipalign, apksigner |
| DEX | 035, D8 `--min-api 10` |
| SDK manifest | min 10 / target 10 |
| APK signing | RSA 2048, JAR/v1 only (API-10-compatible digest selection by apksigner) |
| NDK / Gradle | Not needed; no native code or online Maven resolution |
| Optional desktop export | Python 3 + Pillow 11.3.0 |

Modern host tools do not imply modern runtime APIs: javac uses the **actual API-10 android.jar as bootclasspath**, so unsupported Java/Android methods fail compilation. No Java 8 language features or new libraries are used.

## macOS / Linux

Install basic command-line prerequisites (Python 3, curl, tar, unzip). On Linux use x86_64 with normal libc, libstdc++ and zlib packages; on Apple Silicon the old x86_64 aapt/zipalign executables need Rosetta 2. The JDK itself is native arm64 on Apple Silicon. Linux ARM is not automated. macOS Apple Silicon was tested here; Linux/Intel bootstrap URLs are pinned but execution on those hosts is unverified.

```sh
python3 scripts/bootstrap.py
./scripts/build.sh
```

Downloads go to `.toolchain/downloads`; every archive is verified against SHA-256 in `scripts/toolchain.lock.json` before extraction. No global package changes. Android SDK terms apply to Google's downloaded SDK tools. The bootstrap checks that aapt can execute. Its historical extraction folder `android-11` actually contains build-tools **30.0.3**; API 10 comes from `android-2.3.3_r02-linux` on both hosts.

Outputs:

- `dist/XPanMode-0.1.0.apk` — signed installable-format prototype.
- `dist/XPanMode-0.1.0.apk.sha256` — digest.
- `build/unsigned.apk` — reproducible aligned unsigned payload.
- `build/badging.txt` — package, permissions, SDK and launcher inspection.
- `build/signature.txt` — verification and certificate fingerprint.

The build fails if the DEX version differs from 035, a Sony stub is packaged as an implementation, native libraries appear, signature verification fails, or ZIP alignment fails.

## Keys and reproducibility

The first build creates `.keys/prototype.jks` with alias `xpan` and local prototype password `android`. The private key is intentionally excluded from Git and source archives. Keep it; replacing it prevents updating an already installed copy. A clean checkout generates a different key and hence different signed bytes. To reproduce the delivered APK exactly, build with the same retained local key and pinned tools. The unsigned APK should reproduce without it. ZIP entry dates/order are normalized; RSA v1 signing is deterministic with the same key.

For a separate signing key, supply environment variables:

```sh
export XPAN_KEYSTORE=/absolute/path/to/existing.jks
export XPAN_KEY_ALIAS=your_alias
# Set XPAN_STORE_PASS and XPAN_KEY_PASS securely in your shell environment.
./scripts/build.sh
```

An explicitly selected missing keystore is an error; the script will not replace it. Tool overrides for experienced users: `JAVA_HOME`, `XPAN_BUILD_TOOLS`, `XPAN_ANDROID_JAR`. Overrides invalidate the pinned-toolchain reproducibility claim unless they point to identical files.

## Tests

```sh
python3 -m venv .venv
.venv/bin/pip install -r tools/requirements.txt
XPAN_PYTHON=.venv/bin/python ./scripts/test.sh
```

Tests cover fixed and randomized crop geometry, nonsquare display pixels, letterboxing, stale lifecycle callbacks, overlapping captures, Sony API declarations, EXIF rotation and non-destructive exports. No test simulates a Sony capture and calls it hardware verification.

## Investigated build failures

The initial historical build-tools URL returned 404; Google's repository metadata gives a hash-prefixed macOS URL, now pinned. D8 30.0.3 does not strip quotation marks in its response-file paths; the build now writes relative unquoted paths for D8 and uses a source root with no spaces in relative filenames. This was verified from a workspace whose absolute path contains spaces. javac response files retain their own required quoting.

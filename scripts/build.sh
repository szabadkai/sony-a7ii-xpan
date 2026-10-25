#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$PWD"
VERSION="$(python3 - <<'PY_VERSION'
import xml.etree.ElementTree as ET
m=ET.parse('app/AndroidManifest.xml').getroot()
print(m.attrib['{http://schemas.android.com/apk/res/android}versionName'])
PY_VERSION
)"
APK="dist/XPanMode-$VERSION.apk"
export XPAN_APK_OUTPUT="$APK"
if [ -z "${JAVA_HOME:-}" ]; then
  if [ -d "$ROOT/.toolchain/jdk-17.0.18+8/Contents/Home" ]; then
    export JAVA_HOME="$ROOT/.toolchain/jdk-17.0.18+8/Contents/Home"
  else export JAVA_HOME="$ROOT/.toolchain/jdk-17.0.18+8"; fi
fi
export PATH="$JAVA_HOME/bin:$PATH"
BT="${XPAN_BUILD_TOOLS:-$ROOT/.toolchain/android-11}"
AJ="${XPAN_ANDROID_JAR:-$ROOT/.toolchain/android-2.3.3_r02-linux/android.jar}"
for f in "$JAVA_HOME/bin/javac" "$BT/aapt" "$BT/zipalign" "$BT/lib/d8.jar" "$AJ"; do
  [ -f "$f" ] || { echo "Missing $f; run python3 scripts/bootstrap.py" >&2; exit 1; }
done
rm -rf build/classes build/dex build/gen
mkdir -p build/classes build/dex build/gen dist .keys
"$BT/aapt" package -f -m -J build/gen -M app/AndroidManifest.xml -S app/res -I "$AJ"
python3 - <<'PY'
from pathlib import Path
paths=sorted(Path('app/src').rglob('*.java'))+sorted(Path('build/gen').rglob('*.java'))
Path('build/sources.txt').write_text(''.join('"'+str(p)+'"\n' for p in paths))
PY
javac -encoding UTF-8 -source 7 -target 7 -Xlint:-options -bootclasspath "$AJ" -d build/classes @build/sources.txt
python3 - <<'PY'
from pathlib import Path
Path('build/classes.txt').write_text(''.join(str(p)+'\n' for p in sorted(Path('build/classes').rglob('*.class'))))
PY
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 10 --lib "$AJ" --output build/dex @build/classes.txt
"$BT/aapt" package -f -M app/AndroidManifest.xml -S app/res -I "$AJ" -F build/resources.apk
# Normalize ZIP metadata for repeatable unsigned APKs (and signed APKs with the same key).
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/resources.apk') as src, zipfile.ZipFile('build/unaligned.apk','w') as out:
 entries={name:src.read(name) for name in src.namelist()}
 entries['classes.dex']=open('build/dex/classes.dex','rb').read()
 for name in sorted(entries):
  z=zipfile.ZipInfo(name,(2000,1,1,0,0,0));z.compress_type=zipfile.ZIP_DEFLATED;z.external_attr=0o644<<16
  out.writestr(z,entries[name])
PY
"$BT/zipalign" -f 4 build/unaligned.apk build/unsigned.apk
KS="${XPAN_KEYSTORE:-$ROOT/.keys/prototype.jks}"
export XPAN_STORE_PASS="${XPAN_STORE_PASS:-android}"
export XPAN_KEY_PASS="${XPAN_KEY_PASS:-$XPAN_STORE_PASS}"
ALIAS="${XPAN_KEY_ALIAS:-xpan}"
if [ ! -f "$KS" ]; then
  if [ -n "${XPAN_KEYSTORE:-}" ]; then echo "Specified keystore does not exist" >&2;exit 1;fi
  keytool -genkeypair -keystore "$KS" -storetype JKS -alias "$ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass:env XPAN_STORE_PASS -keypass:env XPAN_KEY_PASS -dname 'CN=XPan Local Prototype'
fi
"$BT/apksigner" sign --ks "$KS" --ks-key-alias "$ALIAS" --ks-pass env:XPAN_STORE_PASS --key-pass env:XPAN_KEY_PASS \
  --min-sdk-version 10 --v1-signing-enabled true --v2-signing-enabled false --v3-signing-enabled false --v4-signing-enabled false \
  --out "$APK" build/unsigned.apk
"$BT/apksigner" verify --verbose --print-certs --min-sdk-version 10 "$APK" > build/signature.txt
"$BT/aapt" dump badging "$APK" > build/badging.txt
"$BT/zipalign" -c 4 "$APK"
python3 scripts/inspect_apk.py "$APK"
python3 - <<'PY'
import hashlib,pathlib,os
p=pathlib.Path(os.environ['XPAN_APK_OUTPUT']);s=hashlib.sha256(p.read_bytes()).hexdigest()
p.with_suffix('.apk.sha256').write_text(s+'  '+p.name+'\n')
print('Built '+str(p)+' SHA256 '+s)
PY

#!/usr/bin/env python3
"""Download a project-local pinned toolchain. No global installs; no camera access."""
import hashlib, json, os, platform, subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LOCK = json.loads((ROOT/'scripts/toolchain.lock.json').read_text())
DEST = ROOT/'.toolchain'

def fetch(spec, name):
    path = DEST/'downloads'/name
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists() or hashlib.sha256(path.read_bytes()).hexdigest() != spec['sha256']:
        temp = path.with_suffix('.part')
        subprocess.run(['curl','-fL','--retry','3',spec['url'],'-o',str(temp)], check=True)
        if hashlib.sha256(temp.read_bytes()).hexdigest() != spec['sha256']:
            temp.unlink()
            raise SystemExit('Checksum mismatch: '+name)
        temp.replace(path)
    print('Verified '+name)
    return path

def main():
    system = {'Darwin':'mac','Linux':'linux'}.get(platform.system())
    arch = {'arm64':'aarch64','aarch64':'aarch64','x86_64':'x64','AMD64':'x64'}.get(platform.machine())
    if system is None or system+'-'+str(arch) not in LOCK['jdk']:
        raise SystemExit('Supported: macOS arm64/x64, Linux x86_64. Linux ARM needs x86_64 build-tools emulation; not automated.')
    print('Downloads Eclipse Temurin 17.0.18+8 and Android SDK platform 10 / build-tools 30.0.3.\n'
          'Android SDK terms: https://developer.android.com/studio/terms')
    jdk = fetch(LOCK['jdk'][system+'-'+arch], 'jdk.tar.gz')
    bt = fetch(LOCK['build_tools'][system], 'build-tools.zip')
    api = fetch(LOCK['platform'], 'platform.zip')
    subprocess.run(['tar','-xzf',str(jdk),'-C',str(DEST)], check=True)
    subprocess.run(['unzip','-qo',str(bt),'-d',str(DEST)], check=True)
    subprocess.run(['unzip','-qo',str(api),'-d',str(DEST)], check=True)
    try: subprocess.run([str(DEST/'android-11/aapt'),'version'], check=True)
    except (OSError,subprocess.CalledProcessError):
        raise SystemExit('aapt cannot execute. On Apple Silicon enable Rosetta 2; on Linux use x86_64 with libstdc++/zlib installed.')
    print('Toolchain ready. Run ./scripts/build.sh')
if __name__ == '__main__': main()

#!/usr/bin/env python3
"""Install the pinned optional DNGLab CLI inside this project, without global changes."""
import hashlib
from pathlib import Path
import platform
import urllib.request
import zipfile

ASSETS = {
    ('Darwin', 'arm64'): ('dnglab-macos-arm64_v0.8.0.zip', 'ee70805cb60f18d5ed62548c4e595f6cee0a33407e15358eb1766a46afc1c16e'),
    ('Linux', 'x86_64'): ('dnglab_linux_x64', 'd45334659d008b010966f56c3a472677f510a1775d78debd45f5078612ac0683'),
    ('Linux', 'aarch64'): ('dnglab_linux_aarch64', '141a438add075718308c833f9b070aa77944dc843d8b12ac44c5f2432005c6e0'),
}


def main():
    key = (platform.system(), platform.machine())
    if key not in ASSETS:
        raise SystemExit('No pinned binary for this host; supply DNGLab 0.8.0 with --dnglab instead.')
    name, expected = ASSETS[key]
    target = Path(__file__).resolve().parents[1]/'.toolchain/dnglab'
    target.mkdir(parents=True, exist_ok=True)
    archive = target/name
    url = 'https://github.com/dnglab/dnglab/releases/download/v0.8.0/'+name
    if not archive.exists():
        with urllib.request.urlopen(url, timeout=60) as response:
            data = response.read()
        if hashlib.sha256(data).hexdigest() != expected:
            raise SystemExit('DNGLab download checksum mismatch')
        archive.write_bytes(data)
    if hashlib.sha256(archive.read_bytes()).hexdigest() != expected:
        raise SystemExit('DNGLab archive checksum mismatch')
    binary = target/'dnglab'
    if name.endswith('.zip'):
        with zipfile.ZipFile(archive) as z:
            binary.write_bytes(z.read('dnglab'))
    else:
        binary.write_bytes(archive.read_bytes())
    binary.chmod(0o755)
    print('Installed pinned DNGLab 0.8.0:', binary)


if __name__ == '__main__':
    main()

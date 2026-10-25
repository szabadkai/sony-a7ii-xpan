#!/usr/bin/env python3
"""Inspect DEX class definitions, not strings: Sony names are expected references, never bundled implementations."""
import struct, sys, zipfile
from pathlib import Path

def inspect(path):
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        assert 'AndroidManifest.xml' in names and 'classes.dex' in names
        assert any(n.endswith('.RSA') and n.startswith('META-INF/') for n in names)
        assert not any(n.startswith('lib/') for n in names), 'Native library unexpectedly packaged'
        d = z.read('classes.dex')
        assert d[:8] == b'dex\n035\0', repr(d[:8])
        u32 = lambda off: struct.unpack_from('<I', d, off)[0]
        strings = []
        for i in range(u32(56)):
            off = u32(u32(60) + 4*i)
            while d[off] & 128: off += 1
            off += 1
            strings.append(d[off:d.index(b'\0', off)].decode('utf-8'))
        types = [strings[u32(u32(68)+4*i)] for i in range(u32(64))]
        classes = [types[u32(u32(100)+32*i)] for i in range(u32(96))]
        assert classes and all(c.startswith('Lorg/xpan/camera/') for c in classes), classes
        print('APK: DEX 035; %d app classes; v1 RSA entry; no Sony stubs or native binaries' % len(classes))
        return classes
if __name__ == '__main__': inspect(sys.argv[1])

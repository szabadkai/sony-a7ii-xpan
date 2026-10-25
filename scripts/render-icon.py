#!/usr/bin/env python3
"""Optional icon regeneration with librsvg (rsvg-convert). PNGs are checked in; normal builds need no SVG tools."""
from pathlib import Path
import shutil,subprocess
root=Path(__file__).resolve().parents[1]
renderer=shutil.which('rsvg-convert')
if not renderer:raise SystemExit('Install librsvg/rsvg-convert to regenerate icons. App builds use the checked-in PNGs.')
for density,size in [('ldpi',36),('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
 out=root/'app/res'/('drawable-'+density)/'ic_launcher.png';out.parent.mkdir(parents=True,exist_ok=True)
 subprocess.run([renderer,'-w',str(size),'-h',str(size),'-o',str(out),str(root/'assets/icon.svg')],check=True)
subprocess.run([renderer,'-w','256','-h','256','-o',str(root/'docs/images/icon.png'),str(root/'assets/icon.svg')],check=True)
print('Generated six legacy-compatible launcher PNGs and documentation icon')

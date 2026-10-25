#!/usr/bin/env python3
"""Recover the exact inspected upstream sources; never executes upstream scripts."""
import json, subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
for name,spec in json.loads((root/'research/sources.lock.json').read_text()).items():
    dest=root/'research/upstream'/name
    if dest.exists():
        actual=subprocess.check_output(['git','-C',str(dest),'rev-parse','HEAD'],text=True).strip()
        if actual!=spec['commit']:raise SystemExit('Existing checkout differs; refusing to reset '+str(dest))
        continue
    dest.parent.mkdir(parents=True,exist_ok=True)
    subprocess.run(['git','clone',spec['url'],str(dest)],check=True)
    subprocess.run(['git','-C',str(dest),'checkout','--detach',spec['commit']],check=True)

#!/usr/bin/env python3
"""Create a new panoramic RAW DNG from an A7 II ARW or full-frame DNG; never modify the source."""
import argparse
from fractions import Fraction
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET

from crop import RATIOS, crop_box

ROOT = Path(__file__).resolve().parents[1]


def digest(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def raw_page(tif):
    candidates = []
    seen = set()
    def visit(pages):
        for page in pages:
            if page.offset in seen or len(seen) >= 32:
                raise ValueError('Cyclic or unexpectedly complex DNG directory structure')
            seen.add(page.offset)
            if int(page.photometric) == 32803 and not page.is_reduced:
                candidates.append(page)
            if page.subifds:
                visit(page.pages)
    visit(tif.pages)
    if len(candidates) != 1:
        raise ValueError('Expected one CFA RAW image; rendered/linear or multi-image DNGs are not supported')
    return candidates[0]


def numbers(tag):
    value = tag.value
    if not isinstance(value, tuple):
        value = (value,)
    if int(tag.dtype) == 5:
        return tuple(Fraction(value[i], value[i+1]) for i in range(0, len(value), 2))
    if int(tag.dtype) not in (3, 4):
        raise ValueError('Unsupported numeric DNG tag type')
    return tuple(Fraction(x) for x in value)


def integer_pair(tag):
    values = numbers(tag)
    if len(values) != 2 or any(v.denominator != 1 for v in values):
        raise ValueError('Expected two integer crop coordinates')
    return tuple(int(v) for v in values)


def segment_digest(tif, page):
    """Hash encoded RAW segments directly, without demosaicing or decoding them."""
    stream = tif.filehandle
    result = hashlib.sha256()
    segments = list(zip(page.dataoffsets, page.databytecounts))
    if not segments:
        raise ValueError('RAW image contains no data segments')
    for offset, size in segments:
        if offset < 8 or size <= 0 or offset + size > stream.size:
            raise ValueError('Invalid RAW segment bounds')
        stream.seek(offset)
        result.update(size.to_bytes(8, 'little'))
        remaining = size
        while remaining:
            data = stream.read(min(1024*1024, remaining))
            if not data:
                raise ValueError('Truncated RAW data')
            result.update(data)
            remaining -= len(data)
    return result.hexdigest()


def set_pair(tag, values):
    if int(tag.dtype) == 5:
        tag.overwrite(tuple(x for v in values for x in (v, 1)))
    else:
        tag.overwrite(tuple(values))


def patch_crop(path, ratio):
    import tifffile
    with tifffile.TiffFile(path, mode='r+') as tif:
        root = tif.pages[0]
        if 50706 not in root.tags:
            raise ValueError('Input is not a DNG')
        model = str(root.tags.valueof(272, ''))
        if model != 'ILCE-7M2':
            raise ValueError('This workflow is calibrated only for Sony ILCE-7M2, got: '+model)
        page = raw_page(tif)
        if 50719 not in page.tags or 50720 not in page.tags:
            raise ValueError('DNG must have explicit DefaultCropOrigin and DefaultCropSize')
        origin = integer_pair(page.tags[50719])
        size = integer_pair(page.tags[50720])
        if size != (6000, 4000):
            raise ValueError('Expected uncropped 6000x4000 default image area, got '+str(size))
        if 50718 in page.tags and numbers(page.tags[50718]) != (1, 1):
            raise ValueError('Non-square RAW pixel scaling is not supported')
        if 51125 in page.tags and numbers(page.tags[51125]) != (0, 0, 1, 1):
            raise ValueError('Input already has a user crop; use a fresh full-frame DNG')
        for parent in (root, page):
            xmp = parent.tags.valueof(700)
            if xmp:
                try:
                    document = ET.fromstring(xmp)
                except ET.ParseError as error:
                    raise ValueError('Cannot validate existing XMP crop') from error
                crop_tag = '{http://ns.adobe.com/camera-raw-settings/1.0/}HasCrop'
                for node in document.iter():
                    if node.attrib.get(crop_tag, '').lower() == 'true' or (node.tag == crop_tag and (node.text or '').lower() == 'true'):
                        raise ValueError('Input already has an XMP crop; use a fresh full-frame DNG')
        active = page.tags.valueof(50829, (0, 0, page.imagelength, page.imagewidth))
        if len(active) != 4 or not (0 <= active[0] < active[2] <= page.imagelength and 0 <= active[1] < active[3] <= page.imagewidth):
            raise ValueError('Invalid DNG active area')
        if min(origin) < 0 or origin[0]+size[0] > active[3]-active[1] or origin[1]+size[1] > active[2]-active[0]:
            raise ValueError('Default crop exceeds sensor active area')
        before = segment_digest(tif, page)
        box = crop_box(*size, *RATIOS[ratio])
        new_origin = (origin[0]+box[0], origin[1]+box[1])
        new_size = (box[2]-box[0], box[3]-box[1])
        set_pair(page.tags[50719], new_origin)
        set_pair(page.tags[50720], new_size)
        orientation = root.tags.valueof(274, 1)
    with tifffile.TiffFile(path) as tif:
        page = raw_page(tif)
        if integer_pair(page.tags[50719]) != new_origin or integer_pair(page.tags[50720]) != new_size:
            raise ValueError('Crop metadata verification failed')
        after = segment_digest(tif, page)
        if before != after:
            raise ValueError('RAW data changed during crop metadata edit')
    return {'model': model, 'ratio': ratio, 'original_default_crop_origin': origin,
            'original_default_crop_size': size, 'crop_box_ltrb_exclusive': box,
            'default_crop_origin': new_origin, 'default_crop_size': new_size,
            'orientation': orientation, 'encoded_raw_sha256_before': before,
            'encoded_raw_sha256_after': after, 'raw_data_preserved': True}


def converter_path(explicit=None):
    path = explicit or os.environ.get('XPAN_DNGLAB') or str(ROOT/'.toolchain/dnglab/dnglab')
    resolved = shutil.which(path)
    if not resolved:
        raise ValueError('DNGLab not found. Run scripts/bootstrap-raw.py or pass --dnglab /path/to/dnglab')
    version = subprocess.check_output([resolved, '--version'], text=True).strip()
    if version != 'dnglab 0.8.0':
        raise ValueError('Expected pinned dnglab 0.8.0, got '+version)
    return resolved


def pixel_checksum(converter, path):
    result = subprocess.run([converter, 'analyze', '--raw-checksum', str(path)], check=True, capture_output=True, text=True)
    # DNGLab prints one MD5 digest for the complete decoded RAW mosaic.
    matches = re.findall(r'(?<![0-9a-f])[0-9a-f]{32}(?![0-9a-f])', result.stdout.lower())
    if len(matches) != 1:
        raise ValueError('Could not read DNGLab RAW pixel checksum')
    return matches[0]


def export(source, destination, ratio='65:24', dnglab=None):
    source, destination = Path(source).resolve(), Path(destination).absolute()
    if source.suffix.lower() not in ('.arw', '.dng') or not source.is_file():
        raise ValueError('Input must be an existing A7 II ARW or full-frame RAW DNG')
    if destination.suffix.lower() != '.dng':
        raise ValueError('Output must be a new .dng file')
    if ratio not in RATIOS:
        raise ValueError('Unknown ratio')
    report = destination.with_suffix('.dng.crop.json')
    if destination.exists() or report.exists() or source == destination.resolve():
        raise FileExistsError('Refusing to replace source, existing DNG or crop report')
    original_digest = digest(source)
    published = report_created = False
    try:
        with tempfile.TemporaryDirectory(prefix='.xpan-raw-', dir=destination.parent) as temporary:
            staged = Path(temporary)/'panorama.dng'
            converter = converter_path(dnglab) if source.suffix.lower() == '.arw' else None
            checksum = None
            if converter:
                checksum = pixel_checksum(converter, source)
                subprocess.run([converter, 'convert', '--compression', 'lossless', '--crop', 'best',
                                '--dng-preview', 'false', '--dng-thumbnail', 'false', '--embed-raw', 'true',
                                str(source), str(staged)], check=True, capture_output=True, text=True)
                if not staged.is_file():
                    raise ValueError('Converter did not create a DNG')
            else:
                shutil.copyfile(source, staged)
            result = patch_crop(staged, ratio)
            if converter:
                output_checksum = pixel_checksum(converter, staged)
                if checksum != output_checksum:
                    raise ValueError('ARW and DNG decoded RAW pixels differ; output rejected')
                result.update(decoded_raw_md5=checksum, arw_to_dng_pixels_verified=True)
            if digest(source) != original_digest:
                raise ValueError('Source changed during export; output rejected')
            result.update(source=source.name, output=destination.name, source_sha256=original_digest,
                          output_sha256=digest(staged), method='DNG default crop metadata; full RAW mosaic retained',
                          embedded_previews='disabled for ARW conversion; inherited for DNG input')
            # Atomic no-replace publication, on the destination filesystem.
            os.link(staged, destination)
            published = True
            with report.open('x') as stream:
                report_created = True
                json.dump(result, stream, indent=2)
                stream.write('\n')
            return result
    except BaseException:
        if published:
            destination.unlink(missing_ok=True)
        if report_created:
            report.unlink(missing_ok=True)
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input')
    parser.add_argument('output')
    parser.add_argument('--ratio', choices=RATIOS, default='65:24')
    parser.add_argument('--dnglab')
    args = parser.parse_args()
    try:
        print(json.dumps(export(args.input, args.output, args.ratio, args.dnglab), indent=2))
    except (ValueError, OSError, subprocess.SubprocessError, ImportError) as error:
        parser.exit(2, str(error)+'\n')


if __name__ == '__main__':
    main()

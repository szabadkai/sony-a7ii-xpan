#!/usr/bin/env python3
"""Non-destructive panorama export from a full-frame JPEG/PNG/TIFF. Never writes or decodes RAW."""
import argparse, json, os
from pathlib import Path

RATIOS = {'65:24': (65,24), '2.39:1': (239,100), '3:1': (3,1), '16:9': (16,9), '3:2': (3,2)}

def crop_box(width, height, n=65, d=24):
    if min(width,height,n,d) <= 0: raise ValueError('Dimensions and ratio must be positive')
    w,h=width,height
    if width*d > height*n: w=max(1,min(width,(height*n+d//2)//d))
    else: h=max(1,min(height,(width*d+n//2)//n))
    x,y=(width-w)//2,(height-h)//2
    return x,y,x+w,y+h

def export(source, destination, ratio='65:24', plan_only=False, vertical_frame=False):
    from PIL import Image, ImageOps
    source,destination=Path(source),Path(destination)
    if source.suffix.lower() not in ('.jpg','.jpeg','.png','.tif','.tiff'):
        raise ValueError('Use a full-frame JPEG or developed TIFF/PNG. RAW files are never modified or decoded.')
    if source.resolve()==destination.resolve(): raise ValueError('Output must differ from source')
    if destination.exists(): raise FileExistsError('Refusing to replace existing output: '+str(destination))
    sidecar=destination.with_suffix(destination.suffix+'.crop.json')
    if sidecar.exists(): raise FileExistsError('Refusing to replace crop record: '+str(sidecar))
    ext=destination.suffix.lower()
    fmt={'.jpg':'JPEG','.jpeg':'JPEG','.png':'PNG','.tif':'TIFF','.tiff':'TIFF'}.get(ext)
    if fmt is None: raise ValueError('Output must be JPEG, PNG, or TIFF')
    with Image.open(source) as original:
        n,d=RATIOS[ratio]
        if vertical_frame: n,d=d,n
        box=crop_box(*original.size,n,d)
        # Crop in the stored camera raster before rotating for display: this is what the camera mask shows.
        # An EXIF-rotated portrait capture therefore produces a vertical panorama, not a different slice.
        cropped=ImageOps.exif_transpose(original.crop(box))
        plan={'source':str(source.resolve()),'stored_size':list(original.size),'ratio':ratio,
              'vertical_frame':vertical_frame,'input_orientation':original.getexif().get(274,1),
              'box_ltrb_exclusive':list(box),'output_size':list(cropped.size),
              'output':str(destination.resolve()),'rounding':'nearest pixel, half up; floor centered origin'}
        if plan_only: return plan
        exif=cropped.getexif()
        exif[274]=1
        exif[256],exif[257]=cropped.size
        # Refresh nested EXIF pixel dimensions; omit embedded thumbnails and offset-sensitive MakerNotes.
        if 34665 in exif:
            sub=exif.get_ifd(34665)
            sub[40962],sub[40963]=cropped.size
            sub.pop(37500,None)
        exif.pop(37500,None)
        exif.pop(513,None);exif.pop(514,None)
        options={'exif':exif.tobytes()}
        if original.info.get('icc_profile'): options['icc_profile']=original.info['icc_profile']
        if fmt=='JPEG':
            if cropped.mode not in ('RGB','L'): cropped=cropped.convert('RGB')
            options.update(quality=95,subsampling=0)
        # Exclusive creation makes overwrite refusal hold even if another process races us.
        with destination.open('xb') as out:
            try: cropped.save(out,format=fmt,**options)
            except BaseException:
                out.close();destination.unlink();raise
        with sidecar.open('x') as out: json.dump(plan,out,indent=2);out.write('\n')
        return plan

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('input');p.add_argument('output');p.add_argument('--ratio',choices=RATIOS,default='65:24')
    p.add_argument('--plan',action='store_true',help='Print crop geometry without writing files')
    p.add_argument('--vertical-frame',action='store_true',help='For portrait files already rotated by a RAW developer: use the inverse crop ratio in the stored raster')
    a=p.parse_args()
    try: print(json.dumps(export(a.input,a.output,a.ratio,a.plan,a.vertical_frame),indent=2))
    except (ValueError,OSError) as error: p.exit(2,str(error)+'\n')
if __name__=='__main__': main()

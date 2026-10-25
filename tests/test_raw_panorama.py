"""Structural synthetic-DNG tests; real camera validation is recorded separately."""
import hashlib
import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]/'tools'))
import raw_panorama

HAS_TIFF = importlib.util.find_spec('tifffile') is not None


@unittest.skipUnless(HAS_TIFF, 'Optional RAW dependencies: tools/requirements-raw.txt')
class RawPanoramaTests(unittest.TestCase):
    def fixture(self, path, rational=False, orientation=1, size=(6000,4000), model='ILCE-7M2', user_crop=None, xmp=None, byteorder='<'):
        import tifffile
        pair_type = '2I' if rational else 'I'
        def pair(values):
            return tuple(x for value in values for x in (value,1)) if rational else values
        tags = [(50706,'B',4,(1,4,0,0),False), (272,'s',0,model,False),
                (50719,pair_type,2,pair((12,12)),False), (50720,pair_type,2,pair(size),False),
                (50829,'I',4,(0,0,4024,6022),False), (274,'H',1,orientation,False)]
        if user_crop is not None:
            tags.append((51125,'2I',4,user_crop,False))
        if xmp is not None:
            tags.append((700,'B',len(xmp),xmp,False))
        tifffile.imwrite(path,shape=(4024,6048),dtype='uint16',photometric=32803,
                         metadata=None,extratags=tags,byteorder=byteorder)

    def test_raw_data_orientation_and_margins_preserved(self):
        for rational,byteorder in ((False,'<'),(True,'>')):
            with self.subTest(rational=rational,byteorder=byteorder), tempfile.TemporaryDirectory() as folder:
                source=Path(folder)/'source.dng';out=Path(folder)/'xpan.dng'
                self.fixture(source,rational=rational,orientation=6,byteorder=byteorder)
                before=raw_panorama.digest(source)
                result=raw_panorama.export(source,out)
                self.assertEqual(result['default_crop_origin'],(12,904))
                self.assertEqual(result['default_crop_size'],(6000,2215))
                self.assertEqual(result['orientation'],6)
                self.assertEqual(result['encoded_raw_sha256_before'],result['encoded_raw_sha256_after'])
                self.assertEqual(raw_panorama.digest(source),before)
                self.assertTrue(out.with_suffix('.dng.crop.json').exists())

    def test_refuses_existing_output_and_report(self):
        with tempfile.TemporaryDirectory() as folder:
            source=Path(folder)/'source.dng';out=Path(folder)/'xpan.dng'
            self.fixture(source)
            out.write_bytes(b'keep me')
            with self.assertRaises(FileExistsError):raw_panorama.export(source,out)
            self.assertEqual(out.read_bytes(),b'keep me')
            out.unlink();out.with_suffix('.dng.crop.json').write_text('keep report')
            with self.assertRaises(FileExistsError):raw_panorama.export(source,out)
            self.assertFalse(out.exists())
            with self.assertRaises(FileExistsError):raw_panorama.export(source,source)

    def test_rejects_wrong_model_and_preexisting_crop(self):
        for options in ({'model':'ILCE-7M3'}, {'size':(6000,2215)},
                        {'user_crop':(1,4,0,1,3,4,1,1)},
                        {'xmp':b'<x xmlns:c="http://ns.adobe.com/camera-raw-settings/1.0/" c:HasCrop="True"/>'}):
            with self.subTest(options=options),tempfile.TemporaryDirectory() as folder:
                source=Path(folder)/'source.dng';out=Path(folder)/'xpan.dng'
                self.fixture(source,**options);before=raw_panorama.digest(source)
                with self.assertRaises(ValueError):raw_panorama.export(source,out)
                self.assertFalse(out.exists())
                self.assertFalse(out.with_suffix('.dng.crop.json').exists())
                self.assertEqual(raw_panorama.digest(source),before)

    def test_other_ratios(self):
        with tempfile.TemporaryDirectory() as folder:
            source=Path(folder)/'source.dng';out=Path(folder)/'wide.dng'
            self.fixture(source)
            result=raw_panorama.export(source,out,'3:1')
            self.assertEqual(result['default_crop_size'],(6000,2000))
            self.assertEqual(result['default_crop_origin'],(12,1012))

if __name__=='__main__':unittest.main()

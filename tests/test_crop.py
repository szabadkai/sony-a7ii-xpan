import hashlib, json, sys, tempfile, unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from crop import crop_box, export
from PIL import Image

class CropTests(unittest.TestCase):
    def test_geometry(self):
        self.assertEqual(crop_box(6000,4000),(0,892,6000,3107))
        self.assertEqual(crop_box(6000,4000,3,2),(0,0,6000,4000))
        self.assertEqual(crop_box(6000,4000,3,1),(0,1000,6000,3000))
        self.assertEqual(crop_box(4000,6000),(0,2261,4000,3738))
        with self.assertRaises(ValueError): crop_box(0,4000)
    def test_export_preserves_source_metadata_and_refuses_overwrite(self):
        with tempfile.TemporaryDirectory() as d:
            src=Path(d)/'original.jpg';out=Path(d)/'pano.jpg'
            exif=Image.Exif();exif[271]='SONY';exif[272]='ILCE-7M2';exif[274]=1
            Image.new('RGB',(600,400),(30,60,90)).save(src,exif=exif)
            before=hashlib.sha256(src.read_bytes()).hexdigest()
            plan=export(src,out,plan_only=True);self.assertFalse(out.exists())
            self.assertEqual(plan['output_size'],[600,222])
            export(src,out)
            self.assertEqual(hashlib.sha256(src.read_bytes()).hexdigest(),before)
            with Image.open(out) as result:
                self.assertEqual(result.size,(600,222));self.assertEqual(result.getexif()[271],'SONY')
            with self.assertRaises(FileExistsError):export(src,out)
            with self.assertRaises(ValueError):export(src,src)
            with self.assertRaises(ValueError):export(Path(d)/'photo.ARW',Path(d)/'raw.jpg')
    def test_orientation(self):
        with tempfile.TemporaryDirectory() as d:
            src=Path(d)/'rotated.jpg';out=Path(d)/'pano.png'
            exif=Image.Exif();exif[274]=6
            Image.new('RGB',(600,400)).save(src,exif=exif)
            plan=export(src,out)
            self.assertEqual(plan['stored_size'],[600,400])
            with Image.open(out) as image:
                self.assertEqual(image.size,(222,600));self.assertEqual(image.getexif()[274],1)
if __name__=='__main__':unittest.main()

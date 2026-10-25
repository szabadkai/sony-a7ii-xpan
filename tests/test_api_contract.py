"""Guard against typos in the reflective Sony API bindings using pinned upstream declarations."""
import re, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class ApiContractTests(unittest.TestCase):
    def test_reflective_names_exist_in_upstream(self):
        api='\n'.join(p.read_text() for p in (ROOT/'third_party/api-reference').glob('*.j'))
        names=set(re.findall(r'^\.method .*? (\w+)\(',api,re.M))
        source='\n'.join(p.read_text() for p in (ROOT/'app/src').rglob('*.java'))
        # Named API calls and getter/setter arrays must refer to declarations in the actual SDK.
        used=set(re.findall(r'"((?:get|set|is|increment|decrement|create|cancel|release|finish|change)[A-Z][A-Za-z]+)"',source))
        self.assertFalse(used-names, 'Undeclared Sony APIs: '+repr(used-names))
    def test_required_descriptors(self):
        camera=(ROOT/'third_party/api-reference/CameraEx.j').read_text()
        for signature in ['open(ILcom/sony/scalar/hardware/CameraEx$OpenOptions;)Lcom/sony/scalar/hardware/CameraEx;',
                          'getNormalCamera()Landroid/hardware/Camera;',
                          'createParametersModifier(Landroid/hardware/Camera$Parameters;)Lcom/sony/scalar/hardware/CameraEx$ParametersModifier;']:
            self.assertIn(signature,camera)
        modifier=(ROOT/'third_party/api-reference/CameraEx$ParametersModifier.j').read_text()
        self.assertIn('getImagePixelSize(Ljava/lang/String;Ljava/lang/String;)Landroid/hardware/Camera$Size;',modifier)
    def test_key_codes(self):
        declarations=(ROOT/'third_party/api-reference/ScalarInput.j').read_text()
        for name,value in [('S1_1',516),('S1_2',517),('S2',518),('MENU',514),('FN',520),('CUSTOM1',622),('ENTER',232)]:
            self.assertIn('ISV_KEY_%s I = %d'%(name,value),declarations)
if __name__=='__main__':unittest.main()

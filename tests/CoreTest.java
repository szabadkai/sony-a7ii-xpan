import org.xpan.camera.Geometry;
import org.xpan.camera.CaptureGate;
import java.util.Random;
public final class CoreTest {
    static int checks=0;
    static void check(boolean ok,String message) {checks++;if(!ok)throw new AssertionError(message);}
    static void near(double actual,double expected) {check(Math.abs(actual-expected)<1e-8,actual+" != "+expected);}
    public static void main(String[] args) {
        Geometry.Box c=Geometry.crop(6000,4000,65,24);
        near(c.left,0);near(c.top,892);near(c.right,6000);near(c.bottom,3107);
        Geometry.Box display=Geometry.fit(640,480,6000,4000,1);
        near(display.left,0);near(display.top,26);near(display.width(),640);near(display.height(),427);
        Geometry.Box mask=Geometry.map(c,6000,4000,display);
        near(mask.left,0);near(mask.right,640);near(mask.top,26+892.0/4000*427);
        Geometry.Box nonSquare=Geometry.fit(640,480,6000,4000,1.125);
        near(nonSquare.width(),640);near(nonSquare.height(),480);
        Geometry.Box wide=Geometry.fit(1920,1080,6000,4000,1);
        near(wide.left,150);near(wide.width(),1620);near(wide.top,0);
        Random random=new Random(65);
        for(int i=0;i<5000;i++) {
            int w=1+random.nextInt(12000),h=1+random.nextInt(12000);
            for(int[] ratio:Geometry.RATIOS) {
                Geometry.Box b=Geometry.crop(w,h,ratio[0],ratio[1]);
                check(b.left>=0&&b.top>=0&&b.right<=w&&b.bottom<=h,"bounds");
                check(b.width()>0&&b.height()>0,"positive crop");
                check(Math.abs(b.left-(w-b.right))<=1&&Math.abs(b.top-(h-b.bottom))<=1,"center");
                check(b.width()==w||b.height()==h,"retain one full dimension");
                if(b.width()==w)check(Math.abs(b.height()-w*(double)ratio[1]/ratio[0])<=1,"height rounding");
                else check(Math.abs(b.width()-h*(double)ratio[0]/ratio[1])<=1,"width rounding");
            }
        }
        CaptureGate gate=new CaptureGate();
        check(!gate.begin(),"closed camera cannot capture");int old=gate.open();
        check(gate.begin(),"first capture");check(!gate.begin(),"duplicate capture denied");
        gate.close();int current=gate.open();check(gate.begin(),"resumed capture");
        check(!gate.end(old)&&gate.busy(),"stale callback cannot clear current capture");
        check(gate.end(current)&&!gate.busy(),"current completion");
        System.out.println("Core tests: "+checks+" checks passed");
    }
}

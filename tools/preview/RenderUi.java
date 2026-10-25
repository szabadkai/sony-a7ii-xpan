import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.xpan.camera.Geometry;
import org.xpan.camera.UiRenderer;

/** Host rendering of the SAME UI code as the APK. A synthetic grid is not a camera/EVF screenshot. */
public final class RenderUi implements UiRenderer.Drawing {
    private final Graphics2D g;
    private RenderUi(Graphics2D graphics){g=graphics;}
    private void color(int argb){g.setColor(new Color(argb,true));}
    public void rect(float l,float t,float r,float b,int argb){color(argb);g.fill(new java.awt.geom.Rectangle2D.Float(l,t,r-l,b-t));}
    public void round(float l,float t,float r,float b,float radius,int argb){color(argb);g.fill(new RoundRectangle2D.Float(l,t,r-l,b-t,radius*2,radius*2));}
    public float measure(String value,float size,boolean bold){g.setFont(new Font("SansSerif",bold?Font.BOLD:Font.PLAIN,1).deriveFont(size));return (float)g.getFontMetrics().getStringBounds(value,g).getWidth();}
    public void text(String value,float x,float y,float size,int argb,boolean bold){measure(value,size,bold);color(argb);g.drawString(value,x,y);}
    public static void main(String[] args)throws Exception {
        File dir=new File(args.length>0?args[0]:"build/ui-preview");dir.mkdirs();
        for(int[] size:new int[][]{{640,480},{640,360},{320,240},{960,720}}) {
            for(String mode:new String[]{"shooting","settings","error","clean","full-frame","probe","jpeg-warning","alignment-fallback","panorama-settings","panorama-progress"}) {
                BufferedImage image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(new Color(0x303d39));g.fillRect(0,0,size[0],size[1]);g.setColor(new Color(0x4d5e55));
                for(int i=0;i<size[0];i+=size[0]/12)g.drawLine(i,0,i,size[1]);
                for(int i=0;i<size[1];i+=size[1]/8)g.drawLine(0,i,size[0],i);
                UiRenderer.State s=new UiRenderer.State();s.width=size[0];s.height=size[1];s.configured=true;s.preview=true;
                s.exposure="f/5.6   1/250s   ISO 100";s.status="Ready";s.focus="AF OK";s.display="FINDER";
                s.menu="settings".equals(mode)||"jpeg-warning".equals(mode);s.menuRow=s.menu?1:0;s.panorama=true;
                if("panorama-settings".equals(mode)){s.menu=true;s.menuRow=2;}
                if("panorama-progress".equals(mode)){s.exporting=true;s.preview=false;s.status="Saving panorama 60%";}
                s.ratio="full-frame".equals(mode)?4:0;s.clean="clean".equals(mode)||"error".equals(mode);
                s.probe="probe".equals(mode)||"alignment-fallback".equals(mode);s.experimental="alignment-fallback".equals(mode);
                if("jpeg-warning".equals(mode))s.format="jpeg";
                if("error".equals(mode)) {s.alert=true;s.status="Capture disabled: the camera did not accept full-size RAW + JPEG. Open settings to retry.";}
                s.source=Geometry.fit(s.width,s.height,6000,4000,1);
                s.crop=Geometry.map(Geometry.crop(6000,4000,Geometry.RATIOS[s.ratio][0],Geometry.RATIOS[s.ratio][1]),6000,4000,s.source);
                new UiRenderer().draw(new RenderUi(g),s);g.dispose();
                ImageIO.write(image,"png",new File(dir,mode+"-"+size[0]+"x"+size[1]+".png"));
            }
        }
        System.out.println("Rendered 40 host previews from production UiRenderer; no camera behavior simulated");
    }
}

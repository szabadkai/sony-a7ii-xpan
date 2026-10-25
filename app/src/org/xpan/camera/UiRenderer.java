package org.xpan.camera;

/** Lightweight shared drawing code. Android Canvas in production, Java2D for honest host layout previews. */
public final class UiRenderer {
    public interface Drawing {
        void rect(float l,float t,float r,float b,int color);
        void round(float l,float t,float r,float b,float radius,int color);
        void text(String text,float x,float baseline,float size,int color,boolean bold);
        float measure(String text,float size,boolean bold);
    }
    public static final class State {
        public int width,height,ratio,menuRow;
        public boolean configured,preview,busy,alert,menu,probe,clean,experimental,sonyRectAvailable,exporting,panorama;
        public String format="rawjpeg",exposure="",status="",focus="",display="";
        public Geometry.Box crop,source;
    }
    private static final int INK=0xfff2eedf, MUTED=0xffa8aea8, AMBER=0xffeab65b;
    private static final int BLACK=0xff000000, PANEL=0xff141716, LINE=0xff343a35, GREEN=0xffa4d6ad;
    private static final String[] RATIOS={"65:24","2.39:1","3:1","16:9","3:2"};
    private static final String[] NAMES={"XPAN","CINEMA","ULTRA","WIDE","FULL FRAME"};
    private Drawing d;
    private float w,h,u,pad;
    private String fit(String value,float size,boolean bold,float width) {
        if(value==null||width<=0)return "";
        if(d.measure(value,size,bold)<=width)return value;
        String suffix="...";
        if(d.measure(suffix,size,bold)>width)return "";
        int end=value.length();
        while(end>0&&d.measure(value.substring(0,end)+suffix,size,bold)>width)end--;
        return value.substring(0,end)+suffix;
    }
    private void label(String value,float x,float y,float size,int color,boolean bold,float width) {
        d.text(fit(value,size,bold,width),x,y,size,color,bold);
    }
    private void right(String value,float edge,float y,float size,int color,boolean bold,float width) {
        String clipped=fit(value,size,bold,width);d.text(clipped,edge-d.measure(clipped,size,bold),y,size,color,bold);
    }
    private void wrap(String value,float x,float y,float size,int color,float width,int lines) {
        if(value==null)return;
        String remaining=value.trim();
        for(int line=0;line<lines&&remaining.length()>0;line++) {
            if(line==lines-1||d.measure(remaining,size,false)<=width) {
                label(remaining,x,y+line*size*1.35f,size,color,false,width);break;
            }
            int end=remaining.length();
            while(end>1&&d.measure(remaining.substring(0,end),size,false)>width)end--;
            int space=remaining.lastIndexOf(' ',end);
            if(space>0)end=space;
            label(remaining.substring(0,end),x,y+line*size*1.35f,size,color,false,width);
            remaining=remaining.substring(end).trim();
        }
    }
    public void draw(Drawing drawing,State s) {
        if(s.width<=0||s.height<=0||s.crop==null||s.source==null)return;
        d=drawing;w=s.width;h=s.height;u=Math.min(w/640f,h/480f);pad=20*u;
        // The exact existing sensor-space crop remains the only source of the mask boundaries.
        float top=(float)s.crop.top,bottom=(float)s.crop.bottom;
        d.rect(0,0,w,top,BLACK);d.rect(0,bottom,w,h,BLACK);
        d.rect(0,top,(float)s.crop.left,bottom,BLACK);d.rect((float)s.crop.right,top,w,bottom,BLACK);
        if(s.menu){settings(s);return;}
        if(!s.clean) {
            // On ratios with little black margin, use a thin readable backplate inside the image.
            if(top<60*u)d.rect(0,0,w,60*u,0xd9000000);
            if(h-bottom<74*u)d.rect(0,h-74*u,w,h,0xd9000000);
            label("XPAN",pad,30*u,17*u,AMBER,true,64*u);
            label(RATIOS[s.ratio],pad+76*u,32*u,25*u,INK,true,w*.45f);
            label(NAMES[s.ratio],pad+78*u,49*u,10*u,MUTED,false,w*.4f);
            String focus=s.focus.length()==0?(s.preview?"LIVE":"WAIT"):s.focus;
            right(focus,w-pad,31*u,15*u,"AF OK".equals(focus)?GREEN:MUTED,true,110*u);
            String output="rawjpeg".equals(s.format)?"RAW + JPG":"JPEG";
            float badgeWidth=d.measure(output,12*u,true)+20*u;
            d.round(w-pad-badgeWidth,h-66*u,w-pad,h-41*u,4*u,0xff252b25);
            right(output,w-pad-10*u,h-49*u,12*u,"rawjpeg".equals(s.format)?INK:AMBER,true,badgeWidth-20*u);
            label(s.exposure,pad,h-48*u,19*u,INK,false,w-pad*3-badgeWidth);
            String hint="Fn frame   Center settings   UP clean";
            if(s.status.length()>0&&!s.status.startsWith("Ready"))hint=s.status;
            label(hint,pad,h-20*u,12*u,MUTED,false,w-pad*2);
        }
        // Clean view never conceals a capture in progress or an error.
        if(s.busy||s.exporting||s.alert||!s.configured||!s.preview) {
            float boxW=Math.min(w-pad*2,480*u),left=(w-boxW)/2;
            float y=Math.max(62*u,h-148*u);
            d.round(left,y,left+boxW,y+66*u,6*u,0xf0141716);
            d.rect(left,y+8*u,left+3*u,y+58*u,AMBER);
            String heading=s.exporting?"SAVING PANORAMA":s.busy?"CAPTURING":(s.alert?"CAMERA MESSAGE":(!s.configured?"CAMERA NOT READY":"STARTING LIVE VIEW"));
            label(heading,left+14*u,y+21*u,12*u,AMBER,true,boxW-28*u);
            wrap(s.status,left+14*u,y+42*u,12*u,INK,boxW-28*u,2);
        }
        if(s.probe) {
            d.rect(w*.1f,h*.43f,w*.34f,h*.57f,0xffff00ff);
            label("EVF PROBE",w*.115f,h*.515f,17*u,BLACK,true,w*.21f);
            String mode=s.experimental?(s.sonyRectAvailable?"SONY RECT / EXPERIMENTAL":"SONY RECT UNAVAILABLE / SURFACE"):"SURFACE";
            label(s.display+" / "+mode,pad,75*u,11*u,AMBER,false,w-pad*2);
            Geometry.Box b=s.source;float stroke=2*u;
            d.rect((float)b.left,(float)b.top,(float)b.right,(float)b.top+stroke,GREEN);
            d.rect((float)b.left,(float)b.bottom-stroke,(float)b.right,(float)b.bottom,GREEN);
            d.rect((float)b.left,(float)b.top,(float)b.left+stroke,(float)b.bottom,GREEN);
            d.rect((float)b.right-stroke,(float)b.top,(float)b.right,(float)b.bottom,GREEN);
        }
    }
    private void settings(State s) {
        d.rect(0,0,w,h,PANEL);
        d.round(pad,22*u,pad+42*u,38*u,2*u,INK);
        d.rect(pad+4*u,26*u,pad+38*u,34*u,PANEL);
        label("XPan Mode",pad+55*u,39*u,23*u,INK,true,w-pad*2-140*u);
        right("SETTINGS",w-pad,36*u,11*u,AMBER,true,100*u);
        d.rect(pad,60*u,w-pad,61*u,LINE);
        String[] titles={"Frame","File format","Panorama JPEG","Preview alignment","Export diagnostics","Restart camera"};
        String[] values={RATIOS[s.ratio]+"  /  "+NAMES[s.ratio],"rawjpeg".equals(s.format)?"RAW + JPEG":"JPEG only",
            s.panorama?"On *":"Off",s.experimental?"Sony rectangle *":"Surface","PRESS CENTER","PRESS CENTER"};
        String[] help={"Choose a frame with LEFT / RIGHT. Full-size photographs are retained.",
            "rawjpeg".equals(s.format)?"RAW + JPEG keeps the full sensor image for later editing.":"JPEG only: no RAW file will be saved. LEFT / RIGHT to change.",
            s.panorama?"Adds a cropped JPEG in XPAN/PANORAMAS after capture. Experimental; keeps originals.":"Save full-frame originals only. LEFT / RIGHT enables the extra cropped JPEG.",
            s.experimental?"Experimental device coordinates. Validate on both displays before trusting this alignment.":"Mask follows the fitted live-image surface. Hardware alignment still needs validation.",
            "Save a new report to the XPAN folder. Remove private details before sharing.",
            "Release and reopen the camera if preview or configuration failed."};
        for(int i=0;i<6;i++) {
            float y=(72+i*44)*u;boolean selected=i==s.menuRow;
            if(selected) {
                d.round(pad-8*u,y,w-pad+8*u,y+40*u,5*u,0xff2d322b);
                d.rect(pad-8*u,y+8*u,pad-5*u,y+32*u,AMBER);
            }
            float valueWidth=w*.49f;
            label(titles[i],pad+8*u,y+26*u,18*u,selected?INK:MUTED,selected,w-pad*2-valueWidth-22*u);
            right(values[i],w-pad-8*u,y+26*u,i<4?16*u:11*u,selected?AMBER:MUTED,i==0,valueWidth);
        }
        wrap(help[s.menuRow],pad,366*u,14*u,INK,w-pad*2,2);
        if(s.status.length()>0&&!s.status.startsWith("Ready"))
            label(s.status,pad,412*u,11*u,AMBER,false,w-pad*2);
        d.rect(pad,h-48*u,w-pad,h-47*u,LINE);
        label("UP / DOWN select    LEFT / RIGHT change",pad,h-25*u,11*u,MUTED,false,w*.69f);
        right("MENU back",w-pad,h-25*u,11*u,INK,true,w*.25f);
    }
}

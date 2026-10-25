package org.xpan.camera;

/** Sensor-space crop, then one affine mapping to the actual displayed image. No Android dependency. */
public final class Geometry {
    public static final int[][] RATIOS = {{65,24},{239,100},{3,1},{16,9},{3,2}};
    public static final String[] LABELS = {"65:24 XPan","2.39:1 Cinema","3:1 Ultra","16:9 Wide","3:2 Full"};
    public static final class Box {
        public final double left, top, right, bottom;
        public Box(double l, double t, double r, double b) { left=l; top=t; right=r; bottom=b; }
        public double width() { return right-left; }
        public double height() { return bottom-top; }
        public String toString() { return left+","+top+","+right+","+bottom; }
    }
    public static Box crop(int w, int h, int n, int d) {
        if (w<=0 || h<=0 || n<=0 || d<=0) throw new IllegalArgumentException("positive dimensions required");
        int cw=w, ch=h;
        if ((long)w*d > (long)h*n) cw=(int)Math.max(1,Math.min(w,((long)h*n+d/2)/d));
        else ch=(int)Math.max(1,Math.min(h,((long)w*d+n/2)/n));
        int x=(w-cw)/2, y=(h-ch)/2;
        return new Box(x,y,x+cw,y+ch);
    }
    public static Box fit(int vw, int vh, int iw, int ih, double pixelAspect) {
        if(vw<=0||vh<=0||iw<=0||ih<=0||pixelAspect<=0||Double.isNaN(pixelAspect)||Double.isInfinite(pixelAspect))
            throw new IllegalArgumentException("invalid fit");
        double logicalAspect=(double)iw/ih/pixelAspect;
        int w=Math.min(vw,(int)Math.round(vh*logicalAspect));
        int h=Math.min(vh,(int)Math.round(w/logicalAspect));
        int x=(vw-w)/2, y=(vh-h)/2;
        return new Box(x,y,x+w,y+h);
    }
    public static Box map(Box sensorCrop, int sw, int sh, Box displayedImage) {
        return new Box(displayedImage.left+sensorCrop.left/sw*displayedImage.width(),
            displayedImage.top+sensorCrop.top/sh*displayedImage.height(),
            displayedImage.left+sensorCrop.right/sw*displayedImage.width(),
            displayedImage.top+sensorCrop.bottom/sh*displayedImage.height());
    }
}

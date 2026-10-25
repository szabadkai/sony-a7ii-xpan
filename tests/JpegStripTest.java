package org.xpan.camera;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;

/** Host codec interoperability tests. Android encoder behavior is a separate device test. */
public final class JpegStripTest {
    private static byte[] jpeg(BufferedImage image)throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        ImageWriter writer=ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageOutputStream stream=ImageIO.createImageOutputStream(bytes);writer.setOutput(stream);
        ImageWriteParam options=writer.getDefaultWriteParam();options.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);options.setCompressionQuality(.95f);
        writer.write(null,new IIOImage(image,null,null),options);stream.close();writer.dispose();return bytes.toByteArray();
    }
    private static BufferedImage band(int width,int top,int rows) {
        BufferedImage image=new BufferedImage(width,rows,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<rows;y++)for(int x=0;x<width;x++) {
            int level=(x/37+(top+y)/29)%2==0?40:210;
            image.setRGB(x,y,(level<<16)|(level<<8)|level);
        }return image;
    }
    public static void main(String[] args)throws Exception {
        int checks=0;
        for(int[] size:new int[][]{{17,1},{31,17},{65,55},{640,237},{6000,2215}}) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();JpegStripWriter join=new JpegStripWriter(out,size[0],size[1]);
            for(int y=0;y<size[1];y+=16){int rows=Math.min(16,size[1]-y);join.add(jpeg(band(size[0],y,rows)),rows);}
            join.finish();BufferedImage result=ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
            if(result==null||result.getWidth()!=size[0]||result.getHeight()!=size[1])throw new AssertionError("dimensions");
            for(int y=0;y<size[1];y++)for(int x=0;x<size[0];x+=13) {
                int expected=(x/37+y/29)%2==0?40:210;
                if(Math.abs((result.getRGB(x,y)&255)-expected)>8)throw new AssertionError("pixels at "+x+","+y);
                checks++;
            }
        }
        byte[] first=jpeg(band(64,0,16));
        try{new JpegStripWriter(new ByteArrayOutputStream(),64,32).finish();throw new AssertionError("partial accepted");}catch(IOException expected){}
        JpegStripWriter join=new JpegStripWriter(new ByteArrayOutputStream(),64,32);join.add(first,16);
        try{join.add(jpeg(band(32,16,16)),16);throw new AssertionError("wrong width accepted");}catch(IOException expected){}
        byte[] changed=first.clone();
        for(int p=2;p<changed.length;p+=2+(((changed[p+2]&255)<<8)|(changed[p+3]&255)))if((changed[p+1]&255)==0xdb){changed[p+5]++;break;}
        try{join.add(changed,16);throw new AssertionError("changed tables accepted");}catch(IOException expected){}
        first[first.length-1]=0;
        try{JpegStripWriter.parse(first);throw new AssertionError("truncated accepted");}catch(IOException expected){}
        if(PanoramaFiles.uniqueNewJpeg(new long[]{2,100},new long[]{100,3,2},new boolean[]{true,true,true})!=3)throw new AssertionError("nonmonotonic IDs");
        try{PanoramaFiles.uniqueNewJpeg(new long[]{2},new long[]{3,4},new boolean[]{true,true});throw new AssertionError("ambiguity");}catch(IOException expected){}
        if(args.length>0) {
            BufferedImage original=ImageIO.read(new File(args[0]));ByteArrayOutputStream out=new ByteArrayOutputStream();
            JpegStripWriter photo=new JpegStripWriter(out,original.getWidth(),original.getHeight());
            for(int y=0;y<original.getHeight();y+=16) {
                int rows=Math.min(16,original.getHeight()-y);
                photo.add(jpeg(original.getSubimage(0,y,original.getWidth(),rows)),rows);
            }
            photo.finish();BufferedImage decoded=ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
            long error=0,count=0;
            for(int y=0;y<original.getHeight();y+=5)for(int x=0;x<original.getWidth();x+=19)for(int shift=0;shift<24;shift+=8) {
                error+=Math.abs(((original.getRGB(x,y)>>shift)&255)-((decoded.getRGB(x,y)>>shift)&255));count++;
            }
            double mae=error/(double)count;if(mae>8)throw new AssertionError("Photograph color error "+mae);
            System.out.println("Real photograph strip encode/decode mean absolute channel error: "+mae+" / 255");
        }
        System.out.println("JPEG strip tests: "+checks+" decoded pixels checked, odd sizes, restart markers, rejection and nonmonotonic image IDs passed");
    }
}

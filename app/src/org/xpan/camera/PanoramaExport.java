package org.xpan.camera;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import java.io.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/** Reads the newly stored JPEG through Sony's provider; writes only new app-owned panorama files. */
final class PanoramaExport {
    private static final AtomicBoolean BUSY=new AtomicBoolean();
    private static final int MAX_RECORDS=100000;
    interface Progress { void update(String message); }
    private final ContentResolver resolver;
    private final Class<?> media;
    private final Uri uri;
    PanoramaExport(ContentResolver resolver)throws Exception {
        this.resolver=resolver;
        media=Class.forName("com.sony.scalar.provider.AvindexStore$Images$Media");
        uri=(Uri)media.getField("EXTERNAL_CONTENT_URI").get(null);
        media.getMethod("getJpegImageInputStream",ContentResolver.class,Uri.class,Long.TYPE);
    }
    static boolean isBusy(){return BUSY.get();}
    static boolean acquire(){return BUSY.compareAndSet(false,true);}
    static void unlock(){BUSY.set(false);}
    private Cursor images()throws IOException {
        Cursor c=resolver.query(uri,new String[]{"_id","exist_jpeg"},null,null,null);
        if(c==null)throw new IOException("Camera image index unavailable");
        if(c.getCount()>MAX_RECORDS){c.close();throw new IOException("Too many images for export index");}
        return c;
    }
    long[] snapshot()throws Exception {
        Cursor c=images();
        try {long[] ids=new long[c.getCount()];int n=0;while(c.moveToNext())ids[n++]=c.getLong(0);Arrays.sort(ids);return ids;}
        finally{c.close();}
    }
    private long newJpeg(long[] before)throws Exception {
        Cursor c=images();
        try {
            long[] ids=new long[c.getCount()];boolean[] jpeg=new boolean[ids.length];int n=0;
            while(c.moveToNext()){ids[n]=c.getLong(0);jpeg[n]=c.getInt(1)!=0;n++;}
            return PanoramaFiles.uniqueNewJpeg(before,ids,jpeg);
        } finally{c.close();}
    }
    private static void cancelled()throws InterruptedIOException {
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Panorama export cancelled");
    }
    private static void checkSize(File file,int w,int h)throws IOException {
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(file.getAbsolutePath(),o);
        if(o.outWidth!=w||o.outHeight!=h)throw new IOException("Unexpected JPEG dimensions: "+o.outWidth+"x"+o.outHeight);
    }
    String export(long[] before,int ratio,Progress progress)throws Exception {
        File source=null,partial=null,reserved=null;BitmapRegionDecoder decoder=null;
        try {
            cancelled();
            if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))throw new IOException("Export storage not mounted");
            File dir=new File(Environment.getExternalStorageDirectory(),"XPAN/PANORAMAS");
            if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create panorama folder");
            source=File.createTempFile(".source-",".jpg",dir);
            long until=SystemClock.elapsedRealtime()+20000,stable=-1,id=-1;boolean ready=false;
            progress.update("Waiting for saved JPEG");
            while(SystemClock.elapsedRealtime()<until) {
                cancelled();id=newJpeg(before);
                if(id>=0&&id==stable) {
                    InputStream in=(InputStream)media.getMethod("getJpegImageInputStream",ContentResolver.class,Uri.class,Long.TYPE).invoke(null,resolver,uri,id);
                    try {PanoramaFiles.copy(in,source);ready=true;break;}
                    catch(InterruptedIOException e){throw e;}
                    catch(IOException e){/* The index can become visible before the JPEG is complete. */}
                }
                stable=id;Thread.sleep(500);
            }
            if(!ready)throw new IOException("Saved JPEG unavailable; originals retained");
            checkSize(source,6000,4000);cancelled();
            ExifInterface originalExif=new ExifInterface(source.getAbsolutePath());
            decoder=BitmapRegionDecoder.newInstance(source.getAbsolutePath(),false);
            if(decoder==null||decoder.getWidth()!=6000||decoder.getHeight()!=4000)throw new IOException("JPEG region decoder unavailable");
            Geometry.Box box=Geometry.crop(6000,4000,Geometry.RATIOS[ratio][0],Geometry.RATIOS[ratio][1]);
            int width=(int)box.width(),height=(int)box.height();
            partial=File.createTempFile("XPAN-"+id+"-",".jpg.part",dir);
            FileOutputStream out=new FileOutputStream(partial);
            try {
                JpegStripWriter writer=new JpegStripWriter(out,width,height);
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;
                options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inDither=false;
                int lastProgress=-1;
                for(int y=0;y<height;y+=16) {
                    cancelled();int rows=Math.min(16,height-y);Bitmap band=null;
                    try {
                        band=decoder.decodeRegion(new Rect((int)box.left,(int)box.top+y,(int)box.right,(int)box.top+y+rows),options);
                        if(band==null||band.getWidth()!=width||band.getHeight()!=rows)throw new IOException("Region decode failed");
                        ByteArrayOutputStream encoded=new ByteArrayOutputStream(65536);
                        if(!band.compress(Bitmap.CompressFormat.JPEG,95,encoded))throw new IOException("JPEG encoding failed");
                        writer.add(encoded.toByteArray(),rows);
                    } finally{if(band!=null)band.recycle();}
                    int percent=(y+rows)*100/height;
                    if(percent/10!=lastProgress){lastProgress=percent/10;progress.update("Saving panorama "+percent+"%");}
                }
                writer.finish();out.getFD().sync();
            } finally{out.close();}
            decoder.recycle();decoder=null;
            checkSize(partial,width,height);cancelled();
            // API-10 ExifInterface only supports a subset; never promise lossless MakerNote/ICC copying.
            ExifInterface dest=new ExifInterface(partial.getAbsolutePath());
            String[] tags={"Make","Model","DateTime","FocalLength","ExposureTime","FNumber","ISOSpeedRatings","WhiteBalance","Flash","GPSLatitude","GPSLatitudeRef","GPSLongitude","GPSLongitudeRef","GPSAltitude","GPSAltitudeRef","GPSDateStamp","GPSTimeStamp"};
            for(String tag:tags){String value=originalExif.getAttribute(tag);if(value!=null)dest.setAttribute(tag,value);}
            int orientation=originalExif.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
            dest.setAttribute(ExifInterface.TAG_ORIENTATION,String.valueOf(orientation>=1&&orientation<=8?orientation:1));dest.saveAttributes();
            checkSize(partial,width,height);cancelled();
            reserved=new File(dir,partial.getName().replace(".jpg.part",".jpg"));
            if(!reserved.createNewFile()){reserved=null;throw new IOException("Panorama filename collision");}
            if(!partial.renameTo(reserved))throw new IOException("Could not finalize panorama");
            String message="Saved "+width+"x"+height+": "+reserved.getAbsolutePath();
            reserved=null;partial=null;return message;
        } finally {
            if(decoder!=null)decoder.recycle();
            if(source!=null)source.delete();
            if(partial!=null)partial.delete();
            if(reserved!=null)reserved.delete();
        }
    }
}

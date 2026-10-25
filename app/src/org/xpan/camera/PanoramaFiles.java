package org.xpan.camera;

import java.io.*;

/** Bounded streaming and unique output ownership, independent of Sony/Android. */
final class PanoramaFiles {
    static final long MAX_JPEG=64L*1024*1024;
    static void copy(InputStream in,File out) throws IOException {
        if(in==null)throw new IOException("JPEG not available yet");
        FileOutputStream stream=null;
        try {
            stream=new FileOutputStream(out);byte[] buffer=new byte[32768];long total=0;int count;
            while((count=in.read(buffer))!=-1) {
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Export cancelled");
                total+=count;if(total>MAX_JPEG)throw new IOException("JPEG exceeds export limit");
                stream.write(buffer,0,count);
            }
            stream.getFD().sync();
            if(total<4)throw new IOException("Empty JPEG");
        } finally {try{if(stream!=null)stream.close();}finally{in.close();}}
        RandomAccessFile check=new RandomAccessFile(out,"r");
        try {
            if(check.readUnsignedShort()!=0xffd8)throw new IOException("Not a JPEG");
            check.seek(check.length()-2);
            if(check.readUnsignedShort()!=0xffd9)throw new IOException("JPEG is incomplete");
        } finally {check.close();}
    }
    static long uniqueNewJpeg(long[] baseline,long[] ids,boolean[] jpeg) throws IOException {
        long found=-1;
        for(int i=0;i<ids.length;i++)if(jpeg[i]&&java.util.Arrays.binarySearch(baseline,ids[i])<0) {
            if(found!=-1&&found!=ids[i])throw new IOException("More than one new JPEG; export skipped");
            found=ids[i];
        }
        return found;
    }
}

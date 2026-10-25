package org.xpan.camera;

import java.io.*;
import java.util.Arrays;

/** Joins independently encoded baseline JPEG strips using restart markers at strip boundaries.
 * Only matching sequential Huffman tables/quantization/component layouts are accepted.
 * Pixel decoding/encoding is provided by Android; this class does not implement a JPEG codec.
 */
final class JpegStripWriter {
    private final OutputStream out;
    private final int width,height;
    private byte[] tables,layout,scan;
    private int written,interval,restart;
    private boolean started,finished;
    JpegStripWriter(OutputStream out,int width,int height) throws IOException {
        if(width<1||width>65535||height<1||height>65535)throw new IOException("Invalid panorama dimensions");
        this.out=out;this.width=width;this.height=height;
    }
    private static int u16(byte[] b,int p){return ((b[p]&255)<<8)|(b[p+1]&255);}
    private static void put16(byte[] b,int p,int value){b[p]=(byte)(value>>8);b[p+1]=(byte)value;}
    static final class Strip {
        byte[] header,tables,layout,scan;
        int width,height,mcuWidth,mcuHeight,entropyStart,entropyEnd;
    }
    static Strip parse(byte[] data)throws IOException {
        if(data.length<4||u16(data,0)!=0xffd8)throw new IOException("Encoder did not return JPEG");
        Strip s=new Strip();ByteArrayOutputStream table=new ByteArrayOutputStream();
        int p=2,sof=-1,sos=-1;boolean dqt=false,dht=false;
        while(p<data.length) {
            if(p+4>data.length||data[p]!=(byte)255)throw new IOException("Invalid JPEG segment");
            int marker=data[p+1]&255,length=u16(data,p+2),end=p+2+length;
            if(length<2||end>data.length)throw new IOException("Truncated JPEG segment");
            if(marker==0xc0) {
                if(sof!=-1||length<11||data[p+4]!=8)throw new IOException("Unsupported JPEG frame");
                sof=p;s.height=u16(data,p+5);s.width=u16(data,p+7);
                int components=data[p+9]&255;
                if(components!=3||length!=8+3*components)throw new IOException("Expected baseline color JPEG");
                int h=0,v=0;
                for(int c=0;c<components;c++) {
                    int hv=data[p+11+c*3]&255;h=Math.max(h,hv>>4);v=Math.max(v,hv&15);
                    if((hv>>4)<1||(hv>>4)>4||(hv&15)<1||(hv&15)>4)throw new IOException("Invalid JPEG sampling");
                }
                s.mcuWidth=h*8;s.mcuHeight=v*8;
                s.layout=Arrays.copyOfRange(data,p+9,end);
            } else if(marker==0xdb||marker==0xc4) {
                table.write(data,p,end-p);if(marker==0xdb)dqt=true;else dht=true;
            } else if(marker==0xda) {
                if(sof<0||!dqt||!dht||length!=12||data[p+4]!=3||data[end-3]!=0||(data[end-2]&255)!=63||data[end-1]!=0)
                    throw new IOException("Unsupported JPEG scan");
                sos=p;s.scan=Arrays.copyOfRange(data,p,end);s.entropyStart=end;break;
            } else if(!(marker>=0xe0&&marker<=0xef)&&marker!=0xfe) {
                // Progressive, arithmetic, restart intervals, extra frames etc. must not be spliced.
                throw new IOException("Unsupported JPEG marker "+marker);
            }
            p=end;
        }
        if(sos<0)throw new IOException("Missing JPEG scan");
        s.tables=table.toByteArray();s.entropyEnd=-1;
        for(p=s.entropyStart;p<data.length-1;p++)if(data[p]==(byte)255) {
            int next=data[++p]&255;
            if(next==0)continue;
            if(next==0xd9&&p==data.length-1){s.entropyEnd=p-1;break;}
            throw new IOException("Unexpected marker inside JPEG strip");
        }
        if(s.entropyEnd<=s.entropyStart)throw new IOException("Incomplete JPEG entropy data");
        s.header=Arrays.copyOfRange(data,0,sos);
        // Header has identical offsets to input up to SOS.
        put16(s.header,sof+5,0); // full panorama height is filled by add()
        return s;
    }
    void add(byte[] jpeg,int rows)throws IOException {
        if(finished)throw new IOException("JPEG already finalized");
        Strip s=parse(jpeg);
        if(s.width!=width||s.height!=rows||rows<1||written+rows>height)throw new IOException("JPEG strip size mismatch");
        boolean last=written+rows==height;
        if(!last&&rows%s.mcuHeight!=0)throw new IOException("Strip is not aligned to MCU rows");
        int mcus=((width+s.mcuWidth-1)/s.mcuWidth)*((rows+s.mcuHeight-1)/s.mcuHeight);
        if(!started) {
            interval=mcus;
            if(interval>65535)throw new IOException("Restart interval too large");
            tables=s.tables;layout=s.layout;scan=s.scan;
            // Locate the single SOF0 in the already validated header.
            for(int p=2;p<s.header.length;p+=2+u16(s.header,p+2))if((s.header[p+1]&255)==0xc0){put16(s.header,p+5,height);break;}
            out.write(s.header);
            out.write(new byte[]{(byte)255,(byte)0xdd,0,4,(byte)(interval>>8),(byte)interval});
            out.write(scan);started=true;
        } else {
            if(!Arrays.equals(tables,s.tables)||!Arrays.equals(layout,s.layout)||!Arrays.equals(scan,s.scan))
                throw new IOException("JPEG encoder changed tables between strips");
            if((!last&&mcus!=interval)||(last&&mcus>interval))throw new IOException("Inconsistent JPEG restart interval");
            out.write(255);out.write(0xd0+(restart++&7));
        }
        out.write(jpeg,s.entropyStart,s.entropyEnd-s.entropyStart);written+=rows;
    }
    void finish()throws IOException {
        if(finished||!started||written!=height)throw new IOException("Incomplete panorama");
        out.write(255);out.write(0xd9);out.flush();finished=true;
    }
}

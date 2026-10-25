package org.xpan.camera;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.hardware.Camera;
import android.os.*;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.widget.FrameLayout;
import java.io.*;
import java.lang.reflect.*;
import java.util.Locale;

/** Camera lifecycle and shutter sequence adapted from MIT PMCADemo, copyright 2015 ma1co. */
public final class MainActivity extends Activity implements SurfaceHolder.Callback {
    private final Handler handler=new Handler();
    private final CaptureGate gate=new CaptureGate();
    private final StringBuilder log=new StringBuilder();
    private SharedPreferences prefs;
    private Sony sony;
    private Object displayManager, displayListener, layoutListener;
    private SurfaceView surface;
    private Overlay overlay;
    private FrameLayout root;
    private boolean resumed, surfaceReady, preview, configured, menu, probe, focusHeld, fullHeld;
    private boolean completionListener, captureStatusListener, exposureRefreshPending;
    private int ratio, menuRow, imageW=6000, imageH=4000;
    private String format="rawjpeg", status="Opening camera", exposure="Exposure pending", focus="";
    private String displayName="Unknown display", originalFormat, originalAspect, originalSize, originalDrive, originalApsc;
    private String shootingRatio;
    private boolean useSonyRect, cleanView, statusAlert, panoramaJpeg;
    private PanoramaExport pendingExporter;
    private long[] pendingImages;
    private int pendingRatio;
    private String panoramaPreparationError;
    private Thread exportThread;
    private final Runnable reopenAfterExport=new Runnable(){public void run(){
        if(!resumed)return;
        if(PanoramaExport.isBusy()){handler.postDelayed(this,200);return;}
        exportThread=null;openCamera();
    }};
    private double pixelAspect=1.0;
    private Geometry.Box imageBox=new Geometry.Box(0,0,0,0), sonyBox;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs=getSharedPreferences("xpan",MODE_PRIVATE);
        ratio=Math.max(0,Math.min(4,prefs.getInt("ratio",0)));
        format=prefs.getBoolean("jpeg",false)?"jpeg":"rawjpeg";
        useSonyRect=prefs.getBoolean("sonyRect",false);
        cleanView=prefs.getBoolean("cleanView",false);
        panoramaJpeg=prefs.getBoolean("panoramaJpeg",true);
        root=new FrameLayout(this) {
            @Override protected void onSizeChanged(int w,int h,int ow,int oh) {
                super.onSizeChanged(w,h,ow,oh);
                handler.post(new Runnable(){ public void run(){ updateDisplay(); }});
            }
        };
        root.setBackgroundColor(Color.BLACK);
        surface=new SurfaceView(this);
        surface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surface.getHolder().addCallback(this);
        root.addView(surface,new FrameLayout.LayoutParams(-1,-1));
        overlay=new Overlay();
        root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        note("Build 0.3.0; model="+Build.MODEL+" device="+Build.DEVICE+" SDK="+Build.VERSION.SDK_INT+" Android="+Build.VERSION.RELEASE);
    }
    @Override protected void onResume() {
        super.onResume(); resumed=true;if(!PanoramaExport.isBusy())exportThread=null; gate.open();shootingRatio=null;exposureRefreshPending=false;
        note("resume session="+gate.token());
        Intent info=new Intent("com.android.server.DAConnectionManagerService.AppInfoReceive");
        info.putExtra("package_name",getPackageName());info.putExtra("class_name",getComponentName().getClassName());sendBroadcast(info);
        openDisplay();
        openCamera();
    }
    private void openCamera() {
        if(PanoramaExport.isBusy()) {
            status="Finishing previous panorama export";overlay.invalidate();
            handler.removeCallbacks(reopenAfterExport);handler.postDelayed(reopenAfterExport,200);return;
        }
        originalFormat=null;originalAspect=null;originalSize=null;originalDrive=null;originalApsc=null;
        try {
            sony=new Sony();
            Object m=sony.modifier(sony.camera.getParameters());
            originalFormat=String.valueOf(Sony.call(m,"getPictureStorageFormat"));
            originalAspect=String.valueOf(Sony.call(m,"getImageAspectRatio"));
            originalSize=String.valueOf(Sony.call(m,"getImageSize"));
            originalDrive=String.valueOf(Sony.call(m,"getDriveMode"));
            if(Boolean.TRUE.equals(Sony.call(m,"isApscModeSupported")))originalApsc=String.valueOf(Sony.call(m,"getApscMode"));
            note("initial parameters="+sony.camera.getParameters().flatten());
            configure();
            registerCallbacks();
            final int token=gate.token();
            sony.camera.setErrorCallback(new Camera.ErrorCallback(){public void onError(final int error,Camera camera) {
                handler.post(new Runnable(){public void run(){if(gate.current(token)) {preview=false;configured=false;fail("Camera error "+error,null);}}});
            }});
            startPreview();
        } catch(Exception e) { fail("Camera unavailable: MENU exit / centre retry",e); closeCamera(); }
        catch(LinkageError e) { fail("Sony runtime unavailable",e); closeCamera(); }
    }
    private void configure() {
        configured=false;
        try {
            sony.configure(format);
            Camera.Size size=sony.imageSize();
            if(size==null||size.width!=6000||size.height!=4000)
                throw new IllegalStateException("Expected ILCE-7M2 full frame 6000x4000; got "+(size==null?"null":size.width+"x"+size.height));
            imageW=size.width; imageH=size.height;
            configured=true;statusAlert=false;status="Ready · "+format.toUpperCase(Locale.US);
            note("configured "+format+" "+imageW+"x"+imageH+" preview="+sony.camera.getParameters().get("preview-size"));
            updateExposure();updateDisplay();
        } catch(Exception e) { fail("Capture disabled: "+e.getMessage(),e); }
    }
    private void registerCallbacks() {
        final int token=gate.token();
        completionListener=false;captureStatusListener=false;
        try {
            sony.listen("setCaptureStatusListener","OnCaptureStatusListener",new Sony.Event(){public void receive(final String method,final Object[] args) {
                handler.post(new Runnable(){public void run(){
                    if(!gate.current(token))return;
                    note("capture "+method+" codes="+codes(args));
                    if("onEnd".equals(method)&&gate.busy()) {
                        captureEnded(token,"Capture ended (check card)");
                    }
                }});
            }});
            completionListener=true;captureStatusListener=true;
        } catch(Exception e) { note("capture status unavailable: "+e); }
        try {
            sony.listen("setShutterListener","ShutterListener",new Sony.Event(){public void receive(String method,final Object[] args) {
                handler.post(new Runnable(){public void run(){
                    if(!gate.current(token))return;
                    note("Sony shutter status="+codes(args));
                    // Legacy single-shot fallback follows the Sony ShutterListener use in TimeLapse.
                    // It is not a storage-complete notification.
                    if(!captureStatusListener&&gate.busy())captureEnded(token,"Shutter status "+codes(args)+"(check card)");
                }});
            }});
            completionListener=true;
        } catch(Exception e) { note("shutter callback unavailable: "+e); }
        try {
            sony.listen("setPreviewStartListener","PreviewStartListener",new Sony.Event(){public void receive(String m,Object[] args) {
                handler.post(new Runnable(){public void run(){
                    if(!gate.current(token))return;
                    if(isExporting()) {
                        preview=false;
                        try{if(sony!=null)sony.camera.stopPreview();}catch(RuntimeException e){note("pause auto-preview: "+e);}
                        return;
                    }
                    note("native preview start");preview=true;updateDisplay();overlay.invalidate();
                }});
            }});
        } catch(Exception e) { note("preview callback unavailable: "+e); }
        try {
            sony.listen("setStoreImageCompleteListener","StoreImageCompleteListener",new Sony.Event(){public void receive(String m,final Object[] args) {
                handler.post(new Runnable(){public void run(){
                    if(!gate.current(token))return;
                    String detail=codes(args);
                    try { Object i=args[1];detail+=" "+i.getClass().getField("DirectoryName").get(i)+"/"+i.getClass().getField("FileName").get(i); } catch(Exception ignored) {}
                    note("store callback "+detail+"; ratio/file association unverified");
                }});
            }});
        } catch(Exception e) { note("store callback unavailable (optional): "+e); }
        if(!completionListener) { configured=false;status="Capture disabled: completion API unavailable"; }
        String[][] exposureCallbacks={{"setApertureChangeListener","ApertureChangeListener"},
            {"setShutterSpeedChangeListener","ShutterSpeedChangeListener"},{"setAutoISOSensitivityListener","AutoISOSensitivityListener"}};
        for(String[] callback:exposureCallbacks)try {
            sony.listen(callback[0],callback[1],new Sony.Event(){public void receive(String method,Object[] args) {
                handler.post(new Runnable(){public void run(){if(gate.current(token)) {
                    if(!exposureRefreshPending){exposureRefreshPending=true;handler.postDelayed(exposureRefresh,100);}
                }}});
            }});
        }catch(Exception e){note("optional exposure callback unavailable: "+e);}
    }
    private final Runnable exposureRefresh=new Runnable(){public void run(){exposureRefreshPending=false;updateExposure();}};
    private void captureEnded(int token,String message) {
        if(!gate.end(token))return;
        handler.removeCallbacks(captureWatchdog);
        if(!focusHeld && sony!=null)try{sony.camera.cancelAutoFocus();}catch(RuntimeException e){note("AF release: "+e);}
        status=message;statusAlert=false;focus="";preview=false;
        if(pendingExporter!=null&&pendingImages!=null)beginPanoramaExport();
        else {startPreview();if(panoramaPreparationError!=null)fail(panoramaPreparationError,null);}
        updateExposure();overlay.invalidate();
    }
    private boolean isExporting(){return exportThread!=null||PanoramaExport.isBusy();}
    private static String exportError(Throwable error) {
        for(int i=0;i<8&&error.getCause()!=null;i++)error=error.getCause();
        return error.getClass().getSimpleName()+": "+error.getMessage();
    }
    private void beginPanoramaExport() {
        final PanoramaExport exporter=pendingExporter;final long[] before=pendingImages;final int selectedRatio=pendingRatio;
        pendingExporter=null;pendingImages=null;
        if(!PanoramaExport.acquire()){fail("Panorama export already running",null);startPreview();return;}
        try{if(sony!=null)sony.camera.stopPreview();}catch(RuntimeException e){note("pause preview: "+e);}
        preview=false;status="Waiting for saved JPEG";
        final int token=gate.token();
        try {exportThread=new Thread(new Runnable(){public void run(){
            String result;boolean failed=false;
            try {
                result=exporter.export(before,selectedRatio,new PanoramaExport.Progress(){public void update(final String message){
                    handler.post(new Runnable(){public void run(){if(resumed&&gate.current(token)){status=message;overlay.invalidate();}}});
                }});
            }catch(Exception e){result="Panorama failed; originals retained: "+exportError(e);failed=true;}
            catch(OutOfMemoryError e){result="Panorama needs more memory; originals retained";failed=true;}
            catch(LinkageError e){result="Panorama API unavailable; originals retained";failed=true;}
            finally{PanoramaExport.unlock();}
            final String finalResult=result;final boolean finalFailed=failed;
            handler.post(new Runnable(){public void run(){
                exportThread=null;note(finalResult);savePrivateLog();
                if(!resumed||!gate.current(token))return;
                status=finalResult;statusAlert=true;
                if(finalFailed)note("Disable Panorama JPEG in settings to shoot originals only");
                startPreview();overlay.invalidate();
            }});
        }},"XPan JPEG export");
        exportThread.start();
        }catch(RuntimeException e){exportThread=null;PanoramaExport.unlock();fail("Cannot start panorama export",e);startPreview();}
        catch(OutOfMemoryError e){exportThread=null;PanoramaExport.unlock();fail("Not enough memory for panorama export",e);startPreview();}
    }
    private static String codes(Object[] args) {
        StringBuilder b=new StringBuilder();for(Object a:args)if(a instanceof Number)b.append(a).append(' ');return b.toString();
    }
    private void startPreview() {
        if(!resumed||sony==null||!surfaceReady||preview||gate.busy()||isExporting())return;
        try { sony.camera.setPreviewDisplay(surface.getHolder());sony.camera.startPreview();preview=true;note("preview requested");updateDisplay(); }
        catch(Exception e) { fail("Preview failed: centre retry",e); }
    }
    public void surfaceCreated(SurfaceHolder h) { surfaceReady=true;startPreview(); }
    public void surfaceChanged(SurfaceHolder h,int f,int w,int height) { note("surface "+w+"x"+height);startPreview(); }
    public void surfaceDestroyed(SurfaceHolder h) {
        surfaceReady=false;preview=false;
        if(sony!=null&&!gate.busy())try{sony.camera.stopPreview();}catch(RuntimeException e){note("stop preview: "+e);}
    }
    @Override protected void onPause() {
        resumed=false;boolean wasBusy=gate.busy()||isExporting();gate.close();handler.removeCallbacksAndMessages(null);
        if(exportThread!=null)exportThread.interrupt();pendingExporter=null;pendingImages=null;
        closeCamera(!wasBusy);closeDisplay();focusHeld=false;fullHeld=false;focus="";
        note("paused: resources released");savePrivateLog();super.onPause();
    }
    @Override protected void onDestroy() { surface.getHolder().removeCallback(this);super.onDestroy(); }
    private void closeCamera() { closeCamera(true); }
    private void closeCamera(boolean restore) {
        Sony old=sony;sony=null;preview=false;configured=false;
        if(old==null)return;
        // Restore only the output settings changed here. Never write backup/protected settings.
        try {
            if(!restore)throw new IllegalStateException("Capture pending: skip output restoration");
            Camera.Parameters p=old.camera.getParameters();Object m=old.modifier(p);
            String[] setters={"setPictureStorageFormat","setImageAspectRatio","setImageSize","setDriveMode","setApscMode"};
            String[] values={originalFormat,originalAspect,originalSize,originalDrive,originalApsc};
            for(int i=0;i<setters.length;i++)if(values[i]!=null)Sony.call(m,setters[i],new Class<?>[]{String.class},values[i]);
            old.camera.setParameters(p);
        } catch(Exception e) { note("output restoration failed: "+e); }
        old.release();
    }
    private void openDisplay() {
        try {
            displayManager=Class.forName(Sony.DM).newInstance();
            Class<?> type=Class.forName(Sony.DM+"$DisplayEventListener");
            final int token=gate.token();
            displayListener=Sony.proxy(type,new Sony.Event(){public void receive(String m,Object[] args) {
                handler.post(new Runnable(){public void run(){if(gate.current(token)){note("display event");updateDisplay();}}});
            }});
            Sony.call(displayManager,"setDisplayStatusListener",new Class<?>[]{type},displayListener);
            try {
                Class<?> layoutType=Class.forName(Sony.DM+"$VideoLayoutStatusListener");
                layoutListener=Sony.proxy(layoutType,new Sony.Event(){public void receive(String m,Object[] args){
                    handler.post(new Runnable(){public void run(){if(gate.current(token))updateDisplay();}});
                }});
                Sony.call(displayManager,"setLayoutStatusListener",new Class<?>[]{layoutType},layoutListener);
            }catch(Exception e){note("video layout listener unavailable: "+e);}
        } catch(Exception e) { note("display manager unavailable: "+e); }
        try {
            Class<?> t=Class.forName("com.sony.scalar.sysutil.didep.Gpelibrary$GS_FRAMEBUFFER_TYPE");
            Class.forName("com.sony.scalar.sysutil.didep.Gpelibrary").getMethod("changeFrameBufferPixel",t).invoke(null,t.getField("RGBA4444").get(null));
        } catch(Exception e) { note("RGBA4444 unavailable: "+e); }
    }
    private void closeDisplay() {
        if(displayManager!=null) {
            try{Sony.call(displayManager,"releaseLayoutStatusListener");}catch(Exception e){note("layout listener release: "+e);}
            try{Sony.call(displayManager,"releaseDisplayStatusListener");}catch(Exception e){note("display listener release: "+e);}
            try{Sony.call(displayManager,"finish");}catch(Exception e){note("display finish: "+e);}
        }
        displayManager=null;displayListener=null;layoutListener=null;
    }
    private void updateDisplay() {
        int w=root.getWidth(),h=root.getHeight();if(w<=0||h<=0)return;
        pixelAspect=1.0;sonyBox=null;
        try {
            if(displayManager!=null) {
                String device=(String)Sony.call(displayManager,"getActiveDevice");
                displayName=device.replace("DEVICE_ID_","");
                Object info=Sony.call(displayManager,"getDeviceInfo",new Class<?>[]{String.class},device);
                int dw=Sony.integer(info,"res_w"),dh=Sony.integer(info,"res_h"),a=Sony.integer(info,"aspect");
                double[] aspects={0,1.5,16.0/9,4.0/3,5.0/3,11.0/9};
                if(a>0&&a<aspects.length)pixelAspect=aspects[a]*h/w;
                Object rect=Sony.call(displayManager,"getDisplayedVideoRect");
                if(dw>0&&dh>0) {
                    double l=Sony.integer(rect,"pxLeft")*(double)w/dw,t=Sony.integer(rect,"pxTop")*(double)h/dh;
                    double r=Sony.integer(rect,"pxRight")*(double)w/dw,b=Sony.integer(rect,"pxBottom")*(double)h/dh;
                    if(l>=0&&t>=0&&r<=w&&b<=h&&r>l&&b>t)sonyBox=new Geometry.Box(l,t,r,b);
                }
                note("display="+displayName+" device="+dw+"x"+dh+" aspect="+a+" view="+w+"x"+h+" videoRect(mapped)="+sonyBox);
            }
        } catch(Exception e) { note("display geometry unavailable: "+e); }
        imageBox=Geometry.fit(w,h,imageW,imageH,pixelAspect);
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)surface.getLayoutParams();
        int nw=(int)imageBox.width(),nh=(int)imageBox.height(),nl=(int)imageBox.left,nt=(int)imageBox.top;
        if(p.width!=nw||p.height!=nh||p.leftMargin!=nl||p.topMargin!=nt) {
            p.width=nw;p.height=nh;p.leftMargin=nl;p.topMargin=nt;surface.setLayoutParams(p);
        }
        overlay.invalidate();
    }
    private void updateExposure() {
        if(sony==null)return;
        try {
            Object m=sony.modifier(sony.camera.getParameters());
            int aperture=((Integer)Sony.call(m,"getAperture")).intValue();
            int iso=((Integer)Sony.call(m,"getISOSensitivity")).intValue();
            Pair<?,?> speed=(Pair<?,?>)Sony.call(m,"getShutterSpeed");
            exposure=String.format(Locale.US,"f/%.1f  %s/%ss  ISO %s",aperture/100.0,speed.first,speed.second,iso<=0?"Auto":String.valueOf(iso));
        } catch(Exception e) { exposure="Exposure: native settings";note("exposure read: "+e); }
        overlay.invalidate();
    }
    private void autofocus(boolean start) {
        if(sony==null||!preview||gate.busy()||isExporting())return;
        try {
            if(start) {
                if("manual".equals(sony.camera.getParameters().getFocusMode())){focus="MF";overlay.invalidate();return;}
                focus="AF..."; final int token=gate.token();
                sony.camera.autoFocus(new Camera.AutoFocusCallback(){public void onAutoFocus(final boolean ok,Camera c) {
                    handler.post(new Runnable(){public void run(){if(gate.current(token)&&focusHeld){focus=ok?"AF OK":"AF ?";updateExposure();overlay.invalidate();}}});
                }});
            } else { sony.camera.cancelAutoFocus();focus=""; }
            overlay.invalidate();
        } catch(RuntimeException e) { fail("Focus request refused",e); }
    }
    private final Runnable captureWatchdog=new Runnable(){public void run(){
        if(gate.busy()) {status="Still waiting: do not remove card; MENU exit";note("capture callback timeout; no automatic restart");overlay.invalidate();}
    }};
    private void shoot() {
        if(!configured||!preview||sony==null||isExporting()||!gate.begin())return;
        pendingExporter=null;pendingImages=null;panoramaPreparationError=null;pendingRatio=ratio;
        if(panoramaJpeg&&ratio!=4)try {
            pendingExporter=new PanoramaExport(getContentResolver());pendingImages=pendingExporter.snapshot();
        }catch(Exception e){pendingExporter=null;pendingImages=null;panoramaPreparationError="Photo captured; panorama unavailable: "+exportError(e);}
        catch(LinkageError e){pendingExporter=null;pendingImages=null;panoramaPreparationError="Photo captured; panorama API unavailable";}
        catch(OutOfMemoryError e){pendingExporter=null;pendingImages=null;panoramaPreparationError="Photo captured; not enough memory for panorama export";}
        menu=false;shootingRatio=Geometry.LABELS[ratio];
        note("shutter composition="+shootingRatio+" crop="+Geometry.crop(imageW,imageH,Geometry.RATIOS[ratio][0],Geometry.RATIOS[ratio][1]));
        savePrivateLog();statusAlert=false;status="Capturing";
        try {sony.camera.takePicture(null,null,null);handler.postDelayed(captureWatchdog,60000);}
        catch(RuntimeException e){pendingExporter=null;pendingImages=null;gate.end(gate.token());fail("Capture refused",e);}
        overlay.invalidate();
    }
    @Override public boolean onKeyDown(int key,KeyEvent event) {
        int scan=event.getScanCode();if(probe)note("key down scan="+scan+" key="+key+" repeat="+event.getRepeatCount());
        if(scan==516||key==KeyEvent.KEYCODE_FOCUS){if(!focusHeld){focusHeld=true;autofocus(true);}return true;}
        if(scan==517)return true;
        if(scan==518||key==KeyEvent.KEYCODE_CAMERA){if(!fullHeld){fullHeld=true;shoot();}return true;}
        if(scan==514||key==KeyEvent.KEYCODE_BACK){if(event.getRepeatCount()==0){if(menu)menu=false;else finish();overlay.invalidate();}return true;}
        if(event.getRepeatCount()>0)return true;
        if(gate.busy()||isExporting())return true;
        if(scan==622){probe=!probe;note("probe="+probe);overlay.invalidate();return true;}
        if(scan==520){ratio=(ratio+1)%5;persist();overlay.invalidate();return true;}
        if(scan==232||key==KeyEvent.KEYCODE_DPAD_CENTER) {
            if(menu&&menuRow==4)exportLog();
            else if(menu&&menuRow==5)retry();
            else menu=!menu;
            overlay.invalidate();return true;
        }
        int step=(scan==106||key==KeyEvent.KEYCODE_DPAD_RIGHT)?1:(scan==105||key==KeyEvent.KEYCODE_DPAD_LEFT)?-1:0;
        if(menu) {
            if(scan==103||key==KeyEvent.KEYCODE_DPAD_UP)menuRow=(menuRow+5)%6;
            else if(scan==108||key==KeyEvent.KEYCODE_DPAD_DOWN)menuRow=(menuRow+1)%6;
            else if(step!=0){
                if(menuRow==0)ratio=(ratio+step+5)%5;
                if(menuRow==1){format="rawjpeg".equals(format)?"jpeg":"rawjpeg";if(sony!=null){configure();if(!completionListener){configured=false;status="Capture disabled: completion API unavailable";}}}
                if(menuRow==2)panoramaJpeg=!panoramaJpeg;
                if(menuRow==3){useSonyRect=!useSonyRect;updateDisplay();}
                persist();
            }
            overlay.invalidate();return true;
        }
        if(scan==103||key==KeyEvent.KEYCODE_DPAD_UP){cleanView=!cleanView;persist();overlay.invalidate();return true;}
        if(step!=0){ratio=(ratio+step+5)%5;persist();overlay.invalidate();return true;}
        if(scan==525||scan==526||scan==528||scan==529) {
            try {if(sony!=null) {Sony.call(sony.ex,(scan==525?"incrementAperture":scan==526?"decrementAperture":scan==528?"incrementShutterSpeed":"decrementShutterSpeed"));updateExposure();}}
            catch(Exception e){fail("Exposure control refused in this mode",e);}return true;
        }
        return super.onKeyDown(key,event);
    }
    @Override public boolean onKeyUp(int key,KeyEvent event) {
        int scan=event.getScanCode();
        if(scan==516||key==KeyEvent.KEYCODE_FOCUS){focusHeld=false;autofocus(false);return true;}
        if(scan==517)return true;
        if(scan==518||key==KeyEvent.KEYCODE_CAMERA){fullHeld=false;try{if(sony!=null)Sony.call(sony.ex,"cancelTakePicture");}catch(Exception e){note("shutter release: "+e);}return true;}
        if(scan==514||scan==520||scan==622||scan==232||scan==105||scan==106||scan==103||scan==108||key==KeyEvent.KEYCODE_BACK)return true;
        return super.onKeyUp(key,event);
    }
    private void retry() {
        if(gate.busy()||isExporting())return;
        closeCamera();gate.close();gate.open();closeDisplay();openDisplay();openCamera();menu=false;
    }
    private void persist(){prefs.edit().putInt("ratio",ratio).putBoolean("jpeg","jpeg".equals(format)).putBoolean("sonyRect",useSonyRect).putBoolean("cleanView",cleanView).putBoolean("panoramaJpeg",panoramaJpeg).commit();}
    private void fail(String message,Throwable e){statusAlert=true;status=message;note(message+(e==null?"":": "+e));if(overlay!=null)overlay.invalidate();}
    private void note(String message){Log.i("XPan",message);log.append(System.currentTimeMillis()).append(' ').append(message).append('\n');if(log.length()>60000)log.delete(0,log.length()-48000);}
    private void savePrivateLog(){try{FileOutputStream out=openFileOutput("diagnostics.txt",MODE_PRIVATE);out.write(log.toString().getBytes("UTF-8"));out.close();}catch(IOException e){Log.e("XPan","save log",e);}}
    private void exportLog(){
        note("export requested");savePrivateLog();
        try {
            if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))throw new IOException("No writable external storage");
            File dir=new File(Environment.getExternalStorageDirectory(),"XPAN");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create XPAN folder");
            File file=File.createTempFile("diagnostics-",".txt",dir);
            FileOutputStream out=new FileOutputStream(file);out.write(log.toString().getBytes("UTF-8"));out.close();statusAlert=false;status="Report: "+file.getAbsolutePath();
        }catch(IOException e){fail("Report kept in private app storage",e);}overlay.invalidate();
    }
    private final class Overlay extends View implements UiRenderer.Drawing {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rounded=new RectF();
        private final UiRenderer renderer=new UiRenderer();
        private final UiRenderer.State state=new UiRenderer.State();
        private Canvas canvas;
        Overlay(){super(MainActivity.this);setFocusable(false);}
        private void style(int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);}
        public void rect(float l,float t,float r,float b,int color){style(color);canvas.drawRect(l,t,r,b,paint);}
        public void round(float l,float t,float r,float b,float radius,int color){
            style(color);rounded.set(l,t,r,b);canvas.drawRoundRect(rounded,radius,radius,paint);
        }
        public float measure(String value,float size,boolean bold){
            paint.setTextSize(size);paint.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);return paint.measureText(value);
        }
        public void text(String value,float x,float baseline,float size,int color,boolean bold){
            measure(value,size,bold);style(color);canvas.drawText(value,x,baseline,paint);
        }
        @Override protected void onDraw(Canvas c) {
            canvas=c;
            state.width=getWidth();state.height=getHeight();state.ratio=ratio;state.menuRow=menuRow;
            state.configured=configured;state.preview=preview;state.busy=gate.busy();state.alert=statusAlert;
            state.exporting=isExporting();state.panorama=panoramaJpeg;
            state.menu=menu;state.probe=probe;state.clean=cleanView;state.experimental=useSonyRect;
            state.sonyRectAvailable=sonyBox!=null;state.format=format;state.exposure=exposure;
            state.status=status;state.focus=focus;state.display=displayName;
            state.source=useSonyRect&&sonyBox!=null?sonyBox:imageBox;
            state.crop=Geometry.map(Geometry.crop(imageW,imageH,Geometry.RATIOS[ratio][0],Geometry.RATIOS[ratio][1]),imageW,imageH,state.source);
            renderer.draw(this,state);
            canvas=null;
        }
    }
}

package org.xpan.camera;

import android.hardware.Camera;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

/** Real firmware APIs only. Reflection permits feature detection; there is no emulated camera fallback.
 * Signatures checked against OpenMemories Jasmin declarations (see research/sources.lock.json).
 */
final class Sony {
    static final String CX="com.sony.scalar.hardware.CameraEx";
    static final String DM="com.sony.scalar.hardware.avio.DisplayManager";
    interface Event { void receive(String method, Object[] args); }
    final Object ex;
    final Camera camera;
    private final List<Object> listeners=new ArrayList<Object>();
    private final List<String> listenerSetters=new ArrayList<String>();
    private final List<Class<?>> listenerTypes=new ArrayList<Class<?>>();
    Sony() throws Exception {
        Class<?> cls=Class.forName(CX);
        ex=cls.getMethod("open",Integer.TYPE,Class.forName(CX+"$OpenOptions")).invoke(null,0,null);
        Camera c;
        try { c=(Camera)call(ex,"getNormalCamera"); }
        catch(Exception e) { call(ex,"release"); throw e; }
        camera=c;
    }
    static Object call(Object obj,String method) throws Exception { return call(obj,method,new Class<?>[0],new Object[0]); }
    static Object call(Object obj,String method,Class<?>[] types,Object... args) throws Exception {
        try { return obj.getClass().getMethod(method,types).invoke(obj,args); }
        catch(InvocationTargetException e) {
            Throwable cause=e.getCause();
            if(cause instanceof Exception) throw (Exception)cause;
            if(cause instanceof Error) throw (Error)cause;
            throw e;
        }
    }
    static int integer(Object obj,String field) throws Exception { return obj.getClass().getField(field).getInt(obj); }
    static Object proxy(final Class<?> iface,final Event event) {
        return Proxy.newProxyInstance(iface.getClassLoader(),new Class<?>[]{iface},new InvocationHandler() {
            public Object invoke(Object p,Method m,Object[] args) {
                if(m.getDeclaringClass()==Object.class) {
                    if("hashCode".equals(m.getName())) return System.identityHashCode(p);
                    if("equals".equals(m.getName())) return p==args[0];
                    return "XPan callback "+iface.getName();
                }
                event.receive(m.getName(),args==null?new Object[0]:args);
                return null;
            }
        });
    }
    void listen(String setter,String iface,Event event) throws Exception {
        Class<?> cls=Class.forName(CX+"$"+iface);
        Object p=proxy(cls,event);
        call(ex,setter,new Class<?>[]{cls},p);
        listeners.add(p);listenerSetters.add(setter);listenerTypes.add(cls);
    }
    Object modifier(Camera.Parameters p) throws Exception {
        return call(ex,"createParametersModifier",new Class<?>[]{Camera.Parameters.class},p);
    }
    private void setSupported(Object supported,Object modifier,String get,String set,String value) throws Exception {
        Object values=call(supported,get);
        if(!(values instanceof List) || !((List<?>)values).contains(value))
            throw new IllegalStateException("Camera does not advertise "+value+" in "+get);
        call(modifier,set,new Class<?>[]{String.class},value);
    }
    void configure(String format) throws Exception {
        Camera.Parameters p=camera.getParameters();
        Object m=modifier(p);
        Object supported;
        try { supported=modifier((Camera.Parameters)call(ex,"getSupportedParameters")); }
        catch(NoSuchMethodException e) { supported=m; }
        setSupported(supported,m,"getSupportedImageAspectRatios","setImageAspectRatio","3:2");
        setSupported(supported,m,"getSupportedImageSizes","setImageSize","large");
        setSupported(supported,m,"getSupportedPictureStorageFormats","setPictureStorageFormat",format);
        setSupported(supported,m,"getSupportedDriveModes","setDriveMode","single");
        // Native image-size/aspect setters, not a guessed 6000x4000 Android picture-size key.
        camera.setParameters(p);
        Object actual=modifier(camera.getParameters());
        if(!format.equals(call(actual,"getPictureStorageFormat")) || !"3:2".equals(call(actual,"getImageAspectRatio"))
           || !"large".equals(call(actual,"getImageSize"))) throw new IllegalStateException("Camera rejected full-frame output settings");
    }
    Camera.Size imageSize() throws Exception {
        Camera.Parameters p=camera.getParameters();
        Camera.Size size=p.getPictureSize();
        if(size!=null && size.width==6000 && size.height==4000) return size;
        Object m=modifier(p);
        // Stub has two String arguments but no parameter names. Read-only probe both orders;
        // accept only the target model's known full-size result, never infer success from a null/default.
        String[][] queries={{"large","3:2"},{"3:2","large"}};
        for(String[] q:queries) {
            try {
                Camera.Size candidate=(Camera.Size)call(m,"getImagePixelSize",new Class<?>[]{String.class,String.class},q[0],q[1]);
                if(candidate!=null && candidate.width==6000 && candidate.height==4000)return candidate;
            } catch(Exception ignored) {}
        }
        return size;
    }
    void release() {
        for(int i=0;i<listenerSetters.size();i++) {
            try { call(ex,listenerSetters.get(i),new Class<?>[]{listenerTypes.get(i)},new Object[]{null}); } catch(Exception ignored) {}
        }
        try { camera.cancelAutoFocus(); } catch(RuntimeException ignored) {}
        try { camera.stopPreview(); } catch(RuntimeException ignored) {}
        try { call(ex,"release"); } catch(Exception ignored) {}
        listeners.clear();
    }
}

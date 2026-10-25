package org.xpan.camera;
/** Serializes single captures and rejects callbacks belonging to a released camera session. */
public final class CaptureGate {
    private int generation;
    private boolean active, busy;
    public int open() { active=true; busy=false; return ++generation; }
    public void close() { active=false; busy=false; ++generation; }
    public int token() { return generation; }
    public boolean current(int token) { return active && token==generation; }
    public boolean begin() { if(!active||busy)return false; busy=true;return true; }
    public boolean end(int token) { if(!current(token))return false;busy=false;return true; }
    public boolean busy() { return busy; }
}

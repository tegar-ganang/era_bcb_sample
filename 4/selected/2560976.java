package midi;

import java.io.Serializable;

public class ModuleInfo implements Serializable {

    protected int id;

    protected boolean bActive;

    protected boolean bLatency;

    protected int time;

    protected double min;

    double max;

    protected boolean bPitch;

    protected boolean bVel;

    protected boolean bMod;

    protected int channel;

    protected int note;

    private boolean invert;

    public ModuleInfo(int id) {
        this.id = id;
        bActive = true;
        bVel = false;
        bMod = true;
        bLatency = false;
        time = 0;
        min = 950;
        max = 1000;
        bPitch = false;
        channel = 0;
        note = 72;
    }

    public boolean isMod() {
        return bActive;
    }

    public void setMod(boolean mod) {
        bMod = mod;
    }

    public boolean isVel() {
        return bVel;
    }

    public void setVel(boolean vel) {
        bVel = vel;
    }

    public boolean isActive() {
        return bActive;
    }

    public void setActive(boolean active) {
        bActive = active;
    }

    public boolean isLatency() {
        return bLatency;
    }

    public void setLatency(boolean latency) {
        bLatency = latency;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double d) {
        this.min = d;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double d) {
        this.max = d;
    }

    public boolean isPitch() {
        return bPitch;
    }

    public void setPitch(boolean pitch) {
        bPitch = pitch;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public boolean isInvert() {
        return invert;
    }
}

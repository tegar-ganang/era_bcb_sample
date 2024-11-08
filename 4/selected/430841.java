package com.frinika.toot.javasoundmultiplexed;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;

class JavaSoundDevice {

    protected byte byteBuffer[];

    protected Mixer mixer;

    protected AudioFormat af;

    protected DataLine.Info info;

    protected DataLine line;

    protected long bufferSizeInFrames;

    int bytesPerFrame;

    public JavaSoundDevice(Mixer mixer, AudioFormat af, DataLine.Info info, int bufferSizeInFrames) {
        this.mixer = mixer;
        this.af = af;
        this.info = info;
        this.line = null;
        bytesPerFrame = 2 * af.getChannels();
        byteBuffer = new byte[bufferSizeInFrames * bytesPerFrame];
    }

    public String toString() {
        if (af.getChannels() == 1) return mixer.getMixerInfo().getName() + " (MONO)"; else if (af.getChannels() == 2) return mixer.getMixerInfo().getName() + " (STEREO)"; else return mixer.getMixerInfo().getName() + "(" + af.getChannels() + ")";
    }

    public int getChannels() {
        return af.getChannels();
    }

    public AudioFormat getFormat() {
        return af;
    }

    public boolean isOpen() {
        if (line == null) return false;
        return line.isOpen();
    }

    public String getName() {
        return toString();
    }

    public boolean isActive() {
        if (line == null) return false;
        return line.isActive();
    }

    public byte[] getBuffer() {
        return byteBuffer;
    }
}

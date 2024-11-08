package gameboy.ui;

import gameboy.core.driver.SoundDriver;

public class Sound implements SoundDriver {

    private int sampleRate;

    private int channels;

    private int bitsPerSample;

    private boolean enabled = false;

    public Sound(int sampleRate, int channels, int bitsPerSample) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public int getChannels() {
        return channels;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void start() {
    }

    public void stop() {
    }

    public void write(byte[] buffer, int length) {
    }
}

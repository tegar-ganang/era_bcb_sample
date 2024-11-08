package com.sts.webmeet.content.common.audio;

public class WHAudioFormat {

    public WHAudioFormat(int iChannelCount, int iSamplesPerSecond, int iBitsPerSample) {
        this.iChannelCount = iChannelCount;
        this.iSamplesPerSecond = iSamplesPerSecond;
        this.iBitsPerSample = iBitsPerSample;
    }

    public static WHAudioFormat getDefaultFormat() {
        return DEFAULT_FORMAT;
    }

    public int getSamplesPerSecond() {
        return iSamplesPerSecond;
    }

    public int getChannelCount() {
        return iChannelCount;
    }

    public int getBitsPerSample() {
        return iBitsPerSample;
    }

    public static final WHAudioFormat MONO_8KHZ_16 = new WHAudioFormat(1, 8000, 16);

    public static final WHAudioFormat DEFAULT_FORMAT = MONO_8KHZ_16;

    public static final WHAudioFormat MONO_16KHZ_16 = new WHAudioFormat(1, 16000, 16);

    public String toString() {
        return super.toString() + " [samplesPerSecond:" + this.iSamplesPerSecond + " channels:" + this.iChannelCount + " bitsPerSample:" + this.iBitsPerSample + "]";
    }

    private int iSamplesPerSecond;

    private int iChannelCount;

    private int iBitsPerSample;
}

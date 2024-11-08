package org.sw.asp;

/**
 *
 * @author Rui Dong
 */
public class JSndInfo {

    private int channels;

    private int samplerate;

    private int format;

    private boolean seekable;

    private int frames;

    private double sections;

    private int bitsPerSamples;

    private int blockalign;

    public JSndInfo() {
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getSamplerate() {
        return samplerate;
    }

    public void setSamplerate(int samplerate) {
        this.samplerate = samplerate;
    }

    public int getFormat() {
        return format;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public boolean isSeekable() {
        return seekable;
    }

    public void setSeekable(boolean seekable) {
        this.seekable = seekable;
    }

    public int getFrames() {
        return frames;
    }

    public void setFrames(int frames) {
        this.frames = frames;
    }

    public double getSections() {
        return sections;
    }

    public void setSections(double sections) {
        this.sections = sections;
    }

    public int getBitsPerSamples() {
        return bitsPerSamples;
    }

    public void setBitsPerSamples(int bps) {
        this.bitsPerSamples = bps;
    }

    public int getBlockalign() {
        return blockalign;
    }

    public void setBlockalign(int blockalign) {
        this.blockalign = blockalign;
    }
}

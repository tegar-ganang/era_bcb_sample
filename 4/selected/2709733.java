package temp;

import java.io.Serializable;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

public class NewFormat implements Serializable {

    private float frameRate;

    private int frameSize;

    private int numChannels;

    private boolean isBigEndian;

    private float sampleRate;

    private int sampleSizeInBits;

    public NewFormat(AudioFormat audioFormat) {
        super();
        frameRate = audioFormat.getFrameRate();
        frameSize = audioFormat.getFrameSize();
        numChannels = audioFormat.getChannels();
        isBigEndian = audioFormat.isBigEndian();
        sampleRate = audioFormat.getSampleRate();
        sampleSizeInBits = audioFormat.getSampleSizeInBits();
    }

    public float getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(float frameRate) {
        this.frameRate = frameRate;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public void setFrameSize(int frameSize) {
        this.frameSize = frameSize;
    }

    public int getNumChannels() {
        return numChannels;
    }

    public void setNumChannels(int numChannels) {
        this.numChannels = numChannels;
    }

    public boolean isBigEndian() {
        return isBigEndian;
    }

    public void setBigEndian(boolean isBigEndian) {
        this.isBigEndian = isBigEndian;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public void setSampleSizeInBits(int sampleSizeInBits) {
        this.sampleSizeInBits = sampleSizeInBits;
    }
}

package org.mobicents.media.server.impl.resource.mediaplayer.mpeg;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * 
 * @author amit bhayani
 * 
 */
public abstract class AudioSampleEntry extends SampleEntry {

    private int channelCount;

    private int sampleSize = 16;

    private double sampleRate;

    public AudioSampleEntry(long size, String type) {
        super(size, type);
    }

    @Override
    protected int load(DataInputStream fin) throws IOException {
        super.load(fin);
        fin.skip(8);
        channelCount = read16(fin);
        sampleSize = read16(fin);
        fin.skip(2);
        fin.skip(2);
        sampleRate = readFixedPoint1616(fin);
        int count = 28 + 8;
        return count;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public double getSampleRate() {
        return sampleRate;
    }
}

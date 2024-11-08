package net.sourceforge.jaad.mp4.boxes.impl.sampleentries;

import net.sourceforge.jaad.mp4.MP4InputStream;
import java.io.IOException;

public class AudioSampleEntry extends SampleEntry {

    private int channelCount, sampleSize, sampleRate;

    public AudioSampleEntry(String name) {
        super(name);
    }

    @Override
    public void decode(MP4InputStream in) throws IOException {
        super.decode(in);
        in.skipBytes(8);
        channelCount = (int) in.readBytes(2);
        sampleSize = (int) in.readBytes(2);
        in.skipBytes(2);
        in.skipBytes(2);
        sampleRate = (int) in.readBytes(2);
        in.skipBytes(2);
        readChildren(in);
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getSampleSize() {
        return sampleSize;
    }
}

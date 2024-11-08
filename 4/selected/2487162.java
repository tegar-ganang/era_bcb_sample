package net.sf.mogbox.pol.ffxi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class PCMDecoder implements Decoder {

    private int channels;

    private int frameSize;

    private int loopOffset;

    public PCMDecoder(FFXISound sound, ReadableByteChannel in) {
        channels = sound.getChannels();
        frameSize = channels * (sound.getBitsPerSample() >> 3);
        loopOffset = sound.getLoopPoint() * frameSize;
    }

    @Override
    public int getFrameSize() {
        return frameSize;
    }

    @Override
    public int getSamplesPerFrame() {
        return 1;
    }

    @Override
    public int getLoopOffset() {
        return loopOffset;
    }

    @Override
    public void reset(boolean loop) {
    }

    @Override
    public void decode(ReadableByteChannel in, ByteBuffer out) throws IOException {
        int read;
        do {
            read = in.read(out);
        } while (read >= 0 && out.hasRemaining());
    }
}

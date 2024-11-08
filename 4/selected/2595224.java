package net.sf.mogbox.pol.ffxi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import net.sf.mogbox.pol.ffxi.loader.UnsupportedFormatException;
import net.sf.mogbox.renderer.engine.Sound;

public class ADPCMDecoder implements Decoder {

    private static final int[] FILTER1 = { 0x000, 0x0F0, 0x1CC, 0x188, 0x1E8 };

    private static final int[] FILTER2 = { 0x000, 0x000, 0x0D0, 0x0DC, 0x0F0 };

    private int channels;

    private int frameSize;

    private int loopOffset;

    private short[][] residual;

    private ByteBuffer buffer;

    public ADPCMDecoder(FFXISound sound, ReadableByteChannel in) {
        channels = sound.getChannels();
        if (sound.getBitsPerSample() != 16) throw new UnsupportedFormatException();
        if (sound.getLength() % 16 != 0) throw new UnsupportedFormatException();
        frameSize = channels * 9;
        loopOffset = sound.getLoopPoint() * frameSize;
        residual = new short[channels][2];
        buffer = ByteBuffer.allocateDirect(Sound.BUFFER_SIZE * channels / 16 * 9);
    }

    @Override
    public int getFrameSize() {
        return frameSize;
    }

    @Override
    public int getSamplesPerFrame() {
        return 16;
    }

    @Override
    public int getLoopOffset() {
        return loopOffset;
    }

    @Override
    public void reset(boolean loop) {
        if (!loop) {
            for (int i = 0; i < channels; i++) {
                residual[i][0] = residual[i][1] = 0;
            }
        }
    }

    @Override
    public void decode(ReadableByteChannel in, ByteBuffer out) throws IOException {
        buffer.clear().limit(out.remaining() / 2 / 16 * 9);
        int read;
        do {
            read = in.read(buffer);
        } while (read >= 0 && buffer.hasRemaining());
        buffer.flip();
        while (buffer.hasRemaining()) {
            int offset = out.position();
            int scale, index, temp;
            for (int c = 0; c < channels; c++) {
                short[] res = residual[c];
                byte b = buffer.get();
                scale = 0xC - (b & 0xF);
                index = b >>> 4;
                for (int i = 0; i < 8; i++) {
                    b = buffer.get();
                    temp = b << 28 >> 28 << scale;
                    temp += (res[0] * FILTER1[index] - res[1] * FILTER2[index]) >> 8;
                    res[1] = res[0];
                    res[0] = clamp(temp);
                    out.putShort(offset + (c + channels * (i * 2)) * 2, res[0]);
                    temp = b >> 4 << scale;
                    temp += (res[0] * FILTER1[index] - res[1] * FILTER2[index]) >> 8;
                    res[1] = res[0];
                    res[0] = clamp(temp);
                    out.putShort(offset + (c + channels * (i * 2 + 1)) * 2, res[0]);
                }
            }
            out.position(offset + 32 * channels);
        }
    }

    private short clamp(int value) {
        if (value > Short.MAX_VALUE) return Short.MAX_VALUE;
        if (value < Short.MIN_VALUE) return Short.MIN_VALUE;
        return (short) value;
    }
}

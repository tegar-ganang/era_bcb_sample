package de.sciss.eisenkraut.io;

import java.io.IOException;
import de.sciss.io.CacheManager;
import de.sciss.io.InterleavedStreamFile;
import de.sciss.io.Span;
import de.sciss.jcollider.Buffer;
import de.sciss.net.OSCBundle;
import de.sciss.timebased.Stake;

/**
 *	A fake silent audio stake that occupies no disk space.
 *	Thanks to scsynth's /b_fill command, this works harmonically
 *	with the supercollider player.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 07-Dec-07
 */
public class SilentAudioStake extends AudioStake {

    private final int numChannels;

    /**
	 */
    protected SilentAudioStake(Span span, int numChannels) {
        super(span);
        this.numChannels = numChannels;
    }

    public void close() throws IOException {
    }

    public void cleanUp() {
    }

    public Stake duplicate() {
        return new SilentAudioStake(span, numChannels);
    }

    public Stake replaceStart(long newStart) {
        return new SilentAudioStake(span.replaceStart(newStart), numChannels);
    }

    public Stake replaceStop(long newStop) {
        return new SilentAudioStake(span.replaceStop(newStop), numChannels);
    }

    public Stake shiftVirtual(long delta) {
        return new SilentAudioStake(span.shift(delta), numChannels);
    }

    public int readFrames(float[][] data, int dataOffset, Span readSpan) throws IOException {
        final int len = (int) readSpan.getLength();
        final int stop = dataOffset + len;
        float[] temp;
        for (int i = 0; i < numChannels; i++) {
            temp = data[i];
            if (temp == null) continue;
            for (int j = dataOffset; j < stop; j++) {
                temp[j] = 0.0f;
            }
        }
        return len;
    }

    public int writeFrames(float[][] data, int dataOffset, Span writeSpan) throws IOException {
        throw new IOException("Not allowed");
    }

    public long copyFrames(InterleavedStreamFile target, Span readSpan) throws IOException {
        final long len = readSpan.getLength();
        final int bufLen = (int) Math.min(8192, readSpan.getLength());
        final float[] empty = new float[bufLen];
        final float[][] buf = new float[numChannels][];
        int chunkLen;
        for (int i = 0; i < buf.length; i++) {
            buf[i] = empty;
        }
        for (long framesWritten = 0; framesWritten < len; ) {
            chunkLen = (int) Math.min(len - framesWritten, bufLen);
            target.writeFrames(buf, 0, chunkLen);
            framesWritten += chunkLen;
        }
        return len;
    }

    public int getChannelNum() {
        return numChannels;
    }

    public void flush() throws IOException {
    }

    public void addToCache(CacheManager cm) {
    }

    public void addBufferReadMessages(OSCBundle bndl, Span s, Buffer[] bufs, int bufOff) {
        final int len = (int) s.getLength();
        if (len == 0) return;
        for (int i = 0; i < bufs.length; i++) {
            bndl.addPacket(bufs[i].fillMsg(bufOff * bufs[i].getNumChannels(), len * bufs[i].getNumChannels(), 0.0f));
        }
    }

    public void debugDump() {
        super.debugDumpBasics();
        System.err.println("  (silent)");
    }
}

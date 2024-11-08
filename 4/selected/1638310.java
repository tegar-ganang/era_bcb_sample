package silence.format.mp3;

import silence.format.*;
import java.io.*;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;

/**
 * Pretty much cut and paste from Player.java from
 * the JLayer sources.
 *
 * Original author: Mat McGowan
 *
 * @author Fredrik Ehnbom
 */
public class Mp3 extends AudioFormat {

    private Bitstream bitstream;

    private Decoder decoder;

    public void load(BufferedInputStream is) throws IOException {
        bitstream = new Bitstream(is);
        decoder = new Decoder();
    }

    public void close() {
        try {
            bitstream.close();
        } catch (BitstreamException ex) {
        }
    }

    private short[] samples = null;

    private int samplePos = -1;

    private int samplesLeft = 0;

    private int pitch = 0;

    private int channels = 2;

    private static final short clamp(int src) {
        src = src < -32768 ? -32768 : src > 32767 ? 32767 : src;
        return (short) src;
    }

    public int read(int[] buffer, int off, int len) {
        int written = 0;
        try {
            while (len > 0) {
                if (samplesLeft == 0) {
                    Header h = bitstream.readFrame();
                    if (h == null) {
                        if (written == 0) return -1;
                        return written;
                    }
                    SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
                    samples = output.getBuffer();
                    samplePos = 0;
                    channels = output.getChannelCount();
                    pitch = (int) (((float) output.getSampleFrequency() / device.getSampleRate()) * 65536);
                    samplesLeft = ((output.getBufferLength() / channels) << 16) / pitch;
                    bitstream.closeFrame();
                }
                int read = Math.min(samplesLeft, len);
                for (int i = 0; i < read; i++) {
                    int realPos = (samplePos >> 16) * channels;
                    int l = clamp(samples[realPos]) & 0xffff;
                    int r = l;
                    if (channels == 2) r = clamp(samples[realPos + 1]) & 0xffff;
                    samplePos += pitch;
                    buffer[off + i] = l | r << 16;
                }
                off += read;
                len -= read;
                samplesLeft -= read;
                written += read;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return written;
    }
}

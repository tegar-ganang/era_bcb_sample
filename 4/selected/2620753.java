package com.landak.ipod.gain;

import java.io.InputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.Obuffer;
import javazoom.jl.decoder.SampleBuffer;

public class AnalyzeFile {

    private Decoder decoder;

    private Bitstream bitstream;

    private Analyzer analyzer;

    private double left_samples[] = new double[Obuffer.OBUFFERSIZE / 2];

    private double right_samples[] = new double[Obuffer.OBUFFERSIZE / 2];

    public AnalyzeFile(InputStream stream) throws JavaLayerException {
        bitstream = new Bitstream(stream);
        decoder = new Decoder();
    }

    /**
     * Decodes a single frame.
     * 
     * @return true if there are no more frames to decode, false otherwise.
     */
    protected boolean decodeFrame() throws JavaLayerException {
        try {
            Header h = bitstream.readFrame();
            if (h == null) return false;
            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            if (analyzer == null) analyzer = new Analyzer(output.getSampleFrequency());
            short[] ob = output.getBuffer();
            for (int a = 0; a < output.getBufferLength() / 2; a++) {
                left_samples[a] = ob[a * 2];
                right_samples[a] = ob[a * 2 + 1];
            }
            analyzer.AnalyzeSamples(left_samples, right_samples, output.getBufferLength() / 2, output.getChannelCount());
            bitstream.closeFrame();
        } catch (RuntimeException ex) {
            throw new JavaLayerException("Exception decoding audio frame", ex);
        }
        return true;
    }

    public double analyze() {
        boolean ret = true;
        int frames;
        while (ret) {
            frames = 500;
            while (frames-- > 0 && ret) {
                try {
                    ret = decodeFrame();
                } catch (JavaLayerException e) {
                }
            }
            frames = 2000;
            while (frames-- > 0 && ret) {
                try {
                    ret = skipFrame();
                } catch (JavaLayerException e) {
                }
            }
        }
        close();
        if (analyzer == null) return 0;
        return analyzer.GetTitleGain();
    }

    /**
     * skips over a single frame
     * @return false    if there are no more frames to decode, true otherwise.
     */
    protected boolean skipFrame() throws JavaLayerException {
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        return true;
    }

    /**
     * Cloases this player. Any audio currently playing is stopped
     * immediately.
     */
    public void close() {
        try {
            bitstream.close();
        } catch (BitstreamException ex) {
        }
    }
}

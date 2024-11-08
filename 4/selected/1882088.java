package com.frinika.audio.analysis.dft;

import java.awt.Dimension;
import java.io.IOException;
import com.frinika.audio.io.LimitedAudioReader;
import com.frinika.audio.analysis.DataBuilder;
import com.frinika.audio.analysis.SpectrogramDataListener;
import uk.org.toot.audio.core.AudioBuffer;

public class ChunkFeeder extends DataBuilder {

    FFTSpectrumClient client;

    private int chunkStartInSamples;

    int chunksize;

    int fftsize;

    private LimitedAudioReader reader;

    int chunkPtr = 0;

    int nFrame;

    private int sizeInChunks;

    private Dimension size;

    private int nBin;

    ChunkReaderProcess process;

    public void setParameters(int chunkSize, int fftsize, LimitedAudioReader reader, ChunkReaderProcess process, FFTSpectrumClient client) {
        abortConstruction();
        this.reader = reader;
        this.process = process;
        this.client = client;
        if (chunkSize == this.chunksize && fftsize == this.fftsize) return;
        nFrame = (int) reader.getEnvelopedLengthInFrames();
        System.out.println(" NFRAME = " + nFrame);
        if (nFrame == 0) {
            System.out.println(" Seeting nFrame to 1000000 ");
            nFrame = 100000;
        }
        this.chunksize = chunkSize;
        this.fftsize = fftsize;
        sizeInChunks = nFrame / chunksize;
        System.out.println("SIZE IN CHUNKS = " + sizeInChunks);
        double dt = chunkSize / reader.getSampleRate();
        process.setParameters(fftsize, process.getSampleRate());
        nBin = process.getBinCount();
        size = new Dimension(sizeInChunks, nBin);
        client.setSize(sizeInChunks, nBin, process.getFreqArray(), dt);
        startConstruction();
    }

    protected void doWork() {
        double fftOut[] = new double[fftsize * 2];
        double input[] = new double[fftsize];
        try {
            reader.seekEnvelopeStart(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int ch = reader.getChannels();
        int nRead = 0;
        AudioBuffer buffer = new AudioBuffer("TEMP", ch, chunksize, 44100);
        chunkPtr = 0;
        notifySizeObservers();
        chunkStartInSamples = 0;
        double maxV = 0.0;
        do {
            if (Thread.interrupted()) {
                return;
            }
            if (fftsize != chunksize) {
                for (int i = 0; i < fftsize - chunksize; i++) input[i] = input[i + chunksize];
            }
            buffer.makeSilence();
            try {
                reader.processAudio(buffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            nRead += chunksize;
            float left[] = buffer.getChannel(0);
            for (int i = fftsize - chunksize, j = 0; i < fftsize; i++, j++) {
                input[i] = left[j];
            }
            for (int i = 0; i < fftsize; i++) fftOut[i] = input[i];
            double spectrum[] = process.process(fftOut);
            client.process(spectrum, nBin);
            chunkPtr++;
            chunkStartInSamples += chunksize;
        } while (chunkPtr < sizeInChunks);
        System.out.println(" DATA BUILT maqxV " + maxV);
        notifyMoreDataObservers();
    }

    public void addSizeObserver(SpectrogramDataListener o) {
        sizeObservers.add(o);
    }

    void notifySizeObservers() {
        for (SpectrogramDataListener o : sizeObservers) o.notifySizeChange(size);
    }

    void notifyMoreDataObservers() {
        for (SpectrogramDataListener o : sizeObservers) o.notifyMoreDataReady();
    }

    public long chunkStartInSamples(long chunkPtr) {
        return chunkStartInSamples + chunkPtr * chunksize;
    }

    public int getChunkAtFrame(long framePtr) {
        int chunkPtr = (int) ((framePtr - chunkStartInSamples) / chunksize);
        return chunkPtr;
    }

    public boolean validAt(long chunkPtr2) {
        return chunkPtr2 >= 0 && chunkPtr2 < this.chunkPtr;
    }
}

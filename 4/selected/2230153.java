package uk.co.drpj.audio.spectral;

import java.awt.Dimension;
import java.util.Vector;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;

/**
 * Creates a spectrogram from a DoubleDataSource
 * 
 * Observers are notified when data changes (during build)
 * 
 * SizeObserver are notify when the number of frequency bins is changed.
 * 
 * @author pjl
 * 
 */
public class CyclicBufferFFTSpectrogramDataBuilder implements CyclicSpectrumDataBuilder {

    Vector<CyclicSpectrogramDataListener> sizeObservers = new Vector<CyclicSpectrogramDataListener>();

    private AudioProcess reader;

    FFTWorker fftWorker;

    private float[][] data;

    private float[][] magnArray;

    double fftBuffer[][];

    int chunkPtr = 0;

    private int sizeInChunks;

    double Fs;

    int chunksize;

    int fftsize;

    double dt;

    Dimension size;

    private int chunkStartInSamples;

    private int totalFramesRendered;

    private double[] input;

    private AudioBuffer buffer;

    boolean abortFlag = false;

    private boolean running = false;

    private Thread runThread = null;

    private Thread abortWaiter;

    private FFTMagnitude fftMagn;

    /**
	 * 
	 * @param minF
	 * @param nOctave
	 * @param binsPerOctave
	 */
    public CyclicBufferFFTSpectrogramDataBuilder(AudioProcess reader, int bufferSize, double Fs) {
        this.reader = reader;
        this.sizeInChunks = bufferSize;
        fftWorker = new FFTWorker(Fs, true);
        fftMagn = new FFTMagnitude();
        data = new float[1][];
    }

    public synchronized void setParameters(int chunksize, int fftsize, float Fs) {
        System.err.println(" AAAA ");
        this.Fs = Fs;
        if (chunksize == this.chunksize && fftsize == this.fftsize) return;
        System.out.println(" ABORT REQUEST " + System.currentTimeMillis());
        abortFlag = true;
        abortWaiter = Thread.currentThread();
        while (running) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        abortWaiter = null;
        System.out.println(" ABORT DONE " + System.currentTimeMillis());
        buffer = new AudioBuffer("TEMP", 1, chunksize, Fs);
        this.chunksize = chunksize;
        this.fftsize = fftsize;
        dt = chunksize / Fs;
        System.out.println(" RESIZE FFT REQUEST " + System.currentTimeMillis());
        resize();
        System.out.println(" RESIZE FFT DONE " + System.currentTimeMillis());
        Runnable runner = new Runnable() {

            public void run() {
                doWork();
                runThread = null;
            }
        };
        runThread = new Thread(runner);
        runThread.start();
    }

    public void abortConstruction() {
    }

    public void addSizeObserver(CyclicSpectrogramDataListener o) {
        sizeObservers.add(o);
    }

    void notifyMoreDataObservers(float buff[]) {
        for (CyclicSpectrogramDataListener o : sizeObservers) o.notifyMoreDataReady(buff);
    }

    public int getSizeInChunks() {
        return sizeInChunks;
    }

    public int getChunkRenderedCount() {
        return totalFramesRendered;
    }

    synchronized void resize() {
        fftWorker.resize(fftsize);
        int nBin = fftWorker.getSizeInBins();
        for (int i = 0; i < nBin; i++) {
            size = new Dimension(sizeInChunks, nBin);
            magnArray = new float[sizeInChunks][nBin];
            fftBuffer = new double[sizeInChunks][fftsize];
        }
        input = new double[fftsize];
        System.out.println(" Resized " + fftsize);
    }

    protected void doWork() {
        running = true;
        abortFlag = false;
        chunkPtr = 0;
        chunkStartInSamples = 0;
        totalFramesRendered = 0;
        do {
            if (abortFlag) break;
            if (fftsize != chunksize) {
                for (int i = 0; i < fftsize - chunksize; i++) input[i] = input[i + chunksize];
            }
            buffer.makeSilence();
            reader.processAudio(buffer);
            float left[] = buffer.getChannel(0);
            for (int i = fftsize - chunksize, j = 0; i < fftsize; i++, j++) {
                input[i] = left[j];
            }
            if (chunkPtr < 0) {
                chunkPtr++;
                chunkStartInSamples += chunksize;
                continue;
            }
            fftWorker.process(input, fftBuffer[chunkPtr]);
            data[0] = magnArray[chunkPtr];
            fftMagn.getData(data, fftBuffer[chunkPtr]);
            notifyMoreDataObservers(magnArray[chunkPtr]);
            chunkPtr++;
            totalFramesRendered++;
            if (chunkPtr >= sizeInChunks) chunkPtr = 0;
        } while (true);
        running = false;
        abortFlag = false;
        if (abortWaiter != null) abortWaiter.interrupt();
        System.out.println(" ABORTED ");
    }

    public float[] getFreqArray() {
        return fftWorker.getFreqArray();
    }

    public float[] getMagnitudeAt(long chunkPtr) {
        if (magnArray == null) return null;
        if (chunkPtr >= magnArray.length || chunkPtr < 0) return null;
        return magnArray[(int) chunkPtr];
    }

    public long chunkStartInSamples(long chunkPtr) {
        return chunkStartInSamples + chunkPtr * chunksize;
    }

    public int getChunkAtFrame(long framePtr) {
        int chunkPtr1 = (int) ((framePtr - chunkStartInSamples) / chunksize);
        return chunkPtr1;
    }

    public boolean validAt(long chunkPtr2) {
        return chunkPtr2 >= 0 && chunkPtr2 < this.chunkPtr;
    }

    public double getSampleRate() {
        return Fs;
    }

    public int getBinCount() {
        return fftWorker.getSizeInBins();
    }

    public float[][] getMagnitude() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

package uk.co.simphoney.audio.dft;

import java.awt.Dimension;
import java.util.Vector;
import rasmus.interpreter.sampled.util.FFT;
import uk.co.simphoney.audio.gui.CyclicSpectrogramDataListener;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;
import com.frinika.global.FrinikaConfig;

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

    private float[][] magnArray;

    private float[][] smoothMagnArray;

    private float[][] phaseArray;

    private float[][] dPhaseFreqHz;

    float freqArray[];

    double freq[];

    int chunkPtr = 0;

    private int sizeInChunks;

    private int nBin;

    double Fs = FrinikaConfig.sampleRate;

    int chunksize;

    int fftsize;

    double dt;

    Dimension size;

    private int chunkStartInSamples;

    private int totalFramesRendered;

    private FFT fft;

    private double[] hanning;

    private double[] dPhaRef;

    private double[] fftOut;

    private double[] input;

    private double logMagn[];

    private double twoPI;

    private AudioBuffer buffer;

    boolean abortFlag = false;

    private boolean running = false;

    private Thread runThread = null;

    private Thread abortWaiter;

    /**
	 * 
	 * @param minF
	 * @param nOctave
	 * @param binsPerOctave
	 */
    public CyclicBufferFFTSpectrogramDataBuilder(AudioProcess reader, int bufferSize) {
        this.reader = reader;
        this.sizeInChunks = bufferSize;
        twoPI = 2 * Math.PI;
    }

    public synchronized void setParameters(int chunksize, int fftsize) {
        System.err.println(" AAAA ");
        if (chunksize == this.chunksize && fftsize == this.fftsize) return;
        System.out.println(" ABORT REQUEST " + System.currentTimeMillis());
        abortFlag = true;
        abortWaiter = Thread.currentThread();
        while (running) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        abortWaiter = null;
        System.out.println(" ABORT DONE " + System.currentTimeMillis());
        buffer = new AudioBuffer("TEMP", 1, chunksize, 44100);
        Fs = FrinikaConfig.sampleRate;
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

    public int getBinCount() {
        return nBin;
    }

    public float[][] getMagnitude() {
        return magnArray;
    }

    public float[][] getSmoothMagnitude() {
        return smoothMagnArray;
    }

    synchronized void resize() {
        fft = new FFT(fftsize);
        nBin = fftsize / 2;
        hanning = fft.wHanning();
        freqArray = new float[nBin];
        freq = new double[nBin];
        for (int i = 0; i < nBin; i++) {
            freq[i] = (i * Fs / nBin);
            freqArray[i] = (float) freq[i];
            size = new Dimension(sizeInChunks, nBin);
            dPhaseFreqHz = new float[sizeInChunks][nBin];
            magnArray = new float[sizeInChunks][nBin];
            smoothMagnArray = new float[sizeInChunks][nBin];
            logMagn = new double[nBin * 2];
            phaseArray = new float[sizeInChunks][nBin];
        }
        dPhaRef = new double[nBin];
        for (int i = 0; i < nBin; i++) {
            dPhaRef[i] = (twoPI * freq[i] * dt);
        }
        fftOut = new double[fftsize * 2];
        input = new double[fftsize];
        System.out.println(" Resized " + fftsize);
    }

    protected void doWork() {
        running = true;
        abortFlag = false;
        chunkPtr = 0;
        int nRead = 0;
        chunkStartInSamples = 0;
        float phaLast[] = phaseArray[0];
        double maxV = 0.0;
        totalFramesRendered = 0;
        do {
            if (abortFlag) break;
            if (fftsize != chunksize) {
                for (int i = 0; i < fftsize - chunksize; i++) input[i] = input[i + chunksize];
            }
            buffer.makeSilence();
            int stat = reader.processAudio(buffer);
            nRead += chunksize;
            float left[] = buffer.getChannel(0);
            for (int i = fftsize - chunksize, j = 0; i < fftsize; i++, j++) {
                input[i] = left[j];
            }
            if (chunkPtr < 0) {
                chunkPtr++;
                chunkStartInSamples += chunksize;
                continue;
            }
            for (int i = 0; i < fftsize; i++) {
                fftOut[i] = input[i] * hanning[i];
            }
            fft.calcReal(fftOut, -1);
            for (int i = 0; i < nBin; i++) {
                double real = fftOut[2 * i];
                double imag = fftOut[2 * i + 1];
                magnArray[chunkPtr][i] = (float) Math.sqrt(real * real + imag * imag);
                maxV = Math.max(maxV, magnArray[chunkPtr][i]);
                phaseArray[chunkPtr][i] = (float) Math.atan2(imag, real);
                double dpha = phaseArray[chunkPtr][i] - phaLast[i];
                dpha = -((dPhaRef[i] - dpha + Math.PI + twoPI) % twoPI - Math.PI);
                dPhaseFreqHz[chunkPtr][i] = (float) (freq[i] + dpha / twoPI / dt);
            }
            phaLast = phaseArray[chunkPtr];
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
        return freqArray;
    }

    public float[] getMagnitudeAt(long chunkPtr) {
        if (magnArray == null) return null;
        if (chunkPtr >= magnArray.length || chunkPtr < 0) return null;
        return magnArray[(int) chunkPtr];
    }

    public float[] getPhaseAt(long chunkPtr) {
        if (phaseArray == null) return null;
        if (chunkPtr >= phaseArray.length || chunkPtr < 0) return null;
        return phaseArray[(int) chunkPtr];
    }

    public float[] getPhaseFreqAt(long chunkPtr) {
        if (dPhaseFreqHz == null) return null;
        if (chunkPtr >= dPhaseFreqHz.length) return null;
        return dPhaseFreqHz[(int) chunkPtr];
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

    public double getSampleRate() {
        return Fs;
    }
}

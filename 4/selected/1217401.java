package uk.co.drpj.audio.constantq;

import java.awt.Dimension;
import java.util.Vector;
import com.frinika.audio.analysis.DataBuilder;
import rasmus.interpreter.sampled.util.FFT;
import uk.co.drpj.audio.spectral.CyclicSpectrogramDataListener;
import uk.co.drpj.audio.spectral.CyclicSpectrumDataBuilder;
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
public class ConstantQSpectrogramDataBuilder extends DataBuilder implements CyclicSpectrumDataBuilder {

    private AudioProcess reader;

    private float[][] magnArray;

    private float[][] phaseArray;

    private float[][] dPhaseFreqHz;

    Vector<CyclicSpectrogramDataListener> sizeObservers = new Vector<CyclicSpectrogramDataListener>();

    float freqArray[];

    int chunkPtr = 0;

    private int sizeInChunks;

    private int nBin;

    double dt = .01;

    double Fs = 44100.0f;

    double minF;

    double maxF;

    int binsPerOctave = 36;

    double thresh = 0.02;

    int chunksize;

    Dimension size;

    double spread;

    FFTConstantQ fftCQ;

    private int chunkStartInSamples;

    public ConstantQSpectrogramDataBuilder(int width, AudioProcess reader) {
        this.sizeInChunks = width;
        this.reader = reader;
    }

    public void setParameters(double minF, double maxF, int binsPerOctave, double thresh, double spread, double dt) {
        abortConstruction();
        if (minF == this.minF && maxF == this.maxF && binsPerOctave == this.binsPerOctave && thresh == this.thresh && this.spread == spread && this.dt == dt) return;
        this.dt = dt;
        Fs = 44100.0f;
        this.minF = minF;
        this.maxF = maxF;
        this.binsPerOctave = binsPerOctave;
        this.thresh = thresh;
        this.spread = spread;
        chunksize = (int) (Fs * dt);
        startConstruction();
    }

    public int getBinCount() {
        return nBin;
    }

    public float[][] getMagnitude() {
        return magnArray;
    }

    protected void doWork() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            System.out.println(" Interrupted before I even started !! ");
            e.printStackTrace();
            return;
        }
        dt = chunksize / Fs;
        fftCQ = new FFTConstantQ(Fs, minF, maxF, binsPerOctave, thresh, spread);
        int fftsize = fftCQ.getFFTSize();
        double freq[] = fftCQ.freqs;
        synchronized (this) {
            freqArray = new float[freq.length];
            for (int i = 0; i < freq.length; i++) {
                freqArray[i] = (float) freq[i];
            }
            System.out.println(" fftsize/chunkSIze = " + fftsize + "/" + chunksize);
            nBin = fftCQ.getNumberOfOutputBands();
            size = new Dimension(sizeInChunks, nBin);
            dPhaseFreqHz = new float[sizeInChunks][nBin];
            magnArray = new float[sizeInChunks][nBin];
            phaseArray = new float[sizeInChunks][nBin];
        }
        double twoPI = 2 * Math.PI;
        double dPhaRef[] = new double[nBin];
        for (int i = 0; i < nBin; i++) {
            dPhaRef[i] = (twoPI * freq[i] * dt);
        }
        double fftOut[] = new double[nBin * 2];
        double fftIn[] = new double[fftsize];
        double input[] = new double[fftsize];
        double testF = minF * 2;
        int ch = 1;
        int nRead = 0;
        AudioBuffer buffer = new AudioBuffer("TEMP", ch, chunksize, 44100);
        chunkPtr = -fftsize / chunksize / 2;
        chunkStartInSamples = 0;
        do {
            if (Thread.interrupted()) {
                return;
            }
            if (fftsize != chunksize) {
                for (int i = 0; i < fftsize - chunksize; i++) input[i] = input[i + chunksize];
            }
            buffer.makeSilence();
            reader.processAudio(buffer);
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
                fftIn[i] = input[i];
            }
            fftCQ.calc(fftIn, fftOut);
            for (int i = 0; i < nBin; i++) {
                double real = fftOut[2 * i];
                double imag = fftOut[2 * i + 1];
                magnArray[chunkPtr][i] = (float) Math.sqrt(real * real + imag * imag);
                phaseArray[chunkPtr][i] = (float) Math.atan2(imag, real);
                double phaLast;
                if (chunkPtr > 0) {
                    phaLast = phaseArray[chunkPtr - 1][i];
                } else {
                    phaLast = 0.0;
                }
                double dpha = phaseArray[chunkPtr][i] - phaLast;
                dpha = -((dPhaRef[i] - dpha + Math.PI + twoPI) % twoPI - Math.PI);
                dPhaseFreqHz[chunkPtr][i] = (float) (freq[i] + dpha / twoPI / dt);
            }
            notifyMoreDataObservers(magnArray[chunkPtr]);
            chunkPtr++;
            if (chunkPtr >= sizeInChunks) chunkPtr = 0;
        } while (true);
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

    public boolean validAt(long chunkPtr2) {
        return chunkPtr2 >= 0 && chunkPtr2 < this.chunkPtr;
    }

    public FFT getFFT() {
        return fftCQ.getFFT();
    }

    public int getSizeInChunks() {
        return sizeInChunks;
    }

    public void addSizeObserver(CyclicSpectrogramDataListener o) {
        sizeObservers.add(o);
    }

    void notifyMoreDataObservers(float buff[]) {
        for (CyclicSpectrogramDataListener o : sizeObservers) o.notifyMoreDataReady(buff);
    }
}

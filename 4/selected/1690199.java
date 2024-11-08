package neuralmusic.brain;

import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;

public class OscillatorBank implements AudioProcess {

    public Oscillator oscs[];

    private final int RE = 0;

    private final int IM = 1;

    private final int REIM = 2;

    private final int RERE = 3;

    private final int IMRE = 4;

    private final int IMIM = 5;

    private int nOscillators = 0;

    private final float Fs;

    public OscillatorBank(float Fs, double freqs[], double halfLife[]) {
        this.Fs = Fs;
        oscs = new Oscillator[freqs.length];
        for (int i = 0; i < freqs.length; i++) {
            double lamb = Math.log(0.5) / halfLife[i];
            double damp = Math.exp(lamb / Fs);
            oscs[i] = new Oscillator(freqs[i], damp);
        }
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void open() throws Exception {
    }

    @Override
    public int processAudio(AudioBuffer arg0) {
        float buff[] = arg0.getChannel(0);
        int nSamp = arg0.getSampleCount();
        synchronized (oscs) {
            for (Oscillator osc : oscs) {
                double re = osc.state[RE];
                double im = osc.state[IM];
                double reRe = osc.state[RERE];
                double reIm = osc.state[REIM];
                double imRe = osc.state[IMRE];
                double imIm = osc.state[IMIM];
                for (int j = 0; j < nSamp; j++) {
                    double re1 = re * reRe + im * reIm;
                    im = re * imRe + im * imIm;
                    re = re1;
                    buff[j] += (float) re;
                }
                osc.state[RE] = re;
                osc.state[IM] = im;
            }
        }
        return AUDIO_OK;
    }

    public class Oscillator {

        double state[] = new double[IMIM + 1];

        public double freq;

        double damp;

        Oscillator(double freq, double damp) {
            double dang = Math.PI * 2.0 * freq / Fs;
            this.freq = freq;
            this.damp = damp;
            state[RE] = 0.0;
            state[IM] = 0.0;
            state[RERE] = Math.cos(dang) * damp;
            state[REIM] = Math.sin(dang) * damp;
            state[IMRE] = -Math.sin(dang) * damp;
            state[IMIM] = Math.cos(dang) * damp;
        }
    }

    public void exciteAll(double force) {
        synchronized (oscs) {
            for (Oscillator osc : oscs) {
                osc.state[RE] += force;
            }
        }
    }

    public void excite(int i, double force) {
        oscs[i].state[RE] += force;
    }
}

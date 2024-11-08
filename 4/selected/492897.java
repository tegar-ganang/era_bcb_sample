package xtrememp.player.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import javax.sound.sampled.AudioInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tritonus.share.sampled.convert.TAsynchronousFilteredAudioInputStream;

/**
 * Based on original work of Martin Holtzer, licensed under the GPL.
 *
 * @author alexs
 * @author Besmir Beqiri
 */
public class EqAudioInputStream extends TAsynchronousFilteredAudioInputStream {

    private final Logger logger = LoggerFactory.getLogger(EqAudioInputStream.class);

    protected static final int BLOCKSIZE = 17640;

    /**
     * The maximum upper frequency. The highest band would have an even higher
     * upper edge frequency, but it gets reduced to this value.
     */
    private static final int MAX_UPPER_FREQ = 19000;

    /**
     * Number of frequency bands.
     */
    private static final int BAND_COUNT = 10;

    /**
     * Number of filter stages for each band. Each stage resembles one
     * second-order stage of the low-shelving prototype, which becomes a
     * fourth-order stage after frequency-shifting. The resulting filter order
     * per band is thus 4 * STAGE_COUNT.
     */
    private static final int STAGE_COUNT = 2;

    /**
     * The sampling rate used to denormalize frequencies from radian/s to Hz. If
     * the audio data uses another sampling-rate, the labels in the UI will be
     * wrong.
     */
    private static final int SAMPLING_RATE = 44100;

    /**
     * Center frequency of the lowest band.
     */
    private static final double FIRST_CENTER_FREQUENCY = 30;

    /**
     * Logarithm of the highest normalized frequency to plot.
     */
    private static final float UPPER_NORM_FREQ_LOG = 0.4971f;

    /**
     * Logarithm of the lowest normalized frequency to plot.
     */
    private static final float LOWER_NORM_FREQ_LOG = -2.5452f;

    /**
     * Value of {@link #K} for unity gain.
     */
    private double[] KBase = new double[BAND_COUNT];

    /**
     * Band-width and gain dependent filter coefficient.
     */
    private double[] K = new double[BAND_COUNT];

    /**
     * Gain dependent filter coefficient.
     */
    private double[] V = new double[BAND_COUNT];

    /**
     * Auxiliary filter coefficient.
     */
    private double[][] a0recip = new double[BAND_COUNT][STAGE_COUNT];

    /**
     * Stage dependent filter coefficient.
     */
    private double[] c = new double[STAGE_COUNT];

    /**
     * Center frequency dependent coefficient.
     */
    private double[] c0 = new double[BAND_COUNT];

    /**
     * The filter states. In each stage, states 1 and 3 hold the state of delay
     * inside the all-passes and states 0 and 2 resemble the additional delay at
     * the all-pass input.
     */
    private double[][][] filterstates = new double[BAND_COUNT][STAGE_COUNT][4];

    private AudioInputStream sourceStream;

    /**
     * Audio input buffer.
     */
    private ByteBuffer audioDataIn;

    /**
     * Audio output buffer.
     */
    private ByteBuffer audioDataOut;

    /**
     * The {@link #audioDataIn} buffer accessed as <code>short</code>s.
     */
    private ShortBuffer shortDataIn;

    /**
     * The {@link #audioDataOut} buffer accessed as <code>short</code>s.
     */
    private ShortBuffer shortDataOut;

    /**
     * Number of input channels.
     */
    private int nChannels;

    public EqAudioInputStream(AudioInputStream sourceStream) {
        super(sourceStream.getFormat(), sourceStream.getFrameLength());
        this.sourceStream = sourceStream;
        init();
    }

    @Override
    public void close() throws IOException {
        super.close();
        sourceStream.close();
    }

    @Override
    public long skip(long n) throws IOException {
        long sk = sourceStream.skip(n);
        return sk;
    }

    @Override
    public void execute() {
        double[] inSamples = new double[nChannels * BLOCKSIZE / 2];
        double[] outSamples = new double[nChannels * BLOCKSIZE / 2];
        int sampleCount;
        int nBytesRead = 0;
        try {
            nBytesRead = sourceStream.read(audioDataIn.array(), 0, BLOCKSIZE * nChannels);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (nBytesRead == -1) {
            getCircularBuffer().close();
            return;
        }
        byte[] trimBuffer = audioDataIn.array();
        if (nBytesRead < trimBuffer.length) {
            trimBuffer = new byte[nBytesRead];
            System.arraycopy(audioDataIn.array(), 0, trimBuffer, 0, nBytesRead);
        }
        if (nChannels > 1) {
            sampleCount = nBytesRead / 2;
            for (int n = 0; n < nBytesRead / 2; n++) {
                inSamples[n] = (double) shortDataIn.get(n) / 32768;
            }
        } else {
            sampleCount = nBytesRead / 2;
            for (int n = 0; n < nBytesRead / 2; n++) {
                inSamples[n] = (double) shortDataIn.get(n) / 32768;
            }
        }
        for (int n = 0; n < sampleCount; n++) {
            double outValue;
            outSamples[n] = processSample(inSamples[n]);
            outValue = outSamples[n] * 32768;
            if (outValue > 32767) {
                outValue = 32767;
            }
            if (outValue < -32768) {
                outValue = -32768;
            }
            shortDataOut.put(n, (short) outValue);
        }
        if (nBytesRead >= 0) {
            getCircularBuffer().write(audioDataOut.array(), 0, sampleCount * 2);
        }
    }

    /**
     * The actual filtering algorithm.
     *
     * @param u The input sample.
     * @return The resulting output sample.
     */
    protected double processSample(double u) {
        double y = u;
        for (int band = 0; band < BAND_COUNT; band++) {
            for (int stage = 0; stage < STAGE_COUNT; stage++) {
                double x4 = -c0[band] * (filterstates[band][stage][0] - filterstates[band][stage][1]);
                double x6 = filterstates[band][stage][1] + x4;
                filterstates[band][stage][1] = filterstates[band][stage][0] + x4;
                double x8 = -c0[band] * (filterstates[band][stage][2] - filterstates[band][stage][3]);
                double x7 = filterstates[band][stage][3] + x8;
                filterstates[band][stage][3] = filterstates[band][stage][2] + x8;
                double x3 = 2 * x6;
                double x2 = x7 + x3;
                double x1 = a0recip[band][stage] * (K[band] * u - (x7 - x3 + K[band] * (-2 * c[stage] * x7 + K[band] * x2)));
                double x5 = K[band] * (x1 + x2);
                filterstates[band][stage][0] = -x1;
                filterstates[band][stage][2] = -x6;
                y = V[band] * (V[band] * x5 + 2 * (x5 - c[stage] * (x7 - x1))) + u;
                u = y;
            }
        }
        return y;
    }

    /**
     * Initializes the filter coefficients and UI components.
     */
    public void init() {
        nChannels = getFormat().getChannels();
        audioDataIn = ByteBuffer.allocate(BLOCKSIZE * nChannels);
        audioDataIn.order(ByteOrder.LITTLE_ENDIAN);
        shortDataIn = audioDataIn.asShortBuffer();
        audioDataOut = ByteBuffer.allocate(BLOCKSIZE * nChannels);
        audioDataOut.order(ByteOrder.LITTLE_ENDIAN);
        shortDataOut = audioDataOut.asShortBuffer();
        for (int stage = 0; stage < STAGE_COUNT; stage++) {
            c[stage] = Math.cos((.5 - (2. * stage + 1) / (4 * STAGE_COUNT)) * Math.PI);
        }
        for (int band = 0; band < BAND_COUNT; band++) {
            double fC = FIRST_CENTER_FREQUENCY * Math.pow(2, band);
            double fL = fC / Math.sqrt(2);
            double fU = fC * Math.sqrt(2);
            if (fU > MAX_UPPER_FREQ) {
                fU = MAX_UPPER_FREQ;
            }
            double fB = fU - fL;
            double wB = 2 * Math.PI / SAMPLING_RATE * fB;
            double wU = 2 * Math.PI / SAMPLING_RATE * fU;
            double wL = 2 * Math.PI / SAMPLING_RATE * fL;
            double wM = 2 * Math.atan(Math.sqrt(Math.tan(wU / 2) * Math.tan(wL / 2)));
            KBase[band] = Math.tan(wB / 2);
            c0[band] = Math.cos(wM);
            setGain(band, 1.0);
        }
    }

    public void setGain(int band, int gain) {
        if (gain > 12) {
            gain = 12;
        } else if (gain < -12) {
            gain = -12;
        }
        setGain(band, Math.pow(10, gain / 20.));
    }

    /**
     * Sets the gain for the given band.
     *
     * @param band The band to set the gain for.
     * @param gain The multiplicative (i.e. not dB) gain.
     */
    protected void setGain(int band, double gain) {
        K[band] = Math.pow(gain, -1. / (4 * STAGE_COUNT)) * KBase[band];
        V[band] = Math.sqrt(gain) - 1;
        for (int stage = 0; stage < STAGE_COUNT; stage++) {
            a0recip[band][stage] = 1 / (1 + 2 * K[band] * c[stage] + K[band] * K[band]);
        }
    }
}

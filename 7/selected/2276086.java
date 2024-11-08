package net.sourceforge.jffmpeg.codecs.audio.ac3;

import javax.media.Buffer;
import net.sourceforge.jffmpeg.GPLLicense;

/**
 *
 */
public class SoundOutput implements GPLLicense {

    public static final double volumeControl = (1 << 14) * Math.sqrt(2);

    public static final boolean debug = false;

    private static final int[] bit_reverse_512 = new int[] { 0x00, 0x40, 0x20, 0x60, 0x10, 0x50, 0x30, 0x70, 0x08, 0x48, 0x28, 0x68, 0x18, 0x58, 0x38, 0x78, 0x04, 0x44, 0x24, 0x64, 0x14, 0x54, 0x34, 0x74, 0x0c, 0x4c, 0x2c, 0x6c, 0x1c, 0x5c, 0x3c, 0x7c, 0x02, 0x42, 0x22, 0x62, 0x12, 0x52, 0x32, 0x72, 0x0a, 0x4a, 0x2a, 0x6a, 0x1a, 0x5a, 0x3a, 0x7a, 0x06, 0x46, 0x26, 0x66, 0x16, 0x56, 0x36, 0x76, 0x0e, 0x4e, 0x2e, 0x6e, 0x1e, 0x5e, 0x3e, 0x7e, 0x01, 0x41, 0x21, 0x61, 0x11, 0x51, 0x31, 0x71, 0x09, 0x49, 0x29, 0x69, 0x19, 0x59, 0x39, 0x79, 0x05, 0x45, 0x25, 0x65, 0x15, 0x55, 0x35, 0x75, 0x0d, 0x4d, 0x2d, 0x6d, 0x1d, 0x5d, 0x3d, 0x7d, 0x03, 0x43, 0x23, 0x63, 0x13, 0x53, 0x33, 0x73, 0x0b, 0x4b, 0x2b, 0x6b, 0x1b, 0x5b, 0x3b, 0x7b, 0x07, 0x47, 0x27, 0x67, 0x17, 0x57, 0x37, 0x77, 0x0f, 0x4f, 0x2f, 0x6f, 0x1f, 0x5f, 0x3f, 0x7f };

    private static final int[] bit_reverse_256 = new int[] { 0x00, 0x20, 0x10, 0x30, 0x08, 0x28, 0x18, 0x38, 0x04, 0x24, 0x14, 0x34, 0x0c, 0x2c, 0x1c, 0x3c, 0x02, 0x22, 0x12, 0x32, 0x0a, 0x2a, 0x1a, 0x3a, 0x06, 0x26, 0x16, 0x36, 0x0e, 0x2e, 0x1e, 0x3e, 0x01, 0x21, 0x11, 0x31, 0x09, 0x29, 0x19, 0x39, 0x05, 0x25, 0x15, 0x35, 0x0d, 0x2d, 0x1d, 0x3d, 0x03, 0x23, 0x13, 0x33, 0x0b, 0x2b, 0x1b, 0x3b, 0x07, 0x27, 0x17, 0x37, 0x0f, 0x2f, 0x1f, 0x3f };

    private static final double[] window = new double[] { 0.00014, 0.00024, 0.00037, 0.00051, 0.00067, 0.00086, 0.00107, 0.00130, 0.00157, 0.00187, 0.00220, 0.00256, 0.00297, 0.00341, 0.00390, 0.00443, 0.00501, 0.00564, 0.00632, 0.00706, 0.00785, 0.00871, 0.00962, 0.01061, 0.01166, 0.01279, 0.01399, 0.01526, 0.01662, 0.01806, 0.01959, 0.02121, 0.02292, 0.02472, 0.02662, 0.02863, 0.03073, 0.03294, 0.03527, 0.03770, 0.04025, 0.04292, 0.04571, 0.04862, 0.05165, 0.05481, 0.05810, 0.06153, 0.06508, 0.06878, 0.07261, 0.07658, 0.08069, 0.08495, 0.08935, 0.09389, 0.09859, 0.10343, 0.10842, 0.11356, 0.11885, 0.12429, 0.12988, 0.13563, 0.14152, 0.14757, 0.15376, 0.16011, 0.16661, 0.17325, 0.18005, 0.18699, 0.19407, 0.20130, 0.20867, 0.21618, 0.22382, 0.23161, 0.23952, 0.24757, 0.25574, 0.26404, 0.27246, 0.28100, 0.28965, 0.29841, 0.30729, 0.31626, 0.32533, 0.33450, 0.34376, 0.35311, 0.36253, 0.37204, 0.38161, 0.39126, 0.40096, 0.41072, 0.42054, 0.43040, 0.44030, 0.45023, 0.46020, 0.47019, 0.48020, 0.49022, 0.50025, 0.51028, 0.52031, 0.53033, 0.54033, 0.55031, 0.56026, 0.57019, 0.58007, 0.58991, 0.59970, 0.60944, 0.61912, 0.62873, 0.63827, 0.64774, 0.65713, 0.66643, 0.67564, 0.68476, 0.69377, 0.70269, 0.71150, 0.72019, 0.72877, 0.73723, 0.74557, 0.75378, 0.76186, 0.76981, 0.77762, 0.78530, 0.79283, 0.80022, 0.80747, 0.81457, 0.82151, 0.82831, 0.83496, 0.84145, 0.84779, 0.85398, 0.86001, 0.86588, 0.87160, 0.87716, 0.88257, 0.88782, 0.89291, 0.89785, 0.90264, 0.90728, 0.91176, 0.91610, 0.92028, 0.92432, 0.92822, 0.93197, 0.93558, 0.93906, 0.94240, 0.94560, 0.94867, 0.95162, 0.95444, 0.95713, 0.95971, 0.96217, 0.96451, 0.96674, 0.96887, 0.97089, 0.97281, 0.97463, 0.97635, 0.97799, 0.97953, 0.98099, 0.98236, 0.98366, 0.98488, 0.98602, 0.98710, 0.98811, 0.98905, 0.98994, 0.99076, 0.99153, 0.99225, 0.99291, 0.99353, 0.99411, 0.99464, 0.99513, 0.99558, 0.99600, 0.99639, 0.99674, 0.99706, 0.99736, 0.99763, 0.99788, 0.99811, 0.99831, 0.99850, 0.99867, 0.99882, 0.99895, 0.99908, 0.99919, 0.99929, 0.99938, 0.99946, 0.99953, 0.99959, 0.99965, 0.99969, 0.99974, 0.99978, 0.99981, 0.99984, 0.99986, 0.99988, 0.99990, 0.99992, 0.99993, 0.99994, 0.99995, 0.99996, 0.99997, 0.99998, 0.99998, 0.99998, 0.99999, 0.99999, 0.99999, 0.99999, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000, 1.00000 };

    private final void swap_cmplx(double[] buf_re, double[] buf_im, int i, int k) {
        double re = buf_re[i];
        double im = buf_im[i];
        buf_re[i] = buf_re[k];
        buf_im[i] = buf_im[k];
        buf_re[k] = re;
        buf_im[k] = im;
    }

    private double[] buf_re = new double[128];

    private double[] buf_im = new double[128];

    public void a52_imdct_512(double[] data, int dataPointer, int delayPointer, double bias) {
        if (debug) {
            System.out.println("a52_imdct_512");
            for (int i = dataPointer; i < dataPointer + 256; i++) {
                System.out.print(AC3Decoder.show_sample(data[i]) + " ");
            }
            System.out.println();
            System.out.println("delay");
            for (int i = delayPointer; i < delayPointer + 256; i++) {
                System.out.print(AC3Decoder.show_sample(data[i]) + " ");
            }
            System.out.println();
        }
        for (int i = 0; i < 128; i++) {
            buf_re[i] = (data[dataPointer + 256 - 2 * i - 1] * xcos1[i]) - (data[dataPointer + 2 * i] * xsin1[i]);
            buf_im[i] = -1.0 * ((data[dataPointer + 2 * i] * xcos1[i]) + (data[dataPointer + 256 - 2 * i - 1] * xsin1[i]));
        }
        for (int i = 0; i < 128; i++) {
            int k = bit_reverse_512[i];
            if (k < i) {
                swap_cmplx(buf_re, buf_im, i, k);
            }
        }
        if (debug) {
            if (debug) System.out.println("before ifft128");
            for (int i = 0; i < 128; i++) {
                if (debug) System.out.print(AC3Decoder.show_sample(buf_re[i]) + " ");
            }
            if (debug) System.out.println();
        }
        int m, p, q;
        int two_m, two_m_plus_one;
        double tmp_a_r, tmp_a_i;
        double tmp_b_r, tmp_b_i;
        for (m = 0; m < 7; m++) {
            if (m != 0) two_m = (1 << m); else two_m = 1;
            two_m_plus_one = (1 << (m + 1));
            for (int k = 0; k < two_m; k++) {
                for (int i = 0; i < 128; i += two_m_plus_one) {
                    p = k + i;
                    q = p + two_m;
                    tmp_a_r = buf_re[p];
                    tmp_a_i = buf_im[p];
                    tmp_b_r = buf_re[q] * w_re[m][k] - buf_im[q] * w_im[m][k];
                    tmp_b_i = buf_im[q] * w_re[m][k] + buf_re[q] * w_im[m][k];
                    buf_re[p] = tmp_a_r + tmp_b_r;
                    buf_im[p] = tmp_a_i + tmp_b_i;
                    buf_re[q] = tmp_a_r - tmp_b_r;
                    buf_im[q] = tmp_a_i - tmp_b_i;
                }
            }
        }
        if (debug) {
            if (debug) System.out.println("after ifft128");
            for (int i = 0; i < 128; i++) {
                if (debug) System.out.print(AC3Decoder.show_sample(buf_re[i]) + " ");
            }
            if (debug) System.out.println();
        }
        for (int i = 0; i < 128; i++) {
            tmp_a_r = buf_re[i];
            tmp_a_i = -1.0 * buf_im[i];
            buf_re[i] = (tmp_a_r * xcos1[i]) - (tmp_a_i * xsin1[i]);
            buf_im[i] = (tmp_a_r * xsin1[i]) + (tmp_a_i * xcos1[i]);
        }
        int dataPtr = dataPointer;
        int delayPtr = delayPointer;
        int windowPointer = 0;
        if (debug) System.out.println("BIAS " + bias);
        for (int i = 0; i < 64; i++) {
            data[dataPtr++] = -buf_im[64 + i] * window[windowPointer++] + data[delayPtr++] + bias;
            data[dataPtr++] = buf_re[64 - i - 1] * window[windowPointer++] + data[delayPtr++] + bias;
        }
        for (int i = 0; i < 64; i++) {
            data[dataPtr++] = -buf_re[i] * window[windowPointer++] + data[delayPtr++] + bias;
            data[dataPtr++] = buf_im[128 - i - 1] * window[windowPointer++] + data[delayPtr++] + bias;
        }
        delayPtr = delayPointer;
        for (int i = 0; i < 64; i++) {
            data[delayPtr++] = -buf_re[64 + i] * window[--windowPointer];
            data[delayPtr++] = buf_im[64 - i - 1] * window[--windowPointer];
        }
        for (int i = 0; i < 64; i++) {
            data[delayPtr++] = buf_im[i] * window[--windowPointer];
            data[delayPtr++] = -buf_re[128 - i - 1] * window[--windowPointer];
        }
        if (debug) {
            if (debug) System.out.println("a52_imdct_512 after");
            for (int i = dataPointer; i < dataPointer + 256; i++) {
                if (debug) System.out.print(AC3Decoder.show_sample(data[i]) + " ");
            }
            if (debug) System.out.println();
            if (debug) System.out.println("delay");
            for (int i = delayPointer; i < delayPointer + 256; i++) {
                if (debug) System.out.print(AC3Decoder.show_sample(data[i]) + " ");
            }
            if (debug) System.out.println();
        }
    }

    private double[] buf_1_re = new double[64];

    private double[] buf_1_im = new double[64];

    private double[] buf_2_re = new double[64];

    private double[] buf_2_im = new double[64];

    public void a52_imdct_256(double[] data, int dataPointer, int delayPointer, double bias) {
        for (int k = 0; k < 64; k++) {
            int p = 2 * (128 - 2 * k - 1);
            int q = 2 * (2 * k);
            buf_1_re[k] = data[p] * xcos2[k] - data[q] * xsin2[k];
            buf_1_im[k] = -1.0f * (data[q] * xcos2[k] + data[p] * xsin2[k]);
            buf_2_re[k] = data[p + 1] * xcos2[k] - data[q + 1] * xsin2[k];
            buf_2_im[k] = -1.0f * (data[q + 1] * xcos2[k] + data[p + 1] * xsin2[k]);
        }
        for (int i = 0; i < 64; i++) {
            int k = bit_reverse_256[i];
            if (k < i) {
                swap_cmplx(buf_1_re, buf_1_im, i, k);
                swap_cmplx(buf_2_re, buf_2_im, i, k);
            }
        }
        int two_m;
        int two_m_plus_one;
        double tmp_a_r, tmp_a_i;
        double tmp_b_r, tmp_b_i;
        for (int m = 0; m < 6; m++) {
            two_m = (1 << m);
            two_m_plus_one = (1 << (m + 1));
            if (m != 0) two_m = (1 << m); else two_m = 1;
            for (int k = 0; k < two_m; k++) {
                for (int i = 0; i < 64; i += two_m_plus_one) {
                    int p = k + i;
                    int q = p + two_m;
                    tmp_a_r = buf_1_re[p];
                    tmp_a_i = buf_1_im[p];
                    tmp_b_r = buf_1_re[q] * w_re[m][k] - buf_1_im[q] * w_im[m][k];
                    tmp_b_i = buf_1_im[q] * w_re[m][k] + buf_1_re[q] * w_im[m][k];
                    buf_1_re[p] = tmp_a_r + tmp_b_r;
                    buf_1_im[p] = tmp_a_i + tmp_b_i;
                    buf_1_re[q] = tmp_a_r - tmp_b_r;
                    buf_1_im[q] = tmp_a_i - tmp_b_i;
                    tmp_a_r = buf_2_re[p];
                    tmp_a_i = buf_2_im[p];
                    tmp_b_r = buf_2_re[q] * w_re[m][k] - buf_2_im[q] * w_im[m][k];
                    tmp_b_i = buf_2_im[q] * w_re[m][k] + buf_2_re[q] * w_im[m][k];
                    buf_2_re[p] = tmp_a_r + tmp_b_r;
                    buf_2_im[p] = tmp_a_i + tmp_b_i;
                    buf_2_re[q] = tmp_a_r - tmp_b_r;
                    buf_2_im[q] = tmp_a_i - tmp_b_i;
                }
            }
        }
        for (int i = 0; i < 64; i++) {
            tmp_a_r = buf_1_re[i];
            tmp_a_i = -buf_1_im[i];
            buf_1_re[i] = (tmp_a_r * xcos2[i]) - (tmp_a_i * xsin2[i]);
            buf_1_im[i] = (tmp_a_r * xsin2[i]) + (tmp_a_i * xcos2[i]);
            tmp_a_r = buf_2_re[i];
            tmp_a_i = -buf_2_im[i];
            buf_2_re[i] = (tmp_a_r * xcos2[i]) - (tmp_a_i * xsin2[i]);
            buf_2_im[i] = (tmp_a_r * xsin2[i]) + (tmp_a_i * xcos2[i]);
        }
        int dataPtr = dataPointer;
        int delayPtr = delayPointer;
        int windowPointer = 0;
        if (debug) System.out.println("BIAS " + bias);
        for (int i = 0; i < 64; i++) {
            data[dataPtr++] = -buf_1_im[i] * window[windowPointer++] + data[delayPtr++] + bias;
            data[dataPtr++] = buf_1_re[64 - i - 1] * window[windowPointer++] + data[delayPtr++] + bias;
        }
        for (int i = 0; i < 64; i++) {
            data[dataPtr++] = -buf_1_re[i] * window[windowPointer++] + data[delayPtr++] + bias;
            data[dataPtr++] = buf_1_im[64 - i - 1] * window[windowPointer++] + data[delayPtr++] + bias;
        }
        delayPtr = delayPointer;
        for (int i = 0; i < 64; i++) {
            data[delayPtr++] = -buf_2_re[i] * window[--windowPointer];
            data[delayPtr++] = buf_2_im[64 - i - 1] * window[--windowPointer];
        }
        for (int i = 0; i < 64; i++) {
            data[delayPtr++] = buf_2_im[i] * window[--windowPointer];
            data[delayPtr++] = -buf_2_re[64 - i - 1] * window[--windowPointer];
        }
    }

    private double[] xcos1 = new double[128];

    private double[] xsin1 = new double[128];

    private double[] xcos2 = new double[64];

    private double[] xsin2 = new double[64];

    private double[][] w_re = new double[8][256];

    private double[][] w_im = new double[8][256];

    /**
     * Constructor cache values
     */
    public SoundOutput() {
        int i, j, k;
        for (i = 0; i < 128; i++) {
            xcos1[i] = -Math.cos((Math.PI / 2048) * (8 * i + 1));
            xsin1[i] = -Math.sin((Math.PI / 2048) * (8 * i + 1));
        }
        for (i = 0; i < 64; i++) {
            xcos2[i] = -Math.cos((Math.PI / 1024) * (8 * i + 1));
            xsin2[i] = -Math.sin((Math.PI / 1024) * (8 * i + 1));
        }
        for (i = 0; i < 7; i++) {
            j = 1 << i;
            for (k = 0; k < j; k++) {
                w_re[i][k] = Math.cos(-Math.PI * k / j);
                w_im[i][k] = Math.sin(-Math.PI * k / j);
            }
        }
    }

    /**
     * Output to buffer
     */
    public void getAudioBuffer(double[] data, int numberOfChannels, Buffer output) {
        if (numberOfChannels > 2) numberOfChannels = 2;
        byte[] outputData = (byte[]) output.getData();
        int outputDataLen = output.getLength();
        if (outputData == null) {
            outputData = new byte[numberOfChannels * 256 * 2 * 1000];
            outputDataLen = 0;
            output.setData(outputData);
            output.setLength(outputDataLen);
        }
        if (outputData.length < numberOfChannels * 256 * 2 + output.getLength()) {
            byte[] newOutputData = new byte[(outputDataLen + numberOfChannels * 256 * 2) * 2];
            System.arraycopy(outputData, 0, newOutputData, 0, outputDataLen);
            outputData = newOutputData;
            output.setData(outputData);
        }
        output.setLength(outputDataLen + numberOfChannels * 256 * 2);
        if (debug) System.out.println("DATAOUT");
        for (int i = 0; i < 256; i++) {
            for (int ch = 0; ch < numberOfChannels; ch++) {
                int v = (int) ((double) volumeControl * (double) data[i + 256 * ch + 256]);
                if (v > (1 << 15) - 1) {
                    v = 2 ^ 15 - 1;
                }
                if (v < -(1 << 15)) {
                    v = -2 ^ 15;
                }
                outputData[outputDataLen++] = (byte) (v & 0xff);
                outputData[outputDataLen++] = (byte) ((v >> 8) & 0xff);
            }
        }
        if (debug) System.out.println();
    }
}

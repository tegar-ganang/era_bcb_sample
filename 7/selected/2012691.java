package sound;

import temp.TypeConverter;

public class SamplesIntSigned extends Samples {

    private int[] samples;

    public SamplesIntSigned(byte[] samples) {
        this.samples = createSamples(samples);
    }

    public int[] createSamples(byte[] samples) {
        int[] sampleInt = new int[samples.length / 4];
        byte[] temp = new byte[4];
        int j = 0;
        for (int i = 0; i < samples.length; i += 4) {
            temp[0] = samples[i];
            temp[1] = samples[i + 1];
            temp[2] = samples[i + 2];
            temp[3] = samples[i + 3];
            sampleInt[j] = TypeConverter.byteArrayToInt(temp);
            j++;
        }
        return sampleInt;
    }

    public int[] getSamples() {
        return samples;
    }

    @Override
    public int getSample(int n) {
        return samples[n - 1];
    }

    @Override
    public int getQtdeSamples() {
        return samples.length;
    }
}

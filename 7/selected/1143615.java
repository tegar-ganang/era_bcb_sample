package sound;

import temp.TypeConverter;

public class SampleShortSigned extends Samples {

    private short[] samples;

    public SampleShortSigned(byte[] samples) {
        this.samples = createSamples(samples);
    }

    public short[] createSamples(byte[] samples) {
        short[] sampleShort = new short[samples.length / 2];
        byte[] temp = new byte[2];
        for (int i = 0, j = 0; i < samples.length; i += 2, j++) {
            temp[0] = samples[i];
            temp[1] = samples[i + 1];
            sampleShort[j] = TypeConverter.byteArrayToShort(temp);
        }
        return sampleShort;
    }

    public short[] getSamples() {
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

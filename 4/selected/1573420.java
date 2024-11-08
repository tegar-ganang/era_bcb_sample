package javazoom.jlme.decoder;

public class SampleBuffer {

    public static final int OBUFFERSIZE = 4 * 1152;

    public static final int MAXCHANNELS = 2;

    private final byte[] buffer = new byte[OBUFFERSIZE];

    private final int[] bufferp = new int[MAXCHANNELS];

    private int channels;

    private int frequency;

    public SampleBuffer(int sample_frequency, int number_of_channels) {
        channels = (number_of_channels == 1) ? 1 : 3;
        frequency = sample_frequency;
        bufferp[0] = 0;
        bufferp[1] = 2;
    }

    public final int getBufferIndex(int channel) {
        return bufferp[channel];
    }

    public final void setBufferIndex(int channel, int index) {
        bufferp[channel] = index;
    }

    public final int getBufferChannelCount() {
        return channels;
    }

    public int getChannelCount() {
        return (channels == 1) ? 1 : 2;
    }

    public int getSampleFrequency() {
        return this.frequency;
    }

    public byte[] getBuffer() {
        return this.buffer;
    }

    public int size() {
        return this.bufferp[0];
    }

    public void clear() {
        bufferp[0] = 0;
        bufferp[1] = 2;
    }
}

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Control;

/** This class creates an object that parses an audioInputStream
 *  into a 2d short array containing each channel's samples.
 *  
 *  	short audioChannels[numChannels][samplesPerChannel]
 *  
 *  The object also returns the number of channels and channel length
 *  in samples.  Only frame sizes equal to 2 bytes (16-bit samples) 
 *  are valid for this class.
 */
public class AudioArray {

    private short[][] chs;

    private int sampleRate, sampleSizeInBits, numChannels, frameSize, channelLength, index, channelBufferSize;

    private float frameRate;

    private AudioInputStream stream;

    public AudioArray(AudioInputStream stream) {
        sampleRate = (int) stream.getFormat().getSampleRate();
        sampleSizeInBits = stream.getFormat().getSampleSizeInBits();
        numChannels = stream.getFormat().getChannels();
        channelLength = (int) stream.getFrameLength();
        frameSize = stream.getFormat().getFrameSize();
        frameRate = stream.getFormat().getFrameRate();
        chs = new short[numChannels][channelLength];
        this.stream = stream;
        index = 0;
        channelBufferSize = 4096;
        loadAudioFile();
    }

    public short[] getChannel(int ch) {
        return chs[ch];
    }

    public int getNumChannels() {
        return numChannels;
    }

    public int getChannelLength() {
        return channelLength;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public AudioFormat getFormat() {
        return stream.getFormat();
    }

    public short[][] read() {
        int length = channelBufferSize;
        if (index >= channelLength) return null;
        if (length > (channelLength - index)) length = channelLength - index;
        short[][] s = new short[numChannels][length];
        for (int j = 0; j < numChannels; j++) {
            System.arraycopy(chs[j], index, s[j], 0, length);
        }
        index += length;
        try {
            Thread.sleep(Math.round(1000 * (length / (double) sampleRate)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    public void setChannelBufferSize(int n) {
        channelBufferSize = n;
    }

    public void reset() {
        index = 0;
    }

    private void loadAudioFile() {
        try {
            int ch_index;
            byte byteFrame[] = new byte[frameSize];
            for (int i = 0; i < channelLength; i++) {
                ch_index = 0;
                stream.read(byteFrame, 0, byteFrame.length);
                for (int ch = 0; ch < numChannels; ch++) {
                    chs[ch][i] = (short) ((byteFrame[ch_index + 1] << 8) + (byteFrame[ch_index] & 0xff));
                    ch_index += 2;
                }
            }
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

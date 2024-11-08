package au.vermilion.samplebank;

import static au.vermilion.Vermilion.logger;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * A support class used to decode WAV files.
 */
public final class WavStreamHandler implements IStreamHandler {

    private AudioFormat.Encoding format;

    private int numChannels;

    private int bitsPerSample;

    private ByteBuffer buffer;

    private boolean swapBytes;

    private static final int BUFFERSIZE = 16384;

    private AudioInputStream str;

    private AudioFormat fmt;

    private long frameLength;

    private int sampleRate;

    private final File fileHdl;

    /**
     * The constructor accepts an audio format and a buffersize for the reading.
     * @param fmt The format of the data we are expecting (as read by Java).
     * @param bufferSize The size of the read buffer we will use.
     */
    public WavStreamHandler(File fileIn) {
        fileHdl = fileIn;
        try {
            str = AudioSystem.getAudioInputStream(fileIn);
            fmt = str.getFormat();
            format = fmt.getEncoding();
            numChannels = fmt.getChannels();
            bitsPerSample = fmt.getSampleSizeInBits();
            swapBytes = !fmt.isBigEndian();
            sampleRate = (int) fmt.getSampleRate();
            frameLength = str.getFrameLength();
            buffer = ByteBuffer.allocate(BUFFERSIZE);
        } catch (Exception ex) {
        }
    }

    /**
     * Processes a chunk of data using the expected format, returning floating
     * point sample data.
     */
    @Override
    public void loadSample(float[][] sampleData) {
        logger.log(Level.INFO, "Trying to load Java supported format {0}", fmt);
        int currChannel = 0;
        int currSample = 0;
        try {
            if (format == AudioFormat.Encoding.PCM_SIGNED) {
                if (bitsPerSample == 16) {
                    int numBytesRead = str.read(buffer.array());
                    while (numBytesRead > 0) {
                        buffer.rewind();
                        for (int x = 0; x < numBytesRead / 2; x++) {
                            short s = buffer.getShort(x * 2);
                            if (swapBytes) s = Short.reverseBytes(s);
                            float f = s / 32768.0f;
                            sampleData[currChannel][currSample] = f;
                            currChannel++;
                            currChannel %= numChannels;
                            if (currChannel == 0) currSample++;
                        }
                        numBytesRead = str.read(buffer.array());
                    }
                } else {
                }
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public long getFrameLength() {
        return frameLength;
    }

    @Override
    public int getChannels() {
        return numChannels;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public boolean isOpen() {
        return str != null;
    }
}

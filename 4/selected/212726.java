package net.lunglet.sound.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;
import net.lunglet.sound.sampled.RawAudioFileWriter;

public final class SoundUtils {

    /**
     * Read audio channel from file and return it as signed 16-bit,
     * little-endian PCM data.
     */
    public static byte[] readChannel(final File file, final int channel) throws FileNotFoundException {
        return readChannel(new FileInputStream(file), channel);
    }

    /**
     * Read audio channel from stream and return it as signed 16-bit,
     * little-endian PCM data.
     */
    public static byte[] readChannel(final InputStream stream, final int channel) {
        final AudioInputStream audioStream;
        try {
            audioStream = AudioSystem.getAudioInputStream(stream);
        } catch (UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        AudioInputStream sourceStream = AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, audioStream);
        int channels = sourceStream.getFormat().getChannels();
        int sampleSizeInBits = sourceStream.getFormat().getSampleSizeInBits();
        if (sampleSizeInBits % 8 != 0) {
            throw new UnsupportedOperationException();
        }
        int sampleSizeInBytes = sampleSizeInBits >>> 3;
        if (sourceStream.getFrameLength() > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException();
        }
        int frameLength = (int) sourceStream.getFrameLength();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            AudioSystem.write(sourceStream, RawAudioFileWriter.RAW, baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] samples = baos.toByteArray();
        byte[] channelData = new byte[sampleSizeInBytes * frameLength];
        for (int i = 0, sampleOffset = 0; i < frameLength; i++) {
            for (int j = 0; j < channels; j++) {
                for (int k = 0; k < sampleSizeInBytes; k++, sampleOffset++) {
                    if (j == channel) {
                        int channelOffset = i * sampleSizeInBytes + k;
                        channelData[channelOffset] = samples[sampleOffset];
                    }
                }
            }
        }
        return channelData;
    }

    /**
     * Read all channels from stream and return it as signed 16-bit PCM data.
     */
    public static byte[][] readChannels(final AudioInputStream origStream) {
        AudioInputStream ais = AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, origStream);
        int channels = ais.getFormat().getChannels();
        int sampleSizeInBits = ais.getFormat().getSampleSizeInBits();
        if (sampleSizeInBits % 8 != 0) {
            throw new UnsupportedOperationException();
        }
        int sampleSizeInBytes = sampleSizeInBits >>> 3;
        if (ais.getFrameLength() > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException();
        }
        int frameLength = (int) ais.getFrameLength();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            AudioSystem.write(ais, RawAudioFileWriter.RAW, baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] samples = baos.toByteArray();
        byte[][] channelsData = new byte[channels][];
        for (int channel = 0; channel < channels; channel++) {
            channelsData[channel] = new byte[sampleSizeInBytes * frameLength];
        }
        for (int i = 0, sampleOffset = 0; i < frameLength; i++) {
            for (int channel = 0; channel < channels; channel++) {
                for (int k = 0; k < sampleSizeInBytes; k++, sampleOffset++) {
                    int channelOffset = i * sampleSizeInBytes + k;
                    channelsData[channel][channelOffset] = samples[sampleOffset];
                }
            }
        }
        return channelsData;
    }

    private SoundUtils() {
    }
}

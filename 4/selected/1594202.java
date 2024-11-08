package net.java.games.joal.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.sound.sampled.*;
import net.java.games.joal.*;

/**
 * A Loader utility for (.wav) files. Creates a WAVData object containing the
 * data used by the AL.alBufferData method.
 *
 * @author Athomas Goldberg
 */
public class WAVLoader implements ALConstants {

    private static final int BUFFER_SIZE = 128000;

    /**
     * This method loads a (.wav) file into a WAVData object.
     *
     * @param filename The name of the (.wav) file
     *
     * @return a WAVData object containing the audio data 
     *
     * @throws UnsupportedAudioFileException if the format of the audio if not
     *                                       supported. 
     * @throws IOException If the file can no be found or some other IO error 
     *                     occurs
     */
    public static WAVData loadFromFile(String filename) throws UnsupportedAudioFileException, IOException {
        WAVData result = null;
        File soundFile = new File(filename);
        AudioInputStream aIn = AudioSystem.getAudioInputStream(soundFile);
        return readFromStream(aIn);
    }

    /**
     * This method loads a (.wav) file into a WAVData object.
     *
     * @param stream An InputStream for the .WAV file.
     *
     * @return a WAVData object containing the audio data 
     *
     * @throws UnsupportedAudioFileException if the format of the audio if not
     *                                       supported. 
     * @throws IOException If the file can no be found or some other IO error 
     *                     occurs
     */
    public static WAVData loadFromStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        WAVData result = null;
        AudioInputStream aIn = AudioSystem.getAudioInputStream(stream);
        return readFromStream(aIn);
    }

    private static WAVData readFromStream(AudioInputStream aIn) throws UnsupportedAudioFileException, IOException {
        ReadableByteChannel aChannel = Channels.newChannel(aIn);
        AudioFormat fmt = aIn.getFormat();
        int numChannels = fmt.getChannels();
        int bits = fmt.getSampleSizeInBits();
        int format = AL_FORMAT_MONO8;
        if ((bits == 8) && (numChannels == 1)) {
            format = AL_FORMAT_MONO8;
        } else if ((bits == 16) && (numChannels == 1)) {
            format = AL_FORMAT_MONO16;
        } else if ((bits == 8) && (numChannels == 2)) {
            format = AL_FORMAT_STEREO8;
        } else if ((bits == 16) && (numChannels == 2)) {
            format = AL_FORMAT_STEREO16;
        }
        int freq = Math.round(fmt.getSampleRate());
        int size = aIn.available();
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        while (buffer.remaining() > 0) {
            aChannel.read(buffer);
        }
        buffer.rewind();
        if ((bits == 16) && (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN)) {
            int len = buffer.remaining();
            for (int i = 0; i < len; i += 2) {
                byte a = buffer.get(i);
                byte b = buffer.get(i + 1);
                buffer.put(i, b);
                buffer.put(i + 1, a);
            }
        }
        WAVData result = new WAVData(buffer, format, size, freq, false);
        aIn.close();
        return result;
    }
}

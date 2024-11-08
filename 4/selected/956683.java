package org.xith3d.sound.loaders;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.xith3d.sound.BufferFormat;
import org.xith3d.sound.SoundBuffer;
import org.xith3d.sound.SoundDataContainer;
import org.xith3d.sound.SoundDriver;

/**
 * Loads Sound data from WAVE files.
 * 
 * @author David Yazel
 * @author Yuri Vl. Gushchin
 * @author Marvin Froehlich (aka Qudus) [code cleaning]
 */
public class WavLoader implements SoundDataContainer {

    /**
     * This class is a holder for WAV (.wav ) data returned from the WavLoader
     * 
     * @author Athomas Goldberg
     */
    private static final class WAVData {

        /** The audio data */
        final ByteBuffer data;

        /** the format of the Data */
        final BufferFormat format;

        /** Size (in bytes) of the data */
        final int size;

        /** The frequency of the data */
        final int freq;

        /** flag indicating whether or not the sound in the data should loop */
        final boolean loop;

        WAVData(ByteBuffer data, BufferFormat format, int size, int freq, boolean loop) {
            this.data = data;
            this.format = format;
            this.size = size;
            this.freq = freq;
            this.loop = loop;
        }
    }

    private WAVData wd = null;

    private SoundBuffer buffer = null;

    public void load(URL url) throws IOException {
        try {
            wd = loadFromUrl(url);
        } catch (UnsupportedAudioFileException e) {
            throw (new IOException(e.getMessage()));
        }
    }

    public void load(String filename) throws IOException {
        try {
            wd = loadFromFile(filename);
        } catch (UnsupportedAudioFileException e) {
            throw (new IOException(e.getMessage()));
        }
    }

    public void load(InputStream data) throws IOException {
        try {
            wd = loadFromInputStream(data);
        } catch (UnsupportedAudioFileException e) {
            throw (new IOException(e.getMessage()));
        }
    }

    public void load(byte[] data) throws IOException {
        try {
            wd = loadFromInputStream(new ByteArrayInputStream(data));
        } catch (UnsupportedAudioFileException e) {
            throw (new IOException(e.getMessage()));
        }
    }

    public boolean isStreaming() {
        return (false);
    }

    public SoundBuffer getData(SoundDriver driver) {
        if (buffer != null) return (buffer); else {
            buffer = driver.allocateSoundBuffer();
            buffer.setData(wd.format, wd.size, wd.freq, wd.data);
            return (buffer);
        }
    }

    public void returnData(SoundDriver driver, SoundBuffer buffer) {
    }

    public void rewind(SoundDriver driver) {
    }

    /**
     * This method loads a (.wav) file into a WAVData object.
     * 
     * @author Yuri Vl. Gushchin
     * 
     * @param filename The name of the (.wav) file
     * @return a WAVData object containing the audio data
     * @throws UnsupportedAudioFileException if the format of the audio if not
     *             supported.
     * @throws IOException If the file can no be found or some other IO error
     *             occurs
     */
    private static WAVData loadFromFile(String filename) throws UnsupportedAudioFileException, IOException {
        return (loadFromAudioInputStream(AudioSystem.getAudioInputStream(new File(filename))));
    }

    /**
     * This method loads a (.wav) data into a WAVData object.
     * 
     * @author Yuri Vl. Gushchin
     * 
     * @param url The name of the (.wav) data
     * @return a WAVData object containing the audio data
     * @throws UnsupportedAudioFileException if the format of the audio if not
     *             supported.
     * @throws IOException If the file can no be found or some other IO error
     *             occurs
     */
    private static WAVData loadFromUrl(URL url) throws UnsupportedAudioFileException, IOException {
        return (loadFromAudioInputStream(AudioSystem.getAudioInputStream(url)));
    }

    /**
     * This method loads a (.wav) data into a WAVData object.
     * 
     * @author Yuri Vl. Gushchin
     * 
     * @param data InputStream that can provide audio data
     * @return a WAVData object containing the audio data
     * @throws UnsupportedAudioFileException if the format of the audio if not
     *             supported.
     * @throws IOException If the file can no be found or some other IO error
     *             occurs
     */
    private static WAVData loadFromInputStream(InputStream data) throws UnsupportedAudioFileException, IOException {
        return (loadFromAudioInputStream(AudioSystem.getAudioInputStream(data)));
    }

    /**
     * This method loads audio data into a WAVData object.
     * 
     * @author Athomas Goldberg
     * @author Yuri Vl. Gushchin
     * 
     * @param aIn AudioInputStream that contains audio data to load
     * @return a WAVData object containing the audio data
     * @throws UnsupportedAudioFileException if the format of the audio if not
     *             supported.
     * @throws IOException If the file can no be found or some other IO error
     *             occurs
     */
    private static WAVData loadFromAudioInputStream(AudioInputStream aIn) throws UnsupportedAudioFileException, IOException {
        WAVData result = null;
        ReadableByteChannel aChannel = Channels.newChannel(aIn);
        AudioFormat fmt = aIn.getFormat();
        int numChannels = fmt.getChannels();
        int bits = fmt.getSampleSizeInBits();
        BufferFormat format;
        if ((bits == 8) && (numChannels == 1)) {
            format = BufferFormat.MONO8;
        } else if ((bits == 16) && (numChannels == 1)) {
            format = BufferFormat.MONO16;
        } else if ((bits == 8) && (numChannels == 2)) {
            format = BufferFormat.STEREO8;
        } else if ((bits == 16) && (numChannels == 2)) {
            format = BufferFormat.STEREO16;
        } else {
            format = BufferFormat.MONO8;
        }
        int freq = Math.round(fmt.getSampleRate());
        int size = aIn.available();
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        aChannel.read(buffer);
        result = new WAVData(buffer, format, size, freq, false);
        aIn.close();
        return (result);
    }
}

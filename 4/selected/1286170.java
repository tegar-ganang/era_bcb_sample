package org.xith3d.loaders.sound.impl.wav;

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
import org.jagatoo.util.nio.BufferUtils;
import org.xith3d.loaders.sound.SoundLoader;
import org.xith3d.sound.BufferFormat;

/**
 * This is a SoundLoader implementation for Wave sounds (.wav).
 * 
 * @author Marvin Froehlich (aka Qudus)
 */
public class WaveLoader extends SoundLoader {

    /**
     * The default extension to assume for Wave files.
     */
    public static final String DEFAULT_EXTENSION = "wav";

    private static WaveLoader singletonInstance = null;

    /**
     * @return the WaveLoader instance to use as singleton.
     */
    public static WaveLoader getInstance() {
        if (singletonInstance == null) singletonInstance = new WaveLoader();
        return (singletonInstance);
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
    private static WaveData loadFromAudioInputStream(AudioInputStream aIn) throws UnsupportedAudioFileException, IOException {
        WaveData result = null;
        ReadableByteChannel aChannel = Channels.newChannel(aIn);
        AudioFormat fmt = aIn.getFormat();
        int numChannels = fmt.getChannels();
        int bits = fmt.getSampleSizeInBits();
        BufferFormat format = null;
        try {
            format = BufferFormat.getFromValues(bits, numChannels);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            format = BufferFormat.MONO8;
        }
        int freq = Math.round(fmt.getSampleRate());
        int size = aIn.available();
        ByteBuffer buffer = BufferUtils.createByteBuffer(size);
        aChannel.read(buffer);
        result = new WaveData(buffer, format, size, freq, false);
        aIn.close();
        return (result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WaveSoundContainer loadSound(InputStream in) throws IOException {
        try {
            WaveData wd = loadFromAudioInputStream(AudioSystem.getAudioInputStream(in));
            WaveSoundContainer container = new WaveSoundContainer(wd);
            return (container);
        } catch (UnsupportedAudioFileException e) {
            IOException e2 = new IOException();
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WaveSoundContainer loadSound(URL url) throws IOException {
        try {
            WaveData wd = loadFromAudioInputStream(AudioSystem.getAudioInputStream(url));
            WaveSoundContainer container = new WaveSoundContainer(wd);
            return (container);
        } catch (UnsupportedAudioFileException e) {
            IOException e2 = new IOException();
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WaveSoundContainer loadSound(String filename) throws IOException {
        try {
            WaveData wd = loadFromAudioInputStream(AudioSystem.getAudioInputStream(new File(filename)));
            WaveSoundContainer container = new WaveSoundContainer(wd);
            return (container);
        } catch (UnsupportedAudioFileException e) {
            IOException e2 = new IOException();
            e2.initCause(e);
            throw e2;
        }
    }
}

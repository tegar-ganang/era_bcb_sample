package org.mazix.kernel.sound;

import static java.util.logging.Level.SEVERE;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static org.mazix.constants.CharacterConstants.COMMA_CHAR;
import static org.mazix.constants.CharacterConstants.LEFT_BRACES_CHAR;
import static org.mazix.constants.CharacterConstants.RIGHT_BRACES_CHAR;
import static org.mazix.constants.GlobalConstants.MINUS_ONE_RETURN_CODE;
import static org.mazix.constants.log.ErrorConstants.CLOSE_AUDIO_ERROR;
import static org.mazix.constants.log.ErrorConstants.CLOSE_AUDIO_STREAM_ERROR;
import static org.mazix.constants.log.ErrorConstants.PLAY_AUDIO_ERROR;
import static org.mazix.constants.log.ErrorConstants.PLAY_AUDIO_STREAM_ERROR;
import static org.mazix.constants.log.ErrorConstants.READ_AUDIO_ERROR;
import static org.mazix.constants.log.ErrorConstants.UNSUPPORTED_AUDIO_FILE_ERROR;
import static org.mazix.constants.log.InfoConstants.DISPLAYING_AUDIO_PROPERTIES_INFO;
import static org.mazix.constants.log.InfoConstants.NO_AUDIO_PROPERTIES_INFO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.mazix.log.LogUtils;

/**
 * The class which allow to read sounds and musics.
 * 
 * @author Benjamin Croizet (graffity2199@yahoo.fr)
 * @since 0.7
 * @version 0.7
 */
public class AudioManager {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger("org.mazix.kernel.sound.AudioManager");

    /** The channels number when converting OGG file to PCM. */
    private static final int PCM_CHANNELS_NUMBER = 16;

    /** The data audio buffer when playing audio files. */
    private static final int DATA_AUDIO_BUFFER = 4096;

    /**
     * Gets the audio {@link SourceDataLine} following the {@link AudioFormat}.
     * 
     * @param audioFormat
     *            the {@link AudioFormat} to get the line from, mustn't be <code>null</code>.
     * @return the audio {@link SourceDataLine} from the passed format.
     * @throws LineUnavailableException
     *             if a matching audio line is not available due to resource restrictions.
     * @since 0.7
     */
    private SourceDataLine getLine(final AudioFormat audioFormat) throws LineUnavailableException {
        assert audioFormat != null : "audioFormat is null";
        final DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        final SourceDataLine res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    /**
     * Plays the audio file following its file path.
     * 
     * @param audioFilePath
     *            the audio file path name to play, mustn't be <code>null</code>.
     * @since 0.7
     */
    public void playAudio(final String audioFilePath) {
        assert audioFilePath != null : "audioFilePath is null";
        final File file = new File(audioFilePath);
        AudioInputStream in = null;
        try {
            in = AudioSystem.getAudioInputStream(file);
            playAudioInputStream(in);
        } catch (final UnsupportedAudioFileException e) {
            LOGGER.log(SEVERE, LogUtils.buildLogString(PLAY_AUDIO_ERROR, audioFilePath), e);
        } catch (final IOException e) {
            LOGGER.log(SEVERE, LogUtils.buildLogString(PLAY_AUDIO_ERROR, audioFilePath), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                    LOGGER.log(SEVERE, LogUtils.buildLogString(CLOSE_AUDIO_ERROR, audioFilePath), e);
                }
            }
        }
    }

    /**
     * Plays the audio file following its {@link URL}.
     * 
     * @param audioURL
     *            the audio file {@link URL} to play, mustn't be <code>null</code>.
     * @since 0.7
     */
    public void playAudio(final URL audioURL) {
        assert audioURL != null : "audioURL is null";
        AudioInputStream in = null;
        try {
            in = AudioSystem.getAudioInputStream(audioURL);
            playAudioInputStream(in);
        } catch (final UnsupportedAudioFileException e) {
            LOGGER.log(SEVERE, LogUtils.buildLogString(PLAY_AUDIO_ERROR, audioURL), e);
        } catch (final IOException e) {
            LOGGER.log(SEVERE, LogUtils.buildLogString(PLAY_AUDIO_ERROR, audioURL), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                    LOGGER.log(SEVERE, LogUtils.buildLogString(CLOSE_AUDIO_ERROR, audioURL), e);
                }
            }
        }
    }

    /**
     * Plays the following audio input stream.
     * 
     * @param audioInputStream
     *            the audio input stream to play, mustn't be <code>null</code>.
     * @since 0.7
     */
    private void playAudioInputStream(final AudioInputStream audioInputStream) {
        assert audioInputStream != null : "audioInputStream is null";
        AudioInputStream din = null;
        try {
            final AudioFormat baseFormat = audioInputStream.getFormat();
            final AudioFormat decodedFormat = new AudioFormat(PCM_SIGNED, baseFormat.getSampleRate(), PCM_CHANNELS_NUMBER, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), baseFormat.isBigEndian());
            din = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);
            rawplay(decodedFormat, din);
        } catch (final IOException e) {
            LOGGER.log(SEVERE, PLAY_AUDIO_STREAM_ERROR, e);
        } catch (final LineUnavailableException e) {
            LOGGER.log(SEVERE, PLAY_AUDIO_STREAM_ERROR, e);
        } finally {
            if (din != null) {
                try {
                    din.close();
                } catch (final IOException e) {
                    LOGGER.log(SEVERE, CLOSE_AUDIO_STREAM_ERROR, e);
                }
            }
        }
    }

    /**
     * This method allow to print audio properties in the log from a sound file path.
     * 
     * @param audioFilePath
     *            the audio file path, mustn't be <code>null</code>.
     * @since 0.7
     */
    public void printAudioProperties(final String audioFilePath) {
        assert audioFilePath != null : "audioFilePath is null";
        final File soundFile = new File(audioFilePath);
        try {
            final AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(soundFile);
            LOGGER.info(LogUtils.buildLogString(DISPLAYING_AUDIO_PROPERTIES_INFO, new Object[] { audioFilePath, audioFileFormat }));
            printPropertiesMap(audioFileFormat.properties());
        } catch (final UnsupportedAudioFileException e) {
            LOGGER.log(SEVERE, LogUtils.buildLogString(UNSUPPORTED_AUDIO_FILE_ERROR, audioFilePath), e);
        } catch (final IOException e) {
            LOGGER.log(SEVERE, LogUtils.buildLogString(READ_AUDIO_ERROR, audioFilePath), e);
        }
    }

    /**
     * This method simply prints a properties {@link Map} in the log.
     * 
     * @param propertiesMap
     *            the properties {@link Map} containing property keys as {@link String} and the
     *            associated {@link Object} value, must'nt be <code>null</code>.
     * @since 0.7
     */
    private void printPropertiesMap(final Map<String, Object> propertiesMap) {
        assert propertiesMap != null : "propertiesMap is null";
        if (propertiesMap.isEmpty()) {
            LOGGER.info(NO_AUDIO_PROPERTIES_INFO);
        } else {
            for (final Map.Entry<String, Object> e : propertiesMap.entrySet()) {
                LOGGER.info(LEFT_BRACES_CHAR + e.getKey() + COMMA_CHAR + e.getValue() + RIGHT_BRACES_CHAR);
            }
        }
    }

    /**
     * Plays the {@link AudioInputStream} in the target {@link AudioFormat}.
     * 
     * @param targetFormat
     *            the target {@link AudioFormat} to read the audio input stream, mustn't be
     *            <code>null</code>.
     * @param audioInputStream
     *            the {@link AudioInputStream} to play, mustn't be <code>null</code>.
     * @throws IOException
     *             if any input exception occurs while reading the audio input stream.
     * @throws LineUnavailableException
     *             if a matching audio line is not available due to resource restrictions.
     * @since 0.7
     */
    private void rawplay(final AudioFormat targetFormat, final AudioInputStream audioInputStream) throws IOException, LineUnavailableException {
        final SourceDataLine line = getLine(targetFormat);
        line.start();
        int nBytesRead = 0;
        final byte[] data = new byte[DATA_AUDIO_BUFFER];
        while (nBytesRead != MINUS_ONE_RETURN_CODE) {
            nBytesRead = audioInputStream.read(data, 0, data.length);
            if (nBytesRead != MINUS_ONE_RETURN_CODE) {
                line.write(data, 0, nBytesRead);
            }
        }
        line.drain();
        line.stop();
        line.close();
    }
}

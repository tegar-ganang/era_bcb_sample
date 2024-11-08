package javaclient3.structures.audiodsp;

import javaclient3.structures.*;

/**
 * Request/reply : Get/set audio properties.
 * Send a null PLAYER_AUDIODSP_GET_CONFIG request to receive the audiodsp
 * configuration.  Send a full PLAYER_AUDIODSP_SET_CONFIG request to modify
 * the configuration (and receive a null response).
 * The sample format is defined in sys/soundcard.h, and defines the byte
 * size and endian format for each sample.
 * The sample rate defines the Hertz at which to sample.
 * Mono or stereo sampling is defined in the channels parameter where
 * 1==mono and 2==stereo.
 * @author Radu Bogdan Rusu
 * @version
 * <ul>
 *      <li>v3.0 - Player 3.0 supported
 * </ul>
 */
public class PlayerAudiodspConfig implements PlayerConstants {

    private int format;

    private float frequency;

    private int channels;

    /**
     * @return  Format with which to sample
     */
    public synchronized int getFormat() {
        return this.format;
    }

    /**
     * @param newFormat  Format with which to sample
     */
    public synchronized void setFormat(int newFormat) {
        this.format = newFormat;
    }

    /**
     * @return  Sample rate [Hz]
     */
    public synchronized float getFrequency() {
        return this.frequency;
    }

    /**
     * @param newFrequency  Sample rate [Hz]
     */
    public synchronized void setFrequency(float newFrequency) {
        this.frequency = newFrequency;
    }

    /**
     * @return  Number of channels to use. 1=mono, 2=stereo
     */
    public synchronized int getChannels() {
        return this.channels;
    }

    /**
     * @param newChannels  Number of channels to use. 1=mono, 2=stereo
     */
    public synchronized void setChannels(int newChannels) {
        this.channels = newChannels;
    }
}

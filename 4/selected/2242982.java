package sdljava.mixer;

/**
 * Encapsulates the return information from the Mix_QuerySpec function call.
 *
 * @author Ivan Z. Ganza
 * @version $Id: MixerSpec.java,v 1.2 2004/12/24 17:32:16 ivan_ganza Exp $
 */
public class MixerSpec {

    /**
     * the frequency actually used by the opened audio device
     *
     */
    int frequency;

    /**
     * the output format actually being used by the audio device
     *
     */
    int format;

    /**
     * A pointer to an int where the number of audio channels will be stored.
     * 2 will mean stereo, 1 will mean mono.
     *
     */
    int channels;

    /**
     * Creates a new <code>MixerSpec</code> instance.
     *
     * @param frequency an <code>int</code> value
     * @param format an <code>int</code> value
     * @param channels an <code>int</code> value
     */
    public MixerSpec(int frequency, int format, int channels) {
        this.frequency = frequency;
        this.format = format;
        this.channels = channels;
    }

    /**
     * Gets the value of frequency
     *
     * @return the value of frequency
     */
    public int getFrequency() {
        return this.frequency;
    }

    /**
     * Sets the value of frequency
     *
     * @param argFrequency Value to assign to this.frequency
     */
    public void setFrequency(int argFrequency) {
        this.frequency = argFrequency;
    }

    /**
     * Gets the value of format
     *
     * @return the value of format
     */
    public int getFormat() {
        return this.format;
    }

    /**
     * Sets the value of format
     *
     * @param argFormat Value to assign to this.format
     */
    public void setFormat(int argFormat) {
        this.format = argFormat;
    }

    /**
     * Gets the value of channels
     *
     * @return the value of channels
     */
    public int getChannels() {
        return this.channels;
    }

    /**
     * Sets the value of channels
     *
     * @param argChannels Value to assign to this.channels
     */
    public void setChannels(int argChannels) {
        this.channels = argChannels;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("MixerSpec[frequency=").append(frequency).append(", format=").append(format).append(", channels=").append(channels).append("]");
        return buf.toString();
    }
}

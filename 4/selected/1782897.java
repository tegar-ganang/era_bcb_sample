package javaclient3.structures.audio;

import javaclient3.structures.*;

/**
 * Player mixer channels (PLAYER_AUDIO_DATA_MIXER_CHANNEL).
 * Describes the state of a set of mixer channels.
 * @author Jorge Santos Simon
 * @version
 * <ul>
 *      <li>v3.0 - Player 3.0 supported
 * </ul>
 */
public class PlayerAudioMixerChannelList implements PlayerConstants {

    /** The channels list. */
    private PlayerAudioMixerChannel[] channels;

    /**
     * @return  Number of channels.
     */
    public synchronized int getChannels_count() {
        return (this.channels == null) ? 0 : channels.length;
    }

    /**
     * @return  The channels list.
     */
    public synchronized PlayerAudioMixerChannel[] getChannels() {
        return this.channels;
    }

    /**
     * @param newChannels  The channels list.
     */
    public synchronized void setChannels(PlayerAudioMixerChannel[] newChannels) {
        this.channels = newChannels;
    }
}

package jorgan.play;

import java.util.ArrayList;
import javax.sound.midi.InvalidMidiDataException;
import jorgan.disposition.Sound;
import jorgan.play.sound.Channel;
import jorgan.play.sound.ChannelFilter;

/**
 * A player of {@link jorgan.disposition.Sound} subclasses.
 */
public abstract class SoundPlayer<E extends Sound> extends Player<E> {

    /**
	 * Created channels.
	 */
    private ArrayList<ChannelImpl> channels = new ArrayList<ChannelImpl>();

    protected SoundPlayer(E sound) {
        super(sound);
    }

    protected int getChannelCount() {
        return 16;
    }

    /**
	 * Create a channel.
	 * 
	 * @return created channel or <code>null</code> if no channel is available
	 */
    public Channel createChannel(ChannelFilter filter) {
        for (int c = 0; c < getChannelCount(); c++) {
            if (filter.accept(c)) {
                while (channels.size() <= c) {
                    channels.add(null);
                }
                if (channels.get(c) == null) {
                    return new ChannelImpl(c);
                }
            }
        }
        return null;
    }

    protected abstract void send(int channel, byte[] datas) throws InvalidMidiDataException;

    /**
	 * A channel implementation.
	 */
    private class ChannelImpl implements Channel {

        /**
		 * The MIDI channel of this sound.
		 */
        private int channel;

        /**
		 * Create a channel.
		 * 
		 * @param channel
		 *            the channel to use
		 */
        public ChannelImpl(int channel) {
            this.channel = channel;
            channels.set(channel, this);
        }

        public void init() {
        }

        /**
		 * Release.
		 */
        public void release() {
            channels.set(channel, null);
        }

        /**
		 * Send a message.
		 * 
		 * @param message
		 *            message
		 * @throws InvalidMidiDataException
		 */
        public void sendMessage(byte[] datas) throws InvalidMidiDataException {
            send(channel, datas);
        }
    }
}

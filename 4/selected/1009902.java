package org.jjazz.midi.device;

import java.util.*;
import javax.sound.midi.*;

/**
 * This MidiDevice dispatches incoming MidiMessages on different transmitters
 * depending on the midi channel of the message.
 */
public class MidiChannelDispatcher extends JJazzMidiDevice {

    /** 16 midi channels (0-15) + 1 for messages like sysex, metamessages, etc. */
    private static final int LAST_CHANNEL = 16;

    /** The transmitters per channel. */
    private ArrayList<Transmitter>[] channelTransmitters = new ArrayList[LAST_CHANNEL + 1];

    public MidiChannelDispatcher() {
        super("MidiChannelDispatcher");
        for (int i = 0; i < channelTransmitters.length; i++) {
            channelTransmitters[i] = new ArrayList<Transmitter>();
        }
    }

    /**
     * Return a transmitter for channel 0.
     */
    @Override
    public Transmitter getTransmitter() {
        return getTransmitter(0);
    }

    /**
     * Return a transmitter for a specific channel only.
     * @param channel The channel associated to this transmitter. 0 <= channel <= 16.
     * Channel 16 is used to transmit MidiMessages not bound to a channel (SysExMessage, MetaMessage...).
     */
    public Transmitter getTransmitter(int channel) {
        if ((channel < 0) || (channel > 16)) throw new IllegalArgumentException("channel=" + channel);
        Transmitter mt = super.getTransmitter();
        channelTransmitters[channel].add(mt);
        return mt;
    }

    public List<Transmitter> getTransmitters(int channel) {
        if ((channel < 0) || (channel > 16)) throw new IllegalArgumentException("channel=" + channel);
        return channelTransmitters[channel];
    }

    @Override
    public Receiver getReceiver() {
        Receiver r = new ChannelReceiver();
        receivers.add(r);
        open();
        return r;
    }

    private class ChannelReceiver implements Receiver {

        boolean isReceiverOpen = true;

        /**
         * Operation called each time a MidiMessage arrives.
         * Dispatch the message on transmitters associated to the message channel.
         */
        @Override
        public void send(MidiMessage msg, long timeStamp) {
            int msgChannel = LAST_CHANNEL;
            if (!isOpen || !isReceiverOpen) {
                throw new IllegalStateException("ChannelDispatcher object is closed");
            }
            if (msg instanceof ShortMessage) msgChannel = ((ShortMessage) msg).getChannel();
            for (Transmitter t : channelTransmitters[msgChannel]) {
                Receiver rcv = t.getReceiver();
                if (rcv != null) rcv.send(msg, timeStamp);
            }
        }

        @Override
        public void close() {
            isReceiverOpen = false;
        }
    }
}

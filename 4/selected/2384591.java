package com.frinika.sequencer.midi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import com.frinika.sequencer.MidiResource;
import com.frinika.sequencer.gui.mixer.MidiDeviceMixerPanel;
import com.frinika.sequencer.model.ControllerListProvider;

/**
 * Wrapper for external midi out devices
 * 
 * @author Peter Johan Salomonsen
 */
public class MidiOutDeviceWrapper implements MidiDevice, MidiListProvider, Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    transient ControllerListProvider controllerList;

    transient MidiDevice midiDevice;

    transient MidiDeviceMixerPanel gui;

    transient Receiver receiver = new Receiver() {

        public void send(MidiMessage message, long timeStamp) {
            try {
                midiDevice.getReceiver().send(message, timeStamp);
                if (message instanceof ShortMessage) {
                    ShortMessage shm = (ShortMessage) message;
                    int channel = shm.getChannel();
                    if (shm.getCommand() == ShortMessage.NOTE_ON) {
                    } else if (shm.getCommand() == ShortMessage.CONTROL_CHANGE) {
                        if (gui != null && shm.getData1() == 7) gui.mixerSlots[channel].setVolume(shm.getData2()); else if (gui != null && shm.getData1() == 10) gui.mixerSlots[channel].setPan(shm.getData2());
                    } else if (shm.getCommand() == ShortMessage.PITCH_BEND) {
                    }
                }
            } catch (Exception e) {
            }
        }

        public void close() {
            try {
                midiDevice.getReceiver().close();
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        }
    };

    public MidiOutDeviceWrapper(MidiDevice midiDevice) {
        this.midiDevice = midiDevice;
        controllerList = MidiResource.getDefaultControllerList();
    }

    public Receiver getReceiver() throws MidiUnavailableException {
        return receiver;
    }

    public List<Receiver> getReceivers() {
        List<Receiver> receivers = new ArrayList<Receiver>();
        receivers.add(receiver);
        for (Receiver recv : midiDevice.getReceivers()) {
            try {
                if (recv != midiDevice.getReceiver()) receivers.add(recv);
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        }
        return receivers;
    }

    public Transmitter getTransmitter() throws MidiUnavailableException {
        return midiDevice.getTransmitter();
    }

    public List<Transmitter> getTransmitters() {
        return midiDevice.getTransmitters();
    }

    public Info getDeviceInfo() {
        return midiDevice.getDeviceInfo();
    }

    public void open() throws MidiUnavailableException {
        midiDevice.open();
    }

    public void close() {
        midiDevice.close();
    }

    public boolean isOpen() {
        return midiDevice.isOpen();
    }

    public long getMicrosecondPosition() {
        return midiDevice.getMicrosecondPosition();
    }

    public int getMaxReceivers() {
        return midiDevice.getMaxReceivers();
    }

    public int getMaxTransmitters() {
        return midiDevice.getMaxTransmitters();
    }

    public ControllerListProvider getControllerList() {
        return controllerList;
    }
}

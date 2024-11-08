package com.frinika.audio.toot.gui;

import java.util.HashMap;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import uk.org.toot.swingui.controlui.ControlPanel;
import com.frinika.audio.toot.MidiHashUtil;

public class MidiControlRouter implements Receiver {

    static final int INIT = 1;

    static final int LEARNING = 1;

    static final int ACTIVE = 2;

    int mode = INIT;

    HashMap<Long, ControlMapper> map;

    ShortMessage lastMessage = null;

    private ControlPanel focus;

    private MidiDevice dev;

    MidiControlRouter(MidiDevice dev) {
        this.dev = dev;
        map = new HashMap<Long, ControlMapper>();
    }

    public void close() {
    }

    public void setLearning(ControlPanel focus) {
        this.focus = focus;
        mode = LEARNING;
        System.out.println(" Learning to control using " + dev.getDeviceInfo().getName());
    }

    public void setActive() {
        mode = ACTIVE;
        if (lastMessage == null) {
            System.out.println(" Control not learnt yet !!! ");
        }
    }

    public void send(MidiMessage mess, long arg1) {
        if (mess.getStatus() >= ShortMessage.MIDI_TIME_CODE) return;
        if (!(mess instanceof ShortMessage)) return;
        ShortMessage smsg = (ShortMessage) mess;
        switch(mode) {
            case LEARNING:
                System.out.println("ch cmd data1 data2: " + smsg.getChannel() + " " + smsg.getCommand() + " " + smsg.getData1() + " " + smsg.getData2());
                lastMessage = smsg;
                break;
            case ACTIVE:
                long key = MidiHashUtil.hashValue((ShortMessage) mess);
                ControlMapper mapper = map.get(key);
                if (mapper == null) return;
                mapper.send(smsg, arg1);
                break;
        }
    }

    public void assignMapper() {
        if (lastMessage == null || focus == null) return;
        long hash = MidiHashUtil.hashValue(lastMessage);
        ControlMapper mapper = map.get(hash);
        if (mapper == null) {
            System.out.println(" Creating new mapper ");
            map.put(hash, new ControlMapper(focus.getControl(), lastMessage));
        } else {
            System.out.println(" Reassigning existing ");
        }
        mode = ACTIVE;
    }
}

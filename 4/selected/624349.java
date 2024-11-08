package com.frinika.tootX.midi;

import java.util.HashMap;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import com.frinika.tootX.midi.MidiFilter;
import uk.org.toot.control.Control;

/**
 * 
 * Implements a MidiFilter. A Controls are associated
 * with events using a hastable.
 * 
 * Usage:
 * 
 *   MidiEventRouter router = midiDeviceRouter.getROuter(dev.getDeviceInfo());
 *   router.setLearning(control);
 *   // use midi controller 
 * 
 * 
 * See for example MidiLearnFrame.
 * 
 * @author pjl
 */
public class MidiEventRouter implements MidiFilter {

    boolean learning;

    HashMap<Long, ControlMapper> map;

    HashMap<Control, Long> controlToHash;

    transient ShortMessage lastMessage = null;

    private Control focus;

    /**
     *
     */
    MidiEventRouter() {
        map = new HashMap<Long, ControlMapper>();
        controlToHash = new HashMap<Control, Long>();
        learning = false;
    }

    public void close() {
    }

    /**
     * set mode to learning 
     * listen to midi event 
     * You then  call assignMapper()
     * 
     * @param focus  (control to be manipulated)
     */
    public void setLearning(Control focus) {
        this.focus = focus;
        learning = true;
    }

    /**
     * implements MidiFilter
     * 
     * @param mess   midimessage
     * @param stamp  time stamp
     * @return  true if the event was found in the map and used
     */
    public boolean consume(MidiMessage mess, long stamp) {
        if (mess.getStatus() >= ShortMessage.MIDI_TIME_CODE) {
            return true;
        }
        if (!(mess instanceof ShortMessage)) {
            return true;
        }
        ShortMessage smsg = (ShortMessage) mess;
        if (learning) {
            System.out.println("LEARNING: ch cmd data1 data2: " + smsg.getChannel() + " " + smsg.getCommand() + " " + smsg.getData1() + " " + smsg.getData2());
            if (smsg.getCommand() == ShortMessage.NOTE_OFF) {
                return true;
            }
            lastMessage = smsg;
            return true;
        } else {
            long key = MidiHashUtil.hashValue((ShortMessage) mess);
            ControlMapper mapper = map.get(key);
            if (mapper == null) {
                return false;
            }
            mapper.send(smsg, stamp);
            return true;
        }
    }

    /**
     * called when last message was the type you want to do the control.
     * 
     */
    public void assignMapper() {
        if (lastMessage == null || focus == null) {
            return;
        }
        long newHash = MidiHashUtil.hashValue(lastMessage);
        Long lastHash = controlToHash.get(focus);
        if (lastHash != null) {
            map.remove(lastHash);
        }
        map.remove(newHash);
        map.put(newHash, new ControlMapper(focus, lastMessage));
        controlToHash.put(focus, newHash);
        learning = false;
    }

    public void assignMapping(Long midiHash, Control contrl) {
        map.put(midiHash, new ControlMapper(contrl, MidiHashUtil.reconstructShortMessage(midiHash, null)));
    }
}

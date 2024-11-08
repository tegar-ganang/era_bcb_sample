package ren.midi;

import java.io.Serializable;
import ren.gui.midi.ActuatorCommander;
import ren.gui.midi.ActuatorEvent;
import ren.util.RMath;
import ren.util.Save;

/**
 * @author wooller
 *
 *18/01/2005
 *
 * Copyright JEDI/Rene Wooller
 *
 */
public class MidiInputLocation extends ActuatorCommander implements MidiLocation, Serializable {

    private int channel, ctrlType;

    public static final String ssn = "mil";

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = RMath.boundHard(channel, 0, 15);
    }

    public int getCtrlType() {
        return ctrlType;
    }

    public void setCtrlType(int ctrlType) {
        this.ctrlType = RMath.boundHard(ctrlType, 0, 127);
    }

    private String name;

    /**
	 * 
	 */
    public MidiInputLocation() {
        super();
    }

    public MidiInputLocation(String name) {
        super(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        if (name == null) return new String(channel + " : " + ctrlType);
        return "ch " + channel + " | " + " ctrl " + ctrlType + " : " + name;
    }

    public void midiCtrlRecieved(int channel, int type, int value, int time) {
        this.fireActuators(new ActuatorEvent(0, 127, value, time));
    }

    public void setMidiController(int chan, int ctrlType) {
        this.setChannel(chan);
        this.setCtrlType(ctrlType);
    }

    /**
	 * when being loaded, the channel and type needs
	 * to be extracted first, and then
	 */
    public static int[] readChanType(String str) {
        int[] ret = new int[2];
        ret[0] = Save.getIntXML(str, "c");
        ret[1] = Save.getIntXML(str, "t");
        return ret;
    }

    public void loadString(String s) {
        super.loadString(s);
    }

    public String saveString() {
        StringBuffer s = new StringBuffer(Save.st(ssn) + "<c>" + this.getChannel() + "</c>" + "<t>" + this.getCtrlType() + "</t>");
        s.append(super.saveString());
        s.append(Save.et(ssn));
        return s.toString();
    }
}

package ren.midi;

import java.io.Serializable;
import ren.gui.midi.Actuator;
import ren.gui.midi.ActuatorEvent;
import ren.util.RMath;
import ren.util.Save;

public class MidiOutputLocation implements Actuator, MidiLocation, Serializable {

    public static final String ssn = "mol";

    private String name = "unnamedloc";

    private MidiOutputManager mom;

    private int channel, ctrlType;

    public MidiOutputLocation() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMidiOutputManager(MidiOutputManager mom) {
        this.mom = mom;
    }

    public void setMidiController(int chan, int ctrlType) {
        this.setChannel(chan);
        this.setCtrlType(ctrlType);
    }

    public void commandRecieved(ActuatorEvent e) {
        mom.getSequencer().sendControllerData(channel, ctrlType, RMath.scaleValueInt(e.getMin(), e.getMax(), this.CTRL_MIN, this.CTRL_MAX, e.getValue()), 0);
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = RMath.boundHard(0, 15, channel);
    }

    public int getCtrlType() {
        return ctrlType;
    }

    public void setCtrlType(int ctrlType) {
        this.ctrlType = RMath.boundHard(0, 127, ctrlType);
    }

    public String toString() {
        return channel + " : " + ctrlType + " : " + name;
    }

    public String getName() {
        return this.name;
    }

    private static final int CTRL_MIN = 0;

    private static final int CTRL_MAX = 127;

    private static final int PITCH_BEND_MIN = 0;

    private static final int PITCH_BEND_MAX = 255;

    public void loadString(String s) {
        this.setChannel(Save.getIntXML(s, "c"));
        this.setCtrlType(Save.getIntXML(s, "t"));
        this.setName(Save.getStringXML(s, "name"));
    }

    public String saveString() {
        StringBuffer sb = new StringBuffer(Save.st(this.ssn));
        sb.append(Save.st("c"));
        sb.append(Integer.toString(this.getChannel()));
        sb.append(Save.et("c"));
        sb.append(Save.st("t"));
        sb.append(Integer.toString(this.getCtrlType()));
        sb.append(Save.et("t"));
        sb.append(Save.st("name"));
        sb.append(this.getName());
        sb.append(Save.et("name"));
        sb.append(Save.et(this.ssn));
        return sb.toString();
    }
}

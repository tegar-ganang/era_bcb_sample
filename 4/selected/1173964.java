package ren.midi;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;
import jm.midi.MidiInputListener;
import jm.midi.event.CChange;
import jm.midi.event.Event;
import ren.gui.midi.Actuator;
import ren.gui.midi.ActuatorContainer;
import ren.util.PO;
import ren.util.Save;

/**
 * @author wooller
 * 
 * 17/01/2005
 * 
 * Copyright JEDI/Rene Wooller
 *  
 */
public class MidiProcessor implements MidiInputListener, TickListener, ActuatorContainer, Serializable {

    private static int num = 0;

    private Sequencer seq;

    private Vector milVect = new Vector(160);

    private MidiInputLocation[][] milArr = new MidiInputLocation[16][128];

    private Vector processes = new Vector(160);

    private MidiClock midiPlayer;

    /**
	 *  
	 */
    public MidiProcessor() {
        super();
        num++;
        PO.p("num of midi processors = " + num);
    }

    public MidiInputLocation createMidiInputLocation() {
        MidiInputLocation mill = null;
        int i, j = 0;
        for (i = 0; i < 16; i++) {
            for (j = 0; j < 128; j++) {
                if (milArr[i][j] == null) {
                    mill = addMidiInputLocation(i, j);
                    break;
                }
            }
            if (mill != null) {
                break;
            }
        }
        return mill;
    }

    public void addMidiInputLocation(MidiInputLocation mil) {
        resolveCollision(mil);
        this.milVect.add(mil);
    }

    public MidiInputLocation addMidiInputLocation(int chan, int ctrlType) {
        MidiInputLocation mil = new MidiInputLocation();
        mil.setMidiController(chan, ctrlType);
        this.addMidiInputLocation(mil);
        return mil;
    }

    public void removeMidiInputLocation(MidiInputLocation mil) {
        this.milVect.remove(mil);
        milArr[mil.getChannel()][mil.getCtrlType()] = null;
    }

    public void removeAllMils() {
        while (this.milVect.size() > 0) {
            removeMidiInputLocation((MidiInputLocation) milVect.get(milVect.size() - 1));
        }
    }

    public void removeAllProcs() {
        this.processes.removeAllElements();
    }

    public void setMidiInputLocation(int chanFrom, int ctrlFrom, int chanTo, int ctrlTo) {
        if (chanFrom == chanTo && ctrlFrom == ctrlTo) return;
        MidiInputLocation mi = milArr[chanFrom][ctrlFrom];
        milArr[chanFrom][ctrlFrom] = null;
        if (mi == null) {
            PO.p("problem, mi is null from = " + chanFrom + " ctrlFrom " + ctrlFrom + " chanTo = " + chanTo + " ctrlTo " + ctrlTo);
        }
        mi.setMidiController(chanTo, ctrlTo);
        milArr[chanTo][ctrlTo] = mi;
    }

    public Vector getMidiInputLocations() {
        return this.milVect;
    }

    public void setMidiInputLocations(Vector v) {
        this.milVect = v;
        Enumeration enumr = milVect.elements();
        while (enumr.hasMoreElements()) {
            resolveCollision((MidiInputLocation) enumr.nextElement());
        }
    }

    /**
	 * resolves any collision by removing the occupying mil, and then moving the
	 * new mil into it merge may be implemented in the future
	 * 
	 * @param mil
	 */
    private void resolveCollision(MidiInputLocation mil) {
        if (milArr[mil.getChannel()][mil.getCtrlType()] != null) {
            this.milVect.remove(milArr[mil.getChannel()][mil.getCtrlType()]);
        }
        milArr[mil.getChannel()][mil.getCtrlType()] = mil;
    }

    public void setSequencer(Sequencer seq) {
        this.seq = seq;
    }

    public Sequencer getSequencer() {
        return this.seq;
    }

    private CChange tcc;

    public void newEvent(Event event) {
        if (event instanceof CChange) {
            tcc = (CChange) event;
            if (!isLearning()) {
                if (milArr[tcc.getMidiChannel()][tcc.getControllerNum()] != null) {
                    milArr[tcc.getMidiChannel()][tcc.getControllerNum()].midiCtrlRecieved(tcc.getMidiChannel(), tcc.getControllerNum(), tcc.getValue(), tcc.getTime());
                }
            } else {
                if (learnLoc instanceof MidiInputLocation) {
                    this.setMidiInputLocation(learnLoc.getChannel(), learnLoc.getCtrlType(), tcc.getMidiChannel(), tcc.getControllerNum());
                } else if (learnLoc instanceof MidiOutputLocation) {
                    learnLoc.setMidiController(tcc.getMidiChannel(), tcc.getControllerNum());
                }
            }
        }
    }

    private MidiLocation learnLoc = null;

    public void setLearning(MidiLocation ll) {
        learnLoc = ll;
    }

    public boolean isLearning() {
        return (learnLoc != null);
    }

    public void tick(TickEvent e) {
        for (int i = 0; i < processes.size(); i++) {
            ((Process) processes.get(i)).process(midiPlayer.getTime());
        }
    }

    private static final Process[] procArr = new Process[0];

    public Process[] getProcessArray() {
        return (Process[]) processes.toArray(procArr);
    }

    public Vector getProcessVector() {
        return processes;
    }

    public void setProcessVector(Vector to) {
        this.processes = to;
    }

    public ActuatorContainer[] getSubActuatorContainers() {
        return getProcessArray();
    }

    public Actuator[] getActuators() {
        return null;
    }

    public int getActuatorContainerCount() {
        return processes.size();
    }

    public int getIndexOfSubContainer(ActuatorContainer sub) {
        return processes.indexOf(sub);
    }

    private String name = "MIDI Processor";

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return getName();
    }

    public void addProcess(Process toAdd) {
        this.processes.add(toAdd);
    }

    public void removeProcess(Process toRemove) {
        this.processes.remove(toRemove);
    }

    public String saveString() {
        StringBuffer sb = new StringBuffer();
        sb.append(Save.st(ssn));
        sb.append("<milocs>");
        for (int i = 0; i < milVect.size(); i++) {
            sb.append(((MidiInputLocation) milVect.get(i)).saveString());
        }
        sb.append("</milocs>");
        sb.append(Save.st("procs"));
        for (int i = 0; i < processes.size(); i++) {
            sb.append(((Process) processes.get(i)).saveString());
        }
        sb.append(Save.et("procs"));
        sb.append(Save.et(ssn));
        return sb.toString();
    }

    public void loadString(String toLoad) {
        this.removeAllMils();
        this.removeAllProcs();
        int[] loc = Save.getSubPosXML(toLoad, ssn);
        toLoad = toLoad.substring(loc[0], loc[1]);
        String str = Save.getStringXML(toLoad, "procs");
        loc = Save.getSubPosXML(str, Process.ssn);
        while (loc[0] != -1) {
            String ts = str.substring(loc[0], loc[1]);
            Process p = ProcessFactory.create(Save.getStringXML(ts, "name"));
            p.loadString(ts);
            this.addProcess(p);
            str = str.substring(loc[1]);
            loc = Save.getSubPosXML(str, Process.ssn);
        }
        str = Save.getStringXML(toLoad, "milocs");
        loc = Save.getSubPosXML(str, MidiInputLocation.ssn);
        while (loc[0] != -1) {
            String ts = str.substring(loc[0], loc[1]);
            int[] var = MidiInputLocation.readChanType(ts);
            this.addMidiInputLocation(var[0], var[1]).loadString(ts);
            str = str.substring(loc[1]);
            loc = Save.getSubPosXML(str, MidiInputLocation.ssn);
        }
    }

    public MidiClock getMidiPlayer() {
        return midiPlayer;
    }

    public void setMidiPlayer(MidiClock midiPlayer) {
        this.midiPlayer = midiPlayer;
    }

    public static final String ssn = "mproc";
}

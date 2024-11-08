package mtn.sevenuplive.max.mxj;

import mtn.sevenuplive.m4l.M4LController;
import mtn.sevenuplive.m4l.M4LMidiOut;
import mtn.sevenuplive.m4l.Note;
import com.cycling74.max.Atom;

public abstract class SevenUp4LiveMidiClient implements M4LMidiOut {

    private int instanceNum;

    private int channel;

    private SevenUp4Live app;

    public SevenUp4LiveMidiClient(SevenUp4Live app, int instanceNum, int ch) {
        this.instanceNum = instanceNum;
        this.app = app;
        this.channel = ch;
    }

    /**
	 * @return the ordinal of our midi outlet
	 */
    protected abstract int getOutletOrdinal();

    public void sendController(M4LController controller) {
        app.outlet(getOutletOrdinal(), new Atom[] { Atom.newAtom(instanceNum), Atom.newAtom(channel + 1), Atom.newAtom(M4LMidiOut.CC), Atom.newAtom(controller.getCC()), Atom.newAtom(controller.getValue()) });
    }

    public void sendNoteOff(Note note) {
        app.outlet(getOutletOrdinal(), new Atom[] { Atom.newAtom(instanceNum), Atom.newAtom(channel + 1), Atom.newAtom(M4LMidiOut.NOTE), Atom.newAtom(144 + channel), Atom.newAtom(note.getPitch()), Atom.newAtom(0) });
    }

    public void sendNoteOn(Note note) {
        app.outlet(getOutletOrdinal(), new Atom[] { Atom.newAtom(instanceNum), Atom.newAtom(channel + 1), Atom.newAtom(M4LMidiOut.NOTE), Atom.newAtom(144 + channel), Atom.newAtom(note.getPitch()), Atom.newAtom(note.getVelocity()) });
    }

    public int getInstanceNum() {
        return instanceNum;
    }

    public void setInstanceNum(int instanceNum) {
        this.instanceNum = instanceNum;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public SevenUp4Live getApp() {
        return app;
    }

    public void setApp(SevenUp4Live app) {
        this.app = app;
    }
}

package blue.ui.core.midi;

import blue.BlueData;
import blue.InstrumentAssignment;
import blue.midi.MidiInputProcessor;
import blue.projects.BlueProject;
import blue.projects.BlueProjectManager;
import blue.ui.core.blueLive.BlueLiveToolBar;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

/**
 *
 * @author syi
 */
public final class MidiInputEngine implements Receiver {

    private static MidiInputEngine instance = new MidiInputEngine();

    private static BlueLiveToolBar toolbar;

    private ArrayList<InstrumentAssignment> arrangement;

    private MidiInputProcessor processor = null;

    public static MidiInputEngine getInstance() {
        if (toolbar == null) {
            toolbar = BlueLiveToolBar.getInstance();
        }
        return instance;
    }

    private MidiInputEngine() {
        BlueProjectManager.getInstance().addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (BlueProjectManager.CURRENT_PROJECT.equals(evt.getPropertyName())) {
                    reinitialize();
                }
            }
        });
    }

    protected void reinitialize() {
        BlueProject project = BlueProjectManager.getInstance().getCurrentProject();
        BlueData data = null;
        if (project != null) {
            data = project.getData();
            arrangement = data.getArrangement().getArrangement();
            processor = data.getMidiInputProcessor();
        }
    }

    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage) {
            ShortMessage shortMsg = (ShortMessage) message;
            int channel = shortMsg.getChannel();
            int noteNum = shortMsg.getData1();
            int velocity = shortMsg.getData2();
            if (processor == null || arrangement == null || channel >= arrangement.size()) {
                return;
            }
            String id = arrangement.get(channel).arrangementId;
            String score = "i";
            switch(shortMsg.getCommand()) {
                case ShortMessage.NOTE_ON:
                    if (velocity > 0) {
                        score = processor.getNoteOn(id, noteNum, noteNum, velocity);
                    } else {
                        score = processor.getNoteOff(id, noteNum);
                    }
                    break;
                case ShortMessage.NOTE_OFF:
                    score = processor.getNoteOff(id, noteNum);
                    break;
            }
            System.err.println(score);
            toolbar.sendEvents(score);
        }
    }

    public void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

package com.frinika.simphoney;

import com.frinika.project.RecordingManager;
import com.frinika.project.gui.ProjectFrame;
import com.frinika.sequencer.FrinikaTrackWrapper;
import com.frinika.sequencer.model.ControllerEvent;
import com.frinika.sequencer.model.Lane;
import com.frinika.sequencer.model.MidiLane;
import com.frinika.sequencer.model.MidiPart;
import com.frinika.sequencer.model.MultiEvent;
import com.frinika.sequencer.model.NoteEvent;
import com.frinika.sequencer.model.PitchBendEvent;
import javax.sound.midi.ShortMessage;
import uk.org.toot.control.BooleanControl;

/**
 *
 * @author pjl
 */
public class SimphoneyRecordManager extends RecordingManager {

    private ProjectFrame frame;

    BooleanControl loopMarker;

    private boolean createTakeRequest = false;

    public SimphoneyRecordManager(ProjectFrame frame) {
        super(frame.getProjectContainer(), 1000);
        this.frame = frame;
        loopMarker = new BooleanControl(0, "loopMarker", false, true) {

            @Override
            public void setValue(boolean flag) {
                System.out.println(" LOOP MARKER  " + flag);
                if (true) {
                    createTakeRequest = true;
                }
                notifyParent(this);
            }
        };
    }

    public BooleanControl getLoopMarkerControl() {
        return loopMarker;
    }

    /**
     * 
     * Do the processing on a low priority Tick notification thread
     *  
     * @param tick
     */
    @Override
    public void notifyTickPosition(long tick) {
        if (createTakeRequest) {
            createTake();
            createTakeRequest = false;
        }
        processEvents();
    }

    void processEvents() {
        Event e = null;
        while ((e = stack.pop()) != null) {
            ShortMessage shm = e.mess;
            long tick = e.stamp;
            if (shm.getCommand() == ShortMessage.NOTE_ON || shm.getCommand() == ShortMessage.NOTE_OFF) {
                if (shm.getCommand() == ShortMessage.NOTE_OFF || shm.getData2() == 0) {
                    NoteEvent noteEvent = pendingNoteEvents.get(shm.getChannel() << 8 | shm.getData1());
                    if (noteEvent != null) {
                        long duration = tick - noteEvent.getStartTick();
                        if (duration < 0) {
                            duration = duration + sequencer.getLoopEndPoint() - sequencer.getLoopStartPoint();
                        }
                        noteEvent.setDuration(duration);
                        pendingNoteEvents.remove(shm.getChannel() << 8 | shm.getData1());
                        addEventToRecordingTracks(noteEvent);
                    }
                } else {
                    pendingNoteEvents.put(shm.getChannel() << 8 | shm.getData1(), new NoteEvent((FrinikaTrackWrapper) null, tick, shm.getData1(), shm.getData2(), shm.getChannel(), 0));
                }
            } else if (shm.getCommand() == ShortMessage.CONTROL_CHANGE) {
                addEventToRecordingTracks(new ControllerEvent((FrinikaTrackWrapper) null, tick, shm.getData1(), shm.getData2()));
            } else if (shm.getCommand() == ShortMessage.PITCH_BEND) {
                addEventToRecordingTracks(new PitchBendEvent((FrinikaTrackWrapper) null, tick, ((shm.getData1()) | (shm.getData2() << 7)) & 0x7fff));
            }
        }
    }

    void createTake() {
        System.out.println(" CREATE A TAKE ");
        MidiLane ml = null;
        if (currentRecordingTake.size() == 0) {
            return;
        }
        for (Lane lane : project.getLanes()) {
            if (!(lane instanceof MidiLane)) {
                continue;
            }
            if (!((MidiLane) lane).isRecording()) continue;
            ml = (MidiLane) lane;
            break;
        }
        if (ml == null) {
            return;
        }
        project.getEditHistoryContainer().mark(" Recording take ");
        MidiPart part = new MidiPart(ml);
        for (MultiEvent event : currentRecordingTake) {
            try {
                part.add((MultiEvent) event.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        part.setBoundsFromEvents();
        project.getEditHistoryContainer().notifyEditHistoryListeners();
        currentRecordingTake.clear();
    }
}

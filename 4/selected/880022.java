package vivace.model;

import java.util.Vector;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import vivace.helper.ProjectHelper;

/**
 * Class implementing a simple metronome
 *
 */
public class Metronome implements MetaEventListener {

    private Project model;

    private TimeSignature timeSignature;

    private int beatCounter;

    /** 
	 * Enum defining the possible modes for the metronome 
	 * ALWAYS - metronome always on
	 * REC_PLAY - metronome on during recording/playback
	 * REC - metronome on during recording only
	 * OFF - metronom always of
	 */
    public static enum Mode {

        ALWAYS, REC_PLAY, REC, OFF
    }

    ;

    private static Mode mode;

    private MetronomeThread thread;

    private static final int ACCENTED_NOTE_NUMBER = 76;

    private static final int NORMAL_NOTE_NUMBER = 77;

    private static final int VELOCITY = 100;

    private boolean run;

    private long wentToSleepAt, timeToSleepOnStart, timeUntilNextBeat;

    /**
	 * Constructor for the metronome class
	 * @param model A project that the metronome will gather data from,
	 * such as tempo and resolution
	 */
    public Metronome(Project model) {
        this.model = model;
        this.model.getSequencer().addMetaEventListener(this);
        System.out.println(model.getSequencer());
        timeSignature = new TimeSignature(model.getPPQResolution());
        mode = Mode.REC;
        beatCounter = 0;
        wentToSleepAt = 0;
        timeToSleepOnStart = 0;
        timeUntilNextBeat = (long) (model.getSequencer().getTempoInMPQ() * 0.004 / timeSignature.getDenominator());
    }

    /**
	 * Starts the metronome
	 *
	 */
    public void start() {
        thread = new MetronomeThread();
        run = true;
        thread.start();
    }

    /**
	 * Stops the metronome
	 *
	 */
    public void stop() {
        run = false;
        timeToSleepOnStart = timeUntilNextBeat - (System.currentTimeMillis() - wentToSleepAt);
    }

    /**
	 * Stops the metronome and resets the synchronizing variables (used when stopping the sequnecer)
	 *
	 */
    public void stopAndReset() {
        if (thread != null) thread.interrupt();
        stop();
        beatCounter = 0;
        wentToSleepAt = 0;
        timeToSleepOnStart = 0;
    }

    /**
	 * Returns true if metronome is running, false otherwise
	 * @return
	 */
    public boolean isRunning() {
        return run;
    }

    /**
	 * Sets the mode of the metronome
	 * @param m
	 */
    public static void setMode(Mode m) {
        mode = m;
    }

    public void meta(MetaMessage m) {
        if (m.getType() == MetaMessageType.TRACK_TIMESIGNATURE) {
            byte[] data = m.getData();
            timeSignature.setNumerator(data[0]);
            timeSignature.setDenominator((int) Math.pow(2, data[1]));
            beatCounter = 0;
            timeUntilNextBeat = (long) (model.getSequencer().getTempoInMPQ() * 0.004 / timeSignature.getDenominator());
            thread.interrupt();
        }
    }

    private class MetronomeThread extends Thread {

        public void run() {
            MidiChannel channel = model.getSynthesizer().getChannels()[9];
            while (run) {
                try {
                    if (timeToSleepOnStart > 0) {
                        sleep(timeToSleepOnStart);
                        timeToSleepOnStart = 0;
                    }
                    if (mode == Mode.ALWAYS || mode == Mode.REC_PLAY && (model.getSequencer().isRecording() || model.getSequencer().isRunning()) || mode == Mode.REC && model.getSequencer().isRecording()) {
                        if (beatCounter == 0) {
                            channel.noteOn(ACCENTED_NOTE_NUMBER, VELOCITY);
                        } else {
                            channel.noteOn(NORMAL_NOTE_NUMBER, VELOCITY);
                        }
                    }
                    wentToSleepAt = System.currentTimeMillis();
                    timeUntilNextBeat = (long) (model.getSequencer().getTempoInMPQ() * 0.004 / timeSignature.getDenominator());
                    sleep(timeUntilNextBeat);
                    beatCounter = (beatCounter + 1) % timeSignature.getNumerator();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}

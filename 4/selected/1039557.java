package keyboardhero;

import java.io.*;
import java.net.URL;
import java.util.*;
import javax.sound.midi.*;
import keyboardhero.MidiSong.*;

@SuppressWarnings("unchecked")
final class MidiSequencer {

    /**
	 * Class for representing the key to be played
	 */
    static final class Key implements Comparable<Key> {

        static final int MAX_VELOCITY = 127;

        /** The key to be played */
        final int key;

        /** The pressure applied to the key */
        private int velocity;

        /** If the key is a higher note */
        final boolean higher;

        Key(int key, boolean higher, int velocity) {
            this.key = key;
            this.higher = higher;
            this.velocity = velocity;
        }

        Key(ShortMessage message) {
            this(message.getData1(), message.getData2());
        }

        Key(int midi, int velocity) {
            int octave = (midi / 12 * 7) - 35;
            this.velocity = velocity;
            switch(midi % 12) {
                case 0:
                    key = octave;
                    higher = false;
                    break;
                case 1:
                    key = octave;
                    higher = true;
                    break;
                case 2:
                    key = octave + 1;
                    higher = false;
                    break;
                case 3:
                    key = octave + 1;
                    higher = true;
                    break;
                case 4:
                    key = octave + 2;
                    higher = false;
                    break;
                case 5:
                    key = octave + 3;
                    higher = false;
                    break;
                case 6:
                    key = octave + 3;
                    higher = true;
                    break;
                case 7:
                    key = octave + 4;
                    higher = false;
                    break;
                case 8:
                    key = octave + 4;
                    higher = true;
                    break;
                case 9:
                    key = octave + 5;
                    higher = false;
                    break;
                case 10:
                    key = octave + 5;
                    higher = true;
                    break;
                default:
                    key = octave + 6;
                    higher = false;
                    break;
            }
        }

        static int toKey(int midi) {
            int octave = (midi / 12 * 7) - 35;
            switch(midi % 12) {
                case 0:
                    return octave;
                case 1:
                    return octave;
                case 2:
                    return octave + 1;
                case 3:
                    return octave + 1;
                case 4:
                    return octave + 2;
                case 5:
                    return octave + 3;
                case 6:
                    return octave + 3;
                case 7:
                    return octave + 4;
                case 8:
                    return octave + 4;
                case 9:
                    return octave + 5;
                case 10:
                    return octave + 5;
                default:
                    return octave + 6;
            }
        }

        final int getVelocity() {
            return velocity;
        }

        final void setVelocity(int velocity) {
            this.velocity = velocity;
        }

        final String toStr(int letters) {
            return toStr(letters, key, higher);
        }

        static String toStr(int letters, int key, boolean higher) {
            switch(letters) {
                case 1:
                    return toName(key, higher);
                case 2:
                    return toShorthand(key, higher);
                case 3:
                    return toNumbered(key, higher);
            }
            return "";
        }

        final String toName() {
            return toName(key, higher);
        }

        static String toName(int key, boolean higher) {
            if (higher) {
                return new String(new char[] { (char) ('A' + (key + 128) % 7), '#' });
            } else {
                return Character.toString((char) ('A' + (key + 128) % 7));
            }
        }

        final String toShorthand() {
            return toShorthand(key, higher);
        }

        static String toShorthand(int key, boolean higher) {
            StringBuffer buff = new StringBuffer();
            if (key < -7) {
                buff.append((char) ('A' + (key + 128) % 7));
                for (int i = -14; i > key; i -= 7) {
                    buff.append(',');
                }
            } else {
                buff.append((char) ('a' + (key + 128) % 7));
                for (int i = 0; i <= key; i += 7) {
                    buff.append('\'');
                }
            }
            if (higher) buff.append('#');
            return buff.toString();
        }

        final String toNumbered() {
            return toNumbered(key, higher);
        }

        static String toNumbered(int key, boolean higher) {
            StringBuffer buff = new StringBuffer();
            buff.append((char) ('A' + (key + 128) % 7));
            buff.append(Integer.toString(key / 7 + 4));
            if (higher) buff.append('#');
            return buff.toString();
        }

        public String toString() {
            return "Key: " + key + ", Velocity: " + velocity + ", Higher: " + higher;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                Key o = (Key) obj;
                return (key == o.key && higher == o.higher);
            } else {
                return false;
            }
        }

        public int compareTo(Key o) {
            if (key < o.key) {
                return -1;
            } else if (key == o.key) {
                if (higher) {
                    if (o.higher) {
                        return 0;
                    } else {
                        return 1;
                    }
                } else {
                    if (o.higher) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            } else {
                return 1;
            }
        }
    }

    /**
	 * Class for representing a note to be played
	 */
    static final class Note implements Comparable<Note> {

        /** The note to be played */
        final Key key;

        /** The length of the note */
        final long length;

        /** The time the note should be played */
        final long time;

        /** If the note has been checked by the algorithm */
        private boolean checked = false;

        /** The time in midi-ticks when the note has been checked by the algorithm */
        private long checkedTime;

        /** The score achieved for this note */
        private int score = 0;

        Note(Key key, long time, long length) {
            this.key = key;
            this.time = time;
            this.length = length;
        }

        final int getScore() {
            return score;
        }

        final void setScore(int score) {
            this.score = score;
        }

        final boolean isChecked() {
            return checked;
        }

        final void setChecked(boolean ifChecked) {
            this.checked = ifChecked;
        }

        final long getCheckedTime() {
            return checkedTime;
        }

        public int compareTo(Note note) {
            if (time < note.time) {
                return -1;
            } else if (time == note.time) {
                return 0;
            } else {
                return 1;
            }
        }

        public String toString() {
            return key + ", Length: " + length + ", Time:  " + time;
        }
    }

    private static final class PressedNote {

        float avgVelocity = 0;

        int lastVelocity;

        long startTime, lastTime;

        private PressedNote(long time, int velocity) {
            this.startTime = this.lastTime = time;
            this.lastVelocity = velocity;
        }

        private void alter(long time, int velocity) {
            avgVelocity = ((lastTime - startTime) * avgVelocity + (time - lastTime) * lastVelocity) / (time - startTime);
            lastTime = time;
            lastVelocity = velocity;
        }
    }

    static final int TICKS_PER_MINUTE_FOR_PPQ = 60000;

    private static final int FALLING_TIME_FOR_PPQ = Game.FALLING_TIME_IN_MILLISECONDS * TICKS_PER_MINUTE_FOR_PPQ / 60000;

    private static final int DELAY_ALLOWANCE_IN_MILLISECONDS = 1000;

    private static final int PAUSE_DELAY = 350;

    private static final MidiSequencer INSTANCE = new MidiSequencer();

    private static final MidiSong SONG = MidiSong.getInstance();

    private static byte pause = (byte) Util.getPropInt("gameDevPause");

    private Sequencer sequencer;

    private TreeMap<Integer, PressedNote[][]> deviceNoteOns = new TreeMap<Integer, PressedNote[][]>();

    private int delay;

    private Note[] notes;

    boolean waitingForController = false;

    boolean waitingForNote = false;

    private boolean isPPQ;

    private float ticksPerSecondBase;

    private TempoTickInfo tempoTickInfo = new TempoTickInfo();

    private int eventTickPosition;

    private long eventTickTime;

    private boolean pauseAllowed = true;

    private int pauseAllowedId = 0;

    /**
	 * Empty default constructor
	 */
    private MidiSequencer() {
        try {
            sequencer = MidiSystem.getSequencer(false);
        } catch (MidiUnavailableException e) {
            Util.error(Util.getMsg("Err_SequencerUnavailable"), e.getLocalizedMessage());
        }
    }

    static final MidiSequencer getInstance() {
        return INSTANCE;
    }

    static void closure() {
        Util.setProp("gameDevPause", pause);
        INSTANCE.sequencer.close();
    }

    /**
	 * Loads the song and converts the given track to an array on notes to be played
	 * 
	 * @param file
	 *            String holding the location of the song
	 * @param speed
	 *            floating point number indicating the tempo factor in which the song should be played.
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 * @throws MidiUnavailableException
	 * @see Game#newGame(URL, float)
	 */
    void loadSong(URL file, float speed) throws InvalidMidiDataException, IOException, MidiUnavailableException {
        SONG.loadMidiFile(file);
        if (sequencer == null) {
            sequencer = MidiSystem.getSequencer();
        }
        sequencer.setSequence(SONG.getMidiSequence());
        if (!sequencer.isOpen()) {
            sequencer.getTransmitter().setReceiver(MidiDevicer.SONG_RECEIVER);
            sequencer.open();
        }
        sequencer.setTempoFactor(speed);
    }

    final void closeSong() {
        sequencer.close();
        Game.setNoteSequence(Game.State.NULL_NOTES);
        Game.setSongInfo(Game.State.NULL_INFO);
        MidiDevicer.resetChannels();
    }

    final void startSong() {
        sequencer.start();
    }

    final void stopSong() {
        if (sequencer.isRunning()) sequencer.stop();
    }

    final long getTickPosition() {
        if (isPPQ) {
            tempoTickInfo = MidiSong.shiftTempoTickInfo(sequencer.getTickPosition(), tempoTickInfo);
            return tempoTickInfo.tick + (long) ((sequencer.getTickPosition() - tempoTickInfo.previous.getKey()) * tempoTickInfo.previous.getValue());
        } else {
            return sequencer.getTickPosition();
        }
    }

    final float getTicksPerMilliSecond() {
        if (isPPQ) {
            return TICKS_PER_MINUTE_FOR_PPQ * sequencer.getTempoFactor() / 60000;
        } else {
            return ticksPerSecondBase * sequencer.getTempoFactor() / 1000;
        }
    }

    final int getFallingTime() {
        if (isPPQ) {
            return FALLING_TIME_FOR_PPQ;
        } else {
            return (int) (Game.FALLING_TIME_IN_MILLISECONDS * ticksPerSecondBase / 1000);
        }
    }

    final int getLoopTime() {
        if (isPPQ) {
            return (int) (TICKS_PER_MINUTE_FOR_PPQ * Game.getSleepTime() * sequencer.getTempoFactor()) / 60000;
        } else {
            return (int) (ticksPerSecondBase * Game.getSleepTime() * sequencer.getTempoFactor()) / 1000;
        }
    }

    final void setPPQ(final boolean isPPQ) {
        this.isPPQ = isPPQ;
    }

    final void setTicksPerSecondBase(final float ticksPerSecondBase) {
        this.ticksPerSecondBase = ticksPerSecondBase;
    }

    final void setTempos(final TreeMap<Long, Float> tempos) {
        tempoTickInfo.tick = 0;
        tempoTickInfo.iterator = tempos.entrySet().iterator();
        if (isPPQ) {
            tempoTickInfo.next = tempoTickInfo.iterator.next();
            tempoTickInfo.previous = new AbstractMap.SimpleImmutableEntry<Long, Float>(0l, TICKS_PER_MINUTE_FOR_PPQ / (120 * ticksPerSecondBase));
        }
        eventTickPosition = 0;
        eventTickTime = -1;
        delay = (int) (getTicksPerMilliSecond() * DELAY_ALLOWANCE_IN_MILLISECONDS);
    }

    final boolean isRunning() {
        return sequencer.isRunning();
    }

    final void setNoteSequence(Note[] notes) {
        this.notes = notes;
    }

    final int getNoteNum() {
        return notes.length;
    }

    final boolean isOpen() {
        return sequencer.isOpen();
    }

    final long getLength() {
        return sequencer.getMicrosecondLength();
    }

    final long getPosition() {
        return sequencer.getMicrosecondPosition();
    }

    void messageReceived(MidiMessage message, int device) {
        if (Util.getDebugLevel() > 64) {
            ShortMessage sm = null;
            if (message instanceof ShortMessage) {
                sm = (ShortMessage) message;
            }
            if (sm == null || sm.getCommand() != 240 || Util.getDebugLevel() > 156) {
                Util.debug("***************************************");
                Util.debug("Byte Length: " + message.getLength());
                Util.debug("Status: " + message.getStatus());
                Util.debug("---------------------------------------");
                if (sm != null) {
                    Util.debug("Channel: " + sm.getChannel());
                    Util.debug("Command: " + sm.getCommand());
                    Util.debug("Data1: " + sm.getData1());
                    Util.debug("Data2: " + sm.getData2());
                }
                Util.debug("***************************************");
            }
        }
        if (message instanceof ShortMessage) {
            final ShortMessage shortMessage = (ShortMessage) message;
            switch(shortMessage.getCommand()) {
                case ShortMessage.CONTROL_CHANGE:
                    if (waitingForController) {
                        waitingForController = false;
                        DialogSettings.setController((byte) shortMessage.getData1());
                    } else {
                        if (waitingForNote) {
                            DialogSettings.setNote((byte) -1);
                        }
                        if (shortMessage.getData1() == pause) {
                            if (pauseAllowed) {
                                Game.togglePause();
                                pauseAllowed = false;
                                (new Thread() {

                                    public void run() {
                                        final int pauseId = ++pauseAllowedId;
                                        try {
                                            Thread.sleep(PAUSE_DELAY);
                                        } catch (InterruptedException e) {
                                            if (Util.getDebugLevel() > 90) e.printStackTrace();
                                        }
                                        if (pauseId == pauseAllowedId) pauseAllowed = true;
                                    }
                                }).start();
                            }
                        }
                    }
                    break;
                case ShortMessage.NOTE_ON:
                    if (waitingForNote) {
                        if (shortMessage.getData2() != 0) {
                            waitingForNote = false;
                            DialogSettings.setNote((byte) shortMessage.getData1());
                        }
                    } else if (waitingForController) {
                        DialogSettings.setController((byte) -1);
                    }
                    if (shortMessage.getChannel() != 9) checkMessage(shortMessage, device, true);
                    break;
                case ShortMessage.NOTE_OFF:
                    if (shortMessage.getChannel() != 9) checkMessage(shortMessage, device, false);
                    break;
                case ShortMessage.POLY_PRESSURE:
                    if (shortMessage.getChannel() != 9) {
                        alterMessage(shortMessage, device, true);
                        Game.activateKey(shortMessage, device);
                    }
                    break;
                case ShortMessage.CHANNEL_PRESSURE:
                    if (shortMessage.getChannel() != 9) {
                        alterMessage(shortMessage, device, false);
                        Game.reactivateKeys(device, shortMessage.getChannel(), shortMessage.getData1());
                    }
                    break;
            }
        }
    }

    void checkForOutNotes(long tickPosition) {
        if (eventTickTime == -1) {
            final long limit = tickPosition - delay;
            while (eventTickPosition < notes.length && notes[eventTickPosition].time < limit) {
                if (!notes[eventTickPosition].checked) {
                    notes[eventTickPosition].checked = true;
                }
                ++eventTickPosition;
            }
        }
        long limit = tickPosition - delay;
        for (int i = eventTickPosition; i < notes.length && notes[i].time < limit; ++i) {
            if (!notes[i].checked && notes[i].time + notes[i].length < limit) {
                notes[i].checked = true;
            }
        }
    }

    @SuppressWarnings("null")
    private void checkMessage(ShortMessage message, int device, boolean noteOn) {
        boolean noteOff = false;
        if (noteOn) {
            if (message.getData2() == 0) {
                noteOff = true;
            } else {
                Game.activateKey(message, device);
                if (Game.isGameActive()) {
                    PressedNote[][] noteOns = deviceNoteOns.get(device);
                    if (noteOns == null) {
                        noteOns = new PressedNote[16][128];
                        deviceNoteOns.put(device, noteOns);
                    }
                    noteOns[message.getChannel()][message.getData1()] = new PressedNote(getTickPosition(), message.getData2());
                }
            }
        } else {
            noteOff = true;
        }
        if (noteOff) {
            Game.deactivateKey(message, device);
            if (Game.isGameActive()) {
                PressedNote[][] noteOns = deviceNoteOns.get(device);
                if (noteOns == null) return;
                final int midiKey = message.getData1();
                final int channel = message.getChannel();
                final PressedNote onNote = noteOns[channel][midiKey];
                if (onNote != null) {
                    if (midiKey < Graphs.getFirstKeyInMidi() || midiKey > Graphs.getLastKeyInMidi()) {
                        noteOns[channel][midiKey] = null;
                        return;
                    }
                    if (onNote.startTime == eventTickTime || eventTickTime == -1) {
                        if (Util.getDebugLevel() > 30) Util.debug("updating eventTickTime");
                        eventTickTime = Long.MAX_VALUE;
                        for (byte i = 0; i < noteOns.length; ++i) {
                            if (noteOns[i] != null) for (int j = 0; j < noteOns[i].length; ++j) {
                                if (noteOns[i][j] != null && noteOns[i][j].startTime < eventTickTime) {
                                    eventTickTime = noteOns[i][j].startTime;
                                }
                            }
                        }
                        final long limit = eventTickTime - delay;
                        while (eventTickPosition < notes.length && notes[eventTickPosition].time < limit) {
                            if (!notes[eventTickPosition].checked) {
                                notes[eventTickPosition].checked = true;
                            }
                            ++eventTickPosition;
                        }
                        if (eventTickTime == onNote.startTime) eventTickTime = -1;
                    }
                    noteOns[channel][midiKey] = null;
                    onNote.alter(getTickPosition(), 0);
                    ArrayList<Note> foundNotes = new ArrayList<Note>();
                    long endTime;
                    final Key key = new Key(midiKey, 0);
                    for (int i = eventTickPosition; i < notes.length; ++i) {
                        if (notes[i].time - delay <= onNote.startTime) {
                            if (key.equals(notes[i].key) && !notes[i].checked && (onNote.startTime <= notes[i].time + delay) && ((endTime = notes[i].time + notes[i].length) - delay <= onNote.lastTime && onNote.lastTime <= endTime + delay)) {
                                foundNotes.add(notes[i]);
                            }
                        } else {
                            break;
                        }
                    }
                    if (foundNotes.size() == 0) {
                        Game.noteScored(key, -25);
                    } else {
                        int maxScore = Integer.MIN_VALUE;
                        Note selected = null;
                        for (Note note : foundNotes) {
                            int score = calculateScore(onNote, note);
                            if (score > maxScore) {
                                maxScore = score;
                                selected = note;
                            }
                        }
                        selected.score = maxScore;
                        selected.checkedTime = onNote.lastTime;
                        selected.checked = true;
                        Game.noteScored(key, maxScore);
                    }
                }
            }
        }
    }

    private int calculateScore(PressedNote onNote, Note note) {
        int score = 0;
        score += Math.max(((float) (delay - Math.abs(onNote.startTime - note.time))) / delay, 0) * 33;
        score += Math.max(((float) (delay - Math.abs(onNote.lastTime - (note.time + note.length)))) / delay, 0) * 33;
        long maxLengthDiff = Math.max(note.length, delay * 2);
        score += ((float) (maxLengthDiff - Math.abs((onNote.lastTime - onNote.startTime) - note.length))) / maxLengthDiff * 17;
        long maxVelocityDiff = Math.max(note.key.velocity, 127 - note.key.velocity);
        score += (maxVelocityDiff - Math.abs(onNote.avgVelocity - note.key.velocity)) / maxVelocityDiff * 17;
        if (Util.getDebugLevel() > 30) Util.debug("AVG VELOCITY: " + onNote.avgVelocity);
        if (Util.getDebugLevel() > 80) Util.debug("SCORE: " + (Math.max(((float) (delay - Math.abs(onNote.startTime - note.time))) / delay, 0) * 33) + " + " + (Math.max(((float) (delay - Math.abs(onNote.lastTime - (note.time + note.length)))) / delay, 0) * 33) + " + " + (((float) (maxLengthDiff - Math.abs((onNote.lastTime - onNote.startTime) - note.length))) / maxLengthDiff * 17) + " + " + (((maxVelocityDiff - Math.abs(onNote.avgVelocity - note.key.velocity))) / maxVelocityDiff * 17) + " = " + score);
        return score;
    }

    private void alterMessage(ShortMessage message, int device, boolean note) {
        if (Game.isGameActive()) {
            if (note) {
                PressedNote[][] noteOns = deviceNoteOns.get(device);
                if (noteOns != null) {
                    PressedNote pressedNote = noteOns[message.getChannel()][message.getData1()];
                    if (pressedNote != null) {
                        pressedNote.alter(getTickPosition(), message.getData2());
                    }
                    noteOns = new PressedNote[16][128];
                    deviceNoteOns.put(device, noteOns);
                }
            } else {
                PressedNote[][] noteOns = deviceNoteOns.get(device);
                if (noteOns != null) {
                    long time = getTickPosition();
                    int velocity = message.getData2();
                    PressedNote[] channelNoteOns = noteOns[message.getChannel()];
                    for (int i = 0; i < channelNoteOns.length; ++i) {
                        if (channelNoteOns[i] != null) {
                            channelNoteOns[i].alter(time, velocity);
                        }
                    }
                }
            }
        }
    }

    void setWaitingForController(boolean waitingForController) {
        this.waitingForController = waitingForController;
    }

    boolean isWaitingForController() {
        return waitingForController;
    }

    static void setPause(byte pause) {
        MidiSequencer.pause = pause;
    }

    static byte getPause() {
        return pause;
    }

    void setWaitingForNote(boolean waitingForNote) {
        this.waitingForNote = waitingForNote;
    }

    boolean isWaitingForNote() {
        return waitingForNote;
    }

    /**
	 * Creates a string containing the most important information about the game. This method is used only for debugging
	 * and testing purposes.
	 * 
	 * @return the created string.
	 */
    static String getString() {
        return "MidiSequencer()";
    }

    /**
	 * This method serves security purposes. Provides an integrity string that will be checked by the
	 * {@link Connection#integrityCheck()} method; thus the application can only be altered if the source is known.
	 * Every class in the {@link keyboardhero} package has an integrity string.
	 * 
	 * @return the string of this class used for integrity checking.
	 */
    static String getIntegrityString() {
        return "5-alS,am3+-ysDD6×as-";
    }

    /**
	 * The tester object of this class. It provides a debugging menu and unit tests for this class. Its only purpose is
	 * debugging or testing.
	 */
    static final Tester TESTER = new Tester("MidiSequencer", new String[] { "getString()" }) {

        void menu(int choice) throws Exception {
            switch(choice) {
                case 5:
                    System.out.println(getString());
                    break;
                default:
                    baseMenu(choice);
                    break;
            }
        }

        void runUnitTests() throws Exception {
            higherTestStart("MidiSequencer");
            testEq("getIntegrityString()", "5-alS,am3+-ysDD6×as-", MidiSequencer.getIntegrityString());
            higherTestEnd();
        }

        boolean isAutoSandbox() {
            return true;
        }

        void sandbox() throws Throwable {
        }
    };

    /**
	 * Starts the class's developing menu. If this build is a developer's one it starts the application in a normal way
	 * with the exception that it starts the debugging tool for this class as well; otherwise exits with an error
	 * message.
	 * 
	 * @param args
	 *            the arguments given to the program.
	 * @see KeyboardHero#startApp()
	 */
    public static void main(String[] args) {
        Tester.mainer(args, TESTER);
    }
}

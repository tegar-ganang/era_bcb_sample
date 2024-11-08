package keyboardhero;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import javax.sound.midi.*;
import keyboardhero.MidiSequencer.*;

/**
 * Used to load a song in the MIDI file format. Details concerning the file and the MIDI data within
 * the file can be retrieved, as well as the MIDI data itself.
 */
final class MidiSong {

    static final class MidiFileInfo {

        private final String author, title;

        private byte difficulty;

        MidiFileInfo(String author, String title, byte difficulty) {
            this.author = author;
            this.title = title;
            this.difficulty = difficulty;
        }

        final String getAuthor() {
            return author;
        }

        final String getTitle() {
            return title;
        }

        final byte getDifficulty() {
            return difficulty;
        }
    }

    static final class TempoTickInfo {

        long tick;

        Iterator<Map.Entry<Long, Float>> iterator;

        Map.Entry<Long, Float> previous, next;
    }

    static final String MIDI_FILES_DIR = "midifiles/";

    static final byte MAX_NUM_OF_CHANNELS = 16;

    private static final String[] INSTRUMENTS = new String[] { ".*(?i)(piano|keyboard|synt).*", ".*(?i)(left|right|\\slh|\\srh|hand).*", ".*(?i)(organ).*" };

    private static final MidiSong INSTANCE = new MidiSong();

    private static final MidiSequencer SEQUENCER = MidiSequencer.getInstance();

    /**
	 * This field is updated by the getMidiFileInfo if findTrackAndTempo is requested, hence when a
	 * new song is loaded. The array stores the indexes of the tracks which should be displayed. The
	 * tracks preferred in the following order:
	 * <ul>
	 * <li>tracks containing the words piano or keyboard</li>
	 * <li>tracks containing hand information (eg.: Right Hand)</li>
	 * <li>track which holds the most number of notes (note on events)</li>
	 * </ul>
	 * 
	 * @see #getMidiFileInfo(URL, boolean, float, Sequence)
	 * @see #loadMidiFile(URL)
	 */
    private static int[] lastFoundTracks;

    /**
	 * This field is updated by the getMidiFileInfo if findTrackAndTempo is requested, hence when a
	 * new song is loaded. The keys are the time positions in midi ticks when the event occurs, and
	 * the values are stored in the following format for caching purposes:<br>
	 * <code>
	 * {@link MidiSequencer#TICKS_PER_MINUTE_FOR_PPQ} / (<beats per minute (BPM) gained from the
	 * event> * <ticks per beat (resolution) obtained from the file format>)</code>
	 */
    private static TreeMap<Long, Float> lastFoundTempos = new TreeMap<Long, Float>();

    private final boolean[] channels = new boolean[MAX_NUM_OF_CHANNELS];

    private MidiFileInfo info;

    private boolean isPPQ;

    private float ticksPerSecondBase;

    private Sequence midiSequence;

    private Note[] noteSequence;

    private MidiSong() {
    }

    static final MidiSong getInstance() {
        return INSTANCE;
    }

    static final MidiFileInfo getMidiFileInfo(URL file) throws IOException, InvalidMidiDataException {
        return getMidiFileInfo(file, false, 0, MidiSystem.getSequence(file));
    }

    static final MidiFileInfo getMidiFileInfo(URL file, boolean findTrackAndTempos, float ticksPerSecondBase) throws IOException, InvalidMidiDataException {
        return getMidiFileInfo(file, findTrackAndTempos, ticksPerSecondBase, MidiSystem.getSequence(file));
    }

    @SuppressWarnings({ "fallthrough", "null" })
    static MidiFileInfo getMidiFileInfo(URL file, boolean findTrackAndTempos, float ticksPerSecondBase, Sequence sequence) {
        String author = null, title = null;
        byte difficulty = -1;
        byte foundInfos = 0;
        final Track[] tracks = sequence.getTracks();
        final int numOfTracks = tracks.length;
        ArrayList<ArrayList<Integer>> instruments = null;
        boolean[] instrumentsAdd;
        int instrumentsNum = 0;
        if (findTrackAndTempos) {
            instruments = new ArrayList<ArrayList<Integer>>();
            instrumentsNum = INSTRUMENTS.length;
            for (int i = 0; i < instrumentsNum; ++i) {
                instruments.add(new ArrayList<Integer>());
            }
        }
        int maxId = -1;
        long maxNum = -1;
        if (findTrackAndTempos) {
            lastFoundTempos.clear();
        }
        findinfos: for (int j = 0; j < numOfTracks; ++j) {
            long numOfNotes = 0;
            final Track track = tracks[j];
            final int max = track.size();
            instrumentsAdd = new boolean[instrumentsNum];
            for (int i = 0; i < max; ++i) {
                final MidiEvent midiEvent = track.get(i);
                final MidiMessage midiMessage = midiEvent.getMessage();
                if (midiMessage instanceof MetaMessage) {
                    MetaMessage metaMessage = (MetaMessage) midiMessage;
                    String cache;
                    switch(metaMessage.getType()) {
                        case 1:
                            cache = (new String(metaMessage.getData())).trim();
                            if (author == null) {
                                if (cache.matches("(?i)by .*")) {
                                    author = cache.substring(3);
                                } else {
                                    author = cache;
                                }
                                if (!findTrackAndTempos && ++foundInfos == 3) break findinfos;
                            }
                            break;
                        case 3:
                            cache = (new String(metaMessage.getData())).trim();
                            if (cache.matches("(?i)by .*")) {
                                if (author == null) {
                                    author = cache.substring(3);
                                    if (!findTrackAndTempos && ++foundInfos == 3) break findinfos;
                                }
                            } else if (title == null) {
                                title = cache;
                                if (!findTrackAndTempos && ++foundInfos == 3) break findinfos;
                            }
                        case 4:
                            if (findTrackAndTempos) {
                                final String instrument = new String(metaMessage.getData());
                                if (Util.getDebugLevel() > 50) Util.debug("INSTRUMENT ON TRACK " + j + " : " + instrument);
                                for (int k = 0; k < instrumentsNum; ++k) {
                                    if (instrument.matches(INSTRUMENTS[k])) {
                                        instrumentsAdd[k] = true;
                                        break;
                                    }
                                }
                            }
                            break;
                        case 81:
                            if (findTrackAndTempos) {
                                final byte[] data = metaMessage.getData();
                                final float bpm = 60000000 / (((data[0] < 0 ? data[0] + 256 : data[0]) << 16) + ((data[1] < 0 ? data[1] + 256 : data[1]) << 8) + (data[2] < 0 ? data[2] + 256 : data[2]));
                                lastFoundTempos.put(midiEvent.getTick(), MidiSequencer.TICKS_PER_MINUTE_FOR_PPQ / (bpm * ticksPerSecondBase));
                            }
                            break;
                        case 127:
                            if (difficulty == -1) {
                                final byte message = metaMessage.getData()[0];
                                switch(message) {
                                    case 10:
                                    case 20:
                                    case 30:
                                    case 40:
                                    case 50:
                                        difficulty = message;
                                        break;
                                    default:
                                        --foundInfos;
                                        break;
                                }
                                if (!findTrackAndTempos && ++foundInfos == 3) break findinfos;
                            }
                            break;
                    }
                } else if (midiMessage instanceof ShortMessage && ((ShortMessage) midiMessage).getCommand() == ShortMessage.NOTE_ON) {
                    ++numOfNotes;
                }
            }
            if (findTrackAndTempos && numOfNotes > 0) {
                if (numOfNotes > maxNum) {
                    maxNum = numOfNotes;
                    maxId = j;
                }
                for (int i = 0; i < instrumentsNum; ++i) {
                    if (instrumentsAdd[i]) {
                        instruments.get(i).add(j);
                        break;
                    }
                }
            }
        }
        if (foundInfos < 3) {
            String path;
            try {
                path = URLDecoder.decode(file.getPath(), "UTF-8");
                final String name = path.substring(path.lastIndexOf('/') + 1);
                String[] parts = Util.ascii2utf(name).replaceFirst("\\.[^.]*$", "").split("\\^", 2);
                if (difficulty == -1 && parts.length == 2) {
                    final String cache = parts[1].trim();
                    for (Map.Entry<Byte, String> entry : DialogSongList.SongSelector.DIFFICULTIES.entrySet()) {
                        if (cache.equals(entry.getValue())) {
                            difficulty = entry.getKey();
                            break;
                        }
                    }
                    ++foundInfos;
                }
                if (foundInfos < 3) {
                    parts = parts[0].split("%", 2);
                    if (parts.length == 2) {
                        if (author == null) author = parts[0].trim();
                        if (title == null) title = parts[1].trim();
                    } else if (title == null) {
                        title = parts[0].trim();
                    }
                }
            } catch (UnsupportedEncodingException e) {
                if (Util.getDebugLevel() > 87) e.printStackTrace();
            }
        }
        if (findTrackAndTempos) {
            boolean notFound = true;
            for (int i = 0; i < instrumentsNum; ++i) {
                final ArrayList<Integer> foundTracks;
                if (!(foundTracks = instruments.get(i)).isEmpty()) {
                    lastFoundTracks = Util.toArray(foundTracks);
                    notFound = false;
                    break;
                }
            }
            if (notFound) {
                if (maxId != -1) {
                    lastFoundTracks = new int[] { maxId };
                } else {
                    lastFoundTracks = new int[0];
                }
            }
        }
        return new MidiFileInfo(author, title, difficulty);
    }

    static File setMidiFileInfo(File file, String author, String title, byte difficulty) {
        try {
            Sequence sequence = MidiSystem.getSequence(file);
            byte foundInfos = 0;
            boolean notFoundAuthor = true, notFoundTitle = true, notFoundDifficulty = true;
            findinfos: for (Track track : sequence.getTracks()) {
                final int max = track.size();
                for (int i = 0; i < max; ++i) {
                    MidiMessage midiMessage = track.get(i).getMessage();
                    if (midiMessage instanceof MetaMessage) {
                        MetaMessage metaMessage = (MetaMessage) midiMessage;
                        switch(metaMessage.getType()) {
                            case 1:
                                if (notFoundAuthor) {
                                    notFoundAuthor = false;
                                    final byte[] data = author.getBytes();
                                    metaMessage.setMessage(1, data, data.length);
                                    if (++foundInfos == 3) break findinfos;
                                }
                                break;
                            case 3:
                                if (notFoundTitle) {
                                    notFoundTitle = false;
                                    MetaMessage newMetaMessage = new MetaMessage();
                                    byte[] data = metaMessage.getData();
                                    newMetaMessage.setMessage(4, data, data.length);
                                    track.add(new MidiEvent(metaMessage, 0));
                                    data = title.getBytes();
                                    metaMessage.setMessage(3, data, data.length);
                                    if (++foundInfos == 3) break findinfos;
                                }
                                break;
                            case 127:
                                if (notFoundDifficulty) {
                                    notFoundDifficulty = false;
                                    if (difficulty == 0) {
                                        track.remove(track.get(i));
                                    } else {
                                        metaMessage.setMessage(127, new byte[] { difficulty }, 1);
                                    }
                                    if (++foundInfos == 3) break findinfos;
                                }
                                break;
                        }
                    }
                }
            }
            if (foundInfos < 3) {
                Track track = sequence.getTracks()[0];
                if (notFoundAuthor) {
                    MetaMessage metaMessage = new MetaMessage();
                    final byte[] data = author.getBytes();
                    metaMessage.setMessage(1, data, data.length);
                    track.add(new MidiEvent(metaMessage, 0));
                }
                if (notFoundTitle) {
                    MetaMessage metaMessage = new MetaMessage();
                    final byte[] data = title.getBytes();
                    metaMessage.setMessage(3, data, data.length);
                    track.add(new MidiEvent(metaMessage, 0));
                }
                if (difficulty != 0 && notFoundDifficulty) {
                    MetaMessage metaMessage = new MetaMessage();
                    metaMessage.setMessage(127, new byte[] { difficulty }, 1);
                    track.add(new MidiEvent(metaMessage, 0));
                }
            }
            try {
                MidiSystem.write(sequence, MidiSystem.getMidiFileTypes(sequence)[0], file);
            } catch (ArrayIndexOutOfBoundsException e) {
                Util.error(Util.getMsg("Err_NotSupportedMidiFileType"));
            }
            final String difficultyStr = DialogSongList.SongSelector.DIFFICULTIES.get(difficulty);
            final File newFile = new File(Util.DATA_FOLDER + MIDI_FILES_DIR + Util.utf2ascii((author.equals("") ? "" : author + " % ") + title) + (difficultyStr == null ? "" : " ^ " + difficultyStr) + ".mid");
            file.renameTo(newFile);
            return newFile;
        } catch (InvalidMidiDataException e) {
            Util.error(Util.getMsg("Err_InvalidMidiData"), e.getLocalizedMessage());
        } catch (FileNotFoundException e) {
            Util.error(Util.getMsg("Err_CouldntAccessFile"), e.getLocalizedMessage());
        } catch (EOFException e) {
            Util.error(Util.getMsg("Err_InvalidMidiData"));
        } catch (Exception e) {
            Util.error(Util.getMsg("Err_CouldntAccessFile"));
        }
        return null;
    }

    /**
	 * Attempts to load the given file, if it is successful the MIDI sequence, the converted list of
	 * notes and file details are stored appropriately.
	 * 
	 * @param file
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
    @SuppressWarnings("unchecked")
    void loadMidiFile(URL file) throws InvalidMidiDataException, IOException {
        midiSequence = MidiSystem.getSequence(file);
        calculateTickPerSecond();
        info = getMidiFileInfo(file, true, ticksPerSecondBase, midiSequence);
        if (lastFoundTracks.length == 0) {
            throw new InvalidMidiDataException(Util.getMsg("Err_NoSoundInFile"));
        }
        calculateTickPerSecondIfConstantPPQ(lastFoundTempos);
        SEQUENCER.setTempos(lastFoundTempos);
        if (Util.getDebugLevel() > 50) {
            Util.debug("SELECTED TRACKS: " + lastFoundTracks[0] + (lastFoundTracks.length >= 2 ? " | " + lastFoundTracks[1] : ""));
        }
        parseTracks(lastFoundTracks, lastFoundTempos);
        MidiDevicer.channelsChanged(channels);
        Game.setNoteSequence(noteSequence);
        Game.setSongInfo(info);
        SEQUENCER.setNoteSequence(noteSequence);
    }

    /**
	 * Converts corresponding noteOn and noteOf events in the provided tracks to a single sequence
	 * of Note.
	 * 
	 * @param trackNums
	 *            the id of the tracks containing the MidiEvents to be converted
	 * @param tempos
	 *            the map of tempo-changing events for PPQ with the key being a tick position
	 */
    private void parseTracks(final int[] trackNums, final TreeMap<Long, Float> tempos) {
        ArrayList<Note> noteList = new ArrayList<Note>();
        final Track[] tracks = midiSequence.getTracks();
        for (int i = 0; i < channels.length; ++i) {
            channels[i] = false;
        }
        for (int trackNum : trackNums) {
            parseTrack(noteList, tracks[trackNum], tempos);
        }
        for (int trackNum = 0; trackNum < tracks.length; ++trackNum) {
            if (Arrays.binarySearch(trackNums, trackNum) >= 0) continue;
            Track track = tracks[trackNum];
            final int max = track.size();
            for (int i = 0; i < max; ++i) {
                final MidiMessage message = track.get(i).getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage shortMessage = (ShortMessage) message;
                    channels[shortMessage.getChannel()] = true;
                }
            }
        }
        Collections.sort(noteList);
        noteSequence = noteList.toArray(Game.State.NULL_NOTES);
    }

    /**
	 * Given a track from the sequence, converts corresponding noteOn and noteOf events to a single
	 * Note. This is then added to the given ArrayList.
	 * 
	 * @param noteList
	 *            the sequence which should be expanded by the given notes
	 * @param track
	 *            the Track containing the MidiEvents to be converted
	 * @param tempos
	 *            the map of tempo-changing events for PPQ with the key being a tick position
	 */
    private void parseTrack(ArrayList<Note> noteList, final Track track, final TreeMap<Long, Float> tempos) {
        TempoTickInfo tempoTickInfo = new TempoTickInfo();
        tempoTickInfo.tick = 0;
        tempoTickInfo.iterator = tempos.entrySet().iterator();
        if (isPPQ) {
            tempoTickInfo.next = tempoTickInfo.iterator.next();
            tempoTickInfo.previous = new AbstractMap.SimpleImmutableEntry<Long, Float>(0l, MidiSequencer.TICKS_PER_MINUTE_FOR_PPQ / (120 * ticksPerSecondBase));
        }
        final MidiEvent[] noteOns = new MidiEvent[128];
        final int max = track.size();
        for (int i = 0; i < max; ++i) {
            MidiEvent midiEvent = track.get(i);
            final MidiMessage message = midiEvent.getMessage();
            if (message instanceof ShortMessage) {
                ShortMessage shortMessage = (ShortMessage) message;
                channels[shortMessage.getChannel()] = true;
                final int command = shortMessage.getCommand();
                boolean noteOff = false;
                if (command == ShortMessage.NOTE_ON) {
                    if (shortMessage.getData2() == 0) {
                        noteOff = true;
                    } else {
                        if (isPPQ) {
                            tempoTickInfo = shiftTempoTickInfo(midiEvent.getTick(), tempoTickInfo);
                            midiEvent = new MidiEvent(midiEvent.getMessage(), tempoTickInfo.tick + (long) ((midiEvent.getTick() - tempoTickInfo.previous.getKey()) * tempoTickInfo.previous.getValue()));
                        }
                        noteOns[shortMessage.getData1()] = midiEvent;
                    }
                } else if (command == ShortMessage.NOTE_OFF) {
                    noteOff = true;
                }
                if (noteOff) {
                    final int midiKey = shortMessage.getData1();
                    final MidiEvent onNote = noteOns[midiKey];
                    if (onNote != null) {
                        noteOns[midiKey] = null;
                        long length;
                        if (isPPQ) {
                            tempoTickInfo = shiftTempoTickInfo(midiEvent.getTick(), tempoTickInfo);
                            length = tempoTickInfo.tick + (long) ((midiEvent.getTick() - tempoTickInfo.previous.getKey()) * tempoTickInfo.previous.getValue()) - onNote.getTick();
                        } else {
                            length = midiEvent.getTick() - onNote.getTick();
                        }
                        noteList.add(new Note(new Key(midiKey, ((ShortMessage) onNote.getMessage()).getData2()), onNote.getTick(), length));
                    }
                }
            }
        }
    }

    static final TempoTickInfo shiftTempoTickInfo(final long eventTick, final TempoTickInfo tempoTickInfo) {
        while (eventTick > tempoTickInfo.next.getKey()) {
            tempoTickInfo.tick += (tempoTickInfo.next.getKey() - tempoTickInfo.previous.getKey()) * tempoTickInfo.previous.getValue();
            tempoTickInfo.previous = tempoTickInfo.next;
            if (tempoTickInfo.iterator.hasNext()) {
                tempoTickInfo.next = tempoTickInfo.iterator.next();
            } else {
                tempoTickInfo.next = new AbstractMap.SimpleImmutableEntry<Long, Float>(Long.MAX_VALUE, 0f);
            }
        }
        return tempoTickInfo;
    }

    private final void calculateTickPerSecond() {
        final float division = midiSequence.getDivisionType();
        if (division == Sequence.PPQ) {
            SEQUENCER.setPPQ(isPPQ = true);
            SEQUENCER.setTicksPerSecondBase(ticksPerSecondBase = midiSequence.getResolution());
        } else {
            SEQUENCER.setPPQ(isPPQ = false);
            SEQUENCER.setTicksPerSecondBase(ticksPerSecondBase = (division * midiSequence.getResolution()));
            if (Util.getDebugLevel() > 30) Util.debug("TYPE - SMPTE: " + ticksPerSecondBase);
        }
    }

    private final void calculateTickPerSecondIfConstantPPQ(TreeMap<Long, Float> tempos) {
        if (isPPQ) {
            if (tempos.isEmpty()) {
                SEQUENCER.setPPQ(isPPQ = false);
                SEQUENCER.setTicksPerSecondBase(ticksPerSecondBase *= 2);
                if (Util.getDebugLevel() > 30) Util.debug("TYPE - CONSTANT PPQ - NO TEMPO: " + ticksPerSecondBase);
            } else if (tempos.size() == 1) {
                Map.Entry<Long, Float> entry = tempos.firstEntry();
                if (entry.getKey() == 0) {
                    SEQUENCER.setPPQ(isPPQ = false);
                    SEQUENCER.setTicksPerSecondBase(ticksPerSecondBase = ticksPerSecondBase * MidiSequencer.TICKS_PER_MINUTE_FOR_PPQ / entry.getValue() / ticksPerSecondBase / 60);
                    if (Util.getDebugLevel() > 30) Util.debug("TYPE - CONSTANT PPQ - ONE TEMPO BEING FIRST EVENT: " + midiSequence.getResolution());
                    return;
                }
            }
        }
    }

    /**
	 * Returns the sequence contained within the file. This sequence contains the Track[s] which
	 * hold MidiEvent[s].
	 * 
	 * @return Sequence holding MidiEvents held in the file
	 */
    Sequence getMidiSequence() {
        return midiSequence;
    }

    /**
	 * Creates a string containing the most important information about the game. This method is
	 * used only for debugging and testing purposes.
	 * 
	 * @return the created string.
	 */
    static String getString() {
        return "MidiSong()";
    }

    /**
	 * This method serves security purposes. Provides an integrity string that will be checked by
	 * the {@link Connection#integrityCheck()} method; thus the application can only be altered if
	 * the source is known. Every class in the {@link keyboardhero} package has an integrity string.
	 * 
	 * @return the string of this class used for integrity checking.
	 */
    static String getIntegrityString() {
        return "jhaÍs+!.Sys-sdf+éUiáső";
    }

    /**
	 * The tester object of this class. It provides a debugging menu and unit tests for this class.
	 * Its only purpose is debugging or testing.
	 */
    static final Tester TESTER = new Tester("MidiSong", new String[] { "getString()", "setMidiFileInfo(file, author, title, difficulty)" }) {

        void menu(int choice) throws Exception {
            switch(choice) {
                case 5:
                    System.out.println(getString());
                    break;
                case 6:
                    final File file = new File(Util.getURL(readString("String file")).toURI());
                    final String author = readString("String author");
                    final String title = readString("String title");
                    final int difficulty = readInt("byte difficulty");
                    setMidiFileInfo(file, author, title, (byte) difficulty);
                    break;
                default:
                    baseMenu(choice);
                    break;
            }
        }

        void runUnitTests() throws Exception {
            higherTestStart("MidiSong");
            testEq("getIntegrityString()", "jhaÍs+!.Sys-sdf+éUiáső", MidiSong.getIntegrityString());
            higherTestEnd();
        }

        boolean isAutoSandbox() {
            return true;
        }

        void sandbox() throws Throwable {
        }
    };

    /**
	 * Starts the class's developing menu. If this build is a developer's one it starts the
	 * application in a normal way with the exception that it starts the debugging tool for this
	 * class as well; otherwise exits with an error message.
	 * 
	 * @param args
	 *            the arguments given to the program.
	 * @see KeyboardHero#startApp()
	 */
    public static void main(String[] args) {
        Tester.mainer(args, TESTER);
    }
}

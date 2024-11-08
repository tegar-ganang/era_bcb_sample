package imp.data;

import imp.util.ErrorLog;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Midi File Importing
 *
 * @author Robert Keller, partly adapted from code in MIDIBeast 
 * by Brandy McMenamy and Jim Herold
 */
public class MidiImport {

    public static final int DRUM_CHANNEL = 9;

    File file;

    private int defaultResolution = 1;

    private int defaultStartFactor = 2;

    private int resolution;

    private int startFactor;

    private static jm.music.data.Score score;

    private static ArrayList<jm.music.data.Part> allParts;

    private LinkedList<MidiImportRecord> melodies;

    public MidiImport() {
        setResolution(defaultResolution);
        setStartFactor(defaultStartFactor);
    }

    public int getResolution() {
        return resolution;
    }

    public final void setResolution(int newResolution) {
        resolution = newResolution;
    }

    public int getStartFactor() {
        return startFactor;
    }

    public final void setStartFactor(int newStartFactor) {
        startFactor = newStartFactor;
    }

    public void importMidi(File file) {
        if (file != null) {
            readMidiFile(file.getAbsolutePath());
        }
    }

    /**
 * @param String midiFileName
 * 
 */
    public void readMidiFile(String midiFileName) {
        score = new jm.music.data.Score();
        try {
            jm.util.Read.midi(score, midiFileName);
        } catch (Error e) {
            ErrorLog.log(ErrorLog.WARNING, "reading of MIDI file " + midiFileName + " failed for some reason (jMusic exception).");
            return;
        }
        scoreToMelodies();
    }

    public void scoreToMelodies() {
        if (score != null) {
            MIDIBeast.setResolution(resolution);
            MIDIBeast.calculateNoteTypes(score.getDenominator());
            allParts = new ArrayList<jm.music.data.Part>();
            allParts.addAll(Arrays.asList(score.getPartArray()));
            ImportMelody importMelody = new ImportMelody(score);
            melodies = new LinkedList<MidiImportRecord>();
            for (int i = 0; i < importMelody.size(); i++) {
                try {
                    jm.music.data.Part part = importMelody.getPart(i);
                    int channel = part.getChannel();
                    int numTracks = part.getSize();
                    for (int j = 0; j < numTracks; j++) {
                        MelodyPart partOut = new MelodyPart();
                        importMelody.convertToImpPart(part, j, partOut, resolution, startFactor);
                        String instrumentString = MIDIBeast.getInstrumentForPart(part);
                        if (channel != DRUM_CHANNEL) {
                            partOut.setInstrument(part.getInstrument());
                        }
                        MidiImportRecord record = new MidiImportRecord(channel, j, partOut, instrumentString);
                        melodies.add(record);
                    }
                } catch (java.lang.OutOfMemoryError e) {
                    ErrorLog.log(ErrorLog.SEVERE, "There is not enough memory to continue importing this MIDI file.");
                    return;
                }
            }
            Collections.sort(melodies);
        }
    }

    public jm.music.data.Score getScore() {
        return score;
    }

    public LinkedList<MidiImportRecord> getMelodies() {
        return melodies;
    }
}

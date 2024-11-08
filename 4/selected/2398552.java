package br.mendonca.gmmidilib;

import java.io.File;
import java.io.IOException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import br.mendonca.gmmidilib.enums.KeySignature;
import br.mendonca.gmmidilib.enums.Note;

/**
 * @author mendonça
 *
 */
public class Music extends Sequence {

    protected KeySignature keySignature;

    protected int totalNotes = 0;

    protected int[] notesQuantity = new int[12];

    protected float[] notesRatio = new float[12];

    protected boolean statisticsAvailable = false;

    private static final int PERCUSSION_CHANNEL = 9;

    /**
	 * @param sequence
	 * @throws InvalidMidiDataException 
	 */
    public Music(Sequence sequence) throws InvalidMidiDataException {
        super(sequence.getDivisionType(), sequence.getResolution());
        for (Track sequenceTrack : sequence.getTracks()) {
            tracks.add(sequenceTrack);
        }
    }

    /**
	 * Obtains a <code>Music</code> object from the specified file.
	 *  
	 * @param path - the path of the file from which the <code>Music</code> will be 
	 * constructed 
	 * @return a <code>Music</code> object based on the file specified by <code>path</code>
	 * @throws IOException
	 * @throws InvalidMidiDataException 
	 */
    public static Music getMusicFromFile(String path) throws IOException, InvalidMidiDataException {
        File file = new File(path);
        return getMusicFromFile(file);
    }

    /**
	 * @param file
	 * @return
	 * @throws IOException 
	 * @throws InvalidMidiDataException 
	 */
    public static Music getMusicFromFile(File file) throws IOException, InvalidMidiDataException {
        return new Music(MidiSystem.getSequence(file));
    }

    /**
	 * Generates statistics based on the <code>Music</code>'s tracks.
	 */
    public void generateStatistics() {
        for (Track track : getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage shortMessage = (ShortMessage) message;
                    if (shortMessage.getChannel() == PERCUSSION_CHANNEL) break;
                    parseShortMessage(shortMessage);
                } else if (message instanceof MetaMessage) {
                    MetaMessage metaMessage = (MetaMessage) message;
                    parseMetaMessage(metaMessage);
                } else if (message instanceof SysexMessage) {
                    SysexMessage sysexMessage = (SysexMessage) message;
                    parseSysexMessage(sysexMessage);
                }
            }
        }
        if (keySignature == null) keySignature = KeySignature.C;
        calculateRatios();
        statisticsAvailable = true;
    }

    @Override
    public String toString() {
        String result = "Key Signature: " + keySignature + "\n\r";
        result += "Total Notes: " + totalNotes + "\n\r";
        result += "Notes Quantity: \n\r";
        for (int i = 0; i < 12; i++) result += "\t" + Note.values()[i] + " - " + notesQuantity[i] + "\n\r";
        result += "Notes Ratio: \n\r";
        for (int i = 0; i < 12; i++) result += "\t" + Note.values()[i] + " - " + notesRatio[i] + "\n\r";
        result += "----------------------------------------\n\r";
        return result;
    }

    /**
	 * 
	 */
    protected void calculateRatios() {
        for (int i = 0; i < 12; i++) {
            notesRatio[i] = (float) (notesQuantity[i]) / (float) (totalNotes);
        }
    }

    /**
	 * Updates the statistics based on a <code>ShortMessage</code>.
	 * 
	 * @param message - a <code>ShortMessage</code> from a track of this 
	 * <code>Music</code>
	 */
    protected void parseShortMessage(ShortMessage message) {
        switch(message.getCommand()) {
            case ShortMessage.NOTE_ON:
                Note note = Note.getNote(message.getData1());
                notesQuantity[note.ordinal()]++;
                totalNotes++;
                break;
        }
    }

    /**
	 * Updates the statistics based on a <code>SysexMessage</code>.
	 * 
	 * @param message - a <code>SysexMessage</code> from a track of this 
	 * <code>Music</code>
	 */
    protected void parseSysexMessage(SysexMessage message) {
    }

    /**
	 * Updates the statistics based on a <code>MetaMessage</code>.
	 * 
	 * @param message - a <code>MetaMessage</code> from a track of this 
	 * <code>Music</code>
	 */
    protected void parseMetaMessage(MetaMessage message) {
        byte[] data = message.getData();
        switch(message.getType()) {
            case 0x59:
                if (keySignature == null) {
                    keySignature = KeySignature.getKeySignature(data);
                }
                break;
            case 3:
                break;
        }
    }

    /**
	 * @param keySignature the keySignature to set
	 */
    public void setKeySignature(KeySignature keySignature) {
        this.keySignature = keySignature;
    }

    /**
	 * @return the keySignature
	 */
    public KeySignature getKeySignature() {
        if (statisticsAvailable) return keySignature; else return null;
    }

    /**
	 * @param totalNotes the totalNotes to set
	 */
    public void setTotalNotes(int totalNotes) {
        this.totalNotes = totalNotes;
    }

    /**
	 * @return the totalNotes
	 */
    public int getTotalNotes() {
        if (statisticsAvailable) return totalNotes; else return 0;
    }

    /**
	 * @param notesQuantity the notesQuantity to set
	 */
    public void setNotesQuantity(int[] notesQuantity) {
        this.notesQuantity = notesQuantity;
    }

    /**
	 * @return the notesQuantity
	 */
    public int[] getNotesQuantity() {
        if (statisticsAvailable) return notesQuantity; else return null;
    }

    /**
	 * @param notesRatio the notesRatio to set
	 */
    public void setNotesRatio(float[] notesRatio) {
        this.notesRatio = notesRatio;
    }

    /**
	 * @return the notesRatio
	 */
    public float[] getNotesRatio() {
        if (statisticsAvailable) return notesRatio; else return null;
    }
}

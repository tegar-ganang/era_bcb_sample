package eu.davidgamez.mas.agents.simplenotes.midi;

import java.util.Iterator;
import java.util.Vector;
import java.util.ArrayList;
import java.io.*;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import org.w3c.dom.Document;
import eu.davidgamez.mas.Constants;
import eu.davidgamez.mas.Globals;
import eu.davidgamez.mas.Util;
import eu.davidgamez.mas.exception.MASXmlException;
import eu.davidgamez.mas.gui.MsgHandler;
import eu.davidgamez.mas.midi.Agent;
import eu.davidgamez.mas.midi.MIDIEvent;
import eu.davidgamez.mas.midi.Track;
import eu.davidgamez.mas.midi.Utilities;

public class SimpleNotes extends Agent implements Constants {

    private int notePitch = 60;

    private int noteVelocity = 80;

    private int noteFrequency_ppq = PPQ_RESOLUTION;

    private int noteLength_ppq = PPQ_RESOLUTION / 2;

    private long lastNote_ppq = -1;

    boolean frequencyChanged = false;

    int newNoteFrequency_ppq = -1;

    boolean connectionStatusChanged = false;

    boolean enabledStatusChanged = false;

    private ArrayList<MIDIEvent> midiEventArrayList = new ArrayList<MIDIEvent>();

    /** Constructor */
    public SimpleNotes() {
        super("Simple notes", "Simple Notes", "SimpleNotes");
    }

    /** Outputs agent parameters as an XML string */
    public String getXML(String indent) {
        String tmpStr = indent + "<midi_agent>";
        tmpStr += super.getXML(indent + "\t");
        tmpStr += indent + "\t<note_pitch>" + notePitch + "</note_pitch>";
        tmpStr += indent + "\t<note_velocity>" + noteVelocity + "</note_velocity>";
        tmpStr += indent + "\t<note_frequency>" + noteFrequency_ppq + "</note_frequency>";
        tmpStr += indent + "\t<note_length>" + noteLength_ppq + "</note_length>";
        tmpStr += indent + "</midi_agent>";
        return tmpStr;
    }

    /** Loads the agent parameters from the supplied XML string */
    public void loadFromXML(String xmlStr) throws MASXmlException {
        super.loadFromXML(xmlStr);
        try {
            Document xmlDoc = Util.getXMLDocument(xmlStr);
            notePitch = Util.getIntParameter("note_pitch", xmlDoc);
            noteVelocity = Util.getIntParameter("note_velocity", xmlDoc);
            noteFrequency_ppq = Util.getIntParameter("note_frequency", xmlDoc);
            noteLength_ppq = Util.getIntParameter("note_length", xmlDoc);
        } catch (Exception ex) {
            System.out.println(xmlStr);
            ex.printStackTrace();
            MsgHandler.error(ex.getMessage());
        }
    }

    public boolean updateTracks(long bufferStart_ppq, long bufferEnd_ppq) throws InvalidMidiDataException {
        if (frequencyChanged) {
            if (newNoteFrequency_ppq < noteFrequency_ppq) while (lastNote_ppq + newNoteFrequency_ppq < bufferStart_ppq) lastNote_ppq += newNoteFrequency_ppq;
            noteFrequency_ppq = newNoteFrequency_ppq;
        }
        if (connectionStatusChanged || enabledStatusChanged) {
            connectionStatusChanged = false;
            enabledStatusChanged = false;
            lastNote_ppq = Utilities.getFirstBeatInBuffer(bufferStart_ppq);
        }
        for (Track midiTrack : trackMap.values()) {
            Iterator<MIDIEvent> iter = midiEventArrayList.iterator();
            while (iter.hasNext()) {
                MIDIEvent tempMidiEvent = iter.next();
                if (tempMidiEvent.getTimeStamp() < bufferStart_ppq) iter.remove(); else if (tempMidiEvent.getTimeStamp() < bufferEnd_ppq) {
                    midiTrack.addMidiMessage(tempMidiEvent.getTimeStamp(), (ShortMessage) tempMidiEvent.getMessage());
                    iter.remove();
                }
            }
            long nextNote_ppq;
            if (bufferStart_ppq == 0) {
                lastNote_ppq = 0;
                nextNote_ppq = 0;
            } else {
                nextNote_ppq = lastNote_ppq + noteFrequency_ppq;
            }
            while (nextNote_ppq >= bufferStart_ppq && nextNote_ppq < bufferEnd_ppq) {
                addNote(nextNote_ppq, bufferEnd_ppq, midiTrack);
                nextNote_ppq = lastNote_ppq + noteFrequency_ppq;
            }
        }
        return true;
    }

    public void connectionStatusChanged() {
        connectionStatusChanged = true;
    }

    public void enabledStatusChanged() {
        enabledStatusChanged = true;
    }

    private void addNote(long noteStart_ppq, long bufferEnd_ppq, Track midiTrack) throws InvalidMidiDataException {
        ShortMessage on = new ShortMessage();
        on.setMessage(ShortMessage.NOTE_ON, midiTrack.getChannel(), notePitch, noteVelocity);
        midiTrack.addMidiMessage(noteStart_ppq, on);
        lastNote_ppq = noteStart_ppq;
        ShortMessage off = new ShortMessage();
        off.setMessage(ShortMessage.NOTE_OFF, midiTrack.getChannel(), notePitch, noteVelocity);
        if ((noteStart_ppq + noteLength_ppq) >= bufferEnd_ppq) {
            midiEventArrayList.add(new MIDIEvent(off, noteStart_ppq + noteLength_ppq));
        } else {
            midiTrack.addMidiMessage(noteStart_ppq + noteLength_ppq, off);
        }
    }

    public int getNotePitch() {
        return notePitch;
    }

    public void setNotePitch(int newPitch) {
        notePitch = newPitch;
    }

    public int getNoteVelocity() {
        return noteVelocity;
    }

    public void setNoteVelocity(int newVelocity) {
        noteVelocity = newVelocity;
    }

    public int getNoteLength() {
        return noteLength_ppq;
    }

    public void setNoteLength(int newLength) {
        noteLength_ppq = newLength;
    }

    public int getNoteFrequency() {
        return noteFrequency_ppq;
    }

    public void setNoteFrequency(int newFrequency_ppq) {
        if (Globals.isPlaying()) {
            newNoteFrequency_ppq = newFrequency_ppq;
            frequencyChanged = true;
        } else noteFrequency_ppq = newFrequency_ppq;
    }

    @Override
    protected void reset() {
    }
}

package eu.davidgamez.mas.agents.midifragmentsequencer.midi;

import javax.sound.midi.*;
import java.util.Iterator;
import java.util.Vector;
import java.io.*;
import eu.davidgamez.mas.exception.NoNoteOnFoundException;
import eu.davidgamez.mas.midi.Agent;
import eu.davidgamez.mas.midi.AgentMessage;

public class MidiFragmentSequencer extends Agent {

    private Vector midiFragments = new Vector();

    private MidiEvent[] midiSequence;

    private int sequenceCounter = 0;

    private long sequenceLength = -1;

    private long currentTrackPosition = 0;

    private long lastAddedEventTick = 0;

    private long lastNoteOnTickPoint = -1;

    public MidiFragmentSequencer() {
        super("MIDI Fragment Sequencer", "MIDI Fragment Sequencer", "MIDIFragmentSequencer");
    }

    public String getXML(String indent) {
        String tmpStr = "";
        tmpStr += indent + "<midi_agent>";
        tmpStr += super.getXML(indent + "\t");
        tmpStr += indent + "</midi_agent>";
        return tmpStr;
    }

    protected boolean updateTracks(long bufferStart, long bufferLength) {
        return true;
    }

    private void setTrackPosition(long tick) {
        long firstSequenceTickInBuffer = tick % sequenceLength;
        System.out.println("first sequence tick in buffer " + firstSequenceTickInBuffer + " tick  " + tick + " Sequence length " + sequenceLength);
        for (int i = 0; i < midiSequence.length; i++) {
            if (midiSequence[i].getTick() >= firstSequenceTickInBuffer) {
                sequenceCounter = i;
                if (sequenceCounter == 0) {
                    for (int j = midiSequence.length - 1; j >= 0; j--) {
                        if (containsNoteOnMessage(midiSequence[j])) {
                            lastNoteOnTickPoint = tick - (sequenceLength - midiSequence[j].getTick());
                            System.out.println("Set track position lastNoteTickPoint " + lastNoteOnTickPoint);
                            return;
                        }
                    }
                }
                return;
            }
        }
    }

    public void connectFragments() throws NoNoteOnFoundException {
        Vector tempMidiEventVector = new Vector();
        tempMidiEventVector.add(new AgentMessage(getAgentDescription(), getName(), 1, "Sequence start"));
        long tickOffset = 0, lastNoteOn_CF = -1;
        for (int i = 0; i < midiFragments.size(); i++) {
            MidiEvent[] midiEventArray = (MidiEvent[]) midiFragments.get(i);
            for (int j = 0; j < midiEventArray.length; j++) {
                MidiEvent midiEvent = midiEventArray[j];
                if (midiEvent.getMessage() instanceof ShortMessage) {
                    long nextTickPoint = midiEvent.getTick() + tickOffset;
                    if (containsNoteOnMessage(midiEvent)) lastNoteOn_CF = nextTickPoint;
                    midiEvent.setTick(nextTickPoint);
                    if (j == midiEventArray.length - 1) {
                        if (nextTickPoint > 0) {
                            System.out.print("next tick point: " + nextTickPoint + " old tick offset " + tickOffset);
                            tickOffset = (lastNoteOn_CF / 960) * 960 + 960;
                            System.out.println(" new tick offset: " + tickOffset);
                        }
                    }
                    tempMidiEventVector.add(midiEvent);
                }
            }
        }
        sequenceLength = (lastNoteOn_CF / 960) * 960 + 960;
        tempMidiEventVector.add(new AgentMessage(getAgentDescription(), getName(), 1, "Sequence end"));
        midiSequence = (MidiEvent[]) tempMidiEventVector.toArray(new MidiEvent[1]);
        printMidiEvents(midiSequence);
        if (lastNoteOn_CF == -1) throw new NoNoteOnFoundException("No notes on in sequence");
    }

    public Vector getMidiFragmentsVector() {
        return midiFragments;
    }

    private boolean containsNoteOnMessage(MidiEvent event) {
        if (event.getMessage() instanceof ShortMessage) {
            if (((ShortMessage) event.getMessage()).getCommand() == ShortMessage.NOTE_ON) return true; else return false;
        } else return false;
    }

    public void printMidiEvents(MidiEvent[] midiEventArray) {
        System.out.println("========================================MIDI EVENTS=======================================");
        System.out.println("Midi event array length = " + midiEventArray.length);
        for (int j = 0; j < midiEventArray.length; j++) {
            if (midiEventArray[j].getMessage() instanceof ShortMessage) {
                System.out.print("Tick: " + midiEventArray[j].getTick());
                ShortMessage sm = (ShortMessage) midiEventArray[j].getMessage();
                switch(sm.getCommand()) {
                    case (ShortMessage.NOTE_ON):
                        System.out.print("; Note On");
                        break;
                    case (ShortMessage.NOTE_OFF):
                        System.out.print("; Note Off");
                        break;
                    default:
                        System.out.print("; Unrecognised");
                }
                System.out.println("; Channel: " + sm.getChannel() + "; Note: " + sm.getData1() + "; Velocity: " + sm.getData2());
            } else {
                System.out.print("Tick: " + midiEventArray[j].getTick());
                System.out.println("; Not a recognised message! ");
            }
        }
        System.out.println();
    }

    @Override
    protected void reset() {
    }
}

package net.sourceforge.midivolumizer;

import java.awt.event.ActionEvent;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

/**
 * Used to play a byte sequence that contains MIDI data.
 */
public class SequencePlayer {

    private static boolean debug = false;

    private Sequencer sequencer;

    private Synthesizer synthesizer;

    /**
	 * Set up the MIDI sequencer
	 * @param is
	 */
    private void initSequencer(Sequence seq) {
        try {
            sequencer = MidiSystem.getSequencer();
            if (sequencer == null) {
                throw new RuntimeException("Midi Sequencer unavailable");
            }
            if (!(sequencer instanceof Synthesizer)) {
                synthesizer = MidiSystem.getSynthesizer();
                synthesizer.open();
                Receiver synRx = synthesizer.getReceiver();
                Transmitter seqTx = sequencer.getTransmitter();
                seqTx.setReceiver(synRx);
                if (debug) {
                    new javax.swing.Timer(2000, new java.awt.event.ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            boolean needNL = false;
                            for (javax.sound.midi.MidiChannel c : synthesizer.getChannels()) {
                                if (c != null) {
                                    if (c.getMute()) {
                                        System.out.print(" m");
                                    } else {
                                        System.out.print(" " + ((c.getController(7) * 128) + c.getController(39)));
                                    }
                                    if (c.getSolo()) System.out.print("s");
                                    needNL = true;
                                }
                            }
                            if (needNL) System.out.println();
                            return;
                        }
                    }).start();
                }
            }
            sequencer.addMetaEventListener(new MetaEventListener() {

                public void meta(MetaMessage meta) {
                    if (meta.getType() == 47) {
                        sequencer.close();
                        if (synthesizer != null) {
                            synthesizer.close();
                        }
                    }
                }
            });
            sequencer.setSequence(seq);
        } catch (MidiUnavailableException e) {
            throw new RuntimeException("Midi default sequencer or synthesizer unavailable", e);
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException("Given data is not a MIDI sequence", e);
        }
    }

    /**
	 * Given a byte array that contains a MIDI sequence, read it in for playing.
	 * @param buf Byte array containing a MIDI sequence.
	 */
    public SequencePlayer(Sequence seq) {
        try {
            initSequencer(seq);
            sequencer.open();
        } catch (MidiUnavailableException e) {
            throw new RuntimeException("Failed to open MIDI sequencer", e);
        }
    }

    /**
	 * Start the MIDI playback
	 */
    public void play() {
        if (!sequencer.isRunning()) {
            sequencer.start();
        }
    }

    /**
	 * Pause the MIDI playback.
	 */
    public void pause() {
        if (sequencer.isRunning()) {
            try {
                sequencer.stop();
            } catch (IllegalStateException e) {
            }
        }
    }

    /**
	 * Set the position for playback.
	 * @param MIDI tick from the start of the MIDI sequence.
	 */
    public void move(long tick) {
        tick = Math.min(tick, length());
        sequencer.setTickPosition(tick);
    }

    /**
	 * Get the current playback position in MIDI ticks.
	 */
    public long position() {
        return sequencer.getTickPosition();
    }

    /**
	 * Indicates whether the sequence is currently playing. Be careful using this,
	 * the sequencer plays in a separate thread and stops when it hits the end,
	 * this can return true at the end of the sequence and the sequence will be
	 * be stopped by the time any action can be taken based on the return value.
	 * @return true if the Sequencer is running, otherwise false.
	 */
    public boolean isPlaying() {
        return sequencer.isRunning();
    }

    /**
	 * Returns the length of the MIDI sequence in MIDI ticks.
	 * @return length of the MIDI sequence in MIDI ticks.
	 */
    public long length() {
        return sequencer.getTickLength();
    }
}

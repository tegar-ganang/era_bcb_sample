package cadenza.player;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import cadenza.exception.CadenzaGeneralException;
import cadenza.player.exception.NoAvailableChannelsException;
import cadenza.player.exception.UnknownInstrumentException;

public class MidiPlayer {

    private static MidiPlayer instance;

    private MidiChannel[] midiChannels;

    private int[] reservedChannels;

    private Synthesizer synthesizer;

    private Instrument[] instruments;

    private MidiPlayer() {
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            midiChannels = synthesizer.getChannels();
            Soundbank sb = synthesizer.getDefaultSoundbank();
            if (sb != null) {
                instruments = sb.getInstruments();
            }
            reservedChannels = new int[midiChannels.length];
        } catch (MidiUnavailableException mue) {
            throw new CadenzaGeneralException();
        }
    }

    /**
	 * @return Instance of the class
	 */
    public static MidiPlayer getInstance() {
        if (instance == null) {
            instance = new MidiPlayer();
        }
        return instance;
    }

    /**
	 * Reserves a channel for a not so that no other note can interrupt the
	 * current
	 * 
	 * @return The reserved channel number
	 * @throws NoAvailableChannelsException
	 *             If there are no available channels
	 */
    public int getAvailableChannel() throws NoAvailableChannelsException {
        synchronized (reservedChannels) {
            for (int i = 0; i < reservedChannels.length; i++) {
                if (reservedChannels[i] == 0) {
                    reservedChannels[i] = 1;
                    return reservedChannels[i];
                }
            }
        }
        throw new NoAvailableChannelsException();
    }

    /**
	 * Plays a single note
	 * 
	 * @param instrumentName
	 *            The instrument to be played
	 * @param channel
	 *            The number of a MidiChannel where the note will be played
	 * @param number
	 *            The note number
	 * @param velocity
	 *            The note velocity
	 */
    public void playNote(String instrumentName, int channel, int number, int velocity) {
        try {
            midiChannels[channel].programChange(getInstrumentByName(instrumentName));
        } catch (UnknownInstrumentException uie) {
            throw new CadenzaGeneralException(uie);
        }
        midiChannels[channel].noteOn(number, velocity);
    }

    /**
	 * Stops the specified note in the specified channel
	 * 
	 * @param channel
	 *            Channel where the note is played
	 * @param number
	 *            Number of the played note
	 */
    public void stopNote(int channel, int number) {
        synchronized (reservedChannels) {
            reservedChannels[channel] = 0;
            midiChannels[channel].noteOff(number);
        }
    }

    private int getInstrumentByName(String name) throws UnknownInstrumentException {
        for (int i = 0; i < instruments.length; i++) {
            if (name.equals(instruments[i].getName())) return instruments[i].getPatch().getProgram();
        }
        throw new UnknownInstrumentException();
    }
}

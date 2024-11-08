package game.assets;

import java.io.IOException;
import java.net.URL;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

/**
 * MidiAsset is intended to provide a playable MIDI sequence clip that can
 * either be played once (repeatedly) or continuously looped.
 * <P>
 * Note: MIDI playback and control within the class is based on
 * that available within Davison's 'Killer Game Programming in Java'.
 *
 * @author <A HREF="mailto:P.Hanna@qub.ac.uk">Philip Hanna</A>
 * @version $Revision: 1 $ $Date: 2007/08 $
 */
public class MidiAsset extends Asset {

    /**
     * MIDI sequencer used to implement the MIDI sequence
     */
    protected static Sequencer sequencer;

    /**
     * MIDI synthesizer used to syntheise the sound
     */
    protected static Synthesizer synthesizer;

    /**
     * MIDI channels used to support the playback of the MIDI sequence.
     * The channels are stored to provide a ready means of modifying
     * the playback volume once playback has commenced
     */
    protected static MidiChannel[] channels;

    /**
     * MIDI sequence to be played.
     * <P>
     * Note: If the class is constructed using a URL reference, then
     * this variable will hold the MIDI sequence referenced by the URL
     */
    protected Sequence sequence;

    /**
     * URl location of the MIDI sequence to be loaded and played
     */
    protected URL midiURL;

    /**
     * Boolean flag determining if playback should be continuous
     */
    protected boolean continuallyPlay;

    /**
     * Current playback volume. Initially assigned a value of -1 to
     * signify that the default playback volume is to be used.
     */
    private int volume = -1;

    /**
     * Constructs a new MIDI asset instance by loading the MIDI sequence 
     * referred to in the specified URL.
     *
     * @param assetName the name to be assigned to this asset
     * @param midiURL URL holding the location of the MIDI sequence to be loaded
     * @param continuallyPlay boolean flag determining if playback is continuous
     * 
     * @exception NullPointerException if a null midiURL is specified
     */
    public MidiAsset(String assetName, URL midiURL, boolean continuallyPlay) {
        super(assetName);
        if (midiURL == null) {
            throw new NullPointerException("MidiAsset.constructor: " + "NULL midi URL specified.");
        }
        this.continuallyPlay = continuallyPlay;
        this.midiURL = midiURL;
        this.sequence = loadMidiSequence(midiURL);
    }

    /**
     * Constructs a new MIDI asset instance using the specified MIDI sequence
     * <P>
     * Note: If this constructor is used to build a MidiAsset instance then
     * it will not be possible to make use of the deepClone method (which
     * requires a URL link to the MIDI sequence)
     * 
     * @param assetName the name to be assigned to this asset
     * @param sequence MIDI sequence to be held inside this asset
     * @param continuallyPlay boolean flag determining if playback is continuous
     * 
     * @exception NullPointerException if a null midiURL is specified
     */
    public MidiAsset(String assetName, Sequence sequence, boolean continuallyPlay) {
        super(assetName);
        if (sequence == null) {
            throw new NullPointerException("MidiAsset.constructor: " + "NULL sequence URL specified.");
        }
        this.continuallyPlay = continuallyPlay;
        this.midiURL = null;
        this.sequence = sequence;
    }

    /**
     * Commence playback of the loaded MIDI sequence
     * 
     * @exception IllegalStateException if no MIDI sequence has been loaded or
     *            if the MIDI sequence cannot be played
     */
    public void play() {
        if (sequence == null) {
            throw new IllegalStateException("MidiAsset.play: " + "Midi sequence not loaded");
        }
        if (sequencer == null) {
            initialiseSequencer();
        }
        try {
            sequencer.setSequence(sequence);
            if (continuallyPlay) {
                sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            }
            sequencer.start();
            channels = synthesizer.getChannels();
            if (volume != -1) {
                setVolume(volume);
            }
        } catch (InvalidMidiDataException exception) {
            throw new IllegalStateException("MidiAsset.play: " + "Cannot play Midi sequence.");
        }
    }

    /**
     * Return the current maximum playback volume across all MIDI channels
     * 
     * @return int containing the current maximum playback volume
     */
    public int getVolume() {
        int maximumVolume = -1;
        if (channels != null) {
            int channelVolume;
            for (int idx = 0; idx < channels.length; idx++) {
                channelVolume = channels[idx].getController(7);
                if (channelVolume > maximumVolume) {
                    maximumVolume = channelVolume;
                }
            }
        }
        return maximumVolume;
    }

    /**
     * Set the playback volume of all MIDI channels to that specified
     * <P>
     * <B>Important:</B> The playback volume cannot be changed until
     * the synthesizer has been started. This is not an unreasonable
     * requirement, however, the request to start the synthesizer 
     * will take a few ms to complete (this will depend upon system
     * performance), with the initial Java request not blocking.
     * <P>
     * In other words, a line of code to set the volume immediately
     * following a line of code that start the synthesizer will likely
     * result in no change to volume as the synthesizer will not yet
     * have started. Generally, changing the volume in a different update
     * tick to that which starts playback will work.
     * <P>
     * Note: There may be a somewhat more elegant means of setting up
     * MIDI playback to avoid this problem - however, it is unknown
     * to me.
     */
    public void setVolume(int volume) {
        this.volume = volume;
        if (synthesizer != null) {
            for (int channelIdx = 0; channelIdx < channels.length; channelIdx++) {
                channels[channelIdx].controlChange(7, volume);
            }
        }
    }

    /**
     * Stop playback of the MIDI sequence.
     * <P>
     * Note: this method will not close the syntheiser, i.e. playback can be 
     * restarted if desired.
     */
    public void stop() {
        if (sequencer != null) {
            if (sequencer.isOpen() && sequencer.isRunning()) {
                sequencer.stop();
            }
        }
    }

    /**
     * Stop playback of the MIDI sequence and close the associated synthesizer
     */
    public void close() {
        if (sequencer != null) {
            if (sequencer.isOpen() && sequencer.isRunning()) {
                sequencer.stop();
            }
            sequencer.close();
            sequencer = null;
        }
        if (synthesizer != null) {
            if (synthesizer.isOpen()) {
                synthesizer.close();
            }
            synthesizer = null;
        }
    }

    /**
     * Load the MIDI sequence from the specified URL
     * 
     * @param sequenceName URL of the MIDI sequence to be loaded
     * @return Sequence loaded MIDI sequence
     * 
     * @exception IllegalArgumentException if the MIDI sequence
     *            cannot be loaded or is not supported
     */
    private Sequence loadMidiSequence(URL sequenceName) {
        Sequence sequence;
        try {
            sequence = MidiSystem.getSequence(sequenceName);
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException("MidiAsset.loadMidiSequence: " + "Unreadable or unsupported Midi file format " + sequenceName);
        } catch (IOException e) {
            throw new IllegalArgumentException("MidiAsset.loadMidiSequence: " + "IO Exception for " + sequenceName);
        }
        return sequence;
    }

    /**
     * Initialise the MIDI sequencer
     * 
     * @exception IllegalStateException if MIDI support is not available
     */
    private void initialiseSequencer() {
        try {
            MidiDevice.Info[] midiDeviceInfo = MidiSystem.getMidiDeviceInfo();
            int sequencerPos = -1;
            for (int idx = 0; idx < midiDeviceInfo.length; idx++) {
                if (midiDeviceInfo[idx].getName().indexOf("Sequencer") != -1) {
                    sequencerPos = idx;
                }
            }
            if (sequencerPos != -1) {
                sequencer = (Sequencer) MidiSystem.getMidiDevice(midiDeviceInfo[sequencerPos]);
            }
            if (sequencer == null) {
                throw new MidiUnavailableException();
            }
            sequencer.open();
            if (!(sequencer instanceof Synthesizer)) {
                synthesizer = MidiSystem.getSynthesizer();
                synthesizer.open();
                Receiver synthReceiver = synthesizer.getReceiver();
                Transmitter seqTransmitter = sequencer.getTransmitter();
                seqTransmitter.setReceiver(synthReceiver);
            } else {
                synthesizer = (Synthesizer) sequencer;
            }
        } catch (MidiUnavailableException e) {
            throw new IllegalStateException("MidiAsset.initialiseSequencer: " + "Cannot initialise sequencer");
        }
    }

    /**
     * Return a shallow clone of this MIDI asset clip.
     *
     * @return  new Asset instance containing a shallow clone of this instance
     */
    public Asset shallowClone() {
        MidiAsset clone = new MidiAsset(assetName, sequence, continuallyPlay);
        return clone;
    }

    /**
     * Return a deep clone of this MIDI asset clip.
     * <P>
     * Note: If this object was constructed without providing a URL to the MIDI
     * sequence then a deep clone of the object cannot be obtained and an 
     * exception will be generated.
     *
     * @return  new Asset instance containing a deep clone of this instance
     *
     * @exception IllegalStateException if it is not possible to obtain a deep 
     *            clone this asset
     */
    public Asset deepClone() {
        if (midiURL == null) {
            throw new IllegalStateException("MidiAsset.deepClone: " + "Object constructed without midi URL specified. " + "Deep clone not possible.");
        }
        MidiAsset clone = new MidiAsset(assetName, midiURL, continuallyPlay);
        return clone;
    }
}

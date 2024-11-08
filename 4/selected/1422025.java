package com.golden.gamedev.engine.audio;

import java.net.URL;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import com.golden.gamedev.engine.BaseAudioRenderer;

/**
 * Play midi sound (*.mid).
 * <p>
 * 
 * Note: Midi sound use soundbank that not delivered in JRE, only JDK can play
 * midi sound properly. <br>
 * In order to play midi sound properly in JRE you must explicitly install
 * soundbank. <br>
 * Download soundbank from java sun website (<a
 * href="http://java.sun.com/products/java-media/sound/soundbanks.html">
 * http://java.sun.com/products/java-media/sound/soundbanks.html</a>) and refer
 * to the manual how to install it.
 */
public class MidiRenderer extends BaseAudioRenderer implements MetaEventListener {

    private static final int MIDI_EOT_MESSAGE = 47;

    private static final int GAIN_CONTROLLER = 7;

    /** *************************** MIDI SEQUENCER ****************************** */
    private Sequencer sequencer;

    /** ************************************************************************* */
    private static boolean available;

    private static boolean volumeSupported;

    private static final int UNINITIALIZED = 0;

    private static final int INITIALIZING = 1;

    private static final int INITIALIZED = 2;

    private static int rendererStatus = MidiRenderer.UNINITIALIZED;

    /**
   * Creates new midi audio renderer.
   */
    public MidiRenderer() {
        if (MidiRenderer.rendererStatus == MidiRenderer.UNINITIALIZED) {
            MidiRenderer.rendererStatus = MidiRenderer.INITIALIZING;
            Thread thread = new Thread() {

                public final void run() {
                    try {
                        Sequencer sequencer = MidiSystem.getSequencer();
                        sequencer.open();
                        MidiRenderer.volumeSupported = (sequencer instanceof Synthesizer);
                        sequencer.close();
                        MidiRenderer.available = true;
                    } catch (Throwable e) {
                        System.err.println("WARNING: Midi audio playback is not available!");
                        MidiRenderer.available = false;
                    }
                    MidiRenderer.rendererStatus = MidiRenderer.INITIALIZED;
                }
            };
            thread.setDaemon(true);
            thread.start();
        }
    }

    public boolean isAvailable() {
        if (MidiRenderer.rendererStatus != MidiRenderer.INITIALIZED) {
            int i = 0;
            while (MidiRenderer.rendererStatus != MidiRenderer.INITIALIZED && i++ < 50) {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                }
            }
            if (MidiRenderer.rendererStatus != MidiRenderer.INITIALIZED) {
                MidiRenderer.rendererStatus = MidiRenderer.INITIALIZED;
                MidiRenderer.available = false;
            }
        }
        return MidiRenderer.available;
    }

    /** ************************************************************************* */
    protected void playSound(URL audiofile) {
        try {
            if (this.sequencer == null) {
                this.sequencer = MidiSystem.getSequencer();
                if (!this.sequencer.isOpen()) {
                    this.sequencer.open();
                }
            }
            Sequence seq = MidiSystem.getSequence(this.getAudioFile());
            this.sequencer.setSequence(seq);
            this.sequencer.start();
            this.sequencer.addMetaEventListener(MidiRenderer.this);
            if (this.volume != 1.0f) {
                this.setSoundVolume(this.volume);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.status = BaseAudioRenderer.ERROR;
        }
    }

    protected void replaySound(URL audiofile) {
        this.sequencer.start();
        this.sequencer.addMetaEventListener(this);
    }

    protected void stopSound() {
        this.sequencer.stop();
        this.sequencer.setMicrosecondPosition(0);
        this.sequencer.removeMetaEventListener(this);
    }

    /**
   * Notified when the sound has finished playing.
   */
    public void meta(MetaMessage msg) {
        if (msg.getType() == MidiRenderer.MIDI_EOT_MESSAGE) {
            this.status = BaseAudioRenderer.END_OF_SOUND;
            this.sequencer.setMicrosecondPosition(0);
            this.sequencer.removeMetaEventListener(this);
        }
    }

    /** ************************************************************************* */
    protected void setSoundVolume(float volume) {
        if (this.sequencer == null) {
            return;
        }
        MidiChannel[] channels = ((Synthesizer) this.sequencer).getChannels();
        for (int i = 0; i < channels.length; i++) {
            channels[i].controlChange(MidiRenderer.GAIN_CONTROLLER, (int) (volume * 127));
        }
    }

    public boolean isVolumeSupported() {
        return MidiRenderer.volumeSupported;
    }
}

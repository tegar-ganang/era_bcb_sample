package spaceopera.gui.window.helper;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import spaceopera.gui.SpaceOpera;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.net.URL;

/** The PlayEffect class is used to play sound effects in its own thread
 * x
 */
public class PlayEffect implements Runnable, LineListener, MetaEventListener {

    private SpaceOpera spaceOpera;

    private Sequencer sequencer;

    private Thread thread;

    private boolean midiEnd, audioEnd;

    private Synthesizer synthesizer;

    private MidiChannel channels[];

    private Object soundObject;

    private void close() {
        if (sequencer != null) {
            sequencer.close();
            sequencer = null;
        }
    }

    private boolean loadSound(Object o) {
        if (o instanceof URL) {
            try {
                soundObject = AudioSystem.getAudioInputStream((URL) o);
            } catch (Exception e) {
                try {
                    soundObject = MidiSystem.getSequence((URL) o);
                } catch (InvalidMidiDataException e2) {
                    System.out.println("Unsupported audio file.");
                    return false;
                } catch (Exception e3) {
                    e3.printStackTrace();
                    soundObject = null;
                    return false;
                }
            }
        } else if (o instanceof File) {
            try {
                soundObject = AudioSystem.getAudioInputStream((File) o);
            } catch (Exception e1) {
                try {
                    FileInputStream fis = new FileInputStream((File) o);
                    soundObject = new BufferedInputStream(fis, 1024);
                } catch (Exception e2) {
                    e2.printStackTrace();
                    soundObject = null;
                    return false;
                }
            }
        }
        if (sequencer == null) {
            soundObject = null;
            return false;
        }
        if (soundObject instanceof AudioInputStream) {
            try {
                AudioInputStream stream = (AudioInputStream) soundObject;
                AudioFormat format = stream.getFormat();
                if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                    AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                    stream = AudioSystem.getAudioInputStream(tmp, stream);
                    format = tmp;
                }
                DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.addLineListener(this);
                clip.open(stream);
                soundObject = clip;
            } catch (Exception ex) {
                ex.printStackTrace();
                soundObject = null;
                return false;
            }
        } else if (soundObject instanceof Sequence || soundObject instanceof BufferedInputStream) {
            try {
                sequencer.open();
                if (soundObject instanceof Sequence) {
                    sequencer.setSequence((Sequence) soundObject);
                } else {
                    sequencer.setSequence((BufferedInputStream) soundObject);
                }
            } catch (InvalidMidiDataException e2) {
                System.out.println("Unsupported audio file.");
                soundObject = null;
                return false;
            } catch (Exception e3) {
                e3.printStackTrace();
                soundObject = null;
                return false;
            }
        }
        return true;
    }

    public static void main(String args[]) {
        String sound = "effects/drum1.wav";
        PlayEffect ps = new PlayEffect(null);
    }

    public void meta(MetaMessage message) {
        if (message.getType() == 47) {
            midiEnd = true;
        }
    }

    private void open() {
        try {
            sequencer = MidiSystem.getSequencer();
            if (sequencer instanceof Synthesizer) {
                synthesizer = (Synthesizer) sequencer;
                channels = synthesizer.getChannels();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        sequencer.addMetaEventListener(this);
    }

    public PlayEffect(SpaceOpera so) {
        spaceOpera = so;
        start();
    }

    public void play(String effect) {
        open();
        File file = new File(effect);
        if (file != null) {
            if (loadSound(file) == true) {
                midiEnd = audioEnd = false;
                if (soundObject instanceof Sequence || soundObject instanceof BufferedInputStream) {
                    sequencer.start();
                    while (!midiEnd && thread != null) {
                        try {
                            Thread.sleep(99);
                        } catch (Exception e) {
                        }
                    }
                    sequencer.stop();
                    sequencer.close();
                } else if (soundObject instanceof Clip && thread != null) {
                    Clip clip = (Clip) soundObject;
                    clip.start();
                    try {
                        Thread.sleep(99);
                    } catch (Exception e) {
                    }
                    while ((clip.isActive()) && thread != null) {
                        try {
                            Thread.sleep(99);
                        } catch (Exception e) {
                        }
                    }
                    clip.stop();
                    clip.close();
                }
                soundObject = null;
            }
        }
        close();
    }

    public void run() {
    }

    public void start() {
        thread = new Thread(this);
        thread.setName("SpaceOpera Sound Effects");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }
        thread = null;
    }

    public Thread getThread() {
        return thread;
    }

    public void update(LineEvent event) {
        if (event.getType() == LineEvent.Type.STOP) {
            audioEnd = true;
        }
    }
}

package org.gerhardb.lib.sound;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

/**
 EZ Sound Found
 Inspired by Sun's demo sound program Juke.java written by Brian Lichtenwalter.
 */
public class SoundPlayer implements LineListener, MetaEventListener {

    Sequencer mySequencer;

    boolean myMidiEomFlag, myAudioEomFlag;

    double myDuration;

    public boolean myBump;

    public boolean iAmPaused = false;

    public SoundPlayer() {
        try {
            this.mySequencer = MidiSystem.getSequencer();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        this.mySequencer.addMetaEventListener(this);
    }

    public void close() {
        if (this.mySequencer.isRunning()) {
            this.mySequencer.stop();
        }
        this.mySequencer.removeMetaEventListener(this);
        this.mySequencer.close();
        this.mySequencer = null;
    }

    public void play(File file) {
        Object playThis = null;
        try {
            playThis = AudioSystem.getAudioInputStream(file);
        } catch (Exception e1) {
            try {
                FileInputStream is = new FileInputStream(file);
                playThis = new BufferedInputStream(is, 1024);
            } catch (Exception e3) {
                e3.printStackTrace();
                return;
            }
        }
        Object sound = loadSound(playThis);
        if (sound != null) {
            playSound(sound);
        }
    }

    public void play(URL url) {
        Object playThis = null;
        try {
            playThis = AudioSystem.getAudioInputStream(url);
        } catch (Exception e) {
            try {
                playThis = MidiSystem.getSequence(url);
            } catch (InvalidMidiDataException imde) {
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        }
        Object sound = loadSound(playThis);
        if (sound != null) {
            playSound(sound);
        }
    }

    @Override
    public void update(LineEvent event) {
        if (event.getType() == LineEvent.Type.STOP && !this.iAmPaused) {
            this.myAudioEomFlag = true;
        }
    }

    @Override
    public void meta(MetaMessage message) {
        if (message.getType() == 47) {
            this.myMidiEomFlag = true;
        }
    }

    private Object loadSound(Object objSound) {
        Object rtnMe = null;
        this.myDuration = 0.0;
        if (objSound instanceof AudioInputStream) {
            try {
                AudioInputStream stream = (AudioInputStream) objSound;
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
                rtnMe = clip;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        } else if (objSound instanceof Sequence || objSound instanceof BufferedInputStream) {
            rtnMe = objSound;
            try {
                this.mySequencer.open();
                if (objSound instanceof Sequence) {
                    this.mySequencer.setSequence((Sequence) objSound);
                } else {
                    this.mySequencer.setSequence((BufferedInputStream) objSound);
                }
            } catch (InvalidMidiDataException imde) {
                System.out.println("Unsupported audio file.");
                return null;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        this.myDuration = getDuration(rtnMe);
        return rtnMe;
    }

    private void playSound(Object objSound) {
        this.myMidiEomFlag = this.myAudioEomFlag = this.myBump = false;
        if (objSound instanceof Sequence || objSound instanceof BufferedInputStream) {
            this.mySequencer.start();
            while (!this.myMidiEomFlag && !this.myBump) {
                try {
                    Thread.sleep(99);
                } catch (Exception e) {
                    break;
                }
            }
            this.mySequencer.stop();
            this.mySequencer.close();
        } else if (objSound instanceof Clip) {
            Clip clip = (Clip) objSound;
            clip.start();
            try {
                Thread.sleep(99);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            while ((this.iAmPaused || clip.isActive()) && !this.myBump) {
                try {
                    Thread.sleep(99);
                } catch (Exception e) {
                    break;
                }
            }
            clip.stop();
            clip.close();
        }
        objSound = null;
    }

    private double getDuration(Object objSound) {
        double duration = 0.0;
        if (objSound instanceof Sequence) {
            duration = ((Sequence) objSound).getMicrosecondLength() / 1000000.0;
        } else if (objSound instanceof BufferedInputStream) {
            duration = this.mySequencer.getMicrosecondLength() / 1000000.0;
        } else if (objSound instanceof Clip) {
            Clip clip = (Clip) objSound;
            duration = clip.getBufferSize() / (clip.getFormat().getFrameSize() * clip.getFormat().getFrameRate());
        }
        return duration;
    }

    public static void main(String args[]) {
        String fileName = "D:/Kids/A1.wav";
        if (args.length > 0) {
            fileName = args[0];
        }
        SoundPlayer s = new SoundPlayer();
        s.play(new File(fileName));
        System.out.println("---------------");
        s.play(new File("D:/Kids/A11.wav"));
        s.close();
        System.out.println("Main exiting normally");
    }
}

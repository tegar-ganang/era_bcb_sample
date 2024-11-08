package com.cirnoworks.cas.impl.jsa;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import odk.lang.FastMath;
import com.cirnoworks.cas.BGMStatus;

/**
 * @author cloudee
 * 
 */
public class JSAPlayer implements Runnable {

    private static final int MODE_STOP = 0;

    private static final int MODE_SAMPLE = 1;

    private static final int MODE_MIDI = 2;

    private final SourceDataLine line;

    private final Object lock = new Object();

    private final BGMStatus status = new BGMStatus();

    private int mode;

    private boolean shutdown;

    private boolean midi;

    private InputStream is;

    private long pos;

    private int loop;

    private float volume;

    private Thread thread;

    private FloatControl volumeControl;

    int size = 0;

    int cap = 2097152;

    byte[] buf = new byte[cap];

    byte[] rbuf = new byte[65536];

    public JSAPlayer() throws LineUnavailableException {
        System.out.println("JSAPlayer");
        Mixer mixer = AudioSystem.getMixer(null);
        System.out.println(mixer.getMixerInfo());
        Line.Info[] lis = mixer.getSourceLineInfo();
        SourceDataLine line = null;
        for (Line.Info li : lis) {
            if (li.getLineClass() == SourceDataLine.class) {
                line = (SourceDataLine) mixer.getLine(li);
                System.out.println(mixer.getMaxLines(li));
                break;
            }
        }
        if (line == null) {
            throw new RuntimeException("Can't open sound!");
        }
        line.open(JSASoundManager.format);
        line.start();
        this.line = line;
        volumeControl = (FloatControl) line.getControl(Type.MASTER_GAIN);
        setVolume(1);
    }

    protected void finalize() {
        System.out.println("JSAPlayer finalize...");
        line.close();
    }

    public void setVolume(float volume) {
        this.volume = volume;
        try {
            float gain = (float) FastMath.log10(volume) * 20;
            if (gain < volumeControl.getMinimum()) {
                gain = volumeControl.getMinimum();
            } else if (gain > volumeControl.getMaximum()) {
                gain = volumeControl.getMaximum();
            }
            volumeControl.setValue(gain);
        } catch (Exception e) {
        }
    }

    public void stop() {
        synchronized (lock) {
            shutdown = true;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    public void play(String name, InputStream is, boolean midi, int loop, long pos) {
        stop();
        if (this.is != null) {
            try {
                this.is.close();
            } catch (Exception e) {
            }
        }
        synchronized (lock) {
            while (mode > 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        this.is = is;
        this.loop = loop;
        this.midi = midi;
        this.pos = pos;
        synchronized (lock) {
            mode = midi ? MODE_MIDI : MODE_SAMPLE;
            shutdown = false;
            status.setName(name);
            status.setPos(pos);
            status.setVolume(volume);
            status.setLoopCount(1);
            status.setLoopTotal(loop);
            lock.notifyAll();
        }
        thread = new Thread(this, "JSA BG Player working thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void run() {
        try {
            int read;
            try {
                while ((read = is.read(rbuf)) >= 0) {
                    if (shutdown) {
                        return;
                    }
                    if (read == 0) {
                        Thread.yield();
                        continue;
                    }
                    if (size + read > cap) {
                        cap *= 2;
                        byte[] nbuf = new byte[cap];
                        System.arraycopy(buf, 0, nbuf, 0, size);
                        buf = nbuf;
                    }
                    System.arraycopy(rbuf, 0, buf, size, read);
                    size += read;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
            if (midi) {
                playMidi(buf, size);
            } else {
                playSample(buf, size);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            size = 0;
            synchronized (lock) {
                mode = MODE_STOP;
                status.setName(null);
                status.setPos(0);
                status.setLoopCount(0);
                lock.notifyAll();
            }
        }
    }

    private void playMidi(byte[] content, int size) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        Sequencer seq = MidiSystem.getSequencer();
        int loop = this.loop;
        if (loop < 0) {
            loop = Sequencer.LOOP_CONTINUOUSLY;
        }
        if (seq == null) {
            throw new MidiUnavailableException();
        }
        seq.open();
        Sequence midi = MidiSystem.getSequence(new ByteArrayInputStream(content, 0, size));
        seq.setSequence(midi);
        seq.setLoopCount(loop);
        seq.start();
        seq.setMicrosecondPosition(pos);
        float lastVolume = -1;
        while (seq.isRunning()) {
            synchronized (lock) {
                status.setPos(seq.getMicrosecondPosition());
                status.setLoopCount(seq.getLoopCount());
            }
            if (shutdown) {
                seq.close();
            }
            try {
                Thread.sleep(8);
            } catch (InterruptedException e) {
            }
        }
    }

    private void playSample(byte[] content, int size) throws UnsupportedAudioFileException, IOException {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        setVolume(volume);
        byte[] abuf = new byte[JSASoundManager.format.getFrameSize() * 16];
        AudioInputStream ais = null;
        try {
            for (int i = 0, max = loop >= 0 ? loop : Integer.MAX_VALUE; i < max; i++) {
                InputStream bis = new ByteArrayInputStream(content, 0, size);
                if (ais != null) {
                    ais.close();
                }
                ais = AudioSystem.getAudioInputStream(JSASoundManager.format, AudioSystem.getAudioInputStream(bis));
                int cf = 0;
                int aread;
                while ((aread = ais.read(abuf)) >= 0) {
                    if (shutdown) {
                        line.flush();
                        return;
                    }
                    if (aread == 0) {
                        continue;
                    }
                    while (line.available() < aread) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                        }
                        if (shutdown) {
                            line.flush();
                            return;
                        }
                    }
                    line.write(abuf, 0, aread);
                    cf += aread / JSASoundManager.format.getFrameSize();
                    synchronized (lock) {
                        status.setPos((long) (1000f * cf / JSASoundManager.format.getFrameRate()));
                        status.setLoopCount(i + 1);
                    }
                }
            }
        } finally {
            try {
                ais.close();
            } catch (Exception e) {
            }
        }
    }

    public BGMStatus getStatus() {
        return status;
    }
}

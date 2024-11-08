package com.cirnoworks.cas.impl.jsa;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import odk.lang.FastMath;

/**
 * @author cloudee
 * 
 */
public class JSASEPlayer {

    class SEData {

        byte[] data;

        int pos;

        int percent;

        FloatControl volumeControl;

        SourceDataLine line;

        float volume = 0.0f / 0.0f;

        public void setVolume(float volume) {
            if (this.volume != volume) {
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
        }
    }

    private final SEData[] seData;

    private final AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);

    private boolean shutdown;

    private Thread thread;

    private int frameSize;

    int size = 0;

    int cap = 2097152;

    byte[] buf = new byte[cap];

    byte[] rbuf = new byte[65536];

    public JSASEPlayer(int lineCount) throws LineUnavailableException {
        seData = new SEData[lineCount];
        frameSize = JSASoundManager.format.getFrameSize();
        for (int i = 0; i < lineCount; i++) {
            Mixer.Info[] infos = AudioSystem.getMixerInfo();
            for (Mixer.Info li : infos) {
                System.out.println(li.toString());
                Mixer mixer = AudioSystem.getMixer(li);
                Line.Info[] lis = mixer.getSourceLineInfo();
                SourceDataLine line = null;
                for (Line.Info lii : lis) {
                    if (lii.getLineClass() == SourceDataLine.class) {
                        line = AudioSystem.getSourceDataLine(format);
                        System.out.println(mixer.getMaxLines(lii));
                        break;
                    }
                }
            }
            Mixer mixer = AudioSystem.getMixer(null);
            System.out.println(mixer.getMixerInfo());
            Line.Info[] lis = mixer.getSourceLineInfo();
            SourceDataLine line = null;
            for (Line.Info li : lis) {
                if (li.getLineClass() == SourceDataLine.class) {
                    line = AudioSystem.getSourceDataLine(format);
                    System.out.println(mixer.getMaxLines(li));
                    break;
                }
            }
            if (line == null) {
                throw new RuntimeException("Can't open sound!");
            }
            line.open(format);
            line.start();
            FloatControl volumeControl = (FloatControl) line.getControl(Type.MASTER_GAIN);
            SEData data = new SEData();
            data.volumeControl = volumeControl;
            data.line = line;
            data.setVolume(1);
            seData[i] = data;
        }
        thread = new WorkingThread(seData);
        thread.setDaemon(true);
        thread.start();
    }

    class WorkingThread extends Thread {

        private final SEData[] data;

        public WorkingThread(SEData[] data) {
            super("JSASEPlayer working thread");
            this.data = data;
        }

        public void run() {
            byte[] data;
            int pos;
            SourceDataLine line;
            SEData sed;
            while (true) {
                for (int i = 0, max = this.data.length; i < max; i++) {
                    synchronized (seData) {
                        if (seData[i].data == null || seData[i].line.available() <= 0) {
                            continue;
                        }
                        data = seData[i].data;
                        pos = seData[i].pos;
                        line = seData[i].line;
                        sed = seData[i];
                    }
                    int dataLeft = data.length - pos;
                    int lineLeft = line.available();
                    int left = dataLeft < lineLeft ? dataLeft : lineLeft;
                    left = left / frameSize * frameSize;
                    if (left > 0) {
                        long t0 = System.nanoTime();
                        left = line.write(data, pos, left);
                        long t1 = System.nanoTime();
                        if (t1 - t0 > 1000000) {
                            System.err.println("push " + left + " bytes in " + (t1 - t0) / 1000000f + "ms la=" + lineLeft);
                        }
                    }
                    synchronized (seData) {
                        pos += left;
                        if (pos >= sed.data.length) {
                            sed.data = null;
                            sed.pos = 0;
                            sed.percent = 0;
                        } else {
                            sed.pos = pos;
                            sed.percent = pos * 100 / data.length;
                        }
                        if (shutdown) {
                            return;
                        }
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void play(byte[] content) {
        long t0 = System.nanoTime();
        synchronized (seData) {
            int maxPos = 0;
            int blankPos = -1;
            int selfPos = -1;
            for (int i = 0, max = seData.length; i < max; i++) {
                if (seData[i].data == null) {
                    blankPos = i;
                    break;
                }
                if (seData[i].data == content) {
                    if (selfPos >= 0) {
                        if (seData[i].pos > seData[selfPos].pos) {
                            selfPos = i;
                        }
                    } else {
                        selfPos = i;
                    }
                }
                if (seData[i].percent > seData[maxPos].percent) {
                    maxPos = i;
                }
            }
            if (blankPos >= 0) {
                maxPos = blankPos;
            } else if (selfPos >= 0) {
                maxPos = selfPos;
            } else {
            }
            seData[maxPos].line.flush();
            seData[maxPos].data = content;
            seData[maxPos].pos = 0;
        }
        long t1 = System.nanoTime();
        float t = (t1 - t0) / 1000000f;
        if (t > 1) {
            System.err.println("Player time " + t + "ms");
        }
    }

    protected void finalize() {
        System.out.println("JSAPlayer finalize...");
        for (int i = 0, max = seData.length; i < max; i++) {
            seData[i].line.close();
        }
    }

    public void stop() {
        synchronized (seData) {
            shutdown = true;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    public void setVolume(float volume) {
        synchronized (seData) {
            for (int i = 0, max = seData.length; i < max; i++) {
                seData[i].setVolume(volume);
            }
        }
    }

    public int getChannels() {
        return seData.length;
    }

    public void destroy() {
        synchronized (seData) {
            shutdown = true;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }
}

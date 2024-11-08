package org.progeeks.audio;

import java.util.*;
import javax.sound.sampled.*;

/**
 *  Manages a data input line (TargetDataLine) and provides
 *  SoundInput for the channels that it supports.
 *
 *  @version   $Revision: 1.6 $
 *  @author    Paul Speed
 */
public class LineInputThread extends Thread {

    private static final int SOUND_LEFT = 0;

    private static final int SOUND_RIGHT = 1;

    private String name;

    private TargetDataLine line;

    private AudioFormat audioFormat;

    private AudioFormat channelFormat;

    private int lineBufferSize = 8192;

    private SoundInput[] soundInputs = new SoundInput[2];

    private int toneChannel = 0;

    private ToneGenerator tone;

    public LineInputThread(String name, TargetDataLine line) {
        this(name, line, 8192);
    }

    public LineInputThread(String name, TargetDataLine line, int bufferSize) {
        super(name);
        this.name = name;
        this.line = line;
        this.lineBufferSize = bufferSize;
        setPriority(MAX_PRIORITY);
        soundInputs[SOUND_LEFT] = new SoundInput(name + "(left)");
        soundInputs[SOUND_RIGHT] = new SoundInput(name + "(right)");
    }

    public void setAudioFormat(AudioFormat audioFormat) throws LineUnavailableException {
        if (audioFormat.getChannels() != 2) throw new IllegalArgumentException("Only support two channel formats.");
        this.audioFormat = audioFormat;
        line.open(audioFormat, lineBufferSize);
        channelFormat = new AudioFormat(audioFormat.getEncoding(), audioFormat.getSampleRate(), audioFormat.getSampleSizeInBits(), 1, audioFormat.getFrameSize() / 2, audioFormat.getFrameRate(), audioFormat.isBigEndian());
        tone = new ToneGenerator(channelFormat.getFrameRate(), 440, 16384);
    }

    public AudioFormat getAudioFormat() {
        return (audioFormat);
    }

    public void setPlayTone(int channel, boolean b) {
        int channelFlag = (int) Math.pow(2, channel);
        if (b) {
            toneChannel = toneChannel | channelFlag;
        } else {
            toneChannel = toneChannel & (~channelFlag);
        }
    }

    public boolean getPlayTone(int channel) {
        int channelFlag = (int) Math.pow(2, channel);
        return ((toneChannel & channelFlag) != 0);
    }

    public List getSoundInputs() {
        return (Arrays.asList(soundInputs));
    }

    public void run() {
        line.start();
        byte[] buffer = new byte[lineBufferSize];
        int maxSize = buffer.length;
        int halfSize = maxSize / 2;
        int synchId = 0;
        while (true) {
            int bytesRead = line.read(buffer, 0, maxSize);
            if (bytesRead < buffer.length) System.out.println("underrun.");
            boolean toneLeft = getPlayTone(SOUND_LEFT);
            boolean toneRight = getPlayTone(SOUND_RIGHT);
            byte[] leftBuffer = new byte[halfSize];
            byte[] rightBuffer = new byte[halfSize];
            int frame = 0;
            for (int i = 0; i < bytesRead; i += 4, frame += 2) {
                int sin = 0;
                if (toneLeft || toneRight) sin = tone.nextValue();
                if (!toneLeft) {
                    leftBuffer[frame] = buffer[i];
                    leftBuffer[frame + 1] = buffer[i + 1];
                } else {
                    leftBuffer[frame] = (byte) (sin & 0xff);
                    leftBuffer[frame + 1] = (byte) ((sin >> 8) & 0xff);
                }
                if (!toneRight) {
                    rightBuffer[frame] = buffer[i + 2];
                    rightBuffer[frame + 1] = buffer[i + 3];
                } else {
                    rightBuffer[frame] = (byte) (sin & 0xff);
                    rightBuffer[frame + 1] = (byte) ((sin >> 8) & 0xff);
                }
            }
            soundInputs[SOUND_LEFT].dispatchSoundData(leftBuffer, frame, channelFormat, synchId);
            soundInputs[SOUND_RIGHT].dispatchSoundData(rightBuffer, frame, channelFormat, synchId);
            synchId++;
        }
    }

    public static List getInputMixers(Line.Info lineInfo) {
        ArrayList results = new ArrayList();
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        for (int i = 0; i < infos.length; i++) {
            Mixer mixer = AudioSystem.getMixer(infos[i]);
            System.out.println("Mixer[" + i + "] = " + infos[i].getName());
            Line.Info[] targets = mixer.getTargetLineInfo();
            for (int j = 0; j < targets.length; j++) {
                System.out.println("    targetLine[" + j + "] = " + targets[j]);
            }
            Line.Info[] sources = mixer.getSourceLineInfo();
            for (int j = 0; j < sources.length; j++) {
                System.out.println("    sourceLine[" + j + "] = " + sources[j]);
            }
            if (mixer.isLineSupported(lineInfo)) results.add(mixer);
        }
        return (results);
    }

    public static void main(String[] args) {
        List threads = new ArrayList();
        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 2, 4, 16000, false);
        Line.Info lineInfo = new Line.Info(TargetDataLine.class);
        List mixers = getInputMixers(lineInfo);
        for (Iterator i = mixers.iterator(); i.hasNext(); ) {
            Mixer m = (Mixer) i.next();
            Mixer.Info info = m.getMixerInfo();
            System.out.println("Input Mixer:" + info.getName() + " - " + info.getDescription());
            int count = m.getMaxLines(lineInfo);
            System.out.println("  max lines:" + (count == AudioSystem.NOT_SPECIFIED ? "Unspecified" : "" + count));
            try {
                TargetDataLine line = (TargetDataLine) m.getLine(lineInfo);
                LineInputThread thread = new LineInputThread(info.getName(), line);
                thread.setAudioFormat(audioFormat);
                threads.add(thread);
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
        }
        for (Iterator i = threads.iterator(); i.hasNext(); ) {
            LineInputThread t = (LineInputThread) i.next();
            t.start();
        }
    }
}

package org.progeeks.audio;

import java.util.*;
import javax.sound.sampled.*;

/**
 *  Manages a data output line (SourceDataLine) and can
 *  provide SoundOutputs for each of its channels.
 *
 *  @version   $Revision: 1.7 $
 *  @author    Paul Speed
 */
public class LineOutputThread extends Thread {

    public static final int SOUND_LEFT = 0;

    public static final int SOUND_RIGHT = 1;

    private String name;

    private SourceDataLine line;

    private AudioFormat audioFormat;

    private AudioFormat channelFormat;

    private int lineBufferSize = 8192;

    private List leftOutputs = new ArrayList();

    private SoundOutput[] leftArray;

    private List rightOutputs = new ArrayList();

    private SoundOutput[] rightArray;

    private int toneChannel = 0;

    private ToneGenerator tone;

    public LineOutputThread(String name, SourceDataLine line) {
        this(name, line, 8192);
    }

    public LineOutputThread(String name, SourceDataLine line, int bufferSize) {
        super(name);
        this.name = name;
        this.line = line;
        this.lineBufferSize = bufferSize;
        setPriority(MAX_PRIORITY);
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

    /**
     *  Returns a sound output object for the specified channel which
     *  will normally be SOUND_LEFT or SOUND_RIGHT.
     */
    public SoundOutput getSoundOutput(int channel) {
        SoundOutput result = new SoundOutput(name + (channel == SOUND_LEFT ? "(left)" : "(right)"));
        if (channel == SOUND_LEFT) {
            leftOutputs.add(result);
            resetLeftArray();
        } else if (channel == SOUND_RIGHT) {
            rightOutputs.add(result);
            resetRightArray();
        } else {
            throw new IllegalArgumentException("Channel not defined:" + channel);
        }
        return (result);
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

    protected void resetLeftArray() {
        SoundOutput[] newArray = new SoundOutput[leftOutputs.size()];
        leftArray = (SoundOutput[]) leftOutputs.toArray(newArray);
    }

    protected void resetRightArray() {
        SoundOutput[] newArray = new SoundOutput[rightOutputs.size()];
        rightArray = (SoundOutput[]) rightOutputs.toArray(newArray);
    }

    protected void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        line.start();
        byte[] buffer = new byte[lineBufferSize];
        int lastDrift = 0;
        while (true) {
            if (toneChannel != 0) {
                buffer = new byte[lineBufferSize];
                if (leftArray != null) leftArray[0].drainEvent();
                if (rightArray != null) rightArray[0].drainEvent();
                for (int i = 0; i < lineBufferSize; ) {
                    int sin = tone.nextValue();
                    byte low = (byte) (sin & 0xff);
                    byte hi = (byte) ((sin >> 8) & 0xff);
                    buffer[i++] = low;
                    buffer[i++] = hi;
                    buffer[i++] = low;
                    buffer[i++] = hi;
                }
                int written = line.write(buffer, 0, buffer.length);
                if (written < buffer.length) System.out.println("write underrun:" + written + " out of:" + buffer.length);
                continue;
            }
            if (leftArray == null && rightArray == null) {
                safeSleep(10);
                continue;
            }
            try {
                buffer = new byte[lineBufferSize];
                SoundEvent leftEvent = null;
                if (leftArray != null) {
                    leftEvent = leftArray[0].getNextEvent();
                }
                SoundEvent rightEvent = null;
                if (rightArray != null) {
                    rightEvent = rightArray[0].getNextEvent();
                }
                if (leftEvent != null && rightEvent != null) {
                    int drift = leftEvent.getSynchId() - rightEvent.getSynchId();
                    if (drift != lastDrift) {
                        System.out.println("Drift:" + drift);
                        lastDrift = drift;
                    }
                }
                if (leftEvent != null) {
                    byte[] left = leftEvent.getBuffer();
                    int frameStart = 0;
                    for (int i = 0; i < left.length; i += 2, frameStart += 4) {
                        buffer[frameStart] = left[i];
                        buffer[frameStart + 1] = left[i + 1];
                    }
                }
                if (rightEvent != null) {
                    byte[] right = rightEvent.getBuffer();
                    int frameStart = 2;
                    for (int i = 0; i < right.length; i += 2, frameStart += 4) {
                        buffer[frameStart] = right[i];
                        buffer[frameStart + 1] = right[i + 1];
                    }
                }
                line.write(buffer, 0, buffer.length);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

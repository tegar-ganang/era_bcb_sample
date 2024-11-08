package com.frinika.benchmark.audio;

import javax.sound.sampled.TargetDataLine;
import com.frinika.benchmark.SyncVoice;
import com.frinika.global.FrinikaConfig;
import com.frinika.sequencer.FrinikaSequencer;
import com.frinika.sequencer.gui.AudioDeviceHandle;

/**
 * Use for capturing inputs and for direct monitoring. Based on Peters Sampler
 * code.
 * 
 * @author pjl
 * @author Peter Johan Salomonsen
 * 
 */
public class ClickListener extends SyncVoice {

    TargetDataLine lineIn = null;

    byte[] inBuffer;

    FrinikaSequencer sequencer;

    static int OFF = 0;

    static int ARMED = 1;

    static int RECORDING = 2;

    AudioDeviceHandle audioDeviceHandle;

    int state = OFF;

    int nChannel = 0;

    long framePtrLast = 0;

    float thresh;

    boolean monit;

    private ClickOscillator out;

    public ClickListener(FrinikaSequencer seq, boolean monit, ClickOscillator out, float thresh) {
        super(seq);
        this.monit = monit;
        this.out = out;
        this.thresh = thresh;
    }

    public void setAudioDeviceHandle(AudioDeviceHandle audio) {
        audioDeviceHandle = audio;
        nChannel = audioDeviceHandle.getChannels();
        System.out.println(audioDeviceHandle.toString() + "   " + nChannel);
    }

    public void fillBuffer(int startBufferPos, int endBufferPos, float[] buffer) {
        try {
            if (lineIn == null) {
                lineIn = audioDeviceHandle.getOpenLine();
                lineIn.start();
            }
            if (inBuffer == null || inBuffer.length != buffer.length * nChannel) inBuffer = new byte[buffer.length * nChannel];
            if (glitched) doSync(0);
            int nByte = nChannel * (endBufferPos - startBufferPos);
            if (lineIn.available() >= nByte) {
                int nread;
                int cnt = 0;
                do {
                    nread = lineIn.read(inBuffer, 0, nByte);
                    cnt++;
                } while (lineIn.available() > 2 * nByte);
                if (cnt != 1) {
                    System.err.println(" LineIn OVER RUN ");
                    glitched = true;
                }
                for (int n = 0; n < nByte / 2; n++) {
                    short sample = (short) ((0xff & inBuffer[2 * n + 1]) + ((0xff & inBuffer[2 * n]) * 256));
                    float val = sample / 32768f;
                    if (n % nChannel == 0) framePtr++;
                    if (val > thresh) {
                        long nF = (framePtr - out.clickFrame);
                        double t = nF / FrinikaConfig.sampleRate;
                        System.out.println(" latency in frames " + nF + " [ " + t + " secs]");
                    }
                    if (monit) {
                        if (nChannel == 1) {
                            buffer[startBufferPos + 2 * n] += val;
                            buffer[startBufferPos + 2 * n + 1] += val;
                        } else {
                            buffer[startBufferPos + n] += val;
                        }
                    }
                }
            } else {
                System.err.println(" LineIn UNDER RUN ");
                glitched = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        framePtrLast = framePtr;
    }

    @Override
    protected void correctGlitch() {
    }
}

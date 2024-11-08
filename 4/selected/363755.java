package com.frinika.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;
import uk.org.toot.audio.mixer.AudioMixer;
import com.frinika.sequencer.FrinikaSequence;
import com.frinika.sequencer.FrinikaSequencer;
import com.frinika.sequencer.FrinikaTrackWrapper;

/**
 * @author Peter Johan Salomonsen
 *
 */
public class MyMidiRenderer extends InputStream {

    long tickPosition = 0;

    double ticksPerSecond;

    AudioMixer mixer;

    double startTimeInMicros;

    FrinikaSequence sequence;

    FrinikaSequencer sequencer;

    double samplePos = 0;

    double tickSamples = 0;

    float sampleRate;

    int available;

    byte[] buffer;

    HashMap<FrinikaTrackWrapper, Integer> trackIndex = new HashMap<FrinikaTrackWrapper, Integer>();

    int readpos = 0;

    byte[] byteBuffer = null;

    public MyMidiRenderer(AudioMixer mixer, FrinikaSequencer sequencer, long startTick, int ticks, float sampleRate) {
        this.mixer = mixer;
        this.sampleRate = sampleRate;
        this.sequence = (FrinikaSequence) sequencer.getSequence();
        this.sequencer = sequencer;
        this.tickPosition = startTick;
        sequencer.setTickPosition(startTick);
        startTimeInMicros = sequencer.getMicrosecondPosition();
        this.available = (int) (getNumberOfSamples(ticks) * 4);
        tickSamples = getNumberOfSamples(1);
        AudioProcess audioProcess = new AudioProcess() {

            public void close() {
            }

            public void open() {
            }

            public int processAudio(AudioBuffer buffer) {
                if (byteBuffer == null) byteBuffer = new byte[buffer.getSampleCount() * 2 * 2];
                int i = 0;
                for (int n = 0; n < buffer.getSampleCount(); n++) {
                    float floatSample = buffer.getChannel(0)[n];
                    short sample;
                    if (floatSample >= 1.0f) sample = 0x7fff; else if (floatSample <= -1.0f) sample = -0x8000; else sample = (short) (floatSample * 0x8000);
                    byteBuffer[i++] = (byte) ((sample & 0xff00) >> 8);
                    byteBuffer[i++] = (byte) (sample & 0xff);
                    floatSample = buffer.getChannel(1)[n];
                    if (floatSample >= 1.0f) sample = 0x7fff; else if (floatSample <= -1.0f) sample = -0x8000; else sample = (short) (floatSample * 0x8000);
                    byteBuffer[i++] = (byte) ((sample & 0xff00) >> 8);
                    byteBuffer[i++] = (byte) (sample & 0xff);
                }
                return AUDIO_OK;
            }
        };
        mixer.getMainBus().setOutputProcess(audioProcess);
    }

    @Override
    public int available() throws IOException {
        return available;
    }

    /** 
     * TODO fix for tempo changes
     * 
     * @param ticks
     * @return
     */
    double getNumberOfSamples(int ticks) {
        double ticksPerSecond = (sequence.getResolution() * sequencer.getTempoInBPM()) / 60.0;
        double seconds = ticks / ticksPerSecond;
        return (seconds * sampleRate);
    }

    void fillBuffer() {
        while (byteBuffer == null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mixer.setEnabled(true);
            mixer.work(-1);
        }
        for (int n = 0; n < buffer.length; n++) {
            if (readpos == byteBuffer.length) {
                mixer.work(-1);
                readpos = 0;
            }
            buffer[n] = byteBuffer[readpos++];
        }
    }

    int bufferPos = 0;

    public int read() throws IOException {
        if (bufferPos == 0) {
            double newSamplePos = samplePos + tickSamples;
            int bufferSize = (int) (newSamplePos) - (int) (samplePos);
            samplePos = newSamplePos;
            if (buffer == null || buffer.length != bufferSize * 4) buffer = new byte[bufferSize * 4];
            tickSamples = getNumberOfSamples(1);
            sequencer.nonRealtimeNextTick();
            fillBuffer();
        }
        int ret = 0xff & buffer[bufferPos++];
        if (bufferPos == buffer.length) bufferPos = 0;
        available--;
        return (ret);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        for (int i = off; i < len; i++) {
            if (available == 0) return i - off;
            if (bufferPos == 0) {
                double newSamplePos = samplePos + tickSamples;
                int bufferSize = (int) (newSamplePos) - (int) (samplePos);
                samplePos = newSamplePos;
                if (buffer == null || buffer.length != bufferSize * 4) buffer = new byte[bufferSize * 4];
                tickSamples = getNumberOfSamples(1);
                sequencer.nonRealtimeNextTick();
                fillBuffer();
            }
            b[i] = buffer[bufferPos++];
            if (bufferPos == buffer.length) bufferPos = 0;
            available--;
        }
        return len;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
	 * PJL
	 * @return start position of rendering in microseconds
	 */
    public double getStartTimeInMicros() {
        return startTimeInMicros;
    }
}

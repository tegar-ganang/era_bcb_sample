package com.sesca.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class PCMUCodec4 implements AudioTranscoder {

    int[] taulu = new int[256];

    int[] mkeys = { -1, -1, -1, -1, -1, -1, -1, -1 };

    int[] mvalues = { -1, -1, -1, -1, -1, -1, -1, -1 };

    int pnum = 0;

    int psilence = 0;

    int ptotal = 0;

    int pwin = 8;

    long plen = 480;

    int silenceThreshold = 75;

    int silenceThresholdF = 90;

    private ByteArrayInputStream byteStream;

    private AudioInputStream rawStream;

    private AudioInputStream encodedStream;

    AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 1, 2, 8000, false);

    AudioFormat pcm16Format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);

    AudioFormat ulawFormat = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 1, 8000, false);

    AudioTranscoderListener listener = null;

    public byte[] encode(byte[] frame) {
        byteStream = new ByteArrayInputStream(frame);
        rawStream = new AudioInputStream(byteStream, pcmFormat, frame.length / 2);
        encodedStream = AudioSystem.getAudioInputStream(ulawFormat, rawStream);
        byte[] encodedFrame = new byte[frame.length / 2];
        try {
            encodedStream.read(encodedFrame, 0, encodedFrame.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encodedFrame;
    }

    public boolean canEncode() {
        return true;
    }

    public boolean canDecode() {
        return true;
    }

    public boolean supportsSilenceSuppression() {
        return false;
    }

    public byte[] decode(byte[] b) {
        analyze(b);
        byteStream = new ByteArrayInputStream(b);
        rawStream = new AudioInputStream(byteStream, ulawFormat, b.length * 2);
        encodedStream = AudioSystem.getAudioInputStream(pcmFormat, rawStream);
        byte[] encodedFrame = new byte[b.length * 2];
        try {
            encodedStream.read(encodedFrame, 0, encodedFrame.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encodedFrame;
    }

    public int getBitRate() {
        return 16;
    }

    public int getSampleRate() {
        return 8000;
    }

    public int getChannels() {
        return 1;
    }

    public int getFrameLength() {
        return 20;
    }

    public int getFrameSize() {
        return 160;
    }

    public int getPayloadType() {
        return 0;
    }

    public void init() {
        resetAnalysis();
    }

    void analyze(byte[] frame) {
        int fsilence = 0;
        int ftotal = 0;
        for (int i = 0; i < frame.length; i++) {
            int k = frame[i] & 0xff;
            taulu[k]++;
            if (k >= 250 && k <= 255) {
                psilence++;
                fsilence++;
            }
            if (k >= 122 && k <= 127) {
                psilence++;
                fsilence++;
            }
            ptotal++;
            ftotal++;
        }
        int f = (fsilence * 100) / ftotal;
        if (f >= silenceThresholdF) listener.setSilentFrame(true); else listener.setSilentFrame(false);
        pnum++;
        if (pnum < pwin) return;
        int p = (psilence * 100) / ptotal;
        boolean b;
        if (p >= silenceThreshold) b = false; else {
            b = true;
            ((AudioProcessor) listener).psstart = System.currentTimeMillis();
        }
        listener.setCalleeIsTalking(b);
        resetAnalysis();
    }

    private void resetAnalysis() {
        for (int i = 0; i < taulu.length; i++) {
            taulu[i] = 0;
        }
        for (int i = 0; i < mkeys.length; i++) {
            mkeys[i] = 0;
        }
        for (int i = 0; i < mvalues.length; i++) {
            mvalues[i] = 0;
        }
        pnum = 0;
        psilence = 0;
        ptotal = 0;
    }

    public PCMUCodec4() {
    }

    public PCMUCodec4(AudioTranscoderListener l) {
        listener = l;
    }

    public boolean supportsPseudoEchoCancellation() {
        return false;
    }

    private byte[] whiteNoise(int min, int max) {
        byte[] noise = new byte[160];
        Random generator = new Random();
        for (int i = 0; i < 160; i += 2) {
            int m1 = (generator.nextInt(max - min) + min + 1);
            int m2 = m1 + 128;
            noise[i] = (byte) (m1);
            noise[i + 1] = (byte) (m2);
        }
        return noise;
    }

    private boolean isBlocked() {
        long delttatee = (System.currentTimeMillis() - ((AudioProcessor) listener).psstart);
        if (listener.getCalleeIsTalking()) {
            return true;
        }
        if (delttatee < plen) {
            return true;
        } else {
            return false;
        }
    }
}

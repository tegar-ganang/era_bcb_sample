package com.arykow.applications.ugabe.standalone;

import java.io.IOException;
import java.io.OutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import com.arykow.applications.ugabe.client.AudioController;
import com.arykow.applications.ugabe.client.IAudioListener;

public class AudioDriver implements IAudioListener {

    SourceDataLine audioSource;

    Mixer.Info mixerinfo;

    AudioController ac;

    OutputStream dumpStream;

    byte[] audioBuffer;

    int audioBufferIndex;

    int sampleRate;

    int audioBufferSize;

    int sampleSize;

    int frameSize;

    int outputInterval;

    int channels;

    boolean signedSound;

    boolean bigEndianSound;

    public AudioDriver() {
        if (!(false)) throw new Error("Assertion failed: " + "false");
    }

    public AudioDriver(AudioController ac) {
        this.ac = ac;
        audioBufferIndex = 0;
        audioBufferSize = 4096;
        audioBuffer = new byte[audioBufferSize];
        sampleRate = 44100;
        sampleSize = 8;
        channels = 2;
        frameSize = 2;
        signedSound = true;
        bigEndianSound = true;
        outputInterval = 1 << 6;
        Mixer.Info[] mi = AudioSystem.getMixerInfo();
        mixerinfo = mi[0];
    }

    public void stop() {
        if (audioSource != null) {
            audioSource.stop();
            audioSource.close();
        }
    }

    public void start() {
        try {
            if (audioSource != null) {
                audioSource.open(new AudioFormat(sampleRate, sampleSize, channels, signedSound, bigEndianSound), audioBufferSize);
                audioSource.start();
            }
        } catch (LineUnavailableException e) {
            System.out.println("Note: Sound will be unavailable");
        }
    }

    public boolean update() {
        frameSize = (sampleSize + 7) / 8 * channels;
        AudioFormat af = new AudioFormat(sampleRate, sampleSize, channels, signedSound, bigEndianSound);
        DataLine.Info di = new DataLine.Info(SourceDataLine.class, af, audioBufferSize);
        Mixer m = AudioSystem.getMixer(mixerinfo);
        if (m.isLineSupported(di)) {
            try {
                stop();
                SourceDataLine sdl = (SourceDataLine) m.getLine(di);
                audioSource = sdl;
                audioBuffer = new byte[audioBufferSize];
                audioBufferIndex = audioBufferSize - (channels * sampleSize);
                start();
                ac.setSampleRate(sampleRate);
                return true;
            } catch (LineUnavailableException e) {
                System.out.println("Note: Sound will be unavailable");
            }
        }
        return false;
    }

    ;

    public int getFrameSize() {
        return frameSize;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int n) {
        channels = n;
        audioBufferIndex = (frameSize) * (audioBufferIndex / (frameSize));
        ;
        update();
    }

    public Mixer.Info getMixerInfo() {
        return mixerinfo;
    }

    public boolean setMixerInfo(String adName, String adVendor, String adDescription, String adVersion) {
        Mixer.Info[] mi = AudioSystem.getMixerInfo();
        for (int i = 0; i < mi.length; ++i) {
            if (mi[i].getName().equals(adName) && mi[i].getVendor().equals(adVendor) && mi[i].getDescription().equals(adDescription) && mi[i].getVersion().equals(adVersion)) return setMixerInfo(mi[i]);
        }
        return false;
    }

    public boolean setMixerInfo(Mixer.Info minfo) {
        Mixer.Info tmp = mixerinfo;
        mixerinfo = minfo;
        if (update()) return true;
        mixerinfo = tmp;
        return false;
    }

    public int getBufferSize() {
        return audioBufferSize;
    }

    public int getOutputInterval() {
        return outputInterval;
    }

    public boolean setOutputInterval(int n) {
        outputInterval = (frameSize) * (n / (frameSize));
        if (outputInterval >= audioBufferSize - 1 - (frameSize)) outputInterval = audioBufferSize - (frameSize);
        outputInterval = Math.max((frameSize), outputInterval);
        return true;
    }

    public boolean setBufferSize(int n) {
        audioBufferSize = (frameSize) * (n / (frameSize));
        ;
        setOutputInterval(Math.min(audioBufferSize, outputInterval));
        if (audioBufferIndex >= audioBufferSize - (frameSize)) audioBufferIndex = (audioBufferSize) - (frameSize);
        audioBuffer = new byte[n];
        return update();
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public boolean setSampleRate(int sr) {
        sampleRate = sr;
        return update();
    }

    public void writeAudioSample(byte l, byte r) {
        if (channels == 2) {
            audioBuffer[audioBufferIndex++] = l;
            audioBuffer[audioBufferIndex++] = r;
        } else {
            audioBuffer[audioBufferIndex++] = (byte) ((l + r) / 2);
        }
        if (audioBufferIndex >= outputInterval) {
            if (audioSource != null) {
                audioSource.write(audioBuffer, 0, audioBufferIndex);
            }
            if (dumpStream != null) {
                for (int i = 0; i < audioBufferIndex; ++i) audioBuffer[i] = (byte) (audioBuffer[i] + 128);
                try {
                    dumpStream.write(audioBuffer, 0, audioBufferIndex);
                } catch (IOException e) {
                }
            }
            audioBufferIndex = 0;
        }
    }

    public void writeAudioSample(byte[] b) {
        if (!(false)) throw new Error("Assertion failed: " + "false");
    }

    public void reset() {
        audioBufferIndex = 0;
    }

    public void setDumpStream(java.io.DataOutputStream dos) {
        dumpStream = dos;
    }
}

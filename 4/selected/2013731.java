package com.frinika.sequencer.gui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import uk.org.toot.audio.server.AudioServer;

public class MixerAudioDeviceHandle implements AudioDeviceHandle {

    private Mixer mixer;

    private AudioFormat af;

    private DataLine.Info info;

    private TargetDataLine line;

    private AudioServer server;

    private byte inBuffer[];

    public MixerAudioDeviceHandle(Mixer mixer, AudioFormat af, DataLine.Info info, AudioServer server) {
        this.mixer = mixer;
        this.af = af;
        this.info = info;
        this.line = null;
        this.server = server;
    }

    public String toString() {
        if (af.getChannels() == 1) return mixer.getMixerInfo().getName() + " (MONO)"; else if (af.getChannels() == 2) return mixer.getMixerInfo().getName() + " (STEREO)"; else return mixer.getMixerInfo().getName() + "channels=" + af.getChannels();
    }

    /**
	 *@deprecated To be rpelaced with COnnections
	 */
    public TargetDataLine getLine() {
        try {
            System.out.println(this + " **** " + af);
            DataLine.Info infoIn = new DataLine.Info(TargetDataLine.class, af);
            line = (TargetDataLine) mixer.getLine(infoIn);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void fillBuffers(int nFrame) {
        try {
            int nByte = af.getChannels() * nFrame * 2;
            if (inBuffer == null || inBuffer.length != nByte) inBuffer = new byte[nByte];
            if (line.available() >= nByte && nByte > 0) {
                int nread;
                int cnt = 0;
                do {
                    nread = line.read(inBuffer, 0, nByte);
                    if (nread == 0) System.out.println("active :" + line.isActive() + " available:" + line.available() + " nByte: " + nByte + " inBuffersize: " + inBuffer.length);
                    cnt++;
                    for (int n = 0; n < nByte / 2; n++) {
                        short sample = (short) ((0xff & inBuffer[2 * n + 1]) + ((0xff & inBuffer[2 * n]) * 256));
                        float val = sample / 32768f;
                    }
                } while (line.available() > 2 * nByte);
                if (cnt != 1) System.out.println(" COUNT WAS " + cnt);
            } else {
                System.err.println(String.format(" GLITCH avail=%d actual=%d ", line.available(), nByte));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * @deprecated   TO be replaced by Connections
	 */
    public TargetDataLine getOpenLine() {
        if (isOpen()) return line;
        try {
            if (line == null) line = (TargetDataLine) mixer.getLine(info);
            line.open(af);
            return line;
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getChannels() {
        return af.getChannels();
    }

    public AudioFormat getFormat() {
        return af;
    }

    public boolean isOpen() {
        if (line == null) return false;
        return line.isOpen();
    }

    /**
	 * open and start the line.
	 */
    public void open() {
        System.out.println(" Opening MixrerAudioDevice line");
        if (isOpen()) return;
        try {
            if (line == null) line = (TargetDataLine) mixer.getLine(info);
            line.open(af);
            System.out.println("  . . ..  Open");
            line.start();
            System.out.println("  . . ..  Start  " + isOpen());
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (!isOpen()) return;
        line.close();
    }

    public byte[] getBuffer() {
        return inBuffer;
    }
}

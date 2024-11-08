package com.frinika.toot.javasoundmultiplexed;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.DataLine.Info;

class JavaSoundInDevice extends JavaSoundDevice {

    private int latencyFrames;

    private boolean doFlush = false;

    private long framesRead = 0;

    public JavaSoundInDevice(Mixer mixer, AudioFormat af, Info info, int bufferSizeInFrames) {
        super(mixer, af, info, bufferSizeInFrames);
        framesRead = 0;
        doFlush = true;
    }

    public void fillBuffer() {
        if (line.available() < byteBuffer.length) {
            System.out.println('_');
        } else if (doFlush) {
            doFlush = false;
            line.flush();
            System.out.println(getName() + " flushed");
        } else {
            latencyFrames = (int) (line.getLongFramePosition() - framesRead);
            try {
                int nread = ((TargetDataLine) line).read(byteBuffer, 0, byteBuffer.length);
                framesRead += nread / 2 / af.getChannels();
                if (nread == 0) System.out.println("active :" + line.isActive() + " available:" + line.available() + " nByte: " + byteBuffer.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void flush() {
        doFlush = true;
    }

    public void start() throws Exception {
        framesRead = 0;
        open();
    }

    public void stop() throws Exception {
        close();
    }

    /**
	 * open and start the line.
	 */
    public void open() {
        System.out.println(" Opening MixrerAudioDevice line");
        if (isOpen()) return;
        try {
            if (line == null) line = (TargetDataLine) mixer.getLine(info);
            ((TargetDataLine) line).open(af);
            System.out.println("  . . ..  Open");
            line.flush();
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

    public boolean isActive() {
        if (line == null) return false;
        return line.isActive();
    }

    public int getLatencyFrames() {
        return latencyFrames;
    }
}

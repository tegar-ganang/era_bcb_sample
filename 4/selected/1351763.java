package com.frinika.toot.javasoundmultiplexed;

import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.audio.server.AudioLine;

class StereoOutConnection implements AudioLine {

    private JavaSoundOutDevice dev;

    private int chan[];

    ChannelFormat chanFormat;

    boolean isBigEndian;

    public StereoOutConnection(JavaSoundOutDevice dev, int chan[]) {
        this.dev = dev;
        this.chan = new int[chan.length];
        for (int i = 0; i < chan.length; i++) this.chan[i] = chan[i];
        isBigEndian = dev.getFormat().isBigEndian();
    }

    public int processAudio(AudioBuffer buffer) {
        byte bytes[] = dev.getBuffer();
        assert (bytes != null);
        int n = buffer.getSampleCount();
        int nchan = dev.getChannels();
        for (int chPtr = 0; chPtr < chan.length; chPtr++) {
            int ch = chan[chPtr];
            float out[] = buffer.getChannel(chPtr);
            if (isBigEndian) {
                for (int i = 0; i < n; i++) {
                    int ib = i * 2 * nchan + ch * 2;
                    short sample = (short) (out[i] * 32768f);
                    bytes[ib + 1] = (byte) (0xff & sample);
                    bytes[ib] = (byte) (0xff & (sample >> 8));
                }
            } else {
                for (int i = 0; i < n; i++) {
                    int ib = i * 2 * nchan + ch * 2;
                    short sample = (short) (out[i] * 32768f);
                    bytes[ib + 1] = (byte) (0xff & sample >> 8);
                    bytes[ib] = (byte) (0xff & (sample));
                }
            }
        }
        return AUDIO_OK;
    }

    public void close() {
    }

    public float getLatencyMilliseconds() {
        return 0;
    }

    public String getName() {
        return "out " + (chan[0] + 1) + "/" + (chan[1] + 1);
    }

    public int getLatencyFrames() {
        return 0;
    }

    public ChannelFormat getChannelFormat() {
        return ChannelFormat.MONO;
    }

    public void open() {
    }
}

package com.frinika.toot.javasoundmultiplexed;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.audio.server.AudioLine;

abstract class JavaSoundAudioLine implements AudioLine {

    protected AudioFormat format;

    protected Mixer.Info mixerInfo;

    protected String label;

    protected int latencyFrames = -1;

    protected ChannelFormat channelFormat;

    public JavaSoundAudioLine(AudioFormat format, Mixer.Info info, String label) {
        this.format = format;
        mixerInfo = info;
        this.label = label;
        switch(format.getChannels()) {
            case 1:
                channelFormat = ChannelFormat.MONO;
            case 2:
                channelFormat = ChannelFormat.STEREO;
        }
    }

    public String getName() {
        return label;
    }

    public ChannelFormat getChannelFormat() {
        return channelFormat;
    }

    public int getLatencyFrames() {
        return latencyFrames;
    }

    public abstract void start() throws Exception;

    public abstract void stop() throws Exception;

    public abstract boolean isActive();
}

package com.omnividea.media.parser.video;

import com.omnividea.FobsConfiguration;
import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.*;
import java.awt.*;
import com.sun.media.format.*;
import java.nio.ByteBuffer;

class AudioTrack implements javax.media.Track {

    private AudioFormat outFormat;

    private boolean enabled = false;

    private Time startTime = new Time(0.0);

    private Time duration = new Time(0.0);

    private Parser parser = null;

    private int bitsPerSample = 0;

    private double frameRate = 0.0;

    private long lts = 0, lt = 0;

    public AudioTrack(double sampleRate, int channelNumber, int bitsPerSample, double frameRate, Time duration, Time startTime, Parser parser) {
        int bytesPerSample = bitsPerSample / 8;
        int frameSizeInBits = channelNumber * bitsPerSample;
        int avgBytesPerSec = (int) (channelNumber * bytesPerSample * sampleRate);
        outFormat = new WavAudioFormat("FFMPEG_AUDIO", sampleRate, bitsPerSample, channelNumber, frameSizeInBits, avgBytesPerSec, parser.isBigEndian() ? AudioFormat.BIG_ENDIAN : AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED, Format.NOT_SPECIFIED, Format.byteArray, new byte[0]);
        this.duration = duration;
        enabled = true;
        this.parser = parser;
        this.frameRate = frameRate;
        this.startTime = startTime;
        this.bitsPerSample = bitsPerSample;
    }

    public Format getFormat() {
        return outFormat;
    }

    public void setEnabled(boolean t) {
        enabled = t;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void readFrame(Buffer buffer) {
        if (buffer == null) return;
        if (!isEnabled()) {
            buffer.setDiscard(true);
            return;
        }
        buffer.setFormat(outFormat);
        Object obj = buffer.getData();
        byte[] data;
        long location;
        int needDataSize;
        needDataSize = outFormat.getChannels() * outFormat.getSampleSizeInBits() * 4000;
        if ((obj == null) || (!(obj instanceof int[])) || (((byte[]) obj).length < needDataSize)) {
            data = new byte[needDataSize];
            buffer.setData(data);
        } else {
            data = (byte[]) obj;
        }
        if (parser.getNextAudioFrame(data, 0, needDataSize)) {
            int size = parser.getAudioSampleNumber();
            buffer.setOffset(0);
            buffer.setLength(size);
            double audioTime = parser.getAudioSampleTimestamp();
            long ts = (long) (audioTime * 1000000000);
            buffer.setTimeStamp(ts);
            long tmp = System.currentTimeMillis();
            lts = ts;
            lt = tmp;
        } else {
            buffer.setLength(0);
            buffer.setEOM(true);
        }
    }

    public int mapTimeToFrame(Time t) {
        return 0;
    }

    public Time mapFrameToTime(int frameNumber) {
        return null;
    }

    public void setTrackListener(TrackListener listener) {
    }

    public Time getDuration() {
        return parser.getDuration();
    }
}

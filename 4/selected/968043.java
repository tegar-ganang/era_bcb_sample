package com.omnividea.media.parser.video;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.*;
import java.awt.*;
import com.sun.media.format.*;

class VideoTrack implements javax.media.Track {

    private static int bMask = 0x000000ff;

    private static int gMask = 0x0000ff00;

    private static int rMask = 0x00ff0000;

    private VideoFormat outFormat;

    private boolean enabled = false;

    private Time startTime = new Time(0.0);

    private Time duration = new Time(0.0);

    private Parser parser = null;

    private long timestamp = 0;

    private long lts = 0, lt = 0;

    private long accTime = 0;

    public VideoTrack(int videoWidth, int videoHeight, float frameRate, Time duration, Time startTime, Parser parser) {
        outFormat = new AviVideoFormat("ffmpeg_video", new Dimension(videoWidth, videoHeight), videoWidth * videoHeight, byte[].class, (float) frameRate, 0, 0, 0, 0, 0, 0, 0, new byte[0]);
        this.duration = duration;
        enabled = true;
        this.parser = parser;
        this.startTime = startTime;
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
        int[] data;
        long location;
        int needDataSize;
        needDataSize = outFormat.getSize().width * outFormat.getSize().height;
        if ((obj == null) || (!(obj instanceof int[])) || (((int[]) obj).length < needDataSize)) {
            data = new int[needDataSize];
            buffer.setData(data);
        } else {
            data = (int[]) obj;
        }
        long tmp = System.currentTimeMillis();
        if (parser.getNextFrame(data, 0, needDataSize)) {
            accTime += System.currentTimeMillis() - tmp;
            buffer.setOffset(0);
            buffer.setLength(needDataSize);
            double videoTime = parser.getTimestamp();
            long ts = (long) ((videoTime) * 1000000000);
            buffer.setTimeStamp(ts);
            lts = ts;
            lt = tmp;
        } else {
            buffer.setLength(0);
            buffer.setEOM(true);
        }
    }

    public int mapTimeToFrame(Time t) {
        System.out.println("FobsVideoTrack: mapTimeToFrame");
        return 0;
    }

    public Time mapFrameToTime(int frameNumber) {
        System.out.println("FobsVideoTrack: mapFrameToTime");
        return null;
    }

    public void setTrackListener(TrackListener listener) {
    }

    public Time getDuration() {
        return parser.getDuration();
    }
}

class AudioTrack implements javax.media.Track {

    private AudioFormat outFormat;

    private boolean enabled = false;

    private Time startTime = new Time(0.0);

    private Time duration = new Time(0.0);

    private Parser parser = null;

    private double frameRate = 0.0;

    private long lts = 0, lt = 0;

    public AudioTrack(double sampleRate, int channelNumber, double frameRate, Time duration, Time startTime, Parser parser) {
        int frameSizeInBits = channelNumber * 16;
        int avgBytesPerSec = (int) (channelNumber * 2 * sampleRate);
        outFormat = new WavAudioFormat("FFMPEG_AUDIO", sampleRate, 16, channelNumber, frameSizeInBits, avgBytesPerSec, parser.isBigEndian() ? AudioFormat.BIG_ENDIAN : AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED, Format.NOT_SPECIFIED, Format.byteArray, new byte[0]);
        this.duration = duration;
        enabled = true;
        this.parser = parser;
        this.frameRate = frameRate;
        this.startTime = startTime;
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

public class Parser implements Demultiplexer {

    private native void avInit(String filename);

    private native boolean avOpen(int peer);

    private native boolean avClose(int peer);

    private native boolean avIsVideoPresent(int peer);

    private native int avGetWidth(int peer);

    private native int avGetHeight(int peer);

    private native float avGetFrameRate(int peer);

    private native double avGetBitRate(int peer);

    private native double avGetDurationSec(int peer);

    private native boolean avIsSeekable(int peer);

    private native double avGetNextFrameTime(int peer);

    private native boolean avIsAudioPresent(int peer);

    private native double avGetAudioBitRate(int peer);

    private native double avGetAudioSampleRate(int peer);

    private native int avGetAudioChannelNumber(int peer);

    private native double avSetPosition(int peer, double position);

    private native boolean avProcess(int peer, Object outData, long outDataBytes, long length);

    private native boolean avProcessAudio(int peer, Object outData, long outDataBytes, long length);

    private native int avGetAudioSampleSize(int peer);

    private native double avGetAudioSampleTimestamp(int peer);

    private native double avGetFirstVideoTime(int peer);

    private native double avGetFirstAudioTime(int peer);

    private native boolean avIsBigEndian();

    private int peer = 0;

    protected com.omnividea.media.protocol.file.DataSource dataSource;

    private ContentDescriptor[] inputContent = new ContentDescriptor[] { new ContentDescriptor("video.ffmpeg") };

    private javax.media.Track[] tracks = null;

    private Time duration = new Time(0.0);

    private int videoWidth = -1;

    private int videoHeight = -1;

    private double videoBitRate = 0.0;

    private float videoFrameRate = 0;

    private boolean isAudioPresent = false;

    private boolean isVideoPresent = false;

    private int trackNumber = 0;

    private double audioBitRate = 0.0;

    private double audioSampleRate = 0.0;

    private int audioChannelNumber = 0;

    public static double lastAudioTime = 0.0;

    public static double lastVideoTime = 0.0;

    private boolean positionable = true;

    private boolean randomAccess = true;

    static {
        try {
            System.loadLibrary("fobs4jmf");
            System.out.println("Fobs4JMF - Native shared library found");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("Fobs4JMF - Native shared library NOT found");
            e.printStackTrace();
            throw new ExceptionInInitializerError(e.getMessage());
        }
    }

    public boolean isBigEndian() {
        return avIsBigEndian();
    }

    public Parser() {
        super();
    }

    public synchronized boolean getNextFrame(Object outData, long outDataBytes, long length) {
        return avProcess(peer, outData, outDataBytes, length);
    }

    public double getTimestamp() {
        return avGetNextFrameTime(peer);
    }

    public int getAudioSampleNumber() {
        return avGetAudioSampleSize(peer);
    }

    public synchronized boolean getNextAudioFrame(Object outData, long outDataBytes, long length) {
        return avProcessAudio(peer, outData, outDataBytes, length);
    }

    public double getAudioSampleTimestamp() {
        return avGetAudioSampleTimestamp(peer);
    }

    public void setSource(DataSource source) throws IncompatibleSourceException {
        if (!(source instanceof com.omnividea.media.protocol.file.DataSource)) {
            IncompatibleSourceException exp = new IncompatibleSourceException("Invalid DataSource");
            exp.printStackTrace();
            throw exp;
        } else {
            dataSource = (com.omnividea.media.protocol.file.DataSource) source;
            if (dataSource.getUrlName() == null) {
                throw new IncompatibleSourceException("Invalid Datasource");
            }
        }
        avInit(dataSource.getUrlName());
        if (avOpen(peer) == false) throw new IncompatibleSourceException("Fobs cannot read such url");
        duration = new Time(avGetDurationSec(peer));
        trackNumber = 0;
        isVideoPresent = avIsVideoPresent(peer);
        if (isVideoPresent) {
            trackNumber++;
            videoWidth = avGetWidth(peer);
            videoHeight = avGetHeight(peer);
            videoBitRate = avGetBitRate(peer);
            videoFrameRate = avGetFrameRate(peer);
        }
        isAudioPresent = avIsAudioPresent(peer);
        if (isAudioPresent) {
            trackNumber++;
            audioBitRate = avGetAudioBitRate(peer);
            audioSampleRate = avGetAudioSampleRate(peer);
            audioChannelNumber = avGetAudioChannelNumber(peer);
        }
        positionable = true;
        randomAccess = avIsSeekable(peer);
        tracks = new javax.media.Track[trackNumber];
        int trackIndex = 0;
        if (isVideoPresent) {
            Time firstVideoTime = new Time(avGetFirstVideoTime(peer));
            tracks[trackIndex++] = new VideoTrack(videoWidth, videoHeight, videoFrameRate, duration, firstVideoTime, this);
        }
        if (isAudioPresent) {
            Time firstAudioTime = new Time(avGetFirstAudioTime(peer));
            tracks[trackIndex++] = new AudioTrack(audioSampleRate, audioChannelNumber, videoFrameRate, duration, firstAudioTime, this);
        }
    }

    public Time getDuration() {
        return duration;
    }

    public ContentDescriptor[] getSupportedInputContentDescriptors() {
        return inputContent;
    }

    public void start() {
    }

    public void stop() {
    }

    public javax.media.Track[] getTracks() throws java.io.IOException, BadHeaderException {
        return tracks;
    }

    public boolean isPositionable() {
        return positionable;
    }

    public boolean isRandomAccess() {
        return randomAccess;
    }

    public Time setPosition(Time where, int rounding) {
        double newTime = avSetPosition(peer, where.getSeconds());
        return new Time(newTime);
    }

    public Time getMediaTime() {
        return new Time(avGetNextFrameTime(peer));
    }

    public String getName() {
        return "FOBS PARSER";
    }

    public void open() {
        avOpen(peer);
    }

    public void reset() {
        close();
        open();
    }

    public void close() {
        avClose(peer);
    }

    public Object[] getControls() {
        return null;
    }

    public Object getControl(String s) {
        return null;
    }
}

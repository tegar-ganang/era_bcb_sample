package com.jmex.audio;

import java.nio.ByteBuffer;

/**
 * Represents a chunk of sound data. This is a lower level class that users will
 * not generally use directly.
 * 
 * @author Joshua Slack
 * @version $Id: AudioBuffer.java 4133 2009-03-19 20:40:11Z blaine.dev $
 */
public class AudioBuffer {

    private float length;

    private int channels;

    private int bitRate;

    private int depth;

    public void setup(ByteBuffer data, int channels, int bitRate, float length, int depth) {
        this.length = length;
        this.channels = channels;
        this.bitRate = bitRate;
        this.depth = depth;
    }

    public float getLength() {
        return length;
    }

    public int getBitRate() {
        return bitRate;
    }

    public int getChannels() {
        return channels;
    }

    public int getDepth() {
        return depth;
    }
}

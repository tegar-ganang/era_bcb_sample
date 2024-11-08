package com.coremedia.iso.boxes.apple;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

/**
 *
 */
public final class AppleLosslessSpecificBox extends AbstractFullBox {

    public static final String TYPE = "alac";

    private long maxSamplePerFrame;

    private int unknown1;

    private int sampleSize;

    private int historyMult;

    private int initialHistory;

    private int kModifier;

    private int channels;

    private int unknown2;

    private long maxCodedFrameSize;

    private long bitRate;

    private long sampleRate;

    public long getMaxSamplePerFrame() {
        return maxSamplePerFrame;
    }

    public void setMaxSamplePerFrame(int maxSamplePerFrame) {
        this.maxSamplePerFrame = maxSamplePerFrame;
    }

    public int getUnknown1() {
        return unknown1;
    }

    public void setUnknown1(int unknown1) {
        this.unknown1 = unknown1;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public int getHistoryMult() {
        return historyMult;
    }

    public void setHistoryMult(int historyMult) {
        this.historyMult = historyMult;
    }

    public int getInitialHistory() {
        return initialHistory;
    }

    public void setInitialHistory(int initialHistory) {
        this.initialHistory = initialHistory;
    }

    public int getKModifier() {
        return kModifier;
    }

    public void setKModifier(int kModifier) {
        this.kModifier = kModifier;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getUnknown2() {
        return unknown2;
    }

    public void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }

    public long getMaxCodedFrameSize() {
        return maxCodedFrameSize;
    }

    public void setMaxCodedFrameSize(int maxCodedFrameSize) {
        this.maxCodedFrameSize = maxCodedFrameSize;
    }

    public long getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public long getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        maxSamplePerFrame = IsoTypeReader.readUInt32(content);
        unknown1 = IsoTypeReader.readUInt8(content);
        sampleSize = IsoTypeReader.readUInt8(content);
        historyMult = IsoTypeReader.readUInt8(content);
        initialHistory = IsoTypeReader.readUInt8(content);
        kModifier = IsoTypeReader.readUInt8(content);
        channels = IsoTypeReader.readUInt8(content);
        unknown2 = IsoTypeReader.readUInt16(content);
        maxCodedFrameSize = IsoTypeReader.readUInt32(content);
        bitRate = IsoTypeReader.readUInt32(content);
        sampleRate = IsoTypeReader.readUInt32(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, maxSamplePerFrame);
        IsoTypeWriter.writeUInt8(byteBuffer, unknown1);
        IsoTypeWriter.writeUInt8(byteBuffer, sampleSize);
        IsoTypeWriter.writeUInt8(byteBuffer, historyMult);
        IsoTypeWriter.writeUInt8(byteBuffer, initialHistory);
        IsoTypeWriter.writeUInt8(byteBuffer, kModifier);
        IsoTypeWriter.writeUInt8(byteBuffer, channels);
        IsoTypeWriter.writeUInt16(byteBuffer, unknown2);
        IsoTypeWriter.writeUInt32(byteBuffer, maxCodedFrameSize);
        IsoTypeWriter.writeUInt32(byteBuffer, bitRate);
        IsoTypeWriter.writeUInt32(byteBuffer, sampleRate);
    }

    public AppleLosslessSpecificBox() {
        super("alac");
    }

    protected long getContentSize() {
        return 28;
    }
}

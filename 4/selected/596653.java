package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import java.nio.ByteBuffer;

/**
 * Contains basic information about the audio samples in this track. Format-specific information
 * is appened as boxes after the data described in ISO/IEC 14496-12 chapter 8.16.2.
 */
public class AudioSampleEntry extends SampleEntry implements ContainerBox {

    public static final String TYPE1 = "samr";

    public static final String TYPE2 = "sawb";

    public static final String TYPE3 = "mp4a";

    public static final String TYPE4 = "drms";

    public static final String TYPE5 = "alac";

    public static final String TYPE7 = "owma";

    public static final String TYPE8 = "ac-3";

    public static final String TYPE9 = "ec-3";

    public static final String TYPE10 = "mlpa";

    /**
     * Identifier for an encrypted audio track.
     *
     * @see com.coremedia.iso.boxes.ProtectionSchemeInformationBox
     */
    public static final String TYPE_ENCRYPTED = "enca";

    private int channelCount;

    private int sampleSize;

    private long sampleRate;

    private int soundVersion;

    private int compressionId;

    private int packetSize;

    private long samplesPerPacket;

    private long bytesPerPacket;

    private long bytesPerFrame;

    private long bytesPerSample;

    private int reserved1;

    private long reserved2;

    private byte[] soundVersion2Data;

    private BoxParser boxParser;

    public AudioSampleEntry(String type) {
        super(type);
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public long getSampleRate() {
        return sampleRate;
    }

    public int getSoundVersion() {
        return soundVersion;
    }

    public int getCompressionId() {
        return compressionId;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public long getSamplesPerPacket() {
        return samplesPerPacket;
    }

    public long getBytesPerPacket() {
        return bytesPerPacket;
    }

    public long getBytesPerFrame() {
        return bytesPerFrame;
    }

    public long getBytesPerSample() {
        return bytesPerSample;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public void setSampleRate(long sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setSoundVersion(int soundVersion) {
        this.soundVersion = soundVersion;
    }

    public void setCompressionId(int compressionId) {
        this.compressionId = compressionId;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    public void setSamplesPerPacket(long samplesPerPacket) {
        this.samplesPerPacket = samplesPerPacket;
    }

    public void setBytesPerPacket(long bytesPerPacket) {
        this.bytesPerPacket = bytesPerPacket;
    }

    public void setBytesPerFrame(long bytesPerFrame) {
        this.bytesPerFrame = bytesPerFrame;
    }

    public void setBytesPerSample(long bytesPerSample) {
        this.bytesPerSample = bytesPerSample;
    }

    public void setReserved1(int reserved1) {
        this.reserved1 = reserved1;
    }

    public void setReserved2(long reserved2) {
        this.reserved2 = reserved2;
    }

    public void setSoundVersion2Data(byte[] soundVersion2Data) {
        this.soundVersion2Data = soundVersion2Data;
    }

    public void setBoxParser(BoxParser boxParser) {
        this.boxParser = boxParser;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        _parseReservedAndDataReferenceIndex(content);
        soundVersion = IsoTypeReader.readUInt16(content);
        reserved1 = IsoTypeReader.readUInt16(content);
        reserved2 = IsoTypeReader.readUInt32(content);
        channelCount = IsoTypeReader.readUInt16(content);
        sampleSize = IsoTypeReader.readUInt16(content);
        compressionId = IsoTypeReader.readUInt16(content);
        packetSize = IsoTypeReader.readUInt16(content);
        sampleRate = IsoTypeReader.readUInt32(content);
        if (!type.equals("mlpa")) {
            sampleRate = sampleRate >>> 16;
        }
        if (soundVersion > 0) {
            samplesPerPacket = IsoTypeReader.readUInt32(content);
            bytesPerPacket = IsoTypeReader.readUInt32(content);
            bytesPerFrame = IsoTypeReader.readUInt32(content);
            bytesPerSample = IsoTypeReader.readUInt32(content);
        }
        if (soundVersion == 2) {
            soundVersion2Data = new byte[20];
            content.get(20);
        }
        _parseChildBoxes(content);
    }

    @Override
    protected long getContentSize() {
        long contentSize = 28;
        contentSize += soundVersion > 0 ? 16 : 0;
        contentSize += soundVersion == 2 ? 20 : 0;
        for (Box boxe : boxes) {
            contentSize += boxe.getSize();
        }
        return contentSize;
    }

    public String toString() {
        return "AudioSampleEntry";
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        IsoTypeWriter.writeUInt16(byteBuffer, soundVersion);
        IsoTypeWriter.writeUInt16(byteBuffer, reserved1);
        IsoTypeWriter.writeUInt32(byteBuffer, reserved2);
        IsoTypeWriter.writeUInt16(byteBuffer, channelCount);
        IsoTypeWriter.writeUInt16(byteBuffer, sampleSize);
        IsoTypeWriter.writeUInt16(byteBuffer, compressionId);
        IsoTypeWriter.writeUInt16(byteBuffer, packetSize);
        if (type.equals("mlpa")) {
            IsoTypeWriter.writeUInt32(byteBuffer, getSampleRate());
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, getSampleRate() << 16);
        }
        if (soundVersion > 0) {
            IsoTypeWriter.writeUInt32(byteBuffer, samplesPerPacket);
            IsoTypeWriter.writeUInt32(byteBuffer, bytesPerPacket);
            IsoTypeWriter.writeUInt32(byteBuffer, bytesPerFrame);
            IsoTypeWriter.writeUInt32(byteBuffer, bytesPerSample);
        }
        if (soundVersion == 2) {
            byteBuffer.put(soundVersion2Data);
        }
        _writeChildBoxes(byteBuffer);
    }
}

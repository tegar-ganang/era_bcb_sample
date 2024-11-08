package com.sts.webmeet.content.common.audio;

import java.io.ByteArrayInputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

public class Mp3Decoder implements AudioDecoder {

    private int iSampleRate;

    private int iChannelCount;

    private WHAudioFormat format;

    private Decoder decoder;

    private SampleBuffer sampleBuffer;

    private Bitstream bitstream;

    private Header header;

    private SwappingByteArrayInputStream is;

    public static final String MP3_SAMPLE_RATE_PROPERTY = "webhuddle.property.audio.mp3.decoder.sample.rate";

    public static final String MP3_CHANNEL_COUNT_PROPERTY = "webhuddle.property.audio.mp3.decoder.channel.count";

    public void setOption(String strName, String strValue) {
        System.out.println(getClass().getName() + ".setOption: " + strName + "=" + strValue);
        if (MP3_SAMPLE_RATE_PROPERTY.endsWith(strName)) {
            this.iSampleRate = Integer.parseInt(strValue);
        } else if (MP3_CHANNEL_COUNT_PROPERTY.endsWith(strName)) {
            this.iChannelCount = Integer.parseInt(strValue);
        } else {
            System.out.println("got unexpected config parameter: " + strName);
        }
    }

    public void decode(byte[] baData, int iOffset, int iLength) {
        if (null == this.is) {
            this.is = new SwappingByteArrayInputStream(baData, iOffset, iLength);
            this.bitstream = new Bitstream(is);
        } else {
            is.swapByteArray(baData, iOffset, iLength);
        }
        try {
            this.header = this.bitstream.readFrame();
            this.decoder.decodeFrame(this.header, this.bitstream);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("problem decoding mp3", e);
        }
    }

    public int getDecodedLengthInBytes() {
        int iLength = this.sampleBuffer.getBufferLength() * 2;
        return iLength;
    }

    public void getDecodedBytes(byte[] baOutput, int iOffset) {
        short[] sa = this.sampleBuffer.getBuffer();
        int outputSize = this.sampleBuffer.getBufferLength();
        for (int i = 0; i < outputSize; i++) {
            int dx = iOffset + (i << 1);
            baOutput[dx] = (byte) (sa[i] & 0xff);
            baOutput[dx + 1] = (byte) ((sa[i] >> 8) & 0xff);
        }
        this.bitstream.closeFrame();
    }

    public WHAudioFormat getFormat() {
        if (null == this.format) {
            this.format = new WHAudioFormat(this.iChannelCount, this.iSampleRate, 16);
            this.decoder = new Decoder();
            this.sampleBuffer = new SampleBuffer(this.format.getSamplesPerSecond(), this.format.getChannelCount());
            this.decoder.setOutputBuffer(this.sampleBuffer);
        }
        return this.format;
    }

    public int getOutputFrameSizeInBytes() {
        return getDecodedLengthInBytes();
    }
}

class SwappingByteArrayInputStream extends ByteArrayInputStream {

    public SwappingByteArrayInputStream(byte[] ba) {
        super(ba);
    }

    public SwappingByteArrayInputStream(byte[] ba, int iOffset, int iLength) {
        super(ba, iOffset, iLength);
    }

    public void swapByteArray(byte[] ba) {
        this.buf = ba;
        this.count = ba.length;
        this.mark(0);
        this.pos = 0;
    }

    public void swapByteArray(byte[] ba, int iOffset, int iLength) {
        this.buf = ba;
        this.count = iLength;
        this.mark(iOffset);
        this.pos = iOffset;
    }
}

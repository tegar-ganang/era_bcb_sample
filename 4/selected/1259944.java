package com.sts.webmeet.server.util.audio;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;

public class Mp3StreamPacketizer {

    private Bitstream bs;

    private DataInputStream is;

    private int iFrameLength;

    private boolean bFirstFrame = true;

    private Header header;

    public static final int MP3_HEADER_LENGTH_BYTES = 4;

    public Mp3StreamPacketizer(File fileMp3) throws Exception {
        this.is = new DataInputStream(new BufferedInputStream(new FileInputStream(fileMp3)));
        this.bs = new Bitstream(new FileInputStream(fileMp3));
        this.header = this.bs.readFrame();
        this.bs.unreadFrame();
    }

    public float getMillisecondsPerPacket() {
        return this.header.ms_per_frame();
    }

    public int getSampleFequency() {
        return this.header.frequency();
    }

    public int getChannelCount() {
        return this.header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
    }

    public int getNextPacketLengthInBytes() throws Exception {
        this.header = this.bs.readFrame();
        if (null != header) {
            this.iFrameLength = header.calculate_framesize() + MP3_HEADER_LENGTH_BYTES;
        } else {
            this.iFrameLength = -1;
        }
        this.bs.closeFrame();
        return this.iFrameLength;
    }

    public void getPacket(byte[] ba, int iOffset, int iLength) throws IOException {
        if (bFirstFrame) {
            bFirstFrame = false;
            advanceToFirstFrame();
        }
        this.is.readFully(ba, iOffset, iLength);
    }

    private void advanceToFirstFrame() throws IOException {
        int iByteOne = 0;
        int iByteTwo = 0;
        while (iByteOne != -1) {
            this.is.mark(10);
            iByteOne = this.is.read();
            if (-1 == (byte) iByteOne) {
                this.is.reset();
                return;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Mp3StreamPacketizer mp3p = new Mp3StreamPacketizer(new File(args[0]));
        System.out.println(mp3p.getMillisecondsPerPacket());
        int iFrames = 0;
        byte[] ba = null;
        while (true) {
            int iLen = mp3p.getNextPacketLengthInBytes();
            if (-1 == iLen) {
                break;
            } else {
                if (null == ba || ba.length < iLen) {
                    ba = new byte[iLen];
                }
                mp3p.getPacket(ba, 0, iLen);
                iFrames++;
            }
        }
        System.out.println("frames: " + iFrames);
    }
}

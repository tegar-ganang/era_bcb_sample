package com.frinika.sequencer.model.audio;

import java.io.IOException;
import java.io.RandomAccessFile;
import javax.sound.sampled.AudioFormat;

public class AudioWavReader {

    protected static String sp = "     ";

    static String indent = sp + "     ";

    protected long audioDataStartBytePtr;

    protected int audioDataByteLength;

    protected int lengthInFrames = 0;

    protected int bytecount = 0;

    protected int riffdata = 0;

    protected AudioFormat format;

    protected int nChannels;

    private RandomAccessFile fis;

    public AudioWavReader(RandomAccessFile fis) throws IOException {
        String sfield = "";
        this.fis = fis;
        fis.seek(0);
        long filesize = (fis.length());
        readChunkHeader(fis);
        while (bytecount < riffdata) {
            sfield = "";
            for (int i = 1; i <= 4; i++) sfield += (char) fis.readByte();
            int chunkSize = 0;
            for (int i = 0; i < 4; i++) chunkSize += fis.readUnsignedByte() * (int) Math.pow(256, i);
            if (sfield.equals("data")) {
                audioDataStartBytePtr = fis.getFilePointer();
                audioDataByteLength = (int) (filesize - audioDataStartBytePtr);
            }
            bytecount += (8 + chunkSize);
            if (sfield.equals("fmt ")) {
                readFormat(fis, chunkSize);
            } else fis.skipBytes(chunkSize);
            lengthInFrames = chunkSize / format.getFrameSize();
        }
        if ((8 + bytecount) != (int) filesize) System.out.println(sp + "!!!!!!! Problem with file structure  !!!!!!!!! ");
    }

    /**
	 * 
	 * read the data size. this will be zero until it is closed.
	 * 
	 * @return
	 * @throws IOException
	 */
    public int getDataSize() throws IOException {
        long ptr = fis.getFilePointer();
        fis.seek(40);
        int chunkSize = 0;
        for (int i = 0; i < 4; i++) chunkSize += fis.readUnsignedByte() * (int) Math.pow(256, i);
        riffdata = chunkSize;
        fis.seek(ptr);
        lengthInFrames = chunkSize / format.getFrameSize();
        System.out.println(" GET DATA SIZE " + lengthInFrames);
        return chunkSize;
    }

    protected void readChunkHeader(RandomAccessFile fis) throws IOException {
        String sfield = "";
        int chunkSize = 0;
        for (int i = 1; i <= 4; i++) sfield += (char) fis.readByte();
        if (!sfield.equals("RIFF")) {
            System.out.println(" ****  Not a valid RIFF file  ****");
            return;
        }
        for (int i = 0; i < 4; i++) chunkSize += fis.readUnsignedByte() * (int) Math.pow(256, i);
        sfield = "";
        for (int i = 1; i <= 4; i++) sfield += (char) fis.readByte();
        riffdata = chunkSize;
        bytecount = 4;
    }

    public int getLengthInFrames() {
        return lengthInFrames;
    }

    protected void readFormat(RandomAccessFile fis, int chunkSize) throws IOException {
        if (chunkSize < 16) {
            System.out.println(" ****  Not a valid fmt chunk  ****");
            return;
        }
        int wFormatTag = fis.readUnsignedByte();
        fis.skipBytes(1);
        nChannels = fis.readUnsignedByte();
        fis.skipBytes(1);
        int nSamplesPerSec = 0;
        for (int i = 0; i < 4; i++) nSamplesPerSec += fis.readUnsignedByte() * (int) Math.pow(256, i);
        int nAvgBytesPerSec = 0;
        for (int i = 0; i < 4; i++) nAvgBytesPerSec += fis.readUnsignedByte() * (int) Math.pow(256, i);
        int nBlockAlign = 0;
        for (int i = 0; i < 2; i++) nBlockAlign += fis.readUnsignedByte() * (int) Math.pow(256, i);
        int nBitsPerSample = 0;
        if (wFormatTag == 1) {
            nBitsPerSample = fis.readUnsignedByte();
            fis.skipBytes(1);
        } else fis.skipBytes(2);
        fis.skipBytes(chunkSize - 16);
        format = new AudioFormat(nSamplesPerSec, nBitsPerSample, nChannels, true, false);
    }

    public AudioFormat getFormat() {
        return format;
    }

    public int getChannels() {
        return nChannels;
    }
}

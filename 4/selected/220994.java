package com.frinika.sequencer.model.audio;

import java.io.IOException;
import java.io.RandomAccessFile;
import javax.sound.sampled.AudioFormat;

/**
 * Wraps up a wav file and allows access using a RandomAccessFile.
 * provides a view of the file that allows reads before the start and after the end of the file
 * you'll just get zeros returned.
 * 
 * @deprecated
 */
public class DAudioReader {

    static String sp = "     ";

    static String indent = sp + "     ";

    private long audioDataStartBytePtr;

    private int audioDataByteLength;

    private int lengthInFrames;

    private RandomAccessFile fis;

    private int bytecount = 0;

    private int riffdata = 0;

    private AudioFormat format;

    int nChannels;

    private long fPtrBytes;

    public DAudioReader(RandomAccessFile fis) throws IOException {
        String sfield = "";
        this.fis = fis;
        long filesize = (fis.length());
        readChunkHeader();
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
                readFormat(chunkSize);
            } else fis.skipBytes(chunkSize);
            lengthInFrames = chunkSize / format.getFrameSize();
        }
        if ((8 + bytecount) != (int) filesize) System.out.println(sp + "!!!!!!! Problem with file structure  !!!!!!!!! ");
    }

    private void readChunkHeader() throws IOException {
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

    private void readFormat(int chunkSize) throws IOException {
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

    /**
	 * 

	 * 
	 * @param framePos
	 *            frame postition reltive to start of audio. e.g. zero is start of audio.
	 *             
	 * @throws IOException
	 */
    public void seekFrame(long framePos) throws IOException {
        fPtrBytes = framePos * 2 * nChannels;
        if (fPtrBytes >= 0) {
            if (fPtrBytes < audioDataByteLength) fis.seek(fPtrBytes + audioDataStartBytePtr);
        } else {
            fis.seek(audioDataStartBytePtr);
        }
    }

    /**
	 * 
	 * 
	 * Read from file into byte buffer and advance the fPtrBytes pointer
	 * it is OK to read before/after start/end of the file you'll just get zeros.
	 * 
	 * @param byteBuffer
	 *            buffer to fill
	 * @param offSet
	 *            offset into byteBuffer
	 * @param n
	 *            number of bytes to be read
	 * @throws IOException
	 */
    public void read(byte[] byteBuffer, int offSet, int n) throws IOException {
        if (fPtrBytes < 0) {
            int nRead = (int) (n + fPtrBytes);
            if (nRead > 0) {
                int nFill = n - nRead;
                for (int i = 0; i < nFill; i++) byteBuffer[i + offSet] = 0;
                fis.read(byteBuffer, offSet + nFill, nRead);
            } else {
                for (int i = 0; i < n; i++) byteBuffer[i + offSet] = 0;
            }
        } else if (fPtrBytes > audioDataByteLength) {
            for (int i = 0; i < n; i++) byteBuffer[i + offSet] = 0;
        } else {
            int nExtra = (int) (fPtrBytes + n - audioDataByteLength);
            if (nExtra > 0) {
                int nRead = n - nExtra;
                fis.read(byteBuffer, offSet, nRead);
                for (int i = nRead; i < n; i++) byteBuffer[i + offSet] = 0;
            } else {
                int nread = fis.read(byteBuffer, offSet, n);
                if (nread != n) try {
                    throw new Exception(" Ooops only read " + nread + " out of " + n + "  " + offSet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        fPtrBytes += n;
    }

    public int getChannels() {
        return nChannels;
    }

    public boolean eof() {
        try {
            return fPtrBytes - audioDataStartBytePtr >= fis.length();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public long getCurrentFrame() {
        return (fPtrBytes - audioDataStartBytePtr) / nChannels / 2;
    }
}

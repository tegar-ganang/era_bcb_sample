package com.frinika.sequencer.model.audio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.sound.sampled.AudioFormat;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;

public class AudioWriter implements AudioProcess {

    private RandomAccessFile fis;

    int count = 0;

    File file = null;

    private int nChannel;

    private byte byteBuffer[];

    public AudioWriter(File file, AudioFormat format) throws IOException {
        this.file = file;
        this.fis = new RandomAccessFile(file, "rw");
        this.fis.setLength(0);
        fis.seek(0);
        fis.write("RIFF".getBytes(), 0, 4);
        writeInt(36, fis);
        fis.write("WAVE".getBytes(), 0, 4);
        fis.write("fmt ".getBytes(), 0, 4);
        writeInt(0x10, fis);
        writeShort(1, fis);
        writeShort(nChannel = format.getChannels(), fis);
        writeInt((int) (format.getFrameRate()), fis);
        writeInt((int) (format.getFrameRate()) * format.getChannels() * 2, fis);
        writeShort((short) format.getChannels() * 2, fis);
        writeShort(16, fis);
        fis.write("data".getBytes(), 0, 4);
        writeInt(0, fis);
    }

    private void writeShort(int i, RandomAccessFile fis2) throws IOException {
        byte[] buff = new byte[2];
        buff[0] = (byte) (0xff & i);
        buff[1] = (byte) (0xff & (i >> 8));
        fis.write(buff, 0, 2);
    }

    private void writeInt(int i, RandomAccessFile fis2) throws IOException {
        byte[] buff = new byte[4];
        buff[0] = (byte) (0xff & i);
        buff[1] = (byte) (0xff & (i >> 8));
        buff[2] = (byte) (0xff & (i >> 16));
        buff[3] = (byte) (0xff & (i >> 24));
        fis.write(buff, 0, 4);
    }

    public void write(byte[] byteBuffer, int offSet, int n) throws IOException {
        fis.write(byteBuffer, offSet, n);
        count += n;
    }

    public void close() {
        if (fis == null) return;
        if (count == 0) {
            discard();
            return;
        }
        long fileSize = 0;
        try {
            fileSize = fis.getFilePointer();
            fis.seek(4);
            writeInt((int) (fileSize - 8), fis);
            fis.seek(40);
            writeInt((int) count, fis);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Closing " + file + "  size/count:" + fileSize + "/" + count);
        fis = null;
    }

    /**
	 * If file ends up not being used then call this.
	 * 
	 */
    public void discard() {
        if (fis == null) return;
        try {
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fis = null;
        file.delete();
        file = null;
    }

    protected void finalize() {
        if (fis == null) return;
        close();
    }

    public int processAudio(AudioBuffer buffer) {
        float[] left;
        float[] right;
        if (byteBuffer == null || byteBuffer.length != buffer.getSampleCount() * 2 * nChannel) byteBuffer = new byte[buffer.getSampleCount() * 2 * nChannel];
        int nSamp = buffer.getSampleCount();
        int count = 0;
        if (nChannel == 2) {
            left = buffer.getChannel(0);
            right = buffer.getChannel(1);
            for (int n = 0; n < nSamp; n++) {
                short leftI = (short) (left[n] * 32768f);
                short rightI = (short) (right[n] * 32768f);
                byteBuffer[count++] = (byte) (0xff & leftI);
                byteBuffer[count++] = (byte) (0xff & (leftI >> 8));
                byteBuffer[count++] = (byte) (0xff & rightI);
                byteBuffer[count++] = (byte) (0xff & (rightI >> 8));
            }
        } else {
            left = buffer.getChannel(0);
            for (int n = 0; n < nSamp; n++) {
                short leftI = (short) (left[n] * 32768f);
                byteBuffer[count++] = (byte) (0xff & leftI);
                byteBuffer[count++] = (byte) (0xff & (leftI >> 8));
            }
        }
        try {
            write(byteBuffer, 0, count);
        } catch (IOException e) {
            e.printStackTrace();
            return AudioProcess.AUDIO_DISCONNECT;
        }
        return AUDIO_OK;
    }

    public File getFile() {
        return file;
    }

    public void open() {
    }
}

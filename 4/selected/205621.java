package com.frinika.audio.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.sound.sampled.AudioFormat;

/**
 * 
 *  minimal implementation of a wav writer. Writes bytes data.
 *
 * @author pjl
 */
public class BasicAudioWriter {

    protected byte[] byteBuffer;

    int count = 0;

    File file = null;

    protected RandomAccessFile fis;

    protected int nChannel;

    public BasicAudioWriter(File file, AudioFormat format) throws IOException {
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

    public void close() {
        if (fis == null) {
            return;
        }
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
        if (fis == null) {
            return;
        }
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
        if (fis == null) {
            return;
        }
        close();
    }

    public File getFile() {
        return file;
    }

    public void open() {
    }

    public void write(byte[] byteBuffer, int offSet, int n) throws IOException {
        fis.write(byteBuffer, offSet, n);
        count += n;
    }

    private void writeInt(int i, RandomAccessFile fis2) throws IOException {
        byte[] buff = new byte[4];
        buff[0] = (byte) (255 & i);
        buff[1] = (byte) (255 & (i >> 8));
        buff[2] = (byte) (255 & (i >> 16));
        buff[3] = (byte) (255 & (i >> 24));
        fis.write(buff, 0, 4);
    }

    private void writeShort(int i, RandomAccessFile fis2) throws IOException {
        byte[] buff = new byte[2];
        buff[0] = (byte) (255 & i);
        buff[1] = (byte) (255 & (i >> 8));
        fis.write(buff, 0, 2);
    }
}

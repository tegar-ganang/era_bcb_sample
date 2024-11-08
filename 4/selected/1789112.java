package com.itstherules.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class StreamWriter {

    private long pos;

    private FileChannel chan;

    private File srcFile;

    private RandomAccessFile raf;

    private boolean isOpened;

    private StreamReader inStream;

    private boolean debug;

    public StreamWriter() {
        pos = 0;
        chan = null;
        srcFile = null;
        raf = null;
        isOpened = false;
        debug = false;
    }

    public StreamWriter(File srcFile) {
        pos = 0;
        chan = null;
        this.srcFile = srcFile;
        raf = null;
        isOpened = false;
        debug = false;
    }

    public void open() throws FileNotFoundException {
        raf = new RandomAccessFile(srcFile, "rw");
        chan = raf.getChannel();
        pos = 0;
        isOpened = true;
    }

    public boolean isOpen() {
        return this.isOpened;
    }

    public void close() {
        try {
            chan.close();
            raf.close();
        } catch (Exception e) {
            if (debug) {
                System.out.println("Error - unable to close stream");
                e.printStackTrace();
            }
        } finally {
            chan = null;
            raf = null;
            isOpened = false;
        }
    }

    public int write(ByteBuffer bbuf) {
        int lenWritten = 0;
        try {
            lenWritten = chan.write(bbuf);
            pos += lenWritten;
        } catch (IOException ex) {
            System.out.println("Error - unable to write specified bytes");
            if (debug) {
                ex.printStackTrace();
            }
        }
        return lenWritten;
    }

    public long writeDirect(long startOff, long len) {
        long lenWritten = 0;
        try {
            lenWritten = inStream.getChan().transferTo(startOff, len, chan);
            pos += lenWritten;
        } catch (IOException ex) {
            System.out.println("Error - unable to transfer specified bytes from srcfile");
            if (debug) {
                ex.printStackTrace();
            }
        }
        return lenWritten;
    }

    public long getPos() {
        return pos;
    }

    public void setPos(long pos) {
        this.pos = pos;
    }

    public FileChannel getChan() {
        return chan;
    }

    public void setChan(FileChannel chan) {
        this.chan = chan;
    }

    public void setInputStream(StreamReader inStream) {
        this.inStream = inStream;
    }

    public StreamReader getInputStream() {
        return inStream;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}

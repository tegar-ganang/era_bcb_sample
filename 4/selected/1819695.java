package com.sonydadc.dw.jflv.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 * @author Jon Keys
 */
public class StreamReader {

    private static final int CHUNK_SIZE = 1048576;

    private long pos;

    private long fpos;

    private long fsize;

    private long byt2write;

    private int written;

    private boolean debug;

    private MappedByteBuffer data;

    private FileChannel chan;

    private File srcFile;

    private RandomAccessFile raf;

    private boolean isOpened;

    /** Creates a new instance of StreamReader */
    public StreamReader() {
        fsize = 0;
        byt2write = 0;
        written = 0;
        pos = 0;
        fpos = 0;
        data = null;
        chan = null;
        srcFile = null;
        raf = null;
        isOpened = false;
        debug = false;
    }

    public StreamReader(File srcFile) {
        fsize = srcFile.length();
        byt2write = 0;
        written = 0;
        pos = 0;
        fpos = 0;
        data = null;
        chan = null;
        this.srcFile = srcFile;
        raf = null;
        isOpened = false;
    }

    public void open() throws FileNotFoundException {
        raf = new RandomAccessFile(srcFile, "r");
        chan = raf.getChannel();
        fsize = srcFile.length();
        byt2write = 0;
        written = 0;
        pos = 0;
        fpos = 0;
        isOpened = true;
        try {
            fillBuffer();
        } catch (Exception e) {
            isOpened = false;
            System.out.println("Error - unable to initialize buffer");
            if (debug) {
                e.printStackTrace();
            }
        }
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

    private void fillBuffer() throws EoflvException {
        if (fpos >= fsize) {
            throw new EoflvException();
        } else if ((fpos + CHUNK_SIZE) > fsize) {
            byt2write = fsize - fpos;
        } else {
            byt2write = CHUNK_SIZE;
        }
        try {
            data = chan.map(FileChannel.MapMode.READ_ONLY, fpos, byt2write);
            fpos += byt2write;
        } catch (IOException ex) {
            System.out.println("Error - unable to refill buffer");
            if (debug) {
                ex.printStackTrace();
            }
        }
    }

    public byte[] read(int len) throws EoflvException {
        byte[] buf = new byte[len];
        if (len > data.remaining()) {
            written = data.remaining();
            data.get(buf, 0, written);
            fillBuffer();
            data.get(buf, written, (len - written));
        } else {
            data.get(buf);
        }
        pos += len;
        return buf;
    }

    public void skip(int len) throws EoflvException {
        if (len > data.remaining()) {
            written = data.remaining();
            fillBuffer();
            data.position((len - written));
        } else {
            data.position(data.position() + len);
        }
        pos += len;
    }

    public long getPos() {
        return pos;
    }

    public MappedByteBuffer getData() {
        return data;
    }

    public FileChannel getChan() {
        return chan;
    }

    public void setChan(FileChannel chan) {
        this.chan = chan;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}

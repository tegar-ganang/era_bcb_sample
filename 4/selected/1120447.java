package com.codechimp.jmtf.mtf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michael Bauer
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class MTFDataStream extends InputStream {

    private MTFDataStreamHeader header;

    private RandomAccessFile raf;

    private long lastRead;

    private byte[] cache;

    private int cacheSpot;

    private long read;

    private static final int MAX_CACHE_SIZE = 1048576;

    private long streamLength;

    private long streamStart;

    private Logger logger;

    private long endingOffset;

    public MTFDataStream(RandomAccessFile raf) throws IOException, MTFChecksumFailedException {
        this(raf, new MTFDataStreamHeader(raf, -1));
    }

    public MTFDataStream(RandomAccessFile raf, MTFDataStreamHeader header) throws IOException, MTFChecksumFailedException {
        logger = Logger.getLogger("com.codechimp.jmtf.mtf");
        this.raf = raf;
        if (!header.passChecksum()) {
            throw new MTFChecksumFailedException("MTFDataStreamHeader failed checksum.");
        }
        setStreamHeader(header);
        this.lastRead = -1;
        this.read = 0;
        this.streamLength = header.getLength();
        this.streamStart = header.getHeaderEndingOffset();
        long nOffset = raf.getFilePointer() + streamLength;
        if (nOffset > 0) {
            logger.log(Level.FINEST, "[MTFDataStream-const(file,header)] Jump to offset:\n" + nOffset);
            raf.seek(nOffset);
        }
    }

    public void setStreamHeader(MTFDataStreamHeader header) {
        this.header = header;
    }

    public MTFDataStreamHeader getStreamHeader() {
        return this.header;
    }

    public long getStreamLength() {
        return streamLength;
    }

    public long getStreamStartingOffset() {
        return streamStart;
    }

    public long getStartingOffset() {
        return this.header.getStartingOffset();
    }

    public long getEndingingOffset() {
        return this.streamStart + this.streamLength;
    }

    public String toString() {
        String str = "+++++++++++++++++++++++++++++++++++++++++++++";
        str += "\nData Stream";
        str += "\n" + this.getStreamHeader().toString();
        str += "\n+++++++++++++++++++++++++++++++++++++++++++++";
        return str;
    }

    public int read() throws IOException {
        int ret;
        long curr = raf.getFilePointer();
        if (lastRead < 0) {
            lastRead = header.getHeaderEndingOffset();
        }
        raf.seek(lastRead);
        logger.log(Level.FINEST, "MTFDataStream-read() - Starting: " + header.getHeaderEndingOffset() + ", Endings: " + (header.getHeaderEndingOffset() + this.streamLength) + ", Current: " + raf.getFilePointer());
        if (cache == null || read < getSize()) {
            ret = readNext();
            if (ret < 0) {
                ret = 255 - (Math.abs(ret) - 1);
            }
            read++;
        } else {
            ret = -1;
        }
        if (read % (1024L * 1024L) == 0) {
            logger.log(Level.INFO, "Stream: Read " + read + " bytes of " + getSize() + "(" + (((double) read / (double) getSize()) * 100.0) + ")...");
        }
        lastRead = raf.getFilePointer();
        raf.seek(curr);
        return ret;
    }

    public int read(byte[] bs) throws IOException {
        long curr = raf.getFilePointer();
        if (lastRead < 0) {
            lastRead = header.getHeaderEndingOffset();
        }
        raf.read(bs);
        lastRead = raf.getFilePointer();
        read += bs.length;
        raf.seek(curr);
        return bs.length;
    }

    public int available() throws IOException {
        return (int) getSize();
    }

    private long getSize() {
        return this.streamLength;
    }

    private long getSizeLeft() {
        return getEndingingOffset() - lastRead;
    }

    private byte readNext() throws IOException {
        if (cache == null || cacheSpot >= cache.length) {
            int chunkSize = getSizeLeft() < MAX_CACHE_SIZE ? (int) getSizeLeft() : MAX_CACHE_SIZE;
            cache = new byte[chunkSize];
            raf.read(cache);
            cacheSpot = 0;
        }
        if (cache.length == 0) {
            return -1;
        }
        return cache[cacheSpot++];
    }

    public void writeToFile(File file, File source) throws IOException {
        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(file));
        BufferedInputStream bin = new BufferedInputStream(new FileInputStream(source));
        bin.skip(header.getHeaderEndingOffset());
        for (long i = 0; i < this.streamLength; i++) {
            bout.write(bin.read());
        }
        bin.close();
        bout.close();
    }
}

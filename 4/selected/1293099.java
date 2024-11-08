package com.jmex.audio.stream;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Iterator;
import com.jmex.audio.filter.Filter;

public class WavInputStream extends AudioInputStream {

    private static final int RIFFid = ('R' << 24) | ('I' << 16) + ('F' << 8) + 'F';

    private static final int WAVEid = ('W' << 24) | ('A' << 16) + ('V' << 8) + 'E';

    private static final int fmtid = ('f' << 24) | ('m' << 16) + ('t' << 8) + ' ';

    private static final int dataid = ('d' << 24) | ('a' << 16) + ('t' << 8) + 'a';

    private int numChannels;

    private int sampleDepth;

    private int sampleRate;

    private long fileSize;

    private long headerSize;

    public WavInputStream(URL resource) throws IOException {
        super(resource, -1);
        dataIn = new DataInputStream(in);
        if (dataIn.readInt() != RIFFid) throw (new IOException("Not a valid RIFF file"));
        int firstByte = (0x000000FF & dataIn.readByte());
        int secondByte = (0x000000FF & dataIn.readByte());
        int thirdByte = (0x000000FF & dataIn.readByte());
        int fourthByte = (0x000000FF & dataIn.readByte());
        fileSize = 8L + (firstByte << 0 | secondByte << 8 | thirdByte << 16 | fourthByte << 24) & 0xFFFFFFFFL;
        if (dataIn.readInt() != WAVEid) throw (new IOException("Not a valid WAVE file"));
        headerSize += 12;
        seekAudio();
        setLength((fileSize - headerSize) * 8f / (getChannelCount() * getBitRate() * getDepth()));
    }

    /**
     * Reads up to len bytes of data from the input stream into a ByteBuffer.
     * 
     * @param b
     *            the buffer into which the data is read.
     * @param off
     *            the start offset of the data.
     * @param len
     *            the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     */
    public int read(ByteBuffer b, int off, int len) throws IOException {
        byte[] buffer = new byte[b.capacity()];
        int bytesRead = read(buffer, off, len);
        if (bytesRead > 0 && filters.size() > 0) {
            Iterator<Filter> it = filters.iterator();
            while (it.hasNext()) {
                buffer = it.next().filter(buffer);
            }
        }
        b.put(buffer);
        b.position(off);
        return bytesRead;
    }

    private DataInputStream dataIn;

    public int getChannelCount() {
        return numChannels;
    }

    public int getDepth() {
        return sampleDepth;
    }

    public int getBitRate() {
        return sampleRate;
    }

    public boolean marksupported() {
        return false;
    }

    public void readSample(byte b[], int start, int length) throws IOException {
        in.read(b, start * numChannels, length * numChannels);
    }

    public void readSample(short b[], int start, int length) throws IOException {
        for (int off = 0; off < length; off++) for (int channel = 0; channel < numChannels; channel++) b[channel + (start + off) * numChannels] = readShort();
    }

    public void readSample(int b[], int start, int length) throws IOException {
        for (int off = 0; off < length; off++) for (int channel = 0; channel < numChannels; channel++) b[b[channel + (start + off) * numChannels]] = readInt();
    }

    private short readShort() throws IOException {
        int a = in.read();
        int b = in.read();
        if (b == -1) throw (new EOFException());
        return (short) (a | (b << 8));
    }

    private int readInt() throws IOException {
        int a = in.read();
        int b = in.read();
        int c = in.read();
        int d = in.read();
        if (d == -1) throw (new EOFException());
        return a | (b << 8) | (c << 16) | (d << 24);
    }

    private void readfmt(int size) throws IOException {
        if (readShort() != 1) throw (new IOException("Can only read PCM files"));
        numChannels = readShort();
        sampleRate = readInt();
        dataIn.readInt();
        dataIn.readShort();
        sampleDepth = readShort();
        headerSize += 16;
        if (sampleDepth != 8 && sampleDepth != 16 && sampleDepth != 32) throw (new IOException("Only 8, 16, or 32 bit samples are handled"));
        int read = 16;
        while (size > read) {
            read();
            read++;
            headerSize++;
        }
    }

    private void seekAudio() throws IOException {
        while (true) {
            int chunkType = dataIn.readInt();
            int ckSize = readInt();
            headerSize += 8;
            switch(chunkType) {
                case fmtid:
                    readfmt(ckSize);
                    break;
                case dataid:
                    return;
                default:
                    if (in.skip(ckSize) != ckSize) throw (new IOException("Input didn't fully skip chunk"));
                    headerSize += ckSize;
            }
        }
    }

    @Override
    public WavInputStream makeNew() throws IOException {
        WavInputStream rVal = new WavInputStream(getResource());
        rVal.filters.addAll(filters);
        return rVal;
    }
}

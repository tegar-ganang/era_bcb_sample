package jmri.jmrit.sound;

/**
 * Wrap a byte array to provide WAV file functionality
 *
 * @author	Bob Jacobsen  Copyright (C) 2006
 * @version	$Revision: 1.8 $
 */
public class WavBuffer {

    /**
     * Create from already existing byte array.
     * The array is expected to be in .wav format, starting
     * with a RIFF header.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EI_EXPOSE_REP2")
    public WavBuffer(byte[] content) {
        buffer = content;
        initFmt();
        initData();
    }

    /**
     * Create from contents of file.
     * The file contents are expected to be in .wav format, starting
     * with a RIFF header.
     */
    public WavBuffer(java.io.File file) throws java.io.IOException {
        if (file == null) {
            throw new java.io.IOException("Null file during ctor");
        }
        java.io.InputStream s = new java.io.BufferedInputStream(new java.io.FileInputStream(file));
        try {
            buffer = new byte[(int) file.length()];
            s.read(buffer);
            initFmt();
            initData();
        } catch (java.io.IOException e1) {
            log.error("error reading file", e1);
            throw e1;
        } finally {
            try {
                s.close();
            } catch (java.io.IOException e2) {
                log.error("Exception closing file", e2);
            }
        }
    }

    /**
     * Find a specific header in the .wav fragment
     * @return offset of the 1st byte of the header in the buffer
     */
    public int findHeader(int i1, int i2, int i3, int i4) {
        int index = 12;
        while (index < buffer.length) {
            if (buffer[index] == i1 && buffer[index + 1] == i2 && buffer[index + 2] == i3 && buffer[index + 3] == i4) {
                return index;
            } else {
                index = index + 8 + fourByte(index + 4);
            }
        }
        log.error("Didn't find chunk");
        return 0;
    }

    /**
     * Cache info from (first) "fmt " header
     */
    private void initFmt() {
        fmtOffset = findHeader(0x66, 0x6D, 0x74, 0x20);
        if (fmtOffset > 0) return;
        log.error("Didn't find fmt chunk");
    }

    /**
     * Cache info from (first) "data" header
     */
    private void initData() {
        dataOffset = findHeader(0x64, 0x61, 0x74, 0x61);
        if (dataOffset > 0) return;
        log.error("Didn't find data chunk");
    }

    int fmtOffset;

    int dataOffset;

    byte[] buffer;

    public float getSampleRate() {
        return fourByte(fmtOffset + 12);
    }

    public int getSampleSizeInBits() {
        return twoByte(fmtOffset + 22);
    }

    public int getChannels() {
        return twoByte(fmtOffset + 10);
    }

    public boolean getBigEndian() {
        return false;
    }

    public boolean getSigned() {
        return (getSampleSizeInBits() > 8);
    }

    /**
     * Offset to the first data byte in the buffer
     */
    public int getDataStart() {
        return dataOffset + 8;
    }

    /**
     * Size of the data segment in bytes
     */
    public int getDataSize() {
        return fourByte(dataOffset + 4);
    }

    /**
     * Offset to the last data byte in the buffer. One more than
     * this points to the next header.
     */
    public int getDataEnd() {
        return dataOffset + 8 + getDataSize() - 1;
    }

    int twoByte(int index) {
        return buffer[index] + buffer[index + 1] * 256;
    }

    int fourByte(int index) {
        return (buffer[index] & 0xFF) + (buffer[index + 1] & 0xFF) * 256 + (buffer[index + 2] & 0xFF) * 256 * 256 + (buffer[index + 3] & 0xFF) * 256 * 256 * 256;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EI_EXPOSE_REP")
    public byte[] getByteArray() {
        return buffer;
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WavBuffer.class.getName());
}

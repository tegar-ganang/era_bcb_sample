package org.jaudiotagger.audio.flac.metadatablock;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * Metadata Block Header
 */
public class MetadataBlockHeader {

    public static final int HEADER_LENGTH = 4;

    private boolean isLastBlock;

    private int dataLength;

    private byte[] bytes;

    private BlockType blockType;

    /**
     * Create header by reading from file
     *
     * @param raf
     * @return
     * @throws IOException
     */
    public static MetadataBlockHeader readHeader(RandomAccessFile raf) throws IOException {
        ByteBuffer rawdata = ByteBuffer.allocate(HEADER_LENGTH);
        int bytesRead = raf.getChannel().read(rawdata);
        if (bytesRead < HEADER_LENGTH) {
            throw new IOException("Unable to read required number of databytes read:" + bytesRead + ":required:" + HEADER_LENGTH);
        }
        rawdata.rewind();
        return new MetadataBlockHeader(rawdata);
    }

    public String toString() {
        return "BlockType:" + blockType + " DataLength:" + dataLength + " isLastBlock:" + isLastBlock;
    }

    /**
     * Construct header by reading bytes
     *
     * @param rawdata
     */
    public MetadataBlockHeader(ByteBuffer rawdata) {
        isLastBlock = ((rawdata.get(0) & 0x80) >>> 7) == 1;
        int type = rawdata.get(0) & 0x7F;
        if (type < BlockType.values().length) {
            blockType = BlockType.values()[type];
        }
        dataLength = (u(rawdata.get(1)) << 16) + (u(rawdata.get(2)) << 8) + (u(rawdata.get(3)));
        bytes = new byte[HEADER_LENGTH];
        for (int i = 0; i < HEADER_LENGTH; i++) {
            bytes[i] = rawdata.get(i);
        }
    }

    /**
     * Construct a new header in order to write metadatablock to file
     *
     * @param isLastBlock
     * @param blockType
     * @param dataLength
     */
    public MetadataBlockHeader(boolean isLastBlock, BlockType blockType, int dataLength) {
        ByteBuffer rawdata = ByteBuffer.allocate(HEADER_LENGTH);
        this.blockType = blockType;
        this.isLastBlock = isLastBlock;
        this.dataLength = dataLength;
        byte type;
        if (isLastBlock) {
            type = (byte) (0x80 | blockType.getId());
        } else {
            type = (byte) blockType.getId();
        }
        rawdata.put(type);
        rawdata.put((byte) ((dataLength & 0xFF0000) >>> 16));
        rawdata.put((byte) ((dataLength & 0xFF00) >>> 8));
        rawdata.put((byte) (dataLength & 0xFF));
        bytes = new byte[HEADER_LENGTH];
        for (int i = 0; i < HEADER_LENGTH; i++) {
            bytes[i] = rawdata.get(i);
        }
    }

    private int u(int i) {
        return i & 0xFF;
    }

    public int getDataLength() {
        return dataLength;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public boolean isLastBlock() {
        return isLastBlock;
    }

    public byte[] getBytesWithoutIsLastBlockFlag() {
        bytes[0] = (byte) (bytes[0] & 0x7F);
        return bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }
}

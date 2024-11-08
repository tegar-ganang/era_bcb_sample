package org.jaudiotagger.audio.mp4.atom;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.audio.mp4.Mp4NotMetaFieldKey;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * StcoBox ( media (stream) header), holds offsets into the Audio data
 */
public class Mp4StcoBox extends AbstractMp4Box {

    public static final int VERSION_FLAG_POS = 0;

    public static final int OTHER_FLAG_POS = 1;

    public static final int NO_OF_OFFSETS_POS = 4;

    public static final int VERSION_FLAG_LENGTH = 1;

    public static final int OTHER_FLAG_LENGTH = 3;

    public static final int NO_OF_OFFSETS_LENGTH = 4;

    public static final int OFFSET_LENGTH = 4;

    private int noOfOffSets = 0;

    private int firstOffSet;

    /**
     * Construct box from data and show contents
     *
     * @param header header info
     * @param buffer data of box (doesnt include header data)
     */
    public Mp4StcoBox(Mp4BoxHeader header, ByteBuffer buffer) {
        this.header = header;
        dataBuffer = buffer.slice();
        dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH);
        this.noOfOffSets = Utils.getIntBE(dataBuffer, dataBuffer.position(), (dataBuffer.position() + NO_OF_OFFSETS_LENGTH - 1));
        dataBuffer.position(dataBuffer.position() + NO_OF_OFFSETS_LENGTH);
        firstOffSet = Utils.getIntBE(dataBuffer, dataBuffer.position(), (dataBuffer.position() + OFFSET_LENGTH - 1));
    }

    public void printTotalOffset() {
        int offset = 0;
        dataBuffer.rewind();
        dataBuffer.position(VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH + NO_OF_OFFSETS_LENGTH);
        for (int i = 0; i < noOfOffSets - 1; i++) {
            offset += Utils.getIntBE(dataBuffer, dataBuffer.position(), (dataBuffer.position() + OFFSET_LENGTH - 1));
            dataBuffer.position(dataBuffer.position() + OFFSET_LENGTH);
        }
        offset += Utils.getIntBE(dataBuffer, dataBuffer.position(), (dataBuffer.position() + OFFSET_LENGTH - 1));
        System.out.println("Print Offset Total:" + offset);
    }

    /**
     * Show All offsets, useful for debugging
     */
    public void printAlloffsets() {
        System.out.println("Print Offsets:start");
        dataBuffer.rewind();
        dataBuffer.position(VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH + NO_OF_OFFSETS_LENGTH);
        for (int i = 0; i < noOfOffSets - 1; i++) {
            int offset = Utils.getIntBE(dataBuffer, dataBuffer.position(), (dataBuffer.position() + OFFSET_LENGTH - 1));
            System.out.println("offset into audio data is:" + offset);
            dataBuffer.position(dataBuffer.position() + OFFSET_LENGTH);
        }
        int offset = Utils.getIntBE(dataBuffer, dataBuffer.position(), (dataBuffer.position() + OFFSET_LENGTH - 1));
        System.out.println("offset into audio data is:" + offset);
        System.out.println("Print Offsets:end");
    }

    public void adjustOffsets(int adjustment) {
        dataBuffer.rewind();
        dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH + NO_OF_OFFSETS_LENGTH);
        for (int i = 0; i < noOfOffSets; i++) {
            int offset = Utils.getIntBE(dataBuffer, dataBuffer.position(), (dataBuffer.position() + NO_OF_OFFSETS_LENGTH - 1));
            offset = offset + adjustment;
            dataBuffer.put(Utils.getSizeBEInt32(offset));
        }
    }

    /**
     * Construct box from data and adjust offets accordingly
     *
     * @param header             header info
     * @param originalDataBuffer data of box (doesnt include header data)
     * @param adjustment
     */
    public Mp4StcoBox(Mp4BoxHeader header, ByteBuffer originalDataBuffer, int adjustment) {
        this.header = header;
        this.dataBuffer = originalDataBuffer.slice();
        dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH);
        this.noOfOffSets = Utils.getIntBE(dataBuffer, dataBuffer.position(), (dataBuffer.position() + NO_OF_OFFSETS_LENGTH - 1));
        dataBuffer.position(dataBuffer.position() + NO_OF_OFFSETS_LENGTH);
        for (int i = 0; i < noOfOffSets; i++) {
            int offset = Utils.getIntBE(dataBuffer, dataBuffer.position(), (dataBuffer.position() + NO_OF_OFFSETS_LENGTH - 1));
            offset = offset + adjustment;
            dataBuffer.put(Utils.getSizeBEInt32(offset));
        }
    }

    /**
     * The number of offsets
     *
     * @return
     */
    public int getNoOfOffSets() {
        return noOfOffSets;
    }

    /**
     * The value of the first offset
     *
     * @return
     */
    public int getFirstOffSet() {
        return firstOffSet;
    }

    public static void debugShowStcoInfo(RandomAccessFile raf) throws IOException, CannotReadException {
        Mp4BoxHeader moovHeader = Mp4BoxHeader.seekWithinLevel(raf, Mp4NotMetaFieldKey.MOOV.getFieldName());
        if (moovHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        ByteBuffer moovBuffer = ByteBuffer.allocate(moovHeader.getLength() - Mp4BoxHeader.HEADER_LENGTH);
        raf.getChannel().read(moovBuffer);
        moovBuffer.rewind();
        Mp4BoxHeader boxHeader = Mp4BoxHeader.seekWithinLevel(moovBuffer, Mp4NotMetaFieldKey.MVHD.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        ByteBuffer mvhdBuffer = moovBuffer.slice();
        Mp4MvhdBox mvhd = new Mp4MvhdBox(boxHeader, mvhdBuffer);
        mvhdBuffer.position(mvhdBuffer.position() + boxHeader.getDataLength());
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4NotMetaFieldKey.TRAK.getFieldName());
        int endOfFirstTrackInBuffer = mvhdBuffer.position() + boxHeader.getDataLength();
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4NotMetaFieldKey.MDIA.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4NotMetaFieldKey.MDHD.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        mvhdBuffer.position(mvhdBuffer.position() + boxHeader.getDataLength());
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4NotMetaFieldKey.MINF.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4NotMetaFieldKey.SMHD.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        mvhdBuffer.position(mvhdBuffer.position() + boxHeader.getDataLength());
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4NotMetaFieldKey.STBL.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4NotMetaFieldKey.STCO.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        Mp4StcoBox stco = new Mp4StcoBox(boxHeader, mvhdBuffer);
        stco.printAlloffsets();
    }
}

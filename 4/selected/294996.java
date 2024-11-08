package org.jaudiotagger.audio.flac;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.flac.metadatablock.BlockType;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataStreamInfo;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockHeader;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 * Read info from Flac file
 */
public class FlacInfoReader {

    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.flac");

    private static final int NO_OF_BITS_IN_BYTE = 8;

    private static final int KILOBYTES_TO_BYTES_MULTIPLIER = 1000;

    public GenericAudioHeader read(RandomAccessFile raf) throws CannotReadException, IOException {
        FlacStreamReader flacStream = new FlacStreamReader(raf);
        flacStream.findStream();
        MetadataBlockDataStreamInfo mbdsi = null;
        boolean isLastBlock = false;
        while (!isLastBlock) {
            MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(raf);
            if (mbh.getBlockType() == BlockType.STREAMINFO) {
                mbdsi = new MetadataBlockDataStreamInfo(mbh, raf);
                if (!mbdsi.isValid()) {
                    throw new CannotReadException("FLAC StreamInfo not valid");
                }
            } else {
                raf.seek(raf.getFilePointer() + mbh.getDataLength());
            }
            isLastBlock = mbh.isLastBlock();
            mbh = null;
        }
        if (mbdsi == null) {
            throw new CannotReadException("Unable to find Flac StreamInfo");
        }
        GenericAudioHeader info = new GenericAudioHeader();
        info.setLength(mbdsi.getSongLength());
        info.setPreciseLength(mbdsi.getPreciseLength());
        info.setChannelNumber(mbdsi.getChannelNumber());
        info.setSamplingRate(mbdsi.getSamplingRate());
        info.setEncodingType(mbdsi.getEncodingType());
        info.setExtraEncodingInfos("");
        info.setBitrate(computeBitrate(mbdsi.getPreciseLength(), raf.length() - raf.getFilePointer()));
        return info;
    }

    private int computeBitrate(float length, long size) {
        return (int) ((size / KILOBYTES_TO_BYTES_MULTIPLIER) * NO_OF_BITS_IN_BYTE / length);
    }

    /**
     * Count the number of metadatablocks, useful for debugging
     *
     * @param f
     * @return
     * @throws CannotReadException
     * @throws IOException
     */
    public int countMetaBlocks(File f) throws CannotReadException, IOException {
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        FlacStreamReader flacStream = new FlacStreamReader(raf);
        flacStream.findStream();
        boolean isLastBlock = false;
        int count = 0;
        while (!isLastBlock) {
            MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(raf);
            logger.info("Found block:" + mbh.getBlockType());
            raf.seek(raf.getFilePointer() + mbh.getDataLength());
            isLastBlock = mbh.isLastBlock();
            mbh = null;
            count++;
        }
        raf.close();
        return count;
    }
}

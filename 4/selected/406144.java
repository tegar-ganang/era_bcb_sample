package com.hadeslee.audiotag.audio.flac;

import com.hadeslee.audiotag.audio.exceptions.CannotWriteException;
import com.hadeslee.audiotag.audio.flac.metadatablock.MetadataBlock;
import com.hadeslee.audiotag.audio.flac.metadatablock.MetadataBlockData;
import com.hadeslee.audiotag.audio.flac.metadatablock.MetadataBlockDataApplication;
import com.hadeslee.audiotag.audio.flac.metadatablock.MetadataBlockDataCueSheet;
import com.hadeslee.audiotag.audio.flac.metadatablock.MetadataBlockDataPadding;
import com.hadeslee.audiotag.audio.flac.metadatablock.MetadataBlockDataPicture;
import com.hadeslee.audiotag.audio.flac.metadatablock.MetadataBlockDataSeekTable;
import com.hadeslee.audiotag.audio.flac.metadatablock.MetadataBlockDataStreamInfo;
import com.hadeslee.audiotag.audio.flac.metadatablock.MetadataBlockHeader;
import com.hadeslee.audiotag.tag.Tag;
import com.hadeslee.audiotag.tag.flac.FlacTag;
import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Write Flac Tag
 */
public class FlacTagWriter {

    public static Logger logger = Logger.getLogger("com.hadeslee.jaudiotagger.audio.flac");

    private List<MetadataBlock> metadataBlockPadding = new ArrayList<MetadataBlock>(1);

    private List<MetadataBlock> metadataBlockApplication = new ArrayList<MetadataBlock>(1);

    private List<MetadataBlock> metadataBlockSeekTable = new ArrayList<MetadataBlock>(1);

    private List<MetadataBlock> metadataBlockCueSheet = new ArrayList<MetadataBlock>(1);

    private FlacTagCreator tc = new FlacTagCreator();

    private FlacTagReader reader = new FlacTagReader();

    /**
     * Delete Tag from file
     *
     * @param raf
     * @param tempRaf
     * @throws IOException
     * @throws CannotWriteException
     */
    public void delete(RandomAccessFile raf, RandomAccessFile tempRaf) throws IOException, CannotWriteException {
        FlacTag emptyTag = new FlacTag(null, new ArrayList<MetadataBlockDataPicture>());
        raf.seek(0);
        tempRaf.seek(0);
        write(emptyTag, raf, tempRaf);
    }

    /**
     * Write tag to file
     *
     * @param tag
     * @param raf
     * @param rafTemp
     * @throws CannotWriteException
     * @throws IOException
     */
    public void write(Tag tag, RandomAccessFile raf, RandomAccessFile rafTemp) throws CannotWriteException, IOException {
        logger.info("Writing tag");
        metadataBlockPadding.clear();
        metadataBlockApplication.clear();
        metadataBlockSeekTable.clear();
        metadataBlockCueSheet.clear();
        byte[] b = new byte[FlacStream.FLAC_STREAM_IDENTIFIER_LENGTH];
        raf.readFully(b);
        String flac = new String(b);
        if (!flac.equals(FlacStream.FLAC_STREAM_IDENTIFIER)) {
            throw new CannotWriteException("This is not a FLAC file");
        }
        boolean isLastBlock = false;
        while (!isLastBlock) {
            MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(raf);
            switch(mbh.getBlockType()) {
                case VORBIS_COMMENT:
                case PADDING:
                case PICTURE:
                    {
                        raf.seek(raf.getFilePointer() + mbh.getDataLength());
                        MetadataBlockData mbd = new MetadataBlockDataPadding(mbh.getDataLength());
                        metadataBlockPadding.add(new MetadataBlock(mbh, mbd));
                        break;
                    }
                case APPLICATION:
                    {
                        MetadataBlockData mbd = new MetadataBlockDataApplication(mbh, raf);
                        metadataBlockApplication.add(new MetadataBlock(mbh, mbd));
                        break;
                    }
                case SEEKTABLE:
                    {
                        MetadataBlockData mbd = new MetadataBlockDataSeekTable(mbh, raf);
                        metadataBlockSeekTable.add(new MetadataBlock(mbh, mbd));
                        break;
                    }
                case CUESHEET:
                    {
                        MetadataBlockData mbd = new MetadataBlockDataCueSheet(mbh, raf);
                        metadataBlockCueSheet.add(new MetadataBlock(mbh, mbd));
                        break;
                    }
                default:
                    {
                        raf.seek(raf.getFilePointer() + mbh.getDataLength());
                        break;
                    }
            }
            isLastBlock = mbh.isLastBlock();
        }
        int availableRoom = computeAvailableRoom();
        int newTagSize = tc.convert(tag).limit();
        int neededRoom = newTagSize + computeNeededRoom();
        raf.seek(0);
        logger.info("Writing tag available bytes:" + availableRoom + ":needed bytes:" + neededRoom);
        if ((availableRoom == neededRoom) || (availableRoom > neededRoom + MetadataBlockHeader.HEADER_LENGTH)) {
            raf.seek(FlacStream.FLAC_STREAM_IDENTIFIER_LENGTH + MetadataBlockHeader.HEADER_LENGTH + MetadataBlockDataStreamInfo.STREAM_INFO_DATA_LENGTH);
            for (int i = 0; i < metadataBlockApplication.size(); i++) {
                raf.write(metadataBlockApplication.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                raf.write(metadataBlockApplication.get(i).getData().getBytes());
            }
            for (int i = 0; i < metadataBlockSeekTable.size(); i++) {
                raf.write(metadataBlockSeekTable.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                raf.write(metadataBlockSeekTable.get(i).getData().getBytes());
            }
            for (int i = 0; i < metadataBlockCueSheet.size(); i++) {
                raf.write(metadataBlockCueSheet.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                raf.write(metadataBlockCueSheet.get(i).getData().getBytes());
            }
            raf.getChannel().write(tc.convert(tag, availableRoom - neededRoom));
        } else {
            FileChannel fc = raf.getChannel();
            b = new byte[FlacStream.FLAC_STREAM_IDENTIFIER_LENGTH + MetadataBlockHeader.HEADER_LENGTH + MetadataBlockDataStreamInfo.STREAM_INFO_DATA_LENGTH];
            raf.readFully(b);
            raf.seek(availableRoom + FlacStream.FLAC_STREAM_IDENTIFIER_LENGTH + MetadataBlockHeader.HEADER_LENGTH + MetadataBlockDataStreamInfo.STREAM_INFO_DATA_LENGTH);
            FileChannel tempFC = rafTemp.getChannel();
            rafTemp.write(b);
            for (int i = 0; i < metadataBlockApplication.size(); i++) {
                rafTemp.write(metadataBlockApplication.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                rafTemp.write(metadataBlockApplication.get(i).getData().getBytes());
            }
            for (int i = 0; i < metadataBlockSeekTable.size(); i++) {
                rafTemp.write(metadataBlockSeekTable.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                rafTemp.write(metadataBlockSeekTable.get(i).getData().getBytes());
            }
            for (int i = 0; i < metadataBlockCueSheet.size(); i++) {
                rafTemp.write(metadataBlockCueSheet.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                rafTemp.write(metadataBlockCueSheet.get(i).getData().getBytes());
            }
            rafTemp.write(tc.convert(tag, FlacTagCreator.DEFAULT_PADDING).array());
            tempFC.transferFrom(fc, tempFC.position(), fc.size());
        }
    }

    /**
     *
     * @return space currently availble for writing all Flac metadatablocks exceprt for StreamInfo which is fixed size
     */
    private int computeAvailableRoom() {
        int length = 0;
        for (int i = 0; i < metadataBlockApplication.size(); i++) {
            length += metadataBlockApplication.get(i).getLength();
        }
        for (int i = 0; i < metadataBlockSeekTable.size(); i++) {
            length += metadataBlockSeekTable.get(i).getLength();
        }
        for (int i = 0; i < metadataBlockCueSheet.size(); i++) {
            length += metadataBlockCueSheet.get(i).getLength();
        }
        for (int i = 0; i < metadataBlockPadding.size(); i++) {
            length += metadataBlockPadding.get(i).getLength();
        }
        return length;
    }

    /**
     *
     * @return space required to write the metadata blocks that are part of Flac but are not part of tagdata
     * in the normal sense.
     */
    private int computeNeededRoom() {
        int length = 0;
        for (int i = 0; i < metadataBlockApplication.size(); i++) {
            length += metadataBlockApplication.get(i).getLength();
        }
        for (int i = 0; i < metadataBlockSeekTable.size(); i++) {
            length += metadataBlockSeekTable.get(i).getLength();
        }
        for (int i = 0; i < metadataBlockCueSheet.size(); i++) {
            length += metadataBlockCueSheet.get(i).getLength();
        }
        return length;
    }
}

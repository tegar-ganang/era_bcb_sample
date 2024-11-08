package org.jaudiotagger.audio.flac;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.flac.metadatablock.*;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.flac.FlacTag;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Write Flac Tag
 */
public class FlacTagWriter {

    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.flac");

    private MetadataBlock streamInfoBlock;

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
        streamInfoBlock = null;
        metadataBlockPadding.clear();
        metadataBlockApplication.clear();
        metadataBlockSeekTable.clear();
        metadataBlockCueSheet.clear();
        FlacStreamReader flacStream = new FlacStreamReader(raf);
        try {
            flacStream.findStream();
        } catch (CannotReadException cre) {
            throw new CannotWriteException(cre.getMessage());
        }
        boolean isLastBlock = false;
        while (!isLastBlock) {
            MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(raf);
            switch(mbh.getBlockType()) {
                case STREAMINFO:
                    {
                        streamInfoBlock = new MetadataBlock(mbh, new MetadataBlockDataStreamInfo(mbh, raf));
                        break;
                    }
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
        raf.seek(flacStream.getStartOfFlacInFile());
        logger.info("Writing tag available bytes:" + availableRoom + ":needed bytes:" + neededRoom);
        if ((availableRoom == neededRoom) || (availableRoom > neededRoom + MetadataBlockHeader.HEADER_LENGTH)) {
            raf.seek(flacStream.getStartOfFlacInFile() + FlacStreamReader.FLAC_STREAM_IDENTIFIER_LENGTH);
            raf.write(streamInfoBlock.getHeader().getBytesWithoutIsLastBlockFlag());
            raf.write(streamInfoBlock.getData().getBytes());
            for (MetadataBlock aMetadataBlockApplication : metadataBlockApplication) {
                raf.write(aMetadataBlockApplication.getHeader().getBytesWithoutIsLastBlockFlag());
                raf.write(aMetadataBlockApplication.getData().getBytes());
            }
            for (MetadataBlock aMetadataBlockSeekTable : metadataBlockSeekTable) {
                raf.write(aMetadataBlockSeekTable.getHeader().getBytesWithoutIsLastBlockFlag());
                raf.write(aMetadataBlockSeekTable.getData().getBytes());
            }
            for (MetadataBlock aMetadataBlockCueSheet : metadataBlockCueSheet) {
                raf.write(aMetadataBlockCueSheet.getHeader().getBytesWithoutIsLastBlockFlag());
                raf.write(aMetadataBlockCueSheet.getData().getBytes());
            }
            raf.getChannel().write(tc.convert(tag, availableRoom - neededRoom));
        } else {
            int dataStartSize = flacStream.getStartOfFlacInFile() + FlacStreamReader.FLAC_STREAM_IDENTIFIER_LENGTH + MetadataBlockHeader.HEADER_LENGTH + MetadataBlockDataStreamInfo.STREAM_INFO_DATA_LENGTH;
            raf.seek(0);
            rafTemp.getChannel().transferFrom(raf.getChannel(), 0, dataStartSize);
            rafTemp.seek(dataStartSize);
            for (MetadataBlock aMetadataBlockApplication : metadataBlockApplication) {
                rafTemp.write(aMetadataBlockApplication.getHeader().getBytesWithoutIsLastBlockFlag());
                rafTemp.write(aMetadataBlockApplication.getData().getBytes());
            }
            for (MetadataBlock aMetadataBlockSeekTable : metadataBlockSeekTable) {
                rafTemp.write(aMetadataBlockSeekTable.getHeader().getBytesWithoutIsLastBlockFlag());
                rafTemp.write(aMetadataBlockSeekTable.getData().getBytes());
            }
            for (MetadataBlock aMetadataBlockCueSheet : metadataBlockCueSheet) {
                rafTemp.write(aMetadataBlockCueSheet.getHeader().getBytesWithoutIsLastBlockFlag());
                rafTemp.write(aMetadataBlockCueSheet.getData().getBytes());
            }
            rafTemp.write(tc.convert(tag, FlacTagCreator.DEFAULT_PADDING).array());
            raf.seek(dataStartSize + availableRoom);
            rafTemp.getChannel().transferFrom(raf.getChannel(), rafTemp.getChannel().position(), raf.getChannel().size());
        }
    }

    /**
     * @return space currently availble for writing all Flac metadatablocks exceprt for StreamInfo which is fixed size
     */
    private int computeAvailableRoom() {
        int length = 0;
        for (MetadataBlock aMetadataBlockApplication : metadataBlockApplication) {
            length += aMetadataBlockApplication.getLength();
        }
        for (MetadataBlock aMetadataBlockSeekTable : metadataBlockSeekTable) {
            length += aMetadataBlockSeekTable.getLength();
        }
        for (MetadataBlock aMetadataBlockCueSheet : metadataBlockCueSheet) {
            length += aMetadataBlockCueSheet.getLength();
        }
        for (MetadataBlock aMetadataBlockPadding : metadataBlockPadding) {
            length += aMetadataBlockPadding.getLength();
        }
        return length;
    }

    /**
     * @return space required to write the metadata blocks that are part of Flac but are not part of tagdata
     *         in the normal sense.
     */
    private int computeNeededRoom() {
        int length = 0;
        for (MetadataBlock aMetadataBlockApplication : metadataBlockApplication) {
            length += aMetadataBlockApplication.getLength();
        }
        for (MetadataBlock aMetadataBlockSeekTable : metadataBlockSeekTable) {
            length += aMetadataBlockSeekTable.getLength();
        }
        for (MetadataBlock aMetadataBlockCueSheet : metadataBlockCueSheet) {
            length += aMetadataBlockCueSheet.getLength();
        }
        return length;
    }
}

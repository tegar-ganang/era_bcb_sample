package org.jaudiotagger.audio.ogg;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.ogg.util.OggCRCFactory;
import org.jaudiotagger.audio.ogg.util.OggPageHeader;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.logging.Logger;

/**
 * Write Vorbis Tag within an ogg
 * <p/>
 * VorbisComment holds the tag information within an ogg file
 */
public class OggVorbisTagWriter {

    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.ogg");

    private OggVorbisCommentTagCreator tc = new OggVorbisCommentTagCreator();

    private OggVorbisTagReader reader = new OggVorbisTagReader();

    public void delete(RandomAccessFile raf, RandomAccessFile tempRaf) throws IOException, CannotReadException, CannotWriteException {
        try {
            reader.read(raf);
        } catch (CannotReadException e) {
            write(VorbisCommentTag.createNewTag(), raf, tempRaf);
            return;
        }
        VorbisCommentTag emptyTag = VorbisCommentTag.createNewTag();
        raf.seek(0);
        write(emptyTag, raf, tempRaf);
    }

    public void write(Tag tag, RandomAccessFile raf, RandomAccessFile rafTemp) throws CannotReadException, CannotWriteException, IOException {
        logger.info("Starting to write file:");
        logger.fine("Read 1st Page:identificationHeader:");
        OggPageHeader pageHeader = OggPageHeader.read(raf);
        raf.seek(0);
        rafTemp.getChannel().transferFrom(raf.getChannel(), 0, pageHeader.getPageLength() + OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageHeader.getSegmentTable().length);
        rafTemp.skipBytes(pageHeader.getPageLength() + OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageHeader.getSegmentTable().length);
        logger.fine("Written identificationHeader:");
        OggPageHeader secondPageHeader = OggPageHeader.read(raf);
        long secondPageHeaderEndPos = raf.getFilePointer();
        logger.fine("Read 2nd Page:comment and setup and possibly audio:Header finishes at file position:" + secondPageHeaderEndPos);
        raf.seek(0);
        OggVorbisTagReader.OggVorbisHeaderSizes vorbisHeaderSizes = reader.readOggVorbisHeaderSizes(raf);
        ByteBuffer newComment = tc.convert(tag);
        int newCommentLength = newComment.capacity();
        int newSecondPageDataLength = vorbisHeaderSizes.getSetupHeaderSize() + newCommentLength + vorbisHeaderSizes.getExtraPacketDataSize();
        logger.fine("Old 2nd Page no of packets: " + secondPageHeader.getPacketList().size());
        logger.fine("Old 2nd Page size: " + secondPageHeader.getPageLength());
        logger.fine("Old last packet incomplete: " + secondPageHeader.isLastPacketIncomplete());
        logger.fine("Setup Header Size: " + vorbisHeaderSizes.getSetupHeaderSize());
        logger.fine("Extra Packets: " + vorbisHeaderSizes.getExtraPacketList().size());
        logger.fine("Extra Packet Data Size: " + vorbisHeaderSizes.getExtraPacketDataSize());
        logger.fine("Old comment: " + vorbisHeaderSizes.getCommentHeaderSize());
        logger.fine("New comment: " + newCommentLength);
        logger.fine("New Page Data Size: " + newSecondPageDataLength);
        if (isCommentAndSetupHeaderFitsOnASinglePage(newCommentLength, vorbisHeaderSizes.getSetupHeaderSize(), vorbisHeaderSizes.getExtraPacketList())) {
            if ((secondPageHeader.getPageLength() < OggPageHeader.MAXIMUM_PAGE_DATA_SIZE) && (((secondPageHeader.getPacketList().size() == 2) && (!secondPageHeader.isLastPacketIncomplete())) || (secondPageHeader.getPacketList().size() > 2))) {
                logger.fine("Header and Setup remain on single page:");
                replaceSecondPageOnly(vorbisHeaderSizes, newCommentLength, newSecondPageDataLength, secondPageHeader, newComment, secondPageHeaderEndPos, raf, rafTemp);
            } else {
                logger.fine("Header and Setup now on single page:");
                replaceSecondPageAndRenumberPageSeqs(vorbisHeaderSizes, newCommentLength, newSecondPageDataLength, secondPageHeader, newComment, raf, rafTemp);
            }
        } else {
            logger.fine("Header and Setup with shift audio:");
            replacePagesAndRenumberPageSeqs(vorbisHeaderSizes, newCommentLength, secondPageHeader, newComment, raf, rafTemp);
        }
    }

    /**
     * Calculate checkSum over the Page
     *
     * @param page
     */
    private void calculateChecksumOverPage(ByteBuffer page) {
        page.putInt(OggPageHeader.FIELD_PAGE_CHECKSUM_POS, 0);
        byte[] crc = OggCRCFactory.computeCRC(page.array());
        for (int i = 0; i < crc.length; i++) {
            page.put(OggPageHeader.FIELD_PAGE_CHECKSUM_POS + i, crc[i]);
        }
        page.rewind();
    }

    /**
     * Create a second Page, and add comment header to it, but page is incomplete may want to add addition header and need to calculate CRC
     *
     * @param vorbisHeaderSizes
     * @param newCommentLength
     * @param newSecondPageLength
     * @param secondPageHeader
     * @param newComment
     * @return
     * @throws IOException
     */
    private ByteBuffer startCreateBasicSecondPage(OggVorbisTagReader.OggVorbisHeaderSizes vorbisHeaderSizes, int newCommentLength, int newSecondPageLength, OggPageHeader secondPageHeader, ByteBuffer newComment) throws IOException {
        logger.fine("WriteOgg Type 1");
        byte[] segmentTable = createSegmentTable(newCommentLength, vorbisHeaderSizes.getSetupHeaderSize(), vorbisHeaderSizes.getExtraPacketList());
        int newSecondPageHeaderLength = OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + segmentTable.length;
        logger.fine("New second page header length:" + newSecondPageHeaderLength);
        logger.fine("No of segments:" + segmentTable.length);
        ByteBuffer secondPageBuffer = ByteBuffer.allocate(newSecondPageLength + newSecondPageHeaderLength);
        secondPageBuffer.order(ByteOrder.LITTLE_ENDIAN);
        secondPageBuffer.put(secondPageHeader.getRawHeaderData(), 0, OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1);
        secondPageBuffer.put((byte) segmentTable.length);
        for (byte aSegmentTable : segmentTable) {
            secondPageBuffer.put(aSegmentTable);
        }
        secondPageBuffer.put(newComment);
        return secondPageBuffer;
    }

    /**
     * Usually can use this method, previously comment and setup header all fit on page 2
     * and they still do, so just replace this page. And copy further pages as is.
     *
     * @param vorbisHeaderSizes
     * @param newCommentLength
     * @param newSecondPageLength
     * @param secondPageHeader
     * @param newComment
     * @param secondPageHeaderEndPos
     * @param raf
     * @param rafTemp
     * @throws IOException
     */
    private void replaceSecondPageOnly(OggVorbisTagReader.OggVorbisHeaderSizes vorbisHeaderSizes, int newCommentLength, int newSecondPageLength, OggPageHeader secondPageHeader, ByteBuffer newComment, long secondPageHeaderEndPos, RandomAccessFile raf, RandomAccessFile rafTemp) throws IOException {
        logger.fine("WriteOgg Type 1");
        ByteBuffer secondPageBuffer = startCreateBasicSecondPage(vorbisHeaderSizes, newCommentLength, newSecondPageLength, secondPageHeader, newComment);
        raf.seek(secondPageHeaderEndPos);
        raf.skipBytes(vorbisHeaderSizes.getCommentHeaderSize());
        raf.getChannel().read(secondPageBuffer);
        calculateChecksumOverPage(secondPageBuffer);
        rafTemp.getChannel().write(secondPageBuffer);
        rafTemp.getChannel().transferFrom(raf.getChannel(), rafTemp.getFilePointer(), raf.length() - raf.getFilePointer());
    }

    /**
     * Previously comment and/or setup header was on a number of pages now can just replace this page fitting all
     * on 2nd page, and renumber subsequent sequence pages
     *
     * @param originalHeaderSizes
     * @param newCommentLength
     * @param newSecondPageLength
     * @param secondPageHeader
     * @param newComment
     * @param raf
     * @param rafTemp
     * @throws IOException
     * @throws org.jaudiotagger.audio.exceptions.CannotReadException
     * @throws org.jaudiotagger.audio.exceptions.CannotWriteException
     */
    private void replaceSecondPageAndRenumberPageSeqs(OggVorbisTagReader.OggVorbisHeaderSizes originalHeaderSizes, int newCommentLength, int newSecondPageLength, OggPageHeader secondPageHeader, ByteBuffer newComment, RandomAccessFile raf, RandomAccessFile rafTemp) throws IOException, CannotReadException, CannotWriteException {
        logger.fine("WriteOgg Type 2");
        ByteBuffer secondPageBuffer = startCreateBasicSecondPage(originalHeaderSizes, newCommentLength, newSecondPageLength, secondPageHeader, newComment);
        int pageSequence = secondPageHeader.getPageSequence();
        byte[] setupHeaderData = reader.convertToVorbisSetupHeaderPacketAndAdditionalPackets(originalHeaderSizes.getSetupHeaderStartPosition(), raf);
        logger.finest(setupHeaderData.length + ":" + secondPageBuffer.position() + ":" + secondPageBuffer.capacity());
        secondPageBuffer.put(setupHeaderData);
        calculateChecksumOverPage(secondPageBuffer);
        rafTemp.getChannel().write(secondPageBuffer);
        writeRemainingPages(pageSequence, raf, rafTemp);
    }

    /**
     * CommentHeader extends over multiple pages OR Comment Header doesnt but it's got larger causing some extra
     * packets to be shifted onto another page.
     *
     * @param originalHeaderSizes
     * @param newCommentLength
     * @param secondPageHeader
     * @param newComment
     * @param raf
     * @param rafTemp
     * @throws IOException
     * @throws CannotReadException
     * @throws CannotWriteException
     */
    private void replacePagesAndRenumberPageSeqs(OggVorbisTagReader.OggVorbisHeaderSizes originalHeaderSizes, int newCommentLength, OggPageHeader secondPageHeader, ByteBuffer newComment, RandomAccessFile raf, RandomAccessFile rafTemp) throws IOException, CannotReadException, CannotWriteException {
        int pageSequence = secondPageHeader.getPageSequence();
        int noOfCompletePagesNeededForComment = newCommentLength / OggPageHeader.MAXIMUM_PAGE_DATA_SIZE;
        logger.info("Comment requires:" + noOfCompletePagesNeededForComment + " complete pages");
        int newCommentOffset = 0;
        if (noOfCompletePagesNeededForComment > 0) {
            for (int i = 0; i < noOfCompletePagesNeededForComment; i++) {
                byte[] segmentTable = this.createSegments(OggPageHeader.MAXIMUM_PAGE_DATA_SIZE, false);
                int pageHeaderLength = OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + segmentTable.length;
                ByteBuffer pageBuffer = ByteBuffer.allocate(pageHeaderLength + OggPageHeader.MAXIMUM_PAGE_DATA_SIZE);
                pageBuffer.order(ByteOrder.LITTLE_ENDIAN);
                pageBuffer.put(secondPageHeader.getRawHeaderData(), 0, OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1);
                pageBuffer.put((byte) segmentTable.length);
                for (byte aSegmentTable : segmentTable) {
                    pageBuffer.put(aSegmentTable);
                }
                ByteBuffer nextPartOfComment = newComment.slice();
                nextPartOfComment.limit(OggPageHeader.MAXIMUM_PAGE_DATA_SIZE);
                pageBuffer.put(nextPartOfComment);
                pageBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, pageSequence);
                pageSequence++;
                if (i != 0) {
                    pageBuffer.put(OggPageHeader.FIELD_HEADER_TYPE_FLAG_POS, OggPageHeader.HeaderTypeFlag.CONTINUED_PACKET.getFileValue());
                }
                calculateChecksumOverPage(pageBuffer);
                rafTemp.getChannel().write(pageBuffer);
                newCommentOffset += OggPageHeader.MAXIMUM_PAGE_DATA_SIZE;
                newComment.position(newCommentOffset);
            }
        }
        int lastPageCommentPacketSize = newCommentLength % OggPageHeader.MAXIMUM_PAGE_DATA_SIZE;
        logger.fine("Last comment packet size:" + lastPageCommentPacketSize);
        if (!isCommentAndSetupHeaderFitsOnASinglePage(lastPageCommentPacketSize, originalHeaderSizes.getSetupHeaderSize(), originalHeaderSizes.getExtraPacketList())) {
            logger.fine("WriteOgg Type 3");
            {
                byte[] segmentTable = createSegments(lastPageCommentPacketSize, true);
                int pageHeaderLength = OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + segmentTable.length;
                ByteBuffer pageBuffer = ByteBuffer.allocate(lastPageCommentPacketSize + pageHeaderLength);
                pageBuffer.order(ByteOrder.LITTLE_ENDIAN);
                pageBuffer.put(secondPageHeader.getRawHeaderData(), 0, OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1);
                pageBuffer.put((byte) segmentTable.length);
                for (byte aSegmentTable : segmentTable) {
                    pageBuffer.put(aSegmentTable);
                }
                newComment.position(newCommentOffset);
                pageBuffer.put(newComment.slice());
                pageBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, pageSequence);
                if (noOfCompletePagesNeededForComment > 0) {
                    pageBuffer.put(OggPageHeader.FIELD_HEADER_TYPE_FLAG_POS, OggPageHeader.HeaderTypeFlag.CONTINUED_PACKET.getFileValue());
                }
                logger.fine("Writing Last Comment Page " + pageSequence + " to file");
                pageSequence++;
                calculateChecksumOverPage(pageBuffer);
                rafTemp.getChannel().write(pageBuffer);
            }
            {
                byte[] segmentTable = this.createSegmentTable(originalHeaderSizes.getSetupHeaderSize(), originalHeaderSizes.getExtraPacketList());
                int pageHeaderLength = OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + segmentTable.length;
                byte[] setupHeaderData = reader.convertToVorbisSetupHeaderPacketAndAdditionalPackets(originalHeaderSizes.getSetupHeaderStartPosition(), raf);
                ByteBuffer pageBuffer = ByteBuffer.allocate(setupHeaderData.length + pageHeaderLength);
                pageBuffer.order(ByteOrder.LITTLE_ENDIAN);
                pageBuffer.put(secondPageHeader.getRawHeaderData(), 0, OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1);
                pageBuffer.put((byte) segmentTable.length);
                for (byte aSegmentTable : segmentTable) {
                    pageBuffer.put(aSegmentTable);
                }
                pageBuffer.put(setupHeaderData);
                pageBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, pageSequence);
                logger.fine("Writing Setup Header and packets Page " + pageSequence + " to file");
                calculateChecksumOverPage(pageBuffer);
                rafTemp.getChannel().write(pageBuffer);
            }
        } else {
            logger.fine("WriteOgg Type 4");
            int newSecondPageDataLength = originalHeaderSizes.getSetupHeaderSize() + lastPageCommentPacketSize + originalHeaderSizes.getExtraPacketDataSize();
            newComment.position(newCommentOffset);
            ByteBuffer lastComment = newComment.slice();
            ByteBuffer lastHeaderBuffer = startCreateBasicSecondPage(originalHeaderSizes, lastPageCommentPacketSize, newSecondPageDataLength, secondPageHeader, lastComment);
            raf.seek(originalHeaderSizes.getSetupHeaderStartPosition());
            byte[] setupHeaderData = reader.convertToVorbisSetupHeaderPacketAndAdditionalPackets(originalHeaderSizes.getSetupHeaderStartPosition(), raf);
            lastHeaderBuffer.put(setupHeaderData);
            lastHeaderBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, pageSequence);
            lastHeaderBuffer.put(OggPageHeader.FIELD_HEADER_TYPE_FLAG_POS, OggPageHeader.HeaderTypeFlag.CONTINUED_PACKET.getFileValue());
            calculateChecksumOverPage(lastHeaderBuffer);
            rafTemp.getChannel().write(lastHeaderBuffer);
        }
        writeRemainingPages(pageSequence, raf, rafTemp);
    }

    /**
     * Write all the remaining pages as they are except that the page sequence needs to be modified.
     *
     * @param pageSequence
     * @param raf
     * @param rafTemp
     * @throws IOException
     * @throws CannotReadException
     * @throws CannotWriteException
     */
    public void writeRemainingPages(int pageSequence, RandomAccessFile raf, RandomAccessFile rafTemp) throws IOException, CannotReadException, CannotWriteException {
        long startAudio = raf.getFilePointer();
        long startAudioWritten = rafTemp.getFilePointer();
        ByteBuffer bb = ByteBuffer.allocate((int) (raf.length() - raf.getFilePointer()));
        ByteBuffer bbTemp = ByteBuffer.allocate((int) (raf.length() - raf.getFilePointer()));
        raf.getChannel().read(bb);
        bb.rewind();
        while (bb.hasRemaining()) {
            OggPageHeader nextPage = OggPageHeader.read(bb);
            ByteBuffer nextPageHeaderBuffer = ByteBuffer.allocate(nextPage.getRawHeaderData().length + nextPage.getPageLength());
            nextPageHeaderBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nextPageHeaderBuffer.put(nextPage.getRawHeaderData());
            ByteBuffer data = bb.slice();
            data.limit(nextPage.getPageLength());
            nextPageHeaderBuffer.put(data);
            nextPageHeaderBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, ++pageSequence);
            calculateChecksumOverPage(nextPageHeaderBuffer);
            bb.position(bb.position() + nextPage.getPageLength());
            nextPageHeaderBuffer.rewind();
            bbTemp.put(nextPageHeaderBuffer);
        }
        bbTemp.rewind();
        rafTemp.getChannel().write(bbTemp);
        if ((raf.length() - startAudio) != (rafTemp.length() - startAudioWritten)) {
            throw new CannotWriteException("File written counts don't match, file not written");
        }
    }

    public void writeRemainingPagesOld(int pageSequence, RandomAccessFile raf, RandomAccessFile rafTemp) throws IOException, CannotReadException, CannotWriteException {
        long startAudio = raf.getFilePointer();
        long startAudioWritten = rafTemp.getFilePointer();
        logger.fine("Writing audio, audio starts in original file at :" + startAudio + ":Written to:" + startAudioWritten);
        while (raf.getFilePointer() < raf.length()) {
            logger.fine("Reading Ogg Page");
            OggPageHeader nextPage = OggPageHeader.read(raf);
            ByteBuffer nextPageHeaderBuffer = ByteBuffer.allocate(nextPage.getRawHeaderData().length + nextPage.getPageLength());
            nextPageHeaderBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nextPageHeaderBuffer.put(nextPage.getRawHeaderData());
            raf.getChannel().read(nextPageHeaderBuffer);
            nextPageHeaderBuffer.putInt(OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, ++pageSequence);
            calculateChecksumOverPage(nextPageHeaderBuffer);
            rafTemp.getChannel().write(nextPageHeaderBuffer);
        }
        if ((raf.length() - startAudio) != (rafTemp.length() - startAudioWritten)) {
            throw new CannotWriteException("File written counts don't match, file not written");
        }
    }

    /**
     * This method creates a new segment table for the second page (header).
     *
     * @param newCommentLength  The length of the Vorbis Comment
     * @param setupHeaderLength The length of Setup Header, zero if comment String extends
     *                          over multiple pages and this is not the last page.
     * @param extraPackets      If there are packets immediately after setup header in same page, they
     *                          need including in the segment table
     * @return new segment table.
     */
    private byte[] createSegmentTable(int newCommentLength, int setupHeaderLength, List<OggPageHeader.PacketStartAndLength> extraPackets) {
        logger.finest("Create SegmentTable CommentLength:" + newCommentLength + ":SetupHeaderLength:" + setupHeaderLength);
        ByteArrayOutputStream resultBaos = new ByteArrayOutputStream();
        byte[] newStart;
        byte[] restShouldBe;
        byte[] nextPacket;
        if (setupHeaderLength == 0) {
            newStart = createSegments(newCommentLength, false);
            return newStart;
        } else {
            newStart = createSegments(newCommentLength, true);
        }
        if (extraPackets.size() > 0) {
            restShouldBe = createSegments(setupHeaderLength, true);
        } else {
            restShouldBe = createSegments(setupHeaderLength, false);
        }
        logger.finest("Created " + newStart.length + " segments for header");
        logger.finest("Created " + restShouldBe.length + " segments for setup");
        try {
            resultBaos.write(newStart);
            resultBaos.write(restShouldBe);
            if (extraPackets.size() > 0) {
                logger.finer("Creating segments for " + extraPackets.size() + " packets");
                for (OggPageHeader.PacketStartAndLength packet : extraPackets) {
                    nextPacket = createSegments(packet.getLength(), false);
                    resultBaos.write(nextPacket);
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create segment table:" + ioe.getMessage());
        }
        return resultBaos.toByteArray();
    }

    /**
     * This method creates a new segment table for the second half of setup header
     *
     * @param setupHeaderLength The length of Setup Header, zero if comment String extends
     *                          over multiple pages and this is not the last page.
     * @param extraPackets      If there are packets immediately after setup header in same page, they
     *                          need including in the segment table
     * @return new segment table.
     */
    private byte[] createSegmentTable(int setupHeaderLength, List<OggPageHeader.PacketStartAndLength> extraPackets) {
        ByteArrayOutputStream resultBaos = new ByteArrayOutputStream();
        byte[] restShouldBe;
        byte[] nextPacket;
        restShouldBe = createSegments(setupHeaderLength, true);
        try {
            resultBaos.write(restShouldBe);
            if (extraPackets.size() > 0) {
                for (OggPageHeader.PacketStartAndLength packet : extraPackets) {
                    nextPacket = createSegments(packet.getLength(), false);
                    resultBaos.write(nextPacket);
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create segment table:" + ioe.getMessage());
        }
        return resultBaos.toByteArray();
    }

    private byte[] createSegments(int length, boolean quitStream) {
        logger.finest("Create Segments for length:" + length + ":QuitStream:" + quitStream);
        if (length == 0) {
            byte[] result = new byte[1];
            result[0] = (byte) 0x00;
            return result;
        }
        byte[] result = new byte[length / OggPageHeader.MAXIMUM_SEGMENT_SIZE + ((length % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0 && !quitStream) ? 0 : 1)];
        int i = 0;
        for (; i < result.length - 1; i++) {
            result[i] = (byte) 0xFF;
        }
        result[result.length - 1] = (byte) (length - (i * OggPageHeader.MAXIMUM_SEGMENT_SIZE));
        return result;
    }

    /**
     * @param commentLength
     * @param setupHeaderLength
     * @param extraPacketList
     * @return true if there is enough room to fit the comment and the setup headers on one page taking into
     *         account the maximum no of segments allowed per page and zero lacing values.
     */
    private boolean isCommentAndSetupHeaderFitsOnASinglePage(int commentLength, int setupHeaderLength, List<OggPageHeader.PacketStartAndLength> extraPacketList) {
        int totalDataSize = 0;
        if (commentLength == 0) {
            totalDataSize++;
        } else {
            totalDataSize = (commentLength / OggPageHeader.MAXIMUM_SEGMENT_SIZE) + 1;
            if (commentLength % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0) {
                totalDataSize++;
            }
        }
        logger.finest("Require:" + totalDataSize + " segments for comment");
        if (setupHeaderLength == 0) {
            totalDataSize++;
        } else {
            totalDataSize += (setupHeaderLength / OggPageHeader.MAXIMUM_SEGMENT_SIZE) + 1;
            if (setupHeaderLength % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0) {
                totalDataSize++;
            }
        }
        logger.finest("Require:" + totalDataSize + " segments for comment plus setup");
        for (OggPageHeader.PacketStartAndLength extraPacket : extraPacketList) {
            if (extraPacket.getLength() == 0) {
                totalDataSize++;
            } else {
                totalDataSize += (extraPacket.getLength() / OggPageHeader.MAXIMUM_SEGMENT_SIZE) + 1;
                if (extraPacket.getLength() % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0) {
                    totalDataSize++;
                }
            }
        }
        logger.finest("Total No Of Segment If New Comment And Header Put On One Page:" + totalDataSize);
        return totalDataSize <= OggPageHeader.MAXIMUM_NO_OF_SEGMENT_SIZE;
    }
}

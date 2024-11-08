package org.jaudiotagger.audio.mp4;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.audio.mp4.atom.Mp4BoxHeader;
import org.jaudiotagger.audio.mp4.atom.Mp4MetaBox;
import org.jaudiotagger.logging.ErrorMessage;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4NonStandardFieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import org.jaudiotagger.tag.mp4.atom.Mp4DataBox;
import org.jaudiotagger.tag.mp4.field.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * Reads metadata from mp4,
 * <p/>
 * <p>The metadata tags are usually held under the ilst atom as shown below<p/>
 * <p>Valid Exceptions to the rule:</p>
 * <p>Can be no udta atom with meta rooted immediately under moov instead<p/>
 * <p>Can be no udta/meta atom at all<p/>
 *
 * <pre>
 * |--- ftyp
 * |--- moov
 * |......|
 * |......|----- mvdh
 * |......|----- trak
 * |......|----- udta
 * |..............|
 * |..............|-- meta
 * |....................|
 * |....................|-- hdlr
 * |....................|-- ilst
 * |.........................|
 * |.........................|---- @nam (Optional for each metadatafield)
 * |.........................|.......|-- data
 * |.........................|....... ecetera
 * |.........................|---- ---- (Optional for reverse dns field)
 * |.................................|-- mean
 * |.................................|-- name
 * |.................................|-- data
 * |.................................... ecetere
 * |
 * |--- mdat
 * </pre
 */
public class Mp4TagReader {

    public static Logger logger = Logger.getLogger("org.jaudiotagger.tag.mp4");

    public Mp4Tag read(RandomAccessFile raf) throws CannotReadException, IOException {
        Mp4Tag tag = new Mp4Tag();
        Mp4BoxHeader moovHeader = Mp4BoxHeader.seekWithinLevel(raf, Mp4NotMetaFieldKey.MOOV.getFieldName());
        if (moovHeader == null) {
            throw new CannotReadException(ErrorMessage.MP4_FILE_NOT_CONTAINER.getMsg());
        }
        ByteBuffer moovBuffer = ByteBuffer.allocate(moovHeader.getLength() - Mp4BoxHeader.HEADER_LENGTH);
        raf.getChannel().read(moovBuffer);
        moovBuffer.rewind();
        Mp4BoxHeader boxHeader = Mp4BoxHeader.seekWithinLevel(moovBuffer, Mp4NotMetaFieldKey.UDTA.getFieldName());
        if (boxHeader != null) {
            boxHeader = Mp4BoxHeader.seekWithinLevel(moovBuffer, Mp4NotMetaFieldKey.META.getFieldName());
            if (boxHeader == null) {
                logger.warning(ErrorMessage.MP4_FILE_HAS_NO_METADATA.getMsg());
                return tag;
            }
            Mp4MetaBox meta = new Mp4MetaBox(boxHeader, moovBuffer);
            meta.processData();
            boxHeader = Mp4BoxHeader.seekWithinLevel(moovBuffer, Mp4NotMetaFieldKey.ILST.getFieldName());
            if (boxHeader == null) {
                logger.warning(ErrorMessage.MP4_FILE_HAS_NO_METADATA.getMsg());
                return tag;
            }
        } else {
            boxHeader = Mp4BoxHeader.seekWithinLevel(moovBuffer, Mp4NotMetaFieldKey.META.getFieldName());
            if (boxHeader == null) {
                logger.warning(ErrorMessage.MP4_FILE_HAS_NO_METADATA.getMsg());
                return tag;
            }
            Mp4MetaBox meta = new Mp4MetaBox(boxHeader, moovBuffer);
            meta.processData();
            boxHeader = Mp4BoxHeader.seekWithinLevel(moovBuffer, Mp4NotMetaFieldKey.ILST.getFieldName());
            if (boxHeader == null) {
                logger.warning(ErrorMessage.MP4_FILE_HAS_NO_METADATA.getMsg());
                return tag;
            }
        }
        int length = boxHeader.getLength() - Mp4BoxHeader.HEADER_LENGTH;
        ByteBuffer metadataBuffer = moovBuffer.slice();
        logger.info("headerlengthsays:" + length + "datalength:" + metadataBuffer.limit());
        int read = 0;
        logger.info("Started to read metadata fields at position is in metadata buffer:" + metadataBuffer.position());
        while (read < length) {
            boxHeader.update(metadataBuffer);
            logger.info("Next position is at:" + metadataBuffer.position());
            createMp4Field(tag, boxHeader, metadataBuffer.slice());
            metadataBuffer.position(metadataBuffer.position() + boxHeader.getDataLength());
            read += boxHeader.getLength();
        }
        return tag;
    }

    /**
     * Process the field and add to the tag
     * <p/>
     * Note:In the case of coverart MP4 holds all the coverart within individual dataitems all within
     * a single covr atom, we will add separate mp4field for each image.
     *
     * @param tag
     * @param header
     * @param raw
     * @return
     * @throws UnsupportedEncodingException
     */
    private void createMp4Field(Mp4Tag tag, Mp4BoxHeader header, ByteBuffer raw) throws UnsupportedEncodingException {
        if (header.getId().equals(Mp4TagReverseDnsField.IDENTIFIER)) {
            try {
                TagField field = new Mp4TagReverseDnsField(header, raw);
                tag.addField(field);
            } catch (Exception e) {
                logger.warning(ErrorMessage.MP4_UNABLE_READ_REVERSE_DNS_FIELD.getMsg(e.getMessage()));
                TagField field = new Mp4TagRawBinaryField(header, raw);
                tag.addField(field);
            }
        } else {
            int currentPos = raw.position();
            boolean isDataIdentifier = Utils.getString(raw, Mp4BoxHeader.IDENTIFIER_POS, Mp4BoxHeader.IDENTIFIER_LENGTH, "ISO-8859-1").equals(Mp4DataBox.IDENTIFIER);
            raw.position(currentPos);
            if (isDataIdentifier) {
                int type = Utils.getIntBE(raw, Mp4DataBox.TYPE_POS_INCLUDING_HEADER, Mp4DataBox.TYPE_POS_INCLUDING_HEADER + Mp4DataBox.TYPE_LENGTH - 1);
                Mp4FieldType fieldType = Mp4FieldType.getFieldType(type);
                logger.info("Box Type id:" + header.getId() + ":type:" + fieldType);
                if (header.getId().equals(Mp4FieldKey.TRACK.getFieldName())) {
                    TagField field = new Mp4TrackField(header.getId(), raw);
                    tag.addField(field);
                } else if (header.getId().equals(Mp4FieldKey.DISCNUMBER.getFieldName())) {
                    TagField field = new Mp4DiscNoField(header.getId(), raw);
                    tag.addField(field);
                } else if (header.getId().equals(Mp4FieldKey.GENRE.getFieldName())) {
                    TagField field = new Mp4GenreField(header.getId(), raw);
                    tag.addField(field);
                } else if (header.getId().equals(Mp4FieldKey.ARTWORK.getFieldName()) || Mp4FieldType.isCoverArtType(fieldType)) {
                    int processedDataSize = 0;
                    int imageCount = 0;
                    while (processedDataSize < header.getDataLength()) {
                        if (imageCount > 0) {
                            type = Utils.getIntBE(raw, processedDataSize + Mp4DataBox.TYPE_POS_INCLUDING_HEADER, processedDataSize + Mp4DataBox.TYPE_POS_INCLUDING_HEADER + Mp4DataBox.TYPE_LENGTH - 1);
                            fieldType = Mp4FieldType.getFieldType(type);
                        }
                        Mp4TagCoverField field = new Mp4TagCoverField(raw, fieldType);
                        tag.addField(field);
                        processedDataSize += field.getDataAndHeaderSize();
                        imageCount++;
                    }
                } else if (fieldType == Mp4FieldType.TEXT) {
                    TagField field = new Mp4TagTextField(header.getId(), raw);
                    tag.addField(field);
                } else if (fieldType == Mp4FieldType.IMPLICIT) {
                    TagField field = new Mp4TagTextNumberField(header.getId(), raw);
                    tag.addField(field);
                } else if (fieldType == Mp4FieldType.INTEGER) {
                    TagField field = new Mp4TagByteField(header.getId(), raw);
                    tag.addField(field);
                } else {
                    boolean existingId = false;
                    for (Mp4FieldKey key : Mp4FieldKey.values()) {
                        if (key.getFieldName().equals(header.getId())) {
                            existingId = true;
                            logger.warning("Known Field:" + header.getId() + " with invalid field type of:" + type + " is ignored");
                            break;
                        }
                    }
                    if (!existingId) {
                        logger.warning("UnKnown Field:" + header.getId() + " with invalid field type of:" + type + " created as binary");
                        TagField field = new Mp4TagBinaryField(header.getId(), raw);
                        tag.addField(field);
                    }
                }
            } else {
                if (header.getId().equals(Mp4NonStandardFieldKey.AAPR.getFieldName())) {
                    TagField field = new Mp4TagRawBinaryField(header, raw);
                    tag.addField(field);
                } else {
                    TagField field = new Mp4TagRawBinaryField(header, raw);
                    tag.addField(field);
                }
            }
        }
    }
}

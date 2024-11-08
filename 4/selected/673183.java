package entagged.audioformats.asf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import entagged.audioformats.Tag;
import entagged.audioformats.asf.data.AsfHeader;
import entagged.audioformats.asf.data.Chunk;
import entagged.audioformats.asf.data.ContentDescription;
import entagged.audioformats.asf.data.ContentDescriptor;
import entagged.audioformats.asf.data.ExtendedContentDescription;
import entagged.audioformats.asf.data.GUID;
import entagged.audioformats.asf.io.AsfHeaderReader;
import entagged.audioformats.asf.util.ChunkPositionComparator;
import entagged.audioformats.asf.util.TagConverter;
import entagged.audioformats.exceptions.CannotWriteException;
import entagged.audioformats.generic.AudioFileWriter;
import entagged.audioformats.generic.GenericTag;

/**
 * This class writes given tags to asf files containing wma content. <br>
 * <br>
 * 
 * @author Christian Laireiter
 */
public class AsfFileWriter extends AudioFileWriter {

    /**
     * This method copies <code>number</code> of bytes from
     * <code>source</code> to <code>destination</code>.<br>
     * 
     * @param source
     *                   Source for copy
     * @param destination
     *                   Destination for copy
     * @param number
     *                   number of bytes to copy.
     * @throws IOException
     *                    read or write errors.
     */
    private void copy(RandomAccessFile source, RandomAccessFile destination, long number) throws IOException {
        byte[] buffer = new byte[8192];
        long bytesCopied = 0;
        int read = -1;
        while ((read = source.read(buffer, 0, ((bytesCopied + 8192 > number) ? (int) (number - bytesCopied) : 8192))) > 0) {
            bytesCopied += read;
            destination.write(buffer, 0, read);
        }
    }

    /**
     * This method creates a modfied copy of the input <code>raf</code> and
     * writes it to <code>rafTemp</code>.<br>
     * If the source contains no extended content description but one is needed (
     * {@link #isExtendedContentDescriptionMandatory(Tag)}) it will be created.
     * If the new file doesn't require such a chunk and the source didn't
     * contain optional values in it, it will be deleted.
     * 
     * @param tag
     *                   Tag containing the new values.
     * @param header
     *                   Read header of <code>raf</code>.
     * @param raf
     *                   source
     * @param rafTemp
     *                   destination.
     * @param ignoreDescriptions
     *                   if <code>true</code> the content description and the
     *                   extended content description won't be written. (For deleting
     *                   tags.).
     * @throws IOException
     *                    read or write errors.
     */
    private void createModifiedCopy(Tag tag, AsfHeader header, RandomAccessFile raf, RandomAccessFile rafTemp, boolean ignoreDescriptions) throws IOException {
        raf.seek(0);
        copy(raf, rafTemp, 30);
        Chunk[] chunks = getOrderedChunks(header);
        long fileSizeDifference = 0;
        long chunkCount = header.getChunkCount();
        long newFileHeaderPos = -1;
        if (header.getExtendedContentDescription() == null && isExtendedContentDescriptionMandatory(tag) && !ignoreDescriptions) {
            chunkCount++;
            fileSizeDifference += createNewExtendedContentDescription(tag, null, rafTemp).getChunkLength().longValue();
        }
        if (header.getContentDescription() == null && isContentdescriptionMandatory(tag) && !ignoreDescriptions) {
            chunkCount++;
            fileSizeDifference += createNewContentDescription(tag, null, rafTemp).getChunkLength().longValue();
        }
        for (int i = 0; i < chunks.length; i++) {
            if (chunks[i] == header.getContentDescription()) {
                if (ignoreDescriptions) {
                    chunkCount--;
                    fileSizeDifference -= header.getContentDescription().getChunkLength().longValue();
                } else {
                    fileSizeDifference += createNewContentDescription(tag, header.getContentDescription(), rafTemp).getChunkLength().subtract(header.getContentDescription().getChunkLength()).longValue();
                }
            } else if (chunks[i] == header.getExtendedContentDescription()) {
                if (deleteExtendedContentDescription(header.getExtendedContentDescription(), tag) || ignoreDescriptions) {
                    chunkCount--;
                    fileSizeDifference -= header.getExtendedContentDescription().getChunkLength().longValue();
                } else {
                    fileSizeDifference += createNewExtendedContentDescription(tag, header.getExtendedContentDescription(), rafTemp).getChunkLength().subtract(header.getExtendedContentDescription().getChunkLength()).longValue();
                }
            } else {
                if (GUID.GUID_FILE.equals(chunks[i].getGuid())) {
                    newFileHeaderPos = rafTemp.getFilePointer();
                }
                raf.seek(chunks[i].getPosition());
                copy(raf, rafTemp, chunks[i].getChunkLength().longValue());
            }
        }
        raf.seek(header.getChunckEnd());
        copy(raf, rafTemp, raf.length() - raf.getFilePointer());
        rafTemp.seek(24);
        write16UINT(chunkCount, rafTemp);
        rafTemp.seek(newFileHeaderPos + 40);
        write32UINT(header.getFileHeader().getFileSize().longValue() + fileSizeDifference, rafTemp);
        rafTemp.seek(16);
        write32UINT(header.getChunkLength().longValue() + fileSizeDifference, rafTemp);
    }

    /**
     * Like
     * {@link #createNewExtendedContentDescription(Tag, ExtendedContentDescription, RandomAccessFile)},
     * this method creates a new chunk for title, ...
     * 
     * @param tag
     *                   tag which contains the values.
     * @param contentDescription
     *                   Description containing old values.
     * @param rafTemp
     *                   output.
     * @return Representation of new Chunk in <code>rafTemp</code>
     * @throws IOException
     *                    write errors.
     */
    private Chunk createNewContentDescription(Tag tag, ContentDescription contentDescription, RandomAccessFile rafTemp) throws IOException {
        long chunkStart = rafTemp.getFilePointer();
        ContentDescription description = TagConverter.createContentDescription(tag);
        if (contentDescription != null) {
            description.setRating(contentDescription.getRating());
        }
        byte[] asfContent = description.getBytes();
        rafTemp.write(asfContent);
        return new Chunk(GUID.GUID_CONTENTDESCRIPTION, chunkStart, BigInteger.valueOf(asfContent.length));
    }

    /**
     * This method writes a completely new extended content description to
     * <code>rafTemp</code>.<br>
     * Some values of {@link Tag}are nested within this chunk. Those values of
     * <code>tagChunk</code> which do not belong to tag will be kept, the rest
     * replaced or even added. <br>
     * 
     * @param tag
     *                   contains new Elements.
     * @param tagChunk
     *                   chunk from source. May contain values, which are not reflected
     *                   by <code>tag</code>. Parameter can be <code>null</code>.
     * @param rafTemp
     *                   File to write to.
     * @return A new chunk object reflecting position and size.
     * @throws IOException
     *                    in Case of write errors.
     */
    private Chunk createNewExtendedContentDescription(Tag tag, ExtendedContentDescription tagChunk, RandomAccessFile rafTemp) throws IOException {
        long chunkStart = rafTemp.getFilePointer();
        if (tagChunk == null) {
            tagChunk = new ExtendedContentDescription();
        }
        TagConverter.assignCommonTagValues(tag, tagChunk);
        TagConverter.assignOptionalTagValues(tag, tagChunk);
        byte[] asfBytes = tagChunk.getBytes();
        rafTemp.write(asfBytes);
        return new Chunk(GUID.GUID_EXTENDED_CONTENT_DESCRIPTION, chunkStart, BigInteger.valueOf(asfBytes.length));
    }

    /**
     * This method decides if the extended content description may be ignored
     * for the modified file. <br>
     * This is the case if the extended content description doesn't contain any
     * additional tags which are not contained in <code>tag</code> and
     * {@link #isExtendedContentDescriptionMandatory(Tag)}returns
     * <code>false</code>.
     * 
     * @param tagHeader
     *                   ContentDescriptor chunk from source.
     * @param tag
     *                   Tag that is written.
     * @return <code>true</code>, if property chunk can be skipped.
     */
    private boolean deleteExtendedContentDescription(ExtendedContentDescription tagHeader, Tag tag) {
        HashSet ignoreDescriptors = new HashSet(Arrays.asList(new String[] { ContentDescriptor.ID_GENRE, ContentDescriptor.ID_GENREID, ContentDescriptor.ID_TRACKNUMBER, ContentDescriptor.ID_ALBUM, ContentDescriptor.ID_YEAR }));
        Iterator it = tagHeader.getDescriptors().iterator();
        boolean found = false;
        while (it.hasNext() && !found) {
            ContentDescriptor current = (ContentDescriptor) it.next();
            found = !ignoreDescriptors.contains(current.getName());
        }
        return !found && !isExtendedContentDescriptionMandatory(tag);
    }

    /**
     * (overridden)
     * 
     * @see entagged.audioformats.generic.AudioFileWriter#deleteTag(java.io.RandomAccessFile,
     *           java.io.RandomAccessFile)
     */
    protected void deleteTag(RandomAccessFile raf, RandomAccessFile tempRaf) throws CannotWriteException, IOException {
        try {
            AsfHeader header = AsfHeaderReader.readHeader(raf);
            if (header == null) {
                throw new NullPointerException("Header is null, so file couldn't be read properly. " + "(Interpretation of data, not file access rights.)");
            }
            createModifiedCopy(new GenericTag(), header, raf, tempRaf, true);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception cre) {
            throw new CannotWriteException("Cannot modify tag because exception occured:\n   " + cre.getMessage());
        }
    }

    /**
     * This method returns all chunks contained within the given
     * <code>header</code> and returns them in an array. Additionally the
     * chunks in the array are sorted ascending by their appearance in the file.
     * <br>
     * This should make it easy to create a copy an modify just specified
     * chunks.
     * 
     * @param header
     *                   The header object containing all chunks.
     * @return All chunks ordered by position.
     */
    private Chunk[] getOrderedChunks(AsfHeader header) {
        ArrayList result = new ArrayList();
        for (int i = 0; i < header.getUnspecifiedChunkCount(); i++) {
            result.add(header.getUnspecifiedChunk(i));
        }
        for (int i = 0; i < header.getStreamChunkCount(); i++) {
            result.add(header.getStreamChunk(i));
        }
        if (header.getContentDescription() != null) {
            result.add(header.getContentDescription());
        }
        result.add(header.getFileHeader());
        if (header.getExtendedContentDescription() != null) {
            result.add(header.getExtendedContentDescription());
        }
        if (header.getEncodingChunk() != null) {
            result.add(header.getEncodingChunk());
        }
        if (header.getStreamBitratePropertiesChunk() != null) {
            result.add(header.getStreamBitratePropertiesChunk());
        }
        Chunk[] tmp = (Chunk[]) result.toArray(new Chunk[result.size()]);
        Arrays.sort(tmp, new ChunkPositionComparator());
        return tmp;
    }

    /**
     * This method decides if a content description chunk is needed in order to
     * store selected values of <code>tag</code>.<br>
     * The selected values are: <br>
     * {@link Tag#getTitle()}<br>
     * {@link Tag#getComment()}<br>
     * {@link Tag#getArtist()}<br>
     * <br>
     * The content description stores another fields which holds the
     * copyright,rating. <br>
     * However entagged doesn't support this.
     * 
     * @param tag
     *                   Tag which should be written.
     * @return <code>true</code>, if a property chunk must be written in
     *               order to store all needed values of tag.
     */
    private boolean isContentdescriptionMandatory(Tag tag) {
        return tag.getFirstArtist().trim().length() > 0 || tag.getFirstComment().trim().length() > 0 || tag.getFirstTitle().trim().length() > 0;
    }

    /**
     * This method decides if a property chunk is needed in order to store
     * selected values of <code>tag</code>.<br>
     * The selected values are: <br>
     * {@link Tag#getTrack()}<br>
     * {@link Tag#getYear()}<br>
     * {@link Tag#getGenre()}<br>
     * {@link Tag#getAlbum()}<br>
     * 
     * @param tag
     *                   Tag which should be written.
     * @return <code>true</code>, if a property chunk must be written in
     *               order to store all needed values of tag.
     */
    private boolean isExtendedContentDescriptionMandatory(Tag tag) {
        return tag.getFirstTrack().trim().length() > 0 || tag.getFirstYear().trim().length() > 0 || tag.getFirstGenre().trim().length() > 0 || tag.getFirstAlbum().trim().length() > 0;
    }

    /**
     * This method writes the lower 16 Bit of <code>value</code> to raf. <br>
     * 
     * @param value
     *                   value
     * @param raf
     *                   output
     * @return number of bytes written (Here always 2)
     * @throws IOException
     *                    Write errrors.
     */
    private int write16UINT(long value, RandomAccessFile raf) throws IOException {
        raf.write((int) value & 0x00FF);
        value >>= 8;
        raf.write((int) value & 0x00FF);
        return 2;
    }

    /**
     * This method writes the lower 32 Bit of <code>value</code> to raf. <br>
     * 
     * @param value
     *                   value
     * @param raf
     *                   output
     * @return number of bytes written (Here always 4)
     * @throws IOException
     *                    Write errrors.
     */
    private int write32UINT(long value, RandomAccessFile raf) throws IOException {
        raf.write((int) value & 0x00FF);
        value >>= 8;
        raf.write((int) value & 0x00FF);
        value >>= 8;
        raf.write((int) value & 0x00FF);
        value >>= 8;
        raf.write((int) value & 0x00FF);
        return 4;
    }

    /**
     * Writes the tag into <code>rafTemp</code>.<br>
     * First the file <code>raf</code> is read as an asf file. If it could be
     * interpreted correctly it should be possible to properly write the
     * modifications.
     * 
     * 
     * @see entagged.audioformats.generic.AudioFileWriter#writeTag(entagged.audioformats.Tag,
     *           java.io.RandomAccessFile, java.io.RandomAccessFile)
     */
    protected void writeTag(Tag tag, RandomAccessFile raf, RandomAccessFile rafTemp) throws CannotWriteException, IOException {
        try {
            AsfHeader header = AsfHeaderReader.readHeader(raf);
            if (header == null) {
                throw new NullPointerException("Header is null, so file couldn't be read properly. " + "(Interpretation of data, not file access rights.)");
            }
            createModifiedCopy(tag, header, raf, rafTemp, false);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception cre) {
            throw new CannotWriteException("Cannot modify tag because exception occured:\n   " + cre.getMessage());
        }
    }
}

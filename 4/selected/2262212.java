package com.hadeslee.audiotag.tag.id3;

import com.hadeslee.audiotag.audio.mp3.MP3File;
import com.hadeslee.audiotag.audio.generic.Utils;
import com.hadeslee.audiotag.tag.FieldDataInvalidException;
import com.hadeslee.audiotag.tag.KeyNotFoundException;
import com.hadeslee.audiotag.tag.Tag;
import com.hadeslee.audiotag.tag.TagField;
import com.hadeslee.audiotag.tag.TagFieldKey;
import com.hadeslee.audiotag.tag.id3.valuepair.PictureTypes;
import com.hadeslee.audiotag.tag.id3.valuepair.ImageFormats;
import com.hadeslee.audiotag.tag.id3.valuepair.TextEncoding;
import com.hadeslee.audiotag.tag.datatype.DataTypes;
import com.hadeslee.audiotag.tag.id3.framebody.AbstractFrameBodyTextInfo;
import com.hadeslee.audiotag.tag.id3.framebody.AbstractFrameBodyUrlLink;
import com.hadeslee.audiotag.tag.id3.framebody.FrameBodyAPIC;
import com.hadeslee.audiotag.tag.id3.framebody.FrameBodyCOMM;
import com.hadeslee.audiotag.tag.id3.framebody.FrameBodyPIC;
import com.hadeslee.audiotag.tag.id3.framebody.FrameBodyTDRC;
import com.hadeslee.audiotag.tag.id3.framebody.FrameBodyTXXX;
import com.hadeslee.audiotag.tag.id3.framebody.FrameBodyUFID;
import com.hadeslee.audiotag.tag.id3.framebody.FrameBodyUnsupported;
import com.hadeslee.audiotag.tag.id3.framebody.FrameBodyWXXX;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.*;

/**
 * This is the abstract base class for all ID3v2 tags.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id: AbstractID3v2Tag.java,v 1.30 2007/12/03 13:28:04 paultaylor Exp $
 */
public abstract class AbstractID3v2Tag extends AbstractID3Tag implements Tag {

    protected static final String TYPE_HEADER = "header";

    protected static final String TYPE_BODY = "body";

    protected static final byte[] TAG_ID = { 'I', 'D', '3' };

    public static final int TAG_HEADER_LENGTH = 10;

    protected static final int FIELD_TAGID_LENGTH = 3;

    protected static final int FIELD_TAG_MAJOR_VERSION_LENGTH = 1;

    protected static final int FIELD_TAG_MINOR_VERSION_LENGTH = 1;

    protected static final int FIELD_TAG_FLAG_LENGTH = 1;

    protected static final int FIELD_TAG_SIZE_LENGTH = 4;

    protected static final int FIELD_TAGID_POS = 0;

    protected static final int FIELD_TAG_MAJOR_VERSION_POS = 3;

    protected static final int FIELD_TAG_MINOR_VERSION_POS = 4;

    protected static final int FIELD_TAG_FLAG_POS = 5;

    protected static final int FIELD_TAG_SIZE_POS = 6;

    protected static final int TAG_SIZE_INCREMENT = 100;

    private static final long MAXIMUM_WRITABLE_CHUNK_SIZE = 10000000;

    /**
     * Map of all frames for this tag
     */
    public HashMap frameMap = null;

    /**
     * Holds the ids of invalid duplicate frames
     */
    protected static final String TYPE_DUPLICATEFRAMEID = "duplicateFrameId";

    protected String duplicateFrameId = "";

    /**
     * Holds byte count of invalid duplicate frames
     */
    protected static final String TYPE_DUPLICATEBYTES = "duplicateBytes";

    protected int duplicateBytes = 0;

    /**
     * Holds byte count of empty frames
     */
    protected static final String TYPE_EMPTYFRAMEBYTES = "emptyFrameBytes";

    protected int emptyFrameBytes = 0;

    /**
     * Holds the size of the tag as reported by the tag header
     */
    protected static final String TYPE_FILEREADSIZE = "fileReadSize";

    protected int fileReadSize = 0;

    /**
     * Holds byte count of invalid frames
     */
    protected static final String TYPE_INVALIDFRAMEBYTES = "invalidFrameBytes";

    protected int invalidFrameBytes = 0;

    /**
     * Empty Constructor
     */
    public AbstractID3v2Tag() {
    }

    /**
     * This constructor is used when a tag is created as a duplicate of another
     * tag of the same type and version.
     */
    protected AbstractID3v2Tag(AbstractID3v2Tag copyObject) {
    }

    /**
     * Copy primitives apply to all tags
     */
    protected void copyPrimitives(AbstractID3v2Tag copyObject) {
        logger.info("Copying Primitives");
        this.duplicateFrameId = new String(copyObject.duplicateFrameId);
        this.duplicateBytes = copyObject.duplicateBytes;
        this.emptyFrameBytes = copyObject.emptyFrameBytes;
        this.fileReadSize = copyObject.fileReadSize;
        this.invalidFrameBytes = copyObject.invalidFrameBytes;
    }

    /**
     * Copy frames from another tag, needs implemanting by subclasses
     */
    protected abstract void copyFrames(AbstractID3v2Tag copyObject);

    /**
     * Returns the number of bytes which come from duplicate frames
     *
     * @return the number of bytes which come from duplicate frames
     */
    public int getDuplicateBytes() {
        return duplicateBytes;
    }

    /**
     * Return the string which holds the ids of all
     * duplicate frames.
     *
     * @return the string which holds the ids of all duplicate frames.
     */
    public String getDuplicateFrameId() {
        return duplicateFrameId;
    }

    /**
     * Returns the number of bytes which come from empty frames
     *
     * @return the number of bytes which come from empty frames
     */
    public int getEmptyFrameBytes() {
        return emptyFrameBytes;
    }

    /**
     * Return  byte count of invalid frames
     *
     * @return byte count of invalid frames
     */
    public int getInvalidFrameBytes() {
        return invalidFrameBytes;
    }

    /**
     * Returns the tag size as reported by the tag header
     *
     * @return the tag size as reported by the tag header
     */
    public int getFileReadBytes() {
        return fileReadSize;
    }

    /**
     * Return whether tag has frame with this identifier
     * <p/>
     * Warning the match is only done against the identifier so if a tag contains a frame with an unsuported body
     * but happens to have an identifier that is valid for another version of the tag it will return true
     *
     * @param identifier frameId to lookup
     * @return true if tag has frame with this identifier
     */
    public boolean hasFrame(String identifier) {
        return frameMap.containsKey(identifier);
    }

    /**
     * Return whether tag has frame with this identifier and a related body. This is required to protect
     * against circumstances whereby a tag contains a frame with an unsupported body
     * but happens to have an identifier that is valid for another version of the tag which it has been converted to
     * <p/>
     * e.g TDRC is an invalid frame in a v23 tag but if somehow a v23tag has been created by another application
     * with a TDRC frame we construct an UnsupportedFrameBody to hold it, then this library constructs a
     * v24 tag, it will contain a frame with id TDRC but it will not have the expected frame body it is not really a
     * TDRC frame.
     *
     * @param identifier frameId to lookup
     * @return true if tag has frame with this identifier
     */
    public boolean hasFrameAndBody(String identifier) {
        if (hasFrame(identifier)) {
            Object o = getFrame(identifier);
            if (o instanceof AbstractID3v2Frame) {
                if (((AbstractID3v2Frame) o).getBody() instanceof FrameBodyUnsupported) {
                    return false;
                }
                return true;
            }
            return true;
        }
        return false;
    }

    /**
     * Return whether tag has frame starting with this identifier
     * <p/>
     * Warning the match is only done against the identifier so if a tag contains a frame with an unsupported body
     * but happens to have an identifier that is valid for another version of the tag it will return true
     *
     * @param identifier start of frameId to lookup
     * @return tag has frame starting with this identifier
     */
    public boolean hasFrameOfType(String identifier) {
        Iterator iterator = frameMap.keySet().iterator();
        String key;
        boolean found = false;
        while (iterator.hasNext() && !found) {
            key = (String) iterator.next();
            if (key.startsWith(identifier)) {
                found = true;
            }
        }
        return found;
    }

    public Object getFrame(String identifier) {
        return frameMap.get(identifier);
    }

    public String getFirst(String identifier) {
        AbstractID3v2Frame frame = getFirstField(identifier);
        if (frame == null) {
            return "";
        }
        if (frame.getBody() instanceof FrameBodyCOMM) {
            return ((FrameBodyCOMM) frame.getBody()).getText();
        } else if (frame.getBody() instanceof AbstractFrameBodyTextInfo) {
            return ((AbstractFrameBodyTextInfo) frame.getBody()).getFirstTextValue();
        } else if (frame.getBody() instanceof AbstractFrameBodyUrlLink) {
            return ((AbstractFrameBodyUrlLink) frame.getBody()).getUrlLink();
        } else {
            return frame.getBody().toString();
        }
    }

    /**
     * Retrieve the first tagfield that exists for this identifier
     *
     * @param identifier
     * @return tag field or null if doesnt exist
     */
    public AbstractID3v2Frame getFirstField(String identifier) {
        Object object = getFrame(identifier);
        if (object == null) {
            return null;
        }
        if (object instanceof List) {
            return (AbstractID3v2Frame) ((List) object).get(0);
        } else {
            return (AbstractID3v2Frame) object;
        }
    }

    public void setFrame(AbstractID3v2Frame frame) {
        frameMap.put(frame.getIdentifier(), frame);
    }

    protected abstract ID3Frames getID3Frames();

    /**
     * @param field
     * @throws FieldDataInvalidException
     */
    public void set(TagField field) throws FieldDataInvalidException {
        if (!(field instanceof AbstractID3v2Frame)) {
            throw new FieldDataInvalidException("Field " + field + " is not of type AbstractID3v2Frame");
        }
        AbstractID3v2Frame newFrame = (AbstractID3v2Frame) field;
        Object o = frameMap.get(field.getId());
        if (o == null || (!getID3Frames().isMultipleAllowed(newFrame.getId()))) {
            System.out.println("Replacing....");
            frameMap.put(field.getId(), field);
        } else if (o instanceof AbstractID3v2Frame) {
            System.out.println("Frame exists");
            AbstractID3v2Frame oldFrame = (AbstractID3v2Frame) o;
            if (newFrame.getBody() instanceof FrameBodyTXXX) {
                if (!((FrameBodyTXXX) newFrame.getBody()).getDescription().equals(((FrameBodyTXXX) oldFrame.getBody()).getDescription())) {
                    List<AbstractID3v2Frame> frames = new ArrayList<AbstractID3v2Frame>();
                    frames.add(oldFrame);
                    frames.add(newFrame);
                    frameMap.put(newFrame.getId(), frames);
                    System.out.println("Adding....");
                } else {
                    System.out.println("Replacing key....");
                    frameMap.put(newFrame.getId(), newFrame);
                }
            }
        } else if (o instanceof List) {
            for (ListIterator<AbstractID3v2Frame> li = ((List<AbstractID3v2Frame>) o).listIterator(); li.hasNext(); ) {
                AbstractID3v2Frame nextFrame = li.next();
                if (newFrame.getBody() instanceof FrameBodyTXXX) {
                    if (((FrameBodyTXXX) newFrame.getBody()).getDescription().equals(((FrameBodyTXXX) nextFrame.getBody()).getDescription())) {
                        li.set(newFrame);
                        frameMap.put(newFrame.getId(), o);
                    }
                }
            }
            ((List<AbstractID3v2Frame>) o).add(newFrame);
        }
    }

    public void setAlbum(String s) throws FieldDataInvalidException {
        set(createAlbumField(s));
    }

    public void setArtist(String s) throws FieldDataInvalidException {
        set(createArtistField(s));
    }

    public void setComment(String s) throws FieldDataInvalidException {
        set(createCommentField(s));
    }

    public void setGenre(String s) throws FieldDataInvalidException {
        set(createGenreField(s));
    }

    public void setTitle(String s) throws FieldDataInvalidException {
        set(createTitleField(s));
    }

    public void setTrack(String s) throws FieldDataInvalidException {
        set(createTrackField(s));
    }

    public void setYear(String s) throws FieldDataInvalidException {
        set(createYearField(s));
    }

    /**
     * @param field
     * @throws FieldDataInvalidException
     */
    public void add(TagField field) throws FieldDataInvalidException {
        if (field == null) {
            return;
        }
        if (!(field instanceof AbstractID3v2Frame)) {
            throw new FieldDataInvalidException("Field " + field + " is not of type AbstractID3v2Frame");
        }
        Object o = frameMap.get(field.getId());
        if (o instanceof List) {
            List list = (List) o;
            list.add(field);
        } else if (o == null) {
            frameMap.put(field.getId(), field);
        } else {
            List list = new ArrayList();
            list.add(o);
            list.add(field);
            frameMap.put(field.getId(), list);
        }
    }

    /**
     * Adds an album to the tag.<br>
     *
     * @param album Album description
     */
    public void addAlbum(String album) throws FieldDataInvalidException {
        add(createAlbumField(album));
    }

    /**
     * Adds an artist to the tag.<br>
     *
     * @param artist Artist's name
     */
    public void addArtist(String artist) throws FieldDataInvalidException {
        add(createArtistField(artist));
    }

    /**
     * Adds a comment to the tag.<br>
     *
     * @param comment Comment.
     */
    public void addComment(String comment) throws FieldDataInvalidException {
        add(createCommentField(comment));
    }

    /**
     * Adds a genre to the tag.<br>
     *
     * @param genre Genre
     */
    public void addGenre(String genre) throws FieldDataInvalidException {
        add(createGenreField(genre));
    }

    /**
     * Adds a title to the tag.<br>
     *
     * @param title Title
     */
    public void addTitle(String title) throws FieldDataInvalidException {
        add(createTitleField(title));
    }

    /**
     * Adds a track to the tag.<br>
     *
     * @param track Track
     */
    public void addTrack(String track) throws FieldDataInvalidException {
        add(createTrackField(track));
    }

    /**
     * Adds a year to the Tag.<br>
     *
     * @param year Year
     */
    public void addYear(String year) throws FieldDataInvalidException {
        add(createYearField(year));
    }

    /**
     * Used for setting multiple frames for a single frame Identifier
     * <p/>
     * Warning if frame(s) already exists for this identifier thay are overwritten
     * <p/>
     * TODO needs to ensure do not add an invalid frame for this tag
     */
    public void setFrame(String identifier, List<AbstractID3v2Frame> multiFrame) {
        frameMap.put(identifier, multiFrame);
    }

    /**
     * Return the number of frames in this tag of a particular type, multiple frames
     * of the same time will only be counted once
     *
     * @return a count of different frames
     */
    public int getFrameCount() {
        if (frameMap == null) {
            return 0;
        } else {
            return frameMap.size();
        }
    }

    /**
     * Return all frames which start with the identifier, this
     * can be more than one which is useful if trying to retrieve
     * similar frames e.g TIT1,TIT2,TIT3 ... and don't know exaclty
     * which ones there are.
     * <p/>
     * Warning the match is only done against the identifier so if a tag contains a frame with an unsupported body
     * but happens to have an identifier that is valid for another version of the tag it will be returned.
     *
     * @param identifier
     * @return an iterator of all the frames starting with a particular identifier
     */
    public Iterator getFrameOfType(String identifier) {
        Iterator iterator = frameMap.keySet().iterator();
        HashSet result = new HashSet();
        String key;
        while (iterator.hasNext()) {
            key = (String) iterator.next();
            if (key.startsWith(identifier)) {
                result.add(frameMap.get(key));
            }
        }
        return result.iterator();
    }

    public void delete(RandomAccessFile file) throws IOException {
        byte[] buffer = new byte[FIELD_TAGID_LENGTH];
        final FileChannel fc = file.getChannel();
        fc.position();
        ByteBuffer byteBuffer = ByteBuffer.allocate(TAG_HEADER_LENGTH);
        fc.read(byteBuffer, 0);
        byteBuffer.flip();
        if (seek(byteBuffer)) {
            file.seek(0L);
            file.write(buffer);
        }
    }

    /**
     * Is this tag equivalent to another
     *
     * @param obj to test for equivalence
     * @return true if they are equivalent
     */
    public boolean equals(Object obj) {
        if ((obj instanceof AbstractID3v2Tag) == false) {
            return false;
        }
        AbstractID3v2Tag object = (AbstractID3v2Tag) obj;
        if (this.frameMap.equals(object.frameMap) == false) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * Return the frames in the order they were added
     *
     * @return and iterator of the frmaes/list of multi value frames
     */
    public Iterator iterator() {
        return frameMap.values().iterator();
    }

    /**
     * Remove frame(s) with this identifier from tag
     *
     * @param identifier frameId to look for
     */
    public void removeFrame(String identifier) {
        logger.finest("Removing frame with identifier:" + identifier);
        frameMap.remove(identifier);
    }

    /**
     * Remove all frame(s) which have an unsupported body, in other words
     * remove all frames that are not part of the standard frameset for
     * this tag
     */
    public void removeUnsupportedFrames() {
        for (Iterator i = iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof AbstractID3v2Frame) {
                if (((AbstractID3v2Frame) o).getBody() instanceof FrameBodyUnsupported) {
                    logger.finest("Removing frame" + ((AbstractID3v2Frame) o).getIdentifier());
                    i.remove();
                }
            }
        }
    }

    /**
     * Remove any frames starting with this
     * identifier from tag
     *
     * @param identifier start of frameId to look for
     */
    public void removeFrameOfType(String identifier) {
        Iterator iterator = this.getFrameOfType(identifier);
        while (iterator.hasNext()) {
            AbstractID3v2Frame frame = (AbstractID3v2Frame) iterator.next();
            logger.finest("Removing frame with identifier:" + frame.getIdentifier() + "because starts with:" + identifier);
            frameMap.remove(frame.getIdentifier());
        }
    }

    /**
     * Write tag to file.
     *
     * @param file
     * @param audioStartByte
     * @throws IOException TODO should be abstract
     */
    public void write(File file, long audioStartByte) throws IOException {
    }

    /**
     * Write tag to file.
     *
     * @param file
     * @throws IOException TODO should be abstract
     */
    public void write(RandomAccessFile file) throws IOException {
    }

    /**
     * Write tag to channel.
     *
     * @param channel
     * @throws IOException TODO should be abstract
     */
    public void write(WritableByteChannel channel) throws IOException {
    }

    /**
     * Checks to see if the file contains an ID3tag and if so return its size as reported in
     * the tag header  and return the size of the tag (including header), if no such tag exists return
     * zero.
     *
     * @param file
     * @return the end of the tag in the file or zero if no tag exists.
     */
    public static long getV2TagSizeIfExists(File file) throws IOException {
        FileInputStream fis = null;
        FileChannel fc = null;
        ByteBuffer bb = null;
        try {
            fis = new FileInputStream(file);
            fc = fis.getChannel();
            bb = ByteBuffer.allocate(TAG_HEADER_LENGTH);
            fc.read(bb);
            bb.flip();
            if (bb.limit() < (TAG_HEADER_LENGTH)) {
                return 0;
            }
        } finally {
            if (fc != null) {
                fc.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
        byte[] tagIdentifier = new byte[FIELD_TAGID_LENGTH];
        bb.get(tagIdentifier, 0, FIELD_TAGID_LENGTH);
        if (!(Arrays.equals(tagIdentifier, TAG_ID))) {
            return 0;
        }
        byte majorVersion = bb.get();
        if ((majorVersion != ID3v22Tag.MAJOR_VERSION) && (majorVersion != ID3v23Tag.MAJOR_VERSION) && (majorVersion != ID3v24Tag.MAJOR_VERSION)) {
            return 0;
        }
        bb.get();
        bb.get();
        int frameSize = ID3SyncSafeInteger.bufferToValue(bb);
        frameSize += TAG_HEADER_LENGTH;
        return frameSize;
    }

    /**
     * Does a tag of the correct version exist in this file.
     *
     * @param byteBuffer to search through
     * @return true if tag exists.
     */
    public boolean seek(ByteBuffer byteBuffer) {
        byteBuffer.rewind();
        logger.info("ByteBuffer pos:" + byteBuffer.position() + ":limit" + byteBuffer.limit() + ":cap" + byteBuffer.capacity());
        byte[] tagIdentifier = new byte[FIELD_TAGID_LENGTH];
        byteBuffer.get(tagIdentifier, 0, FIELD_TAGID_LENGTH);
        if (!(Arrays.equals(tagIdentifier, TAG_ID))) {
            return false;
        }
        if (byteBuffer.get() != getMajorVersion()) {
            return false;
        }
        if (byteBuffer.get() != getRevision()) {
            return false;
        }
        return true;
    }

    /**
     * This method determines the total tag size taking into account
     * where the audio file starts, the size of the tagging data and
     * user options for defining how tags should shrink or grow.
     */
    protected int calculateTagSize(int tagSize, int audioStart) {
        if (tagSize <= audioStart) {
            return audioStart;
        }
        return tagSize + TAG_SIZE_INCREMENT;
    }

    /**
     * Adjust the length of the  padding at the beginning of the MP3 file, this is only called when there is currently
     * not enough space before the start of the audio to write the tag.
     * <p/>
     * A new file will be created with enough size to fit the <code>ID3v2</code> tag.
     * The old file will be deleted, and the new file renamed.
     *
     * @param paddingSize This is total size required to store tag before audio
     * @param file        The file to adjust the padding length of
     * @throws FileNotFoundException if the file exists but is a directory
     *                               rather than a regular file or cannot be opened for any other
     *                               reason
     * @throws IOException           on any I/O error
     */
    public void adjustPadding(File file, int paddingSize, long audioStart) throws FileNotFoundException, IOException {
        logger.finer("Need to move audio file to accomodate tag");
        FileChannel fcIn;
        FileChannel fcOut;
        ByteBuffer paddingBuffer = ByteBuffer.wrap(new byte[paddingSize]);
        File paddedFile = File.createTempFile("temp", ".mp3", file.getParentFile());
        fcOut = new FileOutputStream(paddedFile).getChannel();
        fcIn = new FileInputStream(file).getChannel();
        long written = (long) fcOut.write(paddingBuffer);
        logger.finer("Copying:" + (file.length() - audioStart) + "bytes");
        long audiolength = file.length() - audioStart;
        if (audiolength <= MAXIMUM_WRITABLE_CHUNK_SIZE) {
            long written2 = fcIn.transferTo(audioStart, audiolength, fcOut);
            logger.finer("Written padding:" + written + " Data:" + written2);
            if (written2 != audiolength) {
                throw new RuntimeException("Problem adjusting padding, expecting to write:" + audiolength + ":only wrote:" + written2);
            }
        } else {
            long noOfChunks = audiolength / MAXIMUM_WRITABLE_CHUNK_SIZE;
            long lastChunkSize = audiolength % MAXIMUM_WRITABLE_CHUNK_SIZE;
            long written2 = 0;
            for (int i = 0; i < noOfChunks; i++) {
                written2 += fcIn.transferTo(audioStart + (i * MAXIMUM_WRITABLE_CHUNK_SIZE), MAXIMUM_WRITABLE_CHUNK_SIZE, fcOut);
                Runtime.getRuntime().gc();
            }
            written2 += fcIn.transferTo(audioStart + (noOfChunks * MAXIMUM_WRITABLE_CHUNK_SIZE), lastChunkSize, fcOut);
            logger.finer("Written padding:" + written + " Data:" + written2);
            if (written2 != audiolength) {
                throw new RuntimeException("Problem adjusting padding in large file, expecting to write:" + audiolength + ":only wrote:" + written2);
            }
        }
        long lastModified = file.lastModified();
        fcIn.close();
        fcOut.close();
        file.delete();
        paddedFile.renameTo(file);
        paddedFile.setLastModified(lastModified);
    }

    /**
     * Add frame to HashMap used when converting between tag versions, take into account
     * occurences when two frame may both map to a single frame when converting between
     * versions
     * <p/>
     * TODO the logic here is messy and seems to be specific to date fields only when it
     * was intended to be generic.
     */
    protected void copyFrameIntoMap(String id, AbstractID3v2Frame newFrame) {
        if (frameMap.containsKey(newFrame.getIdentifier())) {
            AbstractID3v2Frame firstFrame = (AbstractID3v2Frame) frameMap.get(newFrame.getIdentifier());
            if (newFrame.getBody() instanceof FrameBodyTDRC) {
                if (firstFrame.getBody() instanceof FrameBodyTDRC) {
                    logger.finest("Modifying frame in map:" + newFrame.getIdentifier());
                    FrameBodyTDRC body = (FrameBodyTDRC) firstFrame.getBody();
                    FrameBodyTDRC newBody = (FrameBodyTDRC) newFrame.getBody();
                    if (newBody.getOriginalID().equals(ID3v23Frames.FRAME_ID_V3_TYER)) {
                        body.setYear(newBody.getText());
                    } else if (newBody.getOriginalID().equals(ID3v23Frames.FRAME_ID_V3_TDAT)) {
                        body.setDate(newBody.getText());
                    } else if (newBody.getOriginalID().equals(ID3v23Frames.FRAME_ID_V3_TIME)) {
                        body.setTime(newBody.getText());
                    } else if (newBody.getOriginalID().equals(ID3v23Frames.FRAME_ID_V3_TRDA)) {
                        body.setReco(newBody.getText());
                    }
                } else if (firstFrame.getBody() instanceof FrameBodyUnsupported) {
                    frameMap.put(newFrame.getIdentifier(), newFrame);
                } else {
                    logger.warning("Found duplicate TDRC frame in invalid situation,discarding:" + newFrame.getIdentifier());
                }
            } else {
                logger.warning("Found duplicate frame in invalid situation,discarding:" + newFrame.getIdentifier());
            }
        } else {
            logger.finest("Adding frame to map:" + newFrame.getIdentifier());
            frameMap.put(newFrame.getIdentifier(), newFrame);
        }
    }

    /**
     * Decides what to with the frame that has just be read from file.
     * If the frame is an allowable duplicate frame and is a duplicate we add all
     * frames into an ArrayList and add the Arraylist to the hashMap. if not allowed
     * to be duplicate we store bytes in the duplicateBytes variable.
     */
    protected void loadFrameIntoMap(String frameId, AbstractID3v2Frame next) {
        if ((ID3v24Frames.getInstanceOf().isMultipleAllowed(frameId)) || (ID3v23Frames.getInstanceOf().isMultipleAllowed(frameId)) || (ID3v22Frames.getInstanceOf().isMultipleAllowed(frameId))) {
            if (frameMap.containsKey(frameId)) {
                Object o = frameMap.get(frameId);
                if (o instanceof ArrayList) {
                    ArrayList multiValues = (ArrayList) o;
                    multiValues.add(next);
                    logger.finer("Adding Multi Frame(1)" + frameId);
                } else {
                    ArrayList multiValues = new ArrayList();
                    multiValues.add(o);
                    multiValues.add(next);
                    frameMap.put(frameId, multiValues);
                    logger.finer("Adding Multi Frame(2)" + frameId);
                }
            } else {
                logger.finer("Adding Multi FrameList(3)" + frameId);
                frameMap.put(frameId, next);
            }
        } else if (frameMap.containsKey(frameId)) {
            logger.warning("Duplicate Frame" + frameId);
            this.duplicateFrameId += (frameId + "; ");
            this.duplicateBytes += ((AbstractID3v2Frame) frameMap.get(frameId)).getSize();
        } else {
            logger.finer("Adding Frame" + frameId);
            frameMap.put(frameId, next);
        }
    }

    /**
     * Return tag size based upon the sizes of the tags rather than the physical
     * no of bytes between start of ID3Tag and start of Audio Data.Should be extended
     * by subclasses to include header.
     *
     * @return size of the tag
     */
    public int getSize() {
        int size = 0;
        Iterator iterator = frameMap.values().iterator();
        AbstractID3v2Frame frame;
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (o instanceof AbstractID3v2Frame) {
                frame = (AbstractID3v2Frame) o;
                size += frame.getSize();
            } else {
                ArrayList multiFrames = (ArrayList) o;
                for (ListIterator li = multiFrames.listIterator(); li.hasNext(); ) {
                    frame = (AbstractID3v2Frame) li.next();
                    size += frame.getSize();
                }
            }
        }
        return size;
    }

    protected ByteArrayOutputStream writeFramesToBuffer() throws IOException {
        ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
        AbstractID3v2Frame frame;
        Iterator iterator;
        iterator = frameMap.values().iterator();
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (o instanceof AbstractID3v2Frame) {
                frame = (AbstractID3v2Frame) o;
                frame.write(bodyBuffer);
            } else {
                ArrayList multiFrames = (ArrayList) o;
                for (ListIterator li = multiFrames.listIterator(); li.hasNext(); ) {
                    frame = (AbstractID3v2Frame) li.next();
                    frame.write(bodyBuffer);
                }
            }
        }
        return bodyBuffer;
    }

    public void createStructure() {
        createStructureHeader();
        createStructureBody();
    }

    public void createStructureHeader() {
        MP3File.getStructureFormatter().addElement(this.TYPE_DUPLICATEBYTES, this.duplicateBytes);
        MP3File.getStructureFormatter().addElement(this.TYPE_DUPLICATEFRAMEID, this.duplicateFrameId);
        MP3File.getStructureFormatter().addElement(this.TYPE_EMPTYFRAMEBYTES, this.emptyFrameBytes);
        MP3File.getStructureFormatter().addElement(this.TYPE_FILEREADSIZE, this.fileReadSize);
        MP3File.getStructureFormatter().addElement(this.TYPE_INVALIDFRAMEBYTES, this.invalidFrameBytes);
    }

    public void createStructureBody() {
        MP3File.getStructureFormatter().openHeadingElement(TYPE_BODY, "");
        AbstractID3v2Frame frame;
        for (Object o : frameMap.values()) {
            if (o instanceof AbstractID3v2Frame) {
                frame = (AbstractID3v2Frame) o;
                frame.createStructure();
            } else {
                ArrayList multiFrames = (ArrayList) o;
                for (ListIterator li = multiFrames.listIterator(); li.hasNext(); ) {
                    frame = (AbstractID3v2Frame) li.next();
                    frame.createStructure();
                }
            }
        }
        MP3File.getStructureFormatter().closeHeadingElement(TYPE_BODY);
    }

    /**
     * Retrieve the  values that exists for this id3 frame id
     */
    public List<TagField> get(String id) throws KeyNotFoundException {
        Object o = getFrame(id);
        if (o == null) {
            return new ArrayList<TagField>();
        } else if (o instanceof List) {
            return (List) o;
        } else if (o instanceof AbstractID3v2Frame) {
            List list = new ArrayList<TagField>();
            list.add(o);
            return list;
        } else {
            throw new RuntimeException("Found entry in frameMap that was not a frame or a list:" + o);
        }
    }

    public List getAlbum() {
        return get(getAlbumId());
    }

    public List getArtist() {
        return get(getArtistId());
    }

    public List getComment() {
        return get(getCommentId());
    }

    public List getGenre() {
        return get(getGenreId());
    }

    public List getTitle() {
        return get(getTitleId());
    }

    public List getTrack() {
        return get(getTrackId());
    }

    public List getYear() {
        return get(getYearId());
    }

    /**
     * @return
     */
    public String getFirstAlbum() {
        List l = getAlbum();
        return (l.size() != 0) ? ((AbstractFrameBodyTextInfo) ((AbstractID3v2Frame) l.get(0)).getBody()).getText() : "";
    }

    /**
     * @return
     */
    public String getFirstArtist() {
        List l = getArtist();
        return (l.size() != 0) ? ((AbstractFrameBodyTextInfo) ((AbstractID3v2Frame) l.get(0)).getBody()).getText() : "";
    }

    /**
     * @return
     */
    public String getFirstComment() {
        List l = getComment();
        return (l.size() != 0) ? ((FrameBodyCOMM) ((AbstractID3v2Frame) l.get(0)).getBody()).getText() : "";
    }

    /**
     * @return
     */
    public String getFirstGenre() {
        List l = getGenre();
        return (l.size() != 0) ? ((AbstractFrameBodyTextInfo) ((AbstractID3v2Frame) l.get(0)).getBody()).getText() : "";
    }

    /**
     * @return
     */
    public String getFirstTitle() {
        List l = getTitle();
        return (l.size() != 0) ? ((AbstractFrameBodyTextInfo) ((AbstractID3v2Frame) l.get(0)).getBody()).getText() : "";
    }

    /**
     * @return
     */
    public String getFirstTrack() {
        List l = getTrack();
        return (l.size() != 0) ? ((AbstractFrameBodyTextInfo) ((AbstractID3v2Frame) l.get(0)).getBody()).getText() : "";
    }

    /**
     * @return
     */
    public String getFirstYear() {
        List l = getYear();
        return (l.size() != 0) ? ((AbstractFrameBodyTextInfo) ((AbstractID3v2Frame) l.get(0)).getBody()).getText() : "";
    }

    /**
     * @return
     */
    protected abstract String getArtistId();

    /**
     * @return
     */
    protected abstract String getAlbumId();

    /**
     * @return
     */
    protected abstract String getTitleId();

    /**
     * @return
     */
    protected abstract String getTrackId();

    /**
     * @return
     */
    protected abstract String getYearId();

    /**
     * @return
     */
    protected abstract String getCommentId();

    /**
     * @return
     */
    protected abstract String getGenreId();

    /**
     * Create Frame of correct ID3 version with the specified id
     *
     * @param id
     * @return
     */
    public abstract AbstractID3v2Frame createFrame(String id);

    /**
     * @param content
     * @return
     */
    public TagField createArtistField(String content) {
        AbstractID3v2Frame frame = createFrame(getArtistId());
        ((AbstractFrameBodyTextInfo) frame.getBody()).setText(content);
        return frame;
    }

    /**
     * @param content
     * @return
     */
    public TagField createAlbumField(String content) {
        AbstractID3v2Frame frame = createFrame(getAlbumId());
        ((AbstractFrameBodyTextInfo) frame.getBody()).setText(content);
        return frame;
    }

    /**
     * @param content
     * @return
     */
    public TagField createTitleField(String content) {
        AbstractID3v2Frame frame = createFrame(getTitleId());
        ((AbstractFrameBodyTextInfo) frame.getBody()).setText(content);
        return frame;
    }

    /**
     * @param content
     * @return
     */
    public TagField createTrackField(String content) {
        AbstractID3v2Frame frame = createFrame(getTrackId());
        ((AbstractFrameBodyTextInfo) frame.getBody()).setText(content);
        return frame;
    }

    /**
     * @param content
     * @return
     */
    public TagField createYearField(String content) {
        AbstractID3v2Frame frame = createFrame(getYearId());
        ((AbstractFrameBodyTextInfo) frame.getBody()).setText(content);
        return frame;
    }

    /**
     * @param content
     * @return
     */
    public TagField createCommentField(String content) {
        AbstractID3v2Frame frame = createFrame(getCommentId());
        ((FrameBodyCOMM) frame.getBody()).setText(content);
        return frame;
    }

    /**
     * @param content
     * @return
     */
    public TagField createGenreField(String content) {
        AbstractID3v2Frame frame = createFrame(getGenreId());
        ((AbstractFrameBodyTextInfo) frame.getBody()).setText(content);
        return frame;
    }

    public boolean hasCommonFields() {
        return true;
    }

    /**
     * Does this tag contain a field with the specified id
     *
     * @see com.hadeslee.jaudiotagger.tag.Tag#hasField(java.lang.String)
     */
    public boolean hasField(String id) {
        return get(id).size() != 0;
    }

    /**
     * Is this tag empty
     *
     * @see com.hadeslee.jaudiotagger.tag.Tag#isEmpty()
     */
    public boolean isEmpty() {
        return frameMap.size() == 0;
    }

    public Iterator getFields() {
        final Iterator<Map.Entry<String, Object>> it = this.frameMap.entrySet().iterator();
        return new Iterator() {

            private Iterator fieldsIt;

            private void changeIt() {
                if (!it.hasNext()) {
                    return;
                }
                Map.Entry<String, Object> e = it.next();
                if (e.getValue() instanceof List) {
                    List<TagField> l = (List) e.getValue();
                    fieldsIt = l.iterator();
                } else {
                    List<TagField> l = new ArrayList<TagField>();
                    l.add((TagField) e.getValue());
                    fieldsIt = l.iterator();
                }
            }

            public boolean hasNext() {
                if (fieldsIt == null) {
                    changeIt();
                }
                return it.hasNext() || (fieldsIt != null && fieldsIt.hasNext());
            }

            public Object next() {
                if (!fieldsIt.hasNext()) {
                    changeIt();
                }
                return fieldsIt.next();
            }

            public void remove() {
                fieldsIt.remove();
            }
        };
    }

    public int getFieldCount() {
        Iterator it = getFields();
        int count = 0;
        while (it.hasNext()) {
            count++;
            it.next();
        }
        return count;
    }

    public boolean setEncoding(String enc) throws FieldDataInvalidException {
        throw new UnsupportedOperationException("Not Implemented Yet");
    }

    /**
     * Retrieve the first value that exists for this generic key
     *
     * @param genericKey
     * @return
     */
    public String getFirst(TagFieldKey genericKey) throws KeyNotFoundException {
        if (genericKey == null) {
            throw new KeyNotFoundException();
        }
        return doGetFirst(getFrameAndSubIdFromGenericKey(genericKey));
    }

    /**
     * Create a new TagField
     * <p/>
     * Only textual data supported at the moment. The genericKey will be mapped
     * to the correct implementation key and return a TagField.
     *
     * @param genericKey is the generic key
     * @param value      to store
     * @return
     */
    public TagField createTagField(TagFieldKey genericKey, String value) throws KeyNotFoundException, FieldDataInvalidException {
        if (genericKey == null) {
            throw new KeyNotFoundException();
        }
        return doCreateTagField(getFrameAndSubIdFromGenericKey(genericKey), value);
    }

    /**
     * Create Frame for Id3 Key
     * <p/>
     * Only textual data supported at the moment, should only be used with frames that
     * support a simple string argument.
     *
     * @param formatKey
     * @param value
     * @return
     * @throws KeyNotFoundException
     * @throws FieldDataInvalidException
     */
    protected TagField doCreateTagField(FrameAndSubId formatKey, String value) throws KeyNotFoundException, FieldDataInvalidException {
        AbstractID3v2Frame frame = createFrame(formatKey.getFrameId());
        if (frame.getBody() instanceof FrameBodyUFID) {
            ((FrameBodyUFID) frame.getBody()).setOwner(formatKey.getSubId());
            try {
                ((FrameBodyUFID) frame.getBody()).setUniqueIdentifier(value.getBytes("ISO-8859-1"));
            } catch (UnsupportedEncodingException uee) {
                throw new RuntimeException("When encoding UFID charset ISO-8859-1 was deemed unsupported");
            }
        } else if (frame.getBody() instanceof FrameBodyTXXX) {
            ((FrameBodyTXXX) frame.getBody()).setDescription(formatKey.getSubId());
            ((FrameBodyTXXX) frame.getBody()).setText(value);
        } else if (frame.getBody() instanceof FrameBodyWXXX) {
            ((FrameBodyWXXX) frame.getBody()).setDescription(formatKey.getSubId());
            ((FrameBodyWXXX) frame.getBody()).setUrlLink(value);
        } else if (frame.getBody() instanceof AbstractFrameBodyTextInfo) {
            ((AbstractFrameBodyTextInfo) frame.getBody()).setText(value);
        } else if ((frame.getBody() instanceof FrameBodyAPIC) || (frame.getBody() instanceof FrameBodyPIC)) {
            throw new UnsupportedOperationException("Please use createArtwork() instead for creating artwork");
        } else {
            throw new FieldDataInvalidException("Field with key of:" + formatKey.getFrameId() + ":does not accept cannot parse data:" + value);
        }
        return frame;
    }

    /**
     *
     * @param formatKey
     * @return
     * @throws KeyNotFoundException
     */
    protected String doGetFirst(FrameAndSubId formatKey) throws KeyNotFoundException {
        if (formatKey.getSubId() == null) {
            return getFirst(formatKey.getFrameId());
        } else {
            List<TagField> list = get(formatKey.getFrameId());
            ListIterator<TagField> li = list.listIterator();
            while (li.hasNext()) {
                AbstractTagFrameBody next = ((AbstractID3v2Frame) li.next()).getBody();
                if (next instanceof FrameBodyTXXX) {
                    if (((FrameBodyTXXX) next).getDescription().equals(formatKey.getSubId())) {
                        return ((FrameBodyTXXX) next).getText();
                    }
                } else if (next instanceof FrameBodyWXXX) {
                    if (((FrameBodyWXXX) next).getDescription().equals(formatKey.getSubId())) {
                        return ((FrameBodyWXXX) next).getUrlLink();
                    }
                } else if (next instanceof FrameBodyUFID) {
                    if (!((FrameBodyUFID) next).getUniqueIdentifier().equals(formatKey.getSubId())) {
                        return new String(((FrameBodyUFID) next).getUniqueIdentifier());
                    }
                } else {
                    throw new RuntimeException("Need to implement get(TagFieldKey genericKey) for:" + next.getClass());
                }
            }
            return "";
        }
    }

    /**
     * Create a link to artwork, this is not recommended because the link may be broken if the mp3 or image
     * file is moved
     *
     * @param url specifies the link, it could be a local file or could be a full url
     * @return
     */
    public TagField createLinkedArtworkField(String url) {
        AbstractID3v2Frame frame = createFrame(getFrameAndSubIdFromGenericKey(TagFieldKey.COVER_ART).getFrameId());
        if (frame.getBody() instanceof FrameBodyAPIC) {
            FrameBodyAPIC body = (FrameBodyAPIC) frame.getBody();
            body.setObjectValue(DataTypes.OBJ_PICTURE_DATA, Utils.getDefaultBytes(url, TextEncoding.CHARSET_ISO_8859_1));
            body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, PictureTypes.DEFAULT_ID);
            body.setObjectValue(DataTypes.OBJ_MIME_TYPE, FrameBodyAPIC.IMAGE_IS_URL);
            body.setObjectValue(DataTypes.OBJ_DESCRIPTION, "");
        } else if (frame.getBody() instanceof FrameBodyPIC) {
            FrameBodyPIC body = (FrameBodyPIC) frame.getBody();
            body.setObjectValue(DataTypes.OBJ_PICTURE_DATA, Utils.getDefaultBytes(url, TextEncoding.CHARSET_ISO_8859_1));
            body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, PictureTypes.DEFAULT_ID);
            body.setObjectValue(DataTypes.OBJ_IMAGE_FORMAT, FrameBodyAPIC.IMAGE_IS_URL);
            body.setObjectValue(DataTypes.OBJ_DESCRIPTION, "");
        }
        return frame;
    }

    /**
     * Create Artwork
     *
     * @see PictureTypes
     *
     * @param data
     * @param mimeType of the image
     */
    public TagField createArtworkField(byte[] data, String mimeType) {
        AbstractID3v2Frame frame = createFrame(getFrameAndSubIdFromGenericKey(TagFieldKey.COVER_ART).getFrameId());
        if (frame.getBody() instanceof FrameBodyAPIC) {
            FrameBodyAPIC body = (FrameBodyAPIC) frame.getBody();
            body.setObjectValue(DataTypes.OBJ_PICTURE_DATA, data);
            body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, PictureTypes.DEFAULT_ID);
            body.setObjectValue(DataTypes.OBJ_MIME_TYPE, mimeType);
            body.setObjectValue(DataTypes.OBJ_DESCRIPTION, "");
        } else if (frame.getBody() instanceof FrameBodyPIC) {
            FrameBodyPIC body = (FrameBodyPIC) frame.getBody();
            body.setObjectValue(DataTypes.OBJ_PICTURE_DATA, data);
            body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, PictureTypes.DEFAULT_ID);
            body.setObjectValue(DataTypes.OBJ_IMAGE_FORMAT, ImageFormats.getFormatForMimeType(mimeType));
            body.setObjectValue(DataTypes.OBJ_DESCRIPTION, "");
        }
        return frame;
    }

    /**
     * Delete fields with this generic key
     *
     * @param genericKey
     */
    public void deleteTagField(TagFieldKey genericKey) throws KeyNotFoundException {
        if (genericKey == null) {
            throw new KeyNotFoundException();
        }
        FrameAndSubId formatKey = getFrameAndSubIdFromGenericKey(genericKey);
        doDeleteTagField(formatKey);
    }

    /**
     * Internal delete method
     *
     * @param formatKey
     * @throws KeyNotFoundException
     */
    protected void doDeleteTagField(FrameAndSubId formatKey) throws KeyNotFoundException {
        if (formatKey.getSubId() == null) {
            removeFrame(formatKey.getFrameId());
        } else {
            List<TagField> list = get(formatKey.getFrameId());
            ListIterator<TagField> li = list.listIterator();
            while (li.hasNext()) {
                AbstractTagFrameBody next = ((AbstractID3v2Frame) li.next()).getBody();
                if (next instanceof FrameBodyTXXX) {
                    if (((FrameBodyTXXX) next).getDescription().equals(formatKey.getSubId())) {
                        li.remove();
                    }
                } else if (next instanceof FrameBodyWXXX) {
                    if (((FrameBodyWXXX) next).getDescription().equals(formatKey.getSubId())) {
                        li.remove();
                    }
                } else if (next instanceof FrameBodyUFID) {
                    if (((FrameBodyUFID) next).getUniqueIdentifier().equals(formatKey.getSubId())) {
                        li.remove();
                    }
                } else {
                    throw new RuntimeException("Need to implement get(TagFieldKey genericKey) for:" + next.getClass());
                }
            }
        }
    }

    protected abstract FrameAndSubId getFrameAndSubIdFromGenericKey(TagFieldKey genericKey);

    /**
     * Get field(s) for this key
     *
     * @param genericKey
     * @return
     * @throws KeyNotFoundException
     */
    public List<TagField> get(TagFieldKey genericKey) throws KeyNotFoundException {
        if (genericKey == null) {
            throw new KeyNotFoundException();
        }
        FrameAndSubId formatKey = getFrameAndSubIdFromGenericKey(genericKey);
        List<TagField> list = get(formatKey.getFrameId());
        List<TagField> filteredList = new ArrayList<TagField>();
        String subFieldId = formatKey.getSubId();
        String frameid = formatKey.getFrameId();
        if (subFieldId != null) {
            for (TagField tagfield : list) {
                AbstractTagFrameBody next = ((AbstractID3v2Frame) tagfield).getBody();
                if (next instanceof FrameBodyTXXX) {
                    if (((FrameBodyTXXX) next).getDescription().equals(formatKey.getSubId())) {
                        filteredList.add(tagfield);
                    }
                } else if (next instanceof FrameBodyWXXX) {
                    if (((FrameBodyWXXX) next).getDescription().equals(formatKey.getSubId())) {
                        filteredList.add(tagfield);
                    }
                } else if (next instanceof FrameBodyUFID) {
                    if (((FrameBodyUFID) next).getUniqueIdentifier().equals(formatKey.getSubId())) {
                        filteredList.add(tagfield);
                    }
                } else {
                    throw new RuntimeException("Need to implement get(TagFieldKey genericKey) for:" + next.getClass());
                }
            }
            return filteredList;
        } else {
            return list;
        }
    }

    /**
     * This class had to be created to minimize the duplicate code in concrete subclasses
     * of this class. It is required in some cases when using the Fieldkey enums because enums
     * cant be subclassed. We want to use enums instead of regular classes because they are
     * much easier for endusers to  to use.
     */
    class FrameAndSubId {

        private String frameId;

        private String subId;

        public FrameAndSubId(String frameId, String subId) {
            this.frameId = frameId;
            this.subId = subId;
        }

        public String getFrameId() {
            return frameId;
        }

        public String getSubId() {
            return subId;
        }
    }
}

package mobi.ilabs.restroom.domainmodel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Vector;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import mobi.ilabs.ByteArrayBackedReadableByteChannel;
import mobi.ilabs.InvariantCheckable;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.resource.ReadableRepresentation;
import org.restlet.resource.Representation;

/**
 * Persistently stored objects with value.
 *
 */
@Entity
@Table(name = "STORED_OBJECTS")
public class ObjectValue implements InvariantCheckable {

    /**
     * Assume incoming is 20K just for starters.
     */
    private static final int DEFAULT_SIZE_FOR_UNKNOWN_INPUT_SIZE = 20 * 1024;

    /**
     * Limit undeclared input to 20 Megabytes.
     */
    private static final int MAX_CONTENT_BYTES = 20 * 1024 * 1024;

    /**
     * Check for invariant violations in the current
     * object.
     * @return a string describing any invariant violations.
     */
    public final String getInvariantViolations() {
        if (getContent() == null) {
            return "Null content";
        }
        return null;
    }

    /**
     * An object identifier. Within a storage server this
     * identifier uniquely identifies a stored object.
     */
    @Id
    @GeneratedValue
    @Column(name = "OBID")
    private Long id;

    /**
     * The content of the object, stored as a "large object" (LOB).
     */
    @Lob
    @Column(name = "CONTENT", columnDefinition = "LONGBLOB")
    private byte[] content;

    /**
     * The charset (MIME) used to encode the content.
     */
    @Column(name = "CHARSET")
    private String charSetString;

    /**
     * The mediatype (MIME) used to encode the content.
     */
    @Column(name = "MEDIATYPE")
    private String mediaType;

    /**
     * The filepath the object is made accessible as.
     */
    @Column(name = "filepath")
    private String filepath;

    /**
     * The size of the object.
     */
    @Column(name = "SIZE")
    private Long size;

    /**
     * A link to the creator of the object, using
     * the creator_id numerical ID for users.
     */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "creator_id")
    private User creator;

    /**
     * Accessor for object id.
     * @return a Long object identifying the object.
     */
    public final Long getId() {
        return id;
    }

    /**
     * True iff no ID is set for this object.
     * @return true iff ID is set for this object.
     */
    public final boolean idIsNotSet() {
        return getId() == null;
    }

    /**
     * The object identifier as a string.
     * @return a string identifying the object.
     */
    public final String getIdString() {
        assert (getId() != null);
        return encodeObidString(getId());
    }

    /**
     * Base of hexadecimal numbers.
     */
    private static final int HEX_BASE = 16;

    /**
     * A string encoding the obid as a HEX (radix 16) number
     * can be translated to a long number using this method.
     *
     * @param obidString the object ID of an object represented
     *        as a hexadecimal string.
     * @return a long representing the parsed obidString
     */
    public static long decodeObidString(final String obidString) {
        return Long.parseLong(obidString, HEX_BASE);
    }

    /**
     * Encoding an obid into a hex nmber.
     * @param obid as long.
     * @return obid as hex string.
     */
    public static String encodeObidString(final long obid) {
        return Long.toHexString(obid);
    }

    /**
     * Setting the object ID using a string with a hex number.
     * @param argId the obid as hex-encoded number.
     */
    public final void setIdString(final String argId) {
        setId(decodeObidString(argId));
    }

    /**
     * Setting the object id fro a long integer.
     * @param argId the obid
     */
    public final void setId(final Long argId) {
        this.id = argId;
    }

    /**
     * Get the character string.
     * @return a charset string.
     */
    public final String getCharacterSetString() {
        return charSetString;
    }

    /**
     * Return the character set used to encode the object.
     * @return the charset
     */
    public final CharacterSet getCharacterSet() {
        return CharacterSet.valueOf(getCharacterSetString());
    }

    /**
     * Setting the character set string.
     * @param s charset name
     */
    public final void setCharacterSetString(final String s) {
        charSetString = s;
    }

    /**
     * Setting the content from a byte vector.
     * @param s the content.
     */
    public final void setContent(final byte[] s) {
        content = s;
        assert (content != null);
    }

    /**
     * Getting the media type as a MIME mediatype string.
     * @return the mediatype.
     */
    public final String getMediaTypeString() {
        return mediaType;
    }

    /**
     * Getting the mediatype as a Mediatype object.
     * @return the mediatype object.
     */
    public final MediaType getMediaType() {
        return MediaType.valueOf(mediaType);
    }

    /**
     * Return the content as an entity that can be delivered in a result.
     *
     * @return the content of the object as a byte array.
     */
    public final byte[] getContent() {
        return content;
    }

    /**
     * Public creator for empty ObjectValue objects.
     *
     */
    public ObjectValue() {
    }

    /**
     * Get a ReadableByteChannel representing the content of the current object.
     * @return the content
     */
    private ReadableByteChannel getReadableByteChannel() {
        byte[] c = getContent();
        long l = 0;
        if (c != null) {
            l = c.length;
        }
        final long s = getSize();
        if (s != l) {
            final String msg = "Declared size (" + s + ") " + "is different than actual size (" + l + ") " + "of stored object with id '" + getIdString() + "'";
            throw new IllegalStateException(msg);
        }
        if (c != null) {
            return new ByteArrayBackedReadableByteChannel(c);
        } else {
            return ByteArrayBackedReadableByteChannel.EMPTY_CHANNEL;
        }
    }

    /**
     * Get a representation of the current object as a readable byte channel.
     * @return A readable representation.
     */
    public final Representation getRepresentation() {
        return new ReadableRepresentation(getReadableByteChannel(), getMediaType());
    }

    /**
     * Read content with know or unknown content.
     * XXX This is -not- a good place to put this method.  It should be somewhere else along
     *     with the constants above.
     * XXX This method must be rewritten, it�s ugly & bloated.
     * @param rb
     * @param expectedSize
     * @return
     * @throws IOException
     */
    private static final byte[] readContent(final ReadableByteChannel rb, final int expectedSize) throws IOException {
        assert (rb.isOpen());
        if (expectedSize >= 0) {
            java.nio.ByteBuffer bb = ByteBuffer.allocate((int) expectedSize);
            long bytesRead = rb.read(bb);
            while (rb.isOpen() && bytesRead >= 0 && bytesRead < expectedSize) {
                long chunkLength = rb.read(bb);
                bytesRead += chunkLength;
            }
            return bb.array();
        } else {
            final List<ByteBuffer> buffers = new Vector<ByteBuffer>();
            boolean moreToRead = true;
            int bytesRead = 0;
            final int chunkBufferSize = DEFAULT_SIZE_FOR_UNKNOWN_INPUT_SIZE;
            while (moreToRead) {
                java.nio.ByteBuffer bb = ByteBuffer.allocate(chunkBufferSize);
                buffers.add(bb);
                int chunkLength = rb.read(bb);
                if (chunkLength < 0) {
                    moreToRead = false;
                } else {
                    bytesRead += chunkLength;
                    int bytesInCurrentBuffer = chunkLength;
                    while (rb.isOpen() && chunkLength > 0 && (bytesInCurrentBuffer < chunkBufferSize) && (bytesRead < MAX_CONTENT_BYTES)) {
                        chunkLength = rb.read(bb);
                        bytesRead += chunkLength;
                        bytesInCurrentBuffer += chunkLength;
                    }
                    moreToRead = (chunkLength != 0);
                }
            }
            bytesRead += 1;
            final byte[] result = new byte[bytesRead];
            int start = 0;
            for (final ByteBuffer buf : buffers) {
                buf.flip();
                int len = buf.remaining();
                buf.get(result, start, len);
                start += len;
            }
            return result;
        }
    }

    /**
     * Set the content of the object from a representation.
     * @param rep the representation to read content from.
     */
    public final void setContentFrom(final Representation rep) {
        try {
            final ReadableByteChannel rb = rep.getChannel();
            final long expectedSize = rep.getSize();
            setContent(readContent(rb, (int) expectedSize));
            if (expectedSize < 0) {
                setSize(new Long(getContent().length));
            } else {
                setSize(expectedSize);
            }
            assert (rep != null);
            assert (rep.getMediaType() != null);
            mediaType = rep.getMediaType().getName();
            final CharacterSet cs = rep.getCharacterSet();
            if (cs != null) {
                charSetString = rep.getCharacterSet().getName();
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        if (content == null) {
            throw new RuntimeException("NULL obval object!!!");
        }
    }

    /**
     * Get the filepath used to access the object.
     * @return the filepath
     */
    public final String getFilepath() {
        return filepath;
    }

    /**
     * Set the filepath used to access the object.
     * @param url a filepath
     */
    public final void setFilepath(final String url) {
        this.filepath = url;
    }

    /**
     * Get object size in number of bytes.
     * @return the number of bytes.
     */
    public final Long getSize() {
        return size;
    }

    /**
     * Set the size of the object in number of bytes.
     * @param sz the size
     */
    public final void setSize(final Long sz) {
        this.size = sz;
    }

    /**
     * Return a reference to the creator of this object.
     * @return the creator
     */
    public final User getCreator() {
        return creator;
    }

    /**
     * Set the creator of this object.
     * @param argCreator the user that created this object.
     */
    public final void setCreator(final User argCreator) {
        this.creator = argCreator;
    }
}

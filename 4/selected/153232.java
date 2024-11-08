package com.noelios.restlet.application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.restlet.data.Encoding;
import org.restlet.resource.Representation;
import org.restlet.util.ByteUtils;
import org.restlet.util.WrapperList;
import org.restlet.util.WrapperRepresentation;

/**
 * Content that encodes a wrapped content. Allows to apply only one encoding.
 * 
 * @author Jerome Louvel (contact@noelios.com)
 */
public class EncodeRepresentation extends WrapperRepresentation {

    /** Indicates if the encoding can happen. */
    private boolean canEncode;

    /** The encoding to apply. */
    private Encoding encoding;

    /** The applied encodings. */
    private List<Encoding> encodings;

    /**
	 * Constructor.
	 * 
	 * @param encoding
	 *            Encoder algorithm.
	 * @param wrappedRepresentation
	 *            The wrapped representation.
	 */
    public EncodeRepresentation(Encoding encoding, Representation wrappedRepresentation) {
        super(wrappedRepresentation);
        this.canEncode = getSupportedEncodings().contains(encoding);
        this.encodings = null;
        this.encoding = encoding;
    }

    /**
	 * Indicates if the encoding can happen.
	 * 
	 * @return True if the encoding can happen.
	 */
    public boolean canEncode() {
        return this.canEncode;
    }

    /**
	 * Returns the size in bytes of the encoded representation if known,
	 * UNKNOWN_SIZE (-1) otherwise.
	 * 
	 * @return The size in bytes if known, UNKNOWN_SIZE (-1) otherwise.
	 */
    @Override
    public long getSize() {
        long result = UNKNOWN_SIZE;
        if (canEncode()) {
            if (this.encoding.equals(Encoding.IDENTITY)) {
                result = getWrappedRepresentation().getSize();
            }
        } else {
            result = getWrappedRepresentation().getSize();
        }
        return result;
    }

    /**
	 * Returns the applied encodings.
	 * 
	 * @return The applied encodings.
	 */
    @Override
    public List<Encoding> getEncodings() {
        if (this.encodings == null) {
            encodings = new WrapperList<Encoding>() {

                @Override
                public void add(int index, Encoding element) {
                    if (element == null) {
                        throw new IllegalArgumentException("Cannot add a null encoding.");
                    } else {
                        super.add(index, element);
                    }
                }

                @Override
                public boolean add(Encoding element) {
                    if (element == null) {
                        throw new IllegalArgumentException("Cannot add a null encoding.");
                    } else {
                        return super.add(element);
                    }
                }

                @Override
                public boolean addAll(Collection<? extends Encoding> elements) {
                    boolean addNull = (elements == null);
                    if (!addNull) {
                        for (Iterator<? extends Encoding> iterator = elements.iterator(); !addNull && iterator.hasNext(); ) {
                            addNull = (iterator.next() == null);
                        }
                    }
                    if (addNull) {
                        throw new IllegalArgumentException("Cannot add a null encoding.");
                    } else {
                        return super.addAll(elements);
                    }
                }

                @Override
                public boolean addAll(int index, Collection<? extends Encoding> elements) {
                    boolean addNull = (elements == null);
                    if (!addNull) {
                        for (Iterator<? extends Encoding> iterator = elements.iterator(); !addNull && iterator.hasNext(); ) {
                            addNull = (iterator.next() == null);
                        }
                    }
                    if (addNull) {
                        throw new IllegalArgumentException("Cannot add a null encoding.");
                    } else {
                        return super.addAll(index, elements);
                    }
                }
            };
            encodings.addAll(getWrappedRepresentation().getEncodings());
            if (canEncode()) {
                encodings.add(this.encoding);
            }
        }
        return this.encodings;
    }

    /**
	 * Returns a readable byte channel. If it is supported by a file a read-only
	 * instance of FileChannel is returned.
	 * 
	 * @return A readable byte channel.
	 */
    @Override
    public ReadableByteChannel getChannel() throws IOException {
        if (canEncode()) {
            return ByteUtils.getChannel(getStream());
        } else {
            return getWrappedRepresentation().getChannel();
        }
    }

    /**
	 * Returns a stream with the representation's content.
	 * 
	 * @return A stream with the representation's content.
	 */
    @Override
    public InputStream getStream() throws IOException {
        if (canEncode()) {
            return ByteUtils.getStream(this);
        } else {
            return getWrappedRepresentation().getStream();
        }
    }

    /**
	 * Writes the representation to a byte channel.
	 * 
	 * @param writableChannel
	 *            A writable byte channel.
	 */
    @Override
    public void write(WritableByteChannel writableChannel) throws IOException {
        if (canEncode()) {
            write(ByteUtils.getStream(writableChannel));
        } else {
            getWrappedRepresentation().write(writableChannel);
        }
    }

    /**
	 * Writes the representation to a byte stream.
	 * 
	 * @param outputStream
	 *            The output stream.
	 */
    @Override
    public void write(OutputStream outputStream) throws IOException {
        if (canEncode()) {
            DeflaterOutputStream encoderOutputStream = null;
            if (this.encoding.equals(Encoding.GZIP)) {
                encoderOutputStream = new GZIPOutputStream(outputStream);
            } else if (this.encoding.equals(Encoding.DEFLATE)) {
                encoderOutputStream = new DeflaterOutputStream(outputStream);
            } else if (this.encoding.equals(Encoding.ZIP)) {
                ZipOutputStream stream = new ZipOutputStream(outputStream);
                stream.putNextEntry(new ZipEntry("entry"));
                encoderOutputStream = stream;
            } else if (this.encoding.equals(Encoding.IDENTITY)) {
            }
            if (encoderOutputStream != null) {
                getWrappedRepresentation().write(encoderOutputStream);
                encoderOutputStream.finish();
            } else {
                getWrappedRepresentation().write(outputStream);
            }
        } else {
            getWrappedRepresentation().write(outputStream);
        }
    }

    /**
	 * Converts the representation to a string value. Be careful when using this
	 * method as the conversion of large content to a string fully stored in
	 * memory can result in OutOfMemoryErrors being thrown.
	 * 
	 * @return The representation as a string value.
	 */
    @Override
    public String getText() throws IOException {
        String result = null;
        if (canEncode()) {
            result = ByteUtils.toString(getStream(), getCharacterSet());
        } else {
            result = getWrappedRepresentation().getText();
        }
        return result;
    }

    /**
	 * Returns the list of supported encodings.
	 * 
	 * @return The list of supported encodings.
	 */
    public static List<Encoding> getSupportedEncodings() {
        return Arrays.<Encoding>asList(Encoding.GZIP, Encoding.DEFLATE, Encoding.ZIP, Encoding.IDENTITY);
    }
}

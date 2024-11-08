package org.apache.axis.attachments;

import org.apache.axis.Part;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.axis.utils.Messages;
import org.apache.commons.logging.Log;
import javax.activation.DataHandler;

/**
 * This simulates the multipart stream.
 *
 * @author Rick Rineholt
 */
public class MultiPartDimeInputStream extends MultiPartInputStream {

    protected static Log log = LogFactory.getLog(MultiPartDimeInputStream.class.getName());

    protected java.util.HashMap parts = new java.util.HashMap();

    protected java.util.LinkedList orderedParts = new java.util.LinkedList();

    protected int rootPartLength = 0;

    protected boolean closed = false;

    protected boolean eos = false;

    protected DimeDelimitedInputStream dimeDelimitedStream = null;

    protected java.io.InputStream soapStream = null;

    protected byte[] boundary = null;

    protected java.io.ByteArrayInputStream cachedSOAPEnvelope = null;

    protected String contentId = null;

    /**
     * Create a new Multipart stream from an input stream.
     *
     * @param is the true input stream that is read from
     * @throws java.io.IOException if it was not possible to build the Multipart
     */
    public MultiPartDimeInputStream(java.io.InputStream is) throws java.io.IOException {
        super(null);
        soapStream = dimeDelimitedStream = new DimeDelimitedInputStream(is);
        contentId = dimeDelimitedStream.getContentId();
    }

    public Part getAttachmentByReference(final String[] id) throws org.apache.axis.AxisFault {
        Part ret = null;
        try {
            for (int i = id.length - 1; ret == null && i > -1; --i) {
                ret = (AttachmentPart) parts.get(id[i]);
            }
            if (null == ret) {
                ret = readTillFound(id);
            }
            log.debug(Messages.getMessage("return02", "getAttachmentByReference(\"" + id + "\"", (ret == null ? "null" : ret.toString())));
        } catch (java.io.IOException e) {
            throw new org.apache.axis.AxisFault(e.getClass().getName() + e.getMessage());
        }
        return ret;
    }

    protected void addPart(String contentId, String locationId, AttachmentPart ap) {
        if (contentId != null && contentId.trim().length() != 0) parts.put(contentId, ap);
        orderedParts.add(ap);
    }

    protected static final String[] READ_ALL = { " * \0 ".intern() };

    protected void readAll() throws org.apache.axis.AxisFault {
        try {
            readTillFound(READ_ALL);
        } catch (Exception e) {
            throw org.apache.axis.AxisFault.makeFault(e);
        }
    }

    public java.util.Collection getAttachments() throws org.apache.axis.AxisFault {
        readAll();
        return new java.util.LinkedList(orderedParts);
    }

    /**
     * This will read streams in till the one that is needed is found.
     *
     * @param id is the stream being sought
     * @return a <code>Part</code> matching the ids
     */
    protected Part readTillFound(final String[] id) throws java.io.IOException {
        if (dimeDelimitedStream == null) {
            return null;
        }
        Part ret = null;
        try {
            if (soapStream != null) {
                if (!eos) {
                    java.io.ByteArrayOutputStream soapdata = new java.io.ByteArrayOutputStream(1024 * 8);
                    byte[] buf = new byte[1024 * 16];
                    int byteread = 0;
                    do {
                        byteread = soapStream.read(buf);
                        if (byteread > 0) {
                            soapdata.write(buf, 0, byteread);
                        }
                    } while (byteread > -1);
                    soapdata.close();
                    soapStream.close();
                    soapStream = new java.io.ByteArrayInputStream(soapdata.toByteArray());
                }
                dimeDelimitedStream = dimeDelimitedStream.getNextStream();
            }
            if (null != dimeDelimitedStream) {
                do {
                    String contentId = dimeDelimitedStream.getContentId();
                    String type = dimeDelimitedStream.getType();
                    if (type != null && !dimeDelimitedStream.getDimeTypeNameFormat().equals(DimeTypeNameFormat.MIME)) {
                        type = "application/uri; uri=\"" + type + "\"";
                    }
                    ManagedMemoryDataSource source = new ManagedMemoryDataSource(dimeDelimitedStream, ManagedMemoryDataSource.MAX_MEMORY_DISK_CACHED, type, true);
                    DataHandler dh = new DataHandler(source);
                    AttachmentPart ap = new AttachmentPart(dh);
                    if (contentId != null) {
                        ap.setMimeHeader(HTTPConstants.HEADER_CONTENT_ID, contentId);
                    }
                    addPart(contentId, "", ap);
                    for (int i = id.length - 1; ret == null && i > -1; --i) {
                        if (contentId != null && id[i].equals(contentId)) {
                            ret = ap;
                        }
                    }
                    dimeDelimitedStream = dimeDelimitedStream.getNextStream();
                } while (null == ret && null != dimeDelimitedStream);
            }
        } catch (Exception e) {
            throw org.apache.axis.AxisFault.makeFault(e);
        }
        return ret;
    }

    /**
     * Return the content location.
     * @return the Content-Location of the stream.
     *   Null if no content-location specified.
     */
    public String getContentLocation() {
        return null;
    }

    /**
     * Return the content id of the stream.
     *
     * @return the Content-Location of the stream.
     *   Null if no content-location specified.
     */
    public String getContentId() {
        return contentId;
    }

    public int read(byte[] b, int off, int len) throws java.io.IOException {
        if (closed) {
            throw new java.io.IOException(Messages.getMessage("streamClosed"));
        }
        if (eos) {
            return -1;
        }
        int read = soapStream.read(b, off, len);
        if (read < 0) {
            eos = true;
        }
        return read;
    }

    public int read(byte[] b) throws java.io.IOException {
        return read(b, 0, b.length);
    }

    public int read() throws java.io.IOException {
        if (closed) {
            throw new java.io.IOException(Messages.getMessage("streamClosed"));
        }
        if (eos) {
            return -1;
        }
        int ret = soapStream.read();
        if (ret < 0) {
            eos = true;
        }
        return ret;
    }

    public void close() throws java.io.IOException {
        closed = true;
        soapStream.close();
    }
}

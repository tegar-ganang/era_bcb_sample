package org.archive.io.arc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpParser;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.RecoverableIOException;

/**
 * An ARC file record.
 * Does not compass the ARCRecord metadata line, just the record content.
 * @author stack
 */
public class ARCRecord extends ArchiveRecord implements ARCConstants {

    /**
     * Http status line object.
     * 
     * May be null if record is not http.
     */
    private StatusLine httpStatus = null;

    /**
     * Http header bytes.
     * 
     * If non-null and bytes available, give out its contents before we
     * go back to the underlying stream.
     */
    private InputStream httpHeaderStream = null;

    /**
     * Http headers.
     * 
     * Only populated after reading of headers.
     */
    private Header[] httpHeaders = null;

    /**
     * Minimal http header length.
     * 
     * I've seen in arcs content length of 1 with no 
     * header.
     */
    private static final long MIN_HTTP_HEADER_LENGTH = "HTTP/1.1 200 OK\r\n".length();

    /**
     * Constructor.
     *
     * @param in Stream cue'd up to be at the start of the record this instance
     * is to represent.
     * @param metaData Meta data.
     * @throws IOException
     */
    public ARCRecord(InputStream in, ArchiveRecordHeader metaData) throws IOException {
        this(in, metaData, 0, true, false, true);
    }

    /**
     * Constructor.
     *
     * @param in Stream cue'd up to be at the start of the record this instance
     * is to represent.
     * @param metaData Meta data.
     * @param bodyOffset Offset into the body.  Usually 0.
     * @param digest True if we're to calculate digest for this record.  Not
     * digesting saves about ~15% of cpu during an ARC parse.
     * @param strict Be strict parsing (Parsing stops if ARC inproperly
     * formatted).
     * @param parseHttpHeaders True if we are to parse HTTP headers.  Costs
     * about ~20% of CPU during an ARC parse.
     * @throws IOException
     */
    public ARCRecord(InputStream in, ArchiveRecordHeader metaData, int bodyOffset, boolean digest, boolean strict, final boolean parseHttpHeaders) throws IOException {
        super(in, metaData, bodyOffset, digest, strict);
        if (parseHttpHeaders) {
            this.httpHeaderStream = readHttpHeader();
        }
    }

    /**
     * Skip over the the http header if one present.
     * 
     * Subsequent reads will get the body.
     * 
     * <p>Calling this method in the midst of reading the header
     * will make for strange results.  Otherwise, safe to call
     * at any time though before reading any of the arc record
     * content is only time that it makes sense.
     * 
     * <p>After calling this method, you can call
     * {@link #getHttpHeaders()} to get the read http header.
     * 
     * @throws IOException
     */
    public void skipHttpHeader() throws IOException {
        if (this.httpHeaderStream != null) {
            for (int available = this.httpHeaderStream.available(); this.httpHeaderStream != null && (available = this.httpHeaderStream.available()) > 0; ) {
                byte[] buffer = new byte[available];
                read(buffer, 0, available);
            }
        }
    }

    public void dumpHttpHeader() throws IOException {
        if (this.httpHeaderStream == null) {
            return;
        }
        for (int available = this.httpHeaderStream.available(); this.httpHeaderStream != null && (available = this.httpHeaderStream.available()) > 0; ) {
            byte[] buffer = new byte[available];
            int read = read(buffer, 0, available);
            System.out.write(buffer, 0, read);
        }
    }

    /**
	 * Read http header if present. Technique borrowed from HttpClient HttpParse
	 * class.
	 * 
	 * @return ByteArrayInputStream with the http header in it or null if no
	 *         http header.
	 * @throws IOException
	 */
    private InputStream readHttpHeader() throws IOException {
        if (!getHeader().getUrl().startsWith("http") || getHeader().getLength() <= MIN_HTTP_HEADER_LENGTH) {
            return null;
        }
        byte[] statusBytes = HttpParser.readRawLine(getIn());
        int eolCharCount = getEolCharsCount(statusBytes);
        if (eolCharCount <= 0) {
            throw new IOException("Failed to read http status where one " + " was expected: " + new String(statusBytes));
        }
        String statusLine = EncodingUtil.getString(statusBytes, 0, statusBytes.length - eolCharCount, ARCConstants.DEFAULT_ENCODING);
        if ((statusLine == null) || !StatusLine.startsWithHTTP(statusLine)) {
            if (statusLine.startsWith("DELETED")) {
                throw new DeletedARCRecordIOException(statusLine);
            } else {
                throw new IOException("Failed parse of http status line.");
            }
        }
        this.httpStatus = new StatusLine(statusLine);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(statusBytes.length + 4 * 1024);
        baos.write(statusBytes);
        for (byte[] lineBytes = null; true; ) {
            lineBytes = HttpParser.readRawLine(getIn());
            eolCharCount = getEolCharsCount(lineBytes);
            if (eolCharCount <= 0) {
                throw new IOException("Failed reading http headers: " + ((lineBytes != null) ? new String(lineBytes) : null));
            }
            baos.write(lineBytes);
            if ((lineBytes.length - eolCharCount) <= 0) {
                break;
            }
        }
        byte[] headerBytes = baos.toByteArray();
        this.getMetaData().setContentBegin(headerBytes.length);
        ByteArrayInputStream bais = new ByteArrayInputStream(headerBytes);
        if (!bais.markSupported()) {
            throw new IOException("ByteArrayInputStream does not support mark");
        }
        bais.mark(headerBytes.length);
        bais.read(statusBytes, 0, statusBytes.length);
        this.httpHeaders = HttpParser.parseHeaders(bais, ARCConstants.DEFAULT_ENCODING);
        this.getMetaData().setStatusCode(Integer.toString(getStatusCode()));
        bais.reset();
        return bais;
    }

    private static class DeletedARCRecordIOException extends RecoverableIOException {

        public DeletedARCRecordIOException(final String reason) {
            super(reason);
        }
    }

    /**
     * Return status code for this record.
     * 
     * This method will return -1 until the http header has been read.
     * @return Status code.
     */
    public int getStatusCode() {
        return (this.httpStatus == null) ? -1 : this.httpStatus.getStatusCode();
    }

    /**
     * @param bytes Array of bytes to examine for an EOL.
     * @return Count of end-of-line characters or zero if none.
     */
    private int getEolCharsCount(byte[] bytes) {
        int count = 0;
        if (bytes != null && bytes.length >= 1 && bytes[bytes.length - 1] == '\n') {
            count++;
            if (bytes.length >= 2 && bytes[bytes.length - 2] == '\r') {
                count++;
            }
        }
        return count;
    }

    /**
     * @return Meta data for this record.
     */
    public ARCRecordMetaData getMetaData() {
        return (ARCRecordMetaData) getHeader();
    }

    /**
     * @return http headers (Only available after header has been read).
     */
    public Header[] getHttpHeaders() {
        return this.httpHeaders;
    }

    /**
     * @return Next character in this ARCRecord's content else -1 if at end of
     * this record.
     * @throws IOException
     */
    public int read() throws IOException {
        int c = -1;
        if (this.httpHeaderStream != null && (this.httpHeaderStream.available() > 0)) {
            c = this.httpHeaderStream.read();
            if (this.httpHeaderStream.available() <= 0) {
                this.httpHeaderStream = null;
            }
            incrementPosition();
        } else {
            c = super.read();
        }
        return c;
    }

    public int read(byte[] b, int offset, int length) throws IOException {
        int read = -1;
        if (this.httpHeaderStream != null && (this.httpHeaderStream.available() > 0)) {
            read = Math.min(length, this.httpHeaderStream.available());
            if (read == 0) {
                read = -1;
            } else {
                read = this.httpHeaderStream.read(b, offset, read);
            }
            if (this.httpHeaderStream.available() <= 0) {
                this.httpHeaderStream = null;
            }
            incrementPosition(read);
        } else {
            read = super.read(b, offset, length);
        }
        return read;
    }

    /**
     * @return Offset at which the body begins (Only known after
     * header has been read) or -1 if none or if we haven't read
     * headers yet.  Usually length of HTTP headers (does not include ARC
     * metadata line length).
     */
    public int getBodyOffset() {
        return this.getMetaData().getContentBegin();
    }

    @Override
    protected String getIp4Cdx(ArchiveRecordHeader h) {
        String result = null;
        if (h instanceof ARCRecordMetaData) {
            result = ((ARCRecordMetaData) h).getIp();
        }
        return (result != null) ? result : super.getIp4Cdx(h);
    }

    @Override
    protected String getStatusCode4Cdx(ArchiveRecordHeader h) {
        String result = null;
        if (h instanceof ARCRecordMetaData) {
            result = ((ARCRecordMetaData) h).getStatusCode();
        }
        return (result != null) ? result : super.getStatusCode4Cdx(h);
    }

    @Override
    protected String getDigest4Cdx(ArchiveRecordHeader h) {
        String result = null;
        if (h instanceof ARCRecordMetaData) {
            result = ((ARCRecordMetaData) h).getDigest();
        }
        return (result != null) ? result : super.getDigest4Cdx(h);
    }
}

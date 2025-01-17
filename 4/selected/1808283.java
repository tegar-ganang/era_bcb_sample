package org.archive.io.arc;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.archive.io.GzippedInputStream;
import org.archive.io.ReplayInputStream;
import org.archive.io.WriterPoolMember;
import org.archive.util.ArchiveUtils;
import org.archive.util.DevUtils;
import org.archive.util.MimetypeUtils;

/**
 * Write ARC files.
 *
 * Assumption is that the caller is managing access to this ARCWriter ensuring
 * only one thread of control accessing this ARC file instance at any one time.
 *
 * <p>ARC files are described here:
 * <a href="http://www.archive.org/web/researcher/ArcFileFormat.php">Arc
 * File Format</a>.  This class does version 1 of the ARC file format.  It also
 * writes version 1.1 which is version 1 with data stuffed into the body of the
 * first arc record in the file, the arc file meta record itself.
 *
 * <p>An ARC file is three lines of meta data followed by an optional 'body' and
 * then a couple of '\n' and then: record, '\n', record, '\n', record, etc.
 * If we are writing compressed ARC files, then each of the ARC file records is
 * individually gzipped and concatenated together to make up a single ARC file.
 * In GZIP terms, each ARC record is a GZIP <i>member</i> of a total gzip'd
 * file.
 *
 * <p>The GZIPping of the ARC file meta data is exceptional.  It is GZIPped
 * w/ an extra GZIP header, a special Internet Archive (IA) extra header field
 * (e.g. FEXTRA is set in the GZIP header FLG field and an extra field is
 * appended to the GZIP header).  The extra field has little in it but its
 * presence denotes this GZIP as an Internet Archive gzipped ARC.  See RFC1952
 * to learn about the GZIP header structure.
 *
 * <p>This class then does its GZIPping in the following fashion.  Each GZIP
 * member is written w/ a new instance of GZIPOutputStream -- actually
 * ARCWriterGZIPOututStream so we can get access to the underlying stream.
 * The underlying stream stays open across GZIPoutputStream instantiations.
 * For the 'special' GZIPing of the ARC file meta data, we cheat by catching the
 * GZIPOutputStream output into a byte array, manipulating it adding the
 * IA GZIP header, before writing to the stream.
 *
 * <p>I tried writing a resettable GZIPOutputStream and could make it work w/
 * the SUN JDK but the IBM JDK threw NPE inside in the deflate.reset -- its zlib
 * native call doesn't seem to like the notion of resetting -- so I gave up on
 * it.
 *
 * <p>Because of such as the above and troubles with GZIPInputStream, we should
 * write our own GZIP*Streams, ones that resettable and consious of gzip
 * members.
 *
 * <p>This class will write until we hit >= maxSize.  The check is done at
 * record boundary.  Records do not span ARC files.  We will then close current
 * file and open another and then continue writing.
 *
 * <p><b>TESTING: </b>Here is how to test that produced ARC files are good
 * using the
 * <a href="http://www.archive.org/web/researcher/tool_documentation.php">alexa
 * ARC c-tools</a>:
 * <pre>
 * % av_procarc hx20040109230030-0.arc.gz | av_ziparc > \
 *     /tmp/hx20040109230030-0.dat.gz
 * % av_ripdat /tmp/hx20040109230030-0.dat.gz > /tmp/hx20040109230030-0.cdx
 * </pre>
 * Examine the produced cdx file to make sure it makes sense.  Search
 * for 'no-type 0'.  If found, then we're opening a gzip record w/o data to
 * write.  This is bad.
 *
 * <p>You can also do <code>gzip -t FILENAME</code> and it will tell you if the
 * ARC makes sense to GZIP.
 * 
 * <p>While being written, ARCs have a '.open' suffix appended.
 *
 * @author stack
 */
public class ARCWriter extends WriterPoolMember implements ARCConstants {

    private static final Logger logger = Logger.getLogger(ARCWriter.class.getName());

    /**
     * Metadata line pattern.
     */
    private static final Pattern METADATA_LINE_PATTERN = Pattern.compile("^\\S+ \\S+ \\S+ \\S+ \\S+(" + LINE_SEPARATOR + "?)$");

    /**
     * Buffer to reuse writing streams.
     */
    private final byte[] readbuffer = new byte[4 * 1024];

    private List metadata = null;

    /**
     * Constructor.
     * Takes a stream. Use with caution. There is no upperbound check on size.
     * Will just keep writing.
     * 
     * @param serialNo  used to generate unique file name sequences
     * @param out Where to write.
     * @param arc File the <code>out</code> is connected to.
     * @param cmprs Compress the content written.
     * @param metadata File meta data.  Can be null.  Is list of File and/or
     * String objects.
     * @param a14DigitDate If null, we'll write current time.
     * @throws IOException
     */
    public ARCWriter(final AtomicInteger serialNo, final PrintStream out, final File arc, final boolean cmprs, String a14DigitDate, final List metadata) throws IOException {
        super(serialNo, out, arc, cmprs, a14DigitDate);
        this.metadata = metadata;
        writeFirstRecord(a14DigitDate);
    }

    /**
     * Constructor.
     *
     * @param serialNo  used to generate unique file name sequences
     * @param dirs Where to drop the ARC files.
     * @param prefix ARC file prefix to use.  If null, we use
     * DEFAULT_ARC_FILE_PREFIX.
     * @param cmprs Compress the ARC files written.  The compression is done
     * by individually gzipping each record added to the ARC file: i.e. the
     * ARC file is a bunch of gzipped records concatenated together.
     * @param maxSize Maximum size for ARC files written.
     */
    public ARCWriter(final AtomicInteger serialNo, final List<File> dirs, final String prefix, final boolean cmprs, final int maxSize) {
        this(serialNo, dirs, prefix, "", cmprs, maxSize, null);
    }

    /**
     * Constructor.
     *
     * @param serialNo  used to generate unique file name sequences
     * @param dirs Where to drop files.
     * @param prefix File prefix to use.
     * @param cmprs Compress the records written. 
     * @param maxSize Maximum size for ARC files written.
     * @param suffix File tail to use.  If null, unused.
     * @param meta File meta data.  Can be null.  Is list of File and/or
     * String objects.
     */
    public ARCWriter(final AtomicInteger serialNo, final List<File> dirs, final String prefix, final String suffix, final boolean cmprs, final int maxSize, final List meta) {
        super(serialNo, dirs, prefix, suffix, cmprs, maxSize, ARC_FILE_EXTENSION);
        this.metadata = meta;
    }

    protected String createFile() throws IOException {
        String name = super.createFile();
        writeFirstRecord(getCreateTimestamp());
        return name;
    }

    private void writeFirstRecord(final String ts) throws IOException {
        write(generateARCFileMetaData(ts));
    }

    /**
     * Write out the ARCMetaData.
     *
     * <p>Generate ARC file meta data.  Currently we only do version 1 of the
     * ARC file formats or version 1.1 when metadata has been supplied (We
     * write it into the body of the first record in the arc file).
     *
     * <p>Version 1 metadata looks roughly like this:
     *
     * <pre>filedesc://testWriteRecord-JunitIAH20040110013326-2.arc 0.0.0.0 \\
     *  20040110013326 text/plain 77
     * 1 0 InternetArchive
     * URL IP-address Archive-date Content-type Archive-length
     * </pre>
     *
     * <p>If compress is set, then we generate a header that has been gzipped
     * in the Internet Archive manner.   Such a gzipping enables the FEXTRA
     * flag in the FLG field of the gzip header.  It then appends an extra
     * header field: '8', '0', 'L', 'X', '0', '0', '0', '0'.  The first two
     * bytes are the length of the field and the last 6 bytes the Internet
     * Archive header.  To learn about GZIP format, see RFC1952.  To learn
     * about the Internet Archive extra header field, read the source for
     * av_ziparc which can be found at
     * <code>alexa/vista/alexa-tools-1.2/src/av_ziparc.cc</code>.
     *
     * <p>We do things in this roundabout manner because the java
     * GZIPOutputStream does not give access to GZIP header fields.
     *
     * @param date Date to put into the ARC metadata.
     *
     * @return Byte array filled w/ the arc header.
	 * @throws IOException
     */
    private byte[] generateARCFileMetaData(String date) throws IOException {
        int metadataBodyLength = getMetadataLength();
        String metadataHeaderLinesTwoAndThree = getMetadataHeaderLinesTwoAndThree("1 " + ((metadataBodyLength > 0) ? "1" : "0"));
        int recordLength = metadataBodyLength + metadataHeaderLinesTwoAndThree.getBytes(DEFAULT_ENCODING).length;
        String metadataHeaderStr = ARC_MAGIC_NUMBER + getBaseFilename() + " 0.0.0.0 " + date + " text/plain " + recordLength + metadataHeaderLinesTwoAndThree;
        ByteArrayOutputStream metabaos = new ByteArrayOutputStream(recordLength);
        metabaos.write(metadataHeaderStr.getBytes(DEFAULT_ENCODING));
        if (metadataBodyLength > 0) {
            writeMetaData(metabaos);
        }
        metabaos.write(LINE_SEPARATOR);
        byte[] bytes = metabaos.toByteArray();
        if (isCompressed()) {
            byte[] gzippedMetaData = GzippedInputStream.gzip(bytes);
            if (gzippedMetaData[3] != 0) {
                throw new IOException("The GZIP FLG header is unexpectedly " + " non-zero.  Need to add smarter code that can deal " + " when already extant extra GZIP header fields.");
            }
            gzippedMetaData[3] = 4;
            gzippedMetaData[9] = 3;
            byte[] assemblyBuffer = new byte[gzippedMetaData.length + ARC_GZIP_EXTRA_FIELD.length];
            System.arraycopy(gzippedMetaData, 0, assemblyBuffer, 0, 10);
            System.arraycopy(ARC_GZIP_EXTRA_FIELD, 0, assemblyBuffer, 10, ARC_GZIP_EXTRA_FIELD.length);
            System.arraycopy(gzippedMetaData, 10, assemblyBuffer, 10 + ARC_GZIP_EXTRA_FIELD.length, gzippedMetaData.length - 10);
            bytes = assemblyBuffer;
        }
        return bytes;
    }

    public String getMetadataHeaderLinesTwoAndThree(String version) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(LINE_SEPARATOR);
        buffer.append(version);
        buffer.append(" InternetArchive");
        buffer.append(LINE_SEPARATOR);
        buffer.append("URL IP-address Archive-date Content-type Archive-length");
        buffer.append(LINE_SEPARATOR);
        return buffer.toString();
    }

    /**
     * Write all metadata to passed <code>baos</code>.
     *
     * @param baos Byte array to write to.
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    private void writeMetaData(ByteArrayOutputStream baos) throws UnsupportedEncodingException, IOException {
        if (this.metadata == null) {
            return;
        }
        for (Iterator i = this.metadata.iterator(); i.hasNext(); ) {
            Object obj = i.next();
            if (obj instanceof String) {
                baos.write(((String) obj).getBytes(DEFAULT_ENCODING));
            } else if (obj instanceof File) {
                InputStream is = null;
                try {
                    is = new BufferedInputStream(new FileInputStream((File) obj));
                    byte[] buffer = new byte[4096];
                    for (int read = -1; (read = is.read(buffer)) != -1; ) {
                        baos.write(buffer, 0, read);
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } else if (obj != null) {
                logger.severe("Unsupported metadata type: " + obj);
            }
        }
        return;
    }

    /**
     * @return Total length of metadata.
     * @throws UnsupportedEncodingException
     */
    private int getMetadataLength() throws UnsupportedEncodingException {
        int result = -1;
        if (this.metadata == null) {
            result = 0;
        } else {
            for (Iterator i = this.metadata.iterator(); i.hasNext(); ) {
                Object obj = i.next();
                if (obj instanceof String) {
                    result += ((String) obj).getBytes(DEFAULT_ENCODING).length;
                } else if (obj instanceof File) {
                    result += ((File) obj).length();
                } else {
                    logger.severe("Unsupported metadata type: " + obj);
                }
            }
        }
        return result;
    }

    public void write(String uri, String contentType, String hostIP, long fetchBeginTimeStamp, int recordLength, ByteArrayOutputStream baos) throws IOException {
        preWriteRecordTasks();
        try {
            write(getMetaLine(uri, contentType, hostIP, fetchBeginTimeStamp, recordLength).getBytes(UTF8));
            baos.writeTo(getOutputStream());
            write(LINE_SEPARATOR);
        } finally {
            postWriteRecordTasks();
        }
    }

    public void write(String uri, String contentType, String hostIP, long fetchBeginTimeStamp, int recordLength, InputStream in) throws IOException {
        preWriteRecordTasks();
        try {
            write(getMetaLine(uri, contentType, hostIP, fetchBeginTimeStamp, recordLength).getBytes(UTF8));
            readFullyFrom(in, recordLength, this.readbuffer);
            write(LINE_SEPARATOR);
        } finally {
            postWriteRecordTasks();
        }
    }

    public void write(String uri, String contentType, String hostIP, long fetchBeginTimeStamp, int recordLength, ReplayInputStream ris) throws IOException {
        preWriteRecordTasks();
        try {
            write(getMetaLine(uri, contentType, hostIP, fetchBeginTimeStamp, recordLength).getBytes(UTF8));
            try {
                ris.readFullyTo(getOutputStream());
                long remaining = ris.remaining();
                if (remaining != 0) {
                    String message = "Gap between expected and actual: " + remaining + LINE_SEPARATOR + DevUtils.extraInfo() + " writing arc " + this.getFile().getAbsolutePath();
                    DevUtils.warnHandle(new Throwable(message), message);
                    throw new IOException(message);
                }
            } finally {
                ris.close();
            }
            write(LINE_SEPARATOR);
        } finally {
            postWriteRecordTasks();
        }
    }

    /**
     * @param uri
     * @param contentType
     * @param hostIP
     * @param fetchBeginTimeStamp
     * @param recordLength
     * @return Metadata line for an ARCRecord made of passed components.
     * @exception IOException
     */
    protected String getMetaLine(String uri, String contentType, String hostIP, long fetchBeginTimeStamp, int recordLength) throws IOException {
        if (fetchBeginTimeStamp <= 0) {
            throw new IOException("Bogus fetchBeginTimestamp: " + Long.toString(fetchBeginTimeStamp));
        }
        return validateMetaLine(createMetaline(uri, hostIP, ArchiveUtils.get14DigitDate(fetchBeginTimeStamp), MimetypeUtils.truncate(contentType), Integer.toString(recordLength)));
    }

    public String createMetaline(String uri, String hostIP, String timeStamp, String mimetype, String recordLength) {
        return uri + HEADER_FIELD_SEPARATOR + hostIP + HEADER_FIELD_SEPARATOR + timeStamp + HEADER_FIELD_SEPARATOR + mimetype + HEADER_FIELD_SEPARATOR + recordLength + LINE_SEPARATOR;
    }

    /**
     * Test that the metadata line is valid before writing.
     * @param metaLineStr
     * @throws IOException
     * @return The passed in metaline.
     */
    protected String validateMetaLine(String metaLineStr) throws IOException {
        if (metaLineStr.length() > MAX_METADATA_LINE_LENGTH) {
            throw new IOException("Metadata line length is " + metaLineStr.length() + " which is > than maximum " + MAX_METADATA_LINE_LENGTH);
        }
        Matcher m = METADATA_LINE_PATTERN.matcher(metaLineStr);
        if (!m.matches()) {
            throw new IOException("Metadata line doesn't match expected" + " pattern: " + metaLineStr);
        }
        return metaLineStr;
    }
}

package com.knowgate.hipermail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CoderResult;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.knowgate.debug.DebugFile;

/**
 * Provides access to an mbox-formatted file.
 * @author Ben Fortuna adapted to hipergate by Sergio Montoro Ten
 * @version 1.0
 */
public class MboxFile {

    public static final String READ_ONLY = "r";

    public static final String READ_WRITE = "rw";

    private static final String TEMP_FILE_EXTENSION = ".tmp";

    /**
     * The prefix for all "From_" lines in an mbox file.
     */
    private static final String FROM__PREFIX = "From ";

    /**
     * A pattern representing the format of the "From_" line
     * for the first message in an mbox file.
     */
    private static final String INITIAL_FROM__PATTERN = FROM__PREFIX + ".*";

    /**
     * A pattern representing the format of all "From_" lines
     * except for the first message in an mbox file.
     */
    private static final String FROM__PATTERN = "\n" + FROM__PREFIX;

    private static final String FROM__DATE_FORMAT = "EEE MMM d HH:mm:ss yyyy";

    private static DateFormat from_DateFormat = new SimpleDateFormat(FROM__DATE_FORMAT);

    static {
        from_DateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * The default "From_" line used if a message doesn't already have one.
     */
    private static final String DEFAULT_FROM__LINE = FROM__PREFIX + "- " + from_DateFormat.format(new Date(0)) + "\n";

    private static Charset charset = Charset.forName("ISO-8859-1");

    private static CharsetDecoder decoder = charset.newDecoder();

    private static CharsetEncoder encoder = charset.newEncoder();

    static {
        encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    private static DebugFile log = new DebugFile();

    /**
     * Used primarily to provide information about
     * the mbox file.
     */
    private File file;

    private String mode;

    /**
     * Used to access the mbox file in a random manner.
     */
    private FileChannel channel;

    /**
     * Used to grant exclusive access to the Mbox file to one thread at a time.
     */
    private FileLock lock;

    /**
     * Tracks all message positions within the mbox file.
     */
    private long[] messagePositions;

    /**
     * Constructor.
     */
    public MboxFile(File file) throws FileNotFoundException, IOException {
        this(file, READ_ONLY);
    }

    /**
     * Constructor.
     * @param file
     * @param mode Either MboxFile.READ_ONLY or MboxFile.READ_WRITE
     */
    public MboxFile(File file, String mode) throws FileNotFoundException, IOException {
        this.file = file;
        this.mode = mode;
        if (mode.equals(READ_WRITE)) lock = getChannel().lock();
    }

    /**
     * Constructor.
     * @param filepath
     * @param mode Either MboxFile.READ_ONLY or MboxFile.READ_WRITE
     */
    public MboxFile(String filepath) {
        this.file = new File(filepath);
        this.mode = READ_ONLY;
    }

    /**
     * Constructor.
     * @param filepath
     * @param mode Either MboxFile.READ_ONLY or MboxFile.READ_WRITE
     */
    public MboxFile(String filepath, String mode) throws FileNotFoundException, IOException {
        this.file = new File(filepath);
        this.mode = mode;
        if (mode.equals(READ_WRITE)) lock = getChannel().lock();
    }

    /**
     * Returns a channel for reading and writing to the mbox file.
     * @return a file channel
     * @throws FileNotFoundException
     */
    private FileChannel getChannel() throws FileNotFoundException {
        if (channel == null) {
            channel = new RandomAccessFile(file, mode).getChannel();
        }
        return channel;
    }

    /**
     * Returns an initialised array of file positions
     * for all messages in the mbox file.
     * @return a long array
     * @throws IOException thrown when unable to read
     * from the specified file channel
     */
    public long[] getMessagePositions() throws IOException {
        if (messagePositions == null) {
            final long length = getChannel().size();
            log.debug("Channel size [" + String.valueOf(length) + "] bytes");
            if (0 == length) return new long[0];
            List posList = new ArrayList();
            final long frame = 32000;
            final long stepback = FROM__PATTERN.length() - 1;
            long size = (length < frame ? length : frame);
            long offset = 0;
            FileChannel chnnl = getChannel();
            ByteBuffer buffer = chnnl.map(FileChannel.MapMode.READ_ONLY, offset, size);
            CharBuffer cb = decoder.decode(buffer);
            if (Pattern.compile(INITIAL_FROM__PATTERN, Pattern.DOTALL).matcher(cb).matches()) {
                log.debug("Matched first message..");
                posList.add(new Long(0));
            }
            Pattern fromPattern = Pattern.compile(FROM__PATTERN);
            Matcher matcher;
            do {
                matcher = fromPattern.matcher(cb);
                while (matcher.find()) {
                    log.debug("Found match at [" + String.valueOf(offset + matcher.start()) + "]");
                    posList.add(new Long(offset + matcher.start() + 1));
                }
                if (size < frame) break;
                size = offset + size + frame < length ? frame : length - (offset + size + 1);
                offset += size - stepback;
                buffer = chnnl.map(FileChannel.MapMode.READ_ONLY, offset, size);
                cb = decoder.decode(buffer);
            } while (true);
            messagePositions = new long[posList.size()];
            int count = 0;
            for (Iterator i = posList.iterator(); i.hasNext(); count++) {
                messagePositions[count] = ((Long) i.next()).longValue();
            }
        }
        return messagePositions;
    }

    /**
     * <p>Get byte offset position of a given message inside the mbox file</p>
     * This method is slow when called for the first time, as it has to parse
     * the whole Mbox file for finding each message index.
     * @param index Message Index
     * @return message byte offset position inside the mbox file
     * @throws IOException
     * @throws ArrayIndexOutOfBoundsException
     */
    public long getMessagePosition(int index) throws IOException, ArrayIndexOutOfBoundsException {
        if (messagePositions == null) getMessagePositions();
        return messagePositions[index];
    }

    /**
     * Get size of a message in bytes
     * @param index Message Index
     * @throws IOException
     * @throws ArrayIndexOutOfBoundsException
     */
    public int getMessageSize(int index) throws IOException, ArrayIndexOutOfBoundsException {
        long position = getMessagePosition(index);
        long size;
        if (index < messagePositions.length - 1) size = messagePositions[index + 1] - position; else size = getChannel().size() - position;
        return (int) size;
    }

    /**
     * Returns the total number of messages in the mbox
     * file.
     * @return an int
     */
    public int getMessageCount() throws IOException {
        return getMessagePositions().length;
    }

    /**
     * Returns a CharSequence containing the data for
     * the message at the specified index.
     * @param index the index of the message to retrieve
     * @return a CharSequence
     */
    public CharSequence getMessage(final int index) throws IOException {
        long position = getMessagePosition(index);
        long size;
        if (index < messagePositions.length - 1) {
            size = messagePositions[index + 1] - position;
        } else {
            size = getChannel().size() - position;
        }
        return decoder.decode(getChannel().map(FileChannel.MapMode.READ_ONLY, position, size));
    }

    public InputStream getMessageAsStream(final long begin, final int size) throws IOException {
        log.debug("MboxFile.getMessageAsStream(" + String.valueOf(begin) + "," + String.valueOf(size) + ")");
        ByteBuffer byFrom = getChannel().map(FileChannel.MapMode.READ_ONLY, begin, 128);
        CharBuffer chFrom = decoder.decode(byFrom);
        int start = 0;
        char c = chFrom.charAt(start);
        while (c == ' ' || c == '\r' || c == '\n' || c == '\t') c = chFrom.charAt(++start);
        if (!chFrom.subSequence(start, start + FROM__PREFIX.length()).toString().equals(FROM__PREFIX)) throw new IOException("MboxFile.getMessageAsStream() starting position " + String.valueOf(start) + " \"" + chFrom.subSequence(start, start + FROM__PREFIX.length()).toString() + "\" does not match a begin message token \"" + FROM__PREFIX + "\"");
        while (chFrom.charAt(start++) != (char) 10) ;
        log.debug("  skip = " + String.valueOf(start));
        log.debug("  start = " + String.valueOf(begin + start));
        MappedByteBuffer byBuffer = getChannel().map(FileChannel.MapMode.READ_ONLY, begin + start, size);
        byte[] byArray = new byte[size];
        byBuffer.get(byArray);
        ByteArrayInputStream byStrm = new ByteArrayInputStream(byArray);
        return byStrm;
    }

    public InputStream getPartAsStream(final long begin, final long offset, final int size) throws IOException {
        log.debug("MboxFile.getPartAsStream(" + String.valueOf(begin) + "," + String.valueOf(offset) + "," + String.valueOf(size) + ")");
        ByteBuffer byFrom = getChannel().map(FileChannel.MapMode.READ_ONLY, begin, 128);
        CharBuffer chFrom = decoder.decode(byFrom);
        log.debug("from line decoded");
        int start = 0;
        char c = chFrom.charAt(start);
        while (c == ' ' || c == '\r' || c == '\n' || c == '\t') c = chFrom.charAt(++start);
        log.debug("first line is " + chFrom.subSequence(start, start + FROM__PREFIX.length()).toString());
        if (!chFrom.subSequence(start, start + FROM__PREFIX.length()).toString().equals(FROM__PREFIX)) throw new IOException("MboxFile.getPartAsStream() starting position " + String.valueOf(start) + " \"" + chFrom.subSequence(start, start + FROM__PREFIX.length()).toString() + "\" does not match a begin message token \"" + FROM__PREFIX + "\"");
        while (chFrom.charAt(start++) != (char) 10) ;
        start += offset;
        log.debug("  skip = " + String.valueOf(start));
        log.debug("  start = " + String.valueOf(start));
        MappedByteBuffer byBuffer = getChannel().map(FileChannel.MapMode.READ_ONLY, begin + start, size);
        byte[] byArray = new byte[size];
        byBuffer.get(byArray);
        ByteArrayInputStream byStrm = new ByteArrayInputStream(byArray);
        return byStrm;
    }

    /**
     * Opens an input stream to the specified message
     * data.
     * @param index the index of the message to open
     * a stream to
     * @return an input stream
     */
    public InputStream getMessageAsStream(int index) throws IOException {
        long position = getMessagePosition(index);
        int size;
        log.debug("MboxFile.getMessageAsStream(" + String.valueOf(position) + ")");
        if (index < messagePositions.length - 1) {
            size = (int) (messagePositions[index + 1] - position);
        } else {
            size = (int) (getChannel().size() - position);
        }
        ByteBuffer byFrom = getChannel().map(FileChannel.MapMode.READ_ONLY, position, 256);
        CharBuffer chFrom = decoder.decode(byFrom);
        int start = 0;
        char c = chFrom.charAt(start);
        while (c == ' ' || c == '\r' || c == '\n' || c == '\t') c = chFrom.charAt(++start);
        if (!chFrom.subSequence(start, start + FROM__PREFIX.length()).toString().equals(FROM__PREFIX)) throw new IOException("MboxFile.getMessageAsStream() starting position " + String.valueOf(start) + " \"" + chFrom.subSequence(start, start + FROM__PREFIX.length()).toString() + "\" does not match a begin message token \"" + FROM__PREFIX + "\"");
        while (chFrom.charAt(start++) != (char) 10) ;
        log.debug("  skip = " + String.valueOf(start));
        log.debug("  start = " + String.valueOf(position + start));
        MappedByteBuffer byBuffer = getChannel().map(FileChannel.MapMode.READ_ONLY, position + start, size - start);
        byte[] byArray = new byte[size - start];
        byBuffer.get(byArray);
        ByteArrayInputStream byStrm = new ByteArrayInputStream(byArray);
        return byStrm;
    }

    /**
     * Appends the specified message from another mbox file
     * @param source Source mbox file
     * @param srcpos Byte offset position of message at source mbox file
     * @param srcsize Size of source message in bytes
     * @return byte offset position where message is appended on this mbox file
     * @throws IOException
     */
    public final long appendMessage(MboxFile source, long srcpos, int srcsize) throws IOException {
        long position = channel.size();
        if (position > 0) {
            channel.write(encoder.encode(CharBuffer.wrap("\n\n")), channel.size());
        }
        channel.write(encoder.encode(CharBuffer.wrap(DEFAULT_FROM__LINE)), channel.size());
        channel.write(source.getChannel().map(FileChannel.MapMode.READ_ONLY, srcpos, srcsize));
        return position;
    }

    /**
     * Appends the specified message from another mbox file
     * @param source Source mbox file
     * @param index Index of message to be appended at the source file
     * @return byte offset position where message is appended on this mbox file
     * @throws IOException
     */
    public final long appendMessage(MboxFile source, int index) throws IOException {
        long srcpos = source.getMessagePosition(index);
        int srcsize;
        if (index < source.messagePositions.length - 1) {
            srcsize = (int) (source.messagePositions[index + 1] - srcpos);
        } else {
            srcsize = (int) (source.getChannel().size() - srcpos);
        }
        return appendMessage(source, srcpos, srcsize);
    }

    /**
     * Appends the specified message (represented by a CharSequence) to the
     * mbox file.
     * @param message
     */
    public final long appendMessage(final CharSequence message) throws IOException {
        return appendMessage(message, getChannel());
    }

    /**
     * Appends the specified message (represented by a CharSequence) to the specified channel.
     * @param message
     * @param channel
     * @throws IOException
     */
    private long appendMessage(final CharSequence message, FileChannel channel) throws IOException {
        long position = channel.size();
        if (!hasFrom_Line(message)) {
            if (position > 0) {
                channel.write(encoder.encode(CharBuffer.wrap("\n\n")), channel.size());
            }
            channel.write(encoder.encode(CharBuffer.wrap(DEFAULT_FROM__LINE)), channel.size());
        }
        channel.write(encoder.encode(CharBuffer.wrap(message)), channel.size());
        return position;
    }

    /**
     * Purge the specified messages from the file.
     */
    public void purge(int[] messageNumbers) throws IOException {
        if (null == messageNumbers) return;
        if (0 == messageNumbers.length) return;
        getMessagePositions();
        if (null == messagePositions) return;
        if (0 == messagePositions.length) return;
        final int total = messagePositions.length;
        final int count = messageNumbers.length;
        int size;
        long next, append;
        boolean perform;
        ByteBuffer messageBuffer;
        byte[] byBuffer = null;
        log.debug("MboxFile.purge(" + String.valueOf(count) + " of " + String.valueOf(total) + ")");
        FileChannel channel = getChannel();
        append = 0;
        for (int index = 0; index < total; index++) {
            perform = false;
            for (int d = 0; d < count && !perform; d++) if (messageNumbers[d] == index) perform = true;
            if (index < total - 1) {
                next = messagePositions[index + 1];
                size = (int) (messagePositions[index + 1] - next);
            } else {
                next = -1l;
                size = 0;
            }
            if (perform) {
                messageBuffer = channel.map(FileChannel.MapMode.READ_WRITE, next, size);
                if (byBuffer == null) byBuffer = new byte[size]; else if (byBuffer.length < size) byBuffer = new byte[size];
                messageBuffer.get(byBuffer, 0, size);
                channel.position(append);
                channel.write(ByteBuffer.wrap(byBuffer));
                append += size;
            } else {
                append += size;
            }
        }
        log.debug("FileChannel.truncate(" + String.valueOf(append) + ")");
        channel.truncate(append);
        messagePositions = null;
    }

    /**
     * Close the mbox file and release any system resources.
     * @throws IOException
     */
    public void close() throws IOException {
        if (lock != null) {
            lock.release();
            lock = null;
        }
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }

    /**
     * Indicates whether the specified CharSequence representation of
     * a message contains a "From_" line.
     * @param message a CharSequence representing a message
     * @return true if a "From_" line is found, otherwise false
     */
    private boolean hasFrom_Line(CharSequence message) {
        return Pattern.compile(FROM__PREFIX + ".*", Pattern.DOTALL).matcher(message).matches();
    }
}

package net.fortuna.mstor.data;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fortuna.mstor.util.CacheAdapter;
import net.fortuna.mstor.util.CapabilityHints;
import net.fortuna.mstor.util.Configurator;
import net.fortuna.mstor.util.EhCacheAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides access to an mbox-formatted file. To read an mbox file using a non-standard file
 * encoding you may specify the following system property:
 *
 * <pre>
 *        -Dmstor.mbox.encoding=&lt;some_encoding&gt;
 * </pre>
 *
 * If no encoding system property is specified the default file encoding will be used.
 *
 * @author Ben Fortuna
 * 
 * <pre>
 * $Id: MboxFile.java,v 1.11 2011/02/19 07:36:02 fortuna Exp $
 *
 * Created: [6/07/2004]
 *
 * Contributors: Paul Legato - fix for purge() method,
 *  Michael G. Kaiser - add/strip of ">" characters from message content
 *  matching "From_" pattern (appendMessage()/getMessage())
 *  </pre>
 * 
 */
public class MboxFile {

    /**
     * A capability hint to indicate the preferred strategy for reading mbox files into a buffer.
     */
    public static final String KEY_BUFFER_STRATEGY = "mstor.mbox.bufferStrategy";

    /**
     * Strategy for I/O buffers.
     *
     */
    public enum BufferStrategy {

        /**
         * Default strategy used in Java nio.
         */
        DEFAULT, /**
         * Map buffers.
         */
        MAPPED, /**
         * Use direct buffers.
         */
        DIRECT
    }

    /**
     * Indicates a file should be opened for reading only.
     */
    public static final String READ_ONLY = "r";

    /**
     * Indicates a file should be opened for reading and writing.
     */
    public static final String READ_WRITE = "rw";

    private static final String TEMP_FILE_EXTENSION = ".tmp";

    /**
     * The prefix for all "From_" lines in an mbox file.
     */
    public static final String FROM__PREFIX = "From ";

    /**
     * A pattern representing the format of the "From_" line for the first message in an mbox file.
     */
    private static final Pattern VALID_MBOX_PATTERN = Pattern.compile("^" + FROM__PREFIX + ".*", Pattern.DOTALL);

    private static final Pattern FROM__LINE_PATTERN = Pattern.compile("(\\A|\\n{2}|(\\r\\n){2})^From .*$", Pattern.MULTILINE);

    private static final Pattern RELAXED_FROM__LINE_PATTERN = Pattern.compile("^(" + "From .*" + ")|(" + "\\u0010\\u0010\\u0010\\u0010\\u0010\\u0010\\u0010" + "\\u0011\\u0011\\u0011\\u0011\\u0011\\u0011\\u0053" + ")$", Pattern.MULTILINE);

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private static Charset charset = Charset.forName(Configurator.getProperty("mstor.mbox.encoding", "ISO-8859-1"));

    private Log log = LogFactory.getLog(MboxFile.class);

    private CharsetDecoder decoder = charset.newDecoder();

    private CharsetEncoder encoder = charset.newEncoder();

    {
        encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    /**
     * Used primarily to provide information about the mbox file.
     */
    private File file;

    private String mode;

    private RandomAccessFile raf;

    /**
     * Used to access the mbox file in a random manner.
     */
    private FileChannel channel;

    /**
     * Tracks all message positions within the mbox file.
     */
    private Long[] messagePositions;

    /**
     * An adapter for the cache for buffers
     */
    private CacheAdapter cacheAdapter;

    /**
     * @param file a reference to an mbox data file
     * @throws FileNotFoundException where the specified file doesn't exist
     */
    public MboxFile(final File file) throws FileNotFoundException {
        this(file, READ_ONLY);
    }

    /**
     * @param file a reference to an mbox data file
     * @param mode the mode used to open the file
     */
    public MboxFile(final File file, final String mode) {
        this.file = file;
        this.mode = mode;
    }

    /**
     * Returns a random access file providing access to the mbox file.
     *
     * @return a random access file
     * @throws FileNotFoundException
     */
    private RandomAccessFile getRaf() throws FileNotFoundException {
        if (raf == null) {
            raf = new RandomAccessFile(file, mode);
        }
        return raf;
    }

    /**
     * Returns a channel for reading and writing to the mbox file.
     *
     * @return a file channel
     * @throws FileNotFoundException
     */
    private FileChannel getChannel() throws FileNotFoundException {
        if (channel == null) {
            channel = getRaf().getChannel();
        }
        return channel;
    }

    /**
     * Reads from the mbox file using the most appropriate buffer strategy available. The buffer is
     * also flipped (for reading) prior to returning.
     * 
     * @param position
     * @param size
     * @return a ByteBuffer containing up to <em>size</em> bytes starting at the specified
     *         position in the file.
     */
    private ByteBuffer read(final long position, final int size) throws IOException {
        ByteBuffer buffer = null;
        try {
            BufferStrategy bufferStrategy = null;
            if (Configurator.getProperty(KEY_BUFFER_STRATEGY) != null) {
                bufferStrategy = BufferStrategy.valueOf(Configurator.getProperty(KEY_BUFFER_STRATEGY).toUpperCase());
            }
            if (BufferStrategy.MAPPED.equals(bufferStrategy)) {
                buffer = getChannel().map(FileChannel.MapMode.READ_ONLY, position, size);
            } else {
                if (BufferStrategy.DIRECT.equals(bufferStrategy)) {
                    buffer = ByteBuffer.allocateDirect(size);
                } else if (BufferStrategy.DEFAULT.equals(bufferStrategy) || bufferStrategy == null) {
                    buffer = ByteBuffer.allocate(size);
                } else {
                    throw new IllegalArgumentException("Unrecognised buffer strategy: " + Configurator.getProperty(KEY_BUFFER_STRATEGY));
                }
                getChannel().position(position);
                getChannel().read(buffer);
                buffer.flip();
            }
        } catch (IOException ioe) {
            log.warn("Error reading bytes using nio", ioe);
            getRaf().seek(position);
            byte[] buf = new byte[size];
            getRaf().read(buf);
            buffer = ByteBuffer.wrap(buf);
        }
        return buffer;
    }

    /**
     * Returns an initialised array of file positions for all messages in the mbox file.
     *
     * @return a long array
     * @throws IOException thrown when unable to read from the specified file channel
     */
    private Long[] getMessagePositions() throws IOException {
        if (messagePositions == null) {
            List<Long> posList = new ArrayList<Long>();
            log.debug("Channel size [" + getChannel().size() + "] bytes");
            int bufferSize = (int) Math.min(getChannel().size(), DEFAULT_BUFFER_SIZE);
            CharSequence cs = null;
            ByteBuffer buffer = read(0, bufferSize);
            cs = decoder.decode(buffer);
            log.debug("Buffer [" + cs + "]");
            long offset = 0;
            for (; ; ) {
                Matcher matcher = null;
                if (CapabilityHints.isHintEnabled(CapabilityHints.KEY_MBOX_RELAXED_PARSING)) {
                    matcher = RELAXED_FROM__LINE_PATTERN.matcher(cs);
                } else {
                    matcher = FROM__LINE_PATTERN.matcher(cs);
                }
                while (matcher.find()) {
                    log.debug("Found match at [" + (offset + matcher.start()) + "]");
                    posList.add(Long.valueOf(offset + matcher.start()));
                }
                if (offset + bufferSize >= getChannel().size()) {
                    break;
                } else {
                    offset += bufferSize - FROM__PREFIX.length() - 2;
                    bufferSize = (int) Math.min(getChannel().size() - offset, DEFAULT_BUFFER_SIZE);
                    buffer = read(offset, bufferSize);
                    cs = decoder.decode(buffer);
                }
            }
            messagePositions = posList.toArray(new Long[posList.size()]);
        }
        return messagePositions;
    }

    /**
     * Returns the total number of messages in the mbox file.
     *
     * @return an int
     * @throws IOException where an error occurs reading messages
     */
    public final int getMessageCount() throws IOException {
        return getMessagePositions().length;
    }

    /**
     * Opens an input stream to the specified message data.
     *
     * @param index the index of the message to open a stream to
     * @return an input stream
     * @throws IOException where an error occurs reading the message
     */
    public final InputStream getMessageAsStream(final int index) throws IOException {
        ByteBuffer buffer = null;
        if (CapabilityHints.isHintEnabled(CapabilityHints.KEY_MBOX_CACHE_BUFFERS)) {
            buffer = retrieveBufferFromCache(index);
        }
        if (buffer == null) {
            long position = getMessagePositions()[index];
            long size;
            if (index < getMessagePositions().length - 1) {
                size = getMessagePositions()[index + 1] - getMessagePositions()[index];
            } else {
                size = getChannel().size() - getMessagePositions()[index];
            }
            buffer = read(position, (int) size);
            if (CapabilityHints.isHintEnabled(CapabilityHints.KEY_MBOX_CACHE_BUFFERS)) {
                putBufferInCache(index, buffer);
            }
        }
        return new MessageInputStream(buffer);
    }

    /**
     * Convenience method that returns a message as a byte array containing the data for the message
     * at the specified index.
     *
     * @param index the index of the message to retrieve
     * @return a byte array
     * @throws IOException where an error occurs reading the message
     */
    public final byte[] getMessage(final int index) throws IOException {
        InputStream in = getMessageAsStream(index);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int read;
        while ((read = in.read()) != -1) {
            bout.write(read);
        }
        return bout.toByteArray();
    }

    /**
     * Appends the specified message (represented by a CharSequence) to the mbox file.
     *
     * @param message
     * @throws IOException where an error occurs writing the message data
     */
    public final void appendMessage(final byte[] message) throws IOException {
        synchronized (file) {
            MessageAppender appender = new MessageAppender(getChannel());
            long newMessagePosition = appender.appendMessage(message);
            if (messagePositions != null) {
                Long[] newMessagePositions = new Long[messagePositions.length + 1];
                System.arraycopy(messagePositions, 0, newMessagePositions, 0, messagePositions.length);
                newMessagePositions[newMessagePositions.length - 1] = newMessagePosition;
                messagePositions = newMessagePositions;
            }
        }
        clearBufferCache();
    }

    /**
     * Purge the specified messages from the file.
     *
     * @param msgnums the indices of the messages to purge
     * @throws IOException where an error occurs updating the data file
     */
    public final void purge(final int[] msgnums) throws IOException {
        File newFile = new File(System.getProperty("java.io.tmpdir"), file.getName() + TEMP_FILE_EXTENSION);
        FileOutputStream newOut = new FileOutputStream(newFile);
        FileChannel newChannel = newOut.getChannel();
        MessageAppender appender = new MessageAppender(newChannel);
        synchronized (file) {
            loop: for (int i = 0; i < getMessagePositions().length; i++) {
                for (int j = 0; j < msgnums.length; j++) {
                    if (msgnums[j] == i) {
                        continue loop;
                    }
                }
                appender.appendMessage(getMessage(i));
            }
            newOut.close();
            close();
            File tempFile = new File(System.getProperty("java.io.tmpdir"), file.getName() + "." + System.currentTimeMillis());
            if (!renameTo(file, tempFile)) {
                throw new IOException("Unable to rename existing file");
            }
            tempFile.deleteOnExit();
            renameTo(newFile, file);
        }
    }

    /**
     * @param source
     * @param dest
     * @return
     */
    private boolean renameTo(final File source, final File dest) {
        if (log.isDebugEnabled()) {
            log.debug("Renaming [" + source + "] to [" + dest + "]");
        }
        if (dest.exists()) {
            dest.delete();
        }
        boolean success = source.renameTo(dest);
        if (!success) {
            try {
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(dest);
                int length;
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                while ((length = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, length);
                }
                in.close();
                out.close();
                try {
                    success = source.delete();
                } catch (Exception e) {
                    log.warn("Error cleaning up", e);
                }
            } catch (IOException ioe) {
                log.error("Failed to rename [" + source + "] to [" + dest + "]", ioe);
            }
        }
        return success;
    }

    /**
     * Close the mbox file and release any system resources.
     *
     * @throws IOException where an error occurs closing the data file
     */
    public final void close() throws IOException {
        if (messagePositions != null) {
            messagePositions = null;
        }
        if (raf != null) {
            raf.close();
            raf = null;
            channel = null;
        }
    }

    /**
     * Indicates whether the specified file appears to be a valid mbox file. Note that this method
     * does not check the entire file for validity, but rather checks the first line for indication
     * that this is an mbox file.
     * @param file an mbox file reference
     * @return true if the specified file is a valid mbox file
     */
    public static boolean isValid(final File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            return line == null || VALID_MBOX_PATTERN.matcher(line).matches();
        } catch (Exception e) {
            Log log = LogFactory.getLog(MboxFile.class);
            log.info("Not a valid mbox file [" + file + "]", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    Log log = LogFactory.getLog(MboxFile.class);
                    log.info("Error closing stream [" + file + "]", ioe);
                }
            }
        }
        return false;
    }

    private void putBufferInCache(int index, ByteBuffer buffer) {
        getCacheAdapter().putObjectIntoCache(index, buffer);
    }

    private void clearBufferCache() {
        getCacheAdapter().clearCache();
    }

    private ByteBuffer retrieveBufferFromCache(int index) {
        return (ByteBuffer) getCacheAdapter().retrieveObjectFromCache(index);
    }

    private CacheAdapter getCacheAdapter() {
        if (cacheAdapter == null) {
            if (Configurator.getProperty("mstor.cache.disabled", "false").equals("true")) {
                this.cacheAdapter = new CacheAdapter();
            } else {
                this.cacheAdapter = new EhCacheAdapter("mstor.mbox." + file.getAbsolutePath().hashCode());
            }
        }
        return cacheAdapter;
    }
}

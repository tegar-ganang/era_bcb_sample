package net.sf.jaer.eventio;

import java.util.logging.Level;
import net.sf.jaer.aemonitor.*;
import net.sf.jaer.util.EngineeringFormat;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class to stream in packets of events from binary input stream from a file recorded by AEViewer.
 *<p>
 *The file format is simple, it consists of an arbitrary number of timestamped AEs:
 *<pre>
 * int32 address
 *int32 timestamp
 *
 * int32 address
 *int32 timestamp
 *</pre>
<p>
(Prior to version 2.0 data files, the address was a 16 bit short value.)
<p>
An optional ASCII header consisting of lines starting with '#' is skipped when opening the file and may be retrieved.
No later comment lines are allowed because the rest ot the file must be pure binary data.
 * <p>
 * The first line of the header specifies the file format (for later versions). Files lacking a header
 * are assumed to be of int16 address form.
 * <p>
 * The first line of the header has a value like  "#!AER-DAT2.0". The 2.0 is the version number.
<p>
 * <strong>PropertyChangeEvents.</strong>
AEFileInputStream has PropertyChangeSupport via getSupport(). PropertyChangeListeners will be informed of
the following events
<ul>
<li> "position" - on any new packet of events, either by time chunk or fixed number of events chunk.
<li> "rewind" - on file rewind.
<li> "eof" - on end of file.
<li> "wrappedTime" - on wrap of time timestamps. This happens every int32 us, which is about 4295 seconds which is 71 minutes. Time is negative, then positive, then negative again.
<li> "init" - on initial read of a file (after creating this with a file input stream). This init event is called on the
initial packet read because listeners can't be added until the object is created.
 * <li> "markset" - on setting mark, old and new mark positions.
 * <li> "markcleared" - on clearing mark, old mark position and zero.
</ul>

 * <strong>Timestamp resets.</strong> AEFileInputStream also supports a special "zero timestamps" operation on reading a file. A  bit mask which is normally
 * zero can be set; if set to a non zero value, then if ORing the bitmask with the raw address results in a nonzero value, then
 * the timestamps are reset to zero at this point. (A timestamp offset is memorized and subtracted from subsequent timestamps read
 * from the file.) This allow synchronization using, e.g. bit 15 of the address space.
 *
 * @author tobi
 * @see net.sf.jaer.eventio.AEDataFile
 */
public class AEFileInputStream extends DataInputStream implements AEFileInputStreamInterface {

    private static final int NUMBER_LINE_SEPARATORS = 2;

    private PropertyChangeSupport support = new PropertyChangeSupport(this);

    static Logger log = Logger.getLogger("net.sf.jaer.eventio");

    private FileInputStream fileInputStream = null;

    long fileSize = 0;

    private File file = null;

    private Class addressType = Short.TYPE;

    public final int MAX_NONMONOTONIC_TIME_EXCEPTIONS_TO_PRINT = 1000;

    private int numNonMonotonicTimeExceptionsPrinted = 0;

    private long markPosition = 0;

    private int eventSizeBytes = AEFileInputStream.EVENT16_SIZE;

    protected boolean firstReadCompleted = false;

    private long absoluteStartingTimeMs = 0;

    private boolean enableTimeWrappingExceptionsChecking = true;

    private int mostRecentTimestamp, firstTimestamp, lastTimestamp;

    private int currentStartTimestamp;

    FileChannel fileChannel = null;

    /** Maximum size of raw packet in events. */
    public static final int MAX_BUFFER_SIZE_EVENTS = 1 << 20;

    /** With new 32bits addresses, use EVENT32_SIZE, but use EVENT16_SIZE for backward compatibility with 16 bit addresses */
    public static final int EVENT16_SIZE = Short.SIZE / 8 + Integer.SIZE / 8;

    /** (new style) int addr, int timestamp */
    public static final int EVENT32_SIZE = Integer.SIZE / 8 + Integer.SIZE / 8;

    /** the size of the memory mapped part of the input file.
    This window is centered over the file position except at the start and end of the file.
     */
    private int CHUNK_SIZE_EVENTS = 1 << 22;

    private int chunkSizeBytes = CHUNK_SIZE_EVENTS * EVENT16_SIZE;

    /** the packet used for reading events. */
    protected AEPacketRaw packet = new AEPacketRaw(MAX_BUFFER_SIZE_EVENTS);

    EventRaw tmpEvent = new EventRaw();

    /** The memory-mapped byte buffer pointing to the file. */
    protected MappedByteBuffer byteBuffer = null;

    /** absolute position in file in events, points to next event number, 0 based (1 means 2nd event) */
    protected long position = 0;

    protected ArrayList<String> header = new ArrayList<String>();

    private int headerOffset = 0;

    private int chunkNumber = 0;

    private int numChunks = 1;

    private final String lineSeparator = System.getProperty("line.separator");

    private int timestampResetBitmask = 0;

    private int timestampOffset = 0;

    /** Creates a new instance of AEInputStream
    @deprecated use the constructor with a File object so that users of this can more easily get file information
     */
    public AEFileInputStream(FileInputStream in) throws IOException {
        super(in);
        init(in);
    }

    /** Creates a new instance of AEInputStream
    @param f the file to open
    @throws FileNotFoundException if file doesn't exist or can't be read
     */
    public AEFileInputStream(File f) throws IOException {
        this(new FileInputStream(f));
        setFile(f);
    }

    public String toString() {
        EngineeringFormat fmt = new EngineeringFormat();
        String s = "AEInputStream with size=" + fmt.format(size()) + " events, firstTimestamp=" + getFirstTimestamp() + " lastTimestamp=" + getLastTimestamp() + " duration=" + fmt.format(getDurationUs() / 1e6f) + " s" + " event rate=" + fmt.format(size() / (getDurationUs() / 1e6f)) + " eps";
        return s;
    }

    /** fires property change "position".
     * @throws IOException if file is empty or there is some other error.
     */
    private void init(FileInputStream fileInputStream) throws IOException {
        this.fileInputStream = fileInputStream;
        readHeader(fileInputStream);
        mostRecentTimestamp = Integer.MIN_VALUE;
        currentStartTimestamp = Integer.MIN_VALUE;
        setupChunks();
        try {
            EventRaw ev = readEventForwards();
            firstTimestamp = ev.timestamp;
            position(size() - 1);
            ev = readEventForwards();
            lastTimestamp = ev.timestamp;
            position(0);
            currentStartTimestamp = firstTimestamp;
            mostRecentTimestamp = firstTimestamp;
        } catch (IOException e) {
            log.warning("couldn't read first event to set starting timestamp - maybe the file is empty?");
        } catch (NonMonotonicTimeException e2) {
            log.warning("On AEInputStream.init() caught " + e2.toString());
        }
        log.info("initialized " + this.toString());
    }

    /** Reads the next event from the stream setting no limit on how far ahead in time it is.
     * 
     * @return the event.
     * @throws IOException on reading the file.
     * @throws net.sf.jaer.eventio.AEFileInputStream.NonMonotonicTimeException if the timestamp is earlier than the one last read.
     */
    private EventRaw readEventForwards() throws IOException, NonMonotonicTimeException {
        return readEventForwards(Integer.MAX_VALUE);
    }

    /** Reads the next event forward, sets mostRecentTimestamp, returns null if the next timestamp is later than maxTimestamp.
     * @param maxTimestamp the latest timestamp that should be read.
      @throws EOFException at end of file
     * @throws NonMonotonicTimeException - the event that has wrapped will be returned on the next readEventForwards 
     * @throws WrappedTimeException  - the event that has wrapped will be returned on the next readEventForwards 
     */
    private EventRaw readEventForwards(int maxTimestamp) throws IOException, NonMonotonicTimeException {
        int ts = -1;
        int addr = 0;
        int lastTs = mostRecentTimestamp;
        try {
            if (addressType == Integer.TYPE) {
                addr = byteBuffer.getInt();
            } else {
                addr = (byteBuffer.getShort() & 0xffff);
            }
            ts = byteBuffer.getInt();
            if ((addr & timestampResetBitmask) != 0) {
                log.log(Level.INFO, "found timestamp reset event addr={0} position={1} timstamp={2}", new Object[] { addr, position, ts });
                timestampOffset = ts;
            }
            ts -= timestampOffset;
            if (ts > maxTimestamp) {
                position(position);
                ts = lastTs;
                mostRecentTimestamp = ts;
                return null;
            }
            if (isWrappedTime(ts, mostRecentTimestamp, 1)) {
                throw new WrappedTimeException(ts, mostRecentTimestamp, position);
            }
            if (enableTimeWrappingExceptionsChecking && ts < mostRecentTimestamp) {
                throw new NonMonotonicTimeException(ts, mostRecentTimestamp, position);
            }
            tmpEvent.address = addr;
            tmpEvent.timestamp = ts;
            position++;
            return tmpEvent;
        } catch (BufferUnderflowException e) {
            try {
                mapNextChunk();
                return readEventForwards(maxTimestamp);
            } catch (IOException eof) {
                byteBuffer = null;
                System.gc();
                getSupport().firePropertyChange(AEInputStream.EVENT_EOF, position(), position());
                throw new EOFException("reached end of file");
            }
        } catch (NullPointerException npe) {
            rewind();
            return readEventForwards(maxTimestamp);
        } finally {
            mostRecentTimestamp = ts;
        }
    }

    /** Reads the next event backwards and leaves the position and byte buffer pointing to event one earlier
    than the one we just read. I.e., we back up, read the event, then back up again to leave us in state to
    either read forwards the event we just read, or to repeat backing up and reading if we read backwards
      @throws EOFException at end of file
     * @throws NonMonotonicTimeException
     * @throws WrappedTimeException     
     */
    private EventRaw readEventBackwards() throws IOException, NonMonotonicTimeException {
        long newPos = position - 1;
        if (newPos < 0) {
            newPos = 0;
            throw new EOFException("reached start of file");
        }
        long newBufPos;
        newBufPos = byteBuffer.position() - eventSizeBytes;
        if (newBufPos < 0) {
            int newChunkNumber = getChunkNumber(newPos);
            if (newChunkNumber != chunkNumber) {
                mapPreviousChunk();
                newBufPos = (eventSizeBytes * newPos) % chunkSizeBytes;
                byteBuffer.position((int) newBufPos);
            }
        } else {
            byteBuffer.position((int) newBufPos);
        }
        int addr;
        if (addressType == Integer.TYPE) {
            addr = byteBuffer.getInt();
        } else {
            addr = (byteBuffer.getShort() & 0xffff);
        }
        int ts = byteBuffer.getInt() - timestampOffset;
        byteBuffer.position((int) newBufPos);
        tmpEvent.address = addr;
        tmpEvent.timestamp = ts;
        mostRecentTimestamp = ts;
        position--;
        if (enableTimeWrappingExceptionsChecking && isWrappedTime(ts, mostRecentTimestamp, -1)) {
            throw new WrappedTimeException(ts, mostRecentTimestamp, position);
        }
        if (enableTimeWrappingExceptionsChecking && ts > mostRecentTimestamp) {
            throw new NonMonotonicTimeException(ts, mostRecentTimestamp, position);
        }
        return tmpEvent;
    }

    /** Uesd to read fixed size packets either forwards or backwards. 
     * Behavior in case of non-monotonic timestamps depends on setting of tim wrapping exception checking.
     * If exception checking is enabled, then the read will terminate on the first non-monotonic timestamp.
    @param n the number of events to read
    @return a raw packet of events of a specfied number of events
    fires a property change "position" on every call, and a property change "wrappedTime" if time wraps around.
     */
    @Override
    public synchronized AEPacketRaw readPacketByNumber(int n) throws IOException {
        if (!firstReadCompleted) {
            fireInitPropertyChange();
        }
        int an = (int) Math.abs(n);
        int cap = packet.getCapacity();
        if (an > cap) {
            an = cap;
            if (n > 0) {
                n = cap;
            } else {
                n = -cap;
            }
        }
        int[] addr = packet.getAddresses();
        int[] ts = packet.getTimestamps();
        long oldPosition = position();
        EventRaw ev;
        int count = 0;
        try {
            if (n > 0) {
                for (int i = 0; i < n; i++) {
                    ev = readEventForwards();
                    count++;
                    addr[i] = ev.address;
                    ts[i] = ev.timestamp;
                    currentStartTimestamp = ts[i];
                }
            } else {
                n = -n;
                for (int i = 0; i < n; i++) {
                    ev = readEventBackwards();
                    count++;
                    addr[i] = ev.address;
                    ts[i] = ev.timestamp;
                    currentStartTimestamp = ts[i];
                }
            }
        } catch (WrappedTimeException e) {
            log.info(e.toString());
            getSupport().firePropertyChange(AEInputStream.EVENT_WRAPPED_TIME, e.getPreviousTimestamp(), e.getCurrentTimestamp());
        } catch (NonMonotonicTimeException e) {
            getSupport().firePropertyChange(AEInputStream.EVENT_NON_MONOTONIC_TIMESTAMP, e.getPreviousTimestamp(), e.getCurrentTimestamp());
        }
        packet.setNumEvents(count);
        getSupport().firePropertyChange(AEInputStream.EVENT_POSITION, oldPosition, position());
        return packet;
    }

    /** Returns an AEPacketRaw at most {@code dt} long up to the max size of the buffer or until end-of-file.
     *Events are read as long as the timestamp does not exceed {@code currentStartTimestamp+dt}. If there are no events in this period
     * then the very last event from the previous packet is returned again.
     * The currentStartTimestamp is incremented after the call by dt, unless there is an exception like EVENT_NON_MONOTONIC_TIMESTAMP, in which case the
     * currentStartTimestamp is set to the most recently read timestamp {@code mostRecentTimestamp}.
     * <p>
     * The following property changes are fired:
     * <ol>
     * <li>Fires a property change AEInputStream.EVENT_POSITION at end of reading each packet.
     * <li>Fires property change AEInputStream.EVENT_WRAPPED_TIME when time wraps from positive to negative or vice versa (when playing backwards).  
     * These events are fired on "big wraps" when the 32 bit 1-us timestamp wraps around, which occurs every 
     * 4295 seconds or 72 minutes. This event signifies that on the next packet read the absolute time should be 
     * advanced or retarded (for backwards reading) by the big wrap.
     * <li>Fires property change  AEInputStream.EVENT_NON_MONOTONIC_TIMESTAMP if a non-monotonically increasing timestamp is 
     * detected that is not a wrapped time non-monotonic event.
     * <li>Fires property change AEInputStream.EVENT_EOF on end of the file.
     * </ol>
     * <p>
     * Non-monotonic timestamps cause warning messages to be printed 
     * (up to MAX_NONMONOTONIC_TIME_EXCEPTIONS_TO_PRINT) and packet
     * reading is aborted when the non-monotonic timestamp or wrapped timestamp is 
     * encountered and the non-monotonic timestamp is NOT included in the packet. 
     * Normally this does not cause problems except that the packet
     * is shorter in duration that called for. But when synchronized playback 
     * is enabled it causes the different threads to desynchronize.
     * Therefore the data files should not contain non-monotonic timestamps 
     * when synchronized playback is desired.
     * 
     *@param dt the timestamp different in units of the timestamp (usually us)
     *@see #MAX_BUFFER_SIZE_EVENTS
     */
    @Override
    public synchronized AEPacketRaw readPacketByTime(int dt) throws IOException {
        if (!firstReadCompleted) {
            fireInitPropertyChange();
        }
        int endTimestamp = currentStartTimestamp + dt;
        boolean bigWrap = isWrappedTime(endTimestamp, currentStartTimestamp, dt);
        if (bigWrap) {
            log.info("bigwrap is true - read should wrap around");
        }
        currentStartTimestamp = endTimestamp;
        int startTimestamp = mostRecentTimestamp;
        int[] addr = packet.getAddresses();
        int[] ts = packet.getTimestamps();
        long oldPosition = position();
        EventRaw ae;
        int i = 0;
        try {
            if (dt > 0) {
                if (!bigWrap) {
                    do {
                        ae = readEventForwards(endTimestamp);
                        if (ae == null) {
                            break;
                        }
                        addr[i] = ae.address;
                        ts[i] = ae.timestamp;
                        i++;
                    } while (mostRecentTimestamp < endTimestamp && i < addr.length && mostRecentTimestamp >= startTimestamp);
                } else {
                    log.info("bigwrap started");
                    do {
                        ae = readEventForwards();
                        if (ae == null) break;
                        addr[i] = ae.address;
                        ts[i] = ae.timestamp;
                        i++;
                    } while (mostRecentTimestamp > 0 && i < addr.length);
                }
            } else {
                if (!bigWrap) {
                    do {
                        ae = readEventBackwards();
                        addr[i] = ae.address;
                        ts[i] = ae.timestamp;
                        i++;
                    } while (mostRecentTimestamp > endTimestamp && i < addr.length && mostRecentTimestamp <= startTimestamp);
                } else {
                    do {
                        ae = readEventBackwards();
                        addr[i] = ae.address;
                        ts[i] = ae.timestamp;
                        i++;
                    } while (mostRecentTimestamp < 0 && i < addr.length - 1);
                    ae = readEventBackwards();
                    addr[i] = ae.address;
                    ts[i] = ae.timestamp;
                    i++;
                }
            }
        } catch (WrappedTimeException w) {
            log.info(w.toString());
            System.out.println(w.toString());
            currentStartTimestamp = w.getCurrentTimestamp();
            mostRecentTimestamp = w.getCurrentTimestamp();
            getSupport().firePropertyChange(AEInputStream.EVENT_WRAPPED_TIME, w.getPreviousTimestamp(), w.getCurrentTimestamp());
        } catch (NonMonotonicTimeException e) {
            if (numNonMonotonicTimeExceptionsPrinted++ < MAX_NONMONOTONIC_TIME_EXCEPTIONS_TO_PRINT) {
                log.log(Level.INFO, "{0} resetting currentStartTimestamp from {1} to {2} and setting mostRecentTimestamp to same value", new Object[] { e, currentStartTimestamp, e.getCurrentTimestamp() });
                if (numNonMonotonicTimeExceptionsPrinted == MAX_NONMONOTONIC_TIME_EXCEPTIONS_TO_PRINT) {
                    log.warning("suppressing further warnings about NonMonotonicTimeException");
                }
            }
            getSupport().firePropertyChange(AEInputStream.EVENT_NON_MONOTONIC_TIMESTAMP, lastTimestamp, mostRecentTimestamp);
        } finally {
        }
        packet.setNumEvents(i);
        getSupport().firePropertyChange(AEInputStream.EVENT_POSITION, oldPosition, position());
        return packet;
    }

    /** rewind to the start, or to the marked position, if it has been set. 
    Fires a property change "position" followed by "rewind". */
    public synchronized void rewind() throws IOException {
        long oldPosition = position();
        position(markPosition);
        try {
            if (markPosition == 0) {
                mostRecentTimestamp = firstTimestamp;
            } else {
                readEventForwards();
            }
        } catch (NonMonotonicTimeException e) {
            log.log(Level.INFO, "rewind from timestamp={0} to timestamp={1}", new Object[] { e.getPreviousTimestamp(), e.getCurrentTimestamp() });
        }
        currentStartTimestamp = mostRecentTimestamp;
        getSupport().firePropertyChange(AEInputStream.EVENT_POSITION, oldPosition, position());
        getSupport().firePropertyChange(AEInputStream.EVENT_REWIND, oldPosition, position());
    }

    /** gets the size of the stream in events
    @return size in events
     */
    @Override
    public long size() {
        return (fileSize - headerOffset) / eventSizeBytes;
    }

    /** set position in events from start of file
    @param event the number of the event, starting with 0
     */
    @Override
    public synchronized void position(long event) {
        int newChunkNumber;
        try {
            if ((newChunkNumber = getChunkNumber(event)) != chunkNumber) {
                mapChunk(newChunkNumber);
            }
            byteBuffer.position((int) ((event * eventSizeBytes) % chunkSizeBytes));
            position = event;
        } catch (IOException e) {
            log.log(Level.WARNING, "caught {0}", e);
            e.printStackTrace();
        } catch (IllegalArgumentException e2) {
            log.warning("caught " + e2);
            e2.printStackTrace();
        }
    }

    /** gets the current position (in events) for reading forwards, i.e., readEventForwards will read this event number.
    @return position in events.
     */
    @Override
    public synchronized long position() {
        return this.position;
    }

    /**Returns the position as a fraction of the total number of events
    @return fractional position in total events*/
    @Override
    public synchronized float getFractionalPosition() {
        return (float) position() / size();
    }

    /** Sets fractional position in events
     * @param frac 0-1 float range, 0 at start, 1 at end
     */
    @Override
    public synchronized void setFractionalPosition(float frac) {
        position((int) (frac * size()));
        try {
            readEventForwards();
        } catch (Exception e) {
        }
    }

    /** AEFileInputStream has PropertyChangeSupport. This support fires events on certain events such as "rewind".
     */
    public PropertyChangeSupport getSupport() {
        return support;
    }

    /** mark the current position.
     * @throws IOException if there is some error in reading the data
     */
    @Override
    public synchronized void mark() throws IOException {
        long old = markPosition;
        markPosition = position();
        markPosition = (markPosition / eventSizeBytes) * eventSizeBytes;
        getSupport().firePropertyChange(AEInputStream.EVENT_MARKSET, old, markPosition);
    }

    /** clear any marked position */
    @Override
    public synchronized void unmark() {
        long old = markPosition;
        markPosition = 0;
        getSupport().firePropertyChange(AEInputStream.EVENT_MARKCLEARED, old, markPosition);
    }

    /** Returns true if mark has been set to nonzero position.
     *
     * @return true if set.
     */
    public boolean isMarkSet() {
        return markPosition != 0;
    }

    @Override
    public void close() throws IOException {
        super.close();
        fileChannel.close();
        System.gc();
        System.runFinalization();
    }

    /** returns the first timestamp in the stream
    @return the timestamp
     */
    public int getFirstTimestamp() {
        return firstTimestamp;
    }

    /** @return last timestamp in file */
    public int getLastTimestamp() {
        return lastTimestamp;
    }

    /** @return the duration of the file in us. <p>
     * Assumes data file is timestamped in us. This method fails to provide a sensible value if the timestamp wwaps.
     */
    public int getDurationUs() {
        return lastTimestamp - firstTimestamp;
    }

    /** @return the present value of the startTimestamp for reading data */
    public synchronized int getCurrentStartTimestamp() {
        return currentStartTimestamp;
    }

    public void setCurrentStartTimestamp(int currentStartTimestamp) {
        this.currentStartTimestamp = currentStartTimestamp;
    }

    /** @return returns the most recent timestamp
     */
    public int getMostRecentTimestamp() {
        return mostRecentTimestamp;
    }

    public void setMostRecentTimestamp(int mostRecentTimestamp) {
        this.mostRecentTimestamp = mostRecentTimestamp;
    }

    /** class used to signal a backwards read from input stream */
    public class NonMonotonicTimeException extends Exception {

        protected int timestamp, lastTimestamp;

        protected long position;

        public NonMonotonicTimeException() {
            super();
        }

        public NonMonotonicTimeException(String s) {
            super(s);
        }

        public NonMonotonicTimeException(int ts) {
            this.timestamp = ts;
        }

        /** Constructs a new NonMonotonicTimeException
         *
         * @param readTs the timestamp just read
         * @param lastTs the previous timestamp
         */
        public NonMonotonicTimeException(int readTs, int lastTs) {
            this.timestamp = readTs;
            this.lastTimestamp = lastTs;
        }

        /** Constructs a new NonMonotonicTimeException
         *
         * @param readTs the timestamp just read
         * @param lastTs the previous timestamp
         * @param position the current position in the stream
         */
        public NonMonotonicTimeException(int readTs, int lastTs, long position) {
            this.timestamp = readTs;
            this.lastTimestamp = lastTs;
            this.position = position;
        }

        public int getCurrentTimestamp() {
            return timestamp;
        }

        public int getPreviousTimestamp() {
            return lastTimestamp;
        }

        @Override
        public String toString() {
            return "NonMonotonicTimeException: position=" + position + " timestamp=" + timestamp + " lastTimestamp=" + lastTimestamp + " jumps backwards by " + (timestamp - lastTimestamp);
        }
    }

    /** Indicates that this timestamp has wrapped around from most positive to most negative signed value.
    The de-facto timestamp tick is us and timestamps are represented as int32 in jAER. Therefore the largest possible positive timestamp
    is 2^31-1 ticks which equals 2147.4836 seconds (35.7914 minutes). This wraps to -2147 seconds. The actual total time
    can be computed taking account of these "big wraps" if
    the time is increased by 4294.9673 seconds on each WrappedTimeException (when reading file forwards).
     * @param readTs the current (just read) timestamp
     * @param lastTs the previous timestamp
     */
    public class WrappedTimeException extends NonMonotonicTimeException {

        public WrappedTimeException(int readTs, int lastTs) {
            super(readTs, lastTs);
        }

        public WrappedTimeException(int readTs, int lastTs, long position) {
            super(readTs, lastTs, position);
        }

        @Override
        public String toString() {
            return "WrappedTimeException: position=" + position + " timestamp=" + timestamp + " lastTimestamp=" + lastTimestamp + " jumps backwards by " + (timestamp - lastTimestamp);
        }
    }

    private boolean isWrappedTime(int read, int prevRead, int dt) {
        if (dt > 0 && read <= 0 && prevRead > 0) {
            return true;
        }
        if (dt < 0 && read >= 0 && prevRead < 0) {
            return true;
        }
        return false;
    }

    /** cuts out the part of the stream from IN to OUT and returns it as a new AEInputStream
    @return the new stream
     */
    public AEFileInputStream cut() {
        AEFileInputStream out = null;
        return out;
    }

    /** copies out the part of the stream from IN to OUT markers and returns it as a new AEInputStream
    @return the new stream
     */
    public AEFileInputStream copy() {
        AEFileInputStream out = null;
        return out;
    }

    /** pastes the in stream at the IN marker into this stream
    @param in the stream to paste
     */
    public void paste(AEFileInputStream in) {
    }

    /** returns the chunk number which starts with 0. For position<CHUNK32_SIZE_BYTES returns 0
     */
    private int getChunkNumber(long position) {
        int chunk;
        chunk = (int) ((position * eventSizeBytes) / chunkSizeBytes);
        return chunk;
    }

    private long positionFromChunk(int chunkNumber) {
        long pos = chunkNumber * (chunkSizeBytes / eventSizeBytes);
        return pos;
    }

    /** Maps in the next chunk of the file. */
    protected void mapNextChunk() throws IOException {
        chunkNumber++;
        if (chunkNumber >= numChunks) {
            throw new EOFException("end of file; tried to map chunkNumber=" + chunkNumber + " but file only has numChunks=" + numChunks);
        }
        long start = getChunkStartPosition(chunkNumber);
        if (start >= fileSize || start < 0) {
            chunkNumber = 0;
        }
        mapChunk(chunkNumber);
    }

    /** Maps back in the previous file chunk. */
    protected void mapPreviousChunk() throws IOException {
        chunkNumber--;
        if (chunkNumber < 0) {
            chunkNumber = 0;
        }
        long start = getChunkStartPosition(chunkNumber);
        if (start >= fileSize || start < 0) {
            chunkNumber = 0;
        }
        mapChunk(chunkNumber);
    }

    private int chunksMapped = 0;

    private final int GC_EVERY_THIS_MANY_CHUNKS = 8;

    /** memory-maps a chunk of the input file.
    @param chunkNumber the number of the chunk, starting with 0
     */
    private void mapChunk(int chunkNumber) throws IOException {
        this.chunkNumber = chunkNumber;
        long start = getChunkStartPosition(chunkNumber);
        if (start >= fileSize) {
            throw new EOFException("start of chunk=" + start + " but file has fileSize=" + fileSize);
        }
        long numBytesToMap = chunkSizeBytes;
        if (start + numBytesToMap >= fileSize) {
            numBytesToMap = (long) (fileSize - start);
        }
        byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, start, numBytesToMap);
        this.position = positionFromChunk(chunkNumber);
        log.info("mapped chunk " + chunkNumber + " of " + (numBytesToMap >> 10) + "kB");
        if (++chunksMapped > GC_EVERY_THIS_MANY_CHUNKS) {
            chunksMapped = 0;
            System.gc();
            System.runFinalization();
            log.info("ran garbage collection after mapping chunk " + chunkNumber);
        }
    }

    /** @return start of chunk in bytes
    @param chunk the chunk number
     */
    private long getChunkStartPosition(long chunk) {
        if (chunk <= 0) {
            return headerOffset;
        }
        return (chunk * chunkSizeBytes) + headerOffset;
    }

    private long getChunkEndPosition(long chunk) {
        return headerOffset + (chunk + 1) * chunkSizeBytes;
    }

    /** skips the header lines (if any) */
    protected void skipHeader() throws IOException {
        position(headerOffset);
    }

    /** reads the header comment lines. Must have eventSize and chunkSizeBytes set for backwards compatiblity for files without headers to short address sizes.
     */
    protected void readHeader(FileInputStream fileInputStream) throws IOException {
        if (fileInputStream == null) {
            throw new IOException("null fileInputStream");
        }
        if (in.available() == 0) {
            throw new IOException("empty file (0 bytes available)");
        }
        BufferedReader bufferedHeaderReader = new BufferedReader(new InputStreamReader(fileInputStream));
        headerOffset = 0;
        if (!bufferedHeaderReader.markSupported()) {
            throw new IOException("no mark supported while reading file header, is this a normal file?");
        }
        String s;
        while ((s = readHeaderLine(bufferedHeaderReader)) != null) {
            header.add(s);
            parseFileFormatVersion(s);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("File header:");
        for (String str : header) {
            sb.append(str);
            sb.append(lineSeparator);
        }
        log.info(sb.toString());
        bufferedHeaderReader = null;
    }

    /** parses the file format version given a string with the header comment character stripped off.
    @see net.sf.jaer.eventio.AEDataFile
     */
    protected void parseFileFormatVersion(String s) {
        float version = 1f;
        if (s.startsWith(AEDataFile.DATA_FILE_FORMAT_HEADER)) {
            try {
                version = Float.parseFloat(s.substring(AEDataFile.DATA_FILE_FORMAT_HEADER.length()));
            } catch (NumberFormatException numberFormatException) {
                log.warning("While parsing header line " + s + " got " + numberFormatException.toString());
            }
            if (version < 2) {
                addressType = Short.TYPE;
                eventSizeBytes = Integer.SIZE / 8 + Short.SIZE / 8;
            } else if (version >= 2) {
                addressType = Integer.TYPE;
                eventSizeBytes = Integer.SIZE / 8 + Integer.SIZE / 8;
            }
            log.info("Data file version=" + version + " and has addressType=" + addressType);
        }
    }

    void setupChunks() throws IOException {
        fileChannel = fileInputStream.getChannel();
        fileSize = fileChannel.size();
        chunkSizeBytes = eventSizeBytes * CHUNK_SIZE_EVENTS;
        numChunks = (int) ((fileSize / chunkSizeBytes) + 1);
        log.info("fileSize=" + fileSize + " chunkSizeBytes=" + chunkSizeBytes + " numChunks=" + numChunks);
        mapChunk(0);
    }

    /** assumes we are positioned at start of line and that we may either
     * read a comment char '#' or something else
    leaves us after the line at start of next line or of raw data.
     * Assumes header lines are written using the AEOutputStream.writeHeaderLine().
    @return header line
     */
    private String readHeaderLine(BufferedReader reader) throws IOException {
        int c = reader.read();
        if (c != AEDataFile.COMMENT_CHAR) {
            return null;
        }
        String s = reader.readLine();
        for (byte b : s.getBytes()) {
            if (b < 32 || b > 126) {
                log.warning("Non printable character (byte value=" + b + ") which is (<32 || >126) detected in header line, aborting header read and resetting to start of file because this file may not have a real header");
                return null;
            }
        }
        headerOffset += s.length() + NUMBER_LINE_SEPARATORS + 1;
        return s;
    }

    /** Gets the header strings from the file
    @return list of strings, one per line
     */
    public ArrayList<String> getHeader() {
        return header;
    }

    /** Call to signal first read from file. */
    protected void fireInitPropertyChange() {
        getSupport().firePropertyChange(AEInputStream.EVENT_INIT, 0, 0);
        firstReadCompleted = true;
    }

    /** Returns the File that is being read, or null if the instance is constructed from a FileInputStream */
    public File getFile() {
        return file;
    }

    /** Sets the File reference but doesn't open the file */
    public void setFile(File f) {
        this.file = f;
        absoluteStartingTimeMs = getAbsoluteStartingTimeMsFromFile(getFile());
    }

    /** When the file is opened, the filename is parsed to try to extract the date and time the file was created from the filename.
    @return the time logging was started in ms since 1970
     */
    public long getAbsoluteStartingTimeMs() {
        return absoluteStartingTimeMs;
    }

    public void setAbsoluteStartingTimeMs(long absoluteStartingTimeMs) {
        this.absoluteStartingTimeMs = absoluteStartingTimeMs;
    }

    /**Parses the filename to extract the file logging date from the name of the file.
     *  
     * @return start of logging time in ms, i.e., in "java" time, since 1970 
     */
    private long getAbsoluteStartingTimeMsFromFile(File f) {
        if (f == null) {
            return 0;
        }
        try {
            String fn = f.getName();
            String dateStr = fn.substring(fn.indexOf('-') + 1);
            Date date = AEDataFile.DATE_FORMAT.parse(dateStr);
            log.info(f.getName() + " has from file name the absolute starting date of " + date.toString());
            return date.getTime();
        } catch (Exception e) {
            log.warning(e.toString());
            return 0;
        }
    }

    @Override
    public boolean isNonMonotonicTimeExceptionsChecked() {
        return enableTimeWrappingExceptionsChecking;
    }

    @Override
    public void setNonMonotonicTimeExceptionsChecked(boolean yes) {
        enableTimeWrappingExceptionsChecking = yes;
    }

    /**
     *    * Returns the bitmask that is OR'ed with raw addresses; if result is nonzero then a new timestamp offset is memorized and subtracted from

     * @return the timestampResetBitmask
     */
    public int getTimestampResetBitmask() {
        return timestampResetBitmask;
    }

    /**
     * Sets the bitmask that is OR'ed with raw addresses; if result is nonzero then a new timestamp offset is memorized and subtracted from
     * all subsequent timestamps.
     *
     * @param timestampResetBitmask the timestampResetBitmask to set
     */
    public void setTimestampResetBitmask(int timestampResetBitmask) {
        this.timestampResetBitmask = timestampResetBitmask;
    }
}

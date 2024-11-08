package ch.unizh.ini.jaer.projects.stereo3D;

import net.sf.jaer.eventio.*;
import net.sf.jaer.util.EngineeringFormat;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class to stream in packets of events from binary input stream from a file recorded by 3D reconstructing filters.
 <p>
 File format is very simple: two adresses for left and right retina + timestamp
 <pre>
 int32 x<br>
 int32 y<br>
 int32 z<br>
 float value<br>
 int32 timestamp
 </pre>
 
 <p>
 An optional header consisting of lines starting with '#' is skipped when opening the file and may be retrieved.
 No later comment lines are allowed because the rest ot the file must be pure binary data.
 <p>
 AE3DFileInputStream has PropertyChangeSupport via getSupport(). PropertyChangeListeners will get informed of
 the following events
 <ul>
 <li> "position" - on any new packet of events, either by time chunk or fixed number of events chunk
 <li> "rewind" - on file rewind
 <li> "eof" - on end of file
 <li> "wrappedTime" - on wrap of time timestamps. This happens every int32 us, which is about 4295 seconds which is 71 minutes. Time is negative, then positive, then negative again.
 <li> "init" - on initial read of a file (after creating this with a file input stream). This init event is called on the
 initial packet read because listeners can't be added until the object is created
 </ul>
 
 * @author tobi
 */
public class AE3DFileInputStream extends DataInputStream {

    private PropertyChangeSupport support = new PropertyChangeSupport(this);

    static Logger log = Logger.getLogger("net.sf.jaer.eventio");

    FileInputStream in;

    long fileSize = 0;

    InputStreamReader reader = null;

    private File file = null;

    public final int MAX_NONMONOTONIC_TIME_EXCEPTIONS_TO_PRINT = 1000;

    private int numNonMonotonicTimeExceptionsPrinted = 0;

    private int markPosition = 0;

    private int markInPosition = 0, markOutPosition = 0;

    private int numChunks = 0;

    private boolean firstReadCompleted = false;

    private long absoluteStartingTimeMs = 0;

    private int mostRecentTimestamp, firstTimestamp, lastTimestamp;

    private int currentStartTimestamp;

    FileChannel fileChannel = null;

    /** Maximum internal buffer sizes in events. */
    public static final int MAX_BUFFER_SIZE_EVENTS = 300000;

    public static final int EVENT_SIZE = Float.SIZE / 8 + Integer.SIZE / 8 + Short.SIZE / 8 * 5;

    /** the size of the memory mapped part of the input file.
     This window is centered over the file posiiton except at the start and end of the file.
     */
    public static final int CHUNK_SIZE_BYTES = EVENT_SIZE * 1000000;

    private AEPacket3D packet;

    Event3D tmpEvent = new Event3D();

    MappedByteBuffer byteBuffer = null;

    private int chunkNumber = 0;

    private int position = 0;

    protected ArrayList<String> header = new ArrayList<String>();

    private int headerOffset = 0;

    private volatile boolean pure3D = false;

    /** Creates a new instance of AEInputStream
     @deprecated use the constructor with a File object so that users of this can more easily get file information
     */
    public AE3DFileInputStream(FileInputStream in) {
        super(in);
        init(in);
    }

    /** Creates a new instance of AEInputStream
     @param f the file to open
     @throws FileNotFoundException if file doesn't exist or can't be read
     */
    public AE3DFileInputStream(File f) throws FileNotFoundException {
        this(new FileInputStream(f));
        setFile(f);
    }

    public String toString() {
        EngineeringFormat fmt = new EngineeringFormat();
        String s = "AE3DFileInputStream with size=" + fmt.format(size()) + " events, firstTimestamp=" + getFirstTimestamp() + " lastTimestamp=" + getLastTimestamp() + " duration=" + fmt.format(getDurationUs() / 1e6f) + " s" + " event rate=" + fmt.format(size() / (getDurationUs() / 1e6f)) + " eps";
        return s;
    }

    /** fires property change "position"
     */
    private void init(FileInputStream fileInputStream) {
        this.in = fileInputStream;
        fileChannel = fileInputStream.getChannel();
        try {
            fileSize = fileChannel.size();
        } catch (IOException e) {
            e.printStackTrace();
            fileSize = 0;
        }
        mostRecentTimestamp = 0;
        currentStartTimestamp = 0;
        boolean openok = false;
        System.gc();
        try {
            mapChunk(0);
        } catch (IOException e) {
            log.warning("couldn't map chunk 0 of file");
            e.printStackTrace();
        }
        reader = new InputStreamReader(fileInputStream);
        try {
            readHeader();
        } catch (IOException e) {
            log.warning("couldn't read header");
        }
        int type = readType();
        if (type == Event3D.DIRECT3D) pure3D = true; else pure3D = false;
        packet = new AEPacket3D(MAX_BUFFER_SIZE_EVENTS, type);
        try {
            Event3D ev = readEventForwards();
            firstTimestamp = ev.timestamp;
            position((int) (size() - 1));
            ev = readEventForwards();
            lastTimestamp = ev.timestamp;
            position(0);
            currentStartTimestamp = firstTimestamp;
            mostRecentTimestamp = firstTimestamp;
        } catch (IOException e) {
            System.err.println("couldn't read first event to set starting timestamp");
        } catch (NonMonotonicTimeException e2) {
            log.warning("On AEInputStream.init() caught " + e2.toString());
        }
        log.info(this.toString());
    }

    private int readType() {
        int type = 0;
        String s;
        for (int i = 0; i < header.size(); i++) {
            s = header.get(i);
            if (s.contains("type")) {
                int j = s.indexOf(':');
                if (j > 0) {
                    String stype = s.substring(j + 1, j + 2);
                    if (stype.contains("0")) return 0;
                    if (stype.contains("1")) return 1;
                }
            }
        }
        return type;
    }

    /** reads the next event forward
     @throws EOFException at end of file
     */
    private Event3D readEventForwards() throws IOException, NonMonotonicTimeException {
        int ts = 0;
        int addrx = 0;
        int addry = 0;
        int addrd = 0;
        int method = 0;
        int lead_side = 0;
        float value = 0;
        int addrz = 0;
        try {
            if (pure3D) {
                addrx = (int) byteBuffer.getShort();
                addry = byteBuffer.getShort();
                addrz = byteBuffer.getShort();
                value = byteBuffer.getFloat();
                ts = byteBuffer.getInt();
            } else {
                addrx = (int) byteBuffer.getShort();
                addry = byteBuffer.getShort();
                addrd = byteBuffer.getShort();
                method = byteBuffer.getShort();
                lead_side = byteBuffer.getShort();
                value = byteBuffer.getFloat();
                ts = byteBuffer.getInt();
            }
            if (isWrappedTime(ts, mostRecentTimestamp, 1)) {
                throw new EOFException("3D Wrapped Time");
            }
            if (ts < mostRecentTimestamp) {
            }
            if (pure3D) {
                tmpEvent.x0 = addrx;
                tmpEvent.y0 = addry;
                tmpEvent.z0 = addrz;
                tmpEvent.value = value;
                tmpEvent.timestamp = ts;
            } else {
                tmpEvent.x = addrx;
                tmpEvent.y = addry;
                tmpEvent.d = addrd;
                tmpEvent.method = method;
                tmpEvent.lead_side = lead_side;
                tmpEvent.value = value;
                tmpEvent.timestamp = ts;
            }
            mostRecentTimestamp = ts;
            position++;
            return tmpEvent;
        } catch (BufferUnderflowException e) {
            try {
                mapChunk(++chunkNumber);
                return readEventForwards();
            } catch (IOException eof) {
                byteBuffer = null;
                System.gc();
                getSupport().firePropertyChange("eof", position(), position());
                throw new EOFException("reached end of file");
            }
        } catch (NullPointerException npe) {
            rewind();
            return readEventForwards();
        } finally {
            mostRecentTimestamp = ts;
        }
    }

    /** Reads the next event backwards and leaves the position and byte buffer pointing to event one earlier
     than the one we just read. I.e., we back up, read the event, then back up again to leave us in state to
     either read forwards the event we just read, or to repeat backing up and reading if we read backwards
     */
    private Event3D readEventBackwards() throws IOException, NonMonotonicTimeException {
        int newPos = position - 1;
        if (newPos < 0) {
            newPos = 0;
            throw new EOFException("reached start of file");
        }
        int newBufPos = byteBuffer.position() - EVENT_SIZE;
        if (newBufPos < 0) {
            int newChunkNumber = getChunkNumber(newPos);
            if (newChunkNumber != chunkNumber) {
                mapChunk(--chunkNumber);
                newBufPos = (EVENT_SIZE * newPos) % CHUNK_SIZE_BYTES;
                byteBuffer.position(newBufPos);
            }
        } else {
            byteBuffer.position(newBufPos);
        }
        int ts = 0;
        int addrx = 0;
        int addry = 0;
        int addrd = 0;
        int method = 0;
        int lead_side = 0;
        float value = 0;
        int addrz = 0;
        if (pure3D) {
            addrx = byteBuffer.getShort();
            addry = byteBuffer.getShort();
            addrz = byteBuffer.getShort();
            value = byteBuffer.getFloat();
            ts = byteBuffer.getInt();
        } else {
            addrx = byteBuffer.getShort();
            addry = byteBuffer.getShort();
            addrd = byteBuffer.getShort();
            method = byteBuffer.getShort();
            lead_side = byteBuffer.getShort();
            value = byteBuffer.getFloat();
            ts = byteBuffer.getInt();
        }
        byteBuffer.position(newBufPos);
        if (pure3D) {
            tmpEvent.x0 = addrx;
            tmpEvent.y0 = addry;
            tmpEvent.z0 = addrz;
            tmpEvent.value = value;
            tmpEvent.timestamp = ts;
        } else {
            tmpEvent.x = addrx;
            tmpEvent.y = addry;
            tmpEvent.d = addrd;
            tmpEvent.method = method;
            tmpEvent.lead_side = lead_side;
            tmpEvent.value = value;
            tmpEvent.timestamp = ts;
        }
        mostRecentTimestamp = ts;
        position--;
        if (isWrappedTime(ts, mostRecentTimestamp, -1)) {
            throw new WrappedTimeException(ts, mostRecentTimestamp, position);
        }
        if (ts > mostRecentTimestamp) {
            throw new NonMonotonicTimeException(ts, mostRecentTimestamp, position);
        }
        return tmpEvent;
    }

    /** Uesd to read fixed size packets.
     @param n the number of events to read
     @return a raw packet of events of a specfied number of events
     fires a property change "position" on every call, and a property change "wrappedTime" if time wraps around.
     */
    public synchronized AEPacket3D readPacketByNumber(int n) throws IOException {
        if (!firstReadCompleted) fireInitPropertyChange();
        int an = (int) Math.abs(n);
        if (an > MAX_BUFFER_SIZE_EVENTS) {
            an = MAX_BUFFER_SIZE_EVENTS;
            if (n > 0) n = MAX_BUFFER_SIZE_EVENTS; else n = -MAX_BUFFER_SIZE_EVENTS;
        }
        int[] coordinates_x = packet.getCoordinates_x();
        int[] coordinates_y = packet.getCoordinates_y();
        int[] coordinates_z = packet.getCoordinates_z();
        int[] disparities = packet.getDisparities();
        int[] methods = packet.getMethods();
        int[] lead_sides = packet.getLead_sides();
        float[] values = packet.getValues();
        int[] ts = packet.getTimestamps();
        int oldPosition = position();
        Event3D ev;
        int count = 0;
        try {
            if (n > 0) {
                for (int i = 0; i < n; i++) {
                    ev = readEventForwards();
                    count++;
                    if (pure3D) {
                        packet.setType(Event3D.DIRECT3D);
                        coordinates_x[i] = ev.x0;
                        coordinates_y[i] = ev.y0;
                        coordinates_z[i] = ev.z0;
                    } else {
                        packet.setType(Event3D.INDIRECT3D);
                        coordinates_x[i] = ev.x;
                        coordinates_y[i] = ev.y;
                        disparities[i] = ev.d;
                        methods[i] = ev.method;
                        lead_sides[i] = ev.lead_side;
                    }
                    values[i] = ev.value;
                    ts[i] = ev.timestamp;
                }
            } else {
                n = -n;
                for (int i = 0; i < n; i++) {
                    ev = readEventBackwards();
                    count++;
                    if (pure3D) {
                        packet.setType(Event3D.DIRECT3D);
                        coordinates_x[i] = ev.x0;
                        coordinates_y[i] = ev.y0;
                        coordinates_z[i] = ev.z0;
                    } else {
                        coordinates_x[i] = ev.x;
                        coordinates_y[i] = ev.y;
                        disparities[i] = ev.d;
                        methods[i] = ev.method;
                        lead_sides[i] = ev.lead_side;
                    }
                    values[i] = ev.value;
                    ts[i] = ev.timestamp;
                }
            }
        } catch (WrappedTimeException e) {
            getSupport().firePropertyChange("wrappedTime", oldPosition, position());
        } catch (NonMonotonicTimeException e) {
        }
        packet.setNumEvents(count);
        getSupport().firePropertyChange("position", oldPosition, position());
        return packet;
    }

    /** returns an AEPacketRaw at least dt long up to the max size of the buffer or until end-of-file.
     *Events are read as long as the timestamp until (and including) the event whose timestamp is greater (for dt>0) than
     * startTimestamp+dt, where startTimestamp is the currentStartTimestamp. currentStartTimestamp is incremented after the call by dt.
     *Fires a property change "position" on each call.
     Fires property change "wrappedTime" when time wraps from positive to negative or vice versa (when playing backwards).
     *@param dt the timestamp different in units of the timestamp (usually us)
     *@see #MAX_BUFFER_SIZE_EVENTS
     */
    public synchronized AEPacket3D readPacketByTime(int dt) throws IOException {
        if (!firstReadCompleted) fireInitPropertyChange();
        int endTimestamp = currentStartTimestamp + dt;
        boolean bigWrap = isWrappedTime(endTimestamp, currentStartTimestamp, dt);
        int startTimestamp = mostRecentTimestamp;
        int[] coordinates_x = packet.getCoordinates_x();
        int[] coordinates_y = packet.getCoordinates_y();
        int[] coordinates_z = packet.getCoordinates_z();
        int[] disparities = packet.getDisparities();
        int[] methods = packet.getMethods();
        int[] lead_sides = packet.getLead_sides();
        float[] values = packet.getValues();
        int[] ts = packet.getTimestamps();
        int oldPosition = position();
        Event3D ae;
        int i = 0;
        try {
            if (!bigWrap) {
                do {
                    ae = readEventForwards();
                    if (pure3D) {
                        packet.setType(Event3D.DIRECT3D);
                        coordinates_x[i] = ae.x0;
                        coordinates_y[i] = ae.y0;
                        coordinates_z[i] = ae.z0;
                    } else {
                        coordinates_x[i] = ae.x;
                        coordinates_y[i] = ae.y;
                        disparities[i] = ae.d;
                        methods[i] = ae.method;
                        lead_sides[i] = ae.lead_side;
                    }
                    values[i] = ae.value;
                    ts[i] = ae.timestamp;
                    i++;
                } while (mostRecentTimestamp < endTimestamp && i < values.length - 1);
            } else {
                do {
                    ae = readEventForwards();
                    if (pure3D) {
                        packet.setType(Event3D.DIRECT3D);
                        coordinates_x[i] = ae.x0;
                        coordinates_y[i] = ae.y0;
                        coordinates_z[i] = ae.z0;
                    } else {
                        coordinates_x[i] = ae.x;
                        coordinates_y[i] = ae.y;
                        disparities[i] = ae.d;
                        methods[i] = ae.method;
                        lead_sides[i] = ae.lead_side;
                    }
                    values[i] = ae.value;
                    ts[i] = ae.timestamp;
                    i++;
                } while (mostRecentTimestamp > 0 && i < values.length - 1);
                ae = readEventForwards();
                if (pure3D) {
                    packet.setType(Event3D.DIRECT3D);
                    coordinates_x[i] = ae.x0;
                    coordinates_y[i] = ae.y0;
                    coordinates_z[i] = ae.z0;
                } else {
                    coordinates_x[i] = ae.x;
                    coordinates_y[i] = ae.y;
                    disparities[i] = ae.d;
                    methods[i] = ae.method;
                    lead_sides[i] = ae.lead_side;
                }
                values[i] = ae.value;
                ts[i] = ae.timestamp;
                i++;
            }
            currentStartTimestamp = mostRecentTimestamp;
        } catch (WrappedTimeException w) {
            log.warning(w.toString());
            currentStartTimestamp = w.getTimestamp();
            mostRecentTimestamp = w.getTimestamp();
            getSupport().firePropertyChange("3D wrappedTime", lastTimestamp, mostRecentTimestamp);
        } catch (NonMonotonicTimeException e) {
            if (numNonMonotonicTimeExceptionsPrinted++ < MAX_NONMONOTONIC_TIME_EXCEPTIONS_TO_PRINT) {
                log.info(e + " resetting currentStartTimestamp from " + currentStartTimestamp + " to " + e.getTimestamp() + " and setting mostRecentTimestamp to same value");
                if (numNonMonotonicTimeExceptionsPrinted == MAX_NONMONOTONIC_TIME_EXCEPTIONS_TO_PRINT) {
                    log.warning("suppressing further warnings about NonMonotonicTimeException");
                }
            }
            currentStartTimestamp = e.getTimestamp();
            mostRecentTimestamp = e.getTimestamp();
        }
        packet.setNumEvents(i);
        getSupport().firePropertyChange("position", oldPosition, position());
        return packet;
    }

    /** rewind to the start, or to the marked position, if it has been set. 
     Fires a property change "position" followed by "rewind". */
    public synchronized void rewind() throws IOException {
        int oldPosition = position();
        position(markPosition);
        try {
            if (markPosition == 0) {
                mostRecentTimestamp = firstTimestamp;
            } else {
                readEventForwards();
            }
        } catch (NonMonotonicTimeException e) {
            log.info("rewind from timestamp=" + e.getLastTimestamp() + " to timestamp=" + e.getTimestamp());
        }
        currentStartTimestamp = mostRecentTimestamp;
        getSupport().firePropertyChange("position", oldPosition, position());
        getSupport().firePropertyChange("rewind", oldPosition, position());
    }

    /** gets the size of the stream in events
     @return size in events */
    public long size() {
        return (fileSize - headerOffset) / EVENT_SIZE;
    }

    /** set position in events from start of file
     @param event the number of the event, starting with 0
     */
    public synchronized void position(int event) {
        int newChunkNumber;
        try {
            if ((newChunkNumber = getChunkNumber(event)) != chunkNumber) {
                mapChunk(newChunkNumber);
            }
            byteBuffer.position((event * EVENT_SIZE) % CHUNK_SIZE_BYTES);
            position = event;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** gets the current position for reading forwards, i.e., readEventForwards will read this event number.
     @return position in events
     */
    public synchronized int position() {
        return this.position;
    }

    /**Returns the position as a fraction of the total number of events
     @return fractional position in total events*/
    public synchronized float getFractionalPosition() {
        return (float) position() / size();
    }

    /** Sets fractional position in events
     * @param frac 0-1 float range, 0 at start, 1 at end
     */
    public synchronized void setFractionalPosition(float frac) {
        position((int) (frac * size()));
        try {
            readEventForwards();
        } catch (Exception e) {
            e.printStackTrace();
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
    public synchronized void mark() throws IOException {
        markPosition = position();
        markPosition = (markPosition / EVENT_SIZE) * EVENT_SIZE;
    }

    /** mark the current position as the IN point for editing.
     * @throws IOException if there is some error in reading the data
     */
    public synchronized void markIn() throws IOException {
        markInPosition = position();
        markInPosition = (markPosition / EVENT_SIZE) * EVENT_SIZE;
    }

    /** mark the current position as the OUT position for editing.
     * @throws IOException if there is some error in reading the data
     */
    public synchronized void markOut() throws IOException {
        markOutPosition = position();
        markOutPosition = (markPosition / EVENT_SIZE) * EVENT_SIZE;
    }

    /** clear any marked position */
    public synchronized void unmark() {
        markPosition = 0;
    }

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

        protected int timestamp, lastTimestamp, position;

        public NonMonotonicTimeException() {
            super();
        }

        public NonMonotonicTimeException(String s) {
            super(s);
        }

        public NonMonotonicTimeException(int ts) {
            this.timestamp = ts;
        }

        public NonMonotonicTimeException(int readTs, int lastTs) {
            this.timestamp = readTs;
            this.lastTimestamp = lastTs;
        }

        public NonMonotonicTimeException(int readTs, int lastTs, int position) {
            this.timestamp = readTs;
            this.lastTimestamp = lastTs;
            this.position = position;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public int getLastTimestamp() {
            return lastTimestamp;
        }

        public String toString() {
            return "NonMonotonicTimeException: position=" + position + " timestamp=" + timestamp + " lastTimestamp=" + lastTimestamp + " jumps backwards by " + (timestamp - lastTimestamp);
        }
    }

    /** Indicates that timestamp has wrapped around from most positive to most negative signed value.
     The de-facto timestamp tick is us and timestamps are represented as int32 in jAER. Therefore the largest possible positive timestamp
     is 2^31-1 ticks which equals 2147.4836 seconds (35.7914 minutes). This wraps to -2147 seconds. The actual total time
     can be computed taking account of these "big wraps" if
     the time is increased by 4294.9673 seconds on each WrappedTimeException (when readimg file forwards).
     */
    public class WrappedTimeException extends NonMonotonicTimeException {

        public WrappedTimeException(int readTs, int lastTs) {
            super(readTs, lastTs);
        }

        public WrappedTimeException(int readTs, int lastTs, int position) {
            super(readTs, lastTs, position);
        }

        public String toString() {
            return "WrappedTimeException: timestamp=" + timestamp + " lastTimestamp=" + lastTimestamp + " jumps backwards by " + (timestamp - lastTimestamp);
        }
    }

    private final boolean isWrappedTime(int read, int prevRead, int dt) {
        if (dt > 0 && read < 0 && prevRead > 0) return true;
        if (dt < 0 && read > 0 && prevRead < 0) return true;
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

    /** returns the chunk number which starts with 0. For position<CHUNK_SIZE_BYTES returns 0
     */
    private int getChunkNumber(int position) {
        int chunk = (int) ((position * EVENT_SIZE) / CHUNK_SIZE_BYTES);
        return chunk;
    }

    /** memory-maps a chunk of the input file.
     @param chunkNumber the number of the chunk, starting with 0
     */
    private void mapChunk(int chunkNumber) throws IOException {
        int chunkSize = CHUNK_SIZE_BYTES;
        int start = chunkStart(chunkNumber);
        if (start >= fileSize) {
            throw new EOFException("start of chunk=" + start + " but file has fileSize=" + fileSize);
        }
        if (start + CHUNK_SIZE_BYTES >= fileSize) {
            chunkSize = (int) (fileSize - start);
        }
        byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, start, chunkSize);
        this.chunkNumber = chunkNumber;
    }

    /** @return start of chunk in bytes
     @param chunk the chunk number
     */
    private int chunkStart(int chunk) {
        if (chunk == 0) return headerOffset;
        return (chunk * CHUNK_SIZE_BYTES) + headerOffset;
    }

    private int chunkEnd(int chunk) {
        return headerOffset + (chunk + 1) * CHUNK_SIZE_BYTES;
    }

    /** skips the header lines (if any) */
    protected void skipHeader() throws IOException {
        readHeader();
    }

    /** reads the header comment lines. Assumes we are rewound to position(0).
     */
    protected void readHeader() throws IOException {
        String s;
        while ((s = readHeaderLine()) != null) {
            header.add(s);
        }
        mapChunk(0);
        StringBuffer sb = new StringBuffer();
        sb.append("File header:");
        for (String str : getHeader()) {
            sb.append(str);
            sb.append("\n");
        }
        log.info(sb.toString());
    }

    /** assumes we are positioned at start of line and that we may either read a comment char '#' or something else
     leaves us after the line at start of next line or of raw data. Assumes header lines are written using the AEOutputStream.writeHeaderLine().
     @return header line
     */
    String readHeaderLine() throws IOException {
        StringBuffer s = new StringBuffer();
        byte c = byteBuffer.get();
        if (c != AEDataFile.COMMENT_CHAR) {
            byteBuffer.position(byteBuffer.position() - 1);
            headerOffset = byteBuffer.position();
            return null;
        }
        while (((char) (c = byteBuffer.get())) != '\r') {
            if (c < 32 || c > 126) {
                log.warning("Non printable character (<32 || >126) detected in header line, aborting header read and resetting to start of file because this file may not have a real header");
                byteBuffer.position(0);
                return null;
            }
            s.append((char) c);
        }
        if ((c = byteBuffer.get()) != '\n') {
            log.warning("header line \"" + s.toString() + "\" doesn't end with LF");
        }
        return s.toString();
    }

    /** Gets the header strings from the file
     @return list of strings, one per line
     */
    public ArrayList<String> getHeader() {
        return header;
    }

    private void fireInitPropertyChange() {
        getSupport().firePropertyChange("init", 0, 0);
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

    /** @return start of logging time in ms, i.e., in "java" time, since 1970 */
    private long getAbsoluteStartingTimeMsFromFile(File f) {
        if (f == null) {
            return 0;
        }
        try {
            String fn = f.getName();
            String dateStr = fn.substring(fn.indexOf('-') + 1);
            Date date = AEDataFile.DATE_FORMAT.parse(dateStr);
            return date.getTime();
        } catch (Exception e) {
            log.warning(e.toString());
            return 0;
        }
    }
}

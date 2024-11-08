package neembuu.http;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpfm.JPfmError;
import jpfm.JPfmReadable;
import jpfm.annotations.PartiallyCompleting;
import jpfm.operations.readwrite.Completer;
import jpfm.operations.readwrite.ReadRequest;
import static jpfm.util.ReadUtils.*;
import jpfm.volume.AbstractFile;
import neembuu.GlobalTestSettings;
import neembuu.common.RangeArrayElement;
import neembuu.common.RangeArrayElementFactory;
import neembuu.util.ContentPeek;
import neembuu.util.logging.LoggerUtil;
import neembuu.vfs.readmanager.Connection;
import neembuu.vfs.readmanager.DownloadDataChannel;
import neembuu.vfs.readmanager.DownloadDataStoragePathNegotiator;
import neembuu.vfs.readmanager.NewConnectionParams;
import neembuu.vfs.readmanager.ReadRequestState;
import neembuu.vfs.test.MonitoredHttpFile;
import neembuu.vfs.readmanager.RegionHandler;
import neembuu.vfs.readmanager.TransientConnectionListener;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;

/**
 *
 * @author Shashank Tulsyan
 */
@NotThreadSafe
public final class SeekableHttpChannel extends RegionHandler implements JPfmReadable, Completer, ReadRequestState, DownloadDataStoragePathNegotiator {

    SeekableHttpFile file;

    FileChannel storeChannel;

    private final Map<String, Object> properties = new ConcurrentHashMap<String, Object>();

    private String throttleStateLabel = null;

    @GuardedBy("newConnectionLock")
    private volatile Connection myConnection = null;

    private final TransientConnectionListenerImpl newConnectionLock = new TransientConnectionListenerImpl();

    private final class TransientConnectionListenerImpl implements TransientConnectionListener {

        @GuardedBy("this")
        private volatile boolean working = false;

        TransientConnectionListenerImpl() {
        }

        @Override
        public void describeState(String state) {
        }

        @Override
        public void reportNumberOfRetriesMadeSoFar(int numberOfretries) {
        }

        @GuardedBy("SeekableHttpChannel.newConnectionLock")
        public synchronized boolean tryNewConnection() {
            if (working) return false;
            if (!SeekableHttpChannel.this.isAlive()) {
                if (!working) {
                    working = true;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void successful(Connection c) {
            if (c == null) {
                IllegalArgumentException exp = new IllegalArgumentException("Connection supplied was null, report failure instead");
                failed(exp);
                throw exp;
            }
            synchronized (this) {
                SeekableHttpChannel.this.myConnection = c;
                if (!working) throw new IllegalStateException("Connection already completed earlier");
                working = false;
            }
            SeekableHttpChannel.this.reBirthOffset = SeekableHttpChannel.this.ending();
            SeekableHttpChannel.this.reBirthTime = System.currentTimeMillis();
            SeekableHttpChannel.this.notifyConnectionSuccess();
        }

        @Override
        public void failed(Throwable reason) {
            synchronized (newConnectionLock) {
                SeekableHttpChannel.this.myConnection = null;
                working = false;
            }
        }
    }

    @GuardedBy("SeekableHttpChannel.newConnectionLock")
    public final synchronized Connection getConnection() {
        return myConnection;
    }

    public final void setThrottleStateLabel(String throttleStateLabel) {
        this.throttleStateLabel = throttleStateLabel;
    }

    public final String getThrottleStateLabel() {
        return throttleStateLabel;
    }

    private volatile long reBirthTime;

    private volatile long reBirthOffset = 0;

    /**
     * When a SeekableHttpChannel range array element is created, it has to be created to zero size.
     * But zero sized elements cannot be a part of RangeArray. So we set the size as one and post pond
     * setting the size correctly. When first byte[] of data is available, we set RAEendRepaired as true
     * to indicate this.
     * Volatile since accessible from 2 thread namely, read thread and download thread.
     */
    private volatile boolean RAEendRepaired;

    private static final int NUMBER_OF_ELEMENTS = 20;

    /**
     * Since read is implemented as partially completing
     * there only one committed thread calling it.
     */
    private final LinkedList<ReadRequestElement> speedMeasureReqs = new LinkedList<ReadRequestElement>();

    volatile double smallAverageDownloadSpeed = 0;

    volatile double smallAverageRequestSpeed = Integer.MAX_VALUE / 10;

    public static final String GRAPH_PROPERTY_KEY = "graph";

    public static final Logger LOGGER = LoggerUtil.getLightWeightLogger();

    private final String store;

    protected SeekableHttpChannel() {
        super(0, 0, 0);
        storeChannel = null;
        store = null;
    }

    protected SeekableHttpChannel(long start, long end) {
        super(start, end, end);
        storeChannel = null;
        store = null;
    }

    public SeekableHttpChannel(SeekableHttpFile file, RangeArrayElement bounds, String store) throws IOException {
        super(bounds);
        this.file = file;
        this.store = store;
        RAEendRepaired = true;
        storeChannel = new RandomAccessFile(store, "rw").getChannel();
    }

    public SeekableHttpChannel(SeekableHttpFile file, RangeArrayElement requestRange) throws IOException {
        super(requestRange.starting(), requestRange.starting(), requestRange.ending());
        this.file = file;
        store = file.store + File.separator + Math.random() + "_0x" + Long.toHexString(requestRange.starting()) + ".partial";
        RAEendRepaired = false;
        storeChannel = new RandomAccessFile(store, "rw").getChannel();
    }

    @Override
    public void copyPropertiesFrom(RangeArrayElement entry) {
        if (true) {
        }
        if (!(entry instanceof SeekableHttpChannel)) return;
        SeekableHttpChannel src = (SeekableHttpChannel) entry;
        this.reBirthTime = src.reBirthTime;
        this.storeChannel = src.storeChannel;
    }

    @Override
    public final boolean dissolves(RangeArrayElement entryToCheck) {
        return false;
    }

    public long getFileSize() {
        return file.getFileSize();
    }

    @Override
    @PartiallyCompleting
    public final void read(ReadRequest read) throws Exception {
        long lastReadOffset = read.getFileOffset() + read.getByteBuffer().capacity() - 1;
        if (!RAEendRepaired) {
            registryForAvailabilityNotification(ending());
            return;
        }
        if (lastReadOffset > super.ending()) {
            if (DEBUG_SEEKABLE_CHANNEL) LOGGER.log(Level.INFO, "buffer not filled yet req.={0}+>{1} avail={2}", new Object[] { read.getFileOffset(), read.getByteBuffer().capacity(), this });
            try {
                storeChannel = new RandomAccessFile(store, "rw").getChannel();
                storeChannel = storeChannel.position(this.ending() + 1);
                System.err.println("lastReadOffset > super.ending()    " + lastReadOffset + " > " + super.ending());
                System.err.println("read=" + read);
                startConnection(this.ending() + 1, (int) (read.getFileOffset() + read.getByteBuffer().capacity() - 1 - super.ending()), true);
            } catch (Exception a) {
                LOGGER.log(Level.INFO, "could not create new connection ", a);
            }
            return;
        }
        if (read.getFileOffset() - this.starting() < 0) {
            System.err.println("SeekableHttpChannel.java 312 : negative error=" + read.getFileOffset() + " " + this.starting());
        }
        int totalRead = 0;
        try {
            totalRead += storeChannel.read(read.getByteBuffer(), read.getFileOffset() - this.starting() + totalRead);
        } catch (Exception any) {
            LOGGER.log(Level.INFO, read.toString() + " this=" + this, any);
        }
        completedSuccessfully(read, this);
    }

    private boolean notFilled(long offset, int capacity, int totalRead) {
        long end = capacity + offset - 1;
        if (this.ending() < end) {
            if (offset + totalRead - 1 > this.ending()) {
                return false;
            } else {
                if (this.RAEendRepaired) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public final boolean isAlive() {
        synchronized (newConnectionLock) {
            if (myConnection == null) {
                return false;
            }
            return myConnection.isAlive();
        }
    }

    public void startConnection(int minimumBuffer, boolean fromRead) throws IOException {
        startConnection(this.starting(), minimumBuffer, fromRead);
    }

    public void startConnection(final long offset, final int minimumBuffer, final boolean fromRead) throws IOException {
        if (!newConnectionLock.tryNewConnection()) return;
        if (fromRead) {
            LOGGER.log(Level.INFO, "attempting to resume connection {0} con={1}", new Object[] { this, this.myConnection != null ? this.myConnection : " no my connection right now" });
            LOGGER.log(Level.INFO, "startConnection", new Throwable());
        }
        LOGGER.log(Level.INFO, "offset=" + offset + " this-" + this);
        DownloadDataChannelToImpl newDataChannel = new DownloadDataChannelToImpl(this, minimumBuffer);
        file.getNewConnectionProvider().provideNewConnection(new NewConnectionParams.Builder().setOffset(offset).setMinimumSizeRequired(minimumBuffer).setDownloadDataChannel(newDataChannel).setReadRequestState(this).setDownloadDataStoragePathNegotiator(this).setTransientConnectionListener(newConnectionLock).setCookies(file.getCookies()).build());
    }

    @Override
    public String getTooltipString() {
        return NumberFormat.getInstance().format(this.getDownloadSpeed() / 1024) + " KBps \n" + " RequestSpeed = " + NumberFormat.getInstance().format(this.getRequestSpeed() / 1024) + " KBps \n" + (isAlive() ? "alive" : "dead");
    }

    private static final boolean DEBUG_SEEKABLE_CHANNEL = GlobalTestSettings.getValue("DEBUG_SEEKABLE_CHANNEL");

    private static final boolean DEBUG_CENTRAL_FILE_READ = GlobalTestSettings.getValue("DEBUG_CENTRAL_FILE_READ");

    @Override
    @PartiallyCompleting
    public final void handleRead(final ReadRequest readRequest) {
        synchronized (speedMeasureReqs) {
            if (speedMeasureReqs.isEmpty()) {
                speedMeasureReqs.add(new ReadRequestElement(readRequest, requestSize(readRequest)));
            } else if (endingOffset(readRequest) > endingOffset(speedMeasureReqs.getLast().readRequest)) {
                speedMeasureReqs.add(new ReadRequestElement(readRequest, (int) (endingOffset(readRequest) - endingOffset(speedMeasureReqs.getLast().readRequest))));
                for (; speedMeasureReqs.size() > NUMBER_OF_ELEMENTS + 1; ) {
                    if (!speedMeasureReqs.getFirst().readRequest.isCompleted()) break;
                    speedMeasureReqs.removeFirst();
                }
            }
        }
        try {
            read(readRequest);
        } catch (Exception any) {
            if (!readRequest.isCompleted()) {
                readRequest.complete(JPfmError.SUCCESS, 0, null);
            }
        }
        if (readRequest.isCompleted()) {
            updateRequestSpeed();
            if (DEBUG_CENTRAL_FILE_READ) LOGGER.log(Level.INFO, "completed=\t{0}\t{1}\t{2}", new Object[] { readRequest.getFileOffset(), ContentPeek.generatePeekString(readRequest), readRequest.getError().toString() });
        }
    }

    @Override
    public final long lastRequestTime() {
        synchronized (speedMeasureReqs) {
            if (speedMeasureReqs.size() == 0) {
                return reBirthTime;
            }
            return speedMeasureReqs.getLast().readRequest.getCreationTime();
        }
    }

    @Override
    public final boolean requestsPresentAlongThisConnection() {
        synchronized (speedMeasureReqs) {
            for (ReadRequestElement rr : speedMeasureReqs) {
                if (!rr.readRequest.isCompleted()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public final boolean requestsPresentAlongSomeConnection() {
        return file.hasPendingRequests();
    }

    @Override
    public final boolean pendingRequestsPresentOutside() {
        synchronized (speedMeasureReqs) {
            for (ReadRequestElement rr : speedMeasureReqs) {
                if (!rr.readRequest.isCompleted()) {
                    if (rr.readRequest.getFileOffset() > this.ending() || endingOffset(rr.readRequest) > this.ending()) return true;
                }
            }
        }
        return false;
    }

    private void updateRequestSpeed() {
        synchronized (speedMeasureReqs) {
            long sizeSum = 0;
            boolean firstSkipped = (speedMeasureReqs.size() <= 1);
            Iterator<ReadRequestElement> it = speedMeasureReqs.iterator();
            while (it.hasNext()) {
                ReadRequestElement rr = it.next();
                if (!firstSkipped) {
                    firstSkipped = true;
                    continue;
                }
                sizeSum += rr.inducedSize;
            }
            long firstTime = System.currentTimeMillis() - 15;
            try {
                firstTime = speedMeasureReqs.getFirst().readRequest.getCreationTime();
            } catch (Exception no) {
            }
            if (sizeSum == 0) {
            }
            smallAverageRequestSpeed = sizeSum / (System.currentTimeMillis() - firstTime);
            if ((SpeedObserver) getProperty(GRAPH_PROPERTY_KEY) != null) ((SpeedObserver) getProperty(GRAPH_PROPERTY_KEY)).addRequestSpeedObservation(smallAverageRequestSpeed);
        }
        file.notifySpeedChange();
    }

    private int norm(int speedArrayIndex) {
        return speedArrayIndex % NUMBER_OF_ELEMENTS;
    }

    @Override
    public String toString() {
        return "SeekableHttpChannel{" + super.toString() + " ,authl=" + authorityLimit() + " isAlive=" + isAlive() + "}";
    }

    @Override
    public int getBytesFilledTillNow(ReadRequest pendingRequest) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void completeNow(ReadRequest pendingRequest) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException("Someone invoking open");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Someone invoking close");
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final long getDownloadSpeed() {
        if (!this.isAlive()) return 0;
        return (long) (smallAverageDownloadSpeed * 1000);
    }

    @Override
    public final long getRequestSpeed() {
        if (!this.isAlive()) return 0;
        return (long) (smallAverageRequestSpeed * 1000);
    }

    @Override
    public final long requestDownloadGap() {
        ReadRequestElement rre = null;
        synchronized (speedMeasureReqs) {
            if (speedMeasureReqs.size() == 0) {
                return 0;
            }
            rre = speedMeasureReqs.getLast();
        }
        return this.ending() - endingOffset(rre.readRequest);
    }

    @Override
    public final AbstractFile provide(String store, boolean shouldNeembuuManage) {
        if (shouldNeembuuManage) {
            try {
                return null;
            } catch (Exception a) {
                LOGGER.log(Level.INFO, "Failed to provide storage path", a);
                return null;
            }
        } else {
            throw new UnsupportedOperationException("Not supported yet. TODO");
        }
    }

    @NotThreadSafe
    private static final class DownloadDataChannelToImpl implements DownloadDataChannel {

        private final SeekableHttpChannel seekableHttpChannel;

        private long lsttime = System.nanoTime();

        @GuardedBy("seekableHttpChannel.newConnectionLock")
        private volatile boolean isOpen = true;

        private final int minimumBuffer;

        public DownloadDataChannelToImpl(SeekableHttpChannel seekableHttpChannel, int minimumBuffer) {
            this.seekableHttpChannel = seekableHttpChannel;
            this.minimumBuffer = minimumBuffer;
            if (seekableHttpChannel.file instanceof MonitoredHttpFile) {
                MonitoredHttpFile casted = (MonitoredHttpFile) seekableHttpChannel.file;
                casted.filePanel.downloadingLabel.setText(seekableHttpChannel.toString());
            }
        }

        private void check() {
            synchronized (seekableHttpChannel.newConnectionLock) {
                if (!seekableHttpChannel.isAlive()) {
                    throw new IllegalStateException("Incorrect implementation of Connection : Attempting to write for a dead connection");
                }
                if (!isOpen) throw new IllegalStateException("Channel closed");
            }
        }

        @Override
        public final void close() {
            synchronized (seekableHttpChannel.newConnectionLock) {
                if (!isOpen) {
                    throw new IllegalStateException("Already closed");
                }
                isOpen = false;
            }
        }

        @Override
        public final boolean isOpen() {
            synchronized (seekableHttpChannel.newConnectionLock) {
                return isOpen;
            }
        }

        @Override
        public final int write(ByteBuffer toWrite) throws IOException {
            check();
            refreshDownloadSpeed(toWrite.limit());
            int done = 0;
            long desiredEnd = -1;
            seekableHttpChannel.storeChannel.position(seekableHttpChannel.getSize());
            if (seekableHttpChannel.RAEendRepaired) {
                done += seekableHttpChannel.storeChannel.write(toWrite, seekableHttpChannel.getSize());
                desiredEnd = seekableHttpChannel.ending() + toWrite.limit();
            } else {
                seekableHttpChannel.storeChannel.position(0);
                done += seekableHttpChannel.storeChannel.write(toWrite);
                if (done > 1) {
                    desiredEnd = seekableHttpChannel.starting() + toWrite.limit() - 1;
                    seekableHttpChannel.notifyRegionAvailability(seekableHttpChannel.ending());
                    seekableHttpChannel.RAEendRepaired = true;
                }
            }
            if (toWrite.hasRemaining()) {
                throw new RuntimeException("The write channel did not commit the entire write request. Total length=" + toWrite.limit() + " committed=" + done);
            }
            if (seekableHttpChannel.expandEndingAndAuthorityLimitCarefully(seekableHttpChannel.file.getConnections(), desiredEnd) < desiredEnd) {
                if (DEBUG_SEEKABLE_CHANNEL) LOGGER.log(Level.INFO, "we leaked into another region {0} killing ", seekableHttpChannel);
                seekableHttpChannel.myConnection.abort();
            }
            seekableHttpChannel.notifyRegionAvailability(seekableHttpChannel.ending());
            return toWrite.capacity();
        }

        private void refreshDownloadSpeed(int currentBufferSize) {
            long currentTime = System.currentTimeMillis();
            long downloadSpeedAveragedOverEntireLifeOfConnection;
            try {
                downloadSpeedAveragedOverEntireLifeOfConnection = (seekableHttpChannel.ending() + currentBufferSize - seekableHttpChannel.reBirthOffset) / (currentTime - seekableHttpChannel.reBirthTime);
            } catch (ArithmeticException ae) {
                downloadSpeedAveragedOverEntireLifeOfConnection = 0;
            }
            seekableHttpChannel.smallAverageDownloadSpeed = downloadSpeedAveragedOverEntireLifeOfConnection;
            if (seekableHttpChannel.getProperty(GRAPH_PROPERTY_KEY) != null) ((SpeedObserver) seekableHttpChannel.getProperty(GRAPH_PROPERTY_KEY)).addSupplySpeedObservation(downloadSpeedAveragedOverEntireLifeOfConnection);
            seekableHttpChannel.updateRequestSpeed();
        }
    }

    public static final class PartFactory implements RangeArrayElementFactory<SeekableHttpChannel> {

        public static final PartFactory INSTANCE = new PartFactory();

        private PartFactory() {
        }

        @Override
        public final SeekableHttpChannel newInstance() {
            return new SeekableHttpChannel();
        }

        @Override
        public final SeekableHttpChannel newInstance(long start, long end) {
            return new SeekableHttpChannel(start, end);
        }

        @Override
        public final boolean entriesNeverDissolve() {
            return true;
        }
    }

    public final Object getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    public final Object setProperty(String propertyName, Object value) {
        return properties.put(propertyName, value);
    }

    public static class ReadRequestElement {

        private final int inducedSize;

        private final ReadRequest readRequest;

        public ReadRequestElement(ReadRequest readRequest, int inducedSize) {
            this.inducedSize = inducedSize;
            this.readRequest = readRequest;
        }
    }
}

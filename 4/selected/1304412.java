package goldengate.ftp.core.utils.bandwith;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jboss.netty.channel.Channel;

/**
 * One Monitor for each Session and One Monitor for the global status
 * @author frederic
 * goldengate.ftp.core.utils ThroughputMonitor
 * 
 */
public class ThroughputMonitor implements Runnable {

    /**
	 * Default global limit 512Mbit
	 */
    public static long DEFAULT_GLOBAL_LIMIT = 0x4000000L;

    /**
	 * Default session limit 64Mbit, so up to 8 full simultaneous clients
	 */
    public static long DEFAULT_SESSION_LIMIT = 0x800000L;

    /**
	 * No limit
	 */
    public static long NO_LIMIT = -1;

    /**
	 * Default delay between two checks: 1s
	 */
    public static long DEFAULT_DELAY = 1000;

    /**
	 * Current writing bytes
	 */
    private AtomicLong currentWritingBytes = new AtomicLong(0);

    /**
	 * Current reading bytes
	 */
    private AtomicLong currentReadingBytes = new AtomicLong(0);

    /**
	 * Last writing bandwith
	 */
    private AtomicLong lastWritingMarker = new AtomicLong(0);

    /**
	 * Last reading bandwith
	 */
    private AtomicLong lastReadingMarker = new AtomicLong(0);

    /**
	 * Last Time Check taken
	 */
    private AtomicLong lastTime = new AtomicLong(0);

    /**
	 * Current Limit in B/s to apply to write
	 */
    private long limitWrite = NO_LIMIT;

    /**
	 * Current Limit in B/s to apply to read
	 */
    private long limitRead = NO_LIMIT;

    /**
	 * Delay between two capture
	 */
    private long delay = DEFAULT_DELAY;

    /**
	 * Sleeping delay
	 */
    private long sleepingDelay = DEFAULT_DELAY >> 4;

    /**
	 * Name of this Monitor
	 */
    private String name = null;

    /**
	 * Is this monitor for a channel monitoring or for global monitoring
	 */
    private boolean isPerChannel = false;

    /**
	 * Associated monitoredChannel if any (global MUST NOT have any)
	 */
    private Channel monitoredChannel = null;

    /**
	 * Default Executor
	 */
    private ScheduledExecutorService executorService = null;

    private int runningScheduled = 0;

    /**
	 * Thread that will host this monitor
	 */
    private ScheduledFuture<?> monitorFuture = null;

    /**
	 * 
	 * @return Get the current ExecutorService
	 */
    private ScheduledExecutorService getExecutorService() {
        if (this.executorService == null) {
            executorService = Executors.newScheduledThreadPool(2);
            this.runningScheduled = 0;
        }
        return this.executorService;
    }

    /**
	 * Start the monitoring process
	 *
	 */
    public void startMonitoring() {
        synchronized (this.lastTime) {
            if (this.monitorFuture != null) {
                return;
            }
            lastTime.set(System.currentTimeMillis());
            this.monitorFuture = getExecutorService().scheduleWithFixedDelay(this, 10, delay, TimeUnit.MILLISECONDS);
            this.runningScheduled++;
        }
    }

    /**
	 * Stop the monitoring process
	 *
	 */
    public void stopMonitoring() {
        synchronized (this.lastTime) {
            if (this.monitorFuture == null) {
                return;
            }
            this.monitorFuture = null;
            if (this.executorService != null) {
                this.executorService.shutdownNow();
                this.executorService = null;
            }
            this.setMonitoredChannel(null);
        }
    }

    /**
	 * Default run
	 */
    public void run() {
        long endTime = System.currentTimeMillis();
        this.resetAccounting(endTime);
    }

    /**
	 * Default constructor with no limit and default name
	 *
	 * @param channel Not null means this monitors will be for this channel only, else it will be for global monitoring.
	 * Channel can be set later on therefore changing its behaviour from global to per channel
	 */
    public ThroughputMonitor(Channel channel) {
        this.name = "DEFAULT";
        this.changeConfiguration(channel, NO_LIMIT, NO_LIMIT, DEFAULT_DELAY);
    }

    /**
	 * Default constructor with no limit
	 * @param channel Not null means this monitors will be for this channel only, else it will be for global monitoring.
	 * Channel can be set later on therefore changing its behaviour from global to per channel
	 * @param name
	 */
    public ThroughputMonitor(Channel channel, String name) {
        this.name = name;
        this.changeConfiguration(channel, NO_LIMIT, NO_LIMIT, DEFAULT_DELAY);
    }

    /**
	 * Constructor with specified limits in MByte (not MBit) and default delay
	 * @param channel Not null means this monitors will be for this channel only, else it will be for global monitoring.
	 * Channel can be set later on therefore changing its behaviour from global to per channel
	 * @param name
	 * @param writeLimit
	 * @param readLimit
	 */
    public ThroughputMonitor(Channel channel, String name, long writeLimit, long readLimit) {
        this.name = name;
        this.changeConfiguration(channel, writeLimit, readLimit, DEFAULT_DELAY);
    }

    /**
	 * Constructor with specified limits in MByte (not MBit) and default delay
	 * @param channel Not null means this monitors will be for this channel only, else it will be for global monitoring.
	 * Channel can be set later on therefore changing its behaviour from global to per channel
	 * @param writeLimit
	 * @param readLimit
	 */
    public ThroughputMonitor(Channel channel, long writeLimit, long readLimit) {
        this.name = "DEFAULT";
        this.changeConfiguration(channel, writeLimit, readLimit, DEFAULT_DELAY);
    }

    /**
	 * Constructor with specified limits in Byte/s (not Bit/s) and the 
	 * specified delay between two computations in ms
	 * @param channel Not null means this monitors will be for this channel only, else it will be for global monitoring.
	 * Channel can be set later on therefore changing its behaviour from global to per channel
	 * @param name
	 * @param writeLimit
	 * @param readLimit
	 * @param delay
	 */
    public ThroughputMonitor(Channel channel, String name, long writeLimit, long readLimit, long delay) {
        this.name = name;
        this.changeConfiguration(channel, writeLimit, readLimit, delay);
    }

    /**
	 * Constructor with specified limits in Byte/s (not Bit/s) and the 
	 * specified delay between two computations in ms
	 * @param channel Not null means this monitors will be for this channel only, else it will be for global monitoring.
	 * Channel can be set later on therefore changing its behaviour from global to per channel
	 * @param writeLimit
	 * @param readLimit
	 * @param delay
	 */
    public ThroughputMonitor(Channel channel, long writeLimit, long readLimit, long delay) {
        this.name = "DEFAULT";
        this.changeConfiguration(channel, writeLimit, readLimit, delay);
    }

    /**
	 * Set the Session monitoredChannel (not for Global Monitor)
	 * @param channel Not null means this monitors will be for this channel only, else it will be for global monitoring.
	 * Channel can be set later on therefore changing its behaviour from global to per channel
	 */
    public void setMonitoredChannel(Channel channel) {
        if (channel != null) {
            this.monitoredChannel = channel;
            this.isPerChannel = true;
        } else {
            this.isPerChannel = false;
            this.monitoredChannel = null;
        }
    }

    /**
	 * Specifies limits in Byte/s (not Bit/s) and default delay to 1s
	 * @param channel Not null means this monitors will be for this channel only, else it will be for global monitoring.
	 * Channel can be set later on therefore changing its behaviour from global to per channel
	 * @param writeLimit
	 * @param readLimit
	 */
    public void changeConfiguration(Channel channel, long writeLimit, long readLimit) {
        this.changeConfiguration(channel, writeLimit, readLimit, DEFAULT_DELAY);
    }

    /**
	 * Specifies limits in Byte/s (not Bit/s) and the 
	 * specified delay between two computations in ms
	 * @param channel Not null means this monitors will be for this channel only, else it will be for global monitoring.
	 * Channel can be set later on therefore changing its behaviour from global to per channel
	 * @param writeLimit
	 * @param readLimit
	 * @param delayToSet
	 */
    public void changeConfiguration(Channel channel, long writeLimit, long readLimit, long delayToSet) {
        this.limitWrite = writeLimit;
        this.limitRead = readLimit;
        this.delay = delayToSet;
        this.sleepingDelay = this.delay >> 4;
        if (this.sleepingDelay > 200) {
            this.sleepingDelay = 200;
        }
        if (this.sleepingDelay < 10) {
            this.sleepingDelay = 10;
        }
        this.setMonitoredChannel(channel);
    }

    /**
	 * 
	 * @return the current delay between two computations in ms
	 */
    public long getDelay() {
        return this.delay;
    }

    /**
	 * Set the accounting on Read and Write
	 * @param newLastTime
	 */
    private void resetAccounting(long newLastTime) {
        synchronized (this.lastTime) {
            long interval = newLastTime - this.lastTime.getAndSet(newLastTime);
            this.lastReadingMarker.set((this.currentReadingBytes.getAndSet(0) / interval) * 1000);
            this.lastWritingMarker.set((this.currentWritingBytes.getAndSet(0) / interval) * 1000);
        }
    }

    /**
	 * 
	 * @return the current IN bandwith in byte/s
	 */
    public long getLastReadByteBySecond() {
        return this.lastReadingMarker.get();
    }

    /**
	 * 
	 * @return the current OUT bandwith in byte/s
	 */
    public long getLastWriteByteBySecond() {
        return this.lastWritingMarker.get();
    }

    /**
	 * 
	 * @return the time that should be necessary to wait to respect limit. Can be negative time
	 */
    private long getReadTimeToWait() {
        synchronized (this.lastTime) {
            long interval = System.currentTimeMillis() - this.lastTime.get();
            if (interval == 0) {
                return 0;
            }
            long bandwith = (this.currentReadingBytes.get() * 1000 / interval);
            long wait = ((bandwith * interval) / this.limitRead) - interval;
            return wait;
        }
    }

    /**
	 * 
	 * @return the time that should be necessary to wait to respect limit. Can be negative time
	 */
    private long getWriteTimeToWait() {
        synchronized (this.lastTime) {
            long interval = System.currentTimeMillis() - this.lastTime.get();
            if (interval == 0) {
                return 0;
            }
            long bandwith = (this.currentWritingBytes.get() * 1000 / interval);
            long wait = ((bandwith * interval) / this.limitWrite) - interval;
            return wait;
        }
    }

    /**
	 * Class to implement setReadable at fix time
	 * FIXME does not work since it seems to setReadable when the connection is closing too!!!
	 * @author frederic
	 * goldengate.ftp.core.utils ReopenRead
	 *
	 */
    private class ReopenRead implements Runnable {

        /**
		 * Monitor
		 */
        private ThroughputMonitor monitor = null;

        /**
		 * @param monitor
		 */
        public ReopenRead(ThroughputMonitor monitor) {
            this.monitor = monitor;
        }

        public void run() {
            if ((this.monitor != null) && (this.monitor.monitoredChannel != null) && (this.monitor.monitoredChannel.isConnected())) {
                this.monitor.monitoredChannel.setReadable(true);
            }
            this.monitor.runningScheduled--;
        }
    }

    /**
	 * If Read is in excess, it will block the read operation until it will be ready again.
	 * FIXME does not work since it seems to setReadable when the connection is closing too!!!
	 * @param recv the size in bytes to read
	 * @throws InterruptedException 
	 */
    public void setReceivedBytes(long recv) throws InterruptedException {
        this.currentReadingBytes.addAndGet(recv);
        if (this.limitRead == NO_LIMIT) {
            return;
        }
        if ((this.isPerChannel) && (this.monitoredChannel != null) && (!this.monitoredChannel.isConnected())) {
            return;
        }
        long wait = this.getReadTimeToWait();
        if (wait > 50) {
            if ((this.isPerChannel) && (this.monitoredChannel != null) && (this.monitoredChannel.isConnected())) {
                this.monitoredChannel.setReadable(false);
                if (true) {
                    Thread.sleep(wait);
                    this.monitoredChannel.setReadable(true);
                    return;
                }
                getExecutorService().schedule(new ReopenRead(this), wait, TimeUnit.MILLISECONDS);
                this.runningScheduled++;
            } else {
                Thread.sleep(wait);
            }
        }
    }

    /**
	 * If Write is in excess, it will block the write operation until it will be ready again.
	 * @param write the size in bytes to write
	 * @throws InterruptedException 
	 */
    public void setToWriteBytes(long write) throws InterruptedException {
        this.currentWritingBytes.addAndGet(write);
        if (this.limitWrite == NO_LIMIT) {
            return;
        }
        long wait = this.getWriteTimeToWait();
        if (wait > 50) {
            Thread.sleep(wait);
        }
    }

    /**
	 * @return the accounting on Read without changing the marker
	 */
    private long getReadByteBySecond() {
        long interval = System.currentTimeMillis() - this.lastTime.get();
        if (interval == 0) {
            return this.currentReadingBytes.get() * 10;
        }
        return ((this.currentReadingBytes.get() / interval) * 1000);
    }

    /**
	 * Get the accounting on Write without changing the marker
	 */
    private long getWriteByteBySecond() {
        long interval = System.currentTimeMillis() - this.lastTime.get();
        if (interval == 0) {
            return this.currentWritingBytes.get() * 10;
        }
        return ((this.currentWritingBytes.get() / interval) * 1000);
    }

    /**
	 * 
	 * @return True if currently the read limit is reached
	 */
    private boolean isReadInExcess() {
        synchronized (this.lastTime) {
            return (this.getReadByteBySecond() > this.limitRead);
        }
    }

    /**
	 * 
	 * @return True if currently the write limit is reached
	 */
    private boolean isWriteInExcess() {
        synchronized (this.lastTime) {
            return (this.getWriteByteBySecond() > this.limitWrite);
        }
    }

    /**
	 * If Read is in excess, it will block the read operation until it will be ready again.
	 * @param recv the size in bytes to read
	 * @return True if the read is in excess
	 * @throws InterruptedException 
	 */
    public boolean setReceivedBytesAwait(long recv) throws InterruptedException {
        this.currentReadingBytes.addAndGet(recv);
        if (this.limitRead == NO_LIMIT) {
            return false;
        }
        if ((this.isPerChannel) && (this.monitoredChannel != null) && (!this.monitoredChannel.isConnected())) {
            return true;
        }
        if (this.isReadInExcess()) {
            if ((this.isPerChannel) && (this.monitoredChannel != null)) {
                this.monitoredChannel.setReadable(false);
            }
            Thread.sleep(this.sleepingDelay);
            while (this.isReadInExcess()) {
                Thread.sleep(this.sleepingDelay);
            }
            if ((this.isPerChannel) && (this.monitoredChannel != null)) {
                this.monitoredChannel.setReadable(true);
            }
        }
        return true;
    }

    /**
	 * If Write is in excess, it will block the write operation until it will be ready again.
	 * @param write the size in bytes to write
	 * @return True if the write is in excess
	 * @throws InterruptedException 
	 */
    public boolean setToWriteBytesAwait(long write) throws InterruptedException {
        this.currentWritingBytes.addAndGet(write);
        if (this.limitWrite == NO_LIMIT) {
            return false;
        }
        if (this.isWriteInExcess()) {
            while (this.isWriteInExcess()) {
                Thread.sleep(this.sleepingDelay);
            }
        }
        return true;
    }

    /**
	 * String information
	 */
    public String toString() {
        return "Monitor " + this.name + " Current Speed Read: " + (this.getLastReadByteBySecond() >> 10) + " KB/s, Write: " + (this.getLastWriteByteBySecond() >> 10) + " KB/s Current Read: " + (this.currentReadingBytes.get() >> 10) + " KB Current Write: " + (this.currentWritingBytes.get() >> 10) + " KB";
    }
}

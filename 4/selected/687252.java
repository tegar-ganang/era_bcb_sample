package org.jboss.netty.handler.traffic;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.DefaultObjectSizeEstimator;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.ObjectSizeEstimator;
import org.jboss.netty.util.internal.ExecutorUtil;

/**
 * AbstractTrafficShapingHandler allows to limit the global bandwidth
 * (see {@link GlobalTrafficShapingHandler}) or per session
 * bandwidth (see {@link ChannelTrafficShapingHandler}), as traffic shaping.
 * It allows too to implement an almost real time monitoring of the bandwidth using
 * the monitors from {@link TrafficCounter} that will call back every checkInterval
 * the method doAccounting of this handler.<br>
 * <br>
 *
 * An {@link ObjectSizeEstimator} can be passed at construction to specify what
 * is the size of the object to be read or write accordingly to the type of
 * object. If not specified, it will used the {@link DefaultObjectSizeEstimator} implementation.<br><br>
 *
 * If you want for any particular reasons to stop the monitoring (accounting) or to change
 * the read/write limit or the check interval, several methods allow that for you:<br>
 * <ul>
 * <li><tt>configure</tt> allows you to change read or write limits, or the checkInterval</li>
 * <li><tt>getTrafficCounter</tt> allows you to have access to the TrafficCounter and so to stop
 * or start the monitoring, to change the checkInterval directly, or to have access to its values.</li>
 * <li></li>
 * </ul>
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Frederic Bregier
 * @version $Rev: 1059 $, $Date: 2012-02-20 17:25:22 -0500 (Mon, 20 Feb 2012) $
 */
public abstract class AbstractTrafficShapingHandler extends SimpleChannelHandler implements ExternalResourceReleasable {

    /**
     * Internal logger
     */
    static InternalLogger logger = InternalLoggerFactory.getInstance(AbstractTrafficShapingHandler.class);

    /**
     * Default delay between two checks: 1s
     */
    public static final long DEFAULT_CHECK_INTERVAL = 1000;

    /**
     * Default minimal time to wait
     */
    private static final long MINIMAL_WAIT = 10;

    /**
     * Traffic Counter
     */
    protected TrafficCounter trafficCounter = null;

    /**
     * ObjectSizeEstimator
     */
    private ObjectSizeEstimator objectSizeEstimator = null;

    /**
     * Executor to associated to any TrafficCounter
     */
    protected Executor executor = null;

    /**
     * Limit in B/s to apply to write
     */
    private long writeLimit = 0;

    /**
     * Limit in B/s to apply to read
     */
    private long readLimit = 0;

    /**
     * Delay between two performance snapshots
     */
    protected long checkInterval = DEFAULT_CHECK_INTERVAL;

    /**
     * Boolean associated with the release of this TrafficShapingHandler.
     * It will be true only once when the releaseExternalRessources is called
     * to prevent waiting when shutdown.
     */
    private final AtomicBoolean release = new AtomicBoolean(false);

    /**
    * @param newObjectSizeEstimator
    * @param newExecutor
    * @param newActive
    * @param newWriteLimit
    * @param newReadLimit
    * @param newCheckInterval
    */
    private void init(ObjectSizeEstimator newObjectSizeEstimator, Executor newExecutor, long newWriteLimit, long newReadLimit, long newCheckInterval) {
        objectSizeEstimator = newObjectSizeEstimator;
        executor = newExecutor;
        writeLimit = newWriteLimit;
        readLimit = newReadLimit;
        checkInterval = newCheckInterval;
    }

    /**
     *
     * @param newTrafficCounter the TrafficCounter to set
     */
    void setTrafficCounter(TrafficCounter newTrafficCounter) {
        trafficCounter = newTrafficCounter;
    }

    /**
     * Constructor using default {@link ObjectSizeEstimator}
     *
     * @param executor
     *          created for instance like Executors.newCachedThreadPool
     * @param writeLimit
     *          0 or a limit in bytes/s
     * @param readLimit
     *          0 or a limit in bytes/s
     * @param checkInterval
     *          The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed
     */
    public AbstractTrafficShapingHandler(Executor executor, long writeLimit, long readLimit, long checkInterval) {
        super();
        init(new DefaultObjectSizeEstimator(), executor, writeLimit, readLimit, checkInterval);
    }

    /**
     * Constructor using the specified ObjectSizeEstimator
     *
     * @param objectSizeEstimator
     *            the {@link ObjectSizeEstimator} that will be used to compute
     *            the size of the message
     * @param executor
     *          created for instance like Executors.newCachedThreadPool
     * @param writeLimit
     *          0 or a limit in bytes/s
     * @param readLimit
     *          0 or a limit in bytes/s
     * @param checkInterval
     *          The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed
     */
    public AbstractTrafficShapingHandler(ObjectSizeEstimator objectSizeEstimator, Executor executor, long writeLimit, long readLimit, long checkInterval) {
        super();
        init(objectSizeEstimator, executor, writeLimit, readLimit, checkInterval);
    }

    /**
     * Constructor using default {@link ObjectSizeEstimator} and using default Check Interval
     *
     * @param executor
     *          created for instance like Executors.newCachedThreadPool
     * @param writeLimit
     *          0 or a limit in bytes/s
     * @param readLimit
     *          0 or a limit in bytes/s
     */
    public AbstractTrafficShapingHandler(Executor executor, long writeLimit, long readLimit) {
        super();
        init(new DefaultObjectSizeEstimator(), executor, writeLimit, readLimit, DEFAULT_CHECK_INTERVAL);
    }

    /**
     * Constructor using the specified ObjectSizeEstimator and using default Check Interval
     *
     * @param objectSizeEstimator
     *            the {@link ObjectSizeEstimator} that will be used to compute
     *            the size of the message
     * @param executor
     *          created for instance like Executors.newCachedThreadPool
     * @param writeLimit
     *          0 or a limit in bytes/s
     * @param readLimit
     *          0 or a limit in bytes/s
     */
    public AbstractTrafficShapingHandler(ObjectSizeEstimator objectSizeEstimator, Executor executor, long writeLimit, long readLimit) {
        super();
        init(objectSizeEstimator, executor, writeLimit, readLimit, DEFAULT_CHECK_INTERVAL);
    }

    /**
     * Constructor using default {@link ObjectSizeEstimator} and using NO LIMIT and default Check Interval
     *
     * @param executor
     *          created for instance like Executors.newCachedThreadPool
     */
    public AbstractTrafficShapingHandler(Executor executor) {
        super();
        init(new DefaultObjectSizeEstimator(), executor, 0, 0, DEFAULT_CHECK_INTERVAL);
    }

    /**
     * Constructor using the specified ObjectSizeEstimator and using NO LIMIT and default Check Interval
     *
     * @param objectSizeEstimator
     *            the {@link ObjectSizeEstimator} that will be used to compute
     *            the size of the message
     * @param executor
     *          created for instance like Executors.newCachedThreadPool
     */
    public AbstractTrafficShapingHandler(ObjectSizeEstimator objectSizeEstimator, Executor executor) {
        super();
        init(objectSizeEstimator, executor, 0, 0, DEFAULT_CHECK_INTERVAL);
    }

    /**
     * Constructor using default {@link ObjectSizeEstimator} and using NO LIMIT
     *
     * @param executor
     *          created for instance like Executors.newCachedThreadPool
     * @param checkInterval
     *          The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed
     */
    public AbstractTrafficShapingHandler(Executor executor, long checkInterval) {
        super();
        init(new DefaultObjectSizeEstimator(), executor, 0, 0, checkInterval);
    }

    /**
     * Constructor using the specified ObjectSizeEstimator and using NO LIMIT
     *
     * @param objectSizeEstimator
     *            the {@link ObjectSizeEstimator} that will be used to compute
     *            the size of the message
     * @param executor
     *          created for instance like Executors.newCachedThreadPool
     * @param checkInterval
     *          The delay between two computations of performances for
     *            channels or 0 if no stats are to be computed
     */
    public AbstractTrafficShapingHandler(ObjectSizeEstimator objectSizeEstimator, Executor executor, long checkInterval) {
        super();
        init(objectSizeEstimator, executor, 0, 0, checkInterval);
    }

    /**
     * Change the underlying limitations and check interval.
     *
     * @param newWriteLimit
     * @param newReadLimit
     * @param newCheckInterval
     */
    public void configure(long newWriteLimit, long newReadLimit, long newCheckInterval) {
        this.configure(newWriteLimit, newReadLimit);
        this.configure(newCheckInterval);
    }

    /**
     * Change the underlying limitations.
     *
     * @param newWriteLimit
     * @param newReadLimit
     */
    public void configure(long newWriteLimit, long newReadLimit) {
        writeLimit = newWriteLimit;
        readLimit = newReadLimit;
        if (trafficCounter != null) {
            trafficCounter.resetAccounting(System.currentTimeMillis() + 1);
        }
    }

    /**
     * Change the check interval.
     *
     * @param newCheckInterval
     */
    public void configure(long newCheckInterval) {
        checkInterval = newCheckInterval;
        if (trafficCounter != null) {
            trafficCounter.configure(checkInterval);
        }
    }

    /**
     * Called each time the accounting is computed from the TrafficCounters.
     * This method could be used for instance to implement almost real time accounting.
     *
     * @param counter
     *            the TrafficCounter that computes its performance
     */
    protected void doAccounting(TrafficCounter counter) {
    }

    /**
     * Class to implement setReadable at fix time
     *
     */
    private class ReopenRead implements Runnable {

        /**
         * Associated ChannelHandlerContext
         */
        private final ChannelHandlerContext ctx;

        /**
         * Time to wait before clearing the channel
         */
        private final long timeToWait;

        /**
         * @param ctx
         *            the associated channelHandlerContext
         * @param timeToWait
         */
        protected ReopenRead(ChannelHandlerContext ctx, long timeToWait) {
            this.ctx = ctx;
            this.timeToWait = timeToWait;
        }

        /**
         * Truly run the waken up of the channel
         */
        public void run() {
            try {
                if (release.get()) {
                    return;
                }
                Thread.sleep(timeToWait);
            } catch (InterruptedException e) {
                return;
            }
            if (ctx != null && ctx.getChannel() != null && ctx.getChannel().isConnected()) {
                ctx.setAttachment(null);
                ctx.getChannel().setReadable(true);
            }
        }
    }

    /**
    *
    * @return the time that should be necessary to wait to respect limit. Can
    *         be negative time
    */
    private long getTimeToWait(long limit, long bytes, long lastTime, long curtime) {
        long interval = curtime - lastTime;
        if (interval == 0) {
            return 0;
        }
        return bytes * 1000 / limit - interval;
    }

    @Override
    public void messageReceived(ChannelHandlerContext arg0, MessageEvent arg1) throws Exception {
        try {
            long curtime = System.currentTimeMillis();
            long size = objectSizeEstimator.estimateSize(arg1.getMessage());
            if (trafficCounter != null) {
                trafficCounter.bytesRecvFlowControl(arg0, size);
                if (readLimit == 0) {
                    return;
                }
                long wait = getTimeToWait(readLimit, trafficCounter.getCurrentReadBytes(), trafficCounter.getLastTime(), curtime);
                if (wait > MINIMAL_WAIT) {
                    Channel channel = arg0.getChannel();
                    if (channel != null && channel.isConnected()) {
                        if (executor == null) {
                            if (release.get()) {
                                return;
                            }
                            Thread.sleep(wait);
                            return;
                        }
                        if (arg0.getAttachment() == null) {
                            arg0.setAttachment(Boolean.TRUE);
                            channel.setReadable(false);
                            executor.execute(new ReopenRead(arg0, wait));
                        } else {
                            if (release.get()) {
                                return;
                            }
                            Thread.sleep(wait);
                        }
                    } else {
                        if (release.get()) {
                            return;
                        }
                        Thread.sleep(wait);
                    }
                }
            }
        } finally {
            super.messageReceived(arg0, arg1);
        }
    }

    @Override
    public void writeRequested(ChannelHandlerContext arg0, MessageEvent arg1) throws Exception {
        try {
            long curtime = System.currentTimeMillis();
            long size = objectSizeEstimator.estimateSize(arg1.getMessage());
            if (trafficCounter != null) {
                trafficCounter.bytesWriteFlowControl(size);
                if (writeLimit == 0) {
                    return;
                }
                long wait = getTimeToWait(writeLimit, trafficCounter.getCurrentWrittenBytes(), trafficCounter.getLastTime(), curtime);
                if (wait > MINIMAL_WAIT) {
                    if (release.get()) {
                        return;
                    }
                    Thread.sleep(wait);
                }
            }
        } finally {
            super.writeRequested(arg0, arg1);
        }
    }

    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof ChannelStateEvent) {
            ChannelStateEvent cse = (ChannelStateEvent) e;
            if (cse.getState() == ChannelState.INTEREST_OPS && (((Integer) cse.getValue()).intValue() & Channel.OP_READ) != 0) {
                boolean readSuspended = ctx.getAttachment() != null;
                if (readSuspended) {
                    e.getFuture().setSuccess();
                    return;
                }
            }
        }
        super.handleDownstream(ctx, e);
    }

    /**
     *
     * @return the current TrafficCounter (if
     *         channel is still connected)
     */
    public TrafficCounter getTrafficCounter() {
        return trafficCounter;
    }

    public void releaseExternalResources() {
        if (trafficCounter != null) {
            trafficCounter.stop();
        }
        release.set(true);
        ExecutorUtil.terminate(executor);
    }

    @Override
    public String toString() {
        return "TrafficShaping with Write Limit: " + writeLimit + " Read Limit: " + readLimit + " and Counter: " + (trafficCounter != null ? trafficCounter.toString() : "none");
    }
}

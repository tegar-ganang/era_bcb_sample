package openr66.protocol.networkhandler;

import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import java.util.concurrent.Executor;
import org.jboss.netty.handler.traffic.ChannelTrafficShapingHandler;
import org.jboss.netty.handler.traffic.TrafficCounter;
import org.jboss.netty.util.ObjectSizeEstimator;

/**
 * @author Frederic Bregier
 *
 */
public class ChannelTrafficHandler extends ChannelTrafficShapingHandler {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(ChannelTrafficHandler.class);

    /**
     * @param objectSizeEstimator
     * @param executor
     * @param writeLimit
     * @param readLimit
     * @param checkInterval
     */
    public ChannelTrafficHandler(ObjectSizeEstimator objectSizeEstimator, Executor executor, long writeLimit, long readLimit, long checkInterval) {
        super(objectSizeEstimator, executor, writeLimit, readLimit, checkInterval);
    }

    @SuppressWarnings("unused")
    @Override
    protected void doAccounting(TrafficCounter counter) {
        if (false) logger.debug(this.toString() + "\n   {}", counter);
    }
}

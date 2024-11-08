package openr66.protocol.networkhandler;

import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import java.util.concurrent.Executor;
import org.jboss.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.jboss.netty.handler.traffic.TrafficCounter;
import org.jboss.netty.util.ObjectSizeEstimator;

/**
 * Global function
 * @author Frederic Bregier
 *
 */
public class GlobalTrafficHandler extends GlobalTrafficShapingHandler {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(GlobalTrafficHandler.class);

    /**
     * @param objectSizeEstimator
     * @param executor
     * @param writeLimit
     * @param readLimit
     * @param checkInterval
     */
    public GlobalTrafficHandler(ObjectSizeEstimator objectSizeEstimator, Executor executor, long writeLimit, long readLimit, long checkInterval) {
        super(objectSizeEstimator, executor, writeLimit, readLimit, checkInterval);
    }

    @SuppressWarnings("unused")
    @Override
    protected void doAccounting(TrafficCounter counter) {
        if (false) logger.debug(this.toString() + "\n   {}", counter);
    }
}

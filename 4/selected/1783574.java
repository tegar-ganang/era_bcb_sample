package org.coos.messaging.routing;

import org.coos.messaging.Link;
import org.coos.messaging.Message;
import org.coos.messaging.ProcessorException;
import org.coos.messaging.ProcessorInterruptException;
import org.coos.messaging.Service;
import org.coos.messaging.impl.DefaultMessage;
import org.coos.messaging.impl.DefaultProcessor;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.management.ObjectName;

/**
 * @author Knut Eilif Husa, Tellu AS
 *
 */
public class LatencyAnalyzer extends DefaultProcessor implements RouterProcessor, Service {

    private static final String MESSAGE_NAME_PING = "pingLatency";

    private static final String MESSAGE_NAME_PONG = "pongLatency";

    private static final String QOSCLASS_LATENCY = "latency";

    private static final String PROPERTY_UPDATE_COST = "updateCost";

    private static final String PROPERTY_MEASURE_INTERVAL = "updateInterval";

    private static final String PROPERTY_LATENCY_ANALYZER_NAME = "analyzerName";

    @SuppressWarnings("unused")
    private String analyzerName = "latencyAnalyzer";

    private boolean updateCost = false;

    private Router router;

    private long samples = 0;

    private double meanLatency;

    private long measureInterval = 0;

    private Timer timer;

    ObjectName name;

    public void processMessage(Message msg) throws ProcessorException {
        if (msg.getHeader(Message.TYPE).equals(Message.TYPE_ANALYZE)) {
            Link inLink = msg.getMessageContext().getInBoundLink();
            if (msg.getHeader(Message.MESSAGE_NAME).equals(MESSAGE_NAME_PONG)) {
                String ts = msg.getHeader(Message.TIME_STAMP);
                long latency = (System.currentTimeMillis() - Long.parseLong(ts)) / 2;
                logger.debug("latency: " + latency);
                if (updateCost) {
                    inLink.setCost(QOSCLASS_LATENCY, (int) latency);
                }
                meanLatency = ((meanLatency * samples) + latency) / (samples + 1);
                samples++;
            } else if (msg.getHeader(Message.MESSAGE_NAME).equals(MESSAGE_NAME_PING)) {
                msg.setHeader(Message.MESSAGE_NAME, MESSAGE_NAME_PONG);
                inLink.getChannel().getOutLink().processMessage(msg);
            }
            throw new ProcessorInterruptException();
        }
    }

    public void start() throws Exception {
        if (measureInterval != 0) {
            timer = new Timer("LatencyAnalyzerTimer", true);
            timer.schedule(new LatencyAnalyzerTask(), measureInterval);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setProperties(Hashtable properties) {
        super.setProperties(properties);
        if (properties.get(PROPERTY_LATENCY_ANALYZER_NAME) != null) {
            analyzerName = (String) properties.get(PROPERTY_LATENCY_ANALYZER_NAME);
        }
        String measureIntervalStr = (String) properties.get(PROPERTY_MEASURE_INTERVAL);
        if (measureIntervalStr != null) {
            measureInterval = Long.parseLong(measureIntervalStr);
        }
        if ((properties.get(PROPERTY_UPDATE_COST) != null) && ((String) properties.get(PROPERTY_UPDATE_COST)).equalsIgnoreCase("true")) {
            updateCost = true;
        }
    }

    public void stop() throws Exception {
        if (timer != null) {
            timer.cancel();
        }
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    public double getRoundTripLatency() {
        return meanLatency;
    }

    private class LatencyAnalyzerTask extends TimerTask {

        @Override
        public void run() {
            Map<String, Link> links = router.getLinks();
            for (Link link : links.values()) {
                Message ping = new DefaultMessage();
                ping.setHeader(Message.TYPE, Message.TYPE_ANALYZE);
                ping.setHeader(Message.MESSAGE_NAME, MESSAGE_NAME_PING);
                ping.setHeader(Message.TIME_STAMP, String.valueOf(System.currentTimeMillis()));
                try {
                    if (link.getChannel() != null) {
                        link.processMessage(ping);
                    }
                } catch (ProcessorException e) {
                    logger.error("Exception in Latency Analyzer ignored.", e);
                }
            }
            try {
                timer.schedule(new LatencyAnalyzerTask(), measureInterval);
            } catch (IllegalStateException e) {
            }
        }
    }
}

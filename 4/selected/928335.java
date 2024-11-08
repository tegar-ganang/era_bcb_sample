package com.ewansilver.raindrop;

import java.util.LinkedList;
import java.util.List;
import com.ewansilver.concurrency.Channel;
import com.ewansilver.concurrency.ChannelFactory;

/**
 * A ResponseTimeMonitor keeps track of the throughput response times for a
 * stage and provides feedback to interested parties on how the throughput is
 * evolving over time.
 * 
 * The actual throughput time calculated depends upon the type of
 * ResponseTimeCalculator being used. The default one monitors the 90th
 * percentile.
 * 
 * @author ewan.silver AT gmail.com
 */
public class ResponseTimeMonitor implements ResponseTimeListener {

    private static final ResponseTimeListener[] EMPTY_RESPONSE_TIME_LISTENER_ARRAY = new ResponseTimeListener[0];

    private List responseTimeListeners;

    private ResponseTimeCalculator calculator;

    private long responseTime;

    private int calculationLimit;

    private Channel throughputChannel;

    private Channel responseMonitoringChannel;

    private long lastThroughputTime;

    /**
	 * Constructor.
	 * @param aResponseMonitoringChannel
	 */
    public ResponseTimeMonitor(Channel aResponseMonitoringChannel) {
        responseMonitoringChannel = aResponseMonitoringChannel;
        responseTime = 0;
        calculator = new ResponseTimeCalculator(90);
        calculationLimit = 100;
        throughputChannel = ChannelFactory.instance().getChannel();
        lastThroughputTime = 0;
        responseTimeListeners = new LinkedList();
    }

    public ResponseTimeCalculator getResponseTimeCalculator() {
        return calculator;
    }

    /**
	 * Log the ThroughputEvent and keep track of when it occured.
	 * 
	 * @param event
	 */
    public synchronized void logThroughputEvent(ThroughputEvent event) {
        try {
            throughputChannel.put(event);
            lastThroughputTime = System.currentTimeMillis();
            if (throughputChannel.size() >= calculationLimit) {
                responseMonitoringChannel.put(new ResponseTimeMonitorEvent(this, throughputChannel));
                throughputChannel = ChannelFactory.instance().getChannel();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Returns the time the last throughput event occured.
	 */
    public long getLastThroughoutEventTime() {
        return lastThroughputTime;
    }

    /**
	 * Returns the time, in ms, that throughputs are taking to pass through the
	 * stage being monitored.
	 * 
	 */
    public long getResponseTime() {
        return responseTime;
    }

    public void responseTimeChanged(long aResponseTime) {
        ResponseTimeListener[] array = (ResponseTimeListener[]) responseTimeListeners.toArray(EMPTY_RESPONSE_TIME_LISTENER_ARRAY);
        int number = array.length;
        for (int i = 0; i < number; i++) {
            array[i].responseTimeChanged(aResponseTime);
        }
        responseTime = aResponseTime;
    }

    /**
	 * Add the ResponseTimeListener to the list of ResponseTimeListeners
	 * interested in this ResponseTime.
	 * 
	 * If the ResponseTimeListener is already registered it is not registered
	 * again.
	 */
    public synchronized void addResponseTimeListener(ResponseTimeListener aListener) {
        if (!responseTimeListeners.contains(aListener)) responseTimeListeners.add(aListener);
    }

    /**
	 * Remove the ResponseTimeListener from the internal list, if it exists.
	 * 
	 * @param aListener
	 *            the ResponseTimeListener to be removed.
	 */
    public void removeResponseTimeListener(ResponseTimeListener aListener) {
        responseTimeListeners.remove(aListener);
    }
}

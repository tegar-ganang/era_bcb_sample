package com.ewansilver.raindrop;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ewansilver.concurrency.Channel;
import com.ewansilver.concurrency.ChannelFactory;

/**
 * The ResponseTimeMonitorThread is responsible for keeping track of the
 * ResponseTimes for all the stages and updating the ResponseTime figures for
 * them.
 * 
 * 
 * @author ewan.silver AT gmail.com
 */
public class ResponseTimeMonitorThread implements Runnable {

    /**
	 * Logger.
	 */
    private static Logger logger = Logger.getLogger(ResponseTimeMonitorThread.class.getName());

    private Channel channel;

    private List monitors;

    /**
	 * Constructor.
	 */
    public ResponseTimeMonitorThread() {
        channel = ChannelFactory.instance().getChannel();
        monitors = new LinkedList();
    }

    /**
	 * Add the supplied monitor to the list of monitors that this thread will
	 * keep a track of.
	 */
    public synchronized void addMonitor(ResponseTimeMonitor aMonitor) {
        if (!monitors.contains(aMonitor)) monitors.add(aMonitor);
    }

    /**
	 * @return the Channel
	 */
    public Channel getChannel() {
        return channel;
    }

    public void run() {
        while (true) {
            try {
                logger.fine("Waiting to get a ResponseTimeMonitorEvent.");
                ResponseTimeMonitorEvent event = (ResponseTimeMonitorEvent) channel.take();
                logger.fine("Taken a ResponseTimeMonitorEvent.");
                Channel channel = event.getChannel();
                int size = channel.size();
                ThroughputEvent[] events = new ThroughputEvent[size];
                for (int i = 0; i < size; i++) {
                    events[i] = (ThroughputEvent) channel.take();
                }
                ResponseTimeCalculator average = event.getMonitor().getResponseTimeCalculator();
                average.updateThroughputTime(events);
                long throughput = average.getResponseTime();
                logger.fine("Average throughput time: " + throughput);
                event.getMonitor().responseTimeChanged(throughput);
            } catch (InterruptedException e) {
                logger.log(Level.INFO, "A problem occurred whilst trying to calculate the average throughput stats.", e);
            }
        }
    }
}

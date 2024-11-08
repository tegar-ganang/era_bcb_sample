package com.ewansilver.raindrop;

import com.ewansilver.concurrency.Channel;

/**
 * Wrapper class to transport the ResponseTimeMonitor and its associated
 * throughput channel to the thread that calculates the throughput rate.
 * 
 * @author ewan.silver AT gmail.com
 */
public class ResponseTimeMonitorEvent {

    private Channel channel;

    private ResponseTimeMonitor monitor;

    /**
	 * Constructor.
	 * 
	 * @param aMonitor the ResponseTimeMonitor.
	 * @param aChannel the Channel that belongs to this channel.
	 */
    public ResponseTimeMonitorEvent(ResponseTimeMonitor aMonitor, Channel aChannel) {
        monitor = aMonitor;
        channel = aChannel;
    }

    /**
	 * The Channel connected to this event.
	 * 
	 * @return the associated channel.
	 */
    public Channel getChannel() {
        return channel;
    }

    /**
	 * The ResponseTimeMonitor connected to this event.
	 * @return the associated ResponseTimeMonitor.
	 */
    public ResponseTimeMonitor getMonitor() {
        return monitor;
    }
}

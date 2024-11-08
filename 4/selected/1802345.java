package org.simpleframework.http;

class DelayEvent implements Event {

    private Collector collector;

    private Channel channel;

    public DelayEvent(Collector collector, Channel channel) {
        this.collector = collector;
        this.channel = channel;
    }

    public Collector getCollector() {
        return collector;
    }

    public Channel getChannel() {
        return channel;
    }

    public Rate getRate() {
        int value = collector.getThrottle();
        return Rate.getRate(value);
    }

    public long getDuration() {
        return getRate().getDelay();
    }
}

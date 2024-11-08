package hu.sztaki.lpds.monitor.tracefile;

/**
 * <p>Title: MetricNChannelPair</p>
 * <p>Description: Represents a metric ID and channel ID pair.</p>
 */
public class MetricNChannelPair {

    private int mid, channel;

    /**
   * Constructor.
   * @param mid Metric ID.
   * @param channel Channel ID.
   */
    public MetricNChannelPair(int mid, int channel) {
        this.mid = mid;
        this.channel = channel;
    }

    /**
   * Getter.
   * @return Metric ID.
   */
    public int getMid() {
        return this.mid;
    }

    /**
   * Getter,
   * @return Channel ID.
   */
    public int getChannel() {
        return this.channel;
    }
}

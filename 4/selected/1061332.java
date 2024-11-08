package gov.sns.ca;

/**
 * Monitor
 *
 * @author  Christopher K. Allen
 * @author Tom Pelaia
 * @version 1.0
 */
public abstract class Monitor {

    /** The monitor is triggered when the PV value change. */
    public static final int VALUE = 1;

    /** The monitor is triggered when the PV log value change. */
    public static final int LOG = 2;

    /** The monitor is triggered when the PV alarm state change. */
    public static final int ALARM = 4;

    protected boolean m_bolMonitoring;

    protected int m_intMaskEvent;

    protected Channel m_xalChan;

    /** 
     *  Creates new Monitor
     *  @param  chan        Channel object to monitor
     *  @param  intMaskEvent code specifying when monitor event is fired
     */
    protected Monitor(Channel chan, int intMaskEvent) {
        m_bolMonitoring = false;
        m_intMaskEvent = intMaskEvent;
        m_xalChan = chan;
    }

    /**
     *  Stop the monitoring of PV
     *  <p>
     *  Note that the implementation of this method
     *  must be thread-safe because this also called by the finalizer.
     */
    public abstract void clear();

    /**
     *  Return the associated Channel object
     *  @return channel being monitored
     */
    public Channel getChannel() {
        return m_xalChan;
    }

    /**
     *  Make sure monitoring is shut down before destruction
     */
    @Override
    protected void finalize() {
        clear();
    }

    /**
     * Post the value record to the listener.
     * @param listener The object receiving the monitor record.
     * @param adaptor The adaptor to the internal data record.
     */
    protected final void post(IEventSinkValue listener, ValueAdaptor adaptor) {
        ChannelRecord record = new ChannelRecord(adaptor);
        record.applyTransform(m_xalChan.getValueTransform());
        listener.eventValue(record, m_xalChan);
    }

    /**
     * Post the value-status record to the listener.
     * @param listener The object receiving the monitor record.
     * @param adaptor The adaptor to the internal data record.
     */
    protected final void post(IEventSinkValStatus listener, StatusAdaptor adaptor) {
        ChannelStatusRecord record = new ChannelStatusRecord(adaptor);
        record.applyTransform(m_xalChan.getValueTransform());
        listener.eventValue(record, m_xalChan);
    }

    /**
     * Post the value-status-timestamp record to the listener.
     * @param listener The object receiving the monitor record.
     * @param adaptor The adaptor to the internal data record.
     */
    protected final void post(IEventSinkValTime listener, TimeAdaptor adaptor) {
        ChannelTimeRecord record = new ChannelTimeRecord(adaptor);
        record.applyTransform(m_xalChan.getValueTransform());
        listener.eventValue(record, m_xalChan);
    }
}

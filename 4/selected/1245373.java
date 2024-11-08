package gov.sns.xal.smf;

import gov.sns.tools.*;
import gov.sns.tools.data.*;
import gov.sns.xal.smf.data.*;
import gov.sns.ca.*;
import java.util.*;

/**
 * TimingCenter holds the timing channels for the accelerator.
 *
 * @author  tap
 */
public class TimingCenter implements DataListener {

    public static final String DATA_LABEL = "timing";

    /** channel suite associated with this node */
    protected ChannelSuite _channelSuite;

    /** beam trigger PV: 0=Trigger, 1=Counting */
    public static final String TRIGGER_HANDLE = "trigger";

    /** beam trigger mode PV: 0=Continuous, 1=Single-shot */
    public static final String MODE_HANDLE = "mode";

    /** specify how many beam pulse(s) */
    public static final String COUNTDOWN_HANDLE = "countDown";

    /** readback while triggered beam pulses are counting down */
    public static final String COUNT_HANDLE = "count";

    /** readback of overall rep rate */
    public static final String REP_RATE_HANDLE = "repRate";

    /** beam on event */
    public static final String BEAM_ON_EVENT_HANDLE = "beamOnEvent";

    /** beam on event counter */
    public static final String BEAM_ON_EVENT_COUNT_HANDLE = "beamOnEventCount";

    /** diagnostic demand event */
    public static final String DIAGNOSTIC_DEMAND_EVENT_HANDLE = "diagnosticDemandEvent";

    /** diagnostic demand event counter */
    public static final String DIAGNOSTIC_DEMAND_EVENT_COUNT_HANDLE = "diagnosticDemandEventCount";

    /** slow (1 Hz) diagnostic event */
    public static final String SLOW_DIAGNOSTIC_EVENT_HANDLE = "slowDiagnosticEvent";

    /** slow (1 Hz) diagnostic event counter */
    public static final String SLOW_DIAGNOSTIC_EVENT_COUNT_HANDLE = "slowDiagnosticEventCount";

    /** fast (6 Hz) diagnostic event */
    public static final String FAST_DIAGNOSTIC_EVENT_HANDLE = "fastDiagnosticEvent";

    /** fast (6 Hz) diagnostic event counter */
    public static final String FAST_DIAGNOSTIC_EVENT_COUNT_HANDLE = "fastDiagnosticEventCount";

    /** readback of the ring frequency in MHz */
    public static final String RING_FREQUENCY_HANDLE = "ringFrequency";

    /** Machine Mode */
    public static final String MACHINE_MODE_HANDLE = "machineMode";

    /**
	 * Create an empty TimingCenter
	 */
    public TimingCenter() {
        _channelSuite = new ChannelSuite();
    }

    /**
	 * Get the default TimingCenter corresponding to the user's default main optics source
	 * @return the default TimingCenter or null if no default has been specified
	 * @throws gov.sns.tools.ExceptionWrapper if an exception occurs while parsing the data source
	 */
    public static TimingCenter getDefaultTimingCenter() throws ExceptionWrapper {
        XMLDataManager dataManager = XMLDataManager.getDefaultInstance();
        return (dataManager != null) ? dataManager.getTimingCenter() : null;
    }

    /** 
     * dataLabel() provides the name used to identify the class in an 
     * external data source.
     * @return a tag that identifies the receiver's type
     */
    public String dataLabel() {
        return DATA_LABEL;
    }

    /**
     * Update the data based on the information provided by the data provider.
     * @param adaptor The adaptor from which to update the data
     */
    public void update(DataAdaptor adaptor) {
        DataAdaptor suiteAdaptor = adaptor.childAdaptor(ChannelSuite.DATA_LABEL);
        if (suiteAdaptor != null) {
            _channelSuite.update(suiteAdaptor);
        }
    }

    /**
     * Write data to the data adaptor for storage.
     * @param adaptor The adaptor to which the receiver's data is written
     */
    public void write(DataAdaptor adaptor) {
        adaptor.writeNode(_channelSuite);
    }

    /**
	 * Get this timing center's channel suite
	 * @return this timing center's channel suite
	 */
    public ChannelSuite getChannelSuite() {
        return _channelSuite;
    }

    /** accessor to channel suite handles */
    public Collection<String> getHandles() {
        return _channelSuite.getHandles();
    }

    /** 
	 * this method returns the Channel object of this timing center, associated with
     * the specified channel handle. Note - xal interacts with EPICS via Channel objects.
     * @param chanHandle The handle to the desired channel stored in the channel suite
     */
    public Channel getChannel(String chanHandle) throws NoSuchChannelException {
        Channel channel = _channelSuite.getChannel(chanHandle);
        if (channel == null) {
            throw new NoSuchChannelException(this, chanHandle);
        }
        return channel;
    }

    /**
     * Get the channel corresponding to the specified handle and connect it. 
     * @param handle The handle for the channel to get.
     * @return The channel associated with this node and the specified handle or null if there is no match.
     * @throws gov.sns.xal.smf.NoSuchChannelException if no such channel as specified by the handle is associated with this node.
     * @throws gov.sns.ca.ConnectionException if the channel cannot be connected
     */
    public Channel getAndConnectChannel(String handle) throws NoSuchChannelException, ConnectionException {
        Channel channel = getChannel(handle);
        channel.connectAndWait();
        return channel;
    }
}

package gov.sns.ca;

import gov.sns.tools.ArrayValue;
import gov.sns.tools.messaging.MessageCenter;
import gov.sns.tools.transforms.ValueTransform;

/**
 * Channel is an abstract high level XAL wrapper for a native process variable (PV) channel.
 * Subclasses provide native implementations.
 *
 * @author  Christopher K. Allen
 * @author Tom Pelaia
 * @version 1.1
 */
public abstract class Channel {

    /** Static variables */
    protected static final ChannelSystem channelSystem;

    /**  Local Attributes */
    protected String m_strId;

    protected double m_dblTmIO;

    protected double m_dblTmEvt;

    private ValueTransform valueTransform;

    /** Notify listeners when connection is made or dropped */
    protected ConnectionListener connectionProxy;

    /** One Message Center for all Channel events */
    protected static MessageCenter messageCenter;

    /** hold connection status */
    protected volatile boolean connectionFlag;

    static {
        channelSystem = ChannelFactory.defaultSystem();
        messageCenter = new MessageCenter("Channel Message Center");
    }

    /** flush IO requests */
    public static void flushIO() {
        channelSystem.flushIO();
    }

    /**
     *  Flush the EPICS Channel Access request buffer and return as soon as complete or timeout has
     *  expired.  
     *
     *  Must use a pendX() function if synchronous request queuing is on!
     *
     *  Requests include Channel.connect() and Channel.getVal()
     *  @param          dblTimeOut      time to wait before giving up
     *  @return                         false if time out occurs
     */
    public static boolean pendIO(double timeout) {
        return channelSystem.pendIO(timeout);
    }

    /**
     *  Flush the EPICS Channel Access request buffer and wait for asyncrhonous event.  This function
     *  blocks until the time out has expired!  Neither will it return until the channel access queue
     *  has been processed.
     *
     *  Must use a pendX() function if synchronous request queuing is on!
     *
     *  Requests include Channel.connectCallback Channel.getValCallback(), Channel.putValCallback and
     *  all monitor events.
     *  @param          dblTimeOut      time to wait before giving up
     */
    public static void pendEvent(double timeout) {
        channelSystem.pendEvent(timeout);
    }

    /**  Creates empty Channel */
    protected Channel() {
        this(null);
    }

    /** 
     *  Creates new Channel 
     *  @param  name     EPICS channel name
     */
    protected Channel(String name) {
        this(name, ValueTransform.noOperationTransform);
    }

    /**
     * Create a new Channel
     * @param name The EPICS PV name
     * @param aTransform The transform to apply to PV values
     */
    protected Channel(String name, ValueTransform aTransform) {
        connectionFlag = false;
        m_strId = name;
        setValueTransform(aTransform);
    }

    /**
     * From the default channel factory, get a channel for the specified signal name.
     * @param signalName the PV for which to get the channel
     * @return a channel for the specified PV
     */
    public static Channel getInstance(final String signalName) {
        return ChannelFactory.defaultFactory().getChannel(signalName);
    }

    /**
     * From the default channel factory, get a channel for the specified signal name and value transform.
     * @param signalName the PV for which to get the channel
     * @return a channel for the specified PV
     */
    public static Channel getInstance(final String signalName, final ValueTransform transform) {
        return ChannelFactory.defaultFactory().getChannel(signalName, transform);
    }

    /**
     * Set a value transform for this channel.
     * @param aTransform The transform to use for this channel.
     */
    public void setValueTransform(ValueTransform aTransform) {
        valueTransform = aTransform;
    }

    /**
     * Get the value transform applied to this channel.
     * @return The value transform applied to this channel.
     */
    public ValueTransform getValueTransform() {
        return valueTransform;
    }

    /** Add a listener of connection changes */
    public void addConnectionListener(ConnectionListener listener) {
        if (connectionProxy == null) {
            connectionProxy = messageCenter.registerSource(this, ConnectionListener.class);
        }
        messageCenter.registerTarget(listener, this, ConnectionListener.class);
        if (isConnected()) listener.connectionMade(this);
    }

    /** Remove a listener of connection changes */
    public void removeConnectionListener(ConnectionListener listener) {
        messageCenter.removeTarget(listener, this, ConnectionListener.class);
    }

    /**
     * Return a unique identifier of this channel so as to distinguish 
     * channels which share the same PV but have different transforms.
     * @return A channel identifier built from the PV and value transform
     */
    public String getId() {
        if (valueTransform != ValueTransform.noOperationTransform) {
            return channelName() + "_transform" + valueTransform.hashCode();
        } else {
            return channelName();
        }
    }

    /**
     *  Returns EPICS channel name for process variable
     *  @return     string descriptor for EPICS channel
     */
    public String channelName() {
        return m_strId;
    }

    /**
     *  Set the EPICS channel name for the connection
     *  @param  strNameChan     EPICS channel name
     */
    public void setChannelName(String strNameChan) {
        m_strId = strNameChan;
    }

    /**
     * Set synchronized request queueing (via pendX() functions)
     * @param  bol     turn external request queueing on/off
     * @deprecated
     */
    @Deprecated
    public static void setSyncRequest(boolean syncFlag) {
        channelSystem.setSyncRequest(syncFlag);
    }

    /**
     * Determine whether requests are synchronized or not
     * @return true if requests are synchronized and false if not
     * @deprecated
     */
    @Deprecated
    public static boolean getSyncRequest() {
        return channelSystem.willSyncRequest();
    }

    public static synchronized void setDebugMode(boolean bDebug) {
        channelSystem.setDebugMode(bDebug);
    }

    /**
     *  Set the channel access Pend IO timeout
     *  @param  dblTm       I/O timeout
     */
    public void setIoTimeout(double dblTm) {
        m_dblTmIO = dblTm;
    }

    /**
     *  Set the channel access Pend Event timeout
     *  @param  dblTm       event timeout
     */
    public void setEventTimeout(double dblTm) {
        m_dblTmEvt = dblTm;
    }

    /**
     *  Get the channel access Pend IO timeout
     *  @return       I/O timeout
     */
    public double getIoTimeout() {
        return m_dblTmIO;
    }

    /**
     *  Get the channel access Pend Event timeout
     *  @return       event timeout
     */
    public double getEventTimeout() {
        return m_dblTmEvt;
    }

    /**
     * Request a connection and continue without waiting if the channel system is set to sycn requests or wait if it is not.
     * @return false if connection could not be established or the channel system is set to sync requests
     * @deprecated use connectAndWait() or requestConnection() instead
     */
    @Deprecated
    public boolean connect() {
        if (channelSystem.willSyncRequest()) {
            requestConnection();
            return false;
        } else {
            return connectAndWait(m_dblTmIO);
        }
    }

    /**
     * Same as connect()
     * @return false if connection could not be established
     * @deprecated use connectAndWait() or requestConnection() instead
     */
    @Deprecated
    public boolean connect_async() {
        return connect();
    }

    /**
     * Connect and wait the default timeout.
     * @return true if the connection was made within the timeout and false if not
     */
    public boolean connectAndWait() {
        return connectAndWait(m_dblTmIO);
    }

    /**
     * Request a new connection and wait for it no longer than the timeout.
     * @return true if the connection was made within the timeout and false if not
     */
    public abstract boolean connectAndWait(final double timeout);

    /**
     * Request that the channel be connected.  Connections are made in the background
     * so this method returns immediately upon making the request.  The connection will be
     * made in the future as soon as possible.  A connection event will be sent to registered
     * connection listeners when the connection has been established.
     */
    public abstract void requestConnection();

    /**
     *  Terminate the network channel connection and clear all events associated
     *  with process variable
     */
    public abstract void disconnect();

    /**
     *  Checks if channel is connected to process variable
     *  @return     true if connected
     */
    public boolean isConnected() {
        return connectionFlag;
    }

    /**
     *  Checks for process variable channel connection and throws a ConnectionException if
     *  absent.
     */
    public void checkConnection() throws ConnectionException {
        if (!connect()) {
            throw new ConnectionException(this, "Channel Error - The channel \"" + m_strId + "\" must be connected to use this feature.");
        }
    }

    /**
     *  Checks for process variable channel connection and throws a ConnectionException if
     *  absent.
     *  @param  strFuncName     name of function using connection
     */
    protected void checkConnection(String strFuncName) throws ConnectionException {
        if (!connect()) {
            throw new ConnectionException(this, "Channel::" + strFuncName + " - The channel \"" + m_strId + "\" must be connected to use this feature.");
        }
    }

    /** get the Java class associated with the native type of this channel */
    public abstract Class<?> elementType() throws ConnectionException;

    /**
     *  Return size of value array associated with process variable
     *  @return     number of values in process variable
     */
    public abstract int elementCount() throws ConnectionException;

    /**
     *  Determine if channel has read access to process variable
     *  @return             true if channel has read access
     *
     *  @exception  ConnectionException     channel not connected
     */
    public abstract boolean readAccess() throws ConnectionException;

    /**
     *  Determine if channel has write access to process variable
     *  @return             true if channel has write access
     *
     *  @exception  ConnectionException     channel not connected
     */
    public abstract boolean writeAccess() throws ConnectionException;

    /** Convenience method which returns the units for this channel. */
    public abstract String getUnits() throws ConnectionException, GetException;

    /** Convenience method which returns the upper display limit. */
    public abstract Number rawUpperDisplayLimit() throws ConnectionException, GetException;

    /** Convenience method which returns the lower display limit. */
    public abstract Number rawLowerDisplayLimit() throws ConnectionException, GetException;

    /** Convenience method which returns the upper alarm limit. */
    public abstract Number rawUpperAlarmLimit() throws ConnectionException, GetException;

    /** Convenience method which returns the lower alarm limit. */
    public abstract Number rawLowerAlarmLimit() throws ConnectionException, GetException;

    /** Convenience method which returns the upper warning limit. */
    public abstract Number rawUpperWarningLimit() throws ConnectionException, GetException;

    /** Convenience method which returns the lower warning limit. */
    public abstract Number rawLowerWarningLimit() throws ConnectionException, GetException;

    /** Convenience method which returns the upper control limit. */
    public abstract Number rawUpperControlLimit() throws ConnectionException, GetException;

    /** Convenience method which returns the lower control limit. */
    public abstract Number rawLowerControlLimit() throws ConnectionException, GetException;

    /** Convenience method which returns the upper display limit. */
    public final Number upperDisplayLimit() throws ConnectionException, GetException {
        ArrayValue rawValue = ArrayValue.numberStore(rawUpperDisplayLimit());
        return valueTransform.convertFromRaw(rawValue);
    }

    /** Convenience method which returns the lower display limit. */
    public final Number lowerDisplayLimit() throws ConnectionException, GetException {
        ArrayValue rawValue = ArrayValue.numberStore(rawLowerDisplayLimit());
        return valueTransform.convertFromRaw(rawValue);
    }

    /** Convenience method which returns the upper alarm limit. */
    public final Number upperAlarmLimit() throws ConnectionException, GetException {
        ArrayValue rawValue = ArrayValue.numberStore(rawUpperAlarmLimit());
        return valueTransform.convertFromRaw(rawValue);
    }

    /** Convenience method which returns the lower alarm limit. */
    public final Number lowerAlarmLimit() throws ConnectionException, GetException {
        ArrayValue rawValue = ArrayValue.numberStore(rawLowerAlarmLimit());
        return valueTransform.convertFromRaw(rawValue);
    }

    /** Convenience method which returns the upper warning limit. */
    public final Number upperWarningLimit() throws ConnectionException, GetException {
        ArrayValue rawValue = ArrayValue.numberStore(rawUpperWarningLimit());
        return valueTransform.convertFromRaw(rawValue);
    }

    /** Convenience method which returns the lower warning limit. */
    public final Number lowerWarningLimit() throws ConnectionException, GetException {
        ArrayValue rawValue = ArrayValue.numberStore(rawLowerWarningLimit());
        return valueTransform.convertFromRaw(rawValue);
    }

    /** Convenience method which returns the upper control limit. */
    public final Number upperControlLimit() throws ConnectionException, GetException {
        ArrayValue rawValue = ArrayValue.numberStore(rawUpperControlLimit());
        return valueTransform.convertFromRaw(rawValue);
    }

    /** Convenience method which returns the lower control limit. */
    public final Number lowerControlLimit() throws ConnectionException, GetException {
        ArrayValue rawValue = ArrayValue.numberStore(rawLowerControlLimit());
        return valueTransform.convertFromRaw(rawValue);
    }

    /**
     *  Get channel value
     *  @return             value of the PV
     *
     *  @exception  ConnectionException channel not connected
     *  @exception  GetException        general channel access PV get failure
     */
    public byte getValByte() throws ConnectionException, GetException {
        return getValueRecord().byteValue();
    }

    public int getValEnum() throws ConnectionException, GetException {
        return getValueRecord().shortValue();
    }

    public int getValInt() throws ConnectionException, GetException {
        return getValueRecord().intValue();
    }

    public float getValFlt() throws ConnectionException, GetException {
        return getValueRecord().floatValue();
    }

    public double getValDbl() throws ConnectionException, GetException {
        return getValueRecord().doubleValue();
    }

    /**
     *  Get channel value as array
     *  @param  chan        the channel for desired PV
     *  @return             value array of the PV
     *
     *  @exception  ConnectionException channel not connected
     *  @exception  GetException        general channel access PV get failure
     */
    public byte[] getArrByte() throws ConnectionException, GetException {
        return getValueRecord().byteArray();
    }

    public int[] getArrInt() throws ConnectionException, GetException {
        return getValueRecord().intArray();
    }

    public float[] getArrFlt() throws ConnectionException, GetException {
        return getValueRecord().floatArray();
    }

    public double[] getArrDbl() throws ConnectionException, GetException {
        return getValueRecord().doubleArray();
    }

    /**
     * Fetch the data value for the channel and return it as an ArrayValue.
     */
    public ArrayValue getArrayValue() throws ConnectionException, GetException {
        return getValueRecord().arrayValue();
    }

    /**
     * Return a raw <code>ChannelRecord</code> representing the fetched record for the 
     * native type of this channel.  This is a convenient way to get the value of
     * the PV.
     */
    public abstract ChannelRecord getRawValueRecord() throws ConnectionException, GetException;

    public abstract ChannelRecord getRawValueRecord(int count) throws ConnectionException, GetException;

    /**
     * Return a raw <code>ChannelStatusRecord</code> representing the fetched record for the 
     * native type of this channel.  This is a convenient way to get the value of
     * the PV along with status.
     */
    public abstract ChannelStatusRecord getRawStatusRecord() throws ConnectionException, GetException;

    public abstract ChannelStatusRecord getRawStatusRecord(int count) throws ConnectionException, GetException;

    /**
     * Return a raw <code>ChannelTimeRecord</code> representing the fetched record for the 
     * native type of this channel.  This is a convenient way to get the value of
     * the PV along with status and timestamp.
     */
    public abstract ChannelTimeRecord getRawTimeRecord() throws ConnectionException, GetException;

    public abstract ChannelTimeRecord getRawTimeRecord(int count) throws ConnectionException, GetException;

    /**
     * Return a <code>ChannelRecord</code> representing the fetched record for the 
     * native type of this channel.  This is a convenient way to get the value of
     * the PV.
     */
    public final ChannelRecord getValueRecord() throws ConnectionException, GetException {
        return getRawValueRecord().applyTransform(valueTransform);
    }

    /**
     * Return a <code>ChannelStatusRecord</code> representing the fetched record for the 
     * native type of this channel.  This is a convenient way to get the value of
     * the PV along with status.
     */
    public final ChannelStatusRecord getStatusRecord() throws ConnectionException, GetException {
        ChannelStatusRecord record = getRawStatusRecord();
        record.applyTransform(valueTransform);
        return record;
    }

    /**
     * Return a <code>ChannelTimeRecord</code> representing the fetched record for the 
     * native type of this channel.  This is a convenient way to get the value of
     * the PV along with status and timestamp.
     */
    public final ChannelTimeRecord getTimeRecord() throws ConnectionException, GetException {
        ChannelTimeRecord record = getRawTimeRecord();
        record.applyTransform(valueTransform);
        return record;
    }

    public final ChannelTimeRecord getTimeRecord(int count) throws ConnectionException, GetException {
        ChannelTimeRecord record = getRawTimeRecord(count);
        record.applyTransform(valueTransform);
        return record;
    }

    /**
     * Handle a callback for getting the raw value for the channel.
     * @param listener The receiver of the callback.
     * @throws gov.sns.ca.ConnectionException
     * @throws gov.sns.ca.GetException
     */
    protected abstract void getRawValueCallback(final IEventSinkValue listener) throws ConnectionException, GetException;

    /**
     *  Get the value of the process variable via a callback to the specified listener.
     *  @param  listener     receiver of the callback event.
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.GetException            general channel access failure
     */
    public final void getValueCallback(final IEventSinkValue listener) throws ConnectionException, GetException {
        getRawValueCallback(new IEventSinkValue() {

            public void eventValue(final ChannelRecord record, final Channel channel) {
                listener.eventValue(record.applyTransform(valueTransform), Channel.this);
            }
        });
    }

    /**
     *  Get the value of the process variable via a callback to the specified listener.
     *  @param  listener     receiver of the callback event.
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.GetException            general channel access failure
     */
    public final void getValByteCallback(final IEventSinkValByte listener) throws ConnectionException, GetException {
        getRawValueCallback(new IEventSinkValue() {

            public void eventValue(final ChannelRecord record, Channel channel) {
                listener.eventValue(record.applyTransform(valueTransform).byteValue(), Channel.this);
            }
        });
    }

    /**
     *  Get the value of the process variable via a callback to the specified listener.
     *  @param  listener     receiver of the callback event.
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.GetException            general channel access failure
     */
    public final void getValIntCallback(final IEventSinkValInt listener) throws ConnectionException, GetException {
        getRawValueCallback(new IEventSinkValue() {

            public void eventValue(final ChannelRecord record, Channel channel) {
                listener.eventValue(record.applyTransform(valueTransform).intValue(), Channel.this);
            }
        });
    }

    /**
     *  Get the value of the process variable via a callback to the specified listener.
     *  @param  listener     receiver of the callback event.
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.GetException            general channel access failure
     */
    public final void getValFltCallback(final IEventSinkValFlt listener) throws ConnectionException, GetException {
        getRawValueCallback(new IEventSinkValue() {

            public void eventValue(final ChannelRecord record, Channel channel) {
                final float value = record.applyTransform(valueTransform).floatValue();
                listener.eventValue(value, Channel.this);
            }
        });
    }

    /**
     *  Get the value of the process variable via a callback to the specified listener.
     *  @param  listener     receiver of the callback event.
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.GetException            general channel access failure
     */
    public final void getValDblCallback(final IEventSinkValDbl listener) throws ConnectionException, GetException {
        getRawValueCallback(new IEventSinkValue() {

            public void eventValue(final ChannelRecord record, Channel channel) {
                final double value = record.applyTransform(valueTransform).doubleValue();
                listener.eventValue(value, Channel.this);
            }
        });
    }

    /**
     *  Get the value of the process variable via a callback to the specified listener.
     *  @param  listener     receiver of the callback event.
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.GetException            general channel access failure
     */
    public final void getArrByteCallback(final IEventSinkArrByte listener) throws ConnectionException, GetException {
        getRawValueCallback(new IEventSinkValue() {

            public void eventValue(final ChannelRecord record, Channel channel) {
                listener.eventArray(record.applyTransform(valueTransform).byteArray(), Channel.this);
            }
        });
    }

    /**
     *  Get the value of the process variable via a callback to the specified listener.
     *  @param  listener     receiver of the callback event.
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.GetException            general channel access failure
     */
    public final void getArrIntCallback(final IEventSinkArrInt listener) throws ConnectionException, GetException {
        getRawValueCallback(new IEventSinkValue() {

            public void eventValue(final ChannelRecord record, Channel channel) {
                listener.eventArray(record.applyTransform(valueTransform).intArray(), Channel.this);
            }
        });
    }

    /**
     *  Get the value of the process variable via a callback to the specified listener.
     *  @param  listener     receiver of the callback event.
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.GetException            general channel access failure
     */
    public final void getArrFltCallback(final IEventSinkArrFlt listener) throws ConnectionException, GetException {
        getRawValueCallback(new IEventSinkValue() {

            public void eventValue(final ChannelRecord record, Channel channel) {
                listener.eventArray(record.applyTransform(valueTransform).floatArray(), Channel.this);
            }
        });
    }

    /**
     *  Get the value of the process variable via a callback to the specified listener.
     *  @param  listener     receiver of the callback event.
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.GetException            general channel access failure
     */
    public final void getArrDblCallback(final IEventSinkArrDbl listener) throws ConnectionException, GetException {
        getRawValueCallback(new IEventSinkValue() {

            public void eventValue(final ChannelRecord record, Channel channel) {
                listener.eventArray(record.applyTransform(valueTransform).doubleArray(), Channel.this);
            }
        });
    }

    /**
     *  Setup a value-status-timestamp monitor on this channel 
     *  @param  listener     interface to data sink
     *  @param  intMaskFire code specifying when the monitor is fired or'ed combination of {Monitor.VALUE, Monitor.LOG, Monitor.ALARM}
     *  @return A new monitor
     *  @throws gov.sns.ca.ConnectionException     channel is not connected
     *  @throws gov.sns.ca.MonitorException        general monitor failure
     */
    public abstract gov.sns.ca.Monitor addMonitorValTime(IEventSinkValTime listener, int intMaskFire) throws ConnectionException, MonitorException;

    /**
     *  Setup a value-status-timestamp monitor on this channel 
     *  @param  listener     interface to data sink
     *  @param  intMaskFire code specifying when the monitor is fired or'ed combination of {Monitor.VALUE, Monitor.LOG, Monitor.ALARM}
     *  @param  count desired number of elements in returned value
     *  @return A new monitor
     *  @throws gov.sns.ca.ConnectionException     channel is not connected
     *  @throws gov.sns.ca.MonitorException        general monitor failure
     */
    public abstract gov.sns.ca.Monitor addMonitorValTime(IEventSinkValTime listener, int intMaskFire, int count) throws ConnectionException, MonitorException;

    /**
     *  Setup a value-status monitor on this channel 
     *  @param  listener     interface to data sink
     *  @param  intMaskFire code specifying when the monitor is fired or'ed combination of {Monitor.VALUE, Monitor.LOG, Monitor.ALARM}
     *  @return A new monitor
     *  @throws gov.sns.ca.ConnectionException     channel is not connected
     *  @throws gov.sns.ca.MonitorException        general monitor failure
     */
    public abstract gov.sns.ca.Monitor addMonitorValStatus(IEventSinkValStatus listener, int intMaskFire) throws ConnectionException, MonitorException;

    /**
     *  Setup a value-status monitor on this channel 
     *  @param  listener     interface to data sink
     *  @param  intMaskFire code specifying when the monitor is fired or'ed combination of {Monitor.VALUE, Monitor.LOG, Monitor.ALARM}
     *  @param  count desired number of elements in returned value
     *  @return A new monitor
     *  @throws gov.sns.ca.ConnectionException     channel is not connected
     *  @throws gov.sns.ca.MonitorException        general monitor failure
     */
    public abstract gov.sns.ca.Monitor addMonitorValStatus(IEventSinkValStatus listener, int intMaskFire, int count) throws ConnectionException, MonitorException;

    /**
     *  Setup a value monitor on this channel 
     *  @param  listener     interface to data sink
     *  @param  intMaskFire code specifying when the monitor is fired or'ed combination of {Monitor.VALUE, Monitor.LOG, Monitor.ALARM}
     *  @return A new monitor
     *  @throws gov.sns.ca.ConnectionException     channel is not connected
     *  @throws gov.sns.ca.MonitorException        general monitor failure
     */
    public abstract gov.sns.ca.Monitor addMonitorValue(IEventSinkValue listener, int intMaskFire) throws ConnectionException, MonitorException;

    /**
     *  Setup a value monitor on this channel 
     *  @param  listener     interface to data sink
     *  @param  intMaskFire code specifying when the monitor is fired or'ed combination of {Monitor.VALUE, Monitor.LOG, Monitor.ALARM}
     *  @param  count desired number of elements in returned value
     *  @return A new monitor
     *  @throws gov.sns.ca.ConnectionException     channel is not connected
     *  @throws gov.sns.ca.MonitorException        general monitor failure
     */
    public abstract gov.sns.ca.Monitor addMonitorValue(IEventSinkValue listener, int intMaskFire, int count) throws ConnectionException, MonitorException;

    /**
     *  Synchronously put a value to the channel process variable.
     *  @param  newVal      value sent to process variable
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.PutException            channel access failure, including
     */
    public void putVal(String newVal) throws ConnectionException, PutException {
        this.putValCallback(newVal, null);
    }

    /**
     *  Synchronously put a value to the channel process variable.
     *  @param  newVal      value sent to process variable
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.PutException            channel access failure, including
     */
    public void putVal(byte newVal) throws ConnectionException, PutException {
        this.putValCallback(newVal, null);
    }

    /**
     *  Synchronously put a value to the channel process variable.
     *  @param  newVal      value sent to process variable
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.PutException            channel access failure, including
     */
    public void putVal(short newVal) throws ConnectionException, PutException {
        this.putValCallback(newVal, null);
    }

    /**
     *  Synchronously put a value to the channel process variable.
     *  @param  newVal      value sent to process variable
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.PutException            channel access failure, including
     */
    public void putVal(int newVal) throws ConnectionException, PutException {
        this.putValCallback(newVal, null);
    }

    /**
     *  Synchronously put a value to the channel process variable.
     *  @param  newVal      value sent to process variable
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.PutException            channel access failure, including
     */
    public void putVal(float newVal) throws ConnectionException, PutException {
        this.putValCallback(newVal, null);
    }

    /**
     *  Synchronously put a value to the channel process variable.
     *  @param  newVal      value sent to process variable
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.PutException            channel access failure, including
     */
    public void putVal(double newVal) throws ConnectionException, PutException {
        this.putValCallback(newVal, null);
    }

    /**
     *  Synchronously put a value to the channel process variable.
     *  @param  newVal      value sent to process variable
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.PutException            channel access failure, including
     */
    public void putVal(byte[] newVal) throws ConnectionException, PutException {
        this.putValCallback(newVal, null);
    }

    /**
     *  Synchronously put a value to the channel process variable.
     *  @param  newVal      value sent to process variable
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.PutException            channel access failure, including
     */
    public void putVal(short[] newVal) throws ConnectionException, PutException {
        this.putValCallback(newVal, null);
    }

    /**
     *  Synchronously put a value to the channel process variable.
     *  @param  newVal      value sent to process variable
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.PutException            channel access failure, including
     */
    public void putVal(int[] newVal) throws ConnectionException, PutException {
        this.putValCallback(newVal, null);
    }

    /**
     *  Synchronously put a value to the channel process variable.
     *  @param  newVal      value sent to process variable
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.PutException            channel access failure, including
     */
    public void putVal(float[] newVal) throws ConnectionException, PutException {
        this.putValCallback(newVal, null);
    }

    /**
     *  Synchronously put a value to the channel process variable.
     *  @param  newVal      value sent to process variable
     *  @throws  gov.sns.ca.ConnectionException     channel is not connected
     *  @throws  gov.sns.ca.PutException            channel access failure, including
     */
    public void putVal(double[] newVal) throws ConnectionException, PutException {
        this.putValCallback(newVal, null);
    }

    /**
     * Asynchronously put a value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public final void putValCallback(String newVal, PutListener listener) throws ConnectionException, PutException {
        String rawValue = valueTransform.convertToRaw(ArrayValue.stringStore(newVal)).stringValue();
        putRawValCallback(rawValue, listener);
    }

    /**
     * Asynchronously put a value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public final void putValCallback(byte newVal, PutListener listener) throws ConnectionException, PutException {
        byte rawValue = valueTransform.convertToRaw(ArrayValue.byteStore(newVal)).byteValue();
        putRawValCallback(rawValue, listener);
    }

    /**
     * Asynchronously put a value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public final void putValCallback(short newVal, PutListener listener) throws ConnectionException, PutException {
        short rawValue = valueTransform.convertToRaw(ArrayValue.shortStore(newVal)).shortValue();
        putRawValCallback(rawValue, listener);
    }

    /**
     * Asynchronously put a value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public final void putValCallback(int newVal, PutListener listener) throws ConnectionException, PutException {
        int rawValue = valueTransform.convertToRaw(ArrayValue.intStore(newVal)).intValue();
        putRawValCallback(rawValue, listener);
    }

    /**
     * Asynchronously put a value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public final void putValCallback(float newVal, PutListener listener) throws ConnectionException, PutException {
        float rawValue = valueTransform.convertToRaw(ArrayValue.floatStore(newVal)).floatValue();
        putRawValCallback(rawValue, listener);
    }

    /**
     * Asynchronously put a value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public final void putValCallback(double newVal, PutListener listener) throws ConnectionException, PutException {
        double rawValue = valueTransform.convertToRaw(ArrayValue.doubleStore(newVal)).doubleValue();
        putRawValCallback(rawValue, listener);
    }

    /**
     * Asynchronously put a value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public final void putValCallback(byte[] newVal, PutListener listener) throws ConnectionException, PutException {
        byte[] rawArray = valueTransform.convertToRaw(ArrayValue.byteStore(newVal)).byteArray();
        putRawValCallback(rawArray, listener);
    }

    /**
     * Asynchronously put a value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public final void putValCallback(short[] newVal, PutListener listener) throws ConnectionException, PutException {
        short[] rawArray = valueTransform.convertToRaw(ArrayValue.shortStore(newVal)).shortArray();
        putRawValCallback(rawArray, listener);
    }

    /**
     * Asynchronously put a value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public final void putValCallback(int[] newVal, PutListener listener) throws ConnectionException, PutException {
        int[] rawArray = valueTransform.convertToRaw(ArrayValue.intStore(newVal)).intArray();
        putRawValCallback(rawArray, listener);
    }

    /**
     * Asynchronously put a value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public final void putValCallback(float[] newVal, PutListener listener) throws ConnectionException, PutException {
        float[] rawArray = valueTransform.convertToRaw(ArrayValue.floatStore(newVal)).floatArray();
        putRawValCallback(rawArray, listener);
    }

    /**
     * Asynchronously put a value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public final void putValCallback(double[] newVal, PutListener listener) throws ConnectionException, PutException {
        double[] rawArray = valueTransform.convertToRaw(ArrayValue.doubleStore(newVal)).doubleArray();
        putRawValCallback(rawArray, listener);
    }

    /**
     * Asynchronously put a raw value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public abstract void putRawValCallback(String newVal, PutListener listener) throws ConnectionException, PutException;

    /**
     * Asynchronously put a raw value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public abstract void putRawValCallback(byte newVal, PutListener listener) throws ConnectionException, PutException;

    /**
     * Asynchronously put a raw value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public abstract void putRawValCallback(short newVal, PutListener listener) throws ConnectionException, PutException;

    /**
     * Asynchronously put a raw value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public abstract void putRawValCallback(int newVal, PutListener listener) throws ConnectionException, PutException;

    /**
     * Asynchronously put a raw value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public abstract void putRawValCallback(float newVal, PutListener listener) throws ConnectionException, PutException;

    /**
     * Asynchronously put a raw value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public abstract void putRawValCallback(double newVal, PutListener listener) throws ConnectionException, PutException;

    /**
     * Asynchronously put a raw value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public abstract void putRawValCallback(byte[] newVal, PutListener listener) throws ConnectionException, PutException;

    /**
     * Asynchronously put a raw value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public abstract void putRawValCallback(short[] newVal, PutListener listener) throws ConnectionException, PutException;

    /**
     * Asynchronously put a raw value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public abstract void putRawValCallback(int[] newVal, PutListener listener) throws ConnectionException, PutException;

    /**
     * Asynchronously put a raw value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public abstract void putRawValCallback(float[] newVal, PutListener listener) throws ConnectionException, PutException;

    /**
     * Asynchronously put a raw value to the channel process variable.  Fire the specified callback
     * when put is complete.
     * @param  newVal      value sent to process variable
     * @param  listener The receiver of the callback event, or null if callback isn't needed
     * @throws gov.sns.ca.ConnectionException     channel is not connected
     * @throws gov.sns.ca.PutException        general put failure
     */
    public abstract void putRawValCallback(double[] newVal, PutListener listener) throws ConnectionException, PutException;
}

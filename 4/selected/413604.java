package gov.sns.jca;

import gov.aps.jca.CAException;
import gov.aps.jca.Context;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.sns.ca.Channel;
import gov.sns.ca.ChannelRecord;
import gov.sns.ca.ChannelStatusRecord;
import gov.sns.ca.ChannelTimeRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.GetException;
import gov.sns.ca.IEventSinkValStatus;
import gov.sns.ca.IEventSinkValTime;
import gov.sns.ca.IEventSinkValue;
import gov.sns.ca.MonitorException;
import gov.sns.ca.PutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Objectizes the Java Channel Access (jca) library by Boucher.  In particular, the jca.PV object
 *  and static jca.Ca are encapsulated.  The the jca.PV and jca.Ca operations are collected
 *  and exposed as necessary to perform rudimentary process variable puts, gets, and monitors.  The
 *  user may request a reference to the associated PV object to perform more complicated operations
 *  as appropriate.
 *
 * @author Christopher K. Allen
 * @author Tom Pelaia
 * @version 1.1
 */
class JcaChannel extends Channel {

    /** default pend IO timeout */
    private static final double c_dblDefTimeIO = 1.0;

    /** default pend event timeout */
    private static final double c_dblDefTimeEvent = 0.1;

    /** channel access initialized */
    private static boolean s_bolCaInit;

    /** channel access library lock */
    private static boolean s_bolCaLock;

    /** channel instance reference count */
    private static long s_lngCntRef;

    private static boolean s_bolDebug;

    /** JCA Channel */
    gov.aps.jca.Channel _jcaChannel;

    /** cache of native JCA channels JCA won't allow us to connect to more than one channel for the same PV signal. */
    private final JcaNativeChannelCache _jcaNativeChannelCache;

    /** JCA Context */
    private final Context _jcaContext;

    /**
     * indicates whether this channel ever initialized CA.
     * We only load and initialize Channel Access on demand.
     */
    private boolean hasInitializedCa;

    /** connection lock for wait and notify actions */
    private final Object _connectionLock = new Object();

    /**
     * @param nativeChannelCache  a cache of native JCA channels
     * @param signalName  EPICS PV name
     * @param jcaContext  the JCA Context within which to create the channel
     */
    JcaChannel(final String signalName, final Context jcaContext, final JcaNativeChannelCache jcaNativeChannelCache) {
        super(signalName);
        _jcaNativeChannelCache = jcaNativeChannelCache;
        _jcaContext = jcaContext;
        m_dblTmIO = c_dblDefTimeIO;
        m_dblTmEvt = c_dblDefTimeEvent;
    }

    /**
      * Set Forte debug mode.
      * The jca shared library (jca.dll on Windows) is normally loaded whenever the first Channel object
      * is instantiated.  This is done with a call to jca.Ca.init() with the Channel.caAddRef() method.
      * Loading the jca shared library seems to confuse the Forte debugger.
      * Use this method to set the Channel Forte debug mode to true.  When in debug mode the jca shared
      * library is never loaded.  Thus, Channel objects may be instatiated, but they cannot be used to
      * connect to EPICS channel access.
      * @param  bDebug      debug flag (on or off)
      */
    public static synchronized void setDebugMode(boolean bDebug) {
        if (s_bolDebug == true && bDebug == false) if (s_lngCntRef > 0) {
            s_bolCaInit = true;
        }
        s_bolDebug = bDebug;
    }

    /**
     *  Check if EPICS Channel Access libaray has been initialized and if not, do so.
     */
    private static synchronized void caAddRef() {
        if (!s_bolCaInit) if (!s_bolDebug) {
            s_bolCaInit = true;
        }
        s_lngCntRef++;
    }

    /**
     *  Check if EPICS Channel Access library is still needed and if not, release it.
     */
    private static synchronized void caRelease() {
        if (s_lngCntRef == 0) return;
        s_lngCntRef--;
        if (s_lngCntRef > 0) return;
        if (s_bolCaLock) return;
        if (s_bolCaInit == true) {
            s_bolCaInit = false;
        }
    }

    /**
     *  Check if Channel Access library can be released
     */
    @Override
    protected void finalize() throws Throwable {
        if (hasInitializedCa) {
            caRelease();
        }
        super.finalize();
    }

    /** Initialize channel access and increment instance count */
    private void initChannelAccess() {
        if (!hasInitializedCa) {
            caAddRef();
            hasInitializedCa = true;
        }
    }

    /**
     * Notify connection listeners that connection has changed
     */
    private ConnectionListener newConnectionListener() {
        return new ConnectionListener() {

            public void connectionChanged(final ConnectionEvent event) {
                synchronized (_connectionLock) {
                    if (event.isConnected()) {
                        processConnectionEvent();
                    } else {
                        connectionFlag = false;
                        if (connectionProxy != null) {
                            connectionProxy.connectionDropped(JcaChannel.this);
                        }
                    }
                }
            }
        };
    }

    /** Process a connection event */
    private void processConnectionEvent() {
        connectionFlag = true;
        proceedFromConnection();
        if (connectionProxy != null) {
            connectionProxy.connectionMade(this);
        }
    }

    /**
     * Request a new connection and wait for it no longer than the timeout.
     * @param timeout seconds to wait for a connection before giving up
     * @return true if the connection was made within the timeout and false if not
     */
    @Override
    public boolean connectAndWait(final double timeout) {
        if (m_strId == null) return false;
        requestConnection();
        flushIO();
        if (this.isConnected()) return true;
        waitForConnection(timeout);
        return isConnected();
    }

    /**
     * Request that the channel be connected.  Connections are made in the background
     * so this method returns immediately upon making the request.  The connection will be
     * made in the future as soon as possible.  A connection event will be sent to registered
     * connection listeners when the connection has been established.
     */
    @Override
    public void requestConnection() {
        if (m_strId == null || isConnected()) return;
        initChannelAccess();
        if (_jcaChannel == null) {
            try {
                synchronized (_connectionLock) {
                    _jcaChannel = _jcaNativeChannelCache.getChannel(m_strId);
                    _jcaChannel.addConnectionListener(newConnectionListener());
                    if (_jcaChannel.getConnectionState() == gov.aps.jca.Channel.CONNECTED) {
                        processConnectionEvent();
                    }
                }
            } catch (CAException exception) {
                final String message = "Error attempting to connect to: " + m_strId;
                Logger.getLogger("global").log(Level.SEVERE, message, exception);
            }
        }
    }

    /**
     * Attempt to connect only if this channel has never been connected in the past.
     */
    private void connectIfNeverConnected() {
        if (!hasEverBeenConnected()) connectAndWait();
    }

    /**
     * Wait until a connection is made or the attempt to connect has timed out.
     * @param timeout seconds to wait for the connection before giving up
     */
    private synchronized void waitForConnection(final double timeout) {
        if (connectionFlag) return;
        try {
            synchronized (_connectionLock) {
                _connectionLock.wait((long) (1000 * timeout));
            }
        } catch (InterruptedException exception) {
            Logger.getLogger("global").log(Level.SEVERE, "Error waiting for connection to: " + m_strId, exception);
            exception.printStackTrace();
        }
    }

    /**
     * Proceed forward since the connection has been made.
     */
    private void proceedFromConnection() {
        synchronized (_connectionLock) {
            _connectionLock.notify();
        }
    }

    /**
     *  Terminate the network channel connection and clear all events associated
     *  with process variable
     */
    @Override
    public void disconnect() {
        try {
            if (!isConnected()) return;
            try {
                _jcaChannel.destroy();
            } catch (CAException exception) {
                Logger.getLogger("global").log(Level.SEVERE, "Error disconnecting: " + m_strId, exception);
            }
        } finally {
            _jcaChannel = null;
        }
    }

    /**
     * Determine if the channel has ever been connected regardless of its present connection state.
     * @return true if the channel has ever been connected and false if not.
     */
    private boolean hasEverBeenConnected() {
        if (_jcaChannel == null) {
            return false;
        }
        final gov.aps.jca.Channel.ConnectionState state = _jcaChannel.getConnectionState();
        return state == gov.aps.jca.Channel.CONNECTED || state == gov.aps.jca.Channel.DISCONNECTED;
    }

    /**
     *  Checks if this channel has ever been connected.
     *  @param  strFuncName     name of function using connection
     */
    private void checkIfEverConnected(final String methodName) throws ConnectionException {
        if (!hasEverBeenConnected()) {
            throw new ConnectionException(this, "Channel::" + methodName + " - The channel \"" + m_strId + "\" must be connected at least once in the past to use this feature.");
        }
    }

    /** get the Java class associated with the native type of this channel */
    @Override
    public Class<?> elementType() throws ConnectionException {
        checkIfEverConnected("elementType()");
        return DbrValueAdaptor.elementType(getJcaType());
    }

    /**
     * Get the JCA field type of this channel
     * @return the field type of the JCA channel
     */
    private DBRType getJcaType() throws ConnectionException {
        checkIfEverConnected("getJcaType()");
        return _jcaChannel.getFieldType();
    }

    /**
     *  Return size of value array associated with process variable
     *  @return     number of values in process variable
     */
    @Override
    public int elementCount() throws ConnectionException {
        checkIfEverConnected("elementCount()");
        return _jcaChannel.getElementCount();
    }

    /**
     *  Determine if channel has read access to process variable
     *  @return             true if channel has read access
     *
     *  @exception  ConnectionException     channel not connected
     */
    @Override
    public boolean readAccess() throws ConnectionException {
        checkIfEverConnected("readAccess()");
        return _jcaChannel.getReadAccess();
    }

    /**
     *  Determine if channel has write access to process variable
     *  @return             true if channel has write access
     *
     *  @exception  ConnectionException     channel not connected
     */
    @Override
    public boolean writeAccess() throws ConnectionException {
        checkIfEverConnected("writeAccess()");
        return _jcaChannel.getWriteAccess();
    }

    /**
     * Get the native value-status DBR type of this channel.
     * @return The native DBR type of this channel.
     */
    DBRType getStatusType() throws ConnectionException, GetException {
        connectIfNeverConnected();
        final DBRType nativeType = getJcaType();
        if (nativeType.isBYTE()) {
            return DBRType.STS_BYTE;
        } else if (nativeType.isENUM()) {
            return DBRType.STS_ENUM;
        } else if (nativeType.isSHORT()) {
            return DBRType.STS_SHORT;
        } else if (nativeType.isINT()) {
            return DBRType.STS_INT;
        } else if (nativeType.isFLOAT()) {
            return DBRType.STS_FLOAT;
        } else if (nativeType.isDOUBLE()) {
            return DBRType.STS_DOUBLE;
        } else if (nativeType.isSTRING()) {
            return DBRType.STS_STRING;
        } else {
            throw new GetException("No status type for type code: " + nativeType + " for pv: " + m_strId);
        }
    }

    /**
     * Get the native DBR value-status-timestamp type of this channel.
     * @return The native DBR type of this channel.
     */
    DBRType getTimeType() throws ConnectionException, GetException {
        connectIfNeverConnected();
        final DBRType nativeType = getJcaType();
        if (nativeType.isBYTE()) {
            return DBRType.TIME_BYTE;
        } else if (nativeType.isENUM()) {
            return DBRType.TIME_ENUM;
        } else if (nativeType.isSHORT()) {
            return DBRType.TIME_SHORT;
        } else if (nativeType.isINT()) {
            return DBRType.TIME_INT;
        } else if (nativeType.isFLOAT()) {
            return DBRType.TIME_FLOAT;
        } else if (nativeType.isDOUBLE()) {
            return DBRType.TIME_DOUBLE;
        } else if (nativeType.isSTRING()) {
            return DBRType.TIME_STRING;
        } else {
            throw new GetException("No status type for type code: " + nativeType + " for pv: " + m_strId);
        }
    }

    DBRType getCtrlType() throws ConnectionException, GetException {
        connectIfNeverConnected();
        final DBRType nativeType = getJcaType();
        if (nativeType.isBYTE()) {
            return DBRType.CTRL_BYTE;
        } else if (nativeType.isENUM()) {
            throw new GetException("No control record for ENUM type for pv: " + m_strId);
        } else if (nativeType.isSHORT()) {
            return DBRType.CTRL_SHORT;
        } else if (nativeType.isINT()) {
            return DBRType.CTRL_INT;
        } else if (nativeType.isFLOAT()) {
            return DBRType.CTRL_FLOAT;
        } else if (nativeType.isDOUBLE()) {
            return DBRType.CTRL_DOUBLE;
        } else {
            throw new GetException("No control record for type code: " + nativeType + " for pv: " + m_strId);
        }
    }

    /** Convenience method which returns the units for this channel. */
    @Override
    public String getUnits() throws ConnectionException, GetException {
        return getCtrlInfo().getUnits();
    }

    /** Convenience method which returns the upper display limit. */
    @Override
    public Number rawUpperDisplayLimit() throws ConnectionException, GetException {
        return getCtrlInfo().getUpperDispLimit();
    }

    /** Convenience method which returns the lower display limit. */
    @Override
    public Number rawLowerDisplayLimit() throws ConnectionException, GetException {
        return getCtrlInfo().getLowerDispLimit();
    }

    /** Convenience method which returns the upper alarm limit. */
    @Override
    public Number rawUpperAlarmLimit() throws ConnectionException, GetException {
        return getCtrlInfo().getUpperAlarmLimit();
    }

    /** Convenience method which returns the lower alarm limit. */
    @Override
    public Number rawLowerAlarmLimit() throws ConnectionException, GetException {
        return getCtrlInfo().getLowerAlarmLimit();
    }

    /** Convenience method which returns the upper warning limit. */
    @Override
    public Number rawUpperWarningLimit() throws ConnectionException, GetException {
        return getCtrlInfo().getUpperWarningLimit();
    }

    /** Convenience method which returns the lower warning limit. */
    @Override
    public Number rawLowerWarningLimit() throws ConnectionException, GetException {
        return getCtrlInfo().getLowerWarningLimit();
    }

    /** Convenience method which returns the upper control limit. */
    @Override
    public Number rawUpperControlLimit() throws ConnectionException, GetException {
        return getCtrlInfo().getUpperCtrlLimit();
    }

    /** Convenience method which returns the lower control limit. */
    @Override
    public Number rawLowerControlLimit() throws ConnectionException, GetException {
        return getCtrlInfo().getLowerCtrlLimit();
    }

    /**
     * Returns a DBR_CTRL instance of the appropriate for this channel.
     * The DBR_CTRL record contains valuable information about the channel
     * such as the units and upper and lower limits for alarm, display, warning
     * and control.  All of these items are returned as a DBData instance.
     * Examples:
     *<pre>
     *      String units = channel.getCtrlInfo().units();
     *      double upperDisplayLimit = channel.getCtrlInfo().upperDispLimit().doubleValue();
     *</pre>
     */
    private CTRL getCtrlInfo() throws ConnectionException, GetException {
        return (CTRL) getVal(getCtrlType());
    }

    /**
     * Return a <code>ChannelRecord</code> representing the fetched record for the
     * native type of this channel.  This is a convenient way to get the value of
     * the PV.
     */
    @Override
    public ChannelRecord getRawValueRecord() throws ConnectionException, GetException {
        return getRawValueRecord(-1);
    }

    @Override
    public ChannelRecord getRawValueRecord(int count) throws ConnectionException, GetException {
        connectAndWait();
        DBR dbr = getVal(getJcaType(), count);
        synchronized (dbr) {
            return new ChannelRecord(new DbrValueAdaptor(dbr));
        }
    }

    /**
     * Return a <code>ChannelStatusRecord</code> representing the fetched record for the
     * native type of this channel.  This is a convenient way to get the value of
     * the PV along with status.
     */
    @Override
    public ChannelStatusRecord getRawStatusRecord() throws ConnectionException, GetException {
        return getRawStatusRecord(-1);
    }

    @Override
    public ChannelStatusRecord getRawStatusRecord(int count) throws ConnectionException, GetException {
        connectAndWait();
        DBR dbr = getVal(getStatusType(), count);
        if (!(dbr instanceof STS)) {
            throw new GetException("Illegal type of data received: " + dbr.getClass().getName());
        }
        synchronized (dbr) {
            return new ChannelStatusRecord(new DbrStatusAdaptor(dbr));
        }
    }

    /**
     * Return a <code>ChannelTimeRecord</code> representing the fetched record for the
     * native type of this channel.  This is a convenient way to get the value of
     * the PV along with status and timestamp.
     */
    @Override
    public ChannelTimeRecord getRawTimeRecord() throws ConnectionException, GetException {
        return getRawTimeRecord(-1);
    }

    @Override
    public ChannelTimeRecord getRawTimeRecord(int count) throws ConnectionException, GetException {
        connectAndWait();
        DBR dbr = this.getVal(getTimeType(), count);
        if (!(dbr instanceof TIME)) {
            throw new GetException("Illegal type of data received: " + dbr.getClass().getName());
        }
        synchronized (dbr) {
            return new ChannelTimeRecord(new DbrTimeAdaptor(dbr));
        }
    }

    /**
     *  Gets the value of a PV as a database request object
     *  @param  type     DBR type code of returned object
     *  @return DBR object containing PV value
     */
    private DBR getVal(DBRType type) throws ConnectionException, GetException {
        return getVal(type, -1);
    }

    /**
     *  Return process variable values in specific type and number
     *  @param  type      DBR type code of returned value
     *  @param  count     number of values to return
     *  @return           DBR containing process variable values
     *
     *  @exception  ConnectionException channel not connected
     *  @exception  GetException        channel access get failure
     */
    private DBR getVal(DBRType type, int count) throws ConnectionException, GetException {
        this.checkConnection("getVal()");
        try {
            DBR dbr = _jcaChannel.get(type, count < 0 ? this.elementCount() : count);
            flushGetIO();
            return dbr;
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error getting value from: " + m_strId, exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void getRawValueCallback(IEventSinkValue listener) throws ConnectionException, GetException {
        if (listener == null) {
            throw new NullPointerException();
        }
        this.checkConnection("getValueCallback()");
        try {
            _jcaChannel.get(new GetNotifier(this, listener));
        } catch (CAException exception) {
            throw new GetException("Get exception in GetBack: " + exception.getMessage());
        }
    }

    @Override
    public void putRawValCallback(final String newVal, final gov.sns.ca.PutListener listener) throws ConnectionException, PutException {
        this.checkConnection("putValCallback()");
        try {
            _jcaChannel.put(newVal, new PutNotifier(this, listener));
            if (listener == null) flushPutIO();
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error putting value to: " + m_strId, exception);
            throw new PutException("JcaChannel.putValCallback(): " + exception.getMessage());
        }
    }

    @Override
    public void putRawValCallback(final byte newVal, final gov.sns.ca.PutListener listener) throws ConnectionException, PutException {
        this.checkConnection("putValCallback()");
        try {
            _jcaChannel.put(newVal, new PutNotifier(this, listener));
            if (listener == null) flushPutIO();
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error putting value to: " + m_strId, exception);
            throw new PutException("JcaChannel.putValCallback(): " + exception.getMessage());
        }
    }

    @Override
    public void putRawValCallback(final short newVal, final gov.sns.ca.PutListener listener) throws ConnectionException, PutException {
        this.checkConnection("putValCallback()");
        try {
            _jcaChannel.put(newVal, new PutNotifier(this, listener));
            if (listener == null) flushPutIO();
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error putting value to: " + m_strId, exception);
            throw new PutException("JcaChannel.putValCallback(): " + exception.getMessage());
        }
    }

    @Override
    public void putRawValCallback(final int newVal, final gov.sns.ca.PutListener listener) throws ConnectionException, PutException {
        this.checkConnection("putValCallback()");
        try {
            _jcaChannel.put(newVal, new PutNotifier(this, listener));
            if (listener == null) flushPutIO();
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error putting value to: " + m_strId, exception);
            throw new PutException("JcaChannel.putValCallback(): " + exception.getMessage());
        }
    }

    @Override
    public void putRawValCallback(final float newVal, final gov.sns.ca.PutListener listener) throws ConnectionException, PutException {
        this.checkConnection("putValCallback()");
        try {
            _jcaChannel.put(newVal, new PutNotifier(this, listener));
            if (listener == null) flushPutIO();
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error putting value to: " + m_strId, exception);
            throw new PutException("JcaChannel.putValCallback(): " + exception.getMessage());
        }
    }

    @Override
    public void putRawValCallback(double newVal, gov.sns.ca.PutListener listener) throws ConnectionException, PutException {
        this.checkConnection("putValCallback()");
        try {
            _jcaChannel.put(newVal, new PutNotifier(this, listener));
            if (listener == null) flushPutIO();
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error putting value to: " + m_strId, exception);
            throw new PutException("JcaChannel.putValCallback(): " + exception.getMessage());
        }
    }

    @Override
    public void putRawValCallback(byte[] newVal, gov.sns.ca.PutListener listener) throws ConnectionException, PutException {
        this.checkConnection("putValCallback()");
        try {
            _jcaChannel.put(newVal, new PutNotifier(this, listener));
            if (listener == null) flushPutIO();
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error putting value to: " + m_strId, exception);
            throw new PutException("JcaChannel.putValCallback(): Incompatible types - " + exception.getMessage());
        }
    }

    @Override
    public void putRawValCallback(short[] newVal, gov.sns.ca.PutListener listener) throws ConnectionException, PutException {
        this.checkConnection("putValCallback()");
        try {
            _jcaChannel.put(newVal, new PutNotifier(this, listener));
            if (listener == null) flushPutIO();
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error putting value to: " + m_strId, exception);
            throw new PutException("JcaChannel.putValCallback(): Incompatible types - " + exception.getMessage());
        }
    }

    @Override
    public void putRawValCallback(int[] newVal, gov.sns.ca.PutListener listener) throws ConnectionException, PutException {
        this.checkConnection("putValCallback()");
        try {
            _jcaChannel.put(newVal, new PutNotifier(this, listener));
            if (listener == null) flushPutIO();
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error putting value to: " + m_strId, exception);
            throw new PutException("JcaChannel.putValCallback(): Incompatible types - " + exception.getMessage());
        }
    }

    @Override
    public void putRawValCallback(float[] newVal, gov.sns.ca.PutListener listener) throws ConnectionException, PutException {
        this.checkConnection("putValCallback()");
        try {
            _jcaChannel.put(newVal, new PutNotifier(this, listener));
            if (listener == null) flushPutIO();
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error putting value to: " + m_strId, exception);
            throw new PutException("JcaChannel.putValCallback(): Incompatible types - " + exception.getMessage());
        }
    }

    @Override
    public void putRawValCallback(double[] newVal, gov.sns.ca.PutListener listener) throws ConnectionException, PutException {
        this.checkConnection("putValCallback()");
        try {
            _jcaChannel.put(newVal, new PutNotifier(this, listener));
            if (listener == null) flushPutIO();
        } catch (CAException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Error putting value to: " + m_strId, exception);
            throw new PutException("JcaChannel.putValCallback(): Incompatible types - " + exception.getMessage());
        }
    }

    @Override
    public gov.sns.ca.Monitor addMonitorValTime(IEventSinkValTime ifcSink, int intMaskFire) throws ConnectionException, MonitorException {
        this.checkConnection("addMonitorValTime()");
        return JcaMonitorValTime.newMonitor(this, ifcSink, intMaskFire);
    }

    @Override
    public gov.sns.ca.Monitor addMonitorValTime(IEventSinkValTime ifcSink, int intMaskFire, int count) throws ConnectionException, MonitorException {
        this.checkConnection("addMonitorValTime()");
        return JcaMonitorValTime.newMonitor(this, ifcSink, intMaskFire, count);
    }

    @Override
    public gov.sns.ca.Monitor addMonitorValStatus(IEventSinkValStatus ifcSink, int intMaskFire) throws ConnectionException, MonitorException {
        this.checkConnection("addMonitorValStatus()");
        return JcaMonitorValStatus.newMonitor(this, ifcSink, intMaskFire);
    }

    @Override
    public gov.sns.ca.Monitor addMonitorValStatus(IEventSinkValStatus ifcSink, int intMaskFire, int count) throws ConnectionException, MonitorException {
        this.checkConnection("addMonitorValStatus()");
        return JcaMonitorValStatus.newMonitor(this, ifcSink, intMaskFire, count);
    }

    @Override
    public gov.sns.ca.Monitor addMonitorValue(IEventSinkValue ifcSink, int intMaskFire) throws ConnectionException, MonitorException {
        this.checkConnection("addMonitorValue()");
        return JcaMonitorValue.newMonitor(this, ifcSink, intMaskFire);
    }

    @Override
    public gov.sns.ca.Monitor addMonitorValue(IEventSinkValue ifcSink, int intMaskFire, int count) throws ConnectionException, MonitorException {
        this.checkConnection("addMonitorValue()");
        return JcaMonitorValue.newMonitor(this, ifcSink, intMaskFire, count);
    }

    /**
     *  Flushes the channel access request buffer for get operations
     *  @exception  GetException        a pendIO time out occurred
     */
    private void flushGetIO() throws GetException {
        try {
            _jcaContext.pendIO(m_dblTmIO);
        } catch (CAException exception) {
            exception.printStackTrace();
            Logger.getLogger("global").log(Level.SEVERE, "Error flushing the channel access GET I/O buffer.", exception);
            throw new GetException("JcaChannel.flushGetIO() - channel access time out occurred");
        } catch (TimeoutException exception) {
            exception.printStackTrace();
            Logger.getLogger("global").log(Level.SEVERE, "Error flushing the channel access GET I/O buffer.", exception);
            throw new GetException("JcaChannel.flushGetIO() - channel access time out occurred");
        }
    }

    /**
     *  Flushs the channel access request buffer for put operations
     *  @exception  PutException        a pendIO time out occurred
     */
    private void flushPutIO() throws PutException {
        try {
            _jcaContext.pendIO(m_dblTmIO);
        } catch (CAException exception) {
            exception.printStackTrace();
            Logger.getLogger("global").log(Level.SEVERE, "Error flushing the channel access PUT I/O buffer.", exception);
            throw new PutException("JcaChannel.flushPutIO() - channel access time out occurred");
        } catch (TimeoutException exception) {
            exception.printStackTrace();
            Logger.getLogger("global").log(Level.SEVERE, "Error flushing the channel access PUT I/O buffer.", exception);
            throw new PutException("JcaChannel.flushPutIO() - channel access time out occurred");
        }
    }
}

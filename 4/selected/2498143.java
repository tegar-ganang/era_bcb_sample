package gov.sns.apps.pta.test;

import static org.junit.Assert.fail;
import gov.sns.ca.Channel;
import gov.sns.ca.ChannelRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.GetException;
import gov.sns.ca.IEventSinkValue;
import gov.sns.ca.Monitor;
import gov.sns.ca.MonitorException;
import gov.sns.ca.PutException;
import gov.sns.tools.plot.BasicGraphData;
import gov.sns.tools.plot.FunctionGraphsJPanel;
import gov.sns.xal.smf.Accelerator;
import gov.sns.xal.smf.AcceleratorNode;
import gov.sns.xal.smf.AcceleratorSeq;
import gov.sns.xal.smf.NoSuchChannelException;
import gov.sns.xal.smf.data.XMLDataManager;
import gov.sns.xal.smf.impl.WireScanner;
import java.awt.Dimension;
import java.util.Calendar;
import javax.swing.Box;
import javax.swing.JFrame;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 *
 * @since  Dec 7, 2009
 * @author Christopher K. Allen
 */
public class TestWireScannerMonitoring {

    /**
     *
     *
     * @since  Dec 7, 2009
     * @author Christopher K. Allen
     */
    abstract static class MonitorBase implements IEventSinkValue {

        /**
         * Returns the XAL channel handle of the
         * device's read back channel that we wish
         * to monitor.
         *
         * @return read back XAL channel handle to be monitored
         * 
         * @since  Dec 7, 2009
         * @author Christopher K. Allen
         */
        public abstract String getReadBackHandle();

        /**
         * Derived classes catch this event to respond
         * to a change in the PV value being monitored.
         *
         * @param recValue
         * 
         * @since  Dec 7, 2009
         * @author Christopher K. Allen
         */
        public abstract void valueChanged(ChannelRecord recValue);

        /** The CA channel we are monitoring */
        private Channel chanDevice;

        /** The CA monitor sending events the we are catching */
        private Monitor monDevice;

        /** The hardware device being monitored */
        private final WireScanner smfDev;

        /** The monitoring active flag */
        private boolean bolMonitoring;

        /** Initial value flag */
        private boolean bolInitEvt;

        /**
         * Create a new <code>MonitorBase</code> object.
         *
         * @param smfDev
         *
         * @since     Dec 7, 2009
         * @author    Christopher K. Allen
         */
        public MonitorBase(WireScanner smfDev) {
            this.smfDev = smfDev;
            this.bolMonitoring = false;
            this.bolInitEvt = true;
        }

        /**
         * getDevice
         *
         * @return      returns the device being monitored
         * 
         * @since  Dec 7, 2009
         * @author Christopher K. Allen
         */
        public WireScanner getDevice() {
            return this.smfDev;
        }

        /**
         * getChannel
         *
         * @return      returns the channel of the device process variable 
         * 
         * @since  Dec 7, 2009
         * @author Christopher K. Allen
         */
        public Channel getChannel() {
            return this.chanDevice;
        }

        /**
         * Begin channel monitoring catching the first (initializing)
         * event.
         *
         * @throws ConnectionException    could not connect to channel
         * @throws NoSuchChannelException channel handle is invalid
         * @throws MonitorException       CA monitor failed to start
         * 
         * @since  Dec 8, 2009
         * @author Christopher K. Allen
         */
        public void begin() throws ConnectionException, NoSuchChannelException, MonitorException {
            this.begin(false);
        }

        /**
         * Begin channel monitoring
         * 
         * @param  bolInitEvent  ignore initializing event 
         *                      (default is <code>false</code> - we catch first event)
         *
         * @throws ConnectionException    could not connect to channel
         * @throws NoSuchChannelException channel handle is invalid
         * @throws MonitorException       CA monitor failed to start
         * 
         * @since  Dec 7, 2009
         * @author Christopher K. Allen
         */
        public void begin(boolean bolInitEvent) throws ConnectionException, NoSuchChannelException, MonitorException {
            this.bolInitEvt = bolInitEvent;
            String strHandle = this.getReadBackHandle();
            this.chanDevice = smfDev.getAndConnectChannel(strHandle);
            this.monDevice = this.chanDevice.addMonitorValue(this, Monitor.VALUE);
            this.bolMonitoring = true;
        }

        /**
         * Discontinue monitor of the channel.
         * (No more events are fired.)
         *
         * 
         * @since  Dec 7, 2009
         * @author Christopher K. Allen
         */
        public void clear() {
            if (!this.bolMonitoring) return;
            this.monDevice.clear();
            this.bolMonitoring = false;
        }

        /**
         * Responds to a change in the channel's value
         * set by the CA monitor.  We call the abstract method 
         * <code>valueChanged(ChannelRecord)</code>.
         *
         * @since       Nov 4, 2009
         * @author  Christopher K. Allen
         *
         * @see gov.sns.ca.IEventSinkValue#eventValue(gov.sns.ca.ChannelRecord, gov.sns.ca.Channel)
         */
        public void eventValue(ChannelRecord record, Channel chan) {
            if (this.bolInitEvt) {
                this.bolInitEvt = false;
                return;
            }
            this.valueChanged(record);
        }
    }

    /**
     * <p>
     * Monitors of the actuation motion send their events to one
     * of these.  There should be one of this class for each
     * actuator in motion.
     * </p>  
     * <p>
     * Each new scanning process needs
     * to fire up one of these event sinks, which has a dynamic
     * state.  After spawning we register ourself with the DACQ
     * controller, that is, let it know we are alive.  When the
     * scanning process completes, this object alerts the 
     * DACQ controller.  (Thus, once all the motion status
     * processes have finished, then we can allow for another scan.)
     * </p>
     *
     * @since  Nov 4, 2009
     * @author Christopher K. Allen
     */
    static class MotionMonitor extends MonitorBase {

        /**
         * State variable enumeration for the monitor process,
         * which have the same values as the motion status
         * process variable.
         *
         * @since  Nov 5, 2009
         * @author Christopher K. Allen
         */
        public enum STATE {

            /** Actuator state is unknown */
            UNKNOWN(-1), /** Actuator is parked */
            HALTED(0), /** actuator is moving */
            MOVING(1), /** Motion failure condition */
            FAIL(2);

            /**
             * Returns the motion state enumeration constant
             * corresponding to the given motion state value.
             * Returns the <code>UNKNOWN</code> constant if the
             * given value is not a motion state value.
             *
             * @param intMotionStatVal  actuator motion state value
             * 
             * @return  enumeration constant corresponding to given value,
             *          <code>UNKNOWN</code> if argument is not a value
             * 
             * @since  Nov 6, 2009
             * @author Christopher K. Allen
             */
            public static STATE getState(int intMotionStatVal) {
                switch(intMotionStatVal) {
                    case 0:
                        return HALTED;
                    case 1:
                        return MOVING;
                    case 2:
                        return FAIL;
                    default:
                        return UNKNOWN;
                }
            }

            /** 
             * Return the value used by the motion status
             * Process Value corresponding to this state.
             *
             * @return  Motion Status PV value for this state
             * 
             * @since  Nov 5, 2009
             * @author Christopher K. Allen
             */
            public int getMotionStatusVal() {
                return this.intStat;
            }

            ;

            /** State enumeration value */
            private final int intStat;

            /** Initialize enumeration constant */
            private STATE(int iStat) {
                this.intStat = iStat;
            }

            ;
        }

        /** Dynamic state variable - active state value */
        private STATE stateCurr;

        /**
         * Create a new <code>StatusMonitor</code> object.
         *
         * @param smfDev       the hardware device being monitored
         *
         * @since     Nov 4, 2009
         * @author    Christopher K. Allen
         * 
         */
        public MotionMonitor(WireScanner smfDev) {
            super(smfDev);
            this.stateCurr = STATE.UNKNOWN;
        }

        /**
         *
         * @since       Dec 7, 2009
         * @author  Christopher K. Allen
         *
         * @see gov.sns.apps.pta.test.TestWireScannerMonitoring.MonitorBase#getReadBackHandle()
         */
        @Override
        public String getReadBackHandle() {
            return WireScanner.DevStatus.PARAM.MVT_STATUS.getPvDescriptor().getRbHandle();
        }

        /**
         * Responds to a channel in the motion PV of the
         * device associated with this object. 
         * 
         * @param       recValue        new value of the read back
         *
         * @since       Nov 4, 2009
         * @author  Christopher K. Allen
         *
         * @see gov.sns.ca.IEventSinkValue#eventValue(gov.sns.ca.ChannelRecord, gov.sns.ca.Channel)
         */
        @Override
        public void valueChanged(ChannelRecord recValue) {
            if (this.stateCurr == STATE.FAIL) return;
            int intNew = recValue.intValue();
            STATE stateNew = STATE.getState(intNew);
            this.stateCurr = stateNew;
            System.out.println("MOTION STATUS: " + stateNew.toString());
        }
    }

    /**
     * Monitors of actuator movements should send their
     * events to one of these.  It will update the progress
     * bar associated with this object.
     *
     * @since  Nov 4, 2009
     * @author Christopher K. Allen
     */
    static class PositionMonitor extends MonitorBase {

        /**
         * Create a new <code>PositionMonitor</code> object for
         * monitoring the given data acquisition device.
         *
         * @param smfDev        the DACQ device to monitor
         *
         * @since     Nov 4, 2009
         * @author    Christopher K. Allen
         * 
         */
        public PositionMonitor(WireScanner smfDev) {
            super(smfDev);
        }

        /**
         *
         * @since       Nov 4, 2009
         * @author  Christopher K. Allen
         *
         * @see gov.sns.ca.IEventSinkValue#eventValue(gov.sns.ca.ChannelRecord, gov.sns.ca.Channel)
         */
        @Override
        public void valueChanged(ChannelRecord recValue) {
            double dblVal = recValue.doubleValue();
            String strTm = Calendar.getInstance().getTime().toString();
            System.out.println("Scan Monitor:Position = " + dblVal + " at time = " + strTm);
        }

        /**
         *
         * @since 	Dec 7, 2009
         * @author  Christopher K. Allen
         *
         * @see gov.sns.apps.pta.test.TestWireScannerMonitoring.MonitorBase#getReadBackHandle()
         */
        @Override
        public String getReadBackHandle() {
            return WireScanner.DevStatus.PARAM.WIRE_POS.getPvDescriptor().getRbHandle();
        }
    }

    /**
     * Monitors the scan sequence identifier PV in order to
     * determine when a DAQ device has completed its scan.
     * Specifically, once a scan completes, it increments its
     * scan sequence identifier.  This monitor class responds
     * to that event by calling the <code>deviceDeativated</code>
     * method.
     *
     * @since  Dec 2, 2009
     * @author Christopher K. Allen
     */
    class ScanIdMonitor extends MonitorBase {

        /**
         * Create a new <code>ScanIdMonitor</code> object.
         *
         * @param smfDev
         *
         * @since     Dec 2, 2009
         * @author    Christopher K. Allen
         */
        public ScanIdMonitor(WireScanner smfDev) {
            super(smfDev);
        }

        /**
         * The scan sequence identifier has been modified.  The scan must
         * have completed - call the <code>deviceDeactivated</code> method.
         *
         * @since       Dec 2, 2009
         * @author  Christopher K. Allen
         *
         * @see gov.sns.ca.IEventSinkValue#eventValue(gov.sns.ca.ChannelRecord, gov.sns.ca.Channel)
         */
        @Override
        public void valueChanged(ChannelRecord recValue) {
            int intVal = recValue.intValue();
            System.out.println(" DAQ device completed scan ID# " + intVal);
            System.out.println("  starting limit switch monitoring");
            createLimitSwitchMonitor();
        }

        /**
         *
         * @since 	Dec 7, 2009
         * @author  Christopher K. Allen
         *
         * @see gov.sns.apps.pta.test.TestWireScannerMonitoring.MonitorBase#getReadBackHandle()
         */
        @Override
        public String getReadBackHandle() {
            return WireScanner.DevStatus.PARAM.ID_SCAN.getPvDescriptor().getRbHandle();
        }
    }

    /**
     *
     *
     * @since  Dec 7, 2009
     * @author Christopher K. Allen
     */
    class LimitSwitchMonitor extends MonitorBase {

        /**
         * Create a new <code>LimitSwitchMonitor</code> object.
         *
         * @param smfDev
         *
         * @since     Dec 7, 2009
         * @author    Christopher K. Allen
         */
        public LimitSwitchMonitor(WireScanner smfDev) {
            super(smfDev);
        }

        /**
         *
         * @since 	Dec 7, 2009
         * @author  Christopher K. Allen
         *
         * @see gov.sns.apps.pta.test.TestWireScannerMonitoring.MonitorBase#getReadBackHandle()
         */
        @Override
        public String getReadBackHandle() {
            return WireScanner.DevStatus.PARAM.LIM_REV.getPvDescriptor().getRbHandle();
        }

        /**
         *
         * @since 	Dec 7, 2009
         * @author  Christopher K. Allen
         *
         * @see gov.sns.apps.pta.test.TestWireScannerMonitoring.MonitorBase#valueChanged(gov.sns.ca.ChannelRecord)
         */
        @Override
        public void valueChanged(ChannelRecord recValue) {
            int intVal = recValue.intValue();
            System.out.println(" DAQ device reverse limit switch (value=" + intVal + ")");
            System.out.println("Launching plot");
            plotSimulate();
        }
    }

    /** Relative location of the XAL configuration file */
    public static final String STR_URL_XAL_MAIN = "./gov/sns/apps/pta/test/resources/main.xal";

    /** ID of sequence containing our test device */
    public static final String STR_ID_SEQ = "SCLMed";

    /** ID of test device device */
    public static final String STR_ID_DEV = "SCL_Diag:WS007";

    /** The device under test */
    public static AcceleratorNode SMF_DEV;

    /**
     * setUpBeforeClass
     *
     * @throws java.lang.Exception
     * 
     * @since  Dec 7, 2009
     * @author Christopher K. Allen
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Accelerator xalAccel = XMLDataManager.acceleratorWithPath(STR_URL_XAL_MAIN);
        AcceleratorSeq xalSeq = xalAccel.findSequence(STR_ID_SEQ);
        SMF_DEV = xalSeq.getNodeWithId(STR_ID_DEV);
    }

    /**
     * tearDownAfterClass
     *
     * @throws java.lang.Exception
     * 
     * @since  Dec 7, 2009
     * @author Christopher K. Allen
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * setUp
     *
     * @throws java.lang.Exception
     * 
     * @since  Dec 7, 2009
     * @author Christopher K. Allen
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * tearDown
     *
     * @throws java.lang.Exception
     * 
     * @since  Dec 7, 2009
     * @author Christopher K. Allen
     */
    @After
    public void tearDown() throws Exception {
    }

    public final void testPositionMonitor() {
        int intWaitTm = 2;
        int intWaitDur = 30000;
        WireScanner ws = (WireScanner) SMF_DEV;
        PositionMonitor monPos = new PositionMonitor(ws);
        try {
            monPos.begin();
            ws.runCommand(WireScanner.CMD.SCAN);
        } catch (ConnectionException e) {
            String strMsg = "ConnectionException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (NoSuchChannelException e) {
            String strMsg = "NoSuchChannelException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (MonitorException e) {
            String strMsg = "MonitorException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (PutException e) {
            String strMsg = "PutException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            String strMsg = "InterruptedException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        }
        System.out.println("  Scan position monitoring started.");
        System.out.println("  Sleeping for " + intWaitTm + " x " + intWaitDur / 1000 + " seconds while waiting for monitor events");
        for (int iMinute = 1; iMinute <= intWaitTm; iMinute++) {
            try {
                Thread.sleep(intWaitDur);
                System.out.println("    " + iMinute + " x " + intWaitDur / 1000 + " seconds ");
            } catch (InterruptedException e) {
                System.err.println("Interrupted Thread???");
                e.printStackTrace();
            }
        }
        System.out.println("Awakened.  Returning from scan test");
        System.out.println("  (NOTE: scan may still be in progress)");
        monPos.clear();
    }

    public final void testMotionStatusMonitor() {
        int intWaitTm = 1;
        int intWaitDur = 30000;
        WireScanner ws = (WireScanner) SMF_DEV;
        MotionMonitor monMotion = new MotionMonitor(ws);
        try {
            monMotion.begin();
            ws.runCommand(WireScanner.CMD.SCAN);
        } catch (ConnectionException e) {
            String strMsg = "ConnectionException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (NoSuchChannelException e) {
            String strMsg = "NoSuchChannelException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (MonitorException e) {
            String strMsg = "MonitorException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (PutException e) {
            String strMsg = "PutException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            String strMsg = "PutException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        }
        System.out.println("  Scan motion status monitoring started.");
        System.out.println("  Sleeping for " + intWaitTm + " x " + intWaitDur / 1000 + " seconds while waiting for monitor events");
        for (int iMinute = 1; iMinute <= intWaitTm; iMinute++) {
            try {
                Thread.sleep(intWaitDur);
                System.out.println("    " + iMinute + " x " + intWaitDur / 1000 + " seconds ");
            } catch (InterruptedException e) {
                System.err.println("Interrupted Thread???");
                e.printStackTrace();
            }
        }
        System.out.println("Awakened.  Returning from scan test");
        System.out.println("  (NOTE: scan may still be in progress)");
        monMotion.clear();
    }

    public final void testScanIdMonitor() {
        int intWaitTm = 1;
        int intWaitDur = 30000;
        WireScanner ws = (WireScanner) SMF_DEV;
        ScanIdMonitor monSeqId = new ScanIdMonitor(ws);
        try {
            monSeqId.begin(true);
            ws.runCommand(WireScanner.CMD.SCAN);
        } catch (ConnectionException e) {
            String strMsg = "ConnectionException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (NoSuchChannelException e) {
            String strMsg = "NoSuchChannelException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (MonitorException e) {
            String strMsg = "MonitorException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (PutException e) {
            String strMsg = "PutException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            String strMsg = "PutException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        }
        System.out.println("  Scan seq id monitoring started.");
        System.out.println("  Sleeping for " + intWaitTm + " x " + intWaitDur / 1000 + " seconds while waiting for monitor events");
        for (int iMinute = 1; iMinute <= intWaitTm; iMinute++) {
            try {
                Thread.sleep(intWaitDur);
                System.out.println("    " + iMinute + " x " + intWaitDur / 1000 + " seconds ");
            } catch (InterruptedException e) {
                System.err.println("Interrupted Thread???");
                e.printStackTrace();
            }
        }
        System.out.println("Awakened.  Returning from scan test");
        System.out.println("  (NOTE: scan may still be in progress)");
        monSeqId.clear();
        System.out.println("Launching plot...");
        this.plotSimulate();
        for (int iMinute = 1; iMinute <= intWaitTm; iMinute++) {
            try {
                Thread.sleep(intWaitDur);
                System.out.println("    " + iMinute + " x " + intWaitDur / 1000 + " seconds ");
            } catch (InterruptedException e) {
                System.err.println("Interrupted Thread???");
                e.printStackTrace();
            }
        }
    }

    public final void testLimitSwitchMonitor() {
        int intWaitTm = 1;
        int intWaitDur = 30000;
        WireScanner ws = (WireScanner) SMF_DEV;
        LimitSwitchMonitor monRevLim = new LimitSwitchMonitor(ws);
        try {
            ws.runCommand(WireScanner.CMD.SCAN);
            monRevLim.begin(true);
        } catch (ConnectionException e) {
            String strMsg = "ConnectionException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (NoSuchChannelException e) {
            String strMsg = "NoSuchChannelException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (MonitorException e) {
            String strMsg = "MonitorException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (PutException e) {
            String strMsg = "PutException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            String strMsg = "PutException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        }
        System.out.println("  Limit switch monitoring started.");
        System.out.println("  Sleeping for " + intWaitTm + " x " + intWaitDur / 1000 + " seconds while waiting for monitor events");
        for (int iMinute = 1; iMinute <= intWaitTm; iMinute++) {
            try {
                Thread.sleep(intWaitDur);
                System.out.println("    " + iMinute + " x " + intWaitDur / 1000 + " seconds ");
            } catch (InterruptedException e) {
                System.err.println("Interrupted Thread???");
                e.printStackTrace();
            }
        }
        System.out.println("Awakened.  Returning from scan test");
        System.out.println("  (NOTE: scan may still be in progress)");
        monRevLim.clear();
        System.out.println("Launching plot...");
        this.plotSimulate();
        for (int iMinute = 1; iMinute <= intWaitTm; iMinute++) {
            try {
                Thread.sleep(intWaitDur);
                System.out.println("    " + iMinute + " x " + intWaitDur / 1000 + " seconds ");
            } catch (InterruptedException e) {
                System.err.println("Interrupted Thread???");
                e.printStackTrace();
            }
        }
    }

    /**
     * Test method for {@link gov.sns.ca.Monitor#clear()}.
     */
    @Test
    public final void testDataRetrieve() {
        int intWaitTm = 1;
        int intWaitDur = 30000;
        WireScanner ws = (WireScanner) SMF_DEV;
        ScanIdMonitor monSeqId = new ScanIdMonitor(ws);
        PositionMonitor monPos = new PositionMonitor(ws);
        try {
            monSeqId.begin(true);
            monPos.begin();
            ws.runCommand(WireScanner.CMD.SCAN);
        } catch (ConnectionException e) {
            String strMsg = "ConnectionException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (NoSuchChannelException e) {
            String strMsg = "NoSuchChannelException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (MonitorException e) {
            String strMsg = "MonitorException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (PutException e) {
            String strMsg = "PutException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            String strMsg = "PutException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        }
        System.out.println("  Scan seq id monitoring started.");
        System.out.println("  Sleeping for " + intWaitTm + " x " + intWaitDur / 1000 + " seconds while waiting for monitor events");
        for (int iMinute = 1; iMinute <= intWaitTm; iMinute++) {
            try {
                Thread.sleep(intWaitDur);
                System.out.println("    " + iMinute + " x " + intWaitDur / 1000 + " seconds ");
            } catch (InterruptedException e) {
                System.err.println("Interrupted Thread???");
                e.printStackTrace();
            }
        }
        System.out.println("Awakened.  Returning from scan test");
        System.out.println("  (NOTE: scan may still be in progress)");
        monSeqId.clear();
        System.out.println("Launching plot...");
        this.plotSimulate();
        for (int iMinute = 1; iMinute <= intWaitTm; iMinute++) {
            try {
                Thread.sleep(intWaitDur);
                System.out.println("    " + iMinute + " x " + intWaitDur / 1000 + " seconds ");
            } catch (InterruptedException e) {
                System.err.println("Interrupted Thread???");
                e.printStackTrace();
            }
        }
    }

    /**
     * createLimitSwitchMonitor
     *
     * 
     * @since  Dec 8, 2009
     * @author Christopher K. Allen
     */
    public void createLimitSwitchMonitor() {
        WireScanner ws = (WireScanner) SMF_DEV;
        LimitSwitchMonitor monRevLim = new LimitSwitchMonitor(ws);
        try {
            monRevLim.begin(true);
        } catch (ConnectionException e) {
            String strMsg = "ConnectionException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (NoSuchChannelException e) {
            String strMsg = "NoSuchChannelException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        } catch (MonitorException e) {
            String strMsg = "MonitorException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
            return;
        }
    }

    /**
     * plotSimulate
     *
     * 
     * @since  Dec 7, 2009
     * @author Christopher K. Allen
     */
    public void plotSimulate() {
        int szWd = 250;
        int szHt = 200;
        Dimension dimPlt = new Dimension(szWd, szHt);
        try {
            WireScanner ws = (WireScanner) SMF_DEV;
            WireScanner.DataRaw data = WireScanner.DataRaw.acquire(ws);
            BasicGraphData datHor = new BasicGraphData();
            BasicGraphData datVer = new BasicGraphData();
            BasicGraphData datDia = new BasicGraphData();
            datHor.addPoint(data.hor.pos, data.hor.val);
            datVer.addPoint(data.ver.pos, data.ver.val);
            datDia.addPoint(data.dia.pos, data.dia.val);
            FunctionGraphsJPanel pnlPltHor = new FunctionGraphsJPanel();
            pnlPltHor.setPreferredSize(dimPlt);
            pnlPltHor.addGraphData(datHor);
            FunctionGraphsJPanel pnlPltVer = new FunctionGraphsJPanel();
            pnlPltVer.setPreferredSize(dimPlt);
            pnlPltVer.addGraphData(datVer);
            FunctionGraphsJPanel pnlPltDia = new FunctionGraphsJPanel();
            pnlPltDia.setPreferredSize(dimPlt);
            pnlPltDia.addGraphData(datDia);
            Box boxPlots = Box.createHorizontalBox();
            boxPlots.add(pnlPltHor);
            boxPlots.add(pnlPltVer);
            boxPlots.add(pnlPltDia);
            JFrame frame = new JFrame();
            frame.getContentPane().add(boxPlots);
            frame.setSize(3 * szWd + 20, szHt);
            frame.setVisible(true);
        } catch (NoSuchChannelException e) {
            String strMsg = "NoSuchChannelException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
        } catch (ConnectionException e) {
            String strMsg = "ConnectionException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
        } catch (GetException e) {
            String strMsg = "GetException: " + e.getMessage();
            System.err.println(strMsg);
            fail(strMsg);
            e.printStackTrace();
        }
    }
}

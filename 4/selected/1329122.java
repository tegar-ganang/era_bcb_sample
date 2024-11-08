package gov.sns.apps.pta.test;

import static org.junit.Assert.fail;
import gov.aps.jca.CAException;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.TimeoutException;
import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ChannelRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.GetException;
import gov.sns.ca.IEventSinkValue;
import gov.sns.ca.MonitorException;
import gov.sns.ca.PutException;
import gov.sns.xal.smf.Accelerator;
import gov.sns.xal.smf.AcceleratorNode;
import gov.sns.xal.smf.NoSuchChannelException;
import gov.sns.xal.smf.data.XMLDataManager;
import gov.sns.xal.smf.impl.WireScanner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.cosylab.epics.caj.CAJContext;

/**
 *
 *
 * @since  Jun 19, 2009
 * @author Christopher K. Allen
 */
public class TestJca {

    /**
     *
     *
     * @since  Nov 4, 2009
     * @author Christopher K. Allen
     */
    class JcaMonitorSink implements IEventSinkValue {

        /**
         *
         * @since 	Nov 4, 2009
         * @author  Christopher K. Allen
         *
         * @see gov.sns.ca.IEventSinkValue#eventValue(gov.sns.ca.ChannelRecord, gov.sns.ca.Channel)
         */
        public void eventValue(ChannelRecord record, Channel chan) {
            String strPvName = chan.channelName();
            int intVal = record.intValue();
            System.out.println("Monitor event for PV " + strPvName + ", value = " + intVal);
        }
    }

    /**
     * setUpBeforeClass
     *
     * @throws java.lang.Exception
     * 
     * @since  Jun 19, 2009
     * @author Christopher K. Allen
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * tearDownAfterClass
     *
     * @throws java.lang.Exception
     * 
     * @since  Jun 19, 2009
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
     * @since  Jun 19, 2009
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
     * @since  Jun 19, 2009
     * @author Christopher K. Allen
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Print out Java virtual machine properties
     *
     * 
     * @since  Nov 4, 2009
     * @author Christopher K. Allen
     */
    @Test
    public void testSystem() {
        System.out.println();
        System.out.println("Java Virtual Machine Properties");
        System.out.print(System.getProperties().toString());
    }

    /**
     * testJcaExample - example code (slightly modified) on 
     * APS's web site 
     * <a href=http://www.aps.anl.gov/bcda/jca/jca/2.1.2/tutorial.html#5>APS Example</a>.
     *
     * 
     * @since  Jun 19, 2009
     * @author Christopher K. Allen
     */
    @Test
    public void testJcaPrintInfo() {
        System.out.println();
        System.out.println();
        System.out.println("This is JCA version");
        try {
            JCALibrary jca = JCALibrary.getInstance();
            Context ctxt = jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
            ctxt.printInfo();
            gov.aps.jca.Channel ch = ctxt.createChannel("SCL_Diag:WS007:Command");
            ctxt.pendIO(5.0);
            ch.printInfo();
            ch.destroy();
            ctxt.destroy();
        } catch (Exception ex) {
            System.err.println(ex);
            fail(ex.getMessage());
        }
    }

    /**
     * Test method for {@link gov.aps.jca.JCALibrary#printInfo()}.
     */
    @Test
    public final void testCajPrintInfo() {
        System.out.println();
        System.out.println();
        System.out.println("This is the CAJ version");
        CAJContext ctxt = new CAJContext();
        ctxt.printInfo();
        gov.aps.jca.Channel ch;
        try {
            ch = ctxt.createChannel("SCL_Diag:WS007:Command");
            ctxt.pendIO(5.0);
            ch.printInfo();
            ch.destroy();
            ctxt.destroy();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            fail("CAJ IllegalArgumentException ");
        } catch (IllegalStateException e) {
            e.printStackTrace();
            fail("CAJ IllegalStateException ");
        } catch (CAException e) {
            e.printStackTrace();
            fail("CAJ CAException ");
        } catch (TimeoutException e) {
            e.printStackTrace();
            fail("CAJ TimeoutException ");
        }
    }

    /**
     * Test method for {@link gov.aps.jca.JCALibrary#printInfo()}.
     */
    @Test
    public final void testXalChannel() {
        String strPvNmGet = "HEBT_Diag:WS09:Hor_trace_raw";
        String strPvNmSet = "SCL_Diag:WS007:Scan_InitialMove_set";
        System.out.println();
        System.out.println();
        System.out.println("Testing XAL connection to PV " + strPvNmGet);
        ChannelFactory factory = ChannelFactory.defaultFactory();
        Channel chan = factory.getChannel(strPvNmGet);
        try {
            int intVal = chan.getValInt();
            System.out.println("The current value of " + strPvNmGet + " = " + intVal);
            intVal = chan.lowerControlLimit().intValue();
            System.out.println("The current lower display limit is " + intVal);
            intVal = chan.upperControlLimit().intValue();
            System.out.println("The current upper display limit is " + intVal);
            Channel chput = factory.getChannel(strPvNmSet);
            chput.putVal(15);
        } catch (ConnectionException e) {
            System.err.println("ERROR: Unable to connect to " + strPvNmGet);
            e.printStackTrace();
            fail("ERROR: Unable to connect to " + strPvNmGet);
        } catch (GetException e) {
            System.err.println("ERROR: general caget exception for " + strPvNmGet);
            e.printStackTrace();
            fail("ERROR: general caget exception for " + strPvNmGet);
        } catch (PutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test the ability to get Live Data from the
     * wire scanner.
     *
     * 
     * @since  Feb 5, 2010
     * @author Christopher K. Allen
     */
    @Test
    public final void textWireScannerData() {
        Accelerator accel = XMLDataManager.loadDefaultAccelerator();
        AcceleratorNode smfNode = accel.getNode("SCL_Diag:WS007");
        if (!(smfNode instanceof WireScanner)) {
            fail(smfNode.getId() + " is not a wire scanner");
            return;
        }
        WireScanner ws = (WireScanner) smfNode;
        try {
            WireScanner.DataLive data = WireScanner.DataLive.acquire(ws);
            System.out.println();
            System.out.println("WireScanner.DataLive data structure:");
            System.out.print(data.toString());
        } catch (ConnectionException e) {
            fail("Could not retrieve data from " + ws.getId());
            e.printStackTrace();
        } catch (GetException e) {
            fail("Could not retrieve data from " + ws.getId());
            e.printStackTrace();
        } catch (NoSuchChannelException e) {
            fail("Could not retrieve data from " + ws.getId());
            e.printStackTrace();
        }
    }

    public final void testXalMonitor() {
        String strPvName = "SCL_Diag:WS007:Ver_point_sig";
        System.out.println();
        System.out.println();
        System.out.println("Testing XAL monitor on PV " + strPvName);
        ChannelFactory factory = ChannelFactory.defaultFactory();
        Channel chan = factory.getChannel(strPvName);
        try {
            JcaMonitorSink sink = new JcaMonitorSink();
            gov.sns.ca.Monitor monitor = chan.addMonitorValue(sink, Monitor.VALUE);
        } catch (ConnectionException e) {
            System.err.println("ERROR: XAL Connection exception " + strPvName);
            e.printStackTrace();
            fail("ERROR: XAL Connection exception " + strPvName);
        } catch (MonitorException e) {
            System.err.println("ERROR: XAL monitor exception " + strPvName);
            e.printStackTrace();
            fail("ERROR: XAL monitor exception " + strPvName);
        }
    }

    public final void testProfileScan() {
        int intWaitTm = 2;
        String strSigName = "SCL_Diag:WS007:Command";
        String strMonName = "SCL_Diag:WS007:MotionStat";
        System.out.println();
        System.out.println();
        System.out.println("Testing Profile Scan using " + strSigName);
        System.out.println("  monitoring on PV " + strMonName);
        ChannelFactory factory = ChannelFactory.defaultFactory();
        Channel chanCmd = factory.getChannel(strSigName);
        Channel chanMon = factory.getChannel(strMonName);
        try {
            chanCmd.connectAndWait(10);
            chanMon.connectAndWait(10);
            JcaMonitorSink sink = new JcaMonitorSink();
            gov.sns.ca.Monitor monitor = chanMon.addMonitorValue(sink, Monitor.VALUE);
            chanCmd.putVal(3);
            System.out.println("  Scan started.");
            System.out.println("  Sleeping for " + intWaitTm + " minutes while waiting for monitor events");
            for (int iMinute = 1; iMinute <= intWaitTm; iMinute++) {
                Thread.sleep(60000);
                System.out.println("    " + iMinute + " minute(s)");
            }
            System.out.println("Awakened.  Returning from scan test");
            System.out.println("  (NOTE: scan may still be in progress)");
            monitor.clear();
        } catch (ConnectionException e) {
            System.err.println("ERROR: XAL Connection exception " + strSigName);
            e.printStackTrace();
            fail("ERROR: XAL Connection exception " + strSigName);
        } catch (PutException e) {
            System.err.println("ERROR: Profile Scan exception " + strSigName);
            e.printStackTrace();
            fail("ERROR: Profile Scann exception " + strSigName);
        } catch (MonitorException e) {
            System.err.println("ERROR: Profile Scan monitoring exception " + strMonName);
            e.printStackTrace();
            fail("ERROR: Profile Scann monitoring exception " + strMonName);
        } catch (InterruptedException e) {
            System.err.println("Scanning sleep thread interrupted while scanning");
            e.printStackTrace();
            fail("Scanning sleep thread interrupted while scanning");
        }
    }

    /**
     * Test method for {@link gov.aps.jca.JCALibrary#createContext(java.lang.String)}.
     */
    @Test
    public final void testCreateContextString() {
    }

    /**
     * Test method for {@link gov.aps.jca.JCALibrary#createContext(gov.aps.jca.configuration.Configuration)}.
     */
    @Test
    public final void testCreateContextConfiguration() {
    }

    /**
     * Test method for {@link gov.aps.jca.JCALibrary#createServerContext(java.lang.String, gov.aps.jca.cas.Server)}.
     */
    @Test
    public final void testCreateServerContextStringServer() {
    }

    /**
     * Test method for {@link gov.aps.jca.JCALibrary#createServerContext(gov.aps.jca.configuration.Configuration, gov.aps.jca.cas.Server)}.
     */
    @Test
    public final void testCreateServerContextConfigurationServer() {
    }
}

package test.tuwien.auto.calimero.buffer;

import java.util.Date;
import junit.framework.TestCase;
import test.tuwien.auto.calimero.Util;
import test.tuwien.auto.calimero.knxnetip.Debug;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.buffer.CommandFilter;
import tuwien.auto.calimero.buffer.Configuration;
import tuwien.auto.calimero.buffer.LDataObjectQueue;
import tuwien.auto.calimero.buffer.NetworkBuffer;
import tuwien.auto.calimero.buffer.StateFilter;
import tuwien.auto.calimero.buffer.LDataObjectQueue.QueueItem;
import tuwien.auto.calimero.datapoint.DatapointMap;
import tuwien.auto.calimero.datapoint.StateDP;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessCommunicatorImpl;

/**
 * @author B. Malinowsky
 */
public class NetworkBufferTest extends TestCase {

    private KNXNetworkLink lnk;

    /**
	 * @param name name of test case
	 */
    public NetworkBufferTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        lnk = new KNXNetworkLinkIP(KNXNetworkLinkIP.TUNNEL, null, Util.getServer(), false, TPSettings.TP1);
        LogManager.getManager().addWriter(lnk.getName(), Util.getLogWriter());
        LogManager.getManager().addWriter(NetworkBuffer.LOG_SERVICE, Util.getLogWriter());
    }

    protected void tearDown() throws Exception {
        if (lnk != null) lnk.close();
        final NetworkBuffer[] b = NetworkBuffer.getAllBuffers();
        for (int i = 0; i < b.length; i++) {
            NetworkBuffer.removeBuffer(b[i].getInstallationID());
        }
        LogManager.getManager().removeWriter(lnk.getName(), Util.getLogWriter());
        LogManager.getManager().removeWriter(NetworkBuffer.LOG_SERVICE, Util.getLogWriter());
        super.tearDown();
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.NetworkBuffer#createBuffer(java.lang.String)}.
	 */
    public final void testCreateBuffer() {
        final NetworkBuffer b = NetworkBuffer.createBuffer("testInstallation");
        assertEquals(b, NetworkBuffer.getBuffer("testInstallation"));
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.NetworkBuffer#getBuffer(java.lang.String)}.
	 */
    public final void testGetBuffer() {
        assertNull(NetworkBuffer.getBuffer("notFound"));
        NetworkBuffer.createBuffer("notfound");
        assertNull(NetworkBuffer.getBuffer("notFound"));
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.NetworkBuffer#removeBuffer(java.lang.String)}.
	 */
    public final void testRemoveBuffer() {
        NetworkBuffer.removeBuffer("notAnInstallation");
        NetworkBuffer.createBuffer("forRemove");
        NetworkBuffer.createBuffer("forremove");
        NetworkBuffer.removeBuffer("forRemove");
        assertNull(NetworkBuffer.getBuffer("forRemove"));
        assertNotNull(NetworkBuffer.getBuffer("forremove"));
        NetworkBuffer.removeBuffer("forremove");
        assertNull(NetworkBuffer.getBuffer("forremove"));
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.NetworkBuffer#createConfiguration(
	 * tuwien.auto.calimero.link.KNXNetworkLink, java.lang.String)}.
	 */
    public final void testCreateConfigurationKNXNetworkLinkString() {
        final Configuration c = NetworkBuffer.createConfiguration(lnk, null);
        final NetworkBuffer b = NetworkBuffer.getBuffer("Installation 1");
        assertEquals(c, b.getConfiguration(c.getBufferedLink()));
        assertFalse(c.isActive());
        assertEquals(lnk, c.getBaseLink());
        assertNull(c.getCache());
        assertNull(c.getDatapointModel());
        assertNull(c.getNetworkFilter());
        NetworkBuffer.removeConfiguration(c, "Installation 2");
        assertEquals(c, b.getConfiguration(c.getBufferedLink()));
        NetworkBuffer.removeConfiguration(c, "Installation 1");
        assertNull(b.getConfiguration(c.getBufferedLink()));
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.NetworkBuffer#createConfiguration(
	 * tuwien.auto.calimero.link.KNXNetworkLink)}.
	 */
    public final void testCreateConfigurationKNXNetworkLink() {
        final NetworkBuffer b = NetworkBuffer.createBuffer(null);
        b.createConfiguration(lnk);
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.buffer.NetworkBuffer#getConfiguration(
	 * tuwien.auto.calimero.link.KNXNetworkLink)}.
	 */
    public final void testGetConfiguration() {
        final Configuration c = NetworkBuffer.createConfiguration(lnk, "testGetConfig");
        final Configuration c2 = NetworkBuffer.getBuffer("testGetConfig").getConfiguration(c.getBufferedLink());
        assertEquals(c, c2);
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.buffer.NetworkBuffer#removeConfiguration(
	 * tuwien.auto.calimero.buffer.Configuration)}.
	 */
    public final void testRemoveConfigurationConfiguration() {
        final Configuration c = NetworkBuffer.createConfiguration(lnk, "testRemoveConfig");
        final NetworkBuffer b = NetworkBuffer.getBuffer("testRemoveConfig");
        final KNXNetworkLink buf = c.getBufferedLink();
        b.removeConfiguration(c);
        assertNull(b.getConfiguration(buf));
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.buffer.NetworkBuffer#getAllBuffers()}.
	 */
    public final void testGetAllBuffers() {
        final NetworkBuffer[] b = NetworkBuffer.getAllBuffers();
        assertNotNull(b);
        NetworkBuffer.createBuffer(null);
        final NetworkBuffer[] b2 = NetworkBuffer.getAllBuffers();
        assertEquals(1, b2.length);
        assertTrue(b2[0].getInstallationID().startsWith("Installation "));
    }

    /**
	 * Test method for state based buffering.
	 * 
	 * @throws InterruptedException
	 * @throws KNXException
	 */
    public final void testStateBasedBuffering() throws InterruptedException, KNXException {
        final GroupAddress group1 = new GroupAddress(0, 0, 1);
        final GroupAddress group2 = new GroupAddress(0, 0, 2);
        final Configuration c = NetworkBuffer.createConfiguration(lnk, "statebasedBuffer");
        assertEquals(c.getBufferedLink().getName(), "buffered " + lnk.getName());
        final StateFilter f = new StateFilter();
        c.setFilter(f, f);
        c.activate(true);
        final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());
        pc.write(group2, true);
        Thread.sleep(50);
        assertTrue(pc.readBool(group2));
        pc.write(group2, false);
        Thread.sleep(50);
        assertFalse(pc.readBool(group2));
        final boolean b1 = pc.readBool(group1);
        final ProcessCommunicator pc2 = new ProcessCommunicatorImpl(lnk);
        final boolean b2 = pc2.readBool(group1);
        assertEquals(b1, b2);
        pc.write(group2, true);
        Thread.sleep(50);
        assertTrue(pc.readBool(group2));
        pc2.write(group1, true);
        Thread.sleep(50);
        assertEquals(pc.readBool(group1), pc2.readBool(group1));
        pc2.write(group1, false);
        Thread.sleep(50);
        assertEquals(pc.readBool(group1), pc2.readBool(group1));
        final DatapointMap map = new DatapointMap();
        final StateDP dp = new StateDP(group1, "group1", 0, "1.001");
        dp.setExpirationTimeout(2);
        map.add(dp);
        c.setDatapointModel(map);
        pc.read(dp);
        Thread.sleep(500);
        pc.read(dp);
        Thread.sleep(2500);
        pc.read(dp);
        pc.read(dp);
        NetworkBuffer.removeBuffer("statebasedBuffer");
    }

    /**
	 * Test method for state based buffering.
	 * 
	 * @throws KNXException
	 * @throws InterruptedException
	 */
    public final void testInvalidationUpdating() throws KNXException, InterruptedException {
        final GroupAddress group1 = new GroupAddress(0, 0, 1);
        final GroupAddress group2 = new GroupAddress(0, 0, 2);
        final GroupAddress group3 = new GroupAddress(0, 0, 3);
        final Configuration c = NetworkBuffer.createConfiguration(lnk, "update/invalidate");
        final DatapointMap map = new DatapointMap();
        final StateDP dp = new StateDP(group1, "group1", 0, "1.001");
        dp.add(group2, false);
        dp.add(group3, true);
        final StateDP dp2 = new StateDP(group2, "group2", 0, "1.001");
        final StateDP dp3 = new StateDP(group3, "group3", 0, "1.001");
        map.add(dp);
        map.add(dp2);
        map.add(dp3);
        c.setDatapointModel(map);
        final StateFilter f = new StateFilter();
        c.setFilter(f, f);
        final ProcessCommunicator init = new ProcessCommunicatorImpl(lnk);
        init.write(dp, "off");
        init.write(dp2, "on");
        init.write(dp3, "on");
        init.detach();
        c.activate(true);
        final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());
        final String s1 = pc.read(dp);
        Thread.sleep(50);
        final String s2 = pc.read(dp2);
        assertEquals(s1, pc.read(dp));
        final String s3 = pc.read(dp3);
        assertEquals(s3, pc.read(dp));
        final String write = s3.equals("off") ? "on" : "off";
        pc.write(dp3, write);
        assertEquals(write, pc.read(dp));
        assertEquals(write, pc.read(dp3));
        assertEquals(write, pc.read(dp));
        pc.write(group3, true);
        Thread.sleep(50);
        assertTrue(pc.readBool(group1));
        pc.write(group3, false);
        Thread.sleep(50);
        assertFalse(pc.readBool(group1));
        pc.write(group2, false);
        Thread.sleep(50);
        assertEquals(s1, pc.read(dp));
        pc.write(group2, false);
        Thread.sleep(50);
        assertEquals(s1, pc.read(dp));
        NetworkBuffer.removeBuffer("update/invalidate");
    }

    /**
	 * Test method for command based buffering.
	 * 
	 * @throws InterruptedException
	 * @throws KNXException
	 */
    public final void testCommandBasedBuffering() throws InterruptedException, KNXException {
        final GroupAddress group1 = new GroupAddress(0, 0, 1);
        final GroupAddress group2 = new GroupAddress(0, 0, 2);
        final Configuration c = NetworkBuffer.createConfiguration(lnk, "cmdbasedBuffer");
        final CommandFilter f = new CommandFilter();
        c.setFilter(f, f);
        c.activate(true);
        final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());
        pc.write(group2, true);
        Thread.sleep(1000);
        pc.write(group2, false);
        Thread.sleep(50);
        assertTrue(f.hasNewIndication());
        final QueueItem qi = f.getNextIndication();
        Debug.printLData(qi.getFrame());
        System.out.println(new Date(qi.getTimestamp()));
        assertTrue(f.hasNewIndication());
        final QueueItem qi2 = f.getNextIndication();
        Debug.printLData(qi2.getFrame());
        System.out.println(new Date(qi2.getTimestamp()));
        assertTrue(qi.getTimestamp() < (qi2.getTimestamp() - 700));
        assertFalse(f.hasNewIndication());
        final class ListenerImpl implements LDataObjectQueue.QueueListener {

            boolean filled;

            public void queueFilled(LDataObjectQueue queue) {
                filled = true;
                assertEquals(10, queue.getFrames().length);
                assertEquals(0, queue.getFrames().length);
            }
        }
        final ListenerImpl listener = new ListenerImpl();
        f.setQueueListener(listener);
        final ProcessCommunicator pc2 = new ProcessCommunicatorImpl(lnk);
        pc2.readBool(group1);
        Thread.sleep(50);
        assertTrue(f.hasNewIndication());
        pc2.write(group2, true);
        Thread.sleep(50);
        for (int i = 0; i < 9; ++i) pc2.write(group2, i % 2 == 1);
        Thread.sleep(50);
        assertTrue(listener.filled);
        assertTrue(f.hasNewIndication());
        try {
            QueueItem qi3 = f.getNextIndication();
            assertEquals(group1, qi3.getFrame().getDestination());
            assertTrue(f.hasNewIndication());
            for (int i = 0; i < 10; ++i) {
                qi3 = f.getNextIndication();
                assertEquals(0, qi3.getTimestamp());
                assertNull(qi3.getFrame());
            }
            assertFalse(f.hasNewIndication());
        } catch (final KNXIllegalStateException e) {
        }
        NetworkBuffer.removeBuffer("cmdbasedBuffer");
    }

    /**
	 * Test method for query buffer only mode.
	 * 
	 * @throws InterruptedException
	 * @throws KNXException
	 */
    public final void testQueryBufferOnly() throws InterruptedException, KNXException {
        final GroupAddress group1 = new GroupAddress(0, 0, 1);
        final GroupAddress group2 = new GroupAddress(0, 0, 2);
        final Configuration c = NetworkBuffer.createConfiguration(lnk, "statebasedBuffer");
        final StateFilter f = new StateFilter();
        c.setFilter(f, f);
        c.setQueryBufferOnly(true);
        c.activate(true);
        final ProcessCommunicator pc = new ProcessCommunicatorImpl(c.getBufferedLink());
        pc.write(group2, true);
        Thread.sleep(50);
        assertTrue(pc.readBool(group2));
        pc.write(group2, false);
        Thread.sleep(50);
        assertFalse(pc.readBool(group2));
        try {
            pc.readBool(group1);
            fail("there should be no " + group1 + " value in the buffer");
        } catch (final KNXTimeoutException e) {
        }
        final ProcessCommunicator pc2 = new ProcessCommunicatorImpl(lnk);
        final boolean b2 = pc2.readBool(group1);
        pc.write(group2, true);
        Thread.sleep(50);
        assertTrue(pc.readBool(group2));
        pc2.write(group1, true);
        Thread.sleep(50);
        assertEquals(pc.readBool(group1), pc2.readBool(group1));
        pc2.write(group1, false);
        Thread.sleep(50);
        assertEquals(pc.readBool(group1), pc2.readBool(group1));
        final DatapointMap map = new DatapointMap();
        final StateDP dp = new StateDP(group1, "group1", 0, "1.001");
        dp.setExpirationTimeout(2);
        map.add(dp);
        c.setDatapointModel(map);
        pc.read(dp);
        Thread.sleep(500);
        pc.read(dp);
        Thread.sleep(2500);
        try {
            pc.read(dp);
            fail(group1 + " value in buffer should be too old");
        } catch (final KNXTimeoutException e) {
        }
        try {
            pc.read(dp);
            fail(group1 + " value in buffer should be too old");
        } catch (final KNXTimeoutException e) {
        }
        NetworkBuffer.removeBuffer("statebasedBuffer");
    }
}

package test.tuwien.auto.calimero.mgmt;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import test.tuwien.auto.calimero.Util;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.mgmt.Destination;
import tuwien.auto.calimero.mgmt.KNXDisconnectException;
import tuwien.auto.calimero.mgmt.ManagementClient;
import tuwien.auto.calimero.mgmt.ManagementClientImpl;

/**
 * @author B. Malinowsky
 */
public class ManagementClientImplTest extends TestCase {

    private KNXNetworkLink lnk;

    private ManagementClient mc;

    private Destination dco, dco2;

    private Destination dcl;

    /**
	 * @param name name of test case
	 */
    public ManagementClientImplTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        LogManager.getManager().addWriter(null, Util.getLogWriter());
        lnk = new KNXNetworkLinkIP(KNXNetworkLinkIP.TUNNEL, null, Util.getServer(), false, TPSettings.TP1);
        mc = new ManagementClientImpl(lnk);
        dco2 = mc.createDestination(Util.getRouterAddress(), true);
        dco = dco2;
        dcl = mc.createDestination(new IndividualAddress(3, 1, 1), false);
    }

    protected void tearDown() throws Exception {
        if (mc != null) mc.detach();
        if (lnk != null) lnk.close();
        LogManager.getManager().removeWriter(null, Util.getLogWriter());
        super.tearDown();
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#ManagementClientImpl(
	 * tuwien.auto.calimero.link.KNXNetworkLink)}.
	 */
    public final void testManagementClientImpl() {
        mc.detach();
        lnk.close();
        try {
            mc = new ManagementClientImpl(lnk);
            fail("link closed");
        } catch (final KNXLinkClosedException e) {
        }
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#authorize
	 * (tuwien.auto.calimero.mgmt.Destination, byte[])}.
	 * 
	 * @throws KNXException
	 */
    public final void testAuthorize() throws KNXException {
        try {
            mc.authorize(dco, new byte[] { 0x10, 0x10, 0x10 });
            fail("invalid key length");
        } catch (final KNXIllegalArgumentException e) {
        }
        int level = 0;
        try {
            level = mc.authorize(dco, new byte[] { 0x10, 0x10, 0x10, 0x10 });
            fail("key does not exist");
        } catch (final KNXDisconnectException e) {
        }
        final byte[] key = new byte[] { 0x10, 0x20, 0x30, 0x40 };
        final byte[] key2 = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
        final byte[] key3 = new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
        try {
            level = mc.authorize(dco, key2);
        } catch (final KNXTimeoutException e) {
        }
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#authorize
	 * (tuwien.auto.calimero.mgmt.Destination, byte[])}.
	 * 
	 * @throws KNXException
	 */
    public final void testAuthorizeCL() throws KNXException {
        try {
            final int level = mc.authorize(dcl, new byte[] { 0x10, 0x10, 0x10, 0x10 });
            fail("connection less");
        } catch (final KNXDisconnectException e) {
        }
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readADC
	 * (tuwien.auto.calimero.mgmt.Destination, int, int)}.
	 * 
	 * @throws KNXException
	 */
    public final void testReadADC() throws KNXException {
        try {
            mc.readADC(dco, -1, 1);
            fail("invalid channel");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readADC(dco, 64, 1);
            fail("invalid channel");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readADC(dco, 1, -1);
            fail("invalid repeat");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readADC(dco, -1, 256);
            fail("invalid repeat");
        } catch (final KNXIllegalArgumentException e) {
        }
        final Destination adcDst = mc.createDestination(new IndividualAddress(4, 5, 1), true);
        final int adc = mc.readADC(adcDst, 1, 1);
        assertTrue(adc > 0);
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readADC
	 * (tuwien.auto.calimero.mgmt.Destination, int, int)}.
	 * 
	 * @throws KNXException
	 */
    public final void testReadADCCL() throws KNXException {
        try {
            final int adc = mc.readADC(dcl, 1, 1);
            fail("connection less");
        } catch (final KNXDisconnectException e) {
        }
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readAddress(boolean)}.
	 * 
	 * @throws InterruptedException
	 * @throws KNXException
	 */
    public final void testReadAddressBoolean() throws InterruptedException, KNXException {
        System.out.println("put device into prog mode for read address...");
        Thread.sleep(5000);
        IndividualAddress[] ias = mc.readAddress(true);
        assertTrue(ias.length <= 1);
        System.out.println(ias[0]);
        final long start = System.currentTimeMillis();
        ias = mc.readAddress(false);
        assertTrue(System.currentTimeMillis() - start >= mc.getResponseTimeout() * 1000);
        System.out.println(ias[0]);
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readAddress(byte[])}.
	 * 
	 * @throws KNXException
	 */
    public final void testReadAddressByteArray() throws KNXException {
        try {
            mc.readAddress(new byte[] { 0x10, 0x10, 0x10, 0x10, 0x10 });
            fail("invalid SN length");
        } catch (final KNXIllegalArgumentException e) {
        }
        final byte[] sno = new byte[] { 0x00, 0x01, 0x00, 0x11, (byte) 0xcb, 0x08 };
        final IndividualAddress addr = new IndividualAddress("3.0.1");
        final IndividualAddress ia = mc.readAddress(sno);
        assertEquals(addr, ia);
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readMemory
	 * (tuwien.auto.calimero.mgmt.Destination, int, int)}.
	 * 
	 * @throws KNXException
	 */
    public final void testReadMemory() throws KNXException {
        try {
            mc.readMemory(dco, -1, 2);
            fail("invalid mem address");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readMemory(dco, 0x10000, 2);
            fail("invalid mem address");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readMemory(dco, 0x100, -1);
            fail("invalid mem range");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readMemory(dco, 0x100, 64);
            fail("invalid mem range");
        } catch (final KNXIllegalArgumentException e) {
        }
        final byte[] mem = mc.readMemory(dco2, 0x105, 2);
        Util.out("read mem from 0x105 = ", mem);
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readMemory
	 * (tuwien.auto.calimero.mgmt.Destination, int, int)}.
	 * 
	 * @throws KNXException
	 */
    public final void testReadMemoryCL() throws KNXException {
        try {
            final byte[] mem = mc.readMemory(dcl, 0x105, 2);
            fail("connection less");
        } catch (final KNXDisconnectException e) {
        }
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readProperty
	 * (tuwien.auto.calimero.mgmt.Destination, int, int, int, int)}.
	 * 
	 * @throws KNXException
	 */
    public final void testReadPropertyDestinationIntIntIntInt() throws KNXException {
        try {
            mc.readProperty(dco, -1, 2, 1, 1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readProperty(dco, 256, 2, 1, 1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readProperty(dco, 1, -1, 1, 1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readProperty(dco, 1, 256, 1, 1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readProperty(dco, 0, 2, -1, 1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readProperty(dco, -1, 2, 0x1000, 1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readProperty(dco, 1, 2, 1, -1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readProperty(dco, 1, 2, 1, 16);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        final byte[] prop = mc.readProperty(dco2, 0, 11, 1, 1);
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readProperty
	 * (tuwien.auto.calimero.mgmt.Destination, int, int, int, int)}.
	 * 
	 * @throws KNXException
	 */
    public final void testReadPropertyDestinationIntIntIntIntCL() throws KNXException {
        dco2.destroy();
        final Destination connless = mc.createDestination(Util.getRouterAddress(), false);
        final byte[] prop = mc.readProperty(connless, 0, 11, 1, 1);
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readPropertyDesc
	 * (tuwien.auto.calimero.mgmt.Destination, int, int, int)}.
	 * 
	 * @throws KNXException
	 */
    public final void testReadPropertyDescDestinationIntIntInt() throws KNXException {
        try {
            mc.readPropertyDesc(dco, -1, 2, 1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readPropertyDesc(dco, 256, 2, 1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readPropertyDesc(dco, 1, -1, 1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readPropertyDesc(dco, 1, 256, 1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readPropertyDesc(dco, 0, 2, -1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readPropertyDesc(dco, -1, 2, 256);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        byte[] desc = mc.readPropertyDesc(dco2, 0, 51, 5);
        desc = mc.readPropertyDesc(dco2, 0, 0, 1);
        final byte[] cmp = mc.readPropertyDesc(dco2, 0, 1, 5);
        desc = mc.readPropertyDesc(dco2, 0, 0, 0);
        assertTrue(Arrays.equals(desc, cmp));
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeAddress
	 * (tuwien.auto.calimero.IndividualAddress)}.
	 * 
	 * @throws InterruptedException
	 * @throws KNXException
	 */
    public final void testWriteAddressIndividualAddress() throws InterruptedException, KNXException {
        System.out.println("put device into prog mode for write address...");
        Thread.sleep(5000);
        final IndividualAddress[] orig = mc.readAddress(true);
        assertEquals(1, orig.length);
        final int dev = (int) (Math.random() * 15);
        final IndividualAddress write = new IndividualAddress(4, 5, dev);
        mc.writeAddress(write);
        Thread.sleep(50);
        final IndividualAddress[] ias = mc.readAddress(true);
        assertEquals(1, ias.length);
        assertEquals(write, ias[0]);
        mc.writeAddress(orig[0]);
        Thread.sleep(50);
        assertEquals(orig[0], mc.readAddress(true)[0]);
        System.out.println("turn prog mode off...");
        Thread.sleep(5000);
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeAddress
	 * (byte[], tuwien.auto.calimero.IndividualAddress)}.
	 * 
	 * @throws KNXException
	 */
    public final void testWriteAddressByteArrayIndividualAddress() throws KNXException {
        final byte[] sno = new byte[] { 0x00, 0x01, 0x00, 0x11, (byte) 0xcb, 0x08 };
        final IndividualAddress write = mc.readAddress(sno);
        mc.writeAddress(sno, write);
        final IndividualAddress read = mc.readAddress(sno);
        assertEquals(write, read);
        final IndividualAddress write2 = new IndividualAddress(3, 0, 2);
        mc.writeAddress(sno, write2);
        final IndividualAddress read2 = mc.readAddress(sno);
        assertEquals(write2, read2);
        mc.writeAddress(sno, write);
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeKey
	 * (tuwien.auto.calimero.mgmt.Destination, int, byte[])}.
	 * 
	 * @throws KNXException
	 */
    public final void testWriteKey() throws KNXException {
        try {
            mc.writeKey(dco, -1, new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 });
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.writeKey(dco, 16, new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 });
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.writeKey(dco, 1, new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 });
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        mc.writeKey(dco, 1, new byte[] { 0x01, 0x02, 0x03, 0x04 });
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeMemory
	 * (tuwien.auto.calimero.mgmt.Destination, int, byte[])}.
	 * 
	 * @throws KNXException
	 */
    public final void testWriteMemory() throws KNXException {
        final byte[] mem = mc.readMemory(dco, 0x105, 2);
        mc.writeMemory(dco, 0x105, mem);
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#createDestination
	 * (tuwien.auto.calimero.IndividualAddress, boolean, boolean, boolean)}.
	 * 
	 * @throws KNXFormatException
	 */
    public final void testCreateDestination() throws KNXFormatException {
        mc.createDestination(new IndividualAddress("1.1.1"), false);
        mc.createDestination(new IndividualAddress("2.2.2"), true, false, true);
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#restart
	 * (tuwien.auto.calimero.mgmt.Destination)}.
	 * 
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
    public final void testRestart() throws KNXTimeoutException, KNXLinkClosedException {
        dcl.destroy();
        mc.restart(mc.createDestination(new IndividualAddress(3, 1, 1), true));
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeProperty
	 * (tuwien.auto.calimero.mgmt.Destination, int, int, int, int, byte[])}.
	 * 
	 * @throws KNXException
	 */
    public final void testWritePropertyDestinationIntIntIntIntByteArray() throws KNXException {
        final byte[] read = mc.readProperty(dco2, 0, 51, 1, 1);
        mc.writeProperty(dco2, 0, 51, 1, 1, new byte[] { 7 });
        final byte[] read2 = mc.readProperty(dco2, 0, 51, 1, 1);
        assertTrue(Arrays.equals(new byte[] { 7 }, read2));
        mc.writeProperty(dco2, 0, 51, 1, 1, read);
    }

    /**
	 * Test method for {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#detach()}.
	 * 
	 * @throws KNXException
	 */
    public final void testClose() throws KNXException {
        mc.detach();
        assertFalse(mc.isOpen());
        try {
            mc.readAddress(true);
            fail("we are closed");
        } catch (final KNXIllegalStateException e) {
        }
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#getResponseTimeout()}.
	 */
    public final void testGetResponseTimeout() {
        assertEquals(5, mc.getResponseTimeout());
        mc.setResponseTimeout(10);
        assertEquals(10, mc.getResponseTimeout());
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#getPriority()}.
	 */
    public final void testGetPriority() {
        assertEquals(Priority.LOW, mc.getPriority());
        mc.setPriority(Priority.URGENT);
        assertEquals(Priority.URGENT, mc.getPriority());
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readDomainAddress (byte[],
	 * tuwien.auto.calimero.IndividualAddress, int)}.
	 * 
	 * @throws KNXException
	 */
    public final void testReadDomainAddressIntIndividualAddressInt() throws KNXException {
        try {
            mc.readDomainAddress(new byte[] { 1 }, Util.getRouterAddress(), -1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readDomainAddress(new byte[] { 1, 2 }, Util.getRouterAddress(), -1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readDomainAddress(new byte[] { 1, 2 }, Util.getRouterAddress(), 256);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        final List doas = mc.readDomainAddress(new byte[] { 1, 2 }, Util.getRouterAddress(), 100);
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readDomainAddress(boolean)}.
	 * 
	 * @throws KNXException
	 */
    public final void testReadDomainAddressBoolean() throws KNXException {
        final List domain = mc.readDomainAddress(true);
        assertTrue(domain.size() <= 1);
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#writeDomainAddress(byte[])}.
	 * 
	 * @throws KNXLinkClosedException
	 * @throws KNXTimeoutException
	 */
    public final void testWriteDomainAddress() throws KNXTimeoutException, KNXLinkClosedException {
        try {
            mc.writeDomainAddress(new byte[] { 1, 2, 3 });
            fail("wrong length of domain address");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.writeDomainAddress(new byte[] { 1 });
            fail("wrong length of domain address");
        } catch (final KNXIllegalArgumentException e) {
        }
        mc.writeDomainAddress(new byte[] { 1, 2 });
    }

    /**
	 * Test method for
	 * {@link tuwien.auto.calimero.mgmt.ManagementClientImpl#readDeviceDesc
	 * (tuwien.auto.calimero.mgmt.Destination, int)}.
	 * 
	 * @throws KNXException
	 */
    public final void testReadDeviceDesc() throws KNXException {
        try {
            mc.readDeviceDesc(dco, -1);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        try {
            mc.readDeviceDesc(dco, 64);
            fail("invalid arg");
        } catch (final KNXIllegalArgumentException e) {
        }
        final byte[] desc = mc.readDeviceDesc(dco2, 0);
        Util.out(dco2.getAddress().toString() + " has desc.type 0 = ", desc);
    }
}

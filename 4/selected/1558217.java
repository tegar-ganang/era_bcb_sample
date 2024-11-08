package org.creativor.rayson.transport.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.creativor.rayson.transport.common.Packet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>RpcConnectionTest</code> contains tests for the class
 * <code>{@link RpcConnection}</code>.
 * <p>
 * Copyright Creativor Studio (c) 2011
 * 
 * @generatedBy CodePro at 11-5-7 上午2:46
 * @author Nick Zhang
 * @version $Revision: 1.0 $
 */
public class RpcConnectionTest {

    /**
	 * Launch the test.
	 * 
	 * @param args
	 *            the command line arguments
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(RpcConnectionTest.class);
    }

    /**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception
	 *             if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Before
    public void setUp() throws Exception {
    }

    /**
	 * Perform post-test clean-up.
	 * 
	 * @throws Exception
	 *             if the clean-up fails for some reason
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @After
    public void tearDown() throws Exception {
    }

    /**
	 * Run the void addSendPacket(Packet) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testAddSendPacket_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        Packet packet = new Packet(new byte[] {});
        fixture.addSendPacket(packet);
    }

    /**
	 * Run the void addSendPacket(Packet) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testAddSendPacket_2() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        Packet packet = new Packet(new byte[] {});
        fixture.addSendPacket(packet);
    }

    /**
	 * Run the void close() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testClose_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.close();
    }

    /**
	 * Run the void close() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testClose_2() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.close();
    }

    /**
	 * Run the void close() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testClose_3() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.close();
    }

    /**
	 * Run the long getId() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testGetId_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        long result = fixture.getId();
        assertEquals(1L, result);
    }

    /**
	 * Run the InetSocketAddress getRemoteAddr() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testGetRemoteAddr_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        InetSocketAddress result = fixture.getRemoteAddr();
        assertEquals(null, result);
    }

    /**
	 * Run the long getTimeoutInterval() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testGetTimeoutInterval_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        long result = fixture.getTimeoutInterval();
        assertEquals(60000L, result);
    }

    /**
	 * Run the byte getVersion() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testGetVersion_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        byte result = fixture.getVersion();
        assertEquals((byte) -1, result);
    }

    /**
	 * Run the void init() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testInit_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.init();
    }

    /**
	 * Run the void init() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testInit_2() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.init();
    }

    /**
	 * Run the void init() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testInit_3() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.init();
    }

    /**
	 * Run the void init() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testInit_4() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.init();
    }

    /**
	 * Run the boolean isSupportedVersion(short) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testIsSupportedVersion_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        short version = (short) -2;
        boolean result = fixture.isSupportedVersion(version);
        assertEquals(false, result);
    }

    /**
	 * Run the boolean isSupportedVersion(short) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testIsSupportedVersion_2() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        short version = (short) 4;
        boolean result = fixture.isSupportedVersion(version);
        assertEquals(false, result);
    }

    /**
	 * Run the boolean isSupportedVersion(short) method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testIsSupportedVersion_3() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        short version = (short) 1;
        boolean result = fixture.isSupportedVersion(version);
        assertEquals(true, result);
    }

    /**
	 * Run the int pendingPacketCount() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testPendingPacketCount_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        int result = fixture.pendingPacketCount();
        assertEquals(0, result);
    }

    /**
	 * Run the int read() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testRead_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        int result = fixture.read();
        assertEquals(0, result);
    }

    /**
	 * Run the int read() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testRead_10() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        int result = fixture.read();
        assertEquals(0, result);
    }

    /**
	 * Run the int read() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testRead_2() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        int result = fixture.read();
        assertEquals(0, result);
    }

    /**
	 * Run the int read() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testRead_3() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        int result = fixture.read();
        assertEquals(0, result);
    }

    /**
	 * Run the int read() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testRead_4() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        int result = fixture.read();
        assertEquals(0, result);
    }

    /**
	 * Run the int read() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testRead_5() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        int result = fixture.read();
        assertEquals(0, result);
    }

    /**
	 * Run the int read() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testRead_6() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        int result = fixture.read();
        assertEquals(0, result);
    }

    /**
	 * Run the int read() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testRead_7() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        int result = fixture.read();
        assertEquals(0, result);
    }

    /**
	 * Run the int read() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testRead_8() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        int result = fixture.read();
        assertEquals(0, result);
    }

    /**
	 * Run the int read() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testRead_9() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        int result = fixture.read();
        assertEquals(0, result);
    }

    /**
	 * Run the RpcConnection(long,SocketChannel,PacketManager,SelectionKey)
	 * constructor test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testRpcConnection_1() throws Exception {
        long id = 1L;
        SocketChannel clientChannel = SocketChannel.open();
        PacketManager packetManager = new PacketManager();
        SelectionKey selectionKey = null;
        RpcConnection result = new RpcConnection(id, clientChannel, packetManager, selectionKey, null);
        assertNotNull(result);
        assertEquals("{id: 1, protocol: RPC, version: -1, last contact: 1304707560781, packet counter: {read: 0, write: 0}, pending packets: 0, address: Socket[unconnected]}", result.toString());
        assertEquals(1L, result.getId());
        assertEquals((byte) -1, result.getVersion());
        assertEquals(null, result.getRemoteAddr());
        assertEquals(0, result.pendingPacketCount());
        assertEquals(1304707560781L, result.getCreationTime());
        assertEquals(1304707560781L, result.getLastContactTime());
        assertEquals(false, result.isTimeOut());
    }

    /**
	 * Run the String toString() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testToString_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        String result = fixture.toString();
        assertEquals("{id: 1, protocol: RPC, version: -1, last contact: 1304707562104, packet counter: {read: 0, write: 0}, pending packets: 0, address: Socket[unconnected]}", result);
    }

    /**
	 * Run the void write() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testWrite_1() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.write();
    }

    /**
	 * Run the void write() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testWrite_10() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.write();
    }

    /**
	 * Run the void write() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testWrite_2() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.write();
    }

    /**
	 * Run the void write() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testWrite_3() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.write();
    }

    /**
	 * Run the void write() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testWrite_4() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.write();
    }

    /**
	 * Run the void write() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testWrite_5() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.write();
    }

    /**
	 * Run the void write() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testWrite_6() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.write();
    }

    /**
	 * Run the void write() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testWrite_7() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.write();
    }

    /**
	 * Run the void write() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testWrite_8() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.write();
    }

    /**
	 * Run the void write() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午2:46
	 */
    @Test
    public void testWrite_9() throws Exception {
        RpcConnection fixture = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        fixture.write();
    }
}

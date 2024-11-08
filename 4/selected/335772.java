package org.creativor.rayson.transport.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.creativor.rayson.transport.common.Packet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>ConnectionPacketLinkTest</code> contains tests for the class
 * <code>{@link ConnectionPacketLink}</code>.
 * <p>
 * Copyright Creativor Studio (c) 2011
 * 
 * @generatedBy CodePro at 11-5-7 上午3:10
 * @author Nick Zhang
 * @version $Revision: 1.0 $
 */
public class ConnectionPacketLinkTest {

    /**
	 * Launch the test.
	 * 
	 * @param args
	 *            the command line arguments
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:10
	 */
    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(ConnectionPacketLinkTest.class);
    }

    /**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception
	 *             if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:10
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
	 * @generatedBy CodePro at 11-5-7 上午3:10
	 */
    @After
    public void tearDown() throws Exception {
    }

    /**
	 * Run the ConnectionPacketLink(RpcConnection,Packet) constructor test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:10
	 */
    @Test
    public void testConnectionPacketLink_1() throws Exception {
        RpcConnection connection = new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null);
        Packet packet = new Packet(new byte[] {});
        ConnectionPacketLink result = new ConnectionPacketLink(connection, packet);
        assertNotNull(result);
    }

    /**
	 * Run the RpcConnection getConnection() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:10
	 */
    @Test
    public void testGetConnection_1() throws Exception {
        ConnectionPacketLink fixture = new ConnectionPacketLink(new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null), new Packet(new byte[] {}));
        RpcConnection result = fixture.getConnection();
        assertNotNull(result);
        assertEquals("{id: 1, protocol: RPC, version: -1, last contact: 1304709029557, packet counter: {read: 0, write: 0}, pending packets: 0, address: Socket[unconnected]}", result.toString());
        assertEquals(1L, result.getId());
        assertEquals((byte) -1, result.getVersion());
        assertEquals(null, result.getRemoteAddr());
        assertEquals(0, result.pendingPacketCount());
        assertEquals(1304709029557L, result.getCreationTime());
        assertEquals(1304709029557L, result.getLastContactTime());
        assertEquals(false, result.isTimeOut());
    }

    /**
	 * Run the Packet getPacket() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:10
	 */
    @Test
    public void testGetPacket_1() throws Exception {
        ConnectionPacketLink fixture = new ConnectionPacketLink(new RpcConnection(1L, SocketChannel.open(), new PacketManager(), (SelectionKey) null, null), new Packet(new byte[] {}));
        Packet result = fixture.getPacket();
        assertNotNull(result);
        assertEquals("{Data length: 0}", result.toString());
        assertEquals((short) 0, result.getDataLength());
    }
}

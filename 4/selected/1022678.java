package org.apache.harmony.nio.tests;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import junit.framework.TestCase;
import org.apache.harmony.nio.AddressUtil;

public class AddressUtilTest extends TestCase {

    /**
     * @tests AddressUtil#getDirectBufferAddress
     */
    public void test_getDirectBufferAddress() throws Exception {
        ByteBuffer buf = ByteBuffer.allocateDirect(10);
        assertTrue(AddressUtil.getDirectBufferAddress(buf) != 0);
    }

    /**
     * @tests AddressUtil#getChannelAddress
     */
    public void test_getFileChannelAddress() throws Exception {
    }

    /**
     * @tests AddressUtil#getChannelAddress
     */
    public void test_getSocketChannelAddress() throws Exception {
        SocketChannel sc = SocketChannel.open();
        assertTrue(AddressUtil.getChannelAddress(sc) > 0);
    }

    /**
     * @tests AddressUtil#getChannelAddress
     */
    public void test_getDatagramChannelAddress() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        assertTrue(AddressUtil.getChannelAddress(dc) > 0);
    }

    /**
     * @tests AddressUtil#getChannelAddress
     */
    public void test_getServerSocketChannelAddress() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        assertTrue(AddressUtil.getChannelAddress(ssc) > 0);
    }

    /**
     * @tests AddressUtil#getChannelAddress
     */
    public void test_getNonNativeChannelAddress() throws Exception {
        Channel channel = new MockChannel();
        assertEquals(0, AddressUtil.getChannelAddress(channel));
    }

    private static class MockChannel implements Channel {

        public boolean isOpen() {
            return false;
        }

        public void close() throws IOException {
        }
    }
}

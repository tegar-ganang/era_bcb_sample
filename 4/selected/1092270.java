package com.ewansilver.concurrency;

import junit.framework.TestCase;

/**
 * Test the Channel interface. This test does not attempt to do any concurrency
 * testing - just ensures that the basic functions are in place.
 * 
 * @author ewan.silver @ gmail.com
 */
public class ChannelTest extends TestCase {

    /**
	 * The Channel we are going to test.
	 */
    private Channel channel;

    protected void setUp() throws Exception {
        super.setUp();
        channel = getChannel();
    }

    public void testPut() throws InterruptedException {
        assertEquals(0, channel.size());
        channel.put("A first test");
        assertEquals(1, channel.size());
        channel.put("A second test");
        assertEquals(2, channel.size());
    }

    public void testTake() throws InterruptedException {
        assertEquals(0, channel.size());
        channel.put("A first test");
        assertEquals(1, channel.size());
        channel.put("A second test");
        assertEquals(2, channel.size());
        assertEquals("A first test", channel.take());
        assertEquals(1, channel.size());
        assertEquals("A second test", channel.take());
        assertEquals(0, channel.size());
    }

    public void testPoll() throws InterruptedException {
        assertEquals(0, channel.size());
        channel.put("A first test");
        assertEquals(1, channel.size());
        channel.put("A second test");
        assertEquals(2, channel.size());
        assertEquals("A first test", channel.poll(100));
        assertEquals(1, channel.size());
        assertEquals("A second test", channel.poll(100));
        assertEquals(0, channel.size());
        assertNull(channel.poll(100));
        assertEquals(0, channel.size());
    }

    /**
	 * Gets the Channel implementation we want to test.
	 * 
	 * @return a ChannelImpl.
	 */
    protected Channel getChannel() {
        return new ChannelImpl();
    }
}

package org.apache.catalina.tribes.test.channel;

import org.apache.catalina.tribes.group.GroupChannel;
import junit.framework.TestCase;
import org.apache.catalina.tribes.transport.ReceiverBase;

/**
 * @author Filip Hanik
 * @version 1.0
 */
public class ChannelStartStop extends TestCase {

    GroupChannel channel = null;

    protected void setUp() throws Exception {
        super.setUp();
        channel = new GroupChannel();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        try {
            channel.stop(channel.DEFAULT);
        } catch (Exception ignore) {
        }
    }

    public void testDoubleFullStart() throws Exception {
        int count = 0;
        try {
            channel.start(channel.DEFAULT);
            count++;
        } catch (Exception x) {
            x.printStackTrace();
        }
        try {
            channel.start(channel.DEFAULT);
            count++;
        } catch (Exception x) {
            x.printStackTrace();
        }
        assertEquals(count, 2);
        channel.stop(channel.DEFAULT);
    }

    public void testScrap() throws Exception {
        System.out.println(channel.getChannelReceiver().getClass());
        ((ReceiverBase) channel.getChannelReceiver()).setMaxThreads(1);
    }

    public void testDoublePartialStart() throws Exception {
        int count = 0;
        try {
            channel.start(channel.SND_RX_SEQ);
            channel.start(channel.MBR_RX_SEQ);
            count++;
        } catch (Exception x) {
            x.printStackTrace();
        }
        try {
            channel.start(channel.MBR_RX_SEQ);
            count++;
        } catch (Exception x) {
        }
        assertEquals(count, 1);
        channel.stop(channel.DEFAULT);
        count = 0;
        try {
            channel.start(channel.SND_RX_SEQ);
            channel.start(channel.MBR_TX_SEQ);
            count++;
        } catch (Exception x) {
            x.printStackTrace();
        }
        try {
            channel.start(channel.MBR_TX_SEQ);
            count++;
        } catch (Exception x) {
        }
        assertEquals(count, 1);
        channel.stop(channel.DEFAULT);
        count = 0;
        try {
            channel.start(channel.SND_RX_SEQ);
            count++;
        } catch (Exception x) {
            x.printStackTrace();
        }
        try {
            channel.start(channel.SND_RX_SEQ);
            count++;
        } catch (Exception x) {
        }
        assertEquals(count, 1);
        channel.stop(channel.DEFAULT);
        count = 0;
        try {
            channel.start(channel.SND_TX_SEQ);
            count++;
        } catch (Exception x) {
            x.printStackTrace();
        }
        try {
            channel.start(channel.SND_TX_SEQ);
            count++;
        } catch (Exception x) {
        }
        assertEquals(count, 1);
        channel.stop(channel.DEFAULT);
    }

    public void testFalseOption() throws Exception {
        int flag = 0xFFF0;
        int count = 0;
        try {
            channel.start(flag);
            count++;
        } catch (Exception x) {
            x.printStackTrace();
        }
        try {
            channel.start(flag);
            count++;
        } catch (Exception x) {
        }
        assertEquals(count, 2);
        channel.stop(channel.DEFAULT);
    }
}

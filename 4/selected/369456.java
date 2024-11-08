package net.jetrix;

import junit.framework.*;
import net.jetrix.config.*;

/**
 * JUnit TestCase for the class net.jetrix.ChannelManager.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 836 $, $Date: 2010-04-11 17:25:35 -0400 (Sun, 11 Apr 2010) $
 */
public class ChannelManagerTest extends TestCase {

    private ChannelManager manager;

    private ChannelConfig config1;

    private ChannelConfig config2;

    public void setUp() {
        manager = ChannelManager.getInstance();
        manager.clear();
        config1 = new ChannelConfig();
        config1.setName("test1");
        config2 = new ChannelConfig();
        config2.setName("test2");
    }

    public void tearDown() {
        manager.clear();
    }

    public void testCreateChannel() {
        assertEquals("channel count before creation", 0, manager.getChannelCount());
        manager.createChannel(config1, false);
        assertEquals("channel count after creation", 1, manager.getChannelCount());
    }

    public void testGetChannel() {
        String name = config1.getName();
        manager.createChannel(config1, false);
        Channel channel = manager.getChannel(name);
        assertNotNull("channel not found", channel);
        assertEquals("channel name", name, channel.getConfig().getName());
    }

    public void testGetChannelSharp() {
        String name = config1.getName();
        manager.createChannel(config1, false);
        Channel channel = manager.getChannel("#" + name);
        assertNotNull("channel not found", channel);
        assertEquals("channel name", name, channel.getConfig().getName());
    }

    public void testGetChannelMixedCase() {
        String name = config1.getName();
        manager.createChannel(config1, false);
        Channel channel = manager.getChannel("tEsT1");
        assertNotNull("channel not found", channel);
        assertEquals("channel name", name, channel.getConfig().getName());
    }

    public void testGetChannelPartialName() {
        config1.setName("xzyt");
        manager.createChannel(config1, false);
        manager.createChannel(config2, false);
        Channel channel = manager.getChannel("test", true);
        assertNotNull("channel not found", channel);
        assertEquals("channel name", config2.getName(), channel.getConfig().getName());
    }

    public void testGetChannelByNumber() {
        String name = config1.getName();
        manager.createChannel(config1, false);
        Channel channel = manager.getChannel(0);
        assertNotNull("channel not found", channel);
        assertEquals("channel name", name, channel.getConfig().getName());
        Channel channel2 = manager.getChannel(1);
        assertNull("channel found at index 1", channel2);
    }

    public void testGetOpenedChannel() {
        config1.setMaxPlayers(0);
        manager.createChannel(config1, false);
        assertNull("closed channel returned", manager.getOpenedChannel());
        config1.setMaxPlayers(6);
        assertNotNull("opened channel not found", manager.getOpenedChannel());
    }

    public void testRemoveChannel() {
        manager.createChannel(config1, false);
        assertEquals("channel count before removal", 1, manager.getChannelCount());
        manager.removeChannel(config1.getName());
        assertEquals("channel count after removal", 0, manager.getChannelCount());
    }
}

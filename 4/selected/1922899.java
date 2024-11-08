package org.coos.messaging.routing.itest;

import junit.framework.TestCase;
import org.coos.messaging.COOS;
import org.coos.messaging.COOSFactory;
import org.coos.messaging.Channel;
import org.coos.messaging.Exchange;
import org.coos.messaging.Plugin;
import org.coos.messaging.PluginFactory;
import org.coos.messaging.impl.DefaultMessage;
import org.coos.messaging.plugin.simple.SimpleProducer;

/**
 * A test checking that a Plugin gets instantiated and is correctly connected to
 * the bus using TCP transports
 * 
 * @author Knut Eilif Husa, Tellu AS
 */
public class PluginRobustDeliveryTest extends TestCase {

    COOS coos;

    public PluginRobustDeliveryTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testPluginConnection() throws Exception {
        coos = COOSFactory.createCOOS(this.getClass().getResourceAsStream("/coos2.xml"), null);
        coos.start();
        System.out.println("Coos started");
        Plugin[] plugins = PluginFactory.createPlugins(this.getClass().getResourceAsStream("/plugin4Robust.xml"));
        for (int i = 0; i < plugins.length; i++) {
            Plugin plugin = plugins[i];
            try {
                plugin.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Exchange exchange = ((SimpleProducer) plugins[0].getEndpoint().createProducer()).sendMessageRobust("coos://segment2.UUID-ep2/foo/bar", new DefaultMessage());
        assertNull(exchange.getInBoundMessage());
        assertNull(exchange.getFaultMessage());
        plugins[0].disconnect((Channel) plugins[0].getChannels().firstElement());
        exchange = ((SimpleProducer) plugins[0].getEndpoint().createProducer()).sendMessageRobust("coos://segment2.UUID-ep2/foo/bar", new DefaultMessage());
        assertNull(exchange.getInBoundMessage());
        assertNotNull(exchange.getFaultMessage());
        System.out.println("testRequestMessage finished");
        System.out.println();
    }

    protected void tearDown() throws Exception {
        coos.stop();
        COOSFactory.clear();
    }
}

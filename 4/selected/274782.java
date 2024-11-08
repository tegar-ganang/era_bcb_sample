package net.jetrix.config;

import java.net.URL;
import junit.framework.*;
import net.jetrix.ChannelManager;

/**
 * JUnit TestCase for the class net.jetrix.config.ServerConfig
 *
 * @author Emmanuel Bourg
 * @version $Revision: 794 $, $Date: 2009-02-17 14:08:39 -0500 (Tue, 17 Feb 2009) $
 */
public class ServerConfigTest extends TestCase {

    private URL serverConfigURL = getClass().getResource("/conf/server.xml");

    public void testGetInstance() {
        try {
            ServerConfig config = new ServerConfig();
            config.load(serverConfigURL);
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    public void testSave() throws Exception {
        ServerConfig config = new ServerConfig();
        config.load(serverConfigURL);
        for (ChannelConfig cc : config.getChannels()) {
            cc.setPersistent(true);
            ChannelManager.getInstance().createChannel(cc, false);
        }
        config.save();
        ServerConfig config2 = new ServerConfig();
        config2.load(serverConfigURL);
    }
}

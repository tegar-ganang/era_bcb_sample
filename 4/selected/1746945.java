package net.jetrix.agent;

import java.util.List;
import junit.framework.TestCase;

/**
 * @author Emmanuel Bourg
 * @version $Revision: 837 $, $Date: 2010-04-11 18:42:20 -0400 (Sun, 11 Apr 2010) $
 */
public class QueryAgentTest extends TestCase {

    private String hostname = "tetrinet.fr";

    public void testGetVersion() throws Exception {
        QueryAgent agent = new QueryAgent();
        agent.connect(hostname);
        String version = agent.getVersion();
        agent.disconnect();
        assertEquals("version", "1.13.2ice Dual server", version);
    }

    public void testGetPlayerNumber() throws Exception {
        QueryAgent agent = new QueryAgent();
        agent.connect(hostname);
        int count = agent.getPlayerNumber();
        agent.disconnect();
        assertTrue("player number", count >= 0);
    }

    public void testGetChannels() throws Exception {
        QueryAgent agent = new QueryAgent();
        agent.connect(hostname);
        List<ChannelInfo> channels = agent.getChannels();
        agent.disconnect();
        assertNotNull("null list", channels);
        assertFalse("list is empty", channels.isEmpty());
    }

    public void testGetPlayers() throws Exception {
        QueryAgent agent = new QueryAgent();
        agent.connect("tetridome.com");
        List<PlayerInfo> players = agent.getPlayers();
        agent.disconnect();
        assertNotNull("null list", players);
        assertFalse("list is empty", players.isEmpty());
    }

    public void testGetPing() throws Exception {
        QueryAgent agent = new QueryAgent();
        agent.connect(hostname);
        long ping = agent.getPing();
        agent.disconnect();
        assertTrue("ping", ping > 0);
    }
}

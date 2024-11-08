package junit_tests;

import spacewalklib.RhnCobblerServer;
import spacewalklib.RhnConn;
import spacewalklib.RhnServer;
import spacewalklib.RhnSystemsGroup;
import junit.framework.TestCase;

/**
 * @author Alfredo Moralejo
 *
 */
public class TestRhnServer extends TestCase {

    /**
	 * Test method for {@link spacewalklib.RhnServer#getServer()}.
	 */
    public void testGetServer() {
        try {
            RhnServer server = new RhnServer("http://satellite1.example.com/rpc/api", "admin", "redhat");
            System.out.println("Spacewalk server: " + server.getServer());
            System.out.println("Spacewalk user: " + server.getUser());
        } catch (Exception ex) {
            fail(ex.toString());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnServer#getClientNames()}.
	 */
    public void testGetClientNames() {
        try {
            System.out.println("Systems registered in the server:");
            RhnServer server = new RhnServer("http://satellite1.example.com/rpc/api", "admin", "redhat");
            for (String client : server.getClientNames()) {
                System.out.println(client);
            }
        } catch (Exception ex) {
            fail(ex.toString());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnServer#getChannelNames()}.
	 */
    public void testGetChannelNames() {
        try {
            System.out.println("Channels registered in the server:");
            RhnServer server = new RhnServer("http://satellite1.example.com/rpc/api", "admin", "redhat");
            for (String channel : server.getChannelNames()) {
                System.out.println(channel);
            }
        } catch (Exception ex) {
            fail(ex.toString());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnServer#getChannelLabels()}.
	 */
    public void testGetChannelLabels() {
        try {
            System.out.println("Channels registered in the server:");
            RhnServer server = new RhnServer("http://satellite1.example.com/rpc/api", "admin", "redhat");
            for (String label : server.getChannelLabels()) {
                System.out.println(label);
            }
        } catch (Exception ex) {
            fail(ex.toString());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnServer#getConfigChannelNames()}.
	 */
    public void testGetConfigChannelNames() {
        try {
            System.out.println("Configuration Channels registered in the server:");
            RhnServer server = new RhnServer("http://satellite1.example.com/rpc/api", "admin", "redhat");
            for (String channel : server.getConfigChannelNames()) {
                System.out.println(channel);
            }
        } catch (Exception ex) {
            fail(ex.toString());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnServer#getConfigChannelLabels()}.
	 */
    public void testGetConfigChannelLabels() {
        try {
            System.out.println("Configuration Channels registered in the server:");
            RhnServer server = new RhnServer("http://satellite1.example.com/rpc/api", "admin", "redhat");
            for (String channel : server.getConfigChannelLabels()) {
                System.out.println(channel);
            }
        } catch (Exception ex) {
            fail(ex.toString());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnServer#getKsProfiles()}.
	 */
    public void testGetKsProfiles() {
        try {
            System.out.println("Kickstart profiles in the server:");
            RhnServer server = new RhnServer("http://satellite1.example.com/rpc/api", "admin", "redhat");
            for (String ks : server.getKsProfiles()) {
                System.out.println(ks);
            }
        } catch (Exception ex) {
            fail(ex.toString());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnServer#getGroupNames()}.
	 */
    public void testGroupNames() {
        try {
            System.out.println("System groups in the server:");
            RhnServer server = new RhnServer("http://satellite1.example.com/rpc/api", "admin", "redhat");
            for (String group : server.getGroupNames()) {
                System.out.println(group);
            }
        } catch (Exception ex) {
            fail(ex.toString());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnServer#getGroups()}.
	 */
    public void testGroups() {
        try {
            System.out.println("System groups in the server:");
            RhnConn rhnconn = new RhnConn("http://satellite1.example.com/rpc/api", "admin", "redhat");
            RhnServer server = new RhnServer(rhnconn);
            for (RhnSystemsGroup group : server.getGroups()) {
                System.out.println(group.getName());
            }
        } catch (Exception ex) {
            fail(ex.toString());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnServer#getGroups()}.
	 */
    public void testGetCobblerServer() {
        try {
            RhnConn rhnconn = new RhnConn("http://satellite1.example.com/rpc/api", "admin", "redhat");
            RhnServer server = new RhnServer(rhnconn);
            RhnCobblerServer cobbler_server = server.getCobblerServer();
            System.out.println("Systems in cobbler:");
            for (String system : cobbler_server.getSystemsNames()) {
                System.out.println(system);
            }
        } catch (Exception ex) {
            fail(ex.toString());
        }
    }
}

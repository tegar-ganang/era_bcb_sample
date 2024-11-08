package junit_tests;

import spacewalklib.RhnConfigChannels;
import spacewalklib.RhnConn;
import junit.framework.TestCase;

/**
 * @author Alfredo Moralejo
 *
 */
public class TestRhnConfigChannels extends TestCase {

    /**
	 * Test method for {@link spacewalklib.RhnConfigChannels#RhnConfigChannels(java.lang.String, java.lang.String, java.lang.String)}.
	 */
    public void testRhnConfigChannels() {
        try {
            RhnConfigChannels channels = new RhnConfigChannels("http://satellite1.example.com/rpc/api", "admin", "redhat");
            for (String label : channels.getLabels()) {
                System.out.println(label);
            }
        } catch (Exception ex) {
            fail("Exception arised: " + ex.toString());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnConfigChannels#RhnConfigChannels(spacewalklib.RhnConn)}.
	 */
    public void testRhnConfigChannelsRhnConn() {
        try {
            RhnConn connection = new RhnConn("http://satellite1.example.com/rpc/api", "admin", "redhat");
            RhnConfigChannels channels = new RhnConfigChannels(connection);
            for (String name : channels.getNames()) {
                System.out.println(name);
            }
        } catch (Exception ex) {
            fail("Exception arised: " + ex.toString());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnConfigChannels#getCounter()}.
	 */
    public void testGetCounter() {
        try {
            RhnConn connection = new RhnConn("http://satellite1.example.com/rpc/api", "admin", "redhat");
            RhnConfigChannels channels = new RhnConfigChannels(connection);
            System.out.println(channels.getCounter());
            if (channels.getCounter() != channels.getChannels().size()) {
                fail("Counter function is not working properly");
            }
        } catch (Exception ex) {
            fail("Exception arised: " + ex.toString());
        }
    }
}

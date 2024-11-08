package junit_tests;

import java.util.ArrayList;
import java.util.List;
import spacewalklib.RhnConnFault;
import spacewalklib.RhnSwChannel;
import spacewalklib.RhnSwChannels;
import junit.framework.TestCase;

/**
 * @author Alfredo Moralejo
 *
 */
public class TestRhnSwChannels extends TestCase {

    /**
	 * Test method for {@link spacewalklib.RhnSwChannels#RhnSwChannels(java.lang.String, java.lang.String, java.lang.String)}.
	 * @throws RhnConnFault
	 */
    public void testRhnSwChannels() throws RhnConnFault {
        try {
            RhnSwChannels channels = new RhnSwChannels("http://satellite1.example.com/rpc/api", "admin", "redhat");
            System.out.println("Number of channels: " + channels.getCounter().toString());
        } catch (Exception ex) {
            fail("Exception arised: " + ex.getMessage());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnSwChannels#getCounter()}.
	 */
    public void testGetCounter() {
        try {
            RhnSwChannels channels = new RhnSwChannels("http://satellite1.example.com/rpc/api", "admin", "redhat");
            if (channels.getCounter() != channels.getChannels().size()) {
                fail("Counter is " + channels.getCounter() + " but there are " + channels.getChannels().size() + " channels.");
            }
        } catch (Exception ex) {
            fail("Exception arised: " + ex.getMessage());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnSwChannels#getNames()}.
	 */
    public void testGetNames() {
        try {
            RhnSwChannels channels = new RhnSwChannels("http://satellite1.example.com/rpc/api", "admin", "redhat");
            List<String> names = new ArrayList<String>();
            names = channels.getNames();
            for (String name : names) {
                System.out.println("Channel Name: " + name);
            }
        } catch (Exception ex) {
            fail("Exception arised: " + ex.getMessage());
        }
    }

    /**
	 * Test method for {@link spacewalklib.RhnSwChannels#getLabels()}.
	 */
    public void testGetLabels() {
        try {
            RhnSwChannels channels = new RhnSwChannels("http://satellite1.example.com/rpc/api", "admin", "redhat");
            List<String> labels = new ArrayList<String>();
            labels = channels.getLabels();
            for (String label : labels) {
                System.out.println("Channel label: " + label);
            }
        } catch (Exception ex) {
            fail("Exception arised: " + ex.getMessage());
        }
    }

    public void testGetChannels() {
        try {
            RhnSwChannels channels = new RhnSwChannels("http://satellite1.example.com/rpc/api", "admin", "redhat");
            List<RhnSwChannel> list = new ArrayList<RhnSwChannel>();
            list = channels.getChannels();
            for (RhnSwChannel channel : list) {
                System.out.println(channel.getName() + " - Channel label: " + channel.getLabel() + " - Channel ID: " + channel.getId().toString() + " - Parent channel: " + channel.getParentchannel());
            }
        } catch (Exception ex) {
            fail("Exception arised: " + ex.getMessage());
        }
    }
}

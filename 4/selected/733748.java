package jhomenet.server.hw.driver;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Class description.
 *
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class NoOpHub implements Hub {

    /**
     * 
     */
    public NoOpHub() {
        super();
    }

    /**
     * @see jhomenet.server.hw.driver.Hub#activateChannel(jhomenet.server.hw.driver.HubChannel, jhomenet.server.hw.driver.HubPort)
     */
    @Override
    public void activateChannel(HubChannel channel, HubPort port) {
    }

    /**
     * @see jhomenet.server.hw.driver.Hub#deactivateAllChannels()
     */
    @Override
    public void deactivateAllChannels() {
    }

    /**
     * @see jhomenet.server.hw.driver.Hub#deactivateChannel(jhomenet.server.hw.driver.HubChannel, jhomenet.server.hw.driver.HubPort)
     */
    @Override
    public void deactivateChannel(HubChannel channel, HubPort port) {
    }

    /**
     * @see jhomenet.server.hw.driver.Hub#getCouplerAddrs()
     */
    @Override
    public List<String> getCouplerAddrs() {
        return new ArrayList<String>();
    }

    /**
     * @see jhomenet.server.hw.driver.Hub#getChannel(java.lang.String)
     */
    @Override
    public HubChannel getChannel(String couplerAddr) {
        return HubChannel.NA;
    }
}

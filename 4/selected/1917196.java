package jhomenet.server.hw.driver.onewire;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.OneWireContainer1F;
import jhomenet.commons.hw.driver.HardwareContainer;
import jhomenet.server.hw.driver.Hub;
import jhomenet.server.hw.driver.ContainerWrapper;
import jhomenet.server.hw.driver.HubChannel;
import jhomenet.server.hw.driver.HubPort;

/**
 * TODO: Class description.
 *
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class OneWireHub implements Hub {

    /**
     * Define a logger.
     */
    private static Logger logger = Logger.getLogger(OneWireHub.class.getName());

    /**
     * Hub description.
     */
    private final String desc;

    /**
     * 
     */
    private final Map<HubChannel, OneWireContainer1F> owSwitch = new HashMap<HubChannel, OneWireContainer1F>();

    /**
     * Default constructor.
     * 
     * @param desc
     * @param container1
     * @param container2
     * @param container3
     */
    public OneWireHub(String desc, OneWireContainer1F container1, OneWireContainer1F container2, OneWireContainer1F container3) {
        super();
        this.desc = desc;
        this.owSwitch.put(HubChannel.ONE, container1);
        this.owSwitch.put(HubChannel.TWO, container2);
        this.owSwitch.put(HubChannel.THREE, container3);
        deactivateAllChannels();
    }

    /**
     * @return the desc
     */
    public final String getDesc() {
        return desc;
    }

    /**
     * @see jhomenet.server.hw.driver.Hub#getCouplerAddrs()
     */
    @Override
    public synchronized List<String> getCouplerAddrs() {
        List<String> tmp = new ArrayList<String>();
        for (OneWireContainer1F container : this.owSwitch.values()) {
            tmp.add(container.getAddressAsString());
        }
        return tmp;
    }

    /**
     * @see jhomenet.server.hw.driver.Hub#getChannel(java.lang.String)
     */
    @Override
    public HubChannel getChannel(String couplerAddr) {
        for (HubChannel channel : this.owSwitch.keySet()) {
            OneWireContainer1F container = this.owSwitch.get(channel);
            if (container.getAddressAsString().equalsIgnoreCase(couplerAddr)) {
                return channel;
            }
        }
        return null;
    }

    /**
     * @see jhomenet.server.hw.driver.Hub#deactivateAllChannels()
     */
    public final void deactivateAllChannels() {
        for (HubChannel c : HubChannel.values()) {
            if (c == HubChannel.ONE || c == HubChannel.TWO || c == HubChannel.THREE) {
                deactivateChannel(c, HubPort.MAIN);
                deactivateChannel(c, HubPort.AUX);
            }
        }
    }

    /**
     * @see jhomenet.server.hw.driver.Hub#activateChannel(jhomenet.server.hw.driver.HubChannel, jhomenet.server.hw.driver.HubPort)
     */
    @Override
    public final void activateChannel(HubChannel channel, HubPort port) {
        int portAsInt = port.getValue();
        logger.debug("Hub: Activating 1-Wire hub channel " + channel.getChannel() + ":" + port);
        try {
            byte[] state = owSwitch.get(channel).readDevice();
            owSwitch.get(channel).setLatchState(portAsInt, true, false, state);
            owSwitch.get(channel).writeDevice(state);
        } catch (OneWireException e) {
            logger.error("Error activating hub channel: " + e.getMessage(), e);
        }
    }

    /**
     * @see jhomenet.server.hw.driver.Hub#deactivateChannel(jhomenet.server.hw.driver.HubChannel, jhomenet.server.hw.driver.HubPort)
     */
    @Override
    public void deactivateChannel(HubChannel channel, HubPort port) {
        int portAsInt = port.getValue();
        logger.debug("Hub: De-activating 1-Wire hub channel " + channel.getChannel() + ":" + port);
        try {
            byte[] state = owSwitch.get(channel).readDevice();
            owSwitch.get(channel).setLatchState(portAsInt, false, false, state);
            owSwitch.get(channel).writeDevice(state);
        } catch (OneWireException e) {
            logger.error("Error de-activating hub channel: " + e.getMessage(), e);
        }
    }
}

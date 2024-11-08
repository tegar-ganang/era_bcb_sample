package jhomenet.server.hw.driver.onewire;

import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.OneWireContainer1F;
import jhomenet.commons.hw.driver.HardwareDriverException;
import jhomenet.commons.hw.driver.HardwareContainer;
import jhomenet.server.cfg.HardwareContainerLoaderConfig;
import jhomenet.server.cfg.HardwareContainerLoaderConfig.HubDef;
import jhomenet.server.cfg.ServerSystemConfigurationImpl;
import jhomenet.server.hw.driver.AbstractContainerLoader;
import jhomenet.server.hw.driver.HubChannel;
import jhomenet.server.hw.driver.HubPort;

/**
 * A class that is used to load the 1-Wire containers.
 *
 * @author T. Bitson 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class OneWireContainerLoader extends AbstractContainerLoader {

    /**
     * Define a logger.
     */
    private static Logger logger = Logger.getLogger(OneWireContainerLoader.class.getName());

    /**
     * 1-wire adapter used to communicate to the 1-wire hardware.
     */
    private static DSPortAdapter onewireAdapter = null;

    /**
     * 
     */
    private final List<OneWireHub> hubDrivers = new ArrayList<OneWireHub>();

    /**
     * Default constructor.
     * 
     * @param loaderConfig
     * @param serverConfiguration
     */
    public OneWireContainerLoader(HardwareContainerLoaderConfig loaderConfig, ServerSystemConfigurationImpl serverConfiguration) {
        super(loaderConfig, serverConfiguration);
        try {
            onewireAdapter = OneWireAccessProvider.getDefaultAdapter();
            this.resetNetwork();
            this.buildHubs(loaderConfig.getHubDefs());
            logger.debug("1-Wire container manager successfully initialized");
        } catch (OneWireException owe) {
            logger.error("Error getting 1-wire adapter: " + owe.getMessage());
        }
    }

    /**
     * @see jhomenet.commons.hw.driver.ContainerLoader#loadContainers()
     */
    public final Map<String, HardwareContainer> loadContainers() throws HardwareDriverException {
        logger.debug("Loading 1-Wire hardware containers");
        Map<String, HardwareContainer> containerMap = new HashMap<String, HardwareContainer>();
        try {
            onewireAdapter.beginExclusive(true);
            this.resetNetwork();
            onewireAdapter.setSearchAllDevices();
            onewireAdapter.targetAllFamilies();
            onewireAdapter.targetFamily(0x001F);
            Enumeration owdEnum;
            OneWireContainer owd;
            if (onewireAdapter.findFirstDevice()) {
                for (owdEnum = onewireAdapter.getAllDeviceContainers(); owdEnum.hasMoreElements(); ) {
                    owd = (OneWireContainer) owdEnum.nextElement();
                    logger.debug("MicroLAN Coupler Found at " + owd.getAddressAsString());
                    OneWireHub hub = this.findHub(owd.getAddressAsString());
                    HubChannel channel = hub.getChannel(owd.getAddressAsString());
                    hub.activateChannel(channel, HubPort.MAIN);
                    searchActiveBranch(hub, channel, HubPort.MAIN, containerMap);
                    hub.deactivateChannel(channel, HubPort.MAIN);
                    hub.activateChannel(channel, HubPort.AUX);
                    searchActiveBranch(hub, channel, HubPort.AUX, containerMap);
                    hub.deactivateChannel(channel, HubPort.AUX);
                }
            } else {
                searchActiveBranch(containerMap);
            }
            logger.debug("Loaded " + containerMap.size() + " 1-Wire hardware containers");
            return containerMap;
        } catch (OneWireException owe) {
            logger.error("1-Wire exception while detecting hardware: " + owe.getMessage());
            throw new HardwareDriverException(owe);
        } finally {
            onewireAdapter.endExclusive();
        }
    }

    /**
     * 
     * @param hubDefs
     */
    private void buildHubs(List<HubDef> hubDefs) {
        logger.debug("Building 1-Wire hubs");
        Map<String, OneWireContainer1F> couplers = new HashMap<String, OneWireContainer1F>();
        try {
            onewireAdapter.beginExclusive(true);
            onewireAdapter.setSearchAllDevices();
            onewireAdapter.targetAllFamilies();
            onewireAdapter.targetFamily(0x001f);
            Enumeration owdEnum;
            OneWireContainer1F container;
            if (onewireAdapter.findFirstDevice()) {
                for (owdEnum = onewireAdapter.getAllDeviceContainers(); owdEnum.hasMoreElements(); ) {
                    container = (OneWireContainer1F) owdEnum.nextElement();
                    couplers.put(container.getAddressAsString(), container);
                }
            }
            logger.debug("Found " + couplers.size() + " DS2409 1-Wire LAN couplers");
        } catch (OneWireException owe) {
            logger.error("1-Wire exception while detecting hardware: " + owe.getMessage());
        } finally {
            onewireAdapter.endExclusive();
        }
        for (HubDef hubDef : hubDefs) {
            OneWireContainer1F channel1 = couplers.get(hubDef.getHardwareAddr(HubChannel.ONE));
            OneWireContainer1F channel2 = couplers.get(hubDef.getHardwareAddr(HubChannel.TWO));
            OneWireContainer1F channel3 = couplers.get(hubDef.getHardwareAddr(HubChannel.THREE));
            this.hubDrivers.add(new OneWireHub(hubDef.getDesc(), channel1, channel2, channel3));
            logger.debug("New 1-Wire hub [" + hubDef.getDesc() + "] successfully created");
        }
    }

    /**
     * 
     * @param searchAddr
     * @return
     */
    private OneWireHub findHub(String searchAddr) {
        logger.debug("Searching for 1-Wire hub with hardware address of " + searchAddr);
        for (OneWireHub hub : this.hubDrivers) {
            for (String hardwareAddr : hub.getCouplerAddrs()) {
                if (hardwareAddr.equalsIgnoreCase(searchAddr)) {
                    return hub;
                }
            }
        }
        return null;
    }

    /**
     * 
     * @param containerMap
     * @throws OneWireException
     */
    private void searchActiveBranch(Map<String, HardwareContainer> containerMap) throws OneWireException {
        searchActiveBranch(null, null, null, containerMap);
    }

    /**
     * Search the currently active branch for 1-Wire sensors/devices.
     * 
     * @param hub
     * @param containerMap
     * @throws OneWireException
     */
    private void searchActiveBranch(OneWireHub hub, HubChannel channel, HubPort port, Map<String, HardwareContainer> containerMap) throws OneWireException {
        logger.debug("Searching active branch for 1-Wire hardware containers");
        logger.debug("  Using hub: " + ((hub == null) ? "null" : hub.getDesc()) + ", channel: " + ((channel == null) ? "null" : channel.getChannel()) + ", port: " + ((port == null) ? "null" : port.getPortName()));
        onewireAdapter.setSearchAllDevices();
        onewireAdapter.targetAllFamilies();
        onewireAdapter.excludeFamily(0x001f);
        Enumeration containerList = onewireAdapter.getAllDeviceContainers();
        int counter = 0;
        while (containerList.hasMoreElements()) {
            OneWireContainer container = (OneWireContainer) containerList.nextElement();
            containerMap.put(container.getAddressAsString(), new OneWireContainerImpl(onewireAdapter, hub, channel, port, container));
            counter++;
        }
        logger.debug("  Found " + counter + " 1-Wire hardware containers");
    }

    /**
     * 
     * @param addr
     * @param channel
     * @throws OneWireException
     */
    private void activateCoupler(String addr, int channel) throws OneWireException {
        byte[] state;
        OneWireContainer1F owc = new OneWireContainer1F(onewireAdapter, addr);
        state = owc.readDevice();
        owc.setLatchState(channel, true, false, state);
        owc.writeDevice(state);
    }

    /**
     * 
     * @param addr
     * @param chan
     * @throws OneWireException
     */
    private void deactivateCoupler(String addr, int chan) throws OneWireException {
        byte[] state;
        OneWireContainer1F owc = new OneWireContainer1F(onewireAdapter, addr);
        state = owc.readDevice();
        owc.setLatchState(chan, false, false, state);
        owc.writeDevice(state);
    }

    /**
     * Reset the 1-Wire network.
     */
    private void resetNetwork() {
        logger.debug("Resetting 1-wire network");
        try {
            int result = onewireAdapter.reset();
            if (result == 0) logger.warn("Reset indicates no Device Present");
            if (result == 3) logger.warn("Reset indicates 1-Wire network is shorted");
            logger.debug("1-Wire network reset successfully");
        } catch (OneWireException e) {
            logger.error("Exception while resetting the 1-Wire network: " + e.getMessage(), e);
        }
    }
}

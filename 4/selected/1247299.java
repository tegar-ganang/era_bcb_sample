package net.sf.dz.daemon.onewire.owapi;

import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.container.HumidityContainer;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.SwitchContainer;
import com.dalsemi.onewire.container.TemperatureContainer;
import com.dalsemi.onewire.utils.OWPath;
import net.sf.dz.daemon.Server;
import net.sf.dz.daemon.ServerModule;
import net.sf.dz.daemon.onewire.DeviceContainer;
import net.sf.dz.daemon.onewire.HumidityContainerListener;
import net.sf.dz.daemon.onewire.OneWireContainerListener;
import net.sf.dz.daemon.onewire.OneWireNetworkEvent;
import net.sf.dz.daemon.onewire.OneWireNetworkEventListener;
import net.sf.dz.daemon.onewire.TemperatureContainerListener;
import net.sf.dz.util.CollectionSynchronizer;
import net.sf.dz2.meta.model.MetaAware;
import net.sf.dz2.meta.model.MetaMeta;
import net.sf.jukebox.conf.Configuration;
import net.sf.jukebox.jmx.JmxDescriptor;
import net.sf.jukebox.sem.RWLock;
import net.sf.jukebox.service.ActiveService;
import java.io.IOException;
import java.util.*;

/**
 * The 1-Wire&reg; server.
 * <p>
 * Communicates with the 1-Wire&reg; devices and allows to broadcast the data
 * from these devices, as well as to control them, from the other applications
 * using a protocol adapter.
 * <p>
 * This approach is necessitated by the fact that the 1-Wire&reg; adapter (at
 * least the serial one) is not really thread safe, and the sharing on the
 * hardware level is not supported either.
 * <p>
 * In fact, this class can be considered a glorified 1-Wire&reg; adapter driver.
 * <p>
 * Even though a similar approach is implemented in the <a
 * href="http://www.ibutton.com/software/1wire/1wire_api.html"
 * target="_top">1-Wire API for Java</a>, it is not really usable at the moment
 * of writing, because it suffers from thread safety and locking problems.
 * <p>
 * All in all, this has to be considered as a quick and dirty hack, mostly
 * because the underlying implementation is not perfect. However, important
 * thing is: it's working reliably.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2007
 * @version $Id: OneWireServer.java,v 1.31 2007-03-26 09:26:08 vtt Exp $
 */
public class OneWireServer extends ActiveService implements ServerModule, OneWireNetworkEventListener, net.sf.dz.daemon.onewire.OneWireServer, MetaAware {

    /**
   * Constant to use as a key for humidity data.
   */
    public static final String DATA_HUM = "humidity";

    /**
   * Constant to use as a key for temperature data.
   */
    public static final String DATA_TEMP = "temperature";

    /**
   * Constant to use as a key for switch data.
   */
    public static final String DATA_SWITCH = "switch";

    /**
   * The server this module is attached to.
   */
    private Server server;

    /**
   * Read/write lock controlling the exclusive access to the 1-Wire devices.
   * <p>
   * This seems to be a better idea than using
   * <code>beginExclusive()/endExclusive()</code>. OneWire API uses
   * <code>Thread.sleep(50)</code>, which doesn't guarantee first come,
   * first served order, but rather a random order depending on the thread
   * timings. This can produce the wait times for the DS2406 handler as long
   * as 120 seconds, and I suspect it could be worse.
   * <p>
   * On the contrary, while the <code>RWLock</code> still doesn't guarantee
   * the FIFO order, it does provide access as soon as the resource is free -
   * the timings are significantly better. Next step would be to modify the
   * <code>RWLock</code> to guarantee the FIFO order, whether it makes sense
   * to do it or not - let's wait and see.
   */
    private RWLock lock = new RWLock();

    /**
   * The 1-Wire&reg; adapter.
   */
    private DSPortAdapter adapter;

    /**
   * Adapter port. There is no default. If the value is not specified, the
   * module will fail to initialize. This may be less convenient for USB
   * adapters in case there's just one, but a) this encourages exactness and
   * b) this will properly handle the case where there is more than one USB
   * adapter.
   */
    private String adapterPort = null;

    /**
   * Adapter speed. Defaults to
   * {@link DSPortAdapter#SPEED_REGULAR SPEED_REGULAR}.
   */
    private int adapterSpeed = DSPortAdapter.SPEED_REGULAR;

    /**
   * Device map. The key is the address, the value is the device container.
   * This map is duplicated by address to device maps contained in the
   * {@link #path2device path to device} map.
   */
    private ContainerMap address2dcGlobal = new ContainerMap();

    /**
   * Device map.
   * <p>
   * The key is the device path, the value is a sorted map where the key is
   * the hardware address, and the value is the device container. Such a
   * complication is required to optimize the access by opening the minimal
   * number of paths and eliminating redundancy.
   */
    private Map<OWPath, ContainerMap> path2device = new TreeMap<OWPath, ContainerMap>();

    /**
   * Data map.
   */
    private DataMap dataMap = new DataMap();

    /**
   * Low-level state map. The key is the device address, the value is last
   * known state obtained using <code>readDevice()</code>.
   */
    private Map<String, byte[]> stateMap = new TreeMap<String, byte[]>();

    /**
   * The listener set.
   */
    private Set<OneWireContainerListener> listenerSet = new HashSet<OneWireContainerListener>();

    /**
   * The network monitor.
   */
    private OneWireNetworkMonitor monitor;

    /**
   * Get a server lock.
   *
   * @return The server lock.
   */
    public final RWLock getLock() {
        return lock;
    }

    /**
   * {@inheritDoc}
   */
    public final void attach(final Server server) {
        if (this.server != null) {
            throw new IllegalStateException("Already attached to the server");
        }
        this.server = server;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected final void configure() throws Throwable {
        String cfroot = getConfigurationRoot();
        Configuration cf = getConfiguration();
        configureOneWire(cfroot, cf);
    }

    /**
   * Configure the 1-Wire&reg; subsystem.
   *
   * @param cfroot Configuration root.
   * @param cf The configuration object.
   * @exception Throwable if anything goes wrong.
   */
    private void configureOneWire(final String cfroot, final Configuration cf) throws Throwable {
        try {
            String cfspeed = cf.getString(cfroot + ".onewire_server.serial.speed");
            if ("overdrive".equalsIgnoreCase(cfspeed)) {
                adapterSpeed = DSPortAdapter.SPEED_OVERDRIVE;
            } else if ("hyperdrive".equalsIgnoreCase(cfspeed)) {
                adapterSpeed = DSPortAdapter.SPEED_HYPERDRIVE;
            } else if ("flex".equalsIgnoreCase(cfspeed)) {
                adapterSpeed = DSPortAdapter.SPEED_FLEX;
            } else if ("regular".equalsIgnoreCase(cfspeed)) {
                adapterSpeed = DSPortAdapter.SPEED_REGULAR;
            } else {
                if (cfspeed != null) {
                    throw new IllegalArgumentException("Invalid value for " + getConfigurationRoot() + ".onewire_server.serial.speed: '" + cfspeed + "'");
                }
            }
        } catch (NoSuchElementException nseex) {
            logger.info("No configuration value for " + cfroot + ".onewire_server.serial.speed found, using default (speed_regular)");
        }
        adapterPort = cf.getString(cfroot + ".onewire_server.serial.port");
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected final void startup() throws Throwable {
        getConfiguration();
        Set<String> portsAvailable = new TreeSet<String>();
        for (Enumeration<DSPortAdapter> adapters = OneWireAccessProvider.enumerateAllAdapters(); adapters.hasMoreElements(); ) {
            DSPortAdapter a = adapters.nextElement();
            logger.debug("Adapter found: " + a.getAdapterName());
            for (Enumeration<String> ports = a.getPortNames(); ports.hasMoreElements(); ) {
                String portName = ports.nextElement();
                logger.debug("Port found: " + portName);
                if (adapterPort.equals(portName)) {
                    adapter = a;
                }
                portsAvailable.add(portName);
            }
        }
        if (adapter == null) {
            throw new IllegalArgumentException("Port '" + adapterPort + "' unavailable, valid values: " + portsAvailable);
        }
        if (!adapter.selectPort(adapterPort)) {
            throw new IllegalArgumentException("Unable to select port '" + adapterPort + "', make sure it's the right one (available: " + portsAvailable + ")");
        }
        try {
            adapter.reset();
        } catch (OneWireIOException ex) {
            if ("Error communicating with adapter".equals(ex.getMessage())) {
                throw new IOException("Port '" + adapterPort + "' doesn't seem to have adapter connected, check others: " + portsAvailable);
            }
        }
        logger.info("Adapter class: " + adapter.getClass().getName());
        logger.info("Adapter port:  " + adapterPort);
        try {
            adapter.setSpeed(adapterSpeed);
            logger.info("set adapter speed to " + adapterSpeed);
        } catch (Throwable t) {
            logger.error("Failed to set adapter speed, cause:", t);
        }
        monitor = new OneWireNetworkMonitor(adapter, lock);
        monitor.start();
        monitor.addListener(this);
        monitor.getSemUp().waitFor();
    }

    /**
   * Keep polling the device state until stopped.
   *
   * @exception Throwable if anything goes wrong.
   */
    @Override
    protected final void execute() throws Throwable {
        while (isEnabled()) {
            try {
                poll();
            } catch (java.util.ConcurrentModificationException cmex) {
                logger.debug("Arrival/departure during poll, ignored");
            } catch (Throwable t) {
                logger.error("Poll broken:", t);
                monitor.rescan().waitFor();
            }
        }
    }

    /**
   * {@inheritDoc}
   */
    @Override
    protected final void shutdown() throws Throwable {
        logger.info("Shutting down...");
        monitor.removeListener(this);
        monitor.stop().waitFor();
        logger.info("Shut down");
    }

    /**
   * Poll the 1-Wire network for temperature, humidity and (later) pressure
   * data.
   *
   * @exception Throwable if anything goes wrong.
   */
    private void poll() throws Throwable {
        DataMap localDataMap = new DataMap();
        Object lockToken = null;
        long start = System.currentTimeMillis();
        try {
            if (path2device.isEmpty()) {
                logger.debug("No devices - forcing rescan");
                lockToken = lock.release(lockToken);
                monitor.rescan().waitFor();
                return;
            }
            for (Iterator<OWPath> i = path2device.keySet().iterator(); i.hasNext(); ) {
                try {
                    lockToken = lock.getWriteLock();
                    logger.debug("Poll: got lock in " + (System.currentTimeMillis() - start) + "ms");
                    start = System.currentTimeMillis();
                    OWPath path = i.next();
                    try {
                        path.open();
                    } catch (NullPointerException npex) {
                        logger.error("null path encountered, all paths: " + path2device.keySet(), npex);
                        lockToken = lock.release(lockToken);
                        start = System.currentTimeMillis();
                        continue;
                    }
                    logger.debug("Path open: " + path + " in " + (System.currentTimeMillis() - start) + "ms");
                    ContainerMap address2dcForPath = path2device.get(path);
                    if (address2dcForPath == null || address2dcForPath.isEmpty()) {
                        logger.warn("Null address set for '" + path + "'");
                        lockToken = lock.release(lockToken);
                        Thread.sleep(500);
                        start = System.currentTimeMillis();
                        continue;
                    }
                    for (Iterator<String> ai = address2dcForPath.iterator(); ai.hasNext(); ) {
                        if (!isEnabled()) {
                            logger.info("Oops! Not enabled anymore...");
                            return;
                        }
                        String address = ai.next();
                        Set<DeviceContainer> dcSet = address2dcForPath.get(address);
                        if (dcSet == null) {
                            logger.warn("No sensors for " + address + "???");
                            continue;
                        }
                        for (Iterator<DeviceContainer> di = dcSet.iterator(); di.hasNext(); ) {
                            OneWireDeviceContainer dc = (OneWireDeviceContainer) di.next();
                            OneWireContainer owc = dc.container;
                            if (dc instanceof OneWireTemperatureContainer) {
                                TemperatureContainer tc = (TemperatureContainer) owc;
                                try {
                                    double temp = getTemperature(tc);
                                    logger.debug(address + ": " + temp + "C");
                                    localDataMap.put(address, DATA_TEMP, new Double(temp));
                                    currentTemperatureChanged(dc, temp);
                                } catch (OneWireIOException owioex) {
                                    throw owioex;
                                } catch (Throwable t) {
                                    logger.warn("Failed to read " + address + ", cause:", t);
                                }
                            } else if (dc instanceof OneWireSwitchContainer) {
                                if (!"DS2409".equals(owc.getName())) {
                                    localDataMap.put(address, DATA_SWITCH, getState((SwitchContainer) owc));
                                }
                            } else if (dc instanceof OneWireHumidityContainer) {
                                HumidityContainer hc = (HumidityContainer) owc;
                                long hstart = System.currentTimeMillis();
                                byte[] state = hc.readDevice();
                                hc.doHumidityConvert(state);
                                double humidity = hc.getHumidity(state);
                                logger.debug("Humidity: " + humidity + " (took " + (System.currentTimeMillis() - hstart) + "ms to figure out)");
                                localDataMap.put(address, DATA_HUM, new Double(humidity));
                                currentHumidityChanged(dc, humidity);
                            }
                        }
                    }
                } finally {
                    lockToken = lock.release(lockToken);
                    logger.debug("Poll: path complete in " + (System.currentTimeMillis() - start) + "ms");
                    Thread.sleep(100);
                    start = System.currentTimeMillis();
                }
            }
            logger.debug("poll done");
            if (path2device.isEmpty()) {
                Thread.sleep(1000);
            }
        } finally {
            lock.release(lockToken);
        }
        localDataMap.transferTo(dataMap);
        logger.debug("Data map: " + dataMap);
    }

    /**
   * Get the temperature container reading.
   *
   * @param tc Temperature container to get the reading from.
   * @exception OneWireException if there was a problem talking to 1-Wire&reg;
   * device.
   * @return Current temperature.
   * @throws OneWireIOException If there was a problem with 1-Wire subsystem.
   */
    final double getTemperature(final TemperatureContainer tc) throws OneWireException, OneWireIOException {
        long start = System.currentTimeMillis();
        long now = start;
        String address = ((OneWireContainer) tc).getAddressAsString();
        double lastTemp;
        byte[] state = stateMap.get(address);
        if (state == null) {
            logger.warn("device state is not available yet, possibly setHiRes failed");
            state = tc.readDevice();
        }
        now = System.currentTimeMillis();
        logger.debug("ReadDevice/0: " + (now - start));
        tc.doTemperatureConvert(state);
        now = System.currentTimeMillis();
        logger.debug("doTemperatureConvert: " + (now - start));
        state = tc.readDevice();
        now = System.currentTimeMillis();
        logger.debug("ReadDevice/1: " + (now - start));
        lastTemp = tc.getTemperature(state);
        if (lastTemp == 85.0) {
            throw new IllegalStateException("Temp read is 85C, ignored");
        }
        stateMap.put(address, state);
        return lastTemp;
    }

    /**
   * Try to set the highest possible resolution available from the temperature
   * container.
   *
   * @param tc Temperature container to set the resolution of.
   * @param path Path to reach the container.
   */
    private void setHiRes(final TemperatureContainer tc, final OWPath path) {
        Object lockToken = null;
        try {
            lockToken = lock.getWriteLock();
            path.open();
            byte[] state = tc.readDevice();
            if (tc.hasSelectableTemperatureResolution()) {
                double[] resolution = tc.getTemperatureResolutions();
                String s = "";
                for (int idx = 0; idx < resolution.length; idx++) {
                    s += Double.toString(resolution[idx]) + " ";
                }
                logger.debug("Temperature resolutions available: " + s);
                tc.setTemperatureResolution(resolution[resolution.length - 1], state);
            }
            tc.writeDevice(state);
            stateMap.put(((OneWireContainer) tc).getAddressAsString(), state);
        } catch (Throwable t) {
            logger.warn("Failed to set high resolution on " + ((OneWireContainer) tc).getAddressAsString() + ", cause:", t);
        } finally {
            lock.release(lockToken);
        }
    }

    /**
   * Get the switch container state.
   *
   * @param sc Switch container to get the state of.
   * @return The switch state object.
   * @exception OneWireException if there was a problem with 1-Wire API.
   */
    private SwitchState getState(final SwitchContainer sc) throws OneWireException {
        SwitchState ss = new SwitchState();
        byte[] state = sc.readDevice();
        ss.smart = sc.hasSmartOn();
        ss.state[0] = sc.getLatchState(0, state);
        ss.state[1] = sc.getLatchState(1, state);
        return ss;
    }

    /**
   * Add the listener.
   *
   * @param listener The listener to add.
   */
    public final synchronized void addListener(final OneWireContainerListener listener) {
        synchronized (listenerSet) {
            listenerSet.add(listener);
        }
        for (Iterator<String> i = address2dcGlobal.iterator(); i.hasNext(); ) {
            String address = i.next();
            Set<DeviceContainer> dcSet = address2dcGlobal.get(address);
            if (dcSet == null) {
                continue;
            }
            for (Iterator<DeviceContainer> di = dcSet.iterator(); di.hasNext(); ) {
                DeviceContainer dc = di.next();
                listener.deviceArrived(dc);
            }
        }
    }

    /**
   * Remove a listener.
   *
   * @param listener The listener to remove.
   */
    public final void removeListener(final OneWireContainerListener listener) {
        synchronized (listenerSet) {
            listenerSet.remove(listener);
        }
    }

    /**
   * Broadcast the notification.
   *
   * @param dc Device container for the sensor whose temperature has changed.
   * @param temp Current sensor temperature.
   */
    private void currentTemperatureChanged(final DeviceContainer dc, final double temp) {
        for (Iterator<OneWireContainerListener> i = (new CollectionSynchronizer<OneWireContainerListener>()).copy(listenerSet).iterator(); i.hasNext(); ) {
            OneWireContainerListener l = i.next();
            if (l instanceof TemperatureContainerListener) {
                try {
                    TemperatureContainerListener tcl = (TemperatureContainerListener) l;
                    tcl.currentTemperatureChanged(dc, temp);
                } catch (Throwable t) {
                    logger.warn("Can't notify, cause:", t);
                }
            }
        }
    }

    /**
   * Broadcast the notification.
   *
   * @param dc Device container for the sensor whose humidity has changed.
   * @param humidity Current sensor humidity.
   */
    private void currentHumidityChanged(final DeviceContainer dc, final double humidity) {
        for (Iterator<OneWireContainerListener> i = (new CollectionSynchronizer<OneWireContainerListener>()).copy(listenerSet).iterator(); i.hasNext(); ) {
            OneWireContainerListener l = i.next();
            if (l instanceof HumidityContainerListener) {
                try {
                    HumidityContainerListener tcl = (HumidityContainerListener) l;
                    tcl.currentHumidityChanged(dc, humidity);
                } catch (Throwable t) {
                    logger.warn("Can't notify, cause:", t);
                }
            }
        }
    }

    /**
   * Handle a network arrival.
   *
   * @param e Network arrival information.
   */
    public final void networkArrival(final OneWireNetworkEvent e) {
        OwapiNetworkEvent e2 = (OwapiNetworkEvent) e;
        OneWireContainer owc = e2.getDeviceContainer();
        String address = owc.getAddressAsString();
        Set<DeviceContainer> dcSet = address2dcGlobal.get(address);
        if (dcSet != null) {
            for (Iterator<DeviceContainer> i = dcSet.iterator(); i.hasNext(); ) {
                DeviceContainer oldContainer = i.next();
                if (oldContainer != null) {
                    logger.warn("Arrival notification for device already present: " + e);
                    logger.warn("Duplicate device is: " + oldContainer);
                }
            }
        }
        if (owc instanceof TemperatureContainer) {
            for (int retry = 0; retry < 5; retry++) {
                try {
                    setHiRes((TemperatureContainer) owc, e2.path);
                    break;
                } catch (Throwable t) {
                    logger.warn("Failed to setHiRes on " + address + ", trying again (" + retry + ")");
                }
            }
        } else if (owc instanceof SwitchContainer) {
            SwitchState ss = (SwitchState) dataMap.get(address, DATA_SWITCH);
            if (ss != null) {
                SwitchContainer sc = (SwitchContainer) owc;
                Object lockToken = null;
                try {
                    lockToken = lock.getWriteLock();
                    byte[] state = sc.readDevice();
                    sc.setLatchState(0, ss.state[0], ss.smart, state);
                    sc.setLatchState(1, ss.state[1], ss.smart, state);
                    sc.writeDevice(state);
                    logger.warn("Restored state for " + address + ": " + ss);
                } catch (OneWireException ex) {
                    logger.error("Failed to restore switch state (" + address + "), cause:", ex);
                } catch (InterruptedException iex) {
                    logger.error("Failed to restore switch state (" + address + "), cause:", iex);
                } finally {
                    lock.release(lockToken);
                }
            }
        }
        ContainerMap address2dcForPath = path2device.get(e2.path);
        if (address2dcForPath == null) {
            address2dcForPath = new ContainerMap();
            path2device.put(e2.path, address2dcForPath);
        }
        Set<OneWireDeviceContainer> newDcSet = createContainer(owc);
        for (Iterator<OneWireDeviceContainer> i = newDcSet.iterator(); i.hasNext(); ) {
            DeviceContainer dc = i.next();
            logger.debug("Created container: " + dc);
            address2dcForPath.add(dc);
            address2dcGlobal.add(dc);
            for (Iterator<OneWireContainerListener> li = (new CollectionSynchronizer<OneWireContainerListener>()).copy(listenerSet).iterator(); li.hasNext(); ) {
                OneWireContainerListener l = li.next();
                l.deviceArrived(dc);
            }
        }
    }

    /**
   * Create DZ containers for 1-Wire container.
   *
   * @param owc 1-Wire device container.
   * @return Set of DZ device containers created for a given 1-Wire device
   * container.
   */
    private Set<OneWireDeviceContainer> createContainer(final OneWireContainer owc) {
        Set<OneWireDeviceContainer> result = new TreeSet<OneWireDeviceContainer>();
        if (owc instanceof HumidityContainer) {
            result.add(new OneWireHumidityContainer(owc));
        }
        if (owc instanceof TemperatureContainer) {
            result.add(new OneWireTemperatureContainer(owc));
        }
        if ((owc instanceof SwitchContainer) && !("DS2409".equals(owc.getName()))) {
            result.add(new OneWireSwitchContainer(owc));
        }
        if (result.isEmpty()) {
            logger.info("createContainer(): don't know how to handle " + owc + ", generic container created");
            result.add(new OneWireDeviceContainer(owc));
        }
        return result;
    }

    /**
   * Handle a network departure.
   *
   * @param e Event to handle.
   */
    public final void networkDeparture(final OneWireNetworkEvent e) {
        OwapiNetworkEvent e2 = (OwapiNetworkEvent) e;
        Set oldContainers = address2dcGlobal.remove(e.address);
        if (oldContainers == null) {
            logger.warn("Departure notification for device that is not present: " + e.address);
        }
        boolean removed = false;
        if (e2.path != null) {
            logger.debug("Departure on known path: " + e2.path);
            ContainerMap address2dcForPath = path2device.get(e2.path);
            if (address2dcForPath == null) {
                logger.warn("networkDeparture(" + e + "): No devices for path " + e2.path);
                removed = networkDeparture(e2.address);
            } else {
                removed = networkDeparture(address2dcForPath, e2.address);
            }
            if (address2dcForPath != null && address2dcForPath.isEmpty()) {
                logger.info("Empty path " + e2.path + ", removed");
                path2device.remove(e2.path);
            }
        } else {
            removed = networkDeparture(e2.address);
        }
        if (!removed) {
            logger.warn("Got the departure notification before arrival notification for " + e.address);
        }
    }

    /**
   * Handle a network departure for a known path.
   *
   * @param address2dcForPath Address to device mapping for the path the
   * device is supposed to be.
   * @param address Device address to handle departure of.
   * @return true if device has been sucessfully unmapped, false otherwise.
   */
    private boolean networkDeparture(final ContainerMap address2dcForPath, final String address) {
        if (address2dcForPath == null) {
            throw new IllegalArgumentException("Null map for " + address);
        }
        Set<DeviceContainer> dcSet = address2dcForPath.get(address);
        if (dcSet == null || dcSet.isEmpty()) {
            return false;
        }
        address2dcForPath.remove(address);
        stateMap.remove(address);
        for (Iterator<OneWireContainerListener> i = (new CollectionSynchronizer<OneWireContainerListener>()).copy(listenerSet).iterator(); i.hasNext(); ) {
            OneWireContainerListener l = i.next();
            for (Iterator<DeviceContainer> di = dcSet.iterator(); di.hasNext(); ) {
                DeviceContainer dc = di.next();
                l.deviceDeparted(dc);
            }
        }
        return true;
    }

    /**
   * Handle a network departure for unknown path.
   *
   * @param address Device addres to handle departure of.
   * @return true if device has been sucessfully unmapped, false otherwise.
   */
    private boolean networkDeparture(final String address) {
        boolean removed = false;
        logger.info("Departure on unknown path");
        for (Iterator<OWPath> pi = path2device.keySet().iterator(); pi.hasNext(); ) {
            OWPath path = pi.next();
            ContainerMap address2dcForPath = path2device.get(path);
            if (address2dcForPath == null) {
                logger.warn("networkDeparture(" + address + "): No devices for path " + path);
                continue;
            }
            removed = networkDeparture(address2dcForPath, address);
            if (address2dcForPath.isEmpty()) {
                logger.info("Path doesn't contain any devices, removed: " + path);
                pi.remove();
            }
            if (removed) {
                break;
            }
        }
        return removed;
    }

    /**
   * Handle a network fault.
   *
   * @param e Network fault information.
   * @param message Fault description.
   */
    public final void networkFault(final OneWireNetworkEvent e, final String message) {
        for (Iterator<OneWireContainerListener> i = (new CollectionSynchronizer<OneWireContainerListener>()).copy(listenerSet).iterator(); i.hasNext(); ) {
            OneWireContainerListener l = i.next();
            l.deviceFault(e.address, message);
        }
    }

    /**
   * Get an iterator on device addresses.
   *
   * @return Iterator where items are string representation of addresses.
   */
    public final Iterator<String> iterator() {
        return address2dcGlobal.iterator();
    }

    /**
   * Get a device container for a given address.
   *
   * @param address Address to get the device container for.
   * @return A device container for a given address, or <code>null</code> if
   * there's none.
   */
    public final Set getDeviceContainer(final String address) {
        logger.debug("Container requested for " + address);
        return address2dcGlobal.get(address);
    }

    /**
   * Get a path object for a given address.
   *
   * @param address Address to get the path for.
   * @return A path object for a given address.
   * @exception NoSuchElementException if there's no path for given address.
   */
    public final OWPath getDevicePath(final String address) {
        for (Iterator<OWPath> i = path2device.keySet().iterator(); i.hasNext(); ) {
            OWPath path = i.next();
            ContainerMap address2dcForPath = path2device.get(path);
            if (address2dcForPath.containsKey(address)) {
                return path;
            }
        }
        throw new NoSuchElementException("No path found for '" + address + "'");
    }

    /**
   * Volatile switch state representation.
   */
    protected class SwitchState {

        /**
     * True if the switch supports smart operation.
     */
        boolean smart = false;

        /**
     * Switch state. VT: FIXME: Extend this for cases like DS2408 (where the
     * number of channels is not 2).
     */
        boolean[] state = { false, false };

        /**
     * @return String representation of the switch state.
     */
        @Override
        public final String toString() {
            String result = "[" + (smart ? "smart" : "dumb");
            result += "][";
            for (int idx = 0; idx < state.length; idx++) {
                if (idx != 0) {
                    result += ",";
                }
                result += state[idx];
            }
            result += "]";
            return result;
        }
    }

    /**
   * A platform independent device container.
   */
    protected class OneWireDeviceContainer extends DeviceContainer implements Comparable {

        /**
     * 1-Wire API device container.
     */
        public final OneWireContainer container;

        /**
     * Create an instance.
     *
     * @param container 1-Wire API device container to base this container
     * on.
     */
        public OneWireDeviceContainer(final OneWireContainer container) {
            if (container == null) {
                throw new IllegalArgumentException("Container can't be null");
            }
            this.container = container;
        }

        /**
     * {@inheritDoc}
     */
        @Override
        public final String getName() {
            return container.getName();
        }

        /**
     * {@inheritDoc}
     */
        @Override
        public final String getAddress() {
            return container.getAddressAsString();
        }

        /**
     * {@inheritDoc}
     */
        @Override
        public String getType() {
            return "G";
        }

        /**
     * {@inheritDoc}
     */
        @Override
        public final String toString() {
            return container.toString();
        }
    }

    /**
   * A platform independent humidity container.
   */
    protected class OneWireHumidityContainer extends OneWireDeviceContainer implements net.sf.dz.daemon.onewire.HumidityContainer {

        /**
     * Create an instance.
     *
     * @param container 1-Wire API container to base this container on.
     */
        OneWireHumidityContainer(final OneWireContainer container) {
            super(container);
        }

        /**
     * {@inheritDoc}
     */
        @Override
        public final String getType() {
            return "H";
        }
    }

    /**
   * A platform independent temperature container.
   */
    protected class OneWireTemperatureContainer extends OneWireDeviceContainer implements net.sf.dz.daemon.onewire.TemperatureContainer {

        /**
     * Create an instance.
     *
     * @param container 1-Wire API container to base this container on.
     */
        OneWireTemperatureContainer(final OneWireContainer container) {
            super(container);
        }

        /**
     * {@inheritDoc}
     */
        @Override
        public final String getType() {
            return "T";
        }
    }

    /**
   * A platform independent switch container.
   */
    protected class OneWireSwitchContainer extends OneWireDeviceContainer implements net.sf.dz.daemon.onewire.SwitchContainer {

        /**
     * Number of channels the device has.
     */
        private int channelCount = 0;

        /**
     * Create an instance.
     *
     * @param container 1-Wire API container to base this container on.
     */
        OneWireSwitchContainer(final OneWireContainer container) {
            super(container);
        }

        /**
     * {@inheritDoc}
     */
        @Override
        public final String getType() {
            return "S";
        }

        /**
     * @return Number of channels the device has.
     */
        public final synchronized int getChannelCount() {
            if (channelCount == 0) {
                Object lockToken = null;
                RWLock lock = null;
                String address = container.getAddressAsString();
                SwitchContainer sc = (SwitchContainer) container;
                try {
                    long start = System.currentTimeMillis();
                    lock = getLock();
                    lockToken = lock.getWriteLock();
                    long gotLock = System.currentTimeMillis();
                    getDevicePath(address).open();
                    byte[] state = sc.readDevice();
                    channelCount = sc.getNumberChannels(state);
                    long now = System.currentTimeMillis();
                    logger.info(address + " has " + channelCount + " channel[s], took us " + (now - start) + "ms to figure out (" + (gotLock - start) + " to get the lock, " + (now - gotLock) + " to retrieve)");
                } catch (Throwable t) {
                    logger.warn(address + ": can't retrieve channel count (assuming 2):", t);
                    channelCount = 2;
                } finally {
                    if (lock != null) {
                        lock.release(lockToken);
                    }
                }
            }
            return channelCount;
        }

        /**
     * Read channel.
     *
     * @param channel Channel to read.
     * @exception IOException if there was a problem reading the device.
     * @return Channel value.
     */
        public final boolean read(final int channel) throws IOException {
            Object lockToken = null;
            RWLock lock = null;
            SwitchContainer sc = (SwitchContainer) container;
            String address = container.getAddressAsString();
            long start = System.currentTimeMillis();
            try {
                lock = getLock();
                lockToken = lock.getWriteLock();
                logger.debug("got lock in " + (System.currentTimeMillis() - start) + "ms");
                start = System.currentTimeMillis();
                getDevicePath(address).open();
                byte[] state = sc.readDevice();
                logger.debug("readDevice: " + (System.currentTimeMillis() - start));
                return sc.getLatchState(channel, state);
            } catch (Throwable t) {
                IOException secondary = new IOException("Unable to read " + container);
                secondary.initCause(t);
                throw secondary;
            } finally {
                if (lock != null) {
                    lock.release(lockToken);
                }
                logger.debug("complete in " + (System.currentTimeMillis() - start) + "ms");
            }
        }

        /**
     * Write channel.
     *
     * @param channel Channel to write.
     * @param value Value to write.
     * @exception IOException if there was a problem writing to the device.
     */
        public final void write(final int channel, final boolean value) throws IOException {
            Object lockToken = null;
            RWLock lock = null;
            SwitchContainer sc = (SwitchContainer) container;
            String address = container.getAddressAsString();
            long start = System.currentTimeMillis();
            try {
                lock = getLock();
                lockToken = lock.getWriteLock();
                logger.debug("got lock in " + (System.currentTimeMillis() - start) + "ms");
                start = System.currentTimeMillis();
                getDevicePath(address).open();
                byte[] state = sc.readDevice();
                logger.debug("readDevice: " + (System.currentTimeMillis() - start));
                boolean smart = sc.hasSmartOn();
                sc.setLatchState(channel, value, smart, state);
                sc.writeDevice(state);
                state = sc.readDevice();
                if (value == sc.getLatchState(channel, state)) {
                    return;
                }
                logger.error("Failed to write " + container);
            } catch (Throwable t) {
                IOException secondary = new IOException("Unable to write " + container);
                secondary.initCause(t);
                throw secondary;
            } finally {
                if (lock != null) {
                    lock.release(lockToken);
                }
                logger.debug("complete in " + (System.currentTimeMillis() - start) + "ms");
            }
        }

        /**
     * Reset the device. In other words, set all channels to 0.
     *
     * @exception IOException if there was an exception writing the device.
     */
        public final void reset() throws IOException {
            for (int channel = 0; channel < getChannelCount(); channel++) {
                write(channel, false);
            }
        }
    }

    /**
   * Get the metadata.
   *
   * @return The metadata.
   */
    public MetaMeta getMetaMeta() {
        List<String> args = new LinkedList<String>();
        Map<String, String> defaults = new TreeMap<String, String>();
        Set<String> requires = new TreeSet<String>();
        Map<String, String> tips = new TreeMap<String, String>();
        args.add("port");
        args.add("speed");
        defaults.put("speed", "regular");
        tips.put("port", "Serial port");
        tips.put("speed", "Adapter speed. Fiddle with at your own risk");
        return new MetaMeta("1-Wire Adapter Driver", "sensor-source", args, defaults, requires, tips);
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        JmxDescriptor d = super.getJmxDescriptor();
        return new JmxDescriptor("DZ", d.name, d.instance, "1-Wire driver");
    }
}

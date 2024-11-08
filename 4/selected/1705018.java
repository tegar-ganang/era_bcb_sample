package org.coos.messaging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.coos.messaging.jmx.ManagedObject;
import org.coos.messaging.jmx.ManagementFactory;
import org.coos.messaging.routing.Router;
import org.coos.messaging.routing.RouterChannel;
import org.coos.messaging.routing.RouterSegment;
import org.coos.messaging.routing.RoutingAlgorithm;
import org.coos.messaging.serializer.ObjectJavaSerializer;
import org.coos.messaging.util.Log;
import org.coos.messaging.util.LogFactory;

/**
 * COOS class.
 * <p>
 * Description needed.
 * <p>
 * The COOS instance has a reference to the COOS-container in which it is running.
 *
 * @author Knut Eilif Husa, Tellu AS
 * @author Robert Bjarum, Tellu AS
 *
 */
public class COOS {

    private static final String COOS_INSTANCE_KEY = "COOSInstance";

    private static final Log LOG = LogFactory.getLog(COOS.class.getName());

    private String name;

    private Router router;

    private Map<String, Transport> transports = new ConcurrentHashMap<String, Transport>();

    private Map<String, RouterChannel> channels = new ConcurrentHashMap<String, RouterChannel>();

    private Map<String, Processor> processors = new ConcurrentHashMap<String, Processor>();

    private Map<String, ChannelServer> channelServers = new ConcurrentHashMap<String, ChannelServer>();

    private Map<String, RouterSegment> segmentMap = new ConcurrentHashMap<String, RouterSegment>();

    private Map<String, RoutingAlgorithm> routingAlgorithmMap = new ConcurrentHashMap<String, RoutingAlgorithm>();

    private COContainer coosContainer;

    private boolean started;

    private ManagedObject managedObject = null;

    protected COOS() {
        SerializerFactory.registerSerializer(Message.SERIALIZATION_METHOD_JAVA, new ObjectJavaSerializer());
        LOG.setInheritMDC(false);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        LOG.putMDC(COOS_INSTANCE_KEY, getName());
    }

    /**
     * Return reference to the COOS Container in which this COOS instance is running.
     *
     * @return COOS Container (COContainer)
     */
    public COContainer getCoosContainer() {
        return coosContainer;
    }

    public void setCoosContainer(COContainer coosContainer) {
        this.coosContainer = coosContainer;
        for (Transport transport : transports.values()) {
            transport.setCoContainer(coosContainer);
        }
        for (Channel channel : channels.values()) {
            channel.setCoContainer(coosContainer);
        }
        for (Processor processor : processors.values()) {
            processor.setCoContainer(coosContainer);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public Router getRouter() {
        return router;
    }

    public void setRouter(Router router) {
        this.router = router;
        this.router.setCOOS(this);
    }

    public void addTransport(String name, Transport transport) {
        transports.put(name, transport);
        transport.setCoContainer(coosContainer);
    }

    public Transport getTransport(String name) {
        return transports.get(name);
    }

    public void addChannel(String name, RouterChannel channel) {
        channels.put(name, channel);
        channel.setCoContainer(coosContainer);
    }

    public void removeChannel(String name) {
        channels.remove(name);
    }

    public RouterChannel getChannel(String name) {
        return channels.get(name);
    }

    public void addProcessor(String name, Processor processor) {
        processors.put(name, processor);
        processor.setCoContainer(coosContainer);
    }

    public Processor getProcessor(String name) {
        return processors.get(name);
    }

    public Map<String, Transport> getTransports() {
        return transports;
    }

    public Map<String, RouterChannel> getChannels() {
        return channels;
    }

    public Map<String, Processor> getProcessors() {
        return processors;
    }

    public Map<String, ChannelServer> getChannelServers() {
        return channelServers;
    }

    public void addChannelServer(String name, ChannelServer server) {
        channelServers.put(name, server);
    }

    public ChannelServer getChannelServer(String name) {
        return channelServers.get(name);
    }

    public Map<String, RouterSegment> getSegmentMap() {
        return segmentMap;
    }

    public void setSegmentMap(Map<String, RouterSegment> segmentMap) {
        this.segmentMap = segmentMap;
    }

    public void addSegment(RouterSegment routerSegment) {
        segmentMap.put(routerSegment.getName(), routerSegment);
    }

    public RouterSegment getSegment(String segmentName) {
        return segmentMap.get(segmentName);
    }

    public void addRoutingAlgorithm(String algorithmName, RoutingAlgorithm routingAlgorithm) {
        routingAlgorithmMap.put(algorithmName, routingAlgorithm);
    }

    public RoutingAlgorithm getRoutingAlgorithm(String algorithmName) {
        return routingAlgorithmMap.get(algorithmName);
    }

    public void start() throws Exception {
        if (coosContainer == null) {
            LOG.warn("The COOS container property (COContainer) has not been set.");
        }
        LOG.info("Starting COOS " + name);
        LOG.info("Starting processors");
        for (Processor processor : processors.values()) {
            if (processor instanceof Service) {
                ((Service) processor).start();
            }
        }
        LOG.info("Starting channel servers");
        for (ChannelServer channelServer : channelServers.values()) {
            if (channelServer instanceof Service) {
                ((Service) channelServer).start();
            }
        }
        LOG.info("Initializing channels");
        for (Channel channel : channels.values()) {
            if (channel.getTransport() != null) {
                channel.connect(router);
            }
        }
        LOG.info("Starting Router");
        router.start();
        managedObject = ManagementFactory.getPlatformManagementService().registerCoos(this);
        LOG.info("COOS " + name + " successfully started");
        started = true;
    }

    public void stop() throws Exception {
        LOG.info("Stopping COOS " + name);
        LOG.info("Stopping Router");
        router.stop();
        LOG.info("Stopping channels");
        for (Channel channel : channels.values()) {
            channel.disconnect();
        }
        LOG.info("Stopping channel servers");
        for (ChannelServer channelServer : channelServers.values()) {
            channelServer.stop();
        }
        LOG.info("Stopping processors");
        for (Processor processor : processors.values()) {
            if (processor instanceof Service) {
                ((Service) processor).stop();
            }
        }
        LOG.info("COOS " + name + " stopped");
        started = false;
        if (managedObject != null) {
            ManagementFactory.getPlatformManagementService().unregister(managedObject);
        }
    }
}

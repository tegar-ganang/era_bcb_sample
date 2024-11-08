package org.coos.messaging;

import org.coos.messaging.serializer.ObjectJavaSerializer;
import org.coos.messaging.util.URIHelper;
import org.coos.messaging.util.UuidHelper;
import org.coos.pluginXMLSchema.ChannelType;
import org.coos.pluginXMLSchema.FilterType;
import org.coos.pluginXMLSchema.InBoundType;
import org.coos.pluginXMLSchema.OutBoundType;
import org.coos.pluginXMLSchema.PluginType;
import org.coos.pluginXMLSchema.PluginsDocument;
import org.coos.pluginXMLSchema.PluginsType;
import org.coos.pluginXMLSchema.ProcessorType;
import org.coos.pluginXMLSchema.PropertyType;
import org.coos.pluginXMLSchema.TransportType;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Factory for Plugin instances
 *
 * @author Knut Eilif Husa, Tellu AS
 */
public class PluginFactory extends BasePluginFactory {

    public static final String JVM_TRANSPORT_CLASS = "org.coos.messaging.transport.JvmTransport";

    private static Map<String, Processor> sharedProcessors = new HashMap<String, Processor>();

    private static Map<String, ProcessorType> processorTypes = new HashMap<String, ProcessorType>();

    private PluginFactory() {
        SerializerFactory.registerSerializer(Message.SERIALIZATION_METHOD_JAVA, new ObjectJavaSerializer());
    }

    /**
     * This factory method creates a defined plugin with a jvm transport
     * @param name name name of endpoint
     * @param className the endpoint implementation class
     * @param segment the segment, default is "."
     * @param coosInstanceName the coos instance name
     * @param channelServerName the coos channel server name
     * @return the Plugin
     * @throws Exception
     */
    public static Plugin createPlugin(String name, String className, String segment, String coosInstanceName, String channelServerName) throws Exception {
        COContainer cl = new COContainerImpl();
        Plugin plugin = createPluginEndpoint(name, className, null, segment, cl);
        Channel channel = createPluginChannel(segment, cl);
        plugin.addChannel(channel);
        Hashtable<String, String> properties = new Hashtable<String, String>();
        if (coosInstanceName != null) properties.put("COOSInstanceName", coosInstanceName);
        if (channelServerName != null) properties.put("ChannelServerName", channelServerName);
        Transport transport = createTransport(JVM_TRANSPORT_CLASS, properties, cl);
        channel.setTransport(transport);
        return plugin;
    }

    /**
     * This factory method creates a set of plugins based on the xml config
     * @param config  xml config provided as InputStream
     * @return the set of Plugins
     * @throws Exception
     */
    public static Plugin[] createPlugins(InputStream config) throws Exception {
        PluginsDocument doc = PluginsDocument.Factory.parse(config);
        COContainer cl = new COContainerImpl();
        Plugin[] plugins = instantiate(doc.getPlugins(), cl);
        return plugins;
    }

    /**
     * This factory method creates a set of plugins based on the xml config
     * @param config xml config provided as InputStream
     * @param cl the container
     * @return the set of Plugins
     * @throws Exception
     */
    public static Plugin[] createPlugins(InputStream config, COContainer cl) throws Exception {
        PluginsDocument doc = PluginsDocument.Factory.parse(config);
        Plugin[] plugins = instantiate(doc.getPlugins(), cl);
        return plugins;
    }

    private static Plugin[] instantiate(PluginsType model, COContainer cl) throws Exception {
        Plugin[] res = new Plugin[model.getPluginArray().length];
        for (int i = 0; i < model.getProcessorArray().length; i++) {
            ProcessorType processorType = model.getProcessorArray()[i];
            if ((processorType.getName() == null) || (processorType.getClass1() == null)) {
                throw new Exception("Plugin processors must have a name and a implementing class. Must be defined!");
            }
            String name = processorType.getName();
            processorTypes.put(name, processorType);
            Processor processor = instantiateProcessor(processorType, cl);
            if (processor.isShared()) {
                sharedProcessors.put(name, processor);
            }
        }
        HashMap<String, Transport> transportMap = new HashMap<String, Transport>();
        for (int i = 0; i < model.getTransportArray().length; i++) {
            TransportType transportType = model.getTransportArray()[i];
            if ((transportType.getName() == null) || (transportType.getClass1() == null)) {
                throw new Exception("Plugin transports must have a name and an implementing class. Must be defined!");
            }
            String name = transportType.getName();
            String className = transportType.getClass1();
            Map<String, String> props = new HashMap<String, String>();
            for (int j = 0; j < transportType.getPropertyArray().length; j++) {
                PropertyType propertyType = transportType.getPropertyArray()[j];
                if ((propertyType.getName() == null) || (propertyType.getValue() == null)) {
                    throw new Exception("Plugin properties must have a name and a value. Must be defined!");
                }
                props.put(propertyType.getName(), propertyType.getValue());
            }
            Class<?> transportClass = PluginFactory.tryClass(cl, className);
            Transport transport = (Transport) transportClass.newInstance();
            transport.setName(name);
            transport.setCoContainer(cl);
            transport.setProperties(new Hashtable<String, String>(props));
            transportMap.put(name, transport);
        }
        HashMap<String, Channel> channelMap = new HashMap<String, Channel>();
        for (int i = 0; i < model.getChannelArray().length; i++) {
            ChannelType channelType = model.getChannelArray()[i];
            if ((channelType.getName() == null) || (channelType.getClass1() == null)) {
                throw new Exception("Plugin channels must have a name and an implementing class. Must be defined!");
            }
            String name = channelType.getName();
            String className = channelType.getClass1();
            String protocol = channelType.getProtocol2();
            String segment = channelType.getSegment();
            Class<?> channelClass = PluginFactory.tryClass(cl, className);
            Channel channel = (Channel) channelClass.newInstance();
            channel.setCoContainer(cl);
            if (protocol != null) {
                channel.addProtocol(protocol);
            } else {
                channel.addProtocol("coos");
            }
            channel.setName(name);
            if (segment != null) {
                channel.setSegment(segment);
            }
            Map<String, String> props = new HashMap<String, String>();
            for (int j = 0; j < channelType.getPropertyArray().length; j++) {
                PropertyType propertyType = channelType.getPropertyArray()[j];
                if ((propertyType.getName() == null) || (propertyType.getValue() == null)) {
                    throw new Exception("Plugin properties must have a name and a value. Must be defined!");
                }
                props.put(propertyType.getName(), propertyType.getValue());
            }
            channel.setProperties(new Hashtable<String, String>(props));
            for (int j = 0; j < channelType.getProtocolArray().length; j++) {
                String prot = channelType.getProtocolArray()[j];
                channel.addProtocol(prot);
            }
            String transportType = channelType.getTransport();
            if (transportType != null) {
                if (!transportMap.keySet().contains(transportType)) {
                    throw new Exception("Transport " + transportType + " is not declared.");
                }
                channel.setTransport((Transport) transportMap.get(transportType).copy());
            } else {
                Class<?> transportClass = PluginFactory.tryClass(cl, JVM_TRANSPORT_CLASS);
                Transport transport = (Transport) transportClass.newInstance();
                transport.setCoContainer(cl);
                channel.setTransport(transport);
            }
            OutBoundType outBoundType = channelType.getOutBound();
            if (outBoundType != null) {
                for (int j = 0; j < outBoundType.getFilterArray().length; j++) {
                    FilterType filterType = outBoundType.getFilterArray()[j];
                    String processor = filterType.getProcessor();
                    ProcessorType procType = processorTypes.get(processor);
                    if (procType == null) {
                        throw new Exception("Processor " + processor + " is not declared.");
                    }
                    if (procType.getShared()) {
                        channel.getOutLink().addFilterProcessor(sharedProcessors.get(processor));
                    } else {
                        channel.getOutLink().addFilterProcessor(instantiateProcessor(procType, cl));
                    }
                }
            }
            InBoundType inBoundType = channelType.getInBound();
            if (inBoundType != null) {
                for (int j = 0; j < inBoundType.getFilterArray().length; j++) {
                    FilterType filterType = inBoundType.getFilterArray()[j];
                    String processor = filterType.getProcessor();
                    ProcessorType procType = processorTypes.get(processor);
                    if (procType == null) {
                        throw new Exception("Processor " + processor + " is not declared.");
                    }
                    if (procType.getShared()) {
                        channel.getInLink().addFilterProcessor(sharedProcessors.get(processor));
                    } else {
                        channel.getInLink().addFilterProcessor(instantiateProcessor(procType, cl));
                    }
                }
            }
            channelMap.put(name, channel);
        }
        for (int i = 0; i < model.getPluginArray().length; i++) {
            Plugin plugin = new Plugin();
            PluginType pluginType = model.getPluginArray()[i];
            if (pluginType.getClass1() == null) {
                throw new Exception("Plugin properties must have a name and a value. Must be defined!");
            }
            String className = pluginType.getClass1();
            Class<?> pluginClass = PluginFactory.tryClass(cl, className);
            Endpoint endpoint = (Endpoint) pluginClass.newInstance();
            endpoint.setCoContainer(cl);
            String name = pluginType.getName();
            String nameSegment = "";
            if (name != null) {
                if (UuidHelper.isUuid(name)) {
                    name = UuidHelper.getQualifiedUuid(name);
                    endpoint.setEndpointUuid(name);
                    endpoint.setEndpointUri("coos://" + name);
                    nameSegment = UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(name);
                } else {
                    endpoint.setEndpointUri("coos://" + name);
                    URIHelper uHelper = new URIHelper(endpoint.getEndpointUri());
                    nameSegment = uHelper.getSegment();
                }
                endpoint.setName(name);
            }
            String channelName = pluginType.getChannel2();
            String startLevelStr = pluginType.getStartLevel();
            if (startLevelStr != null) {
                plugin.setStartLevel(Integer.valueOf(startLevelStr));
            }
            String[] aliases = pluginType.getAliasArray();
            for (int j = 0; j < aliases.length; j++) {
                endpoint.addAlias(aliases[j]);
            }
            if ((channelName == null) && (pluginType.getChannelArray().length == 0)) {
                Class<?> channelClass1 = PluginFactory.tryClass(cl, PLUGIN_CHANNEL_CLASS);
                Channel channel1 = (Channel) channelClass1.newInstance();
                channel1.addProtocol("coos");
                channel1.setCoContainer(cl);
                channel1.setName("default");
                channelMap.put("default", channel1);
                String transportType = pluginType.getTransport();
                if (transportType != null) {
                    if (!transportMap.keySet().contains(transportType)) {
                        throw new Exception("Transport " + transportType + " is not declared.");
                    }
                    channel1.setTransport(transportMap.get(transportType));
                } else {
                    Class<?> transportClass1 = PluginFactory.tryClass(cl, JVM_TRANSPORT_CLASS);
                    Transport transport1 = (Transport) transportClass1.newInstance();
                    transport1.setCoContainer(cl);
                    channel1.setTransport(transport1);
                }
                plugin.addChannel((Channel) channelMap.get("default").copy());
            }
            if (channelName != null) {
                addChannel(channelMap, plugin, name, nameSegment, channelName);
            }
            for (int j = 0; j < pluginType.getChannelArray().length; j++) {
                channelName = pluginType.getChannelArray(j);
                addChannel(channelMap, plugin, name, nameSegment, channelName);
            }
            Map<String, String> props = new HashMap<String, String>();
            for (int k = 0; k < pluginType.getPropertyArray().length; k++) {
                PropertyType propertyType = pluginType.getPropertyArray()[k];
                if ((propertyType.getName() == null) || (propertyType.getValue() == null)) {
                    throw new Exception("Plugin properties must have a name and a value. Must be defined!");
                }
                props.put(propertyType.getName(), propertyType.getValue());
            }
            endpoint.setProperties(new Hashtable<String, String>(props));
            plugin.setEndpoint(endpoint);
            res[i] = plugin;
        }
        return res;
    }

    private static void addChannel(HashMap<String, Channel> channelMap, Plugin plugin, String name, String nameSegment, String channelName) throws Exception {
        if (!channelMap.keySet().contains(channelName)) {
            throw new Exception("Channel " + channelName + " is not declared for plugin: " + name);
        }
        Channel channel = (Channel) channelMap.get(channelName).copy();
        if (!nameSegment.equals("")) {
            if (channel.getSegment().equals("")) {
                channel.setSegment(nameSegment);
            } else if (!nameSegment.equals("dico") && !nameSegment.equals("localcoos") && !channel.getSegment().equals(nameSegment)) {
                throw new Exception("Channel " + channelName + " with segment '" + channel.getSegment() + "' does not match segment declared for plugin: " + name);
            }
        }
        plugin.addChannel(channel);
    }

    private static Processor instantiateProcessor(ProcessorType processorType, COContainer cl) throws Exception {
        String className = processorType.getClass1();
        boolean isShared = processorType.getShared();
        String name = processorType.getName();
        Map<String, String> props = new HashMap<String, String>();
        for (int j = 0; j < processorType.getPropertyArray().length; j++) {
            PropertyType propertyType = processorType.getPropertyArray()[j];
            if ((propertyType.getName() == null) || (propertyType.getValue() == null)) {
                throw new Exception("COOS properties must have a name and a value. Must be defined!");
            }
            props.put(propertyType.getName(), propertyType.getValue());
        }
        Class<?> procClass = PluginFactory.tryClass(cl, className);
        Processor processor = (Processor) procClass.newInstance();
        processor.setCoContainer(cl);
        processor.setName(name);
        processor.setProperties(new Hashtable<String, String>(props));
        processor.setShared(isShared);
        return processor;
    }
}

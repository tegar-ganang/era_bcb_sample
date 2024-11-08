package flex.messaging.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A special mxmlc compiler specific implentation of the configuration
 * parser for JDK 1.4. Only a small subset of the configuration is
 * processed to generate the information that the client needs at runtime,
 * such as channel definitions and service destination properties.
 *
 * @author Peter Farland
 * @exclude
 */
public abstract class ClientConfigurationParser extends AbstractConfigurationParser {

    protected void parseTopLevelConfig(Document doc) {
        Node root = selectSingleNode(doc, "/" + SERVICES_CONFIG_ELEMENT);
        if (root != null) {
            allowedChildElements(root, SERVICES_CONFIG_CHILDREN);
            channelsSection(root);
            services(root);
            clusters(root);
            flexClient(root);
        }
    }

    private void channelsSection(Node root) {
        Node channelsNode = selectSingleNode(root, CHANNELS_ELEMENT);
        if (channelsNode != null) {
            allowedAttributesOrElements(channelsNode, CHANNELS_CHILDREN);
            NodeList channels = selectNodeList(channelsNode, CHANNEL_DEFINITION_ELEMENT);
            for (int i = 0; i < channels.getLength(); i++) {
                Node channel = channels.item(i);
                channelDefinition(channel);
            }
        }
    }

    private void channelDefinition(Node channel) {
        requiredAttributesOrElements(channel, CHANNEL_DEFINITION_REQ_CHILDREN);
        allowedAttributesOrElements(channel, CHANNEL_DEFINITION_CHILDREN);
        String id = getAttributeOrChildElement(channel, ID_ATTR).trim();
        if (isValidID(id)) {
            if (config.getChannelSettings(id) != null) {
                ConfigurationException e = new ConfigurationException();
                e.setMessage(DUPLICATE_CHANNEL_ERROR, new Object[] { id });
                throw e;
            }
            ChannelSettings channelSettings = new ChannelSettings(id);
            String clientType = getAttributeOrChildElement(channel, CLASS_ATTR);
            clientType = clientType.length() > 0 ? clientType : null;
            String serverOnlyString = getAttributeOrChildElement(channel, SERVER_ONLY_ATTR);
            boolean serverOnly = serverOnlyString.length() > 0 && Boolean.valueOf(serverOnlyString).booleanValue();
            if (clientType == null && !serverOnly) {
                ConfigurationException ce = new ConfigurationException();
                ce.setMessage(CLASS_OR_SERVER_ONLY_ERROR, new Object[] { id });
                throw ce;
            } else if (clientType != null && serverOnly) {
                ConfigurationException ce = new ConfigurationException();
                ce.setMessage(CLASS_AND_SERVER_ONLY_ERROR, new Object[] { id });
                throw ce;
            } else {
                if (serverOnly) return;
                channelSettings.setClientType(clientType);
            }
            Node endpoint = selectSingleNode(channel, ENDPOINT_ELEMENT);
            if (endpoint != null) {
                allowedAttributesOrElements(endpoint, ENDPOINT_CHILDREN);
                String uri = getAttributeOrChildElement(endpoint, URL_ATTR);
                if (uri == null || EMPTY_STRING.equals(uri)) uri = getAttributeOrChildElement(endpoint, URI_ATTR);
                channelSettings.setUri(uri);
                config.addChannelSettings(id, channelSettings);
            }
            addProperty(channel, channelSettings, POLLING_ENABLED_ELEMENT);
            addProperty(channel, channelSettings, POLLING_INTERVAL_MILLIS_ELEMENT);
            addProperty(channel, channelSettings, PIGGYBACKING_ENABLED_ELEMENT);
            addProperty(channel, channelSettings, LOGIN_AFTER_DISCONNECT_ELEMENT);
            addProperty(channel, channelSettings, RECORD_MESSAGE_SIZES_ELEMENT);
            addProperty(channel, channelSettings, RECORD_MESSAGE_TIMES_ELEMENT);
            addProperty(channel, channelSettings, CONNECT_TIMEOUT_SECONDS_ELEMENT);
            addProperty(channel, channelSettings, POLLING_INTERVAL_SECONDS_ELEMENT);
            addProperty(channel, channelSettings, CLIENT_LOAD_BALANCING_ELEMENT);
            addProperty(channel, channelSettings, REQUEST_TIMEOUT_SECONDS_ELEMENT);
            NodeList properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/" + SERIALIZATION_ELEMENT);
            if (properties.getLength() > 0) {
                ConfigMap map = properties(properties, getSourceFileOf(channel));
                ConfigMap serialization = map.getPropertyAsMap(SERIALIZATION_ELEMENT, null);
                if (serialization != null) {
                    String enableSmallMessages = serialization.getProperty(ENABLE_SMALL_MESSAGES_ELEMENT);
                    if (enableSmallMessages != null) {
                        ConfigMap clientMap = new ConfigMap();
                        clientMap.addProperty(ENABLE_SMALL_MESSAGES_ELEMENT, enableSmallMessages);
                        channelSettings.addProperty(SERIALIZATION_ELEMENT, clientMap);
                    }
                }
            }
        } else {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_ID, new Object[] { CHANNEL_DEFINITION_ELEMENT, id });
            String details = "An id must be non-empty and not contain any list delimiter characters, i.e. commas, semi-colons or colons.";
            ex.setDetails(details);
            throw ex;
        }
    }

    private void addProperty(Node channel, ChannelSettings channelSettings, String property) {
        NodeList properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/" + property);
        if (properties.getLength() > 0) {
            ConfigMap map = properties(properties, getSourceFileOf(channel));
            if (CLIENT_LOAD_BALANCING_ELEMENT.equals(property)) {
                ConfigMap clientLoadBalancingMap = map.getPropertyAsMap(CLIENT_LOAD_BALANCING_ELEMENT, null);
                if (clientLoadBalancingMap == null) {
                    ConfigurationException ce = new ConfigurationException();
                    ce.setMessage(ERR_MSG_EMPTY_CLIENT_LOAD_BALANCING_ELEMENT, new Object[] { CLIENT_LOAD_BALANCING_ELEMENT, channelSettings.getId() });
                    throw ce;
                }
                List urls = clientLoadBalancingMap.getPropertyAsList(URL_ATTR, null);
                addClientLoadBalancingUrls(urls, channelSettings.getId());
            }
            channelSettings.addProperties(map);
        }
    }

    private void addClientLoadBalancingUrls(List urls, String endpointId) {
        if (urls == null || urls.isEmpty()) {
            ConfigurationException ce = new ConfigurationException();
            ce.setMessage(ERR_MSG_EMPTY_CLIENT_LOAD_BALANCING_ELEMENT, new Object[] { CLIENT_LOAD_BALANCING_ELEMENT, endpointId });
            throw ce;
        }
        Set clientLoadBalancingUrls = new HashSet();
        for (Iterator iterator = urls.iterator(); iterator.hasNext(); ) {
            String url = (String) iterator.next();
            if (url == null || url.length() == 0) {
                ConfigurationException ce = new ConfigurationException();
                ce.setMessage(ERR_MSG_EMTPY_CLIENT_LOAD_BALACNING_URL, new Object[] { CLIENT_LOAD_BALANCING_ELEMENT, endpointId });
                throw ce;
            }
            if (TokenReplacer.containsTokens(url)) {
                ConfigurationException ce = new ConfigurationException();
                ce.setMessage(ERR_MSG_CLIENT_LOAD_BALANCING_URL_WITH_TOKEN, new Object[] { CLIENT_LOAD_BALANCING_ELEMENT, endpointId });
                throw ce;
            }
            if (clientLoadBalancingUrls.contains(url)) iterator.remove(); else clientLoadBalancingUrls.add(url);
        }
    }

    private void services(Node root) {
        Node servicesNode = selectSingleNode(root, SERVICES_ELEMENT);
        if (servicesNode != null) {
            allowedChildElements(servicesNode, SERVICES_CHILDREN);
            Node defaultChannels = selectSingleNode(servicesNode, DEFAULT_CHANNELS_ELEMENT);
            if (defaultChannels != null) {
                allowedChildElements(defaultChannels, DEFAULT_CHANNELS_CHILDREN);
                NodeList channels = selectNodeList(defaultChannels, CHANNEL_ELEMENT);
                for (int c = 0; c < channels.getLength(); c++) {
                    Node chan = channels.item(c);
                    allowedAttributes(chan, new String[] { REF_ATTR });
                    defaultChannel(chan);
                }
            }
            NodeList services = selectNodeList(servicesNode, SERVICE_INCLUDE_ELEMENT);
            for (int i = 0; i < services.getLength(); i++) {
                Node service = services.item(i);
                serviceInclude(service);
            }
            services = selectNodeList(servicesNode, SERVICE_ELEMENT);
            for (int i = 0; i < services.getLength(); i++) {
                Node service = services.item(i);
                service(service);
            }
        }
    }

    private void clusters(Node root) {
        Node clusteringNode = selectSingleNode(root, CLUSTERS_ELEMENT);
        if (clusteringNode != null) {
            allowedAttributesOrElements(clusteringNode, CLUSTERING_CHILDREN);
            NodeList clusters = selectNodeList(clusteringNode, CLUSTER_DEFINITION_ELEMENT);
            for (int i = 0; i < clusters.getLength(); i++) {
                Node cluster = clusters.item(i);
                requiredAttributesOrElements(cluster, CLUSTER_DEFINITION_CHILDREN);
                String clusterName = getAttributeOrChildElement(cluster, ID_ATTR);
                if (isValidID(clusterName)) {
                    String propsFileName = getAttributeOrChildElement(cluster, CLUSTER_PROPERTIES_ATTR);
                    ClusterSettings clusterSettings = new ClusterSettings();
                    clusterSettings.setClusterName(clusterName);
                    clusterSettings.setPropsFileName(propsFileName);
                    String defaultValue = getAttributeOrChildElement(cluster, ClusterSettings.DEFAULT_ELEMENT);
                    if (defaultValue != null && defaultValue.length() > 0) {
                        if (defaultValue.equalsIgnoreCase("true")) clusterSettings.setDefault(true); else if (!defaultValue.equalsIgnoreCase("false")) {
                            ConfigurationException e = new ConfigurationException();
                            e.setMessage(10215, new Object[] { clusterName, defaultValue });
                            throw e;
                        }
                    }
                    String ulb = getAttributeOrChildElement(cluster, ClusterSettings.URL_LOAD_BALANCING);
                    if (ulb != null && ulb.length() > 0) {
                        if (ulb.equalsIgnoreCase("false")) clusterSettings.setURLLoadBalancing(false); else if (!ulb.equalsIgnoreCase("true")) {
                            ConfigurationException e = new ConfigurationException();
                            e.setMessage(10216, new Object[] { clusterName, ulb });
                            throw e;
                        }
                    }
                    ((ClientConfiguration) config).addClusterSettings(clusterSettings);
                }
            }
        }
    }

    private void serviceInclude(Node serviceInclude) {
        requiredAttributesOrElements(serviceInclude, SERVICE_INCLUDE_CHILDREN);
        String src = getAttributeOrChildElement(serviceInclude, SRC_ATTR);
        if (src.length() > 0) {
            Document doc = loadDocument(src, fileResolver.getIncludedFile(src));
            if (fileResolver instanceof LocalFileResolver) {
                LocalFileResolver local = (LocalFileResolver) fileResolver;
                ((ClientConfiguration) config).addConfigPath(local.getIncludedPath(src), local.getIncludedLastModified(src));
            }
            doc.getDocumentElement().normalize();
            Node service = selectSingleNode(doc, "/" + SERVICE_ELEMENT);
            if (service != null) {
                service(service);
                fileResolver.popIncludedFile();
            } else {
                ConfigurationException ex = new ConfigurationException();
                ex.setMessage(INVALID_SERVICE_INCLUDE_ROOT, new Object[] { SERVICE_ELEMENT });
                throw ex;
            }
        }
    }

    private void service(Node service) {
        requiredAttributesOrElements(service, SERVICE_REQ_CHILDREN);
        allowedAttributesOrElements(service, SERVICE_CHILDREN);
        String id = getAttributeOrChildElement(service, ID_ATTR);
        if (isValidID(id)) {
            ServiceSettings serviceSettings = config.getServiceSettings(id);
            if (serviceSettings == null) {
                serviceSettings = new ServiceSettings(id);
                NodeList properties = selectNodeList(service, PROPERTIES_ELEMENT + "/*");
                if (properties.getLength() > 0) {
                    ConfigMap map = properties(properties, getSourceFileOf(service));
                    serviceSettings.addProperties(map);
                }
                config.addServiceSettings(serviceSettings);
            } else {
                ConfigurationException e = new ConfigurationException();
                e.setMessage(DUPLICATE_SERVICE_ERROR, new Object[] { id });
                throw e;
            }
            String className = getAttributeOrChildElement(service, CLASS_ATTR);
            if (className.length() > 0) {
                serviceSettings.setClassName(className);
            } else {
                ConfigurationException ex = new ConfigurationException();
                ex.setMessage(CLASS_NOT_SPECIFIED, new Object[] { SERVICE_ELEMENT, id });
                throw ex;
            }
            Node defaultChannels = selectSingleNode(service, DEFAULT_CHANNELS_ELEMENT);
            if (defaultChannels != null) {
                allowedChildElements(defaultChannels, DEFAULT_CHANNELS_CHILDREN);
                NodeList channels = selectNodeList(defaultChannels, CHANNEL_ELEMENT);
                for (int c = 0; c < channels.getLength(); c++) {
                    Node chan = channels.item(c);
                    allowedAttributes(chan, new String[] { REF_ATTR });
                    defaultChannel(chan, serviceSettings);
                }
            } else if (config.getDefaultChannels().size() > 0) {
                for (Iterator iter = config.getDefaultChannels().iterator(); iter.hasNext(); ) {
                    String channelId = (String) iter.next();
                    ChannelSettings channel = config.getChannelSettings(channelId);
                    serviceSettings.addDefaultChannel(channel);
                }
            }
            NodeList list = selectNodeList(service, DESTINATION_ELEMENT);
            for (int i = 0; i < list.getLength(); i++) {
                Node dest = list.item(i);
                destination(dest, serviceSettings);
            }
            list = selectNodeList(service, DESTINATION_INCLUDE_ELEMENT);
            for (int i = 0; i < list.getLength(); i++) {
                Node dest = list.item(i);
                destinationInclude(dest, serviceSettings);
            }
        } else {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_ID, new Object[] { SERVICE_ELEMENT, id });
            throw ex;
        }
    }

    /**
     * Flex application can declare default channels for its services. If a
     * service specifies its own list of channels it overrides these defaults.
     * <p>
     * &lt;default-channels&gt;<br />
     * ;&lt;channel ref="channel-id" /&gt;<br />
     * &lt;default-channels&gt;
     * </p>
     */
    private void defaultChannel(Node chan) {
        String ref = getAttributeOrChildElement(chan, REF_ATTR);
        if (ref.length() > 0) {
            ChannelSettings channel = config.getChannelSettings(ref);
            if (channel != null) {
                config.addDefaultChannel(channel.getId());
            } else {
                ConfigurationException e = new ConfigurationException();
                e.setMessage(REF_NOT_FOUND, new Object[] { CHANNEL_ELEMENT, ref });
                throw e;
            }
        } else {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_DEFAULT_CHANNEL, new Object[] { "MessageBroker" });
            throw ex;
        }
    }

    /**
     * A service can declare default channels for its destinations. If a destination
     * specifies its own list of channels it overrides these defaults.
     * <p>
     * &lt;default-channels&gt;<br />
     * &lt;channel ref="channel-id" /&gt;<br />
     * &lt;default-channels&gt;
     * </p>
     */
    private void defaultChannel(Node chan, ServiceSettings serviceSettings) {
        String ref = getAttributeOrChildElement(chan, REF_ATTR).trim();
        if (ref.length() > 0) {
            ChannelSettings channel = config.getChannelSettings(ref);
            if (channel != null) {
                serviceSettings.addDefaultChannel(channel);
            } else {
                ConfigurationException e = new ConfigurationException();
                e.setMessage(REF_NOT_FOUND, new Object[] { CHANNEL_ELEMENT, ref });
                throw e;
            }
        } else {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_DEFAULT_CHANNEL, new Object[] { serviceSettings.getId() });
            throw ex;
        }
    }

    private void destinationInclude(Node destInclude, ServiceSettings serviceSettings) {
        requiredAttributesOrElements(destInclude, DESTINATION_INCLUDE_CHILDREN);
        String src = getAttributeOrChildElement(destInclude, SRC_ATTR);
        if (src.length() > 0) {
            Document doc = loadDocument(src, fileResolver.getIncludedFile(src));
            if (fileResolver instanceof LocalFileResolver) {
                LocalFileResolver local = (LocalFileResolver) fileResolver;
                ((ClientConfiguration) config).addConfigPath(local.getIncludedPath(src), local.getIncludedLastModified(src));
            }
            doc.getDocumentElement().normalize();
            Node destinationsNode = selectSingleNode(doc, DESTINATIONS_ELEMENT);
            if (destinationsNode != null) {
                allowedChildElements(destinationsNode, DESTINATIONS_CHILDREN);
                NodeList destinations = selectNodeList(destinationsNode, DESTINATION_ELEMENT);
                for (int a = 0; a < destinations.getLength(); a++) {
                    Node dest = destinations.item(a);
                    destination(dest, serviceSettings);
                }
                fileResolver.popIncludedFile();
            } else {
                Node dest = selectSingleNode(doc, "/" + DESTINATION_ELEMENT);
                if (dest != null) {
                    destination(dest, serviceSettings);
                    fileResolver.popIncludedFile();
                } else {
                    ConfigurationException ex = new ConfigurationException();
                    ex.setMessage(INVALID_DESTINATION_INCLUDE_ROOT, new Object[] { DESTINATIONS_ELEMENT, DESTINATION_ELEMENT });
                    throw ex;
                }
            }
        }
    }

    private void destination(Node dest, ServiceSettings serviceSettings) {
        requiredAttributesOrElements(dest, DESTINATION_REQ_CHILDREN);
        allowedAttributes(dest, DESTINATION_ATTR);
        allowedChildElements(dest, DESTINATION_CHILDREN);
        String serviceId = serviceSettings.getId();
        DestinationSettings destinationSettings;
        String id = getAttributeOrChildElement(dest, ID_ATTR);
        if (isValidID(id)) {
            destinationSettings = (DestinationSettings) serviceSettings.getDestinationSettings().get(id);
            if (destinationSettings != null) {
                ConfigurationException e = new ConfigurationException();
                e.setMessage(DUPLICATE_DESTINATION_ERROR, new Object[] { id, serviceId });
                throw e;
            }
            destinationSettings = new DestinationSettings(id);
            serviceSettings.addDestinationSettings(destinationSettings);
        } else {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_ID_IN_SERVICE, new Object[] { DESTINATION_ELEMENT, id, serviceId });
            throw ex;
        }
        NodeList properties = selectNodeList(dest, PROPERTIES_ELEMENT + "/*");
        if (properties.getLength() > 0) {
            ConfigMap map = properties(properties, getSourceFileOf(dest));
            destinationSettings.addProperties(map);
        }
        destinationChannels(dest, destinationSettings, serviceSettings);
    }

    private void destinationChannels(Node dest, DestinationSettings destinationSettings, ServiceSettings serviceSettings) {
        String destId = destinationSettings.getId();
        String channelsList = evaluateExpression(dest, "@" + CHANNELS_ATTR).toString().trim();
        if (channelsList.length() > 0) {
            StringTokenizer st = new StringTokenizer(channelsList, LIST_DELIMITERS);
            while (st.hasMoreTokens()) {
                String ref = st.nextToken().trim();
                ChannelSettings channel = config.getChannelSettings(ref);
                if (channel != null) {
                    destinationSettings.addChannelSettings(channel);
                } else {
                    ConfigurationException ex = new ConfigurationException();
                    ex.setMessage(REF_NOT_FOUND_IN_DEST, new Object[] { CHANNEL_ELEMENT, ref, destId });
                    throw ex;
                }
            }
        } else {
            Node channelsNode = selectSingleNode(dest, CHANNELS_ELEMENT);
            if (channelsNode != null) {
                allowedChildElements(channelsNode, DESTINATION_CHANNELS_CHILDREN);
                NodeList channels = selectNodeList(channelsNode, CHANNEL_ELEMENT);
                if (channels.getLength() > 0) {
                    for (int c = 0; c < channels.getLength(); c++) {
                        Node chan = channels.item(c);
                        requiredAttributesOrElements(chan, DESTINATION_CHANNEL_REQ_CHILDREN);
                        String ref = getAttributeOrChildElement(chan, REF_ATTR).trim();
                        if (ref.length() > 0) {
                            ChannelSettings channel = config.getChannelSettings(ref);
                            if (channel != null) {
                                destinationSettings.addChannelSettings(channel);
                            } else {
                                ConfigurationException ex = new ConfigurationException();
                                ex.setMessage(REF_NOT_FOUND_IN_DEST, new Object[] { CHANNEL_ELEMENT, ref, destId });
                                throw ex;
                            }
                        } else {
                            ConfigurationException ex = new ConfigurationException();
                            ex.setMessage(INVALID_REF_IN_DEST, new Object[] { CHANNEL_ELEMENT, ref, destId });
                            throw ex;
                        }
                    }
                }
            } else {
                List defaultChannels = serviceSettings.getDefaultChannels();
                Iterator it = defaultChannels.iterator();
                while (it.hasNext()) {
                    ChannelSettings channel = (ChannelSettings) it.next();
                    destinationSettings.addChannelSettings(channel);
                }
            }
        }
        if (destinationSettings.getChannelSettings().size() <= 0) {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(DEST_NEEDS_CHANNEL, new Object[] { destId });
            throw ex;
        }
    }

    private void flexClient(Node root) {
        Node flexClient = selectSingleNode(root, FLEX_CLIENT_ELEMENT);
        if (flexClient != null) {
            FlexClientSettings flexClientSettings = new FlexClientSettings();
            String reliableReconnectDurationMillis = getAttributeOrChildElement(flexClient, FLEX_CLIENT_RELIABLE_RECONNECT_DURATION_MILLIS);
            if (reliableReconnectDurationMillis.length() > 0) {
                try {
                    int millis = Integer.parseInt(reliableReconnectDurationMillis);
                    if (millis < 0) {
                        ConfigurationException e = new ConfigurationException();
                        e.setMessage(INVALID_FLEX_CLIENT_RELIABLE_RECONNECT_DURATION_MILLIS, new Object[] { reliableReconnectDurationMillis });
                        throw e;
                    }
                    flexClientSettings.setReliableReconnectDurationMillis(millis);
                } catch (NumberFormatException nfe) {
                    ConfigurationException e = new ConfigurationException();
                    e.setMessage(INVALID_FLEX_CLIENT_RELIABLE_RECONNECT_DURATION_MILLIS, new Object[] { reliableReconnectDurationMillis });
                    throw e;
                }
            } else {
                flexClientSettings.setReliableReconnectDurationMillis(0);
            }
            String heartbeatIntervalMillis = getAttributeOrChildElement(flexClient, FLEX_CLIENT_HEARTBEAT_INTERVAL_MILLIS);
            if (heartbeatIntervalMillis.length() > 0) {
                try {
                    int millis = Integer.parseInt(heartbeatIntervalMillis);
                    if (millis < 0) {
                        ConfigurationException e = new ConfigurationException();
                        e.setMessage(INVALID_FLEX_CLIENT_HEARTBEAT_INTERVAL_MILLIS, new Object[] { heartbeatIntervalMillis });
                        throw e;
                    }
                    flexClientSettings.setHeartbeatIntervalMillis(millis);
                } catch (NumberFormatException nfe) {
                    ConfigurationException e = new ConfigurationException();
                    e.setMessage(INVALID_FLEX_CLIENT_HEARTBEAT_INTERVAL_MILLIS, new Object[] { heartbeatIntervalMillis });
                    throw e;
                }
            }
            ((ClientConfiguration) config).setFlexClientSettings(flexClientSettings);
        }
    }
}

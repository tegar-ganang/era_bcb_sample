package flex.messaging.config;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import flex.messaging.config.ThrottleSettings.Policy;
import flex.messaging.util.LocaleUtils;

/**
 * Processes DOM representation of a messaging configuration file.
 * <p>
 * Note: Since reference ids are used between elements, certain
 * sections of the document need to be parsed first.
 * </p>
 *
 * @author Peter Farland
 * @exclude
 */
public abstract class ServerConfigurationParser extends AbstractConfigurationParser {

    /**
     * Used to verify that advanced messaging support has been registered if necessary.
     * If other configuration requires it, but it was not registered a ConfigurationException is thrown.
     */
    private boolean verifyAdvancedMessagingSupport = false;

    private boolean advancedMessagingSupportRegistered = false;

    @Override
    protected void parseTopLevelConfig(Document doc) {
        Node root = selectSingleNode(doc, "/" + SERVICES_CONFIG_ELEMENT);
        if (root != null) {
            allowedChildElements(root, SERVICES_CONFIG_CHILDREN);
            securitySection(root);
            serversSection(root);
            channelsSection(root);
            services(root);
            clusters(root);
            logging(root);
            system(root);
            flexClient(root);
            factories(root);
            messageFilters(root);
            validators(root);
            if (verifyAdvancedMessagingSupport && !advancedMessagingSupportRegistered) {
                ConfigurationException e = new ConfigurationException();
                e.setMessage(REQUIRE_ADVANCED_MESSAGING_SUPPORT);
                throw e;
            }
        } else {
            ConfigurationException e = new ConfigurationException();
            e.setMessage(INVALID_SERVICES_ROOT, new Object[] { SERVICES_CONFIG_ELEMENT });
            throw e;
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
                if (!isValidID(clusterName)) continue;
                String propsFileName = getAttributeOrChildElement(cluster, CLUSTER_PROPERTIES_ATTR);
                ClusterSettings clusterSettings = new ClusterSettings();
                clusterSettings.setClusterName(clusterName);
                clusterSettings.setPropsFileName(propsFileName);
                String className = getAttributeOrChildElement(cluster, CLASS_ATTR);
                if (className != null && className.length() > 0) clusterSettings.setImplementationClass(className);
                String defaultValue = getAttributeOrChildElement(cluster, ClusterSettings.DEFAULT_ELEMENT);
                if (defaultValue != null && defaultValue.length() > 0) {
                    if (defaultValue.equalsIgnoreCase(TRUE_STRING)) clusterSettings.setDefault(true); else if (!defaultValue.equalsIgnoreCase(FALSE_STRING)) {
                        ConfigurationException e = new ConfigurationException();
                        e.setMessage(10215, new Object[] { clusterName, defaultValue });
                        throw e;
                    }
                }
                String ulb = getAttributeOrChildElement(cluster, ClusterSettings.URL_LOAD_BALANCING);
                if (ulb != null && ulb.length() > 0) {
                    if (ulb.equalsIgnoreCase(FALSE_STRING)) {
                        clusterSettings.setURLLoadBalancing(false);
                    } else if (!ulb.equalsIgnoreCase(TRUE_STRING)) {
                        ConfigurationException e = new ConfigurationException();
                        e.setMessage(10216, new Object[] { clusterName, ulb });
                        throw e;
                    }
                }
                NodeList properties = selectNodeList(cluster, PROPERTIES_ELEMENT + "/*");
                if (properties.getLength() > 0) {
                    ConfigMap map = properties(properties, getSourceFileOf(cluster));
                    clusterSettings.addProperties(map);
                }
                ((MessagingConfiguration) config).addClusterSettings(clusterSettings);
            }
        }
    }

    private void securitySection(Node root) {
        Node security = selectSingleNode(root, SECURITY_ELEMENT);
        if (security != null) {
            allowedChildElements(security, SECURITY_CHILDREN);
            NodeList list = selectNodeList(security, SECURITY_CONSTRAINT_DEFINITION_ELEMENT);
            for (int i = 0; i < list.getLength(); i++) {
                Node constraint = list.item(i);
                securityConstraint(constraint, false);
            }
            list = selectNodeList(security, LOGIN_COMMAND_ELEMENT);
            for (int i = 0; i < list.getLength(); i++) {
                Node login = list.item(i);
                LoginCommandSettings loginCommandSettings = new LoginCommandSettings();
                requiredAttributesOrElements(login, LOGIN_COMMAND_REQ_CHILDREN);
                allowedAttributesOrElements(login, LOGIN_COMMAND_CHILDREN);
                String server = getAttributeOrChildElement(login, SERVER_ATTR);
                if (server.length() == 0) {
                    ConfigurationException e = new ConfigurationException();
                    e.setMessage(MISSING_ATTRIBUTE, new Object[] { SERVER_ATTR, LOGIN_COMMAND_ELEMENT });
                    throw e;
                }
                loginCommandSettings.setServer(server);
                String loginClass = getAttributeOrChildElement(login, CLASS_ATTR);
                if (loginClass.length() == 0) {
                    ConfigurationException e = new ConfigurationException();
                    e.setMessage(MISSING_ATTRIBUTE, new Object[] { CLASS_ATTR, LOGIN_COMMAND_ELEMENT });
                    throw e;
                }
                loginCommandSettings.setClassName(loginClass);
                boolean isPerClientAuth = Boolean.valueOf(getAttributeOrChildElement(login, PER_CLIENT_AUTH));
                loginCommandSettings.setPerClientAuthentication(isPerClientAuth);
                ((MessagingConfiguration) config).getSecuritySettings().addLoginCommandSettings(loginCommandSettings);
            }
        }
    }

    private SecurityConstraint securityConstraint(Node constraint, boolean inline) {
        SecurityConstraint sc;
        allowedAttributesOrElements(constraint, SECURITY_CONSTRAINT_DEFINITION_CHILDREN);
        String ref = getAttributeOrChildElement(constraint, REF_ATTR);
        if (ref.length() > 0) {
            allowedAttributesOrElements(constraint, new String[] { REF_ATTR });
            sc = ((MessagingConfiguration) config).getSecuritySettings().getConstraint(ref);
            if (sc == null) {
                ConfigurationException e = new ConfigurationException();
                e.setMessage(REF_NOT_FOUND, new Object[] { SECURITY_CONSTRAINT_DEFINITION_ELEMENT, ref });
                throw e;
            }
        } else {
            String id = getAttributeOrChildElement(constraint, ID_ATTR);
            if (inline) {
                sc = new SecurityConstraint("");
            } else if (isValidID(id)) {
                sc = new SecurityConstraint(id);
                ((MessagingConfiguration) config).getSecuritySettings().addConstraint(sc);
            } else {
                ConfigurationException ex = new ConfigurationException();
                ex.setMessage(INVALID_ID, new Object[] { SECURITY_CONSTRAINT_DEFINITION_ELEMENT, id });
                ex.setDetails(INVALID_ID);
                throw ex;
            }
            String method = getAttributeOrChildElement(constraint, AUTH_METHOD_ELEMENT);
            sc.setMethod(method);
            Node rolesNode = selectSingleNode(constraint, ROLES_ELEMENT);
            if (rolesNode != null) {
                allowedChildElements(rolesNode, ROLES_CHILDREN);
                NodeList roles = selectNodeList(rolesNode, ROLE_ELEMENT);
                for (int r = 0; r < roles.getLength(); r++) {
                    Node roleNode = roles.item(r);
                    String role = evaluateExpression(roleNode, ".").toString().trim();
                    if (role.length() > 0) {
                        sc.addRole(role);
                    }
                }
            }
        }
        return sc;
    }

    private void serversSection(Node root) {
        if (!(config instanceof MessagingConfiguration)) return;
        Node serversNode = selectSingleNode(root, SERVERS_ELEMENT);
        if (serversNode != null) {
            allowedAttributesOrElements(serversNode, SERVERS_CHILDREN);
            NodeList servers = selectNodeList(serversNode, SERVER_ELEMENT);
            for (int i = 0; i < servers.getLength(); i++) {
                Node server = servers.item(i);
                serverDefinition(server);
            }
        }
    }

    private void serverDefinition(Node server) {
        requiredAttributesOrElements(server, SERVER_REQ_CHILDREN);
        allowedAttributesOrElements(server, SERVER_CHILDREN);
        String id = getAttributeOrChildElement(server, ID_ATTR);
        if (isValidID(id)) {
            SharedServerSettings settings = new SharedServerSettings();
            settings.setId(id);
            settings.setSourceFile(getSourceFileOf(server));
            String className = getAttributeOrChildElement(server, CLASS_ATTR);
            if (className.length() > 0) {
                settings.setClassName(className);
                NodeList properties = selectNodeList(server, PROPERTIES_ELEMENT + "/*");
                if (properties.getLength() > 0) {
                    ConfigMap map = properties(properties, getSourceFileOf(server));
                    settings.addProperties(map);
                }
                ((MessagingConfiguration) config).addSharedServerSettings(settings);
            } else {
                ConfigurationException ex = new ConfigurationException();
                ex.setMessage(CLASS_NOT_SPECIFIED, new Object[] { SERVER_ELEMENT, id });
                throw ex;
            }
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
        String id = getAttributeOrChildElement(channel, ID_ATTR);
        if (isValidID(id)) {
            if (config.getChannelSettings(id) != null) {
                ConfigurationException e = new ConfigurationException();
                e.setMessage(DUPLICATE_CHANNEL_ERROR, new Object[] { id });
                throw e;
            }
            ChannelSettings channelSettings = new ChannelSettings(id);
            channelSettings.setSourceFile(getSourceFileOf(channel));
            String clientType = getAttributeOrChildElement(channel, CLASS_ATTR);
            clientType = clientType.length() > 0 ? clientType : null;
            String serverOnlyString = getAttributeOrChildElement(channel, SERVER_ONLY_ATTR);
            boolean serverOnly = serverOnlyString.length() > 0 ? Boolean.valueOf(serverOnlyString) : false;
            if (clientType == null && !serverOnly) {
                ConfigurationException ce = new ConfigurationException();
                ce.setMessage(CLASS_OR_SERVER_ONLY_ERROR, new Object[] { id });
                throw ce;
            } else if (clientType != null && serverOnly) {
                ConfigurationException ce = new ConfigurationException();
                ce.setMessage(CLASS_AND_SERVER_ONLY_ERROR, new Object[] { id });
                throw ce;
            } else {
                if (serverOnly) channelSettings.setServerOnly(true); else channelSettings.setClientType(clientType);
            }
            String remote = getAttributeOrChildElement(channel, REMOTE_ATTR);
            channelSettings.setRemote(Boolean.valueOf(remote));
            Node endpoint = selectSingleNode(channel, ENDPOINT_ELEMENT);
            if (endpoint != null) {
                allowedAttributesOrElements(endpoint, ENDPOINT_CHILDREN);
                String type = getAttributeOrChildElement(endpoint, CLASS_ATTR);
                channelSettings.setEndpointType(type);
                String uri = getAttributeOrChildElement(endpoint, URL_ATTR);
                if (uri == null || EMPTY_STRING.equals(uri)) uri = getAttributeOrChildElement(endpoint, URI_ATTR);
                channelSettings.setUri(uri);
                config.addChannelSettings(id, channelSettings);
            }
            Node server = selectSingleNode(channel, SERVER_ELEMENT);
            if (server != null) {
                requiredAttributesOrElements(server, CHANNEL_DEFINITION_SERVER_REQ_CHILDREN);
                String serverId = getAttributeOrChildElement(server, REF_ATTR);
                channelSettings.setServerId(serverId);
            }
            NodeList properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/*");
            if (properties.getLength() > 0) {
                ConfigMap map = properties(properties, getSourceFileOf(channel));
                channelSettings.addProperties(map);
                if (!verifyAdvancedMessagingSupport) {
                    ConfigMap outboundQueueProcessor = map.getPropertyAsMap(FLEX_CLIENT_OUTBOUND_QUEUE_PROCESSOR_ELEMENT, null);
                    if (outboundQueueProcessor != null) {
                        ConfigMap queueProcessorProperties = outboundQueueProcessor.getPropertyAsMap(PROPERTIES_ELEMENT, null);
                        if (queueProcessorProperties != null) {
                            boolean adaptiveFrequency = queueProcessorProperties.getPropertyAsBoolean(ADAPTIVE_FREQUENCY, false);
                            if (adaptiveFrequency) verifyAdvancedMessagingSupport = true;
                        }
                    }
                }
            }
            String ref = evaluateExpression(channel, "@" + SECURITY_CONSTRAINT_ATTR).toString().trim();
            if (ref.length() > 0) {
                SecurityConstraint sc = ((MessagingConfiguration) config).getSecuritySettings().getConstraint(ref);
                if (sc != null) {
                    channelSettings.setConstraint(sc);
                } else {
                    ConfigurationException ex = new ConfigurationException();
                    ex.setMessage(REF_NOT_FOUND_IN_CHANNEL, new Object[] { SECURITY_CONSTRAINT_ATTR, ref, id });
                    throw ex;
                }
            } else {
                Node security = selectSingleNode(channel, SECURITY_ELEMENT);
                if (security != null) {
                    allowedChildElements(security, EMBEDDED_SECURITY_CHILDREN);
                    Node constraint = selectSingleNode(security, SECURITY_CONSTRAINT_ELEMENT);
                    if (constraint != null) {
                        SecurityConstraint sc = securityConstraint(constraint, true);
                        channelSettings.setConstraint(sc);
                    }
                }
            }
        } else {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_ID, new Object[] { CHANNEL_DEFINITION_ELEMENT, id });
            ex.setDetails(INVALID_ID);
            throw ex;
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

    private void serviceInclude(Node serviceInclude) {
        requiredAttributesOrElements(serviceInclude, SERVICE_INCLUDE_CHILDREN);
        String src = getAttributeOrChildElement(serviceInclude, SRC_ATTR);
        if (src.length() > 0) {
            Document doc = loadDocument(src, fileResolver.getIncludedFile(src));
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
                serviceSettings.setSourceFile(getSourceFileOf(service));
                config.addServiceSettings(serviceSettings);
            } else {
                ConfigurationException e = new ConfigurationException();
                e.setMessage(DUPLICATE_SERVICE_ERROR, new Object[] { id });
                throw e;
            }
            String className = getAttributeOrChildElement(service, CLASS_ATTR);
            if (className.length() > 0) {
                serviceSettings.setClassName(className);
                if (className.equals("flex.messaging.services.AdvancedMessagingSupport")) advancedMessagingSupportRegistered = true;
            } else {
                ConfigurationException ex = new ConfigurationException();
                ex.setMessage(CLASS_NOT_SPECIFIED, new Object[] { SERVICE_ELEMENT, id });
                throw ex;
            }
            NodeList properties = selectNodeList(service, PROPERTIES_ELEMENT + "/*");
            if (properties.getLength() > 0) {
                ConfigMap map = properties(properties, getSourceFileOf(service));
                serviceSettings.addProperties(map);
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
            Node defaultSecurityConstraint = selectSingleNode(service, DEFAULT_SECURITY_CONSTRAINT_ELEMENT);
            if (defaultSecurityConstraint != null) {
                requiredAttributesOrElements(defaultSecurityConstraint, new String[] { REF_ATTR });
                allowedAttributesOrElements(defaultSecurityConstraint, new String[] { REF_ATTR });
                String ref = getAttributeOrChildElement(defaultSecurityConstraint, REF_ATTR);
                if (ref.length() > 0) {
                    SecurityConstraint sc = ((MessagingConfiguration) config).getSecuritySettings().getConstraint(ref);
                    if (sc == null) {
                        ConfigurationException e = new ConfigurationException();
                        e.setMessage(REF_NOT_FOUND, new Object[] { SECURITY_CONSTRAINT_DEFINITION_ELEMENT, ref });
                        throw e;
                    }
                    serviceSettings.setConstraint(sc);
                } else {
                    ConfigurationException ex = new ConfigurationException();
                    ex.setMessage(INVALID_SECURITY_CONSTRAINT_REF, new Object[] { ref, id });
                    throw ex;
                }
            }
            Node adapters = selectSingleNode(service, ADAPTERS_ELEMENT);
            if (adapters != null) {
                allowedChildElements(adapters, ADAPTERS_CHILDREN);
                NodeList serverAdapters = selectNodeList(adapters, ADAPTER_DEFINITION_ELEMENT);
                for (int a = 0; a < serverAdapters.getLength(); a++) {
                    Node adapter = serverAdapters.item(a);
                    adapterDefinition(adapter, serviceSettings);
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
     * A Flex application can declare default channels for its services. If a
     * service specifies its own list of channels it overrides these defaults.
     * <p>
     * &lt;default-channels&gt;<br/>
     * &lt;channel ref="channel-id"/&gt;<br/>
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
     * &lt;default-channels&gt;<br/>
     * &lt;channel ref="channel-id"/&gt;<br/>
     * &lt;default-channels&gt;
     * </p>
     */
    private void defaultChannel(Node chan, ServiceSettings serviceSettings) {
        String ref = getAttributeOrChildElement(chan, REF_ATTR);
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

    private void adapterDefinition(Node adapter, ServiceSettings serviceSettings) {
        requiredAttributesOrElements(adapter, ADAPTER_DEFINITION_REQ_CHILDREN);
        allowedChildElements(adapter, ADAPTER_DEFINITION_CHILDREN);
        String serviceId = serviceSettings.getId();
        String id = getAttributeOrChildElement(adapter, ID_ATTR);
        if (isValidID(id)) {
            AdapterSettings adapterSettings = new AdapterSettings(id);
            adapterSettings.setSourceFile(getSourceFileOf(adapter));
            String className = getAttributeOrChildElement(adapter, CLASS_ATTR);
            if (className.length() > 0) {
                adapterSettings.setClassName(className);
                boolean isDefault = Boolean.valueOf(getAttributeOrChildElement(adapter, DEFAULT_ATTR));
                if (isDefault) {
                    adapterSettings.setDefault(isDefault);
                    AdapterSettings defaultAdapter;
                    defaultAdapter = serviceSettings.getDefaultAdapter();
                    if (defaultAdapter != null) {
                        ConfigurationException ex = new ConfigurationException();
                        ex.setMessage(DUPLICATE_DEFAULT_ADAPTER, new Object[] { id, serviceId, defaultAdapter.getId() });
                        throw ex;
                    }
                }
                serviceSettings.addAdapterSettings(adapterSettings);
                NodeList properties = selectNodeList(adapter, PROPERTIES_ELEMENT + "/*");
                if (properties.getLength() > 0) {
                    ConfigMap map = properties(properties, getSourceFileOf(adapter));
                    adapterSettings.addProperties(map);
                }
            } else {
                ConfigurationException ex = new ConfigurationException();
                ex.setMessage(CLASS_NOT_SPECIFIED, new Object[] { ADAPTER_DEFINITION_ELEMENT, id });
                throw ex;
            }
        } else {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_ID_IN_SERVICE, new Object[] { ADAPTER_DEFINITION_ELEMENT, id, serviceId });
            throw ex;
        }
    }

    private void destinationInclude(Node destInclude, ServiceSettings serviceSettings) {
        requiredAttributesOrElements(destInclude, DESTINATION_INCLUDE_CHILDREN);
        String src = getAttributeOrChildElement(destInclude, SRC_ATTR);
        if (src.length() > 0) {
            Document doc = loadDocument(src, fileResolver.getIncludedFile(src));
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
            destinationSettings.setSourceFile(getSourceFileOf(dest));
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
            if (!verifyAdvancedMessagingSupport) {
                ConfigMap networkSettings = map.getPropertyAsMap(NetworkSettings.NETWORK_ELEMENT, null);
                if (networkSettings != null) {
                    String reliable = networkSettings.getPropertyAsString(NetworkSettings.RELIABLE_ELEMENT, null);
                    if (reliable != null && Boolean.valueOf(reliable)) {
                        verifyAdvancedMessagingSupport = true;
                    } else {
                        ConfigMap inbound = networkSettings.getPropertyAsMap(ThrottleSettings.ELEMENT_INBOUND, null);
                        if (inbound != null) {
                            String policy = inbound.getPropertyAsString(ThrottleSettings.ELEMENT_POLICY, null);
                            if (policy != null && (Policy.BUFFER.toString().equalsIgnoreCase(policy) || Policy.CONFLATE.toString().equalsIgnoreCase(policy))) verifyAdvancedMessagingSupport = true;
                        }
                        if (!verifyAdvancedMessagingSupport) {
                            ConfigMap outbound = networkSettings.getPropertyAsMap(ThrottleSettings.ELEMENT_OUTBOUND, null);
                            if (outbound != null) {
                                String policy = outbound.getPropertyAsString(ThrottleSettings.ELEMENT_POLICY, null);
                                if (policy != null && (Policy.BUFFER.toString().equalsIgnoreCase(policy) || Policy.CONFLATE.toString().equalsIgnoreCase(policy))) verifyAdvancedMessagingSupport = true;
                            }
                        }
                    }
                }
            }
        }
        destinationChannels(dest, destinationSettings, serviceSettings);
        destinationSecurity(dest, destinationSettings, serviceSettings);
        destinationAdapter(dest, destinationSettings, serviceSettings);
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
                for (int c = 0; c < channels.getLength(); c++) {
                    Node chan = channels.item(c);
                    requiredAttributesOrElements(chan, DESTINATION_CHANNEL_REQ_CHILDREN);
                    String ref = getAttributeOrChildElement(chan, REF_ATTR);
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

    private void destinationSecurity(Node dest, DestinationSettings destinationSettings, ServiceSettings serviceSettings) {
        String destId = destinationSettings.getId();
        String ref = evaluateExpression(dest, "@" + SECURITY_CONSTRAINT_ATTR).toString().trim();
        if (ref.length() > 0) {
            SecurityConstraint sc = ((MessagingConfiguration) config).getSecuritySettings().getConstraint(ref);
            if (sc != null) {
                destinationSettings.setConstraint(sc);
            } else {
                ConfigurationException ex = new ConfigurationException();
                ex.setMessage(REF_NOT_FOUND_IN_DEST, new Object[] { SECURITY_CONSTRAINT_ATTR, ref, destId });
                throw ex;
            }
        } else {
            Node security = selectSingleNode(dest, SECURITY_ELEMENT);
            if (security != null) {
                allowedChildElements(security, EMBEDDED_SECURITY_CHILDREN);
                Node constraint = selectSingleNode(security, SECURITY_CONSTRAINT_ELEMENT);
                if (constraint != null) {
                    SecurityConstraint sc = securityConstraint(constraint, true);
                    destinationSettings.setConstraint(sc);
                }
            } else {
                SecurityConstraint sc = serviceSettings.getConstraint();
                if (sc != null) {
                    destinationSettings.setConstraint(sc);
                }
            }
        }
    }

    private void destinationAdapter(Node dest, DestinationSettings destinationSettings, ServiceSettings serviceSettings) {
        String destId = destinationSettings.getId();
        String ref = evaluateExpression(dest, "@" + ADAPTER_ATTR).toString().trim();
        if (ref.length() > 0) {
            adapterReference(ref, destinationSettings, serviceSettings);
        } else {
            Node adapter = selectSingleNode(dest, ADAPTER_ELEMENT);
            if (adapter != null) {
                allowedAttributesOrElements(adapter, DESTINATION_ADAPTER_CHILDREN);
                ref = getAttributeOrChildElement(adapter, REF_ATTR);
                adapterReference(ref, destinationSettings, serviceSettings);
            } else {
                AdapterSettings adapterSettings = serviceSettings.getDefaultAdapter();
                if (adapterSettings != null) {
                    destinationSettings.setAdapterSettings(adapterSettings);
                }
            }
        }
        if (destinationSettings.getAdapterSettings() == null) {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(DEST_NEEDS_ADAPTER, new Object[] { destId });
            throw ex;
        }
    }

    private void adapterReference(String ref, DestinationSettings destinationSettings, ServiceSettings serviceSettings) {
        String destId = destinationSettings.getId();
        if (ref.length() > 0) {
            AdapterSettings adapterSettings = serviceSettings.getAdapterSettings(ref);
            if (adapterSettings != null) {
                destinationSettings.setAdapterSettings(adapterSettings);
            } else {
                ConfigurationException ex = new ConfigurationException();
                ex.setMessage(REF_NOT_FOUND_IN_DEST, new Object[] { ADAPTER_ELEMENT, ref, destId });
                throw ex;
            }
        } else {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_REF_IN_DEST, new Object[] { ADAPTER_ELEMENT, ref, destId });
            throw ex;
        }
    }

    private void logging(Node root) {
        Node logging = selectSingleNode(root, LOGGING_ELEMENT);
        if (logging != null) {
            allowedAttributesOrElements(logging, LOGGING_CHILDREN);
            LoggingSettings settings = new LoggingSettings();
            NodeList properties = selectNodeList(logging, PROPERTIES_ELEMENT + "/*");
            if (properties.getLength() > 0) {
                ConfigMap map = properties(properties, getSourceFileOf(logging));
                settings.addProperties(map);
            }
            NodeList targets = selectNodeList(logging, TARGET_ELEMENT);
            for (int i = 0; i < targets.getLength(); i++) {
                Node targetNode = targets.item(i);
                requiredAttributesOrElements(targetNode, TARGET_REQ_CHILDREN);
                allowedAttributesOrElements(targetNode, TARGET_CHILDREN);
                String className = getAttributeOrChildElement(targetNode, CLASS_ATTR);
                if (className.length() > 0) {
                    TargetSettings targetSettings = new TargetSettings(className);
                    String targetLevel = getAttributeOrChildElement(targetNode, LEVEL_ATTR);
                    if (targetLevel.length() > 0) targetSettings.setLevel(targetLevel);
                    Node filtersNode = selectSingleNode(targetNode, FILTERS_ELEMENT);
                    if (filtersNode != null) {
                        allowedChildElements(filtersNode, FILTERS_CHILDREN);
                        NodeList filters = selectNodeList(filtersNode, PATTERN_ELEMENT);
                        for (int f = 0; f < filters.getLength(); f++) {
                            Node pattern = filters.item(f);
                            String filter = evaluateExpression(pattern, ".").toString().trim();
                            targetSettings.addFilter(filter);
                        }
                    }
                    properties = selectNodeList(targetNode, PROPERTIES_ELEMENT + "/*");
                    if (properties.getLength() > 0) {
                        ConfigMap map = properties(properties, getSourceFileOf(targetNode));
                        targetSettings.addProperties(map);
                    }
                    settings.addTarget(targetSettings);
                }
            }
            config.setLoggingSettings(settings);
        }
    }

    private void system(Node root) {
        Node system = selectSingleNode(root, SYSTEM_ELEMENT);
        if (system != null) {
            allowedAttributesOrElements(system, SYSTEM_CHILDREN);
            SystemSettings settings = new SystemSettings();
            settings.setEnforceEndpointValidation(getAttributeOrChildElement(system, ENFORCE_ENDOINT_VALIDATION));
            Node localeNode = selectSingleNode(system, LOCALE_ELEMENT);
            if (localeNode != null) {
                allowedAttributesOrElements(localeNode, LOCALE_CHILDREN);
                String defaultLocaleString = getAttributeOrChildElement(localeNode, DEFAULT_LOCALE_ELEMENT);
                Locale defaultLocale;
                if (defaultLocaleString.length() > 0) defaultLocale = LocaleUtils.buildLocale(defaultLocaleString); else defaultLocale = LocaleUtils.buildLocale(null);
                settings.setDefaultLocale(defaultLocale);
            }
            settings.setManageable(getAttributeOrChildElement(system, MANAGEABLE_ELEMENT));
            Node redeployNode = selectSingleNode(system, REDEPLOY_ELEMENT);
            if (redeployNode != null) {
                allowedAttributesOrElements(redeployNode, REDEPLOY_CHILDREN);
                String enabled = getAttributeOrChildElement(redeployNode, ENABLED_ELEMENT);
                settings.setRedeployEnabled(enabled);
                String interval = getAttributeOrChildElement(redeployNode, WATCH_INTERVAL_ELEMENT);
                if (interval.length() > 0) {
                    settings.setWatchInterval(interval);
                }
                NodeList watches = selectNodeList(redeployNode, WATCH_FILE_ELEMENT);
                for (int i = 0; i < watches.getLength(); i++) {
                    Node watchNode = watches.item(i);
                    String watch = evaluateExpression(watchNode, ".").toString().trim();
                    if (watch.length() > 0) {
                        settings.addWatchFile(watch);
                    }
                }
                NodeList touches = selectNodeList(redeployNode, TOUCH_FILE_ELEMENT);
                for (int i = 0; i < touches.getLength(); i++) {
                    Node touchNode = touches.item(i);
                    String touch = evaluateExpression(touchNode, ".").toString().trim();
                    if (touch.length() > 0) {
                        settings.addTouchFile(touch);
                    }
                }
            }
            ((MessagingConfiguration) config).setSystemSettings(settings);
        } else {
            ((MessagingConfiguration) config).setSystemSettings(new SystemSettings());
        }
    }

    private void flexClient(Node root) {
        Node flexClient = selectSingleNode(root, FLEX_CLIENT_ELEMENT);
        if (flexClient != null) {
            allowedChildElements(flexClient, FLEX_CLIENT_CHILDREN);
            FlexClientSettings flexClientSettings = new FlexClientSettings();
            String timeout = getAttributeOrChildElement(flexClient, FLEX_CLIENT_TIMEOUT_MINUTES_ELEMENT);
            if (timeout.length() > 0) {
                try {
                    long timeoutMinutes = Long.parseLong(timeout);
                    if (timeoutMinutes < 0) {
                        ConfigurationException e = new ConfigurationException();
                        e.setMessage(INVALID_FLEX_CLIENT_TIMEOUT, new Object[] { timeout });
                        throw e;
                    }
                    flexClientSettings.setTimeoutMinutes(timeoutMinutes);
                } catch (NumberFormatException nfe) {
                    ConfigurationException e = new ConfigurationException();
                    e.setMessage(INVALID_FLEX_CLIENT_TIMEOUT, new Object[] { timeout });
                    throw e;
                }
            } else {
                flexClientSettings.setTimeoutMinutes(0);
            }
            Node outboundQueueProcessor = selectSingleNode(flexClient, FLEX_CLIENT_OUTBOUND_QUEUE_PROCESSOR_ELEMENT);
            if (outboundQueueProcessor != null) {
                requiredAttributesOrElements(outboundQueueProcessor, FLEX_CLIENT_OUTBOUND_QUEUE_PROCESSOR_REQ_CHILDREN);
                String outboundQueueProcessClass = getAttributeOrChildElement(outboundQueueProcessor, CLASS_ATTR);
                if (outboundQueueProcessClass.length() > 0) {
                    flexClientSettings.setFlexClientOutboundQueueProcessorClassName(outboundQueueProcessClass);
                } else {
                    ConfigurationException ex = new ConfigurationException();
                    ex.setMessage(CLASS_NOT_SPECIFIED, new Object[] { FLEX_CLIENT_OUTBOUND_QUEUE_PROCESSOR_ELEMENT, "" });
                    throw ex;
                }
                NodeList properties = selectNodeList(outboundQueueProcessor, PROPERTIES_ELEMENT + "/*");
                if (properties.getLength() > 0) {
                    ConfigMap map = properties(properties, getSourceFileOf(outboundQueueProcessor));
                    flexClientSettings.setFlexClientOutboundQueueProcessorProperties(map);
                    boolean adaptiveFrequency = map.getPropertyAsBoolean(ADAPTIVE_FREQUENCY, false);
                    if (adaptiveFrequency) verifyAdvancedMessagingSupport = true;
                }
            }
            ((MessagingConfiguration) config).setFlexClientSettings(flexClientSettings);
        }
    }

    private void factories(Node root) {
        Node factories = selectSingleNode(root, FACTORIES_ELEMENT);
        if (factories != null) {
            allowedAttributesOrElements(factories, FACTORIES_CHILDREN);
            NodeList factoryList = selectNodeList(factories, FACTORY_ELEMENT);
            for (int i = 0; i < factoryList.getLength(); i++) {
                Node factory = factoryList.item(i);
                factory(factory);
            }
        }
    }

    private void factory(Node factory) {
        requiredAttributesOrElements(factory, FACTORY_REQ_CHILDREN);
        String id = getAttributeOrChildElement(factory, ID_ATTR);
        String className = getAttributeOrChildElement(factory, CLASS_ATTR);
        if (isValidID(id)) {
            FactorySettings factorySettings = new FactorySettings(id, className);
            NodeList properties = selectNodeList(factory, PROPERTIES_ELEMENT + "/*");
            if (properties.getLength() > 0) {
                ConfigMap map = properties(properties, getSourceFileOf(factory));
                factorySettings.addProperties(map);
            }
            ((MessagingConfiguration) config).addFactorySettings(id, factorySettings);
        } else {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_ID, new Object[] { FACTORY_ELEMENT, id });
            ex.setDetails(INVALID_ID);
            throw ex;
        }
    }

    private void messageFilters(Node root) {
        typedMessageFilters(root, ASYNC_MESSAGE_FILTERS_ELEMENT, ASYNC_MESSAGE_FILTERS_ELEMENT_CHILDREN);
        typedMessageFilters(root, SYNC_MESSAGE_FILTERS_ELEMENT, SYNC_MESSAGE_FILTERS_ELEMENT_CHILDREN);
    }

    private void typedMessageFilters(Node root, String filterTypeElement, String[] childrenElements) {
        Node messageFiltersNode = selectSingleNode(root, filterTypeElement);
        if (messageFiltersNode == null) return;
        allowedChildElements(messageFiltersNode, childrenElements);
        NodeList messageFilters = selectNodeList(messageFiltersNode, FILTER_ELEMENT);
        for (int i = 0; i < messageFilters.getLength(); i++) {
            Node messageFilter = messageFilters.item(i);
            messageFilter(messageFilter, filterTypeElement);
        }
    }

    private void messageFilter(Node messageFilter, String filterType) {
        requiredAttributesOrElements(messageFilter, FILTER_REQ_CHILDREN);
        allowedAttributesOrElements(messageFilter, FILTER_CHILDREN);
        String id = getAttributeOrChildElement(messageFilter, ID_ATTR);
        if (isValidID(id)) {
            String className = getAttributeOrChildElement(messageFilter, CLASS_ATTR);
            if (className.length() > 0) {
                MessageFilterSettings messageFilterSettings = new MessageFilterSettings();
                messageFilterSettings.setId(id);
                messageFilterSettings.setClassName(className);
                MessageFilterSettings.FilterType type = filterType.equals(ASYNC_MESSAGE_FILTERS_ELEMENT) ? MessageFilterSettings.FilterType.ASYNC : MessageFilterSettings.FilterType.SYNC;
                messageFilterSettings.setFilterType(type);
                NodeList properties = selectNodeList(messageFilter, PROPERTIES_ELEMENT + "/*");
                if (properties.getLength() > 0) {
                    ConfigMap map = properties(properties, getSourceFileOf(messageFilter));
                    messageFilterSettings.addProperties(map);
                }
                ((MessagingConfiguration) config).addMessageFilterSettings(messageFilterSettings);
            } else {
                ConfigurationException ex = new ConfigurationException();
                ex.setMessage(CLASS_NOT_SPECIFIED, new Object[] { FILTER_ELEMENT, id });
                throw ex;
            }
        }
    }

    private void validators(Node root) {
        Node validatorsNode = selectSingleNode(root, VALIDATORS_ELEMENT);
        if (validatorsNode == null) return;
        allowedChildElements(validatorsNode, VALIDATORS_CHILDREN);
        NodeList validators = selectNodeList(validatorsNode, VALIDATOR_ELEMENT);
        for (int i = 0; i < validators.getLength(); i++) {
            Node validator = validators.item(i);
            validator(validator);
        }
    }

    private void validator(Node validator) {
        requiredAttributesOrElements(validator, VALIDATOR_REQ_CHILDREN);
        allowedAttributesOrElements(validator, VALIDATOR_CHILDREN);
        ValidatorSettings validatorSettings = new ValidatorSettings();
        String className = getAttributeOrChildElement(validator, CLASS_ATTR);
        if (className.length() > 0) {
            validatorSettings.setClassName(className);
        } else {
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(CLASS_NOT_SPECIFIED, new Object[] { VALIDATOR_ELEMENT, "" });
            throw ex;
        }
        String type = getAttributeOrChildElement(validator, TYPE_ATTR);
        if (type.length() > 0) validatorSettings.setType(type);
        NodeList properties = selectNodeList(validator, PROPERTIES_ELEMENT + "/*");
        if (properties.getLength() > 0) {
            ConfigMap map = properties(properties, getSourceFileOf(validator));
            validatorSettings.addProperties(map);
        }
        ((MessagingConfiguration) config).addValidatorSettings(validatorSettings);
    }
}

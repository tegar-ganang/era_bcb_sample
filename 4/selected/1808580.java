package flex.messaging.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import flex.management.ManageableComponent;
import flex.management.runtime.messaging.endpoints.EndpointControl;
import flex.messaging.Destination;
import flex.messaging.FlexComponent;
import flex.messaging.FlexContext;
import flex.messaging.MessageBroker;
import flex.messaging.Server;
import flex.messaging.client.FlexClientManager;
import flex.messaging.cluster.ClusterManager;
import flex.messaging.endpoints.AbstractEndpoint;
import flex.messaging.endpoints.Endpoint;
import flex.messaging.endpoints.Endpoint2;
import flex.messaging.log.Log;
import flex.messaging.log.Target;
import flex.messaging.security.LoginCommand;
import flex.messaging.security.LoginManager;
import flex.messaging.services.AuthenticationService;
import flex.messaging.services.Service;
import flex.messaging.services.ServiceAdapter;
import flex.messaging.util.ClassUtil;
import flex.messaging.util.RedeployManager;
import flex.messaging.util.StringUtils;
import flex.messaging.util.ToStringPrettyPrinter;
import flex.messaging.validators.DeserializationValidator;

/**
 * This object encapsulates settings for a MessageBroker instance.
 * The MessageBroker itself has no knowledge of configuration specifics;
 * instead, this object sets the relevant values on the broker using
 * information which a ConfigurationParser has provided for it.
 *
 * @author Peter Farland
 * @author neville
 * @exclude
 */
public class MessagingConfiguration implements ServicesConfiguration {

    private final String asyncMessageBrokerType = "flex.messaging.AsyncMessageBroker";

    private final String asyncFlexClientManagerType = "flex.messaging.client.AsyncFlexClientManager";

    private final Map<String, ChannelSettings> channelSettings;

    private final List<String> defaultChannels;

    private final SecuritySettings securitySettings;

    private final List<ServiceSettings> serviceSettings;

    private final List<SharedServerSettings> sharedServerSettings;

    private LoggingSettings loggingSettings;

    private SystemSettings systemSettings;

    private FlexClientSettings flexClientSettings;

    private final Map<String, ClusterSettings> clusterSettings;

    private final Map<String, FactorySettings> factorySettings;

    private final List<MessageFilterSettings> messageFilterSettings;

    private final Map<String, ValidatorSettings> validatorSettings;

    public MessagingConfiguration() {
        channelSettings = new HashMap<String, ChannelSettings>();
        defaultChannels = new ArrayList<String>(4);
        clusterSettings = new HashMap<String, ClusterSettings>();
        factorySettings = new HashMap<String, FactorySettings>();
        serviceSettings = new ArrayList<ServiceSettings>();
        sharedServerSettings = new ArrayList<SharedServerSettings>();
        securitySettings = new SecuritySettings();
        messageFilterSettings = new ArrayList<MessageFilterSettings>();
        validatorSettings = new HashMap<String, ValidatorSettings>();
    }

    public void configureBroker(MessageBroker broker) {
        boolean async = (broker.getClass().getName().equals(asyncMessageBrokerType));
        broker.setChannelSettings(channelSettings);
        broker.setSecuritySettings(securitySettings);
        broker.setSystemSettings(systemSettings);
        broker.setFlexClientSettings(flexClientSettings);
        createAuthorizationManager(broker);
        createFlexClientManager(broker);
        createRedeployManager(broker);
        createFactories(broker);
        if (async) createSharedServers(broker);
        createEndpoints(broker);
        broker.setDefaultChannels(defaultChannels);
        prepareClusters(broker);
        createServices(broker);
        if (async) createMessageFilters(broker);
        createValidators(broker);
    }

    public MessageBroker createBroker(String id, ClassLoader loader) {
        MessageBroker broker;
        try {
            @SuppressWarnings("unchecked") Class<? extends MessageBroker> messageBrokerClass = ClassUtil.createClass(asyncMessageBrokerType, loader);
            Constructor constructor = messageBrokerClass.getConstructor(boolean.class);
            broker = (MessageBroker) constructor.newInstance(systemSettings.isManageable());
        } catch (Throwable t) {
            broker = new MessageBroker(systemSettings.isManageable());
        }
        broker.setEnforceEndpointValidation(systemSettings.isEnforceEndpointValidation());
        broker.setId(id);
        broker.setClassLoader(loader);
        return broker;
    }

    private void createFactories(MessageBroker broker) {
        for (Iterator<Map.Entry<String, FactorySettings>> iter = factorySettings.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, FactorySettings> entry = iter.next();
            String id = entry.getKey();
            FactorySettings factorySetting = entry.getValue();
            broker.addFactory(id, factorySetting.createFactory());
        }
    }

    private void createFlexClientManager(MessageBroker broker) {
        FlexClientManager flexClientManager = null;
        try {
            @SuppressWarnings("unchecked") Class<? extends FlexClientManager> flexClientManagerClass = ClassUtil.createClass(asyncFlexClientManagerType, broker.getClassLoader());
            Constructor ctor = flexClientManagerClass.getConstructor(broker.getClass());
            flexClientManager = (FlexClientManager) ctor.newInstance(broker);
        } catch (Throwable t) {
            flexClientManager = new FlexClientManager(broker.isManaged(), broker);
        }
        broker.setFlexClientManager(flexClientManager);
    }

    private void createRedeployManager(MessageBroker broker) {
        RedeployManager redeployManager = new RedeployManager();
        redeployManager.setEnabled(systemSettings.getRedeployEnabled());
        redeployManager.setWatchInterval(systemSettings.getWatchInterval());
        redeployManager.setTouchFiles(systemSettings.getTouchFiles());
        redeployManager.setWatchFiles(systemSettings.getWatchFiles());
        broker.setRedeployManager(redeployManager);
    }

    private void createAuthorizationManager(MessageBroker broker) {
        LoginManager loginManager = new LoginManager();
        LoginCommand loginCommand = null;
        Map loginCommands = securitySettings.getLoginCommands();
        LoginCommandSettings loginCommandSettings = (LoginCommandSettings) loginCommands.get(LoginCommandSettings.SERVER_MATCH_OVERRIDE);
        if (loginCommandSettings != null) {
            loginCommand = initLoginCommand(loginCommandSettings);
        } else {
            String serverInfo = securitySettings.getServerInfo();
            loginCommandSettings = (LoginCommandSettings) loginCommands.get(serverInfo);
            if (loginCommandSettings != null) {
                loginCommand = initLoginCommand(loginCommandSettings);
            } else {
                serverInfo = serverInfo.toLowerCase();
                for (Iterator iterator = loginCommands.keySet().iterator(); iterator.hasNext(); ) {
                    String serverMatch = (String) iterator.next();
                    loginCommandSettings = (LoginCommandSettings) loginCommands.get(serverMatch);
                    if (serverInfo.indexOf(serverMatch.toLowerCase()) != -1) {
                        loginCommands.put(serverInfo, loginCommandSettings);
                        loginCommand = initLoginCommand(loginCommandSettings);
                        break;
                    }
                }
            }
        }
        if (loginCommand == null) {
            if (Log.isWarn()) Log.getLogger(ConfigurationManager.LOG_CATEGORY).warn("No login command was found for '" + securitySettings.getServerInfo() + "'. Please ensure that the login-command tag has the correct server attribute value" + ", or use 'all' to use the login command regardless of the server.");
        } else {
            loginManager.setLoginCommand(loginCommand);
        }
        if (loginCommandSettings != null) loginManager.setPerClientAuthentication(loginCommandSettings.isPerClientAuthentication());
        broker.setLoginManager(loginManager);
    }

    private LoginCommand initLoginCommand(LoginCommandSettings loginCommandSettings) {
        String loginClass = loginCommandSettings.getClassName();
        Class c = ClassUtil.createClass(loginClass, FlexContext.getMessageBroker() == null ? null : FlexContext.getMessageBroker().getClassLoader());
        LoginCommand loginCommand = (LoginCommand) ClassUtil.createDefaultInstance(c, LoginCommand.class);
        return loginCommand;
    }

    private void createSharedServers(MessageBroker broker) {
        for (SharedServerSettings settings : sharedServerSettings) {
            String id = settings.getId();
            String className = settings.getClassName();
            Class serverClass = ClassUtil.createClass(className, broker.getClassLoader());
            FlexComponent server = (FlexComponent) ClassUtil.createDefaultInstance(serverClass, serverClass);
            server.initialize(id, settings.getProperties());
            if (broker.isManaged() && (server instanceof ManageableComponent)) {
                ManageableComponent manageableServer = (ManageableComponent) server;
                manageableServer.setManaged(true);
                manageableServer.setParent(broker);
            }
            broker.addServer((Server) server);
            if (Log.isInfo()) {
                Log.getLogger(ConfigurationManager.LOG_CATEGORY).info("Server '" + id + "' of type '" + className + "' created.");
            }
        }
    }

    private void createEndpoints(MessageBroker broker) {
        for (Iterator<String> iter = channelSettings.keySet().iterator(); iter.hasNext(); ) {
            String id = iter.next();
            ChannelSettings chanSettings = channelSettings.get(id);
            String url = chanSettings.getUri();
            String endpointClassName = chanSettings.getEndpointType();
            Endpoint endpoint = broker.createEndpoint(id, url, endpointClassName);
            if (endpoint instanceof AbstractEndpoint) {
                AbstractEndpoint abstractEndpoint = (AbstractEndpoint) endpoint;
                abstractEndpoint.setRemote(chanSettings.isRemote());
                abstractEndpoint.setServerOnly(chanSettings.getServerOnly());
            }
            endpoint.setSecurityConstraint(chanSettings.getConstraint());
            endpoint.setClientType(chanSettings.getClientType());
            String referencedServerId = chanSettings.getServerId();
            if ((referencedServerId != null) && (endpoint instanceof Endpoint2)) {
                Server server = broker.getServer(referencedServerId);
                if (server == null) {
                    ConfigurationException ce = new ConfigurationException();
                    ce.setMessage(11128, new Object[] { chanSettings.getId(), referencedServerId });
                    throw ce;
                }
                ((Endpoint2) endpoint).setServer(broker.getServer(referencedServerId));
            }
            endpoint.initialize(id, chanSettings.getProperties());
            if (Log.isInfo()) {
                String endpointURL = endpoint.getUrl();
                String endpointSecurity = EndpointControl.getSecurityConstraintOf(endpoint);
                if (StringUtils.isEmpty(endpointSecurity)) endpointSecurity = "None";
                Log.getLogger(ConfigurationManager.LOG_CATEGORY).info("Endpoint '" + id + "' created with security: " + endpointSecurity + StringUtils.NEWLINE + "at URL: " + endpointURL);
            }
        }
    }

    private void createServices(MessageBroker broker) {
        AuthenticationService authService = new AuthenticationService();
        authService.setMessageBroker(broker);
        for (Iterator<ServiceSettings> iter = serviceSettings.iterator(); iter.hasNext(); ) {
            ServiceSettings svcSettings = iter.next();
            String svcId = svcSettings.getId();
            String svcClassName = svcSettings.getClassName();
            Service service = broker.createService(svcId, svcClassName);
            service.initialize(svcId, svcSettings.getProperties());
            for (Iterator chanIter = svcSettings.getDefaultChannels().iterator(); chanIter.hasNext(); ) {
                ChannelSettings chanSettings = (ChannelSettings) chanIter.next();
                service.addDefaultChannel(chanSettings.getId());
            }
            Map svcAdapterSettings = svcSettings.getAllAdapterSettings();
            for (Iterator asIter = svcAdapterSettings.values().iterator(); asIter.hasNext(); ) {
                AdapterSettings as = (AdapterSettings) asIter.next();
                service.registerAdapter(as.getId(), as.getClassName());
                if (as.isDefault()) {
                    service.setDefaultAdapter(as.getId());
                }
            }
            Map destinationSettings = svcSettings.getDestinationSettings();
            for (Iterator destSettingsIter = destinationSettings.keySet().iterator(); destSettingsIter.hasNext(); ) {
                String destName = (String) destSettingsIter.next();
                DestinationSettings destSettings = (DestinationSettings) destinationSettings.get(destName);
                createDestination(destSettings, service, svcSettings);
            }
        }
    }

    private void createDestination(DestinationSettings destSettings, Service service, ServiceSettings svcSettings) {
        String destId = destSettings.getId();
        Destination destination = service.createDestination(destId);
        List chanSettings = destSettings.getChannelSettings();
        if (chanSettings.size() > 0) {
            List<String> channelIds = new ArrayList<String>(2);
            for (Iterator iter = chanSettings.iterator(); iter.hasNext(); ) {
                ChannelSettings cs = (ChannelSettings) iter.next();
                channelIds.add(cs.getId());
            }
            destination.setChannels(channelIds);
        }
        SecurityConstraint constraint = destSettings.getConstraint();
        destination.setSecurityConstraint(constraint);
        destination.initialize(destId, svcSettings.getProperties());
        destination.initialize(destId, destSettings.getAdapterSettings().getProperties());
        destination.initialize(destId, destSettings.getProperties());
        createAdapter(destination, destSettings, svcSettings);
    }

    private void createAdapter(Destination destination, DestinationSettings destSettings, ServiceSettings svcSettings) {
        AdapterSettings adapterSettings = destSettings.getAdapterSettings();
        String adapterId = adapterSettings.getId();
        ServiceAdapter adapter = destination.createAdapter(adapterId);
        adapter.initialize(adapterId, svcSettings.getProperties());
        adapter.initialize(adapterId, adapterSettings.getProperties());
        adapter.initialize(adapterId, destSettings.getProperties());
    }

    /**
     * @exclude
     * Used by the MessageBrokerServlet to set up the singleton Log instance
     * and add any targets defined in the logging configuration.
     * This needs to be invoked ahead of creating and bootstrapping a MessageBroker
     * instance so we're sure to have the logging system running in case the bootstrap
     * process needs to log anything out.
     */
    public void createLogAndTargets() {
        if (loggingSettings == null) {
            Log.setPrettyPrinterClass(ToStringPrettyPrinter.class.getName());
            return;
        }
        Log.createLog();
        ConfigMap properties = loggingSettings.getProperties();
        if (properties.getPropertyAsString("pretty-printer", null) == null) {
            Log.setPrettyPrinterClass(ToStringPrettyPrinter.class.getName());
        }
        Log.initialize(null, properties);
        List targets = loggingSettings.getTargets();
        Iterator it = targets.iterator();
        while (it.hasNext()) {
            TargetSettings targetSettings = (TargetSettings) it.next();
            String className = targetSettings.getClassName();
            Class c = ClassUtil.createClass(className, FlexContext.getMessageBroker() == null ? null : FlexContext.getMessageBroker().getClassLoader());
            try {
                Target target = (Target) c.newInstance();
                target.setLevel(Log.readLevel(targetSettings.getLevel()));
                target.setFilters(targetSettings.getFilters());
                target.initialize(null, targetSettings.getProperties());
                Log.addTarget(target);
            } catch (Throwable t) {
                if (t instanceof InvocationTargetException) t = ((InvocationTargetException) t).getCause();
                System.err.println("*** Error setting up logging system");
                t.printStackTrace();
                ConfigurationException cx = new ConfigurationException();
                cx.setMessage(10126, new Object[] { className });
                cx.setRootCause(t);
                throw cx;
            }
        }
    }

    private void createMessageFilters(MessageBroker broker) {
        Class asyncFilterClass = ClassUtil.createClass("flex.messaging.filters.BaseAsyncMessageFilter");
        Class syncFilterClass = ClassUtil.createClass("flex.messaging.filters.BaseSyncMessageFilter");
        for (MessageFilterSettings settings : messageFilterSettings) {
            String id = settings.getId();
            String className = settings.getClassName();
            Class filterClass = ClassUtil.createClass(className, broker.getClassLoader());
            FlexComponent filter = (FlexComponent) ClassUtil.createDefaultInstance(filterClass, null);
            MessageFilterSettings.FilterType filterType = settings.getFilterType();
            boolean filterIsAsync = filterType == MessageFilterSettings.FilterType.ASYNC;
            if ((filterIsAsync && !asyncFilterClass.isAssignableFrom(filterClass)) || (!filterIsAsync && !syncFilterClass.isAssignableFrom(filterClass))) {
                ConfigurationException cx = new ConfigurationException();
                int errorCode = filterIsAsync ? 11144 : 11145;
                cx.setMessage(errorCode, new Object[] { settings.getId() });
                throw cx;
            }
            filter.initialize(id, settings.getProperties());
            if (broker.isManaged() && (filter instanceof ManageableComponent)) {
                ManageableComponent manageableFilter = (ManageableComponent) filter;
                manageableFilter.setManaged(true);
                manageableFilter.setParent(broker);
            }
            try {
                String methodName = filterIsAsync ? "getAsyncMessageFilterChain" : "getSyncMessageFilterChain";
                Method getMessageFilterChain = broker.getClass().getDeclaredMethod(methodName);
                Object filterChain = getMessageFilterChain.invoke(broker, (Object[]) null);
                Class arg = filterIsAsync ? asyncFilterClass : syncFilterClass;
                Method addFilter = filterChain.getClass().getDeclaredMethod("add", arg);
                addFilter.invoke(filterChain, new Object[] { filter });
            } catch (Exception e) {
                ConfigurationException cx = new ConfigurationException();
                int errorCode = filterType == MessageFilterSettings.FilterType.ASYNC ? 11138 : 11143;
                cx.setMessage(errorCode, new Object[] { settings.getId() });
                cx.setRootCause(e);
                throw cx;
            }
            if (Log.isInfo()) {
                Log.getLogger(ConfigurationManager.LOG_CATEGORY).info("MessageFilter '" + id + "' of type '" + className + "' created.");
            }
        }
    }

    private void createValidators(MessageBroker broker) {
        for (Iterator<ValidatorSettings> iter = validatorSettings.values().iterator(); iter.hasNext(); ) {
            ValidatorSettings settings = iter.next();
            String className = settings.getClassName();
            String type = settings.getType();
            Class<?> validatorClass = ClassUtil.createClass(className, broker.getClassLoader());
            Class<?> expectedClass = ClassUtil.createClass(type, broker.getClassLoader());
            Object validator = ClassUtil.createDefaultInstance(validatorClass, expectedClass);
            if (validator instanceof DeserializationValidator) {
                DeserializationValidator deserializationValidator = (DeserializationValidator) validator;
                deserializationValidator.initialize(null, settings.getProperties());
                broker.setDeserializationValidator(deserializationValidator);
                if (Log.isInfo()) {
                    Log.getLogger(ConfigurationManager.LOG_CATEGORY).info("DeserializationValidator of type '" + className + "' created.");
                }
            }
        }
    }

    private void prepareClusters(MessageBroker broker) {
        ClusterManager clusterManager = broker.getClusterManager();
        for (Iterator<String> iter = clusterSettings.keySet().iterator(); iter.hasNext(); ) {
            String clusterId = iter.next();
            ClusterSettings cs = clusterSettings.get(clusterId);
            clusterManager.prepareCluster(cs);
        }
    }

    public void addSharedServerSettings(SharedServerSettings settings) {
        sharedServerSettings.add(settings);
    }

    public void addChannelSettings(String id, ChannelSettings settings) {
        channelSettings.put(id, settings);
    }

    public ChannelSettings getChannelSettings(String ref) {
        return channelSettings.get(ref);
    }

    public Map getAllChannelSettings() {
        return channelSettings;
    }

    public void addDefaultChannel(String id) {
        defaultChannels.add(id);
    }

    public List getDefaultChannels() {
        return defaultChannels;
    }

    public SecuritySettings getSecuritySettings() {
        return securitySettings;
    }

    public void addServiceSettings(ServiceSettings settings) {
        serviceSettings.add(settings);
    }

    public ServiceSettings getServiceSettings(String id) {
        for (Iterator<ServiceSettings> iter = serviceSettings.iterator(); iter.hasNext(); ) {
            ServiceSettings serviceSettings = iter.next();
            if (serviceSettings.getId().equals(id)) return serviceSettings;
        }
        return null;
    }

    public List getAllServiceSettings() {
        return serviceSettings;
    }

    public LoggingSettings getLoggingSettings() {
        return loggingSettings;
    }

    public void setLoggingSettings(LoggingSettings loggingSettings) {
        this.loggingSettings = loggingSettings;
    }

    public void setSystemSettings(SystemSettings ss) {
        systemSettings = ss;
    }

    public SystemSettings getSystemSettings() {
        return systemSettings;
    }

    public void setFlexClientSettings(FlexClientSettings value) {
        flexClientSettings = value;
    }

    public FlexClientSettings getFlexClientSettings() {
        return flexClientSettings;
    }

    public void addClusterSettings(ClusterSettings settings) {
        if (settings.isDefault()) {
            for (Iterator<ClusterSettings> it = clusterSettings.values().iterator(); it.hasNext(); ) {
                ClusterSettings cs = it.next();
                if (cs.isDefault()) {
                    ConfigurationException cx = new ConfigurationException();
                    cx.setMessage(10214, new Object[] { settings.getClusterName(), cs.getClusterName() });
                    throw cx;
                }
            }
        }
        if (clusterSettings.containsKey(settings.getClusterName())) {
            ConfigurationException cx = new ConfigurationException();
            cx.setMessage(10206, new Object[] { settings.getClusterName() });
            throw cx;
        }
        clusterSettings.put(settings.getClusterName(), settings);
    }

    public ClusterSettings getClusterSettings(String clusterId) {
        for (Iterator<ClusterSettings> it = clusterSettings.values().iterator(); it.hasNext(); ) {
            ClusterSettings cs = it.next();
            if (cs.getClusterName() == clusterId) return cs;
            if (cs.getClusterName() != null && cs.getClusterName().equals(clusterId)) return cs;
        }
        return null;
    }

    public ClusterSettings getDefaultCluster() {
        for (Iterator<ClusterSettings> it = clusterSettings.values().iterator(); it.hasNext(); ) {
            ClusterSettings cs = it.next();
            if (cs.isDefault()) return cs;
        }
        return null;
    }

    public void addFactorySettings(String id, FactorySettings settings) {
        factorySettings.put(id, settings);
    }

    public void addMessageFilterSettings(MessageFilterSettings settings) {
        messageFilterSettings.add(settings);
    }

    public void addValidatorSettings(ValidatorSettings settings) {
        String type = settings.getType();
        if (validatorSettings.containsKey(type)) {
            ConfigurationException ce = new ConfigurationException();
            ce.setMessage(11136, new Object[] { type });
            throw ce;
        }
        validatorSettings.put(type, settings);
    }

    public void reportUnusedProperties() {
        ArrayList<Object[]> findings = new ArrayList<Object[]>();
        Iterator<ServiceSettings> serviceItr = serviceSettings.iterator();
        while (serviceItr.hasNext()) {
            ServiceSettings serviceSettings = serviceItr.next();
            gatherUnusedProperties(serviceSettings.getId(), serviceSettings.getSourceFile(), ConfigurationConstants.SERVICE_ELEMENT, serviceSettings, findings);
            Iterator destinationItr = serviceSettings.getDestinationSettings().values().iterator();
            while (destinationItr.hasNext()) {
                DestinationSettings destinationSettings = (DestinationSettings) destinationItr.next();
                gatherUnusedProperties(destinationSettings.getId(), destinationSettings.getSourceFile(), ConfigurationConstants.DESTINATION_ELEMENT, destinationSettings, findings);
                AdapterSettings adapterSettings = destinationSettings.getAdapterSettings();
                if (adapterSettings != null) {
                    gatherUnusedProperties(adapterSettings.getId(), adapterSettings.getSourceFile(), ConfigurationConstants.ADAPTER_ELEMENT, adapterSettings, findings);
                }
            }
        }
        Iterator<ChannelSettings> channelItr = channelSettings.values().iterator();
        while (channelItr.hasNext()) {
            ChannelSettings channelSettings = channelItr.next();
            if (channelSettings.isRemote()) continue;
            gatherUnusedProperties(channelSettings.getId(), channelSettings.getSourceFile(), ConfigurationConstants.CHANNEL_ELEMENT, channelSettings, findings);
        }
        Iterator<SharedServerSettings> serverItr = sharedServerSettings.iterator();
        while (serverItr.hasNext()) {
            SharedServerSettings serverSettings = serverItr.next();
            gatherUnusedProperties(serverSettings.getId(), serverSettings.getSourceFile(), ConfigurationConstants.SERVER_ELEMENT, serverSettings, findings);
        }
        if (!findings.isEmpty()) {
            int errorNumber = 10149;
            ConfigurationException exception = new ConfigurationException();
            StringBuffer allDetails = new StringBuffer();
            for (int i = 0; i < findings.size(); i++) {
                allDetails.append(StringUtils.NEWLINE);
                allDetails.append("  ");
                exception.setDetails(errorNumber, "pattern", findings.get(i));
                allDetails.append(exception.getDetails());
                exception.setDetails(null);
            }
            exception.setMessage(errorNumber, new Object[] { allDetails });
            throw exception;
        }
    }

    private void gatherUnusedProperties(String settingsId, String settingsSource, String settingsType, PropertiesSettings settings, Collection<Object[]> result) {
        List unusedProperties = settings.getProperties().findAllUnusedProperties();
        int size = unusedProperties.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                String path = (String) unusedProperties.get(i);
                result.add(new Object[] { path, settingsType, settingsId, settingsSource });
            }
        }
    }
}

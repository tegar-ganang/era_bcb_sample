package org.datanucleus;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import javax.jdo.spi.JDOImplHelper;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.api.ApiAdapterFactory;
import org.datanucleus.cache.Level2Cache;
import org.datanucleus.cache.NullLevel2Cache;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.exceptions.TransactionIsolationNotSupportedException;
import org.datanucleus.identity.IdentityKeyTranslator;
import org.datanucleus.identity.IdentityStringTranslator;
import org.datanucleus.jta.TransactionManagerFinder;
import org.datanucleus.management.FactoryStatistics;
import org.datanucleus.management.jmx.ManagementManager;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.Extension;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.state.CallbackHandler;
import org.datanucleus.state.JDOStateManager;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.federation.FederatedStoreManager;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.transaction.NucleusTransactionException;
import org.datanucleus.transaction.TransactionManager;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;
import org.datanucleus.validation.BeanValidatorHandler;

/**
 * Representation of the context being run within DataNucleus. Provides a series of services and can be used
 * by JDO persistence, JPA persistence, JDO enhancement, JPA enhancement, amongst other things,
 */
public class NucleusContext {

    /** Localisation of messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation", ClassConstants.NUCLEUS_CONTEXT_LOADER);

    public enum ContextType {

        PERSISTENCE, ENHANCEMENT
    }

    private final ContextType type;

    /** Manager for the datastore used by this PMF/EMF. */
    private transient StoreManager storeMgr = null;

    /** MetaDataManager for handling the MetaData for this PMF/EMF. */
    private MetaDataManager metaDataManager = null;

    /** Flag defining if this is running within the JDO JCA adaptor. */
    private boolean jca = false;

    /** Configuration defining features of the persistence process. */
    private final PersistenceConfiguration config;

    /** Manager for Plug-ins. */
    private final PluginManager pluginManager;

    /** ApiAdapter used by the context. **/
    private final ApiAdapter apiAdapter;

    /** Name of the class providing the ClassLoaderResolver. */
    private final String classLoaderResolverClassName;

    /** Manager for java types and SCO wrappers. */
    private TypeManager typeManager;

    /** Level 2 Cache, caching across ObjectManagers. */
    private Level2Cache cache;

    /** Transaction Manager. */
    private transient TransactionManager txManager = null;

    /** JTA Transaction Manager (if using JTA). */
    private transient javax.transaction.TransactionManager jtaTxManager = null;

    /** Map of the ClassLoaderResolver, keyed by the clr class and the primaryLoader name. */
    private transient Map<String, ClassLoaderResolver> classLoaderResolverMap = new HashMap<String, ClassLoaderResolver>();

    /** Manager for JMX features. */
    private transient ManagementManager jmxManager = null;

    /** Statistics gathering object. */
    private transient FactoryStatistics statistics = null;

    /** Random number generator, for use when needing unique names. */
    public static final Random random = new Random();

    /** Class to use for datastore-identity. */
    private Class datastoreIdentityClass = null;

    /** Identity string translator (if any). */
    private IdentityStringTranslator idStringTranslator = null;

    /** Flag for whether we have initialised the id string translator. */
    private boolean idStringTranslatorInit = false;

    /** Identity key translator (if any). */
    private IdentityKeyTranslator idKeyTranslator = null;

    /** Flag for whether we have initialised the id key translator. */
    private boolean idKeyTranslatorInit = false;

    /** ImplementationCreator for any persistent interfaces. */
    private ImplementationCreator implCreator;

    /** Flag for whether we have initialised the implementation creator. */
    private boolean implCreatorInit = false;

    private List<ExecutionContext.LifecycleListener> executionContextListeners = new ArrayList();

    /** Manager for dynamic fetch groups defined on the PMF/EMF. */
    private transient FetchGroupManager fetchGrpMgr;

    /** Factory for validation. */
    private transient Object validatorFactory = null;

    /** Flag for whether we have initialised the validator factory. */
    private transient boolean validatorFactoryInit = false;

    public static final Set<String> STARTUP_PROPERTIES = new HashSet<String>();

    static {
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_PLUGIN_REGISTRY_CLASSNAME);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_PLUGIN_REGISTRYBUNDLECHECK);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_PLUGIN_ALLOW_USER_BUNDLES);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_PLUGIN_VALIDATEPLUGINS);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_CLASSLOADER_RESOLVER_NAME);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_PERSISTENCE_XML_FILENAME);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY);
    }

    ;

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     */
    public NucleusContext(String apiName, Map startupProps) {
        this(apiName, ContextType.PERSISTENCE, startupProps);
    }

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     * @param pluginMgr Plugin Manager (or null if wanting it to be created)
     */
    public NucleusContext(String apiName, Map startupProps, PluginManager pluginMgr) {
        this(apiName, ContextType.PERSISTENCE, startupProps, pluginMgr);
    }

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param type The type of context required (persistence, enhancement)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     */
    public NucleusContext(String apiName, ContextType type, Map startupProps) {
        this(apiName, type, startupProps, null);
    }

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param type The type of context required (persistence, enhancement)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     * @param pluginMgr Plugin Manager (or null if wanting it to be created)
     */
    public NucleusContext(String apiName, ContextType type, Map startupProps, PluginManager pluginMgr) {
        this.type = type;
        this.config = new PersistenceConfiguration();
        if (pluginMgr != null) {
            this.pluginManager = pluginMgr;
        } else {
            this.pluginManager = PluginManager.createPluginManager(startupProps, this.getClass().getClassLoader());
        }
        config.setDefaultProperties(pluginManager);
        if (startupProps != null && !startupProps.isEmpty()) {
            config.setPersistenceProperties(startupProps);
        }
        String clrName = config.getStringProperty(PropertyNames.PROPERTY_CLASSLOADER_RESOLVER_NAME);
        classLoaderResolverClassName = pluginManager.getAttributeValueForExtension("org.datanucleus.classloader_resolver", "name", clrName, "class-name");
        if (classLoaderResolverClassName == null) {
            throw new NucleusUserException(LOCALISER.msg("001001", clrName)).setFatal();
        }
        this.apiAdapter = ApiAdapterFactory.getInstance().getApiAdapter(apiName, pluginManager);
        config.setDefaultProperties(apiAdapter.getDefaultFactoryProperties());
        if (type == ContextType.PERSISTENCE) {
            AccessController.doPrivileged(new PrivilegedAction() {

                public Object run() {
                    JDOImplHelper.registerAuthorizedStateManagerClass(JDOStateManager.class);
                    return null;
                }
            });
        }
        if (!apiName.equalsIgnoreCase("JDO")) {
            implCreatorInit = true;
            implCreator = null;
        }
    }

    /**
     * Method to initialise the context for use.
     * This creates the required StoreManager(s).
     */
    public synchronized void initialise() {
        if (type == ContextType.PERSISTENCE) {
            ClassLoaderResolver clr = getClassLoaderResolver(null);
            clr.registerUserClassLoader((ClassLoader) config.getProperty(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY));
            Set<String> propNamesWithDatastore = config.getPropertyNamesWithPrefix("datanucleus.datastore.");
            if (propNamesWithDatastore == null) {
                NucleusLogger.DATASTORE.debug("Creating StoreManager for datastore");
                Map<String, Object> datastoreProps = config.getDatastoreProperties();
                this.storeMgr = createStoreManagerForProperties(config.getPersistenceProperties(), datastoreProps, clr, this);
                String transactionIsolation = config.getStringProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION);
                if (transactionIsolation != null) {
                    String reqdIsolation = getTransactionIsolationForStoreManager(storeMgr, transactionIsolation);
                    if (!transactionIsolation.equalsIgnoreCase(reqdIsolation)) {
                        config.setProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION, reqdIsolation);
                    }
                }
            } else {
                NucleusLogger.DATASTORE.debug("Creating FederatedStoreManager to handle federation of primary StoreManager and " + propNamesWithDatastore.size() + " secondary datastores");
                this.storeMgr = new FederatedStoreManager(clr, this);
            }
            NucleusLogger.DATASTORE.debug("StoreManager now created");
            String puName = config.getStringProperty(PropertyNames.PROPERTY_PERSISTENCE_UNIT_NAME);
            if (puName != null) {
                boolean loadClasses = config.getBooleanProperty(PropertyNames.PROPERTY_PERSISTENCE_UNIT_LOAD_CLASSES);
                if (loadClasses) {
                    Collection<String> loadedClasses = getMetaDataManager().getClassesWithMetaData();
                    this.storeMgr.addClasses(loadedClasses.toArray(new String[loadedClasses.size()]), clr);
                }
            }
        }
        logConfiguration();
    }

    /**
     * Method to return the transaction isolation level that will be used for the provided StoreManager
     * bearing in mind the specified level the user requested.
     * @param storeMgr The Store Manager
     * @param transactionIsolation Requested isolation level
     * @return Isolation level to use
     * @throws TransactionIsolationNotSupportedException When no suitable level available given the requested level
     */
    public static String getTransactionIsolationForStoreManager(StoreManager storeMgr, String transactionIsolation) {
        if (transactionIsolation != null) {
            Collection srmOptions = storeMgr.getSupportedOptions();
            if (!srmOptions.contains("TransactionIsolationLevel." + transactionIsolation)) {
                if (transactionIsolation.equals("read-uncommitted")) {
                    if (srmOptions.contains("TransactionIsolationLevel.read-committed")) {
                        return "read-committed";
                    } else if (srmOptions.contains("TransactionIsolationLevel.repeatable-read")) {
                        return "repeatable-read";
                    } else if (srmOptions.contains("TransactionIsolationLevel.serializable")) {
                        return "serializable";
                    }
                } else if (transactionIsolation.equals("read-committed")) {
                    if (srmOptions.contains("TransactionIsolationLevel.repeatable-read")) {
                        return "repeatable-read";
                    } else if (srmOptions.contains("TransactionIsolationLevel.serializable")) {
                        return "serializable";
                    }
                } else if (transactionIsolation.equals("repeatable-read")) {
                    if (srmOptions.contains("TransactionIsolationLevel.serializable")) {
                        return "serializable";
                    }
                } else {
                    throw new TransactionIsolationNotSupportedException(transactionIsolation);
                }
            }
        }
        return transactionIsolation;
    }

    /**
     * Method to create a StoreManager based on the specified properties passed in.
     * @param props The overall persistence properties
     * @param datastoreProps Persistence properties to apply to the datastore
     * @param clr ClassLoader resolver
     * @return The StoreManager
     * @throws NucleusUserException if impossible to create the StoreManager (not in CLASSPATH?, invalid definition?)
     */
    public static StoreManager createStoreManagerForProperties(Map<String, Object> props, Map<String, Object> datastoreProps, ClassLoaderResolver clr, NucleusContext nucCtx) {
        Extension[] exts = nucCtx.getPluginManager().getExtensionPoint("org.datanucleus.store_manager").getExtensions();
        Class[] ctrArgTypes = new Class[] { ClassConstants.CLASS_LOADER_RESOLVER, ClassConstants.NUCLEUS_CONTEXT, Map.class };
        Object[] ctrArgs = new Object[] { clr, nucCtx, datastoreProps };
        StoreManager storeMgr = null;
        String storeManagerType = (String) props.get(PropertyNames.PROPERTY_STORE_MANAGER_TYPE.toLowerCase());
        if (storeManagerType != null) {
            for (int e = 0; storeMgr == null && e < exts.length; e++) {
                ConfigurationElement[] confElm = exts[e].getConfigurationElements();
                for (int c = 0; storeMgr == null && c < confElm.length; c++) {
                    String key = confElm[c].getAttribute("key");
                    if (key.equalsIgnoreCase(storeManagerType)) {
                        try {
                            storeMgr = (StoreManager) nucCtx.getPluginManager().createExecutableExtension("org.datanucleus.store_manager", "key", storeManagerType, "class-name", ctrArgTypes, ctrArgs);
                        } catch (InvocationTargetException ex) {
                            Throwable t = ex.getTargetException();
                            if (t instanceof RuntimeException) {
                                throw (RuntimeException) t;
                            } else if (t instanceof Error) {
                                throw (Error) t;
                            } else {
                                throw new NucleusException(t.getMessage(), t).setFatal();
                            }
                        } catch (Exception ex) {
                            throw new NucleusException(ex.getMessage(), ex).setFatal();
                        }
                    }
                }
            }
            if (storeMgr == null) {
                throw new NucleusUserException(LOCALISER.msg("008004", storeManagerType)).setFatal();
            }
        }
        if (storeMgr == null) {
            String url = (String) props.get("datanucleus.connectionurl");
            if (url != null) {
                int idx = url.indexOf(':');
                if (idx > -1) {
                    url = url.substring(0, idx);
                }
            }
            for (int e = 0; storeMgr == null && e < exts.length; e++) {
                ConfigurationElement[] confElm = exts[e].getConfigurationElements();
                for (int c = 0; storeMgr == null && c < confElm.length; c++) {
                    String urlKey = confElm[c].getAttribute("url-key");
                    if (url == null || urlKey.equalsIgnoreCase(url)) {
                        try {
                            storeMgr = (StoreManager) nucCtx.getPluginManager().createExecutableExtension("org.datanucleus.store_manager", "url-key", url == null ? urlKey : url, "class-name", ctrArgTypes, ctrArgs);
                        } catch (InvocationTargetException ex) {
                            Throwable t = ex.getTargetException();
                            if (t instanceof RuntimeException) {
                                throw (RuntimeException) t;
                            } else if (t instanceof Error) {
                                throw (Error) t;
                            } else {
                                throw new NucleusException(t.getMessage(), t).setFatal();
                            }
                        } catch (Exception ex) {
                            throw new NucleusException(ex.getMessage(), ex).setFatal();
                        }
                    }
                }
            }
            if (storeMgr == null) {
                throw new NucleusUserException(LOCALISER.msg("008004", url)).setFatal();
            }
        }
        return storeMgr;
    }

    /**
     * Clear out resources
     */
    public synchronized void close() {
        if (fetchGrpMgr != null) {
            fetchGrpMgr.clearFetchGroups();
        }
        if (storeMgr != null) {
            storeMgr.close();
            storeMgr = null;
        }
        if (metaDataManager != null) {
            metaDataManager.close();
            metaDataManager = null;
        }
        if (statistics != null) {
            if (jmxManager != null) {
                jmxManager.deregisterMBean(statistics.getRegisteredName());
            }
            statistics = null;
        }
        if (jmxManager != null) {
            jmxManager.close();
            jmxManager = null;
        }
        if (cache != null) {
            cache.close();
            NucleusLogger.CACHE.debug(LOCALISER.msg("004009"));
        }
        classLoaderResolverMap.clear();
        classLoaderResolverMap = null;
        datastoreIdentityClass = null;
    }

    /**
     * Accessor for the type of this context (persistence, enhancer etc).
     * @return The type
     */
    public ContextType getType() {
        return type;
    }

    /**
     * Accessor for the ApiAdapter
     * @return the ApiAdapter
     */
    public ApiAdapter getApiAdapter() {
        return apiAdapter;
    }

    /**
     * Accessor for the name of the API (JDO, JPA, etc).
     * @return the api
     */
    public String getApiName() {
        return apiAdapter.getName();
    }

    /**
     * Accessor for the persistence configuration.
     * @return Returns the persistence configuration.
     */
    public PersistenceConfiguration getPersistenceConfiguration() {
        return config;
    }

    /**
     * Accessor for the Plugin Manager
     * @return the PluginManager
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Accessor for the Type Manager
     * @return the TypeManager
     */
    public TypeManager getTypeManager() {
        if (typeManager == null) {
            this.typeManager = new TypeManager(apiAdapter, pluginManager, getClassLoaderResolver(null));
        }
        return typeManager;
    }

    /**
     * Method to log the configuration of this context.
     */
    protected void logConfiguration() {
        if (NucleusLogger.PERSISTENCE.isDebugEnabled()) {
            NucleusLogger.PERSISTENCE.debug("================= Persistence Configuration ===============");
            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("008000", "DataNucleus", pluginManager.getVersionForBundle("org.datanucleus")));
            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("008001", config.getStringProperty(PropertyNames.PROPERTY_CONNECTION_URL), config.getStringProperty(PropertyNames.PROPERTY_CONNECTION_DRIVER_NAME), config.getStringProperty(PropertyNames.PROPERTY_CONNECTION_USER_NAME)));
            NucleusLogger.PERSISTENCE.debug("JDK : " + System.getProperty("java.version") + " on " + System.getProperty("os.name"));
            NucleusLogger.PERSISTENCE.debug("Persistence API : " + apiAdapter.getName());
            NucleusLogger.PERSISTENCE.debug("Plugin Registry : " + pluginManager.getRegistryClassName());
            if (config.hasPropertyNotNull(PropertyNames.PROPERTY_PERSISTENCE_UNIT_NAME)) {
                NucleusLogger.PERSISTENCE.debug("Persistence-Unit : " + config.getStringProperty(PropertyNames.PROPERTY_PERSISTENCE_UNIT_NAME));
            }
            String timeZoneID = config.getStringProperty(PropertyNames.PROPERTY_SERVER_TIMEZONE_ID);
            if (timeZoneID == null) {
                timeZoneID = TimeZone.getDefault().getID();
            }
            NucleusLogger.PERSISTENCE.debug("Standard Options : " + (config.getBooleanProperty(PropertyNames.PROPERTY_MULTITHREADED) ? "pm-multithreaded" : "pm-singlethreaded") + (config.getBooleanProperty(PropertyNames.PROPERTY_RETAIN_VALUES) ? ", retain-values" : "") + (config.getBooleanProperty(PropertyNames.PROPERTY_RESTORE_VALUES) ? ", restore-values" : "") + (config.getBooleanProperty(PropertyNames.PROPERTY_NONTX_READ) ? ", nontransactional-read" : "") + (config.getBooleanProperty(PropertyNames.PROPERTY_NONTX_WRITE) ? ", nontransactional-write" : "") + (config.getBooleanProperty(PropertyNames.PROPERTY_IGNORE_CACHE) ? ", ignoreCache" : "") + ", serverTimeZone=" + timeZoneID);
            NucleusLogger.PERSISTENCE.debug("Persistence Options :" + (config.getBooleanProperty(PropertyNames.PROPERTY_PERSISTENCE_BY_REACHABILITY_AT_COMMIT) ? " reachability-at-commit" : "") + (config.getBooleanProperty(PropertyNames.PROPERTY_DETACH_ALL_ON_COMMIT) ? " detach-all-on-commit" : "") + (config.getBooleanProperty(PropertyNames.PROPERTY_DETACH_ALL_ON_ROLLBACK) ? " detach-all-on-rollback" : "") + (config.getBooleanProperty(PropertyNames.PROPERTY_DETACH_ON_CLOSE) ? " detach-on-close" : "") + (config.getBooleanProperty(PropertyNames.PROPERTY_MANAGE_RELATIONSHIPS) ? (config.getBooleanProperty(PropertyNames.PROPERTY_MANAGE_RELATIONSHIPS_CHECKS) ? " managed-relations(checked)" : "managed-relations(unchecked)") : "") + " deletion-policy=" + config.getStringProperty(PropertyNames.PROPERTY_DELETION_POLICY));
            NucleusLogger.PERSISTENCE.debug("Transactions : type=" + config.getStringProperty(PropertyNames.PROPERTY_TRANSACTION_TYPE) + " mode=" + (config.getBooleanProperty(PropertyNames.PROPERTY_OPTIMISTIC) ? "optimistic" : "datastore") + " isolation=" + config.getStringProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION));
            NucleusLogger.PERSISTENCE.debug("Value Generation :" + " txn-isolation=" + config.getStringProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ISOLATION) + " connection=" + (config.getStringProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ATTRIBUTE).equalsIgnoreCase("New") ? "New" : "Existing"));
            Object primCL = config.getProperty(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY);
            NucleusLogger.PERSISTENCE.debug("ClassLoading : " + config.getStringProperty(PropertyNames.PROPERTY_CLASSLOADER_RESOLVER_NAME) + (primCL != null ? ("primary=" + primCL) : ""));
            NucleusLogger.PERSISTENCE.debug("Cache : Level1 (" + config.getStringProperty(PropertyNames.PROPERTY_CACHE_L1_TYPE) + ")" + ", Level2 (" + config.getStringProperty(PropertyNames.PROPERTY_CACHE_L2_TYPE) + ", mode=" + config.getStringProperty(PropertyNames.PROPERTY_CACHE_L2_MODE) + ")" + ", QueryResults (" + config.getStringProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_TYPE) + ")" + (config.getBooleanProperty(PropertyNames.PROPERTY_CACHE_COLLECTIONS) ? ", Collections/Maps " : ""));
            NucleusLogger.PERSISTENCE.debug("===========================================================");
        }
    }

    /**
     * Accessor for the class to use for datastore identity.
     * @return Class for datastore-identity
     */
    public synchronized Class getDatastoreIdentityClass() {
        if (datastoreIdentityClass == null) {
            String dsidName = config.getStringProperty(PropertyNames.PROPERTY_DATASTORE_IDENTITY_TYPE);
            String datastoreIdentityClassName = pluginManager.getAttributeValueForExtension("org.datanucleus.store_datastoreidentity", "name", dsidName, "class-name");
            if (datastoreIdentityClassName == null) {
                throw new NucleusUserException(LOCALISER.msg("002001", dsidName)).setFatal();
            }
            ClassLoaderResolver clr = getClassLoaderResolver(null);
            try {
                datastoreIdentityClass = clr.classForName(datastoreIdentityClassName, org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);
            } catch (ClassNotResolvedException cnre) {
                throw new NucleusUserException(LOCALISER.msg("002002", dsidName, datastoreIdentityClassName)).setFatal();
            }
        }
        return datastoreIdentityClass;
    }

    /**
     * Accessor for the current identity string translator to use (if any).
     * @return Identity string translator instance (or null if persistence property not set)
     */
    public synchronized IdentityStringTranslator getIdentityStringTranslator() {
        if (idStringTranslatorInit) {
            return idStringTranslator;
        }
        idStringTranslatorInit = true;
        String translatorType = config.getStringProperty(PropertyNames.PROPERTY_IDENTITY_STRING_TRANSLATOR_TYPE);
        if (translatorType != null) {
            try {
                idStringTranslator = (IdentityStringTranslator) pluginManager.createExecutableExtension("org.datanucleus.identity_string_translator", "name", translatorType, "class-name", null, null);
                return idStringTranslator;
            } catch (Exception e) {
                throw new NucleusUserException(LOCALISER.msg("002001", translatorType)).setFatal();
            }
        }
        return null;
    }

    /**
     * Accessor for the current identity key translator to use (if any).
     * @return Identity key translator instance (or null if persistence property not set)
     */
    public synchronized IdentityKeyTranslator getIdentityKeyTranslator() {
        if (idKeyTranslatorInit) {
            return idKeyTranslator;
        }
        idKeyTranslatorInit = true;
        String translatorType = config.getStringProperty(PropertyNames.PROPERTY_IDENTITY_KEY_TRANSLATOR_TYPE);
        if (translatorType != null) {
            try {
                idKeyTranslator = (IdentityKeyTranslator) pluginManager.createExecutableExtension("org.datanucleus.identity_key_translator", "name", translatorType, "class-name", null, null);
                return idKeyTranslator;
            } catch (Exception e) {
                throw new NucleusUserException(LOCALISER.msg("002001", translatorType)).setFatal();
            }
        }
        return null;
    }

    /**
     * Accessor for whether statistics gathering is enabled.
     * @return Whether the user has enabled statistics or JMX management is enabled
     */
    public boolean statisticsEnabled() {
        return config.getBooleanProperty(PropertyNames.PROPERTY_ENABLE_STATISTICS) || getJMXManager() != null;
    }

    /**
     * Accessor for the JMX manager (if required).
     * Does nothing if the property "datanucleus.jmxType" is unset.
     * @return The JMX manager
     */
    public synchronized ManagementManager getJMXManager() {
        if (jmxManager == null && config.getStringProperty(PropertyNames.PROPERTY_JMX_TYPE) != null) {
            jmxManager = new ManagementManager(this);
        }
        return jmxManager;
    }

    public synchronized FactoryStatistics getStatistics() {
        if (statistics == null && statisticsEnabled()) {
            String name = null;
            if (getJMXManager() != null) {
                name = jmxManager.getDomainName() + ":InstanceName=" + jmxManager.getInstanceName() + ",Type=" + ClassUtils.getClassNameForClass(statistics.getClass()) + ",Name=Factory" + random.nextInt();
            }
            statistics = new FactoryStatistics(name);
            if (jmxManager != null) {
                jmxManager.registerMBean(this.statistics, name);
            }
        }
        return statistics;
    }

    /**
     * Accessor for a ClassLoaderResolver to use in resolving classes.
     * Caches the resolver for the specified primary loader, and hands it out if present.
     * @param primaryLoader Loader to use as the primary loader (or null)
     * @return The ClassLoader resolver
     */
    public ClassLoaderResolver getClassLoaderResolver(ClassLoader primaryLoader) {
        String resolverName = config.getStringProperty(PropertyNames.PROPERTY_CLASSLOADER_RESOLVER_NAME);
        String key = resolverName;
        if (primaryLoader != null) {
            key += ":[" + StringUtils.toJVMIDString(primaryLoader) + "]";
        }
        if (classLoaderResolverMap == null) {
            classLoaderResolverMap = new HashMap<String, ClassLoaderResolver>();
        }
        ClassLoaderResolver clr = classLoaderResolverMap.get(key);
        if (clr != null) {
            return clr;
        }
        try {
            clr = (ClassLoaderResolver) pluginManager.createExecutableExtension("org.datanucleus.classloader_resolver", "name", resolverName, "class-name", new Class[] { ClassLoader.class }, new Object[] { primaryLoader });
            clr.registerUserClassLoader((ClassLoader) config.getProperty(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY));
        } catch (ClassNotFoundException cnfe) {
            throw new NucleusUserException(LOCALISER.msg("001002", classLoaderResolverClassName), cnfe).setFatal();
        } catch (Exception e) {
            throw new NucleusUserException(LOCALISER.msg("001003", classLoaderResolverClassName), e).setFatal();
        }
        classLoaderResolverMap.put(key, clr);
        return clr;
    }

    /**
     * Accessor for the implementation creator for this context.
     * @return The implementation creator
     */
    public synchronized ImplementationCreator getImplementationCreator() {
        if (implCreatorInit) {
            return implCreator;
        }
        String implCreatorName = config.getStringProperty(PropertyNames.PROPERTY_IMPLEMENTATION_CREATOR_NAME);
        if (implCreatorName != null && implCreatorName.equalsIgnoreCase("None")) {
            implCreator = null;
            implCreatorInit = true;
            return implCreator;
        }
        try {
            implCreator = (ImplementationCreator) getPluginManager().createExecutableExtension("org.datanucleus.implementation_creator", "name", implCreatorName, "class-name", new Class[] { ClassConstants.METADATA_MANAGER }, new Object[] { getMetaDataManager() });
        } catch (Exception e) {
            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("008006", implCreatorName));
        }
        if (implCreator == null) {
            ConfigurationElement[] elems = getPluginManager().getConfigurationElementsForExtension("org.datanucleus.implementation_creator", null, null);
            String first = null;
            if (elems != null && elems.length > 0) {
                first = elems[0].getAttribute("name");
                try {
                    implCreator = (ImplementationCreator) getPluginManager().createExecutableExtension("org.datanucleus.implementation_creator", "name", first, "class-name", new Class[] { ClassConstants.METADATA_MANAGER }, new Object[] { getMetaDataManager() });
                } catch (Exception e) {
                    NucleusLogger.PERSISTENCE.info(LOCALISER.msg("008006", first));
                }
            }
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled()) {
            if (implCreator == null) {
                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("008007"));
            } else {
                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("008008", StringUtils.toJVMIDString(implCreator)));
            }
        }
        implCreatorInit = true;
        return implCreator;
    }

    /**
     * Accessor for the Meta-Data Manager.
     * @return Returns the MetaDataManager.
     */
    public synchronized MetaDataManager getMetaDataManager() {
        if (metaDataManager == null) {
            String apiName = apiAdapter.getName();
            try {
                metaDataManager = (MetaDataManager) getPluginManager().createExecutableExtension("org.datanucleus.metadata_manager", new String[] { "name" }, new String[] { apiName }, "class", new Class[] { ClassConstants.NUCLEUS_CONTEXT }, new Object[] { this });
            } catch (Exception e) {
                throw new NucleusException(LOCALISER.msg("008010", apiName, e.getMessage()), e);
            }
            if (metaDataManager == null) {
                throw new NucleusException(LOCALISER.msg("008009", apiName));
            }
        }
        return metaDataManager;
    }

    /**
     * Accessor for the transaction manager.
     * @return The transaction manager.
     */
    public synchronized TransactionManager getTransactionManager() {
        if (txManager == null) {
            txManager = new TransactionManager();
        }
        return txManager;
    }

    /**
     * Accessor for the JTA transaction manager (if using JTA).
     * @return the JTA Transaction Manager
     */
    public synchronized javax.transaction.TransactionManager getJtaTransactionManager() {
        if (jtaTxManager == null) {
            jtaTxManager = new TransactionManagerFinder(this).getTransactionManager(getClassLoaderResolver((ClassLoader) config.getProperty(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY)));
            if (jtaTxManager == null) {
                throw new NucleusTransactionException(LOCALISER.msg("015030"));
            }
        }
        return jtaTxManager;
    }

    public boolean isStoreManagerInitialised() {
        return (storeMgr != null);
    }

    /**
     * Accessor for the StoreManager
     * @return the StoreManager
     */
    public StoreManager getStoreManager() {
        if (storeMgr == null) {
            initialise();
        }
        return storeMgr;
    }

    /**
     * Method to return a handler for validation (JSR303).
     * @param ec The ExecutionContext that the handler is for.
     * @return The handler (or null if not supported on this PMF/EMF, or no validator present)
     */
    public CallbackHandler getValidationHandler(ExecutionContext ec) {
        if (validatorFactoryInit && validatorFactory == null) {
            return null;
        }
        if (config.hasPropertyNotNull(PropertyNames.PROPERTY_VALIDATION_MODE)) {
            if (config.getStringProperty(PropertyNames.PROPERTY_VALIDATION_MODE).equalsIgnoreCase("none")) {
                validatorFactoryInit = true;
                return null;
            }
        }
        try {
            ec.getClassLoaderResolver().classForName("javax.validation.Validation");
        } catch (ClassNotResolvedException cnre) {
            validatorFactoryInit = true;
            return null;
        }
        try {
            if (validatorFactory == null) {
                validatorFactoryInit = true;
                if (config.hasPropertyNotNull(PropertyNames.PROPERTY_VALIDATION_FACTORY)) {
                    validatorFactory = config.getProperty(PropertyNames.PROPERTY_VALIDATION_FACTORY);
                } else {
                    validatorFactory = Validation.buildDefaultValidatorFactory();
                }
            }
            return new BeanValidatorHandler(ec, (ValidatorFactory) validatorFactory);
        } catch (Throwable ex) {
            if (config.hasPropertyNotNull(PropertyNames.PROPERTY_VALIDATION_MODE)) {
                if (config.getStringProperty(PropertyNames.PROPERTY_VALIDATION_MODE).equalsIgnoreCase("callback")) {
                    throw ec.getApiAdapter().getUserExceptionForException(ex.getMessage(), (Exception) ex);
                }
            }
            NucleusLogger.GENERAL.warn("Unable to create validator handler", ex);
        }
        return null;
    }

    /**
     * Return whether there is an L2 cache.
     * @return Whether the L2 cache is enabled
     */
    public boolean hasLevel2Cache() {
        getLevel2Cache();
        return !(cache instanceof NullLevel2Cache);
    }

    /**
     * Accessor for the DataStore (level 2) Cache
     * @return The datastore cache
     */
    public Level2Cache getLevel2Cache() {
        if (cache == null) {
            String level2Type = config.getStringProperty(PropertyNames.PROPERTY_CACHE_L2_TYPE);
            String level2ClassName = pluginManager.getAttributeValueForExtension("org.datanucleus.cache_level2", "name", level2Type, "class-name");
            if (level2ClassName == null) {
                throw new NucleusUserException(LOCALISER.msg("004000", level2Type)).setFatal();
            }
            try {
                cache = (Level2Cache) pluginManager.createExecutableExtension("org.datanucleus.cache_level2", "name", level2Type, "class-name", new Class[] { ClassConstants.NUCLEUS_CONTEXT }, new Object[] { this });
                if (NucleusLogger.CACHE.isDebugEnabled()) {
                    NucleusLogger.CACHE.debug(LOCALISER.msg("004002", level2Type));
                }
            } catch (Exception e) {
                throw new NucleusUserException(LOCALISER.msg("004001", level2Type, level2ClassName), e).setFatal();
            }
        }
        return cache;
    }

    /**
     * Object the array of registered ExecutionContext listeners.
     * @return array of {@link org.datanucleus.store.ExecutionContext.LifecycleListener}
     */
    public ExecutionContext.LifecycleListener[] getExecutionContextListeners() {
        return executionContextListeners.toArray(new ExecutionContext.LifecycleListener[executionContextListeners.size()]);
    }

    /**
     * Register a new Listener for ExecutionContext events.
     * @param listener the listener to register
     */
    public void addExecutionContextListener(ExecutionContext.LifecycleListener listener) {
        executionContextListeners.add(listener);
    }

    /**
     * Unregister a Listener from ExecutionContext events.
     * @param listener the listener to unregister
     */
    public void removeExecutionContextListener(ExecutionContext.LifecycleListener listener) {
        executionContextListeners.remove(listener);
    }

    /**
     * Mutator for whether we are in JCA mode.
     * @param jca true if using JCA connector
     */
    public synchronized void setJcaMode(boolean jca) {
        this.jca = jca;
    }

    /**
     * Accessor for the JCA mode.
     * @return true if using JCA connector.
     */
    public boolean isJcaMode() {
        return jca;
    }

    /** 
     * Convenience accessor for the FetchGroupManager.
     * Creates it if not yet existing.
     * @return The FetchGroupManager
     */
    public synchronized FetchGroupManager getFetchGroupManager() {
        if (fetchGrpMgr == null) {
            fetchGrpMgr = new FetchGroupManager(this);
        }
        return fetchGrpMgr;
    }

    /**
     * Method to add a dynamic FetchGroup for use by this OMF.
     * @param grp The group
     */
    public void addInternalFetchGroup(FetchGroup grp) {
        getFetchGroupManager().addFetchGroup(grp);
    }

    /**
     * Method to remove a dynamic FetchGroup from use by this OMF.
     * @param grp The group
     */
    public void removeInternalFetchGroup(FetchGroup grp) {
        getFetchGroupManager().removeFetchGroup(grp);
    }

    /**
     * Method to create a new internal fetch group for the class+name.
     * @param cls Class that it applies to
     * @param name Name of group
     * @return The group
     */
    public FetchGroup createInternalFetchGroup(Class cls, String name) {
        if (!cls.isInterface() && !getApiAdapter().isPersistable(cls)) {
            throw new NucleusUserException("Cannot create FetchGroup for " + cls + " since it is not persistable");
        } else if (cls.isInterface() && !getMetaDataManager().isPersistentInterface(cls.getName())) {
            throw new NucleusUserException("Cannot create FetchGroup for " + cls + " since it is not persistable");
        }
        return getFetchGroupManager().createFetchGroup(cls, name);
    }

    /**
     * Accessor for an internal fetch group for the specified class.
     * @param cls The class
     * @param name Name of the group
     * @return The FetchGroup
     * @throws NucleusUserException if the class is not persistable
     */
    public FetchGroup getInternalFetchGroup(Class cls, String name) {
        if (!cls.isInterface() && !getApiAdapter().isPersistable(cls)) {
            throw new NucleusUserException("Cannot create FetchGroup for " + cls + " since it is not persistable");
        } else if (cls.isInterface() && !getMetaDataManager().isPersistentInterface(cls.getName())) {
            throw new NucleusUserException("Cannot create FetchGroup for " + cls + " since it is not persistable");
        }
        return getFetchGroupManager().getFetchGroup(cls, name);
    }

    /**
     * Accessor for the fetch groups for the specified name.
     * @param name Name of the group
     * @return The FetchGroup
     */
    public Set<FetchGroup> getFetchGroupsWithName(String name) {
        return getFetchGroupManager().getFetchGroupsWithName(name);
    }
}

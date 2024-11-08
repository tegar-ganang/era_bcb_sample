package org.datanucleus;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import org.datanucleus.cache.Level2Cache;
import org.datanucleus.cache.NullLevel2Cache;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.exceptions.TransactionIsolationNotSupportedException;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.Extension;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.NucleusLogger;

/**
 * ObjectManagerFactory responsible for creation of ObjectManagers for persistence of objects to datastores.
 * Will typically be either extended or utilised by PersistenceManagerFactory (JDO) or EntityManagerFactory (JPA).
 */
public class ObjectManagerFactoryImpl extends PersistenceConfiguration {

    /** Version of DataNucleus being used. Read in at startup from properties. */
    private static String versionNumber = null;

    /** Vendor of this system. */
    private static String vendorName = null;

    /** The context that this ObjectManagerFactory uses. */
    protected OMFContext omfContext;

    /** Level 2 Cache, caching across ObjectManagers. */
    protected Level2Cache cache;

    /** Manager for dynamic fetch groups defined on the OMF. */
    private FetchGroupManager fetchGrpMgr;

    /**
     * Constructor.
     */
    public ObjectManagerFactoryImpl() {
        super();
    }

    /**
     * Method to log the configuration of this factory.
     */
    protected void logConfiguration() {
        NucleusLogger.PERSISTENCE.info("================= Persistence Configuration ===============");
        NucleusLogger.PERSISTENCE.info(LOCALISER.msg("008000", getVendorName(), getVersionNumber()));
        NucleusLogger.PERSISTENCE.info(LOCALISER.msg("008001", getStringProperty("datanucleus.ConnectionURL"), getStringProperty("datanucleus.ConnectionDriverName"), getStringProperty("datanucleus.ConnectionUserName")));
        if (NucleusLogger.PERSISTENCE.isDebugEnabled()) {
            NucleusLogger.PERSISTENCE.debug("JDK : " + System.getProperty("java.version") + " on " + System.getProperty("os.name"));
            NucleusLogger.PERSISTENCE.debug("Persistence API : " + getOMFContext().getApi());
            NucleusLogger.PERSISTENCE.debug("Plugin Registry : " + getOMFContext().getPluginManager().getRegistryClassName());
            if (hasProperty("datanucleus.PersistenceUnitName")) {
                NucleusLogger.PERSISTENCE.debug("Persistence-Unit : " + getStringProperty("datanucleus.PersistenceUnitName"));
            }
            String timeZoneID = getStringProperty("datanucleus.ServerTimeZoneID");
            if (timeZoneID == null) {
                timeZoneID = TimeZone.getDefault().getID();
            }
            NucleusLogger.PERSISTENCE.debug("Standard Options : " + (getBooleanProperty("datanucleus.Multithreaded") ? "multithreaded" : "singlethreaded") + (getBooleanProperty("datanucleus.RetainValues") ? ", retain-values" : "") + (getBooleanProperty("datanucleus.RestoreValues") ? ", restore-values" : "") + (getBooleanProperty("datanucleus.NontransactionalRead") ? ", nontransactional-read" : "") + (getBooleanProperty("datanucleus.NontransactionalWrite") ? ", nontransactional-write" : "") + (getBooleanProperty("datanucleus.IgnoreCache") ? ", ignoreCache" : "") + ", serverTimeZone=" + timeZoneID);
            NucleusLogger.PERSISTENCE.debug("Persistence Options :" + (getBooleanProperty("datanucleus.persistenceByReachabilityAtCommit") ? " reachability-at-commit" : "") + (getBooleanProperty("datanucleus.DetachAllOnCommit") ? " detach-all-on-commit" : "") + (getBooleanProperty("datanucleus.DetachAllOnRollback") ? " detach-all-on-rollback" : "") + (getBooleanProperty("datanucleus.DetachOnClose") ? " detach-on-close" : "") + (getBooleanProperty("datanucleus.manageRelationships") ? (getBooleanProperty("datanucleus.manageRelationshipsChecks") ? " managed-relations(checked)" : "managed-relations(unchecked)") : "") + " deletion-policy=" + getStringProperty("datanucleus.deletionPolicy"));
            NucleusLogger.PERSISTENCE.debug("Transactions : type=" + getStringProperty("datanucleus.TransactionType") + " mode=" + (getBooleanProperty("datanucleus.Optimistic") ? "optimistic" : "datastore") + " isolation=" + getStringProperty("datanucleus.transactionIsolation"));
            NucleusLogger.PERSISTENCE.debug("Value Generation :" + " txn-isolation=" + getStringProperty("datanucleus.valuegeneration.transactionIsolation") + " connection=" + (getStringProperty("datanucleus.valuegeneration.transactionAttribute").equalsIgnoreCase("New") ? "New" : "PM"));
            Object primCL = getProperty("datanucleus.primaryClassLoader");
            NucleusLogger.PERSISTENCE.debug("ClassLoading : " + getStringProperty("datanucleus.classLoaderResolverName") + (primCL != null ? ("primary=" + primCL) : ""));
            NucleusLogger.PERSISTENCE.debug("Cache : Level1 (" + getStringProperty("datanucleus.cache.level1.type") + ")" + ", Level2 (" + getStringProperty("datanucleus.cache.level2.type") + ")" + ", QueryResults (" + getStringProperty("datanucleus.cache.queryResults.type") + ")" + (getBooleanProperty("datanucleus.cache.collections") ? ", Collections/Maps " : ""));
        }
        NucleusLogger.PERSISTENCE.info("===========================================================");
    }

    /**
     * Method to initialise the OMFContext.
     * This should be performed after setting any persistence properties that affect the content
     * of the OMFContext (e.g PluginRegistry, ClassLoaderResolver, etc).
     */
    protected void initialiseOMFContext() {
        omfContext = new OMFContext(this);
    }

    /**
     * Method to initialise the StoreManager used by this factory.
     * @param clr ClassLoaderResolver to use for class loading issues
     */
    protected void initialiseStoreManager(ClassLoaderResolver clr) {
        StoreManager srm = null;
        String storeManagerType = omfContext.getPersistenceConfiguration().getStringProperty("datanucleus.storeManagerType");
        Extension[] exts = omfContext.getPluginManager().getExtensionPoint("org.datanucleus.store_manager").getExtensions();
        if (storeManagerType != null) {
            for (int e = 0; srm == null && e < exts.length; e++) {
                ConfigurationElement[] confElm = exts[e].getConfigurationElements();
                for (int c = 0; srm == null && c < confElm.length; c++) {
                    String key = confElm[c].getAttribute("key");
                    if (key.equalsIgnoreCase(storeManagerType)) {
                        Class[] ctrArgTypes = new Class[] { ClassLoaderResolver.class, OMFContext.class };
                        Object[] ctrArgs = new Object[] { clr, omfContext };
                        try {
                            srm = (StoreManager) omfContext.getPluginManager().createExecutableExtension("org.datanucleus.store_manager", "key", storeManagerType, "class-name", ctrArgTypes, ctrArgs);
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
            if (srm == null) {
                throw new NucleusUserException(LOCALISER.msg("008004", storeManagerType)).setFatal();
            }
        }
        if (srm == null) {
            String url = omfContext.getPersistenceConfiguration().getStringProperty("datanucleus.ConnectionURL");
            if (url != null) {
                int idx = url.indexOf(':');
                if (idx > -1) {
                    url = url.substring(0, idx);
                }
            }
            for (int e = 0; srm == null && e < exts.length; e++) {
                ConfigurationElement[] confElm = exts[e].getConfigurationElements();
                for (int c = 0; srm == null && c < confElm.length; c++) {
                    String urlKey = confElm[c].getAttribute("url-key");
                    if (url == null || urlKey.equalsIgnoreCase(url)) {
                        Class[] ctrArgTypes = new Class[] { ClassLoaderResolver.class, OMFContext.class };
                        Object[] ctrArgs = new Object[] { clr, omfContext };
                        try {
                            srm = (StoreManager) omfContext.getPluginManager().createExecutableExtension("org.datanucleus.store_manager", "url-key", url, "class-name", ctrArgTypes, ctrArgs);
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
            if (srm == null) {
                throw new NucleusUserException(LOCALISER.msg("008004", url)).setFatal();
            }
        }
        String transactionIsolation = omfContext.getPersistenceConfiguration().getStringProperty("datanucleus.transactionIsolation");
        if (transactionIsolation != null) {
            Collection srmOptions = srm.getSupportedOptions();
            if (!srmOptions.contains("TransactionIsolationLevel." + transactionIsolation)) {
                if (transactionIsolation.equals("read-uncommitted")) {
                    if (srmOptions.contains("TransactionIsolationLevel.read-committed")) {
                        transactionIsolation = "read-committed";
                    } else if (srmOptions.contains("TransactionIsolationLevel.repeatable-read")) {
                        transactionIsolation = "serializable";
                    } else if (srmOptions.contains("TransactionIsolationLevel.serializable")) {
                        transactionIsolation = "repeatable-read";
                    }
                } else if (transactionIsolation.equals("read-committed")) {
                    if (srmOptions.contains("TransactionIsolationLevel.repeatable-read")) {
                        transactionIsolation = "repeatable-read";
                    } else if (srmOptions.contains("TransactionIsolationLevel.serializable")) {
                        transactionIsolation = "serializable";
                    }
                } else if (transactionIsolation.equals("repeatable-read")) {
                    if (srmOptions.contains("TransactionIsolationLevel.serializable")) {
                        transactionIsolation = "serializable";
                    }
                } else {
                    throw new TransactionIsolationNotSupportedException(transactionIsolation);
                }
            }
        }
        String puName = omfContext.getPersistenceConfiguration().getStringProperty("datanucleus.PersistenceUnitName");
        if (puName != null) {
            boolean loadClasses = getBooleanProperty("datanucleus.persistenceUnitLoadClasses");
            if (loadClasses) {
                Collection<String> loadedClasses = omfContext.getMetaDataManager().getClassesWithMetaData();
                srm.addClasses(loadedClasses.toArray(new String[loadedClasses.size()]), clr);
            }
        }
    }

    /**
     * Convenience accessor for the StoreManager.
     * This is for public access to DataNucleus APIs.
     * TODO Rework this when there is a FederationManager.
     * @return The StoreManager
     */
    public StoreManager getStoreManager() {
        return getOMFContext().getStoreManager();
    }

    /**
     * Method to initialise the L2 cache.
     */
    protected void initialiseLevel2Cache() {
        String level2Type = getStringProperty("datanucleus.cache.level2.type");
        String level2ClassName = getOMFContext().getPluginManager().getAttributeValueForExtension("org.datanucleus.cache_level2", "name", level2Type, "class-name");
        if (level2ClassName == null) {
            throw new NucleusUserException(LOCALISER.msg("004000", level2Type)).setFatal();
        }
        try {
            cache = (Level2Cache) getOMFContext().getPluginManager().createExecutableExtension("org.datanucleus.cache_level2", "name", level2Type, "class-name", new Class[] { OMFContext.class }, new Object[] { omfContext });
            if (NucleusLogger.CACHE.isDebugEnabled()) {
                NucleusLogger.CACHE.debug(LOCALISER.msg("004002", level2Type));
            }
        } catch (Exception e) {
            throw new NucleusUserException(LOCALISER.msg("004001", level2Type, level2ClassName), e).setFatal();
        }
    }

    /**
     * Close the ObjectManagerFactory.
     * Cleans out all objects in the Level2 cache and closes the context, marking the factory as closed.
     */
    public synchronized void close() {
        if (cache != null) {
            cache.close();
            NucleusLogger.CACHE.info(LOCALISER.msg("004009"));
        }
        if (fetchGrpMgr != null) {
            getFetchGroupManager().clearFetchGroups();
        }
        if (omfContext != null) {
            omfContext.close();
            omfContext = null;
        }
    }

    /**
     * Gets the context for this ObjectManagerFactory
     * @return Returns the context.
     */
    public OMFContext getOMFContext() {
        if (omfContext == null) {
            initialiseOMFContext();
        }
        return omfContext;
    }

    /**
     * Accessor for the PersistenceConfiguration.
     * @return Returns the persistence config.
     */
    public PersistenceConfiguration getPersistenceConfiguration() {
        return this;
    }

    /**
     * Return whether there is an L2 cache.
     * @return Whether the L2 cache is enabled
     */
    public boolean hasLevel2Cache() {
        return (cache != null && !(cache instanceof NullLevel2Cache));
    }

    /**
     * Accessor for the DataStore (level 2) Cache
     * @return The datastore cache
     */
    public Level2Cache getLevel2Cache() {
        return cache;
    }

    /**
     * Utility to get the version of DataNucleus.
     * @return Version number for DataNucleus.
     */
    public static String getVersionNumber() {
        if (versionNumber != null) {
            return versionNumber;
        }
        String version = "Unknown";
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("org.datanucleus.DataNucleusVersion");
            try {
                version = bundle.getString("datanucleus.version");
            } catch (Exception e1) {
            }
        } catch (Exception e) {
        }
        return versionNumber = version;
    }

    /**
     * Utility to get the vendor of DataNucleus.
     * @return Vendor name for DataNucleus.
     */
    public static String getVendorName() {
        if (vendorName != null) {
            return vendorName;
        }
        String vendor = "DataNucleus";
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("org.datanucleus.DataNucleusVersion");
            try {
                vendor = bundle.getString("datanucleus.vendor");
            } catch (Exception e1) {
            }
        } catch (Exception e) {
        }
        return vendorName = vendor;
    }

    /** 
     * Convenience accessor for the FetchGroupManager.
     * Creates it if not yet existing.
     * @return The FetchGroupManager
     */
    protected synchronized FetchGroupManager getFetchGroupManager() {
        if (fetchGrpMgr == null) {
            fetchGrpMgr = new FetchGroupManager(getOMFContext());
        }
        return fetchGrpMgr;
    }

    /**
     * Method to add a dynamic FetchGroup for use by this OMF.
     * @param grp The group
     */
    protected void addInternalFetchGroup(FetchGroup grp) {
        getFetchGroupManager().addFetchGroup(grp);
    }

    /**
     * Method to remove a dynamic FetchGroup from use by this OMF.
     * @param grp The group
     */
    protected void removeInternalFetchGroup(FetchGroup grp) {
        getFetchGroupManager().removeFetchGroup(grp);
    }

    /**
     * Method to create a new internal fetch group for the class+name.
     * @param cls Class that it applies to
     * @param name Name of group
     * @return The group
     */
    protected FetchGroup createInternalFetchGroup(Class cls, String name) {
        if (!cls.isInterface() && !getOMFContext().getApiAdapter().isPersistable(cls)) {
            throw new NucleusUserException("Cannot create FetchGroup for " + cls + " since it is not persistable");
        } else if (cls.isInterface() && !getOMFContext().getMetaDataManager().isPersistentInterface(cls.getName())) {
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
    protected FetchGroup getInternalFetchGroup(Class cls, String name) {
        if (!cls.isInterface() && !getOMFContext().getApiAdapter().isPersistable(cls)) {
            throw new NucleusUserException("Cannot create FetchGroup for " + cls + " since it is not persistable");
        } else if (cls.isInterface() && !getOMFContext().getMetaDataManager().isPersistentInterface(cls.getName())) {
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

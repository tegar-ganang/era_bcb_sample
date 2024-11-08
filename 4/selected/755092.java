package org.datanucleus.store;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.Transaction;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.OID;
import org.datanucleus.identity.SCOID;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.ExtensionMetaData;
import org.datanucleus.metadata.IdentityMetaData;
import org.datanucleus.metadata.IdentityStrategy;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.SequenceMetaData;
import org.datanucleus.metadata.TableGeneratorMetaData;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.properties.PropertyStore;
import org.datanucleus.store.autostart.AutoStartMechanism;
import org.datanucleus.store.autostart.AutoStartMechanism.Mode;
import org.datanucleus.store.connection.ConnectionFactory;
import org.datanucleus.store.connection.ConnectionManager;
import org.datanucleus.store.connection.ConnectionManagerImpl;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.encryption.ConnectionEncryptionProvider;
import org.datanucleus.store.exceptions.DatastoreInitialisationException;
import org.datanucleus.store.exceptions.DatastoreReadOnlyException;
import org.datanucleus.store.exceptions.NoExtentException;
import org.datanucleus.store.query.QueryManager;
import org.datanucleus.store.schema.StoreSchemaHandler;
import org.datanucleus.store.schema.naming.DN2NamingFactory;
import org.datanucleus.store.schema.naming.JPANamingFactory;
import org.datanucleus.store.schema.naming.NamingCase;
import org.datanucleus.store.schema.naming.NamingFactory;
import org.datanucleus.store.valuegenerator.AbstractDatastoreGenerator;
import org.datanucleus.store.valuegenerator.ValueGenerationConnectionProvider;
import org.datanucleus.store.valuegenerator.ValueGenerationManager;
import org.datanucleus.store.valuegenerator.ValueGenerator;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;
import org.datanucleus.util.TypeConversionHelper;

/**
 * An abstract representation of a Store Manager.
 * Manages the persistence of objects to the store.
 * Will be implemented for the type of datastore (RDBMS, ODBMS, etc) in question. 
 * The store manager's responsibilities include:
 * <ul>
 * <li>Creating and/or validating datastore tables according to the persistent classes being 
 * accessed by the application.</li>
 * <li>Serving as the primary intermediary between StateManagers and the database.</li>
 * <li>Serving as the base Extent and Query factory.</li>
 * </ul>
 * <p>
 * A store manager's knowledge of its contents is typically not complete. It knows about
 * the classes that it has encountered in its lifetime. The PersistenceManager can make the
 * StoreManager aware of a class, and can check if the StoreManager knows about a particular class.
 * The Auto-Start mechanism provides a way of inheriting knowledge from the last time the store was used.
 */
public abstract class AbstractStoreManager extends PropertyStore implements StoreManager {

    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Key for this StoreManager e.g "rdbms", "db4o" */
    protected final String storeManagerKey;

    /** Whether this datastore is read only. */
    protected final boolean readOnlyDatastore;

    /** Whether this datastore is fixed (no mods to table structure allowed). */
    protected final boolean fixedDatastore;

    /** Whether to auto create any tables. */
    protected final boolean autoCreateTables;

    /** Whether to auto create any columns that are missing. */
    protected final boolean autoCreateColumns;

    /** Whether to auto create any constraints */
    protected final boolean autoCreateConstraints;

    /** Whether to warn only when any errors occur on auto-create. */
    protected final boolean autoCreateWarnOnError;

    /** Whether to validate any tables */
    protected final boolean validateTables;

    /** Whether to validate any columns */
    protected final boolean validateColumns;

    /** Whether to validate any constraints */
    protected final boolean validateConstraints;

    /** Auto-Start mechanism to use. */
    protected AutoStartMechanism starter = null;

    /** Whether the AutoStart mechanism is initialised */
    protected boolean starterInitialised = false;

    /** Nucleus Context. */
    protected final NucleusContext nucleusContext;

    /** Manager for value generation. Lazy initialised, so use getValueGenerationManager() to access. */
    private ValueGenerationManager valueGenerationMgr;

    /** Manager for the data definition in the datastore. */
    protected StoreDataManager storeDataMgr = new StoreDataManager();

    /** Name of the AutoStart mechanism. */
    protected String autoStartMechanism = null;

    /** Persistence handler. */
    protected StorePersistenceHandler persistenceHandler = null;

    /** Query Manager. Lazy initialised, so use getQueryManager() to access. */
    private QueryManager queryMgr = null;

    /** Schema handler. */
    protected StoreSchemaHandler schemaHandler = null;

    /** Naming factory. */
    protected NamingFactory namingFactory = null;

    /** ConnectionManager **/
    protected ConnectionManager connectionMgr;

    /** Name of transactional connection factory. */
    protected String txConnectionFactoryName;

    /** Name of non-transactional connection factory (null if not present). */
    protected String nontxConnectionFactoryName;

    /**
     * Constructor for a new StoreManager. Stores the basic information required for the datastore management.
     * @param key Key for this StoreManager
     * @param clr the ClassLoaderResolver
     * @param nucleusContext The corresponding nucleus context.
     * @param props Any properties controlling this datastore
     */
    protected AbstractStoreManager(String key, ClassLoaderResolver clr, NucleusContext nucleusContext, Map<String, Object> props) {
        this.storeManagerKey = key;
        this.nucleusContext = nucleusContext;
        if (props != null) {
            Iterator<Map.Entry<String, Object>> propIter = props.entrySet().iterator();
            while (propIter.hasNext()) {
                Map.Entry<String, Object> entry = propIter.next();
                setPropertyInternal(entry.getKey(), entry.getValue());
            }
        }
        this.readOnlyDatastore = getBooleanProperty(PropertyNames.PROPERTY_DATASTORE_READONLY);
        this.fixedDatastore = getBooleanProperty(PropertyNames.PROPERTY_DATASTORE_FIXED);
        if (readOnlyDatastore || fixedDatastore) {
            autoCreateTables = false;
            autoCreateColumns = false;
            autoCreateConstraints = false;
        } else {
            boolean autoCreateSchema = getBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_SCHEMA);
            if (autoCreateSchema) {
                autoCreateTables = true;
                autoCreateColumns = true;
                autoCreateConstraints = true;
            } else {
                autoCreateTables = getBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_TABLES);
                autoCreateColumns = getBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_COLUMNS);
                autoCreateConstraints = getBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_CONSTRAINTS);
            }
        }
        autoCreateWarnOnError = getBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_WARNONERROR);
        boolean validateSchema = getBooleanProperty(PropertyNames.PROPERTY_VALIDATE_SCHEMA);
        if (validateSchema) {
            validateTables = true;
            validateColumns = true;
            validateConstraints = true;
        } else {
            validateTables = getBooleanProperty(PropertyNames.PROPERTY_VALIDATE_TABLES);
            if (!validateTables) {
                validateColumns = false;
            } else {
                validateColumns = getBooleanProperty(PropertyNames.PROPERTY_VALIDATE_COLUMNS);
            }
            validateConstraints = getBooleanProperty(PropertyNames.PROPERTY_VALIDATE_CONSTRAINTS);
        }
        autoStartMechanism = getStringProperty(PropertyNames.PROPERTY_AUTOSTART_MECHANISM);
        registerConnectionMgr();
        registerConnectionFactory();
        nucleusContext.addExecutionContextListener(new ExecutionContext.LifecycleListener() {

            public void preClose(ExecutionContext ec) {
                ConnectionFactory connFactory = connectionMgr.lookupConnectionFactory(txConnectionFactoryName);
                connectionMgr.closeAllConnections(connFactory, ec);
                connFactory = connectionMgr.lookupConnectionFactory(nontxConnectionFactoryName);
                connectionMgr.closeAllConnections(connFactory, ec);
            }
        });
    }

    /**
     * Register the default ConnectionManager implementation
     */
    protected void registerConnectionMgr() {
        this.connectionMgr = new ConnectionManagerImpl(nucleusContext);
    }

    /**
     * Register the Connection Factory defined in plugins
     */
    protected void registerConnectionFactory() {
        ConfigurationElement cfElem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_connectionfactory", new String[] { "datastore", "transactional" }, new String[] { storeManagerKey, "true" });
        if (cfElem != null) {
            txConnectionFactoryName = cfElem.getAttribute("name");
            try {
                ConnectionFactory cf = (ConnectionFactory) nucleusContext.getPluginManager().createExecutableExtension("org.datanucleus.store_connectionfactory", new String[] { "datastore", "transactional" }, new String[] { storeManagerKey, "true" }, "class-name", new Class[] { StoreManager.class, String.class }, new Object[] { this, "tx" });
                connectionMgr.registerConnectionFactory(txConnectionFactoryName, cf);
                if (NucleusLogger.CONNECTION.isDebugEnabled()) {
                    NucleusLogger.CONNECTION.debug(LOCALISER.msg("032018", txConnectionFactoryName));
                }
            } catch (Exception e) {
                throw new NucleusException("Error creating transactional connection factory", e).setFatal();
            }
        } else {
            throw new NucleusException("Error creating transactional connection factory. No connection factory plugin defined");
        }
        cfElem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_connectionfactory", new String[] { "datastore", "transactional" }, new String[] { storeManagerKey, "false" });
        if (cfElem != null) {
            nontxConnectionFactoryName = cfElem.getAttribute("name");
            try {
                ConnectionFactory cf = (ConnectionFactory) nucleusContext.getPluginManager().createExecutableExtension("org.datanucleus.store_connectionfactory", new String[] { "datastore", "transactional" }, new String[] { storeManagerKey, "false" }, "class-name", new Class[] { StoreManager.class, String.class }, new Object[] { this, "nontx" });
                if (NucleusLogger.CONNECTION.isDebugEnabled()) {
                    NucleusLogger.CONNECTION.debug(LOCALISER.msg("032019", nontxConnectionFactoryName));
                }
                connectionMgr.registerConnectionFactory(nontxConnectionFactoryName, cf);
            } catch (Exception e) {
                throw new NucleusException("Error creating nontransactional connection factory", e).setFatal();
            }
        }
    }

    public synchronized void close() {
        if (txConnectionFactoryName != null) {
            ConnectionFactory cf = connectionMgr.lookupConnectionFactory(txConnectionFactoryName);
            if (cf != null) {
                cf.close();
            }
        }
        if (nontxConnectionFactoryName != null) {
            ConnectionFactory cf = connectionMgr.lookupConnectionFactory(nontxConnectionFactoryName);
            if (cf != null) {
                cf.close();
            }
        }
        if (valueGenerationMgr != null) {
            valueGenerationMgr.clear();
        }
        storeDataMgr.clear();
        starterInitialised = false;
        starter = null;
        if (persistenceHandler != null) {
            persistenceHandler.close();
            persistenceHandler = null;
        }
        if (queryMgr != null) {
            queryMgr.close();
            queryMgr = null;
        }
    }

    public ConnectionManager getConnectionManager() {
        return connectionMgr;
    }

    /**
     * Accessor for a connection for the specified ObjectManager.<p>
     * If there is an active transaction, a connection from the transactional
     * connection factory will be returned. If there is no active transaction,
     * a connection from the nontransactional connection factory will be returned.
     * @param ec execution context
     * @return The Connection
     * @throws NucleusException Thrown if an error occurs getting the connection
     */
    public ManagedConnection getConnection(ExecutionContext ec) {
        return getConnection(ec, null);
    }

    /**
     * Accessor for a connection for the specified ObjectManager.<p>
     * If there is an active transaction, a connection from the transactional
     * connection factory will be returned. If there is no active transaction,
     * a connection from the nontransactional connection factory will be returned.
     * @param ec execution context
     * @return The Connection
     * @throws NucleusException Thrown if an error occurs getting the connection
     */
    public ManagedConnection getConnection(ExecutionContext ec, Map options) {
        ConnectionFactory connFactory;
        if (ec.getTransaction().isActive()) {
            connFactory = connectionMgr.lookupConnectionFactory(txConnectionFactoryName);
        } else {
            if (nontxConnectionFactoryName != null) {
                connFactory = connectionMgr.lookupConnectionFactory(nontxConnectionFactoryName);
            } else {
                connFactory = connectionMgr.lookupConnectionFactory(txConnectionFactoryName);
            }
        }
        return connFactory.getConnection(ec, ec.getTransaction(), options);
    }

    /**
     * Utility to return a non-transactional Connection not tied to any ExecutionContext.
     * This returns a connection from a secondary DataSource (e.g. javax.jdo.option.connectionFactory2Name),
     * if it is provided.
     * @param isolation_level The transaction isolation scheme to use e.g Connection.TRANSACTION_NONE
     *     Pass in -1 if just want the default
     * @return The Connection to the datastore
     * @throws NucleusException if an error occurs getting the connection
     */
    public ManagedConnection getConnection(int isolation_level) {
        ConnectionFactory connFactory = null;
        if (nontxConnectionFactoryName != null) {
            connFactory = connectionMgr.lookupConnectionFactory(nontxConnectionFactoryName);
        } else {
            connFactory = connectionMgr.lookupConnectionFactory(txConnectionFactoryName);
        }
        Map options = null;
        if (isolation_level >= 0) {
            options = new HashMap();
            options.put(Transaction.TRANSACTION_ISOLATION_OPTION, Integer.valueOf(isolation_level));
        }
        return connFactory.getConnection(null, null, options);
    }

    /**
     * Convenience accessor for the driver name to use for the connection (if applicable for this datastore).
     * @return Driver name for the connection
     */
    public String getConnectionDriverName() {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_DRIVER_NAME);
    }

    /**
     * Convenience accessor for the URL for the connection
     * @return Connection URL
     */
    public String getConnectionURL() {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_URL);
    }

    /**
     * Convenience accessor for the username to use for the connection
     * @return Username
     */
    public String getConnectionUserName() {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_USER_NAME);
    }

    /**
     * Convenience accessor for the password to use for the connection.
     * Will perform decryption if the persistence property "datanucleus.ConnectionPasswordDecrypter" has
     * also been specified.
     * @return Password
     */
    public String getConnectionPassword() {
        String password = getStringProperty(PropertyNames.PROPERTY_CONNECTION_PASSWORD);
        if (password != null) {
            String decrypterName = getStringProperty(PropertyNames.PROPERTY_CONNECTION_PASSWORD_DECRYPTER);
            if (decrypterName != null) {
                ClassLoaderResolver clr = nucleusContext.getClassLoaderResolver(null);
                try {
                    Class decrypterCls = clr.classForName(decrypterName);
                    ConnectionEncryptionProvider decrypter = (ConnectionEncryptionProvider) decrypterCls.newInstance();
                    password = decrypter.decrypt(password);
                } catch (Exception e) {
                    NucleusLogger.DATASTORE.warn("Error invoking decrypter class " + decrypterName, e);
                }
            }
        }
        return password;
    }

    /**
     * Convenience accessor for the factory for the connection (transactional).
     * @return Connection Factory (transactional)
     */
    public Object getConnectionFactory() {
        return getProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY);
    }

    /**
     * Convenience accessor for the factory name for the connection (transactional).
     * @return Connection Factory name (transactional)
     */
    public String getConnectionFactoryName() {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY_NAME);
    }

    /**
     * Convenience accessor for the factory for the connection (non-transactional).
     * @return Connection Factory (non-transactional)
     */
    public Object getConnectionFactory2() {
        return getProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY2);
    }

    /**
     * Convenience accessor for the factory name for the connection (non-transactional).
     * @return Connection Factory name (non-transactional)
     */
    public String getConnectionFactory2Name() {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY2_NAME);
    }

    public boolean isAutoCreateTables() {
        return autoCreateTables;
    }

    public boolean isAutoCreateColumns() {
        return autoCreateColumns;
    }

    public boolean isAutoCreateConstraints() {
        return autoCreateConstraints;
    }

    public boolean isValidateTables() {
        return validateTables;
    }

    public boolean isValidateColumns() {
        return validateColumns;
    }

    public boolean isValidateConstraints() {
        return validateConstraints;
    }

    public StorePersistenceHandler getPersistenceHandler() {
        return persistenceHandler;
    }

    public QueryManager getQueryManager() {
        if (queryMgr == null) {
            queryMgr = new QueryManager(nucleusContext, this);
        }
        return queryMgr;
    }

    public StoreSchemaHandler getSchemaHandler() {
        return schemaHandler;
    }

    public NamingFactory getNamingFactory() {
        if (namingFactory == null) {
            if (nucleusContext.getApiName().equalsIgnoreCase("JPA")) {
                namingFactory = new JPANamingFactory(nucleusContext);
            } else {
                namingFactory = new DN2NamingFactory(nucleusContext);
            }
            String identifierCase = getStringProperty(PropertyNames.PROPERTY_IDENTIFIER_CASE);
            if (identifierCase != null) {
                if (identifierCase.equalsIgnoreCase("lowercase")) {
                    namingFactory.setNamingCase(NamingCase.LOWER_CASE);
                } else if (identifierCase.equalsIgnoreCase("UPPERCASE")) {
                    namingFactory.setNamingCase(NamingCase.UPPER_CASE);
                } else {
                    namingFactory.setNamingCase(NamingCase.MIXED_CASE);
                }
            }
        }
        return namingFactory;
    }

    /**
     * Method to return a datastore sequence for this datastore matching the passed sequence MetaData.
     * @param ec execution context
     * @param seqmd SequenceMetaData
     * @return The Sequence
     */
    public NucleusSequence getNucleusSequence(ExecutionContext ec, SequenceMetaData seqmd) {
        return new NucleusSequenceImpl(ec, this, seqmd);
    }

    public NucleusConnection getNucleusConnection(ExecutionContext ec) {
        ConnectionFactory cf = connectionMgr.lookupConnectionFactory(txConnectionFactoryName);
        final ManagedConnection mc;
        final boolean enlisted;
        if (!ec.getTransaction().isActive()) {
            enlisted = false;
        } else {
            enlisted = true;
        }
        mc = cf.getConnection(enlisted ? ec : null, enlisted ? ec.getTransaction() : null, null);
        mc.lock();
        Runnable closeRunnable = new Runnable() {

            public void run() {
                mc.unlock();
                if (!enlisted) {
                    mc.close();
                }
            }
        };
        return new NucleusConnectionImpl(mc.getConnection(), closeRunnable);
    }

    public ValueGenerationManager getValueGenerationManager() {
        if (valueGenerationMgr == null) {
            this.valueGenerationMgr = new ValueGenerationManager();
        }
        return valueGenerationMgr;
    }

    public ApiAdapter getApiAdapter() {
        return nucleusContext.getApiAdapter();
    }

    public String getStoreManagerKey() {
        return storeManagerKey;
    }

    public String getQueryCacheKey() {
        return getStoreManagerKey();
    }

    public NucleusContext getNucleusContext() {
        return nucleusContext;
    }

    public MetaDataManager getMetaDataManager() {
        return nucleusContext.getMetaDataManager();
    }

    public Date getDatastoreDate() {
        throw new UnsupportedOperationException();
    }

    /**
     * Method to register some data with the store.
     * This will also register the data with the starter process.
     * @param data The StoreData to add
     */
    protected void registerStoreData(StoreData data) {
        storeDataMgr.registerStoreData(data);
        if (starter != null && starterInitialised) {
            starter.addClass(data);
        }
    }

    /**
     * Method to deregister all existing store data so that we are managing nothing.
     */
    protected void deregisterAllStoreData() {
        storeDataMgr.clear();
        starterInitialised = false;
        clearAutoStarter();
    }

    /**
     * Convenience method to log the configuration of this store manager.
     */
    protected void logConfiguration() {
        if (NucleusLogger.DATASTORE.isDebugEnabled()) {
            NucleusLogger.DATASTORE.debug("======================= Datastore =========================");
            NucleusLogger.DATASTORE.debug("StoreManager : \"" + storeManagerKey + "\" (" + getClass().getName() + ")");
            if (autoStartMechanism != null) {
                String classNames = getStringProperty(PropertyNames.PROPERTY_AUTOSTART_CLASSNAMES);
                NucleusLogger.DATASTORE.debug("AutoStart : mechanism=" + autoStartMechanism + ", mode=" + getStringProperty(PropertyNames.PROPERTY_AUTOSTART_MODE) + ((classNames != null) ? (", classes=" + classNames) : ""));
            }
            NucleusLogger.DATASTORE.debug("Datastore : " + (readOnlyDatastore ? "read-only" : "read-write") + (fixedDatastore ? ", fixed" : "") + (getBooleanProperty(PropertyNames.PROPERTY_SERIALIZE_READ) ? ", useLocking" : ""));
            StringBuffer autoCreateOptions = null;
            if (autoCreateTables || autoCreateColumns || autoCreateConstraints) {
                autoCreateOptions = new StringBuffer();
                boolean first = true;
                if (autoCreateTables) {
                    if (!first) {
                        autoCreateOptions.append(",");
                    }
                    autoCreateOptions.append("Tables");
                    first = false;
                }
                if (autoCreateColumns) {
                    if (!first) {
                        autoCreateOptions.append(",");
                    }
                    autoCreateOptions.append("Columns");
                    first = false;
                }
                if (autoCreateConstraints) {
                    if (!first) {
                        autoCreateOptions.append(",");
                    }
                    autoCreateOptions.append("Constraints");
                    first = false;
                }
            }
            StringBuffer validateOptions = null;
            if (validateTables || validateColumns || validateConstraints) {
                validateOptions = new StringBuffer();
                boolean first = true;
                if (validateTables) {
                    validateOptions.append("Tables");
                    first = false;
                }
                if (validateColumns) {
                    if (!first) {
                        validateOptions.append(",");
                    }
                    validateOptions.append("Columns");
                    first = false;
                }
                if (validateConstraints) {
                    if (!first) {
                        validateOptions.append(",");
                    }
                    validateOptions.append("Constraints");
                    first = false;
                }
            }
            NucleusLogger.DATASTORE.debug("Schema Control : " + "AutoCreate(" + (autoCreateOptions != null ? autoCreateOptions.toString() : "None") + ")" + ", Validate(" + (validateOptions != null ? validateOptions.toString() : "None") + ")");
            String[] queryLanguages = nucleusContext.getPluginManager().getAttributeValuesForExtension("org.datanucleus.store_query_query", "datastore", storeManagerKey, "name");
            NucleusLogger.DATASTORE.debug("Query Languages : " + (queryLanguages != null ? StringUtils.objectArrayToString(queryLanguages) : "none"));
            NucleusLogger.DATASTORE.debug("Queries : Timeout=" + getIntProperty(PropertyNames.PROPERTY_DATASTORE_READ_TIMEOUT));
            NucleusLogger.DATASTORE.debug("===========================================================");
        }
    }

    /**
     * Method to output the information about the StoreManager.
     * Supports the category "DATASTORE".
     */
    public void printInformation(String category, PrintStream ps) throws Exception {
        if (category.equalsIgnoreCase("DATASTORE")) {
            ps.println(LOCALISER.msg("032020", storeManagerKey, getConnectionURL(), (readOnlyDatastore ? "read-only" : "read-write"), (fixedDatastore ? ", fixed" : "")));
        }
    }

    /**
     * Method to initialise the auto-start mechanism, loading up the classes
     * from its store into memory so that we start from where we got to last time.
     * Utilises the "autoStartMechanism" field that should have been set before now.
     * @param clr The ClassLoaderResolver
     * @throws DatastoreInitialisationException
     */
    protected void initialiseAutoStart(ClassLoaderResolver clr) throws DatastoreInitialisationException {
        if (starterInitialised) {
            return;
        }
        if (autoStartMechanism == null) {
            autoStartMechanism = "None";
            starterInitialised = true;
            return;
        }
        String autoStarterClassName = getNucleusContext().getPluginManager().getAttributeValueForExtension("org.datanucleus.autostart", "name", autoStartMechanism, "class-name");
        if (autoStarterClassName != null) {
            String mode = getStringProperty(PropertyNames.PROPERTY_AUTOSTART_MODE);
            Class[] argsClass = new Class[] { ClassConstants.STORE_MANAGER, ClassConstants.CLASS_LOADER_RESOLVER };
            Object[] args = new Object[] { this, clr };
            try {
                starter = (AutoStartMechanism) getNucleusContext().getPluginManager().createExecutableExtension("org.datanucleus.autostart", "name", autoStartMechanism, "class-name", argsClass, args);
                if (mode.equalsIgnoreCase("None")) {
                    starter.setMode(Mode.NONE);
                } else if (mode.equalsIgnoreCase("Checked")) {
                    starter.setMode(Mode.CHECKED);
                } else if (mode.equalsIgnoreCase("Quiet")) {
                    starter.setMode(Mode.QUIET);
                } else if (mode.equalsIgnoreCase("Ignored")) {
                    starter.setMode(Mode.IGNORED);
                }
            } catch (Exception e) {
                NucleusLogger.PERSISTENCE.error(StringUtils.getStringFromStackTrace(e));
            }
        }
        if (starter == null) {
            starterInitialised = true;
            return;
        }
        boolean illegalState = false;
        try {
            if (!starter.isOpen()) {
                starter.open();
            }
            Collection existingData = starter.getAllClassData();
            if (existingData != null && existingData.size() > 0) {
                List classesNeedingAdding = new ArrayList();
                Iterator existingDataIter = existingData.iterator();
                while (existingDataIter.hasNext()) {
                    StoreData data = (StoreData) existingDataIter.next();
                    if (data.isFCO()) {
                        Class classFound = null;
                        try {
                            classFound = clr.classForName(data.getName());
                        } catch (ClassNotResolvedException cnre) {
                            if (data.getInterfaceName() != null) {
                                try {
                                    getNucleusContext().getImplementationCreator().newInstance(clr.classForName(data.getInterfaceName()), clr);
                                    classFound = clr.classForName(data.getName());
                                } catch (ClassNotResolvedException cnre2) {
                                }
                            }
                        }
                        if (classFound != null) {
                            NucleusLogger.PERSISTENCE.info(LOCALISER.msg("032003", data.getName()));
                            classesNeedingAdding.add(data.getName());
                            if (data.getMetaData() == null) {
                                AbstractClassMetaData acmd = getMetaDataManager().getMetaDataForClass(classFound, clr);
                                if (acmd != null) {
                                    data.setMetaData(acmd);
                                } else {
                                    String msg = LOCALISER.msg("034004", data.getName());
                                    if (starter.getMode() == AutoStartMechanism.Mode.CHECKED) {
                                        NucleusLogger.PERSISTENCE.error(msg);
                                        throw new DatastoreInitialisationException(msg);
                                    } else if (starter.getMode() == AutoStartMechanism.Mode.IGNORED) {
                                        NucleusLogger.PERSISTENCE.warn(msg);
                                    } else if (starter.getMode() == AutoStartMechanism.Mode.QUIET) {
                                        NucleusLogger.PERSISTENCE.warn(msg);
                                        NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("034001", data.getName()));
                                        starter.deleteClass(data.getName());
                                    }
                                }
                            }
                        } else {
                            String msg = LOCALISER.msg("034000", data.getName());
                            if (starter.getMode() == AutoStartMechanism.Mode.CHECKED) {
                                NucleusLogger.PERSISTENCE.error(msg);
                                throw new DatastoreInitialisationException(msg);
                            } else if (starter.getMode() == AutoStartMechanism.Mode.IGNORED) {
                                NucleusLogger.PERSISTENCE.warn(msg);
                            } else if (starter.getMode() == AutoStartMechanism.Mode.QUIET) {
                                NucleusLogger.PERSISTENCE.warn(msg);
                                NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("034001", data.getName()));
                                starter.deleteClass(data.getName());
                            }
                        }
                    }
                }
                String[] classesToLoad = new String[classesNeedingAdding.size()];
                Iterator classesNeedingAddingIter = classesNeedingAdding.iterator();
                int n = 0;
                while (classesNeedingAddingIter.hasNext()) {
                    classesToLoad[n++] = (String) classesNeedingAddingIter.next();
                }
                try {
                    addClasses(classesToLoad, clr);
                } catch (Exception e) {
                    NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("034002", e));
                    illegalState = true;
                }
            }
        } finally {
            if (starter.isOpen()) {
                starter.close();
            }
            if (illegalState) {
                NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("034003"));
                starter = null;
            }
        }
        starterInitialised = true;
    }

    /**
     * Method to clear the Auto-Starter status.
     */
    protected void clearAutoStarter() {
        if (starter != null) {
            try {
                if (!starter.isOpen()) {
                    starter.open();
                }
                starter.deleteAllClasses();
            } finally {
                if (starter.isOpen()) {
                    starter.close();
                }
            }
        }
    }

    public boolean managesClass(String className) {
        return storeDataMgr.managesClass(className);
    }

    public void addClass(String className, ClassLoaderResolver clr) {
        addClasses(new String[] { className }, clr);
    }

    public void addClasses(String[] classNames, ClassLoaderResolver clr) {
        if (classNames == null) {
            return;
        }
        String[] filteredClassNames = getNucleusContext().getTypeManager().filterOutSupportedSecondClassNames(classNames);
        Iterator iter = getMetaDataManager().getReferencedClasses(filteredClassNames, clr).iterator();
        while (iter.hasNext()) {
            ClassMetaData cmd = (ClassMetaData) iter.next();
            if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE) {
                if (!storeDataMgr.managesClass(cmd.getFullClassName())) {
                    registerStoreData(newStoreData(cmd, clr));
                }
            }
        }
    }

    /**
     * Instantiate a StoreData instance using the provided ClassMetaData and ClassLoaderResolver. 
     * Override this method if you want to instantiate a subclass of StoreData.
     * @param cmd MetaData for the class
     * @param clr ClassLoader resolver
     */
    protected StoreData newStoreData(ClassMetaData cmd, ClassLoaderResolver clr) {
        return new StoreData(cmd.getFullClassName(), cmd, StoreData.FCO_TYPE, null);
    }

    public void removeAllClasses(ClassLoaderResolver clr) {
    }

    public String manageClassForIdentity(Object id, ClassLoaderResolver clr) {
        String className = null;
        if (id instanceof OID) {
            className = ((OID) id).getPcClass();
            AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(className, clr);
            if (cmd.getIdentityType() != IdentityType.DATASTORE) {
                throw new NucleusUserException(LOCALISER.msg("038001", id, cmd.getFullClassName()));
            }
        } else if (getApiAdapter().isSingleFieldIdentity(id)) {
            className = getApiAdapter().getTargetClassNameForSingleFieldIdentity(id);
            AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(className, clr);
            if (cmd.getIdentityType() != IdentityType.APPLICATION || !cmd.getObjectidClass().equals(id.getClass().getName())) {
                throw new NucleusUserException(LOCALISER.msg("038001", id, cmd.getFullClassName()));
            }
        } else {
            throw new NucleusException("StoreManager.manageClassForIdentity called for id=" + id + " yet should only be called for datastore-identity/SingleFieldIdentity");
        }
        if (!managesClass(className)) {
            addClass(className, clr);
        }
        return className;
    }

    public Extent getExtent(ExecutionContext ec, Class c, boolean subclasses) {
        AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(c, ec.getClassLoaderResolver());
        if (!cmd.isRequiresExtent()) {
            throw new NoExtentException(c.getName());
        }
        if (!managesClass(c.getName())) {
            addClass(c.getName(), ec.getClassLoaderResolver());
        }
        return new DefaultCandidateExtent(ec, c, subclasses, cmd);
    }

    public boolean supportsQueryLanguage(String language) {
        if (language == null) {
            return false;
        }
        String name = getNucleusContext().getPluginManager().getAttributeValueForExtension("org.datanucleus.store_query_query", new String[] { "name", "datastore" }, new String[] { language, storeManagerKey }, "name");
        return (name != null);
    }

    /**
     * Accessor for whether this value strategy is supported.
     * @param strategy The strategy
     * @return Whether it is supported.
     */
    public boolean supportsValueStrategy(String strategy) {
        ConfigurationElement elem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_valuegenerator", new String[] { "name", "unique" }, new String[] { strategy, "true" });
        if (elem != null) {
            return true;
        } else {
            elem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_valuegenerator", new String[] { "name", "datastore" }, new String[] { strategy, storeManagerKey });
            if (elem != null) {
                return true;
            }
        }
        return false;
    }

    public String getClassNameForObjectID(Object id, ClassLoaderResolver clr, ExecutionContext ec) {
        if (id == null) {
            return null;
        } else if (id instanceof SCOID) {
            return ((SCOID) id).getSCOClass();
        } else if (id instanceof OID) {
            return ((OID) id).getPcClass();
        } else if (getApiAdapter().isSingleFieldIdentity(id)) {
            return getApiAdapter().getTargetClassNameForSingleFieldIdentity(id);
        } else {
            Collection<AbstractClassMetaData> cmds = getMetaDataManager().getClassMetaDataWithApplicationId(id.getClass().getName());
            if (cmds != null) {
                Iterator<AbstractClassMetaData> iter = cmds.iterator();
                while (iter.hasNext()) {
                    AbstractClassMetaData cmd = iter.next();
                    return cmd.getFullClassName();
                }
            }
            return null;
        }
    }

    /**
     * Method to return if a particular value strategy is attributed by the datastore.
     * In this implementation we return true for IDENTITY, and false for others.
     * Override in subclass if the datastore will attribute e.g SEQUENCE in the datastore.
     * @param identityStrategy The strategy
     * @param datastoreIdentityField Whether this is a datastore id field
     * @return Whether the strategy is attributed in the datastore
     */
    public boolean isStrategyDatastoreAttributed(IdentityStrategy identityStrategy, boolean datastoreIdentityField) {
        if (identityStrategy == null) {
            return false;
        }
        if (identityStrategy == IdentityStrategy.IDENTITY) {
            return true;
        }
        return false;
    }

    public Object getStrategyValue(ExecutionContext ec, AbstractClassMetaData cmd, int absoluteFieldNumber) {
        AbstractMemberMetaData mmd = null;
        String fieldName = null;
        IdentityStrategy strategy = null;
        String sequence = null;
        String valueGeneratorName = null;
        TableGeneratorMetaData tableGeneratorMetaData = null;
        SequenceMetaData sequenceMetaData = null;
        if (absoluteFieldNumber >= 0) {
            mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(absoluteFieldNumber);
            fieldName = mmd.getFullFieldName();
            strategy = mmd.getValueStrategy();
            sequence = mmd.getSequence();
            valueGeneratorName = mmd.getValueGeneratorName();
        } else {
            fieldName = cmd.getFullClassName() + " (datastore id)";
            strategy = cmd.getIdentityMetaData().getValueStrategy();
            sequence = cmd.getIdentityMetaData().getSequence();
            valueGeneratorName = cmd.getIdentityMetaData().getValueGeneratorName();
        }
        if (valueGeneratorName != null) {
            if (strategy == IdentityStrategy.INCREMENT) {
                tableGeneratorMetaData = getMetaDataManager().getMetaDataForTableGenerator(ec.getClassLoaderResolver(), valueGeneratorName);
                if (tableGeneratorMetaData == null) {
                    throw new NucleusUserException(LOCALISER.msg("038005", fieldName, valueGeneratorName));
                }
            } else if (strategy == IdentityStrategy.SEQUENCE) {
                sequenceMetaData = getMetaDataManager().getMetaDataForSequence(ec.getClassLoaderResolver(), valueGeneratorName);
                if (sequenceMetaData == null) {
                    throw new NucleusUserException(LOCALISER.msg("038006", fieldName, valueGeneratorName));
                }
            }
        } else if (strategy == IdentityStrategy.SEQUENCE && sequence != null) {
            sequenceMetaData = getMetaDataManager().getMetaDataForSequence(ec.getClassLoaderResolver(), sequence);
            if (sequenceMetaData == null) {
                NucleusLogger.VALUEGENERATION.warn("Field " + fieldName + " has been specified to use sequence " + sequence + " but there is no <sequence> specified in the MetaData. " + "Falling back to use a sequence in the datastore with this name directly.");
            }
        }
        String strategyName = strategy.toString();
        if (strategy.equals(IdentityStrategy.CUSTOM)) {
            strategyName = strategy.getCustomName();
        } else if (strategy.equals(IdentityStrategy.NATIVE)) {
            strategyName = getStrategyForNative(cmd, absoluteFieldNumber);
        }
        String generatorName = null;
        String generatorNameKeyInManager = null;
        ConfigurationElement elem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_valuegenerator", new String[] { "name", "unique" }, new String[] { strategyName, "true" });
        if (elem != null) {
            generatorName = elem.getAttribute("name");
            generatorNameKeyInManager = generatorName;
        } else {
            elem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_valuegenerator", new String[] { "name", "datastore" }, new String[] { strategyName, storeManagerKey });
            if (elem != null) {
                generatorName = elem.getAttribute("name");
            }
        }
        if (generatorNameKeyInManager == null) {
            if (absoluteFieldNumber >= 0) {
                generatorNameKeyInManager = mmd.getFullFieldName();
            } else {
                generatorNameKeyInManager = cmd.getBaseAbstractClassMetaData().getFullClassName();
            }
        }
        ValueGenerator generator = null;
        synchronized (this) {
            generator = getValueGenerationManager().getValueGenerator(generatorNameKeyInManager);
            if (generator == null) {
                if (generatorName == null) {
                    throw new NucleusUserException(LOCALISER.msg("038004", strategy));
                }
                Properties props = getPropertiesForGenerator(cmd, absoluteFieldNumber, ec, sequenceMetaData, tableGeneratorMetaData);
                Class cls = null;
                if (elem != null) {
                    cls = nucleusContext.getPluginManager().loadClass(elem.getExtension().getPlugin().getSymbolicName(), elem.getAttribute("class-name"));
                }
                if (cls == null) {
                    throw new NucleusException("Cannot create Value Generator for strategy " + generatorName);
                }
                generator = getValueGenerationManager().createValueGenerator(generatorNameKeyInManager, cls, props, this, null);
            }
        }
        Object oid = getStrategyValueForGenerator(generator, ec);
        if (mmd != null) {
            try {
                Object convertedValue = TypeConversionHelper.convertTo(oid, mmd.getType());
                if (convertedValue == null) {
                    throw new NucleusException(LOCALISER.msg("038003", mmd.getFullFieldName(), oid)).setFatal();
                }
                oid = convertedValue;
            } catch (NumberFormatException nfe) {
                throw new NucleusUserException("Value strategy created value=" + oid + " type=" + oid.getClass().getName() + " but field is of type " + mmd.getTypeName() + ". Use a different strategy or change the type of the field " + mmd.getFullFieldName());
            }
        }
        if (NucleusLogger.VALUEGENERATION.isDebugEnabled()) {
            NucleusLogger.VALUEGENERATION.debug(LOCALISER.msg("038002", fieldName, strategy, generator.getClass().getName(), oid));
        }
        return oid;
    }

    /**
     * Method defining which value-strategy to use when the user specifies "native".
     * Returns "uuid-hex" no matter what the field is since this is built-in and available for all.
     * Override if your datastore requires something else.
     * @param cmd Class requiring the strategy
     * @param absFieldNumber Field of the class
     * @return Just returns "uuid-hex".
     */
    protected String getStrategyForNative(AbstractClassMetaData cmd, int absFieldNumber) {
        if (absFieldNumber >= 0) {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(absFieldNumber);
            Class type = mmd.getType();
            if (String.class.isAssignableFrom(type)) {
                return "uuid-hex";
            } else if (type == Long.class || type == Integer.class || type == Short.class || type == long.class || type == int.class || type == short.class) {
                if (supportsValueStrategy("increment")) {
                    return "increment";
                }
            }
            throw new NucleusUserException("This datastore provider doesn't support native strategy for field of type " + type.getName());
        }
        return "uuid-hex";
    }

    /**
     * Accessor for the next value from the specified generator.
     * This implementation simply returns generator.next(). Any case where the generator requires
     * datastore connections should override this method.
     * @param generator The generator
     * @param ec execution context
     * @return The next value.
     */
    protected Object getStrategyValueForGenerator(ValueGenerator generator, final ExecutionContext ec) {
        Object oid = null;
        synchronized (generator) {
            if (generator instanceof AbstractDatastoreGenerator) {
                ValueGenerationConnectionProvider connProvider = new ValueGenerationConnectionProvider() {

                    ManagedConnection mconn;

                    public ManagedConnection retrieveConnection() {
                        mconn = getConnection(ec);
                        return mconn;
                    }

                    public void releaseConnection() {
                        mconn.release();
                        mconn = null;
                    }
                };
                ((AbstractDatastoreGenerator) generator).setConnectionProvider(connProvider);
            }
            oid = generator.next();
        }
        return oid;
    }

    /**
     * Method to return the properties to pass to the generator for the specified field.
     * Will define the following properties "class-name", "root-class-name", "field-name" (if for a field),
     * "sequence-name", "key-initial-value", "key-cache-size", "sequence-table-name", "sequence-schema-name",
     * "sequence-catalog-name", "sequence-name-column-name", "sequence-nextval-column-name".
     * In addition any extension properties on the respective field or datastore-identity are also passed through 
     * as properties.
     * @param cmd MetaData for the class
     * @param absoluteFieldNumber Number of the field (-1 = datastore identity)
     * @param ec execution context
     * @param seqmd Any sequence metadata
     * @param tablegenmd Any table generator metadata
     * @return The properties to use for this field
     */
    protected Properties getPropertiesForGenerator(AbstractClassMetaData cmd, int absoluteFieldNumber, ExecutionContext ec, SequenceMetaData seqmd, TableGeneratorMetaData tablegenmd) {
        Properties properties = new Properties();
        AbstractMemberMetaData mmd = null;
        IdentityStrategy strategy = null;
        String sequence = null;
        ExtensionMetaData[] extensions = null;
        if (absoluteFieldNumber >= 0) {
            mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(absoluteFieldNumber);
            strategy = mmd.getValueStrategy();
            sequence = mmd.getSequence();
            extensions = mmd.getExtensions();
        } else {
            IdentityMetaData idmd = cmd.getBaseIdentityMetaData();
            strategy = idmd.getValueStrategy();
            sequence = idmd.getSequence();
            extensions = idmd.getExtensions();
        }
        properties.setProperty("class-name", cmd.getFullClassName());
        properties.put("root-class-name", cmd.getBaseAbstractClassMetaData().getFullClassName());
        if (mmd != null) {
            properties.setProperty("field-name", mmd.getFullFieldName());
        }
        if (sequence != null) {
            properties.setProperty("sequence-name", sequence);
        }
        if (extensions != null) {
            for (int i = 0; i < extensions.length; i++) {
                properties.put(extensions[i].getKey(), extensions[i].getValue());
            }
        }
        if (strategy == IdentityStrategy.INCREMENT && tablegenmd != null) {
            properties.put("key-initial-value", "" + tablegenmd.getInitialValue());
            properties.put("key-cache-size", "" + tablegenmd.getAllocationSize());
            if (tablegenmd.getTableName() != null) {
                properties.put("sequence-table-name", tablegenmd.getTableName());
            }
            if (tablegenmd.getCatalogName() != null) {
                properties.put("sequence-catalog-name", tablegenmd.getCatalogName());
            }
            if (tablegenmd.getSchemaName() != null) {
                properties.put("sequence-schema-name", tablegenmd.getSchemaName());
            }
            if (tablegenmd.getPKColumnName() != null) {
                properties.put("sequence-name-column-name", tablegenmd.getPKColumnName());
            }
            if (tablegenmd.getPKColumnName() != null) {
                properties.put("sequence-nextval-column-name", tablegenmd.getValueColumnName());
            }
            if (tablegenmd.getPKColumnValue() != null) {
                properties.put("sequence-name", tablegenmd.getPKColumnValue());
            }
        } else if (strategy == IdentityStrategy.INCREMENT && tablegenmd == null) {
            if (!properties.containsKey("key-cache-size")) {
                int allocSize = getIntProperty(PropertyNames.PROPERTY_VALUEGEN_INCREMENT_ALLOCSIZE);
                properties.put("key-cache-size", "" + allocSize);
            }
        } else if (strategy == IdentityStrategy.SEQUENCE && seqmd != null) {
            if (seqmd.getDatastoreSequence() != null) {
                if (seqmd.getInitialValue() >= 0) {
                    properties.put("key-initial-value", "" + seqmd.getInitialValue());
                }
                if (seqmd.getAllocationSize() > 0) {
                    properties.put("key-cache-size", "" + seqmd.getAllocationSize());
                } else {
                    int allocSize = getIntProperty(PropertyNames.PROPERTY_VALUEGEN_SEQUENCE_ALLOCSIZE);
                    properties.put("key-cache-size", "" + allocSize);
                }
                properties.put("sequence-name", "" + seqmd.getDatastoreSequence());
                ExtensionMetaData[] seqExtensions = seqmd.getExtensions();
                if (seqExtensions != null) {
                    for (int i = 0; i < seqExtensions.length; i++) {
                        properties.put(seqExtensions[i].getKey(), seqExtensions[i].getValue());
                    }
                }
            } else {
            }
        }
        return properties;
    }

    public HashSet<String> getSubClassesForClass(String className, boolean includeDescendents, ClassLoaderResolver clr) {
        HashSet subclasses = new HashSet();
        String[] subclassNames = getMetaDataManager().getSubclassesForClass(className, includeDescendents);
        if (subclassNames != null) {
            for (int i = 0; i < subclassNames.length; i++) {
                if (!storeDataMgr.managesClass(subclassNames[i])) {
                    addClass(subclassNames[i], clr);
                }
                subclasses.add(subclassNames[i]);
            }
        }
        return subclasses;
    }

    /**
     * Accessor for the supported options in string form.
     * Typical values specified here are :-
     * <ul>
     * <li>ApplicationIdentity - if the datastore supports application identity</li>
     * <li>DatastoreIdentity - if the datastore supports datastore identity</li>
     * <li>ORM - if the datastore supports (some) ORM concepts</li>
     * <li>TransactionIsolationLevel.read-committed - if supporting this txn isolation level</li>
     * <li>TransactionIsolationLevel.read-uncommitted - if supporting this txn isolation level</li>
     * <li>TransactionIsolationLevel.repeatable-read - if supporting this txn isolation level</li>
     * <li>TransactionIsolationLevel.serializable - if supporting this txn isolation level</li>
     * <li>TransactionIsolationLevel.snapshot - if supporting this txn isolation level</li>
     * <li>Query.Cancel - if supporting cancelling of queries</li>
     * <li>Query.Timeout - if supporting timeout of queries</li>
     * </ul>
     */
    public Collection<String> getSupportedOptions() {
        return Collections.EMPTY_SET;
    }

    /**
     * Convenience method to assert when this StoreManager is read-only and the specified object
     * is attempting to be updated.
     * @param op ObjectProvider for the object
     */
    public void assertReadOnlyForUpdateOfObject(ObjectProvider op) {
        if (readOnlyDatastore) {
            if (getStringProperty(PropertyNames.PROPERTY_DATASTORE_READONLY_ACTION).equalsIgnoreCase("EXCEPTION")) {
                throw new DatastoreReadOnlyException(LOCALISER.msg("032004", op.toPrintableID()), op.getExecutionContext().getClassLoaderResolver());
            } else {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled()) {
                    NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("032005", op.toPrintableID()));
                }
                return;
            }
        } else {
            AbstractClassMetaData cmd = op.getClassMetaData();
            if (cmd.hasExtension("read-only")) {
                String value = cmd.getValueForExtension("read-only");
                if (!StringUtils.isWhitespace(value)) {
                    boolean readonly = Boolean.valueOf(value).booleanValue();
                    if (readonly) {
                        if (getStringProperty(PropertyNames.PROPERTY_DATASTORE_READONLY_ACTION).equalsIgnoreCase("EXCEPTION")) {
                            throw new DatastoreReadOnlyException(LOCALISER.msg("032006", op.toPrintableID()), op.getExecutionContext().getClassLoaderResolver());
                        } else {
                            if (NucleusLogger.PERSISTENCE.isDebugEnabled()) {
                                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("032007", op.toPrintableID()));
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public Object getProperty(String name) {
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH))) {
            return super.getProperty(name);
        }
        return nucleusContext.getPersistenceConfiguration().getProperty(name);
    }

    @Override
    public int getIntProperty(String name) {
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH))) {
            return super.getIntProperty(name);
        }
        return nucleusContext.getPersistenceConfiguration().getIntProperty(name);
    }

    @Override
    public String getStringProperty(String name) {
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH))) {
            return super.getStringProperty(name);
        }
        return nucleusContext.getPersistenceConfiguration().getStringProperty(name);
    }

    @Override
    public boolean getBooleanProperty(String name) {
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH))) {
            return super.getBooleanProperty(name);
        }
        return nucleusContext.getPersistenceConfiguration().getBooleanProperty(name);
    }

    @Override
    public boolean getBooleanProperty(String name, boolean resultIfNotSet) {
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH))) {
            return super.getBooleanProperty(name, resultIfNotSet);
        }
        return nucleusContext.getPersistenceConfiguration().getBooleanProperty(name, resultIfNotSet);
    }

    @Override
    public Boolean getBooleanObjectProperty(String name) {
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH))) {
            return super.getBooleanObjectProperty(name);
        }
        return nucleusContext.getPersistenceConfiguration().getBooleanObjectProperty(name);
    }

    public void transactionStarted(ExecutionContext ec) {
    }

    public void transactionCommitted(ExecutionContext ec) {
    }

    public void transactionRolledBack(ExecutionContext ec) {
    }
}

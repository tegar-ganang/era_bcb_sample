package org.pixory.pxmodel;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.type.Type;
import org.hibernate.util.JDBCExceptionReporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PXObjectStore extends EmptyInterceptor implements Interceptor {

    private static final Log LOG = LogFactory.getLog(PXObjectStore.class);

    public static final String DRIVER = "org.hsqldb.jdbcDriver";

    public static final String URL_PREFIX = "jdbc:hsqldb:";

    public static final String DEFAULT_DATASET = "./data/test";

    public static final String DEFAULT_URL = URL_PREFIX + DEFAULT_DATASET;

    public static final String USERNAME = "sa";

    public static final String PASSWORD = "";

    public static final String POOL_SIZE = "5";

    public static final Dialect DIALECT = new HSQLDialect();

    public static final String TRANSACTION_STRATEGY = "org.hibernate.transaction.JDBCTransactionFactory";

    public static final String CACHE_PROVIDER_CLASS = "org.hibernate.cache.EhCacheProvider";

    /**
	 * the values here correspond to java.sql.DatabaseMetaData.getTableTypes()
	 */
    private static final String USER_TABLE_TYPE = "TABLE";

    private static PXObjectStore _instance;

    private Properties _storeProperties;

    private boolean _isSetup;

    private PXObjectStoreVersion _version;

    private Configuration _configuration;

    private SessionFactory _sessionFactory;

    private final ThreadLocal _threadSession = new ThreadLocal();

    /**
	 * has package accessibility for testing purposes
	 */
    PXObjectStore(PXObjectStoreVersion version) {
        _version = version;
        _storeProperties = new Properties();
        _storeProperties.put(Environment.DRIVER, DRIVER);
        _storeProperties.put(Environment.USER, USERNAME);
        _storeProperties.put(Environment.PASS, PASSWORD);
        _storeProperties.put(Environment.POOL_SIZE, POOL_SIZE);
        _storeProperties.put(Environment.DIALECT, DIALECT.getClass().getName());
        _storeProperties.put(Environment.TRANSACTION_STRATEGY, TRANSACTION_STRATEGY);
        _storeProperties.put(Environment.CACHE_PROVIDER, CACHE_PROVIDER_CLASS);
    }

    /**
	 * @return a PXObjectStore based on the latest PXObjectStoreVersion
	 */
    public static synchronized PXObjectStore getInstance() {
        if (_instance == null) {
            _instance = new PXObjectStore(PXObjectStoreVersion.getLatestVersion());
        }
        return _instance;
    }

    /**
	 * @param datasetIdentifier
	 *           format: "path/databasename"
	 * @throws PXObjectStoreException
	 *            if connection to the database cannot be established or if the
	 *            posture of the database is somehow broken or unexpected. Works
	 *            on fail-fast principal-- if something is wrong with the store,
	 *            setup is meant to find it rather than having to wait for some
	 *            other operation further downstream
	 *  
	 */
    public synchronized boolean setup(String datasetIdentifier) throws PXObjectStoreException {
        if (!_isSetup) {
            try {
                String aUrl;
                if (datasetIdentifier != null) {
                    aUrl = URL_PREFIX + datasetIdentifier;
                } else {
                    aUrl = DEFAULT_URL;
                }
                _storeProperties.put(Environment.URL, aUrl);
                _configuration = new Configuration();
                _configuration.addProperties(_storeProperties);
                _isSetup = true;
                _version.ensureCurrent(this);
            } catch (Exception e) {
                LOG.warn(null, e);
                try {
                    doTeardown();
                } catch (Exception ignore) {
                }
                _isSetup = false;
                throw new PXObjectStoreException(e);
            }
        } else {
            throw new PXObjectStoreException("attempt to setup() PXObjectStore that is already setup()");
        }
        return _isSetup;
    }

    public void teardown() {
        if (isSetup()) {
            doTeardown();
        }
    }

    /**
	 * HAX ALERT!! Hibernate just doesn't give me access to the api I need to
	 * cleanly close all the jdbc connections, so I resort to this nastiness N.B.
	 * It's the responsibility of the clients to make sure there are no
	 * outstanding open clients
	 */
    private synchronized void doTeardown() {
        try {
            this.closeThreadSession();
        } catch (Exception e) {
            LOG.warn(null, e);
        }
        try {
            this.shutdown();
        } catch (Exception e) {
            LOG.warn(null, e);
        }
        try {
            this.getSessionFactory().close();
        } catch (Exception e) {
            LOG.debug(e);
        }
        _isSetup = false;
        _configuration = null;
        _sessionFactory = null;
    }

    /**
	 * executes an HSQLDb SHUTDOWN SCRIPT.
     * This will clear the cached data: pixory.data
	 */
    private void shutdown() {
        this.applyUpdateStatement("SET SCRIPTFORMAT TEXT");
        this.applyUpdateStatement("SHUTDOWN SCRIPT");
    }

    public boolean isSetup() {
        return _isSetup;
    }

    public Properties getStoreProperties() {
        return _storeProperties;
    }

    public synchronized Session createSession() throws PXObjectStoreException {
        if (!_isSetup) {
            throw new PXObjectStoreException("attempt to use PXObjectStore before setup");
        }
        return this.openSession();
    }

    /**
	 * View layer calls this method to inform store that a view is about to be
	 * opened in the calling Thread. This supports the View-Thread-Session design
	 * pattern. Nominally, this method would create threadSession, but since that
	 * is lazy, we don't need to do anything; mostly here for symmetry
	 */
    public void viewWillOpen() {
        LOG.debug("will open");
    }

    /**
	 * View layer calls this method to inform store that the view associated with
	 * the calling Thread has closed. This supports the View-Thread-Session
	 * design pattern.
	 */
    public void viewHasClosed() throws PXObjectStoreException {
        LOG.debug("has closed");
        this.closeThreadSession();
    }

    public Session getThreadSession() throws PXObjectStoreException {
        Session getThreadSession = (Session) _threadSession.get();
        if (getThreadSession == null) {
            getThreadSession = this.createSession();
            LOG.debug("created Thread session: " + getThreadSession);
            _threadSession.set(getThreadSession);
        }
        return getThreadSession;
    }

    public void closeThreadSession() throws PXObjectStoreException {
        Session theThreadSession = (Session) _threadSession.get();
        if (theThreadSession != null) {
            try {
                theThreadSession.close();
                LOG.debug("closed Thread session: " + theThreadSession);
            } catch (Exception e) {
                LOG.warn(null, e);
                throw new PXObjectStoreException(e);
            } finally {
                _threadSession.set(null);
            }
        }
    }

    private Session openSession() {
        Session openSession = null;
        SessionFactory aSessionFactory = getSessionFactory();
        if (aSessionFactory != null) {
            try {
                openSession = aSessionFactory.openSession(this);
            } catch (Exception anException) {
                LOG.warn(null, anException);
            }
        }
        return openSession;
    }

    Configuration getConfiguration() {
        return _configuration;
    }

    void addPersistentClasses(List classes) {
        if (classes != null) {
            try {
                Configuration aConfiguation = this.getConfiguration();
                Iterator aClassIterator = classes.iterator();
                while (aClassIterator.hasNext()) {
                    Class aClass = (Class) aClassIterator.next();
                    aConfiguation.addClass(aClass);
                }
            } catch (Exception e) {
                LOG.warn(null, e);
            } finally {
                _sessionFactory = null;
            }
        }
    }

    private synchronized SessionFactory getSessionFactory() {
        if (_sessionFactory == null) {
            Configuration aConfiguration = getConfiguration();
            if (aConfiguration != null) {
                try {
                    _sessionFactory = aConfiguration.buildSessionFactory();
                } catch (Exception anException) {
                    LOG.warn(null, anException);
                }
            }
        }
        return _sessionFactory;
    }

    /**
	 * @param tableType--
	 *           either USER_TABLE_TYPE or SYSTEM_TABLE_TYPE null tableType means
	 *           *all* table types
	 * @return List of Maps, one Map /table
	 */
    List getTableMetadata(String tableType) throws PXObjectStoreException {
        List getTableMetadata = null;
        try {
            Session aSession = this.createSession();
            LOG.debug("aSession: " + aSession);
            if (aSession != null) {
                Connection aConnection = aSession.connection();
                if (aConnection != null) {
                    DatabaseMetaData aMetaData = aConnection.getMetaData();
                    String[] someTableTypes = null;
                    if (tableType != null) {
                        someTableTypes = new String[] { tableType };
                    }
                    ResultSet someTables = aMetaData.getTables(null, null, null, someTableTypes);
                    getTableMetadata = PXSqlUtility.asDictionaries(someTables);
                    LOG.debug("getTableMetadata: " + getTableMetadata);
                }
                aSession.close();
            }
        } catch (HibernateException e) {
            throw new PXObjectStoreException(e);
        } catch (SQLException e) {
            throw new PXObjectStoreException(e);
        }
        return getTableMetadata;
    }

    /**
	 * extracts the tablenames from the jdbc provided table metadata
	 */
    Set getTableNames(String tableType) throws PXObjectStoreException {
        Set getTableNames = null;
        List someTableDescriptions = this.getTableMetadata(tableType);
        if ((someTableDescriptions != null) && (someTableDescriptions.size() > 0)) {
            getTableNames = new HashSet();
            Iterator aTableIterator = someTableDescriptions.iterator();
            while (aTableIterator.hasNext()) {
                Map aTableDescription = (Map) aTableIterator.next();
                getTableNames.add(aTableDescription.get("TABLE_NAME"));
            }
        }
        return getTableNames;
    }

    Set getUserTableNames() throws PXObjectStoreException {
        return this.getTableNames(USER_TABLE_TYPE);
    }

    /**
	 * An updateStatement here is an Insert, Update, Delete or DDL The statement
	 * is applied in its own Session, which has method lifespan. all statements
	 * should succeed or fail atomically
	 * 
	 * @param statements
	 *           List of String statements
	 * @return true if and only if all statements succeeded
	 */
    boolean applyUpdateStatements(List statements) {
        boolean applyUpdateStatements = false;
        if (statements != null) {
            Session aSession = null;
            Connection aConnection = null;
            try {
                aSession = this.createSession();
                if (aSession != null) {
                    aConnection = aSession.connection();
                    if (aConnection != null) {
                        aConnection.commit();
                        aConnection.setAutoCommit(true);
                        Iterator aStatementIterator = statements.iterator();
                        while (aStatementIterator.hasNext()) {
                            String aStatementString = (String) aStatementIterator.next();
                            LOG.debug("applying statement: " + aStatementString);
                            Statement aStatement = aConnection.createStatement();
                            aStatement.executeUpdate(aStatementString);
                            aStatement.close();
                        }
                        applyUpdateStatements = true;
                    }
                }
            } catch (Exception anException) {
                LOG.warn(null, anException);
                try {
                    if (aConnection != null) {
                        aConnection.rollback();
                    }
                } catch (SQLException e) {
                    LOG.warn(null, e);
                }
                applyUpdateStatements = false;
            } finally {
                try {
                    if (aConnection != null) {
                        JDBCExceptionReporter.logWarnings(aConnection.getWarnings());
                        aConnection.clearWarnings();
                    }
                    if (aSession != null) {
                        aSession.close();
                    }
                } catch (Exception anException) {
                    applyUpdateStatements = false;
                    LOG.warn(null, anException);
                }
            }
        }
        return applyUpdateStatements;
    }

    /**
	 * calls applyUpdateStatements
	 */
    boolean applyUpdateStatement(String statementString) {
        boolean applyUpdateStatement = false;
        LOG.debug("statement: " + statementString);
        if (statementString != null) {
            List someStatements = new ArrayList();
            someStatements.add(statementString);
            applyUpdateStatement = this.applyUpdateStatements(someStatements);
        }
        return applyUpdateStatement;
    }

    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        return false;
    }

    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
        boolean onFlushDirty = false;
        LOG.debug("entity: " + entity);
        if (entity instanceof PXAuditable) {
            for (int i = 0; i < propertyNames.length; i++) {
                if (propertyNames[i].equals("lastUpdateDate")) {
                    currentState[i] = new Date();
                    onFlushDirty = true;
                    break;
                }
            }
        }
        return onFlushDirty;
    }

    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        boolean onSave = false;
        LOG.debug("entity: " + entity);
        if (entity instanceof PXAuditable) {
            Date aSaveDate = new Date();
            for (int i = 0; i < propertyNames.length; i++) {
                if (propertyNames[i].equals("creationDate") || propertyNames[i].equals("lastUpdateDate")) {
                    state[i] = aSaveDate;
                    onSave = true;
                }
            }
        }
        return onSave;
    }

    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
    }

    public void preFlush(Iterator entities) throws CallbackException {
    }

    public void postFlush(Iterator entities) throws CallbackException {
    }

    /**
	 * @return null selects the default behaviour
	 */
    public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        return null;
    }

    /**
	 * @return null selects the default behaviour
	 */
    public Object instantiate(Class clazz, Serializable id) throws CallbackException {
        return null;
    }
}

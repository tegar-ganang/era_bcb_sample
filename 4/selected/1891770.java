package com.daffodilwoods.daffodildb.server.serversystem;

import java.io.*;
import java.lang.ref.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import com.daffodilwoods.daffodildb.server.backup.*;
import com.daffodilwoods.daffodildb.server.datadictionarysystem.*;
import com.daffodilwoods.daffodildb.server.datasystem.indexsystem.*;
import com.daffodilwoods.daffodildb.server.datasystem.interfaces.*;
import com.daffodilwoods.daffodildb.server.datasystem.interfaces.Utility;
import com.daffodilwoods.daffodildb.server.datasystem.mergesystem.*;
import com.daffodilwoods.daffodildb.server.datasystem.persistentsystem.*;
import com.daffodilwoods.daffodildb.server.serversystem.datatriggersystem.*;
import com.daffodilwoods.daffodildb.server.serversystem.dmlvalidation.constraintsystem.*;
import com.daffodilwoods.daffodildb.server.serversystem.dmlvalidation.statementtriggersystem.*;
import com.daffodilwoods.daffodildb.server.serversystem.dmlvalidation.triggersystem.*;
import com.daffodilwoods.daffodildb.server.sessionsystem.*;
import com.daffodilwoods.daffodildb.server.sql99.*;
import com.daffodilwoods.daffodildb.server.sql99.ddl.descriptors.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.iterator.*;
import com.daffodilwoods.daffodildb.utils.*;
import com.daffodilwoods.daffodildb.utils.byteconverter.*;
import com.daffodilwoods.daffodildb.utils.comparator.*;
import com.daffodilwoods.daffodildb.utils.field.*;
import com.daffodilwoods.daffodildb.utils.parser.*;
import com.daffodilwoods.daffodildb.utils.parser.Parser;
import com.daffodilwoods.database.general.*;
import com.daffodilwoods.database.resource.*;
import com.daffodilwoods.database.utility.*;
import java.lang.reflect.Method;
import java.lang.reflect.*;
import java.nio.channels.FileChannel;
import com.daffodilwoods.daffodildb.server.datasystem.persistentsystem.DatabaseConstants;
import com.daffodilwoods.daffodildb.server.datasystem.persistentsystem.versioninfo.VersionHandlerFactory;

public class ServerSystem implements _ServerSystem {

    private _DataDictionarySystem dataDictionarySystem;

    private _DataTriggerSystem dataTriggerSystem;

    private SessionSystem sessionSystem;

    private _StatementTriggerSystem statementTriggerSystem;

    private _DataSystem persistentSystem;

    _DataSystem persistentIndexSystem;

    int sessionId = 1;

    private TreeMap serverSessionList = new TreeMap();

    public static final boolean FIRE_COMMIT_TIME_LISTENERS = false;

    ArrayList scheduleList = new ArrayList();

    public String daffodilHome;

    RandomAccessFile raf;

    private Parser parser;

    private _SequenceSystem sequenceSystem;

    private String defaultUser = SystemTables.SYSTEM;

    private String defaultPassword = SystemTables.SYSTEM;

    private DXAResource xaResource;

    private boolean isSaveMode;

    _DataSystem cacheSystem;

    private boolean backupUnderProcess = false;

    private boolean isReadOnlyMode = false, isServerActive;

    public CTbnfUzqfDpnqbsbups sameTypeComparator;

    public ServerSystem() throws DException {
        parser = new Parser();
        isServerActive = true;
        init(isReadOnlyMode);
        if (!_Server.ISONEDOLLARDB) {
            StartScheduler startScheduler = new StartScheduler(this);
        }
        sameTypeComparator = new CTbnfUzqfDpnqbsbups();
    }

    public ServerSystem(boolean isReadOnlyMode0) throws DException {
        parser = new Parser();
        isServerActive = true;
        isReadOnlyMode = isReadOnlyMode0;
        init(isReadOnlyMode);
        if (!_Server.ISONEDOLLARDB) {
            StartScheduler startScheduler = new StartScheduler(this);
        }
    }

    public ServerSystem(String path) throws DException {
        daffodilHome = path;
        isServerActive = true;
    }

    private void init(boolean isReadOnlyMode) throws DException {
        if (System.getProperty(DAFFODILDB_HOME) == null) {
            System.setProperty(DAFFODILDB_HOME, System.getProperty("user.home") + File.separator + "DaffodilDB");
        }
        daffodilHome = System.getProperty(DAFFODILDB_HOME);
        checkDatabaseInUse();
        parser.initialisePR();
        String memory = System.getProperty("memory");
        PrintHandler.print(" Memory passed in the server system =  " + memory);
        SystemFieldsCharacteristics sfc = new SystemFieldsCharacteristics();
        double mem = 0;
        dataDictionarySystem = new DataDictionarySystem(null);
        persistentSystem = new PersistentSystem(daffodilHome, isReadOnlyMode);
        persistentIndexSystem = new IndexSystem(persistentSystem, isReadOnlyMode);
        _DataSystem tempIndexSystem = null;
        if (isReadOnlyMode) {
            tempIndexSystem = new ReadOnlyTempIndexSystem();
        } else {
            tempIndexSystem = new TempIndexSystem(daffodilHome);
        }
        cacheSystem = new MergeSystem(tempIndexSystem, persistentIndexSystem);
        _ConstraintSystem constraintSystem = new ConstraintSystem(dataDictionarySystem, false);
        _ConstraintSystem deferrableConstraintSystem = new ConstraintSystem(dataDictionarySystem, true);
        _TriggerSystem triggerSystem = new TriggerSystem(dataDictionarySystem);
        dataTriggerSystem = new DataTriggerSystem(constraintSystem, deferrableConstraintSystem, triggerSystem, dataDictionarySystem);
        statementTriggerSystem = new StatementTriggerSystem(dataDictionarySystem);
        sessionSystem = new SessionSystem(dataDictionarySystem, cacheSystem, deferrableConstraintSystem, constraintSystem, isReadOnlyMode);
        checkDatabase();
    }

    private void checkDatabaseInUse() throws DException {
        StringTokenizer tokenizer = new StringTokenizer(System.getProperty("java.version"), ".");
        String javaVersion = null;
        if (tokenizer.countTokens() > 1) {
            javaVersion = tokenizer.nextToken();
            javaVersion += tokenizer.nextToken();
        }
        if (Integer.parseInt(javaVersion) < 14) {
            checkDatabaseInUseBelowJavaVersion1_4();
            return;
        }
        String logFile = daffodilHome + File.separator + "log.lg";
        File ff = new File(logFile);
        try {
            raf = new RandomAccessFile(ff, "rw");
            Method mt = null;
            try {
                mt = raf.getClass().getDeclaredMethod("getChannel", null);
            } catch (SecurityException ex1) {
                System.err.println(ex1);
            } catch (NoSuchMethodException ex1) {
                ex1.printStackTrace();
                System.err.println(ex1);
            }
            if (mt == null) {
                checkDatabaseInUseBelowJavaVersion1_4();
                return;
            }
            Object channel = null;
            Object lockObject = null;
            try {
                channel = mt.invoke(raf, null);
                if (channel == null) {
                    ff = null;
                    raf = null;
                    checkDatabaseInUseBelowJavaVersion1_4();
                    return;
                }
                mt = null;
                mt = channel.getClass().getMethod("tryLock", null);
                if (mt == null) {
                    ff = null;
                    raf = null;
                    checkDatabaseInUseBelowJavaVersion1_4();
                    return;
                }
                lockObject = mt.invoke(channel, null);
            } catch (InvocationTargetException ex2) {
                ff = null;
                raf = null;
                checkDatabaseInUseBelowJavaVersion1_4();
                return;
            } catch (IllegalArgumentException ex2) {
                ff = null;
                raf = null;
                checkDatabaseInUseBelowJavaVersion1_4();
                return;
            } catch (IllegalAccessException ex2) {
                ff = null;
                raf = null;
                checkDatabaseInUseBelowJavaVersion1_4();
                return;
            } catch (SecurityException ex1) {
                ff = null;
                raf = null;
                checkDatabaseInUseBelowJavaVersion1_4();
                return;
            } catch (NoSuchMethodException ex1) {
                ff = null;
                raf = null;
                checkDatabaseInUseBelowJavaVersion1_4();
                return;
            }
            if (lockObject == null) throw new DException("DSE5522", new Object[] {});
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }
    }

    private void checkDatabase() throws DException {
        String databaseURL = daffodilHome + File.separator + DatabaseConstants.SYSTEMDATABASE;
        File f = new File(databaseURL);
        double version;
        if (!f.exists()) {
            version = VersionHandlerFactory.getLatestVersionHandler().getDbVersion();
            SystemTablesCreator.changeStructure(version);
            createSystemDatabase(DatabaseConstants.SYSTEMDATABASE, DatabaseConstants.INITIALFILESIZE, DatabaseConstants.INCREMENTFACTOR, false, false);
        } else {
            PersistentDatabase pd = (PersistentDatabase) persistentSystem.getDatabase(DatabaseConstants.SYSTEMDATABASE);
            version = pd.getVersionHandler().getDbVersion();
            checkDatabaseIsCompleteBit(pd);
            SystemTablesCreator.changeStructure(version);
        }
    }

    public void setVerbose(boolean verbose) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        PrintHandler.verbose = verbose;
    }

    public void setSaveMode(boolean saveMode0) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        isSaveMode = saveMode0;
    }

    /**
    * @todo delete user
    * delete user as it is always passed in Properties
    */
    public _ServerSession getServerSession(String user, Object sessionConstant, Properties sessionProperties, String databaseURL) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        sessionProperties = P.convertDaffodilDBKeyssToUpperCase(sessionProperties);
        sessionProperties.setProperty(USER, user);
        sessionProperties.setProperty(CREATE, "true");
        _UserSession userSession = sessionSystem.getUserSession(user, sessionConstant, sessionProperties, databaseURL);
        ServerSession serverSession = new ServerSession(databaseURL, userSession, this);
        serverSession.setVerboseUser(sessionProperties.getProperty("verbose", ""));
        return serverSession;
    }

    /**
    * @todo pass properties
    * Properties should be passed so as to know the value of CREATE
    */
    private boolean ensureDatabase(String databaseName, Properties prop) throws DException {
        String databaseURL = daffodilHome + File.separator + databaseName + File.separator + databaseName + ".ddb";
        boolean isExists = false;
        File f = new File(databaseURL);
        try {
            isExists = f.exists();
            if (!isExists) {
                String create = prop.getProperty(CREATE);
                if (create == null || create.equalsIgnoreCase("false")) persistentSystem.getDatabase(databaseName);
                createDatabase1(databaseName, prop);
            }
        } catch (DException ex) {
            f.deleteOnExit();
            throw ex;
        }
        return isExists;
    }

    public _DataTriggerTable getDataTriggerTable(String databaseURL, QualifiedIdentifier tableName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        _DataTriggerDatabase dataTriggerDatabase = dataTriggerSystem.getDataTrigerDatabase(databaseURL);
        return dataTriggerDatabase.getDataTriggerTable(tableName);
    }

    public _DataTriggerTable getDataViewTriggerTable(String databaseURL, QualifiedIdentifier tableName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        _DataTriggerDatabase dataTriggerDatabase = dataTriggerSystem.getDataTrigerDatabase(databaseURL);
        return dataTriggerDatabase.getDataViewTriggerTable(tableName);
    }

    public _StatementTriggerTable getStatementTriggerTable(String databaseURL, QualifiedIdentifier tableName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        _StatementTriggerDatabase StatementTriggerDatabase = statementTriggerSystem.getStatementTriggerDatabase(databaseURL);
        return StatementTriggerDatabase.getStatementTriggerTable(tableName);
    }

    public _DataDictionarySystem getDataDictionarySystem() throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return dataDictionarySystem;
    }

    public _Database getFileDatabase(String databaseURL) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return persistentSystem.getDatabase(databaseURL);
    }

    public _Connection getConnection(String databaseName, Properties prop) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        synchronized (this) {
            if (backupUnderProcess) throw new DException("DSE5577", null);
        }
        prop = P.convertDaffodilDBKeyssToUpperCase(prop);
        databaseName = databaseName.toLowerCase();
        String databaseURL = databaseName;
        String user = prop.getProperty(USER, "Public");
        String password = prop.getProperty(PASSWORD, "PUBLIC");
        String verboseUser = prop.getProperty(VERBOSE, "");
        String schema = prop.getProperty(SCHEMA);
        if (user == null || user.equals(SystemTables.SYSTEM)) throw new DatabaseException("DSE1047", null);
        ensureDatabase(databaseURL, prop);
        if (!isValidUser(user, password, databaseName)) throw new DException("DSE1308", new Object[] { user });
        String xid = prop.getProperty(XID);
        _UserSession userSession = sessionSystem.getUserSession(user, xid, prop, databaseURL);
        Connection connection = isSaveMode ? new SaveModeConnection(databaseURL, userSession, this) : isReadOnlyMode ? new ReadOnlyConnection(databaseURL, userSession, this) : new Connection(databaseURL, userSession, this);
        connection.setVerboseUser(verboseUser);
        if (!(user.equalsIgnoreCase(ServerSystem.browserUser) || user.equalsIgnoreCase(SystemTables.SYSTEM))) {
            connection.setCurrentCatalog("users");
        }
        if (schema != null && getDataDictionary(databaseName).isValidSchema(connection.getCurrentCatalog(), schema)) {
            connection.setCurrentSchema(schema);
        } else {
        }
        String isVisible = prop.getProperty("isvisible");
        if (isVisible == null || (!isVisible.equalsIgnoreCase("false"))) addServerSessionInList(databaseURL, connection);
        if (isSaveMode) ((SaveModeConnection) connection).getSaveModeHandler().write(connection.getServerSession().getSessionId(), user, password);
        if (connection.verbose) PrintHandler.print(" Connection got on database " + databaseName + " with user " + user, null, verboseUser);
        return connection;
    }

    public _Connection get_Connection(String databaseURL, Properties prop) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        databaseURL = databaseURL.trim();
        databaseURL = databaseURL.toLowerCase();
        String user = prop.getProperty(USER);
        String password = prop.getProperty(PASSWORD);
        if (user == null) throw new DatabaseException("DSE1047", null);
        String xid = prop.getProperty(XID);
        _UserSession userSession = sessionSystem.getUserSession(user, xid, prop, databaseURL);
        _Connection connection = new Connection(databaseURL, userSession, this);
        return connection;
    }

    private void addServerSessionInList(String databaseURL, _Connection connection) throws DException {
        ArrayList list = (ArrayList) serverSessionList.get(databaseURL);
        if (list == null) {
            list = new ArrayList();
            serverSessionList.put(databaseURL, list);
        }
        list.add(new WeakReference(connection));
    }

    public void deleteTable(String databaseURL, QualifiedIdentifier tableName, boolean dropTable) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        _DataDictionary dataDictionary = dataDictionarySystem.getDataDictionary(databaseURL);
        dataDictionary.removeTable(tableName);
        _DataTriggerDatabase dataTriggerDatabase = dataTriggerSystem.getDataTrigerDatabase(databaseURL);
        dataTriggerDatabase.removeTable(tableName);
        _StatementTriggerDatabase statementTriggerDatabase = statementTriggerSystem.getStatementTriggerDatabase(databaseURL);
        statementTriggerDatabase.removeTable(tableName);
        _SessionDatabase sessionDatabase = ((SessionSystem) sessionSystem).getSessionDatabase(databaseURL, null);
        sessionDatabase.removeTable(tableName, dropTable);
        removeEntriesFromServerSession(databaseURL, tableName);
    }

    public void refereshTable(String databaseURL, QualifiedIdentifier tableName, boolean dropTable) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        _DataDictionary dataDictionary = dataDictionarySystem.getDataDictionary(databaseURL);
        dataDictionary.removeTable(tableName);
        _DataTriggerDatabase dataTriggerDatabase = dataTriggerSystem.getDataTrigerDatabase(databaseURL);
        dataTriggerDatabase.removeTable(tableName);
        _StatementTriggerDatabase statementTriggerDatabase = statementTriggerSystem.getStatementTriggerDatabase(databaseURL);
        statementTriggerDatabase.removeTable(tableName);
        _SessionDatabase sessionDatabase = ((SessionSystem) sessionSystem).getSessionDatabase(databaseURL, null);
        removeEntriesFromServerSession(databaseURL, tableName);
    }

    public _User getUser(String userName, String password) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return new User(userName, this, password);
    }

    public ArrayList getAllDatabases(String userName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        ArrayList databases = ((PersistentSystem) persistentSystem).getAllDatabases();
        for (int i = databases.size(); i-- > 0; ) {
            String databaseName = (String) databases.get(i);
            databaseName = databaseName.toLowerCase();
            try {
                _DataDictionary dic = getDataDictionary(databaseName);
                _PrivilegeCharacteristics pc = dic.getPrivilegeCharacteristics(userName, _PrivilegeCharacteristics.AUTHORIZATION_USER);
                if (pc == null) databases.remove(i);
            } catch (DException ex) {
                databases.remove(i);
                continue;
            }
        }
        return databases;
    }

    public ArrayList getAllSessions(String databaseName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        ArrayList list = (ArrayList) serverSessionList.get(databaseName);
        if (list == null || list.size() == 0) return null;
        synchronized (list) {
            ArrayList sessions = new ArrayList(list.size());
            for (int i = list.size(); i-- > 0; ) {
                WeakReference wk = (WeakReference) list.get(i);
                Object obj = wk.get();
                if (obj != null) sessions.add(obj);
            }
            return sessions;
        }
    }

    private void removeClosedConnection(ArrayList list, ServerSession serverSession) {
        synchronized (list) {
            for (int i = list.size(); i-- > 0; ) {
                WeakReference wk = (WeakReference) list.get(i);
                Object session = wk.get();
                if (session == null) list.remove(i); else if (serverSession.equals(session)) list.remove(i);
            }
        }
    }

    public void close(String databaseURL, ServerSession serverSession) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        ArrayList list = (ArrayList) serverSessionList.get(databaseURL);
        try {
            if (list != null) {
                removeClosedConnection(list, serverSession);
                if (list.size() == 0) {
                    SessionDatabase sessionDatabase = (SessionDatabase) ((SessionSystem) sessionSystem).getSessionDatabase(databaseURL, null);
                    sessionDatabase.getSystemFieldsValue().updateLastTransactionId();
                }
            }
        } catch (DException ex) {
        }
    }

    public _DataDictionary getDataDictionary(String databaseName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return sessionSystem.getSessionDatabase(databaseName, new Properties()).getDataDictionary();
    }

    public _Database getMergeDatabase(String databasePath) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return sessionSystem.getSessionDatabase(databasePath, new Properties()).getMergeDatabase();
    }

    public _MemoryManager getMemoryManager() throws DException {
        throw new UnsupportedOperationException("Method getMemoryManager not supported");
    }

    public String getHostName() throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return "localHost";
    }

    public int getPortNumber() throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return -1;
    }

    public void checkUserPassword(String user, String password, String databaseURL) throws DException {
        throw new UnsupportedOperationException("Method not supported");
    }

    private void closeAllServerSession() throws DException {
        Iterator it = serverSessionList.values().iterator();
        if (it.hasNext()) {
            ArrayList list = (ArrayList) it.next();
            for (int i = list.size(); i-- > 0; ) {
                WeakReference wk = (WeakReference) list.get(i);
                _ServerSession session = (_ServerSession) wk.get();
                session.close();
            }
        }
    }

    private void closeAllServerSession(String databaseName) throws DException {
        ArrayList list = (ArrayList) serverSessionList.get(databaseName);
        if (list == null) return;
        for (int i = list.size(); i-- > 0; ) {
            WeakReference wk = (WeakReference) list.get(i);
            _ServerSession session = (_ServerSession) wk.get();
            session.close();
        }
    }

    public void changeHome(String home) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        closeServerSystem();
        isServerActive = true;
        removeDatabasesFromCurrentPath();
        removeAllSchedules();
        daffodilHome = home;
        ((PersistentSystem) persistentSystem).changeHome(home);
        if (!isReadOnlyMode) ((MergeSystem) cacheSystem).getMemorySystem().changeHome(home);
        System.gc();
        System.runFinalization();
        checkDatabaseInUse();
        checkDatabase();
        synchronized (this) {
            xaResource = null;
            StartScheduler startScheduler = new StartScheduler(this);
        }
    }

    public void createDatabase(String databaseName, Properties prop) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        createDatabase1(databaseName, prop);
        dataDictionarySystem.deleteDatabase(databaseName);
        dataTriggerSystem.deleteDatabase(databaseName);
        statementTriggerSystem.deleteDatabase(databaseName);
        serverSessionList.remove(databaseName);
        sessionSystem.deleteDatabase(databaseName, true, false);
    }

    public void createDatabase1Old(String databaseName, Properties prop) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (databaseName == null || databaseName.equalsIgnoreCase(DatabaseConstants.SYSTEMDATABASE) || databaseName.equalsIgnoreCase(DatabaseConstants.TEMPDATABASE)) throw new DException("DSE5576", new Object[] { databaseName });
        if (!(databaseName.length() <= DatabaseConstants.MAXDATABASENAMELENGTH)) throw new DException("DSE2051", null);
        prop = P.convertDaffodilDBKeyssToUpperCase(prop);
        if (databaseName == null || databaseName.equalsIgnoreCase("")) throw new DException("DSE5502", new Object[] { "[" + databaseName + "]" });
        databaseName = databaseName.toLowerCase();
        String user = prop.getProperty(USER);
        String password = prop.getProperty(PASSWORD);
        if (user == null && password == null) {
            user = "PUBLIC";
            password = "PUBLIC";
        }
        if (user == null ^ password == null) throw new DException("DSE1208", null);
        if (user.equals("")) throw new DException("DSE1208", null);
        user = P.makeDelimitedIdentifier(user);
        DatabaseConnection connection = new DatabaseConnection(this);
        ((PersistentSystem) persistentSystem).createDatabase(databaseName, prop);
        PersistentDatabase dDatabase = (PersistentDatabase) ((PersistentSystem) persistentSystem).getDatabase(databaseName);
        boolean powerFileOption = dDatabase.getPowerFileOption();
        dDatabase.setWriteInPowerFile(false);
        IndexDatabase indexDatabase = (IndexDatabase) persistentIndexSystem.getDatabase(databaseName);
        ArrayList tableDetails = SystemTableInformation.getTableDetails();
        ColumnInformation columnInformation = null;
        for (int i = 0; i < tableDetails.size(); i++) {
            Object[] values = (Object[]) tableDetails.get(i);
            QualifiedIdentifier tableName = (QualifiedIdentifier) values[0];
            columnInformation = (ColumnInformation) values[1];
            dDatabase.createTable(tableName, columnInformation);
            createPermanentIndex(tableName, indexDatabase, i);
        }
        _ServerSession returnedConn = ((DatabaseConnection) connection).executeAll(databaseName);
        password = password.length() == 0 ? "\"\"" : P.makeDelimitedIdentifier(password);
        String createUser = "Create user " + user + " password " + password;
        returnedConn.execute(createUser, 0);
        String defaultSchema = "Create schema users.users Authorization " + user;
        returnedConn.execute(defaultSchema, 0);
        if (user != null && user.equalsIgnoreCase("\"" + browserUser + "\"") == false) {
            createUser = "Create user \"" + browserUser + "\" password  daffodil";
            returnedConn.execute(createUser, 0);
        }
        if (user != null && user.equals("\"PUBLIC\"") == false) {
            createUser = "Create user \"PUBLIC\" password \"PUBLIC\" ";
            returnedConn.execute(createUser, 0);
        }
        createSystemViews(returnedConn);
        createDummyTable(returnedConn);
        returnedConn.commit();
        returnedConn.close();
        createDummyTable(databaseName, prop);
        dDatabase.writeInFile(dDatabase.getDatabaseProperties().ISCOMPLETE_BIT_IN_DATABASE, CCzufDpowfsufs.getBytes(new Byte(DatabaseConstants.TRUE)));
        _DataDictionary dd = getDataDictionary(databaseName);
        dd.refereshConstraints();
        sessionSystem.refreshConstraintSystem(databaseName);
        dataTriggerSystem.deleteDatabase(databaseName);
        dDatabase.setWriteInPowerFile(powerFileOption);
    }

    public void createDatabase1(String databaseName, Properties prop) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (databaseName == null || databaseName.equalsIgnoreCase(DatabaseConstants.SYSTEMDATABASE) || databaseName.equalsIgnoreCase(DatabaseConstants.TEMPDATABASE)) throw new DException("DSE5576", new Object[] { databaseName });
        if (!(databaseName.length() <= DatabaseConstants.MAXDATABASENAMELENGTH)) throw new DException("DSE2051", null);
        prop = P.convertDaffodilDBKeyssToUpperCase(prop);
        if (databaseName == null || databaseName.equalsIgnoreCase("")) throw new DException("DSE5502", new Object[] { "[" + databaseName + "]" });
        databaseName = databaseName.toLowerCase();
        String user = prop.getProperty(USER);
        if (user != null && user.equalsIgnoreCase("PUBLIC")) throw new DException("DSE8180", new Object[] { user });
        String password = prop.getProperty(PASSWORD);
        if (user == null && password == null) {
            user = "PUBLIC";
            password = "PUBLIC";
        }
        if (user == null ^ password == null) throw new DException("DSE1208", null);
        if (user.equals("")) throw new DException("DSE1208", null);
        user = P.makeDelimitedIdentifier(user);
        DatabaseConnection connection = new DatabaseConnection(this);
        ((PersistentSystem) persistentSystem).createDatabase(databaseName, prop);
        PersistentDatabase dDatabase = (PersistentDatabase) ((PersistentSystem) persistentSystem).getDatabase(databaseName);
        boolean powerFileOption = dDatabase.getPowerFileOption();
        dDatabase.setWriteInPowerFile(false);
        IndexDatabase indexDatabase = (IndexDatabase) persistentIndexSystem.getDatabase(databaseName);
        ArrayList tableDetails = SystemTableInformation.getTableDetails();
        ColumnInformation columnInformation = null;
        for (int i = 0; i < tableDetails.size(); i++) {
            Object[] values = (Object[]) tableDetails.get(i);
            QualifiedIdentifier tableName = (QualifiedIdentifier) values[0];
            columnInformation = (ColumnInformation) values[1];
            dDatabase.createTable(tableName, columnInformation);
            createPermanentIndex(tableName, indexDatabase, i);
        }
        SystemIndexInformation.createAllSystemTableIndexes(indexDatabase);
        _ServerSession returnedConn = ((DatabaseConnection) connection).executeAllNew(databaseName);
        password = password.length() == 0 ? "\"\"" : P.makeDelimitedIdentifier(password);
        String createUser = "Create user " + user + " password " + password;
        returnedConn.execute(createUser, 0);
        String defaultSchema = "Create schema users.users Authorization " + user;
        returnedConn.execute(defaultSchema, 0);
        if (user != null && user.equalsIgnoreCase("\"" + browserUser + "\"") == false) {
            createUser = "Create user \"" + browserUser + "\" password  daffodil";
            returnedConn.execute(createUser, 0);
        }
        if (user != null && user.equalsIgnoreCase("\"PUBLIC\"") == false) {
            createUser = "Create user \"PUBLIC\" password \"PUBLIC\" ";
            returnedConn.execute(createUser, 0);
        }
        createSystemViews(returnedConn);
        createDummyTable(returnedConn);
        returnedConn.commit();
        returnedConn.close();
        createDummyTable(databaseName, prop);
        dDatabase.writeInFile(dDatabase.getDatabaseProperties().ISCOMPLETE_BIT_IN_DATABASE, CCzufDpowfsufs.getBytes(new Byte(DatabaseConstants.TRUE)));
        _DataDictionary dd = getDataDictionary(databaseName);
        dd.setSystemTableConstraint(true);
        dd.refereshConstraints();
        sessionSystem.refreshConstraintSystem(databaseName);
        dataTriggerSystem.deleteDatabase(databaseName);
        dDatabase.setWriteInPowerFile(powerFileOption);
    }

    private void createDummyTable(String databaseName, Properties prop) throws DException {
        String viewQuery = " create view users.users.dual as Select * from system.information_schema.dualSystemTable";
        String grantSelect = "grant SELECT on users.users.dual to PUBLIC";
        _Connection serverSession = getConnection(databaseName, prop);
        serverSession.execute(viewQuery, 0, 0);
        serverSession.execute(grantSelect, 0, 0);
        serverSession.commit();
        serverSession.close();
    }

    private void createDummyTable(_ServerSession serverSession) throws DException {
        String tableQuery = " create table system.information_schema.dualSystemTable (col1 int) ";
        String insertQuery = " insert into system.information_schema.dualSystemTable values(20976)";
        serverSession.execute(tableQuery, 0, 0);
        serverSession.executeUpdate(insertQuery, 0);
    }

    private void createSystemViews(_ServerSession serverSession) throws DException {
        ArrayList viewsList = SystemTablesCreator.getSystemViews();
        for (int i = 0, size = viewsList.size(); i < size; i++) {
            serverSession.execute((String) viewsList.get(i), 0);
        }
    }

    public void createSystemDatabaseOld(String databaseName, String initialSize, int incrementalFactor, boolean unicodeSupport, boolean multipleFileSupport) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        DatabaseConnection connection = new DatabaseConnection(this);
        databaseName = databaseName.toLowerCase();
        PersistentDatabase dDatabase = (PersistentDatabase) ((PersistentSystem) persistentSystem).createSystemDatabase(databaseName, initialSize, incrementalFactor, unicodeSupport, multipleFileSupport);
        boolean powerFileOption = dDatabase.getPowerFileOption();
        dDatabase.setWriteInPowerFile(false);
        IndexDatabase indexDatabase = (IndexDatabase) persistentIndexSystem.getDatabase(databaseName);
        ArrayList tableDetails = SystemTableInformation.getTableDetails();
        ColumnInformation columnInformation = null;
        for (int i = 0; i < tableDetails.size(); i++) {
            Object[] values = (Object[]) tableDetails.get(i);
            QualifiedIdentifier tableName = (QualifiedIdentifier) values[0];
            columnInformation = (ColumnInformation) values[1];
            dDatabase.createTable(tableName, columnInformation);
            createPermanentIndex(tableName, indexDatabase, i);
        }
        _ServerSession returnedConn = ((DatabaseConnection) connection).executeAll(databaseName);
        returnedConn.commit();
        returnedConn.close();
        dDatabase.writeInFile(dDatabase.getDatabaseProperties().ISCOMPLETE_BIT_IN_DATABASE, CCzufDpowfsufs.getBytes(new Byte(DatabaseConstants.TRUE)));
        _DataDictionary dd = getDataDictionary(databaseName);
        dd.refereshConstraints();
        sessionSystem.refreshConstraintSystem(databaseName);
        dDatabase.setWriteInPowerFile(powerFileOption);
    }

    public void createSystemDatabase(String databaseName, String initialSize, int incrementalFactor, boolean unicodeSupport, boolean multipleFileSupport) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        DatabaseConnection connection = new DatabaseConnection(this);
        databaseName = databaseName.toLowerCase();
        PersistentDatabase dDatabase = (PersistentDatabase) ((PersistentSystem) persistentSystem).createSystemDatabase(databaseName, initialSize, incrementalFactor, unicodeSupport, multipleFileSupport);
        boolean powerFileOption = dDatabase.getPowerFileOption();
        dDatabase.setWriteInPowerFile(false);
        IndexDatabase indexDatabase = (IndexDatabase) persistentIndexSystem.getDatabase(databaseName);
        ArrayList tableDetails = SystemTableInformation.getTableDetails();
        ColumnInformation columnInformation = null;
        for (int i = 0; i < tableDetails.size(); i++) {
            Object[] values = (Object[]) tableDetails.get(i);
            QualifiedIdentifier tableName = (QualifiedIdentifier) values[0];
            columnInformation = (ColumnInformation) values[1];
            dDatabase.createTable(tableName, columnInformation);
            createPermanentIndex(tableName, indexDatabase, i);
        }
        SystemIndexInformation.createAllSystemTableIndexes(indexDatabase);
        _ServerSession returnedConn = ((DatabaseConnection) connection).executeAllNew(databaseName);
        dDatabase.writeInFile(dDatabase.getDatabaseProperties().ISCOMPLETE_BIT_IN_DATABASE, CCzufDpowfsufs.getBytes(new Byte(DatabaseConstants.TRUE)));
        _DataDictionary dd = getDataDictionary(databaseName);
        dd.setSystemTableConstraint(true);
        dd.refereshConstraints();
        sessionSystem.refreshConstraintSystem(databaseName);
        dDatabase.setWriteInPowerFile(powerFileOption);
    }

    private void removeDatabasesFromCurrentPath() throws DException {
        String currentPath = daffodilHome;
        File file = new File(currentPath);
        String[] allDatabases = file.list();
        for (int i = 0; i < allDatabases.length; i++) {
            String databaseName = allDatabases[i];
            dataDictionarySystem.deleteDatabase(databaseName);
            dataTriggerSystem.deleteDatabase(databaseName);
            statementTriggerSystem.deleteDatabase(databaseName);
            serverSessionList.remove(databaseName);
            sessionSystem.deleteDatabase(databaseName, true, false);
        }
    }

    private void createPermanentIndex(QualifiedIdentifier tableName, IndexDatabase indexDatabase, int i) throws DException {
        String indexName = "default_index" + i;
        QualifiedIdentifier indexTableName = new QualifiedIdentifier(tableName.catalog, tableName.schema, indexName);
        Object[][] indexStructure = indexDatabase.getVersionHandler().getDefaultIndexStructure();
        IndexInformations indexInformationGetter = new IndexInformations(tableName, indexName, -1, false, indexStructure, indexDatabase.getVersionHandler());
        indexDatabase.createPermanantIndexForSystemTable(tableName, indexName, indexInformationGetter);
    }

    public void dropDatabase(String databaseName, String userName, String userDatabaseName) throws com.daffodilwoods.database.resource.DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        String databaseURL = daffodilHome + File.separator + databaseName + File.separator + databaseName + ".ddb";
        databaseName = databaseName.trim().toLowerCase();
        databaseURL = P.getAbsolutePath(databaseURL);
        synchronized (databaseName) {
            ArrayList list = (ArrayList) serverSessionList.get(databaseName);
            if (list != null) {
                synchronized (list) {
                    int noOfConnection = list.size();
                    if (noOfConnection > 1) throw new DException("DSE5519", new Object[] { databaseName });
                    if (noOfConnection == 1) {
                        if (!databaseName.equalsIgnoreCase(userDatabaseName)) throw new DException("DSE5519", new Object[] { databaseName });
                        WeakReference wk = (WeakReference) list.get(0);
                        _ServerSession session = (_ServerSession) wk.get();
                        if (session != null) {
                            if (!session.getUserSession().getUserName().equalsIgnoreCase(userName)) throw new DException("DSE5519", new Object[] { databaseName });
                        }
                    }
                }
            }
            dataDictionarySystem.deleteDatabase(databaseName);
            dataTriggerSystem.deleteDatabase(databaseName);
            statementTriggerSystem.deleteDatabase(databaseName);
            closeAllServerSession(databaseName);
            serverSessionList.remove(databaseName);
            sessionSystem.deleteDatabase(databaseName, true, true);
        }
        removeAllSchedules();
    }

    public void dropDatabase(String databaseName, String userName, String password, String userDatabaseName) throws com.daffodilwoods.database.resource.DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        try {
            Properties prop = new Properties();
            prop.setProperty(_Server.USER, userName);
            prop.setProperty(_Server.PASSWORD, password);
            _Connection conn = getConnection(databaseName, prop);
            checkIsValidUserBeforeDropDatabase(userName, conn);
            conn.close();
        } catch (DException ex) {
            if (ex.getDseCode().equalsIgnoreCase("DSE5539") || ex.getDseCode().equalsIgnoreCase("DSE5544")) {
                dropDatabase(databaseName, userName, userDatabaseName);
                return;
            } else if (ex.getDseCode().equalsIgnoreCase("DSE1210")) throw new DException("DSE1207", null); else throw ex;
        }
        String databaseURL = daffodilHome + File.separator + databaseName + File.separator + databaseName + ".ddb";
        databaseName = databaseName.trim().toLowerCase();
        databaseURL = P.getAbsolutePath(databaseURL);
        synchronized (databaseName) {
            ArrayList list = (ArrayList) serverSessionList.get(databaseName);
            if (list != null) {
                synchronized (list) {
                    int noOfConnection = list.size();
                    if (noOfConnection > 1) throw new DException("DSE5519", new Object[] { databaseName });
                    if (noOfConnection == 1) {
                        if (!databaseName.equalsIgnoreCase(userDatabaseName)) throw new DException("DSE5519", new Object[] { databaseName });
                        WeakReference wk = (WeakReference) list.get(0);
                        _ServerSession session = (_ServerSession) wk.get();
                        if (session != null) {
                            if (!session.getUserSession().getUserName().equalsIgnoreCase(userName)) throw new DException("DSE5519", new Object[] { databaseName });
                        }
                    }
                }
            }
            dataDictionarySystem.deleteDatabase(databaseName);
            dataTriggerSystem.deleteDatabase(databaseName);
            statementTriggerSystem.deleteDatabase(databaseName);
            closeAllServerSession(databaseName);
            serverSessionList.remove(databaseName);
            sessionSystem.deleteDatabase(databaseName, true, true);
        }
        removeAllSchedules();
    }

    public _QueryReWriter getQueryReWriter(String query) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return new QueryReWriter(query);
    }

    public _SequenceManager getSequenceManager(String databaseURL) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (sequenceSystem == null) sequenceSystem = new SequenceSystem(this, dataDictionarySystem);
        return sequenceSystem.getSequenceManager(databaseURL);
    }

    private void removeEntriesFromServerSession(String databaseURL, QualifiedIdentifier tableName) throws DException {
        ArrayList list = (ArrayList) serverSessionList.get(databaseURL);
        if (list == null) return;
        ServerSession serverSession;
        synchronized (list) {
            for (int i = list.size(); i-- > 0; ) {
                WeakReference wk = (WeakReference) list.get(i);
                serverSession = (ServerSession) wk.get();
                if (serverSession != null) serverSession.removeTable(tableName);
            }
        }
    }

    public boolean isValidUser(String userName, String password) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (defaultUser.equalsIgnoreCase(userName) && defaultPassword.equalsIgnoreCase(password)) return true;
        ArrayList databases = getAllDatabases(userName);
        boolean userfound_passwordNotfound = false;
        for (int i = 0; i < databases.size(); i++) {
            String databaseName = (String) databases.get(i);
            _DataDictionary dd = sessionSystem.getSessionDatabase(databaseName, new Properties()).getDataDictionary();
            _Iterator iter = dd.getIteratorForUserValidity(userName);
            if (iter.first()) {
                Object[] data = (Object[]) iter.getColumnValues();
                if (!password.equals((String) ((FieldBase) data[SystemTablesFields.users_table_user_password]).getObject())) {
                    userfound_passwordNotfound = true;
                } else return true;
            }
        }
        if (userfound_passwordNotfound) throw new DException("DSE1309", null);
        return (databases.size() == 0 && userName.equalsIgnoreCase(browserUser) && password.equalsIgnoreCase(browserUser));
    }

    public synchronized _DXAResource getDxaResource() throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return xaResource == null ? (xaResource = new DXAResource(this)) : xaResource;
    }

    public boolean isActiveAuthorization(String Authorization, String databaseName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        ArrayList list = (ArrayList) serverSessionList.get(databaseName);
        boolean found = false;
        if (list != null) {
            synchronized (list) {
                for (int i = list.size(); i-- > 0; ) {
                    WeakReference wk = (WeakReference) list.get(i);
                    Object session = wk.get();
                    if (session == null) {
                        list.remove(i);
                    } else {
                        found = ((_ServerSession) session).isEnabledAuthorizationIdentifier(Authorization, false);
                        if (found) return found;
                    }
                }
            }
        }
        return false;
    }

    private void checkDatabaseIsCompleteBit(PersistentDatabase pd) throws DException {
        byte[] bytes = pd.readBytes(pd.getDatabaseProperties().ISCOMPLETE_BIT_IN_DATABASE, 1);
        Boolean isComplete = CCzufDpowfsufs.getBoolean(bytes);
        if (!isComplete.booleanValue()) {
            throw new DException("DSE5543", null);
        }
    }

    public boolean isValidUser(String userName, String password, String databaseName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (defaultUser.equalsIgnoreCase(userName) && defaultPassword.equalsIgnoreCase(password)) return true;
        _DataDictionary dd = sessionSystem.getSessionDatabase(databaseName, new Properties()).getDataDictionary();
        _Iterator iter = dd.getIteratorForUserValidity(userName);
        if (iter.first()) {
            Object[] data = (Object[]) iter.getColumnValues();
            if (!password.equals((String) ((FieldBase) data[SystemTablesFields.users_table_user_password]).getObject())) throw new DException("DSE1309", null);
            return true;
        }
        return (userName.equalsIgnoreCase(browserUser) && password.equalsIgnoreCase(browserUser));
    }

    public void closeServerSystem() throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (raf != null) {
            try {
                raf.close();
                raf = null;
            } catch (IOException ex) {
            }
        }
        ((MergeSystem) cacheSystem).closeAllDatabases();
        removeAllSchedules();
        closeAllServerSession();
        isServerActive = false;
    }

    public synchronized void getInconsistentOnlineBackup(String destination, String databaseNameSource, String databaseNameDestination, boolean overwrite) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (_Server.ISONEDOLLARDB) throw new DException("DSE2054", new Object[] { " Online Backup " });
        if (!((MergeDatabase) getMergeDatabase(databaseNameSource)).getVersionHandler().isBackUpSupported()) {
            throw new DException("DSE5590", new Object[] { "Online Backup" });
        }
        if (daffodilHome.equalsIgnoreCase(destination)) throw new DException("DSE5574", null);
        OnlineBackup onlineBackup = new OnlineBackup(this);
        onlineBackup.onlineBackup(destination, databaseNameSource, databaseNameDestination, overwrite);
    }

    public synchronized void offlineBackup(String userName, String password, String destination, String databaseNameSource, String databaseNameDestination, boolean overwrite) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (_Server.ISONEDOLLARDB) throw new DException("DSE2054", new Object[] { " Offline Backup " });
        try {
            if (!((MergeDatabase) getMergeDatabase(databaseNameSource)).getVersionHandler().isBackUpSupported()) {
                throw new DException("DSE5590", new Object[] { "Offline Backup" });
            }
            checkUserValidity(databaseNameSource, userName, password);
            checkConnections(databaseNameSource);
            backupUnderProcess = true;
            OfflineBackup backUp = new OfflineBackup(this);
            backUp.offlineBackup(destination, databaseNameSource, databaseNameDestination, overwrite);
        } finally {
            backupUnderProcess = false;
        }
    }

    public synchronized void restore(String userName, String password, String sourcePath, String databaseNameSource, String databaseNameDestination, boolean overwrite) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (_Server.ISONEDOLLARDB) throw new DException("DSE2054", new Object[] { " Restore Backup " });
        try {
            if (!((MergeDatabase) getMergeDatabase(DatabaseConstants.SYSTEMDATABASE)).getVersionHandler().isBackUpSupported()) {
                throw new DException("DSE5590", new Object[] { "Restore Backup" });
            }
            try {
                checkUserValidity(databaseNameDestination, userName, password);
                checkConnections(databaseNameDestination);
            } catch (DException ex) {
                if (!ex.getDseCode().equalsIgnoreCase("DSE316")) throw ex;
            }
            backupUnderProcess = true;
            OfflineBackup backUp = new OfflineBackup(this);
            backUp.restoreBackup(sourcePath, databaseNameSource, databaseNameDestination, overwrite);
        } finally {
            backupUnderProcess = false;
        }
    }

    /**
    * This method is used to add schedule to a database for onlinebackup.
    * User when enters schedule its entry is made in the SCHEDULEINFO table
    * and corresponding Thread is invoked for the respective schedule.
    *
    * @param databaseName database for which scheduling is to be done
    * @param timeIntervalForBackup time interval after which backup is to be taken
    * @param backupType type of back whether it is InconsistentOnline,ConsistentOnline,Incremental
    * @param backupPath path on which backup is to be taken
    * @param databaseNameForBackup name with which backup is to be taken
    * @throws DException DSE5562 if the databse on which schedule is to be taken doesn't exist.
    */
    public void addSchedule(String databaseName, String scheduleName, String timeIntervalForBackup, String backupType, String backupPath, String databaseNameForBackup, long lastBackupTime) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (_Server.ISONEDOLLARDB) throw new DException("DSE2054", new Object[] { " Add Schedule " });
        PersistentDatabase userDatabase = null;
        try {
            userDatabase = (PersistentDatabase) ((IndexDatabase) ((MergeDatabase) getMergeDatabase(databaseName)).getFileDatabase()).getUnderLyingDatabase();
        } catch (DException ex) {
            if (ex.getDseCode().equalsIgnoreCase("DSE316")) throw new DException("DSE5562", new Object[] { databaseName });
        }
        if (!userDatabase.getVersionHandler().isBackUpSupported()) {
            throw new DException("DSE5590", new Object[] { "Adding Schedule" });
        }
        _DatabaseUser user = null;
        PersistentDatabase systemDatabase = (PersistentDatabase) ((IndexDatabase) ((MergeDatabase) getMergeDatabase(DatabaseConstants.SYSTEMDATABASE)).getFileDatabase()).getUnderLyingDatabase();
        _Table table = null;
        try {
            table = systemDatabase.getTable(SystemTables.SCHEDULEINFO);
        } catch (DException ex1) {
            if (ex1.getDseCode().equalsIgnoreCase("DSE959")) {
                throw new DException("DSE5578", null);
            }
        }
        DatabaseUserTableIterator scheduleInfoTableIteartor = (DatabaseUserTableIterator) ((_DataTable) table).getIterator();
        BufferRange bytesOfScheduleName = new BufferRange(CCzufDpowfsufs.getBytes(scheduleName, scheduleName.length(), false));
        CTusjohJoTfotjujwfDpnqbsbups stringComparator = new CTusjohJoTfotjujwfDpnqbsbups();
        if (scheduleInfoTableIteartor.first()) {
            do {
                BufferRange bytesGot = (BufferRange) scheduleInfoTableIteartor.getColumnValues(1);
                if (stringComparator.compare(bytesGot, bytesOfScheduleName) == 0) {
                    throw new DException("DSE5561", new Object[] { scheduleName });
                }
            } while (scheduleInfoTableIteartor.next());
        }
        _TableCharacteristics tableCharacteristicsScheduleInfo = table.getTableCharacteristics();
        ArrayList list = new ArrayList();
        list.add(SystemTables.SCHEDULEINFO);
        user = systemDatabase.getDatabaseUser(list);
        try {
            long timeIntervalBackup = getTimeIntervalForBackupInMillis(timeIntervalForBackup);
            lastBackupTime = lastBackupTime - timeIntervalBackup;
            Object[] valueForScheduleInfo = new Object[] { databaseName, scheduleName, new Long(timeIntervalBackup), backupType, new Long(lastBackupTime), backupPath, databaseNameForBackup };
            BufferRange[] bytesForScheduleInfo = Utility.convertIntoBufferRange(tableCharacteristicsScheduleInfo.getColumnTypes(), valueForScheduleInfo, tableCharacteristicsScheduleInfo.getCollator());
            ((_UserTableOperations) scheduleInfoTableIteartor).insert(user, bytesForScheduleInfo);
            user.writeToFile();
            Scheduler schedule = new Scheduler(this, databaseName, scheduleName, lastBackupTime, timeIntervalBackup, backupType, backupPath, databaseNameForBackup);
            Thread t = new Thread(schedule);
            addScheduleInScheduleList(schedule);
            t.start();
        } finally {
            user.releaseCluster();
        }
    }

    /**
    * Drops the schedule by first removing it from the arrayList of schedules maintained
    * and stops the thread running for this schedule.
    * Removes the entry of the dropped schedule from the SCHEDULEINFO system table.
    *
    * @param scheduleName name of the schedule to be dropped
    * @throws DException
    */
    public void dropSchedule(String scheduleName) throws DException {
        _DatabaseUser user = null;
        if (!isServerActive) throw new DException("DSE2023", null);
        if (_Server.ISONEDOLLARDB) throw new DException("DSE2054", new Object[] { " Add Schedule " });
        try {
            removeScheduleFromScheduleList(scheduleName);
            PersistentDatabase systemDatabase = (PersistentDatabase) ((IndexDatabase) ((MergeDatabase) getMergeDatabase(DatabaseConstants.SYSTEMDATABASE)).getFileDatabase()).getUnderLyingDatabase();
            if (!systemDatabase.getVersionHandler().isBackUpSupported()) {
                throw new DException("DSE5590", new Object[] { "Drop Schedule" });
            }
            _Table table = systemDatabase.getTable(SystemTables.SCHEDULEINFO);
            DatabaseUserTableIterator scheduleInfoTableIteartor = (DatabaseUserTableIterator) ((_DataTable) table).getIterator();
            _TableCharacteristics tableCharacteristicsScheduleInfo = table.getTableCharacteristics();
            ArrayList list = new ArrayList();
            list.add(SystemTables.SCHEDULEINFO);
            user = systemDatabase.getDatabaseUser(list);
            BufferRange bytesOfScheduleName = new BufferRange(CCzufDpowfsufs.getBytes(scheduleName, scheduleName.length(), false));
            CTusjohJoTfotjujwfDpnqbsbups stringComparator = new CTusjohJoTfotjujwfDpnqbsbups();
            if (scheduleInfoTableIteartor.first()) {
                do {
                    BufferRange bytesGot = (BufferRange) scheduleInfoTableIteartor.getColumnValues(1);
                    if (stringComparator.compare(bytesGot, bytesOfScheduleName) == 0) {
                        ((_UserTableOperations) scheduleInfoTableIteartor).delete(user);
                        break;
                    }
                } while (scheduleInfoTableIteartor.next());
            }
            user.writeToFile();
        } catch (DException ex) {
            if (!ex.getDseCode().equalsIgnoreCase("DSE959")) throw ex;
        } finally {
            user.releaseCluster();
        }
    }

    private long getTimeIntervalForBackupInMillis(String time) {
        if (time.indexOf("YEARS") != -1) {
            long size = Long.parseLong(time.substring(0, time.indexOf("YEARS")).trim());
            return size * 365 * 24 * 60 * 60 * 1000;
        }
        if (time.indexOf("MONTHS") != -1) {
            long size = Long.parseLong(time.substring(0, time.indexOf("MONTHS")).trim());
            return size * 30 * 24 * 60 * 60 * 1000;
        }
        if (time.indexOf("WEEKS") != -1) {
            long size = Long.parseLong(time.substring(0, time.indexOf("WEEKS")).trim());
            return size * 7 * 24 * 60 * 60 * 1000;
        }
        if (time.indexOf("DAYS") != -1) {
            long size = Long.parseLong(time.substring(0, time.indexOf("DAYS")).trim());
            return size * 24 * 60 * 60 * 1000;
        }
        if (time.indexOf("HOURS") != -1) {
            long size = Long.parseLong(time.substring(0, time.indexOf("HOURS")).trim());
            return size * 60 * 60 * 1000;
        }
        if (time.indexOf("MINUTES") != -1) {
            long size = Long.parseLong(time.substring(0, time.indexOf("MINUTES")).trim());
            return size * 60 * 1000;
        }
        if (time.indexOf("SECONDS") != -1) {
            long size = Long.parseLong(time.substring(0, time.indexOf("SECONDS")).trim());
            return size * 1000;
        }
        return 0;
    }

    public void addScheduleInScheduleList(Object obj) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (_Server.ISONEDOLLARDB) throw new DException("DSE2054", new Object[] { " Add Schedule " });
        scheduleList.add(obj);
    }

    /**
    * searches the schedule to be droped from the schedule list and if exists
    * removes schedule from scheduleList and Stops the thread running on this Schedule
    *
    * @param scheduleName name of the schedule to be dropped
    */
    private void removeScheduleFromScheduleList(String scheduleName) {
        for (int i = 0; i < scheduleList.size(); i++) {
            Scheduler scheduleEntry = (Scheduler) scheduleList.get(i);
            if (scheduleEntry.getScheduleName().equalsIgnoreCase(scheduleName)) {
                ((Scheduler) scheduleList.get(i)).stopThread();
                scheduleList.remove(i);
                break;
            }
        }
    }

    private void removeAllSchedules() {
        for (int i = 0; i < scheduleList.size(); i++) {
            ((Scheduler) scheduleList.get(i)).stopThread();
            scheduleList.remove(i);
        }
    }

    private void checkUserValidity(String databaseName, String userName, String password) throws DException {
        try {
            Properties prop = new Properties();
            prop.setProperty(_Server.USER, userName);
            prop.setProperty(_Server.PASSWORD, password);
            prop.setProperty(_Server.CREATE, "true");
            _Connection conn = getConnection(databaseName, prop);
            conn.close();
        } catch (DException ex) {
            if (ex.getDseCode().equalsIgnoreCase("DSE1210")) throw new DException("DSE5575", null); else throw ex;
        }
    }

    private void checkConnections(String databaseName) throws DException {
        synchronized (databaseName) {
            ArrayList list = (ArrayList) serverSessionList.get(databaseName);
            int noOfConnection = 0;
            if (list != null) {
                synchronized (list) {
                    noOfConnection = list.size();
                }
            }
            if (noOfConnection == 1) {
                WeakReference wk = (WeakReference) list.get(0);
                _ServerSession session = (_ServerSession) wk.get();
                if (session.getUserSession().getUserName().equalsIgnoreCase("_Server")) {
                    session.close();
                } else throw new DException("DSE5519", new Object[] { databaseName });
            } else if (noOfConnection == 0) {
                PersistentDatabase pd = (PersistentDatabase) persistentSystem.getDatabase(databaseName);
                dataDictionarySystem.deleteDatabase(databaseName);
                dataTriggerSystem.deleteDatabase(databaseName);
                statementTriggerSystem.deleteDatabase(databaseName);
                serverSessionList.remove(databaseName);
                sessionSystem.deleteDatabase(databaseName, true, false);
            } else throw new DException("DSE5519", new Object[] { databaseName });
        }
    }

    public ArrayList getScheduleForDatabase(String databaseName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        if (_Server.ISONEDOLLARDB) throw new DException("DSE2054", new Object[] { " Get Schedule " });
        PersistentDatabase systemDatabase = (PersistentDatabase) ((IndexDatabase) ((MergeDatabase) getMergeDatabase(DatabaseConstants.SYSTEMDATABASE)).getFileDatabase()).getUnderLyingDatabase();
        if (!systemDatabase.getVersionHandler().isBackUpSupported()) {
            throw new DException("DSE5590", new Object[] { "Scheduling" });
        }
        ArrayList scheduleNames = new ArrayList();
        _DatabaseUser user = null;
        try {
            _Table table = systemDatabase.getTable(SystemTables.SCHEDULEINFO);
            DatabaseUserTableIterator scheduleInfoTableIteartor = (DatabaseUserTableIterator) ((_DataTable) table).getIterator();
            _TableCharacteristics tableCharacteristicsScheduleInfo = table.getTableCharacteristics();
            ArrayList list = new ArrayList();
            list.add(SystemTables.SCHEDULEINFO);
            user = systemDatabase.getDatabaseUser(list);
            BufferRange bytesOfDatabaseName = new BufferRange(CCzufDpowfsufs.getBytes(databaseName, databaseName.length(), false));
            CTusjohJoTfotjujwfDpnqbsbups stringComparator = new CTusjohJoTfotjujwfDpnqbsbups();
            if (scheduleInfoTableIteartor.first()) {
                do {
                    BufferRange bytesGot = (BufferRange) scheduleInfoTableIteartor.getColumnValues(0);
                    if (stringComparator.compare(bytesGot, bytesOfDatabaseName) == 0) {
                        scheduleNames.add(CCzufDpowfsufs.getString(((BufferRange) scheduleInfoTableIteartor.getColumnValues(1)).getBytes()));
                    }
                } while (scheduleInfoTableIteartor.next());
            }
            user.releaseCluster();
        } catch (DException ex) {
            if (!ex.getDseCode().equalsIgnoreCase("DSE959")) throw ex;
        } finally {
            user.releaseCluster();
        }
        return scheduleNames;
    }

    /**
    * Rename the file to the log file and make xml file empty
    * Read SessionId From the XML File, Check if the session id is present in
    * the map if not, create new connection and put it into map.
    * Fire the corresponding method or the query with that new created or
    * mapped connection to restore the particular state.
    **/
    public void restoreSaveMode(String oldDBName, String newdbName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        String oldDBHome = System.getProperty(ServerSystem.DAFFODILDB_HOME) + File.separator + oldDBName + File.separator;
        String xmlFilePath = oldDBHome + SaveModeConstants.XML_FILENAME;
        appendRootTag(xmlFilePath);
        restoreStatements(oldDBHome, newdbName);
        createLogFile(xmlFilePath);
    }

    private void appendRootTag(String xmlFilePath) throws DException {
        try {
            FileOutputStream fos = new FileOutputStream(xmlFilePath, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write("\n</transactions>");
            osw.close();
            fos.close();
        } catch (IOException ex) {
            throw new DException("DSE0", new Object[] { "Save Mode File Close Error " + ex.getMessage() });
        }
    }

    private void restoreStatements(String oldDBHome, String newDBName0) throws DException {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            XMLReader reader = parser.getXMLReader();
            ContentHandler handler = new OperationHandler(this, newDBName0, oldDBHome);
            reader.setContentHandler(handler);
            reader.parse(oldDBHome + SaveModeConstants.XML_FILENAME);
        } catch (SAXException ex1) {
            ex1.printStackTrace();
            throw new DException("DSE0", new Object[] { " Restore Operation Error " });
        } catch (Exception ex1) {
            ex1.printStackTrace();
            throw new DException("DSE0", new Object[] { " Restore Operation Error " });
        }
    }

    private void createLogFile(String xmlFilePath) throws DException {
        int logFileIndex = 0;
        while ((new File(xmlFilePath.substring(0, xmlFilePath.indexOf(SaveModeConstants.XML_FILENAME)) + SaveModeConstants.LOG_FILEPREFIX + ++logFileIndex + ".xml")).exists()) ;
        if (!new File(xmlFilePath).renameTo(new File(xmlFilePath.substring(0, xmlFilePath.indexOf(SaveModeConstants.XML_FILENAME)) + SaveModeConstants.LOG_FILEPREFIX + logFileIndex + ".xml"))) throw new DException("DSE0", new Object[] { " Log creation error " });
    }

    public _DataSystem getPersistentSystem() throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return persistentSystem;
    }

    private void createIndexForDirtyTransactions(QualifiedIdentifier tableName, IndexDatabase indexDatabase) throws DException {
        String indexName = "dirty_distributions_index";
        QualifiedIdentifier indexTableName = new QualifiedIdentifier(tableName.catalog, tableName.schema, indexName);
        Object[][] indexStructure = getDirtyDistributionsIndexStructure();
        IndexInformations indexInformationGetter = new IndexInformations(tableName, indexName, -1, false, indexStructure, indexDatabase.getVersionHandler());
        indexDatabase.createPermanantIndexForSystemTable(tableName, indexName, indexInformationGetter);
    }

    private Object[][] getDirtyDistributionsIndexStructure() {
        return new Object[][] { { SystemFields.systemFields[SystemFields.sessionId], Boolean.TRUE, new Integer(0), new Integer(0) } };
    }

    public _DataTriggerDatabase getDataTriggerDatabase(String url) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return dataTriggerSystem.getDataTrigerDatabase(url);
    }

    public void refreshTriggers(String databaseName, QualifiedIdentifier tableName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        ArrayList list = (ArrayList) serverSessionList.get(databaseName);
        if (list != null) {
            synchronized (list) {
                for (int i = list.size(); i-- > 0; ) {
                    WeakReference wk = (WeakReference) list.get(i);
                    Object session = wk.get();
                    if (session != null) {
                        ((_ServerSession) session).refreshTriggerInfo(tableName);
                    }
                }
            }
        }
    }

    public String getDaffodilHome() throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return daffodilHome;
    }

    public boolean getReadOnlyMode() throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        return isReadOnlyMode;
    }

    public void refreshConstraintTable(String databaseURL, QualifiedIdentifier tableName) throws DException {
        if (!isServerActive) throw new DException("DSE2023", null);
        _DataTriggerDatabase dataTriggerDatabase = getDataTriggerDatabase(databaseURL);
        dataTriggerDatabase.refreshConstraint(tableName);
    }

    private void checkDatabaseInUseBelowJavaVersion1_4() throws DException {
        String logFile = daffodilHome + File.separator + "log.lg";
        File ff = new File(logFile);
        boolean isDelete = false;
        File dirExist = new File(daffodilHome);
        if (!dirExist.exists()) {
            if (!dirExist.mkdirs()) {
                throw new DException("DSE5523", new Object[] {});
            }
        }
        if (ff.exists()) {
            isDelete = ff.delete();
            if (!isDelete) throw new DException("DSE5522", new Object[] {});
        }
        try {
            raf = new RandomAccessFile(ff, "rw");
        } catch (FileNotFoundException ex) {
        }
    }

    /**
    * New Method by harvinder related to bug 12052. */
    public void checkIsValidUserBeforeDropDatabase(String userName, _Connection conn) throws DException {
        DataDictionary dd = (DataDictionary) ((_ServerSession) conn).getDataDictionary();
        _SelectQueryIterator schemaIterator = (_SelectQueryIterator) dd.getPreparedStatementGetter().getSchemataTableExecuter().executeForFresh(new Object[] { "users", "users" });
        if (schemaIterator.first()) {
            Object[] ob = (Object[]) schemaIterator.getObject();
            for (int i = 0; i < ob.length; i++) {
                if (!(userName.equalsIgnoreCase(ob[2].toString()) || userName.equalsIgnoreCase(ServerSystem.browserUser))) {
                    conn.close();
                    throw new DException("DSE8179", new Object[] { userName });
                }
            }
        }
    }

    public boolean isUserActiveMoreThanOnceOnSameDatabase(String databaseName, String userName) throws DException {
        ArrayList listOfConnections = (ArrayList) serverSessionList.get(databaseName);
        if (listOfConnections == null || listOfConnections.size() == 0) return false;
        int count = 0;
        synchronized (listOfConnections) {
            for (int i = listOfConnections.size(); i-- > 0; ) {
                WeakReference wk = (WeakReference) listOfConnections.get(i);
                Object obj = wk.get();
                if (obj != null) if (((_Connection) obj).getCurrentUser().equalsIgnoreCase(userName)) {
                    count++;
                    if (count > 1) return true;
                }
            }
        }
        return false;
    }
}

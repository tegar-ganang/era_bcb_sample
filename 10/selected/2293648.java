package com.daffodilwoods.daffodildb.server.serversystem;

import java.sql.*;
import java.util.*;
import com.daffodilwoods.daffodildb.server.datadictionarysystem.*;
import com.daffodilwoods.daffodildb.server.datasystem.indexsystem.*;
import com.daffodilwoods.daffodildb.server.datasystem.interfaces.*;
import com.daffodilwoods.daffodildb.server.datasystem.mergesystem.*;
import com.daffodilwoods.daffodildb.server.datasystem.persistentsystem.*;
import com.daffodilwoods.daffodildb.server.serversystem.chainedcolumn.*;
import com.daffodilwoods.daffodildb.server.serversystem.datatriggersystem.*;
import com.daffodilwoods.daffodildb.server.serversystem.deeprecordcopy.*;
import com.daffodilwoods.daffodildb.server.serversystem.dmlvalidation.statementtriggersystem.*;
import com.daffodilwoods.daffodildb.server.serversystem.triggerInfo.*;
import com.daffodilwoods.daffodildb.server.sessionsystem.*;
import com.daffodilwoods.daffodildb.server.sessionsystem.sessionversioninfo.*;
import com.daffodilwoods.daffodildb.server.sql99.*;
import com.daffodilwoods.daffodildb.server.sql99.common.*;
import com.daffodilwoods.daffodildb.server.sql99.dcl.sqlcontrolstatement.*;
import com.daffodilwoods.daffodildb.server.sql99.dcl.sqlsessionstatement.*;
import com.daffodilwoods.daffodildb.server.sql99.dcl.sqltransactionstatement.*;
import com.daffodilwoods.daffodildb.server.sql99.ddl.schemadefinition.*;
import com.daffodilwoods.daffodildb.server.sql99.ddl.schemamanipulation.*;
import com.daffodilwoods.daffodildb.server.sql99.dml.*;
import com.daffodilwoods.daffodildb.server.sql99.dml.declarecursor.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.execution.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.iterator.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.iterator.condition.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.iterator.order.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.listenerevents.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.plan.table.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.queryexpression.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.queryexpression.orderbyclause.*;
import com.daffodilwoods.daffodildb.server.sql99.expression.booleanvalueexpression.*;
import com.daffodilwoods.daffodildb.server.sql99.utils.*;
import com.daffodilwoods.daffodildb.utils.*;
import com.daffodilwoods.daffodildb.utils.byteconverter.*;
import com.daffodilwoods.daffodildb.utils.comparator.*;
import com.daffodilwoods.daffodildb.utils.parser.*;
import com.daffodilwoods.database.general.*;
import com.daffodilwoods.database.resource.*;

public class ServerSession implements _ServerSession {

    protected _UserSession userSession;

    public ServerSystem serverSystem;

    protected String databaseURL;

    private WeakOrderedKeyList serverTableList = new WeakOrderedKeyList(new CTusjohDbtfJoTfotjujwfDpnqbsbups());

    private String moduleSchema = SystemTables.DEFAULT_SCHEMA;

    private String moduleCatalog = SystemTables.DEFAULT_CATALOG;

    private String currentSchema = SystemTables.DEFAULT_SCHEMA;

    private String currentCatalog = SystemTables.DEFAULT_CATALOG;

    private HashMap viewMap = new HashMap();

    private TreeMap cursorPool = new TreeMap(String.CASE_INSENSITIVE_ORDER);

    private HashMap materializedViewTableMap = new HashMap();

    _Iterator foreignKeyIterator;

    boolean commit = true;

    private boolean checkConstraint = false;

    public boolean verbose = true;

    public int queryTimeOut;

    private int type = IteratorConstants.NONSCROLLABLE;

    protected boolean connectionStatus = false;

    private HashMap iteratorsList = new HashMap();

    protected String verboseUser;

    private _DataTriggerDatabase datatriggerDatabase;

    protected ArrayList createIndexesList = new ArrayList(4);

    private TriggerInformationStore triggerInfoStore;

    private TriggerInformationStore statementLevelTriggerInfoStore;

    public ServerSession() throws DException {
    }

    public ServerSession(String databaseURL, _UserSession userSession, ServerSystem serverSystem) throws DException {
        this.databaseURL = databaseURL;
        this.userSession = userSession;
        this.serverSystem = serverSystem;
        triggerInfoStore = new TriggerInformationStore();
        statementLevelTriggerInfoStore = new TriggerInformationStore();
    }

    protected void finalize() {
        try {
            if (connectionStatus == false) {
                serverSystem.close(databaseURL, this);
            }
        } catch (DException ex) {
        }
    }

    public void commit() throws DException {
        if (connectionStatus) throw new DatabaseException("DSE279", null);
        if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
        if (commit) {
            _StatementExecutionContext statementExecutionContext = getStatementExecutionContext();
            _TriggerExecutionContext triggerExecutionContext = new TriggerExecutionContext();
            statementExecutionContext.setTriggerExecutionContext(triggerExecutionContext);
            int length = createIndexesList.size();
            if (length > 0) {
                HashMap tablesRefreshed = new HashMap(5);
                QualifiedIdentifier tableName;
                for (int i = 0; i < length; i++) {
                    IndexInfo indexInfo = (IndexInfo) createIndexesList.get(i);
                    tableName = indexInfo.getTableName();
                    if (indexInfo.type == IndexInfo.CREATEINDEX) {
                        _IndexCharacteristics ic = serverSystem.getDataDictionary(databaseURL).getIndexCharacteristics(tableName);
                        if (tablesRefreshed.get(tableName) == null) {
                            ic.refresh();
                            tablesRefreshed.put(tableName, "");
                        }
                        boolean isNonUnique = indexInfo.getIsNonUnique();
                        _IndexInformation iif = ic.getIndexInformations(indexInfo.getIndexName(), isNonUnique);
                        if (isNonUnique) ((MergeDatabase) serverSystem.getMergeDatabase(databaseURL)).getSessionVersionHandler().update(iif);
                        indexInfo.setIndexInformation(iif);
                    }
                }
            }
            statementExecutionContext.setCreateIndexList(createIndexesList);
            try {
                userSession.commit(statementExecutionContext);
            } finally {
                createIndexesList.clear();
            }
            userSession.getSavePointVector().clear();
        }
    }

    public void rollback() throws DException {
        if (connectionStatus) throw new DatabaseException("DSE279", null);
        if (getUserSession().getTransactionAccessMode().equals("Read Only")) throw new DException("DSE1184", (Object[]) null);
        if (commit) {
            _StatementExecutionContext statementExecutionContext = getStatementExecutionContext();
            TriggerExecutionContext triggerExecutionContext = new TriggerExecutionContext();
            statementExecutionContext.setTriggerExecutionContext(triggerExecutionContext);
            try {
                userSession.rollback(statementExecutionContext);
            } finally {
                createIndexesList.clear();
            }
            userSession.getSavePointVector().clear();
        }
    }

    public synchronized _ServerTable getServerTable(QualifiedIdentifier tableName) throws DException {
        if (connectionStatus) throw new DatabaseException("DSE279", null);
        _ServerTable serverTable = (_ServerTable) serverTableList.get(tableName.getIdentifier());
        if (serverTable != null) return serverTable;
        _DataTriggerTable dataTriggerTable = getDataTriggerTable(tableName);
        _StatementTriggerTable statementTriggerTable = serverSystem.getStatementTriggerTable(databaseURL, tableName);
        serverTable = new ServerTable(tableName, this, dataTriggerTable, statementTriggerTable);
        serverTableList.put(tableName.getIdentifier(), serverTable);
        return serverTable;
    }

    public _StatementExecutionContext getStatementExecutionContext() throws DException {
        StatementExecutionContext statementExecutionContext = new StatementExecutionContext();
        statementExecutionContext.setServerSession(this);
        statementExecutionContext.setServerSystem(serverSystem);
        return statementExecutionContext;
    }

    public String getDatabaseURL() throws DException {
        return this.databaseURL;
    }

    public _UserSession getUserSession() throws DException {
        return userSession;
    }

    public _ColumnCharacteristics getColumnCharacteristics(QualifiedIdentifier tableName) throws DException {
        if (connectionStatus) throw new DatabaseException("DSE279", null);
        _DataDictionarySystem dataDictionarySystem = serverSystem.getDataDictionarySystem();
        return dataDictionarySystem.getDataDictionary(databaseURL).getColumnCharacteristics(tableName, true);
    }

    public void setTimeOut(int timeOut) throws DException {
        queryTimeOut = timeOut;
    }

    public void setModuleSchema(String moduleSchema) throws DException {
        this.moduleSchema = moduleSchema;
    }

    public String getModuleSchema() throws DException {
        return moduleSchema;
    }

    public void setModuleCatalog(String moduleCatalog) throws DException {
        this.moduleCatalog = moduleCatalog;
    }

    public String getModuleCatalog() throws DException {
        return moduleCatalog;
    }

    public String getCurrentRole() throws DException {
        return userSession.getCurrentRole();
    }

    public String getCurrentUser() throws DException {
        return userSession.getCurrentUser();
    }

    public java.sql.Date getDate() throws DException {
        return userSession.getDate();
    }

    public Time getTime() throws DException {
        return userSession.getTime();
    }

    public void deleteTable(QualifiedIdentifier tableName, boolean dropTable) throws DException {
        try {
            serverSystem.deleteTable(databaseURL, tableName, dropTable);
        } catch (DatabaseException e) {
            throw e;
        }
    }

    public void setCurrentSchema(String currentSchema) throws DException {
        this.currentSchema = currentSchema;
    }

    public String getCurrentSchema() throws DException {
        return currentSchema;
    }

    public void setCurrentCatalog(String currentCatalog) throws DException {
        this.currentCatalog = currentCatalog;
    }

    public String getCurrentCatalog() throws DException {
        return currentCatalog;
    }

    public boolean isEnabledAuthorizationIdentifier(String authorizationIdentifier, boolean checkBrowserUser) throws DException {
        if (checkBrowserUser && getCurrentUser().equalsIgnoreCase(ServerSystem.browserUser)) return !authorizationIdentifier.equalsIgnoreCase(SystemTables.SYSTEM);
        return userSession.isEnabledAuthorizationIdentifier(authorizationIdentifier);
    }

    public String getAuthorizationIdentifier() throws DException {
        return userSession.getAuthorizationIdentifier();
    }

    public void setTransactionMode(SessionTransactionMode sessionTransactionMode) throws DException {
        userSession.setTransactionMode(sessionTransactionMode);
    }

    public SessionTransactionMode getTransactionMode() throws DException {
        return userSession.getTransactionMode();
    }

    public void setUserSession(_UserSession userSession) throws DException {
        this.userSession = userSession;
    }

    public void close() throws DException {
        try {
            userSession.rollback(getStatementExecutionContext());
        } catch (DException e) {
        }
        connectionStatus = true;
        serverSystem.close(databaseURL, this);
    }

    public java.sql.Timestamp getTimeStamp() throws DException {
        return userSession.getTimeStamp();
    }

    public String getDatabase() throws DException {
        int index = databaseURL.lastIndexOf(".");
        String databaseName = databaseURL.substring(index + 1);
        return databaseName;
    }

    public Object getSessionConstant() throws DException {
        return userSession.getSessionConstant();
    }

    public int getIsolationLevel() throws DException {
        return userSession.getSession().getIsolationLevel();
    }

    public SessionConditionInfo getSessionCondition() throws DException {
        throw new UnsupportedOperationException(" getSessionCondition not supported ");
    }

    public Object getSessionId() throws DException {
        return userSession.getSession().getSessionId();
    }

    public _ServerSession getGlobalSession() throws DException {
        return userSession.getGlobalSession();
    }

    public String[] getSuitableIndex(String tableName, String[] columns) throws DException {
        throw new UnsupportedOperationException();
    }

    public boolean prepare() throws DException {
        if (connectionStatus) throw new DException("DSE279", null);
        _StatementExecutionContext statementExecutionContext = getStatementExecutionContext();
        TriggerExecutionContext triggerExecutionContext = new TriggerExecutionContext();
        statementExecutionContext.setTriggerExecutionContext(triggerExecutionContext);
        return userSession.prepare(statementExecutionContext);
    }

    public boolean makePersistent() throws DException {
        if (connectionStatus) throw new DException("DSE279", null);
        _StatementExecutionContext statementExecutionContext = getStatementExecutionContext();
        TriggerExecutionContext triggerExecutionContext = new TriggerExecutionContext();
        statementExecutionContext.setTriggerExecutionContext(triggerExecutionContext);
        statementExecutionContext.setCreateIndexList(createIndexesList);
        return userSession.makePersistent(statementExecutionContext);
    }

    public TimeZone getTimeZone() throws DException {
        return userSession.getTimeZone();
    }

    public _ServerSession getSystemServerSession() throws DException {
        java.util.Properties systemProperties = new java.util.Properties();
        return serverSystem.getServerSession(SystemTables.SYSTEM, SystemTables.SYSTEM, systemProperties, databaseURL);
    }

    public _Connection getSystemConnection() throws DException {
        java.util.Properties systemProperties = new java.util.Properties();
        systemProperties.setProperty(_Server.USER, SystemTables.SYSTEM);
        systemProperties.setProperty(_Server.XID, SystemTables.SYSTEM);
        return serverSystem.get_Connection(databaseURL, systemProperties);
    }

    public _DataDictionary getDataDictionary() throws DException {
        return serverSystem.getDataDictionarySystem().getDataDictionary(databaseURL);
    }

    public void setAutoCommit(boolean autoCommit0) throws DException {
        if (userSession.getAutoCommit() != autoCommit0) {
            if (userSession.getAutoCommit() == false) commit();
            userSession.setAutoCommit(autoCommit0);
        }
    }

    public void createDatabase(String databaseName, Properties prop) throws DException {
        serverSystem.createDatabase(databaseName, prop);
    }

    public void createTable(QualifiedIdentifier tableName) throws DException {
        _ColumnCharacteristics cc = getColumnCharacteristics(tableName);
        String country = ((ColumnCharacteristics) cc).getCountry();
        String[] columns = cc.getColumnNames();
        Object[][] columnsDetail = new Object[columns.length][4];
        for (int i = 0; i < columns.length; i++) {
            columnsDetail[i][0] = columns[i];
            int index = cc.getColumnIndex(columns[i]);
            int type = cc.getColumnType(index);
            int size = cc.getSize(index);
            size = country != null && ColumnCharacteristicsUtilities.isStringType(type) ? (size + 1) * 2 : size;
            columnsDetail[i][1] = new Long(type);
            columnsDetail[i][2] = new Integer(size);
            columnsDetail[i][3] = new Long(index);
        }
        ColumnInformation columnInfo = new ColumnInformation();
        if (country != null) {
            columnInfo.setCountry(country);
            columnInfo.setLanguage(((ColumnCharacteristics) cc).getLanguage());
        }
        columnInfo.setObjects(columnsDetail);
        _Database mergeDatabase = serverSystem.getMergeDatabase(databaseURL);
        mergeDatabase.createTable(tableName, columnInfo);
    }

    public _Iterator getIterator(QualifiedIdentifier tableName, _SingleTableExecuter singleTableExecuter) throws DException {
        if (connectionStatus) throw new DatabaseException("DSE279", null);
        if (singleTableExecuter.getInternalIteratorRequired()) return userSession.getUserSessionTable(tableName).getInternalIterator(singleTableExecuter, this); else return userSession.getUserSessionTable(tableName).getIterator(singleTableExecuter, this);
    }

    public Object execute(String query, int queryTimeOut) throws DException {
        return execute(query, queryTimeOut, getType());
    }

    public Object execute(String query, int queryTimeOut, int type) throws DException {
        if (verbose) PrintHandler.print(query, null, verboseUser);
        if (connectionStatus) throw new DException("DSE279", null);
        try {
            Object parsedQuery = Parser.parseQuery(query);
            return execute(parsedQuery, queryTimeOut, type);
        } catch (DException de) {
            throw de;
        } catch (RuntimeException de) {
            throw de;
        }
    }

    public Object execute(Object parsedQuery, int queryTimeOut, int type) throws DException {
        if (parsedQuery instanceof queryexpression) {
            {
                userSession.startTransaction();
            }
            int tp = getType();
            queryexpression qurexp = (queryexpression) parsedQuery;
            ServerSessionWrapper sersewr = new ServerSessionWrapper(this);
            if (qurexp.isSimpleQuery(sersewr)) {
                if (type == IteratorConstants.UPDATABLE) {
                    tp = IteratorConstants.UPDATABLE;
                }
            } else {
                tp = IteratorConstants.NONSCROLLABLE;
            }
            sersewr.setType(tp);
            _Executer executer = (_Executer) (qurexp).run(sersewr);
            Object obj = executer.execute((Object[]) null);
            return obj;
        }
        if (parsedQuery instanceof SQLschemastatement) {
            commit();
            _ServerSession ss = getSystemServerSession();
            if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
            Object returnedObject = null;
            _DataDictionary dd = getDataDictionary();
            dd.lockDDL();
            try {
                try {
                    returnedObject = ((SQLschemastatement) parsedQuery).run(this);
                } catch (DException ex) {
                    dd.restoreGeneratedKeys();
                    createIndexesList.clear();
                    ss.rollback();
                    throw ex;
                }
                if (!(parsedQuery instanceof dropdatabasestatement)) {
                    ((ServerSession) ss).createIndexesList = createIndexesList;
                    ss.commit();
                    createIndexesList.clear();
                }
            } finally {
                dd.releaseDDL();
                ss.close();
            }
            if (returnedObject == null) return new Integer(Integer.MIN_VALUE);
            return returnedObject;
        }
        if (parsedQuery instanceof SQLtransactionstatement) {
            Object returnedObject = ((SQLtransactionstatement) parsedQuery).run(this);
            return new Integer(Integer.MIN_VALUE);
        }
        if (parsedQuery instanceof SQLsessionstatement) {
            _StatementExecutionContext sec = getStatementExecutionContext();
            Object returnedObject = ((SQLsessionstatement) parsedQuery).run(sec);
            return new Integer(Integer.MIN_VALUE);
        }
        if (parsedQuery instanceof SQLdatastatement) {
            if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
            _StatementExecutionContext sec = getStatementExecutionContext();
            _Executer executer = (_Executer) ((SQLdatastatement) parsedQuery).run(sec);
            Object returnedObject = executer.execute((_VariableValues) null);
            if (returnedObject == null) return new Integer(Integer.MIN_VALUE);
            return returnedObject;
        }
        if (parsedQuery instanceof SQLcontrolstatement) {
            _StatementExecutionContext sec = getStatementExecutionContext();
            _Executer executer = (_Executer) ((SQLcontrolstatement) parsedQuery).run(sec);
            Object returnedObject = executer.execute((_VariableValues) null);
            ((CallResult) returnedObject).print();
            if (returnedObject == null) return new Integer(Integer.MIN_VALUE);
            return returnedObject;
        }
        throw new DException("DSE531", null);
    }

    public Object execute(String query, int queryTimeOut, int type, int queryType, int autoGeneratedType, Object autoGenetatedValues) throws DException {
        try {
            Object parsedQuery = Parser.parseQuery(query);
            if (parsedQuery instanceof SQLdatastatement) {
                if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
                _StatementExecutionContext sec = getStatementExecutionContext();
                sec.setAutoGeneratedInfo(autoGeneratedType, autoGenetatedValues);
                _Executer executer = (_Executer) ((SQLdatastatement) parsedQuery).run(sec);
                return executer.execute((_VariableValues) null);
            } else {
                if (queryType == _Connection.EXECUTE) return execute(parsedQuery, queryTimeOut, type); else return executeUpdate(parsedQuery, queryTimeOut);
            }
        } catch (DException de) {
            throw de;
        } catch (RuntimeException de) {
            throw de;
        }
    }

    public Object executeParameterised(String query, int queryTimeOut) throws DException {
        try {
            Object parsedQuery = Parser.parseQuery(query);
            if (parsedQuery instanceof queryexpression) {
                {
                    userSession.startTransaction();
                }
                queryexpression qurexp = (queryexpression) parsedQuery;
                ServerSessionWrapper serwr = new ServerSessionWrapper(this);
                serwr.setType(IteratorConstants.NONSCROLLABLE);
                if (qurexp.isSimpleQuery(serwr)) return qurexp.run(serwr);
                return qurexp.run(serwr);
            }
            if (parsedQuery instanceof SQLschemastatement) {
                commit();
                _ServerSession ss = getSystemServerSession();
                _DataDictionary dd = getDataDictionary();
                if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
                Object retunObject = null;
                dd.lockDDL();
                try {
                    try {
                        retunObject = ((SQLschemastatement) parsedQuery).run(this);
                    } catch (DException ex) {
                        dd.restoreGeneratedKeys();
                        createIndexesList.clear();
                        ss.rollback();
                        throw ex;
                    }
                    if (!(parsedQuery instanceof dropdatabasestatement)) {
                        ((ServerSession) ss).createIndexesList = createIndexesList;
                        ss.commit();
                        createIndexesList.clear();
                    }
                } finally {
                    dd.releaseDDL();
                }
                return retunObject;
            }
            if (parsedQuery instanceof SQLtransactionstatement) {
                ((SQLtransactionstatement) parsedQuery).run(this);
                return new Integer(Integer.MIN_VALUE);
            }
            if (parsedQuery instanceof SQLsessionstatement) {
                _StatementExecutionContext sec = getStatementExecutionContext();
                ((SQLsessionstatement) parsedQuery).run(sec);
                return new Integer(Integer.MIN_VALUE);
            }
            if (parsedQuery instanceof SQLdatastatement) {
                if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
                _StatementExecutionContext sec = getStatementExecutionContext();
                return ((SQLdatastatement) parsedQuery).run(sec);
            }
            if (parsedQuery instanceof SQLcontrolstatement) {
                _StatementExecutionContext sec = getStatementExecutionContext();
                return ((SQLcontrolstatement) parsedQuery).run(sec);
            }
            if (parsedQuery instanceof SQLprocedurestatement) {
                if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
                return (_Executer) ((SQLprocedurestatement) parsedQuery).run(this);
            }
        } catch (DException de) {
            throw de;
        } catch (RuntimeException de) {
            throw de;
        }
        throw new DException("DSE86", null);
    }

    public Object executeUpdate(String query, int queryTimeOut) throws DException {
        if (verbose) PrintHandler.print(query, null, verboseUser);
        try {
            if (connectionStatus) throw new DException("DSE279", null);
            Object parsedObject = Parser.parseQuery(query);
            return executeUpdate(parsedObject, queryTimeOut);
        } catch (DException de) {
            throw de;
        } catch (RuntimeException de) {
            throw de;
        }
    }

    public Object executeUpdate(Object parsedObject, int queryTimeOut) throws DException {
        if (parsedObject instanceof SQLdatastatement) {
            if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
            _StatementExecutionContext sec = getStatementExecutionContext();
            _Executer executer = (_Executer) ((SQLdatastatement) parsedObject).run(sec);
            Object returnedObject = executer.execute((_VariableValues) null);
            if (returnedObject == null) return new Integer(Integer.MIN_VALUE);
            return returnedObject;
        } else if (parsedObject instanceof SQLschemastatement) {
            commit();
            _ServerSession ss = getSystemServerSession();
            if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
            Object returnedObject = null;
            _DataDictionary dd = getDataDictionary();
            dd.lockDDL();
            try {
                try {
                    returnedObject = ((SQLschemastatement) parsedObject).run(this);
                } catch (DException ex) {
                    dd.restoreGeneratedKeys();
                    createIndexesList.clear();
                    ss.rollback();
                    throw ex;
                }
                if (!(parsedObject instanceof dropdatabasestatement)) {
                    ((ServerSession) ss).createIndexesList = createIndexesList;
                    ss.commit();
                    createIndexesList.clear();
                }
            } finally {
                dd.releaseDDL();
            }
            if (returnedObject == null) return new Integer(Integer.MIN_VALUE);
            return returnedObject;
        }
        throw new DException("DSE533", null);
    }

    public Object executeUpdateParameterised(String query, int queryTimeOut) throws DException {
        try {
            if (connectionStatus) throw new DException("DSE279", null);
            Object parsedObject = Parser.parseQuery(query);
            if (parsedObject instanceof SQLdatastatement) {
                if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
                _StatementExecutionContext sec = getStatementExecutionContext();
                return ((StatementExecuter) parsedObject).run(sec);
            } else if (parsedObject instanceof SQLschemastatement) {
                commit();
                _ServerSession ss = getSystemServerSession();
                if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
                _DataDictionary dd = getDataDictionary();
                dd.lockDDL();
                try {
                    try {
                        ((SQLschemastatement) parsedObject).run(this);
                    } catch (DException ex) {
                        dd.restoreGeneratedKeys();
                        createIndexesList.clear();
                        ss.rollback();
                        throw ex;
                    }
                    if (!(parsedObject instanceof dropdatabasestatement)) {
                        ((ServerSession) ss).createIndexesList = createIndexesList;
                        ss.commit();
                        createIndexesList.clear();
                    }
                } finally {
                    dd.releaseDDL();
                }
                return new Integer(Integer.MIN_VALUE);
            }
        } catch (DException de) {
            throw de;
        } catch (RuntimeException de) {
            throw de;
        }
        throw new DException("DSE533", null);
    }

    public Object executeQuery(String query, int queryTimeOut) throws DException {
        return executeQuery(query, queryTimeOut, getType());
    }

    public Object executeQuery(String query, int queryTimeOut, int type) throws DException {
        if (verbose) PrintHandler.print(query, null, verboseUser);
        try {
            if (connectionStatus) throw new DException("DSE279", null);
            Object parsedQuery = Parser.parseQuery(query);
            if (parsedQuery instanceof queryexpression) {
                {
                    userSession.startTransaction();
                }
                queryexpression qurexp = (queryexpression) parsedQuery;
                int tp = IteratorConstants.NONSCROLLABLE;
                ServerSessionWrapper sersewr = new ServerSessionWrapper(this);
                if (type == IteratorConstants.UPDATABLE) tp = qurexp.isSimpleQuery(sersewr) ? IteratorConstants.UPDATABLE : IteratorConstants.NONSCROLLABLE;
                sersewr.setType(tp);
                _Executer executer = (_Executer) qurexp.run(sersewr);
                return executer.execute((Object[]) null);
            }
        } catch (DException de) {
            throw de;
        } catch (RuntimeException de) {
            throw de;
        }
        throw new DException("DSE86", null);
    }

    public Object executeQueryParameterised(String query, int queryTimeOut) throws DException {
        try {
            if (connectionStatus) throw new DException("DSE279", null);
            if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
            Object parsedQuery = Parser.parseQuery(query);
            if (parsedQuery instanceof queryexpression) {
                {
                    userSession.startTransaction();
                }
                queryexpression qurexp = (queryexpression) parsedQuery;
                ServerSessionWrapper serwr = new ServerSessionWrapper(this);
                serwr.setType(IteratorConstants.NONSCROLLABLE);
                if (qurexp.isSimpleQuery(serwr)) return qurexp.run(serwr);
                return qurexp.run(serwr);
            }
        } catch (DException de) {
            throw de;
        } catch (RuntimeException de) {
            throw de;
        }
        throw new DException("DSE86", null);
    }

    public _Iterator getIndexedIterator(_Iterator selectIterator, _ExpressionOrderValues order, CbCUsffWbmvfIboemfs btreeValueHandler) throws DException {
        if (serverSystem.getReadOnlyMode()) {
            return new ReadOnlyTempIndexIterator(selectIterator, order, ((MergeDatabase) serverSystem.getMergeDatabase(databaseURL)).getVersionHandler());
        }
        TempIndexDatabase tempIndexDatabase = (TempIndexDatabase) ((MergeDatabase) serverSystem.getMergeDatabase(databaseURL)).getMemoryDatabase();
        return new TemporaryIndexIterator(selectIterator, order, btreeValueHandler, tempIndexDatabase);
    }

    public _IndexTable getIndexTable(QualifiedIdentifier tableName) throws DException {
        return (_IndexTable) serverSystem.getMergeDatabase(databaseURL).getTable(tableName);
    }

    public int getType() {
        return type;
    }

    public void setType(int type0) {
        type = type0;
    }

    public void createIndex(QualifiedIdentifier tableName, String indexName, boolean isNonUnique) throws DException {
        IndexInfo indexInfo = new IndexInfo(IndexInfo.CREATEINDEX, tableName, indexName, null);
        indexInfo.setIsNonUnique(isNonUnique);
        createIndexesList.add(indexInfo);
    }

    public void createFullTextIndex(QualifiedIdentifier tableName, String indexName, String[] columnName) throws DException {
        MergeDatabase md = ((MergeDatabase) userSession.getSession().getSessionDatabase().getMergeDatabase());
        if (!md.getVersionHandler().isFullTextSupported()) throw new DException("DSE5590", new Object[] { "createFullTextIndex" });
        md.createFullTextIndex(tableName, indexName, columnName);
    }

    public void dropFullTextIndex(QualifiedIdentifier tableName, String indexName) throws DException {
        ((_IndexDatabase) userSession.getSession().getSessionDatabase().getMergeDatabase()).dropFullTextIndex(tableName, indexName);
    }

    public void createIndexForSystemTable(QualifiedIdentifier tableName, String indexName) throws DException {
        createIndex(tableName, indexName, true);
    }

    public void dropTable(QualifiedIdentifier tableName) throws DException {
        serverSystem.getMergeDatabase(databaseURL).dropTable(tableName);
    }

    public void dropIndex(QualifiedIdentifier tableName, String indexName) throws DException {
        IndexInfo indexInfo = new IndexInfo(IndexInfo.DROPINDEX, tableName, indexName, null);
        createIndexesList.add(indexInfo);
        ((_IndexDatabase) userSession.getSession().getSessionDatabase().getMergeDatabase()).dropTemporaryIndex(tableName, indexName);
    }

    public boolean getAutoCommit() {
        return userSession.getAutoCommit();
    }

    public String[] getUniqueInformation(QualifiedIdentifier tableName) throws DException {
        _PrimaryAndUniqueConstraintCharacteristics pucc = getDataDictionary().getDDSConstraintsOperation().getPrimaryAndUniqueConstraintCharacteristics(tableName, false);
        _UniqueConstraint uc = pucc.getPrimaryConstraints();
        return uc == null ? null : uc.getColumnNames();
    }

    public HashMap getViewMap() throws DException {
        return viewMap;
    }

    public _Iterator getForeignKeyIterator(QualifiedIdentifier tableName, _SingleTableExecuter conditionExecuter, _TableAlias[] tableAlias) throws DException {
        if (connectionStatus) throw new DatabaseException("DSE279", null);
        HashMap tablesMapping = new HashMap();
        HashMap columnsMapping = new HashMap();
        ArrayList tableDetailsList = new ArrayList();
        _DataDictionary dic = getDataDictionary();
        ChainedColumnInfo cci = new ChainedColumnInfo(tableName, "");
        ChainedTableInfo cti = new ChainedTableInfo(cci, tableName);
        tablesMapping.put(cci, cti);
        TableDetails tableD0 = new TableDetails();
        tableD0.setTableName(new String[] { tableName.catalog, tableName.schema, tableName.name });
        tableD0.cc = dic.getColumnCharacteristics(tableD0.getQualifiedIdentifier(), true);
        tableDetailsList.add(tableD0);
        for (int i = 0; i < tableAlias.length; i++) {
            String[] columns = tableAlias[i].getColumnName();
            ChainedTableInfo parent = cti;
            for (int j = 0; j < columns.length; j++) {
                QualifiedIdentifier tempName = parent.getTableName();
                ChainedColumnInfo childColumnInfo = new ChainedColumnInfo(tempName, columns[j]);
                ChainedTableInfo childTableInfo = (ChainedTableInfo) tablesMapping.get(childColumnInfo);
                if (childTableInfo != null) {
                    parent = childTableInfo;
                    continue;
                }
                _ColumnCharacteristics cc = dic.getColumnCharacteristics(tempName, true);
                _ReferencingConstraintCharacteristics rcc = dic.getDDSConstraintsOperation().getReferencingConstraintCharacteristics(tempName, false);
                _ReferentialConstraint rc = rcc.getReferencingConstraints(new int[] { cc.getColumnIndex(columns[j]) })[0];
                QualifiedIdentifier childtemp = rc.getReferencedTable();
                TableDetails tableD = new TableDetails();
                tableD.setTableName(new String[] { childtemp.catalog, childtemp.schema, childtemp.name });
                childTableInfo = new ChainedTableInfo(childColumnInfo, childtemp);
                int[] referencedIndexes = rc.getReferencedColumns();
                int columnIndex = cc.getColumnIndex(columns[j]);
                booleanvalueexpression referencingCondition = rc.getReferencingCondition(rc.getReferencingColumns());
                booleanvalueexpression referencedCondition = rc.getReferencedCondition(referencedIndexes);
                _Reference[] referecingCoditionRefs = GeneralPurposeStaticClass.changeReferences(referencingCondition.getParameters(null));
                _Reference[] referencedConditionRefs = GeneralPurposeStaticClass.changeReferences(referencedCondition.getParameters(null));
                TableDetails parentDetails = new TableDetails();
                parentDetails.setTableName(new String[] { tempName.catalog, tempName.schema, tempName.name });
                IteratorInfo ii1 = new IteratorInfo(childtemp, referencedCondition, tableD, tempName, columnIndex, referencedConditionRefs);
                IteratorInfo ii2 = new IteratorInfo(tempName, referencingCondition, parentDetails, childtemp, referencedIndexes[0], referecingCoditionRefs);
                childTableInfo.setReferencingToReferencedTableIteratorInfo(ii1);
                childTableInfo.setReferencedToReferencingTableIteratorInfo(ii2);
                childTableInfo.setParentTableInfo(parent);
                parent.addChildTableInfo(childTableInfo);
                tablesMapping.put(childColumnInfo, childTableInfo);
                tableD.cc = dic.getColumnCharacteristics(tableD.getQualifiedIdentifier(), true);
                tableDetailsList.add(tableD);
                parent = childTableInfo;
                if (j == columns.length - 1) {
                    columnsMapping.put(columns, childColumnInfo);
                }
            }
        }
        ForeignConstraintTable fct = new ForeignConstraintTable(this, tablesMapping, cti, columnsMapping, tableDetailsList);
        _Iterator sessionIterator = userSession.getUserSessionTable(tableName).getForeignConstraintIterator(conditionExecuter, fct);
        foreignKeyIterator = ((SessionIterator) sessionIterator).getForeignKeyIterator();
        return sessionIterator;
    }

    public ForeignKeyIterator getForeignKeyIterator() throws DException {
        if (foreignKeyIterator instanceof ForeignKeyIterator) return (ForeignKeyIterator) foreignKeyIterator;
        if (foreignKeyIterator instanceof NonIndexedFilterIterator) return (ForeignKeyIterator) ((NonIndexedFilterIterator) foreignKeyIterator).getForeignKeyIterator();
        if (foreignKeyIterator instanceof IndexedFilterIterator) return (ForeignKeyIterator) ((IndexedFilterIterator) foreignKeyIterator).getForeignKeyIterator();
        throw new DException("DSE5537", new Object[] { foreignKeyIterator.getClass().getName() });
    }

    public Object[] getForeignConstraintCharacteritics(QualifiedIdentifier tableName, String columnName) throws DException {
        try {
            _ColumnCharacteristics cc = getColumnCharacteristics(tableName);
            if (cc.getTableType() == TypeConstants.VIEW) {
                return getViewObject(tableName, true).getForeignConstraintCharacteritics(columnName, this, cc);
            }
            _ReferencingConstraintCharacteristics rcc = getDataDictionary().getDDSConstraintsOperation().getReferencingConstraintCharacteristics(tableName, false);
            _ReferentialConstraint rc = rcc.getReferencingConstraints(new int[] { cc.getColumnIndex(columnName) })[0];
            QualifiedIdentifier referencedTable = rc.getReferencedTable();
            String[] referencedColumns = rc.getReferencedColumnNames();
            return new Object[] { referencedTable, getColumnCharacteristics(referencedTable), referencedColumns };
        } catch (NullPointerException nex) {
            throw new DException("DSE5538", new Object[] { columnName });
        }
    }

    public synchronized _ViewObject getViewObject(QualifiedIdentifier viewName, boolean checkUserRight) throws DException {
        _DataDictionary dataDictionary = getDataDictionary();
        if (checkUserRight) {
            _PrivilegeTable pt = userSession.getPrivilegeCharacteristics().getPrivilegeTable(viewName);
            boolean hasRight = pt.hasTablePrivileges(_PrivilegeTable.SELECT);
            if (!hasRight) throw new DException("DSE8132", null);
        }
        return dataDictionary.getViewObject(viewName, this);
    }

    public boolean cursorAlreadyCreated(String name) {
        return cursorPool.containsKey(name);
    }

    public void removeCursor(String name) {
        cursorPool.remove(name);
    }

    public void addCursor(String cname, _Cursor cur) {
        cursorPool.put(cname, cur);
    }

    public _Cursor getCursor(String name) {
        return (_Cursor) cursorPool.get(name);
    }

    public int getEstimatedRowCount(QualifiedIdentifier parm1) throws com.daffodilwoods.database.resource.DException {
        return ((_IndexTable) serverSystem.getMergeDatabase(databaseURL).getTable(parm1)).getEstimatedRowCount();
    }

    public void alterTable(QualifiedIdentifier tableName, _ColumnCharacteristics cc, Object defaultValue) throws DException {
        ColumnInformation columnInfo = getColumnInfo(cc);
        _AlterRecord alterRecord = new AlterRecord(columnInfo);
        IndexInfo indexInfo = new IndexInfo(IndexInfo.ALTERTABLE, tableName, columnInfo, alterRecord, defaultValue);
        createIndexesList.add(indexInfo);
    }

    public ColumnInformation getColumnInfo(_ColumnCharacteristics cc) throws DException {
        String[] columns = cc.getColumnNames();
        Object[][] columnsDetail = new Object[columns.length][4];
        for (int i = 0; i < columns.length; i++) {
            columnsDetail[i][0] = columns[i];
            int index = cc.getColumnIndex(columns[i]);
            int type = cc.getColumnType(index);
            int size = cc.getSize(index);
            columnsDetail[i][1] = new Long(type);
            columnsDetail[i][2] = new Integer(size);
            columnsDetail[i][3] = new Long(index);
        }
        ColumnInformation columnInfo = new ColumnInformation();
        columnInfo.setObjects(columnsDetail);
        return columnInfo;
    }

    public Object[] createDeepRecordCopy(QualifiedIdentifier tableName, Object[] keys, String[] tableNames) throws DException {
        HierarchyCreator hr = new HierarchyCreator(tableName, serverSystem.getDataDictionary(databaseURL), tableNames, true);
        hr.createHierarchy();
        TableCopy tableCopy = hr.getRootNode();
        CopyHierarchy ch = new CopyHierarchy(this);
        Object[] val = ch.copy(keys, tableCopy);
        return val;
    }

    public Object[] createDeepRecordCopy(QualifiedIdentifier tableName, Object[] keys) throws DException {
        HierarchyCreator hr = new HierarchyCreator(tableName, serverSystem.getDataDictionary(databaseURL), null, false);
        hr.createHierarchy();
        TableCopy tableCopy = hr.getRootNode();
        CopyHierarchy ch = new CopyHierarchy(this);
        Object[] val = ch.copy(keys, tableCopy);
        return val;
    }

    public boolean hasRecordInMemory(QualifiedIdentifier tableName) throws DException {
        _Database mergeDatabase = serverSystem.getMergeDatabase(databaseURL);
        _Iterator memoryIterator = ((MergeTable) mergeDatabase.getTable(tableName)).getMemoryTable().getIterator(0);
        return memoryIterator.first();
    }

    public void startSavePoint() throws DException {
        userSession.startSavePoint();
    }

    public void commitSavePoint() throws DException {
        userSession.commitSavePoint(getStatementExecutionContext());
    }

    public void rollbackSavePoint() throws DException {
        userSession.rollbackSavePoint(getStatementExecutionContext());
    }

    public void releaseSavePoint() throws DException {
        userSession.releaseSavePoint(getStatementExecutionContext());
    }

    public void setTransactionIsolation(int level) throws DException {
        if (iteratorsList.size() > 0) throw new DException("DSE5503", null);
        userSession.getSession().setIsolationLevel(level);
    }

    public _SequenceManager getSequenceManager() throws DException {
        return serverSystem.getSequenceManager(databaseURL);
    }

    public void dropDatabase(String databaseName, String userName, String password) throws DException {
        serverSystem.dropDatabase(databaseName, userName, password, getDatabase());
    }

    public boolean isConstraintCheckingDeffered() {
        return checkConstraint;
    }

    public void removeTable(QualifiedIdentifier tableName) throws DException {
        serverTableList.remove(tableName.getIdentifier());
        materializedViewTableMap.remove(tableName);
        viewMap.remove(tableName);
        ((UserSession) userSession).removeTable(tableName);
    }

    public boolean isDataModified() throws DException {
        return userSession.getSession().isDataModified();
    }

    public void hideSavePoint() throws DException {
        userSession.hideSavePoint();
    }

    public void unhideSavePoint() throws DException {
        userSession.unhideSavePoint();
    }

    public void ignoreParallelSavePoint() throws DException {
        userSession.ignoreParallelSavePoint();
    }

    public void allowParallelSavePoint() throws DException {
        userSession.allowParallelSavePoint();
    }

    public void checkImmediateConstraintsOnCommit() throws DException {
        userSession.checkImmediateConstraintsOnCommit();
    }

    public void setSavePoint(String savepoint) throws DException {
        ArrayList savePointVector = userSession.getSavePointVector();
        if (!savePointVector.contains(savepoint)) {
            userSession.addSavePoint(savepoint);
            userSession.startSavePoint();
        } else throw new DException("DSE889", new Object[] { savepoint });
    }

    public String setSavePoint() throws DException {
        String savepoint = "sp" + userSession.getSavePointVector().size();
        setSavePoint(savepoint);
        return savepoint;
    }

    public void releaseSavePoint(String savepoint) throws DException {
        ArrayList savePointVector = userSession.getSavePointVector();
        int index = savePointVector.indexOf(savepoint);
        if (index == -1) throw new DException("DSE890", null);
        userSession = commitUptoLevel(userSession, getStatementExecutionContext(), savePointVector.size() - index);
    }

    protected _UserSession commitUptoLevel(_UserSession userSession, _StatementExecutionContext sec, int level) throws com.daffodilwoods.database.resource.DException {
        for (int i = 0; i < level; i++) {
            userSession.commitSavePoint(sec);
            userSession.releaseLastSavePoint();
        }
        return userSession;
    }

    public void rollbackSavePoint(String savepoint) throws DException {
        ArrayList userSavepointVector = userSession.getSavePointVector();
        _StatementExecutionContext sec = getStatementExecutionContext();
        if (savepoint != null) {
            int index = userSavepointVector.indexOf(savepoint);
            if (index < 0) throw new DException("DSE877", null);
            CommitToParentAndRollback(userSession, sec, userSavepointVector.size() - index);
            userSession.rollbackSavePoint(sec);
            userSession.releaseLastSavePoint();
        } else {
            CommitToParentAndRollback(userSession, sec, userSavepointVector.size() + 1);
            userSession.rollback(sec);
        }
    }

    protected void CommitToParentAndRollback(_UserSession userSession, _StatementExecutionContext sec, int level) throws com.daffodilwoods.database.resource.DException {
        for (int i = 0; i < level - 1; i++) {
            userSession.commitSavePoint(sec);
            userSession.releaseLastSavePoint();
        }
    }

    public void setVerboseForCreatingDatabase() {
        verbose = false;
    }

    public void setVerboseUser(String verboseUser0) {
        verboseUser = verboseUser0;
    }

    public void setRole(String roleName) throws DException {
        userSession.setRole(roleName);
    }

    public boolean isActiveAuthorization(String authorization) throws DException {
        return serverSystem.isActiveAuthorization(authorization, databaseURL);
    }

    public _Iterator getInternalIterator(QualifiedIdentifier tableName, _SingleTableExecuter singleTableExecuter) throws DException {
        return userSession.getUserSessionTable(tableName).getInternalIterator(singleTableExecuter, this);
    }

    public ArrayList getCreateIndexesList() {
        return createIndexesList;
    }

    public _DataTriggerTable getDataTriggerTable(QualifiedIdentifier tableName) throws DException {
        if (datatriggerDatabase == null) datatriggerDatabase = serverSystem.getDataTriggerDatabase(databaseURL);
        return datatriggerDatabase.getDataTriggerTable(tableName);
    }

    public double getDbVersion() throws DException {
        return ((MergeDatabase) serverSystem.getMergeDatabase(databaseURL)).getVersionHandler().getDbVersion();
    }

    public void refereshTable(QualifiedIdentifier tableName, boolean dropTable) throws DException {
        serverSystem.refereshTable(databaseURL, tableName, dropTable);
    }

    public _Triggers[] getAfterInsertTrigger(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext) throws DException {
        return triggerInfoStore.getAfterInsertTrigger(triggerCharacteristics, tableName, statementExecutionContext);
    }

    public _Triggers[] getBeforeInsertTrigger(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext) throws DException {
        return triggerInfoStore.getBeforeInsertTrigger(triggerCharacteristics, tableName, statementExecutionContext);
    }

    public _Triggers[] getAfterUpdateTrigger(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext, int[] cols) throws DException {
        return triggerInfoStore.getAfterUpdateTrigger(triggerCharacteristics, tableName, statementExecutionContext, cols);
    }

    public _Triggers[] getBeforeUpdateTrigger(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext, int[] cols) throws DException {
        return triggerInfoStore.getBeforeUpdateTrigger(triggerCharacteristics, tableName, statementExecutionContext, cols);
    }

    public _Triggers[] getAfterDeleteTrigger(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext) throws DException {
        return triggerInfoStore.getAfterDeleteTrigger(triggerCharacteristics, tableName, statementExecutionContext);
    }

    public _Triggers[] getBeforeDeleteTrigger(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext) throws DException {
        return triggerInfoStore.getBeforeDeleteTrigger(triggerCharacteristics, tableName, statementExecutionContext);
    }

    public void refreshTriggers(QualifiedIdentifier tableName) throws DException {
        serverSystem.refreshTriggers(databaseURL, tableName);
    }

    public void refreshTriggerInfo(QualifiedIdentifier tableName) throws DException {
        triggerInfoStore.removeTriggerInfo(tableName);
        statementLevelTriggerInfoStore.removeTriggerInfo(tableName);
    }

    public SessionVersionHandler getSessionVersionHandler() throws DException {
        return ((MergeDatabase) serverSystem.getMergeDatabase(databaseURL)).getSessionVersionHandler();
    }

    public _Triggers[] getAfterInsertTriggerOfStatementLevel(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext) throws DException {
        return statementLevelTriggerInfoStore.getAfterInsertTrigger(triggerCharacteristics, tableName, statementExecutionContext);
    }

    public _Triggers[] getBeforeInsertTriggerOfStatementLevel(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext) throws DException {
        return statementLevelTriggerInfoStore.getBeforeInsertTrigger(triggerCharacteristics, tableName, statementExecutionContext);
    }

    public _Triggers[] getAfterUpdateTriggerOfStatementLevel(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext, int[] cols) throws DException {
        return statementLevelTriggerInfoStore.getAfterUpdateTrigger(triggerCharacteristics, tableName, statementExecutionContext, cols);
    }

    public _Triggers[] getBeforeUpdateTriggerOfStatementLevel(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext, int[] cols) throws DException {
        return statementLevelTriggerInfoStore.getBeforeUpdateTrigger(triggerCharacteristics, tableName, statementExecutionContext, cols);
    }

    public _Triggers[] getAfterDeleteTriggerOfStatementLevel(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext) throws DException {
        return statementLevelTriggerInfoStore.getAfterDeleteTrigger(triggerCharacteristics, tableName, statementExecutionContext);
    }

    public _Triggers[] getBeforeDeleteTriggerOfStatementLevel(QualifiedIdentifier tableName, _TriggerCharacteristics triggerCharacteristics, _StatementExecutionContext statementExecutionContext) throws DException {
        return statementLevelTriggerInfoStore.getBeforeDeleteTrigger(triggerCharacteristics, tableName, statementExecutionContext);
    }

    public ArrayList getCreateIndexList() throws DException {
        return createIndexesList;
    }

    public void resetTime() throws DException {
        userSession.getSession().resetTime();
    }

    public void refreshConstraint(QualifiedIdentifier tableName) throws DException {
        serverSystem.refreshConstraintTable(databaseURL, tableName);
        getDataDictionary().refereshCheckConstraints(tableName);
    }

    /**
    * New Method by harvinder related to bug 12052. */
    public int getTypeForPrivilige() throws DException {
        return serverSession;
    }

    public boolean isUserActiveMoreThanOnceOnSameDatabase(String databaseName, String userName) throws DException {
        return serverSystem.isUserActiveMoreThanOnceOnSameDatabase(databaseName, userName);
    }
}

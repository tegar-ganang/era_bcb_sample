package com.daffodilwoods.daffodildb.server.serversystem;

import java.util.*;
import com.daffodilwoods.daffodildb.server.datadictionarysystem.*;
import com.daffodilwoods.daffodildb.server.sessionsystem.*;
import com.daffodilwoods.daffodildb.server.sql99.*;
import com.daffodilwoods.daffodildb.server.sql99.common.*;
import com.daffodilwoods.daffodildb.server.sql99.dcl.sqlcontrolstatement.*;
import com.daffodilwoods.daffodildb.server.sql99.dcl.sqlsessionstatement.*;
import com.daffodilwoods.daffodildb.server.sql99.dcl.sqltransactionstatement.*;
import com.daffodilwoods.daffodildb.server.sql99.ddl.schemamanipulation.*;
import com.daffodilwoods.daffodildb.server.sql99.dml.*;
import com.daffodilwoods.daffodildb.server.sql99.dml.declarecursor.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.execution.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.listenerevents.*;
import com.daffodilwoods.daffodildb.server.sql99.dql.queryexpression.*;
import com.daffodilwoods.daffodildb.server.sql99.utils.*;
import com.daffodilwoods.daffodildb.utils.parser.*;
import com.daffodilwoods.database.general.*;
import com.daffodilwoods.database.resource.*;

public class SaveModeConnection extends Connection {

    private SaveModeHandler saveModeHandler;

    private Object saveModeSessionId;

    public SaveModeConnection(String databaseURL, _UserSession userSession, ServerSystem serverSystem) throws DException {
        super(databaseURL, userSession, serverSystem);
        saveModeSessionId = userSession.getSession().getSessionId();
        Session session = (Session) userSession.getSession();
        saveModeHandler = session.sessionDatabase.getSaveModeHandler(serverSystem.getDaffodilHome());
    }

    public Object execute(Object parsedQuery, int queryTimeOut, int type) throws DException {
        if (parsedQuery instanceof queryexpression) {
            if (!userSession.getAutoCommit()) {
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
            return executer.execute((Object[]) null);
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
                    ss.rollback();
                    throw ex;
                }
                if (!(parsedQuery instanceof dropdatabasestatement)) ss.commit();
            } finally {
                dd.releaseDDL();
            }
            saveModeHandler.write(saveModeSessionId, parsedQuery);
            if (returnedObject == null) return new Integer(Integer.MIN_VALUE);
            return returnedObject;
        }
        if (parsedQuery instanceof SQLtransactionstatement) {
            Object returnedObject = ((SQLtransactionstatement) parsedQuery).run(this);
            saveModeHandler.write(saveModeSessionId, parsedQuery);
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
            saveModeHandler.write(saveModeSessionId, parsedQuery);
            if (returnedObject == null) return new Integer(Integer.MIN_VALUE);
            return returnedObject;
        }
        if (parsedQuery instanceof SQLcontrolstatement) {
            _StatementExecutionContext sec = getStatementExecutionContext();
            _Executer executer = (_Executer) ((SQLcontrolstatement) parsedQuery).run(sec);
            Object returnedObject = executer.execute((_VariableValues) null);
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
                Object ob = executer.execute((_VariableValues) null);
                saveModeHandler.write(saveModeSessionId, parsedQuery);
                return ob;
            } else {
                if (queryType == _Connection.EXECUTE) return execute(parsedQuery, queryTimeOut, type); else return executeUpdate(parsedQuery, queryTimeOut);
            }
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
            saveModeHandler.write(saveModeSessionId, parsedObject);
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
                    ss.rollback();
                    throw ex;
                }
                ss.commit();
            } finally {
                dd.releaseDDL();
            }
            saveModeHandler.write(saveModeSessionId, parsedObject);
            if (returnedObject == null) return new Integer(Integer.MIN_VALUE);
            return returnedObject;
        }
        throw new DException("DSE533", null);
    }

    public void setAutoCommit(boolean autoCommit0) throws DException {
        if (userSession.getAutoCommit() != autoCommit0) {
            if (userSession.getAutoCommit() == false) commit();
            userSession.setAutoCommit(autoCommit0);
            saveModeHandler.write(saveModeSessionId, autoCommit0 ? SaveModeHandler.SETCOMMITON : SaveModeHandler.SETCOMMITOFF);
        }
    }

    public _PreparedStatement getPreparedStatement(Object parsedQuery, String query, int type) throws DException {
        if (parsedQuery instanceof queryexpression) {
            queryexpression qurexp = (queryexpression) parsedQuery;
            ServerSessionWrapper sersewr = new ServerSessionWrapper(this);
            if (type == IteratorConstants.UPDATABLE) if (!qurexp.isSimpleQuery(sersewr)) type = IteratorConstants.NONSCROLLABLE;
            sersewr.setType(type);
            _Executer executer = (_Executer) qurexp.run(sersewr);
            ParameterMetaData parameterMetaData = new ParameterMetaData(query, qurexp.getParameterInfo());
            Object[] parameters = qurexp.getParameters(this);
            PreparedStatement preparedStatment = new PreparedStatement(executer, parameters == null ? 0 : parameters.length, parameterMetaData, query, verboseUser, this);
            preparedStatment.setQueryType(PreparedStatement.queryexpression);
            return preparedStatment;
        }
        if (parsedQuery instanceof SQLdatastatement) {
            if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
            _StatementExecutionContext sec = getStatementExecutionContext();
            SQLdatastatement sqldatastatement = (SQLdatastatement) parsedQuery;
            Object[] parameters = sqldatastatement.getParameters(this);
            _Executer executer = (_Executer) sqldatastatement.run(sec);
            ParameterMetaData parameterMetaData = new ParameterMetaData(query, ((SQLdatachangestatement) parsedQuery).getParameterInfo());
            SaveModePreparedStatement preparedStatment = new SaveModePreparedStatement(executer, parameters == null ? 0 : parameters.length, parameterMetaData, query, verboseUser, saveModeHandler, saveModeSessionId, this);
            preparedStatment.setQueryType(PreparedStatement.SQLdatastatement);
            return preparedStatment;
        }
        if (parsedQuery instanceof SQLschemastatement) {
            if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
            SaveModeNonPreparedStatement nps = new SaveModeNonPreparedStatement(this, (StatementExecuter) parsedQuery, verboseUser, saveModeHandler, saveModeSessionId);
            nps.setQueryType(PreparedStatement.SQLschemastatement);
            return nps;
        }
        if (parsedQuery instanceof SQLtransactionstatement) {
            SaveModeNonPreparedStatement nps = new SaveModeNonPreparedStatement(this, (StatementExecuter) parsedQuery, verboseUser, saveModeHandler, saveModeSessionId);
            nps.setQueryType(PreparedStatement.SQLtransactionstatement);
            return nps;
        }
        if (parsedQuery instanceof SQLsessionstatement) {
            NonPreparedStatement nps = new NonPreparedStatement(this, (StatementExecuter) parsedQuery, verboseUser);
            nps.setQueryType(PreparedStatement.SQLsessionstatement);
            return nps;
        }
        if (parsedQuery instanceof SQLcontrolstatement) {
            _StatementExecutionContext sec = getStatementExecutionContext();
            SQLcontrolstatement sqlcontrolstatement = (SQLcontrolstatement) parsedQuery;
            Object[] parameters = sqlcontrolstatement.getParameters(this);
            _Executer executer = (_Executer) sqlcontrolstatement.run(sec);
            ParameterMetaData pmd = new ParameterMetaData(query, sqlcontrolstatement.getParameterInfo());
            PreparedStatement preparedStatment = new PreparedStatement(executer, parameters == null ? 0 : parameters.length, pmd, query, verboseUser, this);
            preparedStatment.setQueryType(PreparedStatement.callstatement);
            return preparedStatment;
        }
        throw new DException("DSE565", new Object[] { "Query" });
    }

    public void commit() throws DException {
        if (connectionStatus) throw new DatabaseException("DSE279", null);
        if (((String) getUserSession().getTransactionAccessMode()).equalsIgnoreCase("Read Only")) throw new DException("DSE1184", (Object[]) null);
        if (commit) {
            _StatementExecutionContext statementExecutionContext = getStatementExecutionContext();
            _TriggerExecutionContext triggerExecutionContext = new TriggerExecutionContext();
            statementExecutionContext.setTriggerExecutionContext(triggerExecutionContext);
            userSession.commit(statementExecutionContext);
            userSession.getSavePointVector().clear();
            saveModeHandler.write(saveModeSessionId, SaveModeHandler.COMMIT);
        }
    }

    public void rollback() throws DException {
        if (connectionStatus) throw new DatabaseException("DSE279", null);
        if (getUserSession().getTransactionAccessMode().equals("Read Only")) throw new DException("DSE1184", (Object[]) null);
        if (commit) {
            _StatementExecutionContext statementExecutionContext = getStatementExecutionContext();
            TriggerExecutionContext triggerExecutionContext = new TriggerExecutionContext();
            statementExecutionContext.setTriggerExecutionContext(triggerExecutionContext);
            userSession.rollback(statementExecutionContext);
            userSession.getSavePointVector().clear();
            saveModeHandler.write(saveModeSessionId, SaveModeHandler.ROLLBACK);
        }
    }

    public String setSavePoint() throws DException {
        String savepoint = "sp" + userSession.getSavePointVector().size();
        setSavePoint(savepoint);
        saveModeHandler.write(saveModeSessionId, SaveModeHandler.SETSAVEPOINT, savepoint);
        return savepoint;
    }

    public void setSavePoint(String savepoint) throws DException {
        ArrayList savePointVector = userSession.getSavePointVector();
        if (!savePointVector.contains(savepoint)) {
            userSession.addSavePoint(savepoint);
            userSession.startSavePoint();
            saveModeHandler.write(saveModeSessionId, SaveModeHandler.SETSAVEPOINT, savepoint);
        } else throw new DException("DSE889", new Object[] { savepoint });
    }

    public void releaseSavePoint(String savepoint) throws DException {
        ArrayList savePointVector = userSession.getSavePointVector();
        int index = savePointVector.indexOf(savepoint);
        if (index == -1) throw new DException("DSE890", null);
        userSession = commitUptoLevel(userSession, getStatementExecutionContext(), savePointVector.size() - index);
        saveModeHandler.write(saveModeSessionId, SaveModeHandler.RELEASESAVEPOINT, savepoint);
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
        saveModeHandler.write(saveModeSessionId, SaveModeHandler.ROLLBACKSAVEPOINT, savepoint);
    }

    public void commitSavePoint() throws DException {
        userSession.commitSavePoint(getStatementExecutionContext());
        saveModeHandler.write(saveModeSessionId, SaveModeHandler.COMMITSAVEPOINT);
    }

    public void close() throws DException {
        try {
            userSession.rollback(getStatementExecutionContext());
        } catch (DException e) {
        }
        connectionStatus = true;
        serverSystem.close(databaseURL, this);
        saveModeHandler.write(saveModeSessionId, SaveModeHandler.CLOSE);
    }

    public Object[] executeBatch(String query) throws DException {
        Object[] parsedQuery = null;
        try {
            parsedQuery = (Object[]) Parser.parseBatch(query);
        } catch (DException ex) {
            PrintHandler.print(query, verboseUser, ex);
            throw ex;
        }
        Object[] result = new Object[parsedQuery.length];
        for (int i = 0; i < result.length; i++) {
            PrintHandler.print(" " + parsedQuery[i].toString());
            if (parsedQuery[i] instanceof insertstatement) {
                if (super.userSession.getTransactionAccessMode().equals("Read Only")) throw new DException("DSE1184", (Object[]) null);
                insertstatement in = (insertstatement) parsedQuery[i];
                _StatementExecutionContext sec = getStatementExecutionContext();
                in.run(sec);
                ParameterInfo[] parameterInfo = in.getParameterInfo();
                if (parameterInfo != null && parameterInfo.length > 0) {
                    ParameterMetaData parameterMetaData = new ParameterMetaData(query, parameterInfo);
                    QueryInfo qi = new QueryInfo(parsedQuery[i].toString(), QueryInfo.INSERT, parameterMetaData);
                    result[i] = qi;
                } else {
                    QueryInfo qi = new QueryInfo(parsedQuery[i].toString(), QueryInfo.INSERT, null);
                    result[i] = qi;
                }
            } else if (parsedQuery[i] instanceof updatestatementsearched) {
                if (super.userSession.getTransactionAccessMode().equals("Read Only")) throw new DException("DSE1184", (Object[]) null);
                updatestatementsearched up = (updatestatementsearched) parsedQuery[i];
                _StatementExecutionContext sec = getStatementExecutionContext();
                up.run(sec);
                ParameterInfo[] parameterInfo = up.getParameterInfo();
                if (parameterInfo != null && parameterInfo.length > 0) {
                    ParameterMetaData parameterMetaData = new ParameterMetaData(query, parameterInfo);
                    QueryInfo qu = new QueryInfo(parsedQuery[i].toString(), QueryInfo.UPDATE, parameterMetaData);
                    result[i] = qu;
                } else {
                    QueryInfo qu = new QueryInfo(parsedQuery[i].toString(), QueryInfo.UPDATE, null);
                    result[i] = qu;
                }
            } else if (parsedQuery[i] instanceof deletestatementsearched) {
                if (super.userSession.getTransactionAccessMode().equals("Read Only")) throw new DException("DSE1184", (Object[]) null);
                deletestatementsearched ds = (deletestatementsearched) parsedQuery[i];
                _StatementExecutionContext sec = getStatementExecutionContext();
                ds.run(sec);
                ParameterInfo[] parameterInfo = ds.getParameterInfo();
                if (parameterInfo != null && parameterInfo.length > 0) {
                    ParameterMetaData parameterMetaData = new ParameterMetaData(query, parameterInfo);
                    QueryInfo qd = new QueryInfo(parsedQuery[i].toString(), QueryInfo.DELETE, parameterMetaData);
                    result[i] = qd;
                } else {
                    QueryInfo qd = new QueryInfo(parsedQuery[i].toString(), QueryInfo.DELETE, null);
                    result[i] = qd;
                }
            } else if (parsedQuery[i] instanceof queryexpression) {
                queryexpression qs = (queryexpression) parsedQuery[i];
                qs.checkSemantic(this, true);
                ParameterInfo[] parameterInfo = qs.getParameterInfo();
                if (parameterInfo != null && parameterInfo.length > 0) {
                    ParameterMetaData parameterMetaData = new ParameterMetaData(query, parameterInfo);
                    QueryInfo qsd = new QueryInfo(parsedQuery[i].toString(), QueryInfo.SELECT, parameterMetaData);
                    result[i] = qsd;
                } else {
                    QueryInfo qsd = new QueryInfo(parsedQuery[i].toString(), QueryInfo.SELECT, null);
                    result[i] = qsd;
                }
            } else if (parsedQuery[i] instanceof SQLcontrolstatement) {
                SQLcontrolstatement cs = (SQLcontrolstatement) parsedQuery[i];
                _StatementExecutionContext sec = getStatementExecutionContext();
                cs.run(sec);
                ParameterInfo[] parameterInfo = cs.getParameterInfo();
                if (parameterInfo != null && parameterInfo.length > 0) {
                    ParameterMetaData parameterMetaData = new ParameterMetaData(query, parameterInfo);
                    QueryInfo qsd = new QueryInfo(parsedQuery[i].toString(), QueryInfo.CALLSTATEMENT, parameterMetaData);
                    result[i] = qsd;
                } else {
                    QueryInfo qsd = new QueryInfo(parsedQuery[i].toString(), QueryInfo.CALLSTATEMENT, null);
                    result[i] = qsd;
                }
            } else {
                if (parsedQuery[i] instanceof SQLsessionstatement) {
                    _StatementExecutionContext sec = getStatementExecutionContext();
                    ((SQLsessionstatement) parsedQuery[i]).run(sec);
                } else if (parsedQuery[i] instanceof SQLschemastatement) {
                    commit();
                    _ServerSession ss = getSystemServerSession();
                    _DataDictionary dd = getDataDictionary();
                    dd.lockDDL();
                    try {
                        try {
                            ((StatementExecuter) parsedQuery[i]).run(this);
                        } catch (DException ex) {
                            dd.restoreGeneratedKeys();
                            ss.rollback();
                            throw ex;
                        }
                        if (!(parsedQuery[i] instanceof dropdatabasestatement)) ss.commit();
                    } finally {
                        dd.releaseDDL();
                    }
                    saveModeHandler.write(saveModeSessionId, parsedQuery[i]);
                } else if (parsedQuery[i] instanceof SQLdatastatement) {
                    throw new DException("DSE8090", null);
                } else if (parsedQuery[i] instanceof declarecursor) {
                    declarecursor ds = (declarecursor) parsedQuery[i];
                    _StatementExecutionContext sec = getStatementExecutionContext();
                    ds.run(sec);
                } else if (parsedQuery[i] instanceof SQLtransactionstatement) {
                    SQLtransactionstatement sts = (SQLtransactionstatement) parsedQuery[i];
                    sts.run(this);
                    saveModeHandler.write(saveModeSessionId, parsedQuery[i]);
                } else ((StatementExecuter) parsedQuery[i]).run(this);
                result[i] = new QueryInfo(parsedQuery[i].toString(), QueryInfo.OTHERS, null);
            }
        }
        return result;
    }

    public SaveModeHandler getSaveModeHandler() {
        return saveModeHandler;
    }
}

package org.isi.monet.core.agents;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import org.isi.monet.core.configuration.Configuration;
import org.isi.monet.core.constants.ApplicationInterface;
import org.isi.monet.core.constants.Database;
import org.isi.monet.core.constants.ErrorCode;
import org.isi.monet.core.constants.SiteFiles;
import org.isi.monet.core.constants.Strings;
import org.isi.monet.core.exceptions.DatabaseException;
import org.isi.monet.core.exceptions.FilesystemException;
import org.isi.monet.core.exceptions.SystemException;
import org.isi.monet.core.library.LibraryString;
import org.isi.monet.core.model.Context;
import org.isi.monet.core.model.DatabaseQuery;
import org.isi.monet.core.pool.PoolDBConnection;
import org.isi.monet.core.utils.BufferedQuery;

public abstract class AgentDatabase {

    String sError;

    Properties oProperties;

    String sQueryFieldPrefix;

    String sQueryFieldBindPrefix;

    String sQueryFieldSuffix;

    String sQueryFieldBindSuffix;

    String idRoot;

    Context oContext;

    Configuration oConfiguration;

    PoolDBConnection oPoolDBConnection;

    String sDirectory;

    String sConnectionChain;

    String sDatabase;

    String sUsername;

    String sPassword;

    protected static AgentDatabase oInstance;

    protected static String sType;

    private static final String QUERY_ESCAPED_SEMICOLON = "::SEMICOLON::";

    protected AgentDatabase() {
        this.sError = Strings.EMPTY;
        this.oProperties = new Properties();
        this.oContext = Context.getInstance();
        this.oConfiguration = Configuration.getInstance();
        this.oPoolDBConnection = PoolDBConnection.getInstance();
        this.sDirectory = "";
        this.sConnectionChain = "";
        this.sDatabase = "";
        this.sUsername = "";
        this.sPassword = "";
    }

    public abstract class ColumnTypes {

        public static final String BOOLEAN = "boolean";

        public static final String DATE = "date";

        public static final String STRING = "string";

        public static final String MEMO = "memo";

        public static final String INTEGER = "integer";
    }

    public abstract String getDateAsText(Date dtDate);

    public abstract Date getDate(ResultSet oResult, String idColumn);

    public abstract String getNullValue();

    public abstract Boolean isValid(Connection oConnection);

    public abstract String getColumnDefinition(String Type);

    private String getIdConnection(String idSession) {
        Long idThread = Thread.currentThread().getId();
        String sService = this.oContext.getApplication(idThread);
        String sInterface = this.oContext.getApplicationInterface(idThread);
        if (idSession == null) {
            idSession = Strings.EMPTY;
            if (sInterface.equals(ApplicationInterface.APPLICATION)) {
                idSession = this.oContext.getIdSession(idThread);
            }
        }
        return sService + Strings.UNDERLINED + sInterface + Strings.UNDERLINED + idSession;
    }

    private String getIdConnection() {
        return this.getIdConnection(null);
    }

    protected String getParameterValue(String sName, Object oData) {
        String sResult;
        if (oData == null) return null;
        if ((oData.getClass() == java.util.Date.class) || (oData.getClass() == java.sql.Date.class)) {
            sResult = this.getDateAsText((Date) oData);
        } else {
            try {
                sResult = String.valueOf(oData);
            } catch (ClassCastException oException) {
                throw new DatabaseException(ErrorCode.GET_QUERY_PARAMETER_VALUE, sName, oException);
            }
        }
        return sResult;
    }

    public static synchronized AgentDatabase getInstance() {
        if (sType == null) throw new DatabaseException(ErrorCode.DATABASE_CONNECTION, null);
        if (oInstance == null) {
            if (sType.equalsIgnoreCase(Database.Types.MYSQL)) oInstance = new AgentDatabaseMysql(); else if (sType.equalsIgnoreCase(Database.Types.ORACLE)) oInstance = new AgentDatabaseOracle();
            String sConnector = oInstance.getConnector();
            try {
                Class.forName(sConnector);
            } catch (ClassNotFoundException oException) {
                throw new DatabaseException(ErrorCode.DATABASE_CONNECTION, sConnector, oException);
            }
        }
        return oInstance;
    }

    public Connection getConnection() {
        Long idThread = Thread.currentThread().getId();
        String idSession = this.oContext.getIdSession(idThread);
        String sConnectionType = this.oContext.getDatabaseConnectionType(idThread);
        Connection oConnection;
        oConnection = this.oPoolDBConnection.get(this.getIdConnection());
        if (oConnection != null) return oConnection;
        oConnection = this.createConnection();
        try {
            if (sConnectionType.equals(Database.ConnectionTypes.AUTO_COMMIT)) oConnection.setAutoCommit(true); else oConnection.setAutoCommit(false);
        } catch (SQLException oException) {
            throw new DatabaseException(ErrorCode.DATABASE_CONNECTION, idSession, oException);
        }
        return oConnection;
    }

    public static synchronized Boolean setType(String sNewType) {
        sType = sNewType;
        return true;
    }

    public Boolean initialize(String sDirectory, String sConnectionChain, String sDatabase, String sUsername, String sPassword) {
        String sQueriesFilename = sDirectory + Strings.BAR45 + sType + SiteFiles.Suffix.QUERIES;
        InputStream oQueriesFile;
        this.sDirectory = sDirectory;
        this.sConnectionChain = sConnectionChain;
        this.sDatabase = sDatabase;
        this.sUsername = sUsername;
        this.sPassword = sPassword;
        this.oProperties = new Properties();
        try {
            oQueriesFile = AgentFilesystem.getInputStream(sQueriesFilename);
            this.oProperties.load(oQueriesFile);
            oQueriesFile.close();
        } catch (IOException oException) {
            throw new FilesystemException(ErrorCode.FILESYSTEM_READ_FILE, sQueriesFilename, oException);
        }
        this.sQueryFieldPrefix = this.oProperties.getProperty(Database.QueryFields.PREFIX);
        this.sQueryFieldBindPrefix = this.oProperties.getProperty(Database.QueryFields.PREFIX_BIND);
        this.sQueryFieldSuffix = this.oProperties.getProperty(Database.QueryFields.SUFFIX);
        this.sQueryFieldBindSuffix = this.oProperties.getProperty(Database.QueryFields.SUFFIX_BIND);
        this.idRoot = this.oProperties.getProperty(Database.QueryFields.DATA_ID_ROOT);
        return true;
    }

    public Boolean reloadConnection() {
        Connection oConnection = this.getConnection();
        try {
            if (this.isValid(oConnection)) return false;
            this.createConnection();
            return true;
        } catch (Exception oException) {
            throw new SystemException(ErrorCode.DATABASE_CONNECTION, sConnectionChain, oException);
        }
    }

    public Connection createConnection() {
        Connection oConnection;
        try {
            oConnection = DriverManager.getConnection(this.sConnectionChain, this.sUsername, this.sPassword);
            this.selectDatabase(this.sDatabase);
        } catch (SQLException oException) {
            String sConnectionChain = this.sConnectionChain + Strings.SPACE + sUsername + Strings.SPACE + sPassword;
            throw new SystemException(ErrorCode.DATABASE_CONNECTION, sConnectionChain, oException);
        }
        this.oPoolDBConnection.register(this.getIdConnection(), oConnection);
        return oConnection;
    }

    public Boolean removeConnection(String idSession) {
        String idConnection = this.getIdConnection(idSession);
        Connection oConnection;
        try {
            oConnection = this.oPoolDBConnection.get(idConnection);
            if (oConnection != null) oConnection.close();
        } catch (SQLException oException) {
            throw new SystemException(ErrorCode.DATABASE_DISCONNECTION, idConnection, oException);
        }
        this.oPoolDBConnection.unregister(idConnection);
        return true;
    }

    private ResultSet doSelectQuery(String sName, LinkedHashMap<String, Object> hmParameters, PreparedStatement oStatement) {
        ResultSet oResult;
        Connection oConnection = this.getConnection();
        oResult = null;
        if (oConnection == null) {
            return null;
        }
        try {
            oResult = oStatement.executeQuery();
            oResult.first();
        } catch (SQLException oException) {
            if (this.reloadConnection()) return this.doSelectQuery(sName, hmParameters, oStatement); else {
                throw new DatabaseException(ErrorCode.DATABASE_SELECT_QUERY, this.getQuery(sName, hmParameters), oException);
            }
        }
        return oResult;
    }

    private Boolean doUpdateQuery(String sName, LinkedHashMap<String, Object> hmParameters, PreparedStatement oStatement) {
        Connection oConnection = this.getConnection();
        int iResult;
        if (oConnection == null) {
            return null;
        }
        try {
            iResult = oStatement.executeUpdate();
        } catch (Exception oException) {
            if (this.reloadConnection()) return this.doUpdateQuery(sName, hmParameters, oStatement); else throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, this.getQuery(sName, hmParameters), oException);
        } finally {
            try {
                oStatement.close();
            } catch (Exception oInternalException) {
                throw new SystemException(ErrorCode.CLOSE_QUERY, null, oInternalException);
            }
        }
        return (iResult > 0);
    }

    public String getRootId() {
        return this.idRoot;
    }

    public String getConnector() {
        if (sType == null) return Strings.EMPTY;
        if (sType.equalsIgnoreCase(Database.Types.MYSQL)) return "com.mysql.jdbc.Driver"; else if (sType.equalsIgnoreCase(Database.Types.ORACLE)) return "oracle.jdbc.driver.OracleDriver";
        return Strings.EMPTY;
    }

    public Boolean createDatabase(String sDatabase) {
        String sQuery = "CREATE DATABASE " + sDatabase;
        PreparedStatement oStatement = this.getPreparedStatement(sQuery);
        return (this.doUpdateQuery(sQuery, new LinkedHashMap<String, Object>(), oStatement) != null);
    }

    public Boolean removeDatabase(String sDatabase) {
        String sQuery = "DROP DATABASE IF EXISTS " + sDatabase;
        PreparedStatement oStatement = this.getPreparedStatement(sQuery);
        return (this.doUpdateQuery(sQuery, new LinkedHashMap<String, Object>(), oStatement) != null);
    }

    public Boolean selectDatabase(String sDatabase) {
        return true;
    }

    public PreparedStatement[] getPreparedStatements(DatabaseQuery[] aQueries) {
        PreparedStatement[] aResult = new PreparedStatement[aQueries.length];
        Integer iPos = 0;
        for (iPos = 0; iPos < aQueries.length; iPos++) {
            aResult[iPos] = this.getPreparedStatement(aQueries[iPos].getName(), aQueries[iPos].getParameters());
        }
        return aResult;
    }

    public PreparedStatement getPreparedStatement(String sName, LinkedHashMap<String, Object> hmParameters) {
        String sQuery;
        Iterator<String> oIterator = hmParameters.keySet().iterator();
        Iterator<String> oListIterator;
        Connection oConnection = this.getConnection();
        ArrayList<String> alObjects = new ArrayList<String>();
        Integer iPos = 0;
        PreparedStatement oPreparedStatement = null;
        if (!this.oProperties.containsKey(sName)) {
            throw new DatabaseException(ErrorCode.UNKOWN_DATABASE_QUERY, sName);
        }
        sQuery = (String) this.oProperties.get(sName);
        while (oIterator.hasNext()) {
            String sParameterName = oIterator.next();
            String sParameterValue = this.getParameterValue(sParameterName, hmParameters.get(sParameterName));
            if (sParameterValue == null) sParameterValue = this.getNullValue();
            sQuery = LibraryString.replaceAll(sQuery, this.sQueryFieldPrefix + sParameterName + this.sQueryFieldSuffix, sParameterValue);
            if (sQuery.indexOf(this.sQueryFieldBindPrefix + sParameterName + this.sQueryFieldBindSuffix) != -1) {
                alObjects.add(sParameterValue);
                sQuery = sQuery.replace(this.sQueryFieldBindPrefix + sParameterName + this.sQueryFieldBindSuffix, Strings.QUESTION);
            }
        }
        try {
            oPreparedStatement = oConnection.prepareStatement(sQuery, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            oListIterator = alObjects.iterator();
            while (oListIterator.hasNext()) {
                iPos++;
                oPreparedStatement.setObject(iPos, oListIterator.next());
            }
        } catch (SQLException oException) {
            if (this.reloadConnection()) return this.getPreparedStatement(sName, hmParameters); else throw new DatabaseException(ErrorCode.QUERY_FAILED, sQuery, oException);
        }
        return oPreparedStatement;
    }

    public PreparedStatement getPreparedStatement(String sName) {
        return this.getPreparedStatement(sName, new LinkedHashMap<String, Object>());
    }

    public String getQuery(String sName, LinkedHashMap<String, Object> hmParameters) {
        String sQuery;
        Iterator<String> oIterator = hmParameters.keySet().iterator();
        if (!this.oProperties.containsKey(sName)) {
            throw new DatabaseException(ErrorCode.UNKOWN_DATABASE_QUERY, sName);
        }
        sQuery = (String) this.oProperties.get(sName);
        if (sQuery.indexOf(this.sQueryFieldBindPrefix) != -1) {
            throw new DatabaseException(ErrorCode.UNKOWN_DATABASE_QUERY, sName);
        }
        while (oIterator.hasNext()) {
            String sParameterName = oIterator.next();
            String sParameterValue = this.getParameterValue(sParameterName, hmParameters.get(sParameterName));
            sQuery = LibraryString.replaceAll(sQuery, this.sQueryFieldPrefix + sParameterName + this.sQueryFieldSuffix, (sParameterValue != null) ? sParameterValue : Strings.EMPTY);
        }
        return sQuery;
    }

    public String getTableName(String sName) {
        if (!this.oProperties.containsKey(sName)) {
            throw new DatabaseException(ErrorCode.UNKOWN_DATABASE_TABLE, sName);
        }
        return (String) this.oProperties.get(sName);
    }

    public String getQuery(String sName) {
        return this.getQuery(sName, new LinkedHashMap<String, Object>());
    }

    public String[] getQueries(DatabaseQuery[] aDatabaseQueries) {
        Integer iPos;
        String[] aQueries = new String[aDatabaseQueries.length];
        for (iPos = 0; iPos < aDatabaseQueries.length; iPos++) {
            aQueries[iPos] = this.getQuery(aDatabaseQueries[iPos].getName(), aDatabaseQueries[iPos].getParameters());
        }
        return aQueries;
    }

    public Boolean executeUpdateTransaction(DatabaseQuery[] aDatabaseQueries) {
        Integer iPos = 0;
        PreparedStatement[] aQueries = this.getPreparedStatements(aDatabaseQueries);
        Connection oConnection = this.getConnection();
        Boolean bAutoCommit = true;
        if (aQueries.length == 0) {
            return true;
        }
        if (oConnection == null) {
            return false;
        }
        try {
            bAutoCommit = oConnection.getAutoCommit();
            oConnection.setAutoCommit(false);
            for (iPos = 0; iPos < aQueries.length; iPos++) {
                aQueries[iPos].executeUpdate();
                aQueries[iPos].close();
            }
            if (bAutoCommit) {
                oConnection.commit();
                oConnection.setAutoCommit(true);
            }
        } catch (SQLException oException) {
            if (this.reloadConnection()) return this.executeUpdateTransaction(aDatabaseQueries); else {
                try {
                    oConnection.rollback();
                } catch (SQLException oRollbackException) {
                    throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, Strings.ROLLBACK, oRollbackException);
                }
                throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, this.getQueries(aDatabaseQueries).toString(), oException);
            }
        } finally {
            try {
                oConnection.setAutoCommit(bAutoCommit);
            } catch (SQLException oException) {
                throw new SystemException(ErrorCode.CLOSE_QUERY, null, oException);
            }
        }
        return true;
    }

    public Boolean executeUpdateTransaction(BufferedQuery oBufferedQuery) {
        Connection oConnection = this.getConnection();
        Boolean bAutoCommit = true;
        String sQuery;
        Statement oStatement = null;
        if (oConnection == null) {
            return false;
        }
        try {
            bAutoCommit = oConnection.getAutoCommit();
            oConnection.setAutoCommit(false);
            oStatement = oConnection.createStatement();
            while ((sQuery = oBufferedQuery.readQuery()) != null) {
                oStatement.execute(sQuery);
            }
            if (bAutoCommit) {
                oConnection.commit();
                oConnection.setAutoCommit(true);
            }
        } catch (SQLException oException) {
            if (this.reloadConnection()) return this.executeUpdateTransaction(oBufferedQuery); else {
                try {
                    oConnection.rollback();
                } catch (SQLException oRollbackException) {
                    throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, Strings.ROLLBACK, oRollbackException);
                }
                throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, "bufferedquery", oException);
            }
        } finally {
            try {
                if (oStatement != null) oStatement.close();
                oConnection.setAutoCommit(bAutoCommit);
            } catch (SQLException oException) {
                throw new SystemException(ErrorCode.CLOSE_QUERY, null, oException);
            }
        }
        return true;
    }

    public Boolean executeQueries(DatabaseQuery[] aDatabaseQueries) {
        Integer iPos = 0;
        String[] aQueries = this.getQueries(aDatabaseQueries);
        Connection oConnection = this.getConnection();
        Statement oStatement = null;
        Boolean bAutoCommit = true;
        if (aQueries.length == 0) {
            return true;
        }
        if (oConnection == null) {
            return false;
        }
        try {
            bAutoCommit = oConnection.getAutoCommit();
            oConnection.setAutoCommit(false);
            oStatement = oConnection.createStatement();
            for (iPos = 0; iPos < aQueries.length; iPos++) {
                if (aQueries[iPos].equals(Strings.EMPTY)) continue;
                oStatement.execute(aQueries[iPos]);
            }
            if (bAutoCommit) {
                oConnection.commit();
                oConnection.setAutoCommit(true);
            }
        } catch (SQLException oException) {
            if (this.reloadConnection()) return this.executeQueries(aDatabaseQueries); else {
                try {
                    oConnection.rollback();
                } catch (SQLException oRollbackException) {
                    throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, Strings.ROLLBACK, oRollbackException);
                }
                throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, aQueries.toString(), oException);
            }
        } finally {
            try {
                oStatement.close();
                oConnection.setAutoCommit(bAutoCommit);
            } catch (SQLException oException) {
                throw new SystemException(ErrorCode.CLOSE_QUERY, null, oException);
            }
        }
        return true;
    }

    public Boolean executeBatchQueries(String sBatchQueries) {
        Connection oConnection = this.getConnection();
        Statement oStatement = null;
        Boolean bAutoCommit = true;
        String[] aBatchQueries = sBatchQueries.split(Strings.SEMICOLON);
        Integer iPos;
        if (aBatchQueries.length == 0) {
            return true;
        }
        if (oConnection == null) {
            return false;
        }
        try {
            bAutoCommit = oConnection.getAutoCommit();
            oConnection.setAutoCommit(false);
            oStatement = oConnection.createStatement();
            for (iPos = 0; iPos < aBatchQueries.length; iPos++) {
                aBatchQueries[iPos] = aBatchQueries[iPos].trim();
                if (aBatchQueries[iPos].equals(Strings.EMPTY)) continue;
                aBatchQueries[iPos] = aBatchQueries[iPos].replace(AgentDatabase.QUERY_ESCAPED_SEMICOLON, Strings.SEMICOLON);
                oStatement.addBatch(aBatchQueries[iPos].trim());
            }
            oStatement.executeBatch();
            if (bAutoCommit) {
                oConnection.commit();
                oConnection.setAutoCommit(true);
            }
        } catch (SQLException oException) {
            if (this.reloadConnection()) return this.executeBatchQueries(sBatchQueries);
            {
                try {
                    oConnection.rollback();
                } catch (SQLException oRollbackException) {
                    throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, Strings.ROLLBACK, oRollbackException);
                }
                throw new DatabaseException(ErrorCode.DATABASE_UPDATE_QUERY, sBatchQueries, oException);
            }
        } finally {
            try {
                oStatement.close();
                oConnection.setAutoCommit(bAutoCommit);
            } catch (SQLException oException) {
                throw new SystemException(ErrorCode.CLOSE_QUERY, null, oException);
            }
        }
        return true;
    }

    public ResultSet executeSelectQuery(String sName, LinkedHashMap<String, Object> hmParameters) {
        PreparedStatement oStatement = this.getPreparedStatement(sName, hmParameters);
        return this.doSelectQuery(sName, hmParameters, oStatement);
    }

    public ResultSet executeSelectQuery(String sName) {
        return this.executeSelectQuery(sName, new LinkedHashMap<String, Object>());
    }

    public Boolean executeUpdateQuery(String sName, LinkedHashMap<String, Object> hmParameters) {
        PreparedStatement oStatement = this.getPreparedStatement(sName, hmParameters);
        return this.doUpdateQuery(sName, hmParameters, oStatement);
    }

    public Boolean executeUpdateQuery(String sName) {
        return this.executeUpdateQuery(sName, new LinkedHashMap<String, Object>());
    }

    public Boolean doCommit() {
        Long idThread = Thread.currentThread().getId();
        Connection oConnection = this.getConnection();
        if (oConnection == null) {
            return null;
        }
        try {
            oConnection.commit();
        } catch (SQLException oException) {
            if (this.reloadConnection()) return this.doCommit(); else throw new DatabaseException(ErrorCode.DATABASE_COMMIT, this.oContext.getApplication(idThread), oException);
        }
        return true;
    }

    public Boolean closeQuery(ResultSet oResult) {
        if (oResult == null) return false;
        try {
            if (oResult.getStatement() != null) oResult.getStatement().close();
            oResult.close();
        } catch (Exception oException) {
            throw new SystemException(ErrorCode.CLOSE_QUERY, null, oException);
        }
        return true;
    }
}

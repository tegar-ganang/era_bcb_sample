package org.jdu.dao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.commons.configuration.DatabaseConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.jdu.dao.config.DbParam;
import org.jdu.dao.config.Query;
import org.jdu.dao.wrapper.LOBWrapper;
import org.jdu.dao.wrapper.LOBWrapperFactory;
import org.jdu.exception.ServiceException;
import org.jlu.logger.JluLogger;

/**
 * 
 * <p>
 * Classe service che esegue le operazioni sul database.
 * 
 * </p>
 * 
 * @author epelli
 * @param <T>
 * @param <E>
 * 
 */
public class DAOService<T> {

    private Connection tCon;

    private ResourceBundle bundle = ResourceBundle.getBundle("crparch");

    private Logger log = JluLogger.getLogger();

    private static Hashtable htMethodsToCall = new Hashtable();

    static {
    }

    public DAOService() {
        try {
            initStatementCalls();
        } catch (SecurityException e) {
            log.error("[DAOService::DAOService] " + e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            log.error("[DAOService::DAOService] " + e.getMessage(), e);
        }
    }

    private void initStatementCalls() throws NoSuchMethodException {
        Method setString = PreparedStatement.class.getMethod("setString", new Class[] { Integer.TYPE, String.class });
        Method setBinary = PreparedStatement.class.getMethod("setBinaryStream", new Class[] { Integer.TYPE, InputStream.class, Integer.TYPE });
        Method setBlob = PreparedStatement.class.getMethod("setBlob", new Class[] { Integer.TYPE, Blob.class });
        Method setBoolean = PreparedStatement.class.getMethod("setBoolean", new Class[] { Integer.TYPE, Boolean.TYPE });
        Method setByte = PreparedStatement.class.getMethod("setByte", new Class[] { Integer.TYPE, Byte.TYPE });
        Method setBytes = PreparedStatement.class.getMethod("setBytes", new Class[] { Integer.TYPE, byte[].class });
        Method setDate = PreparedStatement.class.getMethod("setDate", new Class[] { Integer.TYPE, Date.class });
        Method setDouble = PreparedStatement.class.getMethod("setDouble", new Class[] { Integer.TYPE, Double.TYPE });
        Method setFloat = PreparedStatement.class.getMethod("setFloat", new Class[] { Integer.TYPE, Float.TYPE });
        Method setInt = PreparedStatement.class.getMethod("setInt", new Class[] { Integer.TYPE, Integer.TYPE });
        Method setLong = PreparedStatement.class.getMethod("setLong", new Class[] { Integer.TYPE, Long.TYPE });
        Method setObject = PreparedStatement.class.getMethod("setObject", new Class[] { Integer.TYPE, Object.class });
        Method setClob = PreparedStatement.class.getMethod("setClob", new Class[] { Integer.TYPE, Clob.class });
        htMethodsToCall.put(new Integer(DbParam.STRING), setString);
        htMethodsToCall.put(new Integer(DbParam.BINARY_STREAM), setBinary);
        htMethodsToCall.put(new Integer(DbParam.BLOB), setBlob);
        htMethodsToCall.put(new Integer(DbParam.BOOLEAN), setBoolean);
        htMethodsToCall.put(new Integer(DbParam.BYTE), setByte);
        htMethodsToCall.put(new Integer(DbParam.BYTES), setBytes);
        htMethodsToCall.put(new Integer(DbParam.DATE), setDate);
        htMethodsToCall.put(new Integer(DbParam.DOUBLE), setDouble);
        htMethodsToCall.put(new Integer(DbParam.FLOAT), setFloat);
        htMethodsToCall.put(new Integer(DbParam.INT), setInt);
        htMethodsToCall.put(new Integer(DbParam.LONG), setLong);
        htMethodsToCall.put(new Integer(DbParam.OBJECT), setObject);
        htMethodsToCall.put(new Integer(DbParam.CLOB), setClob);
    }

    /**
	 * <p>
	 * Dato un oggetto di tipo sequence esegue la query e restituisce il
	 * risultato
	 * </p>
	 * 
	 * @param dbName
	 *            nome logico del db a cui connettersi (presente nel file di
	 *            configurazione crparch.properties
	 * @param sequence
	 *            oggetto con le informazioni sulla query
	 * @return
	 * @throws ServiceException
	 */
    public String getSequenceNextValue(String dbName, Sequence sequence) throws ServiceException {
        String sql = sequence.getQuery();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String result = "";
        try {
            con = getDbConnection().getConnection(dbName);
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                result = rs.getString(1);
            }
            log.info("[DAOService::getSequenceNextValue] return " + result);
            return result;
        } catch (DbException e) {
            log.error("[DAOService::getSequenceNextValue] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } catch (SQLException e) {
            log.error("[DAOService::getSequenceNextValue] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } finally {
            log.info("[DAOService::getSequenceNextValue] END");
            closeConnection(con, pstmt, rs);
        }
    }

    /**
	 * Esegue la query e restituisce i il risultato all'interno di una
	 * Collection
	 * 
	 * @param dbName
	 *            nome logico del db
	 * @param query
	 * @return
	 * @throws ServiceException
	 */
    public Collection<T> find(String dbName, Query query) throws ServiceException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            log.debug(query.getSql());
            Collection results = new ArrayList();
            DBConnection dbConnection = getDbConnection();
            log.debug("Classe connessione db: " + dbConnection);
            con = dbConnection.getConnection(dbName);
            pstmt = con.prepareStatement(query.getSql());
            int i;
            addParametersToQuery(query, pstmt);
            rs = pstmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();
            while (rs.next()) {
                Map map = new HashMap();
                for (i = 0; i < colCount; i++) {
                    String columnName = rsmd.getColumnName(i + 1);
                    int type = rsmd.getColumnType(i + 1);
                    Object value = null;
                    DbFieldInfo dbFieldInfo = new DbFieldInfo();
                    dbFieldInfo.setColumnName(columnName);
                    dbFieldInfo.setSqlType(type);
                    switch(type) {
                        case Types.BLOB:
                            value = rs.getBlob(columnName);
                            break;
                        case Types.CLOB:
                            value = rs.getClob(columnName);
                            break;
                        default:
                            value = rs.getString(columnName);
                    }
                    dbFieldInfo.setValue(value);
                    map.put(columnName, dbFieldInfo);
                }
                results.add(new DAOUtil(getBundle()).bind(query.getName(), map));
            }
            return results;
        } catch (MarshalException e) {
            log.error("[DAOService::find] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } catch (ValidationException e) {
            log.error("[DAOService::find] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } catch (SQLException e) {
            log.error("[DAOService::find] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } catch (Exception e) {
            log.error("[DAOService::find] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } finally {
            closeConnection(con, pstmt, rs);
        }
    }

    /**
	 * Esegue l'aggiornamento sul db
	 * 
	 * @param dbName
	 *            nome logico del db
	 * @param query
	 * @return
	 * @throws ServiceException
	 */
    public boolean update(String dbName, Query query) throws ServiceException {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = getDbConnection().getConnection(dbName);
            pstmt = con.prepareStatement(query.getSql());
            addParametersToQuery(query, pstmt);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (DbException e) {
            log.error("[DAOService::update] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } catch (SQLException e) {
            log.error("[DAOService::update] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } finally {
            closeConnection(con, pstmt, null);
        }
    }

    /**
	 * Permette l'esecuzione di operazioni di inserimento , cancellazione e
	 * aggiornamento in una stessa sessione di connessioni. Autocommit �
	 * impostato a false
	 * 
	 * @param dbName
	 * @param query
	 * @return
	 * @throws ServiceException
	 */
    public boolean update(String dbName, Query[] queries) throws ServiceException {
        Connection con = null;
        PreparedStatement pstmt = null;
        int rows = 0;
        try {
            con = getDbConnection().getConnection(dbName);
            con.setAutoCommit(false);
            for (int i = 0; i < queries.length; i++) {
                Query query = queries[i];
                System.out.println(query.getSql());
                pstmt = con.prepareStatement(query.getSql());
                addParametersToQuery(query, pstmt);
                rows += pstmt.executeUpdate();
            }
            con.commit();
            return rows > 0;
        } catch (DbException e) {
            log.error("[DAOService::update]  " + e.getMessage(), e);
            log.error("[DAOService::update] Execute rollback " + e.getMessage(), e);
            try {
                con.rollback();
            } catch (SQLException e1) {
                log.error("[DAOService::update] Errore durante il rollback " + e.getMessage(), e);
                throw new ServiceException(e.getMessage());
            }
            throw new ServiceException(e.getMessage());
        } catch (SQLException e) {
            log.error("[DAOService::update]  " + e.getMessage(), e);
            try {
                con.rollback();
            } catch (SQLException e1) {
                log.error("[DAOService::update] Errore durante il rollback " + e.getMessage(), e);
                throw new ServiceException(e.getMessage());
            }
            throw new ServiceException(e.getMessage());
        } finally {
            closeConnection(con, pstmt, null);
        }
    }

    /**
	 * Verifica se la connessione � attiva.
	 * 
	 * @param dbName
	 *            nome logico del db
	 * @return
	 * @throws ServiceException
	 */
    public boolean checkConnection(String dbName) throws ServiceException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = getDbConnection().getConnection(dbName);
            pstmt = con.prepareStatement("select SYSDATE from dual");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return true;
            }
            return false;
        } catch (DbException e) {
            log.error("[DAOService::find] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } catch (SQLException e) {
            log.error("[DAOService::find] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } finally {
            closeConnection(con, pstmt, rs);
        }
    }

    /**
	 * <p>
	 * Gestisce l'inserimento dei campi lob.
	 * 
	 * </p>
	 * 
	 * @param type
	 *            tipo del campo {@link DbParam#BLOB} oppure
	 *            {@link DbParam#CLOB}
	 * @param dbName
	 *            nome logico del db
	 * @param insert
	 *            oggetto contenente la query per inserire un cambo blob/clob
	 *            vuoto.
	 * @param select
	 *            oggetto contenente la select per recuperare il campo CLOB/BLOB
	 *            vuoto
	 * @param update
	 *            query di aggiornamento con il BLOB/CLOB pieno
	 * @return
	 * @deprecated use {@link DAOServiceLob#insertLob}
	 * @throws ServiceException
	 */
    public boolean insertLob(int type, String dbName, Query insert, Query select, Query update) throws ServiceException {
        log.info("[DAOService::insertBlob] BEGIN");
        log.info("[DAOService::insertBlob] Inserisco BLOB vuoto");
        boolean result = update(dbName, insert);
        result = updateLob(type, dbName, select, update);
        return result;
    }

    /**
	 * Aggiorna un campo BLOB/CLOB della tabella
	 * 
	 * @param type
	 *            tipo del campo {@link DbParam#BLOB} oppure
	 *            {@link DbParam#CLOB}
	 * @param dbName
	 *            nome logico del db
	 * @param select
	 *            oggetto contenente la select per recuperare il campo CLOB/BLOB
	 *            vuoto
	 * @param update
	 *            query di aggiornamento con il BLOB/CLOB pieno
	 * @return
	 * @deprecated
	 * @throws ServiceException
	 */
    public boolean updateLob(int type, String dbName, Query select, Query update) throws ServiceException {
        log.info("[DAOService::updateBlob] BEGIN");
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean result;
        try {
            con = getDbConnection().getConnection(dbName);
            pstmt = con.prepareStatement(select.getSql());
            log.info("[DAOService::insertBlob] Query: " + select.getSql());
            addParametersToQuery(select, pstmt);
            rs = pstmt.executeQuery();
            Object lob = null;
            if (rs.next()) {
                switch(type) {
                    case DbParam.BLOB:
                        lob = rs.getBlob(1);
                        break;
                    case DbParam.CLOB:
                        lob = rs.getClob(1);
                        break;
                }
            }
            rs.close();
            pstmt.close();
            log.info("[DAOService::insertBlob] Lob vuoto recuperato: " + lob);
            Collection c = update.getParams();
            for (Iterator iterator = c.iterator(); iterator.hasNext(); ) {
                DbParam dbParam = (DbParam) iterator.next();
                if (dbParam.getType() == DbParam.BLOB || dbParam.getType() == DbParam.CLOB) {
                    dbParam.setLob(lob);
                }
            }
            pstmt = con.prepareStatement(update.getSql());
            addParametersToQuery(update, pstmt);
            result = pstmt.executeUpdate() > 0;
            return result;
        } catch (DbException e) {
            log.error("[DAOService::updateBlob] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } catch (SQLException e) {
            log.error("[DAOService::updateBlob] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } finally {
            log.info("[DAOService::updateBlob] END");
            closeConnection(con, pstmt, rs);
        }
    }

    public byte[] getBlob(String dbName, Query select) throws ServiceException {
        log.info("[DAOService::getBlob] BEGIN");
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        byte[] bytes = null;
        try {
            con = getDbConnection().getConnection(dbName);
            pstmt = con.prepareStatement(select.getSql());
            addParametersToQuery(select, pstmt);
            rs = pstmt.executeQuery();
            Blob blob = null;
            if (rs.next()) {
                blob = rs.getBlob(1);
                bytes = IOUtils.toByteArray(blob.getBinaryStream());
            }
            log.info("[DAOService::getBlob] return " + blob);
            return bytes;
        } catch (DbException e) {
            log.error("[DAOService::getBlob] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } catch (SQLException e) {
            log.error("[DAOService::getBlob] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } catch (IOException e) {
            log.error("[DAOService::getBlob] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } finally {
            log.info("[DAOService::getBlob] Chiudo le connessioni al db");
            log.info("[DAOService::getBlob] END");
            closeConnection(con, pstmt, rs);
        }
    }

    /**
	 * Esegue una procedura pl/sql
	 * 
	 * @param dbName
	 * @param query
	 * @return
	 * @throws ServiceException
	 */
    public boolean callFunctionWithNoReturn(String dbName, Query query) throws ServiceException {
        log.info("[DAOService::callFunctionWithNoReturn] BEGIN");
        Connection con = null;
        CallableStatement cstmt = null;
        try {
            con = getDbConnection().getConnection(dbName);
            cstmt = con.prepareCall(query.getSql());
            addParametersToQuery(query, cstmt);
            boolean result = cstmt.execute();
            return result;
        } catch (DbException e) {
            log.error("[DAOService::callFunctionWithNoReturn] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } catch (SQLException e) {
            log.error("[DAOService::callFunctionWithNoReturn] " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } finally {
            log.info("[DAOService::callFunctionWithNoReturn] Chiudo la connessione");
            closeConnection(con, cstmt, null);
            log.info("[DAOService::callFunctionWithNoReturn] END");
        }
    }

    /**
	 * 
	 * @param dbKeyName nome logico del db
	 * @param tableName nome della tabella con i parametri applicativi
	 * @param keyColumnName nome colonna con il nome della property
	 * @param valueColumnName nome della colonna con il valore della property
	 * @return
	 * @throws ServiceException
	 */
    public DatabaseConfiguration getDbConfiguration(String dbKeyName, String tableName, String keyColumnName, String valueColumnName) throws ServiceException {
        try {
            DataSource datasource = getDbConnection().getDataSource(dbKeyName);
            DatabaseConfiguration configuration = new DatabaseConfiguration(datasource, tableName, keyColumnName, valueColumnName);
            configuration.setThrowExceptionOnMissing(true);
            return configuration;
        } catch (DbException e) {
            log.error("[DAOService::getDbConfiguration]  " + e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
	 * 
	 * @param dbKeyName nome logico del db
	 * @param tableName nome della tabella con i parametri applicativi. I nomi dell colonne della tabella con la coppia chiave/valore si chiamano NOME e VALORE
	 * @return
	 * @throws ServiceException
	 */
    public DatabaseConfiguration getDbConfiguration(String dbKeyName, String tableName) throws ServiceException {
        return getDbConfiguration(dbKeyName, tableName, "NOME", "VALORE");
    }

    /**
	 * 
	 * Il nome della tabella con i parametri applicativi � PARAMETRI_APPLICATIVI. I nomi delle colonne della tabella con la coppia chiave/valore si chiamano NOME e VALORE
	 * @param dbKeyName nome logico del db
	 * @return
	 * @throws ServiceException
	 */
    public DatabaseConfiguration getDbConfiguration(String dbKeyName) throws ServiceException {
        return getDbConfiguration(dbKeyName, "PARAMETRI_APPLICATIVI", "NOME", "VALORE");
    }

    public DAOServiceLob getDAOServiceLob() {
        return new DAOServiceLob(this);
    }

    protected DBConnection getDbConnection() {
        DBConnection dbConnection = DBConnectionFactory.getInstance();
        return dbConnection;
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    /***************************************************************************
	 * METODI PRIVATI DELLA CLASSE
	 */
    void addParametersToQuery(Query query, PreparedStatement pstmt) throws ServiceException, SQLException {
        Set params = query.getParams();
        int i = 0;
        for (Iterator iter = params.iterator(); iter.hasNext(); i++) {
            DbParam param = (DbParam) iter.next();
            if (param.getPos() == 0 || param.getPos() == -1) {
                throw new ServiceException("Parametro con posizionamento non corretto: " + param);
            }
            Method m = (Method) htMethodsToCall.get(new Integer(param.getType()));
            try {
                if (m.getName().equalsIgnoreCase("setBinaryStream")) {
                    InputStream in = new ByteArrayInputStream((byte[]) param.getValue());
                    m.invoke(pstmt, new Object[] { new Integer(param.getPos()), in, new Integer(((byte[]) param.getValue()).length) });
                } else {
                    if (param.getType() == DbParam.BLOB) {
                        LOBWrapper wrapper = LOBWrapperFactory.getInstance(LOBWrapperFactory.BLOB_WRAPPER);
                        wrapper.setLob(param.getLob());
                        wrapper.writeLob(param.getValue());
                        m.invoke(pstmt, new Object[] { new Integer(param.getPos()), param.getLob() });
                        continue;
                    }
                    if (param.getType() == DbParam.CLOB) {
                        LOBWrapper wrapper = LOBWrapperFactory.getInstance(LOBWrapperFactory.CLOB_WRAPPER);
                        wrapper.setLob(param.getLob());
                        wrapper.writeLob(param.getValue());
                        m.invoke(pstmt, new Object[] { new Integer(param.getPos()), param.getLob() });
                        continue;
                    }
                    m.invoke(pstmt, new Object[] { new Integer(param.getPos()), param.getValue() });
                }
            } catch (IllegalArgumentException e) {
                log.error("[DAOService::addParametersToQuery] " + e.getMessage(), e);
                throw new ServiceException(e.getMessage());
            } catch (IllegalAccessException e) {
                log.error("[DAOService::addParametersToQuery] " + e.getMessage(), e);
                throw new ServiceException(e.getMessage());
            } catch (InvocationTargetException e) {
                log.error("[DAOService::addParametersToQuery] " + e.getMessage(), e);
                throw new ServiceException(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void closeConnection(Connection con, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
            if (con != null) {
                con.close();
            }
        } catch (SQLException e) {
            log.info("[DAOService::find] Errore nella chiusura della connessione al db");
            log.warn("[DAOService::find] " + e.getMessage(), e);
        }
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }
}

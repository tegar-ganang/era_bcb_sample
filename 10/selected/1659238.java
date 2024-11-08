package org.utopia.efreet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import org.apache.log4j.Logger;

/**
 * DAO basic class <br>
 * This is the class generated by the Factory that can be used by the BLOBs
 * to access the RDBMS.<br>
 * To use this class you must first generate an instance through the factory.
 * <br>
 * After that you must set the jdbc connection with the RDBMS.<br>
 * Then the DAO is ready to go. The following examples describe some 
 * common operations :
 * <UL>
 *   <LI>Queries - For queries you don't need to set the values of the
 * columns in the DAO. All you need is the name of the query defined in
 * the model and set the parameters as an array of Objects.</LI>
 * 
 */
public class DataAccessObject {

    /** JDBC Connection with the RDBMS */
    protected Connection con = null;

    /** Number of records retrieved on the last query */
    protected int totalLastQuery = 0;

    /** The model of this DAO */
    protected DAOModel model = null;

    /** Flag que indica modo de transacao para fechar a conexao */
    protected boolean transactionMode = false;

    static Logger logger = Logger.getLogger(DataAccessObject.class.getName());

    public Connection getConnection() {
        return this.con;
    }

    public void setConnection(Connection connection) {
        this.con = connection;
    }

    public int getTotalLastQuery() {
        return this.totalLastQuery;
    }

    public void setTotalLastQuery(int param) {
        this.totalLastQuery = param;
    }

    public DAOModel getModel() {
        return this.model;
    }

    public void setModel(DAOModel param) {
        this.model = param;
    }

    private HashMap conditionalQueries = null;

    /**
     * Sets the parameters to the prepared statement
     * @param ps prepared statement
     * @param queryObj Query object
     * @param params parameters
     */
    private void setParameters(PreparedStatement ps, Query queryObj, QueryParameter params) throws SQLException, Exception {
        Vector vp = queryObj.getParameters();
        if (vp != null && params != null) {
            for (int i = 1; i < vp.size(); i++) {
                Object thisObj = vp.get(i);
                if (thisObj != null && thisObj instanceof ParameterModel) {
                    ParameterModel model = (ParameterModel) thisObj;
                    Object thisParam = null;
                    if (params.getParametersHash() != null && !params.getParametersHash().isEmpty()) {
                        thisParam = params.get(model.getParamName());
                    } else {
                        thisParam = params.getParameters()[i - 1];
                    }
                    if (thisParam != null) {
                        if (thisParam instanceof java.util.Date) {
                            long milis = ((java.util.Date) thisParam).getTime();
                            if (model.getParamType() == Types.TIME) {
                                logger.debug("[TIME]");
                                Time pTm = new Time(milis);
                                ps.setTime(i, pTm);
                            } else if (model.getParamType() == Types.TIMESTAMP) {
                                logger.debug("[TIMESTAMP]");
                                Timestamp pTm = new Timestamp(milis);
                                ps.setTimestamp(i, pTm);
                            } else {
                                logger.debug("[DATE]");
                                java.sql.Date pDt = new java.sql.Date(milis);
                                ps.setDate(i, pDt);
                            }
                        } else {
                            if (model.getParamType() == Types.CHAR && thisParam instanceof String) {
                                if (model.getParamSize() > 0 && ((String) thisParam).length() > model.getParamSize()) {
                                    ps.setString(i, ((String) thisParam).substring(0, model.getParamSize()));
                                } else {
                                    ps.setString(i, ((String) thisParam));
                                }
                            } else {
                                ps.setObject(i, thisParam);
                            }
                        }
                    } else {
                        ps.setNull(i, model.getParamType());
                    }
                }
            }
        }
    }

    /**
     * Basic Query Operation - Several Elements
     *
     * @param query Name of the query defined for the query to be executed
     * @param params object QueryParameter with the parameters for the query
     */
    public Collection executeQuery(String query, QueryParameter params) throws DAOException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        ArrayList col = new ArrayList();
        Query queryObj = getModel().getQuery(query);
        if (conditionalQueries != null && conditionalQueries.containsKey(query)) {
            queryObj = (Query) conditionalQueries.get(query);
        }
        String sql = queryObj.getStatement(params.getVariables());
        logger.debug(sql);
        try {
            if (con == null || con.isClosed()) {
                con = DataSource.getInstance().getConnection(getModel().getDataSource());
            }
            ps = con.prepareStatement(sql);
            setParameters(ps, queryObj, params);
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int numberOfColumns = metaData.getColumnCount();
            while (rs.next()) {
                QueryResult elemento = new QueryResult();
                for (int i = 1; i <= numberOfColumns; i++) {
                    ResultModel rModel = queryObj.getResult(i);
                    String columnName = metaData.getColumnName(i);
                    if (rModel != null && rModel.getResultName() != null) {
                        columnName = rModel.getResultName();
                    }
                    int dataType = metaData.getColumnType(i);
                    if (rModel != null && rModel.getResultType() != Types.JAVA_OBJECT) {
                        dataType = rModel.getResultType();
                    }
                    if (dataType == Types.TIMESTAMP) {
                        elemento.set(columnName, rs.getTimestamp(i));
                    } else {
                        elemento.set(columnName, rs.getObject(i));
                    }
                }
                col.add(elemento);
            }
            setTotalLastQuery(col.size());
            logger.debug("Total Registros : " + getTotalLastQuery());
            return col;
        } catch (SQLException e) {
            logger.error("DataBase Error :", e);
            transactionMode = false;
            throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", e.getMessage());
        } catch (Exception ex) {
            logger.error("Error :", ex);
            transactionMode = false;
            throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", ex.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (!transactionMode && con != null) con.close();
            } catch (SQLException e) {
                logger.fatal("error.DAO.database", e);
                throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", e.getMessage());
            }
        }
    }

    /**
     * Basic Query Operation - Block of several ordered Elements
     *
     * @param query Name of the query defined for the query to be executed
     * @param params object QueryParameter with the parameters for the query
     * @param firstElement first element of the block
     * @param nofElements block size
     */
    public Collection executeQuery(String query, QueryParameter params, int firstElement, int nofElements) throws DAOException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        ArrayList col = new ArrayList();
        Query queryObj = getModel().getQuery(query);
        if (conditionalQueries != null && conditionalQueries.containsKey(query)) {
            queryObj = (Query) conditionalQueries.get(query);
        }
        String sql = queryObj.getStatement(params.getVariables());
        logger.debug(sql);
        try {
            if (con == null || con.isClosed()) {
                con = DataSource.getInstance().getConnection(getModel().getDataSource());
            }
            ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            setParameters(ps, queryObj, params);
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int numberOfColumns = metaData.getColumnCount();
            if ((rs != null) && (rs.absolute(firstElement))) {
                do {
                    QueryResult elemento = new QueryResult();
                    for (int i = 1; i <= numberOfColumns; i++) {
                        ResultModel rModel = queryObj.getResult(i);
                        String columnName = metaData.getColumnName(i);
                        if (rModel != null && rModel.getResultName() != null) {
                            columnName = rModel.getResultName();
                        }
                        int dataType = metaData.getColumnType(i);
                        if (rModel != null && rModel.getResultType() != Types.JAVA_OBJECT) {
                            dataType = rModel.getResultType();
                        }
                        if (dataType == Types.TIMESTAMP) {
                            elemento.set(columnName, rs.getTimestamp(i));
                        } else {
                            elemento.set(columnName, rs.getObject(i));
                        }
                    }
                    col.add(elemento);
                } while (rs.next() && (rs.getRow() < firstElement + nofElements));
                logger.debug("Ultimo registro recuperado : " + rs.getRow());
                rs.last();
                setTotalLastQuery(rs.getRow());
                logger.debug("Total Registros : " + getTotalLastQuery());
            }
            return col;
        } catch (SQLException e) {
            logger.error("DataBase Error :", e);
            transactionMode = false;
            throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", e.getMessage());
        } catch (Exception ex) {
            logger.error("Error :", ex);
            transactionMode = false;
            throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", ex.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (!transactionMode && con != null) con.close();
            } catch (SQLException e) {
                throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", e.getMessage());
            }
        }
    }

    /**
     * Execute query but returns a single result instead of a collection
     * @param query String containing the name of the query to execute
     * @param params object QueryParameter with the parameters for the query
     * @return A QueryResult object
     * @throws DAOException in case of error
     */
    public QueryResult executeQuerySingle(String query, QueryParameter params) throws DAOException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        QueryResult result = null;
        Query queryObj = getModel().getQuery(query);
        if (conditionalQueries != null && conditionalQueries.containsKey(query)) {
            queryObj = (Query) conditionalQueries.get(query);
        }
        String sql = queryObj.getStatement(params.getVariables());
        logger.debug(sql);
        try {
            if (con == null || con.isClosed()) {
                con = DataSource.getInstance().getConnection(getModel().getDataSource());
            }
            ps = con.prepareStatement(sql);
            setParameters(ps, queryObj, params);
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int numberOfColumns = metaData.getColumnCount();
            if (rs.next()) {
                result = new QueryResult();
                for (int i = 1; i <= numberOfColumns; i++) {
                    ResultModel rModel = queryObj.getResult(i);
                    String columnName = metaData.getColumnName(i);
                    if (rModel != null && rModel.getResultName() != null) {
                        columnName = rModel.getResultName();
                    }
                    int dataType = metaData.getColumnType(i);
                    if (rModel != null && rModel.getResultType() != Types.JAVA_OBJECT) {
                        dataType = rModel.getResultType();
                    }
                    if (dataType == Types.TIMESTAMP) {
                        result.set(columnName, rs.getTimestamp(i));
                    } else {
                        result.set(columnName, rs.getObject(i));
                    }
                }
            }
            setTotalLastQuery((result != null) ? 1 : 0);
            logger.debug("Total Registros : " + getTotalLastQuery());
            return result;
        } catch (SQLException e) {
            logger.error("DataBase Error :", e);
            transactionMode = false;
            throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", e.getMessage());
        } catch (Exception ex) {
            logger.error("Error :", ex);
            transactionMode = false;
            throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", ex.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (!transactionMode && con != null) con.close();
            } catch (SQLException e) {
                logger.fatal("error.DAO.database", e);
                throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", e.getMessage());
            }
        }
    }

    /**
     * Overwrite of the method in case you don't need parameters for the query
     * @param query name of the query defined
     * @return
     * @throws DAOException
     */
    public Collection executeQuery(String query) throws DAOException {
        return executeQuery(query, new QueryParameter());
    }

    /**
     * Execute a query
     * @deprecated Use {@link #executeQuery(String, QueryParameter)} instead
     * @param query name of the query defined
     * @param params
     * @return
     * @throws DAOException
     */
    public Collection executeQuery(String query, Object[] params) throws DAOException {
        QueryParameter qp = new QueryParameter();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                qp.add(params[i]);
            }
        }
        return executeQuery(query, qp);
    }

    /**
     * Execute a query starting and ending at specific positions in the result set
     * @param query name of the query defined
     * @param firstElement
     * @param nofElements
     * @return
     * @throws DAOException
     */
    public Collection executeQuery(String query, int firstElement, int nofElements) throws DAOException {
        return executeQuery(query, new QueryParameter(), firstElement, nofElements);
    }

    /**
     * Execute a query starting and ending at specific positions in the result set 
     * @deprecated Use @link #executeQuery(String, QueryParameter, int, int) instead
     * @param query name of the query defined
     * @param params
     * @param firstElement
     * @param nofElements
     * @return
     * @throws DAOException
     */
    public Collection executeQuery(String query, Object[] params, int firstElement, int nofElements) throws DAOException {
        QueryParameter qp = new QueryParameter();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                qp.add(params[i]);
            }
        }
        return executeQuery(query, qp, firstElement, nofElements);
    }

    /**
     * Execute a query returning a single result
     * @param query name of the query defined
     * @return
     * @throws DAOException
     */
    public QueryResult executeQuerySingle(String query) throws DAOException {
        return executeQuerySingle(query, new QueryParameter());
    }

    /**
     * Execute a query returning a single result
     * @deprecated Use @link #executeQuerySingle(String, QueryParameter) instead
     * @param query name of the query defined
     * @return
     * @throws DAOException
     */
    public QueryResult executeQuerySingle(String query, Object[] params) throws DAOException {
        QueryParameter qp = new QueryParameter();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                qp.add(params[i]);
            }
        }
        return executeQuerySingle(query, qp);
    }

    /**
     * Method to execute update, insert and delete operation
     * @param query query name
     * @param params parameters
     * @return int number of updated rows
     * @throws DAOException
     */
    public int executeUpdate(String query, QueryParameter params) throws DAOException {
        PreparedStatement ps = null;
        Query queryObj = getModel().getQuery(query);
        if (conditionalQueries != null && conditionalQueries.containsKey(query)) {
            queryObj = (Query) conditionalQueries.get(query);
        }
        String sql = queryObj.getStatement(params.getVariables());
        logger.debug(sql);
        try {
            if (con == null || con.isClosed()) {
                con = DataSource.getInstance().getConnection(getModel().getDataSource());
            }
            ps = con.prepareStatement(sql);
            setParameters(ps, queryObj, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("DataBase Error :", e);
            if (transactionMode) rollback();
            transactionMode = false;
            throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", e.getMessage());
        } catch (Exception ex) {
            logger.error("Error :", ex);
            if (transactionMode) rollback();
            transactionMode = false;
            throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", ex.getMessage());
        } finally {
            try {
                if (!transactionMode) con.commit();
                if (ps != null) ps.close();
                if (!transactionMode && con != null) con.close();
            } catch (SQLException e) {
                throw new DAOException("Unexpected Error Query (" + query + ")", "error.DAO.database", e.getMessage());
            }
        }
    }

    /**
     * Execute update, insert and delete operation
     * @param query
     * @return
     * @throws DAOException
     */
    public int executeUpdate(String query) throws DAOException {
        return executeUpdate(query, new QueryParameter());
    }

    /**
     * Execute update, insert and delete operation
     * @deprecated Use @link #executeUpdate(String, QueryParameter) instead
     * @param query
     * @param params
     * @return
     * @throws DAOException
     */
    public int executeUpdate(String query, Object[] params) throws DAOException {
        QueryParameter qp = new QueryParameter();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                qp.add(params[i]);
            }
        }
        return executeUpdate(query, qp);
    }

    /**
     * The transaction mode of the DAO 
	 * @return Returns the transactionMode.
	 */
    public boolean isTransactionMode() {
        return transactionMode;
    }

    /**
	 * If transactionMode is set to true, it means that the DAO will NOT close
	 * the connection after the statements, and will take care of rollbacks.
	 * However, you must close the connection at the end of your transaction.
	 * @param transactionMode The transactionMode to set.
	 */
    public void setTransactionMode(boolean transactionMode) {
        this.transactionMode = transactionMode;
    }

    /**
     * Commit the transaction
     */
    public boolean commit() throws SQLException, DAOException {
        try {
            if ((this.con == null) || (this.con.isClosed())) return false;
            this.con.commit();
            return true;
        } catch (SQLException e) {
            logger.error("Commit Error : ", e);
            return false;
        }
    }

    /**
     * Rollback on the transaction. Since this method is supposed to
     * be called often from inside catch statements, it does not throws
     * an exception , instead it returns false if something went wrong.
     */
    public boolean rollback() {
        try {
            if ((this.con == null) || (this.con.isClosed())) return false;
            this.con.rollback();
            return true;
        } catch (SQLException e) {
            logger.error("Rollback Error : ", e);
            return false;
        }
    }

    /**
     * Close current connection, these method is supposed to be called if you
     * set the transaction mode to true.
     * @return true if the connection is successfully closed
     */
    public boolean close() {
        try {
            if ((this.con == null) || (this.con.isClosed())) return false;
            this.con.close();
            return true;
        } catch (SQLException e) {
            logger.error("Connection Close Error : ", e);
            return false;
        }
    }

    /**
     * Appends a conditional query to the end of a query
     * @param queryName query name
     * @param conditional conditional name
     */
    public void appendConditionalToQuery(String queryName, String conditional) {
        Query query = getModel().getQuery(queryName);
        if (conditionalQueries != null && conditionalQueries.containsKey(queryName)) {
            query = (Query) conditionalQueries.get(queryName);
        }
        Query condt = getModel().getQuery(conditional);
        if (query != null && condt != null) {
            try {
                query = (Query) query.clone();
                condt = (Query) condt.clone();
            } catch (CloneNotSupportedException e) {
                query = null;
                condt = null;
            }
        }
        if (query != null && condt != null) {
            if (conditionalQueries == null) {
                conditionalQueries = new HashMap();
            }
            String qStatement = query.getStatement();
            String cStatement = condt.getStatement();
            if (qStatement != null && cStatement != null) {
                query.setStatement(qStatement.concat(" ".concat(cStatement)));
            }
            Vector qParameters = query.getParameters();
            Vector cParameters = condt.getParameters();
            if (cParameters != null) {
                if (qParameters == null) {
                    qParameters = new Vector();
                    qParameters.add(null);
                }
                Iterator iter = cParameters.iterator();
                while (iter != null && iter.hasNext()) {
                    Object o = iter.next();
                    if (o != null) {
                        qParameters.add(o);
                    }
                }
                query.setParameters(qParameters);
            }
            conditionalQueries.put(query.getName(), query);
        }
    }

    /**
     * Removes the conditional appends for this query
     * @param queryName name of the query 
     */
    public void resetConditionalQuery(String queryName) {
        if (conditionalQueries != null && queryName != null) {
            conditionalQueries.remove(queryName);
        }
    }

    /**
     * Removes all conditional queries for all queries
     */
    public void resetAllConditionals() {
        if (conditionalQueries != null) {
            conditionalQueries.clear();
        }
    }
}

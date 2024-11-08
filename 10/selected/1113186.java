package de.iteratec.turm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import de.iteratec.turm.common.Logger;
import de.iteratec.turm.common.TurmProperties;
import de.iteratec.turm.dao.common.OlVersionCheck;
import de.iteratec.turm.dao.common.UpdateStatement;
import de.iteratec.turm.exceptions.DaoException;
import de.iteratec.turm.exceptions.UniqueConstaintException;

/**
 * A base class for DAOs. 
 * 
 * Provides functionality to execute select, update and insert statements within 
 * one transaction. 
 */
public class BaseDao {

    private static final Logger LOGGER = Logger.getLogger(BaseDao.class);

    private DataSource ds = null;

    /**
   * Looks for the configured data source and stores it for future reference.
   * 
   * @throws DaoException When the data source could not be retrieved.
   */
    protected BaseDao() throws DaoException {
        InitialContext cxt = null;
        try {
            cxt = new InitialContext();
        } catch (NamingException e) {
            throw new DaoException("error.noDatasource", e);
        }
        try {
            String datasource = TurmProperties.getProperties().getProperty(TurmProperties.TURM_DATASOURCE);
            ds = (DataSource) cxt.lookup(datasource);
        } catch (NamingException e) {
            throw new DaoException("error.noDatasource", e);
        }
        if (ds == null) {
            throw new DaoException("error.noDatasource");
        }
    }

    /**
   * Execute one select query.
   * 
   * @param query The query to execute.
   * @param columns The columns the query will return.
   * @param params The parameters that are to be set into the prepared statement.
   * @return A List of Object[]. A List entry represents a row and the Object[] the
   *         values in that row.
   * @throws DaoException
   */
    protected List<Object[]> executeQuery(String query, String[] columns, Object[] params) throws DaoException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("start executeQuery");
        }
        ResultSet rs = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        List<Object[]> result = new ArrayList<Object[]>();
        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            conn.rollback();
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            stmt = conn.prepareStatement(query);
            if (params != null) {
                for (int parameterIndex = 1; parameterIndex <= params.length; parameterIndex++) {
                    stmt.setObject(parameterIndex, params[parameterIndex - 1]);
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(" **** Sending statement:\n" + query);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                Object[] row = new Object[columns.length];
                for (int columnIndex = 1; columnIndex <= columns.length; columnIndex++) {
                    row[columnIndex - 1] = rs.getObject(columnIndex);
                }
                result.add(row);
            }
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
            conn.commit();
            conn.close();
            conn = null;
        } catch (SQLException e) {
            LOGGER.error("database error", e);
            throw new DaoException("error.databaseError", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                throw new DaoException("error.databaseError", e);
            } finally {
                rs = null;
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                throw new DaoException("error.databaseError", e);
            } finally {
                stmt = null;
            }
            try {
                if (conn != null) {
                    conn.rollback();
                    conn.close();
                }
            } catch (SQLException e) {
                throw new DaoException("error.databaseError", e);
            } finally {
                conn = null;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("finish executeQuery");
        }
        return result;
    }

    /**
   * Performs a number of insert or update statements within one transaction.
   * 
   * @param statements The update or insert statements to execute.
   * @param olVersionCheck A check for the optimistic locking version. 
   *                       If it fails, no statements will be executed. Can be null.
   * @return The number of affected rows. Returns -1 when the olVersionCheck failed.
   * @throws DaoException
   */
    protected int executeUpdates(List<UpdateStatement> statements, OlVersionCheck olVersionCheck) throws DaoException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("start executeUpdates");
        }
        PreparedStatement stmt = null;
        Connection conn = null;
        int rowsAffected = 0;
        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            conn.rollback();
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            if (olVersionCheck != null) {
                stmt = conn.prepareStatement(olVersionCheck.getQuery());
                stmt.setObject(1, olVersionCheck.getId());
                ResultSet rs = stmt.executeQuery();
                rs.next();
                Number olVersion = (Number) rs.getObject("olVersion");
                stmt.close();
                stmt = null;
                if (olVersion.intValue() != olVersionCheck.getOlVersionToCheck().intValue()) {
                    rowsAffected = -1;
                }
            }
            if (rowsAffected >= 0) {
                for (UpdateStatement query : statements) {
                    stmt = conn.prepareStatement(query.getQuery());
                    if (query.getParams() != null) {
                        for (int parameterIndex = 1; parameterIndex <= query.getParams().length; parameterIndex++) {
                            Object object = query.getParams()[parameterIndex - 1];
                            stmt.setObject(parameterIndex, object);
                        }
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(" **** Sending statement:\n" + query.getQuery());
                    }
                    rowsAffected += stmt.executeUpdate();
                    stmt.close();
                    stmt = null;
                }
            }
            conn.commit();
            conn.close();
            conn = null;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                LOGGER.info("Integrity constraint violation", e);
                throw new UniqueConstaintException();
            }
            throw new DaoException("error.databaseError", e);
        } finally {
            try {
                if (stmt != null) {
                    LOGGER.debug("closing open statement!");
                    stmt.close();
                }
            } catch (SQLException e) {
                throw new DaoException("error.databaseError", e);
            } finally {
                stmt = null;
            }
            try {
                if (conn != null) {
                    LOGGER.debug("rolling back open connection!");
                    conn.rollback();
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                throw new DaoException("error.databaseError", e);
            } finally {
                conn = null;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("finish executeUpdates");
        }
        return rowsAffected;
    }

    /**
   * Determines the next valid id.
   * 
   * Since iTURM has to work with different databases, the key generation is done
   * explicitly by storing the next valid id in a database table. This method 
   * fetches the next valid id and increases it by one.
   * 
   * @return The next valid id.
   * @throws DaoException If the next id could not be read or it could not be 
   *                      incremented.
   */
    public Number getNextId() throws DaoException {
        List<Object[]> nextIdRes = executeQuery("select max(nextId) as nextId from im_id_table", new String[] { "nextId" }, null);
        Number nextId = (Number) nextIdRes.get(0)[0];
        List<UpdateStatement> statement = new ArrayList<UpdateStatement>();
        statement.add(new UpdateStatement("update im_id_table set nextId=(nextId+1) where nextId = ?", new Number[] { nextId }, false));
        int affectedRows = executeUpdates(statement, null);
        if (affectedRows == 0) {
            throw new DaoException("error.concurrentError");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("returning id: " + nextId);
        }
        return nextId;
    }
}

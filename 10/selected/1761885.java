package edu.clemson.cs.nestbed.server.adaptation.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.clemson.cs.nestbed.common.model.ProgramProfilingSymbol;
import edu.clemson.cs.nestbed.server.adaptation.AdaptationException;
import edu.clemson.cs.nestbed.server.adaptation.ProgramProfilingSymbolAdapter;

public class ProgramProfilingSymbolSqlAdapter extends SqlAdapter implements ProgramProfilingSymbolAdapter {

    private static final Log log = LogFactory.getLog(ProgramProfilingSymbolSqlAdapter.class);

    private enum Index {

        ID, PDCONFID, PROGSYMID, TIMESTAMP;

        public int index() {
            return ordinal() + 1;
        }
    }

    public Map<Integer, ProgramProfilingSymbol> readProgramProfilingSymbols() throws AdaptationException {
        Map<Integer, ProgramProfilingSymbol> profilingSymbols = new HashMap<Integer, ProgramProfilingSymbol>();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "SELECT * FROM ProgramProfilingSymbols";
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                ProgramProfilingSymbol profilingSymbol = getProfilingSymbol(resultSet);
                profilingSymbols.put(profilingSymbol.getID(), profilingSymbol);
            }
        } catch (SQLException ex) {
            String msg = "SQLException in readProgramProfilingSymbols";
            log.error(msg, ex);
            throw new AdaptationException(msg, ex);
        } finally {
            try {
                resultSet.close();
            } catch (Exception ex) {
            }
            try {
                statement.close();
            } catch (Exception ex) {
            }
            try {
                connection.close();
            } catch (Exception ex) {
            }
        }
        return profilingSymbols;
    }

    public ProgramProfilingSymbol createNewProfilingSymbol(int configID, int programSymbolID) throws AdaptationException {
        ProgramProfilingSymbol profilingSymbol = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "INSERT INTO ProgramProfilingSymbols" + "(projectDeploymentConfigurationID, programSymbolID)" + " VALUES (" + configID + ", " + programSymbolID + ")";
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            statement.executeUpdate(query);
            query = "SELECT * FROM ProgramProfilingSymbols WHERE " + "projectDeploymentConfigurationID = " + configID + " AND programSymbolID             = " + programSymbolID;
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Attempt to create program profiling " + "symbol failed.";
                log.error(msg);
                throw new AdaptationException(msg);
            }
            profilingSymbol = getProfilingSymbol(resultSet);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in createNewProfilingSymbol";
            log.error(msg, ex);
            throw new AdaptationException(msg, ex);
        } finally {
            try {
                resultSet.close();
            } catch (Exception ex) {
            }
            try {
                statement.close();
            } catch (Exception ex) {
            }
            try {
                connection.close();
            } catch (Exception ex) {
            }
        }
        return profilingSymbol;
    }

    public ProgramProfilingSymbol deleteProfilingSymbol(int id) throws AdaptationException {
        ProgramProfilingSymbol profilingSymbol = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "SELECT * FROM ProgramProfilingSymbols " + "WHERE id = " + id;
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Attempt to delete program profiling " + "symbol failed.";
                log.error(msg);
                throw new AdaptationException(msg);
            }
            profilingSymbol = getProfilingSymbol(resultSet);
            query = "DELETE FROM ProgramProfilingSymbols " + "WHERE id = " + id;
            statement.executeUpdate(query);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in deleteProfilingSymbol";
            log.error(msg, ex);
            throw new AdaptationException(msg, ex);
        } finally {
            try {
                resultSet.close();
            } catch (Exception ex) {
            }
            try {
                statement.close();
            } catch (Exception ex) {
            }
            try {
                connection.close();
            } catch (Exception ex) {
            }
        }
        return profilingSymbol;
    }

    public ProgramProfilingSymbol updateProgramProfilingSymbol(int id, int configID, int programSymbolID) throws AdaptationException {
        ProgramProfilingSymbol pps = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "UPDATE ProgramProfilingSymbols SET " + "projectDeploymentConfigurationID = " + configID + ", " + "programSymbolID                  = " + programSymbolID + ", " + "WHERE id = " + id;
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            statement.executeUpdate(query);
            query = "SELECT * from ProgramProfilingSymbols WHERE " + "id = " + id;
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Attempt to update program profiling " + "symbol failed.";
                log.error(msg);
                throw new AdaptationException(msg);
            }
            pps = getProfilingSymbol(resultSet);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in updateProgramProfilingSymbol";
            log.error(msg, ex);
            throw new AdaptationException(msg, ex);
        } finally {
            try {
                resultSet.close();
            } catch (Exception ex) {
            }
            try {
                statement.close();
            } catch (Exception ex) {
            }
            try {
                connection.close();
            } catch (Exception ex) {
            }
        }
        return pps;
    }

    private final ProgramProfilingSymbol getProfilingSymbol(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt(Index.ID.index());
        int tbDepConfID = resultSet.getInt(Index.PDCONFID.index());
        int programSymbolID = resultSet.getInt(Index.PROGSYMID.index());
        Date timestamp = resultSet.getDate(Index.TIMESTAMP.index());
        return new ProgramProfilingSymbol(id, tbDepConfID, programSymbolID, timestamp);
    }
}

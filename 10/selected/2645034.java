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
import edu.clemson.cs.nestbed.common.model.ProgramProfilingMessageSymbol;
import edu.clemson.cs.nestbed.server.adaptation.AdaptationException;
import edu.clemson.cs.nestbed.server.adaptation.ProgramProfilingMessageSymbolAdapter;

public class ProgramProfilingMessageSymbolSqlAdapter extends SqlAdapter implements ProgramProfilingMessageSymbolAdapter {

    private static final Log log = LogFactory.getLog(ProgramProfilingMessageSymbolSqlAdapter.class);

    private enum Index {

        ID, PDCONFID, PROGMSGSYMID, TIMESTAMP;

        public int index() {
            return ordinal() + 1;
        }
    }

    public Map<Integer, ProgramProfilingMessageSymbol> readProgramProfilingMessageSymbols() throws AdaptationException {
        Map<Integer, ProgramProfilingMessageSymbol> profilingMessageSymbols;
        profilingMessageSymbols = new HashMap<Integer, ProgramProfilingMessageSymbol>();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "SELECT * FROM ProgramProfilingMessageSymbols";
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                ProgramProfilingMessageSymbol profilingMessageSymbol = getProfilingMessageSymbol(resultSet);
                profilingMessageSymbols.put(profilingMessageSymbol.getID(), profilingMessageSymbol);
            }
        } catch (SQLException ex) {
            String msg = "SQLException in readProgramProfilingMessageSymbols";
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
        return profilingMessageSymbols;
    }

    public ProgramProfilingMessageSymbol createNewProfilingMessageSymbol(int configID, int programMessageSymbolID) throws AdaptationException {
        ProgramProfilingMessageSymbol profilingMessageSymbol = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "INSERT INTO ProgramProfilingMessageSymbols" + "(projectDeploymentConfigurationID, programMessageSymbolID)" + " VALUES (" + configID + ", " + programMessageSymbolID + ")";
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            statement.executeUpdate(query);
            query = "SELECT * FROM ProgramProfilingMessageSymbols WHERE " + "projectDeploymentConfigurationID = " + configID + " AND " + "programMessageSymbolID           = " + programMessageSymbolID;
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Attempt to create program profiling message " + "symbol failed.";
                log.error(msg);
                throw new AdaptationException(msg);
            }
            profilingMessageSymbol = getProfilingMessageSymbol(resultSet);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in createNewProfilingMessageSymbol";
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
        return profilingMessageSymbol;
    }

    public ProgramProfilingMessageSymbol deleteProfilingMessageSymbol(int id) throws AdaptationException {
        ProgramProfilingMessageSymbol profilingMessageSymbol = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "SELECT * FROM ProgramProfilingMessageSymbols " + "WHERE id = " + id;
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Attempt to delete program profiling message " + "symbol failed.";
                log.error(msg);
                throw new AdaptationException(msg);
            }
            profilingMessageSymbol = getProfilingMessageSymbol(resultSet);
            query = "DELETE FROM ProgramProfilingMessageSymbols " + "WHERE id = " + id;
            statement.executeUpdate(query);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in deleteProfilingMessageSymbol";
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
        return profilingMessageSymbol;
    }

    private final ProgramProfilingMessageSymbol getProfilingMessageSymbol(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt(Index.ID.index());
        int pdConfId = resultSet.getInt(Index.PDCONFID.index());
        int progMsgSymID = resultSet.getInt(Index.PROGMSGSYMID.index());
        Date timestamp = resultSet.getDate(Index.TIMESTAMP.index());
        return new ProgramProfilingMessageSymbol(id, pdConfId, progMsgSymID, timestamp);
    }
}

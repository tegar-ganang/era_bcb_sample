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
import edu.clemson.cs.nestbed.common.model.Program;
import edu.clemson.cs.nestbed.server.adaptation.AdaptationException;
import edu.clemson.cs.nestbed.server.adaptation.ProgramAdapter;

public class ProgramSqlAdapter extends SqlAdapter implements ProgramAdapter {

    private static final Log log = LogFactory.getLog(ProgramSqlAdapter.class);

    private enum Index {

        ID, PROJECTID, NAME, DESCRIPTION, SOURCEPATH, TIMESTAMP;

        public int index() {
            return ordinal() + 1;
        }
    }

    public Map<Integer, Program> readPrograms() throws AdaptationException {
        Map<Integer, Program> programs = new HashMap<Integer, Program>();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "SELECT * FROM Programs";
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                Program program = getProgram(resultSet);
                programs.put(program.getID(), program);
            }
        } catch (SQLException ex) {
            String msg = "SQLException in readPrograms";
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
        return programs;
    }

    public Program createNewProgram(int projectID, String name, String description) throws AdaptationException {
        Program program = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection(CONN_STR);
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            String query = "INSERT INTO Programs(projectID, name, " + "description, sourcePath) VALUES ( " + projectID + ", " + "'" + name + "', " + "'" + description + "', " + "'" + "[unknown]" + "')";
            log.debug("SQL Query:\n" + query);
            statement.executeUpdate(query);
            query = "SELECT * FROM Programs WHERE " + " projectID   =  " + projectID + "  AND " + " name        = '" + name + "' AND " + " description = '" + description + "'";
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Attempt to create program failed";
                log.error(msg);
                throw new AdaptationException(msg);
            }
            program = getProgram(resultSet);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in createNewProgram";
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
        return program;
    }

    public Program updateProgramPath(int id, String sourcePath) throws AdaptationException {
        Program program = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "UPDATE Programs SET " + "sourcePath = '" + sourcePath + "' " + "WHERE id = " + id;
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            statement.executeUpdate(query);
            query = "SELECT * from Programs WHERE id = " + id;
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Attempt to update program failed.";
                log.error(msg);
                throw new AdaptationException(msg);
            }
            program = getProgram(resultSet);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in updateProgramPath";
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
        return program;
    }

    public Program deleteProgram(int id) throws AdaptationException {
        Program program = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "SELECT * FROM Programs WHERE id = " + id;
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Attempt to delete program failed.";
                log.error(msg);
                throw new AdaptationException(msg);
            }
            program = getProgram(resultSet);
            query = "DELETE FROM Programs WHERE id = " + id;
            statement.executeUpdate(query);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in deleteProgram";
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
        return program;
    }

    private final Program getProgram(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt(Index.ID.index());
        int projectID = resultSet.getInt(Index.PROJECTID.index());
        String name = resultSet.getString(Index.NAME.index());
        String description = resultSet.getString(Index.DESCRIPTION.index());
        String sourcePath = resultSet.getString(Index.SOURCEPATH.index());
        Date timestamp = resultSet.getDate(Index.TIMESTAMP.index());
        return new Program(id, projectID, name, description, sourcePath, timestamp);
    }
}

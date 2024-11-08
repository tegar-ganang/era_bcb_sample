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
import edu.clemson.cs.nestbed.common.model.Project;
import edu.clemson.cs.nestbed.server.adaptation.AdaptationException;
import edu.clemson.cs.nestbed.server.adaptation.ProjectAdapter;

public class ProjectSqlAdapter extends SqlAdapter implements ProjectAdapter {

    private static final Log log = LogFactory.getLog(ProjectSqlAdapter.class);

    private enum Index {

        ID, TESTBEDID, NAME, DESCRIPTION, TIMESTAMP;

        public int index() {
            return ordinal() + 1;
        }
    }

    public Map<Integer, Project> readProjects() throws AdaptationException {
        Map<Integer, Project> projects = new HashMap<Integer, Project>();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "SELECT * FROM Projects";
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                Project project = getProject(resultSet);
                projects.put(project.getID(), project);
            }
        } catch (SQLException ex) {
            String msg = "SQLException in readProjects";
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
        return projects;
    }

    public Project createProject(int testbedID, String name, String description) throws AdaptationException {
        Project project = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "INSERT INTO Projects(testbedID, name, " + "description) VALUES (" + testbedID + ", '" + name + "', '" + description + "')";
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            statement.executeUpdate(query);
            query = "SELECT * FROM Projects WHERE " + " testbedID   = " + testbedID + "  AND " + " name        = '" + name + "' AND " + " description = '" + description + "'";
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Attempt to create project failed.";
                log.error(msg);
                throw new AdaptationException(msg);
            }
            project = getProject(resultSet);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in createProject";
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
        return project;
    }

    public Project deleteProject(int projectID) throws AdaptationException {
        Project project = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "SELECT * FROM Projects WHERE id = " + projectID;
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Attempt to delete project failed.";
                log.error(msg);
                throw new AdaptationException(msg);
            }
            project = getProject(resultSet);
            query = "DELETE FROM Projects WHERE id = " + projectID;
            statement.executeUpdate(query);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in deleteProject";
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
        return project;
    }

    private final Project getProject(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt(Index.ID.index());
        int testbedID = resultSet.getInt(Index.TESTBEDID.index());
        String name = resultSet.getString(Index.NAME.index());
        String description = resultSet.getString(Index.DESCRIPTION.index());
        Date timestamp = resultSet.getDate(Index.TIMESTAMP.index());
        return new Project(id, testbedID, name, description, timestamp);
    }
}

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
import edu.clemson.cs.nestbed.common.model.MoteDeploymentConfiguration;
import edu.clemson.cs.nestbed.server.adaptation.AdaptationException;
import edu.clemson.cs.nestbed.server.adaptation.MoteDeploymentConfigurationAdapter;

public class MoteDeploymentConfigurationSqlAdapter extends SqlAdapter implements MoteDeploymentConfigurationAdapter {

    private static final Log log = LogFactory.getLog(MoteDeploymentConfigurationSqlAdapter.class);

    private enum Index {

        ID, PROJECTCONFID, MOTEID, PROGRAMID, RADIOPOWERLEVEL, TIMESTAMP;

        public int index() {
            return ordinal() + 1;
        }
    }

    public Map<Integer, MoteDeploymentConfiguration> readMoteDeploymentConfigurations() throws AdaptationException {
        Map<Integer, MoteDeploymentConfiguration> moteDepConfigs = new HashMap<Integer, MoteDeploymentConfiguration>();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "SELECT * FROM MoteDeploymentConfigurations";
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                MoteDeploymentConfiguration moteDepConfiguration;
                moteDepConfiguration = getMoteDeploymentConfiguration(resultSet);
                moteDepConfigs.put(moteDepConfiguration.getID(), moteDepConfiguration);
            }
        } catch (SQLException ex) {
            String msg = "SQLException in readMoteDeploymentConfigurations";
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
        return moteDepConfigs;
    }

    public MoteDeploymentConfiguration updateMoteDeploymentConfiguration(int mdConfigID, int programID, int radioPowerLevel) throws AdaptationException {
        MoteDeploymentConfiguration mdc = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "UPDATE MoteDeploymentConfigurations SET " + "programID       = " + programID + ", " + "radioPowerLevel = " + radioPowerLevel + "  " + "WHERE id = " + mdConfigID;
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            statement.executeUpdate(query);
            query = "SELECT * from MoteDeploymentConfigurations WHERE " + "id = " + mdConfigID;
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Unable to select updated config.";
                log.error(msg);
                ;
                throw new AdaptationException(msg);
            }
            mdc = getMoteDeploymentConfiguration(resultSet);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in updateMoteDeploymentConfiguration";
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
        return mdc;
    }

    public MoteDeploymentConfiguration addMoteDeploymentConfiguration(int projectDepConfID, int moteID, int programID, int radioPowerLevel) throws AdaptationException {
        MoteDeploymentConfiguration mdc = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "INSERT INTO MoteDeploymentConfigurations(" + "projectDeploymentConfigurationID, " + "moteID, programID, radioPowerLevel) VALUES (" + projectDepConfID + ", " + moteID + ", " + programID + ", " + radioPowerLevel + ")";
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            statement.executeUpdate(query);
            query = "SELECT * from MoteDeploymentConfigurations WHERE " + "projectDeploymentConfigurationID = " + projectDepConfID + " AND moteID = " + moteID;
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                connection.rollback();
                String msg = "Unable to select newly added config.";
                log.error(msg);
                ;
                throw new AdaptationException(msg);
            }
            mdc = getMoteDeploymentConfiguration(resultSet);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in addMoteDeploymentConfiguration";
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
        return mdc;
    }

    public MoteDeploymentConfiguration deleteMoteDeploymentConfiguration(int id) throws AdaptationException {
        MoteDeploymentConfiguration mdc = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String query = "SELECT * FROM MoteDeploymentConfigurations " + "WHERE id = " + id;
            connection = DriverManager.getConnection(CONN_STR);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                String msg = "Unable to select config to delete.";
                log.error(msg);
                throw new AdaptationException(msg);
            }
            mdc = getMoteDeploymentConfiguration(resultSet);
            query = "DELETE FROM MoteDeploymentConfigurations " + "WHERE id = " + id;
            statement.executeUpdate(query);
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (Exception e) {
            }
            String msg = "SQLException in deleteMoteDeploymentConfiguration";
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
        return mdc;
    }

    private final MoteDeploymentConfiguration getMoteDeploymentConfiguration(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt(Index.ID.index());
        int configID = resultSet.getInt(Index.PROJECTCONFID.index());
        int moteID = resultSet.getInt(Index.MOTEID.index());
        int programID = resultSet.getInt(Index.PROGRAMID.index());
        int radioPowerLevel = resultSet.getInt(Index.RADIOPOWERLEVEL.index());
        Date timestamp = resultSet.getDate(Index.TIMESTAMP.index());
        return new MoteDeploymentConfiguration(id, configID, moteID, programID, radioPowerLevel, timestamp);
    }
}

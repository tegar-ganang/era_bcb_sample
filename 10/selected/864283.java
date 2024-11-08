package gpsmate.io.dbase.mysql;

import gpsmate.geodata.GeoDataContainer;
import gpsmate.geodata.Placemark;
import gpsmate.geodata.Point;
import gpsmate.geodata.Track;
import gpsmate.gui.utils.StateSaver;
import gpsmate.io.dbase.DatabaseSettingsDialog;
import gpsmate.io.dbase.GpsDatabaseExporter;
import gpsmate.utils.Configuration;
import gpsmate.utils.FileTool;
import gpsmate.utils.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * MySqlExporter
 * 
 * @author longdistancewalker
 * 
 */
public class MySqlExporter extends GpsDatabaseExporter {

    private static final String MYSQL_SQL_SCHEMA_REPLACEMENT = "###SCHEMA_NAME###";

    private static final String MYSQL_SQL_TRACK_ID_REPLACEMENT = "###TRACK_ID###";

    private static final String MYSQL_SQL_PLACEMARK_ID_REPLACEMENT = "###PLACEMARK_ID###";

    private static final String MYSQL_SQL_LAT = "###LAT###";

    private static final String MYSQL_SQL_LON = "###LON###";

    private static final String MYSQL_SQL_ELEVATION = "###ELEVATION###";

    private static final String MYSQL_SQL_NAME = "###NAME###";

    private static final String MYSQL_SQL_DESCRIPTION = "###DESCRIPTION###";

    private static final String MYSQL_SQL_ICON = "###ICON###";

    private Properties language = null;

    public MySqlExporter() {
        language = Configuration.getInstance().getLanguageConfiguration();
    }

    private Connection getConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://" + StateSaver.getInstance().getDatabaseSettings().getHost() + ":" + StateSaver.getInstance().getDatabaseSettings().getPort() + "?user=" + StateSaver.getInstance().getDatabaseSettings().getUsername() + "&password=" + StateSaver.getInstance().getDatabaseSettings().getPassword());
            return con;
        } catch (ClassNotFoundException e) {
            Logger.logException(e);
        } catch (SQLException e) {
            Logger.logException(e);
        }
        JOptionPane.showMessageDialog(null, language.getProperty("database.messages.notconnected"), language.getProperty("dialog.error.title"), JOptionPane.ERROR_MESSAGE);
        return null;
    }

    @Override
    public boolean setupDatabaseSchema() {
        Configuration cfg = Configuration.getInstance();
        Connection con = getConnection();
        if (null == con) return false;
        try {
            String sql = FileTool.readFile(cfg.getProperty("database.sql.rootdir") + System.getProperty("file.separator") + cfg.getProperty("database.sql.mysql.setupschema"));
            sql = sql.replaceAll(MYSQL_SQL_SCHEMA_REPLACEMENT, StateSaver.getInstance().getDatabaseSettings().getSchema());
            con.setAutoCommit(false);
            Statement stmt = con.createStatement();
            String[] sqlParts = sql.split(";");
            for (String sqlPart : sqlParts) {
                if (sqlPart.trim().length() > 0) stmt.executeUpdate(sqlPart);
            }
            con.commit();
            JOptionPane.showMessageDialog(null, language.getProperty("database.messages.executionsuccess"), language.getProperty("dialog.information.title"), JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (SQLException e) {
            Logger.logException(e);
        }
        try {
            if (con != null) con.rollback();
        } catch (SQLException e) {
            Logger.logException(e);
        }
        JOptionPane.showMessageDialog(null, language.getProperty("database.messages.executionerror"), language.getProperty("dialog.error.title"), JOptionPane.ERROR_MESSAGE);
        return false;
    }

    @Override
    public boolean export(GeoDataContainer geoData) {
        Connection con = getConnection();
        if (null == con) return false;
        try {
            con.setAutoCommit(false);
            for (Track t : geoData.getTracks()) insertTrack(t, con);
            for (Placemark p : geoData.getPlacemarks()) insertPlacemark(p, con, null);
            con.commit();
            return true;
        } catch (SQLException e) {
            Logger.logException(e);
        }
        try {
            con.rollback();
        } catch (SQLException e) {
            Logger.logException(e);
        }
        return false;
    }

    private boolean insertTrack(Track t, Connection c) throws SQLException {
        Configuration cfg = Configuration.getInstance();
        String sql = FileTool.readFile(cfg.getProperty("database.sql.rootdir") + System.getProperty("file.separator") + cfg.getProperty("database.sql.mysql.inserttrack"));
        sql = sql.replaceAll(MYSQL_SQL_SCHEMA_REPLACEMENT, StateSaver.getInstance().getDatabaseSettings().getSchema());
        sql = sql.replaceAll(MYSQL_SQL_NAME, t.getName());
        sql = sql.replaceAll(MYSQL_SQL_DESCRIPTION, t.getDescription() != null ? t.getDescription() : "");
        Statement stmt = c.createStatement();
        stmt.executeUpdate(sql);
        sql = FileTool.readFile(cfg.getProperty("database.sql.rootdir") + System.getProperty("file.separator") + cfg.getProperty("database.sql.mysql.lasttrackid"));
        sql = sql.replaceAll(MYSQL_SQL_SCHEMA_REPLACEMENT, StateSaver.getInstance().getDatabaseSettings().getSchema());
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            int trackId = rs.getInt(1);
            for (Point p : t.getPoints()) {
                sql = FileTool.readFile(cfg.getProperty("database.sql.rootdir") + System.getProperty("file.separator") + cfg.getProperty("database.sql.mysql.inserttrackcoordinates"));
                sql = sql.replaceAll(MYSQL_SQL_SCHEMA_REPLACEMENT, StateSaver.getInstance().getDatabaseSettings().getSchema());
                sql = sql.replaceAll(MYSQL_SQL_TRACK_ID_REPLACEMENT, Integer.toString(trackId));
                sql = sql.replaceAll(MYSQL_SQL_LAT, Double.toString(p.getLatitude()));
                sql = sql.replaceAll(MYSQL_SQL_LON, Double.toString(p.getLongitude()));
                sql = sql.replaceAll(MYSQL_SQL_ELEVATION, Double.toString(p.getElevation()));
                stmt.executeUpdate(sql);
            }
            for (Placemark p : t.getPointsOfInterest()) insertPlacemark(p, c, trackId);
            return true;
        } else {
            throw new SQLException("Last ID not found");
        }
    }

    private boolean insertPlacemark(Placemark p, Connection c, Integer parent) throws SQLException {
        Configuration cfg = Configuration.getInstance();
        String sql = FileTool.readFile(cfg.getProperty("database.sql.rootdir") + System.getProperty("file.separator") + cfg.getProperty("database.sql.mysql.insertplacemark"));
        sql = sql.replaceAll(MYSQL_SQL_SCHEMA_REPLACEMENT, StateSaver.getInstance().getDatabaseSettings().getSchema());
        sql = sql.replaceAll(MYSQL_SQL_NAME, p.getName());
        sql = sql.replaceAll(MYSQL_SQL_DESCRIPTION, p.getDescription() != null ? p.getDescription() : "");
        sql = sql.replaceAll(MYSQL_SQL_ICON, p.getIconName() != null ? p.getIconName() : "");
        Statement stmt = c.createStatement();
        stmt.executeUpdate(sql);
        sql = FileTool.readFile(cfg.getProperty("database.sql.rootdir") + System.getProperty("file.separator") + cfg.getProperty("database.sql.mysql.lastplacemarkid"));
        sql = sql.replaceAll(MYSQL_SQL_SCHEMA_REPLACEMENT, StateSaver.getInstance().getDatabaseSettings().getSchema());
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            int placemarkId = rs.getInt(1);
            sql = FileTool.readFile(cfg.getProperty("database.sql.rootdir") + System.getProperty("file.separator") + cfg.getProperty("database.sql.mysql.insertplacemarklocation"));
            sql = sql.replaceAll(MYSQL_SQL_SCHEMA_REPLACEMENT, StateSaver.getInstance().getDatabaseSettings().getSchema());
            sql = sql.replaceAll(MYSQL_SQL_PLACEMARK_ID_REPLACEMENT, Integer.toString(placemarkId));
            sql = sql.replaceAll(MYSQL_SQL_LAT, Double.toString(p.getLocation().getLatitude()));
            sql = sql.replaceAll(MYSQL_SQL_LON, Double.toString(p.getLocation().getLongitude()));
            sql = sql.replaceAll(MYSQL_SQL_ELEVATION, Double.toString(p.getLocation().getElevation()));
            stmt.executeUpdate(sql);
            if (parent != null) {
                sql = FileTool.readFile(cfg.getProperty("database.sql.rootdir") + System.getProperty("file.separator") + cfg.getProperty("database.sql.mysql.inserttracksplacemarks"));
                sql = sql.replaceAll(MYSQL_SQL_SCHEMA_REPLACEMENT, StateSaver.getInstance().getDatabaseSettings().getSchema());
                sql = sql.replaceAll(MYSQL_SQL_PLACEMARK_ID_REPLACEMENT, Integer.toString(placemarkId));
                sql = sql.replaceAll(MYSQL_SQL_TRACK_ID_REPLACEMENT, parent.toString());
                stmt.executeUpdate(sql);
            }
            return true;
        } else {
            throw new SQLException("Last placemark ID not found");
        }
    }

    @Override
    public JDialog getConfigurationDialog() {
        DatabaseSettingsDialog d = new DatabaseSettingsDialog(null);
        d.setShowSetupSchemaButton(true);
        d.setExporter(this);
        return d;
    }

    @Override
    public String getDescription() {
        return Configuration.getInstance().getLanguageConfiguration().getProperty("export.exporters.mysql.description");
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isExportable(int numberOfSelectedTracks, int numberOfSelectedPlacemarks) {
        return numberOfSelectedTracks > 0 || numberOfSelectedPlacemarks > 0;
    }
}

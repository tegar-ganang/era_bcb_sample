package com.art.anette.client.database;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import com.art.anette.client.controller.BasicController;
import com.art.anette.client.main.Global;
import com.art.anette.common.FileUtils;
import com.art.anette.common.SharedGlobal;
import com.art.anette.datamodel.datacontrol.DBConnector;

/**
 * Realisiert die Datenverbindung für den Client.
 *
 * @author Markus Groß
 */
public class ClientDB extends DBConnector {

    /**
     * Der Dateiname der SQLite Datenbankdatei.
     */
    private String filename;

    /**
     * Erzeugt eine neue Datenbankverbindung.
     *
     * @param employeeId Die ID des eingeloggten Benutzers.
     * @throws IOException
     */
    public ClientDB(long employeeId) throws IOException {
        super("sa", "", "");
        String baseFilename = SharedGlobal.APP_HOME_DIR + "db-" + Long.toString(employeeId);
        File scriptFile = new File(baseFilename + ".script");
        File propFile = new File(baseFilename + ".properties");
        if (!scriptFile.exists() || !propFile.exists()) {
            FileUtils.copyFile(getClass().getResourceAsStream(Global.DEFAULT_DB_SCRIPT), scriptFile);
            FileUtils.copyFile(getClass().getResourceAsStream(Global.DEFAULT_DB_PROPERTIES), propFile);
        }
        filename = baseFilename;
    }

    @Override
    public boolean isConnected() {
        return connection != null;
    }

    /**
     * Gibt die Adresse der SQLite Datenbank-Datei an.
     *
     * @return Die URL für die SQLite Datenbank.
     */
    @Override
    protected String getDBUrl() {
        return String.format("jdbc:hsqldb:file:%s;shutdown=true", filename);
    }

    /**
     * Liefert die Klasse für den SQLite Datenbank-Treiber zurück.
     *
     * @return Die Klasse für den SQLite Datenbank-Treiber.
     */
    @Override
    protected String getDBClass() {
        return "org.hsqldb.jdbcDriver";
    }

    protected void handleSQLException(SQLException ex) throws SQLException {
        throw new SQLException(ex.getMessage() + BasicController.getInstance().getHistory(), ex);
    }

    protected void addToHistory(String query) {
        BasicController.getInstance().addHistory("SQL " + query);
    }

    public String getFilename() {
        return filename;
    }
}

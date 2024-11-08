package jmodnews.db.mckoi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import jmodnews.controller.Controller;
import jmodnews.db.export.McKoiExporter;
import jmodnews.logging.ExceptionHandler;
import jmodnews.logging.Logger;

/**
 * Helper class for creating and maintaining McKoi databases for jModNews
 * @author Michael Schierl <schierlm@gmx.de>
 */
public class McKoiHelper {

    public static String username = "jmodnews";

    public static String password = "jmodnews";

    private String url1, url2;

    private final File profileDir;

    public McKoiHelper(File profileDir) {
        this.profileDir = profileDir;
        url1 = makeURL(profileDir, "db");
        url2 = makeURL(profileDir, "artcldb");
    }

    public void createDB() {
        createDB(new File(profileDir, "db"), url1);
        createDB(new File(profileDir, "artcldb"), url2);
    }

    public void createDB(File dir, String url) {
        try {
            dir.mkdirs();
            if (new File(dir, "db.conf").exists()) {
                Logger.log(Logger.DEBUG, "Database already exists.");
                return;
            }
            Logger.log(Logger.INFO, "Creating new database...");
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(dir, "db.conf")));
            bw.write("database_path=./data\n" + "log_path=./log\n" + "root_path=configuration\n" + "jdbc_server_port=99999\n" + "ignore_case_for_identifiers=disabled\n" + "regex_library=gnu.regexp\n" + "data_cache_size=2097152\n" + "max_cache_entry_size=8192\n" + "maximum_worker_threads=2\n" + "debug_log_file=debug.log\n" + "debug_level=20\n");
            bw.flush();
            bw.close();
            Connection connection = DriverManager.getConnection(url + "?create=true", username, password);
            connection.close();
        } catch (SQLException ex) {
            ExceptionHandler.handle(ex);
        } catch (IOException ex) {
            ExceptionHandler.handle(ex);
            ex.printStackTrace();
        }
    }

    public void fillDatabase() {
        fillDatabase(url1, DatabaseHelper.TABLES, DatabaseHelper.UPDATES);
        fillDatabase(url2, DatabaseHelper.TABLES2, DatabaseHelper.UPDATES2);
    }

    public void fillDatabase(String url, String[] TABLES, DatabaseHelper.DatabaseUpdate[] updates) {
        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();
            ResultSet rs;
            String version = null;
            try {
                rs = stmt.executeQuery("SELECT value from jmodnews_globals where name='schema_version'");
                if (rs.next()) {
                    version = rs.getString("value");
                }
                rs.close();
            } catch (SQLException ex) {
                Logger.log(Logger.INFO, "Table jmodnews_globals not found");
            }
            if (version != null) {
                if (version.equals(DatabaseHelper.SCHEMA_VERSION)) {
                    Logger.log(Logger.DEBUG, "Database schema already exists");
                    return;
                } else {
                    Logger.log(Logger.INFO, "Updating database schema...");
                    updateDatabase(conn, version, updates);
                    return;
                }
            }
            Logger.log(Logger.INFO, "Creating database schema...");
            for (int i = 0; i < TABLES.length; i++) {
                stmt.addBatch(TABLES[i]);
            }
            stmt.addBatch("INSERT INTO jmodnews_globals VALUES " + "      ( 'schema_version', '" + DatabaseHelper.SCHEMA_VERSION + "' ) ");
            stmt.executeBatch();
            stmt.close();
            conn.close();
        } catch (SQLException ex) {
            ExceptionHandler.handle(ex);
        }
    }

    private void updateDatabase(Connection conn, String version, DatabaseHelper.DatabaseUpdate[] updates) throws SQLException {
        boolean doUpdate = false;
        for (int i = 0; i < updates.length; i++) {
            if (version.equals(updates[i].getVersion())) doUpdate = true;
            if (doUpdate) {
                String[] changes = updates[i].getChanges();
                if (changes != null && changes.length > 0) {
                    Statement stmt = conn.createStatement();
                    for (int j = 0; j < changes.length; j++) {
                        stmt.addBatch(changes[j]);
                    }
                    stmt.executeBatch();
                    stmt.close();
                }
                if (updates[i].getRunner() != null) {
                    updates[i].getRunner().runUpdate(conn);
                }
            }
        }
        while (!doUpdate) {
            ExceptionHandler.handle(new Error("Unknown database version: " + version));
        }
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("UPDATE jmodnews_globals " + "   SET value = '" + DatabaseHelper.SCHEMA_VERSION + "' " + "   WHERE name = 'schema_version'");
        stmt.close();
    }

    /**
     * @param profileDir
     *
     */
    public static String makeURL(File profileDir, String folder) {
        try {
            String path = profileDir.toURL().getPath();
            return "jdbc:mckoi:local://" + path + "/" + folder + "/db.conf";
        } catch (MalformedURLException ex) {
            ExceptionHandler.handle(ex);
            return null;
        }
    }

    /**
     * @param c
     */
    public static void initializeDB(Controller c) {
        File profileDir = c.getProfileDirectory();
        McKoiHelper mkh = new McKoiHelper(profileDir);
        mkh.createDB();
        mkh.fillDatabase();
        DatabaseImpl db = new DatabaseImpl(c, McKoiHelper.makeURL(profileDir, "db"), McKoiHelper.makeURL(profileDir, "artcldb"), McKoiHelper.username, McKoiHelper.password, profileDir);
        if (false) {
            try {
                Writer w = new FileWriter("databaseDump.txt");
                McKoiExporter.exportDatabase(db, w);
                w.close();
            } catch (IOException ex) {
                ExceptionHandler.handle(ex);
            }
        }
    }
}

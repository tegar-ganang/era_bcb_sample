package com.eroi.migrate.version;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import junit.framework.TestCase;
import com.eroi.migrate.ConfigStore;
import com.eroi.migrate.Configure;
import com.eroi.migrate.Engine;
import com.eroi.migrate.Execute;
import com.eroi.migrate.generators.Generator;
import com.eroi.migrate.generators.GeneratorFactory;
import com.eroi.migrate.misc.Closer;
import db.migrations.Migration_1;
import db.migrations.Migration_5;
import db.migrations.project_x.ProjectX;

public class TestVersionMigration extends TestCase {

    public static final String REPORT_FILE_NAME = "validation_report.log";

    private ConfigStore config;

    private ConfigStore defaultConfiguration;

    protected void setUp() throws Exception {
        super.setUp();
        Configure.configure("migrate4j.test.properties");
        this.defaultConfiguration = Configure.getDefaultConfiguration();
        this.config = new ConfigStore("migrate4j.test.properties");
        String versionTableNew = _getNewVersionTable();
        if (Execute.tableExists(getConnection(), versionTableNew)) {
            Execute.dropTable(getConnection(), versionTableNew);
        }
        String tableName = _getTrueTableName(this.config.getVersionTable());
        if (tableName != null) {
            Execute.dropTable(getConnection(), tableName);
        }
    }

    private Connection getConnection() {
        return this.config.getConnection();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        String versionTableNew = _getNewVersionTable();
        if (Execute.tableExists(getConnection(), versionTableNew)) {
            Execute.dropTable(getConnection(), versionTableNew);
        }
        String tableName = _getTrueTableName(this.config.getVersionTable());
        if (tableName != null) {
            Execute.dropTable(getConnection(), tableName);
        }
        if (Execute.tableExists(getConnection(), Migration_1.TABLE_NAME)) {
            Execute.dropTable(getConnection(), Migration_1.TABLE_NAME);
        }
        if (Execute.tableExists(getConnection(), Migration_5.CHILD_TABLE_NAME)) {
            Execute.dropTable(getConnection(), Migration_5.CHILD_TABLE_NAME);
        }
        if (Execute.tableExists(getConnection(), Migration_5.PARENT_TABLE_NAME)) {
            Execute.dropTable(getConnection(), Migration_5.PARENT_TABLE_NAME);
        }
        if (Execute.tableExists(getConnection(), ProjectX.X1_TABLE_NAME)) {
            Execute.dropTable(getConnection(), ProjectX.X1_TABLE_NAME);
        }
        _writeToFile("\n");
    }

    public void test_noVersionTableSoFar_continueOldStyle() throws SQLException {
        _writeTestStart("No Version Table so far. Continue old style");
        String versionTable = _getNewVersionTable();
        assertFalse(Execute.tableExists(getConnection(), versionTable));
        Engine.migrate(1);
        assertTrue(Execute.tableExists(versionTable));
        assertTrue(2 == _getNumberOfRows(defaultConfiguration, versionTable));
        assertTrue(Execute.columnExists(ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(defaultConfiguration, "com.eroi.migrate.version"));
        assertTrue(1 == VersionQuery.getVersion(defaultConfiguration, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertTrue(Execute.tableExists(Migration_1.TABLE_NAME));
        Engine.migrate(0);
        assertTrue(Execute.tableExists(versionTable));
        assertTrue(2 == _getNumberOfRows(defaultConfiguration, versionTable));
        assertTrue(Execute.columnExists(ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(defaultConfiguration, "com.eroi.migrate.version"));
        assertTrue(0 == VersionQuery.getVersion(defaultConfiguration, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertFalse(Execute.tableExists(Migration_1.TABLE_NAME));
        _writeTestPass();
    }

    public void test_noVersionTableSoFar_switchToNewStyle_setProjectID() throws SQLException {
        _writeTestStart("No Version Table so far. Switch to new style and set project ID");
        this.config.setProjectID("db.migrations");
        String versionTable = _getNewVersionTable();
        assertFalse(Execute.tableExists(getConnection(), versionTable));
        Engine.migrate(this.config, 1);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(2 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(1 == VersionQuery.getVersion(this.config, "db.migrations"));
        assertTrue(Execute.tableExists(Migration_1.TABLE_NAME));
        Engine.migrate(this.config, 0);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(2 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(0 == VersionQuery.getVersion(this.config, "db.migrations"));
        assertFalse(Execute.tableExists(getConnection(), Migration_1.TABLE_NAME));
        _writeTestPass();
    }

    public void test_migrateFormOldVersionTable_continueOldStyle() throws SQLException {
        _writeTestStart("Already existing version table. Continue old style");
        String versionTable = _getNewVersionTable();
        String versionTableOld = this.config.getVersionTable();
        assertFalse(Execute.tableExists(versionTable));
        _createOldStyleVersionTable(4);
        _tableExists(versionTableOld);
        assertTrue(1 == _getNumberOfRows(defaultConfiguration, versionTableOld));
        assertFalse(_columnExists(ConfigStore.PROJECT_FIELD_NAME, versionTableOld));
        assertTrue(_columnExists(ConfigStore.VERSION_FIELD_NAME, versionTableOld));
        Engine.migrate(5);
        assertTrue(Execute.tableExists(versionTable));
        assertTrue(2 == _getNumberOfRows(defaultConfiguration, versionTable));
        assertTrue(Execute.columnExists(ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(defaultConfiguration, "com.eroi.migrate.version"));
        assertTrue(5 == VersionQuery.getVersion(defaultConfiguration, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertFalse(Execute.tableExists(Migration_1.TABLE_NAME));
        assertTrue(Execute.tableExists(Migration_5.PARENT_TABLE_NAME));
        assertTrue(Execute.tableExists(Migration_5.CHILD_TABLE_NAME));
        Engine.migrate(0);
        assertTrue(Execute.tableExists(versionTable));
        assertTrue(2 == _getNumberOfRows(defaultConfiguration, versionTable));
        assertTrue(Execute.columnExists(ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(defaultConfiguration, "com.eroi.migrate.version"));
        assertTrue(0 == VersionQuery.getVersion(defaultConfiguration, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertFalse(Execute.tableExists(Migration_1.TABLE_NAME));
        assertFalse(Execute.tableExists(Migration_5.PARENT_TABLE_NAME));
        assertFalse(Execute.tableExists(Migration_5.CHILD_TABLE_NAME));
        _writeTestPass();
    }

    public void test_migrateFormOldVersionTable_setEstablishedProjectID_switchToNewStyle() throws SQLException {
        _writeTestStart("Already existing version table. Set established project ID and switch to new style");
        String versionTable = _getNewVersionTable();
        String versionTableOld = this.config.getVersionTable();
        assertFalse(Execute.tableExists(getConnection(), versionTable));
        _createOldStyleVersionTable(4);
        _tableExists(versionTableOld);
        assertTrue(1 == _getNumberOfRows(this.config, versionTableOld));
        assertFalse(_columnExists(ConfigStore.PROJECT_FIELD_NAME, versionTableOld));
        assertTrue(_columnExists(ConfigStore.VERSION_FIELD_NAME, versionTableOld));
        this.config.setEstablishedProjectID("db.migrations");
        Engine.migrate(this.config, 5);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(2 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(5 == VersionQuery.getVersion(this.config, "db.migrations"));
        assertFalse(Execute.tableExists(getConnection(), Migration_1.TABLE_NAME));
        assertTrue(Execute.tableExists(getConnection(), Migration_5.PARENT_TABLE_NAME));
        assertTrue(Execute.tableExists(getConnection(), Migration_5.CHILD_TABLE_NAME));
        Engine.migrate(this.config, 0);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(2 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(0 == VersionQuery.getVersion(this.config, "db.migrations"));
        assertFalse(Execute.tableExists(getConnection(), Migration_1.TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_5.PARENT_TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_5.CHILD_TABLE_NAME));
        _writeTestPass();
    }

    public void test_migrateFormOldVersionTable_ignoreEstablishedProject_addNewProject() throws SQLException {
        _writeTestStart("Already existing version table. Ignore established project ID and add a new Project");
        String versionTable = _getNewVersionTable();
        String versionTableOld = this.config.getVersionTable();
        assertFalse(Execute.tableExists(getConnection(), versionTable));
        _createOldStyleVersionTable(4);
        _tableExists(versionTableOld);
        assertTrue(1 == _getNumberOfRows(this.config, versionTableOld));
        assertFalse(_columnExists(ConfigStore.PROJECT_FIELD_NAME, versionTableOld));
        assertTrue(_columnExists(ConfigStore.VERSION_FIELD_NAME, versionTableOld));
        this.config.setPackageName(ProjectX.PACKAGE);
        this.config.setProjectID(ProjectX.PROJEXT_ID);
        Engine.migrate(this.config);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(3 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(2 == VersionQuery.getVersion(this.config, ProjectX.PROJEXT_ID));
        assertTrue(4 == VersionQuery.getVersion(this.config, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertTrue(Execute.tableExists(getConnection(), ProjectX.X1_TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_1.TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_5.PARENT_TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_5.CHILD_TABLE_NAME));
        Engine.migrate(this.config, 0);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(3 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(0 == VersionQuery.getVersion(this.config, ProjectX.PROJEXT_ID));
        assertTrue(4 == VersionQuery.getVersion(this.config, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertFalse(Execute.tableExists(getConnection(), ProjectX.X1_TABLE_NAME));
        _writeTestPass();
    }

    public void test_migrateFormOldVersionTable_setEstablishedProjectID_addNewProject() throws SQLException {
        _writeTestStart("Already existing version table. Set established project ID and add a new Project");
        String versionTable = _getNewVersionTable();
        String versionTableOld = this.config.getVersionTable();
        assertFalse(Execute.tableExists(getConnection(), versionTable));
        _createOldStyleVersionTable(4);
        _tableExists(versionTableOld);
        assertTrue(1 == _getNumberOfRows(this.config, versionTableOld));
        assertFalse(_columnExists(ConfigStore.PROJECT_FIELD_NAME, versionTableOld));
        assertTrue(_columnExists(ConfigStore.VERSION_FIELD_NAME, versionTableOld));
        this.config.setEstablishedProjectID("db.migrations");
        this.config.setPackageName(ProjectX.PACKAGE);
        this.config.setProjectID(ProjectX.PROJEXT_ID);
        Engine.migrate(this.config);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(3 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(2 == VersionQuery.getVersion(this.config, ProjectX.PROJEXT_ID));
        assertTrue(4 == VersionQuery.getVersion(this.config, "db.migrations"));
        assertTrue(Execute.tableExists(getConnection(), ProjectX.X1_TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_1.TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_5.PARENT_TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_5.CHILD_TABLE_NAME));
        Engine.migrate(this.config, 0);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(3 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(0 == VersionQuery.getVersion(this.config, ProjectX.PROJEXT_ID));
        assertTrue(4 == VersionQuery.getVersion(this.config, "db.migrations"));
        assertFalse(Execute.tableExists(getConnection(), ProjectX.X1_TABLE_NAME));
        _writeTestPass();
    }

    public void test_useNewStyle_butForgetProjectID() throws SQLException {
        _writeTestStart("Switch to new style but forget to define a projectID");
        String versionTable = _getNewVersionTable();
        assertFalse(Execute.tableExists(getConnection(), versionTable));
        assertFalse(Execute.tableExists(getConnection(), versionTable));
        Engine.migrate(this.config, 1);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(2 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(1 == VersionQuery.getVersion(this.config, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertTrue(Execute.tableExists(getConnection(), Migration_1.TABLE_NAME));
        Engine.migrate(this.config, 0);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(2 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(0 == VersionQuery.getVersion(this.config, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertFalse(Execute.tableExists(getConnection(), Migration_1.TABLE_NAME));
        _writeTestPass();
    }

    public void test_runMixedMode() throws SQLException {
        _writeTestStart("Already existing version table. Run mixed mode");
        String versionTable = _getNewVersionTable();
        String versionTableOld = this.config.getVersionTable();
        assertFalse(Execute.tableExists(getConnection(), versionTable));
        _createOldStyleVersionTable(4);
        _tableExists(versionTableOld);
        assertTrue(1 == _getNumberOfRows(this.config, versionTableOld));
        assertFalse(_columnExists(ConfigStore.PROJECT_FIELD_NAME, versionTableOld));
        assertTrue(_columnExists(ConfigStore.VERSION_FIELD_NAME, versionTableOld));
        this.config.setPackageName(ProjectX.PACKAGE);
        this.config.setProjectID(ProjectX.PROJEXT_ID);
        Engine.migrate(this.config);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(3 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(2 == VersionQuery.getVersion(this.config, ProjectX.PROJEXT_ID));
        assertTrue(4 == VersionQuery.getVersion(this.config, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertTrue(Execute.tableExists(getConnection(), ProjectX.X1_TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_1.TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_5.PARENT_TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_5.CHILD_TABLE_NAME));
        Engine.migrate(5);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(3 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(2 == VersionQuery.getVersion(this.config, ProjectX.PROJEXT_ID));
        assertTrue(5 == VersionQuery.getVersion(this.config, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertTrue(Execute.tableExists(getConnection(), ProjectX.X1_TABLE_NAME));
        assertTrue(Execute.tableExists(getConnection(), Migration_5.PARENT_TABLE_NAME));
        assertTrue(Execute.tableExists(getConnection(), Migration_5.CHILD_TABLE_NAME));
        Engine.migrate(this.config, 0);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(3 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(0 == VersionQuery.getVersion(this.config, ProjectX.PROJEXT_ID));
        assertTrue(5 == VersionQuery.getVersion(this.config, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertFalse(Execute.tableExists(getConnection(), ProjectX.X1_TABLE_NAME));
        assertTrue(Execute.tableExists(getConnection(), Migration_5.PARENT_TABLE_NAME));
        assertTrue(Execute.tableExists(getConnection(), Migration_5.CHILD_TABLE_NAME));
        Engine.migrate(0);
        assertTrue(Execute.tableExists(getConnection(), versionTable));
        assertTrue(3 == _getNumberOfRows(this.config, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.PROJECT_FIELD_NAME, versionTable));
        assertTrue(Execute.columnExists(getConnection(), ConfigStore.VERSION_FIELD_NAME, versionTable));
        assertTrue(2 <= VersionQuery.getVersion(this.config, "com.eroi.migrate.version"));
        assertTrue(0 == VersionQuery.getVersion(this.config, ProjectX.PROJEXT_ID));
        assertTrue(0 == VersionQuery.getVersion(this.config, ConfigStore.PROJECT_ID_UNSPECIFIED));
        assertFalse(Execute.tableExists(getConnection(), ProjectX.X1_TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_5.PARENT_TABLE_NAME));
        assertFalse(Execute.tableExists(getConnection(), Migration_5.CHILD_TABLE_NAME));
        _writeTestPass();
    }

    private String _getNewVersionTable() {
        return this.config.getFullQualifiedVersionTable();
    }

    private String _getTrueTableName(String tableName) {
        if (!Execute.tableExists(tableName)) {
            tableName = tableName.toLowerCase();
            if (!Execute.tableExists(tableName)) {
                tableName = tableName.toUpperCase();
                if (!Execute.tableExists(tableName)) {
                    tableName = null;
                }
            }
        }
        return tableName;
    }

    private boolean _tableExists(String tableName) {
        boolean result = _getTrueTableName(tableName) != null;
        return result;
    }

    private boolean _columnExists(String columnName, String tableName) {
        tableName = _getTrueTableName(tableName);
        boolean result = Execute.columnExists(columnName, tableName) || Execute.columnExists(columnName.toLowerCase(), tableName) || Execute.columnExists(columnName.toUpperCase(), tableName);
        return result;
    }

    private int _getNumberOfRows(ConfigStore config, String tableName) throws SQLException {
        tableName = _getTrueTableName(tableName);
        Generator g = GeneratorFactory.getGenerator(config.getConnection());
        String query = String.format("SELECT count(*) FROM %s", g.wrapName(tableName));
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = config.getConnection().createStatement();
            resultSet = statement.executeQuery(query);
            if (resultSet != null && resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                throw new IllegalStateException("Cannot count rows of version table for " + config.toString() + " because the result set is empty");
            }
        } finally {
            Closer.close(resultSet);
            Closer.close(statement);
        }
    }

    private void _createOldStyleVersionTable(int version) throws SQLException {
        String query;
        query = "create table version (version int not null primary key)";
        Execute.executeStatement(getConnection(), query);
        query = String.format("INSERT INTO %s (%s) VALUES (%d)", this.config.getVersionTable(), ConfigStore.VERSION_FIELD_NAME, version);
        Execute.executeStatement(getConnection(), query);
    }

    private void _writeTestStart(String message) {
        _writeToFile(message + ":  ");
    }

    private void _writeTestPass() {
        _writeToFile("PASS");
    }

    private static void _writeToFile(String message) {
        File file = new File(REPORT_FILE_NAME);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            writer.write(message);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

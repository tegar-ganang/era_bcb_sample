package ch.intertec.storybook.recoverytool;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import ch.intertec.storybook.model.InternalPeer;
import ch.intertec.storybook.model.TagLinkPeer;
import ch.intertec.storybook.model.TagPeer;
import ch.intertec.storybook.model.PersistenceManager;
import ch.intertec.storybook.toolkit.Constants;
import ch.intertec.storybook.toolkit.PrefManager;
import ch.intertec.storybook.toolkit.Constants.Preference;
import ch.intertec.storybook.toolkit.swing.SwingTools;

public class Recovery {

    private static Logger logger = Logger.getLogger(Recovery.class);

    private static Statement stmt;

    private static int partId;

    private static File file;

    public static boolean doRecovery(File f) {
        try {
            file = f;
            logTitle("Start Recovery");
            log("Tool Version: " + RecoveryTool.VERSION);
            log("Time Stamp: " + new Date());
            log();
            readFromInternalDb();
            if (file == null) {
                log();
                log("Ready.");
                log();
                log("DO NOT USE THIS TOOL UNLESS A FILE SEEMS TO BE BROKEN.");
                log();
                log("Use 'Open file' to open the file you want to recover.");
                return false;
            }
            startRecovery(file);
        } catch (AbortedException e) {
            JOptionPane.showMessageDialog(RecoveryTool.getInstance(), "The recovery process was aborted." + "\nHave a look at the shown log information.", "Recovery process aborted", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (Exception e) {
            SwingTools.showException(e);
            return false;
        }
        return true;
    }

    private static void startRecovery(File file) throws Exception {
        logTitle("starting recovering of \"" + file + "\"");
        File backupFile = new File(file.getAbsolutePath() + ".bak");
        log("backup file " + file + " to " + backupFile);
        FileUtils.copyFile(file, backupFile);
        log();
        log("open file ...");
        openProject(file);
        stmt = PersistenceManager.getInstance().getConnection().createStatement();
        getInfos();
        checkPart();
        checkChapter();
        checkStrand();
        checkNoteFields();
        checkPerson();
        checkItem();
        InternalPeer.setDbModelVersion();
        PersistenceManager.getInstance().closeConnection();
        log();
        log("Recovery has been finished.");
        log("Your original file has been saved as " + backupFile);
        JOptionPane.showMessageDialog(RecoveryTool.getInstance(), "The recovery process has been finished." + "\nPlease copy-paste the log text to a file " + "\nin case the recovery was not sucessful. " + "\n\n*** Close this program before starting Storybook ***", "Recovery process finished", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void getInfos() throws SQLException {
        logTitle("Existing Tables");
        String sql = "select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC'";
        executeSQLStatement(sql);
        ResultSet rs = stmt.getResultSet();
        while (rs.next()) {
            log("- " + rs.getString(1));
        }
    }

    private static void checkPart() throws Exception {
        logTitle("Check table part");
        String sql = "select id from part";
        executeSQLStatement(sql);
        ResultSet rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- no part present");
            throw new AbortedException();
        }
        partId = rs.getInt(1);
        log("- first part has id " + partId);
        sql = "update internal set integer_value = '" + partId + "' where key='partid'";
        executeSQLStatement(sql);
        log("- set active part id to part with id: " + partId);
    }

    private static void checkChapter() throws Exception {
        logTitle("Check table chapter");
        log("- check if table chapter exists ...");
        String sql = "select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC' and TABLE_NAME='CHAPTER'";
        executeSQLStatement(sql);
        ResultSet rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- chapter table doesn't exists");
            throw new AbortedException();
        }
        log("- chapter table exists");
        sql = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='PUBLIC' and TABLE_NAME='CHAPTER' and COLUMN_NAME='PART_ID'";
        executeSQLStatement(sql);
        rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- part_id doesn't exists, create it");
            sql = "alter table chapter add column part_id int";
            executeSQLStatement(sql);
            sql = "update chapter set part_id = " + partId;
            executeSQLStatement(sql);
        } else {
            log("- part_id exists");
        }
    }

    private static void checkStrand() throws Exception {
        logTitle("Check table strand");
        log("- check if column sort exists");
        String sql = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='PUBLIC' and TABLE_NAME='STRAND' and COLUMN_NAME='SORT'";
        executeSQLStatement(sql);
        ResultSet rs = stmt.getResultSet();
        rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- column sort doesn't exist, create it");
            sql = "alter table strand add sort int";
            executeSQLStatement(sql);
            log("- update column sort, set default sorting");
            sql = "select id from strand";
            executeSQLStatement(sql);
            rs = stmt.getResultSet();
            Set<Integer> ids = new TreeSet<Integer>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            int i = 0;
            for (int id : ids) {
                sql = "update strand set sort='" + i + "' where id='" + id + "'";
                executeSQLStatement(sql);
                ++i;
            }
        } else {
            log("- column sort exists");
        }
    }

    private static void checkNoteFields() throws Exception {
        logTitle("Check note fields");
        log("- notes in table scene");
        String sql = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='PUBLIC' and TABLE_NAME='SCENE' and COLUMN_NAME='NOTES'";
        executeSQLStatement(sql);
        ResultSet rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- scene has no notes, create it");
            sql = "alter table scene add notes varchar(4096) default ''";
            executeSQLStatement(sql);
        } else {
            log("- scene has notes");
        }
        log("- notes in table person");
        sql = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='PUBLIC' and TABLE_NAME='PERSON' and COLUMN_NAME='NOTES'";
        executeSQLStatement(sql);
        rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- person has no notes, create it");
            sql = "alter table person add notes varchar(4096) default ''";
            executeSQLStatement(sql);
        } else {
            log("- person has notes");
        }
        log("- notes in table location");
        sql = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='PUBLIC' and TABLE_NAME='LOCATION' and COLUMN_NAME='NOTES'";
        executeSQLStatement(sql);
        rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- location has no notes, create it");
            sql = "alter table location add notes varchar(4096) default ''";
            executeSQLStatement(sql);
        } else {
            log("- location has notes");
        }
        log("- notes in table chapter");
        sql = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='PUBLIC' and TABLE_NAME='CHAPTER' and COLUMN_NAME='NOTES'";
        executeSQLStatement(sql);
        rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- chapter has no notes, create it");
            sql = "alter table chapter add notes varchar(4096) default ''";
            executeSQLStatement(sql);
        } else {
            log("- chapter has notes");
        }
        log("- notes in table strand");
        sql = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='PUBLIC' and TABLE_NAME='STRAND' and COLUMN_NAME='NOTES'";
        executeSQLStatement(sql);
        rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- strand has no notes, create it");
            sql = "alter table strand add notes varchar(4096) default ''";
            executeSQLStatement(sql);
        } else {
            log("- strand has notes");
        }
    }

    private static void checkPerson() throws Exception {
        logTitle("Check table person");
        log("- category in table person");
        String sql = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='PUBLIC' and TABLE_NAME='PERSON' and COLUMN_NAME='CATEGORY'";
        executeSQLStatement(sql);
        ResultSet rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- category doesn't exists, create it");
            sql = "alter table person add category int default 0";
            executeSQLStatement(sql);
            log("- assign all characters to category 1");
            sql = "update person set category = 1";
            executeSQLStatement(sql);
        } else {
            log("- category exists");
        }
    }

    private static void checkItem() throws Exception {
        logTitle("Check table item");
        log("- check if table item exists ...");
        String sql = "select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC' and TABLE_NAME='ITEM'";
        executeSQLStatement(sql);
        ResultSet rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- table item doesn't exists, create it");
            TagPeer.createTable();
        } else {
            log("- table item exists");
        }
        log("- check if table item_link exists ...");
        sql = "select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC' and TABLE_NAME='ITEM_LINK'";
        executeSQLStatement(sql);
        rs = stmt.getResultSet();
        if (!rs.next()) {
            log("- table item_link doesn't exists, create it");
            TagLinkPeer.createTable();
        } else {
            log("- table item_link exists");
        }
    }

    public static boolean openProject(File file) {
        try {
            PersistenceManager.getInstance().open(file);
            PersistenceManager.getInstance().getConnection();
            log("- file \"" + file + "\" is opened");
            String version = InternalPeer.getDbModelVersion();
            log("- file model version: " + version);
            String cVersion = Constants.Application.DB_MODEL_VERSION.toString();
            log("- current model version: " + cVersion);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void readFromInternalDb() {
        log("get internal settings ...");
        String lang = PrefManager.getInstance().getStringValue(Preference.LANG);
        log("- language: " + lang);
        String lastOpenedFile = PrefManager.getInstance().getStringValue(Preference.LAST_OPENED_FILE);
        log("- last opened file: \"" + lastOpenedFile + "\"");
        String lastOpenedDir = PrefManager.getInstance().getStringValue(Preference.LAST_OPENED_DIRECTORY);
        log("- last opened directory: \"" + lastOpenedDir + "\"");
    }

    private static void executeSQLStatement(String sql) {
        try {
            log("SQL: " + sql);
            stmt.execute(sql);
        } catch (SQLException e) {
            logger.error(e);
        }
    }

    private static void log() {
        log("");
    }

    private static void logTitle(String title) {
        log();
        log("============== " + title + " ==============");
    }

    private static void log(String text) {
        logger.info(text);
        DateFormat df = new SimpleDateFormat("kk:mm:ss");
        String now = df.format(new Date());
        RecoveryTool.getInstance().getLoggerPanel().appendText(now + ": " + text + "\n");
    }
}

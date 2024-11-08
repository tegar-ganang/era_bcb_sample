package vqwiki;

import java.util.Collection;
import java.util.ArrayList;
import java.sql.*;
import org.apache.log4j.*;

public class DatabaseHandler implements Handler {

    protected static final String TABLE_TOPIC = "CREATE TABLE Topic(" + "name VARCHAR(255) PRIMARY KEY NOT NULL," + "contents TEXT )";

    protected static final String TABLE_VERSION = "CREATE TABLE TopicVersion(" + "name VARCHAR(255)," + "contents TEXT," + "versionat DATETIME)";

    protected static final String TABLE_CHANGE = "CREATE TABLE TopicChange(" + "topic VARCHAR(255)," + "username VARCHAR(255)," + "changeat DATETIME)";

    protected static final String TABLE_LOCK = "CREATE TABLE TopicLock(" + "topic VARCHAR(255)," + "sessionkey VARCHAR(255)," + "lockat DATETIME)";

    protected static final String TABLE_READ_ONLY = "CREATE TABLE TopicReadOnly(" + "topic VARCHAR(255))";

    protected static final String STATEMENT_READ = "SELECT contents FROM Topic WHERE name = ?";

    protected static final String STATEMENT_UPDATE = "UPDATE Topic SET contents = ? WHERE name = ?";

    protected static final String STATEMENT_INSERT = "INSERT INTO Topic( name, contents ) VALUES ( ?, ? )";

    protected static final String STATEMENT_EXISTS = "SELECT COUNT(*) FROM Topic WHERE name = ?";

    protected static final String STATEMENT_SET_LOCK = "INSERT INTO TopicLock( topic, sessionkey, lockat ) VALUES( ?, ?, ? )";

    protected static final String STATEMENT_CHECK_LOCK = "SELECT lockat FROM TopicLock WHERE topic = ? and sessionkey = ?";

    protected static final String STATEMENT_REMOVE_LOCK = "DELETE FROM TopicLock WHERE topic = ? and sessionkey = ?";

    protected static final String STATEMENT_REMOVE_ANY_LOCK = "DELETE FROM TopicLock WHERE topic = ?";

    protected static final String STATEMENT_READONLY_INSERT = "INSERT INTO TopicReadOnly( topic ) VALUES ( ? )";

    protected static final String STATEMENT_READONLY_DELETE = "DELETE FROM TopicReadOnly WHERE topic = ?";

    protected static final String STATEMENT_READONLY_ALL = "SELECT topic FROM TopicReadOnly";

    protected static final String STATEMENT_READONLY_FIND = "SELECT COUNT(*) FROM TopicReadOnly WHERE topic = ?";

    protected static Category cat = Category.getInstance(DatabaseHandler.class);

    protected static Connection conn;

    protected static PreparedStatement readStatement, insertStatement, updateStatement;

    protected static PreparedStatement existsStatement, removeAnyLockStatement;

    protected static PreparedStatement setLockStatement, checkLockStatement, removeLockStatement;

    protected static PreparedStatement addReadOnlyStatement, deleteReadOnlyStatement, getReadOnlyStatement;

    protected static PreparedStatement findReadOnlyStatement;

    public DatabaseHandler() throws Exception {
        setDefaults();
    }

    private void setConnection() {
        try {
            Class.forName(Environment.getInstance().getDriver());
            cat.info("New database connection created");
            conn = DriverManager.getConnection(Environment.getInstance().getUrl(), Environment.getInstance().getUserName(), Environment.getInstance().getPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDefaults() throws Exception {
        cat.debug("Setting defaults");
        if (conn != null) if (!conn.isClosed()) conn.close();
        setConnection();
        cat.info("Connected to database @ " + Environment.getInstance().getUrl());
        Statement st = conn.createStatement();
        try {
            cat.debug(TABLE_TOPIC);
            st.execute(TABLE_TOPIC);
        } catch (Exception err) {
            cat.debug("Error on create: " + err);
        }
        try {
            cat.debug(TABLE_CHANGE);
            st.execute(TABLE_CHANGE);
        } catch (Exception err) {
            cat.debug("Error on create: " + err);
        }
        try {
            cat.debug(TABLE_VERSION);
            st.execute(TABLE_VERSION);
        } catch (Exception err) {
            cat.debug("Error on create: " + err);
        }
        try {
            cat.debug(TABLE_LOCK);
            st.execute(TABLE_LOCK);
        } catch (Exception err) {
            cat.debug("Error on create: " + err);
        }
        try {
            cat.debug(TABLE_READ_ONLY);
            st.execute(TABLE_READ_ONLY);
        } catch (Exception err) {
            cat.debug("Error on create: " + err);
        }
        st.close();
        readStatement = conn.prepareStatement(STATEMENT_READ);
        insertStatement = conn.prepareStatement(STATEMENT_INSERT);
        updateStatement = conn.prepareStatement(STATEMENT_UPDATE);
        existsStatement = conn.prepareStatement(STATEMENT_EXISTS);
        setLockStatement = conn.prepareStatement(STATEMENT_SET_LOCK);
        removeAnyLockStatement = conn.prepareStatement(STATEMENT_REMOVE_ANY_LOCK);
        removeLockStatement = conn.prepareStatement(STATEMENT_REMOVE_LOCK);
        checkLockStatement = conn.prepareStatement(STATEMENT_CHECK_LOCK);
        getReadOnlyStatement = conn.prepareStatement(STATEMENT_READONLY_ALL);
        deleteReadOnlyStatement = conn.prepareStatement(STATEMENT_READONLY_DELETE);
        addReadOnlyStatement = conn.prepareStatement(STATEMENT_READONLY_INSERT);
        findReadOnlyStatement = conn.prepareStatement(STATEMENT_READONLY_FIND);
        if (!exists("StartingPoints")) {
            cat.debug("Setting up StartingPoints");
            write(WikiBase.readDefaultTopic("StartingPoints"), true, "StartingPoints");
        }
        if (!exists("TextFormattingRules")) {
            cat.debug("Setting up TextFormattingRules");
            write(WikiBase.readDefaultTopic("TextFormattingRules"), true, "TextFormattingRules");
        }
        if (!exists("FritzTextFormattingRules")) {
            cat.debug("Setting up FritzTextFormattingRules");
            write(WikiBase.readDefaultTopic("FritzTextFormattingRules"), true, "FritzTextFormattingRules");
        }
        if (!exists("RecentChanges")) write("", false, "RecentChanges");
        if (!exists("WikiSearch")) write("", false, "WikiSearch");
        if (!exists("SetUsername")) write("", false, "SetUsername");
    }

    public String read(String topicName) throws Exception {
        readStatement.setString(1, topicName);
        ResultSet rs = readStatement.executeQuery();
        if (!rs.next()) return "This is a new topic";
        return (rs.getString("contents"));
    }

    public void write(String contents, boolean convertTabs, String topicName) throws Exception {
        if (convertTabs) contents = Utilities.convertTabs(contents);
        if (!exists(topicName)) {
            cat.debug("Inserting into topic " + topicName + ", " + contents);
            if (conn == null) setConnection();
            insertStatement.setString(1, topicName);
            insertStatement.setString(2, contents);
            insertStatement.execute();
        } else {
            if (conn == null) setConnection();
            cat.debug("Updating topic " + topicName + " to " + contents);
            updateStatement.setString(2, topicName);
            updateStatement.setString(1, contents);
            updateStatement.execute();
        }
        if (Environment.getInstance().isVersioningOn()) {
            WikiBase.getInstance().getVersionManagerInstance().recordRevision(topicName, contents);
        }
    }

    public boolean exists(String topicName) throws Exception {
        if (conn == null) setConnection();
        existsStatement.setString(1, topicName);
        ResultSet rs = existsStatement.executeQuery();
        rs.next();
        return (rs.getInt(1) == 0 ? false : true);
    }

    public boolean holdsLock(String topicName, String key) throws Exception {
        if (conn == null) setConnection();
        checkLockStatement.setString(1, topicName);
        checkLockStatement.setString(2, key);
        ResultSet rs = checkLockStatement.executeQuery();
        if (!rs.next()) {
            rs.close();
            return false;
        }
        rs.close();
        return true;
    }

    public boolean lockTopic(String topicName, String key) throws Exception {
        if (conn == null) setConnection();
        checkLockStatement.setString(1, topicName);
        checkLockStatement.setString(2, key);
        ResultSet rs = checkLockStatement.executeQuery();
        if (rs.next()) {
            DBDate date = new DBDate(rs.getTimestamp("lockat"));
            DBDate now = new DBDate();
            long fiveMinutesAgo = now.getTime() - 60000 * Environment.getInstance().getEditTimeOut();
            if (date.getTime() < fiveMinutesAgo) {
                cat.debug("Lock expired");
                removeLockStatement.setString(1, topicName);
                removeLockStatement.setString(2, key);
                removeLockStatement.execute();
            } else {
                cat.debug("Already locked");
                rs.close();
                return false;
            }
        }
        if (conn == null) setConnection();
        cat.debug("Setting lock");
        setLockStatement.setString(1, topicName);
        setLockStatement.setString(2, key);
        setLockStatement.setTimestamp(3, (new DBDate()).asTimestamp());
        setLockStatement.execute();
        rs.close();
        return true;
    }

    public void unlockTopic(String topicName) throws Exception {
        removeAnyLockStatement.setString(1, topicName);
        removeAnyLockStatement.execute();
    }

    public boolean isTopicReadOnly(String topicName) throws Exception {
        if (conn == null) setConnection();
        findReadOnlyStatement.setString(1, topicName);
        ResultSet rs = findReadOnlyStatement.executeQuery();
        rs.next();
        if (rs.getInt(1) > 0) {
            rs.close();
            return true;
        }
        rs.close();
        return false;
    }

    public Collection getReadOnlyTopics() throws Exception {
        if (conn == null) setConnection();
        Collection all = new ArrayList();
        ResultSet rs = getReadOnlyStatement.executeQuery();
        while (rs.next()) all.add(rs.getString("topic"));
        rs.close();
        return all;
    }

    public void addReadOnlyTopic(String topicName) throws Exception {
        if (conn == null) setConnection();
        addReadOnlyStatement.setString(1, topicName);
        addReadOnlyStatement.execute();
    }

    public void removeReadOnlyTopic(String topicName) throws Exception {
        if (conn == null) setConnection();
        deleteReadOnlyStatement.setString(1, topicName);
        deleteReadOnlyStatement.execute();
    }

    public void executeSQL(String sql) throws Exception {
        Statement st = conn.createStatement();
        st.execute(sql);
        st.close();
    }

    public void initialise() throws Exception {
        this.setDefaults();
    }
}

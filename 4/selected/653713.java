package vqwiki.persistence.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import vqwiki.Constants;
import vqwiki.Environment;
import vqwiki.TopicLock;
import vqwiki.WikiBase;
import vqwiki.WikiException;
import vqwiki.persistence.PersistenceHandler;
import vqwiki.svc.PseudoTopicHandler;
import vqwiki.svc.VersionManager;
import vqwiki.utils.SystemTime;
import vqwiki.utils.Utilities;

public class DatabaseHandler implements PersistenceHandler {

    private static final String STATEMENT_READ = "SELECT contents FROM Topic WHERE name = ? AND virtualWiki = ?";

    private static final String STATEMENT_UPDATE = "UPDATE Topic SET contents = ? WHERE name = ? AND virtualWiki = ?";

    private static final String STATEMENT_INSERT = "INSERT INTO Topic( name, contents, virtualWiki ) VALUES ( ?, ?, ? )";

    private static final String STATEMENT_EXISTS = "SELECT COUNT(*) FROM Topic WHERE name = ? AND virtualWiki = ?";

    private static final String STATEMENT_RENAME = "UPDATE Topic SET name = ? WHERE name = ? AND virtualWiki = ?";

    private static final String STATEMENT_SET_LOCK = "INSERT INTO TopicLock( topic, sessionkey, lockat, virtualWiki ) VALUES( ?, ?, ?, ? )";

    private static final String STATEMENT_CHECK_LOCK = "SELECT lockat, sessionkey FROM TopicLock WHERE topic = ? AND virtualWiki = ?";

    private static final String STATEMENT_CHECK_SPECIFIC_LOCK = "SELECT lockat, sessionkey FROM TopicLock WHERE topic = ? AND virtualWiki = ? AND sessionkey = ?";

    private static final String STATEMENT_REMOVE_LOCK = "DELETE FROM TopicLock WHERE topic = ? AND virtualWiki = ?";

    private static final String STATEMENT_REMOVE_ANY_LOCK = "DELETE FROM TopicLock WHERE topic = ? AND virtualWiki = ?";

    private static final String STATEMENT_READONLY_INSERT = "INSERT INTO TopicReadOnly( topic, virtualWiki ) VALUES ( ?, ? )";

    private static final String STATEMENT_READONLY_DELETE = "DELETE FROM TopicReadOnly WHERE topic = ? AND virtualWiki = ?";

    private static final String STATEMENT_READONLY_ALL = "SELECT topic FROM TopicReadOnly where virtualWiki = ?";

    private static final String STATEMENT_READONLY_FIND = "SELECT COUNT(*) FROM TopicReadOnly WHERE topic = ? AND virtualWiki = ?";

    private static final String STATEMENT_GET_ALL_VIRTUAL_WIKIS = "SELECT name FROM VirtualWiki";

    private static final String STATEMENT_GET_TEMPLATE_NAMES = "SELECT name FROM WikiTemplate WHERE virtualWiki = ?";

    private static final String STATEMENT_GET_TEMPLATE = "SELECT contents FROM WikiTemplate WHERE virtualWiki = ? AND name = ?";

    private static final String STATEMENT_ADD_VIRTUAL_WIKI = "INSERT INTO VirtualWiki VALUES(?)";

    private static final String STATEMENT_PURGE_TOPIC_DELETES = "DELETE FROM Topic WHERE virtualWiki = ? AND (contents = 'delete\n' or contents = '\n' or contents = '')";

    private static final String STATEMENT_PURGE_TEMPLATE_DELETES = "DELETE FROM WikiTemplate WHERE virtualWiki = ? AND (contents = 'delete\n' or contents = '\n' or contents = '')";

    protected static final String STATEMENT_PURGE_TOPIC = "DELETE FROM Topic WHERE virtualWiki = ? AND name = ?";

    private static final String STATEMENT_TOPICS_TO_PURGE = "SELECT name FROM Topic WHERE virtualWiki = ? AND (contents = 'delete\n' or contents = '\n' or contents = '')";

    private static final String STATEMENT_TEMPLATES_TO_PURGE = "SELECT name FROM WikiTemplate WHERE virtualWiki = ? AND (contents = 'delete\n' or contents = '\n' or contents = '')";

    protected static final String STATEMENT_ALL_OLDER_TOPICS = "SELECT name, contents FROM Topic WHERE virtualWiki = ? AND versionat < ?";

    private static final String STATEMENT_PURGE_VERSIONS = "DELETE FROM TopicVersion WHERE versionat < ? AND virtualWiki = ?";

    private static final String STATEMENT_ADD_TEMPLATE = "INSERT INTO WikiTemplate( virtualWiki, name, contents ) VALUES( ?, ?, ? )";

    private static final String STATEMENT_TEMPLATE_EXISTS = "SELECT COUNT(*) FROM WikiTemplate WHERE virtualWiki = ? AND name = ?";

    private static final String STATEMENT_UPDATE_TEMPLATE = "UPDATE WikiTemplate SET contents = ? WHERE virtualWiki = ? AND name = ?";

    private static final String STATEMENT_GET_LOCK_LIST = "SELECT * FROM TopicLock WHERE virtualWiki = ?";

    private static final Logger logger = Logger.getLogger(DatabaseHandler.class.getName());

    /**
     * Default constructor
     *
     * @throws Exception thrown in case of SQLException
     */
    public DatabaseHandler() {
    }

    /**
     * Setting default values accordingly to the locale
     *
     * @param locale the specified Locale
     * @throws Exception thrown in case of SQLException
     */
    private void setupDefaults(Locale locale) throws Exception {
        logger.fine("Setting defaults");
        ResourceBundle messages = ResourceBundle.getBundle("ApplicationResources", locale);
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(STATEMENT_GET_ALL_VIRTUAL_WIKIS);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                stmt.close();
                stmt = conn.prepareStatement(STATEMENT_ADD_VIRTUAL_WIKI);
                stmt.setString(1, Constants.DEFAULT_VWIKI);
                stmt.execute();
            }
            stmt.close();
            Statement st = conn.createStatement();
            rs = st.executeQuery(STATEMENT_GET_ALL_VIRTUAL_WIKIS);
            while (rs.next()) {
                String virtualWiki = rs.getString("name");
                setupSpecialPage(virtualWiki, messages.getString("specialpages.startingpoints"));
                setupSpecialPage(virtualWiki, messages.getString("specialpages.textformattingrules"));
                setupSpecialPage(virtualWiki, messages.getString("specialpages.leftMenu"));
                setupSpecialPage(virtualWiki, messages.getString("specialpages.topArea"));
                setupSpecialPage(virtualWiki, messages.getString("specialpages.bottomArea"));
                setupSpecialPage(virtualWiki, messages.getString("specialpages.stylesheet"));
                setupSpecialPage(virtualWiki, messages.getString("specialpages.adminonlytopics"));
                setupSpecialPage(virtualWiki, messages.getString("specialpages.quickhelp"));
                setupSpecialPage(virtualWiki, messages.getString("specialpages.wikihelp"));
                setupSpecialPage(virtualWiki, messages.getString("specialpages.wikihelpbasicformatting"));
                setupSpecialPage(virtualWiki, messages.getString("specialpages.wikihelpadvancedformatting"));
                setupSpecialPage(virtualWiki, messages.getString("specialpages.wikihelpmakinglinks"));
                if (!exists(virtualWiki, "SetUsername")) {
                    write(virtualWiki, "", false, "SetUsername");
                }
            }
            rs.close();
            st.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    /**
    *
    */
    private void setupSpecialPage(String vWiki, String specialPage) throws Exception {
        if (!exists(vWiki, specialPage)) {
            logger.fine("Setting up " + specialPage);
            write(vWiki, WikiBase.readDefaultTopic(specialPage), true, specialPage);
        }
    }

    /**
     * Run the create tables script Ignore SQL exceptions as these may just be
     * the result of existing tables getting in the way of create table calls
     *
     * @throws java.lang.Exception
     */
    private void createTables() throws Exception {
        String databaseType = Environment.getInstance().getDatabaseType();
        String resourceName = new StringBuffer().append("create_").append(databaseType).append(".sql").toString();
        InputStream createScriptStream = WikiBase.getInstance().getResourceAsStream(resourceName);
        if (createScriptStream == null) {
            logger.log(Level.SEVERE, "Can't find create script: " + resourceName);
            throw new WikiException("unable to create database, sql script file missing: " + resourceName);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(createScriptStream));
        StringBuffer buffer = new StringBuffer();
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            if (line.length() > 0 && line.charAt(0) != '#') buffer.append(line);
        }
        in.close();
        StringTokenizer tokens = new StringTokenizer(buffer.toString(), ";");
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();
                try {
                    st.executeUpdate(token);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "", e);
                }
            }
            st.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    protected String readText(ResultSet rs, String field) throws SQLException, IOException {
        return rs.getString(field);
    }

    /**
     *
     */
    public String read(String virtualWiki, String topicName) throws Exception {
        if (virtualWiki == null || virtualWiki.length() == 0) {
            virtualWiki = Constants.DEFAULT_VWIKI;
        }
        String contents = null;
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement readStatement = conn.prepareStatement(STATEMENT_READ);
            readStatement.setString(1, topicName);
            readStatement.setString(2, virtualWiki);
            ResultSet rs = readStatement.executeQuery();
            if (!rs.next()) {
                return "This is a new topic";
            }
            contents = readText(rs, "contents");
            rs.close();
            readStatement.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return (contents);
    }

    /**
     *
     */
    public void write(String virtualWiki, String contents, boolean convertTabs, String topicName) throws Exception {
        if (virtualWiki == null || virtualWiki.length() == 0) {
            virtualWiki = Constants.DEFAULT_VWIKI;
        }
        if (convertTabs) {
            contents = Utilities.convertTabs(contents);
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (!exists(virtualWiki, topicName)) {
                logger.fine("Inserting into topic " + topicName + ", " + contents);
                writeInsert(virtualWiki, contents, topicName, conn);
            } else {
                logger.fine("Updating topic " + topicName + " to " + contents);
                writeUpdate(virtualWiki, contents, topicName, conn);
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        if (Environment.getInstance().isVersioningOn()) {
            VersionManager versionManager = WikiBase.getInstance().getVersionManager();
            versionManager.addVersion(virtualWiki, topicName, contents, SystemTime.asDate());
        }
    }

    public void writeUpdate(String virtualWiki, String contents, String topicName, Connection conn) throws Exception {
        PreparedStatement updateStatement = conn.prepareStatement(STATEMENT_UPDATE);
        updateStatement.setString(2, topicName);
        updateStatement.setString(1, contents);
        updateStatement.setString(3, virtualWiki);
        updateStatement.execute();
        updateStatement.close();
    }

    public void writeInsert(String virtualWiki, String contents, String topicName, Connection conn) throws Exception {
        PreparedStatement insertStatement = conn.prepareStatement(STATEMENT_INSERT);
        insertStatement.setString(1, topicName);
        insertStatement.setString(2, contents);
        insertStatement.setString(3, virtualWiki);
        insertStatement.execute();
        insertStatement.close();
    }

    /**
    *
    */
    public boolean exists(String virtualWiki, String topicName) throws Exception {
        if (virtualWiki == null || virtualWiki.length() == 0) {
            virtualWiki = Constants.DEFAULT_VWIKI;
        }
        Connection conn = null;
        boolean result = false;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement existsStatement = conn.prepareStatement(STATEMENT_EXISTS);
            existsStatement.setString(1, topicName);
            existsStatement.setString(2, virtualWiki);
            ResultSet rs = existsStatement.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            result = (count == 0) ? false : true;
            rs.close();
            existsStatement.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return result;
    }

    /**
    *
    */
    public boolean holdsLock(String virtualWiki, String topicName, String key) throws Exception {
        Connection conn = null;
        if (virtualWiki == null || virtualWiki.length() == 0) {
            virtualWiki = Constants.DEFAULT_VWIKI;
        }
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement checkLockStatement = conn.prepareStatement(STATEMENT_CHECK_SPECIFIC_LOCK);
            checkLockStatement.setString(1, topicName);
            checkLockStatement.setString(2, virtualWiki);
            checkLockStatement.setString(3, key);
            ResultSet rs = checkLockStatement.executeQuery();
            if (!rs.next()) {
                rs.close();
                checkLockStatement.close();
                return lockTopic(virtualWiki, topicName, key);
            }
            Date lockedAt = rs.getTimestamp("lockat");
            VersionManager versionManager = WikiBase.getInstance().getVersionManager();
            Date lastRevision = versionManager.lastRevisionDate(virtualWiki, topicName);
            logger.fine("Checking for lock possession: locked at " + lockedAt + " last changed at " + lastRevision);
            if (lastRevision != null) {
                if (lastRevision.after(lockedAt)) {
                    return false;
                }
            }
            rs.close();
            checkLockStatement.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return true;
    }

    /**
    *
    * @param virtualWiki
    * @param topicName
    * @param key
    * @return
    * @throws Exception
    */
    public boolean lockTopic(String virtualWiki, String topicName, String key) throws Exception {
        Connection conn = null;
        if (virtualWiki == null || virtualWiki.length() == 0) {
            virtualWiki = Constants.DEFAULT_VWIKI;
        }
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement checkLockStatement = conn.prepareStatement(STATEMENT_CHECK_LOCK);
            checkLockStatement.setString(1, topicName);
            checkLockStatement.setString(2, virtualWiki);
            ResultSet rs = checkLockStatement.executeQuery();
            if (rs.next()) {
                Date date = rs.getTimestamp("lockat");
                logger.fine("Already locked at " + date);
                long fiveMinutesAgo = SystemTime.asMillis() - 60000 * Environment.getInstance().getEditTimeout();
                if (date.getTime() < fiveMinutesAgo) {
                    logger.fine("Lock expired");
                    PreparedStatement removeLockStatement = conn.prepareStatement(STATEMENT_REMOVE_LOCK);
                    removeLockStatement.setString(1, topicName);
                    removeLockStatement.setString(2, virtualWiki);
                    removeLockStatement.execute();
                    removeLockStatement.close();
                } else {
                    String existingKey = rs.getString("sessionkey");
                    rs.close();
                    checkLockStatement.close();
                    boolean sameKey = existingKey.equals(key);
                    logger.fine("Same key: " + sameKey);
                    return sameKey;
                }
            }
            logger.fine("Setting lock");
            PreparedStatement setLockStatement = conn.prepareStatement(STATEMENT_SET_LOCK);
            setLockStatement.setString(1, topicName);
            setLockStatement.setString(2, key);
            setLockStatement.setTimestamp(3, new Timestamp(SystemTime.asMillis()));
            setLockStatement.setString(4, virtualWiki);
            setLockStatement.execute();
            setLockStatement.close();
            rs.close();
            checkLockStatement.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void unlockTopic(String virtualWiki, String topicName) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            doUnlockTopic(conn, virtualWiki, topicName);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private void doUnlockTopic(Connection conn, String virtualWiki, String topicName) throws SQLException {
        if (virtualWiki == null || virtualWiki.length() == 0) {
            virtualWiki = Constants.DEFAULT_VWIKI;
        }
        PreparedStatement pstm = conn.prepareStatement(STATEMENT_REMOVE_ANY_LOCK);
        try {
            pstm.setString(1, topicName);
            pstm.setString(2, virtualWiki);
            pstm.execute();
        } finally {
            pstm.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTopicReadOnly(String virtualWiki, String topicName) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement findReadOnlyStatement = conn.prepareStatement(STATEMENT_READONLY_FIND);
            findReadOnlyStatement.setString(1, topicName);
            findReadOnlyStatement.setString(2, virtualWiki);
            ResultSet rs = findReadOnlyStatement.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                rs.close();
                findReadOnlyStatement.close();
                return true;
            }
            rs.close();
            findReadOnlyStatement.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return false;
    }

    /**
    * Listing read only topics for a specified virtual wiki
    *
    * @param virtualWiki the virtual wiki
    * @return a collection of String of virtual wiki
    * @throws Exception is a SQLException
    */
    public Collection getReadOnlyTopics(String virtualWiki) throws Exception {
        Collection all = new ArrayList();
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement readOnlyStatement = conn.prepareStatement(STATEMENT_READONLY_ALL);
            readOnlyStatement.setString(1, virtualWiki);
            ResultSet rs = readOnlyStatement.executeQuery();
            while (rs.next()) {
                all.add(rs.getString("topic"));
            }
            rs.close();
            readOnlyStatement.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return all;
    }

    /**
    *
    */
    public void addReadOnlyTopic(String virtualWiki, String topicName) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement addReadOnlyStatement = conn.prepareStatement(STATEMENT_READONLY_INSERT);
            addReadOnlyStatement.setString(1, topicName);
            addReadOnlyStatement.setString(2, virtualWiki);
            addReadOnlyStatement.execute();
            addReadOnlyStatement.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    /**
    *
    */
    public void removeReadOnlyTopic(String virtualWiki, String topicName) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement deleteReadOnlyStatement = conn.prepareStatement(STATEMENT_READONLY_DELETE);
            deleteReadOnlyStatement.setString(1, topicName);
            deleteReadOnlyStatement.setString(2, virtualWiki);
            deleteReadOnlyStatement.execute();
            deleteReadOnlyStatement.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    /**
    *
    */
    public void executeSQL(String sql) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            st.execute(sql);
            st.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    /**
     *
     */
    public void initialise(Locale locale) throws Exception {
        createTables();
        if (locale != null) {
            setupDefaults(locale);
        }
    }

    /**
     *
     */
    public Collection getVirtualWikis() throws Exception {
        Connection conn = null;
        Collection all = new ArrayList();
        try {
            conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(STATEMENT_GET_ALL_VIRTUAL_WIKIS);
            while (rs.next()) {
                all.add(rs.getString("name"));
            }
            rs.close();
            st.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        if (!all.contains(Constants.DEFAULT_VWIKI)) {
            all.add(Constants.DEFAULT_VWIKI);
        }
        return all;
    }

    /**
    *
    */
    public Collection getTemplateNames(String virtualWiki) throws Exception {
        logger.fine("Returning template names for " + virtualWiki);
        Collection all = new ArrayList();
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(STATEMENT_GET_TEMPLATE_NAMES);
            stmt.setString(1, virtualWiki);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                all.add(rs.getString("name"));
            }
            rs.close();
            stmt.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        logger.fine(all.size() + " templates exist");
        return all;
    }

    /**
    *
    */
    public String getTemplate(String virtualWiki, String templateName) throws Exception {
        Connection conn = null;
        String contents;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(STATEMENT_GET_TEMPLATE);
            stmt.setString(1, virtualWiki);
            stmt.setString(2, templateName);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                rs.close();
                stmt.close();
                throw new WikiException("Template not found: " + templateName);
            }
            contents = readText(rs, "contents");
            rs.close();
            stmt.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return contents;
    }

    /**
    *
    */
    public void addVirtualWiki(String virtualWiki) throws Exception {
        if (getVirtualWikis().contains(virtualWiki)) {
            return;
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(STATEMENT_ADD_VIRTUAL_WIKI);
            stmt.setString(1, virtualWiki);
            stmt.execute();
            stmt.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    /**
     *
     */
    public Collection purgeDeletes(String virtualWiki) throws Exception {
        PseudoTopicHandler pseudoTopicHandler = WikiBase.getInstance().getPseudoTopicHandler();
        Collection all = new ArrayList();
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            for (Iterator i = Arrays.asList(new String[] { STATEMENT_TOPICS_TO_PURGE, STATEMENT_TEMPLATES_TO_PURGE }).iterator(); i.hasNext(); ) {
                PreparedStatement stmt = conn.prepareStatement((String) i.next());
                stmt.setString(1, virtualWiki);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String topicName = rs.getString("name");
                    if (!pseudoTopicHandler.isPseudoTopic(topicName)) {
                        all.add(topicName);
                    }
                }
                rs.close();
                stmt.close();
            }
            for (Iterator i = Arrays.asList(new String[] { STATEMENT_PURGE_TOPIC_DELETES, STATEMENT_PURGE_TEMPLATE_DELETES }).iterator(); i.hasNext(); ) {
                PreparedStatement stmt = conn.prepareStatement((String) i.next());
                stmt.setString(1, virtualWiki);
                stmt.execute();
                stmt.close();
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return all;
    }

    /**
     *
     */
    public void purgeVersionsOlderThan(String virtualWiki, Date date) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(STATEMENT_PURGE_VERSIONS);
            stmt.setTimestamp(1, new Timestamp(date.getTime()));
            stmt.setString(2, virtualWiki);
            stmt.execute();
            stmt.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    /**
    *
    */
    public void saveAsTemplate(String virtualWiki, String templateName, String contents) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(STATEMENT_TEMPLATE_EXISTS);
            stmt.setString(1, virtualWiki);
            stmt.setString(2, templateName);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            rs.close();
            stmt.close();
            if (count < 1) {
                saveAsTemplateAdd(virtualWiki, templateName, contents, conn);
            } else {
                saveAsTemplateUpdate(virtualWiki, templateName, contents, conn);
            }
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    protected void saveAsTemplateUpdate(String virtualWiki, String templateName, String contents, Connection conn) throws Exception {
        PreparedStatement stmt = conn.prepareStatement(STATEMENT_UPDATE_TEMPLATE);
        stmt.setString(1, contents);
        stmt.setString(2, virtualWiki);
        stmt.setString(3, templateName);
        stmt.execute();
        stmt.close();
    }

    protected void saveAsTemplateAdd(String virtualWiki, String templateName, String contents, Connection conn) throws Exception {
        PreparedStatement stmt = conn.prepareStatement(STATEMENT_ADD_TEMPLATE);
        stmt.setString(1, virtualWiki);
        stmt.setString(2, templateName);
        stmt.setString(3, contents);
        stmt.execute();
        stmt.close();
    }

    /**
    *
    */
    public List getLockList(String virtualWiki) throws Exception {
        List all = new ArrayList();
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(STATEMENT_GET_LOCK_LIST);
            stmt.setString(1, virtualWiki);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                TopicLock lock = new TopicLock(rs.getString("virtualWiki"), rs.getString("topic"), rs.getTimestamp("lockat"), rs.getString("sessionkey"));
                all.add(lock);
            }
            rs.close();
            stmt.close();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return all;
    }

    /**
     *
     */
    public void rename(String virtualWiki, String oldTopicName, String newTopicName) throws Exception {
        Connection conn = DatabaseConnection.getConnection();
        try {
            boolean commit = false;
            conn.setAutoCommit(false);
            try {
                PreparedStatement pstm = conn.prepareStatement(STATEMENT_RENAME);
                try {
                    pstm.setString(1, newTopicName);
                    pstm.setString(2, oldTopicName);
                    pstm.setString(3, virtualWiki);
                    if (pstm.executeUpdate() == 0) throw new SQLException("Unable to rename topic " + oldTopicName + " on wiki " + virtualWiki);
                } finally {
                    pstm.close();
                }
                doUnlockTopic(conn, virtualWiki, oldTopicName);
                doRenameAllVersions(conn, virtualWiki, oldTopicName, newTopicName);
                commit = true;
            } finally {
                if (commit) conn.commit(); else conn.rollback();
            }
        } finally {
            conn.close();
        }
    }

    private void doRenameAllVersions(Connection conn, String virtualWiki, String oldTopicName, String newTopicName) throws Exception {
        VersionManager versionManager = WikiBase.getInstance().getVersionManager();
        ((DatabaseVersionManager) versionManager).renameAllVersions(conn, virtualWiki, oldTopicName, newTopicName);
    }

    public void destroy() {
        DatabaseConnection.destroy();
    }

    public void cleanup(String virtualWikiName) {
    }
}

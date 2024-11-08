package com.once.server.security;

import org.apache.log4j.Logger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.once.Action;
import com.once.server.config.ConfigManager;
import com.once.server.data.DataAccessException;
import com.once.server.data.source.sql.ISQLResults;
import com.once.server.data.source.sql.SQLResults;
import com.once.server.data.util.SQLUtils;
import com.once.server.jdb.DataAccessor;

class SecurityManager extends DataAccessor implements ISecurityManager {

    private static final Logger m_logger = Logger.getLogger(SecurityManager.class);

    private static final String VALUE_ORGANIZATION = "Public";

    private static final String VALUE_TYPE = "Branch";

    public boolean canBlockDelete(int userId, String block) throws DataAccessException {
        return check(userId, block, ACTION_DELETE, OBJECT_TYPE_BLOCK);
    }

    public boolean canBlockRead(int userId, String block) throws DataAccessException {
        return check(userId, block, ACTION_READ, OBJECT_TYPE_BLOCK);
    }

    public boolean canBlockWrite(int userId, String block) throws DataAccessException {
        return check(userId, block, ACTION_WRITE, OBJECT_TYPE_BLOCK);
    }

    public boolean canTableDelete(int userId, String table) throws DataAccessException {
        return check(userId, table, ACTION_DELETE, OBJECT_TYPE_TABLE);
    }

    public boolean canTableRead(int userId, String table) throws DataAccessException {
        return check(userId, table, ACTION_READ, OBJECT_TYPE_TABLE);
    }

    public boolean canTableWrite(int userId, String table) throws DataAccessException {
        return check(userId, table, ACTION_WRITE, OBJECT_TYPE_TABLE);
    }

    public boolean canUseApplication(int userId, String application) throws DataAccessException {
        resetStartTime();
        ConfigManager config = ConfigManager.getInstance();
        String localSQL;
        Connection conn = null;
        try {
            conn = getConnection();
            Statement st = getStatement(conn);
            localSQL = "SELECT " + config.getDefaultPasswordsTableName() + ".\"" + application + "\" FROM " + config.getDefaultPasswordsTableName() + " WHERE " + config.getDefaultPasswordsTableName() + ".\"primary\"=" + userId + ";";
            m_lastSQL = localSQL;
            if (m_logger.isDebugEnabled()) {
                m_logger.debug(m_lastSQL);
            }
            ResultSet res = executeQueryAsync(st, localSQL);
            boolean allow = false;
            while (res.next()) {
                allow = res.getBoolean(1);
            }
            return allow;
        } catch (Exception ex) {
            m_logger.error(ex);
            throw new DataAccessException("Unable to check application permissions", ex);
        } finally {
            closeConnection(conn);
        }
    }

    public void restrictTableDelete(int userId, String table) throws DataAccessException {
        restrict(userId, table, ACTION_DELETE, OBJECT_TYPE_TABLE);
    }

    public void restrictTableRead(int userId, String table) throws DataAccessException {
        restrict(userId, table, ACTION_READ, OBJECT_TYPE_TABLE);
    }

    public void restrictTableWrite(int userId, String table) throws DataAccessException {
        restrict(userId, table, ACTION_WRITE, OBJECT_TYPE_TABLE);
    }

    public String getTableReadRestriction(int userId, String table, String model) throws DataAccessException {
        return (getTableRestriction(userId, table, ACTION_READ, model));
    }

    private String getOwnerOrganisation(int userId) throws DataAccessException {
        String ownerOrganisation;
        Connection connection;
        ResultSet results;
        String tableMembership;
        tableMembership = ConfigManager.getInstance().getDefaultMembershipTableName();
        connection = null;
        ownerOrganisation = null;
        try {
            connection = getConnection();
            results = executeQueryAsync(getStatement(connection), "SELECT " + tableMembership + ".\"ownerorganisation\" FROM " + tableMembership + " WHERE " + tableMembership + ".\"primary\" = " + userId);
            if (results.next() == true) ownerOrganisation = results.getString("ownerorganisation");
        } catch (Exception except) {
            m_logger.error(except, except);
            throw new DataAccessException("Unable to get owner organisation.", except);
        } finally {
            closeConnection(connection);
        }
        return (ownerOrganisation);
    }

    public String applyTableRestriction(String wherePart, String tableName, int userId, String action, String model) throws DataAccessException {
        String restriction;
        restriction = getTableRestriction(userId, tableName, action, model);
        if (restriction != null && restriction.length() > 0) {
            if (wherePart == null) wherePart = new String(); else if (wherePart.length() > 0) wherePart = "(" + wherePart + ") AND ";
            wherePart += restriction;
        }
        return (wherePart);
    }

    public String getTableRestriction(int userId, String tableName, String action, String modelName) throws DataAccessException {
        Connection connection;
        ConfigManager settings;
        ResultSet results;
        String localSQL;
        String tableGroup;
        String tablePermission;
        String tablePermissionGroup;
        String tableRestriction;
        String tableMembership;
        String tablePerson;
        String valueContact;
        Matcher match;
        StringBuffer buffer;
        Statement state;
        String eachRestriction;
        resetStartTime();
        settings = ConfigManager.getInstance();
        tableGroup = settings.getDefaultGroupTableName();
        tablePermission = settings.getDefaultTablePermissionsTableName();
        tablePermissionGroup = settings.getDefaultGroupPermissionsTableName();
        localSQL = "SELECT " + tablePermission + ".\"" + action + "restrictions\" FROM " + tablePermission + " INNER JOIN " + tableGroup + " ON (" + tableGroup + ".\"primary\" = " + tablePermission + ".\"fkgroup\") INNER JOIN " + tablePermissionGroup + " ON (" + tablePermissionGroup + ".\"fkgroup\" = " + tableGroup + ".\"primary\") WHERE " + tablePermissionGroup + ".\"fkpersonmember\" = " + userId + " AND " + tablePermission + ".\"table\" = '" + tableName + "' AND " + tablePermission + ".\"ownerorganisation\" = '" + getOwnerOrganisation(userId) + "';";
        tableRestriction = null;
        connection = null;
        try {
            connection = getConnection();
            state = getStatement(connection);
            results = executeQueryAsync(state, localSQL);
            tableRestriction = null;
            while (results.next() == true) {
                eachRestriction = results.getString(action + "restrictions");
                if (eachRestriction != null && eachRestriction.length() > 0) {
                    if (tableRestriction != null) {
                        tableRestriction = "(" + tableRestriction + ") AND " + eachRestriction;
                    } else tableRestriction = eachRestriction;
                } else {
                    tableRestriction = null;
                    break;
                }
            }
            if (tableRestriction != null) {
                tableMembership = ConfigManager.getInstance().getDefaultMembershipTableName();
                tablePerson = "\"contacts\".\"person\"";
                valueContact = null;
                match = Pattern.compile("(\\$(V|F)\\{([^}]*?)\\})", Pattern.DOTALL).matcher(tableRestriction);
                modelName = Action.quoteAliases(modelName);
                buffer = new StringBuffer();
                while (match.find() == true) {
                    if ("F".equals(match.group(2)) == true) {
                        match.appendReplacement(buffer, (("write".equals(action) == false && "modify".equals(action) == false && "delete".equals(action) == false) ? modelName + "." : "") + Action.quoteAliases(match.group(3)));
                    } else {
                        if ("contact".equals(match.group(3)) == true) {
                            if (valueContact == null) {
                                localSQL = "SELECT " + tablePerson + ".\"contact\" FROM " + tablePerson + " INNER JOIN " + tableMembership + " ON (" + tablePerson + ".\"primary\" = " + tableMembership + ".\"fkperson\") WHERE " + tableMembership + ".\"primary\" = " + userId + ";";
                                results = executeQueryAsync(state, localSQL);
                                match.appendReplacement(buffer, results.next() == true ? (valueContact = results.getString("contact")) : "");
                            } else match.appendReplacement(buffer, valueContact);
                        } else match.appendReplacement(buffer, "");
                    }
                }
                match.appendTail(buffer);
                tableRestriction = "(" + buffer.toString() + ")";
            }
        } catch (Exception except) {
            m_logger.error(except, except);
            throw new DataAccessException("Unable to get table " + action + " restrictions for \"" + tableName + "\".", except);
        } finally {
            closeConnection(connection);
        }
        return (tableRestriction);
    }

    private boolean check(int userId, String objectName, String action, String objectType) throws DataAccessException {
        boolean allowed = false;
        Connection connection;
        ConfigManager settings;
        ResultSet results;
        Statement st;
        String tableGroup;
        String tableMembership;
        String tablePermission;
        String tablePermissionGroup;
        String ownerOrganisation;
        String localSQL;
        resetStartTime();
        settings = ConfigManager.getInstance();
        tableGroup = settings.getDefaultGroupTableName();
        tableMembership = settings.getDefaultMembershipTableName();
        tablePermission = objectType.equals(OBJECT_TYPE_BLOCK) ? settings.getDefaultBlockPermissionsTableName() : settings.getDefaultTablePermissionsTableName();
        tablePermissionGroup = settings.getDefaultGroupPermissionsTableName();
        localSQL = "SELECT " + tableMembership + ".\"ownerorganisation\" FROM " + tableMembership + " WHERE " + tableMembership + ".\"primary\" = " + userId;
        m_lastSQL = localSQL;
        if (m_logger.isDebugEnabled()) {
            m_logger.debug(m_lastSQL);
        }
        connection = null;
        ownerOrganisation = null;
        try {
            connection = getConnection();
            st = getStatement(connection);
            results = executeQueryAsync(st, localSQL);
            while (results.next() == true) {
                ownerOrganisation = results.getString("ownerorganisation");
            }
            localSQL = "SELECT " + tablePermission + ".\"read\", " + tablePermission + ".\"write\", " + tablePermission + ".\"delete\" FROM " + tablePermission + ", " + tablePermissionGroup + ", " + tableGroup + " WHERE " + tablePermission + ".\"fkgroup\" = " + tableGroup + ".\"primary\" AND " + tablePermissionGroup + ".\"fkgroup\" = " + tableGroup + ".\"primary\" AND " + tablePermissionGroup + ".\"fkpersonmember\" = " + userId + " AND " + tablePermission + ".\"" + objectType + "\" = '" + objectName + "' AND " + tablePermission + ".\"ownerorganisation\" = '" + ownerOrganisation + "';";
            m_lastSQL = localSQL;
            if (m_logger.isDebugEnabled()) {
                m_logger.debug(m_lastSQL);
            }
            results = executeQueryAsync(st, localSQL);
            while (results.next() == true) {
                allowed |= results.getBoolean(action);
            }
        } catch (Exception except) {
            m_logger.error(except, except);
            throw new DataAccessException("Unable to check security permissions", except);
        } finally {
            closeConnection(connection);
        }
        return allowed;
    }

    private void restrict(int userId, String objectName, String action, String objectType) throws DataAccessException {
        resetStartTime();
        String localSQL;
        ConfigManager config = ConfigManager.getInstance();
        String tableName = objectType.equals(OBJECT_TYPE_BLOCK) ? config.getDefaultBlockPermissionsTableName() : config.getDefaultTablePermissionsTableName();
        localSQL = "UPDATE " + tableName + " SET \"" + action + "\"=false WHERE " + tableName + ".\"fkgroup\"=security.group.\"primary\" AND " + config.getDefaultGroupPermissionsTableName() + ".\"fkgroup\"=" + config.getDefaultGroupTableName() + ".\"primary\" AND " + config.getDefaultGroupPermissionsTableName() + ".\"fkpersonmember\"=" + userId + " AND " + tableName + ".\"" + objectType + "\"='" + objectName + "';";
        m_lastSQL = localSQL;
        Connection conn = null;
        try {
            conn = getConnection();
            Statement st = getStatement(conn);
            if (m_logger.isDebugEnabled()) {
                m_logger.debug(m_lastSQL);
            }
            executeUpdateAsync(st, localSQL);
        } catch (Exception ex) {
            m_logger.error(ex);
            throw new DataAccessException("Unable to set security permissions", ex);
        } finally {
            closeConnection(conn);
        }
    }

    public void setUserPreferences(int userId, Map<String, String> userPrefs) throws DataAccessException {
        resetStartTime();
        String localSQL;
        ConfigManager config = ConfigManager.getInstance();
        String tableName = config.getDefaultPasswordsTableName();
        if (userPrefs != null && userPrefs.size() > 0) {
            String sql = "";
            for (Iterator<String> i = userPrefs.keySet().iterator(); i.hasNext(); ) {
                String prefName = i.next();
                if (USER_PREFERENCES_MARQUE_EACH.equalsIgnoreCase(prefName)) {
                    sql = sql + "\"marqueeeach\"=" + String.valueOf(userPrefs.get(prefName));
                } else if (USER_PREFERENCES_MARQUE_WHOLE.equalsIgnoreCase(prefName)) {
                    sql = sql + "\"marqueewhole\"=" + String.valueOf(userPrefs.get(prefName));
                } else if (USER_PREFERENCES_PAGE_BORDER.equalsIgnoreCase(prefName)) {
                    sql = sql + "\"pageborder\"=" + String.valueOf(userPrefs.get(prefName));
                } else if (USER_PREFERENCES_AUTO_COMMIT.equalsIgnoreCase(prefName)) {
                    sql = sql + "\"autocommit\"=" + String.valueOf(userPrefs.get(prefName));
                } else if (USER_PREFERENCES_MENU_BAR_X.equalsIgnoreCase(prefName)) {
                    sql = sql + "\"menubarx\"=" + String.valueOf(userPrefs.get(prefName));
                } else if (USER_PREFERENCES_INSERT_RETURN_STYLE.equalsIgnoreCase(prefName)) {
                    sql = sql + "\"insertreturnstyle\"=" + String.valueOf(userPrefs.get(prefName));
                } else if (USER_PREFERENCES_LARGE_SUBBLOCK_ICONS.equalsIgnoreCase(prefName)) {
                    sql = sql + "\"largesubblockicons\"=" + String.valueOf(userPrefs.get(prefName));
                } else if (USER_PREFERENCES_WILDCARD_SEARCH.equalsIgnoreCase(prefName)) {
                    sql = sql + "\"wildcardsearch\"=" + String.valueOf(userPrefs.get(prefName));
                }
                if (i.hasNext()) {
                    sql = sql + ", ";
                }
            }
            if (sql.length() > 0) {
                localSQL = "UPDATE " + tableName + " SET " + sql + " WHERE " + tableName + ".\"primary\" = " + String.valueOf(userId) + ";";
            } else localSQL = "";
            m_lastSQL = localSQL;
            if (m_logger.isDebugEnabled()) {
                m_logger.debug(m_lastSQL);
            }
            Connection conn = null;
            try {
                conn = getConnection();
                Statement st = getStatement(conn);
                if (m_logger.isDebugEnabled()) {
                    m_logger.debug(m_lastSQL);
                }
                executeUpdateAsync(st, localSQL);
            } catch (Exception ex) {
                m_logger.error(ex);
                throw new DataAccessException("Unable to set user preferences", ex);
            } finally {
                closeConnection(conn);
            }
        }
    }

    public Map<String, String> getUserPreferences(int userId) throws DataAccessException {
        resetStartTime();
        ConfigManager config = ConfigManager.getInstance();
        String tableName = config.getDefaultPasswordsTableName();
        String localSQL;
        localSQL = "SELECT " + tableName + ".\"marqueeeach\", " + tableName + ".\"marqueewhole\", " + tableName + ".\"pageborder\", " + tableName + ".\"autocommit\", " + tableName + ".\"menubarx\", " + tableName + ".\"insertreturnstyle\", " + tableName + ".\"largesubblockicons\", " + tableName + ".\"wildcardsearch\"" + " FROM " + tableName + " WHERE " + tableName + ".\"primary\"=" + String.valueOf(userId) + ";";
        m_lastSQL = localSQL;
        Connection conn = null;
        try {
            conn = getConnection();
            Statement st = getStatement(conn);
            if (m_logger.isDebugEnabled()) {
                m_logger.debug(m_lastSQL);
            }
            ResultSet res = executeQueryAsync(st, localSQL);
            if (res.next()) {
                Map<String, String> userPrefs = new HashMap<String, String>();
                userPrefs.put(USER_PREFERENCES_MARQUE_EACH, res.getString("marqueeeach"));
                userPrefs.put(USER_PREFERENCES_MARQUE_WHOLE, res.getString("marqueewhole"));
                userPrefs.put(USER_PREFERENCES_PAGE_BORDER, res.getString("pageborder"));
                userPrefs.put(USER_PREFERENCES_AUTO_COMMIT, res.getString("autocommit"));
                userPrefs.put(USER_PREFERENCES_MENU_BAR_X, res.getString("menubarx"));
                userPrefs.put(USER_PREFERENCES_INSERT_RETURN_STYLE, res.getString("insertreturnstyle"));
                userPrefs.put(USER_PREFERENCES_LARGE_SUBBLOCK_ICONS, res.getString("largesubblockicons"));
                userPrefs.put(USER_PREFERENCES_WILDCARD_SEARCH, res.getString("wildcardsearch"));
                return userPrefs;
            } else {
                return null;
            }
        } catch (Exception ex) {
            m_logger.error(ex);
            throw new DataAccessException("Unable to get user preferences" + ex.getMessage(), ex);
        } finally {
            closeConnection(conn);
        }
    }

    public int createUser(String userName, String passwordHash, String ownerOrganization, String title, String firstName, String lastName, String middleName) throws DataAccessException, AccessDeniedException {
        ConfigManager configuration;
        int primaryGroupUser;
        if (userName == null || userName.length() < 1) {
            throw new DataAccessException("User name is not specified");
        }
        if (passwordHash == null || passwordHash.length() < 1) {
            throw new DataAccessException("Password is not specified");
        }
        if (ownerOrganization == null || ownerOrganization.length() < 1) {
            throw new DataAccessException("User organization is not specified");
        }
        if (title == null) {
            title = "";
        }
        if (firstName == null || firstName.length() < 1) {
            throw new DataAccessException("User first name is not specified");
        }
        if (lastName == null || lastName.length() < 1) {
            throw new DataAccessException("User last name is not specified");
        }
        if (middleName == null) {
            middleName = "";
        }
        if (isUsernameUsed(userName)) {
            throw new DataAccessException("User name " + userName + " from " + ownerOrganization + " is already used");
        }
        Connection conn = null;
        String sql = "";
        configuration = ConfigManager.getInstance();
        try {
            conn = getConnection();
            int primaryPerson = SQLUtils.getNextPrimary("contacts.person", 1).get(0).intValue();
            Statement st = getStatement(conn);
            sql = "INSERT INTO \"contacts\".\"person\" (\"primary\", \"firstname\", \"lastname\", \"middlename\", \"ownerorganisation\", \"title\") VALUES (" + primaryPerson + ", '" + firstName + "', '" + lastName + "', '" + middleName + "', '" + ownerOrganization + "', '" + title + "');";
            executeUpdateAsync(st, sql);
            conn.commit();
            sql = "INSERT INTO \"security\".\"usersecurity\" (\"client\", \"fkperson\", \"ownerorganisation\", \"password\", \"username\") VALUES (True, " + primaryPerson + ", '" + ownerOrganization + "', '" + passwordHash + "', '" + userName + "');";
            executeUpdateAsync(st, sql);
            conn.commit();
            sql = "SELECT \"contacts\".\"levelstructure\".\"primary\" FROM \"contacts\"." + "\"levelstructure\" INNER JOIN \"contacts\".\"organisation\" ON (\"contacts\".\"levelstructure\".\"fkorganisation\" = \"contacts\".\"organisation\".\"primary\") WHERE \"contacts\".\"organisation\".\"organisation" + "\" = '" + VALUE_ORGANIZATION + "' AND \"contacts\".\"organisation\".\"" + "ownerorganisation\" = '" + ownerOrganization + "'";
            ResultSet res = executeQueryAsync(st, sql);
            int primaryLevel = -1;
            if (res.next()) {
                primaryLevel = res.getInt("primary");
            }
            if (primaryLevel == -1) {
                primaryLevel = getNewPrimaryLevel(ownerOrganization, conn);
            }
            int primaryPersonMember = SQLUtils.getNextPrimary("contacts.personmember", 1).get(0).intValue();
            sql = "INSERT INTO \"contacts\".\"personmember\" (\"primary\", \"fkperson\", \"fklevel\", " + "\"ownerorganisation\") VALUES (" + primaryPersonMember + ", " + primaryPerson + ", " + primaryLevel + ", '" + ownerOrganization + "');";
            executeUpdateAsync(st, sql);
            conn.commit();
            if (configuration.useIndividualGroups() == true) {
                primaryGroupUser = SQLUtils.getNextPrimary("security.group", 1).get(0).intValue();
                sql = "INSERT INTO \"security\".\"group\" (\"primary\", \"groupname\", \"ownerorganisati" + "on\") VALUES (" + primaryGroupUser + ", '" + userName + "', '" + ownerOrganization + "');";
                executeUpdateAsync(st, sql);
                conn.commit();
            } else primaryGroupUser = -1;
            sql = "SELECT \"security\".\"group\".\"primary\" FROM \"security\".\"group\" " + "WHERE \"groupname\" = '" + VALUE_ORGANIZATION + "' AND \"ownerorganisation\" = '" + ownerOrganization + "';";
            res = executeQueryAsync(st, sql);
            int primaryGroupPublic = -1;
            if (res.next()) {
                primaryGroupPublic = res.getInt("primary");
            }
            if (primaryGroupPublic == -1) {
                primaryGroupPublic = getNewPrimaryGroupPublic(ownerOrganization, conn);
            }
            if (configuration.useIndividualGroups() == true) {
                sql = "INSERT INTO \"security\".\"personmembergroup\" (\"fkpersonmember\", \"fkgroup\", \"ownerorganisation\") VALUES (" + primaryPersonMember + ", " + primaryGroupUser + ", '" + ownerOrganization + "');";
                executeUpdateAsync(st, sql);
            }
            conn.commit();
            sql = "INSERT INTO \"security\".\"personmembergroup\" (\"fkpersonmember\", \"fkgroup\", \"ownerorganisation\") VALUES (" + primaryPersonMember + ", " + primaryGroupPublic + ", '" + ownerOrganization + "');";
            executeUpdateAsync(st, sql);
            conn.commit();
            sql = "SELECT \"security\".\"usersecurity\".\"primary\" FROM \"security\".\"usersecurity\" WHERE \"username\"='" + userName + "' AND \"ownerorganisation\"='" + ownerOrganization + "';";
            res = executeQueryAsync(st, sql);
            int primaryUserSecurity = -1;
            if (res.next()) {
                primaryUserSecurity = res.getInt("primary");
            }
            conn.commit();
            return primaryUserSecurity;
        } catch (Throwable t) {
            m_logger.error(t, t);
            rollbackConnection(conn);
            throw new DataAccessException("Unable to create user", t);
        } finally {
            closeConnection(conn);
        }
    }

    public void updateUser(int userId, String passwordHash, String title, String firstName, String lastName, String middleName) throws DataAccessException, AccessDeniedException {
        throw new UnsupportedOperationException();
    }

    public boolean isUsernameUsed(String userName) throws DataAccessException {
        Connection conn;
        ResultSet res;
        Statement st;
        String sql;
        boolean used;
        conn = null;
        used = false;
        try {
            conn = getConnection();
            st = getStatement(conn);
            sql = "SELECT \"security\".\"usersecurity\".\"username\" FROM \"security\".\"usersecurity\" WHERE \"username\"='" + userName + "';";
            res = executeQueryAsync(st, sql);
            used = res.next();
        } catch (Throwable t) {
            m_logger.error(t, t);
            rollbackConnection(conn);
            throw (new DataAccessException("Unable to check is user name available", t));
        } finally {
            closeConnection(conn);
        }
        return (used);
    }

    private int getNewPrimaryGroupPublic(String ownerOrganization, Connection conn) throws DataAccessException {
        try {
            int primary = SQLUtils.getNextPrimary("security.group", 1).get(0).intValue();
            Statement st = getStatement(conn);
            String sql = "INSERT INTO \"security\".\"group\" (\"primary\", \"groupname\", \"ownerorganisation" + "\") VALUES (" + primary + ", '" + VALUE_ORGANIZATION + "', '" + ownerOrganization + "');";
            executeUpdateAsync(st, sql);
            return primary;
        } catch (Throwable t) {
            m_logger.error(t);
            rollbackConnection(conn);
            throw new DataAccessException("Unable to insert new Owner Organisation record", t);
        }
    }

    private int getNewPrimaryLevel(String ownerOrganization, Connection conn) throws DataAccessException {
        try {
            int primaryOrganization = SQLUtils.getNextPrimary("contacts.organisation", 1).get(0).intValue();
            Statement st = getStatement(conn);
            String sql = "INSERT INTO \"contacts\".\"organisation\" (\"primary\", \"organisation\", \"ownerorganisation\") VALUES (" + primaryOrganization + ", '" + VALUE_ORGANIZATION + "', '" + ownerOrganization + "');";
            executeUpdateAsync(st, sql);
            int primaryOrganisationalLevel = SQLUtils.getNextPrimary("contacts.organisationallevel", 1).get(0).intValue();
            sql = "INSERT INTO \"contacts\".\"organisationallevel\" (\"primary\", \"type\", \"name\", " + "\"ownerorganisation\", \"fkorganisation\") VALUES (" + primaryOrganisationalLevel + ", '" + VALUE_TYPE + "', '" + VALUE_ORGANIZATION + "', '" + ownerOrganization + "', " + primaryOrganization + ");";
            executeUpdateAsync(st, sql);
            int primaryLevelStucture = SQLUtils.getNextPrimary("contacts.levelstructure", 1).get(0).intValue();
            sql = "INSERT INTO \"contacts\".\"levelstructure\" (\"primary\", \"fkorganisation\", \"fkbranch\", \"ownerorganisation\") VALUES (" + primaryLevelStucture + ", " + primaryOrganization + ", " + primaryOrganisationalLevel + ", '" + ownerOrganization + "');";
            executeUpdateAsync(st, sql);
            return primaryLevelStucture;
        } catch (Throwable t) {
            m_logger.error(t);
            rollbackConnection(conn);
            throw new DataAccessException("Unable to insert new Organisational level record", t);
        }
    }

    public ISQLResults getUserInfo(int userId) throws DataAccessException, AccessDeniedException {
        Connection conn = null;
        String sql = "";
        try {
            conn = getConnection();
            Statement st = getStatement(conn);
            sql = "SELECT \"username\", \"title\", \"firstname\", \"lastname\", \"middlename\", \"dob\", \"contacts\".\"person\".\"ownerorganisation\", \"jobposition\" " + "FROM \"contacts\".\"person\", \"security\".\"usersecurity\" " + "WHERE \"contacts\".\"person\".\"primary\"=\"security\".\"usersecurity\".\"fkperson\" " + "AND \"security\".\"usersecurity\".\"primary\"=" + userId + ";";
            ResultSet res = executeQueryAsync(st, sql);
            return new SQLResults(res);
        } catch (Throwable t) {
            m_logger.error(t, t);
            rollbackConnection(conn);
            throw new DataAccessException("Unable to get user info", t);
        } finally {
            closeConnection(conn);
        }
    }
}

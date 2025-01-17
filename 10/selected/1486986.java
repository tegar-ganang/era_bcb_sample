package cz.fi.muni.xkremser.editor.server.DAO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import cz.fi.muni.xkremser.editor.client.util.ClientUtils;
import cz.fi.muni.xkremser.editor.client.util.Constants;
import cz.fi.muni.xkremser.editor.server.exception.DatabaseException;
import cz.fi.muni.xkremser.editor.shared.domain.DigitalObjectModel;
import cz.fi.muni.xkremser.editor.shared.rpc.RecentlyModifiedItem;

/**
 * The Class RecentlyModifiedItemDAOImpl.
 */
public class RecentlyModifiedItemDAOImpl extends AbstractDAO implements RecentlyModifiedItemDAO {

    /** The Constant SELECT_LAST_N_STATEMENT. */
    public static final String SELECT_LAST_N_STATEMENT = "SELECT uuid, name, MAX(description) AS description, model, MAX(modified) AS modified FROM " + Constants.TABLE_RECENTLY_MODIFIED_NAME + " GROUP BY uuid, name, model ORDER by modified DESC LIMIT(?)";

    /** The Constant SELECT_LAST_N_STATEMENT_FOR_USER. */
    public static final String SELECT_LAST_N_STATEMENT_FOR_USER = "SELECT uuid, name, description, model, modified FROM " + Constants.TABLE_RECENTLY_MODIFIED_NAME + " WHERE user_id IN (SELECT user_id FROM " + Constants.TABLE_OPEN_ID_IDENTITY + " WHERE identity = (?)) ORDER BY modified DESC LIMIT (?)";

    /** The Constant INSERT_ITEM_STATEMENT. */
    public static final String INSERT_ITEM_STATEMENT = "INSERT INTO " + Constants.TABLE_RECENTLY_MODIFIED_NAME + " (uuid, name, description, model, user_id, modified) VALUES ((?),(?),(?),(?),(SELECT user_id FROM " + Constants.TABLE_OPEN_ID_IDENTITY + " WHERE identity = (?)),(CURRENT_TIMESTAMP))";

    /** The Constant FIND_ITEM_STATEMENT. */
    public static final String FIND_ITEM_STATEMENT = "SELECT id FROM " + Constants.TABLE_RECENTLY_MODIFIED_NAME + " WHERE uuid = (?) AND user_id IN (SELECT user_id FROM " + Constants.TABLE_OPEN_ID_IDENTITY + " WHERE identity = (?))";

    /** The Constant INSERT_COMMON_DESCRIPTION_STATEMENT. */
    public static final String INSERT_COMMON_DESCRIPTION_STATEMENT = "INSERT INTO " + Constants.TABLE_DESCRIPTION + " (description, uuid) VALUES ((?),(?))";

    /** The Constant UPDATE_COMMON_DESCRIPTION_STATEMENT. */
    public static final String UPDATE_COMMON_DESCRIPTION_STATEMENT = "UPDATE " + Constants.TABLE_DESCRIPTION + " SET description = (?) WHERE uuid = (?)";

    /** The Constant SELECT_COMMON_DESCRIPTION_STATEMENT. */
    public static final String SELECT_COMMON_DESCRIPTION_STATEMENT = "SELECT description FROM " + Constants.TABLE_DESCRIPTION + " WHERE uuid = (?)";

    /** The Constant UPDATE_USER_DESCRIPTION_STATEMENT. */
    public static final String UPDATE_USER_DESCRIPTION_STATEMENT = "UPDATE " + Constants.TABLE_RECENTLY_MODIFIED_NAME + " SET description = (?) WHERE uuid = (?) AND user_id IN (SELECT user_id FROM " + Constants.TABLE_OPEN_ID_IDENTITY + " WHERE identity = (?))";

    /** The Constant SELECT_USER_DESCRIPTION_STATEMENT. */
    public static final String SELECT_USER_DESCRIPTION_STATEMENT = "SELECT description, modified FROM " + Constants.TABLE_RECENTLY_MODIFIED_NAME + " WHERE uuid = (?) AND user_id IN (SELECT user_id FROM " + Constants.TABLE_OPEN_ID_IDENTITY + " WHERE identity = (?))";

    /** The Constant UPDATE_ITEM_STATEMENT. */
    public static final String UPDATE_ITEM_STATEMENT = "UPDATE " + Constants.TABLE_RECENTLY_MODIFIED_NAME + " SET modified = CURRENT_TIMESTAMP WHERE id = (?)";

    /** The Constant DELETE_REMOVED_ITEM */
    private static final String DELETE_REMOVED_ITEM = "DELETE FROM " + Constants.TABLE_RECENTLY_MODIFIED_NAME + " WHERE uuid = (?)";

    private static final Logger LOGGER = Logger.getLogger(RecentlyModifiedItemDAOImpl.class);

    @Override
    public boolean put(RecentlyModifiedItem toPut, String openID) throws DatabaseException {
        if (toPut == null) throw new NullPointerException("toPut");
        if (toPut.getUuid() == null || "".equals(toPut.getUuid())) throw new NullPointerException("toPut.getUuid()");
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            LOGGER.warn("Unable to set autocommit off", e);
        }
        boolean found = true;
        try {
            PreparedStatement findSt = getConnection().prepareStatement(FIND_ITEM_STATEMENT);
            PreparedStatement statement = null;
            findSt.setString(1, toPut.getUuid());
            findSt.setString(2, openID);
            ResultSet rs = findSt.executeQuery();
            found = rs.next();
            int modified = 0;
            if (found) {
                int id = rs.getInt(1);
                statement = getConnection().prepareStatement(UPDATE_ITEM_STATEMENT);
                statement.setInt(1, id);
                modified = statement.executeUpdate();
            } else {
                statement = getConnection().prepareStatement(INSERT_ITEM_STATEMENT);
                statement.setString(1, toPut.getUuid());
                statement.setString(2, toPut.getName() == null ? "" : ClientUtils.trimLabel(toPut.getName(), Constants.MAX_LABEL_LENGTH));
                statement.setString(3, toPut.getDescription() == null ? "" : toPut.getDescription());
                statement.setInt(4, toPut.getModel().ordinal());
                statement.setString(5, openID);
                modified = statement.executeUpdate();
            }
            if (modified == 1) {
                getConnection().commit();
                LOGGER.debug("DB has been updated. Queries: \"" + findSt + "\" and \"" + statement + "\"");
            } else {
                getConnection().rollback();
                LOGGER.error("DB has not been updated -> rollback! Queries: \"" + findSt + "\" and \"" + statement + "\"");
                found = false;
            }
        } catch (SQLException e) {
            LOGGER.error(e);
            found = false;
        } finally {
            closeConnection();
        }
        return found;
    }

    @Override
    public ArrayList<RecentlyModifiedItem> getItems(int nLatest, String openID) throws DatabaseException {
        PreparedStatement selectSt = null;
        ArrayList<RecentlyModifiedItem> retList = new ArrayList<RecentlyModifiedItem>();
        try {
            if (openID != null) {
                selectSt = getConnection().prepareStatement(SELECT_LAST_N_STATEMENT_FOR_USER);
                selectSt.setString(1, openID);
                selectSt.setInt(2, nLatest);
            } else {
                selectSt = getConnection().prepareStatement(SELECT_LAST_N_STATEMENT);
                selectSt.setInt(1, nLatest);
            }
        } catch (SQLException e) {
            LOGGER.error("Could not get select items statement", e);
        }
        try {
            ResultSet rs = selectSt.executeQuery();
            while (rs.next()) {
                int modelId = rs.getInt("model");
                retList.add(new RecentlyModifiedItem(rs.getString("uuid"), rs.getString("name"), openID != null ? rs.getString("description") : "", DigitalObjectModel.values()[modelId], rs.getDate("modified")));
            }
        } catch (SQLException e) {
            LOGGER.error("Query: " + selectSt, e);
        } finally {
            closeConnection();
        }
        return retList;
    }

    @Override
    public boolean putDescription(String uuid, String description) throws DatabaseException {
        if (uuid == null) throw new NullPointerException("uuid");
        if (description == null) throw new NullPointerException("description");
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            LOGGER.warn("Unable to set autocommit off", e);
        }
        boolean found = true;
        try {
            PreparedStatement findSt = getConnection().prepareStatement(SELECT_COMMON_DESCRIPTION_STATEMENT);
            PreparedStatement updSt = null;
            findSt.setString(1, uuid);
            ResultSet rs = findSt.executeQuery();
            found = rs.next();
            int modified = 0;
            updSt = getConnection().prepareStatement(found ? UPDATE_COMMON_DESCRIPTION_STATEMENT : INSERT_COMMON_DESCRIPTION_STATEMENT);
            updSt.setString(1, description);
            updSt.setString(2, uuid);
            modified = updSt.executeUpdate();
            if (modified == 1) {
                getConnection().commit();
                LOGGER.debug("DB has been updated. Queries: \"" + findSt + "\" and \"" + updSt + "\"");
            } else {
                getConnection().rollback();
                LOGGER.error("DB has not been updated -> rollback! Queries: \"" + findSt + "\" and \"" + updSt + "\"");
                found = false;
            }
        } catch (SQLException e) {
            LOGGER.error(e);
            found = false;
        } finally {
            closeConnection();
        }
        return found;
    }

    @Override
    public String getDescription(String uuid) throws DatabaseException {
        if (uuid == null) throw new NullPointerException("uuid");
        String description = null;
        PreparedStatement findSt = null;
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            LOGGER.warn("Unable to set autocommit off", e);
        }
        try {
            findSt = getConnection().prepareStatement(SELECT_COMMON_DESCRIPTION_STATEMENT);
            findSt.setString(1, uuid);
            ResultSet rs = findSt.executeQuery();
            while (rs.next()) {
                description = rs.getString("description");
            }
        } catch (SQLException e) {
            LOGGER.error("Query: " + findSt, e);
        } finally {
            closeConnection();
        }
        return description;
    }

    @Override
    public boolean putUserDescription(String openID, String uuid, String description) throws DatabaseException {
        if (uuid == null) throw new NullPointerException("uuid");
        if (description == null) throw new NullPointerException("description");
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            LOGGER.warn("Unable to set autocommit off", e);
        }
        boolean found = true;
        try {
            int modified = 0;
            PreparedStatement updSt = getConnection().prepareStatement(UPDATE_USER_DESCRIPTION_STATEMENT);
            updSt.setString(1, description);
            updSt.setString(2, uuid);
            updSt.setString(3, openID);
            modified = updSt.executeUpdate();
            if (modified == 1) {
                getConnection().commit();
                LOGGER.debug("DB has been updated. Query: \"" + updSt + "\"");
            } else {
                getConnection().rollback();
                LOGGER.error("DB has not been updated -> rollback!  Query: \"" + updSt + "\"");
                found = false;
            }
        } catch (SQLException e) {
            LOGGER.error(e);
            found = false;
        } finally {
            closeConnection();
        }
        return found;
    }

    @Override
    public RecentlyModifiedItem getUserDescriptionAndDate(String openID, String uuid) throws DatabaseException {
        if (uuid == null) throw new NullPointerException("uuid");
        RecentlyModifiedItem ret = null;
        PreparedStatement findSt = null;
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            LOGGER.warn("Unable to set autocommit off", e);
        }
        try {
            findSt = getConnection().prepareStatement(SELECT_USER_DESCRIPTION_STATEMENT);
            findSt.setString(1, uuid);
            findSt.setString(2, openID);
            ResultSet rs = findSt.executeQuery();
            while (rs.next()) {
                ret = new RecentlyModifiedItem();
                ret.setDescription(rs.getString("description"));
                ret.setModified(rs.getDate("modified"));
            }
        } catch (SQLException e) {
            LOGGER.error("Query: " + findSt, e);
        } finally {
            closeConnection();
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws DatabaseException
     */
    @Override
    public boolean deleteRemovedItem(String uuid) throws DatabaseException {
        PreparedStatement deleteSt = null;
        try {
            deleteSt = getConnection().prepareStatement(DELETE_REMOVED_ITEM);
            deleteSt.setString(1, uuid);
        } catch (SQLException e) {
            LOGGER.error("Could not get delete statement", e);
        }
        try {
            deleteSt.executeUpdate();
            getConnection().commit();
            LOGGER.debug("DB has been updated. Queries: \"" + deleteSt + "\"");
        } catch (SQLException e) {
            LOGGER.error("Query: " + deleteSt, e);
        } finally {
            closeConnection();
        }
        return true;
    }
}

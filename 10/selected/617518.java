package org.isqlviewer.sql.embedded;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.isqlviewer.BindVariable;
import org.isqlviewer.ServiceReference;
import org.isqlviewer.bookmarks.Bookmark;
import org.isqlviewer.bookmarks.BookmarkFolder;
import org.isqlviewer.bookmarks.BookmarkReference;
import org.isqlviewer.bookmarks.ColorLabel;
import org.isqlviewer.history.CommandType;
import org.isqlviewer.history.HistoricalCommand;
import org.isqlviewer.util.BasicUtilities;
import org.isqlviewer.util.IsqlToolkit;

/**
 * Embedded Database Wrapper for providing high-level application functionaliy backed by a JDBC database.
 * <p>
 * This class currently uses Apache Derby as the backed. The DDL that defines the structures are defined by the
 * dbinit.sql script within this package.
 * 
 * @author Mark A. Kobold &lt;mkobold at isqlviewer dot com&gt;
 * @version 1.0
 */
public class EmbeddedDatabase {

    private static final String HISTORY_DELETE_ALL = "DELETE FROM HISTORY";

    private static final String CONSTRAINT_VIOLATION = "23505";

    private static final String NO_DATA_AVAILABLE = "24000";

    private static final String BOOKMARK_INSERT = "INSERT INTO BOOKMARK (IS_FAVORITE, CREATION_TIME, LAST_ACCESS, NAME, COMMAND_TEXT, USAGE_COUNT, FOLDER, COLOR_LABEL) VALUES (?, ?, ?, ?, ?, ? ,?, ?)";

    private static final String BOOKMARK_UPDATE = "UPDATE  BOOKMARK SET IS_FAVORITE = ?, LAST_ACCESS = CURRENT_TIMESTAMP,  NAME = ?, COMMAND_TEXT = ?, USAGE_COUNT = ?,  FOLDER = ?, COLOR_LABEL=? WHERE BOOKMARK_ID=?";

    private static final String BOOKMARK_UPDATE_LITE = "UPDATE  BOOKMARK SET  NAME = ?, IS_FAVORITE = ?, LAST_ACCESS = CURRENT_TIMESTAMP,  FOLDER = ?, COLOR_LABEL=? WHERE BOOKMARK_ID=?";

    private static final String BOOKMARK_SELECT_LITE = "SELECT BOOKMARK_ID,NAME,FOLDER,IS_FAVORITE, COLOR_LABEL  FROM BOOKMARK ORDER BY FOLDER";

    private static final String BOOKMARK_SELECT_SINGLE = "SELECT BOOKMARK_ID, IS_FAVORITE, CREATION_TIME, LAST_ACCESS, NAME, COMMAND_TEXT, USAGE_COUNT, COLOR_LABEL FROM BOOKMARK WHERE BOOKMARK_ID = ?";

    private static final String BOOKMARK_DELETE = "DELETE FROM  BOOKMARK  WHERE BOOKMARK_ID=?";

    private static final String BOOKMARK_DELETE_FOLDER = "DELETE FROM  BOOKMARK  WHERE FOLDER LIKE ''{0}%'' ";

    private static final String BOOKMARK_FOLDER_RENAME = "UPDATE BOOKMARK SET FOLDER = REGEX_REPLACE(FOLDER, ?, ?) WHERE FOLDER LIKE ?";

    private static final String BOOKMARK_BIND_VARIABLE_INSERT = "INSERT INTO BOOKMARK_BIND_VARIABLE (BOOKMARK_ID, PARAMETER_INDEX, SQL_TYPE, USER_DATA, OPTIONS) VALUES (?, ?, ?, ?, ?)";

    private static final String SERVICE_SELECT_ALL = "SELECT SERVICE_ID, NAME, ORDER_PREFERENCE, LAST_USED, CREATION_TIME, RESOURCE_URL FROM SERVICE WHERE SERVICE_ID > 0 ORDER BY SERVICE_ID";

    private static final String SERVICE_SELECT_BY_NAME = "SELECT SERVICE_ID, NAME, ORDER_PREFERENCE, LAST_USED, CREATION_TIME, RESOURCE_URL FROM SERVICE WHERE NAME = ?";

    private static final String SERVICE_INSERT = "INSERT INTO SERVICE (NAME, ORDER_PREFERENCE, LAST_USED,RESOURCE_URL, CREATION_TIME) values (?, ?, ?, ?, ?)";

    private static final String SERVICE_ID_FOR_NAME = "SELECT SERVICE_ID FROM SERVICE WHERE NAME = ?";

    private static final String SERVICE_UPDATE = "UPDATE  SERVICE SET NAME = ?, ORDER_PREFERENCE = ?,  LAST_USED = ?, RESOURCE_URL = ? WHERE SERVICE_ID=?";

    private static final String SERVICE_INSTALL_UNKNOWN = "INSERT INTO SERVICE (NAME, ORDER_PREFERENCE, RESOURCE_URL, CREATION_TIME) values ( '???', -1, '', CURRENT_TIMESTAMP)";

    private static final String SERVICE_DELETE = "DELETE FROM SERVICE WHERE SERVICE_ID = ?";

    private static final String HISTORY_INSERT = "INSERT INTO HISTORY (SERVICE_ID, QUERY_TIME, TRANSACTION_ID, COMMAND_TYPE, COMMAND_TEXT) values (?, ?, ?, ?, ?)";

    private static final String HISTORY_SELECT_ALL = "SELECT HISTORY_ID, QUERY_TIME, TRANSACTION_ID, COMMAND_TYPE, COMMAND_TEXT FROM HISTORY ORDER BY HISTORY_ID, QUERY_TIME";

    private static final String HISTORY_SELECT_SINGLE = "SELECT H.HISTORY_ID, S.NAME, H.QUERY_TIME, H.TRANSACTION_ID, H.COMMAND_TYPE, H.COMMAND_TEXT FROM HISTORY H, SERVICE S WHERE HISTORY_ID = ? AND H.SERVICE_ID = S.SERVICE_ID";

    private static final String HISTORY_SELECT_SINGLE_BATCH = "SELECT H.HISTORY_ID, S.NAME, H.QUERY_TIME, H.TRANSACTION_ID, H.COMMAND_TYPE, H.COMMAND_TEXT FROM HISTORY H, SERVICE S WHERE TRANSACTION_ID = ? AND H.SERVICE_ID = S.SERVICE_ID ORDER BY HISTORY_ID";

    private static final String HISTORY_DELETE_SERVICE = "DELETE FROM HISTORY WHERE SERVICE_ID = ?";

    private static final String PRIMARY_KEY_QUERY = "values IDENTITY_VAL_LOCAL()";

    private static final EmbeddedDatabase sharedInstance;

    public static EmbeddedDatabase getSharedInstance() {
        return sharedInstance;
    }

    static {
        sharedInstance = new EmbeddedDatabase();
    }

    private Connection embeddedConnection = null;

    private File databaseHome = new File(IsqlToolkit.getBaseDirectory(), "derby");

    private EmbeddedDatabase() {
        new EmbeddedDriver();
    }

    public boolean addService(ServiceReference serviceReference) throws SQLException {
        Object[] bindVariables = new Object[5];
        bindVariables[0] = serviceReference.getName();
        bindVariables[1] = new Integer(serviceReference.getOrder());
        Date lastUsed = serviceReference.getLastUsed();
        bindVariables[2] = new Timestamp(lastUsed == null ? 0 : lastUsed.getTime());
        bindVariables[3] = serviceReference.getResourceURL().toExternalForm();
        Date createdOn = serviceReference.getCreatedOn();
        bindVariables[4] = new Timestamp(createdOn == null ? System.currentTimeMillis() : createdOn.getTime());
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(SERVICE_INSERT);
            for (int i = 0; i < bindVariables.length; i++) {
                preparedStatement.setObject(i + 1, bindVariables[i]);
            }
            int affectedCount = preparedStatement.executeUpdate();
            long identityValue = getInsertedPrimaryKey();
            serviceReference.setId(identityValue);
            return affectedCount == 1;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public HistoricalCommand addHistoricalCommand(HistoricalCommand command) throws SQLException {
        HistoricalCommand reference = null;
        if (command.hasChildCommands()) {
            reference = addHistoricalCommand0(command);
            Enumeration<HistoricalCommand> childCommands = command.elements();
            while (childCommands.hasMoreElements()) {
                HistoricalCommand next = childCommands.nextElement();
                reference.addSubcommand(addHistoricalCommand0(next));
            }
        } else {
            return addHistoricalCommand0(command);
        }
        return reference;
    }

    public HistoricalCommand getHistoricalCommand(HistoricalCommand reference) throws SQLException {
        ensureConnection();
        PreparedStatement preparedStatement = null;
        HistoricalCommand parentCommand = null;
        ResultSet cursor = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(HISTORY_SELECT_SINGLE_BATCH);
            preparedStatement.setLong(1, reference.getTransactionId());
            boolean hasResults = preparedStatement.execute();
            if (hasResults) {
                cursor = preparedStatement.getResultSet();
                while (cursor.next()) {
                    HistoricalCommand command = new HistoricalCommand();
                    command.setId(cursor.getLong(1));
                    command.setService(cursor.getString(2));
                    Date executionTime = new Date(cursor.getTimestamp(3).getTime());
                    command.setQueryTime(executionTime);
                    command.setTransactionId(cursor.getLong(4));
                    command.setType(CommandType.valueOf(cursor.getString(5)));
                    String commandText = cursor.getString(6);
                    command.setCommandText(commandText);
                    if (parentCommand == null) {
                        parentCommand = command;
                    } else {
                        parentCommand.addSubcommand(command);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SQLException ignored) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return parentCommand;
    }

    public HistoricalCommand getHistoricalCommand(long historyId) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet cursor = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(HISTORY_SELECT_SINGLE);
            preparedStatement.setLong(1, historyId);
            boolean hasResults = preparedStatement.execute();
            if (hasResults) {
                cursor = preparedStatement.getResultSet();
                if (cursor.next()) {
                    HistoricalCommand command = new HistoricalCommand();
                    command.setId(cursor.getLong(1));
                    command.setService(cursor.getString(2));
                    command.setQueryTime(cursor.getTimestamp(3));
                    command.setTransactionId(cursor.getLong(4));
                    CommandType type = CommandType.valueOf(cursor.getString(5));
                    command.setType(type);
                    command.setCommandText(cursor.getString(6));
                    return command;
                }
            }
            return null;
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SQLException ignored) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public Collection<HistoricalCommand> getHistory() throws SQLException {
        ensureConnection();
        ArrayList<HistoricalCommand> historySet = new ArrayList<HistoricalCommand>();
        Statement stmt = embeddedConnection.createStatement();
        ResultSet cursor = null;
        try {
            boolean hasResults = stmt.execute(HISTORY_SELECT_ALL);
            if (hasResults) {
                cursor = stmt.getResultSet();
                long currentTransactionId = -1;
                HistoricalCommand parentCommand = null;
                while (cursor.next()) {
                    HistoricalCommand reference = new HistoricalCommand();
                    reference.setId(cursor.getLong(1));
                    Date executionTime = new Date(cursor.getTimestamp(2).getTime());
                    reference.setQueryTime(executionTime);
                    reference.setTransactionId(cursor.getLong(3));
                    reference.setType(CommandType.valueOf(cursor.getString(4)));
                    String commandText = cursor.getString(5);
                    StringBuilder builder = new StringBuilder("");
                    for (int i = 0; i < commandText.length(); i++) {
                        char c = commandText.charAt(i);
                        if (Character.isWhitespace(c)) {
                            break;
                        }
                        builder.append(c);
                    }
                    long transactionId = reference.getTransactionId();
                    if (transactionId > 0 && parentCommand == null) {
                        parentCommand = reference;
                        currentTransactionId = transactionId;
                        historySet.add(reference);
                    } else if (transactionId > 0 && parentCommand != null) {
                        if (transactionId == currentTransactionId) {
                            parentCommand.addSubcommand(reference);
                        } else {
                            parentCommand = reference;
                            currentTransactionId = transactionId;
                            historySet.add(reference);
                        }
                    } else if (transactionId < 0) {
                        parentCommand = null;
                        currentTransactionId = -1;
                        historySet.add(reference);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SQLException ignored) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return historySet;
    }

    public long getServiceIDForName(String service) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet cursor = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(SERVICE_ID_FOR_NAME);
            preparedStatement.setString(1, service);
            boolean hasResults = preparedStatement.execute();
            if (hasResults) {
                cursor = preparedStatement.getResultSet();
                if (cursor.next()) {
                    long serviceID = cursor.getLong(1);
                    if (cursor.wasNull()) {
                        return 1;
                    }
                    return serviceID;
                }
            }
        } catch (SQLException sqle) {
            if (NO_DATA_AVAILABLE.equals(sqle.getSQLState())) {
                return 1;
            }
            throw sqle;
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SQLException ignored) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return 1;
    }

    public boolean addBookmarkFolder(BookmarkFolder bookmarkFolder) throws SQLException {
        int childCount = bookmarkFolder.getChildCount();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(BOOKMARK_INSERT);
            for (int i = 0; i < childCount; i++) {
                Object childElement = bookmarkFolder.getChild(i);
                if (childElement instanceof Bookmark) {
                    addBookmark0((Bookmark) childElement, bookmarkFolder, preparedStatement);
                } else if (childElement instanceof BookmarkFolder) {
                    addBookmarkFolder0((BookmarkFolder) childElement, 1, preparedStatement);
                }
            }
            preparedStatement.executeBatch();
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return true;
    }

    public Bookmark getBookmark(BookmarkReference reference) throws SQLException {
        Bookmark bookmark = getBookmark(reference.getId());
        bookmark.setFolder(reference.getFolder());
        return bookmark;
    }

    public Bookmark getBookmark(long bookmarkId) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet cursor = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(BOOKMARK_SELECT_SINGLE);
            preparedStatement.setLong(1, bookmarkId);
            boolean hasResults = preparedStatement.execute();
            if (hasResults) {
                cursor = preparedStatement.getResultSet();
                if (cursor.next()) {
                    Bookmark bookmark = new Bookmark();
                    bookmark.setId(cursor.getLong(1));
                    bookmark.setFavorite(cursor.getBoolean(2));
                    bookmark.setCreationTime(cursor.getTimestamp(3));
                    bookmark.setLastAccess(cursor.getTimestamp(4));
                    bookmark.setName(cursor.getString(5));
                    bookmark.setCommandText(cursor.getString(6));
                    bookmark.setUseCount(cursor.getLong(7));
                    String labelColorText = cursor.getString(8);
                    if (labelColorText != null) {
                        bookmark.setColorLabel(ColorLabel.valueOf(labelColorText));
                    }
                    return bookmark;
                }
            }
            return null;
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SQLException ignored) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean updateService(ServiceReference serviceReference) throws SQLException {
        Object[] bindVariables = new Object[5];
        int[] types = new int[5];
        types[0] = Types.VARCHAR;
        types[1] = Types.INTEGER;
        types[2] = Types.TIMESTAMP;
        types[2] = Types.VARCHAR;
        types[4] = Types.BIGINT;
        bindVariables[0] = serviceReference.getName();
        bindVariables[1] = new Integer(serviceReference.getOrder());
        Date lastUsed = serviceReference.getLastUsed();
        bindVariables[2] = new Timestamp(lastUsed == null ? System.currentTimeMillis() : lastUsed.getTime());
        bindVariables[3] = serviceReference.getResourceURL();
        bindVariables[3] = new Long(serviceReference.getId());
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(SERVICE_UPDATE);
            for (int i = 0; i < bindVariables.length; i++) {
                if (bindVariables[i] == null) {
                    preparedStatement.setNull(i + 1, types[i]);
                } else {
                    preparedStatement.setObject(i + 1, bindVariables[i]);
                }
            }
            int affected = preparedStatement.executeUpdate();
            return affected == 1;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean updateBookmark(BookmarkReference bookmark) throws SQLException {
        Object[] bindVariables = new Object[5];
        int[] types = new int[5];
        types[0] = Types.VARCHAR;
        types[1] = Types.BOOLEAN;
        types[2] = Types.VARCHAR;
        types[2] = Types.VARCHAR;
        types[4] = Types.BIGINT;
        bindVariables[0] = bookmark.getName();
        bindVariables[1] = Boolean.valueOf(bookmark.isFavorite());
        bindVariables[2] = bookmark.getPath();
        ColorLabel colorLabel = bookmark.getColorLabel();
        bindVariables[3] = colorLabel == null ? null : colorLabel.name();
        bindVariables[4] = new Long(bookmark.getId());
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(BOOKMARK_UPDATE_LITE);
            for (int i = 0; i < bindVariables.length; i++) {
                if (bindVariables[i] == null) {
                    preparedStatement.setNull(i + 1, types[i]);
                } else {
                    preparedStatement.setObject(i + 1, bindVariables[i]);
                }
            }
            int affected = preparedStatement.executeUpdate();
            return affected == 1;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean updateBookmark(Bookmark bookmark) throws SQLException {
        Object[] bindVariables = new Object[7];
        int[] types = new int[7];
        types[0] = Types.BOOLEAN;
        types[1] = Types.VARCHAR;
        types[2] = Types.VARCHAR;
        types[3] = Types.BIGINT;
        types[4] = Types.VARCHAR;
        types[5] = Types.VARCHAR;
        types[6] = Types.BIGINT;
        bindVariables[0] = Boolean.valueOf(bookmark.isFavorite());
        bindVariables[1] = bookmark.getName();
        bindVariables[2] = bookmark.getCommandText();
        bindVariables[3] = new Long(bookmark.getUseCount());
        bindVariables[4] = bookmark.getPath();
        ColorLabel colorLabel = bookmark.getColorLabel();
        bindVariables[5] = colorLabel == null ? null : colorLabel.name();
        bindVariables[6] = new Long(bookmark.getId());
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(BOOKMARK_UPDATE);
            for (int i = 0; i < bindVariables.length; i++) {
                if (bindVariables[i] == null) {
                    preparedStatement.setNull(i + 1, types[i]);
                } else {
                    preparedStatement.setObject(i + 1, bindVariables[i]);
                }
            }
            int affected = preparedStatement.executeUpdate();
            return affected == 1;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean removeBookmarkFolder(BookmarkFolder folder) throws SQLException {
        Statement statement = null;
        String query = MessageFormat.format(BOOKMARK_DELETE_FOLDER, new Object[] { folder.getPath() });
        try {
            statement = embeddedConnection.createStatement();
            int affectedCount = statement.executeUpdate(query);
            return affectedCount == 1;
        } catch (SQLException sqle) {
            if (NO_DATA_AVAILABLE.equals(sqle.getSQLState())) {
                return false;
            }
            throw sqle;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean removeBookmark(BookmarkReference bookmark) throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(BOOKMARK_DELETE);
            preparedStatement.setLong(1, bookmark.getId());
            int affectedCount = preparedStatement.executeUpdate();
            return affectedCount == 1;
        } catch (SQLException sqle) {
            if (NO_DATA_AVAILABLE.equals(sqle.getSQLState())) {
                return false;
            }
            throw sqle;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean removeService(ServiceReference reference) throws SQLException {
        ensureConnection();
        PreparedStatement statement = null;
        try {
            statement = embeddedConnection.prepareStatement(SERVICE_DELETE);
            statement.setLong(1, reference.getId());
            int affectedCount = statement.executeUpdate();
            statement.close();
            statement = embeddedConnection.prepareStatement(HISTORY_DELETE_SERVICE);
            statement.setLong(1, reference.getId());
            statement.executeUpdate();
            return affectedCount == 1;
        } catch (SQLException sqle) {
            if (NO_DATA_AVAILABLE.equals(sqle.getSQLState())) {
                return false;
            }
            throw sqle;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public BookmarkFolder getBookmarks() throws SQLException {
        ensureConnection();
        BookmarkFolder rootFolder = BookmarkFolder.createRootFolder();
        Statement stmt = embeddedConnection.createStatement();
        ResultSet cursor = null;
        try {
            boolean hasResults = stmt.execute(BOOKMARK_SELECT_LITE);
            if (hasResults) {
                cursor = stmt.getResultSet();
                while (cursor.next()) {
                    BookmarkReference bookmark = new BookmarkReference();
                    bookmark.setId(cursor.getLong(1));
                    bookmark.setName(cursor.getString(2));
                    bookmark.setFavorite(cursor.getBoolean(4));
                    String labelColorText = cursor.getString(5);
                    if (labelColorText != null) {
                        bookmark.setColorLabel(ColorLabel.valueOf(labelColorText));
                    }
                    BookmarkFolder folder = rootFolder.mkdirs(cursor.getString(3));
                    if (folder != null) {
                        folder.addBookmark(bookmark);
                        bookmark.setFolder(folder);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SQLException ignored) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return rootFolder;
    }

    public ServiceReference getServiceForName(String serviceName) throws SQLException {
        ensureConnection();
        PreparedStatement preparedStatement = null;
        ResultSet cursor = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(SERVICE_SELECT_BY_NAME);
            preparedStatement.setString(1, serviceName);
            boolean hasResults = preparedStatement.execute();
            if (hasResults) {
                cursor = preparedStatement.getResultSet();
                if (cursor.next()) {
                    ServiceReference reference = new ServiceReference();
                    reference.setId(cursor.getLong(1));
                    reference.setName(cursor.getString(2));
                    reference.setOrder(cursor.getInt(3));
                    reference.setLastUsed(cursor.getDate(4));
                    reference.setCreatedOn(cursor.getDate(5));
                    reference.setResourceURL(new URL(cursor.getString(6)));
                    return reference;
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SQLException ignored) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return null;
    }

    public Collection<ServiceReference> getRegisteredServices() throws SQLException {
        ensureConnection();
        ArrayList<ServiceReference> serviceReferenceSet = new ArrayList<ServiceReference>();
        Statement stmt = embeddedConnection.createStatement();
        ResultSet cursor = null;
        try {
            boolean hasResults = stmt.execute(SERVICE_SELECT_ALL);
            if (hasResults) {
                cursor = stmt.getResultSet();
                while (cursor.next()) {
                    ServiceReference reference = new ServiceReference();
                    reference.setId(cursor.getLong(1));
                    reference.setName(cursor.getString(2));
                    reference.setOrder(cursor.getInt(3));
                    reference.setLastUsed(cursor.getDate(4));
                    reference.setCreatedOn(cursor.getDate(5));
                    try {
                        reference.setResourceURL(new URL(cursor.getString(6)));
                    } catch (MalformedURLException e) {
                        continue;
                    }
                    serviceReferenceSet.add(reference);
                }
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SQLException ignored) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return serviceReferenceSet;
    }

    public BookmarkReference addBookmark(Bookmark bookmark) throws SQLException {
        if (addBookmark0(bookmark, null, null)) {
            return bookmark.toBookmarkReference();
        }
        return null;
    }

    public int removeAllHistory() throws SQLException {
        ensureConnection();
        Statement stmt = embeddedConnection.createStatement();
        try {
            return stmt.executeUpdate(HISTORY_DELETE_ALL);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean renameBookmarkFolder(String existingPath, String newPath) throws SQLException {
        Object[] bindVariables = new Object[3];
        int[] types = new int[3];
        types[0] = Types.VARCHAR;
        types[1] = Types.VARCHAR;
        types[2] = Types.VARCHAR;
        bindVariables[0] = MessageFormat.format("({0})(.*)", new Object[] { existingPath });
        bindVariables[1] = MessageFormat.format("{0}$2", new Object[] { newPath });
        bindVariables[2] = MessageFormat.format("{0}%", new Object[] { existingPath });
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(BOOKMARK_FOLDER_RENAME);
            for (int i = 0; i < bindVariables.length; i++) {
                if (bindVariables[i] == null) {
                    preparedStatement.setNull(i + 1, types[i]);
                } else {
                    preparedStatement.setObject(i + 1, bindVariables[i]);
                }
            }
            int affected = preparedStatement.executeUpdate();
            return affected >= 1;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Initializes the internal databases by creating the connection and creating all the nessecary database structures.
     * <p>
     * If this method returns true, the calling method should perform any nessecary upgrade and or import functions as
     * deemed nessecary by the caller.
     * <p>
     * This method should only be called once.
     * 
     * @return <tt>true</tt> if the database was initialized as a new instance, <tt>false</tt> if the database was
     *         pre-existing.
     * @throws SQLException
     */
    public synchronized boolean initialize() throws SQLException {
        boolean createTables = false;
        if (!databaseHome.exists()) {
            createTables = true;
        }
        ensureConnection();
        if (createTables) {
            InputStream initStream = EmbeddedDatabase.class.getResourceAsStream("dbinit.sql");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(initStream));
                StringBuilder sqlBuffer = new StringBuilder("");
                while (reader.ready()) {
                    String line = reader.readLine();
                    if (line.length() == 0) {
                        try {
                            if (sqlBuffer.length() > 0) {
                                executeStatement(sqlBuffer.toString());
                            }
                        } finally {
                            sqlBuffer.setLength(0);
                        }
                    } else {
                        sqlBuffer.append(line);
                        sqlBuffer.append(' ');
                    }
                }
                if (sqlBuffer.length() > 0) {
                    executeStatement(sqlBuffer.toString());
                }
                executeStatement(SERVICE_INSTALL_UNKNOWN);
            } catch (IOException error) {
                SQLException sqlError = new SQLException(error.getMessage());
                BasicUtilities.wrapThrowable(error, sqlError);
                throw sqlError;
            }
        }
        return createTables;
    }

    private boolean addBookmark0(Bookmark bookmark, BookmarkFolder folder, PreparedStatement preparedStatement) throws SQLException {
        Object[] bindVariables = new Object[8];
        int[] types = new int[8];
        types[0] = Types.BOOLEAN;
        types[1] = Types.TIMESTAMP;
        types[2] = Types.TIMESTAMP;
        types[3] = Types.VARCHAR;
        types[4] = Types.VARCHAR;
        types[5] = Types.BIGINT;
        types[6] = Types.VARCHAR;
        types[7] = Types.VARCHAR;
        bindVariables[0] = Boolean.valueOf(bookmark.isFavorite());
        Date time = bookmark.getCreationTime();
        bindVariables[1] = new Timestamp(time == null ? System.currentTimeMillis() : time.getTime());
        time = bookmark.getLastAccess();
        bindVariables[2] = new Timestamp(time == null ? System.currentTimeMillis() : time.getTime());
        bindVariables[3] = bookmark.getName();
        bindVariables[4] = bookmark.getCommandText();
        bindVariables[5] = new Long(bookmark.getUseCount());
        bindVariables[6] = folder == null ? bookmark.getPath() : folder.getPath();
        ColorLabel colorLabel = bookmark.getColorLabel();
        bindVariables[7] = colorLabel == null ? null : colorLabel.name();
        boolean doBatch = (preparedStatement != null);
        boolean hasError = true;
        embeddedConnection.setAutoCommit(false);
        PreparedStatement statement = null;
        try {
            if (preparedStatement == null) {
                statement = embeddedConnection.prepareStatement(BOOKMARK_INSERT);
            } else {
                statement = preparedStatement;
            }
            for (int i = 0; i < bindVariables.length; i++) {
                if (bindVariables[i] == null) {
                    statement.setNull(i + 1, types[i]);
                } else {
                    statement.setObject(i + 1, bindVariables[i]);
                }
            }
            try {
                int affectedCount = statement.executeUpdate();
                long identityValue = getInsertedPrimaryKey();
                bookmark.setId(identityValue);
                addBindVariables(bookmark);
                hasError = false;
                return affectedCount == 1;
            } catch (SQLException exception) {
                if (CONSTRAINT_VIOLATION.equals(exception.getSQLState())) {
                    return false;
                }
                throw exception;
            }
        } finally {
            if (hasError) {
                embeddedConnection.rollback();
            } else {
                embeddedConnection.commit();
            }
            embeddedConnection.setAutoCommit(true);
            if (preparedStatement != null) {
                if (!doBatch) {
                    try {
                        preparedStatement.close();
                    } catch (SQLException ignored) {
                    }
                } else if (doBatch) {
                    preparedStatement.clearParameters();
                    preparedStatement.clearWarnings();
                }
            }
        }
    }

    private HistoricalCommand addHistoricalCommand0(HistoricalCommand command) throws SQLException {
        Object[] bindVariables = new Object[5];
        bindVariables[0] = new Long(getServiceIDForName(command.getService()));
        Date executionTime = command.getQueryTime();
        bindVariables[1] = new Timestamp(executionTime == null ? 0 : executionTime.getTime());
        bindVariables[2] = new Long(command.getTransactionId());
        bindVariables[3] = command.getType().toString();
        bindVariables[4] = command.getCommandText();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = embeddedConnection.prepareStatement(HISTORY_INSERT);
            for (int i = 0; i < bindVariables.length; i++) {
                preparedStatement.setObject(i + 1, bindVariables[i]);
            }
            int affectedCount = preparedStatement.executeUpdate();
            if (affectedCount == 1) {
                long identityValue = getInsertedPrimaryKey();
                command.setId(identityValue);
                HistoricalCommand referenceObject = new HistoricalCommand();
                referenceObject.setId(identityValue);
                referenceObject.setQueryTime(command.getQueryTime());
                referenceObject.setTransactionId(command.getTransactionId());
                referenceObject.setType(command.getType());
                String commandText = (String) bindVariables[4];
                StringBuilder builder = new StringBuilder("");
                for (int i = 0; i < commandText.length(); i++) {
                    char c = commandText.charAt(i);
                    if (Character.isWhitespace(c)) {
                        break;
                    }
                    builder.append(c);
                }
                return referenceObject;
            }
            return null;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private void addBindVariables(Bookmark bookmark) throws SQLException {
        Object[] bindVariables = new Object[5];
        bindVariables[0] = new Long(bookmark.getId());
        Enumeration<BindVariable> variables = bookmark.variables();
        PreparedStatement preparedStatement = null;
        int localParameterIndex = 1;
        try {
            preparedStatement = embeddedConnection.prepareStatement(BOOKMARK_BIND_VARIABLE_INSERT);
            while (variables.hasMoreElements()) {
                BindVariable next = variables.nextElement();
                int index = next.getIndex();
                if (index <= 0) {
                    index = localParameterIndex;
                }
                localParameterIndex++;
                bindVariables[1] = new Integer(index);
                bindVariables[2] = new Integer(next.getType());
                bindVariables[3] = next.getUserData();
                bindVariables[4] = next.getFormatOptions();
                for (int i = 0; i < bindVariables.length; i++) {
                    preparedStatement.setObject(i + 1, bindVariables[i]);
                }
                preparedStatement.executeUpdate();
            }
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private void addBookmarkFolder0(BookmarkFolder bookmarkFolder, int stackDepth, PreparedStatement preparedStatement) throws SQLException {
        int childCount = bookmarkFolder.getChildCount();
        for (int i = 0; i < childCount; i++) {
            Object childElement = bookmarkFolder.getChild(i);
            if (childElement instanceof Bookmark) {
                addBookmark0((Bookmark) childElement, bookmarkFolder, preparedStatement);
            } else if (childElement instanceof BookmarkFolder) {
                addBookmarkFolder0((BookmarkFolder) childElement, stackDepth + 1, preparedStatement);
            }
        }
    }

    private void executeStatement(String sqlBuffer) throws SQLException {
        Statement stmt = null;
        try {
            stmt = embeddedConnection.createStatement();
            stmt.execute(sqlBuffer.toString());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private void ensureConnection() throws SQLException {
        if (embeddedConnection != null) {
            return;
        }
        String url = constructURL();
        boolean hasError = true;
        try {
            embeddedConnection = DriverManager.getConnection(url);
            hasError = false;
        } catch (SQLException error) {
            throw error;
        } finally {
            if (hasError) {
                invalidateConnection();
            }
        }
    }

    private void invalidateConnection() {
        if (embeddedConnection != null) {
            try {
                embeddedConnection.close();
            } catch (Exception ignored) {
            } finally {
                embeddedConnection = null;
            }
        }
    }

    private long getInsertedPrimaryKey() throws SQLException {
        Statement stmt = null;
        ResultSet identity = null;
        try {
            stmt = embeddedConnection.createStatement();
            boolean hasResults = stmt.execute(PRIMARY_KEY_QUERY);
            if (hasResults) {
                identity = stmt.getResultSet();
                if (identity.next()) {
                    return identity.getLong(1);
                }
            }
            return -1;
        } finally {
            if (identity != null) {
                try {
                    identity.close();
                } catch (SQLException ignored) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private String constructURL() {
        MessageFormat urlFormat = new MessageFormat("jdbc:derby:{0}/;create=true;upgrade=true;");
        Object[] parameters = new Object[1];
        parameters[0] = databaseHome.getAbsolutePath();
        return urlFormat.format(parameters);
    }
}

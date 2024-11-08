package com.once;

import org.apache.log4j.Logger;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.once.log.PerformanceLogger;
import com.once.server.config.ConfigManager;
import com.once.server.data.DataAccessException;
import com.once.server.jdb.DataAccessor;
import com.once.server.security.IAuthenticator;
import com.once.server.security.SecurityFactory;

public class ActionSyncBlockList extends Action {

    private static final long serialVersionUID = -5797780773885978350L;

    private static final Logger m_logger = Logger.getLogger(ActionSyncBlockList.class);

    private static final String ERROR_DATA_ACCESS = "Data access exception.";

    private static final String ERROR_PERMISSION_DENIED = "Permission denied.";

    private static final String ERROR_SQL = "Database (SQL) error.";

    private static final String FLAG_ERROR = "ERROR";

    private static final String FLAG_SUCCESS = "SUCCESS";

    private static final String HEADER_ACCESS_DELETE = "delete";

    private static final String HEADER_ACCESS_MODIFY = "modify";

    private static final String HEADER_ACCESS_READ = "read";

    private static final String HEADER_ACCESS_WRITE = "write";

    private static final String HEADER_GROUP_PRIMARY = "group";

    private static final String HEADER_SESSION = "session";

    private static final String SYMBOL_NEW_LINE = "\n";

    private String lastSQL;

    protected void serve(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        long groupPrimary;
        ConfigManager settings;
        IAuthenticator authentication;
        PrintWriter page;
        String session;
        List<String> directoryList;
        int deadBlocks;
        int newBlocks;
        String accessDelete;
        String accessModify;
        String accessRead;
        String accessWrite;
        String ownerOrganisation;
        resetStartTime();
        request.getSession();
        response.setContentType(CLIENT_FORMAT);
        response.setCharacterEncoding(CLIENT_ENCODING);
        page = getOutputPage(response);
        session = decodeHeader(HEADER_SESSION, request, true);
        groupPrimary = decodeHeaderLong(HEADER_GROUP_PRIMARY, request, true);
        PerformanceLogger plog = PerformanceLogger.getLogger(session, this.getClass().getName());
        plog.logStart("Synchronize blocks");
        if (m_logger.isInfoEnabled()) {
            m_logger.info("SyncBlockList request, groupPrimary: " + groupPrimary + ", session: " + session);
        }
        if ((accessDelete = decodeHeader(HEADER_ACCESS_DELETE, request, false)) == null) {
            accessDelete = "false";
        }
        if ((accessModify = decodeHeader(HEADER_ACCESS_MODIFY, request, false)) == null) {
            accessModify = "false";
        }
        if ((accessRead = decodeHeader(HEADER_ACCESS_READ, request, false)) == null) {
            accessRead = "false";
        }
        if ((accessWrite = decodeHeader(HEADER_ACCESS_WRITE, request, false)) == null) {
            accessWrite = "false";
        }
        authentication = SecurityFactory.getInstance().getAuthenticator();
        settings = ConfigManager.getInstance();
        try {
            if (authentication.isUserLoggedIn(session) == true) {
                ownerOrganisation = authentication.getUserOwnerOrganisation(session);
                directoryList = recursiveDirectory(settings.getBlocksRepository(), "xml", ownerOrganisation);
                lastSQL = "";
                try {
                    deadBlocks = removeDeadBlocks(page, directoryList, settings, ownerOrganisation, groupPrimary);
                    newBlocks = refreshBlockList(page, directoryList, settings, ownerOrganisation, groupPrimary, accessDelete, accessModify, accessRead, accessWrite);
                    page.println(FLAG_SUCCESS);
                    page.println(deadBlocks);
                    page.println(newBlocks);
                } catch (SQLException except) {
                    m_logger.error(except, except);
                    page.println(FLAG_ERROR);
                    page.println(ERROR_SQL);
                    page.print(except.getMessage() + "\nSQL:\n" + lastSQL);
                }
            } else {
                page.println(FLAG_ERROR);
                page.println(ERROR_PERMISSION_DENIED);
            }
        } catch (DataAccessException except) {
            m_logger.error(except, except);
            page.print(FLAG_ERROR + SYMBOL_NEW_LINE + ERROR_DATA_ACCESS + SYMBOL_NEW_LINE + except.getMessage());
        }
        page.close();
        plog.logFinish();
    }

    private List<String> recursiveDirectory(String location, String pattern, String ownerOrganisation) {
        return getFileListing(new File(location), pattern, "", ownerOrganisation, false);
    }

    private List<String> getFileListing(File location, final String pattern, String path, String ownerOrganisation, boolean ignoreOwnerOrg) {
        File entry;
        File[] directoryFile;
        FilenameFilter mask;
        Iterator<File> entries;
        List<File> directoryList;
        List<String> fileList;
        Connection connection = null;
        Statement statement;
        DataAccessor da = new DataAccessor();
        String sql;
        ResultSet result;
        boolean found;
        mask = new FilenameFilter() {

            public boolean accept(File location, String name) {
                File candidate;
                candidate = new File(location, name);
                return (name.endsWith(pattern) || candidate.isDirectory() == true);
            }
        };
        fileList = new ArrayList<String>();
        directoryFile = location.listFiles(mask);
        directoryList = Arrays.asList(directoryFile);
        for (entries = directoryList.iterator(); entries.hasNext(); ) {
            entry = entries.next();
            if (entry.isDirectory() == true) {
                if ("".equals(path) == true) {
                    connection = null;
                    statement = null;
                    try {
                        found = false;
                        connection = da.getConnection();
                        statement = da.getStatement(connection);
                        sql = "SELECT DISTINCT \"ownerorganisation\" FROM contacts.organisation;";
                        lastSQL = sql;
                        result = da.executeQueryAsync(statement, sql);
                        while (result.next() == true) {
                            if (entry.getName().equals(result.getString("ownerorganisation")) == true) {
                                found = true;
                            }
                        }
                        if (found == true) {
                            fileList.addAll(getFileListing(entry, pattern, path + entry.getName() + "/", ownerOrganisation, true));
                            continue;
                        }
                    } catch (Exception except) {
                        m_logger.error(except, except);
                    } finally {
                        da.closeConnection(connection);
                    }
                }
                fileList.addAll(getFileListing(entry, pattern, path + entry.getName() + "/", ownerOrganisation, ignoreOwnerOrg));
            } else {
                if (ignoreOwnerOrg == true) {
                    if (ownerOrganisation.equals(path.substring(0, path.indexOf("/"))) == true) {
                        fileList.add(path.substring(path.indexOf("/") + 1) + entry.getName());
                    }
                } else {
                    fileList.add(path + entry.getName());
                }
            }
        }
        if ("".equals(path) == true) {
            Collections.sort(fileList);
        }
        return fileList;
    }

    private int removeDeadBlocks(PrintWriter page, List<String> directoryList, ConfigManager settings, String ownerOrganisation, long groupPrimary) throws DataAccessException, SQLException {
        DataAccessor da = new DataAccessor();
        Connection conn = null;
        Statement stmt;
        String tableBlockList;
        ResultSet dataResult;
        String blockName;
        String inPart;
        int deadBlockCount;
        try {
            conn = da.getConnection();
            stmt = da.getStatement(conn);
            deadBlockCount = 0;
            tableBlockList = settings.getDefaultBlockPermissionsTableName();
            lastSQL = "SELECT \"block\", \"primary\" FROM " + tableBlockList + " WHERE \"fkgroup\" = " + groupPrimary + " AND \"ownerorganisation\" = '" + ownerOrganisation + "';";
            dataResult = da.executeQueryAsync(stmt, lastSQL);
            inPart = "";
            while (dataResult.next()) {
                blockName = dataResult.getString("block");
                if (directoryList.contains(blockName + ".xml") == false) {
                    if (inPart.length() > 0) {
                        inPart += ", ";
                    }
                    inPart += dataResult.getInt("primary");
                    deadBlockCount++;
                }
            }
            if (inPart.length() > 0) {
                lastSQL = "DELETE FROM " + tableBlockList + " WHERE \"primary\" IN (" + inPart + ");";
                da.executeUpdateAsync(stmt, lastSQL);
            }
            return deadBlockCount;
        } finally {
            da.closeConnection(conn);
        }
    }

    private int refreshBlockList(PrintWriter page, List<String> directoryList, ConfigManager settings, String ownerOrganisation, long groupPrimary, String accessDelete, String accessModify, String accessRead, String accessWrite) throws DataAccessException, SQLException {
        DataAccessor da = new DataAccessor();
        Connection conn = null;
        Statement stmt;
        String tableBlockList;
        ResultSet dataResult;
        Iterator<String> entries;
        String eachEntry;
        String blockEntry;
        int refreshedBlocksCount;
        try {
            conn = da.getConnection();
            stmt = da.getStatement(conn);
            refreshedBlocksCount = 0;
            tableBlockList = settings.getDefaultBlockPermissionsTableName();
            for (entries = directoryList.iterator(); entries.hasNext(); ) {
                eachEntry = (String) entries.next();
                blockEntry = eachEntry.substring(0, eachEntry.indexOf(".xml"));
                lastSQL = "SELECT \"primary\" FROM " + tableBlockList + " WHERE \"block\" = '" + blockEntry + "' AND \"ownerorganisation\" = '" + ownerOrganisation + "' AND \"fkgroup\" = " + groupPrimary + ";";
                dataResult = da.executeQueryAsync(stmt, lastSQL);
                boolean hasBlock = false;
                while (dataResult.next()) {
                    hasBlock = true;
                }
                if (hasBlock == false) {
                    lastSQL = "INSERT INTO " + tableBlockList + "(\"block\", \"read\", \"write\", \"delete\", \"modify\", \"owneror" + "ganisation\", \"fkgroup\") VALUES ('" + blockEntry + "', " + accessRead + ", " + accessWrite + ", " + accessDelete + ", " + accessModify + ", '" + ownerOrganisation + "', " + groupPrimary + ");";
                    da.executeUpdateAsync(stmt, lastSQL);
                    refreshedBlocksCount++;
                }
            }
            return refreshedBlocksCount;
        } finally {
            da.closeConnection(conn);
        }
    }
}

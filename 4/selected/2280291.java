package com.once;

import org.apache.log4j.Logger;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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

public class ActionSyncTableList extends Action {

    private static final long serialVersionUID = -4186568517083253271L;

    private static final Logger m_logger = Logger.getLogger(ActionSyncTableList.class);

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
        List<String> allTables;
        int deadTables;
        int newTables;
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
        plog.logStart("Synchronize tables");
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
                allTables = getAllTables();
                lastSQL = "";
                try {
                    deadTables = removeDeadTables(page, allTables, settings, ownerOrganisation, groupPrimary);
                    newTables = refreshTableList(page, allTables, settings, ownerOrganisation, groupPrimary, accessDelete, accessModify, accessRead, accessWrite);
                    page.println(FLAG_SUCCESS);
                    page.println(deadTables);
                    page.println(newTables);
                } catch (DataAccessException except) {
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

    private List<String> getAllTables() throws DataAccessException {
        DataAccessor da = new DataAccessor();
        Connection conn = da.getConnection();
        Statement stmt = da.getStatement(conn);
        lastSQL = "SELECT schemaname, tablename FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema') ORDER BY schemaname, tablename;";
        ResultSet res = da.executeQueryAsync(stmt, lastSQL);
        ArrayList<String> tables = new ArrayList<String>();
        try {
            while (res.next()) {
                tables.add(res.getString("schemaname") + "." + res.getString("tablename"));
            }
        } catch (SQLException ex) {
            throw new DataAccessException(ex);
        } finally {
            da.closeConnection(conn);
        }
        return tables;
    }

    private int removeDeadTables(PrintWriter page, List<String> allTables, ConfigManager settings, String ownerOrganisation, long groupPrimary) throws DataAccessException {
        DataAccessor da = new DataAccessor();
        Connection conn = null;
        Statement stmt;
        ResultSet dataResult;
        String inPart;
        try {
            conn = da.getConnection();
            stmt = da.getStatement(conn);
            int deadTablesCount = 0;
            String tableTableList = settings.getDefaultTablePermissionsTableName();
            lastSQL = "SELECT \"table\", \"primary\" FROM " + tableTableList + " WHERE \"fkgroup\" = " + groupPrimary + " AND \"ownerorganisation\" = '" + ownerOrganisation + "';";
            dataResult = da.executeQueryAsync(stmt, lastSQL);
            inPart = "";
            while (dataResult.next()) {
                String tableName = dataResult.getString("table");
                if (!allTables.contains(tableName)) {
                    if (inPart.length() > 0) {
                        inPart += ", ";
                    }
                    inPart += dataResult.getInt("primary");
                    deadTablesCount++;
                }
            }
            if (inPart.length() > 0) {
                lastSQL = "DELETE FROM " + tableTableList + " WHERE \"primary\" IN (" + inPart + ");";
                da.executeUpdateAsync(stmt, lastSQL);
            }
            return deadTablesCount;
        } catch (SQLException ex) {
            throw new DataAccessException(ex);
        } finally {
            da.closeConnection(conn);
        }
    }

    private int refreshTableList(PrintWriter page, List<String> allTables, ConfigManager settings, String ownerOrganisation, long groupPrimary, String accessDelete, String accessModify, String accessRead, String accessWrite) throws DataAccessException {
        DataAccessor da = new DataAccessor();
        Connection conn = null;
        Statement stmt;
        ResultSet dataResult;
        try {
            conn = da.getConnection();
            stmt = da.getStatement(conn);
            int refreshedTablesCount = 0;
            String tableTableList = settings.getDefaultTablePermissionsTableName();
            for (Iterator<String> entries = allTables.iterator(); entries.hasNext(); ) {
                String tableName = (String) entries.next();
                lastSQL = "SELECT \"primary\" FROM " + tableTableList + " WHERE \"table\" = '" + tableName + "' AND \"ownerorganisation\" = '" + ownerOrganisation + "' AND \"fkgroup\" = " + groupPrimary + ";";
                dataResult = da.executeQueryAsync(stmt, lastSQL);
                boolean hasTable = dataResult.next();
                if (!hasTable) {
                    lastSQL = "INSERT INTO " + tableTableList + "(\"table\", \"read\", \"write\", \"delete\", \"modify\", \"ownerorganisation\", " + "\"fkgroup\") VALUES ('" + tableName + "', " + accessRead + ", " + accessWrite + ", " + accessDelete + ", " + accessModify + ", '" + ownerOrganisation + "', " + groupPrimary + ");";
                    da.executeUpdateAsync(stmt, lastSQL);
                    refreshedTablesCount++;
                }
            }
            return refreshedTablesCount;
        } catch (SQLException ex) {
            throw new DataAccessException(ex);
        } finally {
            da.closeConnection(conn);
        }
    }
}

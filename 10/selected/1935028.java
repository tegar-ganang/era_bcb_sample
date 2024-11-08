package avoware.intchat.server.db;

import avoware.intchat.server.IntChatServer;
import avoware.intchat.server.IntChatServerSettings;
import avoware.intchat.shared.IntChatConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 *
 * @author Andrew Orlov
 */
public class IntChatDatabaseStructure {

    /**
     * Database structure used by Internal Chat Server since 5.5.9 (first public
     * release) up to 5.6.6 (last current public release) is supposed to be
     * revision 1. It is important to note that 5.5.9 and 5.5.11 versions
     * of server were supplied with PostgreSQL initialization script only. We
     * must take it into account in conversion routine.
     *
     * Final database structure of revision 1 (together with MySQL script) was
     * created on 24/07/2008, in third public release (5.5.12).
     */
    private static final int DATABASE_REVISION = 2;

    /**
     * Name of parameter in ic_systemsettings table that stores database revision.
     */
    private static final String DATABASE_REVISION_PARAMNAME = "DATABASE_REVISION";

    private static Database database = Database.create(Table.create("ic_systemsettings", Column.create("paramname", Types.VARCHAR), Column.create("paramval", (IntChatDatabaseOperations.getDialect() == IntChatDatabaseOperations.DIALECT_POSTGRESQL ? Types.VARCHAR : Types.LONGVARCHAR))), Table.create("ic_ldap", Column.create("id", Types.INTEGER), Column.create("ldapdomain", Types.VARCHAR), Column.create("ldapurl", Types.VARCHAR), Column.create("ldapprincipalformat", Types.VARCHAR), Column.create("ldapcnattribute", Types.VARCHAR), Column.create("ldaploginattribute", Types.VARCHAR), Column.create("ldapmappingattribute", Types.VARCHAR), Column.create("ldapisbinaryattribute", Types.BIT), Column.create("ldapsearchbase", Types.VARCHAR), Column.create("ldapfilter", Types.VARCHAR)), Table.create("ic_templates", Column.create("templatename", Types.VARCHAR)), Table.create("ic_modules", Column.create("modulename", Types.VARCHAR)), Table.create("ic_permissions", Column.create("permission", Types.VARCHAR)), Table.create("ic_groups", Column.create("id", Types.INTEGER), Column.create("groupname", Types.VARCHAR), Column.create("pid", Types.INTEGER)), Table.create("ic_messagetypes", Column.create("id", Types.INTEGER), Column.create("typename", Types.VARCHAR), Column.create("templatename", Types.VARCHAR), Column.create("defaultpermission", Types.VARCHAR)), Table.create("ic_users", Column.create("id", Types.INTEGER), Column.create("isvalid", Types.BIT), Column.create("username", Types.VARCHAR), Column.create("timezone", Types.VARCHAR), Column.create("locale", Types.VARCHAR), Column.create("gid", Types.INTEGER), Column.create("email", Types.VARCHAR), Column.create("phonelocal", Types.VARCHAR), Column.create("phoneext", Types.VARCHAR), Column.create("phonemob", Types.VARCHAR), Column.create("fax", Types.VARCHAR), Column.create("birthday", Types.DATE), Column.create("userposition", Types.VARCHAR), Column.create("picture", (IntChatDatabaseOperations.getDialect() == IntChatDatabaseOperations.DIALECT_POSTGRESQL ? Types.VARCHAR : Types.LONGVARCHAR)), Column.create("ipint", Types.VARCHAR), Column.create("ipext", Types.VARCHAR), Column.create("hostname", Types.VARCHAR), Column.create("dnsname", Types.VARCHAR), Column.create("clientversion", Types.VARCHAR)), Table.create("ic_auth_internal", Column.create("uid", Types.INTEGER), Column.create("userlogin", Types.VARCHAR), Column.create("userpassword", Types.VARCHAR)), Table.create("ic_auth_external", Column.create("uid", Types.INTEGER), Column.create("ldapmapping", Types.VARCHAR), Column.create("lid", Types.INTEGER)), Table.create("ic_moduleaccess", Column.create("uid", Types.INTEGER), Column.create("modulename", Types.VARCHAR), Column.create("moduleaccess", Types.BIT)), Table.create("ic_messagetypepermissions", Column.create("uid", Types.INTEGER), Column.create("tid", Types.INTEGER), Column.create("permission", Types.VARCHAR)), Table.create("ic_messages", Column.create("id", Types.BIGINT), Column.create("tid", Types.INTEGER), Column.create("mhead", (IntChatDatabaseOperations.getDialect() == IntChatDatabaseOperations.DIALECT_POSTGRESQL ? Types.VARCHAR : Types.LONGVARCHAR)), Column.create("mbody", (IntChatDatabaseOperations.getDialect() == IntChatDatabaseOperations.DIALECT_POSTGRESQL ? Types.VARCHAR : Types.LONGVARCHAR)), Column.create("mdate", Types.BIGINT), Column.create("sid", Types.INTEGER)), Table.create("ic_messages_binarydata", Column.create("mid", Types.BIGINT), Column.create("chunk_id", Types.BIGINT), Column.create("chunk_data", (IntChatDatabaseOperations.getDialect() == IntChatDatabaseOperations.DIALECT_POSTGRESQL ? Types.BINARY : Types.LONGVARBINARY))), Table.create("ic_recipients", Column.create("mid", Types.BIGINT), Column.create("rid", Types.INTEGER), Column.create("processed", Types.BIT), Column.create("pcomment", (IntChatDatabaseOperations.getDialect() == IntChatDatabaseOperations.DIALECT_POSTGRESQL ? Types.VARCHAR : Types.LONGVARCHAR)), Column.create("pdate", Types.BIGINT)), Table.create("ic_journal", Column.create("uid", Types.INTEGER), Column.create("_in", Types.BIGINT), Column.create("_out", Types.BIGINT)), Table.create("ic_recipientlists", Column.create("id", Types.INTEGER), Column.create("uid", Types.INTEGER), Column.create("listname", Types.VARCHAR), Column.create("isshared", Types.BIT)), Table.create("ic_recipientlistmembers", Column.create("lid", Types.INTEGER), Column.create("uid", Types.INTEGER)), Table.create("ic_journalselection", Column.create("uid", Types.INTEGER), Column.create("sid", Types.INTEGER)));

    public static boolean check(Connection conn) throws InterruptedException {
        try {
            checkStructure(conn);
            checkVersion(conn);
        } catch (SQLException se) {
            System.err.println(IntChatConstants.getCurrentTimeString() + "database structure error");
            System.err.println(se.getLocalizedMessage());
            return false;
        }
        return true;
    }

    private static void checkStructure(Connection conn) throws InterruptedException, SQLException {
        for (int i = 0; i < database.getTables().length; i++) {
            Table table = database.getTables()[i];
            ResultSet rs = IntChatDatabaseOperations.executeQuery(conn, "SELECT * FROM " + table.getName() + " LIMIT 1");
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int j = 0; j < table.getColumns().length; j++) {
                Column column = table.getColumns()[j];
                int k = rs.findColumn(column.getName());
                if (rsmd.getColumnType(k) != column.getType()) {
                    throw new SQLException("Wrong column type: " + table.getName() + "." + column.getName() + ", (code required)=" + column.getType() + ", (code found)=" + rsmd.getColumnType(k));
                }
            }
        }
    }

    private static void checkVersion(Connection conn) throws InterruptedException, SQLException {
        int revision = getRevision(conn);
        if (revision != DATABASE_REVISION) {
            throw new SQLException("Wrong database revision: expected revision is " + DATABASE_REVISION + " but got " + revision);
        }
    }

    public static void convert(IntChatServerSettings icss, Connection conn) throws InterruptedException, SQLException, IOException {
        int revision = getRevision(conn);
        for (int i = revision; i < DATABASE_REVISION; i++) {
            switch(i) {
                case 1:
                    convert_rev1_to_rev2(icss, conn);
                    break;
            }
        }
    }

    private static int getRevision(Connection conn) throws InterruptedException, SQLException {
        int revision = 1;
        ResultSet rs = IntChatDatabaseOperations.executeQuery(conn, "SELECT paramval FROM ic_systemsettings WHERE paramname='" + DATABASE_REVISION_PARAMNAME + "' LIMIT 1");
        if (rs != null && rs.next()) {
            revision = Integer.parseInt(rs.getString("paramval"));
        }
        return revision;
    }

    private static void convert_rev1_to_rev2(IntChatServerSettings icss, Connection conn) throws SQLException, InterruptedException, IOException {
        System.out.println("Database structure conversion from Revision 1 to Revision 2 is started.\n" + "It may take a long time (about an hour or even more!) depending on size of your database and server performance.\n" + "Please, be a patient.");
        try {
            checkStructure(conn);
            System.out.println("Stage 1: database structure is already converted.");
        } catch (SQLException se) {
            URL sqlScriptUrl = null;
            switch(IntChatDatabaseOperations.getDialect()) {
                case IntChatDatabaseOperations.DIALECT_POSTGRESQL:
                    sqlScriptUrl = IntChatDatabaseStructure.class.getResource("/avoware/intchat/server/db/diff_postgresql_rev1_rev2.sql");
                    break;
                case IntChatDatabaseOperations.DIALECT_MYSQL:
                    sqlScriptUrl = IntChatDatabaseStructure.class.getResource("/avoware/intchat/server/db/diff_mysql_rev1_rev2.sql");
                    break;
                default:
                    throw new SQLException("Unsupported SQL dialect");
            }
            System.out.println("Stage 1: convert database structure itself, moving inner data.");
            String[] instructions = simpleParse(sqlScriptUrl.openStream());
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < instructions.length; i++) {
                    System.out.println("Executing: " + instructions[i] + ";");
                    IntChatDatabaseOperations.executeUpdate(conn, instructions[i]);
                    System.out.println("done...\n");
                }
                System.out.print("Commiting structure changes... ");
                conn.commit();
                System.out.println("done.");
            } finally {
                conn.setAutoCommit(true);
            }
        }
        System.out.println("Stage 1: is completed.");
        System.out.println("Stage 2: we should move file data from file pool into database.");
        String LAST_UPLOADED_FILE_ID = "96b342f5-d367-461c-9ec3-ab0b484d8018";
        long lastUploadedFileId = -1;
        ResultSet rs = IntChatDatabaseOperations.executeQuery(conn, "SELECT paramval FROM ic_systemsettings WHERE paramname='" + LAST_UPLOADED_FILE_ID + "' LIMIT 1");
        if (rs.next()) {
            lastUploadedFileId = Long.parseLong(rs.getString(1));
        } else {
            IntChatDatabaseOperations.executeUpdate(conn, "INSERT INTO ic_systemsettings (paramname, paramval) VALUES ('" + LAST_UPLOADED_FILE_ID + "', '-1')");
        }
        rs = null;
        rs = IntChatDatabaseOperations.executeQuery(conn, "SELECT count(*) FROM ic_messages, ic_recipients WHERE ic_messages.id>" + lastUploadedFileId + " AND ic_messages.tid=(SELECT id FROM ic_messagetypes WHERE templatename='IC_FILES' LIMIT 1) " + "AND ic_messages.id=ic_recipients.mid AND ic_recipients.processed=FALSE");
        if (rs.next()) {
            int filesToUpload = rs.getInt(1);
            System.out.println("Files to upload: " + filesToUpload);
            rs = null;
            String fileSpool = icss.getStringValue("FileSpool");
            if (!(new File(fileSpool).isAbsolute())) fileSpool = IntChatServer.SERVER_DIR_ABSOLUTE_PATH + IntChatServer.FILE_SEPARATOR + fileSpool;
            rs = IntChatDatabaseOperations.executeQuery(conn, "SELECT ic_messages.id, ic_messages.mbody FROM ic_messages, ic_recipients WHERE ic_messages.id>" + lastUploadedFileId + " AND ic_messages.tid=(SELECT id FROM ic_messagetypes WHERE templatename='IC_FILES' LIMIT 1) " + "AND ic_messages.id=ic_recipients.mid AND ic_recipients.processed=FALSE GROUP BY ic_messages.id, ic_messages.mbody ORDER BY ic_messages.id");
            conn.setAutoCommit(false);
            try {
                int currentFile = 0;
                while (rs.next()) {
                    currentFile++;
                    long id = rs.getLong("id");
                    InputStream in = null;
                    try {
                        File f = new File(fileSpool + IntChatServer.FILE_SEPARATOR + rs.getString("mbody"));
                        in = new FileInputStream(f);
                        long fileLength = f.length();
                        System.out.print("Uploading file " + currentFile + " of " + filesToUpload + " (id=" + id + ")... ");
                        if (!avoware.intchat.server.servlet.File.insertBLOB(conn, in, fileLength, id, 0)) {
                            in.close();
                            System.out.print("rolling back transaction... ");
                            conn.rollback();
                            System.out.println("exiting");
                            return;
                        }
                        in.close();
                        IntChatDatabaseOperations.executeUpdate(conn, "UPDATE ic_messages SET mbody='" + fileLength + "' WHERE id=" + id);
                        IntChatDatabaseOperations.executeUpdate(conn, "UPDATE ic_systemsettings SET paramval='" + id + "' WHERE paramname='" + LAST_UPLOADED_FILE_ID + "'");
                    } catch (FileNotFoundException fnfe) {
                        System.out.print("File " + currentFile + " of " + filesToUpload + " (id=" + id + ") is not found, writing FILE_NOT_FOUND info... ");
                        IntChatDatabaseOperations.executeUpdate(conn, "UPDATE ic_recipients SET processed=TRUE, pcomment='" + IntChatConstants.FileOperations.FILE_NOT_FOUND + "', pdate=" + System.currentTimeMillis() + " WHERE mid=" + id + " AND processed=FALSE");
                    }
                    conn.commit();
                    System.out.println("done");
                }
                IntChatDatabaseOperations.executeUpdate(conn, "DELETE FROM ic_systemsettings WHERE paramname='" + DATABASE_REVISION_PARAMNAME + "'");
                IntChatDatabaseOperations.executeUpdate(conn, "INSERT INTO ic_systemsettings (paramname, paramval) VALUES ('" + DATABASE_REVISION_PARAMNAME + "', '2')");
                IntChatDatabaseOperations.executeUpdate(conn, "DELETE FROM ic_systemsettings WHERE paramname='" + LAST_UPLOADED_FILE_ID + "'");
                conn.commit();
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static String[] simpleParse(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringWriter sw = new StringWriter();
        int c;
        while ((c = br.read()) > -1) {
            sw.write(c);
        }
        br = null;
        String sqlWithoutComments = sw.toString().replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)|(?:--.*)", "");
        sw = null;
        return sqlWithoutComments.split(";");
    }
}

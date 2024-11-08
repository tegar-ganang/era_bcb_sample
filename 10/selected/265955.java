package frost.storage.database.applayer;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import frost.fileTransfer.*;
import frost.fileTransfer.download.*;
import frost.storage.database.*;

/**
 * Stores manually added download files
 */
public class DownloadFilesDatabaseTable extends AbstractDatabaseTable {

    private static Logger logger = Logger.getLogger(DownloadFilesDatabaseTable.class.getName());

    private static final String SQL_DDL = "CREATE TABLE IF NOT EXISTS DOWNLOADFILES (" + "name VARCHAR NOT NULL," + "targetpath VARCHAR," + "size BIGINT," + "fnkey VARCHAR," + "enabled BOOLEAN," + "state INT," + "downloadaddedtime BIGINT," + "downloadstartedtime BIGINT," + "downloadfinishedtime BIGINT," + "retries INT," + "lastdownloadstoptime BIGINT," + "gqid VARCHAR," + "filelistfilesha VARCHAR," + "CONSTRAINT DOWNLOADFILES_1 UNIQUE (fnkey) )";

    public List<String> getTableDDL() {
        ArrayList<String> lst = new ArrayList<String>(1);
        lst.add(SQL_DDL);
        return lst;
    }

    public boolean compact(Statement stmt) throws SQLException {
        stmt.executeUpdate("COMPACT TABLE DOWNLOADFILES");
        return true;
    }

    public void saveDownloadFiles(List downloadFiles) throws SQLException {
        Connection conn = AppLayerDatabase.getInstance().getPooledConnection();
        try {
            conn.setAutoCommit(false);
            Statement s = conn.createStatement();
            s.executeUpdate("DELETE FROM DOWNLOADFILES");
            s.close();
            s = null;
            PreparedStatement ps = conn.prepareStatement("INSERT INTO DOWNLOADFILES " + "(name,targetpath,size,fnkey,enabled,state,downloadaddedtime,downloadstartedtime,downloadfinishedtime," + "retries,lastdownloadstoptime,gqid,filelistfilesha) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
            for (Iterator i = downloadFiles.iterator(); i.hasNext(); ) {
                FrostDownloadItem dlItem = (FrostDownloadItem) i.next();
                int ix = 1;
                ps.setString(ix++, dlItem.getFilename());
                ps.setString(ix++, dlItem.getTargetPath());
                ps.setLong(ix++, (dlItem.getFileSize() == null ? 0 : dlItem.getFileSize().longValue()));
                ps.setString(ix++, dlItem.getKey());
                ps.setBoolean(ix++, (dlItem.isEnabled() == null ? true : dlItem.isEnabled().booleanValue()));
                ps.setInt(ix++, dlItem.getState());
                ps.setLong(ix++, dlItem.getDownloadAddedTime());
                ps.setLong(ix++, dlItem.getDownloadStartedTime());
                ps.setLong(ix++, dlItem.getDownloadFinishedTime());
                ps.setInt(ix++, dlItem.getRetries());
                ps.setLong(ix++, dlItem.getLastDownloadStopTime());
                ps.setString(ix++, dlItem.getGqIdentifier());
                ps.setString(ix++, dlItem.getFileListFileObject() == null ? null : dlItem.getFileListFileObject().getSha());
                ps.executeUpdate();
            }
            ps.close();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Exception during save", t);
            try {
                conn.rollback();
            } catch (Throwable t1) {
                logger.log(Level.SEVERE, "Exception during rollback", t1);
            }
            try {
                conn.setAutoCommit(true);
            } catch (Throwable t1) {
            }
        } finally {
            AppLayerDatabase.getInstance().givePooledConnection(conn);
        }
    }

    public List<FrostDownloadItem> loadDownloadFiles() throws SQLException {
        LinkedList<FrostDownloadItem> downloadItems = new LinkedList<FrostDownloadItem>();
        AppLayerDatabase db = AppLayerDatabase.getInstance();
        PreparedStatement ps = db.prepareStatement("SELECT " + "name,targetpath,size,fnkey,enabled,state,downloadaddedtime,downloadstartedtime,downloadfinishedtime," + "retries,lastdownloadstoptime,gqid,filelistfilesha FROM DOWNLOADFILES");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int ix = 1;
            String filename = rs.getString(ix++);
            String targetPath = rs.getString(ix++);
            long size = rs.getLong(ix++);
            String key = rs.getString(ix++);
            boolean enabledownload = rs.getBoolean(ix++);
            int state = rs.getInt(ix++);
            long downloadAddedTime = rs.getLong(ix++);
            long downloadStartedTime = rs.getLong(ix++);
            long downloadFinishedTime = rs.getLong(ix++);
            int retries = rs.getInt(ix++);
            long lastDownloadStopTime = rs.getLong(ix++);
            String gqId = rs.getString(ix++);
            String sharedFileSha = rs.getString(ix++);
            FrostFileListFileObject sharedFileObject = null;
            if (sharedFileSha != null && sharedFileSha.length() > 0) {
                sharedFileObject = AppLayerDatabase.getFileListDatabaseTable().retrieveFileBySha(sharedFileSha);
                if (sharedFileObject == null && key == null) {
                    logger.warning("DownloadUpload items file list file object does not exist, and there is no key. " + "Removed from upload files: " + filename);
                }
            }
            FrostDownloadItem dlItem = new FrostDownloadItem(filename, targetPath, (size == 0 ? null : new Long(size)), key, Boolean.valueOf(enabledownload), state, downloadAddedTime, downloadStartedTime, downloadFinishedTime, retries, lastDownloadStopTime, gqId);
            dlItem.setFileListFileObject(sharedFileObject);
            downloadItems.add(dlItem);
        }
        rs.close();
        ps.close();
        return downloadItems;
    }
}

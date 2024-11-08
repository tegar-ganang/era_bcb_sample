package frost.storage.database.applayer;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import frost.*;
import frost.fileTransfer.sharing.*;
import frost.storage.database.*;
import frost.util.gui.translation.*;

/**
 * This table contains all our own shared files.
 * One file can be shared only by one local identity.
 */
public class SharedFilesDatabaseTable extends AbstractDatabaseTable {

    private static Logger logger = Logger.getLogger(SharedFilesDatabaseTable.class.getName());

    private static final String SQL_SHAREDFILES_DDL = "CREATE TABLE IF NOT EXISTS SHAREDFILES (" + "path VARCHAR NOT NULL," + "size BIGINT NOT NULL," + "fnkey VARCHAR," + "sha VARCHAR NOT NULL," + "owner VARCHAR NOT NULL," + "comment VARCHAR," + "rating INT," + "keywords VARCHAR," + "lastuploaded BIGINT," + "uploadcount INT," + "reflastsent BIGINT NOT NULL," + "requestlastreceived BIGINT," + "requestsreceivedcount INT," + "lastmodified BIGINT," + "CONSTRAINT SHAREDFILES_1 UNIQUE(sha) )";

    public List<String> getTableDDL() {
        ArrayList<String> lst = new ArrayList<String>(1);
        lst.add(SQL_SHAREDFILES_DDL);
        return lst;
    }

    public boolean compact(Statement stmt) throws SQLException {
        stmt.executeUpdate("COMPACT TABLE SHAREDFILES");
        return true;
    }

    public void saveSharedFiles(List<FrostSharedFileItem> sfFiles) throws SQLException {
        Connection conn = AppLayerDatabase.getInstance().getPooledConnection();
        try {
            conn.setAutoCommit(false);
            Statement s = conn.createStatement();
            s.executeUpdate("DELETE FROM SHAREDFILES");
            s.close();
            s = null;
            PreparedStatement ps = conn.prepareStatement("INSERT INTO SHAREDFILES (" + "path,size,fnkey,sha,owner,comment,rating,keywords," + "lastuploaded,uploadcount,reflastsent,requestlastreceived,requestsreceivedcount,lastmodified) " + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            for (Iterator<FrostSharedFileItem> i = sfFiles.iterator(); i.hasNext(); ) {
                FrostSharedFileItem sfItem = i.next();
                int ix = 1;
                ps.setString(ix++, sfItem.getFile().getPath());
                ps.setLong(ix++, sfItem.getFileSize());
                ps.setString(ix++, sfItem.getChkKey());
                ps.setString(ix++, sfItem.getSha());
                ps.setString(ix++, sfItem.getOwner());
                ps.setString(ix++, sfItem.getComment());
                ps.setInt(ix++, sfItem.getRating());
                ps.setString(ix++, sfItem.getKeywords());
                ps.setLong(ix++, sfItem.getLastUploaded());
                ps.setInt(ix++, sfItem.getUploadCount());
                ps.setLong(ix++, sfItem.getRefLastSent());
                ps.setLong(ix++, sfItem.getRequestLastReceived());
                ps.setInt(ix++, sfItem.getRequestsReceived());
                ps.setLong(ix++, sfItem.getLastModified());
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

    public List<FrostSharedFileItem> loadSharedFiles() throws SQLException {
        LinkedList<FrostSharedFileItem> sfItems = new LinkedList<FrostSharedFileItem>();
        AppLayerDatabase db = AppLayerDatabase.getInstance();
        PreparedStatement ps = db.prepareStatement("SELECT " + "path,size,fnkey,sha,owner,comment,rating,keywords," + "lastuploaded,uploadcount,reflastsent,requestlastreceived,requestsreceivedcount,lastmodified " + "FROM SHAREDFILES");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int ix = 1;
            String filepath = rs.getString(ix++);
            long filesize = rs.getLong(ix++);
            String key = rs.getString(ix++);
            String sha = rs.getString(ix++);
            String owner = rs.getString(ix++);
            String comment = rs.getString(ix++);
            int rating = rs.getInt(ix++);
            String keywords = rs.getString(ix++);
            long lastUploaded = rs.getLong(ix++);
            int uploadCount = rs.getInt(ix++);
            long refLastSent = rs.getLong(ix++);
            long requestLastReceived = rs.getLong(ix++);
            int requestsReceivedCount = rs.getInt(ix++);
            long lastModified = rs.getLong(ix++);
            boolean fileIsOk = true;
            File file = new File(filepath);
            if (!Core.frostSettings.getBoolValue(SettingsClass.DISABLE_FILESHARING)) {
                Language language = Language.getInstance();
                if (!file.isFile()) {
                    String title = language.getString("StartupMessage.sharedFile.sharedFileNotFound.title");
                    String text = language.formatMessage("StartupMessage.sharedFile.sharedFileNotFound.text", filepath);
                    MainFrame.enqueueStartupMessage(title, text, JOptionPane.WARNING_MESSAGE);
                    logger.severe("Shared file does not exist: " + filepath);
                    fileIsOk = false;
                } else if (file.length() != filesize) {
                    String title = language.getString("StartupMessage.sharedFile.sharedFileSizeChanged.title");
                    String text = language.formatMessage("StartupMessage.sharedFile.sharedFileSizeChanged.text", filepath);
                    MainFrame.enqueueStartupMessage(title, text, JOptionPane.WARNING_MESSAGE);
                    logger.severe("Size of shared file changed: " + filepath);
                    fileIsOk = false;
                } else if (file.lastModified() != lastModified) {
                    String title = language.getString("StartupMessage.sharedFile.sharedFileLastModifiedChanged.title");
                    String text = language.formatMessage("StartupMessage.sharedFile.sharedFileLastModifiedChanged.text", filepath);
                    MainFrame.enqueueStartupMessage(title, text, JOptionPane.WARNING_MESSAGE);
                    logger.severe("Last modified date of shared file changed: " + filepath);
                    fileIsOk = false;
                }
            }
            FrostSharedFileItem sfItem = new FrostSharedFileItem(file, filesize, key, sha, owner, comment, rating, keywords, lastUploaded, uploadCount, refLastSent, requestLastReceived, requestsReceivedCount, lastModified, fileIsOk);
            sfItems.add(sfItem);
        }
        rs.close();
        ps.close();
        return sfItems;
    }
}

package hambo.messaging.hambo_db;

import java.util.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.io.*;
import javax.mail.*;
import javax.mail.search.*;
import hambo.messaging.*;
import hambo.util.HamboFatalException;
import hambo.user.*;
import hambo.svc.database.DBConnection;

/**
 * This class is used for retrieving filters, and perform matching of 
 * messages against them.
 */
public class FiltersImpl extends MaildbAccessor implements Filters {

    /** <code>filterVect</code> Vector containing filters.*/
    protected Vector filterVect = new Vector();

    /** */
    private BigDecimal user_oid;

    private String userid;

    private MailStorageImpl storage;

    private boolean isloaded;

    /**
     * Factory specific instance creation.  This is only accessed from inside
     * the package.
     * @param userid the User Id.  May not be null or more than 30 characters.
     */
    FiltersImpl(BigDecimal user_oid, String userid, MailStorageImpl storage) {
        this.user_oid = user_oid;
        this.userid = userid;
        this.storage = storage;
        this.isloaded = false;
    }

    public void deleteFilter(long filterid) {
        DBConnection con = getConnection(false);
        try {
            PreparedStatement query = con.prepareStatement("delete msg_FilterRedirect where filterID=?");
            query.setLong(1, filterid);
            con.executeUpdate(query, null);
            query = con.prepareStatement("delete msg_Filter where ID=? and user_oid=?");
            query.setLong(1, filterid);
            query.setBigDecimal(2, user_oid);
            if (con.executeUpdate(query, null) == 0) {
                throw new HamboFatalException("Tried to delete bogus filter #" + filterid);
            }
            con.commit();
        } catch (Exception e) {
            try {
                con.rollback();
            } catch (Exception err2) {
                logError("Failed to rollback after " + e, err2);
            }
            if (e instanceof RuntimeException) throw (RuntimeException) e; else throw new HamboFatalException("Failed to delete filter", e);
        } finally {
            releaseConnection(con);
        }
    }

    public void reOrder(long index, boolean moveUp) {
        DBConnection con = getConnection(false);
        try {
            PreparedStatement stmt = con.prepareStatement("select sequence from msg_Filter where user_oid=? and ID=?");
            stmt.setBigDecimal(1, user_oid);
            stmt.setLong(2, index);
            ResultSet rs = con.executeQuery(stmt, null);
            if (!rs.next()) throw new HamboFatalException("No such filter");
            int seq1 = rs.getInt(1);
            rs.close();
            stmt = con.prepareStatement("select ID, sequence from msg_Filter where user_oid=? and" + " sequence=(select " + (moveUp ? "max" : "min") + " (sequence) from msg_Filter where user_oid=? and sequence" + (moveUp ? "<" : ">") + "?)");
            stmt.setBigDecimal(1, user_oid);
            stmt.setBigDecimal(2, user_oid);
            stmt.setInt(3, seq1);
            rs = con.executeQuery(stmt, null);
            if (!rs.next()) throw new HamboFatalException("Cant move filter that way");
            long index2 = rs.getLong(1);
            int seq2 = rs.getInt(2);
            stmt = con.prepareStatement("update msg_Filter set sequence=? where ID=?");
            stmt.setInt(1, seq2);
            stmt.setLong(2, index);
            con.executeUpdate(stmt, null);
            stmt.setInt(1, seq1);
            stmt.setLong(2, index2);
            con.executeUpdate(stmt, null);
            con.commit();
        } catch (Exception err) {
            rollback(con);
            if (err instanceof RuntimeException) throw (RuntimeException) err; else throw new HamboFatalException("Failed to reorder filters", err);
        } finally {
            releaseConnection(con);
        }
    }

    private long[] getLongResultArray(ResultSet rs, int column) throws SQLException {
        Vector result = new Vector();
        while (rs.next()) {
            result.add(new Long(rs.getLong(column)));
        }
        long[] array = new long[result.size()];
        for (int i = 0; i < result.size(); i++) array[i] = ((Long) result.get(i)).longValue();
        return array;
    }

    public void makeFilter(long index, String name, FolderType folder, Vector smss, Vector nsms, Vector emails, SearchTerm condition) {
        DBConnection con = getConnection(false);
        try {
            long filterid = 0;
            BigDecimal folderid = null;
            try {
                folderid = storage.getFolderID(con, folder);
            } catch (FolderException err) {
                logError("Failed to get folder for filter", err);
            }
            PreparedStatement query = null;
            if (index == 0) {
                Inserter inserter = new Inserter(con, "insert into msg_Filter (user_oid, name, folderID," + " sequence, condition)" + " select ?, ?, ?, isnull(max(sequence), 0)+1, ?" + " from msg_Filter where user_oid=?", "msg_Filter");
                inserter.setBigDecimal(1, user_oid);
                inserter.setString(2, name);
                inserter.setBigDecimal(3, folderid);
                inserter.setBytes(4, filterData(condition));
                inserter.setBigDecimal(5, user_oid);
                filterid = inserter.execute();
            } else {
                query = con.prepareStatement("update msg_Filter set name=?, folderID=?, condition=?" + " where user_oid=? and ID=?");
                query.setString(1, name);
                query.setBigDecimal(2, folderid);
                query.setBytes(3, filterData(condition));
                query.setBigDecimal(4, user_oid);
                filterid = index;
                query.setLong(5, filterid);
                if (con.executeUpdate(query, null) == 0) {
                    con.rollback();
                    throw new HamboFatalException("Hacker alert...tried to update something that did not belong to the user!");
                }
                query = con.prepareStatement("delete msg_FilterRedirect where filterID=?");
                query.setLong(1, filterid);
                con.executeUpdate(query, null);
            }
            for (int i = 0; i < smss.size(); ++i) {
                query = con.prepareStatement("insert into msg_FilterRedirect" + " (filterID, type, recipient, split_max)" + " values (?,2,?,?)");
                query.setLong(1, filterid);
                query.setString(2, (String) smss.elementAt(i));
                query.setInt(3, ((Integer) nsms.elementAt(i)).intValue());
                con.executeUpdate(query, null);
            }
            for (int i = 0; i < emails.size(); ++i) {
                query = con.prepareStatement("insert into msg_FilterRedirect" + " (filterID, type, recipient, split_max)" + " values (?,1,?,0)");
                query.setLong(1, filterid);
                query.setString(2, (String) emails.elementAt(i));
                con.executeUpdate(query, null);
            }
            con.commit();
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (Exception err2) {
                logError("Failed to rollback after " + e, err2);
            }
            throw new HamboFatalException("Failed to create filter", e);
        } finally {
            releaseConnection(con);
        }
    }

    private byte[] filterData(SearchTerm term) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutput o = new ObjectOutputStream(b);
            o.writeObject(term);
            o.close();
            return b.toByteArray();
        } catch (IOException e) {
            throw new HamboFatalException("Broken filter data", e);
        }
    }

    public Vector getBasicVect() {
        DBConnection con = getConnection();
        ResultSet rs = null;
        try {
            PreparedStatement query = con.prepareStatement("select msg_Filter.ID, msg_Filter.name as name," + " msg_Folder.name as folder, condition" + " from msg_Filter, msg_Folder" + " where msg_Filter.user_oid=? and msg_Folder.ID=folderID" + " order by sequence, name");
            query.setBigDecimal(1, user_oid);
            rs = con.executeQuery(query, null);
            Vector filterVect = new Vector();
            while (rs.next()) {
                long id = rs.getLong("ID");
                String name = rs.getString("name");
                FolderType folder = Messaging.folderType(rs.getString("folder"));
                ObjectInput data = new ObjectInputStream(rs.getBinaryStream("condition"));
                SearchTerm condition = (SearchTerm) data.readObject();
                data.close();
                filterVect.add(new Filter(id, name, folder, condition));
            }
            return filterVect;
        } catch (ClassNotFoundException e) {
            throw new HamboFatalException("Unknown class in filter data", e);
        } catch (IOException e) {
            throw new HamboFatalException("Broken filter data", e);
        } catch (SQLException e) {
            throw new HamboFatalException("Failed to get filters", e);
        } finally {
            close(rs);
            releaseConnection(con);
        }
    }

    public Filter getFilter(long filterid) {
        DBConnection con = getConnection();
        ResultSet rs = null;
        try {
            PreparedStatement query = con.prepareStatement("select msg_Filter.name as name, msg_Folder.name as folder," + " condition from msg_Filter, msg_Folder" + " where msg_Filter.user_oid=? and msg_Folder.ID=folderID" + " and msg_Filter.ID=?");
            query.setBigDecimal(1, user_oid);
            query.setLong(2, filterid);
            rs = con.executeQuery(query, null);
            if (!rs.next()) throw new HamboFatalException("No such filter #" + filterid);
            String name = rs.getString("name");
            FolderType folder = Messaging.folderType(rs.getString("folder"));
            ObjectInput data = new ObjectInputStream(rs.getBinaryStream("condition"));
            SearchTerm condition = (SearchTerm) data.readObject();
            data.close();
            Filter result = new Filter(filterid, name, folder, condition);
            rs.close();
            rs = null;
            con.reset();
            query = con.prepareStatement("select type, recipient, split_max from msg_FilterRedirect" + " where filterID=?");
            query.setLong(1, filterid);
            rs = con.executeQuery(query, null);
            while (rs.next()) {
                int type = rs.getInt("type");
                String value = rs.getString("recipient");
                if (value != null && !value.equals("")) {
                    if (type == 1) result.addEmailFwd(value); else if (type == 2) result.addMobFwd(value, rs.getInt("split_max"));
                }
            }
            return result;
        } catch (ClassNotFoundException e) {
            throw new HamboFatalException("Unknown class in filter data", e);
        } catch (IOException e) {
            throw new HamboFatalException("Broken filter data", e);
        } catch (SQLException err) {
            throw new HamboFatalException("Failed to load filter #" + filterid, err);
        } finally {
            close(rs);
            releaseConnection(con);
        }
    }

    public FolderType filterMessage(Message msg, SendBuffer msgsender) {
        for (int i = 0; i < filterVect.size(); i++) {
            Filter filter = (Filter) filterVect.elementAt(i);
            if (filter.condition.match(msg)) {
                checkNotify(msg, filter, msgsender);
                return filter.folder;
            }
        }
        return new FolderType(FolderType.DEFAULT);
    }

    private void checkNotify(Message msg, Filter filter, SendBuffer msgsender) {
        DBConnection con = getConnection();
        try {
            Vector nrVect = new Vector();
            Vector addrVect = new Vector();
            PreparedStatement stmt = con.prepareStatement("select recipient, type, split_max from msg_Filter_Forward" + " where filterID=?");
            ResultSet rs = con.executeQuery(stmt, null);
            while (rs.next()) {
                switch(rs.getInt("type")) {
                    case 1:
                        addrVect.add(rs.getString("recipient"));
                    case 2:
                        nrVect.add(rs.getString("recipient"));
                }
            }
            rs.close();
            rs = null;
            con.reset();
            String mobilenr = UserManager.getUserManager().findUser(userid).getContactInfo().getMobileNumber();
            msgsender.sendMsgAsSMS(msg, (String[]) nrVect.toArray(new String[nrVect.size()]), mobilenr, filter.maxNumberOfSMS);
            msgsender.forwardEmail(msg, (String[]) addrVect.toArray(new String[addrVect.size()]));
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            releaseConnection(con);
        }
    }
}

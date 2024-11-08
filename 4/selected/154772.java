package net.sourceforge.mythtvj.mythtvprotocol.sql;

import net.sourceforge.mythtvj.mythtvprotocol.*;
import java.sql.*;
import java.util.Vector;

/**
 *
 * @author jjwin2k
 */
public class SQLConnection {

    /** The database driver that should be used i.e.the full qualified class name (?) */
    private static final String DBDRIVER = "com.mysql.jdbc.Driver";

    /** The current SQL connection for this object. */
    private java.sql.Connection dbConnection;

    /** The storagegroups the database contains. Useful to know them at connection time, 
     *  so later they can later be used as java objects. */
    private Storagegroup[] storagegroups;

    private Source[] sources;

    private Recorder[] recorders;

    /** Create a database connection. 
     * 
     * @param MasterBackendIP The IP of the SQL-Server
     * @param Port
     * @param SQLUsername
     * @param SQLPassword
     * @param SQLDatabase
     * @throws java.sql.SQLException
     */
    public SQLConnection(String MasterBackendIP, int Port, String SQLUsername, String SQLPassword, String SQLDatabase) throws SQLException {
        String dbURL = "jdbc:mysql://" + MasterBackendIP + ":" + Port + "" + "/" + SQLDatabase;
        try {
            Class.forName(DBDRIVER).newInstance();
            dbConnection = DriverManager.getConnection(dbURL, SQLUsername, SQLPassword);
            storagegroups = queryStoragegroups();
            sources = querySources();
            recorders = queryRecorders();
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    /**
     * Closes the Connection to the database.
     */
    public void close() throws SQLException {
        dbConnection.close();
    }

    /**
     * Reads the informationen for the given channel id from the database. All text content will be treatet as UTF8-encoded text.
     * @param chanid The channel ID (NOT the Number!)
     * @return A Channel Object representing the databse information.
     * @throws java.sql.SQLException
     */
    public Channel getChannel(int chanid) throws SQLException {
        PreparedStatement pstmt = dbConnection.prepareStatement("SELECT channum, sourceid, " + "CONVERT(CONVERT(callsign USING binary) USING UTF8) AS callsign, " + "CONVERT(CONVERT(name USING binary) USING UTF8) AS name, " + "visible FROM channel WHERE chanid = ? LIMIT 1");
        pstmt.setInt(1, chanid);
        ResultSet srs = pstmt.executeQuery();
        srs.first();
        return new Channel(chanid, srs.getInt("channum"), getSourceByID(srs.getInt("sourceid")), srs.getString("callsign"), srs.getString("name"), srs.getInt("visible") == 1);
    }

    /**
     * Reads the informaionen for the given channel number and the given source id from the database
     * @param sourceid The id source of the source on which the channel is located.
     * @param channum The channel number.
     * @return A Channel Object representing the databse information.
     * @throws java.sql.SQLException
     */
    public int getChannelIDBYChannum(int sourceid, int channum) throws SQLException {
        PreparedStatement pstmt = dbConnection.prepareStatement("SELECT chanid FROM channel WHERE channum = ? AND sourceid = ?");
        pstmt.setInt(1, channum);
        pstmt.setInt(2, sourceid);
        ResultSet srs = pstmt.executeQuery();
        srs.first();
        return srs.getInt("chanid");
    }

    /**
     * Reads EPG-Data from the database. All text content will be treatet as UTF8-encoded text.
     * @param channel The channel for which the program information should be read. 
     * @param from The date from when on the data should be read. (E.g. all entries past the 16.08.2003 16:55) 
     * @param limit How many entries should be read at most.
     * @return An array. Each element represents one program entry in the database.
     * @throws java.sql.SQLException
     */
    public Program[] getProgramForChannel(Channel channel, Timestamp from, int limit) throws SQLException {
        PreparedStatement pstmt = dbConnection.prepareStatement("SELECT starttime, endtime, " + "CONVERT(CONVERT(title USING binary) USING UTF8) AS title, " + "CONVERT(CONVERT(subtitle USING binary) USING UTF8) AS subtitle, " + "CONVERT(CONVERT(description USING binary) USING UTF8) AS description, " + "CONVERT(CONVERT(category USING binary) USING UTF8) AS category " + "FROM program WHERE chanid = ? " + "AND endtime >= ? LIMIT ?");
        pstmt.setInt(1, channel.getChanid());
        pstmt.setTimestamp(2, from);
        pstmt.setInt(3, limit);
        ResultSet srs = pstmt.executeQuery();
        Vector<Program> v = new Vector<Program>();
        while (srs.next()) {
            v.add(new Program(channel, srs.getTimestamp("starttime"), srs.getTimestamp("endtime"), srs.getString("title"), srs.getString("subtitle"), srs.getString("description"), srs.getString("category").split("/")));
        }
        Program[] returnValue = new Program[v.size()];
        return v.toArray(returnValue);
    }

    /**
     * Reads EPG-Data from the database. 
     * @param channel The channel for which the program information should be read.
     * @param from The date from when on the data should be read. (E.g. all entries past the 16.08.2003 16:55) 
     * @param to The date untilthe data should be read. (E.g. all entries before the 16.08.2003 16:55) 
     * @return An array. Each element represents one program entry in the database.
     * @throws java.sql.SQLException
     */
    public Program[] getProgramForChannel(Channel channel, Timestamp from, Timestamp to) throws SQLException {
        PreparedStatement pstmt = dbConnection.prepareStatement("SELECT starttime, endtime, " + "CONVERT(CONVERT(title USING binary) USING UTF8) AS title, " + "CONVERT(CONVERT(subtitle USING binary) USING UTF8) AS subtitle, " + "CONVERT(CONVERT(description USING binary) USING UTF8) AS description, " + "CONVERT(CONVERT(category USING binary) USING UTF8) AS category " + "FROM program WHERE chanid = ? " + "AND endtime >= ? " + "AND endtime <= ?");
        pstmt.setInt(1, channel.getChanid());
        pstmt.setTimestamp(2, from);
        pstmt.setTimestamp(3, to);
        ResultSet srs = pstmt.executeQuery();
        Vector<Program> v = new Vector<Program>();
        while (srs.next()) {
            v.add(new Program(channel, srs.getTimestamp("starttime"), srs.getTimestamp("endtime"), srs.getString("title"), srs.getString("subtitle"), srs.getString("description"), srs.getString("category").split("/")));
        }
        Program[] returnValue = new Program[v.size()];
        return v.toArray(returnValue);
    }

    /** 
     * Retrieves the source of the given recorder.
     * @param recorderid
     * @return The id of the source.
     * @throws java.sql.SQLException
     */
    public int getSourceByRecorder(int recorderid) throws SQLException {
        PreparedStatement pstmt = dbConnection.prepareStatement("SELECT sourceid FROM cardinput WHERE cardid = ? LIMIT 1");
        pstmt.setInt(1, recorderid);
        ResultSet srs = pstmt.executeQuery();
        srs.first();
        return srs.getInt("sourceid");
    }

    /** Retrieves all sources from the database.
     * 
     * @return
     * @throws java.sql.SQLException
     */
    private Source[] querySources() throws SQLException {
        Statement stmt = dbConnection.createStatement();
        ResultSet srs = stmt.executeQuery("SELECT sourceid, name FROM videosource");
        Vector<Source> v = new Vector<Source>();
        while (srs.next()) {
            v.add(new Source(srs.getInt("sourceid"), srs.getString("name")));
        }
        Source[] returnValue = new Source[v.size()];
        return v.toArray(returnValue);
    }

    /**
      * Retrieves all Recordings from the Database. All text content will be treatet as UTF8-encoded text.
      * @return An array. Each element represents one recording in the database. 
      * @throws java.sql.SQLException
      */
    public Recorded[] getRecordings() throws SQLException {
        Statement stmt = dbConnection.createStatement();
        ResultSet srs = stmt.executeQuery("SELECT chanid, starttime, endtime, " + "CONVERT(CONVERT(title USING binary) USING UTF8) AS title, " + "CONVERT(CONVERT(description USING binary) USING UTF8) AS description, " + "CONVERT(CONVERT(basename USING binary) USING UTF8) AS basename, " + "progstart, progend, storagegroup FROM recorded");
        Vector<Recorded> v = new Vector<Recorded>();
        while (srs.next()) {
            v.add(new Recorded(getChannel(srs.getInt("chanid")), srs.getDate("starttime"), srs.getDate("endtime"), srs.getString("title"), srs.getString("description"), srs.getString("basename"), srs.getDate("progstart"), srs.getDate("progend"), getStoragegroupByName(srs.getString("storagegroup"))));
        }
        Recorded[] returnValue = new Recorded[v.size()];
        return v.toArray(returnValue);
    }

    public Recorded[] getRecordingsDescending() throws SQLException {
        Statement stmt = dbConnection.createStatement();
        ResultSet srs = stmt.executeQuery("SELECT chanid, starttime, endtime, " + "CONVERT(CONVERT(title USING binary) USING UTF8) AS title, " + "CONVERT(CONVERT(description USING binary) USING UTF8) AS description, " + "CONVERT(CONVERT(basename USING binary) USING UTF8) AS basename, " + "progstart, progend, storagegroup FROM recorded ORDER BY recorded.starttime DESC");
        Vector<Recorded> v = new Vector<Recorded>();
        while (srs.next()) {
            v.add(new Recorded(getChannel(srs.getInt("chanid")), srs.getDate("starttime"), srs.getDate("endtime"), srs.getString("title"), srs.getString("description"), srs.getString("basename"), srs.getDate("progstart"), srs.getDate("progend"), getStoragegroupByName(srs.getString("storagegroup"))));
        }
        Recorded[] returnValue = new Recorded[v.size()];
        return v.toArray(returnValue);
    }

    public Recorded[] getRecordingsRecording() throws SQLException {
        Statement stmt = dbConnection.createStatement();
        ResultSet srs = stmt.executeQuery("SELECT chanid, starttime, endtime, " + "CONVERT(CONVERT(title USING binary) USING UTF8) AS title, " + "CONVERT(CONVERT(description USING binary) USING UTF8) AS description, " + "CONVERT(CONVERT(basename USING binary) USING UTF8) AS basename, " + "progstart, progend, storagegroup FROM recorded AND recorded.endtime >= NOW() " + "AND recorded.starttime <= NOW() ORDER BY recorded.starttime");
        Vector<Recorded> v = new Vector<Recorded>();
        while (srs.next()) {
            v.add(new Recorded(getChannel(srs.getInt("chanid")), srs.getDate("starttime"), srs.getDate("endtime"), srs.getString("title"), srs.getString("description"), srs.getString("basename"), srs.getDate("progstart"), srs.getDate("progend"), getStoragegroupByName(srs.getString("storagegroup"))));
        }
        Recorded[] returnValue = new Recorded[v.size()];
        return v.toArray(returnValue);
    }

    /**
     * Retrieves all Channels from the Database. All text content will be treatet as UTF8-encoded text.
     * @return An array. Each element represents one channel in the database. 
     * @throws java.sql.SQLException
     */
    public Channel[] getListOfChannels() throws SQLException {
        Statement stmt = dbConnection.createStatement();
        ResultSet srs = stmt.executeQuery("SELECT chanid, channum, sourceid, " + "CONVERT(CONVERT(callsign USING binary) USING UTF8) AS callsign, " + "CONVERT(CONVERT(name USING binary) USING UTF8) AS name, " + "visible FROM channel ORDER BY CAST(channum AS UNSIGNED)");
        Vector<Channel> v = new Vector<Channel>();
        while (srs.next()) {
            v.add(new Channel(srs.getInt("chanid"), srs.getInt("channum"), getSourceByID(srs.getInt("sourceid")), srs.getString("callsign"), srs.getString("name"), srs.getInt("visible") == 1));
        }
        Channel[] returnValue = new Channel[v.size()];
        return v.toArray(returnValue);
    }

    /** Retrieves all storagegroups from the database.
     * 
     * @return
     * @throws java.sql.SQLException
     */
    private Storagegroup[] queryStoragegroups() throws SQLException {
        Statement stmt = dbConnection.createStatement();
        ResultSet srs = stmt.executeQuery("SELECT * FROM storagegroup");
        Vector<Storagegroup> v = new Vector<Storagegroup>();
        v.add(new Storagegroup(-1, "LiveTV", "", ""));
        while (srs.next()) {
            v.add(new Storagegroup(srs.getInt("id"), srs.getString("groupname"), srs.getString("hostname"), srs.getString("dirname")));
        }
        Storagegroup[] returnValue = new Storagegroup[v.size()];
        return v.toArray(returnValue);
    }

    /**Retrieves all recorders (inputs) from the database.
     * 
     * @return
     * @throws java.sql.SQLException
     */
    private Recorder[] queryRecorders() throws SQLException {
        Statement stmt = dbConnection.createStatement();
        ResultSet srs = stmt.executeQuery("SELECT cardinput.cardinputid, cardinput.inputname, cardinput.sourceid, " + "capturecard.hostname  FROM cardinput, capturecard WHERE capturecard.cardid = cardinput.cardid");
        Vector<Recorder> v = new Vector<Recorder>();
        while (srs.next()) {
            v.add(new Recorder(srs.getInt("cardinput.cardinputid"), srs.getString("cardinput.inputname"), getSourceByID(srs.getInt("cardinput.sourceid")), srs.getString("capturecard.hostname")));
        }
        Recorder[] returnValue = new Recorder[v.size()];
        return v.toArray(returnValue);
    }

    /**
     * Returns the recorder with the given id.
     * @param id
     * @return
     */
    public Recorder getRecorderByID(int id) {
        for (Recorder r : recorders) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns the storagegroup with the given name.
     * @param name
     * @return
     */
    public Storagegroup getStoragegroupByName(String name) {
        for (Storagegroup s : storagegroups) {
            if (s.getGroupname().equals(name)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns all storagegroups.
     * @return
     */
    public Storagegroup[] getStoragegroups() {
        return storagegroups;
    }

    /**
     * Returns the source with the given id.
     * @param id
     * @return
     */
    public Source getSourceByID(int id) {
        for (Source s : sources) {
            if (s.getSourceid() == id) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns the source with the give name.
     * @param name
     * @return
     */
    public Source getSourceByName(String name) {
        for (Source s : sources) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns all sources.
     * @return
     */
    public Source[] getSources() {
        return sources;
    }

    /**
     * Returns all recorders.
     * @return
     */
    public Recorder[] getRecorders() {
        return recorders;
    }
}
